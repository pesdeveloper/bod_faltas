# 07 — Pendientes

## Mojibake residual

`99-pendientes-siguientes-slices.md` tiene mojibake en seccion historica.
No fue corregido por falta de fuente confiable de referencia.
Accion: micro-slice documental separado.

## Handoff 8F-11M no staged

`backend/api-faltas-core/docs/spec-as-source/handoff/8F-11M-B1-INMEMORY-TESTS-SPEC-ECONOMIA/`
era ignorado por .gitignore (regla erronea). Al corregir la regla, quedo visible.
Requiere decision de staging separada.

## Proximo slice

8F-12A / Slice 9 - Incorporacion MariaDB/JDBC.
No iniciar hasta tener commit de frontera pre-MariaDB con build verde.

## SANEAMIENTO-CODIFICACION-2026-07-10.md

Archivo suelto en raiz. Puede eliminarse o moverse a este handoff segun criterio del equipo.

## Resuelto en microcierre final

- `.vscode/settings.json`: java.jdt.ls.vmargs staged. Sin modificaciones unstaged.
- Mojibake en src, spec-as-source y docs/faltas: git grep --cached = 0 resultados.
- Documento 100 eliminado fisicamente (git rm).
- `02-estados-bloques-eventos.md`: 3 encabezados corregidos.
- `ActaDocumentoServiceTest.java`, `FlujoCoreIT.java`: comentarios corregidos.
