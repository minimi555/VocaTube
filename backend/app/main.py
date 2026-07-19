import json
from fastapi import FastAPI, Depends, HTTPException, Query
from fastapi.staticfiles import StaticFiles
from sqlalchemy.orm import Session
from typing import List

from database import get_wordbase_db, get_videobase_db
from models import Word, Translation, Category, WordCategory, Video, School, SchoolSearchHistory
from schemas import (
    WordDetail, WordCategoryCheck, VideoTitle, VideoDetail,
    TranslationItem, PhraseItem, SentenceItem, AskRequest, AskResponse,
    SchoolItem, SchoolSearchRequest, SchoolSearchResponse, SchoolSearchHistoryItem,
)
import RAG
import school_searcher

app = FastAPI(title="VocaTube API")

app.mount("/assets", StaticFiles(directory="/opt/assets"), name="assets")


@app.get("/words/{word}", response_model=WordDetail)
def get_word_detail(word: str, db: Session = Depends(get_wordbase_db)):
    word_obj = db.query(Word).filter(Word.word == word).first()
    if not word_obj:
        raise HTTPException(status_code=404, detail="Word not found")

    translations = [
        TranslationItem(type=t.type, translation=t.translation)
        for t in sorted(word_obj.translations, key=lambda x: x.sort_order)
    ]

    phrases = []
    if word_obj.phrases:
        try:
            phrases = json.loads(word_obj.phrases)
        except json.JSONDecodeError:
            phrases = []

    sentences = []
    if word_obj.sentences:
        try:
            sentences = json.loads(word_obj.sentences)
        except json.JSONDecodeError:
            sentences = []

    category_ids = db.query(WordCategory.category_id).filter(WordCategory.word_id == word_obj.id).all()
    category_codes = [
        db.query(Category.code).filter(Category.id == cat_id[0]).scalar()
        for cat_id in category_ids
    ]

    return WordDetail(
        word=word_obj.word,
        translations=translations,
        phrases=phrases,
        sentences=sentences,
        categories=category_codes
    )


@app.get("/words/{word}/category/{category_code}", response_model=WordCategoryCheck)
def check_word_category(word: str, category_code: str, db: Session = Depends(get_wordbase_db)):
    word_obj = db.query(Word).filter(Word.word == word).first()
    if not word_obj:
        raise HTTPException(status_code=404, detail="Word not found")

    category = db.query(Category).filter(Category.code == category_code).first()
    if not category:
        raise HTTPException(status_code=404, detail="Category not found")

    exists = db.query(WordCategory).filter(
        WordCategory.word_id == word_obj.id,
        WordCategory.category_id == category.id
    ).first() is not None

    return WordCategoryCheck(word=word, category_code=category_code, belongs=exists)


@app.get("/videos", response_model=List[VideoTitle])
def list_videos(db: Session = Depends(get_videobase_db)):
    videos = db.query(Video).all()
    return videos


@app.get("/videos/{video_id}", response_model=VideoDetail)
def get_video_detail(video_id: int, db: Session = Depends(get_videobase_db)):
    video = db.query(Video).filter(Video.id == video_id).first()
    if not video:
        raise HTTPException(status_code=404, detail="Video not found")

    video_url = f"/assets/video/{video.video_path}"
    subtitle_zh_url = f"/assets/subs/zh/{video.subtitle_path_zh}" if video.subtitle_path_zh else None
    subtitle_en_url = f"/assets/subs/en/{video.subtitle_path_en}" if video.subtitle_path_en else None

    return VideoDetail(
        id=video.id,
        title=video.title,
        video_url=video_url,
        subtitle_zh_url=subtitle_zh_url,
        subtitle_en_url=subtitle_en_url
    )


@app.post("/ask", response_model=AskResponse)
def ask(req: AskRequest):
    """RAG over the english_learning_md docs (CET4/CET6/SAT/kaoyan)."""
    if not req.question.strip():
        raise HTTPException(status_code=400, detail="question is empty")
    try:
        result = RAG.answer(req.question, k=req.k)
    except RuntimeError as e:
        # vector store not built yet -> tell the caller to run `python RAG.py ingest`
        raise HTTPException(status_code=503, detail=str(e))
    return result


# --------------------------------------------------------------------------- #
# School search endpoints                                                      #
# --------------------------------------------------------------------------- #

@app.get("/schools", response_model=List[SchoolItem])
def list_schools(db: Session = Depends(get_wordbase_db)):
    """List all QS top-150 schools, ordered by rank."""
    return db.query(School).order_by(School.qs_rank, School.name).all()


@app.post("/school/search", response_model=SchoolSearchResponse)
def school_search(req: SchoolSearchRequest, db: Session = Depends(get_wordbase_db)):
    """Search QS top-150 school websites for English requirements etc."""
    if not req.question.strip():
        raise HTTPException(status_code=400, detail="question is empty")
    try:
        result = school_searcher.search_school(req.question)
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"School search agent error: {e}")

    # persist to history
    record = SchoolSearchHistory(question=req.question, answer=result["answer"])
    db.add(record)
    db.commit()

    return SchoolSearchResponse(answer=result["answer"])


@app.get("/school/history", response_model=List[SchoolSearchHistoryItem])
def school_search_history(
    limit: int = Query(default=20, ge=1, le=100),
    db: Session = Depends(get_wordbase_db),
):
    """Return recent school search history, newest first."""
    rows = (
        db.query(SchoolSearchHistory)
        .order_by(SchoolSearchHistory.created_at.desc())
        .limit(limit)
        .all()
    )
    return rows
