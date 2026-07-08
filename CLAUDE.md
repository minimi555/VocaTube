# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

VocaTube is a Python application for downloading YouTube videos using an agent-based workflow system. The project uses LangGraph-style state management to coordinate video download operations. The codebase is written with Chinese comments and docstrings.

## Architecture

**Main Components:**
- `main.py` — Workflow orchestration layer. Defines `AgentState` (TypedDict) to manage state flow and the `VideoDownloader` function that acts as the main workflow node. It retrieves the download list from state, calls the downloader, and handles exceptions.
- `YouTube_Installer.py` — Download execution layer. Implements `Download_Video()` using `yt_dlp` library to fetch and merge video/audio streams into MP4 format. Handles per-URL error tracking and batch reporting.
- `video/` — Output directory for downloaded MP4 files (created automatically if missing)

**State Flow:**
The workflow uses a TypedDict-based state pattern where:
- `messages` — conversation/workflow history (List[str])
- `downlaod_list` — list of YouTube video URLs to download (List[str]) — **Note: typo in key name, should be "download_list"**

The `VideoDownloader` function reads state, processes downloads, and returns updated state unchanged (side-effect based).

**Download Implementation:**
`Download_Video()` uses `yt_dlp.YoutubeDL()` with these behaviors:
- Format: `'bestvideo*+bestaudio/best'` (best quality + audio, with fallback to premerged stream)
- Merge format: MP4
- Files saved to `OUTPUT_DIR` with template: `%(title)s.%(ext)s`
- Per-URL error handling: continues on failure, collects failed URLs
- Raises exception at end if any URLs failed (contains list of failures)

## Development & Testing

**Run the downloader:**
```bash
python YouTube_Installer.py          # Tests with hardcoded URL (requires active network)
python main.py                       # Runs workflow orchestration
```

**Requirements:**
- Python 3.12+
- Virtual environment: `.venv/` (already initialized)
- `yt_dlp` (installed in .venv, 2026.7.4)
- `ffmpeg` (system dependency) — required for merging video+audio. Install via package manager:
  ```bash
  # macOS
  brew install ffmpeg
  # Ubuntu/Debian
  sudo apt-get install ffmpeg
  # Windows (with chocolatey)
  choco install ffmpeg
  ```

## Known Issues

1. **Typo: "downlaod_list"** — The state key is misspelled throughout (`main.py:6`, `main.py:13`). Should be `"download_list"`. This is a non-breaking typo (works but confusing); fix when refactoring state.
2. **No entry point** — `main.py` defines the workflow but has no execution block (`if __name__ == '__main__'`). Currently just defines the function. To use, either add a runner in `main.py` or call `VideoDownloader()` from elsewhere with a populated `AgentState`.
3. **Error handling is strict** — `Download_Video()` raises an exception if *any* URL fails. This stops workflow progress. Consider whether partial success should be an error or logged warning.

## Notes for Future Work

- The codebase uses Chinese documentation; maintain this for consistency if extending
- `yt_dlp` options can be expanded (e.g., subtitle download, format preferences, authentication)
- State management is minimal; consider integrating with LangGraph if the workflow grows
- No logging framework currently; using print() for output
