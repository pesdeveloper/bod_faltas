# 110 — Matriz Maestra de Paridad MariaDB ↔ InMemory

Slice: 8F-11A + 8F-11A-R1 + 8F-11F (corrección documental) / 8F-11D + 8F-11D-R1 (valorizacion + invariantes) / 8F-11F (fallo + apelacion + firmeza) / **8F-11G (observacion + paralizacion + archivo + motivo_archivo)**
**Fecha:** 2026-07-04 (8F-11A) / 2026-07-05 (8F-11A-R1, 8F-11B, 8F-11C) / 2026-07-06 (8F-11D, 8F-11D-R1, 8F-11E, 8F-11F)
**Tipo:** Auditoría y documentación exhaustiva. Sin cambios funcionales Java.
**Módulo:** backend/api-faltas-core
**Build base:** 1976 tests, 0 failures, 0 errors, BUILD SUCCESS (verificado en 8F-11F)
**Fuente vigente:** este documento reemplaza a 109 como matriz de paridad activa. El 109 queda como historial de auditoría de 8F-9/8F-10. Corregido en 8F-11A-R1: conteos (62/29 INMEMORY). 8F-11B cerrado: identidades, enums, versionRow. 8F-11C cerrado: FalPersona+FalPersonaDomicilio ALINEADOS (27→21 FALTA_EN_INMEMORY), P1 cerrada (SMALLINT 0-4), P2 cerrada (firmeza en fallo), catálogos, roadmap. 8F-11D+R1 cerrado: 6 tablas normativa/valorizacion ALINEADOS (21 FALTA_EN_INMEMORY), 30 tests nuevos, 1785 total. Correccion 8F-11D-R1-DOC: FALTA_EN_INMEMORY canonicas=21. 8F-11E cerrado: combinacion documental, DocumentoVariableContextBuilder. 8F-11F cerrado: fal_acta_fallo + fal_acta_apelacion ALINEADOS, fal_acta_apelacion_documento NUEVO ALINEADO, firmeza inline, multi-fallo, 4 tipos resolucion, 1976 tests, 39 tests nuevos. **8F-11I cerrado: fal_notificacion_intento + fal_notificacion_acuse + fal_lote_correo ALINEADOS (1 FALTA_EN_INMEMORY restante), 2247 tests, +140 tests.** **8F-11J cerrado: fal_acta_documento ALINEADO (FalActaDocumento+ActaDocumentoId+RolDocuActa+InMemoryActaDocumentoRepository+ActaDocumentoService, idDocuUlt en snapshot), 2280 tests, +33 tests. RELACION_INCOMPLETA=0.** **8F-11M-B1 cerrado: fal_acta_economia_proyeccion + modelo pagos InMemory ALINEADOS (importe_aplicado_total, enums canonicos), 2374 tests.** **8F-11K cerrado: fal_acta_qr_acceso ALINEADO (FalActaQrAcceso+CanalAccesoQr+ResultadoAccesoQr+QrAccesoRepository+InMemoryQrAccesoRepository+QrActaService+AesGcmQrTokenProtector), codigoQr en FalActa, 59 ALINEADO, 0 FALTA_EN_INMEMORY.** **8F-11M-B1-R2 cerrado: economia InMemory endurecida (movimiento append-only con vinculo canonico unico movimientoOrigenId, conciliacion solo agregada reconstruida desde input absoluto de Tesoreria sin historia local por movimiento, snapshot sin economia, reversos que reviven obligacion/forma, JWT obligatorio en /api/faltas/pagos, idempotencia completa, plan finalizado atomico), 2400 tests.** **8F-11M-B1-R2-R1 aplicado: reverso atomico revertirMovimiento (1 mov + 1 PAGREV + recalculo automatico, endpoint generico bloqueado), siPagoConfirmado vigente (hayPagoConfirmadoActivo), idempotencia reforzada (ref obligatoria, motivo validado, nextId lazy), bypass plan bloqueado, snapshot absoluto reemplazable (reemplazarEstadoAbsoluto+snapshotActual), 2418 tests.**

---

## 0. Objetivo y guardrails

Este documento es la fuente única de verdad sobre la paridad entre el modelo MariaDB lógico documentado y la implementación InMemory actual.

**Criterio de completitud:** Toda tabla MariaDB tiene estado. Toda entidad persistible InMemory tiene tabla candidata. Toda diferencia tiene acción y slice asignado.

**Guardrails de este slice:**
- NO implementar JDBC, MariaDB real, SQL ejecutable, Flyway, Liquibase
- NO usar `@Entity`, `JpaRepository`, `EntityManager`
- NO implementar frontend Angular, PDF real, storage real
- NO crear endpoints productivos nuevos
- NO reabrir decisiones D1–D9 ya cerradas

---

## 1. Resumen ejecutivo de paridad

| Categoría | Cantidad |
|-----------|----------|
| Tablas MariaDB auditadas | 62 |
| Entidades InMemory persistibles | 31 |
| FALTA_EN_MARIADB | 0 |
| **ALINEADO** | **59 tablas** (8F-11L: +2 fal_acta, fal_acta_evento) (8F-11H: +4 pagos; 8F-11I: +3 notificaciones; 8F-11J: +1 fal_acta_documento; **8F-11K: +1 fal_acta_qr_acceso**) |
| **FALTA_EN_INMEMORY** | **0 tablas** |
| FALTA_EN_MARIADB | 0 |
| IDENTIDAD_INCOMPATIBLE | 0 (5 resueltas en 8F-11B) |
| TIPO_INCOMPATIBLE | 0 (3 resueltos en 8F-11B) |
| **PARCIAL** | **0 tablas** (cerrado en 8F-11L) (fal_acta, fal_acta_evento — fal_acta_fallo y fal_acta_apelacion ahora ALINEADOS por 8F-11F) |
| SEMANTICA_INCOMPATIBLE | 0 (resuelta en 8F-11H) |
| ENUM_SIN_CODIGO | 0 (2 resueltos en 8F-11B) |
| **RELACION_INCOMPLETA** | **0 tablas** (GAP-12 cerrado en 8F-11J) |
| **NO_PERSISTIBLE tablas** | **3 tablas** stor_* (NO_PERSISTIBLE desde dominio Faltas) |
| *Suma (categorias excluyentes)* | *33 + 21 + 4 + 0 + 1 + 3 = 62* |
| NULABILIDAD_INCOMPATIBLE | 0 |
| SOLO_DEMO_TEST | 21 clases Java |
| SOLO_INFRAESTRUCTURA | 2 categorías |
| NO_PERSISTIBLE entidades | 25+ (servicios, DTOs, enums puro Java) |


---

## 2. Inventario canónico MariaDB — Fase 1

### Sección 1 — Dependencias

#### `fal_dependencia`
- **Propósito:** Unidades administrativas (organismos/dependencias)
- **PK:** id BIGINT AUTO_INCREMENT
- **Campos clave:** codigo_dependencia VARCHAR(20), nombre VARCHAR(200), tipo_acta SMALLINT, si_activo BOOLEAN
- **Auditoría:** fh_alta, id_user_alta
- **Entidad Java esperada:** FalDependencia ✓ existe
- **Estado InMemory:** ALINEADO

#### `fal_dependencia_version`
- **Propósito:** Versiones de la dependencia con datos cambiantes
- **PK:** id_dependencia + version_dep (compuesta)
- **FK:** id_dependencia → fal_dependencia
- **Campos clave:** nombre_version, tipo_acta SMALLINT, si_activo, fh_vig_desde, fh_vig_hasta
- **Auditoría:** fh_alta, id_user_alta
- **Entidad Java esperada:** FalDependenciaVersion ✓ existe
- **Estado InMemory:** ALINEADO

---

### Sección 2 — Inspectores

#### `fal_inspector`
- **Propósito:** Inspectores/agentes del organismo
- **PK:** id BIGINT AUTO_INCREMENT
- **Campos clave:** legajo VARCHAR(20), apellido, nombre, si_activo BOOLEAN
- **Auditoría:** fh_alta, id_user_alta
- **Entidad Java esperada:** FalInspector ✓ existe
- **Estado InMemory:** ALINEADO (base)

#### `fal_inspector_version`
- **Propósito:** Versión del inspector con datos del período
- **PK:** id_inspector + version_insp (compuesta)
- **FK:** id_inspector → fal_inspector
- **Campos clave:** apellido, nombre, id_dependencia, ver_dependencia, si_activo, fh_vig_desde, fh_vig_hasta
- **Auditoría:** fh_alta, id_user_alta
- **Entidad Java esperada:** FalInspectorVersion ✓ existe
- **Estado InMemory:** COMPLETO

---

### Sección 3 — Firmantes

#### `fal_firmante`
- **PK:** id BIGINT AUTO_INCREMENT
- **Entidad Java:** FalFirmante ✓ existe — **Estado:** ALINEADO (8A-3D)

#### `fal_firmante_version`
- **PK:** id_firmante + ver_firmante (compuesta)
- **FK:** id_firmante → fal_firmante
- **Entidad Java:** FalFirmanteVersion ✓ existe — **Estado:** ALINEADO

#### `fal_firmante_version_habilitacion`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_firmante + ver_firmante → fal_firmante_version
- **Campos clave:** tipo_docu SMALLINT, rol_firma_req SMALLINT, mecanismo_firma_req SMALLINT
- **Entidad Java:** FalFirmanteVersionHabilitacion ✓ existe — **Estado:** ALINEADO

---

### Sección 4 — Personas y domicilios

#### `fal_persona`
- **Propósito:** Maestro de personas (infractores, presentantes)
- **PK:** id BIGINT AUTO_INCREMENT
- **Campos clave:** nro_documento VARCHAR(20), tipo_documento SMALLINT, nombre_completo VARCHAR(300), fh_nacimiento DATE NULL, sexo SMALLINT NULL, si_identificado BOOLEAN, si_extranjero BOOLEAN, id_ign BIGINT NULL, id_indec BIGINT NULL, id_local BIGINT NULL
- **Auditoría:** fh_alta, id_user_alta, fh_mod, id_user_mod
- **Entidad Java esperada:** `FalPersona` — **EXISTE**
- **Estado InMemory:** ALINEADO — InMemoryPersonaRepository — 8F-11C CERRADO (2026-07-05)

#### `fal_persona_domicilio`
- **Propósito:** Domicilios de personas
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_persona → fal_persona
- **Campos clave:** tipo_domicilio SMALLINT, id_calle_version BIGINT NULL FK, nro_puerta VARCHAR(10) NULL, piso VARCHAR(5) NULL, dpto VARCHAR(5) NULL, localidad_id BIGINT NULL, texto_libre VARCHAR(500) NULL, si_principal TINYINT(1), id_tca BIGINT NULL, id_loc BIGINT NULL
- **Auditoría:** fh_alta, id_user_alta
- **Entidad Java esperada:** `FalPersonaDomicilio` — **EXISTE**
- **Estado InMemory:** ALINEADO — InMemoryPersonaDomicilioRepository — 8F-11C CERRADO (2026-07-05)

---

### Sección 5 — Acta core

#### `fal_acta`
- **Propósito:** Entidad central del expediente de faltas
- **PK:** id BIGINT AUTO_INCREMENT
- **UK:** uuid_tecnico CHAR(36)
- **FK:** id_talonario → num_talonario, id_inspector + ver_inspector → fal_inspector_version, id_dependencia + ver_dependencia → fal_dependencia_version, id_persona → fal_persona (futuro)
- **Campos:**
  - nro_acta VARCHAR(20) NULL
  - tipo_acta SMALLINT NOT NULL (enum TipoActa)
  - id_talonario BIGINT NULL, nro_talonario_usado INT NULL
  - fecha_acta DATE NOT NULL, fecha_labrado DATETIME(6) NOT NULL
  - domicilio_hecho VARCHAR(500) NULL, lat_infr DOUBLE NULL, lon_infr DOUBLE NULL
  - resultado_firma_infractor SMALLINT NOT NULL
  - bloque_actual SMALLINT NOT NULL, estado_procesal SMALLINT NOT NULL
  - situacion_administrativa SMALLINT NOT NULL
  - resultado_final SMALLINT NOT NULL (o VARCHAR(30) — decisión P1)
  - id_persona BIGINT NULL FK
- **Auditoría:** fh_alta, id_user_alta, fh_mod, id_user_mod
- **Entidad Java:** FalActa ✓ existe
- **Estado InMemory:** ALINEADO (8F-11L: todos los campos del modelo MariaDB incorporados, OrigenCaptura, ver_dep, ver_insp, geolocation, location local, qrPayloadVersion, fhCierre, fhArchivo) — gaps: tipoActa String, resultadoFinal tipo, datos persona embebidos, sin auditoría

---

### Sección 6 — Eventos, snapshot, evidencias y observaciones

#### `fal_acta_evento`
- **Propósito:** Log append-only de eventos del expediente
- **PK:** id BIGINT AUTO_INCREMENT (o UUID — verificar modelo final)
- **FK:** id_acta → fal_acta
- **Campos clave:** tipo_evt SMALLINT NOT NULL, fecha_evento DATETIME(6), orden_logico INT, id_documento BIGINT NULL, id_notificacion BIGINT NULL, id_operador CHAR(36), descripcion TEXT NULL, payload JSON NULL
- **Entidad Java:** FalActaEvento → si (class final con Builder, id Long) → **Estado:** ALINEADO (8F-11L: reescrita record→class+Builder, id Long, origenEvt, bloqueFunc, estProcAnt/Nvo, sitAdmAnt/Nva, actorTipo, actorId, actorRef, idDocuRel, idNotifRel, idPresRel, idUserEvt, siEvtCierre, siEvtExt, siPermiteReing, descripcionLegible, correlacionId)

#### `fal_acta_snapshot`
- **Propósito:** Proyección operativa derivada del expediente (1:1 con acta)
- **PK:** id_acta BIGINT FK
- **Entidad Java:** FalActaSnapshot ✓ — **Estado:** ALINEADO

#### `fal_acta_evidencia`
- **Propósito:** Evidencias del acta
- **PK:** id BIGINT AUTO_INCREMENT
- **Entidad Java:** FalActaEvidencia ✓ — **Estado:** ALINEADO

#### `fal_observacion`
- **Propósito:** Observaciones tipificadas por entidad
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_acta → fal_acta
- **Campos clave:** entidad_tipo SMALLINT, entidad_id BIGINT, texto TEXT, id_user_alta, fh_alta
- **Entidad Java:** NO EXISTE
- **Entidad Java:** FalObservacion -- EXISTE (8F-11G)
- **Estado InMemory:** ALINEADO -- ObservacionRepository + InMemoryObservacionRepository + ObservacionService + EntidadTipoObservada (22 codigos) -- 8F-11G

---

### Sección 7 — Normativa

#### `fal_dependencia_normativa`
- **Entidad Java:** FalDependenciaNormativa ✓ — **Estado:** ALINEADO

#### `fal_normativa_faltas`
- **Entidad Java:** FalNormativaFaltas ✓ — **Estado:** ALINEADO

#### `fal_articulo_normativa_faltas`
- **Entidad Java:** FalArticuloNormativaFaltas ✓ — **Estado:** ALINEADO

#### `fal_tarifario_unidad_faltas`
- **Propósito:** Valores unitarios por artículo para cálculo de valorización
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_articulo → fal_articulo_normativa_faltas
- **Campos clave:** fh_vig_desde DATE, fh_vig_hasta DATE NULL, valor_unitario DECIMAL(14,4)
- **Entidad Java:** NO EXISTE
- **Estado InMemory:** ALINEADO (8F-11D + R1-D) — sin solapamiento de rangos activos

#### `fal_medida_preventiva`
- **Entidad Java:** FalMedidaPreventiva EXISTE — version atomica, una activa por codigo
- **Estado:** ALINEADO (8F-11D + R1-D)

#### `fal_articulo_medida_preventiva`
- **Entidad Java:** FalArticuloMedidaPreventiva EXISTE — PK compuesta record, sin reactivacion silenciosa
- **Estado:** ALINEADO (8F-11D + R1-D)

---

### Sección 8 — Artículos infringidos y valorización

#### `fal_acta_articulo_infringido`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_acta → fal_acta, id_articulo → fal_articulo_normativa_faltas
- **Entidad Java:** NO EXISTE
- **Estado InMemory:** ALINEADO (8F-11D + R1-D) — UK activo (actaId, articuloId)

#### `fal_acta_valorizacion`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_acta → fal_acta
- **Campos:** monto_total DECIMAL(14,2), fh_calculo DATETIME(6), si_vigente BOOLEAN
- **Entidad Java:** FalActaValorizacion EXISTE — confirmarVigenteAtomico garantiza una sola vigente CONFIRMADA por acta+tipo
- **Estado InMemory:** ALINEADO (8F-11D + R1-B)

#### `fal_acta_valorizacion_item`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_valorizacion → fal_acta_valorizacion
- **Entidad Java:** FalActaValorizacionItem EXISTE — inmutable post-confirmacion (R1-C)
- **Estado:** ALINEADO (8F-11D + R1-C)

---

### Sección 9 — Satélites del acta

#### `fal_acta_transito`
- **PK:** id_acta BIGINT (1:1)
- **Entidad Java:** NO EXISTE — **Estado:** **ALINEADO** (8F-11E)

#### `fal_acta_transito_alcoholemia`
- **PK:** id BIGINT AUTO_INCREMENT
- **Entidad Java:** NO EXISTE — **Estado:** **ALINEADO** (8F-11E)

#### `fal_acta_vehiculo`
- **PK:** id BIGINT AUTO_INCREMENT
- **Entidad Java:** NO EXISTE — **Estado:** **ALINEADO** (8F-11E)

#### `fal_acta_contravencion`
- **PK:** id_acta BIGINT (1:1)
- **Entidad Java:** NO EXISTE — **Estado:** **ALINEADO** (8F-11E)

#### `fal_acta_sustancias_alimenticias`
- **PK:** id_acta BIGINT (1:1)
- **Entidad Java:** NO EXISTE — **Estado:** **ALINEADO** (8F-11E)

#### `fal_acta_medida_preventiva`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_acta → fal_acta, id_medida → fal_medida_preventiva
- **Entidad Java:** NO EXISTE — **Estado:** **ALINEADO** (8F-11E)

---

### Sección 10 — Bloqueantes

#### `fal_acta_bloqueante_cierre_material`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_acta → fal_acta
- **Campos:** origen SMALLINT, estado SMALLINT, si_activo BOOLEAN, descripcion TEXT NULL, fh_alta DATETIME(6), fh_cierre DATETIME(6) NULL
- **Entidad Java:** FalBloqueanteMaterial ✓, **id String (UUID)**
- **Estado InMemory:** ALINEADO (8F-11B)


---

### Sección 11 — Documentos, plantillas y firma

#### `fal_documento`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_acta → fal_acta, id_plantilla → fal_documento_plantilla
- **Campos:** tipo_docu SMALLINT, nro_docu VARCHAR(30) NULL, estado_docu SMALLINT, storage_key VARCHAR(500) NULL, hash_docu VARCHAR(128) NULL, tipo_firma_req SMALLINT, fh_alta, id_user_alta
- **Entidad Java:** FalDocumento ✓ — **Estado:** ALINEADO (auditado 8C-6E)

#### `fal_acta_documento`
- **Propósito:** Tabla pivot relación canónica acta-documento (pertenencia funcional)
- **PK:** id_acta + id_documento (compuesta, sin ID artificial)
- **Campos:** rol_docu_acta SMALLINT, si_principal BOOLEAN, fh_alta DATETIME(6), id_user_alta CHAR(36)
- **Entidad Java:** `FalActaDocumento` + `ActaDocumentoId` (value object) — slice **8F-11J**
- **Repository:** `ActaDocumentoRepository` / `InMemoryActaDocumentoRepository` (thread-safe, principalLock)
- **Service:** `ActaDocumentoService` (asociar, asociarPrincipal, reemplazarPrincipal, validaciones rol/tipo)
- **Enum:** `RolDocuActa` (12 valores, codigo short estable)
- **Snapshot:** `FalActaSnapshot.idDocuUlt` derivado via pivot (FALLO > RESOLUCION > NOTIFICACION > ACTA_PRINCIPAL)
- **Estado InMemory:** **ALINEADO** (GAP-12 cerrado — 8F-11J)

#### `fal_documento_firma`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_documento → fal_documento, id_firmante + ver_firmante → fal_firmante_version
- **Campos:** estado_firma SMALLINT, seq_firma_req SMALLINT, tipo_firma SMALLINT, storage_key VARCHAR(500) NULL, hash_firma VARCHAR(128) NULL, fh_firma DATETIME(6) NULL
- **Entidad Java:** FalDocumentoFirma ✓ — **Estado:** ALINEADO (refactor 8C-6B-1)

#### `fal_documento_firma_req`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_documento → fal_documento
- **Entidad Java:** FalDocumentoFirmaReq ✓ — **Estado:** ALINEADO

#### `fal_documento_plantilla`
- **PK:** id BIGINT AUTO_INCREMENT
- **Campos:** codigo VARCHAR(50), nombre VARCHAR(200), tipo_docu SMALLINT, accion_documental SMALLINT, tipo_acta SMALLINT NULL, tipo_firma_req SMALLINT, si_requiere_numeracion BOOLEAN, momento_numeracion_docu SMALLINT, si_notificable BOOLEAN, si_genera_pdf BOOLEAN, si_seleccionable BOOLEAN, si_activa BOOLEAN, fh_vig_desde DATE, fh_vig_hasta DATE NULL, fh_alta DATETIME(6), id_user_alta CHAR(36)
- **Entidad Java:** FalDocumentoPlantilla ✓ — **Estado:** ALINEADO

#### `fal_documento_plantilla_firma_req`
- **PK:** id_plantilla + seq_firma_req (compuesta)
- **Entidad Java:** FalDocumentoPlantillaFirmaReq ✓ — **Estado:** ALINEADO

#### `fal_documento_plantilla_contenido` (nueva en 8F-10)
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_plantilla → fal_documento_plantilla
- **Campos:**
  - version_contenido SMALLINT NOT NULL
  - titulo VARCHAR(200) NOT NULL
  - cuerpo_markdown TEXT NOT NULL ← renombrado desde cuerpo_template (8F-10-R1)
  - encabezado_markdown TEXT NULL ← renombrado desde encabezado_template (8F-10-R1)
  - pie_markdown TEXT NULL ← renombrado desde pie_template (8F-10-R1)
  - variables_declaradas_json JSON NOT NULL DEFAULT '[]' (D4)
  - si_activo BOOLEAN NOT NULL
  - fh_vig_desde DATETIME(6) NOT NULL, fh_vig_hasta DATETIME(6) NULL
  - fh_alta DATETIME(6) NOT NULL, id_user_alta CHAR(36) NOT NULL
- **Nota:** formato/FormatoPlantillaContenido ELIMINADO en 8F-10-R1
- **Entidad Java:** FalDocumentoPlantillaContenido ✓ — **Estado:** ALINEADO (8F-10-R2)

#### `fal_documento_plantilla_default` (nueva en 8F-10)
- **PK:** id BIGINT AUTO_INCREMENT
- **Índice:** (accion_documental, tipo_acta, id_dependencia)
- **Campos:**
  - accion_documental SMALLINT NOT NULL
  - tipo_acta SMALLINT NULL (NULL = genérico)
  - tipo_docu SMALLINT NOT NULL
  - id_dependencia BIGINT NULL, ver_dependencia SMALLINT NULL
  - id_plantilla BIGINT NOT NULL FK → fal_documento_plantilla
  - prioridad SMALLINT NOT NULL (D9)
  - fh_vig_desde DATETIME(6) NOT NULL, fh_vig_hasta DATETIME(6) NULL
  - si_activo BOOLEAN NOT NULL
  - fh_alta DATETIME(6) NOT NULL, id_user_alta CHAR(36) NOT NULL
- **Entidad Java:** FalDocumentoPlantillaDefault ✓ — **Estado:** ALINEADO

#### `fal_documento_redaccion` (nueva en 8F-10)
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_documento → fal_documento, id_plantilla_contenido → fal_documento_plantilla_contenido, redaccion_origen_id → fal_documento_redaccion (self)
- **UK:** id_documento + nro_revision
- **Campos:**
  - nro_revision SMALLINT NOT NULL
  - redaccion_origen_id BIGINT NULL (self FK)
  - estado_redaccion SMALLINT NOT NULL (BORRADOR=1, CONFIRMADA=2, ANULADA=3)
  - contenido_base_markdown TEXT NOT NULL, contenido_editable_markdown TEXT NOT NULL
  - variables_snapshot_json JSON NOT NULL DEFAULT '{}', variables_faltantes_json JSON NOT NULL DEFAULT '[]'
  - diagnostico_json JSON NOT NULL DEFAULT '{}', recursos_snapshot_json JSON NULL
  - fh_creacion DATETIME(6) NOT NULL, id_user_creacion CHAR(36) NOT NULL
  - fh_ultima_regeneracion DATETIME(6) NULL, id_user_ultima_regeneracion CHAR(36) NULL
  - fh_ultima_edicion DATETIME(6) NULL, id_user_ultima_edicion CHAR(36) NULL
  - fh_confirmacion DATETIME(6) NULL, id_user_confirmacion CHAR(36) NULL
  - fh_anulacion DATETIME(6) NULL, id_user_anulacion CHAR(36) NULL, motivo_anulacion TEXT NULL
- **Nota:** estado REABIERTA ELIMINADO en 8F-10-R1. 8F-10-R2 alineó entidad Java completamente.
- **Entidad Java:** FalDocumentoRedaccion ✓ — **Estado:** ALINEADO (8F-10-R2) ← COMPLETAMENTE ALINEADA

---

### Sección 12 — Notificaciones

#### `fal_notificacion`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_acta → fal_acta, id_documento → fal_documento
- **Campos:** tipo_docu_notificado SMALLINT, canal VARCHAR(50), fecha_envio DATETIME(6), estado SMALLINT, resultado SMALLINT NULL, fecha_resultado DATETIME(6) NULL, intentos INT, observaciones TEXT NULL
- **Entidad Java:** FalNotificacion ✓, **id String (UUID)**
- **Estado InMemory:** ALINEADO (8F-11B)

#### `fal_notificacion_intento`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_notificacion → fal_notificacion
- **Campos:** nro_intento SMALLINT, canal VARCHAR(50), fecha_intento DATETIME(6), resultado SMALLINT, detalle TEXT NULL
- **Entidad Java:** FalNotificacionIntento -- **Estado:** ALINEADO (8F-11I)

#### `fal_notificacion_acuse`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_notificacion → fal_notificacion
- **Entidad Java:** FalNotificacionAcuse -- **Estado:** ALINEADO (8F-11I)

#### `fal_lote_correo`
- **PK:** id BIGINT AUTO_INCREMENT
- **Campos:** tipo_lote SMALLINT, fecha_generacion DATETIME(6), estado_lote SMALLINT, id_user_alta CHAR(36)
- **Entidad Java:** FalLoteCorreo -- **Estado:** ALINEADO (8F-11I)

---

### Sección 13 — Fallo y firmeza

#### `fal_acta_fallo`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_acta → fal_acta, valorizacion_id → fal_acta_valorizacion, fallo_reemplazado_id → fal_acta_fallo (self), documento_id → fal_documento
- **Campos:**
  - tipo_fallo SMALLINT NOT NULL, estado_fallo SMALLINT NOT NULL
  - monto_condena DECIMAL(14,2) NULL, resultado_fallo SMALLINT NULL
  - fundamentos TEXT NULL (D5), documento_id BIGINT NULL FK
  - valorizacion_id BIGINT NULL FK, fallo_reemplazado_id BIGINT NULL FK
  - fecha_dictado DATETIME(6) NOT NULL, fecha_notificacion DATETIME(6) NULL
  - fecha_resultado_final DATETIME(6) NULL
  - fh_firma DATETIME(6) NULL, fh_vto_apelacion DATETIME(6) NULL
  - si_activo BOOLEAN NOT NULL
  - si_firme BOOLEAN NOT NULL DEFAULT FALSE (D1)
  - fh_firmeza DATETIME(6) NULL (D1)
  - origen_firmeza SMALLINT NULL (D1: 1=VENCIMIENTO_PLAZO_APELACION, 2=APELACION_RECHAZADA)
  - version_row INT NOT NULL DEFAULT 0
- **Auditoría:** fh_alta, id_user_alta, fh_mod, id_user_mod
- **Entidad Java:** FalActaFallo ✓, **id String (UUID)**
- **Estado InMemory:** IDENTIDAD_INCOMPATIBLE + múltiples campos FALTA_EN_INMEMORY — Slice 8F-11B (id), 8F-11F (campos)

> **Nota D1:** FalActaFirmezaCondena NO tiene tabla propia. Los campos de firmeza van en fal_acta_fallo.
> La entidad Java FalActaFirmezaCondena existe actualmente; ver decisión P2.

---

### Sección 14 — Apelación

#### `fal_acta_apelacion`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_acta → fal_acta, id_fallo → fal_acta_fallo, documento_resolucion_id → fal_documento
- **Campos:**
  - fallo_id BIGINT NOT NULL FK
  - estado_apelacion SMALLINT NOT NULL, fecha_presentacion DATETIME(6) NOT NULL
  - canal_apelacion SMALLINT NOT NULL, tipo_presentacion SMALLINT NOT NULL
  - texto_apelacion TEXT NULL, presentante VARCHAR(300) NULL
  - fundamentos TEXT NULL, observaciones TEXT NULL, si_activa BOOLEAN NOT NULL
  - fecha_resolucion DATETIME(6) NULL, fundamentos_resolucion TEXT NULL
  - observaciones_resolucion TEXT NULL, documento_resolucion_id BIGINT NULL FK
  - version_row INT NOT NULL DEFAULT 0
- **Auditoría:** fh_alta, id_user_alta, fh_mod, id_user_mod
- **Entidad Java:** FalActaApelacion ✓, **id String (UUID), falloId String**
- **Estado InMemory:** IDENTIDAD_INCOMPATIBLE + múltiples campos FALTA_EN_INMEMORY — Slice 8F-11B, 8F-11F

#### `fal_acta_apelacion_documento`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_apelacion → fal_acta_apelacion, id_documento → fal_documento
- **Entidad Java:** NO EXISTE — **Estado:** FALTA_EN_INMEMORY, Slice 8F-11F

---

### Sección 15 — Paralización

#### `fal_acta_paralizacion`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_acta → fal_acta
- **Campos:** motivo SMALLINT NOT NULL, descripcion TEXT NULL, id_user_inicio CHAR(36) NOT NULL, fh_inicio DATETIME(6) NOT NULL, id_user_cierre CHAR(36) NULL, fh_cierre DATETIME(6) NULL, motivo_cierre SMALLINT NULL, si_activa BOOLEAN NOT NULL
- **Auditoría:** fh_alta, id_user_alta
- **Entidad Java:** NO EXISTE (FalActaParalizacion)
- **Entidad Java:** FalActaParalizacion -- EXISTE (8F-11G)
- **Estado InMemory:** ALINEADO -- ActaParalizacionRepository + InMemoryActaParalizacionRepository + ParalizacionActaService -- 8F-11G

---

### Sección 16 — Archivo

#### `fal_acta_archivo`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_acta → fal_acta
- **Campos:** motivo_archivo SMALLINT NOT NULL, descripcion TEXT NULL, id_user_archivo CHAR(36) NOT NULL, fh_archivo DATETIME(6) NOT NULL, id_user_reingreso CHAR(36) NULL, fh_reingreso DATETIME(6) NULL, motivo_reingreso SMALLINT NULL, si_archivado BOOLEAN NOT NULL
- **Auditoría:** fh_alta, id_user_alta
- **Entidad Java:** NO EXISTE (FalActaArchivo)
- **Entidad Java:** FalActaArchivo -- EXISTE (8F-11G)
- **Estado InMemory:** ALINEADO -- ActaArchivoRepository + InMemoryActaArchivoRepository + ArchivoActaService -- 8F-11G

---

### Sección 17 — Gestión externa

#### `fal_acta_gestion_externa`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_acta → fal_acta
- **Entidad Java:** FalGestionExterna ✓, **id String (UUID)**
- **Estado InMemory:** ALINEADO (8F-11B)
- **Campos InMemory:** completamente alineados excepto id y auditoría (fh_alta, id_user_alta)

---

### Sección 18 — Pagos

#### `fal_acta_obligacion_pago`
- **Entidad Java:** `FalActaObligacionPago` + `ObligacionPagoRepository` / `InMemoryObligacionPagoRepository`
- **Enums:** `TipoObligacionPago`, `EstadoObligacionPago`
- **Estado InMemory:** **ALINEADO (8F-11M-B1)**

#### `fal_acta_forma_pago`
- **Entidad Java:** `FalActaFormaPago` + repos InMemory
- **Enums:** `TipoFormaPago`, `EstadoFormaPago`
- **Estado InMemory:** **ALINEADO (8F-11M-B1)**

#### `fal_acta_plan_pago_ref`
- **Entidad Java:** `FalActaPlanPagoRef` + repos InMemory
- **Enums:** `EstadoPlanPago`, `MotivoPlanCaidoCalculado`, `MotivoAptitudIntimacion`
- **Estado InMemory:** **ALINEADO (8F-11M-B1)**

#### `fal_acta_pago_movimiento`
- **Entidad Java:** `FalActaPagoMovimiento` append-only + `PagoMovimientoRepository`
- **Enums:** `TipoMovimientoPago`, `OrigenMovimiento`, `ClasificacionPago`, `EstadoConciliacionActual`, `OrigenUltimaActualizacion`
- **Estado InMemory:** **ALINEADO (8F-11M-B1)**

#### `fal_acta_economia_proyeccion`
- **Campo:** `importe_aplicado_total` DECIMAL(14,2) NOT NULL DEFAULT 0
- **Entidad Java:** `FalActaEconomiaProyeccion` + `EconomiaProyeccionRepository` / `InMemoryEconomiaProyeccionRepository`
- **Servicios:** `EconomiaProyeccionRecalculador`, `ProcesoNocturnoEconomicoService`
- **Estado InMemory:** **ALINEADO (8F-11M-B1)**

---

### Sección 19 — Talonarios y numeración

#### `num_politica`
- **Entidad Java:** NumPolitica ✓ — **Estado:** ALINEADO (deuda DDL fh_alta/id_user_alta en modelo MariaDB)

#### `num_talonario`
- **Entidad Java:** NumTalonario ✓ — **Estado:** ALINEADO

#### `num_talonario_ambito`
- **Entidad Java:** NumTalonarioAmbito ✓ — **Estado:** ALINEADO

#### `num_talonario_inspector`
- **Entidad Java:** NumTalonarioInspector ✓ — **Estado:** ALINEADO

#### `num_talonario_movimiento`
- **Entidad Java:** NumTalonarioMovimiento ✓ — **Estado:** ALINEADO


### Subseccion 19B - Paridad de concurrencia/idempotencia para numeracion documental (CIERRE-D14-D18)

Esta subseccion documenta la equivalencia entre la implementacion InMemory vigente y las garantias
productivas requeridas en el adapter MariaDB para el endpoint `POST /api/faltas/documentos/{id}/numerar`.

#### InMemory vigente (CIERRE-D14-D18)

| Aspecto | Implementacion InMemory |
|---------|------------------------|
| Exclusion | `synchronized` en `DocumentoService.numerarDocumentoParaFirmas` |
| Alcance | Una sola instancia JVM; no es mecanismo multiinstancia |
| Idempotencia | `nroDocu != null` -> devolver numero existente; `yaEstabaNumerado = true` |
| Evidencia | `DocumentoNumeracionFirmasTest` 17 casos; concurrencia testeada en instancia unica |
| Estado | ALINEADO con contrato observable |

#### MariaDB requerido (pendiente de implementacion en adapter JDBC)

| Aspecto | Requisito MariaDB |
|---------|------------------|
| Correlativo | `NEXT VALUE FOR <secuencia>` donde `<secuencia>` es `num_talonario.nombre_secuencia` |
| Unicidad | Constraint unica `(id_talonario, nro_talonario)` en `num_talonario_movimiento` |
| OCC documento | Actualizar `fal_documento.version_row` con condicion; rechazar si cambiado |
| Transaccion | Todo en una unica transaccion de negocio: movimiento + asociacion + OCC |
| Idempotencia | Verificar `nro_docu IS NOT NULL` antes de consumir otra SEQUENCE; recargar tras conflicto |
| Multiinstancia | Sin dependencia de locks JVM; SEQUENCE garantiza atomicidad entre instancias |
| Saltos tecnicos | Una SEQUENCE puede tener saltos por rollback/cache/reinicio; aceptables para electronicos |
| Estado | CONTRATO_FUNCIONAL_INMEMORY_ALINEADO / GARANTIA_PRODUCTIVA_PENDIENTE_ADAPTER_MARIADB |

**Diferencia clave electronico vs manual fisico:**
- Talonario electronico: atomicidad/unicidad garantizada por SEQUENCE + constraints; pueden existir saltos tecnicos aceptables.
- Talonario manual fisico: control exhaustivo de rangos fisicos; cada numero usado, anulado, devuelto o justificado debe estar contabilizado.

---

### Sección 20 — QR y portal

#### `fal_acta_qr_acceso`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_acta → fal_acta
- **Campos:** token_qr VARCHAR(200), url_acceso VARCHAR(500), fh_generacion DATETIME(6), fh_vencimiento DATETIME(6) NULL, si_activo BOOLEAN
- **Entidad Java:** FalActaQrAcceso (8F-11K)
- **Estado InMemory:** **ALINEADO** - FalActaQrAcceso + CanalAccesoQr + ResultadoAccesoQr + QrAccesoRepository + InMemoryQrAccesoRepository + QrActaService + AesGcmQrTokenProtector. Slice 8F-11K.


---

## 3. Inventario canónico InMemory — Fase 2

### A. Entidades de dominio persistibles

| Clase Java | Tabla MariaDB | ID tipo Java | ID tipo MariaDB | Estado |
|---|---|---|---|---|
| `FalActa` | `fal_acta` | Long → si | BIGINT → si | ALINEADO (8F-11L: todos los campos de MariaDB incorporados; gaps arquitecturales aceptados: tipo_acta String, persona embebida, sin auditoria DB) |
| `FalActaEvento` | `fal_acta_evento` | Long | ALINEADO | ALINEADO |
| `FalActaSnapshot` | `fal_acta_snapshot` | Long (idActa) | BIGINT PK ✓ | ALINEADO |
| `FalActaEvidencia` | `fal_acta_evidencia` | Long ✓ | BIGINT ✓ | ALINEADO |
| `FalActaFallo` | `fal_acta_fallo` | **Long | ALINEADO id (8F-11B) + campos (8F-11F) |
| `FalActaApelacion` | `fal_acta_apelacion` | **Long | ALINEADO id (8F-11B) + campos (8F-11F) |
| `FalActaFirmezaCondena` | campos en `fal_acta_fallo` (D1) | String | — sin tabla propia | SEMANTICA_INCOMPATIBLE |
| `FalBloqueanteMaterial` | `fal_acta_bloqueante_cierre_material` | **String** | **BIGINT** ✗ | IDENTIDAD_INCOMPATIBLE |
| `FalDependencia` | `fal_dependencia` | Long ✓ | BIGINT ✓ | ALINEADO |
| `FalDependenciaVersion` | `fal_dependencia_version` | Long+short | BIGINT+SMALLINT ✓ | ALINEADO |
| `FalDocumento` | `fal_documento` | Long ✓ | BIGINT ✓ | ALINEADO |
| `FalDocumentoFirma` | `fal_documento_firma` | Long ✓ | BIGINT ✓ | ALINEADO |
| `FalDocumentoFirmaReq` | `fal_documento_firma_req` | Long ✓ | BIGINT ✓ | ALINEADO |
| `FalDocumentoPlantilla` | `fal_documento_plantilla` | Long ✓ | BIGINT ✓ | ALINEADO |
| `FalDocumentoPlantillaFirmaReq` | `fal_documento_plantilla_firma_req` | Long+short | BIGINT+SMALLINT ✓ | ALINEADO |
| `FalDocumentoPlantillaContenido` | `fal_documento_plantilla_contenido` | Long ✓ | BIGINT ✓ | ALINEADO (8F-10-R2) |
| `FalDocumentoPlantillaDefault` | `fal_documento_plantilla_default` | Long ✓ | BIGINT ✓ | ALINEADO |
| `FalDocumentoRedaccion` | `fal_documento_redaccion` | Long ✓ | BIGINT ✓ | ALINEADO (8F-10-R2) |
| `FalFirmante` | `fal_firmante` | Long ✓ | BIGINT ✓ | ALINEADO |
| `FalFirmanteVersion` | `fal_firmante_version` | Long+short | BIGINT+SMALLINT ✓ | ALINEADO |
| `FalFirmanteVersionHabilitacion` | `fal_firmante_version_habilitacion` | Long ✓ | BIGINT ✓ | ALINEADO |
| `FalGestionExterna` | `fal_acta_gestion_externa` | **String** | **BIGINT** ✗ | IDENTIDAD_INCOMPATIBLE |
| `FalInspector` | `fal_inspector` | Long ✓ | BIGINT ✓ | ALINEADO |
| `FalInspectorVersion` | `fal_inspector_version` | Long+short | BIGINT+SMALLINT ✓ | PARCIAL (faltan campos firma) |
| `FalNormativaFaltas` | `fal_normativa_faltas` | Long ✓ | BIGINT ✓ | ALINEADO |
| `FalArticuloNormativaFaltas` | `fal_articulo_normativa_faltas` | Long ✓ | BIGINT ✓ | ALINEADO |
| `FalDependenciaNormativa` | `fal_dependencia_normativa` | Long+Long | — ✓ | ALINEADO |
| `FalNotificacion` | `fal_notificacion` | **Long | ALINEADO (8F-11B) |
| `FalPagoVoluntario` | `fal_acta_obligacion_pago` (tipo=1) | String | BIGINT ✗ | SEMANTICA_INCOMPATIBLE (D2 cerrada) |
| `FalPagoCondena` | `fal_acta_obligacion_pago` (tipo=2) | String | BIGINT ✗ | SEMANTICA_INCOMPATIBLE (D2 cerrada) |
| `NumPolitica` | `num_politica` | Long ✓ | BIGINT ✓ | ALINEADO |
| `NumTalonario` | `num_talonario` | Long ✓ | BIGINT ✓ | ALINEADO |
| `NumTalonarioAmbito` | `num_talonario_ambito` | — compuesta | — compuesta ✓ | ALINEADO |
| `NumTalonarioInspector` | `num_talonario_inspector` | Long ✓ | BIGINT ✓ | ALINEADO |
| `NumTalonarioMovimiento` | `num_talonario_movimiento` | Long ✓ | BIGINT ✓ | ALINEADO |

### B. Repositorios existentes (25 interfaces + 25 InMemory)

| Interfaz | InMemory | Entidad |
|---|---|---|
| `ActaRepository` | `InMemoryActaRepository` | FalActa |
| `ActaEventoRepository` | `InMemoryActaEventoRepository` | FalActaEvento |
| `ActaSnapshotRepository` | `InMemoryActaSnapshotRepository` | FalActaSnapshot |
| `ActaEvidenciaRepository` | `InMemoryActaEvidenciaRepository` | FalActaEvidencia |
| `ApelacionActaRepository` | `InMemoryApelacionActaRepository` | FalActaApelacion |
| `BloqueanteMaterialRepository` | `InMemoryBloqueanteMaterialRepository` | FalBloqueanteMaterial |
| `DependenciaRepository` | `InMemoryDependenciaRepository` | FalDependencia/Version |
| `DocumentoFirmaRepository` | `InMemoryDocumentoFirmaRepository` | FalDocumentoFirma |
| `DocumentoFirmaReqRepository` | `InMemoryDocumentoFirmaReqRepository` | FalDocumentoFirmaReq |
| `DocumentoPlantillaContenidoRepository` | `InMemoryDocumentoPlantillaContenidoRepository` | FalDocumentoPlantillaContenido |
| `DocumentoPlantillaDefaultRepository` | `InMemoryDocumentoPlantillaDefaultRepository` | FalDocumentoPlantillaDefault |
| `DocumentoPlantillaRepository` | `InMemoryDocumentoPlantillaRepository` | FalDocumentoPlantilla |
| `DocumentoRedaccionRepository` | `InMemoryDocumentoRedaccionRepository` | FalDocumentoRedaccion |
| `DocumentoRepository` | `InMemoryDocumentoRepository` | FalDocumento |
| `FalloActaRepository` | `InMemoryFalloActaRepository` | FalActaFallo |
| `FirmanteRepository` | `InMemoryFirmanteRepository` | FalFirmante/Version/Habilitacion |
| `FirmezaCondenaRepository` | `InMemoryFirmezaCondenaRepository` | FalActaFirmezaCondena |
| `GestionExternaRepository` | `InMemoryGestionExternaRepository` | FalGestionExterna |
| `InspectorRepository` | `InMemoryInspectorRepository` | FalInspector/Version |
| `NormativaRepository` | `InMemoryNormativaRepository` | FalNormativaFaltas/Articulos/Dependencia |
| `NotificacionRepository` | `InMemoryNotificacionRepository` | FalNotificacion |
| `PagoCondenaRepository` | `InMemoryPagoCondenaRepository` | FalPagoCondena |
| `PagoVoluntarioRepository` | `InMemoryPagoVoluntarioRepository` | FalPagoVoluntario |
| `TalonarioRepository` | `InMemoryTalonarioRepository` | NumPolitica/Talonario/Ambito/Inspector/Movimiento |

**Repositorios pendientes de crear (22 entidades FALTA_EN_INMEMORY):** PersonaRepository, PersonaDomicilioRepository, ActaParalizacionRepository, ActaArchivoRepository, ObservacionRepository, NotificacionIntentoRepository, NotificacionAcuseRepository, LoteCorreoRepository, TarifarioUnidadRepository, MedidaPreventiva, ArticuloMedida, ActaArticuloInfringidoRepository, ActaValorizacionRepository, ActaValorizacionItemRepository, ActaDocumentoRepository, ActaApelacionDocumentoRepository, ActaObligacionPagoRepository (modelo real), ActaFormaPagoRepository, ActaPlanPagoRepository, ActaPagoMovimientoRepository, satélites (6), QrAccesoRepository.

### C. Enums de dominio — estado de código numérico

| Enum | Código numérico | Columna MariaDB | Estado |
|---|---|---|---|
| `TipoActa` (TRANSITO, CONTRAVENCION, SUSTANCIAS_ALIMENTICIAS, COMERCIO) | **NO** | tipo_acta SMALLINT | ENUM_SIN_CODIGO — agregar 1-4 en 8F-11B |
| `EstadoRedaccionDocumento` (BORRADOR=1, CONFIRMADA=2, ANULADA=3) | **SÍ** | estado_redaccion SMALLINT | ALINEADO |
| `OrigenFirmezaCondena` (VENCIMIENTO_PLAZO_APELACION, APELACION_RECHAZADA) | **NO** | origen_firmeza SMALLINT NULL | ENUM_SIN_CODIGO — agregar 1=VENCIMIENTO, 2=RECHAZADA en 8F-11F |
| `ResultadoFinalActa` (0=SIN_RESULTADO_FINAL, 1=PAGO_VOLUNTARIO_PAGADO, 2=ABSUELTO, 3=CONDENA_FIRME, 4=ANULADO) | **SMALLINT** (pendiente implementación) | resultado_final SMALLINT NOT NULL | TIPO_INCOMPATIBLE — P1 CERRADA; implementar en 8F-11B |
| Demás enums funcionales | verificar | columnas SMALLINT | verificar por enum en 8F-11B |

### D. Clases NO persistibles

| Categoría | Clases |
|---|---|
| SOLO_DEMO | PlantillasMockSeeder, DemoActaMaterializadorService, DemoHealthService, DocumentoGraphDemoService, DocumentoGeneracionMockService, DocumentoPdfMockRenderer |
| SOLO_DEMO_DEV | DevInMemoryResetService, DevResetController, DevResetResponse |
| SOLO_DEMO_TEST | DatasetFuncionalDominioCatalog, ActaMockFuncionalDefinicion, CasoUsoFuncionalRunner, DocumentoEsperadoPorActaMock, GraphDemoActaFactory |
| SOLO_DEMO (DTOs) | DemoActaDetalleResponse + sub-DTOs, DemoHealthResponse + sub-DTOs, DemoTimelineEventoDto, DemoDocumentoDetalleDto |
| SOLO_INFRAESTRUCTURA_DEMO | DemoCorsConfig |
| SOLO_INFRAESTRUCTURA_INMEMORY | ResettableInMemoryRepository, todos los InMemory*Repository |
| NO_PERSISTIBLE (servicios) | DocumentoCombinacionService, DocumentoVariableContextBuilder, DocumentoVariableRegistry, DocumentoPlantillaDefaultService, DocumentoRedaccionService |
| NO_PERSISTIBLE (DTOs operativos) | todos los *Request / *Response de /api/faltas |
| NO_PERSISTIBLE (enums puro Java) | DocumentoVariableNamespace, TipoDatoVariableDocumento |
| NO_PERSISTIBLE (record Java) | DocumentoVariableDefinicion |


---

## 4. Matriz maestra campo por campo — Fase 3

### Leyenda de estados

| Estado | Significado |
|---|---|
| `ALINEADO` | Campo existe en ambos lados con tipo y semántica equivalente |
| `FALTA_EN_INMEMORY` | Campo existe en tabla MariaDB pero no en entidad Java |
| `FALTA_EN_MARIADB` | Campo existe en Java pero no hay tabla/columna MariaDB |
| `TIPO_INCOMPATIBLE` | Existe en ambos con tipo diferente |
| `NULABILIDAD_INCOMPATIBLE` | Difiere la nulabilidad |
| `IDENTIDAD_INCOMPATIBLE` | ID en Java es String/UUID, debe ser Long/BIGINT |
| `RELACION_INCOMPLETA` | FK o tabla pivot faltante en InMemory |
| `ENUM_SIN_CODIGO` | Enum sin código numérico para SMALLINT |
| `SEMANTICA_INCOMPATIBLE` | Modelado diferente (decisión cerrada) |
| `SOLO_DEMO_TEST` | Solo para demo/test, no persistir |
| `SOLO_INFRAESTRUCTURA` | Infraestructura InMemory, no persistir |
| `NO_PERSISTIBLE` | Servicio, DTO, enum puro Java — sin tabla |

---

### Matriz: FalActa / fal_acta

| Campo MariaDB | Tipo MariaDB | Null | Campo Java | Tipo Java | Estado | Acción | Slice |
|---|---|---|---|---|---|---|---|---|
| id | BIGINT AUTO_INCREMENT | NO | id | Long | ALINEADO | — | — |
| uuid_tecnico | CHAR(36) | NO | uuidTecnico | String | ALINEADO | — | — |
| nro_acta | VARCHAR(20) | YES | nroActa | String | ALINEADO | — | — |
| tipo_acta | SMALLINT NOT NULL | NO | tipoActa | **TipoActa | ALINEADO (8F-11B) |
| id_talonario | BIGINT | YES | idTalonario | Long | ALINEADO | — | — |
| nro_talonario_usado | INT | YES | nroTalonarioUsado | Integer | ALINEADO | — | — |
| fecha_acta | DATE | NO | fechaActa | LocalDate | ALINEADO | — | — |
| fecha_labrado | DATETIME(6) | NO | fechaLabrado | LocalDateTime | ALINEADO | — | — |
| domicilio_hecho | VARCHAR(500) | YES | domicilioHecho | String | ALINEADO | — | — |
| lat_infr | DOUBLE | YES | latInfr | Double | ALINEADO | — | — |
| lon_infr | DOUBLE | YES | lonInfr | Double | ALINEADO | — | — |
| resultado_firma_infractor | SMALLINT | NO | resultadoFirmaInfractor | ResultadoFirmaInfractor | ALINEADO | — | — |
| bloque_actual | SMALLINT | NO | bloqueActual | BloqueActual | ALINEADO | — | — |
| estado_procesal | SMALLINT | NO | estadoProcesal | EstadoProcesalActa | ALINEADO | — | — |
| situacion_administrativa | SMALLINT | NO | situacionAdministrativa | SituacionAdministrativaActa | ALINEADO | — | — |
| resultado_final | SMALLINT NOT NULL | NO | resultadoFinal | ResultadoFinalActa | ALINEADO (8F-11B) |
| id_persona | BIGINT | YES | — | — | FALTA_EN_INMEMORY | Agregar cuando FalPersona exista | 8F-11C |
| id_inspector | BIGINT | NO | idInspector | **Long | ALINEADO (8F-11B) |
| id_dependencia | BIGINT | NO | idDependencia | **Long | ALINEADO (8F-11B) |
| infractor_nombre | — (va a fal_persona) | — | infractorNombre | String | SEMANTICA_INCOMPATIBLE | Mover a FalPersona.nombre_completo | 8F-11C |
| infractor_documento | — (va a fal_persona) | — | infractorDocumento | String | SEMANTICA_INCOMPATIBLE | Mover a FalPersona.nro_documento | 8F-11C |
| domicilio_infractor | — (va a fal_persona_domicilio) | — | domicilioInfractor | String | SEMANTICA_INCOMPATIBLE | Mover a FalPersonaDomicilio.texto_libre | 8F-11C |
| fh_alta | DATETIME(6) | NO | — | — | ALINEADO (8F-11B) |
| id_user_alta | CHAR(36) | NO | — | — | ALINEADO (8F-11B) |
| fh_mod | DATETIME(6) | YES | — | — | ALINEADO (8F-11B) |
| id_user_mod | CHAR(36) | YES | — | — | ALINEADO (8F-11B) |

### Matriz: FalActaFallo / fal_acta_fallo

| Campo MariaDB | Tipo MariaDB | Null | Campo Java | Tipo Java | Estado | Acción | Slice |
|---|---|---|---|---|---|---|---|---|
| id | BIGINT AUTO_INCREMENT | NO | id | **Long | ALINEADO (8F-11B) |
| id_acta | BIGINT | NO | actaId | Long | ALINEADO | — | — |
| tipo_fallo | SMALLINT | NO | tipoFallo | TipoFalloActa | ALINEADO | — | — |
| estado_fallo | SMALLINT | NO | estadoFallo | EstadoFalloActa | ALINEADO | — | — |
| monto_condena | DECIMAL(14,2) | YES | montoCondena | BigDecimal | ALINEADO (simplificado) | Vincular a valorizacion_id en 8F-11D | 8F-11F |
| resultado_fallo | SMALLINT | YES | — | — | FALTA_EN_INMEMORY | Agregar enum ResultadoFalloActa o SMALLINT | 8F-11F |
| fundamentos | TEXT | YES | fundamentos | String | ALINEADO (D5) | — | — |
| documento_id | BIGINT | YES | documentoId | Long | ALINEADO | — | — |
| valorizacion_id | BIGINT | YES | — | — | FALTA_EN_INMEMORY | Agregar Long valorizacionId | 8F-11F |
| fallo_reemplazado_id | BIGINT | YES | — | — | FALTA_EN_INMEMORY | Agregar Long falloReemplazadoId | 8F-11F |
| fecha_dictado | DATETIME(6) | NO | fechaDictado | LocalDateTime | ALINEADO | — | — |
| fecha_notificacion | DATETIME(6) | YES | fechaNotificacion | LocalDateTime | ALINEADO | — | — |
| fecha_resultado_final | DATETIME(6) | YES | fechaResultadoFinal | LocalDateTime | ALINEADO | — | — |
| fh_firma | DATETIME(6) | YES | — | — | FALTA_EN_INMEMORY | Agregar LocalDateTime fhFirma | 8F-11F |
| fh_vto_apelacion | DATETIME(6) | YES | — | — | FALTA_EN_INMEMORY | Agregar LocalDateTime fhVtoApelacion | 8F-11F |
| si_activo | BOOLEAN | NO | siActivo | boolean | ALINEADO | — | — |
| si_firme | BOOLEAN | NO | — | — | FALTA_EN_INMEMORY | Agregar boolean siFirme (D1) | 8F-11F |
| fh_firmeza | DATETIME(6) | YES | — | — | FALTA_EN_INMEMORY | Agregar LocalDateTime fhFirmeza (D1) | 8F-11F |
| origen_firmeza | SMALLINT | YES | — | — | FALTA_EN_INMEMORY | Agregar OrigenFirmezaCondena con código (D1) | 8F-11F |
| version_row | INT | NO | — | — | FALTA_EN_INMEMORY | Agregar int versionRow | 8F-11F |
| fh_alta | DATETIME(6) | NO | — | — | FALTA_EN_INMEMORY | Auditoría | 8F-11B |
| id_user_alta | CHAR(36) | NO | — | — | FALTA_EN_INMEMORY | Auditoría | 8F-11B |
| fh_mod | DATETIME(6) | YES | — | — | FALTA_EN_INMEMORY | Auditoría | 8F-11B |
| id_user_mod | CHAR(36) | YES | — | — | FALTA_EN_INMEMORY | Auditoría | 8F-11B |

### Matriz: FalActaApelacion / fal_acta_apelacion

| Campo MariaDB | Tipo MariaDB | Null | Campo Java | Tipo Java | Estado | Acción | Slice |
|---|---|---|---|---|---|---|---|---|
| id | BIGINT AUTO_INCREMENT | NO | id | **Long | ALINEADO (8F-11B) |
| id_acta | BIGINT | NO | actaId | Long | ALINEADO | — | — |
| fallo_id | BIGINT | NO | falloId | **String** | IDENTIDAD_INCOMPATIBLE | Cambiar a Long | 8F-11F |
| estado_apelacion | SMALLINT | NO | estadoApelacion | EstadoApelacionActa | ALINEADO | — | — |
| fecha_presentacion | DATETIME(6) | NO | fechaPresentacion | LocalDateTime | ALINEADO | — | — |
| canal_apelacion | SMALLINT | NO | — | — | FALTA_EN_INMEMORY | Agregar enum/SMALLINT canalApelacion | 8F-11F |
| tipo_presentacion | SMALLINT | NO | — | — | FALTA_EN_INMEMORY | Agregar enum/SMALLINT tipoPresentacion | 8F-11F |
| texto_apelacion | TEXT | YES | — | — | FALTA_EN_INMEMORY | Agregar String textoApelacion | 8F-11F |
| presentante | VARCHAR(300) | YES | presentante | String | ALINEADO | — | — |
| fundamentos | TEXT | YES | fundamentos | String | ALINEADO | — | — |
| observaciones | TEXT | YES | observaciones | String | ALINEADO | — | — |
| si_activa | BOOLEAN | NO | siActiva | boolean | ALINEADO | — | — |
| fecha_resolucion | DATETIME(6) | YES | fechaResolucion | LocalDateTime | ALINEADO | — | — |
| fundamentos_resolucion | TEXT | YES | fundamentosResolucion | String | ALINEADO | — | — |
| observaciones_resolucion | TEXT | YES | observacionesResolucion | String | ALINEADO | — | — |
| documento_resolucion_id | BIGINT | YES | — | — | FALTA_EN_INMEMORY | Agregar Long documentoResolucionId | 8F-11F |
| version_row | INT | NO | — | — | FALTA_EN_INMEMORY | Agregar int versionRow | 8F-11F |
| fh_alta, id_user_alta, fh_mod, id_user_mod | DATETIME(6)/CHAR(36) | varios | — | — | ALINEADO (8F-11B) |

### Matriz: FalNotificacion / fal_notificacion

| Campo MariaDB | Tipo MariaDB | Null | Campo Java | Tipo Java | Estado | Acción | Slice |
|---|---|---|---|---|---|---|---|---|
| id | BIGINT AUTO_INCREMENT | NO | id | **Long | ALINEADO (8F-11B) |
| id_acta | BIGINT | NO | idActa | Long | ALINEADO | — | — |
| id_documento | BIGINT | NO | idDocumento | Long | ALINEADO | — | — |
| tipo_docu_notificado | SMALLINT | NO | tipoDocumentoNotificado | TipoDocu | ALINEADO | — | — |
| canal | VARCHAR(50) | NO | canal | String | ALINEADO | — | — |
| fecha_envio | DATETIME(6) | NO | fechaEnvio | LocalDateTime | ALINEADO | — | — |
| estado | SMALLINT | NO | estado | EstadoNotificacion | ALINEADO | — | — |
| resultado | SMALLINT | YES | resultado | ResultadoNotificacion | ALINEADO | — | — |
| fecha_resultado | DATETIME(6) | YES | fechaResultado | LocalDateTime | ALINEADO | — | — |
| intentos | INT | NO | intentos | int | ALINEADO (simplificado) | Separar a fal_notificacion_intento en 8F-11I | 8F-11I |
| observaciones | TEXT | YES | observaciones | String | ALINEADO | — | — |

### Matrices alineadas (sin gaps de campo)

Las siguientes entidades están completamente alineadas; se omite el detalle de campo por campo:

| Entidad | Tabla | Verificado en |
|---|---|---|
| FalDocumentoPlantillaContenido | fal_documento_plantilla_contenido | 8F-10-R2 (ver sección 2) |
| FalDocumentoPlantillaDefault | fal_documento_plantilla_default | 8F-10-R1 (ver sección 2) |
| FalDocumentoRedaccion | fal_documento_redaccion | 8F-10-R2 (ver sección 2) |
| FalDependencia / FalDependenciaVersion | fal_dependencia* | 8C-6E |
| FalInspector (base) | fal_inspector | 8C-6E |
| FalFirmante / FalFirmanteVersion / FalFirmanteVersionHabilitacion | fal_firmante* | 8A-3D |
| FalDocumento | fal_documento | 8C-6E |
| FalDocumentoFirma / FalDocumentoFirmaReq | fal_documento_firma* | 8C-6B-1 |
| FalDocumentoPlantilla / FalDocumentoPlantillaFirmaReq | fal_documento_plantilla* | 8C-6E |
| FalNormativaFaltas / FalArticuloNormativaFaltas / FalDependenciaNormativa | fal_normativa* | 8A-1 |
| NumPolitica / NumTalonario / NumTalonarioAmbito / NumTalonarioInspector / NumTalonarioMovimiento | num_* | 8B-1 |
| FalActaSnapshot | fal_acta_snapshot | snapshot recalculador |
| FalActaEvidencia | fal_acta_evidencia | slice evidencias |


---

## 5. Gaps conocidos verificados — Fase 4

### GAP-1 CERRADO en 8F-10
Las tres tablas nuevas de 8F (fal_documento_plantilla_contenido, fal_documento_plantilla_default, fal_documento_redaccion) fueron incorporadas al modelo MariaDB en 8F-10. Las entidades Java fueron completamente alineadas en 8F-10-R2. **No hay gap.**

### GAP-2 APLICADO en 8F-10 (D1 cerrada)
origen_firmeza SMALLINT NULL incorporado en fal_acta_fallo del modelo MariaDB. **Pendiente en InMemory:** agregar si_firme, fh_firmeza, origen_firmeza (con código) a FalActaFallo Java — Slice 8F-11F.

### GAP-3 DECISION CERRADA D2
FalPagoVoluntario y FalPagoCondena → fal_acta_obligacion_pago con tipo. **Pendiente:** implementar modelo real de pagos en 8F-11H.

### GAP-4 ACTIVO — FalActaParalizacion faltante
No existe FalActaParalizacion ni repo. D3 cerrada: implementar InMemory antes de JDBC. **Slice: 8F-11G.**

### GAP-5 ACTIVO — FalActaArchivo faltante
No existe FalActaArchivo ni repo. **Slice: 8F-11G.**

### GAP-6 ACTIVO — FalPersona / FalPersonaDomicilio faltantes
Datos de persona embebidos en FalActa. D6 cerrada: implementar InMemory antes de JDBC. **Slice: 8F-11C.**

### GAP-7 ACTIVO — Valorización y artículos faltantes
No existen: FalTarifarioUnidadFaltas, FalActaArticuloInfringido, FalActaValorizacion, FalActaValorizacionItem. FalActaFallo.montoCondena es placeholder. **Slice: 8F-11D.**

### GAP-8 ELIMINADO (8F-11B)
Decision de diseno (8F-11B): `firma_storage_key`, `firma_hash`, `fh_firma_registrada` eliminados definitivamente del modelo MariaDB y del modelo Java. La firma maestra del inspector no se registra.

### GAP-9 ACTIVO — FalActaFallo campos faltantes
Faltan: valorizacion_id, resultado_fallo, fh_firma, fh_vto_apelacion, fallo_reemplazado_id, si_firme, fh_firmeza, origen_firmeza, version_row, auditoría. Ver matriz sección 4. **Slice: 8F-11F.**

### GAP-10 ACTIVO — FalActaApelacion campos faltantes
Faltan: canal_apelacion, tipo_presentacion, texto_apelacion, documento_resolucion_id, version_row, auditoría; falloId debe ser Long. Ver matriz sección 4. **Slice: 8F-11F.**

### GAP-11 DIFERIBLE — FalObservacion faltante
PAGAPR usa evento.descripcion transitoriamente. **Slice: 8F-11G.**

### GAP-12 DIFERIBLE — fal_acta_documento pivot faltante
FalDocumento.idActa cubre caso simple. **Slice: 8F-11J.**

### GAP-13 ACTIVO — Enums sin código numérico
- TipoActa: necesita TRANSITO=1, CONTRAVENCION=2, SUSTANCIAS_ALIMENTICIAS=3, COMERCIO=4 (D8 cerrada). **Slice: 8F-11B.**
- OrigenFirmezaCondena: necesita 1=VENCIMIENTO_PLAZO_APELACION, 2=APELACION_RECHAZADA (D1 cerrada). **Slice: 8F-11F.**

### GAP-14 ACTIVO — Identidades String en 5 entidades
FalActaFallo.id, FalActaApelacion.id, FalNotificacion.id, FalBloqueanteMaterial.id, FalGestionExterna.id — todos deben migrar a Long. **Slice: 8F-11B.**

### GAP-15 ACTIVO — Auditoría ausente en entidades clave
FalActa, FalActaFallo, FalActaApelacion, FalGestionExterna carecen de fh_alta, id_user_alta, fh_mod, id_user_mod. **Slice: 8F-11B.**

### GAP-16 ACTIVO — Satélites de acta faltantes
No existen: FalActaTransito, FalActaAlcoholemia, FalActaVehiculo, FalActaContravención, FalActaSustanciasAlimenticias, FalActaMedidaPreventiva. **Slice: 8F-11E.**

### GAP-17 DIFERIBLE — Notificaciones ciclo completo faltante
FalNotificacionIntento, FalNotificacionAcuse, FalLoteCorreo no implementados. **Slice: 8F-11I.**

### GAP-18 CERRADO - QR portal
fal_acta_qr_acceso ALINEADO. FalActaQrAcceso + InMemoryQrAccesoRepository + QrActaService implementados en **Slice: 8F-11K.**

---

## 6. Decisiones cerradas P1 y P2 - Fase 5

Ambas decisiones están cerradas. No reabrir.

---

### Decisión P1 — ResultadoFinalActa: CERRADA

**Decisión aprobada:** columna MariaDB `resultado_final SMALLINT NOT NULL`; enum Java con código explícito.

**No usar:** ordinal Java, `name()`, VARCHAR, CHAR(6).

**Valores definitivos:**

| Código | Valor |
|---:|---|
| 0 | `SIN_RESULTADO_FINAL` |
| 1 | `PAGO_VOLUNTARIO_PAGADO` |
| 2 | `ABSUELTO` |
| 3 | `CONDENA_FIRME` |
| 4 | `ANULADO` |

**Semántica:**

- `ResultadoFinalActa` expresa la forma definitiva en que fue resuelta el acta, no el último estado transitorio.
- Una solicitud de pago voluntario pendiente conserva `SIN_RESULTADO_FINAL`.
- Solo el pago voluntario efectivamente acreditado produce `PAGO_VOLUNTARIO_PAGADO`.
- El pago posterior de una condena no cambia `CONDENA_FIRME`.
- El estado del pago vive en la obligación y sus movimientos.
- Gestión externa no es un resultado final.
- `PRESCRIPTO` no existe en el dominio aprobado.
- Archivo no equivale automáticamente a resultado final.

**Valores eliminados del enum (no deben existir en la implementación Java final):**
- `PAGO_VOLUNTARIO_CONFIRMADO` — eliminado; reemplazado por `PAGO_VOLUNTARIO_PAGADO`
- `CONDENA_FIRME_PAGADA` — eliminado; el pago no cambia `ResultadoFinalActa`
- `FALLO_CONDENATORIO_PAGADO` — eliminado
- `FALLO_CONDENATORIO_GESTION_EXTERNA` — eliminado; gestión externa no es resultado final
- `PRESCRIPTO` — eliminado; no existe en el dominio aprobado
**Implementación Java:** Slice 8F-11B.

**Impacto en sección 11.2:** La interpretación anterior de CHAR(6) queda reemplazada por esta decisión. Ver corrección en 11.2.

---

### Decisión P2 — FalActaFirmezaCondena: CERRADA (Opción B)

**Decisión aprobada:** refactor en 8F-11F.

**Fuente de verdad jurídica:** la firmeza pertenece al fallo concreto y se persiste en `fal_acta_fallo`:
- `si_firme`
- `fh_firmeza`
- `origen_firmeza`

**Estado operativo del acta:** la situación de firmeza vigente debe proyectarse en `FalActaSnapshot`.

**En 8F-11F:**
- Completar `FalActaFallo` con siFirme, fhFirmeza, origenFirmeza.
- Refactorizar servicios para escribir la firmeza sobre el fallo.
- Eliminar `FalActaFirmezaCondena` como entidad persistible separada.
- Eliminar `FirmezaCondenaRepository`.
- Mantener proyección en snapshot.
- No duplicar segunda fuente de verdad en `FalActa`.

---

> No hay otras decisiones abiertas. D1 a D9 están cerradas y no deben reabrirse. P1 y P2 cerradas en 8F-11A-R1.
## 7. Plan finito de implementación — Fase 6

### 8F-11B — Identidades, enums, auditoría, versionRow y campos core
**Objetivo:** Eliminar todos los ids String, agregar códigos a enums core, agregar auditoría, corregir tipos de FK.

**Entidades afectadas:**
- FalActaFallo.id: String → Long
- FalActaApelacion.id: String → Long
- FalNotificacion.id: String → Long (D7)
- FalBloqueanteMaterial.id: String → Long
- FalGestionExterna.id: String → Long
- FalActa.tipoActa: String → enum TipoActa con código SMALLINT (D8) — valores 1–4
- `ResultadoFinalActa`: SMALLINT 0–4 definitivo (P1 CERRADA)
- FalActa.idInspector, idDependencia: String → Long
- Campos de auditoría en FalActa, FalActaFallo, FalActaApelacion, FalGestionExterna
- `FalInspectorVersion`: campos de firma eliminados del modelo (GAP-8 ELIMINADO)
- `versionRow INT` en FalActa, FalDocumento, FalNotificacion, FalGestionExterna y otras entidades existentes que lo requieran

**Repositorios:** todos los mencionados arriba — actualizar AtomicLong counters
**Tests:** todos los que usen ids de fallo/apelación/notificación/bloqueante/gestión como String
**Documentación:** actualizar matriz 110 con ALINEADO para los campos corregidos
**Criterio de cierre:** Build >= 1509 tests, 0 failures; 0 entidades persistibles con id String (excepto uuid_tecnico en FalActa)

---

### 8F-11C — FalPersona y FalPersonaDomicilio
**Objetivo:** Implementar entidades de persona separadas de FalActa.

**Entidades a crear:** FalPersona (con tipo_persona, Id_Suj, Id_Bie), FalPersonaDomicilio (con modo_domicilio, refs geo)
**Repositorios a crear:** PersonaRepository + InMemory, PersonaDomicilioRepository + InMemory
**Refactor en FalActa:** agregar idPersona Long nullable; infractorNombre/infractorDocumento pueden mantenerse como campos de captura transitoria
**Dependencias:** 8F-11B (FalActa.idPersona como Long)
**Criterio de cierre:** FalPersona y FalPersonaDomicilio existen con repos; build verde

---

### 8F-11D — Normativa, tarifario, medidas y valorización
**Objetivo:** Implementar el modelo de cálculo de montos.

**Entidades a crear:** FalTarifarioUnidadFaltas, FalMedidaPreventiva, FalArticuloMedidaPreventiva, FalActaArticuloInfringido, FalActaValorizacion, FalActaValorizacionItem
**Repositorios:** 6 repos + InMemory
**Impacto:** FalActaFallo.montoCondena vinculado a FalActaValorizacion.id
**Criterio de cierre:** Valorización calculada desde artículos; build verde

---

### 8F-11E — Satélites de acta y catálogos relacionados
**Objetivo:** Implementar datos específicos de cada tipo de acta y catálogos de soporte.

**Entidades a crear — satélites:**
- FalActaTransito, FalActaTransitoAlcoholemia, FalActaVehiculo
- FalActaContravencion, FalActaSustanciasAlimenticias, FalActaMedidaPreventiva

**Entidades a crear — catálogos:**
- FalVehiculoMarca (id, descripcion, si_activo; CRUD; baja lógica)
- FalVehiculoModelo (id, marca_vehiculo_id, descripcion, si_activo; modelo pertenece a marca; sin descripción duplicada por marca)
- FalRubroVersion (conservar versionado: row_hash, previous_row_hash, source_operation, close_operation, si_version_actual, valid_from, valid_to, synced_at; consultar por Id_Rub; recuperar versión vigente)

**Repositorios:** 6 + 3 catálogos + InMemory
**Guardrail:** tipo_acta determina qué satélite existe
**Dependencias:** 8F-11B (tipoActa correcto)
**Criterio de cierre:** Cada tipo tiene satélite; guardrail activo; catálogos con repos InMemory; build verde

---

### 8F-11F — Fallo, firmeza y apelación
**Objetivo:** Completar FalActaFallo y FalActaApelacion; refactorizar firmeza (P2 CERRADA Opción B).

**Cambios en FalActaFallo:** id ya Long (8F-11B); agregar valorizacionId, resultadoFallo, fhFirma, fhVtoApelacion, falloReemplazadoId, siFirme, fhFirmeza, origenFirmeza, versionRow, auditoría
**Cambios en FalActaApelacion:** id ya Long (8F-11B); falloId → Long; agregar canalApelacion, tipoPresentacion, textoApelacion, documentoResolucionId, versionRow, auditoría
**Nuevas entidades:** FalActaApelacionDocumento
**Firmeza (P2 CERRADA):** eliminar FalActaFirmezaCondena y FirmezaCondenaRepository; agregar siFirme, fhFirmeza, origenFirmeza (con código) a FalActaFallo; proyección en snapshot
**Dependencias:** 8F-11B (ids), 8F-11D (valorizacion_id)
**Criterio de cierre:** FalActaFallo con todos los campos del modelo; apelación completa; firmeza resuelta; build verde

---

### 8F-11G — Paralización, archivo, motivos, observaciones y gestión externa
**Objetivo:** Implementar ciclos de paralización y archivo; crear observaciones; completar gestión externa.

**Entidades a crear:** FalActaParalizacion, FalActaArchivo, FalMotivoArchivo (id, descripcion, si_activo; FK desde fal_acta_archivo; motivos inactivos válidos históricamente), FalObservacion
**Repos:** 4 repos + InMemory
**Refactor:** PAGAPR escribe en FalObservacion en lugar de evento.descripcion
**Dependencias:** 8F-11B (ids Long en gestión externa ya corregidos)
**Criterio de cierre:** Ciclo paralización/cierre funciona; FalActaArchivo registra archivo; FalObservacion captura PAGAPR; FalMotivoArchivo con CRUD; build verde

---

### 8F-11H — Pagos reales unificados (D2 CERRADA)
**Objetivo:** Implementar modelo real de pagos unificado.

**Entidades a crear:** FalActaObligacionPago, FalActaFormaPago, FalActaPlanPagoRef, FalActaPagoMovimiento
**Enum aprobado:** OrigenObligacionPago (PAGO_VOLUNTARIO=1, CONDENA=2)
**Repos:** 4 repos + InMemory; PagoVoluntarioRepository y PagoCondenaRepository eliminados o wrappers transitorios
**Migración:** FalPagoVoluntario y FalPagoCondena ya no son entidades persistibles separadas
**Dependencias:** 8F-11D (valorización)
**Criterio de cierre:** FalActaObligacionPago unifica ambos tipos de pago; ciclos de pago funcionan; build verde

---

### 8F-11I — Notificaciones completas
**Objetivo:** Ciclo completo de notificación con intentos y acuses.

**Entidades a crear:** FalNotificacionIntento, FalNotificacionAcuse, FalLoteCorreo
**Repos:** 3 repos + InMemory
**Dependencias:** 8F-11B (FalNotificacion.id Long)
**Criterio de cierre:** Ciclo con intentos y acuse funciona; build verde

---

### 8F-11J — Documentos y relaciones restantes
**Objetivo:** Completar relaciones documentales.

**Pendientes:** FalActaDocumento (pivot); FalDocumento campos faltantes (plantilla_id); no implementar PDF/storage real
**Dependencias:** 8F-11B
**Criterio de cierre:** FalDocumento completo; fal_acta_documento resuelta; build verde

---

### 8F-11K — Portal QR y auditoría final de paridad cero
**Objetivo:** Implementar QrAcceso; verificar 62 tablas a 0 gaps.

**Entidades a crear:** FalActaQrAcceso
**Actividades:**
- Recorrer la matriz 110 y confirmar: todos los estados son ALINEADO, SOLO_DEMO_TEST, SOLO_INFRAESTRUCTURA o NO_PERSISTIBLE
- Revisión final de las 62 tablas — 0 gaps no justificados
- Actualizar 110 con estados finales
- Verificar guardrails: Test-Path docs\spec-as-source desde raíz = FALSE; sin @Entity; sin JpaRepository; sin SQL
- Build final completo

**Criterio de cierre (paridad total):**
- 0 FALTA_EN_INMEMORY sin clasificación
- 0 IDENTIDAD_INCOMPATIBLE
- 0 TIPO_INCOMPATIBLE sin justificación
- Matriz 110 al 100% ALINEADO o clasificación justificada
- Build >= 1509 tests, 0 failures

---
## 8. Tabla de decisiones cerradas D1–D9 — referencia rápida

No volver a mostrar estas decisiones como pendientes.

| # | Decisión | Descripción | Estado |
|---|---|---|---|
| D1 | Firmeza | FalActaFirmezaCondena sin tabla; campos en fal_acta_fallo (fh_firmeza, si_firme, origen_firmeza SMALLINT NULL) | CERRADA |
| D2 | Pagos | FalPagoVoluntario + FalPagoCondena → fal_acta_obligacion_pago con tipo_obligacion | CERRADA |
| D3 | Paralización | FalActaParalizacion: implementar InMemory antes de JDBC | CERRADA |
| D4 | JSON | Campos JSON documentales → JSON nativo MariaDB 12.3.2 | CERRADA |
| D5 | Fundamentos fallo | FalActaFallo.fundamentos → TEXT NULL directamente en fal_acta_fallo | CERRADA |
| D6 | Persona | FalPersona + FalPersonaDomicilio: implementar InMemory antes de JDBC | CERRADA |
| D7 | Notificación PK | FalNotificacion.id → BIGINT AUTO_INCREMENT sin UUID | CERRADA |
| D8 | TipoActa | FalActa.tipoActa → enum TipoActa con código SMALLINT | CERRADA |
| D9 | Prioridad default | fal_documento_plantilla_default.prioridad → SMALLINT | CERRADA |
| GAP-1 | 3 tablas 8F | fal_documento_plantilla_contenido/default y fal_documento_redaccion en modelo MariaDB + Java alineado 8F-10-R2 | CERRADA |

---

## 9. Checklist de validación — 8F-11A (original) + 8F-11A-R1 (corrección)

| Ítem | Estado |
|------|--------|
| Build ejecutado: 1509 tests, 0 failures, BUILD SUCCESS | VERIFICADO |
| Test-Path docs\spec-as-source desde raíz = FALSE (guardrail) | VERIFICADO |
| Sin @Entity nuevo | VERIFICADO |
| Sin JpaRepository | VERIFICADO |
| Sin EntityManager | VERIFICADO |
| Sin SQL/migraciones nuevas | VERIFICADO |
| Sin frontend Angular nuevo | VERIFICADO |
| Sin endpoints productivos nuevos en este slice | VERIFICADO |
| Documento 110 creado como fuente vigente de paridad | COMPLETADO |
| 109 referencia al 110 como fuente vigente | VERIFICADO (8F-11A) |
| 99-pendientes actualizado con roadmap 8F-11B a 8F-11K | COMPLETADO (8F-11A-R1) |
| **62 tablas MariaDB auditadas** (corregido desde 55) | COMPLETADO (8F-11A-R1) |
| 31 entidades InMemory persistibles inventariadas | COMPLETADO |
| 0 tablas sin estado asignado | COMPLETADO |
| 0 entidades persistibles sin tabla candidata | COMPLETADO |
| D1-D9 marcadas CERRADA en sección 8 | COMPLETADO |
| 0 TODOs vagos sin slice asignado | COMPLETADO |
| **P1 CERRADA: ResultadoFinalActa SMALLINT 0–4** | COMPLETADO (8F-11A-R1) |
| **P2 CERRADA: firmeza en fallo, refactor en 8F-11F** | COMPLETADO (8F-11A-R1) |
| **29 tablas FALTA_EN_INMEMORY enumeradas explícitamente** | COMPLETADO (8F-11A-R1) |
| Roadmap finito 8F-11B a 8F-11K con 9 slices | COMPLETADO |
| Sección 11.2 corregida (sin CHAR(6) incorrecto) | COMPLETADO (8F-11A-R1) |

---
## 10. Entrega final del slice — 8F-11A (original) + 8F-11A-R1 (corrección documental)

| Indicador | Valor |
|---|---|
| **Build final** | BUILD SUCCESS |
| **Total tests** | 1509 |
| **Failures / Errors** | 0 / 0 |
| **Archivos creados/modificados** | 110-matriz-maestra-paridad-mariadb-inmemory.md (creado en 8F-11A; corregido en 8F-11A-R1) |
| **Total tablas MariaDB** | **62** |
| **Entidades InMemory persistibles inventariadas** | 31 |
| **Estado ALINEADO (entidades)** | 20 entidades completamente alineadas |
| **FALTA_EN_INMEMORY (tablas completas)** | **29 tablas sin entidad Java** |
| **IDENTIDAD_INCOMPATIBLE** | 0 (5 resueltas en 8F-11B) |
| **TIPO_INCOMPATIBLE (campos)** | 0 (3 resueltos en 8F-11B) |
| **SEMANTICA_INCOMPATIBLE** | 2 (FalPagoVoluntario→D2, FalPagoCondena→D2) + FalActaFirmezaCondena→P2 CERRADA |
| **ENUM_SIN_CODIGO** | 0 (2 resueltos en 8F-11B) |
| **RELACION_INCOMPLETA** | 3 (fal_acta_documento, fal_acta_apelacion_documento, inspector firma FK) |
| **SOLO_DEMO_TEST** | 21 clases |
| **SOLO_INFRAESTRUCTURA** | 2 categorías |
| **NO_PERSISTIBLE** | 25+ (servicios, DTOs, enums puro Java) |
| **P1 CERRADA: ResultadoFinalActa** | SMALLINT NOT NULL; 5 valores (0=SIN_RESULTADO_FINAL … 4=ANULADO) |
| **P2 CERRADA: Firmeza** | siFirme/fhFirmeza/origenFirmeza en fal_acta_fallo; refactor en 8F-11F |
| **Modelo unificado de pagos** | FalActaObligacionPago + OrigenObligacionPago; implementar en 8F-11H |
| **4 catálogos confirmados** | VehiculoMarca, VehiculoModelo, RubroVersion (versionado), MotivoArchivo |
| **Roadmap de cierre** | 8F-11B a 8F-11K — 9 slices finitos |
| **Proximo slice recomendado** | **8F-11C** - FalPersona y FalPersonaDomicilio |

---

> **Criterio de cierre verificado (8F-11A-R1):** existen las 62 tablas inventariadas; las 29 tablas FALTA_EN_INMEMORY están enumeradas explícitamente; P1 y P2 están cerradas; el roadmap cubre el 100% de las tablas persistibles (8F-11B a 8F-11K); el modelo MariaDB refleja `resultado_final SMALLINT NOT NULL`; build y guardrails verdes.

---
## 11. Correcciones post-auditoría — 8F-11A y 8F-11A-R1

### 11.1 Recuento de tablas corregido

El modelo MariaDB final tiene **62 tablas** en el dominio Faltas, no 55.

Las 7 tablas adicionales identificadas en la auditoría:

| Tabla adicional | Área | Estado InMemory |
|---|---|---|
| `fal_vehiculo_marca` | Catálogo de vehículos — tránsito | **ALINEADO** (8F-11E) |
| `fal_vehiculo_modelo` | Catálogo de vehículos — tránsito | **ALINEADO** (8F-11E) |
| `fal_rubro_version` | Catálogo rubros versionado — contravención | **ALINEADO** (8F-11E) |
| `fal_motivo_archivo` | Catálogo motivo de archivo — administrable | FALTA_EN_INMEMORY — Slice 8F-11G |
| `stor_backend` | Storage — infraestructura técnica | NO_PERSISTIBLE desde dominio Faltas |
| `stor_politica` | Storage — infraestructura técnica | NO_PERSISTIBLE desde dominio Faltas |
| `stor_objeto` | Storage — referencia binarios | NO_PERSISTIBLE (storage_key en entidades de dominio) |

**Nota:** `fal_rubro_version` es una tabla de dominio Faltas con sincronización externa (Informix). No es infraestructura técnica. Requiere entidad `FalRubroVersion` con InMemory, respetando el modelo de versionado (`row_hash`, `previous_row_hash`, `source_operation`, `close_operation`, `si_version_actual`, `valid_from`, `valid_to`, `synced_at`).

Tablas geográficas `geo_*` (geo_ign_*, geo_indec_*, geo_bahra_*, geo_malv_*): externas a Faltas, no administradas por este módulo. Clasificadas NO_PERSISTIBLE a efectos de paridad Faltas.

---

### 11.2 Decisión P1 — CERRADA (corrección 8F-11A-R1)

**⚠ Corrección:** La sección 11.2 original decía que `resultado_final` era `CHAR(6)` (incorrecto). Esa interpretación era incorrecta. La decisión aprobada de dominio reemplaza esa definición.

**Decisión definitiva aprobada:**

`resultado_final SMALLINT NOT NULL` en `fal_acta`.

**Por qué no CHAR(6):** Aunque otros campos de estado del acta usan CHAR(4/6) como código corto de texto (`bloque_actual CHAR(4)`, `est_proc_act CHAR(4)`, `sit_adm_act CHAR(4)`), `resultado_final` es diferente en naturaleza: es el resultado final de resolución del expediente, con un dominio cerrado de 5 valores enteros, y se persiste con un código numérico explícito para garantizar estabilidad, eficiencia de índice y compatibilidad con la capa JDBC.

**Valores definitivos:**

| Código SMALLINT | Constante Java |
|---:|---|
| 0 | `SIN_RESULTADO_FINAL` |
| 1 | `PAGO_VOLUNTARIO_PAGADO` |
| 2 | `ABSUELTO` |
| 3 | `CONDENA_FIRME` |
| 4 | `ANULADO` |

**Reglas:**
- Una solicitud de pago voluntario pendiente conserva `SIN_RESULTADO_FINAL`.
- Solo el pago voluntario efectivamente acreditado produce `PAGO_VOLUNTARIO_PAGADO`.
- El pago posterior de una condena no cambia `CONDENA_FIRME`.
- Gestión externa no es un resultado final.
- `PRESCRIPTO` no existe.
- Archivo no equivale automáticamente a resultado final.

**Enum eliminado:** `PAGO_VOLUNTARIO_CONFIRMADO`, `CONDENA_FIRME_PAGADA`, `FALLO_CONDENATORIO_PAGADO`, `FALLO_CONDENATORIO_GESTION_EXTERNA`, `PRESCRIPTO` no deben existir en el enum final.

**Implementación Java:** Slice 8F-11B.

---

### 11.3 Correcciones de tipo en FalActa

| Campo Java | Tipo Java actual | Tipo MariaDB real | Corrección |
|---|---|---|---|
| `tipoActa` | String | `tipo_acta SMALLINT` | TIPO_INCOMPATIBLE — agregar TipoActa enum con short 1–4 |
| `bloqueActual` | BloqueActual (enum) | `bloque_actual CHAR(4)` | ALINEADO si el enum usa String CAPT/ENRI/etc. |
| `estadoProcesal` | EstadoProcesalActa | `est_proc_act CHAR(4)` | verificar códigos en 8F-11B |
| `situacionAdministrativa` | SituacionAdministrativaActa | `sit_adm_act CHAR(4)` | verificar códigos en 8F-11B |
| `resultadoFinal` | ResultadoFinalActa | `resultado_final SMALLINT NOT NULL` | TIPO_INCOMPATIBLE — P1 CERRADA; implementar short 0–4 en 8F-11B |
| `resultadoFirmaInfractor` | ResultadoFirmaInfractor | `resultado_firma_infractor SMALLINT` | ALINEADO (short 1–5 ya en enum) |

---

### 11.4 version_row en FalActa y aggregates

El modelo exige `version_row INT NOT NULL DEFAULT 0` en al menos:
`fal_acta`, `fal_documento`, `fal_notificacion`, `fal_acta_valorizacion`, `fal_acta_fallo`,
`fal_acta_apelacion`, `fal_acta_paralizacion`, `fal_acta_archivo`, `fal_acta_gestion_externa`,
`fal_acta_obligacion_pago`, `fal_acta_forma_pago`, `fal_acta_plan_pago_ref`, `num_talonario`.

**InMemory actual:** NumTalonario, FalActa, FalDocumento, FalNotificacion, FalGestionExterna, FalActaFallo, FalActaApelacion tienen `versionRow` implementado (8F-11B).

**Entidades existentes con versionRow (8F-11B CERRADO):** FalActa, FalDocumento, FalNotificacion, FalGestionExterna, FalActaFallo, FalActaApelacion.

**Entidades futuras que nacerán con versionRow** (en cada slice de creación): FalActaValorizacion (8F-11D), FalActaParalizacion (8F-11G), FalActaArchivo (8F-11G), FalActaObligacionPago (8F-11H), FalActaFormaPago (8F-11H), FalActaPlanPagoRef (8F-11H).

---

### 11.5 FalPersona más compleja de lo documentado

`fal_persona` incluye:
- `tipo_persona` SMALLINT (FISICA=1, JURIDICA=2)
- `Id_Suj`, `Id_Bie` BIGINT — integración Ingresos
- `SujBieEstado` SMALLINT (sin cuenta / activa / inactiva)
- `fh_ult_mod`, `id_user_ult_mod` (auditoría completa)

`fal_persona_domicilio` es más compleja:
- `modo_domicilio` (MALVINAS_LOCAL / EXTERNO)
- refs geo versionadas Malvinas
- `origen_domicilio`, múltiples FKs geo

Impacto en 8F-11C: `FalPersona` necesita campos de Ingresos y tipo_persona; `FalPersonaDomicilio` necesita modo_domicilio y refs geo simplificadas para InMemory.

---

### 11.6 Enums que ya tienen código short (confirmado en auditoría 8F-11A)

Con `short codigo()` implementado:
`AccionDocumental` (1–11), `AlcanceTalonario` (1–3), `ClaseNumeracion` (1–2), `EstadoAsignacionTalonario` (1–4), `EstadoDocu` (1–7), `EstadoFirma` (1–6), `EstadoFirmaReq` (1–5), `EstadoNumeroTalonario` (1–5), `EstadoRedaccionDocumento` (1–3), `MomentoNumeracionDocu` (0–4), `MotivoAnulacionTalonario` (1–5), `ResultadoFirmaInfractor` (1–5), `TipoDocu` (1–12), `TipoEvidenciaActa` (6), `TipoFirma` (1–4), `TipoFirmaReq` (0–5), `TipoTalonario` (1–2).

Con valores String directos (CHAR(4/6) en MariaDB — patrón correcto):
`BloqueActual`, `TipoEventoActa`, `EstadoProcesalActa`, `SituacionAdministrativaActa`.

Sin código confirmado (verificar en 8F-11B):
`EstadoApelacionActa`, `EstadoBloqueanteMaterial`, `EstadoFalloActa`, `EstadoGestionExterna`, `EstadoNotificacion`, `EstadoPagoCondena`, `EstadoPagoVoluntario`, `ModoReingresoGestionExterna`, `OrigenBloqueanteMaterial`, `ResultadoGestionExterna`, `ResultadoNotificacion`, `TipoActa` (pendiente SMALLINT 1–4), `TipoGestionExterna`, `TipoUnidad`.

---

### 11.7 Las 12 tablas FALTA_EN_INMEMORY - listado canonico post-8F-11E

Estas son las tablas MariaDB reales que no tienen todavia equivalente InMemory persistible.
Ninguna debe clasificarse como NO_PERSISTIBLE o diferible sin implementacion antes de JDBC.

**Observaciones (1):**
1. `fal_observacion`

**Satelites del acta (6):**
2. `fal_acta_transito`
3. `fal_acta_transito_alcoholemia`
4. `fal_acta_vehiculo`
5. `fal_acta_contravencion`
6. `fal_acta_sustancias_alimenticias`
7. `fal_acta_medida_preventiva`

**Notificaciones (3):**
8. `fal_notificacion_intento`
9. `fal_notificacion_acuse`
10. `fal_lote_correo`

**Apelacion, paralizacion y archivo (3):**
11. `fal_acta_apelacion_documento`
12. `fal_acta_paralizacion`
13. `fal_acta_archivo`

**Pagos (3):**
14. `fal_acta_forma_pago`
15. `fal_acta_plan_pago_ref`
16. `fal_acta_pago_movimiento`

**Portal (1):**
17. `fal_acta_qr_acceso`

**Catalogos del dominio (4):**
18. `fal_vehiculo_marca`
19. `fal_vehiculo_modelo`
20. `fal_rubro_version`
21. `fal_motivo_archivo`

**Tablas NO incluidas en este conteo (clasificacion correcta):**
- `fal_acta_fallo` -> PARCIAL (campos faltantes: 8F-11F)
- `fal_acta_apelacion` -> PARCIAL (campos faltantes: 8F-11F)
- `fal_acta_obligacion_pago` -> SEMANTICA_INCOMPATIBLE (D2 cerrada; unificar en 8F-11H)
- `fal_acta_documento` -> RELACION_INCOMPLETA (GAP-12; 8F-11J)

**Tablas resueltas en slices anteriores (ya ALINEADO):**
- `fal_persona` -> ALINEADO (8F-11C, 2026-07-05)
- `fal_persona_domicilio` -> ALINEADO (8F-11C, 2026-07-05)
- `fal_tarifario_unidad_faltas` -> ALINEADO (8F-11D+R1, 2026-07-06)
- `fal_medida_preventiva` -> ALINEADO (8F-11D+R1, 2026-07-06)
- `fal_articulo_medida_preventiva` -> ALINEADO (8F-11D+R1, 2026-07-06)
- `fal_acta_articulo_infringido` -> ALINEADO (8F-11D+R1, 2026-07-06)
- `fal_acta_valorizacion` -> ALINEADO (8F-11D+R1, 2026-07-06)
- `fal_acta_valorizacion_item` -> ALINEADO (8F-11D+R1, 2026-07-06)

---

### 11.8 Correccion de delta de tests - 8F-11D-R1

**Baseline 8F-11D:** 1755 tests
**Build 8F-11D-R1:** 1785 tests
**Delta real:** +30 tests (no 27 como informaba el reporte preliminar)

Todos los tests nuevos estan en ValorizacionInvariantesR1Test:

| Clase interna | Tests antes | Tests despues | Delta | Descripcion |
|---|---:|---:|---:|---|
| SnapshotProyeccionR1ATest | 0 | 4 | +4 | Proyeccion de valorizacion en snapshot |
| ConfirmarVigenteAtomicoR1BTest | 0 | 4 | +4 | Atomicidad CAS + CyclicBarrier concurrencia real |
| ItemInmutabilidadR1CTest | 0 | 4 | +4 | Items inmutables post-confirmacion |
| TarifarioSuperposicionR1DTest | 0 | **6** | **+6** | Solapamiento de vigencia (5 reportados + find_ultimo_vigente_detecta_invariante_rota) |
| MedidaPreventivaVersionAtomicaR1DTest | 0 | 4 | +4 | UK (codigo, versionMedida); version atomica |
| ActaArticuloInfringidoUKR1DTest | 0 | **5** | **+5** | UK activo (actaId, articuloId) (3 reportados + diferente_acta/articulo_no_conflicta) |
| ArticuloMedidaReactivacionR1DTest | 0 | 3 | +3 | Reactivacion silenciosa rechazada |
| **TOTAL** | **0** | **30** | **+30** | |

**Nota:** El reporte preliminar de 8F-11D-R1 indicaba 27 tests nuevos (5 en Tarifario, 3 en ArticuloInfringidoUK).
La auditoria del archivo fuente confirma 30 tests: 6 en TarifarioSuperposicionR1DTest y 5 en ActaArticuloInfringidoUKR1DTest.
Delta 1755 -> 1785 = 30 tests, todos en ValorizacionInvariantesR1Test. Explicacion completa.

---

## Actualización 8F-11D (2026-07-05)

### Tablas alineadas (6)
1. fal_tarifario_unidad_faltas → FalTarifarioUnidadFaltas
2. fal_medida_preventiva → FalMedidaPreventiva
3. fal_articulo_medida_preventiva → FalArticuloMedidaPreventiva (PK compuesta via ArticuloMedidaPreventivaId)
4. fal_acta_articulo_infringido → FalActaArticuloInfringido
5. fal_acta_valorizacion → FalActaValorizacion (versionRow, optimistic locking)
6. fal_acta_valorizacion_item → FalActaValorizacionItem

### Enums nuevos con short codigo()
- TipoUnidadFaltas: 1=SALARIO, 2=UNIDAD_FIJA, 3=MONTO
- EstadoValorizacion: 1=PRELIMINAR, 2=CONFIRMADA, 3=REEMPLAZADA, 4=ANULADA
- TipoValorizacionActa: 1=INFRACCION_BASE ... 7=GESTION_EXTERNA
- CriterioTarifario: 1=ULTIMO_VIGENTE, 2=MANTIENE_ANTERIOR, 3=MANUAL
- OrigenValorizacion: 1=SISTEMA ... 5=APREMIO
- TipoValorizacionItem: 1=AUTOMATICA, 2=PAGO_VOLUNTARIO, 3=MANUAL, 4=FALLO
- MotivoBajaArticuloInfringido: 1=CORRECCION_IMPUTACION ... 4=ERROR_CARGA
- MotivoManualizacionValorizacion: 1=CRITERIO_AUTORIDAD ... 5=OTRO_FUNDADO

### Conteo actualizado
- **27 → 21 tablas FALTA_EN_INMEMORY** (29 originales - 2 de 8F-11C - 6 de 8F-11D = 21)
- Tests: 1660 → **1755** (8F-11D) → **1785** (8F-11D-R1: +30 tests en ValorizacionInvariantesR1Test)
- Próximo slice: **8F-11E**

---

## Actualizacion 8F-11D-R1 (2026-07-06)

### Invariantes aplicados (11)
- R1-A: SnapshotRecalculador proyecta valorizacion vigente CONFIRMADA en snapshot
- R1-B: confirmarVigenteAtomico: una sola valorizacion vigente+confirmada por acta+tipo (CAS + synchronized)
- R1-C: FalActaValorizacionItem inmutable post-confirmacion del padre
- R1-D.8.5: FalActaValorizacion: inmutabilidad economica via verificarMutable()
- R1-D.8.6: transiciones de estado via metodos de dominio (marcarConfirmada/Reemplazada/Anulada)
- R1-D.8.7: FalTarifarioUnidadFaltas: guarda atomica solapamiento de vigencia
- R1-D.8.8: FalMedidaPreventiva: crearNuevaVersionAtomico (UK codigo+version)
- R1-D.8.9: FalArticuloMedidaPreventiva: guarda contra reactivacion silenciosa
- R1-D.8.10: FalActaArticuloInfringido: UK activo (actaId, articuloId)
- R1-D.8.11: calcularBasePreliminar: lanza PrecondicionVioladaException si sin tarifario activo
- R1-E: Documentacion: 110, 99, 109 actualizados; conteo FALTA_EN_INMEMORY corregido a 21

### Paridad post-R1

| Categoria | Antes R1 | Despues R1 |
|-----------|----------|------------|
| ALINEADO | 26 | 32 | -> **41** (8F-11E) |
| FALTA_EN_INMEMORY | 27 | 21 | -> **12** (8F-11E) |
| PARCIAL | 4 | 4 |
| SEMANTICA_INCOMPATIBLE | 1 tabla | 1 tabla |
| RELACION_INCOMPLETA | 1 tabla | 1 tabla |
| NO_PERSISTIBLE tablas | 3 | 3 |
| **Total tablas explicadas** | **62** | **62** |

### Tests
- Antes 8F-11D-R1: 1755
- Despues 8F-11D-R1: **1785** (+30 en ValorizacionInvariantesR1Test)
- Detalle: SnapshotProyeccion(4) + ConfirmarVigenteAtomico(4) + ItemInmutabilidad(4) + TarifarioSuperposicion(**6**) + MedidaPreventivaVersion(4) + ActaArticuloInfringidoUK(**5**) + ArticuloMedidaReactivacion(3) = 30
- Los 3 tests adicionales vs. reporte preliminar (27): TarifarioSuperposicion tiene 6 no 5 (find_ultimo_vigente_detecta_invariante_rota); ActaArticuloInfringidoUK tiene 5 no 3 (diferente_acta_no_conflicta, diferente_articulo_no_conflicta)

### Frase de cierre
> 8F-11D-R1 CERRADO 8F-11D-R1 - 1785 tests - 32 ALINEADO - 21 FALTA_EN_INMEMORY - 4 PARCIAL
CERRADO 8F-11E - 1901 tests - 41 ALINEADO - 12 FALTA_EN_INMEMORY - 4 PARCIAL RELACION_INCOMPLETA - 3 NO_PERSISTIBLE stor_* - Total: 62 tablas explicadas.

| Aspecto temporal | InMemory | MariaDB |
|---|---|---|
| Origen timestamp funcional | FaltasClock (servicio) | Mismo valor recibido del servicio |
| Tests deterministas | Clock.fixed via FaltasClockTestSupport | Mismos asserts con reloj fijo |
| Regresion | FaltasClockUsageGuardTest | N/A (misma regla de origen) |


### Garantias OCC - CIERRE-OCC-INMEMORY-PRE-R11 (2026-07-09)

| Garantia | InMemory | MariaDB |
|---|---|---|
| OCC | `ConcurrentHashMap.compute()` - CAS atomico por `idActa` | `UPDATE ... WHERE id=? AND version_row=?` |
| Ganador | Primer thread cuya lambda en `compute` pasa el check | Primera transaccion que actualiza la fila |
| Perdedor | `ConcurrenciaConflictoException` (RuntimeException) | `rowsAffected==0` -> misma excepcion |
| Incremento | `versionRow = esperado + 1` (exactamente una vez) | `version_row = version_row + 1` (exactamente una vez) |
| Aislamiento | Por `idActa`: actas distintas no se bloquean entre si | Por `id_acta`: filas distintas no se bloquean entre si |
| Estado parcial del perdedor | Ninguno: mapa sin cambios | Ninguno: fila sin cambios (rollback implicito) |

---

## Cierre pre-MariaDB — R-08, OCC y R-11

### R-08 — Tiempo determinista con `FaltasClock`

Estado: **CERRADO**.

- `FaltasClock` es la abstracción temporal canónica del módulo.
- Zona funcional: `America/Argentina/Buenos_Aires`.
- Un único instante por operación.
- Eliminadas lecturas directas de tiempo en los flujos auditados.
- Tests de reloj inyectable e instante único por operación.

### OCC InMemory — Concurrencia optimista

Estado: **CERRADO**.

- Control de concurrencia optimista atómico en repositorios InMemory.
- `ActaConcurrenciaTest`: **50/50 PASS**.
- Suite previa a R-11: **2461 tests, 0 failures, 0 errors**.
- No quedó una falla aislada atribuible solo a la suite completa.

### R-11 — Contrato global de errores HTTP

Estado: **CERRADO**.

- DTO canónico: `ErrorResponse`.
- Advice global: `GlobalFaltasControllerAdvice`.
- Eliminados handlers locales duplicados en 21 controladores.
- Cobertura arquitectural de 50 excepciones de dominio.
- OCC: HTTP 409.
- Precondiciones: HTTP 422.
- Validaciones/request inválido: HTTP 400.
- No encontrado: HTTP 404.
- Método/media type: HTTP 405/415.
- 401/403 preservados por Spring Security.
- Fallback 500 seguro, sin exposición de stack trace.
- Suite final: **2478 tests, 0 failures, 0 errors**.
- Sin cambios de dominio, rutas, eventos, estados ni transiciones.
- Sin JDBC/JPA/MariaDB/DDL en este cierre.

### Estado de preparación para persistencia

Precondiciones cumplidas:
- tiempo determinista;
- OCC estabilizado;
- contrato global de errores;
- suite completa verde;
- modelo InMemory preservado como referencia de paridad.

Siguiente slice autorizado:

> **8F-12A / Slice 9 — Infraestructura y DDL MariaDB**
