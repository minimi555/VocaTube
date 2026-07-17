"""Simple RAG over the english_learning_md/ markdown docs.

Embedding : Zhipu embedding-3 (HTTP, wrapped as a LangChain Embeddings)
Vector DB : Chroma (persisted to ./chroma_db)
LLM       : DeepSeek deepseek-v4-flash (via langchain-deepseek)

Usage:
    python RAG.py ingest          # (re)build the vector store from the md files
    python RAG.py ask "问题..."    # one-off question from the command line
    python RAG.py                 # interactive Q&A loop
"""
import os
import sys
import glob
import shutil
from typing import List

import requests
from dotenv import load_dotenv
from langchain_core.embeddings import Embeddings
from langchain_core.documents import Document
from langchain_text_splitters import (
    MarkdownHeaderTextSplitter,
    RecursiveCharacterTextSplitter,
)
from langchain_chroma import Chroma
from langchain_deepseek import ChatDeepSeek

# .env lives at backend/.env (one level up from this app/ dir)
load_dotenv(os.path.join(os.path.dirname(__file__), "..", ".env"))

MD_DIR = os.path.join(os.path.dirname(__file__), "english_learning_md")
CHROMA_DIR = os.path.join(os.path.dirname(__file__), "chroma_db")
COLLECTION = "english_learning"

EMBED_MODEL = "embedding-3"
EMBED_BATCH = 64          # zhipu embeddings accept batched input
LLM_MODEL = "deepseek-v4-flash"
TOP_K = 4                 # how many chunks to feed the LLM


# --------------------------------------------------------------------------- #
# Embeddings: thin LangChain wrapper around the Zhipu embedding HTTP endpoint  #
# --------------------------------------------------------------------------- #
class ZhipuEmbeddings(Embeddings):
    def __init__(self, model: str = EMBED_MODEL):
        self.model = model
        self.url = os.getenv("ZHIPU_URL")
        self.key = os.getenv("ZHIPU_API_KEY")
        if not self.url or not self.key:
            raise RuntimeError("ZHIPU_URL / ZHIPU_API_KEY missing in .env")

    def _embed(self, texts: List[str]) -> List[List[float]]:
        headers = {
            "Authorization": f"Bearer {self.key}",
            "Content-Type": "application/json",
        }
        out: List[List[float]] = []
        for i in range(0, len(texts), EMBED_BATCH):
            batch = texts[i:i + EMBED_BATCH]
            resp = requests.post(
                self.url,
                headers=headers,
                json={"model": self.model, "input": batch},
                timeout=60,
            )
            resp.raise_for_status()
            data = resp.json()["data"]
            # keep the API's returned order stable
            data.sort(key=lambda d: d["index"])
            out.extend(d["embedding"] for d in data)
        return out

    def embed_documents(self, texts: List[str]) -> List[List[float]]:
        return self._embed(texts)

    def embed_query(self, text: str) -> List[float]:
        return self._embed([text])[0]


# --------------------------------------------------------------------------- #
# Ingest: markdown -> header-aware chunks -> Chroma                            #
# --------------------------------------------------------------------------- #
def load_and_split() -> List[Document]:
    """Split each md file on its headers, then cap oversized sections."""
    header_splitter = MarkdownHeaderTextSplitter(
        headers_to_split_on=[("#", "h1"), ("##", "h2"), ("###", "h3")],
        strip_headers=False,
    )
    char_splitter = RecursiveCharacterTextSplitter(
        chunk_size=800, chunk_overlap=100
    )

    docs: List[Document] = []
    for path in sorted(glob.glob(os.path.join(MD_DIR, "*.md"))):
        source = os.path.basename(path)
        with open(path, encoding="utf-8") as f:
            text = f.read()
        for sec in header_splitter.split_text(text):
            # build a readable header trail e.g. "四级 > 听力"
            trail = " > ".join(
                sec.metadata[k] for k in ("h1", "h2", "h3") if sec.metadata.get(k)
            )
            for chunk in char_splitter.split_text(sec.page_content):
                docs.append(Document(
                    page_content=chunk,
                    metadata={"source": source, "section": trail},
                ))
    return docs


def build_vectorstore() -> Chroma:
    """(Re)build the persistent Chroma store from scratch."""
    if os.path.exists(CHROMA_DIR):
        shutil.rmtree(CHROMA_DIR)
    docs = load_and_split()
    print(f"[ingest] {len(docs)} chunks from {MD_DIR}")
    store = Chroma.from_documents(
        documents=docs,
        embedding=ZhipuEmbeddings(),
        collection_name=COLLECTION,
        persist_directory=CHROMA_DIR,
    )
    print(f"[ingest] done -> {CHROMA_DIR}")
    return store


def get_vectorstore() -> Chroma:
    if not os.path.exists(CHROMA_DIR):
        raise RuntimeError("Vector store not found. Run: python RAG.py ingest")
    return Chroma(
        collection_name=COLLECTION,
        embedding_function=ZhipuEmbeddings(),
        persist_directory=CHROMA_DIR,
    )


# --------------------------------------------------------------------------- #
# Query: retrieve -> prompt -> DeepSeek                                        #
# --------------------------------------------------------------------------- #
_PROMPT = """你是一个英语备考助手。请**仅根据**下面提供的资料回答用户的问题。
如果资料中没有相关信息，就直说“资料中没有相关内容”，不要编造。请用中文回答。

【资料】
{context}

【问题】
{question}

【回答】"""

_llm = None
_store = None


def _get_llm() -> ChatDeepSeek:
    global _llm
    if _llm is None:
        _llm = ChatDeepSeek(
            model=LLM_MODEL,
            api_key=os.getenv("DEEPSEEK_API_KEY"),
            temperature=0.3,
        )
    return _llm


def answer(question: str, k: int = TOP_K) -> dict:
    """Retrieve the top-k chunks and let DeepSeek answer from them."""
    global _store
    if _store is None:
        _store = get_vectorstore()

    hits = _store.similarity_search(question, k=k)
    context = "\n\n---\n\n".join(
        f"[来源: {d.metadata.get('source')} | {d.metadata.get('section')}]\n{d.page_content}"
        for d in hits
    )
    prompt = _PROMPT.format(context=context, question=question)
    resp = _get_llm().invoke(prompt)

    sources = [
        {"source": d.metadata.get("source"), "section": d.metadata.get("section")}
        for d in hits
    ]
    return {"answer": resp.content, "sources": sources}


# --------------------------------------------------------------------------- #
# CLI                                                                          #
# --------------------------------------------------------------------------- #
def _repl():
    print("RAG 已就绪，输入问题（回车提问，Ctrl-C 退出）")
    try:
        while True:
            q = input("\n问> ").strip()
            if not q:
                continue
            res = answer(q)
            print("\n答>", res["answer"])
            print("\n来源:", ", ".join(
                f"{s['source']}({s['section']})" for s in res["sources"]))
    except (KeyboardInterrupt, EOFError):
        print("\nbye")


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else "repl"
    if cmd == "ingest":
        build_vectorstore()
    elif cmd == "ask":
        res = answer(" ".join(sys.argv[2:]))
        print(res["answer"])
        print("\n来源:", res["sources"])
    else:
        _repl()
