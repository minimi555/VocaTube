from sqlalchemy import Column, Integer, String, Text, ForeignKey, SmallInteger, DateTime
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from database import WordbaseBase, VideobaseBase


class Video(VideobaseBase):
    __tablename__ = "videos"

    id = Column(Integer, primary_key=True, autoincrement=True)
    title = Column(String(255), nullable=False)
    video_path = Column(String(100), nullable=False)
    subtitle_path_zh = Column(String(100))
    subtitle_path_en = Column(String(100))


class Category(WordbaseBase):
    __tablename__ = "categories"

    id = Column(Integer, primary_key=True, autoincrement=True)
    code = Column(String(50), nullable=False, unique=True)
    name = Column(String(100), nullable=False)


class Word(WordbaseBase):
    __tablename__ = "words"

    id = Column(Integer, primary_key=True, autoincrement=True)
    word = Column(String(255), nullable=False, unique=True)
    phrases = Column(Text)
    sentences = Column(Text)

    translations = relationship("Translation", back_populates="word_obj")


class Translation(WordbaseBase):
    __tablename__ = "translations"

    id = Column(Integer, primary_key=True, autoincrement=True)
    word_id = Column(Integer, ForeignKey("words.id", ondelete="CASCADE"), nullable=False)
    type = Column(String(20))
    translation = Column(String(500), nullable=False)
    sort_order = Column(SmallInteger, nullable=False, default=0)

    word_obj = relationship("Word", back_populates="translations")


class WordCategory(WordbaseBase):
    __tablename__ = "word_categories"

    word_id = Column(Integer, ForeignKey("words.id", ondelete="CASCADE"), primary_key=True)
    category_id = Column(Integer, ForeignKey("categories.id", ondelete="CASCADE"), primary_key=True)


class School(WordbaseBase):
    __tablename__ = "schools"

    id = Column(Integer, primary_key=True, autoincrement=True)
    name = Column(String(255), nullable=False)
    domain = Column(String(255), nullable=False, unique=True)
    qs_rank = Column(SmallInteger, nullable=False)


class SchoolSearchHistory(WordbaseBase):
    __tablename__ = "school_search_history"

    id = Column(Integer, primary_key=True, autoincrement=True)
    question = Column(Text, nullable=False)
    answer = Column(Text, nullable=False)
    created_at = Column(DateTime, server_default=func.now())
