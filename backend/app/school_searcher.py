"""QS top-150 school search agent.

Uses LangChain + DeepSeek + Tavily (domain-restricted to QS top-150 official
websites) to answer questions about university English proficiency requirements.

Usage as a module:
    from school_searcher import search_school
    result = search_school("香港中文大学计算机硕士雅思要求")
    print(result["answer"])

Usage as a script (demo):
    python school_searcher.py
"""

import os
import sys
from datetime import date
from typing import List

from langchain.agents import create_agent
from langchain_deepseek import ChatDeepSeek
from langchain_tavily import TavilySearch
from langchain_core.tools import tool
from dotenv import load_dotenv

# .env lives at backend/.env (one level up from this app/ dir)
load_dotenv(os.path.join(os.path.dirname(__file__), "..", ".env"))

# Import QS domain list — QS.py lives one level up (backend/)
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
from QS import QS_150_list  # noqa: E402


# --------------------------------------------------------------------------- #
# Tools                                                                       #
# --------------------------------------------------------------------------- #
@tool
def get_current_date() -> str:
    """返回当前日期（只包含年月日），格式为"YYYY年M月D日"。"""
    today = date.today()
    return f"{today.year}年{today.month}月{today.day}日"


# --------------------------------------------------------------------------- #
# Lazy-init agent singleton (same pattern as RAG.py _get_llm)                 #
# --------------------------------------------------------------------------- #
_agent = None


def _get_agent():
    global _agent
    if _agent is not None:
        return _agent

    tavily_search = TavilySearch(
        max_results=5,
        topic="general",
        search_depth="basic",
        include_domains=QS_150_list,
    )

    _agent = create_agent(
        model=ChatDeepSeek(model="deepseek-v4-flash", timeout=None),
        tools=[tavily_search, get_current_date],
        system_prompt=(
            "你是一个针对QS前150的学校的web检索助手，你可以根据用户的提问，"
            "检索相关学校的官方网站内容，并提供准确的回答。"
            "请确保你的回答基于可靠的来源，并尽量引用官方网站的信息并附上具体网址。"
            "注意信息来源时间和用户提问时间，确保信息的时效性。"
            "若用户指明自己的意向专业，请搜索各个学校的官方网站，"
            "提供该该学校专业对英语要求的详细信息，并附上具体网址。"
        ),
    )
    return _agent


# --------------------------------------------------------------------------- #
# Public API                                                                   #
# --------------------------------------------------------------------------- #
def search_school(question: str) -> dict:
    """Search QS top-150 school websites and return an answer.

    Args:
        question: user question in natural language (Chinese or English)

    Returns:
        {"answer": str} — the agent's final answer text (includes inline URLs)
    """
    agent = _get_agent()
    current_date = get_current_date.invoke({})
    response = agent.invoke(
        {"messages": [{"role": "user", "content": f"当前时间为{current_date}，{question}"}]}
    )
    return {"answer": response["messages"][-1].content}


# --------------------------------------------------------------------------- #
# CLI demo                                                                     #
# --------------------------------------------------------------------------- #
if __name__ == "__main__":
    print("Agent initialized and ready to use.")
    print("searching...")
    result = search_school("香港中文大学计算机相关硕士项目的对英语雅思和托福的要求")
    print("\n===== 最终回答 =====")
    print(result["answer"])
