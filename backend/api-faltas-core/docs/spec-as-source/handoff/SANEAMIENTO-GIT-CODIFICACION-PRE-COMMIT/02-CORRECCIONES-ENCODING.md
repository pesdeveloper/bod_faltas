# 02 — Correcciones de encoding

## Archivos Java reparados (saneamiento previo)

- GestionExternaService.java: limpio, sin BOM, sin mojibake
- ActaSnapshotRepository, SnapshotRecalculador: no revisados individualmente en esta sesion
- BloqueantesMaterialesTest, GestionExternaTest, SatelitesCatalogosTest, TalonarioTest, FlujoCoreIT: limpios

## Archivos Markdown verificados (spec-as-source activa)

| Archivo                                        | BOM | Mojibake |
|------------------------------------------------|-----|----------|
| 03-comandos-precondiciones-efectos.md          | No  | No       |
| 05-api-core-endpoints.md                       | No  | No       |
| 06-tests-core.md                               | No  | No       |
| 104-plantillas-redaccion-combinacion.md        | No  | No       |
| 108-frontend-ready-demo.md                     | No  | No       |
| 110-matriz-maestra-paridad-mariadb-inmemory.md | No  | No       |

## Hallazgo: mojibake residual

`99-pendientes-siguientes-slices.md`: tiene secuencias mojibake en bullet points
patrones de doble o triple codificación donde debían existir acentos en español
- No fue corregido (requiere fuente confiable de referencia)
- Queda como pendiente: micro-slice documental

`DELTA_MODELO_MARIADB_DESDE_IMPLEMENTACION_IN_MEMORY.md`: limpio tras correccion de CRLF y trailing whitespace.

## Correcciones CRLF en esta sesion

- `DocumentoVariableContextBuilder.java`: CRLF -> LF (secuencias `\r\r\n` detectadas)
- `FirmezaCondenaService.java`: CRLF -> LF (243 ocurrencias)
- `DELTA_MODELO_MARIADB...md`: CRLF -> LF (1881 ocurrencias) + 24 trailing spaces eliminados

## Documento 110

Todos los terminos requeridos presentes:
R-08, R-11, FaltasClock, ActaConcurrenciaTest, 50/50, 2461 tests, 2478 tests,
GlobalFaltasControllerAdvice, 8F-12A / Slice 9