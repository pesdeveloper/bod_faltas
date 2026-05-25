from pathlib import Path

base = Path(r"S:/Source/Repos/Bod-Faltas/apps/web-direccion-faltas/angular/src/app/features/demo")
out = Path(r"S:/Source/Repos/Bod-Faltas/scripts/_mojibake_report.txt")
lines_out = []
for name in ["demo-shell.component.html", "demo-acta-list.component.html", "demo-acta-list.component.ts", "demo-shell.component.ts"]:
    text = (base / name).read_text(encoding="utf-8")
    for i, line in enumerate(text.splitlines(), 1):
        if "\ufffd" in line or "??" in line or "Ã" in line:
            lines_out.append(f"{name}:{i}: {line}\n")
out.write_text("".join(lines_out) if lines_out else "clean\n", encoding="utf-8")
