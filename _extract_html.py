import json
import os
import re
from pathlib import Path

BASE = Path(r"C:\Users\pablito\.cursor\projects\s-Source-Repos-Bod-Faltas\agent-transcripts")
OUT_DIR = Path(r"S:\Source\Repos\Bod-Faltas\_extracted_html")

OUT_DIR.mkdir(exist_ok=True)

markers = [
    "bandejasColapsadas",
    "list-panel__tools",
    "puedeMostrarBloqueCierreActa",
    "generarNulidad",
    "documentoEsFirmable",
    "event-timeline",
    "cierre-acta-titulo",
    "archivo-acta-titulo",
    "gestion-externa-titulo",
    "notificacion-acta-titulo",
    "pago-voluntario-titulo",
    "piezas-redaccion",
]

candidates = []

for root, _, files in os.walk(BASE):
    for fn in files:
        if not fn.endswith(".jsonl"):
            continue
        raw = (Path(root) / fn).read_text(encoding="utf-8", errors="ignore")
        for match in re.finditer(
            r"<mat-sidenav-container[\s\S]{3000,}?</mat-sidenav-container>", raw
        ):
            chunk = match.group(0)
            chunk = chunk.replace("\\n", "\n").replace('\\"', '"')
            chunk = chunk.replace("<motion", "<div").replace("</motion>", "</div>")
            start = chunk.find("<mat-sidenav-container")
            end = chunk.find("</mat-sidenav-container>")
            if start >= 0 and end >= 0:
                chunk = chunk[start : end + len("</mat-sidenav-container>")]
            score = sum(1 for m in markers if m in chunk)
            candidates.append((len(chunk), score, os.path.basename(root), fn, chunk))

candidates.sort(key=lambda x: (-x[1], -x[0]))
print(f"total candidates: {len(candidates)}")
for i, (ln, score, sess, fn, _) in enumerate(candidates[:15]):
    print(i, ln, score, sess, fn)

if candidates:
    best = max(candidates, key=lambda x: (x[1], x[0]))
    out = OUT_DIR / "best_by_score.html"
    out.write_text(best[4], encoding="utf-8")
    print(f"saved score-best {out} len={best[0]} score={best[1]} from {best[2]}")

    longest = max(candidates, key=lambda x: x[0])
    out2 = OUT_DIR / "longest.html"
    out2.write_text(longest[4], encoding="utf-8")
    print(f"saved longest {out2} len={longest[0]} score={longest[1]} from {longest[2]}")

# Also extract Write tool contents for demo-shell.component.html
write_candidates = []
for root, _, files in os.walk(BASE):
    for fn in files:
        if not fn.endswith(".jsonl"):
            continue
        for line in (Path(root) / fn).read_text(encoding="utf-8", errors="ignore").splitlines():
            if "demo-shell.component.html" not in line or '"contents"' not in line:
                continue
            try:
                obj = json.loads(line)
            except json.JSONDecodeError:
                continue
            msg = obj.get("message", {})
            for part in msg.get("content", []):
                if part.get("type") != "tool_use":
                    continue
                inp = part.get("input", {})
                if inp.get("path", "").endswith("demo-shell.component.html") and "contents" in inp:
                    c = inp["contents"]
                    if "<mat-sidenav-container" in c:
                        score = sum(1 for m in markers if m in c)
                        write_candidates.append((len(c), score, os.path.basename(root), c))

write_candidates.sort(key=lambda x: (-x[1], -x[0]))
print(f"write candidates: {len(write_candidates)}")
for ln, score, sess, _ in write_candidates[:10]:
    print(" write", ln, score, sess)
if write_candidates:
    best = write_candidates[0]
    out = OUT_DIR / "best_write.html"
    out.write_text(best[3], encoding="utf-8")
    print(f"saved write-best {out} len={best[0]} score={best[1]}")
