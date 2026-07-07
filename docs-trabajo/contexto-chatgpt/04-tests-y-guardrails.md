# 04 — Tests y guardrails activos

**Generado:** 2026-07-03

---

## Build actual (al último commit main — 8F-4C)

- **Tests run:** 1419
- **Failures:** 0
- **Errors:** 0
- **Skipped:** 0
- **Resultado:** BUILD SUCCESS

(8F-5 está en working tree sin commitear; según spec tendría 1437 tests al buildear.)

---

## Suites funcionales (Slice 8F-4C)

| Suite | Descripción |
|-------|-------------|
| `ActaFlujoCapturaFuncionalTest` | ACT-001, ACT-002 — labrado y captura |
| `ActaFlujoDocumentalFuncionalTest` | ACT-003, ACT-023, ACT-024, ACT-025, ACT-027 — documentos y redacción |
| `ActaFlujoNotificacionFuncionalTest` | ACT-004, ACT-005, ACT-013, ACT-026 — notificaciones |
| `ActaFlujoPagoVoluntarioFuncionalTest` | ACT-007, ACT-008, ACT-009 — pago voluntario |
| `ActaFlujoFalloFuncionalTest` | ACT-010, ACT-011, ACT-012, ACT-015, ACT-028 — fallos |
| `ActaFlujoApelacionFuncionalTest` | ACT-014 — apelación |
| `ActaFlujoPagoCondenaFuncionalTest` | ACT-016, ACT-017, ACT-030, ACT-031 — pago condena |
| `ActaFlujoGestionExternaFuncionalTest` | ACT-018, ACT-019 — gestión externa |
| `ActaFlujoParalizacionFuncionalTest` | ACT-020 — paralización |
| `ActaFlujoBloqueanteFuncionalTest` | ACT-021, ACT-022 — bloqueantes materiales |
| `ActaFlujoReingresoFuncionalTest` | ACT-029 — reingreso desde gestión externa |
| `DatasetFuncionalFlujosCoberturaTest` | Cobertura total de los 31 casos |

---

## Suites de dataset e infraestructura

| Suite | Tests | Descripción |
|-------|-------|-------------|
| `DatasetFuncionalDominioCatalogTest` | 17 | Estructura e integridad del catálogo |
| `DatasetFuncionalCoberturaTest` | 7 | Cálculo de cobertura funcional |
| `ActasMockFuncionalesFactoryTest` | 9 | Construcción determinística de actas mock |
| `DatasetFuncionalDocumentosEsperadosTest` | 9 | Validación de documentos esperados |
| `DatasetFuncionalDominioCatalogTest` | — | Catálogo completo de dominio |

---

## Tests del motor documental

| Suite | Descripción |
|-------|-------------|
| `DocumentoGraphDemoServiceTest` | Graph demo documental (8 flujos completos) |
| `DocumentoGeneracionMockGraphDemoTest` | Generación mock + PDF por acta |
| `DocumentoRedaccionGraphDemoTest` | Redacción en graph demo |
| `DocumentoGeneracionMockServiceTest` | Servicio de generación mock |
| `DocumentoRedaccionServiceTest` | Servicio de redacción |
| `DocumentoCombinacionServiceTest` | Motor de combinación plantilla + contexto |
| `DocumentoVariableContextBuilderTest` | Context builder por namespace |
| `DocumentoPdfMockRendererTest` | Renderer PDF mock |
| `DocumentoPlantillasMockTest` | Plantillas mock sembradas |
| `DocumentoGraphDemoServiceTest` | Endpoint GET /demo/documentos/graph |
| `EnumGuardrailTest` | Guardrails sobre enums de dominio |

---

## Tests de reset (Slice 8F-5, en working tree sin commitear)

| Suite | Tests | Descripción |
|-------|-------|-------------|
| `DevInMemoryResetServiceTest` | 13 | Servicio de reset in-memory |
| `DevResetControllerIT` | 14 | Integration test POST /demo/dev/reset habilitado |
| `DevResetDisabledIT` | 1 | IT: 404 cuando reset está deshabilitado |
| `DevResetGuardrailTest` | 5 | Guardrails estáticos del reset |

---

## Guardrails activos

### JDBC / persistencia — PROHIBIDO

- No repositorios JDBC de dominio todavía
- No MariaDB real en tests normales (requiere `FALTAS_DB_URL` definida externamente)
- No scripts SQL de tablas ni seeds
- No Flyway / Liquibase
- No `@Entity`, `@Table`, `@Id`
- No `JpaRepository`, `CrudRepository`
- No `EntityManager`
- No `JdbcClient` nuevo para dominio
- No `DROP`, `CREATE`, `ALTER`, `TRUNCATE`
- No `INSERT`, `UPDATE`, `DELETE` en SQL de dominio

### Tecnología — PROHIBIDO

- No JPA / Hibernate
- No Angular / frontend en api-faltas-core
- No PDF real / storage real
- No escritura de archivos físicos como storage documental
- No SpEL / eval / ScriptEngine / reflection
- No workers, colas ni integraciones reales

### Dominio — PROHIBIDO

- No inventar eventos que no existen en el enum `TipoEventoActa`
- No inventar bloques que no existen en `BloqueActual`
- No inventar estados centrales fuera de spec
- No usar strings libres para dominio (siempre enums)
- No usar `PAGCON` — prohibido, evento obsoleto
- No usar `APELAC` — usar `APEPRE`
- No usar `ACTCER` — el cierre usa `CIERRA`
- No usar `DRVEXT` — prohibido
- No reintroducir `D3_DOCUMENTAL` — no existe como bloque

### Estructura del repo — PROHIBIDO

- No crear `docs/spec-as-source` en raíz del repo
- Test-Path `docs\spec-as-source` desde raíz debe dar `False`

---

## Verificación de guardrails de estructura

Desde la raíz del repo (`S:\Source\Repos\Bod-Faltas`):

```powershell
Test-Path docs\spec-as-source
# Resultado esperado: False
```

Resultado confirmado: `False`

La spec viva vive en:
`backend/api-faltas-core/docs/spec-as-source/`
