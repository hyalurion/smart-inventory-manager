// Command server is the Go backend for the Smart Home Inventory Manager.
//
// It is a drop-in replacement for the original Python/Flask backend: same
// routes, same JSON contract, same SQLite database (src/database/app.db), and
// it also serves the static SPA from src/static.
package main

import (
	"log"
	"net/http"
	"os"
	"path/filepath"
	"runtime"
	"time"

	"smartinventory/backend/internal/handlers"
	"smartinventory/backend/internal/middleware"
	"smartinventory/backend/internal/store"
)

func main() {
	dbPath := envOr("DB_PATH", "src/database/app.db")
	staticDir := envOr("STATIC_DIR", "src/static")
	expensesURL := envOr("EXPENSES_API_URL", "http://192.168.10.9:3010/api/expenses")
	port := envOr("PORT", "5000")

	// Make sure the database directory exists.
	if dir := filepath.Dir(dbPath); dir != "" && dir != "." {
		_ = os.MkdirAll(dir, 0o755)
	}

	st, err := store.Open(dbPath)
	if err != nil {
		log.Fatalf("failed to open database: %v", err)
	}
	defer st.Close()

	h := &handlers.Handler{
		Store: st,
		Config: handlers.Config{
			ExpensesURL: expensesURL,
			StaticDir:   staticDir,
		},
	}

	mux := http.NewServeMux()

	// Health
	mux.HandleFunc("GET /api/health", h.Health)

	// Inventory API
	mux.HandleFunc("GET /api/inventory/items", h.ListItems)
	mux.HandleFunc("POST /api/inventory/items", h.CreateItem)
	mux.HandleFunc("GET /api/inventory/items/generate-purchase-list", h.PurchaseList)
	mux.HandleFunc("POST /api/inventory/items/batch", h.Batch)
	mux.HandleFunc("POST /api/inventory/items/{id}/use", h.UseItem)
	mux.HandleFunc("GET /api/inventory/items/{id}", h.GetItem)
	mux.HandleFunc("PUT /api/inventory/items/{id}", h.UpdateItem)
	mux.HandleFunc("DELETE /api/inventory/items/{id}", h.DeleteItem)
	mux.HandleFunc("GET /api/inventory/stats", h.Stats)
	mux.HandleFunc("GET /api/inventory/categories", h.Categories)
	mux.HandleFunc("GET /api/inventory/expenses", h.ProxyExpenses)

	// AI API
	mux.HandleFunc("POST /api/ai/generate-records", h.GenerateRecords)

	// Static SPA (catch-all, excluding /api/* which 404 inside the handler).
	// The multi-segment wildcard also matches the root path "/".
	mux.HandleFunc("GET /{path...}", h.SPAHandler)

	handler := middleware.CORS(loggingMiddleware(mux))

	addr := ":" + port
	srv := &http.Server{
		Addr:              addr,
		Handler:           handler,
		ReadHeaderTimeout: 15 * time.Second,
	}

	log.Printf("smart-inventory-manager backend (Go %s) listening on http://0.0.0.0%s", runtime.Version(), addr)
	if err := srv.ListenAndServe(); err != nil {
		log.Fatalf("server error: %v", err)
	}
}

func loggingMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		sw := &statusWriter{ResponseWriter: w, status: http.StatusOK}
		next.ServeHTTP(sw, r)
		log.Printf("%s %s %d %s", r.Method, r.URL.Path, sw.status, time.Since(start))
	})
}

type statusWriter struct {
	http.ResponseWriter
	status int
}

func (s *statusWriter) WriteHeader(code int) {
	s.status = code
	s.ResponseWriter.WriteHeader(code)
}

func envOr(key, def string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return def
}
