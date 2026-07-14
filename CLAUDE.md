# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

VocaTube downloads YouTube videos and subtitles, then builds a vocabulary/learning database from the subtitle content. It's a Python backend (no frontend yet) split into a downloader pipeline (`YouTube_Installer.py`), an unrelated school-search LangChain agent (`school_searcher.py`), and SQL schema notes for two logically separate databases (`vediobase.md`, `wordbase.md`).

## Setup and commands

Dependency management uses `uv` (see `pyproject.toml`, requires Python >=3.12). `backend/requirements.txt` is a separate, more complete pinned dependency list (installed into `backend/.venv`) — check which one is actually in use before adding packages, as they've drifted apart (e.g. `langchain`, `fastapi`, `PyMySQL` appear only in `requirements.txt`).

There is no test suite, lint config, or build step in the repo currently.

Run the video downloader directly (also its own smoke test):
```
cd backend/app && python YouTube_Installer.py
```

Run the school-search agent (requires `TAVILY_API_KEY` and `DEEPSEEK_API_KEY` in `backend/.env`):
```
cd backend/app && python school_searcher.py
```

## Architecture

**Video/subtitle pipeline** (`backend/app/YouTube_Installer.py`):
- `Download_Video(download_list)` uses `yt-dlp` to download best video+audio (merged to mp4 via ffmpeg) for each URL, then separately attempts subtitle downloads.
- Subtitle language fallback is priority-ordered: Chinese tries `['zh', 'zh-Hans', 'zh-Hant', 'zh-TW']`, English tries `['en', 'en-US']`, stopping at the first available match (checks both manual `subtitles` and `automatic_captions`).
- Output paths are namespaced by a `sub_path` value imported from the video URL module: `/opt/assets/video/{sub_path}`, `/opt/assets/subs/zh/{sub_path}`, `/opt/assets/subs/en/{sub_path}`. These are absolute host paths, not relative to the repo.
- Per-URL failures are collected and don't stop the batch; a summary exception is raised at the end if any URL failed outright (subtitle failures are only warned, not fatal).
- `main.py` wraps this in a LangGraph-style `AgentState`/node (`VideoDownloader`) for future graph composition, but is not yet wired into an actual graph.

**URL batches** (`backend/app/video_url/url_*.py`): each file defines a `url_1`/`url_2`/... list of YouTube URLs plus a `sub_path` string (currently a plain numeral like `"1"`) that becomes the output subdirectory name. `YouTube_Installer.py` currently hardcodes the import to `url_1` — swap the import to process a different batch.

**School search agent** (`backend/app/school_searcher.py`, `backend/QS.py`): an unrelated LangChain `create_agent` setup using `ChatDeepSeek` + Tavily search, restricted to `include_domains=QS_150_list` (official domains of the QS top-150 universities, defined in `QS.py`). Used to answer questions about English test (IELTS/TOEFL) requirements at these schools, grounded to their official sites. Requires `.env` with `TAVILY_API_KEY` and `DEEPSEEK_API_KEY`.

**Database schemas** (not code, but define the target data model):
- `vediobase.md`: `videos` table — maps a video title to its video file path and zh/en subtitle file paths (paths correspond to the `/opt/assets/...` output structure from the downloader).
- `wordbase.md`: vocabulary schema — `categories`, `words`, `translations` (FK to `words`), `word_categories` (many-to-many join). This is the target schema for whatever NLP/vocabulary-extraction step consumes the downloaded subtitles (not yet implemented in `backend/app`).

There is no actual DB connection code yet (`PyMySQL`/`SQLAlchemy` are only in `requirements.txt`, unused so far) and no code that populates `wordbase`/`vediobase` from the downloaded subtitles — the `.md` files are schema design docs, not migrations.
