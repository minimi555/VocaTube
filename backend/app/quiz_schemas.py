from pydantic import BaseModel
from typing import Optional


class QuizGenerateRequest(BaseModel):
    subtitle_text: str
    category_code: str
    video_id: Optional[int] = None


class ClozeBlank(BaseModel):
    index: int
    answer: str
    lemma: str
    sentence: str


class ReadingQuestion(BaseModel):
    index: int
    type: str
    question: str
    options: dict[str, str]
    answer: str
    explanation: str


class QuizGenerateResponse(BaseModel):
    quiz_id: str
    cloze_passage: str
    cloze_count: int
    reading_passage: str
    reading_questions: list[dict]


class QuizGradeRequest(BaseModel):
    quiz_id: str
    cloze_answers: dict[str, str]
    reading_answers: dict[str, str]


class ClozeResult(BaseModel):
    index: int
    user_answer: str
    correct_answer: str
    is_correct: bool
    lemma: str
    explanation: Optional[str] = None


class ReadingResult(BaseModel):
    index: int
    user_answer: str
    correct_answer: str
    is_correct: bool
    explanation: Optional[str] = None


class QuizScore(BaseModel):
    cloze_correct: int
    cloze_total: int
    reading_correct: int
    reading_total: int
    total_correct: int
    total_questions: int


class QuizGradeResponse(BaseModel):
    score: QuizScore
    cloze_results: list[ClozeResult]
    reading_results: list[ReadingResult]
