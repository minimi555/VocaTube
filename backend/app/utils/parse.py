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
    srt_path = "/home/xjc/Files/program/word_process/srt/JR： Can Art Change the World？ ｜ TED.en.srt" if not Path(path).exists() else path
    return _parse_srt(srt_path)
