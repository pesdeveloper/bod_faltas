import re
from pathlib import Path

BASE = Path(r"C:\Users\pablito\.cursor\projects\s-Source-Repos-Bod-Faltas\agent-transcripts")
patterns = [
    r'<section class="detail-card" aria-labelledby="bloqueantes-titulo">[\s\S]{200,5000}?</section>',
    r'@if \(documentoEsFirmable\(doc\)\)[\s\S]{100,1500}',
    r'@for \(accion of accionesPagoDisponiblesDesdeBackend\(\)\)[\s\S]{100,2000}',
    r'@if \(actaMuestraSeccionPiezasRedaccion\(d\)\)[\s\S]{200,6000}?</section>',
    r'generarMedidaPreventiva[\s\S]{100,800}',
]

for pat in patterns:
    best = (0, "", "")
    for f in BASE.rglob("*.jsonl"):
        raw = f.read_text(encoding="utf-8", errors="ignore").replace("\\n","\n").replace('\\"','"')
        for m in re.finditer(pat, raw):
            if len(m.group(0)) > best[0]:
                best = (len(m.group(0)), f.parent.name, m.group(0))
    print("===", pat[:50], "===")
    if best[0]:
        print(best[1], best[0])
        print(best[2][:3500])
    else:
        print("NOT FOUND")
    print()
