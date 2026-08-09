// Package handlers exposes the HTTP API for the inventory service.
//
// Every endpoint aims to be wire-compatible with the original Flask backend:
// same paths, same methods, same JSON envelope ({ success, data, message,
// pagination, error, error_code, message_code, ... }) and same camelCase keys.
package handlers

import (
	"encoding/json"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"runtime"
	"strconv"
	"strings"

	"smartinventory/backend/internal/model"
	"smartinventory/backend/internal/store"
)

// Config holds runtime configuration for the HTTP handlers.
type Config struct {
	// ExpensesURL is the upstream API the /expenses proxy forwards to.
	ExpensesURL string
	// StaticDir is the directory that holds the SPA front-end.
	StaticDir string
}

// Handler wires the store and config into HTTP handlers.
type Handler struct {
	Store  *store.Store
	Config Config
}

func writeJSON(w http.ResponseWriter, status int, v interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}

func writeError(w http.ResponseWriter, status int, err string) {
	writeJSON(w, status, map[string]interface{}{"success": false, "error": err})
}

func decodeJSON(r *http.Request, v interface{}) error {
	dec := json.NewDecoder(io.LimitReader(r.Body, 4<<20))
	return dec.Decode(v)
}

// ---------------------------------------------------------------------------
// Items
// ---------------------------------------------------------------------------

type createItemInput struct {
	Name        *string `json:"name"`
	Category    *string `json:"category"`
	Quantity    *int    `json:"quantity"`
	Unit        *string `json:"unit"`
	MinQuantity *int    `json:"minQuantity"`
	ExpiryDate  *string `json:"expiryDate"`
	UsageCount  *int    `json:"usageCount"`
	LastUsedAt  *string `json:"lastUsedAt"`
	Description *string `json:"description"`
}

func (h *Handler) ListItems(w http.ResponseWriter, r *http.Request) {
	q := r.URL.Query()
	search := q.Get("search")
	category := q.Get("category")
	page, _ := strconv.Atoi(q.Get("page"))
	perPage, _ := strconv.Atoi(q.Get("per_page"))
	if page < 1 {
		page = 1
	}
	if perPage < 1 {
		perPage = 50
	}

	items, total, pages, err := h.Store.List(search, category, page, perPage)
	if err != nil {
		writeError(w, http.StatusInternalServerError, "Internal server error")
		return
	}
	writeJSON(w, http.StatusOK, map[string]interface{}{
		"success": true,
		"data":    items,
		"pagination": map[string]interface{}{
			"page":     page,
			"pages":    pages,
			"per_page": perPage,
			"total":    total,
		},
	})
}

func (h *Handler) GetItem(w http.ResponseWriter, r *http.Request) {
	id, err := strconv.Atoi(r.PathValue("id"))
	if err != nil {
		writeError(w, http.StatusBadRequest, "Invalid item id")
		return
	}
	item, err := h.Store.Get(id)
	if err == store.ErrNotFound {
		writeError(w, http.StatusNotFound, "Item not found")
		return
	}
	if err != nil {
		writeError(w, http.StatusInternalServerError, "Internal server error")
		return
	}
	writeJSON(w, http.StatusOK, map[string]interface{}{"success": true, "data": item})
}

func (h *Handler) CreateItem(w http.ResponseWriter, r *http.Request) {
	var in createItemInput
	if err := decodeJSON(r, &in); err != nil {
		writeError(w, http.StatusBadRequest, "Invalid JSON body")
		return
	}

	required := map[string]interface{}{
		"name":     in.Name,
		"category": in.Category,
		"quantity": in.Quantity,
		"unit":     in.Unit,
	}
	for field, val := range required {
		switch v := val.(type) {
		case *string:
			if v == nil || strings.TrimSpace(*v) == "" {
				writeError(w, http.StatusBadRequest, "Missing required field: "+field)
				return
			}
		case *int:
			if v == nil || *v <= 0 {
				writeError(w, http.StatusBadRequest, "Missing required field: "+field)
				return
			}
		}
	}

	item := &model.InventoryItem{
		Name:        strings.TrimSpace(*in.Name),
		Category:    strings.TrimSpace(*in.Category),
		Quantity:    *in.Quantity,
		Unit:        strings.TrimSpace(*in.Unit),
		MinQuantity: derefInt(in.MinQuantity, 0),
		UsageCount:  derefInt(in.UsageCount, 0),
	}
	item.Description = descFromPtr(in.Description)
	if in.ExpiryDate != nil && strings.TrimSpace(*in.ExpiryDate) != "" {
		if d, err := model.ParseDate(strings.TrimSpace(*in.ExpiryDate)); err == nil {
			item.ExpiryDate = d
		}
	}
	if in.LastUsedAt != nil && strings.TrimSpace(*in.LastUsedAt) != "" {
		if t, err := model.ParseDateTime(strings.TrimSpace(*in.LastUsedAt)); err == nil {
			item.LastUsedAt = t
		}
	}

	if err := h.Store.Create(item); err != nil {
		writeError(w, http.StatusInternalServerError, "Internal server error")
		return
	}
	writeJSON(w, http.StatusCreated, map[string]interface{}{
		"success": true,
		"data":    item,
		"message": "Item created successfully",
	})
}

type updateItemInput struct {
	Name        *string `json:"name"`
	Category    *string `json:"category"`
	Quantity    *int    `json:"quantity"`
	Unit        *string `json:"unit"`
	MinQuantity *int    `json:"minQuantity"`
	UsageCount  *int    `json:"usageCount"`
	ExpiryDate  *string `json:"expiryDate"`
	LastUsedAt  *string `json:"lastUsedAt"`
	Description *string `json:"description"`
}

func (h *Handler) UpdateItem(w http.ResponseWriter, r *http.Request) {
	id, err := strconv.Atoi(r.PathValue("id"))
	if err != nil {
		writeError(w, http.StatusBadRequest, "Invalid item id")
		return
	}
	item, err := h.Store.Get(id)
	if err == store.ErrNotFound {
		writeError(w, http.StatusNotFound, "Item not found")
		return
	}
	if err != nil {
		writeError(w, http.StatusInternalServerError, "Internal server error")
		return
	}

	var in updateItemInput
	if err := decodeJSON(r, &in); err != nil {
		writeError(w, http.StatusBadRequest, "Invalid JSON body")
		return
	}

	if in.Name != nil {
		item.Name = strings.TrimSpace(*in.Name)
	}
	if in.Category != nil {
		item.Category = strings.TrimSpace(*in.Category)
	}
	if in.Quantity != nil {
		item.Quantity = *in.Quantity
	}
	if in.Unit != nil {
		item.Unit = strings.TrimSpace(*in.Unit)
	}
	if in.MinQuantity != nil {
		item.MinQuantity = *in.MinQuantity
	}
	if in.UsageCount != nil {
		item.UsageCount = *in.UsageCount
	}
	if in.Description != nil {
		item.Description = model.NullString{Valid: true, String: *in.Description}
	}
	if in.ExpiryDate != nil {
		if strings.TrimSpace(*in.ExpiryDate) == "" {
			item.ExpiryDate = model.Date{Valid: false}
		} else if d, err := model.ParseDate(strings.TrimSpace(*in.ExpiryDate)); err == nil {
			item.ExpiryDate = d
		}
	}
	if in.LastUsedAt != nil {
		if strings.TrimSpace(*in.LastUsedAt) == "" {
			item.LastUsedAt = model.DateTime{Valid: false}
		} else if t, err := model.ParseDateTime(strings.TrimSpace(*in.LastUsedAt)); err == nil {
			item.LastUsedAt = t
		}
	}

	if err := h.Store.Update(item); err != nil {
		writeError(w, http.StatusInternalServerError, "Internal server error")
		return
	}
	writeJSON(w, http.StatusOK, map[string]interface{}{
		"success": true,
		"data":    item,
		"message": "Item updated successfully",
	})
}

func (h *Handler) DeleteItem(w http.ResponseWriter, r *http.Request) {
	id, err := strconv.Atoi(r.PathValue("id"))
	if err != nil {
		writeError(w, http.StatusBadRequest, "Invalid item id")
		return
	}
	if err := h.Store.Delete(id); err == store.ErrNotFound {
		writeError(w, http.StatusNotFound, "Item not found")
		return
	} else if err != nil {
		writeError(w, http.StatusInternalServerError, "Internal server error")
		return
	}
	writeJSON(w, http.StatusOK, map[string]interface{}{"success": true, "message": "Item deleted successfully"})
}

func (h *Handler) Stats(w http.ResponseWriter, r *http.Request) {
	st, err := h.Store.Stats()
	if err != nil {
		writeError(w, http.StatusInternalServerError, "Internal server error")
		return
	}
	writeJSON(w, http.StatusOK, map[string]interface{}{"success": true, "data": st})
}

func (h *Handler) Categories(w http.ResponseWriter, r *http.Request) {
	cats, err := h.Store.Categories()
	if err != nil {
		writeError(w, http.StatusInternalServerError, "Internal server error")
		return
	}
	writeJSON(w, http.StatusOK, map[string]interface{}{"success": true, "data": cats})
}

func (h *Handler) UseItem(w http.ResponseWriter, r *http.Request) {
	id, err := strconv.Atoi(r.PathValue("id"))
	if err != nil {
		writeError(w, http.StatusBadRequest, "Invalid item id")
		return
	}
	item, err := h.Store.Use(id)
	if err == store.ErrInsufficientStock {
		orig, _ := h.Store.Get(id)
		name := ""
		if orig != nil {
			name = orig.Name
		}
		writeJSON(w, http.StatusBadRequest, map[string]interface{}{
			"success":    false,
			"error_code": "insufficientStock",
			"error_data": map[string]interface{}{"itemName": name},
		})
		return
	}
	if err == store.ErrNotFound {
		writeError(w, http.StatusNotFound, "Item not found")
		return
	}
	if err != nil {
		writeError(w, http.StatusInternalServerError, "Internal server error")
		return
	}
	writeJSON(w, http.StatusOK, map[string]interface{}{
		"success":      true,
		"message_code": "itemUsedSuccessfully",
		"message_data": map[string]interface{}{
			"itemName": item.Name,
			"quantity": item.Quantity,
			"unit":     item.Unit,
		},
		"data": item,
	})
}

func (h *Handler) PurchaseList(w http.ResponseWriter, r *http.Request) {
	list, err := h.Store.GeneratePurchaseList()
	if err != nil {
		writeError(w, http.StatusInternalServerError, "Internal server error")
		return
	}
	writeJSON(w, http.StatusOK, map[string]interface{}{"success": true, "data": list})
}

type batchInput struct {
	Operation   string `json:"operation"`
	ItemIDs     []int  `json:"itemIds"`
	NewCategory string `json:"newCategory"`
}

func (h *Handler) Batch(w http.ResponseWriter, r *http.Request) {
	var in batchInput
	if err := decodeJSON(r, &in); err != nil {
		writeError(w, http.StatusBadRequest, "Invalid JSON body")
		return
	}
	switch in.Operation {
	case "delete":
		n, err := h.Store.BatchDelete(in.ItemIDs)
		if err != nil {
			writeError(w, http.StatusInternalServerError, "Internal server error")
			return
		}
		writeJSON(w, http.StatusOK, map[string]interface{}{
			"success": true,
			"message": "Successfully deleted " + strconv.Itoa(n) + " items",
		})
	case "update_category":
		if strings.TrimSpace(in.NewCategory) == "" {
			writeError(w, http.StatusBadRequest, "New category is required")
			return
		}
		n, err := h.Store.BatchUpdateCategory(in.ItemIDs, strings.TrimSpace(in.NewCategory))
		if err != nil {
			writeError(w, http.StatusInternalServerError, "Internal server error")
			return
		}
		writeJSON(w, http.StatusOK, map[string]interface{}{
			"success": true,
			"message": "Successfully updated category for " + strconv.Itoa(n) + " items",
		})
	default:
		writeError(w, http.StatusBadRequest, "Invalid operation")
	}
}

func (h *Handler) ProxyExpenses(w http.ResponseWriter, r *http.Request) {
	url := h.Config.ExpensesURL
	if q := r.URL.Query().Encode(); q != "" {
		url += "?" + q
	}
	resp, err := http.Get(url)
	if err != nil {
		writeError(w, http.StatusBadGateway, "Failed to reach upstream expenses API")
		return
	}
	defer resp.Body.Close()
	body, _ := io.ReadAll(resp.Body)
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(resp.StatusCode)
	_, _ = w.Write(body)
}

// ---------------------------------------------------------------------------
// Health
// ---------------------------------------------------------------------------

func (h *Handler) Health(w http.ResponseWriter, r *http.Request) {
	dbStatus := "healthy"
	if err := h.Store.Ping(); err != nil {
		dbStatus = "error"
	}
	var m runtime.MemStats
	runtime.ReadMemStats(&m)
	writeJSON(w, http.StatusOK, map[string]interface{}{
		"status":   "healthy",
		"database": dbStatus,
		"memory_usage": map[string]interface{}{
			"alloc_bytes": m.Alloc,
			"alloc_mb":    m.Alloc / 1024 / 1024,
			"sys_bytes":   m.Sys,
			"sys_mb":      m.Sys / 1024 / 1024,
			"goroutines":  runtime.NumGoroutine(),
		},
		"service": "smart-inventory-manager",
		"version": "2026.02.03",
	})
}

// ---------------------------------------------------------------------------
// Static SPA serving
// ---------------------------------------------------------------------------

// SPAHandler serves the built front-end, falling back to index.html for any
// non-API route (supporting client-side routing).
func (h *Handler) SPAHandler(w http.ResponseWriter, r *http.Request) {
	if strings.HasPrefix(r.URL.Path, "/api/") {
		http.NotFound(w, r)
		return
	}
	clean := filepath.Clean(r.URL.Path)
	target := filepath.Join(h.Config.StaticDir, clean)
	// Guard against path traversal outside the static directory.
	if !strings.HasPrefix(target, filepath.Clean(h.Config.StaticDir)) {
		http.NotFound(w, r)
		return
	}
	if info, err := os.Stat(target); err == nil && !info.IsDir() {
		http.ServeFile(w, r, target)
		return
	}
	// Fallback to index.html
	indexPath := filepath.Join(h.Config.StaticDir, "index.html")
	if _, err := os.Stat(indexPath); err == nil {
		http.ServeFile(w, r, indexPath)
		return
	}
	http.NotFound(w, r)
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

func derefInt(p *int, def int) int {
	if p == nil {
		return def
	}
	return *p
}

func descFromPtr(p *string) model.NullString {
	if p == nil {
		return model.NullString{Valid: true, String: ""}
	}
	return model.NullString{Valid: true, String: *p}
}
