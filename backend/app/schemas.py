from pydantic import BaseModel
from typing import List, Optional


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
