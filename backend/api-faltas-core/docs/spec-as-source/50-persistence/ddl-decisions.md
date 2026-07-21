# Decisiones DDL — registro único de `DECISION_DDL-*`

> **Estado documental:** PRE_DDL_PLAN
> **Autoridad DDL:** SUPPORTING
> Este documento es el registro cerrado de las 24 decisiones físicas
> identificadas con prefijo `DECISION_DDL-`. Ninguna decisión listada
> aquí reabre un contrato funcional, un evento, un estado o una transición: son
> exclusivamente de diseño físico/DDL y no bloquean `READY_FOR_DDL` (ver
> `../00-governance/ready-for-ddl-gate.md`). El inventario de tablas al que
> refieren estas decisiones vive en `mariadb-logical-model.md`; los deltas
> transversales de infraestructura viven en `inmemory-mariadb-deltas.md`.

## Pendientes

> **0 decisiones `DECISION_DDL-*` pendientes.** Todas las decisiones registradas
> han sido resueltas en `SPEC-AS-SOURCE-CLEAN-ROOM-Y-DDL-CLOSURE-001`.

_Ninguna entrada pendiente._

## Cerradas

> **27 decisiones cerradas:** 20 anteriores (RF-005, SNAP-01/02, EVID-01,
> PAGO-03, FORMA-01, PLAN-01, MOV-01..04, ECPR-01, DOC-01, NOTI-01, GEXT-01,
> BLOQ-01, PERS-01, PAGO-01, PAGO-MOV-01, ENUM-01) + 4 transversales nuevas
> (EXEC-01, BASELINE-01, SEED-01, COMMENT-01) + 3 nuevas CORRECCION-10
> (EVID-02, PERS-02, DOCU-02).

### `DECISION_DDL-RF-005` — `ResultadoFinalActa.FALLO_CONDENATORIO_PAGADO` (código 5)

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | El valor permanece en el enum y en el catálogo físico como `LEGACY_RESERVED`. Se admite para lectura/importación compatible. Ningún comando funcional nuevo puede producirlo. El resultado canónico nuevo es `CONDENA_FIRME_PAGADA` (código 4). |
| **Fundamento** | No existe `fal_acta` productiva en el baseline entregado; no se ejecuta migración de datos. Para importación futura: mapear 5 → 4 solo cuando la semántica histórica esté comprobada; preservar 5 como legacy de solo lectura si no puede comprobarse. |
| **Consecuencia Java** | `ResultadoFinalActa.FALLO_CONDENATORIO_PAGADO` permanece con código 5. El `CHECK` de `fal_acta.resultado_final` debe aceptar los códigos 0..9, incluido 5 como reservado. |
| **Consecuencia DDL futura** | `resultado_final SMALLINT NOT NULL`; catálogo 10 valores (0..9); código 5 aceptado pero marcado obsoleto en documentación. |
| **Tests/guardrails** | `SpecAsSourceGuardrailTest.G14ResultadoFinalLegacy`, `G21CierreDDL.resultado_final_acta_codigo5_legacy_reserved`. |

### `DECISION_DDL-SNAP-01` — `fal_acta_snapshot`: OCC y `cod_bandeja`/`accion_pendiente` VARCHAR

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | Agregar `version_row INT NOT NULL DEFAULT 0` a `FalActaSnapshot` e incremento controlado en cada reemplazo. `cod_bandeja VARCHAR(50) NULL`. `accion_pendiente VARCHAR(50) NULL`. `CodigoBandeja` y `AccionPendiente` exponen `codigo()` String estable y `desdeCodigo(...)`. No usar `CHAR(6)` para `accion_pendiente`. |
| **Fundamento** | Snapshot es mutable y regenerable; OCC impide que una actualización atrasada sobreescriba una proyección más nueva. Los códigos String estables garantizan idempotencia de persistencia sin depender de `name()`. |
| **Consecuencia Java** | `FalActaSnapshot.versionRow int`; `InMemoryActaSnapshotRepository.guardar()` auto-incrementa `versionRow`. `CodigoBandeja.codigo()` y `AccionPendiente.codigo()` exponen `String` estable. |
| **Consecuencia DDL futura** | `version_row INT NOT NULL DEFAULT 0`, `cod_bandeja VARCHAR(50) NULL`, `accion_pendiente VARCHAR(50) NULL`. |
| **Tests/guardrails** | `G21CierreDDL.fal_acta_snapshot_tiene_version_row`, `G21CierreDDL.accion_pendiente_codigos_unicos_y_longitud`, `G21CierreDDL.codigo_bandeja_codigos_unicos_y_longitud`. |

### `DECISION_DDL-SNAP-02` — `fal_acta_snapshot`: exclusión de campos históricos de economía de pagos

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | El snapshot no transporta economía de pagos. Las lecturas económicas salen de `fal_acta_economia_proyeccion`. `SnapshotRecalculador.proyectarPagos` es no-op. Eliminados: `tipo_obligacion_pago`, `estado_obligacion_pago`, `monto_obligacion_pago`, `tipo_forma_pago_vigente`, `estado_forma_pago_vigente`, `si_plan_pago`, `estado_plan_pago`, `cant_cuotas_plan`, `valor_cuota_plan`, `cant_cuotas_pagadas`, `cant_cuotas_mora`, `cant_cuotas_mora_consec`, `cant_dias_mora`, `si_apta_intimacion`, `motivo_apta_intimacion`, `si_pago_procesado`, `si_pago_confirmado`, `fh_ult_sync_ingresos`. `monto_operativo_vigente` permanece solo como valorización UX. |
| **Fundamento** | Snapshot mínimo, operativo y regenerable. Sin duplicación de datos económicos con `fal_acta_economia_proyeccion`. |
| **Consecuencia Java** | Campos de pago eliminados de `FalActaSnapshot.java`. Enums de pago eliminados de imports de FalActaSnapshot. `SnapshotRecalculador.proyectarPagos()` es ya no-op confirmado. |
| **Consecuencia DDL futura** | `fal_acta_snapshot` no incluye las columnas de pago listadas. Contrato normativo: solo `fal_acta_economia_proyeccion` transporta estado económico detallado. |
| **Tests/guardrails** | `G21CierreDDL.snapshot_no_contiene_campos_economia_pago`, `MariaDbLogicalModelParityGuardrailTest.SnapshotEvidenciaR3.fal_acta_snapshot`. |

### `DECISION_DDL-EVID-01` — `fal_acta_evidencia`: campos de auditoría y hash

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | Campos aprobados: `id`, `id_acta`, `tipo_evid`, `storage_key`, `fecha_registro DATETIME(6) NOT NULL`, `hash_evid CHAR(64) NULL` (SHA-256 hex opcional), `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`. No agregar: `nombre_archivo`, `mime_type`, `orden_evid`, `fh_captura` adicional. |
| **Fundamento** | `storage_key` referencia al almacenamiento; nombre y MIME pertenecen al metadato del storage. `fecha_registro` = instante funcional de captura; `fh_alta` = instante técnico. No duplicar. |
| **Consecuencia Java** | `FalActaEvidencia` actualizado con `fhAlta LocalDateTime NOT NULL`, `idUserAlta String NOT NULL`, `hashEvid String NULL` (validación 64 caracteres hex). Constructor exige los tres. `ActaService.labrar()` actualizado. |
| **Consecuencia DDL futura** | `fecha_registro DATETIME(6) NOT NULL`, `hash_evid CHAR(64) NULL`, `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`. Append-only: inmutable post-creación. |
| **Tests/guardrails** | `G21CierreDDL.fal_acta_evidencia_campos_auditoria_y_hash`. |

### `DECISION_DDL-PAGO-03` — `fal_acta_obligacion_pago`: `origen_obligacion` y `obligacion_reemplazada_id`

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | Incorporar `origenObligacion` (enum `OrigenObligacionPago`, obligatorio en toda entidad persistible, default `FALTAS`) y `obligacionReemplazadaId Long` (nullable). Catálogo: `FALTAS=1, APREMIO=2, JUZGADO=3`. Restricciones de `obligacionReemplazadaId`: FK self `ON DELETE RESTRICT`; `UNIQUE(obligacion_reemplazada_id)` cuando no es NULL (garantiza que una obligacion anterior sea reemplazada por una sola nueva); CHECK en aplicacion que el id propio no sea igual a `obligacionReemplazadaId`. |
| **Fundamento** | Soporte estructural para `EstadoObligacionPago.REEMPLAZADA` y evento `OBLREP`. Trazabilidad de origen. La unicidad garantiza que no se crean dos obligaciones que reemplazan a la misma anterior. |
| **Consecuencia Java** | `FalActaObligacionPago.origenObligacion` (default `FALTAS`; no acepta null en entidad persistible), `obligacionReemplazadaId Long`. Nuevo enum `OrigenObligacionPago` con `codigo()/desdeCodigo()`. Validacion en aplicacion: si `obligacionReemplazadaId != null`, entonces `obligacionReemplazadaId != id`. |
| **Consecuencia DDL futura** | `origen_obligacion SMALLINT NOT NULL DEFAULT 1`, `obligacion_reemplazada_id BIGINT NULL FK self ON DELETE RESTRICT`, `UNIQUE(obligacion_reemplazada_id)` parcial (cuando no es NULL, MariaDB admite multiples NULL en UNIQUE). |
| **Tests/guardrails** | `G21CierreDDL.fal_acta_obligacion_pago_campos_pago03`. |

### `DECISION_DDL-FORMA-01` — `fal_acta_forma_pago`: `fh_vencimiento`

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | Incorporar `fhVencimiento LocalDateTime` nullable. Representación futura: `fh_vencimiento DATETIME(6) NULL`. Informativa; no rechaza automáticamente pagos fuera de término. |
| **Fundamento** | Registrar vencimiento del recibo sin rechazar pagos fuera de término. Campo del histórico 2026-06-23 verificado contra necesidad operativa. |
| **Consecuencia Java** | `FalActaFormaPago.fhVencimiento` nullable, getter/setter y `copia()` actualizado. |
| **Consecuencia DDL futura** | `fh_vencimiento DATETIME(6) NULL`. |
| **Tests/guardrails** | `G21CierreDDL.fal_acta_forma_pago_tiene_fh_vencimiento`. |

### `DECISION_DDL-PLAN-01` — `fal_acta_plan_pago_ref`: `fh_ultimo_pago` y `fh_ult_sync_ingresos`

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | Incluir ambos en el modelo lógico y conservar en Java. `fh_ultimo_pago` = último pago conocido del plan. `fh_ult_sync_ingresos` = watermark de sincronización con Ingresos. Son conceptos distintos. |
| **Fundamento** | Ambos presentes en Java vigente (`FalActaPlanPagoRef`); ausentes en histórico 2026-06-23. Java es autoritativo. Uso funcional confirmado para UX de mora/seguimiento. |
| **Consecuencia Java** | Sin cambio (campos ya presentes en `FalActaPlanPagoRef.java`). |
| **Consecuencia DDL futura** | `fh_ultimo_pago DATETIME(6) NULL`, `fh_ult_sync_ingresos DATETIME(6) NULL`. |
| **Tests/guardrails** | `MariaDbLogicalModelParityGuardrailTest.PagosR3.fal_acta_plan_pago_ref`. |

### `DECISION_DDL-MOV-01` — `fal_acta_pago_movimiento`: `tipo_vencimiento_pago` excluido

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | No incorporar `tipo_vencimiento_pago`. Cuando se necesite, derivar de `fal_acta_pago_movimiento.fh_movimiento` + `fal_acta_forma_pago.fh_vencimiento`. No crear enum ni columna sin contrato funcional. |
| **Fundamento** | Java no define el enum; el histórico 2026-06-23 lo tenía, pero no tiene respaldo funcional en el código vigente. Derivable combinando campos existentes. |
| **Consecuencia Java** | Sin cambio. `tipo_vencimiento_pago` no se agrega a `FalActaPagoMovimiento`. |
| **Consecuencia DDL futura** | Columna excluida de `fal_acta_pago_movimiento`. |
| **Tests/guardrails** | `MariaDbLogicalModelParityGuardrailTest.PagosR3.fal_acta_pago_movimiento`. |

### `DECISION_DDL-MOV-02` — `fal_acta_pago_movimiento`: `fh_recepcion_tecnica` excluido

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | No incorporar `fh_recepcion_tecnica`. Semántica definitiva: `fh_movimiento` = instante funcional del hecho económico; `fh_alta` = instante técnico de recepción/persistencia en Faltas. |
| **Fundamento** | Java no lo usa. `fh_alta` ya registra cuándo se persistió la fila. No hay caso de uso de latencia que requiera campo adicional. |
| **Consecuencia Java** | Sin cambio. |
| **Consecuencia DDL futura** | Columna excluida de `fal_acta_pago_movimiento`. |
| **Tests/guardrails** | `MariaDbLogicalModelParityGuardrailTest.PagosR3.fal_acta_pago_movimiento`. |

### `DECISION_DDL-MOV-03` — `fal_acta_pago_movimiento`: longitud de `motivo_aplicacion_pago_anterior`

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | `motivo_aplicacion_pago_anterior VARCHAR(500) NULL`. El campo es opcional: null de entrada es aceptado. Cuando se informa un valor no nulo, se aplica trim; el resultado puede ser cadena vacía. Máximo 500 caracteres. No es obligatorio ni not-blank; el contrato funcional de PAGRES no lo exige. |
| **Fundamento** | Contrato funcional aprobado: `motivo` es opcional en PAGRES. No existe precondicion de no-blank. Capacidad para motivos largos sin limite de TEXT que romperia consistencia. |
| **Consecuencia Java** | `FalActaPagoMovimiento.Builder.motivoAplicacionPagoAnterior(String)` aplica trim cuando el valor es no nulo y valida max 500. Null aceptado. Cadena vacia aceptada tras trim. |
| **Consecuencia DDL futura** | `motivo_aplicacion_pago_anterior VARCHAR(500) NULL`. |
| **Tests/guardrails** | `G21CierreDDL.motivo_aplicacion_pago_anterior_valida_max_500`. |

### `DECISION_DDL-MOV-04` — `fal_acta_pago_movimiento`: catálogo `ClasificacionPago` autoritativo

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | Catálogo autoritativo: `NORMAL=1, DUPLICADO_REAL=2, EXCEDENTE=3, OBLIGACION_ANTERIOR=4`. No conservar códigos históricos alternativos. No existe tabla productiva actual de movimientos en el baseline entregado. |
| **Fundamento** | Java vigente define 4 valores con códigos propios; histórico 2026-06-23 tenía 3 valores con códigos distintos. Java es autoritativo (producción vigente). |
| **Consecuencia Java** | `ClasificacionPago.java` vigente: `NORMAL=1/DUPLICADO_REAL=2/EXCEDENTE=3/OBLIGACION_ANTERIOR=4`. Sin cambio. |
| **Consecuencia DDL futura** | `clasificacion_pago SMALLINT NOT NULL DEFAULT 1`; catálogo: 4 valores 1..4 con semántica Java. |
| **Tests/guardrails** | `SpecAsSourceGuardrailTest.G9ParidadEnums` (ClasificacionPago). |

### `DECISION_DDL-ECPR-01` — `fal_acta_economia_proyeccion`: nullabilidad de `fh_corte_economico` y `fh_ult_mod`

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | `fh_corte_economico DATETIME(6) NULL` (puede ser NULL mientras no fue calculada la primera vez). `fh_ult_mod DATETIME(6) NOT NULL` (debe existir para toda fila persistida). Sin `DEFAULT NOW()`: la aplicación suministra `fhUltMod` desde `FaltasClock`. En cada recalculo: capturar un unico instante de `FaltasClock` y usarlo coherentemente para `fhCorteEconomico` y `fhUltMod`. |
| **Fundamento** | Semantica explícita: corte puede ser NULL en estado inicial; `fh_ult_mod` es audit obligatorio. El instante unico garantiza coherencia temporal sin drift entre los dos campos de un mismo recalculo. |
| **Consecuencia Java** | `InMemoryEconomiaProyeccionRepository.save()` lanza `IllegalStateException` si `fhUltMod == null`. En `EconomiaProyeccionRecalculador`: capturar `faltasClock.now()` una sola vez y asignarlo a ambos campos. |
| **Consecuencia DDL futura** | `fh_corte_economico DATETIME(6) NULL`, `fh_ult_mod DATETIME(6) NOT NULL`. El adapter JDBC debe rechazar `INSERT`/`UPDATE` con `fhUltMod` nulo antes de ejecutar SQL. |
| **Tests/guardrails** | `G23OccYActor.economia_proyeccion_rechaza_fh_ult_mod_null`. |

### `DECISION_DDL-DOC-01` — `fal_documento`: OCC con `version_row`

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | Agregar `versionRow int` (default 0) a `FalDocumento`. `InMemoryDocumentoRepository.guardar()` detecta versión desactualizada y lanza `ConcurrenciaConflictoException`. No cambiar lifecycle documental. |
| **Fundamento** | OCC requerido para el endpoint de numeración y estados documentales. Protección de concurrencia replicable en adapter JDBC con `SELECT … FOR UPDATE` o `UPDATE … WHERE version_row = ?`. |
| **Consecuencia Java** | `FalDocumento.versionRow int`; `InMemoryDocumentoRepository` aplica OCC in-place. |
| **Consecuencia DDL futura** | `version_row INT NOT NULL DEFAULT 0`. |
| **Tests/guardrails** | `G21CierreDDL.fal_documento_tiene_version_row`. |

### `DECISION_DDL-NOTI-01` — `fal_notificacion`: OCC con `version_row`

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | Agregar `versionRow int` (default 0) a `FalNotificacion`. `InMemoryNotificacionRepository.guardar()` aplica OCC. Los monitores JVM continúan como protección local durante esta fase; la garantía productiva futura será transacción JDBC + OCC/constraints. |
| **Fundamento** | Coherencia con otras entidades mutables. Protección multiinstancia en JDBC. |
| **Consecuencia Java** | `FalNotificacion.versionRow int`; `InMemoryNotificacionRepository` aplica OCC. |
| **Consecuencia DDL futura** | `version_row INT NOT NULL DEFAULT 0`. |
| **Tests/guardrails** | `G21CierreDDL.fal_notificacion_tiene_version_row`. |

### `DECISION_DDL-GEXT-01` — `fal_acta_gestion_externa`: OCC con `version_row`

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | Agregar `versionRow int` (default 0) a `FalGestionExterna`. La futura garantía física será doble: OCC por `version_row` + unicidad condicional de una sola gestión activa por acta mediante columna generada + UNIQUE. No implementar SQL todavía. |
| **Fundamento** | OCC para entidad mutable con invariante de unicidad de activa. |
| **Consecuencia Java** | `FalGestionExterna.versionRow int`; `InMemoryGestionExternaRepository` aplica OCC. |
| **Consecuencia DDL futura** | `version_row INT NOT NULL DEFAULT 0`; columna generada conceptual para unicidad de activa. |
| **Tests/guardrails** | `G21CierreDDL.fal_gestion_externa_tiene_version_row`. |

### `DECISION_DDL-BLOQ-01` — `fal_acta_bloqueante_cierre_material`: OCC con `version_row`

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | Agregar `versionRow int` (default 0) a `FalBloqueanteMaterial`. `InMemoryBloqueanteMaterialRepository.guardar()` aplica OCC. |
| **Fundamento** | Coherencia con otras entidades mutables. |
| **Consecuencia Java** | `FalBloqueanteMaterial.versionRow int`; `InMemoryBloqueanteMaterialRepository` aplica OCC. |
| **Consecuencia DDL futura** | `version_row INT NOT NULL DEFAULT 0`. |
| **Tests/guardrails** | `G21CierreDDL.fal_bloqueante_material_tiene_version_row`. |

### `DECISION_DDL-PERS-01` — `fal_persona`: unicidad física `(tipo_doc, nro_doc)`

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | `CHECK`: `tipo_doc` y `nro_doc` son ambos NULL o ambos NOT NULL. `UNIQUE(tipo_doc, nro_doc)`. MariaDB admite múltiples NULL en un UNIQUE: coexisten personas técnicas/no identificadas. La aplicación normaliza `nroDoc` antes de persistir. No agregar `si_identificado`, columna generada ni índice parcial. Nombres físicos vigentes: `tipo_doc`, `nro_doc`. |
| **Fundamento** | Integridad referencial de persona identificada sin romper soporte de personas técnicas. |
| **Consecuencia Java** | Sin cambio en `FalPersona.java`. |
| **Consecuencia DDL futura** | `UNIQUE(tipo_doc, nro_doc)` + `CHECK((tipo_doc IS NULL) = (nro_doc IS NULL))`. |
| **Tests/guardrails** | `MariaDbLogicalModelParityGuardrailTest.FalPersona`. |

### `DECISION_DDL-PAGO-01` — Identidad física `FalPagoVoluntario` / `FalPagoCondena`

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | `FalPagoVoluntario` y `FalPagoCondena` permanecen como vistas de aplicación sobre `fal_acta_obligacion_pago`. No crear tablas ni vistas SQL propias. No duplicar identidad. |
| **Fundamento** | Decisión D2 ya cerrada. Las vistas de aplicación son suficientes para el modelo funcional vigente. |
| **Consecuencia Java** | Sin cambio. |
| **Consecuencia DDL futura** | Sin tabla adicional. |
| **Tests/guardrails** | `MariaDbLogicalModelParityGuardrailTest.PagosR3.fal_acta_obligacion_pago` (verifica ausencia de tabla propia). |

### `DECISION_DDL-PAGO-MOV-01` — `fal_acta_pago_movimiento`: contrato físico completo

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | Ternas EM/PG: completas o totalmente NULL. PG obligatoria en todo `PAGO_CONFIRMADO` original (`movimiento_origen_id IS NULL`). Unicidad futura: `UNIQUE(origen_movimiento, cmte_pg, pref_pg, nro_pg)` (NULLs excluyen), `UNIQUE(origen_movimiento, referencia_externa)` cuando informada. `movimiento_origen_id FK self ON DELETE RESTRICT`. CHECK: `movimiento_origen_id IS NULL OR movimiento_origen_id <> id`. Unicidad de aplicacion unica PAGRES: columna generada nullable `aplicacion_pago_anterior_origen_id` que expone `movimiento_origen_id` solo cuando `tipo_movimiento = PAGO_CONFIRMADO AND clasificacion_pago = NORMAL AND movimiento_origen_id IS NOT NULL`; `UNIQUE` sobre esa columna generada. |
| **Fundamento** | Garantía de integridad de recibos únicos, unicidad de aplicación por PAGANT, trazabilidad. |
| **Consecuencia Java** | Sin cambio de estructura; validaciones ya presentes en `PagoMovimientoService`. |
| **Consecuencia DDL futura** | Ternas con CHECK, UNIQUE condicionales, FK self, columna generada `aplicacion_pago_anterior_origen_id` para unicidad de aplicacion PAGRES. |
| **Tests/guardrails** | `MariaDbLogicalModelParityGuardrailTest.PagosR3.fal_acta_pago_movimiento`. |

### `DECISION_DDL-ENUM-01` — Politica de enums persistibles: `codigo()` + `SMALLINT` (`EstadoFalloActa`, `EstadoApelacionActa`, `EstadoPagoCondena`, `TipoDiaNoComputable`, `OrigenDiaNoComputable`)

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | Política global: agregar código numérico explícito, persistir `SMALLINT`, prohibido `ordinal()`, prohibido persistir `name()` accidentalmente. Enums promovidos y sus códigos: `EstadoFalloActa` (1-6), `EstadoApelacionActa` (1-6), `EstadoPagoCondena` (1-5), `TipoDiaNoComputable` (1-3), `OrigenDiaNoComputable` (1-2). Ver detalle de valores en sección 3 de `mariadb-logical-model.md`. |
| **Fundamento** | Preparacion para SMALLINT fisico. Estabilidad de serializacion. Prohibido `ordinal()` e inferencia por posicion. |
| **Consecuencia Java** | Cinco enums actualizados con `codigo() short` y `desdeCodigo(short)`. Categoría cambia de `NO_EXPLICIT_CODE` a `EXPLICIT_NUMERIC_CODE`. |
| **Consecuencia DDL futura** | Columnas `estado_fallo`, `estado_apelacion` (tablas fallo/apelación), `tipo`/`origen` (tabla `fal_dia_no_computable`), `estado_pago_condena` (si se persiste) — todas `SMALLINT`. |
| **Tests/guardrails** | `G16EstrategiaEnums.enums_decision_ddl_enum01_tienen_codigo_numerico`, `G21CierreDDL.enums_enum01_round_trip`. |

---

## Decisiones transversales nuevas (slice DDL-CLOSURE-001-R1)

### `DECISION_DDL-EXEC-01` — Mecanismo de ejecución DDL

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | Flyway = NO. Liquibase = NO. DDL automático al iniciar Spring = NO. Ejecución SQL = manual por el usuario desde HeidiSQL (u otro cliente). Los scripts futuros deben vivir versionados en Git fuera del classpath/runtime (p. ej. bajo `database/`). Spring JDBC puede conectar y operar; Spring no crea ni actualiza el esquema. |
| **Fundamento** | Entorno de proyecto actual: MariaDB 12.3.2, Windows x64, InnoDB, utf8mb4. Control total sobre cuándo y cómo se ejecuta el DDL. Sin riesgo de ejecución automática en entornos no deseados. |
| **Consecuencia Java** | Sin cambio. Spring Boot no incluirá `spring.datasource.initialize=true` ni equivalente. |
| **Consecuencia DDL futura** | Scripts SQL versionados en Git (fuera de `src/`). Sin runner automático. |
| **Tests/guardrails** | `G22CleanRoomYSeeding.ddl_execution_seeding_doc_presente` (verifica que el documento `50-persistence/ddl-execution-and-test-seeding.md` existe y contiene la instrucción). |

### `DECISION_DDL-BASELINE-01` — Objetos protegidos pre-existentes

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | Lista exacta de objetos protegidos: tablas `fal_informix_sync_error`, `fal_informix_sync_run`, `fal_rubro_version`, `geo_bahra_asentamiento`, `geo_calle_alturas_barrio`, `geo_dataset_load_error`, `geo_dataset_load_run`, `geo_dataset_row_version`, `geo_ign_departamento`, `geo_ign_municipio`, `geo_ign_provincia`, `geo_indec_calles`, `geo_indec_localidad`, `geo_indec_localidad_censal`, `geo_malv_calle_version`, `geo_malv_localidad_version`; vistas `vw_fal_rubro_actual`, `vw_geo_malv_calle_actual`, `vw_geo_malv_localidad_actual`, `vw_geo_municipio_departamento`. Sin `CREATE/ALTER/DROP/TRUNCATE/DELETE` sobre ellos. `fal_rubro_version` se adopta como tabla canónica preexistente (`PREEXISTING_CANONICAL_ADOPTED`). El DDL futuro crea 64 tablas faltantes y valida 65 canónicas presentes. Los guardrails usan lista exacta, no `LIKE 'fal_%'`. Datos comprobados: MariaDB 12.3.2, Windows x64, InnoDB, utf8mb4, `utf8mb4_uca1400_ai_ci`, `lower_case_table_names = 1`, `id_tca` máximo observado en `geo_calle_alturas_barrio` = 4, valores `id_tca` con más de 10 caracteres = 0. |
| **Fundamento** | Preservar infraestructura operativa existente. Separación clara entre tablas canónicas Faltas (65) y dependencias técnicas protegidas. |
| **Consecuencia Java** | Sin cambio. |
| **Consecuencia DDL futura** | El script DDL comienza con un bloque de verificación/protección de la lista exacta. |
| **Tests/guardrails** | `G22CleanRoomYSeeding.objetos_protegidos_documentados`, `G22CleanRoomYSeeding.fal_rubro_version_preexisting_adopted`. |

### `DECISION_DDL-SEED-01` — Contrato del seeder futuro

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | No implementar el seeder en este slice. Contrato normativo únicamente: una sola implementación reutilizable; operaciones `VERIFY`, `SEED`, `RESET_TEST_DATA`, `RESET_AND_SEED`; deshabilitado por defecto; requiere perfil/flag/acción/confirmación explícitos; allowlist de base y host; prohibido en producción; limpia solo datos de escenarios Faltas; nunca toca GEO, rubros ni sincronización; reutiliza casos canónicos de los tests InMemory; ejecutable desde test de integración y manualmente para UX posterior. |
| **Fundamento** | Garantía de seguridad y reproducibilidad. Sin riesgo de borrar datos de producción o infraestructura GEO. |
| **Consecuencia Java** | Sin cambio (seeder no implementado). |
| **Consecuencia DDL futura** | La implementación del seeder seguirá este contrato. |
| **Tests/guardrails** | `G22CleanRoomYSeeding.ddl_execution_seeding_doc_presente` (verifica que el documento contiene el contrato del seeder). |

### `DECISION_DDL-COMMENT-01` — Comentarios obligatorios en DDL futuro

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | Todo DDL futuro debe incluir comentarios de tabla y columna. Tabla: `QUÉ ES: ... USO: ... RAZÓN: ...`. Columnas: describir qué dato contiene, para qué se utiliza, regla relevante cuando exista. Comentarios breves, claros y declarativos. No repetir solo el tipo SQL. Sin marcadores pendientes. No modificar objetos protegidos solo para agregar comentarios. Validación futura mediante `information_schema`. |
| **Fundamento** | Auto-documentación del esquema en el motor. Facilita auditorías y onboarding. |
| **Consecuencia Java** | Sin cambio. |
| **Consecuencia DDL futura** | Cada `CREATE TABLE` y `ALTER TABLE` incluirá `COMMENT` de tabla y por columna. |
| **Tests/guardrails** | `G22CleanRoomYSeeding.ddl_execution_seeding_doc_presente` (verifica que el documento describe la política de comentarios). |

---

## Decisiones FULL-R1.2-CORRECCION-10

### `DECISION_DDL-EVID-02` — `fal_acta_evidencia`: catálogo completo `TipoEvidenciaActa`

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | `HUMAN_DECISION_CLOSED — FULL-R1.2-CORRECCION-10`. El único valor activo anterior (`FIRMA_OLOGRAFA_INFRACTOR = 6`) deja de ser válido. El catálogo aprobado es: `FOTO=4, VIDEO=8, AUDIO=12, PDF=16, DOCUMENTO_OFIMATICO=20, PLANILLA_CALCULO=24, FIRMA_OLOGRAFA_INFRACTOR=48`. Códigos 28–44 reservados para futuros tipos. No agregar constantes `RESERVADO_*`. El DDL todavía no fue ejecutado; no existe información productiva; no se crea migración ni compatibilidad temporal. |
| **Fundamento** | Catálogo de evidencias ampliado: el acta puede adjuntar fotos, videos, audios, documentos y firma ológrafa, no solo firma. La secuencia en múltiplos de 4 deja espacio para futuros tipos sin re-numerar los existentes. La firma queda en el extremo superior actual (48) separada semánticamente de los multimedia. |
| **Consecuencia Java** | `TipoEvidenciaActa` actualizado con los 7 valores. Tipo del constructor `(short)`. Resolución por código; prohibido `ordinal()`. El código 6 lanza `IllegalArgumentException`. La regla funcional `resultadoFirmaInfractor=FIRMADA` se verifica por `tipo == TipoEvidenciaActa.FIRMA_OLOGRAFA_INFRACTOR`, no por posición ni valor numérico. |
| **Consecuencia DDL futura** | `tipo_evid SMALLINT NOT NULL` (sin cambio de tipo). Comentario actualizado con los 7 códigos. Expresión `único valor activo` eliminada del DDL. |
| **Tests/guardrails** | `TipoEvidenciaActaGuardrailTest` (7 valores, unicidad, múltiplos de 4, código 6 inválido, código 48 válido, regla de firma). `MariaDbLogicalModelParityGuardrailTest` actualizado. |

### `DECISION_DDL-PERS-02` — `fal_persona` / `fal_acta_contravencion` / `fal_acta_snapshot`: tipos `id_suj` / `id_bie`

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | `HUMAN_DECISION_CLOSED — FULL-R1.2-CORRECCION-10`. Tipos físicos aprobados: `id_suj TINYINT UNSIGNED` (rango 1-255), `id_bie MEDIUMINT UNSIGNED` (rango 1-9999999). En Java, ambos se representan como `Integer`. No usar `Long` para estos campos. No usar `byte` (Java `byte` = -128 a 127). `id_suj` no es un identificador único de persona; es el código de tipo de sujeto del sistema de Ingresos Municipales (catálogo abierto). Valores conocidos: 1=INMUEBLE, 2=COMERCIO, 3=RODADO, 18=CEMENTERIO, 20=FALTAS, 99=VARIOS. Prohibido `CHECK id_suj IN (1,2,3,18,20,99)` o variantes cerradas. `id_bie` requiere `id_suj` (CHECK id_bie IS NULL OR id_suj IS NOT NULL). La regla contextual `idSuj=20` para registros originados en Faltas es válida localmente; el rango físico permanece abierto (1-255). |
| **Fundamento** | `BIGINT` era excesivo para identificadores de catálogo acotado. `TINYINT UNSIGNED` cubre 255 tipos de sujeto; `MEDIUMINT UNSIGNED` cubre hasta 9.999.999 bienes/cuentas. Ingresos puede incorporar nuevos tipos de sujeto sin DDL. |
| **Consecuencia Java** | `FalPersona.idSuj Long → Integer`, `FalPersona.idBie Long → Integer`. `FalActaContravencion.idSujI/idBieI/idSujC/idBieC Long → Integer`. `FalActaSnapshot.idBieI/idBieC Long → Integer`. `PersonaRepository.buscarPorIdSujBie(Integer, Integer)`. `PersonaService.actualizarVinculoIngresos` usa `Integer`. |
| **Consecuencia DDL futura** | `fal_persona`: `id_suj TINYINT UNSIGNED NULL`, `id_bie MEDIUMINT UNSIGNED NULL`, CHECK 1-255 y 1-9999999. `fal_acta_contravencion`: CHECK `id_suj_i/c BETWEEN 1 AND 255` (antes BETWEEN 1 AND 99). `fal_acta_snapshot`: `id_bie_i/c MEDIUMINT UNSIGNED NULL` (antes BIGINT). |
| **Tests/guardrails** | `IdSujBieGuardrailTest` (fronteras, Java Integer, snapshots, copias). `SatelitesCatalogosTest` actualizado. |

### `DECISION_DDL-DOCU-02` — `fal_documento_plantilla`: comentario `momento_numeracion_docu`

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | `HUMAN_DECISION_CLOSED — FULL-R1.2-CORRECCION-10`. Comentario del DDL corregido para documentar el catálogo completo: `0=NO_APLICA, 1=AL_CREAR, 2=AL_EMITIR, 3=AL_ENVIAR_A_FIRMA, 4=AL_FIRMAR`. El comentario anterior comenzaba en 1 y omitía los últimos dos valores. El tipo físico `SMALLINT NOT NULL` no cambia en este slice. El enum Java `MomentoNumeracionDocu` ya contenía los 5 valores correctos. |
| **Fundamento** | Corrección documental pura. El catálogo Java era correcto; el comentario DDL estaba desfasado. |
| **Consecuencia Java** | Sin cambio en el enum. |
| **Consecuencia DDL futura** | `momento_numeracion_docu SMALLINT NOT NULL COMMENT '...'` con los 5 valores. |
| **Tests/guardrails** | `MomentoNumeracionDocuGuardrailTest` (comentario DDL documenta 5 valores, no aparece catálogo anterior). |

---

## Regla de gobierno de este registro

- Todo nuevo `DECISION_DDL-*` se agrega únicamente a este archivo.
- Ninguna decisión aquí puede alterar un estado, evento, transición, comando o
  contrato HTTP ya cerrado en `10-domain/`, `20-application/`, `30-projections/`
  o `40-api/`. Si una decisión física parece requerir ese cambio, se detiene y
  se reporta como gap, no se resuelve por inferencia.
- Al cerrar una decisión, se mueve de "Pendientes" a "Cerradas" con:
  decisión adoptada, fundamento, consecuencia Java, consecuencia DDL futura,
  tests/guardrails que la protegen.
