"""Multi-agent quiz system based on LangGraph.

Three agents:
  1. 出题Agent (generate_quiz_node) — generates cloze + reading comprehension from subtitles
  2. 审题Agent (review_quiz_node) — validates format and quality, loops back if needed
  3. 批改Agent (grade_quiz) — grades user answers with LLM-generated explanations

Usage as a module:
    from quiz_agent import generate_quiz, grade_quiz
    result = generate_quiz("subtitle text...", "CET4")
    grade = grade_quiz(result["quiz_id"], cloze_answers, reading_answers)
"""

import json
import os
import time
import uuid
from typing import TypedDict, Optional

from langchain_deepseek import ChatDeepSeek
from langgraph.graph import StateGraph, START, END
from dotenv import load_dotenv

from database import WordbaseSessionLocal
from models import Word, Category, WordCategory
from utils.lemma import _get_lemma
from quiz_prompts import (
    GENERATE_SYSTEM_PROMPT,
    GENERATE_USER_PROMPT,
    FEEDBACK_SECTION_TEMPLATE,
    REVIEW_SYSTEM_PROMPT,
    REVIEW_USER_PROMPT,
    GRADE_SYSTEM_PROMPT,
    GRADE_EXPLANATION_PROMPT,
)

load_dotenv(os.path.join(os.path.dirname(__file__), "..", ".env"))


# --------------------------------------------------------------------------- #
# In-memory quiz store (quiz_id -> {quiz_json, created_at})                    #
# --------------------------------------------------------------------------- #
_quiz_store: dict[str, dict] = {}
_STORE_TTL = 3600


def _store_quiz(quiz_id: str, quiz_json: dict):
    _quiz_store[quiz_id] = {"quiz": quiz_json, "created_at": time.time()}


def _get_stored_quiz(quiz_id: str) -> Optional[dict]:
    entry = _quiz_store.get(quiz_id)
    if entry is None:
        return None
    if time.time() - entry["created_at"] > _STORE_TTL:
        del _quiz_store[quiz_id]
        return None
    return entry["quiz"]


# --------------------------------------------------------------------------- #
# Per-agent LLM instances                                                      #
# --------------------------------------------------------------------------- #
_generate_llm = None
_review_llm = None
_grade_llm = None


def _get_generate_llm():
    global _generate_llm
    if _generate_llm is None:
        _generate_llm = ChatDeepSeek(model="deepseek-v4-pro", temperature=0.4, timeout=300)
    return _generate_llm


def _get_review_llm():
    global _review_llm
    if _review_llm is None:
        _review_llm = ChatDeepSeek(model="deepseek-v4-pro", temperature=0.2, timeout=300)
    return _review_llm


def _get_grade_llm():
    global _grade_llm
    if _grade_llm is None:
        _grade_llm = ChatDeepSeek(model="deepseek-v4-pro", temperature=0.6, timeout=300)
    return _grade_llm


# --------------------------------------------------------------------------- #
# LangGraph State                                                              #
# --------------------------------------------------------------------------- #
class QuizState(TypedDict):
    subtitle_text: str
    category_code: str
    category_words: list[str]
    subtitle_words: list[str]
    quiz_json: Optional[dict]
    review_feedback: Optional[str]
    review_count: int
    review_passed: bool
    validated_quiz: Optional[dict]


# --------------------------------------------------------------------------- #
# Node 1: prepare_words (pure script)                                          #
# --------------------------------------------------------------------------- #
def prepare_words_node(state: QuizState) -> dict:
    text = state["subtitle_text"]
    category_code = state["category_code"]

    lemmas = _get_lemma(text)
    subtitle_words = sorted(set(lemmas))

    db = WordbaseSessionLocal()
    try:
        category = db.query(Category).filter(Category.code == category_code).first()
        if not category: #不存在单词对应分类，eg CET4
            return {
                "subtitle_words": subtitle_words,
                "category_words": [],
                "review_count": 0,
                "review_passed": False,
            }
        matching = (
            db.query(Word.word)
            .join(WordCategory, Word.id == WordCategory.word_id)
            .filter(
                WordCategory.category_id == category.id,
                Word.word.in_(subtitle_words),
            )
            .all()
        )
        category_words = [row[0] for row in matching]
    finally:
        db.close()

    return {
        "subtitle_words": subtitle_words,
        "category_words": category_words,
        "review_count": 0,
        "review_passed": False,
    }


# --------------------------------------------------------------------------- #
# Node 2: generate_quiz (LLM call)                                             #
# --------------------------------------------------------------------------- #
def generate_quiz_node(state: QuizState) -> dict:
    llm = _get_generate_llm()

    feedback_section = ""
    if state.get("review_feedback"):
        feedback_section = FEEDBACK_SECTION_TEMPLATE.format(feedback=state["review_feedback"])

    user_prompt = GENERATE_USER_PROMPT.format(
        subtitle_text=state["subtitle_text"],
        category_words=", ".join(state["category_words"]) if state["category_words"] else "（无匹配词汇，请从字幕词汇中随即选择）",
        subtitle_words=", ".join(state["subtitle_words"][:60]),
        feedback_section=feedback_section,
    )

    response = llm.invoke([
        {"role": "system", "content": GENERATE_SYSTEM_PROMPT},
        {"role": "user", "content": user_prompt},
    ])

    content = response.content
    try:
        if "```json" in content:
            content = content.split("```json")[1].split("```")[0]
        elif "```" in content:
            content = content.split("```")[1].split("```")[0]
        quiz_json = json.loads(content.strip())
    except (json.JSONDecodeError, IndexError):
        quiz_json = {"error": "JSON parse failed", "raw": response.content[:500]}

    return {"quiz_json": quiz_json}


# --------------------------------------------------------------------------- #
# Node 3: review_quiz (script + LLM)                                           #
# --------------------------------------------------------------------------- #
def _script_validate(quiz_json: dict, subtitle_words: list[str]) -> Optional[str]:
    """Deterministic structure validation. Returns error message or None."""
    if "error" in quiz_json:
        return f"出题Agent未能生成有效JSON: {quiz_json.get('error', '')}"

    cloze = quiz_json.get("cloze")
    if not cloze or not isinstance(cloze, dict):
        return "缺少 cloze 字段或格式错误"
    if "passage" not in cloze:
        return "cloze 缺少 passage 字段"
    blanks = cloze.get("blanks")
    if not blanks or not isinstance(blanks, list):
        return "cloze 缺少 blanks 列表"
    if len(blanks) != 10:
        return f"cloze blanks 数量为 {len(blanks)}，要求10个"

    for i, blank in enumerate(blanks):
        for field in ("index", "answer", "lemma", "sentence"):
            if field not in blank:
                return f"cloze blank #{i+1} 缺少 {field} 字段"
        lemma = blank["lemma"].lower()
        if lemma not in subtitle_words:
            return f"cloze blank #{i+1} 的lemma '{lemma}' 不在字幕词汇中"

    word_bank = cloze.get("word_bank")
    if not word_bank or not isinstance(word_bank, list):
        return "cloze 缺少 word_bank 列表"
    if len(word_bank) != 15:
        return f"word_bank 数量为 {len(word_bank)}，要求15个"
    answers_exact = {blank["answer"] for blank in blanks}
    if len(answers_exact) != 10:
        return "cloze blanks 中存在重复答案词"
    bank_lower = [w.lower() for w in word_bank]
    if len(set(bank_lower)) != 15:
        return "word_bank 中存在重复单词"
    bank_set = set(word_bank)
    for ans in answers_exact:
        if ans not in bank_set:
            return f"word_bank 中缺少答案词 '{ans}'（大小写须一致）"
    answers_lower = {a.lower() for a in answers_exact}
    distractors = [w for w in bank_lower if w not in answers_lower]
    for w in distractors:
        if w not in subtitle_words:
            return f"word_bank 干扰词 '{w}' 不在字幕词汇中"

    rc = quiz_json.get("reading_comprehension")
    if not rc or not isinstance(rc, dict):
        return "缺少 reading_comprehension 字段"
    if "passage" not in rc:
        return "reading_comprehension 缺少 passage 字段"
    questions = rc.get("questions")
    if not questions or not isinstance(questions, list):
        return "reading_comprehension 缺少 questions 列表"
    if len(questions) != 2:
        return f"reading_comprehension questions 数量为 {len(questions)}，要求2个"

    for i, q in enumerate(questions):
        for field in ("index", "question", "options", "answer"):
            if field not in q:
                return f"reading question #{i+1} 缺少 {field} 字段"
        opts = q.get("options", {})
        if not all(k in opts for k in ("A", "B", "C", "D")):
            return f"reading question #{i+1} 选项不完整，需要A/B/C/D"
        if q["answer"] not in ("A", "B", "C", "D"):
            return f"reading question #{i+1} answer '{q['answer']}' 不在 A/B/C/D 中"

    return None


def review_quiz_node(state: QuizState) -> dict:
    quiz_json = state["quiz_json"]
    subtitle_words = state["subtitle_words"]
    new_count = state["review_count"] + 1

    script_error = _script_validate(quiz_json, subtitle_words)
    if script_error:
        return {
            "review_passed": False,
            "review_feedback": f"【格式错误】{script_error}",
            "review_count": new_count,
        }

    llm = _get_review_llm()
    review_prompt = REVIEW_USER_PROMPT.format(
        subtitle_text=state["subtitle_text"][:2000],
        subtitle_words=", ".join(subtitle_words[:80]),
        quiz_json=json.dumps(quiz_json, ensure_ascii=False, indent=2)[:3000],
    )
    response = llm.invoke([
        {"role": "system", "content": REVIEW_SYSTEM_PROMPT},
        {"role": "user", "content": review_prompt},
    ])

    answer = response.content.strip()
    if "PASS" in answer.upper() and len(answer) < 50:
        return {"review_passed": True, "review_count": new_count}

    return {
        "review_passed": False,
        "review_feedback": answer,
        "review_count": new_count,
    }


# --------------------------------------------------------------------------- #
# Node 4: finalize                                                             #
# --------------------------------------------------------------------------- #
def finalize_node(state: QuizState) -> dict:
    return {"validated_quiz": state["quiz_json"]}


# --------------------------------------------------------------------------- #
# Conditional routing                                                          #
# --------------------------------------------------------------------------- #
def route_after_review(state: QuizState) -> str:
    if state["review_passed"] or state["review_count"] >= 2:
        return "finalize"
    return "generate_quiz"


# --------------------------------------------------------------------------- #
# Graph compilation (lazy singleton)                                           #
# --------------------------------------------------------------------------- #
_graph = None


def _get_graph():
    global _graph
    if _graph is not None:
        return _graph

    builder = StateGraph(QuizState)
    builder.add_node("prepare_words", prepare_words_node)
    builder.add_node("generate_quiz", generate_quiz_node)
    builder.add_node("review_quiz", review_quiz_node)
    builder.add_node("finalize", finalize_node)

    builder.add_edge(START, "prepare_words")
    builder.add_edge("prepare_words", "generate_quiz")
    builder.add_edge("generate_quiz", "review_quiz")
    builder.add_conditional_edges(
        "review_quiz",
        route_after_review,
        {"generate_quiz": "generate_quiz", "finalize": "finalize"},
    )
    builder.add_edge("finalize", END)

    _graph = builder.compile()
    return _graph


# --------------------------------------------------------------------------- #
# Public API: generate_quiz                                                    #
# --------------------------------------------------------------------------- #
def generate_quiz(subtitle_text: str, category_code: str) -> dict:
    """Run the generate+review graph and return quiz for frontend.

    Args:
        subtitle_text: plain text from subtitles
        category_code: e.g. "CET4", "TOEFL"

    Returns:
        {"quiz_id": str, "cloze_passage": str, "cloze_count": int,
         "word_bank": list, "reading_passage": str, "reading_questions": list}
    """

    graph = _get_graph()
    result = graph.invoke({
        "subtitle_text": subtitle_text,
        "category_code": category_code,
        "category_words": [],
        "subtitle_words": [],
        "quiz_json": None,
        "review_feedback": None,
        "review_count": 0,
        "review_passed": False,
        "validated_quiz": None,
    })

    quiz_json = result["validated_quiz"]
    quiz_id = str(uuid.uuid4())
    _store_quiz(quiz_id, quiz_json)

    reading_questions_no_answer = []
    for q in quiz_json.get("reading_comprehension", {}).get("questions", []):
        reading_questions_no_answer.append({
            "index": q["index"],
            "type": q.get("type", ""),
            "question": q["question"],
            "options": q["options"],
        })

    return {
        "quiz_id": quiz_id,
        "cloze_passage": quiz_json.get("cloze", {}).get("passage", ""),
        "cloze_count": len(quiz_json.get("cloze", {}).get("blanks", [])),
        "word_bank": quiz_json.get("cloze", {}).get("word_bank", []),
        "reading_passage": quiz_json.get("reading_comprehension", {}).get("passage", ""),
        "reading_questions": reading_questions_no_answer,
    }


# --------------------------------------------------------------------------- #
# Public API: grade_quiz (批改Agent)                                            #
# --------------------------------------------------------------------------- #
def grade_quiz(quiz_id: str, cloze_answers: dict[str, str], reading_answers: dict[str, str]) -> dict:
    """Grade user answers against stored correct answers.

    Returns grading result with score and per-question details.
    Calls LLM for explanations on wrong answers.
    """
    quiz_json = _get_stored_quiz(quiz_id)
    if quiz_json is None:
        raise ValueError("Quiz not found or expired")

    blanks = quiz_json.get("cloze", {}).get("blanks", [])
    questions = quiz_json.get("reading_comprehension", {}).get("questions", [])

    cloze_results = []
    wrong_items = []

    for blank in blanks:
        idx = str(blank["index"])
        user_ans = cloze_answers.get(idx, "")
        correct = blank["answer"]
        is_correct = user_ans == correct
        cloze_results.append({
            "index": blank["index"],
            "user_answer": user_ans,
            "correct_answer": correct,
            "is_correct": is_correct,
            "lemma": blank["lemma"],
            "explanation": None,
        })
        if not is_correct:
            wrong_items.append({
                "index": blank["index"],
                "type": "cloze",
                "question": f"填空第{blank['index']}题，原句: {blank['sentence']}",
                "user_answer": user_ans,
                "correct_answer": correct,
            })

    reading_results = []
    for q in questions:
        idx = str(q["index"])
        user_ans = reading_answers.get(idx, "")
        correct = q["answer"]
        is_correct = user_ans == correct
        reading_results.append({
            "index": q["index"],
            "user_answer": user_ans,
            "correct_answer": correct,
            "is_correct": is_correct,
            "explanation": None,
        })
        if not is_correct:
            wrong_items.append({
                "index": q["index"],
                "type": "reading",
                "question": q["question"],
                "user_answer": user_ans,
                "correct_answer": correct,
            })

    cloze_correct = sum(1 for r in cloze_results if r["is_correct"])
    reading_correct = sum(1 for r in reading_results if r["is_correct"])

    if wrong_items:
        explanations = _generate_explanations(
            quiz_json.get("reading_comprehension", {}).get("passage", ""),
            wrong_items,
        )
        for exp in explanations:
            idx = exp["index"]
            exp_type = exp["type"]
            explanation_text = exp["explanation"]
            if exp_type == "cloze":
                for r in cloze_results:
                    if r["index"] == idx and not r["is_correct"]:
                        r["explanation"] = explanation_text
            elif exp_type == "reading":
                for r in reading_results:
                    if r["index"] == idx and not r["is_correct"]:
                        r["explanation"] = explanation_text

    return {
        "score": {
            "cloze_correct": cloze_correct,
            "cloze_total": len(blanks),
            "reading_correct": reading_correct,
            "reading_total": len(questions),
            "total_correct": cloze_correct + reading_correct,
            "total_questions": len(blanks) + len(questions),
        },
        "cloze_results": cloze_results,
        "reading_results": reading_results,
    }


def _generate_explanations(subtitle_text: str, wrong_items: list[dict]) -> list[dict]:
    """Call LLM to generate explanations for wrong answers."""
    llm = _get_grade_llm()

    wrong_text = ""
    for item in wrong_items:
        wrong_text += (
            f"- 题号{item['index']}（{item['type']}）：{item['question']}\n"
            f"  学生答案：{item['user_answer']}，正确答案：{item['correct_answer']}\n"
        )

    prompt = GRADE_EXPLANATION_PROMPT.format(
        subtitle_text=subtitle_text[:2000],
        wrong_questions=wrong_text,
    )

    response = llm.invoke([
        {"role": "system", "content": GRADE_SYSTEM_PROMPT},
        {"role": "user", "content": prompt},
    ])
    content = response.content

    try:
        if "```json" in content:
            content = content.split("```json")[1].split("```")[0]
        elif "```" in content:
            content = content.split("```")[1].split("```")[0]
        result = json.loads(content.strip())
        return result.get("explanations", [])
    except (json.JSONDecodeError, IndexError):
        return [{"index": item["index"], "type": item["type"], "explanation": "解析生成失败"} for item in wrong_items]
