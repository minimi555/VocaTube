from pydantic_settings import BaseSettings
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base


class Settings(BaseSettings):
    DB_HOST: str
    DB_PORT: int
    DB_USER: str
    DB_PASSWORD: str
    DB_NAME_MAIN: str
    DB_NAME_LOG: str

    class Config:
        env_file = "../.env"
        extra = "ignore"


settings = Settings()

wordbase_url = f"mysql+pymysql://{settings.DB_USER}:{settings.DB_PASSWORD}@{settings.DB_HOST}:{settings.DB_PORT}/{settings.DB_NAME_MAIN}?charset=utf8mb4"
videobase_url = f"mysql+pymysql://{settings.DB_USER}:{settings.DB_PASSWORD}@{settings.DB_HOST}:{settings.DB_PORT}/{settings.DB_NAME_LOG}?charset=utf8mb4"

wordbase_engine = create_engine(wordbase_url, pool_pre_ping=True)
videobase_engine = create_engine(videobase_url, pool_pre_ping=True)

WordbaseSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=wordbase_engine)
VideobaseSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=videobase_engine)

WordbaseBase = declarative_base()
VideobaseBase = declarative_base()


def get_wordbase_db():
    db = WordbaseSessionLocal()
    try:
        yield db
    finally:
        db.close()


def get_videobase_db():
    db = VideobaseSessionLocal()
    try:
        yield db
    finally:
        db.close()
