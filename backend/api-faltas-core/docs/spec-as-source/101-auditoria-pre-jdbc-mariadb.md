# Auditoria transversal final de spec-as-source y readiness para DDL MariaDB

> **Estado documental:** SUPPORTING_CURRENT
> **Autoridad DDL:** SUPPORTING
> Este documento es un informe/gate de conformidad, no una fuente de reglas de
> dominio. Es el cierre formal de la etapa spec-as-source y la puerta de
> entrada al bloque de diseño y generación del DDL versionado de MariaDB. No
> redefine reglas de dominio: certifica que la spec normativa (`00-governance/`,
> `10-domain/`, `20-application/`, documentos top-level NORMATIVE) es consistente,
> trazable contra el código vigente y suficiente para iniciar el DDL. Ante
> contradicción sobre una regla de dominio, prevalecen los documentos temáticos
> y de contrato de comandos (ver README, sección 3).

**Fecha de esta auditoría transversal final:** 2026-07-13
**Alcance:** `backend/api-faltas-core/docs/spec-as-source/**` (fuera de `handoff/**`) y su trazabilidad contra el código vigente del módulo `backend/api-faltas-core`.
**Naturaleza:** documental, de trazabilidad, de conformidad código/spec y de preparación para DDL. No es un slice funcional, no reabre los siete comandos canónicos (`CMD-FALLO-001..007`), no migra JDBC ni genera SQL.

## Estado: READY_FOR_DDL

La auditoría transversal final aprueba todos los ítems del checklist (sección
"Checklist final" más abajo). No quedan bloqueadores internos. La etapa
spec-as-source queda formalmente cerrada; la siguiente etapa autorizada es el
diseño y la generación del DDL versionado de MariaDB (ver "Entrada del
siguiente bloque").

`READY_FOR_DDL` significa que la spec-as-source está lista para **iniciar**
el diseño físico y resolver cada `DECISION_DDL-*` pendiente listada en
`110-matriz-maestra-paridad-mariadb-inmemory.md`. NO significa que el DDL
final ya esté completamente decidido: las decisiones físicas abiertas
(identidad de algunos agregados, OCC multi-nodo, representación de enums
`NO_EXPLICIT_CODE`, unicidad física, etc.) se resuelven explícitamente
durante ese bloque, no antes.

## Base normativa

Esta auditoría se apoya en, y no reemplaza a, los siguientes documentos:

- [`README.md`](README.md) — autoridad, precedencia y orden de lectura.
- [`00-governance/spec-document-registry.md`](00-governance/spec-document-registry.md) — registro documental canónico completo.
- [`00-governance/glossary.md`](00-governance/glossary.md) — vocabulario canónico del dominio.
- [`00-governance/command-contract-standard.md`](00-governance/command-contract-standard.md) — estándar normativo de contratos de comandos.
- [`10-domain/lifecycle-states.md`](10-domain/lifecycle-states.md) — dimensiones de estado y lifecycle.
- [`10-domain/firma-notificacion-fallo.md`](10-domain/firma-notificacion-fallo.md) — circuito de firma y cola notificatoria del fallo.
- [`10-domain/calendario-plazos-administrativos.md`](10-domain/calendario-plazos-administrativos.md) — calendario y plazos administrativos.
- [`20-application/fallo-command-contracts.md`](20-application/fallo-command-contracts.md) — contratos definitivos de CMD-FALLO-001..007.
- [`02-estados-bloques-eventos.md`](02-estados-bloques-eventos.md) — catálogo de estados, bloques y eventos.
- [`04-snapshot-bandejas-acciones.md`](04-snapshot-bandejas-acciones.md) — snapshot, bandejas y acciones.
- [`05-api-core-endpoints.md`](05-api-core-endpoints.md) — contratos HTTP.
- [`109-delta-modelo-mariadb-inmemory.md`](109-delta-modelo-mariadb-inmemory.md) — delta InMemory/MariaDB vigente.
- [`110-matriz-maestra-paridad-mariadb-inmemory.md`](110-matriz-maestra-paridad-mariadb-inmemory.md) — matriz de entrada a DDL/JDBC.

## Checklist final

| # | Ítem | Resultado | Evidencia | Archivo propietario |
|---|------|-----------|-----------|----------------------|
| 1 | Autoridad documental | PASS | Registro completo con clasificación y autoridad DDL para cada `.md` fuera de `handoff/**`; cero paths duplicados o faltantes | `00-governance/spec-document-registry.md` |
| 2 | Estados/enums | PASS | Auditoría cruzada de `SituacionAdministrativaActa`, `BloqueActual`, `EstadoProcesalActa`, `ResultadoFinalActa`, `TipoFalloActa`, `ResultadoFalloActa`, `EstadoFalloActa`, `EstadoApelacionActa`, `ResultadoResolucionApelacion`, `EstadoPagoCondena`, `EstadoPagoVoluntario`, `ModoReingresoGestionExterna`, `ResultadoGestionExterna` contra el código vigente; sin valores inventados ni residuos ocultos | `02-estados-bloques-eventos.md`, `10-domain/lifecycle-states.md` |
| 3 | Eventos | PASS | Catálogo completo de los 65 valores de `TipoEventoActa` verificado uno por uno contra `TipoEventoActa.java`; matriz de comando completa para los eventos del circuito de fallo | `02-estados-bloques-eventos.md` (sección "Catálogo completo de TipoEventoActa"), `20-application/fallo-command-contracts.md` |
| 4 | Comandos | PASS | CMD-FALLO-001..007 con exactamente una sección de nivel 2 cada uno; ningún comando adicional de esa familia más allá del séptimo; contratos completos (precondiciones, orden de efectos, eventos, errores, retry, concurrencia) | `20-application/fallo-command-contracts.md` |
| 5 | Actores/seguridad | PASS | Política de seguridad verificada por comando (no una regla común a los siete); ver "Política de seguridad por comando" más abajo | `00-governance/command-contract-standard.md`, `20-application/fallo-command-contracts.md`, `SecurityConfig`, `JwtActorFilter` |
| 6 | Reloj | PASS | Instante canónico (`FaltasClock.now()` capturado una sola vez) documentado por comando; sin `LocalDateTime.now()` directo documentado como vigente | `20-application/fallo-command-contracts.md` |
| 7 | Snapshot/bandejas | PASS | Matriz de condición fuente → bandeja/acción verificada contra `SnapshotRecalculador`; snapshot documentado como proyección derivada, nunca fuente de verdad | `04-snapshot-bandejas-acciones.md` |
| 8 | Errores HTTP | PASS | Matrices de error por comando con excepción exacta, código HTTP y reintentabilidad; distinción 400/401/404/422 verificada contra `GlobalFaltasControllerAdvice` | `20-application/fallo-command-contracts.md`, `05-api-core-endpoints.md` |
| 9 | Idempotencia/concurrencia | PASS | Semántica de reintento (idempotente por referencia externa, por identidad natural, o no idempotente) documentada por comando; fronteras reales (`firmezaMonitor`, `pagoCondenaMonitor`, `ResultadoPositivoInMemoryMonitor`) distinguidas de garantías multi-instancia/MariaDB | `20-application/fallo-command-contracts.md` |
| 10 | Links | PASS | Todos los links relativos Markdown fuera de `handoff/**` resuelven a un archivo existente | Verificación transversal (esta auditoría) |
| 11 | Términos obsoletos | PASS | Cero ocurrencias semánticas vigentes de `PAGCON`, `ACTCER`, `APELAC`, `DRVEXT`, `D3_DOCUMENTAL`, `REINGRESAR_A_ANALISIS`, `REINGRESAR_A_PAGO_CONDENA` en documentos NORMATIVE/SUPPORTING_CURRENT/PRE_DDL_PLAN; `FALLO_CONDENATORIO_PAGADO` (código 5) confirmado como valor compilado vigente de `ResultadoFinalActa`, documentado como LEGACY_RESERVED (sin productor canónico vigente; ver `02-estados-bloques-eventos.md` y `110`, `DECISION_DDL-RF-005`) | `SpecAsSourceGuardrailTest`, `EnumGuardrailTest` |
| 12 | Roadmap | PASS | `99-pendientes-siguientes-slices.md` contiene únicamente DDL/JDBC, integraciones externas e ítems no bloqueantes; sin ítems ya cerrados ni identificadores internos de brecha | `99-pendientes-siguientes-slices.md` |
| 13 | Tests | PASS | Suite completa verde (ver "Evidencia de tests"); guardrail documental fortalecido | `SpecAsSourceGuardrailTest` |
| 14 | Pago de condena coherente | PASS | `02`, `03`, `05` y `20-application/fallo-command-contracts.md` afirman la misma regla: `PCOCNF` se registra siempre que la confirmación supera sus precondiciones; los bloqueantes materiales activos solo impiden `CIERRA`, nunca rechazan la confirmación; sin duplicación de `InformarPagoCondenaCommand` fuera de `20` | `02-estados-bloques-eventos.md`, `03-comandos-precondiciones-efectos.md`, `05-api-core-endpoints.md`, `20-application/fallo-command-contracts.md`, guardrail G-15 |
| 15 | Documentos vigentes sin cronología histórica | PASS | `99`, `102`, `103` (todos vigentes) y `03` sin identificadores de bloques/fases históricas, conteos de build ni `Tests run:` de diario, y sin headings de cierre/fix/numeración histórica; `103` reclasificado `SUPPORTING_CURRENT` | `99-pendientes-siguientes-slices.md`, `102-slice-9-estrategia-jdbc-mariadb.md`, `103-slice-9-1-infraestructura-jdbc.md`, `03-comandos-precondiciones-efectos.md`, guardrail G-17 |
| 16 | Estrategia de enums clasificada | PASS | Enums persistibles clasificados en tres categorías (`EXPLICIT_NUMERIC_CODE`, `EXPLICIT_STRING_CODE`, `NO_EXPLICIT_CODE`) verificadas por reflexión contra el código Java vigente; sin afirmación universal de "todo enum tiene código numérico" | `109-delta-modelo-mariadb-inmemory.md`, `110-matriz-maestra-paridad-mariadb-inmemory.md`, `102-slice-9-estrategia-jdbc-mariadb.md`, guardrail G-16 |
| 17 | `DECISION_DDL-ENUM-01` registrada | PASS | Decisión física explícita para la representación de `EstadoFalloActa`, `EstadoApelacionActa`, `EstadoPagoCondena` (enums `NO_EXPLICIT_CODE`), con alternativas y criterio de cierre; sin códigos inventados | `110-matriz-maestra-paridad-mariadb-inmemory.md` (sección `DECISION_DDL`), guardrail G-16 |

## Política de seguridad por comando

No existe una regla común de "JWT Bearer para los siete comandos". La
política real es por comando (fuente autoritativa:
[`20-application/fallo-command-contracts.md`](20-application/fallo-command-contracts.md),
`SecurityConfig`, `JwtActorFilter`):

| Comando | Canal | Política de identidad |
|---|---|---|
| CMD-FALLO-001 | `POST /api/faltas/documentos/{id}/firmar-real` | JWT Bearer obligatorio; actor = `sub` del JWT |
| CMD-FALLO-002 | `POST /api/faltas/actas/{id}/notificaciones/enviar` | JWT Bearer obligatorio; actor = `sub` del JWT |
| CMD-FALLO-003 | `LoteCorreoService.generarLoteDesdePendientes` (proceso técnico; sin endpoint HTTP humano) | Identidad técnica autenticada del proceso invocador; no aplica JWT de usuario final |
| CMD-FALLO-004 | `POST /api/faltas/notificaciones/{id}/positiva` | JWT Bearer obligatorio; actor = `sub` del JWT |
| CMD-FALLO-005 | `POST /api/faltas/actas/{id}/firmeza/vencer-plazo-apelacion` | JWT Bearer obligatorio; actor = `sub` del JWT |
| CMD-FALLO-006 | `POST /api/faltas/actas/{id}/firmeza/por-apelacion-rechazada` | JWT Bearer obligatorio; actor = `sub` del JWT |
| CMD-FALLO-007 | `POST /api/faltas/actas/{id}/pago-condena/informar` | JWT Bearer obligatorio; actor = `sub` del JWT |

CMD-FALLO-003 es la única excepción: es un proceso técnico sin endpoint HTTP
para usuario final, por lo que no aplica JWT de usuario final ni matcher en
`SecurityConfig` orientado a humano. Los seis comandos restantes sí exigen JWT
Bearer obligatorio con actor extraído exclusivamente de
`ActorContextHolder.get().sub()`.

## Bloqueadores internos

Ninguno.

## Decisiones físicas pendientes (no bloquean el diseño inicial)

Las decisiones de diseño físico/DDL pendientes están listadas y trazadas en
[`110-matriz-maestra-paridad-mariadb-inmemory.md`](110-matriz-maestra-paridad-mariadb-inmemory.md)
(sección `DECISION_DDL`). Ninguna reabre dominio, contratos de comando,
eventos ni estados.

## Dependencias externas no bloqueantes para DDL

Estas dependencias no bloquean el diseño ni la generación del DDL de MariaDB;
se documentan conceptualmente en `99-pendientes-siguientes-slices.md`:

- Integración real con la aplicación de Firmas.
- Integración real con el sistema de Notificaciones (envío postal/electrónico real).
- Integración real con Ingresos/Tesorería.
- Storage y comprobantes reales (hoy simulados con esquema `mock://`).
- Sincronización real de calendario administrativo con un proveedor externo.
- Frontend productivo, cuando corresponda.

## Entrada del siguiente bloque

- Generar el paquete DDL versionado de MariaDB a partir de `109-delta-modelo-mariadb-inmemory.md` y `110-matriz-maestra-paridad-mariadb-inmemory.md`.
- No alterar contratos funcionales, estados, eventos, transiciones ni bandejas vigentes.
- InMemory continúa como oráculo de paridad hasta cerrar la paridad de infraestructura JDBC.

## Evidencia de tests

Baseline de entrada a esta auditoría (R1): 2878 tests.

Suite de salida de esta auditoría transversal final R2 (tras fortalecer
`SpecAsSourceGuardrailTest` de 43 a 60 tests, +17 respecto del baseline R1):

```text
Tests run: 2895
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

Guardrail documental fortalecido: `SpecAsSourceGuardrailTest` (60 tests; ver
detalle de guardrails G-1..G-18 en el Javadoc de la clase).

---

## Anexo histórico

La historia permanece en Git. El detalle de auditorías internas anteriores
sobre documentos y modelo pre-JDBC, y de decisiones tempranas de estrategia
JDBC, no se mantiene en este documento `SUPPORTING_CURRENT`. El estado
vigente de readiness, seguridad, bloqueadores y decisiones físicas
pendientes está exclusivamente en las secciones anteriores de este
documento y en `109`/`110`.
