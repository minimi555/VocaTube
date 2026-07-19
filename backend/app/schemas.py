from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime


class TranslationItem(BaseModel):
    type: Optional[str]
    translation: str

    class Config:
        from_attributes = True


class PhraseItem(BaseModel):
    phrase: str
    translation: str


class SentenceItem(BaseModel):
    sentence: str
    translation: str


class WordDetail(BaseModel):
    word: str
    translations: List[TranslationItem]
    phrases: List[PhraseItem]
    sentences: List[SentenceItem]
    categories: List[str]


class WordCategoryCheck(BaseModel):
    word: str
    category_code: str
    belongs: bool


class VideoTitle(BaseModel):
    id: int
    title: str

    class Config:
        from_attributes = True


class VideoDetail(BaseModel):
    id: int
    title: str
    video_url: str
    subtitle_zh_url: Optional[str]
    subtitle_en_url: Optional[str]

    class Config:
        from_attributes = True


class SourceItem(BaseModel):
    source: Optional[str]
    section: Optional[str]


class AskRequest(BaseModel):
    question: str
    k: int = 4


class AskResponse(BaseModel):
    answer: str
    sources: List[SourceItem]


# ---- School search schemas ------------------------------------------------ #

class SchoolItem(BaseModel):
    id: int
    name: str
    domain: str
    qs_rank: int

    class Config:
        from_attributes = True


class SchoolSearchRequest(BaseModel):
    question: str


class SchoolSearchResponse(BaseModel):
    answer: str


class SchoolSearchHistoryItem(BaseModel):
    id: int
    question: str
    answer: str
    created_at: datetime

    class Config:
        from_attributes = True
