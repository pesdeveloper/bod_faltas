# 01 - Estado del Build - 8F-11L

## Resultado final

- **Fecha:** 2026-07-07
- **Tests run:** 2363
- **Failures:** 0
- **Errors:** 0
- **Skipped:** 0
- **BUILD:** SUCCESS

## Baseline de entrada

- Tests run: 2323 (8F-11K)
- Failures: 0
- Errors: 0

## Delta

- Tests nuevos: +40
  - ActaTransitionMatrixTest: 30 tests
  - ActaEventoInvariantesTest: 12 tests (el ajuste final quedo en 10)
  - ActaConcurrenciaTest: 8 tests

## Notas tecnicas

- BOM (byte order mark) detectado en 27 archivos Java; corregido eliminando bytes EF BB BF
- Patron de escritura: [System.IO.File]::WriteAllBytes(path, UTF8-no-BOM.GetBytes(content))
- Las migraciones de egistrarEvento requirieron diferenciacion cuidadosa de call sites:
  - Calls a egistrarEvento que pasan a Long
  - Calls a ComandoResultado.de(...) que mantienen String
  - Calls a constructores de excepciones que mantienen String