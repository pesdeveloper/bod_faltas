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
> (EVID-02, PERS-02, DOCU-02). Adicionalmente, 3 decisiones físicas de la
> integración EDA Faltas↔Firmas (`FIRMAREQ-01`, `FIRMA-02`, `BUS-01`,
> registradas más abajo con encabezado `####` para no alterar el conteo de
> 27 anteriores).

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

### `DECISION_DDL-PERS-01` — `fal_persona`: documento opcional, canónico y estructurado

> **Versión definitiva y única autoridad.** Reescrita por
> `JDBC-DOMINIO-EDA-WAVE-1A2-B1.5A-PERSONA-DOCUMENTO-NORMATIVO-002`. La redacción
> anterior de esta misma decisión —que declaraba los nombres físicos `tipo_doc` y
> `nro_doc`, `UNIQUE(tipo_doc, nro_doc)` y "sin cambio en `FalPersona.java`"— queda
> **OBSOLETA**. También queda **OBSOLETA** la definición física ejecutada de
> documento obligatorio y exclusivamente numérico (`tipo_documento SMALLINT NOT
> NULL`, `nro_doc INT UNSIGNED NOT NULL`). No coexisten dos decisiones cerradas
> incompatibles: la única vigente es la de esta tabla.

#### 1. Nombres físicos definitivos

El nombre físico canónico de la columna de tipo documental es **`tipo_documento`**.
**`tipo_doc` está prohibido como nombre físico**: no existe en el motor, no existe
en `database/ddl/create-bod-faltas-domain.sql` y
`DdlPhysicalParityTest.falPersona_documentoEstructurado` verifica su ausencia. Todo
SQL, backfill, guardrail o documento que use `tipo_doc` como nombre de columna es
incorrecto y debe corregirse a `tipo_documento`. `nro_doc`, `prefijo_cuit_cuil`,
`digito_verificador` y `doc_key` conservan su nombre.

#### 2. Tipos y nulabilidad del modelo objetivo

| Columna | Tipo objetivo | Nulabilidad | Cambio respecto del esquema ejecutado |
|---|---|---|---|
| `tipo_documento` | `SMALLINT` | **NULL** | de `NOT NULL` a `NULL` |
| `prefijo_cuit_cuil` | `TINYINT UNSIGNED` | NULL | sin cambio de tipo; se estrecha el `CHECK` |
| `nro_doc` | **`VARCHAR(12)`** | **NULL** | de `INT UNSIGNED NOT NULL` a `VARCHAR(12) NULL` |
| `digito_verificador` | `TINYINT UNSIGNED` | NULL | sin cambio |
| `doc_key` | **`VARCHAR(12)`** `STORED GENERATED` | NULL | se redefine la expresión y se reduce de `VARCHAR(20)` |

`VARCHAR(12)` es la longitud definitiva de `nro_doc`:
no `VARCHAR(20)`, no `VARCHAR(10)`, no volver a `INT UNSIGNED`.
Ningún tipo documental del catálogo
`TipoDocumentoPersona` exige más de 12 caracteres canónicos una vez que CUIT/CUIL se
almacena descompuesto. Si aparece un tipo documental legal y vigente que exija más,
se detiene con evidencia y se revisa esta decisión; no se amplía por prevención.

#### 3. Coherencia y semántica del documento

`CHECK` de coherencia: `(tipo_documento IS NULL) = (nro_doc IS NULL)`.

| Caso | `tipo_documento` | `nro_doc` | `prefijo_cuit_cuil` | `digito_verificador` | `doc_key` |
|---|---|---|---|---|---|
| sin documento | NULL | NULL | NULL | NULL | NULL |
| documento no CUIT/CUIL | NOT NULL | NOT NULL | NULL | NULL | `nro_doc` |
| CUIT o CUIL | NOT NULL | NOT NULL (8 dígitos) | NOT NULL | NOT NULL | 11 dígitos |

La persona sin documento es un estado modelado y válido. Es lo que exige
`SituacionIdentificacionInfractor.IDENTIFICADO_PARCIAL` en
`../10-domain/states-events-catalogs.md`: existe una `fal_persona` real con los
datos verificables disponibles y puede no existir documento. Queda prohibido
representar la ausencia de documento con **persona centinela**, persona ficticia,
**documento inventado**, PK reservada, valor `0`, cadena vacía o cualquier
centinela numérico.

#### 4. Forma canónica de `nro_doc` (opción A)

`nro_doc` almacena **ya** la forma canónica. `doc_key` no la calcula: sólo compone
los tres componentes de CUIT/CUIL. La forma visual amigable se resuelve fuera de la
persistencia y nunca se almacena.

Función canónica única, por tipo documental, aplicada por la capa de aplicación
antes de persistir:

1. se eliminan espacios (todos, incluso internos) y los separadores sin valor de
   identidad: punto, guion y barra;
2. las letras pasan a mayúscula;
3. el juego de caracteres permitido depende del tipo;
4. el resultado no puede quedar vacío;
5. el resultado no puede superar 12 caracteres.

| Tipo | Código | Juego permitido | Longitud canónica | Ceros a la izquierda |
|---|---|---|---|---|
| `DNI` | 1 | `[0-9]` | 1..8 dígitos | **se eliminan**: el cero inicial no forma parte de la identidad de un documento numérico |
| `CUIT` | 2 | `[0-9]` | exactamente 8 (cuerpo central) | **se conservan**: el cuerpo es un campo de ancho fijo del CUIT |
| `CUIL` | 3 | `[0-9]` | exactamente 8 (cuerpo central) | **se conservan** |
| `PASAPORTE` | 4 | `[0-9A-Z]` | 1..12 | **se conservan**: todo carácter es significativo |
| `DNI_EXTRANJERO` | 5 | `[0-9A-Z]` | 1..12 | **se conservan** |
| `OTRO` | 9 | `[0-9A-Z]` | 1..12 | **se conservan** |

Queda prohibido convertir un documento alfanumérico a entero,
descartar caracteres para forzar `INT UNSIGNED`, truncar en silencio para respetar
un ancho y aplicar dos normalizaciones distintas en dos caminos de aplicación. Un
valor que no cumple la forma canónica de su tipo se **rechaza antes de persistir**.

#### 5. CUIT / CUIL: representación estructurada

CUIT y CUIL se almacenan descompuestos en tres columnas, no como los once dígitos
en `nro_doc`:

```text
prefijo_cuit_cuil   dos digitos significativos
nro_doc             cuerpo central canonico, exactamente ocho digitos como texto
digito_verificador  un digito
```

Para `tipo_documento IN (2, 3)`: `prefijo_cuit_cuil`, `nro_doc` y
`digito_verificador` son los tres obligatorios. Para cualquier otro tipo:
`prefijo_cuit_cuil` y `digito_verificador` son NULL.

La capa de aplicación descompone y valida estructuralmente el valor recibido antes
de persistir. Un CUIT/CUIL incompleto o estructuralmente inválido se rechaza:
no se degrada a `DNI`, no se guarda como documento genérico,
no se inventa prefijo ni dígito y no se rellena ningún componente faltante.
El formato visual `XX-XXXXXXXX-X` se reconstruye desde los tres componentes.

`prefijo_cuit_cuil` es `TINYINT UNSIGNED` y por lo tanto no distingue `5` de `05`.
El `CHECK` que exige dos dígitos significativos (`BETWEEN 10 AND 99`) hace
inalcanzable esa ambigüedad y es lo que permite componer `doc_key` sin relleno. Si
alguna vez existiera un prefijo legal con cero inicial, el tipo físico tendría que
pasar a `CHAR(2)`; hasta entonces no se agrega relleno.

#### 6. `doc_key` y `UNIQUE`

```sql
doc_key VARCHAR(12) AS (
  CASE
    WHEN tipo_documento IS NULL          THEN NULL
    WHEN tipo_documento IN (2, 3)        THEN CONCAT(prefijo_cuit_cuil, nro_doc, digito_verificador)
    ELSE nro_doc
  END
) STORED
UNIQUE KEY uq_fal_persona_tipo_doc_key (tipo_documento, doc_key)
```

Queda prohibido `COALESCE(...)` y `LPAD(...)` dentro de la expresión: no se
rellenan componentes ausentes para producir una clave de apariencia completa. Son
los `CHECK` los que exigen los componentes y las longitudes correctas **antes** de
que la clave se genere; con ellos vigentes, la rama CUIT/CUIL produce exactamente
once dígitos y la rama restante produce el `nro_doc` canónico. Sin documento,
`doc_key` es NULL y MariaDB admite ilimitadas filas con clave NULL bajo el UNIQUE:
así coexisten las personas identificadas parcialmente sin documento.

`doc_key` deja de incluir el prefijo `tipo_documento-`: es redundante, porque
`tipo_documento` ya es la primera columna del UNIQUE. Consecuencia deliberada del
UNIQUE compuesto: los mismos once dígitos pueden existir una vez como `CUIT` y otra
como `CUIL`, porque son dos tipos documentales distintos.

#### 7. `CHECK` físicos del modelo objetivo

| Constraint | Cláusula |
|---|---|
| `chk_fal_persona_doc_coherente` | `(tipo_documento IS NULL) = (nro_doc IS NULL)` |
| `chk_fal_persona_doc_tipo` | `tipo_documento IS NULL OR tipo_documento IN (1,2,3,4,5,9)` |
| `chk_fal_persona_doc_cuit` | `tipo_documento IS NULL OR tipo_documento NOT IN (2,3) OR (prefijo_cuit_cuil IS NOT NULL AND digito_verificador IS NOT NULL AND nro_doc REGEXP '^[0-9]{8}$')` |
| `chk_fal_persona_doc_no_cuit` | `tipo_documento IS NULL OR tipo_documento IN (2,3) OR (prefijo_cuit_cuil IS NULL AND digito_verificador IS NULL)` |
| `chk_fal_persona_nro_doc_canonico` | `nro_doc IS NULL OR nro_doc REGEXP '^[0-9A-Z]{1,12}$'` |
| `chk_fal_persona_nro_doc_dni` | `tipo_documento IS NULL OR tipo_documento <> 1 OR nro_doc REGEXP '^[1-9][0-9]{0,7}$'` |
| `chk_fal_persona_prefijo` | `prefijo_cuit_cuil IS NULL OR prefijo_cuit_cuil BETWEEN 10 AND 99` |
| `chk_fal_persona_digito` | `digito_verificador IS NULL OR digito_verificador BETWEEN 0 AND 9` |

`chk_fal_persona_nro_doc` (`nro_doc BETWEEN 1 AND 99999999`) desaparece: es un
`CHECK` numérico sobre una columna que pasa a ser texto. Su límite superior de ocho
dígitos se preserva para `DNI` en `chk_fal_persona_nro_doc_dni`.
`chk_fal_persona_tipo`, `chk_fal_persona_id_suj`, `chk_fal_persona_id_bie` y
`chk_fal_persona_id_bie_suj` no cambian.

Los `CHECK` verifican estructura, no aritmética. La verificación del dígito
verificador de CUIT/CUIL por módulo 11 **no** queda exigida por esta decisión:
haría rechazable un dato histórico y sería una regla de dominio nueva. Registrada
como `D-PERS-CUIT-DV-01` y decidible en un slice propio.

#### 8. Consecuencia Java

`FalPersona` conserva `nroDoc` como `String`, pero pasa a contener la forma
canónica y su longitud máxima baja de 20 a 12. `prefijoCuitCuil` y
`digitoVerificador` dejan de ser campos muertos: hoy tienen cero callsites de
escritura en producción y en tests, y la capa de aplicación debe asignarlos para
CUIT/CUIL. `FalPersona` no gana un campo `docKey`: la clave la genera el motor y la
aplicación no la informa.

La normalización queda en **un solo lugar**, y la autoridad es la función canónica
de la sección 4. Esa autoridad única existe: es el value object `DocumentoPersona`,
creado por `WAVE 1A2-B1.5B0-A`, que valida sus invariantes en el constructor del
record, de modo que ninguna instancia incoherente puede llegar a `FalPersona`.

Antecedente histórico, no estado presente: al momento de decidir **existían dos
normalizaciones incompatibles**. `PersonaService.aplicarDocumento` conservaba
puntos, no aplicaba mayúsculas, admitía hasta 20 caracteres y exigía el par
tipo/número; `PersonaService.crearMinimal` y `ActaService.crearPersonaMinimal`
eliminaban los puntos, truncaban en silencio a 20 y producían `nroDoc` informado
con `tipoDoc` NULL, combinación que `chk_fal_persona_doc_coherente` rechaza.
**Las dos expresiones regulares fueron eliminadas**: los tres caminos convergieron
en `DocumentoPersona` y ningún servicio conserva hoy una regex de documento, lo que
verifica `SpecAsSourceGuardrailTest.G31PersonaJavaIdentidad.normalizador_unico`.

Consecuencia sobre los dos caminos "minimal", que reciben el documento como texto
libre sin tipo documental: **no infieren el tipo**. Si el tipo documental no se
conoce, el documento de `fal_persona` queda NULL y el texto aportado permanece en
`FalActa.infractorDocumento`, que es su registro literal. La persona resultante es
`IDENTIFICADO_PARCIAL`. Inferir `DNI` porque el valor parece numérico sería
inventar identificación.

La forma visual del documento se reconstruye para presentación: `FalPersona.docTxt()`
y la variable de plantilla `infractor.nroDoc` deben rendir `XX-XXXXXXXX-X` cuando el
tipo es CUIT/CUIL, no el cuerpo central desnudo.

#### 9. Consecuencia JDBC

El adaptador `JdbcPersonaRepository` mapea `tipo_documento` por
`TipoDocumentoPersona.codigo()`; queda prohibido mapearlo por `ordinal()` o `name()`.
Escribe `nro_doc` como texto ya canónico; escribe `prefijo_cuit_cuil` y
`digito_verificador` como enteros o NULL; y **no informa `doc_key`**, que es
`STORED GENERATED`. La búsqueda por documento se hace por
`(tipo_documento, doc_key)` cuando el documento existe, y no aplica cuando es NULL.
`doc_key` es `VARCHAR` con collation `utf8mb4_uca1400_ai_ci`, insensible a caso y
acentos; como la forma canónica ya está en mayúsculas y restringida a `[0-9A-Z]`,
la insensibilidad no puede producir un falso positivo. `fal_persona` no tiene
`version_row`: no hay OCC.

#### 10. Consecuencia de migración

> **Estado de ejecución: `DDL_EXECUTED`.** `WAVE 1A2-B1.5B0-B` aplicó el plan
> completo sobre `sb_faltas_db` el 2026-08-03. El esquema ejecutado coincide desde
> entonces con el modelo objetivo de las secciones 2 a 7, y
> `database/ddl/create-bod-faltas-domain.sql` quedó actualizado en el mismo
> trabajo. Los artefactos versionados son
> `database/ddl/alter-fal-persona-documento-opcional-001-forward.sql`,
> `database/ddl/alter-fal-persona-documento-opcional-001-rollback.sql` y los tres
> diagnósticos `verify-fal-persona-documento-opcional-001-{precheck,postcheck,rollback-precheck}.sql`.
> Los catorce controles del precheck devolvieron 0 sobre una tabla de cero filas,
> de modo que no hubo migración de datos ni normalización material que aplicar.
> Este párrafo registra la ejecución; no reabre ni modifica la decisión.

Requiere `ALTER TABLE` sobre `fal_persona` y la actualización del mismo bloque de
`database/ddl/create-bod-faltas-domain.sql`, en el mismo trabajo. Ningún paso se
ejecuta en el slice que cierra esta decisión.

El plan **no asume la tabla vacía**. Que hoy tenga cero filas es un hecho
coyuntural verificado, no un contrato: el orden debe ser correcto con datos.

| # | Paso | Naturaleza |
|---|---|---|
| 1 | Precheck: conteo de filas; `MAX(nro_doc)` y su longitud decimal; `tipo_documento` distintos; filas que violarían cada `CHECK` nuevo; duplicados del `doc_key` futuro; filas con `prefijo_cuit_cuil` o `digito_verificador` NULL y `tipo_documento IN (2,3)` | read-only, bloqueante |
| 2 | Backup lógico de la tabla completa, incluido `doc_key` materializado | reversible |
| 3 | `DROP INDEX uq_fal_persona_tipo_doc_key` | reversible |
| 4 | `DROP COLUMN doc_key` | reversible: es derivada |
| 5 | `DROP CONSTRAINT chk_fal_persona_nro_doc` | reversible |
| 6 | `MODIFY nro_doc VARCHAR(12) NULL` | **punto de atención**: la conversión `INT → VARCHAR` es textualmente exacta para los datos numéricos existentes, porque MariaDB los renderiza en decimal sin ceros a la izquierda y esos ceros nunca existieron en `INT UNSIGNED` |
| 7 | `MODIFY tipo_documento SMALLINT NULL` | reversible mientras ninguna fila tenga NULL |
| 8 | Normalización de los valores existentes a forma canónica: mayúsculas y sin separadores; y descomposición de todo CUIT/CUIL cuyos once dígitos estuvieran en `nro_doc` | DML de migración; sobre tabla vacía es no-op y debe existir igual |
| 9 | `DROP CONSTRAINT chk_fal_persona_prefijo` y recrearlo con `BETWEEN 10 AND 99` | reversible |
| 10 | `ADD COLUMN doc_key VARCHAR(12) AS (...) STORED` con la expresión de la sección 6 | derivada |
| 11 | `ADD UNIQUE KEY uq_fal_persona_tipo_doc_key (tipo_documento, doc_key)` | falla si el paso 1 no detectó duplicados |
| 12 | `ADD CONSTRAINT` de los seis `CHECK` de la sección 7 | falla si el paso 8 quedó incompleto |
| 13 | Postcheck: mismo conteo de filas que el paso 1; cero violaciones de cada `CHECK`; `doc_key` NULL exactamente donde `tipo_documento IS NULL`; cero duplicados de `(tipo_documento, doc_key)`; los tres componentes de CUIT/CUIL informados en toda fila con `tipo_documento IN (2,3)` | read-only, bloqueante |

**Rollback.** Se ejecuta en orden inverso: bajar los `CHECK` nuevos, bajar el
UNIQUE, bajar `doc_key`, restituir `nro_doc INT UNSIGNED NOT NULL` y
`tipo_documento SMALLINT NOT NULL`, restituir `chk_fal_persona_nro_doc`, recrear
`doc_key` con la expresión anterior y recrear el UNIQUE anterior. La restitución de
los datos, si hiciera falta, sale del backup del paso 2.

**Ventana de rollback y punto de no retorno.** El rollback es posible mientras
ninguna fila tenga `nro_doc` con un carácter no numérico y ninguna fila tenga
`tipo_documento IS NULL`. La primera fila que incumpla cualquiera de las dos
condiciones cierra la ventana: `INT UNSIGNED NOT NULL` no puede representarla y el
retorno pasa a ser con pérdida. Por eso el rollback pertenece a la misma ventana de
cambio que el forward y no a una posterior.

`prefijo_cuit_cuil` y `digito_verificador` **no se eliminan**: la estructura de tres
componentes es justamente lo que esta decisión ratifica. No se plantea, por lo
tanto, la reconstrucción del documento completo por pérdida de componentes.

#### 11. Guardrails

`SpecAsSourceGuardrailTest.G30PersonaDocumento` fija esta decisión, la prohibición
de `tipo_doc` como nombre físico, la opcionalidad conjunta, `VARCHAR(12)`, el
soporte alfanumérico, la estructura de CUIT/CUIL, `doc_key` sin `COALESCE` ni
`LPAD`, el UNIQUE, la coherencia con la persona parcial y la separación entre
modelo objetivo y esquema ejecutado.

Se invierten **en el mismo trabajo que ejecute el `ALTER`, nunca antes**:
`DdlPhysicalParityTest.falPersona_documentoEstructurado`, que hasta ese momento
afirma `tipo_documento SMALLINT` y `nro_doc INT UNSIGNED` del esquema ejecutado. Su
aserción de que `tipo_doc` está ausente **no** se invierte: sigue siendo verdadera
en el modelo objetivo. `MariaDbLogicalModelParityGuardrailTest.FalPersona` y
`DdlPhysicalParityTest.falPersona_sinVersionRow` no cambian.

La inversión se ejecutó en `WAVE 1A2-B1.5B0-B`, junto con el `ALTER`. Ese trabajo
sumó además `DdlPhysicalParityTest.falPersona_docKeySinRelleno` y
`DdlPhysicalParityTest.falPersona_checksDelDocumento`, que fijan la expresión de
`doc_key` sin `COALESCE` ni `LPAD` y los seis `CHECK` de la sección 7.

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

### `DECISION_DDL-DEP-01` — `fal_dependencia`: regla autorreferencial `id_dep_padre <> id` sin `CHECK` físico

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | Retirar el `CONSTRAINT chk_dep_no_self CHECK (id_dep_padre IS NULL OR id_dep_padre <> id)` del DDL físico de `fal_dependencia`. La regla funcional (una dependencia no puede ser su propio padre) no cambia; su enforcement se desplaza a `FalDependencia.setIdDepPadre()` (dominio), que lanza `IllegalArgumentException` cuando `idDepPadre` coincide con el propio `idDep`. |
| **Fundamento** | Detectado al ejecutar `create-bod-faltas-domain.sql` contra `sb_faltas_db` (MariaDB 12.3.2, slice `MARIADB-BASELINE-EXEC-001`): `ERROR 1901 (HY000): Function or expression 'AUTO_INCREMENT' cannot be used in the CHECK clause of `id``. MariaDB no admite que una columna `AUTO_INCREMENT` participe en una restricción `CHECK`, sea directa o vía otra columna comparada contra ella. El `CHECK` original venía de `SPEC-MODEL-DDL-CLOSURE-001`, decidido antes de validar la ejecución material contra el motor real. Mismo patrón ya vigente para otras autorreferencias del dominio (`DECISION_DDL-PAGO-03`: `FalActaObligacionPago.setObligacionReemplazadaId`). |
| **Consecuencia Java** | `FalDependencia.setIdDepPadre(Long)` valida `idDepPadre != null && idDepPadre.equals(this.idDep)` y lanza `IllegalArgumentException`. Sin cambio de tipos ni de campos. |
| **Consecuencia DDL futura** | `fal_dependencia` sin `CONSTRAINT chk_dep_no_self`. Comentarios de columna/tabla documentan la regla y su enforcement en aplicación. |
| **Tests/guardrails** | `DdlPhysicalParityTest.F2.falDependencia_sinCheckFisicoAutoReferencial` (ausencia del `CHECK` físico); `DependenciaTest.IdDepPadreAutoReferencialTest` (autorreferencia rechazada, `null` aceptado, id diferente aceptado). |
| **Historial de enmienda** | 2026-08-02, slice `MARIADB-BASELINE-EXEC-001`: corrección detectada durante el primer forward contra `sb_faltas_db`; sin ejecución previa exitosa del `CHECK` en ningún entorno real. |

---

## Decisiones transversales nuevas (slice DDL-CLOSURE-001-R1)

### `DECISION_DDL-EXEC-01` — Mecanismo de ejecución DDL

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | Flyway = NO. Liquibase = NO. DDL automático al iniciar Spring = NO, en ningún entorno. Ejecución SQL = manual por el usuario desde HeidiSQL (u otro cliente). Los scripts futuros deben vivir versionados en Git fuera del classpath/runtime (p. ej. bajo `database/`). Spring JDBC puede conectar y operar; Spring no crea ni actualiza el esquema. **Producción:** ejecución manual y deliberada, con autorización humana explícita, desde HeidiSQL (u otro cliente). **Sandbox `sb_faltas_db`, únicamente:** ejecución automatizada autorizada mediante un slice explícito (p. ej. `MARIADB-DDL-EXEC-001`, `MARIADB-BASELINE-EXEC-001`) que incluya precheck, backup, allowlist, protección de datos preexistentes, postcheck, rollback, segunda instalación y evidencia. No se extiende a producción ni habilita ejecución automática al iniciar Spring. |
| **Fundamento** | Entorno de proyecto actual: MariaDB 12.3.2, Windows x64, InnoDB, utf8mb4. Control total sobre cuándo y cómo se ejecuta el DDL. Sin riesgo de ejecución automática en entornos no deseados. |
| **Consecuencia Java** | Sin cambio. Spring Boot no incluirá `spring.datasource.initialize=true` ni equivalente. |
| **Consecuencia DDL futura** | Scripts SQL versionados en Git (fuera de `src/`). Sin runner automático. |
| **Tests/guardrails** | `G22CleanRoomYSeeding.ddl_execution_seeding_doc_presente` (verifica que el documento `50-persistence/ddl-execution-and-test-seeding.md` existe y contiene la instrucción). |
| **Historial de enmienda** | 2026-08-02: ampliación de alcance sandbox, slices `MARIADB-DDL-EXEC-001` y `MARIADB-BASELINE-EXEC-001`. Redacción original preservada arriba salvo la distinción producción/sandbox. |

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

## Decisiones físicas — integración EDA Faltas↔Firmas (`SPEC-FIRMAS-EDA-CLOSURE-001`)

> Estas decisiones documentan el modelo físico de la integración EDA
> Faltas↔Firmas (`DEC-EDA-016`/`020`/`021`/`022`,
> `../10-domain/firma-notificacion-fallo.md`,
> `firma-eda-physical-model.md`). Se registran con encabezado `####` (no
> `###`) para preservar intacto el conteo cerrado de 27 decisiones
> `DECISION_DDL-*` de la sección "Cerradas" anterior
> (`G-21CierreDDL.exactamente_24_decisiones_cerradas`); no reabren, alteran
> ni reemplazan ninguna de esas 27. `PHYSICAL_MODEL_COMPLETE`, `DDL_READY`,
> `DDL_NOT_EXECUTED`.

#### `DECISION_DDL-FIRMAREQ-01` — `fal_documento_firma_req`: lifecycle completo, OCC y correlación EDA

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | Extender de forma aditiva `estado_firma_req` (5→9 valores: `+6=CANCELACION_SOLICITADA, +7=CANCELADO, +8=RECHAZADO, +9=RECHAZADO_CONTRATO`), sin renombrar los 5 valores existentes. Agregar `version_row INT NOT NULL DEFAULT 0` (OCC, ausente hasta este cierre), `solicitud_firma_id CHAR(36) NULL`, `revision_solicitud SMALLINT NOT NULL DEFAULT 0`, `fh_solicitud`/`fh_aceptacion`/`fh_inicio_firma`/`vence_en`/`fh_terminal DATETIME(6) NULL`, `motivo_terminal_firma_req SMALLINT NULL`, `detalle_terminal_firma_req VARCHAR(255) NULL`. |
| **Fundamento** | Único lifecycle físico para todo el ciclo EDA de un requisito de firma, reutilizando al máximo el catálogo ya cerrado en 8C-4; sin proliferación de tablas paralelas. Detalle completo de motivos, mapeo y regla de carrera en `10-domain/firma-notificacion-fallo.md`. |
| **Consecuencia Java** | `EstadoFirmaReq` gana 4 constantes nuevas (aditivo). `FalDocumentoFirmaReq` gana los campos correspondientes (slice de código de roadmap; `IMPLEMENTATION_NOT_STARTED`). |
| **Consecuencia DDL futura** | Ver `firma-eda-physical-model.md` §1 (columnas, catálogo, constraints, índices, backfill exactos). |
| **Tests/guardrails** | No aplica a Java productivo en este cierre documental; el detalle de tests queda fijado en el slice de código de implementación conforme al roadmap (`IMPLEMENTATION_NOT_STARTED`). |

#### `DECISION_DDL-FIRMA-02` — `fal_documento_firma`: `hash_salida`, revisiones, actor técnico y evidencia PKI

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | Agregar `hash_salida VARCHAR(128) NULL`, `revision_entrada`/`revision_salida SMALLINT NULL`, columnas de actor técnico (`actor_tecnico_tipo`, `actor_tecnico_id`, `actor_tecnico_app`, `actor_tecnico_key_fingerprint`, `correlation_id`, `event_id`, `received_at`) y metadata PKI mínima (`perfil_firma`, `algoritmo_hash VARCHAR(16) NULL` — nunca `NOT NULL DEFAULT`, `DEC-EDA-029` —, `resultado_firma_id`, `evidencia_id`, y los 12 campos de certificado/cadena/revocación/sello de tiempo/validación de `firma-eda-physical-model.md` §2.3), más `CONSTRAINT chk_firma_eda_completo` condicional (§2.4: exige el conjunto mínimo de columnas EDA únicamente cuando `resultado_firma_id IS NOT NULL`). `hash_firma` no cambia de nombre ni de significado (sigue siendo `hashEntrada`). |
| **Fundamento** | Resuelve la ausencia de persistencia de `hashSalida` identificada en `DEC-EDA-013`; persiste el actor técnico M2M sin sustituir nunca a `id_user_firma` (`DEC-EDA-021`); persiste evidencia PKI mínima verificable localmente sin depender de conservar PDFs intermedios (`DEC-EDA-023`); `algoritmo_hash` nullable evita completar retroactivamente filas históricas con un algoritmo no demostrado, mientras el `CHECK` condicional asegura completitud para toda fila del circuito EDA nuevo (`DEC-EDA-029`). |
| **Consecuencia Java** | `FalDocumentoFirma` gana los campos correspondientes (slice de código de roadmap; `IMPLEMENTATION_NOT_STARTED`). |
| **Consecuencia DDL futura** | Ver `firma-eda-physical-model.md` §2 (tipos, nullability, constraints, índices, backfill exactos). |
| **Tests/guardrails** | No aplica a Java productivo en este cierre documental; el detalle de tests queda fijado en el slice de código de implementación conforme al roadmap (`IMPLEMENTATION_NOT_STARTED`). |

#### `DECISION_DDL-BUS-01` — Bus de eventos municipal: modelo mínimo aprobado (`bus_evento`, `fal_outbox`, `fal_inbox`)

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | El bus central está compuesto únicamente por una tabla central append-only (`bus_evento`) más Outbox local del productor (`fal_outbox`) e Inbox local del consumidor (`fal_inbox`, `PK = bus_evento_id`), con columnas, claves e índices exactos según `firma-eda-physical-model.md` §6. Sin fan-out central, sin tabla de entregas, sin suscripciones, sin rutinas ni cuenta definidora, sin vistas diagnósticas. Códigos compactos (`productor_codigo VARCHAR(8)`, `evento_codigo VARCHAR(16)`, `agregado_codigo VARCHAR(12)`) en vez de nombres extensos repetidos; `evento_id`/`correlation_id`/`causation_id` como `BINARY(16)`; `payload` como `JSON` nativo. `bus_evento_id` (`AUTO_INCREMENT`) es un cursor técnico local a `bus_evento`; `evento_id` es la identidad lógica estable, idéntica en `bus_evento`/`fal_outbox`/`fal_inbox`. Idempotencia de publicación por `UNIQUE(evento_id)` en `bus_evento`/`fal_outbox`, e idempotencia de ingestión por `UNIQUE(evento_id)` en `fal_inbox` (`uq_fal_inbox_evento_id`; nunca `UNIQUE(consumer, event_id)`, porque cada Inbox local no tiene columna `consumer`), no por fingerprint calculado en SQL. `fir_outbox`/`fir_inbox` quedan fuera de este paquete DDL (bounded context Firmas, otra base de datos; paquete ejecutable propio en `database/ddl/firmas/`, `DECISION_DDL-FIRMAS-01`). |
| **Fundamento** | Outbox local + bus central append-only es el único mecanismo que garantiza atomicidad publicación↔dominio sin 2PC/XA (`30-integrations/municipal-eventing.md`, `DEC-EDA-011`/`019`/`022`); un único punto de escritura sin fan-out, entregas ni suscripciones tabulares mantiene el mínimo privilegio necesario sin superficie de mantenimiento adicional. |
| **Consecuencia Java** | Ninguna en este cierre (`IMPLEMENTATION_NOT_STARTED`); slice de código de roadmap para el relay y los consumidores locales. |
| **Consecuencia DDL futura** | Ver `firma-eda-physical-model.md` §6, §12 (columnas, índices, orden de creación, sin ciclos de FK). |
| **Tests/guardrails** | No aplica a Java productivo en este cierre documental; el detalle de tests queda fijado en el slice de código de implementación conforme al roadmap (`IMPLEMENTATION_NOT_STARTED`). |

#### `DECISION_DDL-RESULTADO-01` — `fal_documento_firma_resultado`: staging temporal del candidato firmado

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | Crear tabla nueva `fal_documento_firma_resultado` (columnas, catálogo `estado_resultado` de 5 valores, FK a `fal_documento`/`fal_documento_firma_req`, índices y checks exactos en `firma-eda-physical-model.md` §4). El `PUT .../resultados/{resultadoFirmaId}` escribe únicamente en esta tabla; nunca en `fal_documento_firma` ni en `fal_documento`. |
| **Fundamento** | El único punto de aplicación del efecto de dominio de una firma es el consumidor de `DocumentoFirmado`; el `PUT` HTTP necesita una tabla propia para su recibo técnico idempotente y su TTL de 24 horas, sin mezclar staging con historial de dominio (`../30-integrations/firma-binary-transfer.md` §0bis). |
| **Consecuencia Java** | Ninguna en este cierre (`IMPLEMENTATION_NOT_STARTED`); slice de código de roadmap para el controller de carga y el consumidor de `DocumentoFirmado`. |
| **Consecuencia DDL futura** | Ver `firma-eda-physical-model.md` §4 (columnas, catálogo, constraints, índices, transiciones exactas). |
| **Tests/guardrails** | No aplica a Java productivo en este cierre documental; el detalle de tests queda fijado en el slice de código de implementación conforme al roadmap (`IMPLEMENTATION_NOT_STARTED`). |

#### `DECISION_DDL-DOC-02` — `fal_documento.revision_artefacto`: revisión física del único PDF vigente

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | Agregar `revision_artefacto INT NOT NULL DEFAULT 0` a `fal_documento`, con `CHECK (revision_artefacto >= 0)`. Incrementa de `0` a `1` en la primera preparación y en exactamente `1` por cada firma válida aplicada. |
| **Fundamento** | Los contratos `revisionArtefacto`/`revisionEntrada`/`revisionSalida` (`../30-integrations/firma-binary-transfer.md`, `../40-api/firma-event-contracts.md`) no tenían columna física de respaldo en `fal_documento`; sin ella, la validación `revisionEntrada == revision vigente` no es verificable en base. |
| **Consecuencia Java** | Ninguna en este cierre (`IMPLEMENTATION_NOT_STARTED`); slice de código de roadmap. |
| **Consecuencia DDL futura** | Ver `firma-eda-physical-model.md` §3. |
| **Tests/guardrails** | No aplica a Java productivo en este cierre documental; el detalle de tests queda fijado en el slice de código de implementación conforme al roadmap (`IMPLEMENTATION_NOT_STARTED`). |

#### `DECISION_DDL-BUS-02` — Checkpoint local por consumidor y tabla local de errores de Inbox

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | Crear `fal_bus_checkpoint` (tabla local singleton, `checkpoint_id = 1` fijo por `CHECK`, `ultimo_evento_id` avanza solo hacia adelante) y `fal_inbox_error` (tabla local de errores de procesamiento de `fal_inbox`, FK a `fal_inbox(bus_evento_id)`, `error_detail VARCHAR(255)` sin stack trace). Cada consumidor lee `bus_evento` por `bus_evento_id > ultimo_evento_id`, copia a su Inbox los eventos relevantes por `evento_codigo` y avanza el checkpoint en la misma transacción local. Sin tabla catálogo de estados: `status`/`si_error` documentados como enum/constante. |
| **Fundamento** | Sin un checkpoint local persistido, cada consumidor tendría que recorrer `bus_evento` completo en cada ciclo de lectura o inventar su propio mecanismo de marcado sobre la tabla central, violando la regla de que `bus_evento` no registra estados de consumo; sin una tabla de errores separada, la fila principal de Inbox mezclaría progreso funcional con diagnóstico técnico. |
| **Consecuencia Java** | Ninguna en este cierre (`IMPLEMENTATION_NOT_STARTED`); slice de código de roadmap para el lector local y el worker de procesamiento. |
| **Consecuencia DDL futura** | Ver `firma-eda-physical-model.md` §6.4, §6.6. |
| **Tests/guardrails** | No aplica a Java productivo en este cierre documental; el detalle de tests queda fijado en el slice de código de implementación conforme al roadmap (`IMPLEMENTATION_NOT_STARTED`). |

#### `DECISION_DDL-BUS-03` — Transporte JDBC/SQL directo del bus central y cuentas SQL fijas de mínimo privilegio

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | El bus central (única tabla `bus_evento`) se accede exclusivamente por JDBC/SQL; nunca por HTTP/`X-Api-Key`, y sin rutinas `SQL SECURITY DEFINER` ni cuenta definidora. Cinco usuarios MariaDB de mínimo privilegio (`bus_publisher_faltas`, `bus_publisher_firmas`, `bus_consumer_faltas`, `bus_consumer_firmas`, `bus_maintenance`), definidos sin contraseñas en `database/ddl/grants-firmas-eda-bus.sql`. Los 2 publishers reciben `INSERT` **column-level** sobre las 12 columnas de `bus_evento` distintas de `bus_evento_id` (nunca `INSERT` de tabla completa: el cliente jamás escribe el cursor técnico, que siempre lo genera `AUTO_INCREMENT`); los 2 consumers reciben `SELECT` de tabla completa; `bus_maintenance` recibe `SELECT, DELETE` de tabla completa. |
| **Fundamento** | El transporte del bus usa JDBC/SQL directo y grants mínimos porque el bus físico son tablas MariaDB sin ninguna API HTTP (`../30-integrations/eventing-operations.md` §1-§2bis); grants directos sobre una única tabla logran el mínimo privilegio sin necesitar una capa de rutinas `SECURITY DEFINER`. |
| **Consecuencia Java** | Ninguna en este cierre (`IMPLEMENTATION_NOT_STARTED`); el datasource JDBC dedicado del relay/consumidor es slice de código de roadmap. |
| **Consecuencia DDL futura** | Ver `firma-eda-physical-model.md` §9-§10, `database/ddl/grants-firmas-eda-bus.sql`. |
| **Tests/guardrails** | No aplica a Java productivo en este cierre documental; el detalle de tests queda fijado en el slice de código de implementación conforme al roadmap (`IMPLEMENTATION_NOT_STARTED`). |

#### `DECISION_DDL-FIRMAS-01` — Paquete DDL ejecutable propio del bounded context Firmas

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | `fir_outbox`/`fir_inbox` tienen un paquete DDL ejecutable propio (`database/ddl/firmas/create-firmas-eda-outbox-inbox.sql`, `rollback-firmas-eda-outbox-inbox.sql`, `database/diagnostics/firmas/verify-firmas-eda-outbox-inbox-precheck.sql`, `-postcheck.sql`), sin `USE` de ninguna base fija (el precheck exige `DATABASE() IS NOT NULL`; el operador selecciona la base destino explícitamente al conectarse). Reemplaza el archivo de referencia comentado anterior. |
| **Fundamento** | Un archivo íntegramente comentado no es un paquete DDL ejecutable para el equipo de Firmas; el nombre de la base de datos de Firmas no es un objeto lógico de este repositorio y no corresponde declararlo "a definir" en la spec de Faltas — el precheck lo resuelve sin inventarlo. |
| **Consecuencia Java** | Ninguna en este repositorio (Firmas es otro repositorio; `IMPLEMENTATION_NOT_STARTED` desde la perspectiva de Faltas). |
| **Consecuencia DDL futura** | Ver `firma-eda-physical-model.md` §9. |
| **Tests/guardrails** | No aplica a Java productivo de este repositorio. |

#### `DECISION_DDL-FIRMAREQ-02` (`DEC-EDA-030`) — Preparación persistida e idempotente por requisito

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | Agregar a `fal_documento_firma_req`: `revision_artefacto_preparada INT NULL`, `hash_entrada_preparado VARCHAR(128) NULL`, `content_length_preparado BIGINT NULL`, `fh_preparacion DATETIME(6) NULL` — todos NULL o todos informados (`chk_firma_req_preparacion`) — más `chk_firma_req_rev_preparada`/`chk_firma_req_content_length_preparado`. |
| **Fundamento** | `preparar-artefacto` prometía idempotencia y una revisión preparada sin persistir qué PDF exacto fue expuesto a Firmas; sin estas columnas esa promesa no era verificable contra base, solo contra memoria del proceso HTTP. |
| **Consecuencia Java** | Ninguna en este cierre (`IMPLEMENTATION_NOT_STARTED`); slice de código de roadmap para el endpoint de preparación y de descarga. |
| **Consecuencia DDL futura** | Ver `firma-eda-physical-model.md` §1.1bis. |
| **Tests/guardrails** | No aplica a Java productivo en este cierre documental. |

#### `DECISION_DDL-DOC-03` (`DEC-EDA-031`) — Backfill seguro de `revision_artefacto` por consistencia `storage_key`/`hash_docu`

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | El `UPDATE` de backfill de `revision_artefacto` fija `1` únicamente cuando `storage_key IS NOT NULL OR hash_docu IS NOT NULL`; nunca `1` indiscriminado. Precheck obligatorio reporta y bloquea la ejecución si existe una fila con exactamente uno de esos dos campos informado (inconsistencia preexistente ajena a este cierre). |
| **Fundamento** | Un `DEFAULT 0` de `ADD COLUMN` es incorrecto para cualquier documento que ya tenga contenido físico cargado antes de la migración; fijar `1` sin condición sería igualmente incorrecto para documentos realmente vacíos. |
| **Consecuencia Java** | Ninguna en este cierre (`IMPLEMENTATION_NOT_STARTED`). |
| **Consecuencia DDL futura** | Ver `firma-eda-physical-model.md` §3. |
| **Tests/guardrails** | No aplica a Java productivo en este cierre documental. |

#### `DECISION_DDL-RESULTADO-02` (`DEC-EDA-032`) — Swap de storage recuperable tras crash y limpieza durable

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | Renombrar/sustituir `storage_key_temporal` por `storage_key_candidato` (clave interna, inmutable y ya definitiva desde el `PUT`, nunca una ruta que requiera `rename`/`copy` posterior). Agregar `storage_key_anterior VARCHAR(196) NULL`, `cleanup_status SMALLINT NOT NULL DEFAULT 1` (catálogo `1=NO_APLICA, 2=PENDIENTE, 3=COMPLETADO, 4=RETRY, 5=QUARANTINED`), `cleanup_attempts`, `cleanup_next_attempt_at`, `cleanup_last_error`, `cleanup_completed_at`. El swap de `fal_documento.storage_key` ocurre en la misma transacción que el efecto de dominio; la eliminación física del PDF anterior ocurre después del commit, con reintentos y cuarentena auditables. |
| **Fundamento** | El nuevo PDF debe existir físicamente antes de que la transacción cambie el puntero de `fal_documento`; un crash entre el commit y la limpieza no debe perder la clave del PDF anterior ni dejar `fal_documento.storage_key` apuntando a un objeto inexistente. |
| **Consecuencia Java** | Ninguna en este cierre (`IMPLEMENTATION_NOT_STARTED`); slice de código de roadmap para el job de limpieza asíncrona. |
| **Consecuencia DDL futura** | Ver `firma-eda-physical-model.md` §4, §5bis. |
| **Tests/guardrails** | No aplica a Java productivo en este cierre documental. |

#### `DECISION_DDL-RESULTADO-03` (`DEC-EDA-033`) — Recibo del `PUT` vinculado criptográfica y técnicamente al evento

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | Agregar a `fal_documento_firma_resultado`: `referencia_firma_ext VARCHAR(64) NOT NULL`, `evidencia_id VARCHAR(128) NOT NULL`, `upload_actor_id VARCHAR(64) NOT NULL`, `upload_actor_app VARCHAR(32) NOT NULL`, `upload_key_fingerprint VARCHAR(64) NOT NULL` — los tres últimos derivados exclusivamente de la `X-Api-Key` autenticada, nunca del JSON de Firmas. Al consumir `DocumentoFirmado`, exigir igualdad exacta con estos campos y con `resultadoFirmaId`/`solicitudFirmaId`/`documentoId`/requisito/revisiones/hashes; una divergencia es un rechazo permanente, no un retry técnico. |
| **Fundamento** | Sin esta metadata, el consumidor de `DocumentoFirmado` confiaba ciegamente en el payload del evento para vincularlo con la carga HTTP previa, sin verificación cruzada contra un recibo autenticado. |
| **Consecuencia Java** | Ninguna en este cierre (`IMPLEMENTATION_NOT_STARTED`). |
| **Consecuencia DDL futura** | Ver `firma-eda-physical-model.md` §4, §5bis paso 1. |
| **Tests/guardrails** | No aplica a Java productivo en este cierre documental. |

#### `DECISION_DDL-FIRMA-03` (`DEC-EDA-034`) — `chk_firma_eda_completo` integral y FK física a `fal_documento_firma_resultado`

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | Completar `chk_firma_eda_completo` con el conjunto íntegro de columnas de evidencia (perfil, evidencia, certificado completo, `tsa_nombre`, `version_validador`, rangos de enums, `revision_salida = revision_entrada + 1`) cuando `resultado_firma_id IS NOT NULL`. Agregar `fk_firma_resultado FOREIGN KEY (resultado_firma_id) REFERENCES fal_documento_firma_resultado (resultado_firma_id)`, creada en una sentencia separada después de que esa tabla exista. |
| **Fundamento** | La versión anterior del `CHECK` (`DEC-EDA-029`) cubría un subconjunto de 12 columnas; el resto de la evidencia física de §2.3 quedaba sin exigencia real, y `resultado_firma_id` era una referencia únicamente lógica, no verificada por el motor. |
| **Consecuencia Java** | Ninguna en este cierre (`IMPLEMENTATION_NOT_STARTED`). |
| **Consecuencia DDL futura** | Ver `firma-eda-physical-model.md` §2.4, §12 paso 5. |
| **Tests/guardrails** | No aplica a Java productivo en este cierre documental. |

#### `DECISION_DDL-FIRMA-04` (`DEC-EDA-035`) — Perfil PKI único `PAdES-B-LT`, sin excepción `B-T`

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | `CONSTRAINT chk_firma_perfil_unico CHECK (perfil_firma IS NULL OR perfil_firma = 'PAdES-B-LT')`, sin condicionar a `resultado_firma_id`. El catálogo de observaciones permitidas para `VALIDO_CON_OBSERVACIONES_PERMITIDAS` queda reducido a `TSA_NO_CUALIFICADA_CONFIABLE` y `REVOCACION_POR_CRL_SIN_OCSP`. |
| **Fundamento** | La versión anterior tenía tres observaciones permitidas, incluida una que aceptaba `PAdES-B-T` (sin LTV embebido) como perfil tolerado, lo cual rebajaba el perfil normativo único. |
| **Consecuencia Java** | Ninguna en este cierre (`IMPLEMENTATION_NOT_STARTED`). |
| **Consecuencia DDL futura** | Ver `firma-eda-physical-model.md` §2.4bis. |
| **Tests/guardrails** | No aplica a Java productivo en este cierre documental. |

#### `DECISION_DDL-RESULTADO-04` (`DEC-EDA-036`) — Resultado PKI inválido: cierre local exacto, sin reuso de `resultadoFirmaId`

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | Cuando la PKI reportada por `DocumentoFirmado` es inválida (o la metadata diverge del recibo, `DEC-EDA-033`), la transacción local de procesamiento del consumidor transiciona `fal_documento_firma_resultado` a `RECHAZADO` (nunca queda en `VALIDADO`), registra un código estructurado, deja el requisito sin satisfacer y no reemplaza el PDF vigente. Es un **rechazo funcional de dominio**, no un error técnico: `fal_inbox` cierra en `status = PROCESADO`, `si_error = FALSE`, sin insertar fila en `fal_inbox_error`. `bus_evento` no tiene ningún estado de entrega ni cuarentena central: no existe "entrega central en cuarentena" para este ni ningún otro evento. Ese `resultado_firma_id` queda agotado: Firmas debe producir un nuevo resultado y un nuevo `evento_id` tras corregir la causa. |
| **Fundamento** | Una PKI inválida detectada al consumir el evento no puede dejar el candidato en un estado ambiguo (`VALIDADO`); la base local debe reflejar el rechazo de forma idempotente. El rechazo es exclusivamente un hecho de negocio local (Faltas): `bus_evento` es append-only e inmutable, sin estado de consumo ni de cuarentena por consumidor. |
| **Consecuencia Java** | Ninguna en este cierre (`IMPLEMENTATION_NOT_STARTED`). |
| **Consecuencia DDL futura** | Ver `firma-eda-physical-model.md` §5bis paso 1a, `../10-domain/firma-notificacion-fallo.md`. |
| **Tests/guardrails** | No aplica a Java productivo en este cierre documental. |

#### `DECISION_DDL-BUS-04` — Grants sin placeholders y precheck de principals

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | Eliminar `__PASSWORD_PLACEHOLDER__`/`CREATE USER` del DDL versionado; el aprovisionamiento de los 5 usuarios MariaDB es responsabilidad del secret manager/pipeline, fuera del repositorio. `grants-firmas-eda-bus.sql` concede exclusivamente `INSERT`/`SELECT`/`SELECT, DELETE` directo sobre `bus_evento` (§10), y solo se ejecuta después de que `database/diagnostics/verify-firmas-eda-bus-principals-precheck.sql` confirma que los usuarios ya existen con host permitido y `REQUIRE SSL`. Rollback ejecutable de grants en `database/ddl/rollback-grants-firmas-eda-bus.sql`. El rollback de Firmas (§8) usa un precheck propio (`database/diagnostics/firmas/verify-firmas-eda-outbox-inbox-rollback-precheck.sql`) que no reutiliza el precheck de creación. |
| **Fundamento** | Placeholders de contraseña en DDL versionado son un riesgo de seguridad y una promesa vacía (nunca se completan de forma segura en un repositorio de código); un rollback no puede validar la misma precondición ("las tablas no existen") que un `CREATE`, que es exactamente la contraria. |
| **Consecuencia Java** | Ninguna en este cierre (`IMPLEMENTATION_NOT_STARTED`). |
| **Consecuencia DDL futura** | Ver `firma-eda-physical-model.md` §10, §14. |
| **Tests/guardrails** | No aplica a Java productivo en este cierre documental. |

#### `DECISION_DDL-COORD-01` — Tipo físico único para coordenadas geográficas

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | Una latitud o longitud en grados decimales tiene un único tipo físico en todo el dominio: `DECIMAL(10,7) NULL`. Aplica a `fal_persona_domicilio.lat`/`.lon` (ya conformes) y a las cuatro coordenadas de `fal_acta`: `lat_captura`, `lon_captura`, `lat_infr`, `lon_infr`, que el baseline había creado como `DECIMAL(12,8)`. `precision_captura_m` queda expresamente fuera: es una distancia en metros, no una coordenada, y conserva `DECIMAL(8,2)`. Ejecutado por `database/ddl/alter-coord-type-unify-001-forward.sql`; reversión en `alter-coord-type-unify-001-rollback.sql`. |
| **Fundamento** | El mismo dato no puede tener dos tipos. `DECIMAL(10,7)` era ya la decisión cerrada para domicilios (`SPEC-MODEL-DDL-CLOSURE-001`) y `mariadb-logical-model.md` pedía coherencia con esa tabla; el baseline ejecutado no la siguió para `fal_acta`. Además `DECIMAL(10,7)` es el tipo adecuado: 3 dígitos enteros cubren el rango real (latitud −90..90, longitud −180..180) y 7 decimales dan ~1,1 cm de resolución. `DECIMAL(12,8)` reservaba un cuarto dígito entero que sólo admite valores imposibles. Para metros, en cambio, `DECIMAL(8,2)` es correcto: hasta 999.999,99 m con resolución de centímetro. |
| **Consecuencia Java** | Ninguna. `FalActa.latCaptura`, `lonCaptura`, `latInfr`, `lonInfr` y `precisionCapturaM` siguen siendo `Double`; el rango y la resolución de `DECIMAL(10,7)` los representan sin pérdida. |
| **Consecuencia DDL futura** | `create-bod-faltas-domain.sql` declara las seis coordenadas del dominio como `DECIMAL(10,7)`. Toda columna de coordenadas que se agregue en adelante usa ese tipo. |
| **Tests/guardrails** | `DdlPhysicalParityTest`: `fal_acta: las cuatro coordenadas son DECIMAL(10,7) - no DECIMAL(12,8)`, `fal_acta: precision_captura_m sigue siendo DECIMAL(8,2) - son metros, no grados` y el ya existente `fal_persona_domicilio: lat/lon son DECIMAL(10,7) - no DECIMAL(12,8)`. |

#### `DECISION_DDL-OBS-COMMENT-01` — `fal_observacion.origen_observacion`: etiquetas obsoletas en el `COMMENT`

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | El `COMMENT` de `fal_observacion.origen_observacion` describe los tres códigos como `1=OPERADOR, 2=SISTEMA, 3=EXTERNO`, mientras los nombres canónicos del dominio son `1=USUARIO, 2=SISTEMA, 3=INTEGRACION` (`DECISION_OBS-ENUM-01`, `40-domain/observaciones.md` §4.1). Los códigos coinciden: la divergencia es exclusivamente de etiquetas dentro de un comentario y no afecta datos, mapeo ni comportamiento. La corrección del texto se ejecuta con `ALTER TABLE fal_observacion MODIFY COLUMN origen_observacion` y la actualización del literal en `database/ddl/create-bod-faltas-domain.sql`, agrupada con la remediación física de `fal_observacion` (baja de `fk_obs_acta_contexto ... ON DELETE CASCADE` y de los 21 triggers `*_ad_observaciones`) que `DECISION_OBS-01` dejó registrada. No se ejecuta DDL para corregir únicamente un comentario. |
| **Fundamento** | Un comentario de columna es documentación embebida y `DECISION_DDL-COMMENT-01` la exige exacta; un lector que sólo mire el motor concluiría que el código 1 se llama `OPERADOR`. Al mismo tiempo, un `ALTER TABLE` exclusivo para reescribir un comentario consume una ventana de cambio sin reducir riesgo funcional: los tres códigos ya son correctos y el enum Java es la autoridad de nombres. Agrupar la corrección con la remediación física que igualmente requiere DDL sobre esta tabla es la forma de pagar una sola ventana. |
| **Consecuencia Java** | Ninguna. `OrigenObservacion` conserva `USUARIO(1)`, `SISTEMA(2)`, `INTEGRACION(3)`, y `JdbcObservacionRepository` sigue mapeando por `codigo()`. |
| **Consecuencia DDL futura** | El paquete de remediación de `fal_observacion` incluye, además de la baja de la FK con `CASCADE` y de los 21 triggers, el `MODIFY COLUMN` que deja el `COMMENT` con `1=USUARIO, 2=SISTEMA, 3=INTEGRACION`, y la misma corrección en el DDL fuente. Hasta entonces, la autoridad de nombres es `40-domain/observaciones.md` §4.1. |
| **Tests/guardrails** | `G28ObservacionNombresYFrontera.origen_observacion_tabla_canonica` fija los tres pares código/nombre en la spec y su paridad con `OrigenObservacion.values()`; `G28ObservacionNombresYFrontera.comment_fisico_obsoleto_registrado_en_ddl_decisions` verifica que esta decisión siga registrada. La conducta física del código está cubierta por `JdbcObservacionRepositoryIT` (`enumsSeGuardanPorCodigoFisico`, `origenNuloNoRecibeDefault`). |

#### `DECISION_DDL-ACTA-INFR-01` — `fal_acta`: situación de identificación del infractor y nulabilidad de `id_persona_infractor`

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | `fal_acta` gana la columna `situacion_identificacion_infractor SMALLINT NOT NULL`, sin valor por defecto, que persiste el código de `SituacionIdentificacionInfractor` (`10-domain/states-events-catalogs.md`). `id_persona_infractor` pasa de `BIGINT NOT NULL` a `BIGINT NULL`, conservando `fk_acta_persona_infractor` y el índice `ix_fal_acta_persona`. Un `CHECK` fija la coherencia entre ambas: hay persona exactamente cuando la situación es identificación completa o parcial. La ausencia de sujeto identificado se expresa con `NULL`, nunca con una persona centinela, ficticia, con documento inventado ni con una PK reservada. |
| **Fundamento** | El modelo vigente afirma que toda acta tiene sujeto técnico y por eso declara la FK `NOT NULL`. Materialmente eso obliga a inventar una fila de `fal_persona` cuando el infractor está ausente, se niega a identificarse, no aporta datos o los datos son insuficientes, y una persona inventada contamina el padrón, rompe `DECISION_DDL-PERS-01` y afirma un hecho jurídico falso. `ResultadoFirmaInfractor` no cubre el caso: califica la firma, no la presencia ni la identificación. La única representación honesta de "no hay sujeto identificado" es la ausencia de referencia más una calificación explícita del motivo. |
| **CHECK de coherencia** | `CONSTRAINT chk_acta_infractor_coherente CHECK ((situacion_identificacion_infractor IN (1,2) AND id_persona_infractor IS NOT NULL) OR (situacion_identificacion_infractor IN (3,4,5,6) AND id_persona_infractor IS NULL))`. Un `CHECK` de dominio adicional, `chk_acta_situacion_infractor CHECK (situacion_identificacion_infractor BETWEEN 1 AND 6)`, acota los códigos igual que `chk_acta_tipo` acota `tipo_acta`. |
| **Consecuencia Java** | `FalActa` gana el campo `situacionIdentificacionInfractor` de tipo `SituacionIdentificacionInfractor`, obligatorio en el constructor principal igual que `resultadoFirmaInfractor`, e `idPersonaInfractor` pasa a admitir `null` explícitamente. Se crea el enum `SituacionIdentificacionInfractor` con `codigo()` `short` y `fromCodigo(short)`, categoría `EXPLICIT_NUMERIC_CODE`. `LabrarActaCommand` recibe la situación como dato obligatorio del alta. `JdbcActaRepository` suma la columna a su mapeo y pasa de 56 a 57 columnas. |
| **Consecuencia DDL futura** | Para instalaciones limpias, `database/ddl/create-bod-faltas-domain.sql` declara la columna nueva, la nulabilidad de `id_persona_infractor` y los dos `CHECK`. Para una base existente, un paquete forward/rollback independiente en este orden: verificar el conteo de filas; `ALTER TABLE fal_acta ADD COLUMN situacion_identificacion_infractor SMALLINT NULL`; backfill de la situación por cada fila existente; `ALTER TABLE fal_acta MODIFY COLUMN situacion_identificacion_infractor SMALLINT NOT NULL`; `ALTER TABLE fal_acta MODIFY COLUMN id_persona_infractor BIGINT NULL`; recién entonces agregar los dos `CHECK`. El `NOT NULL` se aplica después del backfill y los `CHECK` después de ambos `MODIFY`, porque de lo contrario el motor rechaza las filas preexistentes. El rollback recorre el orden inverso: bajar los dos `CHECK`, restituir `id_persona_infractor BIGINT NOT NULL` y descartar la columna nueva; sólo es aplicable mientras ninguna fila tenga `id_persona_infractor` NULL. La tabla vacía no es un contrato: el paquete debe ser correcto con filas. |
| **Backfill** | Ninguna fila preexistente puede clasificarse por inferencia. Con `id_persona_infractor` informado, la situación de backfill es identificación completa cuando la persona tiene `tipo_documento` y `nro_doc` informados, y parcial en caso contrario. No existe otra rama posible, porque hoy la columna es `NOT NULL`. Un valor por defecto permanente está prohibido: la situación es una calificación jurídica del acto y el alta debe informarla explícitamente. |
| **Aplicación física** | `DDL_EXECUTED` — `WAVE 1A2-B1.5B1`, 2026-08-03, base `sb_faltas_db`, MariaDB 12.3.2. `fal_acta` pasó de 56 a 57 columnas; `situacion_identificacion_infractor` quedó en la posición ordinal 22, inmediatamente antes de `id_persona_infractor`, que pasó a `BIGINT NULL`. Los cuatro `CHECK` de la tabla son `chk_acta_tipo`, `chk_acta_qr_ver`, `chk_acta_situacion_infractor` y `chk_acta_infractor_coherente`. `fk_acta_persona_infractor` conserva `RESTRICT`/`RESTRICT` e `ix_fal_acta_persona` sigue presente. El precheck de catorce controles y el postcheck de doce controles se ejecutaron sin bloqueos ni divergencias, y el rollback versionado se probó de extremo a extremo restituyendo la forma física anterior exacta antes de reaplicar el forward. |
| **Tests/guardrails** | `G29ActaAltaRules` verifica que la spec declare los seis significados sin fusionarlos, la matriz situación/persona, la prohibición de persona centinela y esta decisión. `MariaDbLogicalModelParityGuardrailTest.FalActa.fk_persona_nullable_con_situacion` es la contraparte invertida de la antigua `fk_persona_not_null`: exige `id_persona_infractor BIGINT NULL`, la columna nueva y los dos `CHECK` en `mariadb-logical-model.md`. La conducta física está cubierta por `JdbcActaRepositoryIT` (casos 53 a 63: 57 columnas, round-trip de los seis códigos, matriz, CHECK físicos, FK con columna nullable, paridad InMemory y backfill determinístico). |

#### `DECISION_DDL-ACTA-QR-01` — `fal_acta.codigo_qr`: formato canónico del token y algoritmo de firma

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | El token que se persiste en `codigo_qr` es exactamente `QR0.<id_tecnico>.<version>.<firma-base64url>`, donde `id_tecnico` es el UUID canónico en minúsculas de 36 caracteres, `version` identifica la versión del **formato y del contenido firmado del payload** (`DECISION-QR-INMUTABLE-01`), y la firma es `HMAC-SHA-256` sobre el contenido canónico `QR0.<id_tecnico>.<version>`, codificada en Base64 URL-safe sin relleno. La columna permanece `VARCHAR(128)` y `uq_fal_acta_codigo_qr` se conserva. |
| **Determinismo** | El token es determinístico: el mismo `id_tecnico`, la misma versión y la misma clave producen exactamente el mismo token. Queda prohibido incorporar nonce aleatorio, vector de inicialización aleatorio, marca temporal, `id_acta`, cualquier dato variable del entorno y relleno Base64. |
| **Longitud** | `QR0.` son 4 caracteres, el UUID canónico 36, el separador 1, la versión 1 o 2, el separador 1 y la firma `HMAC-SHA-256` en Base64URL sin relleno exactamente 43. El total queda en torno a 86 caracteres y por lo tanto holgadamente dentro de `VARCHAR(128)`. |
| **Fundamento** | El QR necesita autenticidad, integridad y estabilidad, no confidencialidad. `id_tecnico` puede viajar en claro porque es un identificador técnico opaco: no contiene datos personales ni contenido jurídico. La versión embebida identifica el formato del payload, de modo que un formato futuro sea distinguible del actual sin ambigüedad. La comparación de la firma se hace en tiempo constante. |
| **Contenido prohibido en el token** | documento del infractor, nombre, patente, domicilio, datos del acta y cualquier otra información sensible. El token no transporta estado ni contenido: sólo identidad técnica, versión y firma. |
| **Momento de generación** | El token se genera antes de la primera escritura del acta, a partir de `id_tecnico` y nunca de `id_acta`, de modo que el labrado sin conectividad puede producirlo y el valor permanece estable cuando la persistencia asigna la PK. `QrActaService` es el único responsable de generarlo y validarlo; `ActaService.labrar` obtiene el `id_tecnico`, pide el token, construye `FalActa` con `codigo_qr` informado y sólo entonces invoca `ActaRepository.guardar`. `JdbcActaRepository` no genera, firma, cifra ni modifica el QR. Un `codigo_qr` aportado por el llamador no es fuente canónica: el valor productivo surge siempre de `QrActaService`. |
| **Consecuencia Java** | La implementación vigente `AesGcmQrTokenProtector` es incompatible con este contrato por tres motivos verificados materialmente: produce 211 caracteres y no entra en la columna; usa un nonce de 16 bytes, un vector de inicialización de 12 bytes y una marca temporal, de modo que no es determinística; y resuelve confidencialidad de un identificador técnico opaco cuando lo que el QR necesita es autenticidad e integridad. `QrTokenProtector` debe pasar a una implementación `HMAC-SHA-256` que respete este formato, con clave versionada, comparación en tiempo constante y rechazo de todo token no canónico. El límite de 512 caracteres que hoy declaran `QrActaService` y el protector deja de tener sentido y se alinea con 128. |
| **Consecuencia DDL futura** | Ninguna. `codigo_qr VARCHAR(128)` y `uq_fal_acta_codigo_qr` no cambian, y no se ejecuta `ALTER TABLE` por esta decisión. El `COMMENT` físico de la columna ya describe este formato y queda ratificado. |
| **Matriz de verificación** | mismo UUID y versión producen el mismo token; UUID distinto produce token distinto; clave o versión distinta produce firma distinta; un token válido verifica; una modificación del UUID, de la versión o de la firma se rechaza; una firma truncada se rechaza; Base64 con relleno o formato no canónico se rechaza; la longitud total no supera 128; el token no contiene `id_acta`; el token no contiene datos personales; el valor persiste exactamente en `codigo_qr`; `uq_fal_acta_codigo_qr` sigue siendo válida. |
| **Tests/guardrails** | `G29ActaAltaRules` verifica que la spec declare el formato canónico, la derivación desde `id_tecnico`, la prohibición de `id_acta` y esta decisión. `DdlPhysicalParityTest.acta_codigoQr_varchar128` se conserva sin cambios: esta decisión ratifica el ancho de 128 en lugar de reversarlo. |

#### `DECISION-QR-INMUTABLE-01` — `fal_acta.codigo_qr`: emisión única, inmutabilidad y semántica de `qr_payload_version`

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | El QR del acta se emite **una sola vez**, durante el labrado y la generación del ticket de infracción. Una vez emitido, `codigo_qr` y `qr_payload_version` son **inmutables**. No existe caso de uso de rotación, regeneración, revocación ni reemplazo individual del QR de un acta, y no existe hoy requerimiento funcional de rotación de la clave QR. `qr_payload_version` representa **exclusivamente la versión del formato y del contenido firmado del payload**, nunca una versión de clave ni un contador de reemisiones. La versión inicial es `0` y corresponde al formato `QR0.<id_tecnico>.<version>.<firma-base64url>` de `DECISION_DDL-ACTA-QR-01`. |
| **Fundamento** | El QR impreso en el ticket de infracción es un objeto físico entregado al infractor: una vez impreso no puede volver a emitirse sin reimprimir el acta, de modo que un caso de uso de rotación no tendría contraparte material. Fijar la inmutabilidad hace que el token sea una propiedad estable de la identidad del acta y elimina toda ambigüedad sobre qué significa la versión. La alternativa —conservar una operación de rotación sin caso de uso— obligaba a inventar una semántica que la spec no define y dejaba abierta una puerta de regeneración que contradice la estabilidad del ticket. |
| **Versión futura** | Una versión distinta de `0` se introduce **únicamente** mediante una decisión normativa nueva, y sólo si cambia el payload, su estructura o sus reglas de validación. Mientras exista un solo formato, una versión activa distinta de `0` es un error de configuración y debe impedir el arranque, no degradar a un formato desconocido. Está prohibido anticipar `QR1`. |
| **Estabilidad de la clave** | La clave de firma de `QR0` no cambia entre reinicios ni despliegues: los tickets ya emitidos deben seguir verificando. Cambiar la clave usada por una versión de payload existente invalidaría tickets históricos y está prohibido sin una decisión normativa explícita, un plan de compatibilidad y validación de los históricos. La clave no vive en el repositorio, se inyecta por secreto de ambiente y **no existe fallback funcional**: si falta o es insuficiente, la aplicación no arranca. |
| **Prohibiciones** | nonce; vector de inicialización; marca temporal; contador; generación por acta; aleatoriedad; almacenamiento de múltiples claves; rotación automática; un `QR1` anticipado; una operación de aplicación que permita cambiar el QR de un acta ya emitida; un endpoint de regeneración o revocación individual. |
| **Consecuencia Java** | `QrActaService` conserva únicamente la emisión previa al alta, la validación y el registro de acceso: **no existe `rotarQr` ni `generarQr`**. `FalActa` rechaza cambiar `codigo_qr` y `qr_payload_version` una vez informado el token, de modo que la inmutabilidad se hace cumplir en el dominio y no sólo por convención de la capa de aplicación. La configuración expone una única clave estable y la versión de payload activa; una versión activa distinta de `0` falla al iniciar como no implementada. El evento `QRGENA` del catálogo significa **generación inicial**, nunca rotación. |
| **Consecuencia DDL futura** | Ninguna. No se agrega `CHECK`, `TRIGGER` ni columna: la inmutabilidad se protege en la aplicación y en el dominio. `codigo_qr VARCHAR(128)`, `qr_payload_version SMALLINT NOT NULL DEFAULT 0`, `chk_acta_qr_ver` y `uq_fal_acta_codigo_qr` no cambian, y no se ejecuta `ALTER TABLE` por esta decisión. |
| **Cómo se resolvió la pregunta abierta** | `DECISION_DDL-ACTA-QR-01` fijaba el determinismo del token pero no definía qué significaba rotar el QR de un acta, y esa indefinición quedó registrada como pendiente durante la implementación. Se resuelve **eliminando el caso de uso**, no inventando una semántica de rotación: el negocio confirmó que el QR se emite una sola vez con el ticket y que no existe requerimiento de rotación. La trazabilidad de ese cierre vive en la evidencia del slice que lo ejecutó, no en este registro normativo. |
| **Tests/guardrails** | `G29ActaAltaRules` verifica que esta decisión esté registrada con la emisión única, la inmutabilidad y la semántica de payload. `QrConfigFailClosedTest` verifica que la configuración falle sin clave válida y que una versión de payload distinta de `0` se rechace. `ActaQrInmutabilidadTest` verifica que el agregado rechace un segundo `codigo_qr` y un cambio de `qr_payload_version`. `QrApiMinimaGuardrailTest` verifica en código ejecutable la ausencia de `rotarQr`, `generarQr`, aleatoriedad y de una clave funcional por defecto en la configuración base. |

#### `DECISION-ACTA-UNIQUE-01` — `fal_acta`: los dos UNIQUE no primarios son técnicos y su violación no es un conflicto funcional

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | `uq_fal_acta_id_tecnico` y `uq_fal_acta_codigo_qr` **se conservan** y son los dos únicos UNIQUE no primarios de `fal_acta`. Ambos protegen valores **generados internamente por el sistema**: `id_tecnico` es un UUID que el alta genera y que el cliente no puede proponer, y `codigo_qr` se deriva determinísticamente de `id_tecnico` y de la versión del payload (`DECISION_DDL-ACTA-QR-01`, `DECISION-QR-INMUTABLE-01`). Por lo tanto, una colisión de cualquiera de los dos valores **no es una regla de negocio que el usuario pueda corregir** ni un conflicto funcional del contrato HTTP. |
| **Fundamento** | Una unicidad se traduce a excepción de dominio y a conflicto HTTP cuando el valor duplicado lo eligió el llamador y puede corregirlo: ahí el conflicto es información útil. Ninguno de estos dos valores llega del request. `LabrarActaCommand` no declara `id_tecnico` ni `codigo_qr`, y `codigo_qr` es una función determinística de `id_tecnico`, de modo que dos actas sólo pueden colisionar en el QR si antes colisionaron en el UUID. Inventar una excepción funcional para un caso que el usuario no provoca y no puede resolver sería crear contrato sin comportamiento detrás, y devolver un 409 le pediría corregir un dato que nunca envió. La colisión real de un UUID versión 4 indica una falla del generador o de la infraestructura: es un defecto, no un caso de uso. |
| **Excepción funcional** | **No se crea ninguna.** No existe `ActaDuplicadaException`, `ConflictoActaException` ni ninguna otra excepción de dominio para estas dos colisiones, y está prohibido agregarlas mientras esta decisión esté vigente. |
| **Contrato HTTP** | **No se agrega.** No existe ni se agrega un mapeo a `409 CONFLICT` para estas dos constraints. Los conflictos 409 vigentes corresponden a concurrencia optimista y a duplicaciones de pagos y de documentos, todas con su propia excepción de dominio y su propio contrato; ninguna se activa por la violación de estos dos UNIQUE. |
| **Excepción que sí viaja** | `JdbcActaRepository` conserva la `DuplicateKeyException` que respalda el motor, sin traducirla, y **el nombre de la constraint debe permanecer identificable** en el mensaje: es lo que permite distinguir cuál de las dos se violó al diagnosticar. Una violación de clave ajena, de `CHECK` o de `NOT NULL` llega como `DataIntegrityViolationException` y no se enmascara como duplicado. |
| **Sin reintento silencioso** | Ante una colisión **no se reintenta** con otro UUID ni se regenera el QR. Un reintento silencioso ocultaría una falla del generador de identificadores y contradiría la emisión única del QR. |
| **Delta aceptado del test double** | `InMemoryActaRepository` **no simula** estas dos unicidades técnicas. La diferencia respecto del adaptador JDBC queda **aceptada y documentada** como límite del doble de prueba: el oráculo de paridad cubre identidad, OCC y comportamiento observable de las lecturas, no la unicidad delegada al motor, que por definición no puede sostenerse en memoria entre varias instancias de la aplicación. Verificar estas dos constraints es responsabilidad de las pruebas de integración contra el motor real. |
| **Alcance de la unicidad** | **No se agrega** `UNIQUE` sobre `nro_acta`, sobre `id_talonario`, sobre `nro_talonario_usado`, sobre ninguna combinación de numeración ni sobre ningún otro campo de `fal_acta`. La numeración del acta y del talonario tiene sus propias reglas y su tratamiento no forma parte de esta decisión. |
| **Consecuencia Java** | Ninguna. No se agrega, elimina ni modifica ninguna clase: la decisión ratifica el comportamiento vigente y cierra la pregunta sobre si había que traducir estas violaciones. `JdbcActaRepository` conserva su Javadoc de integridad, que ya afirma que el puerto no promete excepción funcional para estos dos UNIQUE. |
| **Consecuencia DDL** | Ninguna. No se ejecuta `ALTER TABLE`, no se crea ni se elimina índice, `UNIQUE` o `CHECK`, y no se renombra ningún objeto físico. `fal_acta` conserva sus 57 columnas, sus cuatro `CHECK` y sus dos UNIQUE no primarios con sus nombres exactos. |
| **Matriz de verificación** | `id_tecnico` duplicado produce `DuplicateKeyException` con `uq_fal_acta_id_tecnico`; `codigo_qr` duplicado produce `DuplicateKeyException` con `uq_fal_acta_codigo_qr`; las dos se distinguen entre sí por el nombre de la constraint; clave ajena, `CHECK` y `NOT NULL` no se enmascaran como duplicado; `fal_acta` tiene exactamente esos dos UNIQUE no primarios; `id_tecnico` no es un campo del comando de alta; `codigo_qr` no es un campo del comando de alta; no existe excepción de dominio de acta duplicada; no existe handler que traduzca `DuplicateKeyException` a `409`; `nro_acta` no tiene `UNIQUE`. |
| **Tests/guardrails** | `JdbcActaRepositoryIT` cubre la conducta física: los dos duplicados con su constraint distinguible, la no contaminación de clave ajena, `CHECK` y `NOT NULL`, y el inventario de los UNIQUE no primarios de la tabla. `ActaBatchAlcanceGuardrailTest` verifica en código ejecutable que no aparezcan las excepciones prohibidas, que el advice global no traduzca `DuplicateKeyException`, que el comando de alta no exponga los dos valores y que el adaptador conserve el nombre de la constraint. `G29ActaAltaRules` verifica que esta decisión siga registrada. |

## Consultas operativas listables — decisión y deuda física

> Esta sección agrupa la decisión transversal de contratos de consulta y la deuda de
> optimización física que se difiere deliberadamente. La decisión está **cerrada**; la deuda
> está **abierta** y su estado se declara en su propia fila, de modo que no se confunda con
> las decisiones cerradas de las secciones anteriores. Ninguna de las dos altera un estado,
> evento, transición, comando o contrato HTTP ya cerrado.

#### `DECISION-LIST-PAGE-SORT-01` — toda consulta pública de una entidad operacional listable devuelve una página

| Campo | Detalle |
|---|---|
| **Alcance** | Decisión **transversal** de contratos de consulta. Aplica a toda entidad operacional listable, no sólo a las ya persistidas en MariaDB. No redefine ningún estado, evento, transición, comando ni contrato HTTP vigente: agrega la forma obligatoria de las consultas de colección. |
| **Regla central** | Toda consulta pública de una entidad operacional listable devuelve **una página**. No existe una variante pública ilimitada del listado, y un método interno ilimitado no puede presentarse como contrato público. |
| **Ausencia de parámetros** | La ausencia **no** significa consulta ilimitada. Equivale exactamente a `page = 0`, `size = 50` y el orden predeterminado de la entidad. Los parámetros son opcionales para el cliente; el recorte no lo es. |
| **Tamaño máximo** | `100`. Un tamaño mayor **se rechaza explícitamente** y no se limita en silencio. El motivo es concreto: un cliente que pidió 500 y recibió 100 sin aviso no puede distinguir esa página de la última del conjunto, y a partir de ahí pagina mal sobre datos que no cambiaron. |
| **Validaciones de contrato** | Se rechazan `page < 0`, `size <= 0`, `size > 100`, campo de orden desconocido, campo de orden repetido, dirección distinta de `ASC`/`DESC` y más criterios que el máximo declarado. El rechazo es explícito; ninguna de esas entradas se corrige por inferencia. |
| **Filtros** | Contratos **tipados y específicos por entidad**. Está prohibido implementar un lenguaje genérico campo-operador-valor: con campos tipados el conjunto de predicados queda fijado en compilación y el adapter puede escribir su `WHERE` con fragmentos constantes; con un lenguaje genérico habría que construir SQL a partir de texto del cliente. Cada campo nulo significa "sin filtro". |
| **Orden** | Una **lista ordenada** de criterios funcionales, cada uno campo + dirección `ASC`/`DESC`. La precedencia coincide con el orden de aparición. No se usa un mapa: un mapa no garantiza precedencia. |
| **Allowlist por entidad** | Cada entidad declara un conjunto explícito y tipado de campos funcionales ordenables. El cliente **nunca** aporta nombres de tabla, de columna, alias, funciones ni fragmentos de SQL: sólo puede nombrar una constante de la allowlist. La traducción del campo funcional a expresión SQL vive en el adapter, en una constante interna suya. Un campo funcional puede expandirse a varias expresiones. |
| **Orden estable** | Cada entidad declara un orden predeterminado. **Todo** orden, predeterminado o solicitado, agrega una clave única y estable como último criterio de desempate. Sin ese desempate, dos filas empatadas pueden llegar en distinto orden en dos ejecuciones de la misma consulta, y entonces la página siguiente repite u omite filas sobre un conjunto que no cambió. No se depende de un orden accidental del motor. |
| **Ejecución en la base** | Filtros, orden y paginación se ejecutan en el motor. **No** se filtra, ordena ni pagina en memoria después de cargar la consulta, y no se usa un método ilimitado para resolver la primera página. Un adaptador InMemory recorta en memoria porque la memoria *es* su almacenamiento; no promete eficiencia de motor ni ACID, sólo el comportamiento observable del contrato. |
| **Parámetros enlazados** | Todos los valores de filtro viajan al motor como parámetros enlazados. Está prohibido concatenar valores al texto de la sentencia. Las expresiones de `ORDER BY` sólo se construyen desde constantes o allowlists internas del adapter. |
| **Relaciones batch** | Las colecciones hijas se consultan **sólo** para los IDs de la página actual. Está prohibido leer la tabla completa y después recortar, y está prohibido pedir el batch para todos los IDs de la tabla (`DECISION-OBS-BATCH-01`). |
| **Scroll infinito** | Reutiliza el mismo contrato de filtros, orden y tamaño que la paginación tradicional. Puede omitir el total y consultar `size + 1` para determinar si existe página siguiente, devolviendo sólo `size` elementos. Cuando no se contó, el total se representa ausente y **no** se sintetiza un número. |
| **Excepciones** | Toda excepción debe estar respaldada por una decisión normativa explícita. Los catálogos pequeños deliberadamente no paginados (`/dependencias`, `/inspectores`, `/firmantes`, `/talonarios`, plantillas documentales) son la excepción vigente: son listas administrativas acotadas, no bandejas operativas. |
| **Consecuencia Java** | Contratos comunes en `application/query`: `DireccionOrden`, `CriterioOrden<C>`, `PaginaSolicitada<C>` y `PaginaResultado<T>`. Allowlists tipadas `CampoOrdenActa` y `CampoOrdenPersona`; filtros tipados `FiltroActa` y `FiltroPersona`. Los puertos `ActaRepository` y `PersonaRepository` declaran `listarPagina(filtro, pagina)`, que **no** es `default`: obligar a las dos implementaciones a escribirla es lo que impide que un adapter recorte en memoria sin que nadie lo note. Casos de uso `ActaService.listarActas` y `PersonaService.listarPersonas`. No se agrega Spring Data ni ninguna otra dependencia para obtener `Pageable` o `Sort`. |
| **Orden predeterminado de Acta** | `fechaActa DESC`, más la PK como desempate. Campos ordenables: `nroActa`, `fechaActa`, `fechaLabrado`, `fechaAlta`, `tipoActa`, `bloqueActual`, `estadoProcesal`, `situacionAdministrativa`, `dependencia`, `inspector`, `personaInfractor`. `titular` **no** integra la allowlist: `fal_acta` no tiene columna de titular, la identidad del infractor vive en `fal_persona` y `FalActa.infractorNombre` es compatibilidad no persistida; ordenar por titular exige un `LEFT JOIN` cuyo plan pertenece a `D-PERSISTENCE-PAGING-INDEX-01`. |
| **Orden predeterminado de Persona** | `fechaAlta DESC`, más la PK como desempate. Se elige `fh_alta` y no `nombre_mostrar` porque `fh_alta` es `NOT NULL`: un orden predeterminado sobre una columna nullable pondría a todas las personas sin nombre de presentación en un extremo de la primera página de cualquier consulta que no pida orden. Campos ordenables: `nombreMostrar`, `apellido`, `razonSocial`, `tipoDocumento`, `numeroDocumento`, `tipoPersona`, `fechaAlta`. `doc_key` no integra la allowlist porque es `STORED GENERATED` y `FalPersona` no la expone (`DECISION_DDL-PERS-01`). |
| **Política de nulos** | El orden nativo de MariaDB: nulos primero en `ASC` y últimos en `DESC`. **No** se envuelve la columna en `ISNULL()` ni en `COALESCE()`: una columna envuelta en una función no puede usar su índice, y la política quedaría pagada con una ordenación en memoria del motor en todas las consultas para cambiar la posición de un grupo de filas. La política es explícita y verificada, no accidental. Los adaptadores InMemory la reproducen con `nullsFirst` más inversión. |
| **Enums en el orden** | Se ordena por el **código canónico** de la columna. Está prohibido ordenar por `ordinal()`, por `name()` o por `toString()` de un enum. Los comparadores InMemory comparan por `codigo()` para que la paridad no sea falsa. |
| **Consecuencia DDL** | **Ninguna.** No se ejecuta `ALTER TABLE`, no se crea, modifica ni elimina ningún índice, `UNIQUE` o `CHECK`. La optimización física de estas consultas es una deuda declarada aparte en `D-PERSISTENCE-PAGING-INDEX-01`. |
| **Exposición HTTP** | Esta decisión **no** declara rutas. El contrato queda implementado en puerto, caso de uso y adaptadores; la exposición web de los listados de Acta y Persona, con sus DTO, códigos de error, seguridad y representación de metadatos de página, es materia de una wave propia y debe declararse en `../40-api/http-contracts.md` antes de existir. Ningún endpoint puede exponer un listado ilimitado. |
| **Tests/guardrails** | `ConsultaPaginadaTest` verifica el contrato completo sobre el oráculo InMemory: defaults, límites, rechazos, precedencia, allowlists, desempate y política de nulos. `PaginacionOrdenSeguroGuardrailTest` verifica sobre código ejecutable que ninguna expresión de `ORDER BY` se construya con texto recibido y que los valores de filtro viajen enlazados. `JdbcPaginacionIT` verifica el contrato contra el motor real, incluida la ausencia de repetición u omisión entre páginas y el batch limitado a los IDs de la página. `G32ListPageSort.decision_list_page_sort_registrada` y `G32ListPageSort.deuda_paging_index_registrada_y_abierta` verifican el registro documental. |

#### `D-PERSISTENCE-PAGING-INDEX-01` — optimización física de las consultas paginadas (ABIERTA, diferida deliberadamente)

| Campo | Detalle |
|---|---|
| **Estado** | **ABIERTA.** Diferida deliberadamente hasta completar los contratos de listado, filtrado, orden y paginación de las entidades persistentes. No es un descuido: elegir índices antes de conocer las consultas reales produce índices especulativos que hay que sostener con costo de escritura sin evidencia de que alguna consulta los use. |
| **Por qué se difiere y no se resuelve junto con el contrato** | Un índice se justifica por una consulta real, su frecuencia, su criticidad, su plan y una medición antes/después. Nada de eso existe mientras las entidades listables no tengan su contrato implementado y su SQL final estabilizado. Cerrar la optimización dentro de un slice que sólo implementa funcionalidad obligaría a inventar la evidencia. |
| **Wave que la cierra** | `WAVE 1A2-PERSISTENCE-PERF-01 — PAGINATION QUERY PLANS AND INDEX OPTIMIZATION`. |
| **Qué analizará por entidad** | SQL real; filtros y órdenes relevantes; joins; volúmenes y selectividad; índices actuales; `EXPLAIN` y, cuando sea seguro, `ANALYZE FORMAT=JSON` o equivalente; presencia de ordenación en memoria del motor, tablas temporales, recorridos completos y cardinalidad estimada contra real; costo de escritura de cada índice candidato; medición antes/después; y conveniencia de `OFFSET` frente a keyset/cursor en páginas profundas. |
| **Registro de Acta** | Tabla `fal_acta`, sin joins. Filtros soportados: `tipo_acta`, `bloque_actual`, `est_proc_act`, `sit_adm_act`, `id_dep`, `id_insp`, `nro_acta`, rango de `fecha_acta`. Orden predeterminado `fecha_acta DESC, id DESC`. Índices actuales verificados contra el motor: `PRIMARY`, dos UNIQUE (`uq_fal_acta_id_tecnico`, `uq_fal_acta_codigo_qr`), ocho de clave ajena y **cuatro de consulta**: `ix_fal_acta_fecha (fecha_acta)`, `ix_fal_acta_estado (bloque_actual, est_proc_act, sit_adm_act)`, `ix_fal_acta_dep (id_dep)`, `ix_fal_acta_persona (id_persona_infractor)`. Los cuatro coinciden con los predicados y el orden que la consulta paginada usa: `ix_fal_acta_fecha` cubre el orden predeterminado y el rango de fecha, e `ix_fal_acta_estado` cubre la combinación de bandeja completa por su prefijo izquierdo. **Lo que queda por medir no es la ausencia de índices sino su elección real con volumen**: con la tabla vacía el motor recorre la tabla completa porque es más barato, de modo que estos planes no dicen qué índice usaría con datos. Colección hija: observaciones, resueltas con una operación batch limitada a los IDs de la página (`DECISION-OBS-BATCH-01`), servida por el prefijo izquierdo de `ix_obs_entidad`. |
| **Registro de Persona** | Tabla `fal_persona`, sin joins. Filtros soportados: `tipo_persona`, `tipo_documento`, `nro_doc`, `id_suj`, `id_bie`, rango de `fh_alta`. Orden predeterminado `fh_alta DESC, id DESC`. Índices actuales verificados contra el motor: `PRIMARY`, `uq_fal_persona_tipo_doc_key (tipo_documento, doc_key)`, `ix_fal_persona_apellido (apellido)`, `ix_fal_persona_razon_social (razon_social)`. El prefijo izquierdo `tipo_documento` del UNIQUE sirve ese predicado; `nro_doc` no está indexado y se resuelve por filtro. **Ningún índice cubre `fh_alta`, que es el orden predeterminado, ni `tipo_persona`, `id_suj` o `id_bie`**, de modo que la ordenación por defecto se resuelve hoy con una ordenación en memoria del motor. Es la diferencia material con Acta y el candidato más concreto de esta deuda, sujeto a medición con volumen antes de crear nada. Sin colecciones hijas: este adaptador no compone observaciones y por lo tanto no tiene consulta batch. |
| **Evidencia de planes** | `EXPLAIN` de las diez consultas principales de las dos entidades, más el inventario de índices y la verificación de cobertura de los dos órdenes predeterminados, capturados con acceso de sólo lectura al motor. Se declara explícitamente que las tablas están vacías en el entorno de captura: los planes describen la forma de la consulta y no un costo representativo. |
| **Métodos ilimitados a migrar o eliminar** | `ActaRepository.listarTodas()` se conserva como operación interna: su único consumidor de producción es `ActaRefPersonaDomicilioUsoChecker`, que necesita un predicado disyuntivo sobre `id_domicilio_infractor_act` e `id_domicilio_notif_act` que el filtro tipado conjuntivo no expresa; hoy recorre la tabla completa en memoria. `PersonaRepository.listarTodas()` **no tiene ningún consumidor de producción**: sólo lo ejercitan tests. Los dos quedan inventariados para migración o eliminación en esta deuda. |
| **Prohibiciones mientras esté abierta** | No crear un índice por cada campo filtrable u ordenable; no crear índices por combinatoria teórica o especulativa; no resolver paginación, filtrado u orden en memoria por falta de un índice; no ampliar DDL salvo que una wave lo autorice expresamente; no ocultar un plan deficiente creando un índice sin diagnóstico, justificación y medición. |
| **Condición de cierre** | Debe cerrarse **antes** de declarar productivos los listados y antes de generar la documentación definitiva para desarrolladores. |
| **Consecuencia DDL** | Ninguna mientras esté abierta. Su cierre es el único momento autorizado para seleccionar, modificar o eliminar índices de estas consultas. |
| **Tests/guardrails** | `G32ListPageSort.deuda_paging_index_registrada_y_abierta` verifica que esta deuda siga registrada, declarada abierta y con su wave nombrada, y que no se declare cerrada mientras el registro no lo diga. |
---

## Consistencia transaccional del ciclo de vida de Acta después de la activación JDBC (`POST-B2-TX-CONSUMERS-01`)

> B2 activó los adaptadores JDBC de Acta, Persona, Evidencia y Snapshot y certificó
> `ActaService.labrar` con `@Transactional`. Esta sección certifica el resto del ciclo de vida:
> los comandos que la activación de B2 dejó consumiendo los mismos puertos JDBC sin frontera
> transaccional efectiva. La decisión sobre la semántica de acceso QR + notificación está
> **cerrada**; la deuda de los 16 servicios restantes está **abierta** y se declara en su propia
> fila para no confundirse con las decisiones cerradas de esta sección.

#### `DECISION-QR-ACCESO-NOTIFICACION-01` — el acceso QR y la notificación por portal no comparten transacción

| Campo | Detalle |
|---|---|
| **Alcance** | `QrActaService.registrarAcceso` y `QrActaService.registrarAccesoConNotificacion`. No redefine `DECISION-QR-INMUTABLE-01` ni `DECISION_DDL-ACTA-QR-01`: ninguna de las dos toca la emisión ni la inmutabilidad del token. |
| **Regla central** | El acceso QR (`fal_acta_qr_acceso`), el evento `QRACCA` y el snapshot forman **una única transacción real** (`QrAccesoTransaccionalService.registrarConfirmado`), que debe confirmar completamente antes de que comience cualquier colaboración con `NotificacionIntentoService.registrarPortalPositivo`. La notificación es **posterior e independiente**: si falla, el acceso QR ya confirmado **no se revierte**. No existe una única transacción para las dos escrituras ni `REQUIRES_NEW` simulando independencia: son dos llamadas de nivel superior reales, cada una con su propia frontera. |
| **Fundamento** | El acceso QR es append-only por diseño (`50-persistence/mariadb-logical-model.md`, tabla `fal_acta_qr_acceso`) y su auditoría de seguridad no puede depender de que una notificación downstream tenga éxito: un mismatch QR-acta-notificación, un fallo del documento del fallo o cualquier otro rechazo de `registrarPortalPositivo` no puede borrar la evidencia de que el acceso ocurrió. Es la semántica que el Javadoc histórico de `QrActaService` ya documentaba y que `QrActaServiceTest.IntegracionPortal` ya verificaba (4 casos) antes de esta wave; la decisión la cierra normativamente porque la spec canónica no la declaraba. |
| **Self-invocation resuelta** | `QrActaService.registrarAccesoConNotificacion` invoca `registrarAcceso` dentro del mismo bean. Anotar `@Transactional` directamente sobre `registrarAcceso` habría neutralizado la frontera exactamente para esa llamada interna. La escritura real se extrajo a `QrAccesoTransaccionalService` (`@Service` separado, con `@Transactional` sobre `registrarConfirmado`), invocado siempre a través de la referencia inyectada. `registrarAcceso` no lleva `@Transactional` propia. |
| **Consecuencia Java** | `QrAccesoTransaccionalService` (nuevo). `QrActaService` deja de depender directamente de `ActaEventoRepository`, `ActaSnapshotRepository` y `SnapshotRecalculador`: esas dependencias las asume el colaborador. `QrAccesoRepository` pasa a tener adapter JDBC (`JdbcQrAccesoRepository`, `@Profile("jdbc")`, PK `AUTO_INCREMENT`, sin `nextId()` persistente) y el InMemory queda restringido a `@Profile("!jdbc")`. |
| **Consecuencia DDL** | Ninguna. `fal_acta_qr_acceso` ya existía con PK `AUTO_INCREMENT` y su contrato de puerto no cambió. |
| **Tests/guardrails** | `QrAccesoTransaccionalIT`: commit atómico de las tres escrituras, rollback total ante fallo en cualquiera de las tres, la notificación arranca sólo después del commit del acceso (verificado por lectura independiente), y un fallo de notificación no revierte el acceso ya confirmado. `JdbcProfileContextIT` casos 33 a 35: `QrAccesoRepository` resuelto por JDBC bajo `jdbc` e InMemory sin él, `QrAccesoTransaccionalService` proxificado con atributo transaccional efectivo, y `registrarAcceso` verificado sin `@Transactional` propia. Los 4 casos preexistentes de `QrActaServiceTest.IntegracionPortal` se preservan sin reescritura. |

#### `D-POST-B2-CROSS-COMMAND-TX-01` — servicios del dominio con la misma ausencia de frontera transaccional (ABIERTA)

| Campo | Detalle |
|---|---|
| **Estado** | **ABIERTA.** Inventariada por `POST-B2-TX-CONSUMERS-01`, deliberadamente no resuelta en esa misma wave por decisión explícita del usuario (alcance mínimo, opción A de la consulta de la sección de decisiones). |
| **Hallazgo** | Ningún método público de `application/service`, salvo `ActaService.labrar`, `ActaService.completarCaptura`, `ActaService.enriquecer` y `QrAccesoTransaccionalService.registrarConfirmado`, tiene `@Transactional`. Los 16 servicios listados abajo combinan, dentro de la misma operación lógica, al menos una escritura sobre un puerto ya JDBC (`ActaRepository`, `ActaEventoRepository` o `ActaSnapshotRepository` vía `SnapshotRecalculador`) con al menos una escritura sobre un puerto que **sólo** tiene implementación InMemory en el workspace. |
| **Por qué no se resuelve con sólo `@Transactional`** | Anotar el método público protegería la porción MariaDB, pero no la porción InMemory de la misma operación: un rollback de la transacción JDBC no revierte lo ya escrito en el mapa en memoria, y la transacción JDBC puede fallar después de que el efecto InMemory ya ocurrió. Cerrarlo por completo exige migrar a JDBC los puertos InMemory que cada servicio usa, lo cual es una **migración masiva de repositorios del resto del dominio** fuera del alcance de una micro-wave. |
| **Wave que la cierra** | Sin nombrar todavía. Debe decidir, servicio por servicio: (a) `@Transactional` parcial que sólo protege la porción JDBC, documentando la limitación; (b) migración del puerto InMemory a JDBC contra tabla y contrato ya existentes, si los hay; o (c) ambas. No puede cerrarse declarando "@Transactional en todos" sin decidir explícitamente qué pasa con la porción InMemory de cada uno. |
| **Registro por servicio** | Ver tabla siguiente. |
| **Prohibiciones mientras esté abierta** | No agregar `@Transactional` a estos 16 servicios sin decidir explícitamente el tratamiento de su porción InMemory; no migrar ninguno de sus puertos InMemory a JDBC sin autorización expresa de una wave que lo nombre; no declarar cerrado el ciclo de vida transaccional completo de Acta mientras esta deuda esté abierta. |
| **Consecuencia DDL** | Ninguna mientras esté abierta. |
| **Tests/guardrails** | `analysis/inventario-consumidores-post-b2.md`, `analysis/matriz-comandos-mutantes.md` y `analysis/decisiones-consultadas.md` del RUN `POST-B2-TX-CONSUMERS-01` son la evidencia material completa. |

| # | Servicio | Método(s) mutante(s) | Puertos JDBC tocados | Puertos InMemory-only tocados | Frontera actual | Riesgo | Prioridad | Dependencia/decisión necesaria |
|---|---|---|---|---|---|---|---|---|
| 1 | `FalloActaService` | `dictarAbsolutorio`, `dictarCondenatorio` | `ActaEventoRepository`, `ActaSnapshotRepository` | `FalloActaRepository` | ninguna | alto: fallo dicta sin persistir o persiste sin evento/snapshot | alta (afecta firmeza y apelación) | decidir si `FalloActaRepository` migra a JDBC o si se acepta `@Transactional` parcial |
| 2 | `ApelacionActaService` | `registrarApelacion`, `resolverRechazada`, `resolverAceptaAbsuelve`, `pasarAAnalisis`, `resolverModificaCondena`, `resolverNulidad`, `registrarDocumento` | `ActaRepository`, `ActaEventoRepository`, `ActaSnapshotRepository` | `ApelacionActaRepository`, `FalloActaRepository` | ninguna | alto: resoluciones de apelación con efectos parciales sobre el acta | alta | idem; además evaluar orden de escritura entre apelación y fallo |
| 3 | `DocumentoService` | `generarDocumento`, `generarDesdePlantilla`, `numerarDocumento`, `enviarAFirma`, `firmarDocumento`, `emitirDocumento`, `incorporarDocumentoEscaneado` | `ActaEventoRepository`, `ActaSnapshotRepository` | `DocumentoRepository` | ninguna | alto: documento y evento pueden desincronizarse; `enviarAFirma`/`firmarDocumento` pueden tener efecto externo a auditar | alta | migrar `DocumentoRepository` a JDBC es la más nombrada como pendiente en B2; auditar efecto externo de firma antes de decidir atomicidad |
| 4 | `NotificacionIntentoService` | `registrarIntento`, `registrarReintentoPorVencimiento`, `registrarResultadoIntento`, `registrarPortalPositivo` | `ActaRepository`, `ActaEventoRepository`, `ActaSnapshotRepository` | `NotificacionRepository`, `NotificacionIntentoRepository`, `FalloActaRepository`, `DocumentoRepository`, `LoteCorreoRepository` | ninguna (sincroniza con `ResultadoPositivoInMemoryMonitor`, que no es una transacción de base) | alto: es el flujo con más escrituras combinadas del dominio | alta | decidir orden de migración de los 5 puertos InMemory involucrados antes de anotar nada |
| 5 | `NotificacionService` | `enviarNotificacion`, `registrarPositiva`, `registrarNegativa`, `registrarVencida` | `ActaEventoRepository`, `ActaSnapshotRepository` | `NotificacionRepository` | ninguna | medio-alto | alta | migrar `NotificacionRepository` o aceptar `@Transactional` parcial |
| 6 | `NotificacionAcuseService` | `registrarAcuse`, `validarAcuse`, `observarAcuse`, `anularAcuse` | `ActaEventoRepository` (indirecto) | `NotificacionAcuseRepository` | ninguna | medio | media | verificar si `NotificacionAcuseRepository` tiene adapter JDBC antes de decidir |
| 7 | `PagoVoluntarioService` | `solicitar`, `fijarMonto`, `informar`, `confirmar`, `observar`, `vencer` | `ActaRepository`, `ActaEventoRepository`, `ActaSnapshotRepository` | `PagoVoluntarioRepository` | ninguna | alto: pagos con efectos económicos | alta | migrar `PagoVoluntarioRepository` o `@Transactional` parcial documentado |
| 8 | `PagoCondenaService` | `informar`, `confirmar`, `observar` | `ActaEventoRepository`, `ActaSnapshotRepository` | `PagoCondenaRepository` | ninguna | alto: idem pago voluntario | alta | idem |
| 9 | `GestionExternaService` | `derivar`, `reingresar`, `registrarPagoExternoGestion` | `ActaRepository`, `ActaEventoRepository`, `ActaSnapshotRepository` | `GestionExternaRepository` | ninguna | medio-alto | media | verificar adapter JDBC de `GestionExternaRepository` |
| 10 | `LoteCorreoService` | `generarLoteDesdePendientes`, `generarLote`, `emitirLote`, `procesarLote`, `marcarConError`, `anularLote` | `ActaEventoRepository` (indirecto vía intentos) | `LoteCorreoRepository` | ninguna | medio | media | idem |
| 11 | `ArchivoActaService` | `archivar`, `reingresar` | `ActaRepository`, `ActaEventoRepository`, `ActaSnapshotRepository` | `MotivoArchivoRepository`/repos de archivo | ninguna | alto: archivo/reingreso cambia bloque y situación administrativa | alta | verificar cuáles de sus repos ya tienen adapter JDBC (`MotivoArchivoRepository` sí, ver topología B0B/1A1) |
| 12 | `ParalizacionActaService` | `paralizar`, `reactivar` | `ActaRepository`, `ActaEventoRepository`, `ActaSnapshotRepository` | repos de paralización | ninguna | medio-alto | media | idem |
| 13 | `ActaParalizacionService` | `paralizar`, `reactivar` | `ActaRepository`, `ActaEventoRepository`, `ActaSnapshotRepository` | repos de paralización | ninguna | medio-alto | media | aclarar primero si coexiste con `ParalizacionActaService` o si uno está obsoleto |
| 14 | `FirmezaCondenaService` | `vencerPlazoApelacion`, `declararFirmePorApelacionRechazada` | `ActaEventoRepository`, `ActaSnapshotRepository` | `FalloActaRepository`, `ApelacionActaRepository` | ninguna | alto: firmeza es un hito jurídico | alta | depende de la decisión de `FalloActaRepository`/`ApelacionActaRepository` de las filas 1 y 2 |
| 15 | `CierreActaHelper` | `emitirCierre` (helper invocado desde varios de los anteriores) | `ActaRepository` (vía llamador), `ActaEventoRepository` | depende del llamador | ninguna propia | depende del llamador | se resuelve junto con cada llamador | no requiere decisión propia si sus llamadores quedan resueltos |
| 16 | `DocumentoGraphDemoService` | lectura agregada con un punto de escritura indirecta | `ActaSnapshotRepository` (indirecto) | `DocumentoRepository` | ninguna | bajo: uso principal es de demo/lectura | baja | confirmar si tiene algún consumidor productivo antes de priorizar |

---

## Corrección de identidad del acceso QR y gap de canal (`POST-B2-TX-CONSUMERS-01-R1`)

> Remediación mínima sobre `POST-B2-TX-CONSUMERS-01`. No modifica el RUN histórico sellado
> (`POST-B2-TX-CONSUMERS-01-RUN-20260804-180046.zip`), no reabre `DECISION-QR-ACCESO-NOTIFICACION-01`
> ni `D-POST-B2-CROSS-COMMAND-TX-01`, y no toca la semántica transaccional acceso + `QRACCA` +
> snapshot ya cerrada.

### Corrección aplicada (no es una decisión nueva, es un defecto corregido)

`QrAccesoTransaccionalService.registrarConfirmado` devolvía en `AccesoQrResultado.idAcceso` el
valor **provisional** de `QrAccesoRepository.nextId()` en lugar del id real que devuelve
`QrAccesoRepository.registrar(...)`. Bajo `JdbcQrAccesoRepository` la PK la asigna
`AUTO_INCREMENT` y difiere del contador provisional, de modo que el resultado podía informar un
`idAcceso` que no correspondía a ninguna fila real. Corregido capturando el retorno de
`registrar(...)` y usando `persistido.getId()`. Sin cambios en la estrategia `AUTO_INCREMENT`, sin
DDL, sin retries, sin alterar la atomicidad ni la independencia posterior de la notificación.
Verificado por `QrAccesoTransaccionalServiceTest` (unitario, determinista, `nextId()=111` !=
`registrar(...).getId()=999`) y por `QrAccesoTransaccionalIT` casos 9 y 10 (contra MariaDB real,
con `buscarPorId` y `registrarAccesoConNotificacion`).

#### `D-QR-ACCESO-CANAL-PERSISTENCE-01` — `fal_acta_qr_acceso` no persiste el canal de acceso (ABIERTA)

| Campo | Detalle |
|---|---|
| **Estado** | **ABIERTA.** Registrada prospectivamente por `POST-B2-TX-CONSUMERS-01-R1`, no resuelta: no se autorizó DDL ni se inventó un dato para cerrarla. |
| **Comportamiento actual** | `fal_acta_qr_acceso` no tiene columna `canal_acceso_qr` (`SPEC-MODEL-DDL-CLOSURE-001`: se eliminó del DDL mientras el único canal fuera `WEB`, por ausencia de variabilidad funcional). `JdbcQrAccesoRepository.mapear` reconstruye **siempre** `CanalAccesoQr.PORTAL` al leer una fila desde el motor (`buscarPorId`, `listarPorActa`), sin importar el canal real con el que se registró el acceso. El resultado **inmediato** de `registrarAcceso`/`registrarConfirmado` sí conserva el canal real, porque `QrAccesoTransaccionalService` lo construye en memoria a partir del parámetro de entrada, sin releer la fila. |
| **Pérdida de fidelidad al releer** | Un acceso registrado con `CanalAccesoQr.APP` (o `INTEGRACION`, u `OTRO`) se persiste correctamente en cuanto a acta, instante, IP, user-agent y resultado, pero **cualquier lectura posterior** de esa fila (`buscarPorId`, `listarPorActa`, y por extensión `QrActaService.listarAccesosPorActa`) informa `CanalAccesoQr.PORTAL`, sea cual sea el canal real. Verificado materialmente por `QrAccesoTransaccionalIT` caso 11: registrar con `APP`, releer con `buscarPorId` y `listarPorActa`, obtener `PORTAL` en ambos. |
| **Contrato Java afectado** | `QrAccesoRepository.buscarPorId`, `QrAccesoRepository.listarPorActa`, `QrActaService.listarAccesosPorActa` y, por extensión, cualquier consumidor futuro de auditoría que necesite distinguir el canal real de un acceso ya persistido. El campo `FalActaQrAcceso.getCanalAcceso()` sigue `@Deprecated` desde B2/`POST-B2-TX-CONSUMERS-01`, pero el contrato del constructor de dominio y de `QrActaService.registrarAcceso` **todavía exige y acepta** `CanalAccesoQr.APP`, `INTEGRACION` y `OTRO`, no sólo `PORTAL`. |
| **Decisión normativa futura requerida** | Si el canal real de un acceso QR debe ser recuperable después de escribirlo (por ejemplo, para auditoría de seguridad que distinga accesos desde `APP` de accesos desde el portal web), hace falta una decisión que reintroduzca la persistencia del canal. No puede resolverse por inferencia porque `SPEC-MODEL-DDL-CLOSURE-001` la eliminó deliberadamente por ausencia de variabilidad funcional en su momento, y esa premisa cambió: el contrato Java ya acepta varios canales aunque el DDL sólo modele uno. |
| **Opciones posibles** | `A.` Reintroducir `canal_acceso_qr` en el DDL (requiere autorización expresa de DDL y revisar `SPEC-MODEL-DDL-CLOSURE-001`). `B.` Restringir el contrato Java de `registrarAcceso`/`FalActaQrAcceso` a un único canal persistible (`PORTAL`), eliminando `APP`, `INTEGRACION` y `OTRO` si no tienen consumidor real. `C.` Aceptar la pérdida de fidelidad como comportamiento permanente y documentar que el canal sólo es confiable en el resultado inmediato de la operación, nunca en una relectura. Ninguna opción se elige aquí. |
| **Ausencia de impacto sobre la atomicidad cerrada en esta wave** | Este gap **no afecta** la transacción acceso + `QRACCA` + snapshot ni la independencia posterior de la notificación: el canal no participa de ninguna de las tres escrituras condicionadas por rollback, y `AccesoQrResultado.canal()` en el resultado inmediato sigue siendo correcto tras la corrección de identidad de esta misma wave. |
| **Tests/guardrails** | `QrAccesoTransaccionalIT` caso 11 demuestra el gap materialmente contra MariaDB real. No se agregó ningún guardrail que impida el estado actual, porque el estado actual **es** el comportamiento documentado, no una regresión a prevenir. |

---

## Remediación integral del alta de acta (`ACTA-ALTA-INTEGRAL-JDBC-MASTER-20260804-224853`)

> Este MASTER-RUN superó explícitamente el modelo de captura progresiva que certificaron
> `00-governance/ready-for-ddl-gate.md` y
> `00-governance/ready-for-backend-clean-room-reconstruction.md` hasta 2026-07-18. Las
> decisiones funcionales (`DECISION-ACTA-*`) viven en
> [`../20-application/command-contracts.md`](../20-application/command-contracts.md); esta
> sección registra únicamente las consecuencias físicas/DDL.

### `DECISION_DDL-SIN-CHECKS-CODIGOS-01` — eliminación de CHECKs de código/enum/estado y de regla funcional no justificada

| Campo | Detalle |
|---|---|
| **Decisión adoptada** | Se elimina todo `CHECK` de `database/ddl/create-bod-faltas-domain.sql` cuyo propósito sea validar un código, enum, estado, bloque, evento, tipo de acta o combinación funcional entre campos, salvo justificación técnica explícita registrada en esta misma sección. La integridad de esos dominios pasa a garantizarse por tipo/tamaño de columna, `PRIMARY KEY`/`FOREIGN KEY`/`UNIQUE`, `version_row`/OCC, validación de dominio Java y tests, no por `CHECK`. |
| **Alcance material (Wave 2)** | Inventario completo de CHECKs por tabla reconstruido contra `database/ddl/create-bod-faltas-domain.sql` y el esquema real. Candidatos a eliminación: `chk_fal_persona_tipo`, `chk_num_pol_clase`, `chk_dia_nc_tipo`, `chk_dia_nc_origen`, `chk_fal_lote_estado`, `chk_tal_insp_estado`, `chk_pers_dom_tipo`, `chk_pers_dom_modo`, `chk_acta_tipo`, `chk_acta_situacion_infractor`, `chk_valorizacion_estado`, `chk_redaccion_estado`, `chk_fallo_estado`, `chk_fallo_tipo`, `chk_fallo_origen_firmeza`, `chk_apelacion_estado`, `chk_oblig_tipo`, `chk_oblig_origen`, `chk_oblig_estado`, `chk_forma_pago_tipo`, `chk_plan_pago_estado`, `chk_pago_mov_tipo`, `chk_pago_mov_clasificacion`, `chk_pago_mov_motivo_anulacion`, más los de coherencia funcional (`chk_acta_infractor_coherente`, `chk_dia_nc_ref_ext`, `chk_obs_baja_coherente`, `chk_fal_persona_id_bie_suj`, coherencia de documento de persona). Los CHECK de invariante de formato de dato (longitud de hash, regex de código de lote, rangos `>0`) se evalúan caso por caso y pueden conservarse con justificación explícita. |
| **Consecuencia sobre `DECISION_DDL-RF-005`** | La consecuencia DDL original de `RF-005` ("el `CHECK` de `fal_acta.resultado_final` debe aceptar los códigos 0..9") queda **superada**: no se agrega ese `CHECK` porque congelaría un catálogo de código cerrado. El valor `LEGACY_RESERVED` (código 5) y el resto del catálogo siguen garantizados por `ResultadoFinalActa` en Java, no por `CHECK` físico. No se modifica ninguna otra parte de `RF-005`. |
| **Ejecución** | `EXECUTED`. Ejecutada en la Wave 2 de `ACTA-ALTA-INTEGRAL-JDBC-MASTER-20260804-224853` contra el esquema real `sb_faltas_db` (sandbox de desarrollo local). 58 CHECKs eliminados de 24 tablas del dominio de 64 tablas canónicas; 1 conservado con justificación individual (`fal_acta_evidencia.chk_evidencia_hash`, invariante criptográfico fijo SHA-256 = 64 caracteres, no es catálogo de código). Explícitamente fuera de alcance: los 38 CHECKs de las tablas de la integración EDA Faltas↔Firmas (`bus_evento`, `fal_bus_checkpoint`, `fal_documento_firma`, `fal_documento_firma_req`, `fal_documento_firma_resultado`, `fal_inbox`, `fal_outbox`, `fir_bus_checkpoint`, `fir_inbox`, `fir_outbox`), gobernados por sus propias decisiones `DEC-EDA-*` y no alcanzados por este prompt. Comentario físico de `fal_acta.bloque_actual` corregido para retirar `CAPT` (columna sin `CHECK` físico, solo documental). Verificado: conteos de todas las tablas maestras y transaccionales idénticos antes/después (0 diferencias); DDL canónica (`database/ddl/create-bod-faltas-domain.sql`) actualizada y validada end-to-end contra una base temporal descartable (64/64 tablas creadas, exactamente 1 `CHECK` `chk_%` remanente). Migración forward/rollback en `.work/Auditorias/ACTA-ALTA-INTEGRAL-JDBC-MASTER-20260804-224853/wave-02-ddl/`. |
| **Tests/guardrails** | Verificación material ejecutada: `information_schema.CHECK_CONSTRAINTS` post-migración (39 CHECKs remanentes: 38 EDA + 1 conservado); `information_schema.TABLES`/`COUNT(*)` idénticos antes/después en las 94 tablas reales de `sb_faltas_db`; recreación completa del script canónico en base descartable (`64 tablas_creadas`, `1 checks_chk`). Pendiente para una wave posterior: tests de integración Java que confirmen que la validación de dominio sigue rechazando valores antes protegidos por los CHECKs retirados (alcance de Wave 3/4, no de esta decisión física). |

### Nota de superación sobre `D-POST-B2-CROSS-COMMAND-TX-01`

La fila de hallazgo de `D-POST-B2-CROSS-COMMAND-TX-01` (más arriba) menciona
`ActaService.completarCaptura` como uno de los métodos que sí tenía `@Transactional`
efectiva. `completarCaptura` fue eliminado por `DECISION-ACTA-LIFECYCLE-SIN-CAPT-01`: el
hallazgo original se preserva sin editar como evidencia histórica del estado previo a
esta remediación; el método ya no existe en el código productivo desde la Wave 3 de
`ACTA-ALTA-INTEGRAL-JDBC-MASTER-20260804-224853`.

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
