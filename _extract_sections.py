import json
import os
import re
from pathlib import Path

BASE = Path(r"C:\Users\pablito\.cursor\projects\s-Source-Repos-Bod-Faltas\agent-transcripts")
OUT = Path(r"S:\Source\Repos\Bod-Faltas\_extracted_sections.txt")

sections = {}
patterns = {
    "sidenav_collapsible": r'<mat-sidenav[^>]*bandejasColapsadas[\s\S]{0,4000}?</mat-sidenav>',
    "list_tools": r'list-panel__tools[\s\S]{0,2000}?list-panel__feedback|listadoEstado',
    "cierre": r'cierre-acta-titulo[\s\S]{0,3500}?cierre-actions',
    "archivo": r'archivo-acta-titulo[\s\S]{0,3500}?archivo-actions|archivo-acta-titulo[\s\S]{0,3500}?</section>',
    "gestion": r'gestion-externa-titulo[\s\S]{0,4500}?gestion-externa-actions|gestion-externa-titulo[\s\S]{0,4500}?</section>',
    "notificacion": r'notificacion-acta-titulo[\s\S]{0,3500}?notificacion-actions|notificacion-acta-titulo[\s\S]{0,3500}?</section>',
    "pago": r'pago-voluntario-titulo[\s\S]{0,5000}?pago-actions',
    "documentos_api": r'documentosEstado\(\)[\s\S]{0,5000}?doc-list',
    "eventos": r'eventosEstado\(\)[\s\S]{0,5000}?event-timeline',
    "piezas_redaccion": r'piezas-redaccion|generarNulidad|actaMuestraSeccionPiezasRedaccion[\s\S]{0,5000}?detail-piezas-resumen',
    "bloqueantes_buttons": r'bloqueantes-titulo[\s\S]{0,5000}?cumplirMaterialmente',
    "header_detail": r'@else if \(detalle\(\); as d\)[\s\S]{0,2500}?detail-header',
}

for root, _, files in os.walk(BASE):
    for fn in files:
        if not fn.endswith(".jsonl"):
            continue
        raw = (Path(root) / fn).read_text(encoding="utf-8", errors="ignore")
        raw = raw.replace("\\n", "\n").replace('\\"', '"')
        sess = os.path.basename(root)
        for name, pat in patterns.items():
            for m in re.finditer(pat, raw):
                chunk = m.group(0)
                if len(chunk) < 80:
                    continue
                key = (name, len(chunk), sess)
                if name not in sections or len(chunk) > sections[name][0]:
                    sections[name] = (len(chunk), sess, chunk[:8000])

lines = []
for name in patterns:
    if name in sections:
        ln, sess, chunk = sections[name]
        lines.append(f"\n===== {name} len={ln} from {sess} =====\n{chunk}\n")
    else:
        lines.append(f"\n===== {name} NOT FOUND =====\n")

OUT.write_text("\n".join(lines), encoding="utf-8")
print(f"wrote {OUT}")
for name in patterns:
    if name in sections:
        print(name, sections[name][0], sections[name][1])
