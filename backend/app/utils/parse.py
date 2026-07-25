import re
from pathlib import Path

# PATH_SRT = "/opt/assets/subs/..." 
def _parse_srt(path):
    text = Path(path).read_text(encoding="utf-8")
    lines = []
    for block in re.split(r"\n\s*\n", text.strip()):
        for line in block.splitlines():
            line = line.strip()
            if not line or line.isdigit() or "-->" in line:
                continue
            lines.append(line)
    return " ".join(lines)


def _parse_subs(path)->str:
    return _parse_srt(path)
