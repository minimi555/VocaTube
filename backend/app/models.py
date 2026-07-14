from sqlalchemy import Column, Integer, String, Text, ForeignKey, SmallInteger
from sqlalchemy.orm import relationship
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
