# 📋 Changelog

All notable changes to **Markdown File Viewer** are documented here.  
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [1.5.4] - 2026-08-15

### 🐛 Fixed
- 💥 **Print preview crash** (`RTTexture.contentsUseful()` NPE) — WebView is no longer stretched to the full document height (that overflowed the GPU texture). Pages now scroll inside a single-page WebView

---

## [1.5.3] - 2026-08-15

### 🐛 Fixed
- 🖱️ **Options could not be changed or scrolled** — the tall print preview no longer breaks the dialog layout; the left panel now scrolls and controls respond
- 🔤 **PDF text was too large** — print/preview now use a document font size (default 11 pt)

### ✨ Added
- 🔠 **Font size** — 9–18 pt in Print options; changing it updates the preview and the PDF

---

## [1.5.2] - 2026-08-15

### 🐛 Fixed
- 📄 **PDF was always 1 page** — JavaFX `WebEngine.print()` scaled the whole document onto a single sheet. Print now writes one PDF page per paper page
- 🔢 **Preview showed "Page 1 of 1"** — page count is now `content height ÷ paper height`, so long markdown files show Page 1 of N and the arrows work

---

## [1.5.1] - 2026-08-15

### 🐛 Fixed
- 🖨️ **Print/Close buttons cut off** — options now scroll, the button bar stays pinned at the bottom, and the window fits the screen
- 📄 **Preview clipped on the right** — the page is centered and clipped inside the preview pane

### ✨ Added
- 📑 **Pages** — All pages, Current page (single), or a custom range (e.g. `1-3`)
- 📰 **Pages per sheet** — 1 (single page), 2, 4, 6, or 9
- 📚 **Collate** and **print quality** (High / Normal / Draft)

---

## [1.5.0] - 2026-08-15

### ✨ Added
- 🖨️ **Print preview** — Print / Ctrl+P opens a split window: print options on the left (printer, copies, paper, orientation, color, margins, sides) and a live page preview on the right
- ◀▶ **Page navigation** — step through preview pages, or use the mouse wheel

### 🔄 Changed
- Printing no longer jumps straight to the system dialog; you confirm from the preview window

---

## [1.4.0] - 2026-08-15

### ✨ Added
- 🖨️ **Print** — a Print button next to the in-document search bar opens the system print dialog and prints the rendered Markdown (same GitHub-styled layout shown in the viewer)
- ⌨️ **Ctrl+P shortcut** — print the currently open document from the keyboard
- 📄 **Print CSS** — page-break rules so headings, tables, and images print more cleanly

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
