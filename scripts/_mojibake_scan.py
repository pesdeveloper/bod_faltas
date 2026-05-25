from pathlib import Path

text = Path(
    r"S:/Source/Repos/Bod-Faltas/apps/web-direccion-faltas/angular/src/app/features/demo/demo-shell.component.html"
).read_text(encoding="utf-8")
for i, line in enumerate(text.splitlines(), 1):
    if any(x in line for x in ["??", "\ufffd", "Ã", "Â", "anolisis", "seggn", "retorn??"]):
        with open(r"S:/Source/Repos/Bod-Faltas/scripts/_mojibake_lines.txt", "a", encoding="utf-8") as f:
            f.write(f"{i}: {line}\n")

print("written")
