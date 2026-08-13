# 📋 Changelog

All notable changes to **Markdown File Viewer** are documented here.  
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [1.3.1] - 2026-08-13

### 🐛 Fixed
- 🛠️ **App failed to start on JDK 26** — `javafx.web` required the `jdk.jsobject` module, which was removed from the JDK. The project now uses JavaFX 26.0.2, which ships `jdk.jsobject.jar`.

### 🔄 Changed
- Runtime stack is now **Java 26** and **JavaFX 26.0.2** (was JavaFX 21.0.10)
- CI build downloads JavaFX 26.0.2 and uses JDK 26

---

## [1.3.0] - 2026-08-13

### ✨ Added
- 📷 **README screenshots** — two app screenshots (`image.png`, `image2.png`) in the Screenshots section
- 📦 **`download_emojis.ps1`** — PowerShell script to download Twemoji v14.0.2 SVGs into `src/markdownviewer/emojis/`
- 🖼️ **Local Twemoji bundle** — ~3,700 emoji SVGs shipped with the app (no CDN dependency)

### 🐛 Fixed
- 📸 **Local images not showing** — relative image paths (e.g. `image.png`) are now embedded as base64 data URIs so they display in JavaFX WebView loaded via `loadContent()`
- 🖼️ **Emoji icons showing as empty boxes** — emojis are embedded as base64 data URIs from bundled Twemoji SVGs instead of blocked `file://` or CDN URLs
- ✕ **Close icon (U+2715) broken** — excluded from emoji conversion since Twemoji has no `2715.svg`; renders as plain text instead of a missing image

### 🔄 Changed
- Emoji rendering switched from CDN / font-based approach to locally bundled Twemoji SVG data URIs

---

## [1.2.0] - 2026-08-13

### ✨ Added
- 🔎 **In-document search bar** — floating search box at top-right of document view
- ⬆⬇ **Navigate matches** — Previous (▲) and Next (▼) buttons to cycle through results
- ✕ **Close search bar** button — hides search panel when not needed
- 🔍 **Re-open search icon button** — small floating icon appears when search is closed; click to reopen
- ⌨️ **Ctrl+F shortcut** — press Ctrl+F inside any document to instantly open the search bar
- ✕ **Clear button inside sidebar search** — appears when typing; clears the search field instantly

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
