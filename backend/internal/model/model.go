// Package model defines the domain types for the inventory service.
//
// The custom Date / DateTime / NullString types implement both database/sql
// Scanner/Valuer and encoding/json Marshal/Unmarshal so that the JSON wire
// format stays identical to the original Flask backend:
//   - expiryDate  -> "YYYY-MM-DD" or null
//   - createdAt / updatedAt / lastUsedAt -> "YYYY-MM-DDTHH:MM:SS" or null
//   - description -> string or null
package model

import (
	"database/sql/driver"
	"encoding/json"
	"errors"
	"strings"
	"time"
)

// dateLayouts and dateTimeLayouts are tried (in order) whenever a value is
// read back from SQLite. This keeps us compatible with databases written by
// the original Python/SQLAlchemy backend as well as databases we write ourselves.
var (
	dateLayouts = []string{
		"2006-01-02",
		"2006-01-02 15:04:05.999999999",
		"2006-01-02 15:04:05",
		time.RFC3339,
		"2006-01-02T15:04:05.999999999",
		"2006-01-02T15:04:05",
	}
	dateTimeLayouts = []string{
		"2006-01-02 15:04:05.999999999",
		"2006-01-02 15:04:05",
		time.RFC3339,
		"2006-01-02T15:04:05.999999999",
		"2006-01-02T15:04:05",
		"2006-01-02",
	}
)

func asString(v interface{}) (string, bool) {
	switch t := v.(type) {
	case string:
		return t, true
	case []byte:
		return string(t), true
	case time.Time:
		return t.Format("2006-01-02 15:04:05"), true
	case nil:
		return "", false
	default:
		return "", false
	}
}

func tryParse(s string, layouts []string) (time.Time, error) {
	for _, l := range layouts {
		if t, err := time.Parse(l, s); err == nil {
			return t, nil
		}
	}
	return time.Time{}, errors.New("model: cannot parse time value: " + s)
}

// ---------------------------------------------------------------------------
// Date: a nullable calendar date (YYYY-MM-DD)
// ---------------------------------------------------------------------------

type Date struct {
	Valid bool
	Time  time.Time
}

func (d *Date) Scan(value interface{}) error {
	if value == nil {
		d.Valid = false
		return nil
	}
	s, ok := asString(value)
	if !ok {
		d.Valid = false
		return nil
	}
	s = strings.TrimSpace(s)
	if s == "" {
		d.Valid = false
		return nil
	}
	t, err := tryParse(s, dateLayouts)
	if err != nil {
		// Fall back to just the leading YYYY-MM-DD portion.
		if len(s) >= 10 {
			if t2, err2 := time.Parse("2006-01-02", s[:10]); err2 == nil {
				d.Time, d.Valid = t2, true
				return nil
			}
		}
		return err
	}
	d.Time, d.Valid = t, true
	return nil
}

func (d Date) Value() (driver.Value, error) {
	if !d.Valid {
		return nil, nil
	}
	return d.Time.Format("2006-01-02"), nil
}

func (d Date) MarshalJSON() ([]byte, error) {
	if !d.Valid {
		return []byte("null"), nil
	}
	return json.Marshal(d.Time.Format("2006-01-02"))
}

func (d *Date) UnmarshalJSON(b []byte) error {
	var s *string
	if err := json.Unmarshal(b, &s); err != nil {
		return err
	}
	if s == nil || *s == "" {
		d.Valid = false
		return nil
	}
	t, err := time.Parse("2006-01-02", *s)
	if err != nil {
		return err
	}
	d.Time, d.Valid = t, true
	return nil
}

// ParseDate parses a "YYYY-MM-DD" string into a valid Date.
func ParseDate(s string) (Date, error) {
	t, err := time.Parse("2006-01-02", strings.TrimSpace(s))
	if err != nil {
		return Date{}, err
	}
	return Date{Valid: true, Time: t}, nil
}

// ---------------------------------------------------------------------------
// DateTime: a nullable timestamp (stored as "YYYY-MM-DD HH:MM:SS", UTC)
// ---------------------------------------------------------------------------

type DateTime struct {
	Valid bool
	Time  time.Time
}

func (d *DateTime) Scan(value interface{}) error {
	if value == nil {
		d.Valid = false
		return nil
	}
	s, ok := asString(value)
	if !ok {
		d.Valid = false
		return nil
	}
	s = strings.TrimSpace(s)
	if s == "" {
		d.Valid = false
		return nil
	}
	t, err := tryParse(s, dateTimeLayouts)
	if err != nil {
		return err
	}
	d.Time, d.Valid = t, true
	return nil
}

func (d DateTime) Value() (driver.Value, error) {
	if !d.Valid {
		return nil, nil
	}
	return d.Time.UTC().Format("2006-01-02 15:04:05"), nil
}

func (d DateTime) MarshalJSON() ([]byte, error) {
	if !d.Valid {
		return []byte("null"), nil
	}
	return json.Marshal(d.Time.UTC().Format("2006-01-02T15:04:05"))
}

func (d *DateTime) UnmarshalJSON(b []byte) error {
	var s *string
	if err := json.Unmarshal(b, &s); err != nil {
		return err
	}
	if s == nil || *s == "" {
		d.Valid = false
		return nil
	}
	t, err := tryParse(*s, dateTimeLayouts)
	if err != nil {
		return err
	}
	d.Time, d.Valid = t, true
	return nil
}

// ParseDateTime parses a timestamp string into a valid DateTime.
func ParseDateTime(s string) (DateTime, error) {
	t, err := tryParse(strings.TrimSpace(s), dateTimeLayouts)
	if err != nil {
		return DateTime{}, err
	}
	return DateTime{Valid: true, Time: t}, nil
}

// Now returns the current time wrapped as a valid DateTime (UTC).
func Now() DateTime {
	return DateTime{Valid: true, Time: time.Now().UTC()}
}

// ---------------------------------------------------------------------------
// NullString: a nullable string (matches SQL TEXT NULL semantics)
// ---------------------------------------------------------------------------

type NullString struct {
	Valid  bool
	String string
}

func (n *NullString) Scan(value interface{}) error {
	if value == nil {
		n.Valid = false
		return nil
	}
	s, ok := asString(value)
	if !ok {
		n.Valid = false
		return nil
	}
	n.String, n.Valid = s, true
	return nil
}

func (n NullString) Value() (driver.Value, error) {
	if !n.Valid {
		return nil, nil
	}
	return n.String, nil
}

func (n NullString) MarshalJSON() ([]byte, error) {
	if !n.Valid {
		return []byte("null"), nil
	}
	return json.Marshal(n.String)
}

func (n *NullString) UnmarshalJSON(b []byte) error {
	var s *string
	if err := json.Unmarshal(b, &s); err != nil {
		return err
	}
	if s == nil {
		n.Valid = false
		n.String = ""
		return nil
	}
	n.Valid = true
	n.String = *s
	return nil
}

// ---------------------------------------------------------------------------
// InventoryItem: the core domain entity
// ---------------------------------------------------------------------------

type InventoryItem struct {
	ID          int        `json:"id"`
	Name        string     `json:"name"`
	Category    string     `json:"category"`
	Quantity    int        `json:"quantity"`
	Unit        string     `json:"unit"`
	MinQuantity int        `json:"minQuantity"`
	ExpiryDate  Date       `json:"expiryDate"`
	Description NullString `json:"description"`
	UsageCount  int        `json:"usageCount"`
	LastUsedAt  DateTime   `json:"lastUsedAt"`
	CreatedAt   DateTime   `json:"createdAt"`
	UpdatedAt   DateTime   `json:"updatedAt"`
}

const (
	CategoryFood        = "food"
	CategoryMedicine    = "medicine"
	CategoryCleaning    = "cleaning"
	CategoryPersonal    = "personal"
	CategoryHousehold   = "household"
	CategoryElectronics = "electronics"
)

// ValidCategories is the set of categories accepted by the AI record generator.
var ValidCategories = map[string]bool{
	CategoryFood:        true,
	CategoryMedicine:    true,
	CategoryCleaning:    true,
	CategoryPersonal:    true,
	CategoryHousehold:   true,
	CategoryElectronics: true,
}

// PurchaseItem is a single entry in a generated purchase list.
type PurchaseItem struct {
	ID                int      `json:"id"`
	Name              string   `json:"name"`
	Category          string   `json:"category"`
	CurrentQuantity   int      `json:"currentQuantity"`
	MinQuantity       int      `json:"minQuantity"`
	SuggestedQuantity int      `json:"suggestedQuantity"`
	Unit              string   `json:"unit"`
	LastUsedAt        DateTime `json:"lastUsedAt"`
}

// Stats aggregates dashboard counters.
type Stats struct {
	TotalItems        int `json:"totalItems"`
	LowStockItems     int `json:"lowStockItems"`
	ExpiringSoonItems int `json:"expiringSoonItems"`
	ExpiredItems      int `json:"expiredItems"`
	Categories        int `json:"categories"`
}
