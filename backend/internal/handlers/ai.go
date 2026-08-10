package handlers

import (
	"bytes"
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"path/filepath"
	"strings"
	"time"

	"smartinventory/backend/internal/model"
)

const siliconFlowURL = "https://api.siliconflow.cn/v1/chat/completions"

var allowedImageExt = map[string]bool{
	".png":  true,
	".jpg":  true,
	".jpeg": true,
	".gif":  true,
}

// processedImage is a decoded, base64-encoded upload ready to send to the model.
type processedImage struct {
	Data string
	Type string
}

// aiRecord is the structured record returned to the front-end.
type aiRecord struct {
	Name        string `json:"name"`
	Category    string `json:"category"`
	Quantity    int    `json:"quantity"`
	Unit        string `json:"unit"`
	ExpiryDate  string `json:"expiryDate,omitempty"`
	Description string `json:"description"`
}

type siliconFlowMessage struct {
	Role    string      `json:"role"`
	Content interface{} `json:"content"`
}

type siliconFlowRequest struct {
	Model       string               `json:"model"`
	Messages    []siliconFlowMessage `json:"messages"`
	MaxTokens   int                  `json:"max_tokens"`
	Temperature float64              `json:"temperature"`
}

type siliconFlowResponse struct {
	Choices []struct {
		Message struct {
			Content string `json:"content"`
		} `json:"message"`
	} `json:"choices"`
}

// GenerateRecords handles POST /api/ai/generate-records.
// It accepts a multipart form with `api_key` and one or more `images`, sends
// them to the SiliconFlow vision model and returns structured inventory records.
func (h *Handler) GenerateRecords(w http.ResponseWriter, r *http.Request) {
	if err := r.ParseMultipartForm(10 << 20); err != nil {
		writeError(w, http.StatusBadRequest, "Invalid multipart form")
		return
	}

	apiKey := r.FormValue("api_key")
	if apiKey == "" {
		writeError(w, http.StatusBadRequest, "API key is required")
		return
	}

	files := r.MultipartForm.File["images"]
	if len(files) == 0 {
		writeError(w, http.StatusBadRequest, "No images provided")
		return
	}

	processed := make([]processedImage, 0, len(files))
	for _, fh := range files {
		ext := strings.ToLower(filepath.Ext(fh.Filename))
		if !allowedImageExt[ext] {
			continue
		}
		f, err := fh.Open()
		if err != nil {
			continue
		}
		data, err := io.ReadAll(f)
		_ = f.Close()
		if err != nil {
			continue
		}
		ctype := fh.Header.Get("Content-Type")
		if ctype == "" {
			ctype = "image/" + strings.TrimPrefix(ext, ".")
		}
		processed = append(processed, processedImage{
			Data: base64.StdEncoding.EncodeToString(data),
			Type: ctype,
		})
	}

	if len(processed) == 0 {
		writeError(w, http.StatusBadRequest, "No valid images processed")
		return
	}

	records, err := h.analyzeImagesWithSiliconFlow(apiKey, processed)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "Failed to generate records from images")
		return
	}
	if len(records) == 0 {
		writeError(w, http.StatusInternalServerError, "Failed to generate records from images")
		return
	}

	writeJSON(w, http.StatusOK, map[string]interface{}{
		"success": true,
		"records": records,
		"message": fmt.Sprintf("Successfully generated %d records", len(records)),
	})
}

func (h *Handler) analyzeImagesWithSiliconFlow(apiKey string, images []processedImage) ([]aiRecord, error) {
	now := time.Now()
	dateStr := now.Format("2006-01-02")
	weekdayMap := []string{"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"}
	weekdayStr := weekdayMap[int(now.Weekday())]

	systemContent := fmt.Sprintf(`
You are an intelligent inventory record generator that needs to identify items from images and generate inventory records.

Current date information: Today is %s, %s. Use this information to generate realistic expiry dates and ensure dates are in the future.

Please analyze all items in the images and output JSON data in the following format:
{
  "records": [
    {
      "name": "Item Name",
      "category": "Item Category",
      "quantity": Item Quantity,
      "unit": "Quantity Unit",
      "expiryDate": "Expiry Date (if any, format: YYYY-MM-DD)",
      "description": "Item Description (optional)"
    }
  ]
}

Item Category can only be one of the following: food, medicine, cleaning, personal, household, electronics

Please ensure the output JSON format is correct and does not include any additional text.
`, dateStr, weekdayStr)

	userContent := []interface{}{
		map[string]string{"type": "text", "text": "Please analyze these images and generate inventory records."},
	}
	for _, img := range images {
		userContent = append(userContent, map[string]interface{}{
			"type": "image_url",
			"image_url": map[string]string{
				"url": fmt.Sprintf("data:%s;base64,%s", img.Type, img.Data),
			},
		})
	}

	payload := siliconFlowRequest{
		Model:       "THUDM/GLM-4.1V-9B-Thinking",
		MaxTokens:   1000,
		Temperature: 0.2,
		Messages: []siliconFlowMessage{
			{Role: "system", Content: systemContent},
			{Role: "user", Content: userContent},
		},
	}

	body, err := json.Marshal(payload)
	if err != nil {
		return nil, err
	}

	ctx, cancel := context.WithTimeout(context.Background(), 90*time.Second)
	defer cancel()

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, siliconFlowURL, bytes.NewReader(body))
	if err != nil {
		return nil, err
	}
	req.Header.Set("Authorization", "Bearer "+apiKey)
	req.Header.Set("Content-Type", "application/json")

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	respBody, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, err
	}
	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("siliconflow status %d: %s", resp.StatusCode, string(respBody))
	}

	var sf siliconFlowResponse
	if err := json.Unmarshal(respBody, &sf); err != nil {
		return nil, err
	}
	if len(sf.Choices) == 0 {
		return nil, fmt.Errorf("siliconflow returned no choices")
	}
	content := sf.Choices[0].Message.Content

	var parsed struct {
		Records []map[string]interface{} `json:"records"`
	}
	if err := json.Unmarshal([]byte(content), &parsed); err != nil {
		return nil, err
	}

	validCategories := model.ValidCategories
	out := make([]aiRecord, 0)
	for _, rec := range parsed.Records {
		name, _ := rec["name"].(string)
		category, _ := rec["category"].(string)
		unit, _ := rec["unit"].(string)
		qtyRaw, ok := rec["quantity"]
		if !ok {
			continue
		}
		qty := toInt(qtyRaw)
		desc, _ := rec["description"].(string)

		if name == "" || category == "" || unit == "" || !validCategories[category] {
			continue
		}

		record := aiRecord{
			Name:        strings.TrimSpace(name),
			Category:    category,
			Quantity:    maxInt(1, qty),
			Unit:        strings.TrimSpace(unit),
			Description: strings.TrimSpace(desc),
		}
		if expiry, ok := rec["expiryDate"].(string); ok && expiry != "" {
			if t, err := time.Parse("2006-01-02", expiry); err == nil {
				record.ExpiryDate = t.Format("2006-01-02")
			}
		}
		out = append(out, record)
	}
	return out, nil
}

func toInt(v interface{}) int {
	switch n := v.(type) {
	case float64:
		return int(n)
	case int:
		return n
	case string:
		var i int
		fmt.Sscanf(n, "%d", &i)
		return i
	default:
		return 0
	}
}

func maxInt(a, b int) int {
	if a > b {
		return a
	}
	return b
}
