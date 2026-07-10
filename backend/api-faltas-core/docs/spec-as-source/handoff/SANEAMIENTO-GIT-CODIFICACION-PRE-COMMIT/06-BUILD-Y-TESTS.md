# 05 — Build y tests

## mvn clean test

`
Tests run: 2478, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
`

Ejecutado dos veces:
1. `mvn test` (incremental): verde
2. `mvn clean test` (build limpio): verde

## Clases individuales verificadas

| Clase                         | Resultado |
|-------------------------------|-----------|
| DocumentoFirmaReqTest         | 30/0/0    |
| TalonarioTest                 | 107/0/0   |
| DependenciaTest               | 15/0/0    |
| ValorizacionInvariantesR1Test | 30/0/0    |

## Evolucion del fix

Baseline inicial del dia: 2478/3/74
Tras fix DocumentoFirmaReqTest: 2478/3/49 (aprox)
Tras fix TalonarioTest y DependenciaTest: mejora
Tras fix sistemico LocalDate.now(): 2478/0/0