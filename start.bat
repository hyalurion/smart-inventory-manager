@echo off
:: Smart Inventory Manager - Go backend launcher
:: Requires Go 1.26.5+ installed and available on PATH.
cd /d "%~dp0backend"

echo Building Go backend (this may take a moment on first run)...
go build -o bin/server.exe .
if %ERRORLEVEL% neq 0 (
    echo.
    echo Build failed. Please make sure Go 1.26.5 or newer is installed
    echo and added to your PATH, then try again.
    pause
    exit /b 1
)

echo Starting Smart Inventory Manager backend on http://0.0.0.0:5000 ...
echo Open http://localhost:5000 in your browser.
bin\server.exe
pause
