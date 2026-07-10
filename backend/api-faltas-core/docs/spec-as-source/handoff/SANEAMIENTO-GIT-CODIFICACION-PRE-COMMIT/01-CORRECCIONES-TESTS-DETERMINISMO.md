# 01 — Correcciones de tests: determinismo temporal

## Problema

Los tests usaban `LocalDate.now()` para calcular fechas de vigencia (`vigDesde`) de:
- Plantillas de documentos
- Politicas de numeracion
- Talonarios y ambitos
- Dependencias e inspectores
- Normativas y rubros

El clock de test fijo es `FaltasClockTestSupport.FIXED` = `2026-07-09T18:00:00Z`.
Al ejecutar el `2026-07-10`, `LocalDate.now()` devuelve `2026-07-10 > FIXED = 2026-07-09`,
rompiendo la validacion de vigencia.

## Baseline previo al fix

Tests run: 2478, Failures: 3, Errors: 74

## Archivos corregidos

Reemplazo uniforme: `LocalDate.now()` -> `FaltasClockTestSupport.FIXED.now().toLocalDate()`

| Archivo                                    | Ocurrencias reemplazadas |
|--------------------------------------------|--------------------------|
| DocumentoFirmaReqTest.java                 | 6                        |
| TalonarioTest.java                         | 48                       |
| DependenciaTest.java                       | 11                       |
| DocumentoGeneracionDesdePlantillaTest.java | 8                        |
| TarifarioValorizacionTest.java             | 12                       |
| ActaConcurrenciaTest.java                  | 3                        |
| ActaConcurrenciaOccFocalizadoTest.java     | 3                        |
| (34 archivos mas con 1-20 ocurrencias c/u) | 156 total batch          |
| FlujoCoreIT.java                           | 1 (+ import agregado)    |

## Correcciones adicionales

- `DocumentoVariableContextBuilder.java`: referencia `java.time.LocalDate.now()` (fully qualified)
  convertida correctamente en el test.
- `FalloInvariantesTest.java`: mismo patron, corregido.

## Resultado post-fix

Tests run: 2478, Failures: 0, Errors: 0, BUILD SUCCESS

## Auditoria ValorizacionInvariantesR1Test

9 sustituciones ya realizadas en slice previo, verificadas:
- Todas en fixtures/tests (no en produccion)
- Solo parametros `vigDesde` de normativa, rubro y fecha de acta
- Sin alteracion de regla de valorizacion
- Tests pasan con 0 failures