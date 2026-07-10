# 04 — Staging final

## Clasificacion de los 638 archivos staged

| Categoria               | Cantidad |
|-------------------------|----------|
| Codigo productivo Java  | 131      |
| Tests Java              | 85+      |
| Spec-as-source activa   | 11       |
| Handoffs                | 0        |
| Configuracion git/pom   | 3        |
| docs/faltas             | 2        |
| application.yml         | 1        |

Total: 638 archivos (incluye microcierre final)

## Auditoría

- Sin artefactos no deseados
- Sin ZIPs en staging
- Sin rutas >= 240 caracteres
- Sin conflictos sin resolver
- git diff --cached --check: limpio
- git grep --cached mojibake en src + spec + docs/faltas: 0 resultados

## Archivos NO staged (intencionalmente)

- ~~`.vscode/settings.json`~~: STAGED en microcierre final (java.jdt.ls.vmargs para JVM del Language Server)
- `SANEAMIENTO-CODIFICACION-2026-07-10.md`: trasladado al handoff canonico
- `backend/api-faltas-core/docs/spec-as-source/handoff/8F-11M-B1-INMEMORY-TESTS-SPEC-ECONOMIA/`:
  handoff ya existente, ahora visible; requiere decision separada de staging