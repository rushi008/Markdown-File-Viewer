# 📄 Markdown File Viewer

![Version](https://img.shields.io/badge/version-1.3.1-blue)
![Java](https://img.shields.io/badge/Java-26-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-26-green)
![License](https://img.shields.io/badge/license-MIT-purple)

> 📋 See [CHANGELOG.md](CHANGELOG.md) for full history of changes.

A sleek, fast, and beautiful **JavaFX desktop application** for reading and navigating Markdown (`.md`) files — styled with a modern dark UI inspired by GitHub's design.


---

## ✨ Features

- 📂 **Open multiple `.md` files** at once via sidebar
- 🔍 **Search open files** — filter by filename in the sidebar
- 🔎 **In-document search** — search text inside the rendered document (Ctrl+F)
- 💾 **Session restore** — reopens your last open files automatically on launch
- 🎨 **GitHub-styled rendering** — clean, beautiful Markdown output
- 🌙 **Dark sidebar UI** — modern Catppuccin-inspired theme
- ✕ **Close individual files** from the sidebar
- 🖼️ **Emoji & image rendering** — locally bundled Twemoji SVGs and embedded images display correctly in WebView
- 📸 **Local image support** — relative paths like `image.png` in markdown resolve and render inline

---

## 🖥️ Screenshots

![README rendered in the app](image2.png)

---

## 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| Java 26 | Core language |
| JavaFX 26 | UI framework |
| CommonMark | Markdown parsing |
| Twemoji (local) | Emoji SVG assets bundled with the app |
| NetBeans | IDE / Build tool |

---

## 🚀 Getting Started

### Prerequisites

- Java JDK 26 (JDK 24+ removed `jdk.jsobject`, which JavaFX WebView needs)
- JavaFX SDK 26+ ([download here](https://openjfx.io/)) — includes `jdk.jsobject.jar`
- NetBeans IDE (recommended) or any Java IDE

### Run the Project

1. **Clone the repo:**
   ```bash
   git clone https://github.com/rushi008/Markdown-File-Viewer.git
   cd Markdown-File-Viewer
   ```

2. **Open in NetBeans:**
   - File → Open Project → Select the `MarkdownApp` folder

3. **Set up JavaFX SDK 26.0.2:**
   - Download the Windows SDK from [openjfx.io](https://openjfx.io/)
   - Unzip it as `javafx-sdk-26.0.2/` in the project root (must include `lib/jdk.jsobject.jar`)
   - In NetBeans: Project Properties → Libraries → confirm the `javafx-sdk-26.0.2/lib` JARs are listed

4. **Run:**
   - Press `F6` or click the Run button

> **Emoji assets:** Twemoji SVGs are bundled under `src/markdownviewer/emojis/`. To refresh them, run `download_emojis.ps1` from the project root.

---

## ⌨️ Keyboard Shortcuts

| Shortcut | Action |
|---|---|
| `Ctrl + F` | Open in-document search bar |
| `Enter` | Find next match |
| `▲ / ▼` buttons | Navigate previous / next match |
| `✕` button | Close search bar |

---

## 📁 Project Structure

```
MarkdownApp/
├── src/
│   └── markdownviewer/
│       ├── MarkdownViewer.java   # Main application
│       └── emojis/               # Bundled Twemoji SVG assets
├── lib/                          # CommonMark JAR dependencies
├── javafx-sdk-26.0.2/            # JavaFX SDK (not committed; download locally)
├── download_emojis.ps1           # Script to download Twemoji SVGs
├── image.png                     # Screenshot assets for README
├── image2.png
├── nbproject/                    # NetBeans project config
├── build.xml                     # Ant build file
└── README.md
```

---

## 🔄 How to Update GitHub

After making changes to your code:

```bash
# 1. Stage all changes
git add .

# 2. Commit with a message
git commit -m "describe your change here"

# 3. Push to GitHub
git push
```

---

## 📦 Dependencies

- [commonmark-java](https://github.com/commonmark/commonmark-java) — Markdown parser
- [commonmark-ext-gfm-tables](https://github.com/commonmark/commonmark-java) — GitHub Flavored Markdown tables
- [Twemoji](https://github.com/twitter/twemoji) — Emoji SVG assets (v14.0.2, bundled locally)

---

## 🤝 Contributing

Pull requests are welcome! For major changes, please open an issue first.

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

---

## 🕓 Latest Changes

### v1.3.1 — 2026-08-13
- 🛠️ Switched to Java 26 + JavaFX 26.0.2 so WebView runs on JDK 26 (`jdk.jsobject` is now bundled with JavaFX)

### v1.3.0 — 2026-08-13
- 🖼️ Fixed emoji rendering — Twemoji SVGs bundled locally and embedded as data URIs in WebView
- 📸 Fixed local screenshots/images — relative paths like `image.png` now render correctly
- ✕ Fixed close icon (U+2715) showing as a broken image in rendered markdown
- 📷 Added README screenshots section with two app screenshots

### v1.2.0 — 2026-08-13
- 🔎 In-document search bar with ▲ ▼ navigation and Ctrl+F shortcut
- ✕ Close & 🔍 reopen search icon button
- ✕ Clear button inside sidebar file search

### v1.1.0 — 2026-08-13
- 🔍 Sidebar search to filter open files by name

### v1.0.0 — 2026-08-13
- 🎉 Initial release with multi-file sidebar, dark theme, session restore & GitHub-styled rendering

> 📋 Full details in [CHANGELOG.md](CHANGELOG.md)
