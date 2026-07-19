"""Seed the schools table from QS.py data.

Creates the `schools` and `school_search_history` tables if they don't exist,
then upserts all QS top-150 school entries (idempotent by domain).

Usage:
    cd backend/app && python seed_schools.py
"""

import sys
import os

sys.path.insert(0, os.path.dirname(__file__))
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from database import wordbase_engine, WordbaseSessionLocal, WordbaseBase
from models import School, SchoolSearchHistory  # noqa: F401 — registers tables
from QS import QS_150_schools


def seed():
    # Create tables that don't exist yet
    WordbaseBase.metadata.create_all(bind=wordbase_engine)
    print("[seed] tables ensured: schools, school_search_history")

    db = WordbaseSessionLocal()
    try:
        added = 0
        updated = 0
        for entry in QS_150_schools:
            existing = db.query(School).filter(School.domain == entry["domain"]).first()
            if existing:
                # Update if rank or name changed
                if existing.qs_rank != entry["rank"] or existing.name != entry["name"]:
                    existing.qs_rank = entry["rank"]
                    existing.name = entry["name"]
                    updated += 1
            else:
                db.add(School(
                    name=entry["name"],
                    domain=entry["domain"],
                    qs_rank=entry["rank"],
                ))
                added += 1

        db.commit()
        total = db.query(School).count()
        print(f"[seed] done — added {added}, updated {updated}, total {total} schools")
    finally:
        db.close()


if __name__ == "__main__":
    seed()
