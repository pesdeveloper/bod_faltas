import json
import re
from pathlib import Path

TRANSCRIPTS = Path(r"C:\Users\pablito\.cursor\projects\s-Source-Repos-Bod-Faltas\agent-transcripts")
OUT = Path(r"S:\Source\Repos\Bod-Faltas\_extracted_sections.txt")

queries = [
    ("doc_list_full", r'<section class="detail-card" aria-labelledby="documentos-titulo">[\s\S]{500,8000}?</section>'),
    ("eventos_full", r'<section class="detail-card" aria-labelledby="eventos-titulo">[\s\S]{500,8000}?</section>'),
    ("piezas_full", r'actaMuestraSeccionPiezasRedaccion[\s\S]{200,6000}?</section>'),
    ("bloqueantes_full", r'<section class="detail-card" aria-labelledby="bloqueantes-titulo">[\s\S]{500,8000}?</section>'),
    ("cierre_full", r'<section class="detail-card" aria-labelledby="cierre-acta-titulo">[\s\S]{500,4000}?</section>'),
    ("notif_full", r'<section class="detail-card" aria-labelledby="notificacion-acta-titulo">[\s\S]{500,5000}?</section>'),
    ("pago_full", r'<section class="detail-card" aria-labelledby="pago-voluntario-titulo">[\s\S]{500,6000}?</section>'),
    ("list_panel_new", r'list_panel_new = """[\s\S]{200,2500}?"""'),
    ("detail_header", r'@else if \(detalle\(\); as d\) \{[\s\S]{200,3500}?<section class="detail-card"'),
    ("acta_row", r'class="acta-row"[\s\S]{200,2000}?badges'),
    ("empty_detail", r'state-box--empty-detail[\s\S]{100,800}'),
]

best = {}

for root in TRANSCRIPTS.rglob("*.jsonl"):
    raw = root.read_text(encoding="utf-8", errors="ignore")
    raw = raw.replace("\\n", "\n").replace('\\"', '"')
    sess = root.parent.name
    for name, pat in queries:
        for m in re.finditer(pat, raw):
            chunk = m.group(0)
            if name not in best or len(chunk) > best[name][0]:
                best[name] = (len(chunk), sess, chunk)

lines = []
for name, _ in queries:
    if name in best:
        ln, sess, chunk = best[name]
        lines.append(f"\n===== {name} len={ln} from {sess} =====\n{chunk}\n")
    else:
        lines.append(f"\n===== {name} NOT FOUND =====\n")

OUT.write_text("\n".join(lines), encoding="utf-8")
print(f"wrote {OUT}")
for name, (ln, sess, _) in sorted(best.items(), key=lambda x: -x[1][0]):
    print(name, ln, sess)
