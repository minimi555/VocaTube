# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

VocaTube is a Python application for downloading YouTube videos using an agent-based workflow system. The project uses LangGraph-style state management to coordinate video download operations.

## Architecture

**Main Components:**
- `main.py` — Defines the workflow orchestration using `AgentState` (a TypedDict) and the `VideoDownloader` function that processes download requests
- `YouTube_Installer.py` — Contains `Download_Video()` function (currently a stub) that handles the actual YouTube video downloading logic
- `video/` — Output directory for downloaded videos

**State Management:**
The application uses a TypedDict-based state pattern (`AgentState`) with:
- `messages` — conversation/workflow history
- `downlaod_list` — list of YouTube video URLs to download (note: has a typo in the codebase, currently "downlaod_list")

The `VideoDownloader` function takes an `AgentState`, processes the download list, and returns the updated state.

## Current Issues

1. **Syntax error in main.py:14** — `Download_Video(downlaod_list : List)` has incorrect syntax for passing arguments. Should be `Download_Video(downlaod_list)` (remove type annotation from function call).
2. **Typo** — "downlaod_list" should be "download_list" throughout for clarity.
3. **YouTube_Installer.py incomplete** — `Download_Video()` function has no implementation; needs logic to handle the actual download operation.

## Development Setup

```bash
# Run the main workflow
python main.py

# (Add actual test/lint commands once the project structure matures)
```

## Dependencies

The project currently imports:
- `typing` (standard library) — for type hints
- YouTube downloading library (to be added to `YouTube_Installer.py`) — likely `yt-dlp` or `pytube`

Ensure the YouTube download library is installed before implementing the `Download_Video()` function.
