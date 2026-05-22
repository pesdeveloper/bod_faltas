import re
from pathlib import Path

raw = Path(r"C:\Users\pablito\.cursor\projects\s-Source-Repos-Bod-Faltas\agent-transcripts\58796824-2773-451e-b55b-59357ddacd08\58796824-2773-451e-b55b-59357ddacd08.jsonl").read_text(encoding="utf-8", errors="ignore")
raw = raw.replace("\\n", "\n").replace('\\"', '"')
for pat in [
    r'<section class="detail-card" aria-labelledby="notificacion-acta-titulo">[\s\S]{200,4000}?</section>',
    r'@if \(pendientesBloqueantes\(d\)\.length[\s\S]{200,4000}?</section>',
    r'documentoEsFirmable[\s\S]{200,2000}',
    r'@for \(accion of accionesPagoDisponiblesDesdeBackend\(\)[\s\S]{200,1500}',
    r'actaMuestraSeccionPiezasRedaccion[\s\S]{200,5000}?</section>',
]:
    m = re.search(pat, raw)
    print("===", pat[:40], "===")
    print(m.group(0)[:3000] if m else "NOT FOUND")
    print()
