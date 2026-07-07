# 01 - Estado del Build al cierre de 8F-11H

## Build final

```
[INFO] Tests run: 2107, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  18.642 s
[INFO] Finished at: 2026-07-06T21:17:05-03:00
```

## Baseline de entrada (8F-11G)

- Tests: 2048
- Failures: 0
- Errors: 0
- ALINEADO: 48
- FALTA_EN_INMEMORY: 7

## Delta 8F-11H

- Tests nuevos: +59
- ALINEADO: +4 (fal_acta_obligacion_pago, fal_acta_forma_pago, fal_acta_plan_pago_ref, fal_acta_pago_movimiento)
- SEMANTICA_INCOMPATIBLE: -1 (fal_acta_obligacion_pago reconciliada)
- FALTA_EN_INMEMORY: -3 (7 -> 4)

## Estado de build al cierre

- Tests: 2107
- Failures: 0
- Errors: 0
- ALINEADO: 52
- FALTA_EN_INMEMORY: 4
- SEMANTICA_INCOMPATIBLE: 0

## Comando de verificacion

```bash
cd backend/api-faltas-core
mvn test
```

## Notas tecnicas

- Spring context arranca correctamente con `PagoObligacionMockSeeder` (@PostConstruct)
- `SnapshotRecalculador` usa `@Autowired(required=false)` para los 4 repos nuevos; tests viejos no requieren cambio
- Adapters `PagoVoluntarioAdapterRepository` y `PagoCondenaAdapterRepository` son `@Repository @Primary`; proveen beans para los servicios viejos
- `InMemoryPagoVoluntarioRepository` y `InMemoryPagoCondenaRepository` sin `@Repository`; usados directamente en `CasoUsoFuncionalRunner` como instancias locales