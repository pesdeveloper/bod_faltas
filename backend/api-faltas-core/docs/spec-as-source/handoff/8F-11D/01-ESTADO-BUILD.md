# 01 — Estado del Build

## Datos del build

| Campo | Valor |
|---|---|
| Comando | `mvn test` |
| Directorio | `backend/api-faltas-core` |
| Fecha/hora | 2026-07-05 18:26:23 (UTC-3) |
| Resultado | **BUILD SUCCESS** |
| Tests totales | **1755** |
| Failures | **0** |
| Errors | **0** |
| Skipped | **0** |
| Duración | **8.835 s** |
| Java | JDK (version según pom.xml del proyecto) |
| Maven | configurado en el entorno |

---

## Resumen final del log

`
[INFO] Tests run: 1755, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  8.835 s
[INFO] Finished at: 2026-07-05T18:26:23-03:00
[INFO] ------------------------------------------------------------------------
`

---

## Log completo

El log completo está en: `files/mvn-test.log`

---

## Notas

- No se usó `-DskipTests`.
- El build se ejecutó desde el módulo `backend/api-faltas-core` con `mvn test`.
- 0 failures y 0 errors confirman que todos los tests nuevos de 8F-11D pasan sin regresiones.
- Baseline previo al slice: 1660 tests (post 8F-11C).
- Tests nuevos incorporados en 8F-11D: ~95 tests.
