# 📋 Changelog

All notable changes to **Markdown File Viewer** are documented here.  
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [1.2.0] - 2026-08-13

### ✨ Added
- 🔎 **In-document search bar** — floating search box at top-right of document view
- ⬆⬇ **Navigate matches** — Previous (▲) and Next (▼) buttons to cycle through results
- ✕ **Close search bar** button — hides search panel when not needed
- 🔍 **Re-open search icon button** — small floating icon appears when search is closed; click to reopen
- ⌨️ **Ctrl+F shortcut** — press Ctrl+F inside any document to instantly open the search bar
- ✕ **Clear button inside sidebar search** — appears when typing; clears the search field instantly

### 🐛 Fixed
- 🖼️ **Emoji rendering** — Added `Segoe UI Emoji`, `Apple Color Emoji`, `Noto Color Emoji` fonts so emojis display correctly instead of showing as `?` boxes in WebView

---

## [1.1.0] - 2026-08-13

### ✨ Added
- 🔍 **Sidebar file search** — filter open files by typing filename in the sidebar search box
- 📋 **FilteredList** — sidebar list view now uses JavaFX `FilteredList` for real-time filtering

---

## [1.0.0] - 2026-08-13

### 🎉 Initial Release

- 📂 **Open multiple `.md` files** at once via the sidebar
- 📄 **CommonMark rendering** — Markdown parsed and rendered as styled HTML in JavaFX WebView
- 🌙 **Dark sidebar UI** — Catppuccin-inspired dark theme (`#1e1e2e`)
- 🎨 **GitHub-styled document view** — clean, readable rendering matching GitHub's Markdown style
- 💾 **Session restore** — remembers open files and last selected file across restarts
- ✕ **Close individual files** from sidebar with a close button per file item
- 🏗️ **NetBeans / Ant build** — project structure compatible with NetBeans IDE
