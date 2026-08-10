// Package store implements all persistence for the inventory service on top of
// SQLite using the pure-Go modernc.org/sqlite driver (no CGO required).
package store

import (
	"database/sql"
	"errors"
	"fmt"
	"strings"
	"time"

	"smartinventory/backend/internal/model"

	_ "modernc.org/sqlite"
)

// ErrNotFound is returned when a requested row does not exist.
var ErrNotFound = errors.New("not found")

// ErrInsufficientStock is returned when an item cannot be "used" because its
// quantity is already zero.
var ErrInsufficientStock = errors.New("insufficient stock")

const schema = `
CREATE TABLE IF NOT EXISTS inventory_items (
	id            INTEGER PRIMARY KEY AUTOINCREMENT,
	name          VARCHAR(100) NOT NULL,
	category      VARCHAR(50)  NOT NULL,
	quantity      INTEGER NOT NULL DEFAULT 0,
	unit          VARCHAR(20)  NOT NULL,
	min_quantity  INTEGER NOT NULL DEFAULT 0,
	expiry_date   DATE,
	description   TEXT,
	usage_count   INTEGER NOT NULL DEFAULT 0,
	last_used_at  TIMESTAMP,
	created_at    DATETIME NOT NULL,
	updated_at    DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS categories (
	id          INTEGER PRIMARY KEY AUTOINCREMENT,
	name        VARCHAR(50) NOT NULL UNIQUE,
	icon        VARCHAR(50),
	created_at  DATETIME NOT NULL
);
`

const selectCols = `id, name, category, quantity, unit, min_quantity, expiry_date,
	description, usage_count, last_used_at, created_at, updated_at`

// Store wraps a *sql.DB.
type Store struct {
	db *sql.DB
}

// Open opens (and initializes) the SQLite database at dbPath.
func Open(dbPath string) (*Store, error) {
	// busy_timeout avoids "database is locked" under brief concurrent writes.
	dsn := dbPath + "?_pragma=busy_timeout(5000)"
	db, err := sql.Open("sqlite", dsn)
	if err != nil {
		return nil, fmt.Errorf("open database: %w", err)
	}
	db.SetMaxOpenConns(1) // SQLite is single-writer; serialise access.
	if err := db.Ping(); err != nil {
		return nil, fmt.Errorf("ping database: %w", err)
	}
	if _, err := db.Exec(schema); err != nil {
		return nil, fmt.Errorf("init schema: %w", err)
	}
	return &Store{db: db}, nil
}

// Close closes the underlying database.
func (s *Store) Close() error { return s.db.Close() }

// Ping verifies the database connection is alive.
func (s *Store) Ping() error {
	_, err := s.db.Exec("SELECT 1")
	return err
}

func scanItem(scan func(...interface{}) error) (*model.InventoryItem, error) {
	it := &model.InventoryItem{}
	err := scan(&it.ID, &it.Name, &it.Category, &it.Quantity, &it.Unit,
		&it.MinQuantity, &it.ExpiryDate, &it.Description, &it.UsageCount,
		&it.LastUsedAt, &it.CreatedAt, &it.UpdatedAt)
	if err != nil {
		return nil, err
	}
	return it, nil
}

// List returns a page of items filtered by an optional search term and category.
func (s *Store) List(search, category string, page, perPage int) ([]model.InventoryItem, int, int, error) {
	if page < 1 {
		page = 1
	}
	if perPage < 1 {
		perPage = 50
	}

	var (
		where string
		args  []interface{}
	)
	if search != "" {
		where += " AND (name LIKE ? OR description LIKE ?)"
		like := "%" + search + "%"
		args = append(args, like, like)
	}
	if category != "" {
		where += " AND category = ?"
		args = append(args, category)
	}

	var total int
	if err := s.db.QueryRow("SELECT COUNT(*) FROM inventory_items WHERE 1=1"+where, args...).Scan(&total); err != nil {
		return nil, 0, 0, err
	}

	offset := (page - 1) * perPage
	q := "SELECT " + selectCols + " FROM inventory_items WHERE 1=1" + where +
		" ORDER BY id DESC LIMIT ? OFFSET ?"
	qArgs := append(append([]interface{}{}, args...), perPage, offset)

	rows, err := s.db.Query(q, qArgs...)
	if err != nil {
		return nil, 0, 0, err
	}
	defer rows.Close()

	items := make([]model.InventoryItem, 0)
	for rows.Next() {
		it, err := scanItem(rows.Scan)
		if err != nil {
			return nil, 0, 0, err
		}
		items = append(items, *it)
	}
	if err := rows.Err(); err != nil {
		return nil, 0, 0, err
	}

	pages := total / perPage
	if total%perPage != 0 {
		pages++
	}
	if pages == 0 {
		pages = 1
	}
	return items, total, pages, nil
}

// Get returns a single item by id.
func (s *Store) Get(id int) (*model.InventoryItem, error) {
	row := s.db.QueryRow("SELECT "+selectCols+" FROM inventory_items WHERE id = ?", id)
	it, err := scanItem(row.Scan)
	if errors.Is(err, sql.ErrNoRows) {
		return nil, ErrNotFound
	}
	if err != nil {
		return nil, err
	}
	return it, nil
}

// Create inserts a new item, populating its ID, CreatedAt and UpdatedAt.
func (s *Store) Create(it *model.InventoryItem) error {
	now := model.Now()
	it.CreatedAt, it.UpdatedAt = now, now
	res, err := s.db.Exec(
		`INSERT INTO inventory_items
			(name, category, quantity, unit, min_quantity, expiry_date, description, usage_count, last_used_at, created_at, updated_at)
		 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
		it.Name, it.Category, it.Quantity, it.Unit, it.MinQuantity,
		it.ExpiryDate, it.Description, it.UsageCount, it.LastUsedAt,
		it.CreatedAt, it.UpdatedAt,
	)
	if err != nil {
		return err
	}
	id, err := res.LastInsertId()
	if err != nil {
		return err
	}
	it.ID = int(id)
	return nil
}

// Update persists changes to an existing item, refreshing UpdatedAt.
func (s *Store) Update(it *model.InventoryItem) error {
	it.UpdatedAt = model.Now()
	_, err := s.db.Exec(
		`UPDATE inventory_items SET
			name=?, category=?, quantity=?, unit=?, min_quantity=?, expiry_date=?,
			description=?, usage_count=?, last_used_at=?, updated_at=?
		 WHERE id=?`,
		it.Name, it.Category, it.Quantity, it.Unit, it.MinQuantity,
		it.ExpiryDate, it.Description, it.UsageCount, it.LastUsedAt,
		it.UpdatedAt, it.ID,
	)
	return err
}

// Delete removes an item. It returns ErrNotFound when nothing was deleted.
func (s *Store) Delete(id int) error {
	res, err := s.db.Exec("DELETE FROM inventory_items WHERE id = ?", id)
	if err != nil {
		return err
	}
	n, _ := res.RowsAffected()
	if n == 0 {
		return ErrNotFound
	}
	return nil
}

// Stats returns dashboard counters.
func (s *Store) Stats() (model.Stats, error) {
	today := time.Now().UTC().Format("2006-01-02")
	sevenLater := time.Now().UTC().AddDate(0, 0, 7).Format("2006-01-02")

	var st model.Stats
	if err := s.db.QueryRow("SELECT COUNT(*) FROM inventory_items").Scan(&st.TotalItems); err != nil {
		return st, err
	}
	if err := s.db.QueryRow("SELECT COUNT(*) FROM inventory_items WHERE quantity <= min_quantity").Scan(&st.LowStockItems); err != nil {
		return st, err
	}
	if err := s.db.QueryRow("SELECT COUNT(*) FROM inventory_items WHERE expiry_date >= ? AND expiry_date <= ?", today, sevenLater).Scan(&st.ExpiringSoonItems); err != nil {
		return st, err
	}
	if err := s.db.QueryRow("SELECT COUNT(*) FROM inventory_items WHERE expiry_date < ?", today).Scan(&st.ExpiredItems); err != nil {
		return st, err
	}
	if err := s.db.QueryRow("SELECT COUNT(DISTINCT category) FROM inventory_items").Scan(&st.Categories); err != nil {
		return st, err
	}
	return st, nil
}

// Categories returns the distinct (non-empty) categories currently in use.
func (s *Store) Categories() ([]string, error) {
	rows, err := s.db.Query("SELECT DISTINCT category FROM inventory_items WHERE category <> '' ORDER BY category")
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := make([]string, 0)
	for rows.Next() {
		var c string
		if err := rows.Scan(&c); err != nil {
			return nil, err
		}
		out = append(out, c)
	}
	return out, rows.Err()
}

// Use decrements an item's quantity by one, increments its usage count and
// records the usage timestamp. It returns ErrInsufficientStock when quantity
// is already zero.
func (s *Store) Use(id int) (*model.InventoryItem, error) {
	tx, err := s.db.Begin()
	if err != nil {
		return nil, err
	}
	defer tx.Rollback()

	it, err := scanItem(func(args ...interface{}) error {
		return tx.QueryRow("SELECT "+selectCols+" FROM inventory_items WHERE id = ?", id).Scan(args...)
	})
	if errors.Is(err, sql.ErrNoRows) {
		return nil, ErrNotFound
	}
	if err != nil {
		return nil, err
	}
	if it.Quantity <= 0 {
		return nil, ErrInsufficientStock
	}

	it.Quantity--
	it.UsageCount++
	it.LastUsedAt = model.Now()
	it.UpdatedAt = model.Now()

	if _, err := tx.Exec(
		`UPDATE inventory_items SET quantity=?, usage_count=?, last_used_at=?, updated_at=? WHERE id=?`,
		it.Quantity, it.UsageCount, it.LastUsedAt, it.UpdatedAt, it.ID,
	); err != nil {
		return nil, err
	}
	if err := tx.Commit(); err != nil {
		return nil, err
	}
	return it, nil
}

// GeneratePurchaseList returns low-stock items with a suggested purchase quantity.
func (s *Store) GeneratePurchaseList() ([]model.PurchaseItem, error) {
	rows, err := s.db.Query(
		"SELECT id, name, category, quantity, min_quantity, unit, last_used_at " +
			"FROM inventory_items WHERE quantity <= min_quantity ORDER BY category",
	)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	list := make([]model.PurchaseItem, 0)
	for rows.Next() {
		var p model.PurchaseItem
		if err := rows.Scan(&p.ID, &p.Name, &p.Category, &p.CurrentQuantity,
			&p.MinQuantity, &p.Unit, &p.LastUsedAt); err != nil {
			return nil, err
		}
		suggested := int(float64(p.MinQuantity)*1.5) - p.CurrentQuantity
		if suggested < 1 {
			suggested = 1
		}
		p.SuggestedQuantity = suggested
		list = append(list, p)
	}
	return list, rows.Err()
}

// BatchDelete removes all items whose id is in ids.
func (s *Store) BatchDelete(ids []int) (int, error) {
	if len(ids) == 0 {
		return 0, nil
	}
	placeholders := strings.Repeat("?,", len(ids))
	placeholders = placeholders[:len(placeholders)-1]
	args := make([]interface{}, len(ids))
	for i, id := range ids {
		args[i] = id
	}
	res, err := s.db.Exec("DELETE FROM inventory_items WHERE id IN ("+placeholders+")", args...)
	if err != nil {
		return 0, err
	}
	n, _ := res.RowsAffected()
	return int(n), nil
}

// BatchUpdateCategory reassigns the category for all items whose id is in ids.
func (s *Store) BatchUpdateCategory(ids []int, newCategory string) (int, error) {
	if len(ids) == 0 {
		return 0, nil
	}
	placeholders := strings.Repeat("?,", len(ids))
	placeholders = placeholders[:len(placeholders)-1]
	args := make([]interface{}, 0, len(ids)+2)
	args = append(args, newCategory, model.Now())
	for _, id := range ids {
		args = append(args, id)
	}
	res, err := s.db.Exec(
		"UPDATE inventory_items SET category = ?, updated_at = ? WHERE id IN ("+placeholders+")",
		args...,
	)
	if err != nil {
		return 0, err
	}
	n, _ := res.RowsAffected()
	return int(n), nil
}
