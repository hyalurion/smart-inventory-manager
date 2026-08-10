# Smart Home Inventory Management System

A modern web application for home inventory management, supporting multiple languages and intelligent features.

## 🌟 Features

### Core Features
- **Inventory Management**: Add, edit, delete, and view household items.
- **Category Management**: Support categories such as food, medicine, cleaning supplies, personal care, household items, and electronics.
- **Search Function**: Quickly search for item names and descriptions.
- **Expiry Date Management**: Set and track item expiry dates.

### Intelligent Features
- **Intelligent Reminders**:
  - Reminders for expired items.
  - Warnings for items about to expire (within 7 days).
  - Reminders for low inventory.
- **Intelligent Recommendations**:
  - Purchase suggestions based on inventory status.
  - Recommendations based on usage frequency analysis.

### Multi-language Support (21 languages)
Arabic, Bengali, German, English, Spanish, French, Hindi, Indonesian, Italian, Japanese, Korean, Persian, Portuguese, Russian, Tamil, Thai, Turkish, Urdu, Vietnamese, Chinese (Simplified) and Chinese (Traditional).

### Technical Features
- **Responsive Design**: Support for desktop and mobile devices.
- **Modern Interface**: Uses Tailwind CSS and Font Awesome icons.
- **Real-time Data**: Frontend-backend separation architecture with real-time data synchronization.
- **Data Persistence**: SQLite database storage.

## 🛠️ Technology Stack

### Frontend
- **Vue.js 3**: Progressive JavaScript framework.
- **Vue I18n**: Internationalization support.
- **Tailwind CSS**: Modern CSS framework.
- **Font Awesome**: Icon library.
- **Native JavaScript**: Core logic implementation.

### Backend (Go)
- **Go 1.26.5** (requires Go 1.22+; uses the standard library `net/http` router).
- **modernc.org/sqlite**: Pure-Go SQLite driver (no CGO / no C compiler required).
- **SQLite**: Lightweight database (reuses the same `src/database/app.db` file).
- **Zero third-party web framework**: routing, middleware and static serving are all standard library.

> The backend was migrated from Python/Flask to Go. The Python implementation has been removed; the Go service is the only backend.

## 📁 Project Structure

```
smart-inventory-manager/
├── .gitignore
├── README.md
├── start.bat                # Builds and launches the Go backend (Windows)
├── backend/                 # Backend source code (Go)
│   ├── go.mod              # Go module definition (requires go 1.26.5)
│   ├── go.sum
│   ├── main.go             # Server entry point (routing, middleware, SPA serving)
│   ├── bin/                # Compiled binary (git-ignored)
│   ├── internal/
│   │   ├── model/          # Domain types + custom Date/DateTime/NullString
│   │   ├── store/          # SQLite persistence layer
│   │   ├── handlers/       # HTTP handlers (inventory, AI, health)
│   │   └── middleware/     # CORS middleware
│   └── src/
│       ├── static/         # Frontend build (served by the Go server)
│       │   ├── index.html
│       │   ├── app.js
│       │   └── locales/    # Language files
│       └── database/       # SQLite database (app.db)
└── android/                # Android client (if present)
```

## 🔧 Local Development

### Environment Requirements
- **Go 1.26.5** (or any Go 1.22+ toolchain)
- Modern browser (supporting ES6+)

### Installation & Run (Windows)

1. **Install Go**
   Download and install Go 1.26.5 from https://go.dev/dl/ and ensure `go` is on your PATH:
   ```bash
   go version   # should print go1.26.5 (or newer)
   ```

2. **Fetch dependencies**
   ```bash
   cd backend
   go mod download   # populates the module cache
   go mod verify     # confirms checksums
   ```
   If `proxy.golang.org` is unreachable from your network (common in mainland China),
   point Go at a reachable mirror once — the setting is stored globally per user:
   ```bash
   go env -w GOPROXY=https://goproxy.cn,direct
   go env -w GOSUMDB=sum.golang.google.cn
   ```
   Keeping `GOSUMDB` on the mirror preserves checksum verification; do not use `GOSUMDB=off`.

3. **Build and start with one click**
   ```bat
   start.bat
   ```
   This builds the server into `backend/bin/server.exe` and starts it.

   Alternatively, run it manually from the `backend` directory:
   ```bash
   cd backend
   go run .
   # or build first, then run:
   go build -o bin/server.exe .
   bin/server.exe
   ```

4. **Access the Application**
   - Local: open http://localhost:5000
   - LAN: use the computer's LAN IP, e.g. http://192.168.1.100:5000

### Configuration (optional, via environment variables)

| Variable           | Default                              | Description                                   |
|--------------------|--------------------------------------|-----------------------------------------------|
| `PORT`             | `5000`                               | HTTP listen port                              |
| `DB_PATH`          | `src/database/app.db`                | Path to the SQLite database file              |
| `STATIC_DIR`       | `src/static`                         | Directory that holds the SPA front-end        |
| `EXPENSES_API_URL` | `http://192.168.10.9:3010/api/expenses` | Upstream URL for the `/expenses` proxy     |

## 📊 Database Design

### inventory_items
- `id`: Primary key
- `name`: Item name
- `category`: Category
- `quantity`: Quantity
- `unit`: Unit
- `min_quantity`: Minimum stock
- `expiry_date`: Expiration date
- `description`: Description
- `usage_count`: Times the item has been used
- `last_used_at`: Last usage timestamp
- `created_at`: Creation time
- `updated_at`: Update time

### categories
- `id`: Primary key
- `name`: Category name (unique)
- `icon`: Icon
- `created_at`: Creation time

## 🌐 API Interfaces

### Inventory Management (`/api/inventory`)
- `GET    /items` — List items (supports `search`, `category`, `page`, `per_page`)
- `GET    /items/{id}` — Get a single item
- `POST   /items` — Create an item
- `PUT    /items/{id}` — Update an item
- `DELETE /items/{id}` — Delete an item
- `GET    /stats` — Dashboard statistics
- `GET    /categories` — List distinct categories
- `POST   /items/{id}/use` — Record usage (decrements quantity)
- `GET    /items/generate-purchase-list` — Low-stock purchase suggestions
- `POST   /items/batch` — Batch delete / category update (`operation`: `delete` | `update_category`)
- `GET    /expenses` — Proxy to the external expense-tracking API

### AI (`/api/ai`)
- `POST   /generate-records` — Upload one or more images (multipart `images` + form field `api_key`); the SiliconFlow vision model returns structured inventory records.

### Health
- `GET    /api/health` — Service status, database health and memory usage.

## 👥 Contribution

Welcome to submit Issues and Pull Requests to improve this project!

## 🙏 Acknowledgments

- Vue.js
- Go
- Tailwind CSS
- Font Awesome
