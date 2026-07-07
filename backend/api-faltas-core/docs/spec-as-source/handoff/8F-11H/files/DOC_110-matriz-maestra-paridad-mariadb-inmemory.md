# 110 Ã¢â‚¬â€ Matriz Maestra de Paridad MariaDB Ã¢â€ â€ InMemory

Slice: 8F-11A + 8F-11A-R1 + 8F-11F (correcci??n documental) / 8F-11D + 8F-11D-R1 (valorizacion + invariantes) / 8F-11F (fallo + apelacion + firmeza) / **8F-11G (observacion + paralizacion + archivo + motivo_archivo)**  
**Fecha:** 2026-07-04 (8F-11A) / 2026-07-05 (8F-11A-R1, 8F-11B, 8F-11C) / 2026-07-06 (8F-11D, 8F-11D-R1, 8F-11E, 8F-11F)  
**Tipo:** AuditorÃƒÂ­a y documentaciÃƒÂ³n exhaustiva. Sin cambios funcionales Java.  
**MÃƒÂ³dulo:** backend/api-faltas-core  
**Build base:** 1976 tests, 0 failures, 0 errors, BUILD SUCCESS (verificado en 8F-11F)  
**Fuente vigente:** este documento reemplaza a 109 como matriz de paridad activa. El 109 queda como historial de auditorÃƒÂ­a de 8F-9/8F-10. Corregido en 8F-11A-R1: conteos (62/29 INMEMORY). 8F-11B cerrado: identidades, enums, versionRow. 8F-11C cerrado: FalPersona+FalPersonaDomicilio ALINEADOS (27Ã¢â€ â€™21 FALTA_EN_INMEMORY), P1 cerrada (SMALLINT 0-4), P2 cerrada (firmeza en fallo), catÃƒÂ¡logos, roadmap. 8F-11D+R1 cerrado: 6 tablas normativa/valorizacion ALINEADOS (21 FALTA_EN_INMEMORY), 30 tests nuevos, 1785 total. Correccion 8F-11D-R1-DOC: FALTA_EN_INMEMORY canonicas=21. 8F-11E cerrado: combinacion documental, DocumentoVariableContextBuilder. 8F-11F cerrado: fal_acta_fallo + fal_acta_apelacion ALINEADOS, fal_acta_apelacion_documento NUEVO ALINEADO, firmeza inline, multi-fallo, 4 tipos resolucion, 1976 tests, 39 tests nuevos.

---

## 0. Objetivo y guardrails

Este documento es la fuente ÃƒÂºnica de verdad sobre la paridad entre el modelo MariaDB lÃƒÂ³gico documentado y la implementaciÃƒÂ³n InMemory actual.

**Criterio de completitud:** Toda tabla MariaDB tiene estado. Toda entidad persistible InMemory tiene tabla candidata. Toda diferencia tiene acciÃƒÂ³n y slice asignado.

**Guardrails de este slice:**
- NO implementar JDBC, MariaDB real, SQL ejecutable, Flyway, Liquibase
- NO usar `@Entity`, `JpaRepository`, `EntityManager`
- NO implementar frontend Angular, PDF real, storage real
- NO crear endpoints productivos nuevos
- NO reabrir decisiones D1Ã¢â‚¬â€œD9 ya cerradas

---

## 1. Resumen ejecutivo de paridad

| CategorÃƒÂ­a | Cantidad |
|-----------|----------|
| Tablas MariaDB auditadas | 62 |
| Entidades InMemory persistibles | 31 |
| FALTA_EN_MARIADB | 0 |
| **ALINEADO** | **52 tablas** (8F-11H: +4 pagos) |
| **FALTA_EN_INMEMORY** | **4 tablas** |
| FALTA_EN_MARIADB | 0 |
| IDENTIDAD_INCOMPATIBLE | 0 (5 resueltas en 8F-11B) |
| TIPO_INCOMPATIBLE | 0 (3 resueltos en 8F-11B) |
| **PARCIAL** | **2 tablas** (fal_acta, fal_acta_evento Ã¢â‚¬â€ fal_acta_fallo y fal_acta_apelacion ahora ALINEADOS por 8F-11F) |
| SEMANTICA_INCOMPATIBLE | 0 (resuelta en 8F-11H) |
| ENUM_SIN_CODIGO | 0 (2 resueltos en 8F-11B) |
| **RELACION_INCOMPLETA** | **1 tabla** (fal_acta_documento - GAP-12, 8F-11J) |
| **NO_PERSISTIBLE tablas** | **3 tablas** stor_* (NO_PERSISTIBLE desde dominio Faltas) |
| *Suma (categorias excluyentes)* | *32 + 21 + 4 + 1 + 1 + 3 = 62* |
| NULABILIDAD_INCOMPATIBLE | 0 |
| SOLO_DEMO_TEST | 21 clases Java |
| SOLO_INFRAESTRUCTURA | 2 categorÃƒÂ­as |
| NO_PERSISTIBLE entidades | 25+ (servicios, DTOs, enums puro Java) |


---

## 2. Inventario canÃƒÂ³nico MariaDB Ã¢â‚¬â€ Fase 1

### SecciÃƒÂ³n 1 Ã¢â‚¬â€ Dependencias

#### `fal_dependencia`
- **PropÃƒÂ³sito:** Unidades administrativas (organismos/dependencias)
- **PK:** id BIGINT AUTO_INCREMENT
- **Campos clave:** codigo_dependencia VARCHAR(20), nombre VARCHAR(200), tipo_acta SMALLINT, si_activo BOOLEAN
- **AuditorÃƒÂ­a:** fh_alta, id_user_alta
- **Entidad Java esperada:** FalDependencia Ã¢Å“â€œ existe
- **Estado InMemory:** ALINEADO

#### `fal_dependencia_version`
- **PropÃƒÂ³sito:** Versiones de la dependencia con datos cambiantes
- **PK:** id_dependencia + version_dep (compuesta)
- **FK:** id_dependencia Ã¢â€ â€™ fal_dependencia
- **Campos clave:** nombre_version, tipo_acta SMALLINT, si_activo, fh_vig_desde, fh_vig_hasta
- **AuditorÃƒÂ­a:** fh_alta, id_user_alta
- **Entidad Java esperada:** FalDependenciaVersion Ã¢Å“â€œ existe
- **Estado InMemory:** ALINEADO

---

### SecciÃƒÂ³n 2 Ã¢â‚¬â€ Inspectores

#### `fal_inspector`
- **PropÃƒÂ³sito:** Inspectores/agentes del organismo
- **PK:** id BIGINT AUTO_INCREMENT
- **Campos clave:** legajo VARCHAR(20), apellido, nombre, si_activo BOOLEAN
- **AuditorÃƒÂ­a:** fh_alta, id_user_alta
- **Entidad Java esperada:** FalInspector Ã¢Å“â€œ existe
- **Estado InMemory:** ALINEADO (base)

#### `fal_inspector_version`
- **PropÃƒÂ³sito:** VersiÃƒÂ³n del inspector con datos del perÃƒÂ­odo
- **PK:** id_inspector + version_insp (compuesta)
- **FK:** id_inspector Ã¢â€ â€™ fal_inspector
- **Campos clave:** apellido, nombre, id_dependencia, ver_dependencia, si_activo, fh_vig_desde, fh_vig_hasta
- **AuditorÃƒÂ­a:** fh_alta, id_user_alta
- **Entidad Java esperada:** FalInspectorVersion Ã¢Å“â€œ existe
- **Estado InMemory:** COMPLETO

---

### SecciÃƒÂ³n 3 Ã¢â‚¬â€ Firmantes

#### `fal_firmante`
- **PK:** id BIGINT AUTO_INCREMENT
- **Entidad Java:** FalFirmante Ã¢Å“â€œ existe Ã¢â‚¬â€ **Estado:** ALINEADO (8A-3D)

#### `fal_firmante_version`
- **PK:** id_firmante + ver_firmante (compuesta)
- **FK:** id_firmante Ã¢â€ â€™ fal_firmante
- **Entidad Java:** FalFirmanteVersion Ã¢Å“â€œ existe Ã¢â‚¬â€ **Estado:** ALINEADO

#### `fal_firmante_version_habilitacion`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_firmante + ver_firmante Ã¢â€ â€™ fal_firmante_version
- **Campos clave:** tipo_docu SMALLINT, rol_firma_req SMALLINT, mecanismo_firma_req SMALLINT
- **Entidad Java:** FalFirmanteVersionHabilitacion Ã¢Å“â€œ existe Ã¢â‚¬â€ **Estado:** ALINEADO

---

### SecciÃƒÂ³n 4 Ã¢â‚¬â€ Personas y domicilios

#### `fal_persona`
- **PropÃƒÂ³sito:** Maestro de personas (infractores, presentantes)
- **PK:** id BIGINT AUTO_INCREMENT
- **Campos clave:** nro_documento VARCHAR(20), tipo_documento SMALLINT, nombre_completo VARCHAR(300), fh_nacimiento DATE NULL, sexo SMALLINT NULL, si_identificado BOOLEAN, si_extranjero BOOLEAN, id_ign BIGINT NULL, id_indec BIGINT NULL, id_local BIGINT NULL
- **AuditorÃƒÂ­a:** fh_alta, id_user_alta, fh_mod, id_user_mod
- **Entidad Java esperada:** `FalPersona` Ã¢â‚¬â€ **EXISTE**
- **Estado InMemory:** ALINEADO Ã¢â‚¬â€ InMemoryPersonaRepository Ã¢â‚¬â€ 8F-11C CERRADO (2026-07-05)

#### `fal_persona_domicilio`
- **PropÃƒÂ³sito:** Domicilios de personas
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_persona Ã¢â€ â€™ fal_persona
- **Campos clave:** tipo_domicilio SMALLINT, id_calle_version BIGINT NULL FK, nro_puerta VARCHAR(10) NULL, piso VARCHAR(5) NULL, dpto VARCHAR(5) NULL, localidad_id BIGINT NULL, texto_libre VARCHAR(500) NULL, si_principal TINYINT(1), id_tca BIGINT NULL, id_loc BIGINT NULL
- **AuditorÃƒÂ­a:** fh_alta, id_user_alta
- **Entidad Java esperada:** `FalPersonaDomicilio` Ã¢â‚¬â€ **EXISTE**
- **Estado InMemory:** ALINEADO Ã¢â‚¬â€ InMemoryPersonaDomicilioRepository Ã¢â‚¬â€ 8F-11C CERRADO (2026-07-05)

---

### SecciÃƒÂ³n 5 Ã¢â‚¬â€ Acta core

#### `fal_acta`
- **PropÃƒÂ³sito:** Entidad central del expediente de faltas
- **PK:** id BIGINT AUTO_INCREMENT
- **UK:** uuid_tecnico CHAR(36)
- **FK:** id_talonario Ã¢â€ â€™ num_talonario, id_inspector + ver_inspector Ã¢â€ â€™ fal_inspector_version, id_dependencia + ver_dependencia Ã¢â€ â€™ fal_dependencia_version, id_persona Ã¢â€ â€™ fal_persona (futuro)
- **Campos:**
  - nro_acta VARCHAR(20) NULL
  - tipo_acta SMALLINT NOT NULL (enum TipoActa)
  - id_talonario BIGINT NULL, nro_talonario_usado INT NULL
  - fecha_acta DATE NOT NULL, fecha_labrado DATETIME(6) NOT NULL
  - domicilio_hecho VARCHAR(500) NULL, lat_infr DOUBLE NULL, lon_infr DOUBLE NULL
  - resultado_firma_infractor SMALLINT NOT NULL
  - bloque_actual SMALLINT NOT NULL, estado_procesal SMALLINT NOT NULL
  - situacion_administrativa SMALLINT NOT NULL
  - resultado_final SMALLINT NOT NULL (o VARCHAR(30) Ã¢â‚¬â€ decisiÃƒÂ³n P1)
  - id_persona BIGINT NULL FK
- **AuditorÃƒÂ­a:** fh_alta, id_user_alta, fh_mod, id_user_mod
- **Entidad Java:** FalActa Ã¢Å“â€œ existe
- **Estado InMemory:** PARCIAL Ã¢â‚¬â€ gaps: tipoActa String, resultadoFinal tipo, datos persona embebidos, sin auditorÃƒÂ­a

---

### SecciÃƒÂ³n 6 Ã¢â‚¬â€ Eventos, snapshot, evidencias y observaciones

#### `fal_acta_evento`
- **PropÃƒÂ³sito:** Log append-only de eventos del expediente
- **PK:** id BIGINT AUTO_INCREMENT (o UUID Ã¢â‚¬â€ verificar modelo final)
- **FK:** id_acta Ã¢â€ â€™ fal_acta
- **Campos clave:** tipo_evt SMALLINT NOT NULL, fecha_evento DATETIME(6), orden_logico INT, id_documento BIGINT NULL, id_notificacion BIGINT NULL, id_operador CHAR(36), descripcion TEXT NULL, payload JSON NULL
- **Entidad Java:** FalActaEvento Ã¢Å“â€œ (record), id String Ã¢â‚¬â€ **Estado:** PARCIAL

#### `fal_acta_snapshot`
- **PropÃƒÂ³sito:** ProyecciÃƒÂ³n operativa derivada del expediente (1:1 con acta)
- **PK:** id_acta BIGINT FK
- **Entidad Java:** FalActaSnapshot Ã¢Å“â€œ Ã¢â‚¬â€ **Estado:** ALINEADO

#### `fal_acta_evidencia`
- **PropÃƒÂ³sito:** Evidencias del acta
- **PK:** id BIGINT AUTO_INCREMENT
- **Entidad Java:** FalActaEvidencia Ã¢Å“â€œ Ã¢â‚¬â€ **Estado:** ALINEADO

#### `fal_observacion`
- **PropÃƒÂ³sito:** Observaciones tipificadas por entidad
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_acta Ã¢â€ â€™ fal_acta
- **Campos clave:** entidad_tipo SMALLINT, entidad_id BIGINT, texto TEXT, id_user_alta, fh_alta
- **Entidad Java:** NO EXISTE
- **Entidad Java:** FalObservacion -- EXISTE (8F-11G)
- **Estado InMemory:** ALINEADO -- ObservacionRepository + InMemoryObservacionRepository + ObservacionService + EntidadTipoObservada (22 codigos) -- 8F-11G

---

### SecciÃƒÂ³n 7 Ã¢â‚¬â€ Normativa

#### `fal_dependencia_normativa`
- **Entidad Java:** FalDependenciaNormativa Ã¢Å“â€œ Ã¢â‚¬â€ **Estado:** ALINEADO

#### `fal_normativa_faltas`
- **Entidad Java:** FalNormativaFaltas Ã¢Å“â€œ Ã¢â‚¬â€ **Estado:** ALINEADO

#### `fal_articulo_normativa_faltas`
- **Entidad Java:** FalArticuloNormativaFaltas Ã¢Å“â€œ Ã¢â‚¬â€ **Estado:** ALINEADO

#### `fal_tarifario_unidad_faltas`
- **PropÃƒÂ³sito:** Valores unitarios por artÃƒÂ­culo para cÃƒÂ¡lculo de valorizaciÃƒÂ³n
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_articulo Ã¢â€ â€™ fal_articulo_normativa_faltas
- **Campos clave:** fh_vig_desde DATE, fh_vig_hasta DATE NULL, valor_unitario DECIMAL(14,4)
- **Entidad Java:** NO EXISTE
- **Estado InMemory:** ALINEADO (8F-11D + R1-D) Ã¢â‚¬â€ sin solapamiento de rangos activos

#### `fal_medida_preventiva`
- **Entidad Java:** FalMedidaPreventiva EXISTE Ã¢â‚¬â€ version atomica, una activa por codigo
- **Estado:** ALINEADO (8F-11D + R1-D)

#### `fal_articulo_medida_preventiva`
- **Entidad Java:** FalArticuloMedidaPreventiva EXISTE Ã¢â‚¬â€ PK compuesta record, sin reactivacion silenciosa
- **Estado:** ALINEADO (8F-11D + R1-D)

---

### SecciÃƒÂ³n 8 Ã¢â‚¬â€ ArtÃƒÂ­culos infringidos y valorizaciÃƒÂ³n

#### `fal_acta_articulo_infringido`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_acta Ã¢â€ â€™ fal_acta, id_articulo Ã¢â€ â€™ fal_articulo_normativa_faltas
- **Entidad Java:** NO EXISTE
- **Estado InMemory:** ALINEADO (8F-11D + R1-D) Ã¢â‚¬â€ UK activo (actaId, articuloId)

#### `fal_acta_valorizacion`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_acta Ã¢â€ â€™ fal_acta
- **Campos:** monto_total DECIMAL(14,2), fh_calculo DATETIME(6), si_vigente BOOLEAN
- **Entidad Java:** FalActaValorizacion EXISTE Ã¢â‚¬â€ confirmarVigenteAtomico garantiza una sola vigente CONFIRMADA por acta+tipo
- **Estado InMemory:** ALINEADO (8F-11D + R1-B)

#### `fal_acta_valorizacion_item`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_valorizacion Ã¢â€ â€™ fal_acta_valorizacion
- **Entidad Java:** FalActaValorizacionItem EXISTE Ã¢â‚¬â€ inmutable post-confirmacion (R1-C)
- **Estado:** ALINEADO (8F-11D + R1-C)

---

### SecciÃƒÂ³n 9 Ã¢â‚¬â€ SatÃƒÂ©lites del acta

#### `fal_acta_transito`
- **PK:** id_acta BIGINT (1:1)
- **Entidad Java:** NO EXISTE Ã¢â‚¬â€ **Estado:** **ALINEADO** (8F-11E)

#### `fal_acta_transito_alcoholemia`
- **PK:** id BIGINT AUTO_INCREMENT
- **Entidad Java:** NO EXISTE Ã¢â‚¬â€ **Estado:** **ALINEADO** (8F-11E)

#### `fal_acta_vehiculo`
- **PK:** id BIGINT AUTO_INCREMENT
- **Entidad Java:** NO EXISTE Ã¢â‚¬â€ **Estado:** **ALINEADO** (8F-11E)

#### `fal_acta_contravencion`
- **PK:** id_acta BIGINT (1:1)
- **Entidad Java:** NO EXISTE Ã¢â‚¬â€ **Estado:** **ALINEADO** (8F-11E)

#### `fal_acta_sustancias_alimenticias`
- **PK:** id_acta BIGINT (1:1)
- **Entidad Java:** NO EXISTE Ã¢â‚¬â€ **Estado:** **ALINEADO** (8F-11E)

#### `fal_acta_medida_preventiva`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_acta Ã¢â€ â€™ fal_acta, id_medida Ã¢â€ â€™ fal_medida_preventiva
- **Entidad Java:** NO EXISTE Ã¢â‚¬â€ **Estado:** **ALINEADO** (8F-11E)

---

### SecciÃƒÂ³n 10 Ã¢â‚¬â€ Bloqueantes

#### `fal_acta_bloqueante_cierre_material`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_acta Ã¢â€ â€™ fal_acta
- **Campos:** origen SMALLINT, estado SMALLINT, si_activo BOOLEAN, descripcion TEXT NULL, fh_alta DATETIME(6), fh_cierre DATETIME(6) NULL
- **Entidad Java:** FalBloqueanteMaterial Ã¢Å“â€œ, **id String (UUID)**
- **Estado InMemory:** ALINEADO (8F-11B)


---

### SecciÃƒÂ³n 11 Ã¢â‚¬â€ Documentos, plantillas y firma

#### `fal_documento`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_acta Ã¢â€ â€™ fal_acta, id_plantilla Ã¢â€ â€™ fal_documento_plantilla
- **Campos:** tipo_docu SMALLINT, nro_docu VARCHAR(30) NULL, estado_docu SMALLINT, storage_key VARCHAR(500) NULL, hash_docu VARCHAR(128) NULL, tipo_firma_req SMALLINT, fh_alta, id_user_alta
- **Entidad Java:** FalDocumento Ã¢Å“â€œ Ã¢â‚¬â€ **Estado:** ALINEADO (auditado 8C-6E)

#### `fal_acta_documento`
- **PropÃƒÂ³sito:** Tabla pivot relaciÃƒÂ³n acta-documento
- **PK:** id_acta + id_documento (compuesta)
- **Entidad Java:** NO EXISTE como entidad separada (FalDocumento.idActa cubre caso simple)
- **Estado InMemory:** RELACION_INCOMPLETA Ã¢â‚¬â€ GAP-12, Slice 8F-11J

#### `fal_documento_firma`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_documento Ã¢â€ â€™ fal_documento, id_firmante + ver_firmante Ã¢â€ â€™ fal_firmante_version
- **Campos:** estado_firma SMALLINT, seq_firma_req SMALLINT, tipo_firma SMALLINT, storage_key VARCHAR(500) NULL, hash_firma VARCHAR(128) NULL, fh_firma DATETIME(6) NULL
- **Entidad Java:** FalDocumentoFirma Ã¢Å“â€œ Ã¢â‚¬â€ **Estado:** ALINEADO (refactor 8C-6B-1)

#### `fal_documento_firma_req`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_documento Ã¢â€ â€™ fal_documento
- **Entidad Java:** FalDocumentoFirmaReq Ã¢Å“â€œ Ã¢â‚¬â€ **Estado:** ALINEADO

#### `fal_documento_plantilla`
- **PK:** id BIGINT AUTO_INCREMENT
- **Campos:** codigo VARCHAR(50), nombre VARCHAR(200), tipo_docu SMALLINT, accion_documental SMALLINT, tipo_acta SMALLINT NULL, tipo_firma_req SMALLINT, si_requiere_numeracion BOOLEAN, momento_numeracion_docu SMALLINT, si_notificable BOOLEAN, si_genera_pdf BOOLEAN, si_seleccionable BOOLEAN, si_activa BOOLEAN, fh_vig_desde DATE, fh_vig_hasta DATE NULL, fh_alta DATETIME(6), id_user_alta CHAR(36)
- **Entidad Java:** FalDocumentoPlantilla Ã¢Å“â€œ Ã¢â‚¬â€ **Estado:** ALINEADO

#### `fal_documento_plantilla_firma_req`
- **PK:** id_plantilla + seq_firma_req (compuesta)
- **Entidad Java:** FalDocumentoPlantillaFirmaReq Ã¢Å“â€œ Ã¢â‚¬â€ **Estado:** ALINEADO

#### `fal_documento_plantilla_contenido` (nueva en 8F-10)
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_plantilla Ã¢â€ â€™ fal_documento_plantilla
- **Campos:**
  - version_contenido SMALLINT NOT NULL
  - titulo VARCHAR(200) NOT NULL
  - cuerpo_markdown TEXT NOT NULL Ã¢â€ Â renombrado desde cuerpo_template (8F-10-R1)
  - encabezado_markdown TEXT NULL Ã¢â€ Â renombrado desde encabezado_template (8F-10-R1)
  - pie_markdown TEXT NULL Ã¢â€ Â renombrado desde pie_template (8F-10-R1)
  - variables_declaradas_json JSON NOT NULL DEFAULT '[]' (D4)
  - si_activo BOOLEAN NOT NULL
  - fh_vig_desde DATETIME(6) NOT NULL, fh_vig_hasta DATETIME(6) NULL
  - fh_alta DATETIME(6) NOT NULL, id_user_alta CHAR(36) NOT NULL
- **Nota:** formato/FormatoPlantillaContenido ELIMINADO en 8F-10-R1
- **Entidad Java:** FalDocumentoPlantillaContenido Ã¢Å“â€œ Ã¢â‚¬â€ **Estado:** ALINEADO (8F-10-R2)

#### `fal_documento_plantilla_default` (nueva en 8F-10)
- **PK:** id BIGINT AUTO_INCREMENT
- **ÃƒÂndice:** (accion_documental, tipo_acta, id_dependencia)
- **Campos:**
  - accion_documental SMALLINT NOT NULL
  - tipo_acta SMALLINT NULL (NULL = genÃƒÂ©rico)
  - tipo_docu SMALLINT NOT NULL
  - id_dependencia BIGINT NULL, ver_dependencia SMALLINT NULL
  - id_plantilla BIGINT NOT NULL FK Ã¢â€ â€™ fal_documento_plantilla
  - prioridad SMALLINT NOT NULL (D9)
  - fh_vig_desde DATETIME(6) NOT NULL, fh_vig_hasta DATETIME(6) NULL
  - si_activo BOOLEAN NOT NULL
  - fh_alta DATETIME(6) NOT NULL, id_user_alta CHAR(36) NOT NULL
- **Entidad Java:** FalDocumentoPlantillaDefault Ã¢Å“â€œ Ã¢â‚¬â€ **Estado:** ALINEADO

#### `fal_documento_redaccion` (nueva en 8F-10)
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_documento Ã¢â€ â€™ fal_documento, id_plantilla_contenido Ã¢â€ â€™ fal_documento_plantilla_contenido, redaccion_origen_id Ã¢â€ â€™ fal_documento_redaccion (self)
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
- **Nota:** estado REABIERTA ELIMINADO en 8F-10-R1. 8F-10-R2 alineÃƒÂ³ entidad Java completamente.
- **Entidad Java:** FalDocumentoRedaccion Ã¢Å“â€œ Ã¢â‚¬â€ **Estado:** ALINEADO (8F-10-R2) Ã¢â€ Â COMPLETAMENTE ALINEADA

---

### SecciÃƒÂ³n 12 Ã¢â‚¬â€ Notificaciones

#### `fal_notificacion`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_acta Ã¢â€ â€™ fal_acta, id_documento Ã¢â€ â€™ fal_documento
- **Campos:** tipo_docu_notificado SMALLINT, canal VARCHAR(50), fecha_envio DATETIME(6), estado SMALLINT, resultado SMALLINT NULL, fecha_resultado DATETIME(6) NULL, intentos INT, observaciones TEXT NULL
- **Entidad Java:** FalNotificacion Ã¢Å“â€œ, **id String (UUID)**
- **Estado InMemory:** ALINEADO (8F-11B)

#### `fal_notificacion_intento`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_notificacion Ã¢â€ â€™ fal_notificacion
- **Campos:** nro_intento SMALLINT, canal VARCHAR(50), fecha_intento DATETIME(6), resultado SMALLINT, detalle TEXT NULL
- **Entidad Java:** NO EXISTE Ã¢â‚¬â€ **Estado:** FALTA_EN_INMEMORY, Slice 8F-11I

#### `fal_notificacion_acuse`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_notificacion Ã¢â€ â€™ fal_notificacion
- **Entidad Java:** NO EXISTE Ã¢â‚¬â€ **Estado:** FALTA_EN_INMEMORY, Slice 8F-11I

#### `fal_lote_correo`
- **PK:** id BIGINT AUTO_INCREMENT
- **Campos:** tipo_lote SMALLINT, fecha_generacion DATETIME(6), estado_lote SMALLINT, id_user_alta CHAR(36)
- **Entidad Java:** NO EXISTE Ã¢â‚¬â€ **Estado:** FALTA_EN_INMEMORY, Slice 8F-11I

---

### SecciÃƒÂ³n 13 Ã¢â‚¬â€ Fallo y firmeza

#### `fal_acta_fallo`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_acta Ã¢â€ â€™ fal_acta, valorizacion_id Ã¢â€ â€™ fal_acta_valorizacion, fallo_reemplazado_id Ã¢â€ â€™ fal_acta_fallo (self), documento_id Ã¢â€ â€™ fal_documento
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
- **AuditorÃƒÂ­a:** fh_alta, id_user_alta, fh_mod, id_user_mod
- **Entidad Java:** FalActaFallo Ã¢Å“â€œ, **id String (UUID)**
- **Estado InMemory:** IDENTIDAD_INCOMPATIBLE + mÃƒÂºltiples campos FALTA_EN_INMEMORY Ã¢â‚¬â€ Slice 8F-11B (id), 8F-11F (campos)

> **Nota D1:** FalActaFirmezaCondena NO tiene tabla propia. Los campos de firmeza van en fal_acta_fallo.
> La entidad Java FalActaFirmezaCondena existe actualmente; ver decisiÃƒÂ³n P2.

---

### SecciÃƒÂ³n 14 Ã¢â‚¬â€ ApelaciÃƒÂ³n

#### `fal_acta_apelacion`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_acta Ã¢â€ â€™ fal_acta, id_fallo Ã¢â€ â€™ fal_acta_fallo, documento_resolucion_id Ã¢â€ â€™ fal_documento
- **Campos:**
  - fallo_id BIGINT NOT NULL FK
  - estado_apelacion SMALLINT NOT NULL, fecha_presentacion DATETIME(6) NOT NULL
  - canal_apelacion SMALLINT NOT NULL, tipo_presentacion SMALLINT NOT NULL
  - texto_apelacion TEXT NULL, presentante VARCHAR(300) NULL
  - fundamentos TEXT NULL, observaciones TEXT NULL, si_activa BOOLEAN NOT NULL
  - fecha_resolucion DATETIME(6) NULL, fundamentos_resolucion TEXT NULL
  - observaciones_resolucion TEXT NULL, documento_resolucion_id BIGINT NULL FK
  - version_row INT NOT NULL DEFAULT 0
- **AuditorÃƒÂ­a:** fh_alta, id_user_alta, fh_mod, id_user_mod
- **Entidad Java:** FalActaApelacion Ã¢Å“â€œ, **id String (UUID), falloId String**
- **Estado InMemory:** IDENTIDAD_INCOMPATIBLE + mÃƒÂºltiples campos FALTA_EN_INMEMORY Ã¢â‚¬â€ Slice 8F-11B, 8F-11F

#### `fal_acta_apelacion_documento`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_apelacion Ã¢â€ â€™ fal_acta_apelacion, id_documento Ã¢â€ â€™ fal_documento
- **Entidad Java:** NO EXISTE Ã¢â‚¬â€ **Estado:** FALTA_EN_INMEMORY, Slice 8F-11F

---

### SecciÃƒÂ³n 15 Ã¢â‚¬â€ ParalizaciÃƒÂ³n

#### `fal_acta_paralizacion`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_acta Ã¢â€ â€™ fal_acta
- **Campos:** motivo SMALLINT NOT NULL, descripcion TEXT NULL, id_user_inicio CHAR(36) NOT NULL, fh_inicio DATETIME(6) NOT NULL, id_user_cierre CHAR(36) NULL, fh_cierre DATETIME(6) NULL, motivo_cierre SMALLINT NULL, si_activa BOOLEAN NOT NULL
- **AuditorÃƒÂ­a:** fh_alta, id_user_alta
- **Entidad Java:** NO EXISTE (FalActaParalizacion)
- **Entidad Java:** FalActaParalizacion -- EXISTE (8F-11G)
- **Estado InMemory:** ALINEADO -- ActaParalizacionRepository + InMemoryActaParalizacionRepository + ParalizacionActaService -- 8F-11G

---

### SecciÃƒÂ³n 16 Ã¢â‚¬â€ Archivo

#### `fal_acta_archivo`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_acta Ã¢â€ â€™ fal_acta
- **Campos:** motivo_archivo SMALLINT NOT NULL, descripcion TEXT NULL, id_user_archivo CHAR(36) NOT NULL, fh_archivo DATETIME(6) NOT NULL, id_user_reingreso CHAR(36) NULL, fh_reingreso DATETIME(6) NULL, motivo_reingreso SMALLINT NULL, si_archivado BOOLEAN NOT NULL
- **AuditorÃƒÂ­a:** fh_alta, id_user_alta
- **Entidad Java:** NO EXISTE (FalActaArchivo)
- **Entidad Java:** FalActaArchivo -- EXISTE (8F-11G)
- **Estado InMemory:** ALINEADO -- ActaArchivoRepository + InMemoryActaArchivoRepository + ArchivoActaService -- 8F-11G

---

### SecciÃƒÂ³n 17 Ã¢â‚¬â€ GestiÃƒÂ³n externa

#### `fal_acta_gestion_externa`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_acta Ã¢â€ â€™ fal_acta
- **Entidad Java:** FalGestionExterna Ã¢Å“â€œ, **id String (UUID)**
- **Estado InMemory:** ALINEADO (8F-11B)
- **Campos InMemory:** completamente alineados excepto id y auditorÃƒÂ­a (fh_alta, id_user_alta)

---

### SecciÃƒÂ³n 18 Ã¢â‚¬â€ Pagos

#### `fal_acta_obligacion_pago`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_acta Ã¢â€ â€™ fal_acta
- **Campos:** tipo_obligacion SMALLINT NOT NULL (1=PAGO_VOLUNTARIO, 2=CONDENA), estado SMALLINT NOT NULL, monto DECIMAL(14,2) NULL, referencia_pago VARCHAR(200) NULL, observaciones TEXT NULL, motivo_observacion TEXT NULL, fechas varios DATETIME(6)
- **AuditorÃƒÂ­a:** fh_alta, id_user_alta, fh_mod, id_user_mod
- **Entidad Java:** FalPagoVoluntario (tipo=1) y FalPagoCondena (tipo=2) Ã¢â‚¬â€ id String
- **Estado InMemory:** SEMANTICA_INCOMPATIBLE (D2 cerrada) Ã¢â‚¬â€ dos clases separadas vs tabla ÃƒÂºnica Ã¢â‚¬â€ Slice 8F-11H

#### `fal_acta_forma_pago`
- **Entidad Java:** NO EXISTE Ã¢â‚¬â€ **Estado:** FALTA_EN_INMEMORY, Slice 8F-11H

#### `fal_acta_plan_pago_ref`
- **Entidad Java:** NO EXISTE Ã¢â‚¬â€ **Estado:** FALTA_EN_INMEMORY, Slice 8F-11H

#### `fal_acta_pago_movimiento`
- **Entidad Java:** NO EXISTE Ã¢â‚¬â€ **Estado:** FALTA_EN_INMEMORY, Slice 8F-11H

---

### SecciÃƒÂ³n 19 Ã¢â‚¬â€ Talonarios y numeraciÃƒÂ³n

#### `num_politica`
- **Entidad Java:** NumPolitica Ã¢Å“â€œ Ã¢â‚¬â€ **Estado:** ALINEADO (deuda DDL fh_alta/id_user_alta en modelo MariaDB)

#### `num_talonario`
- **Entidad Java:** NumTalonario Ã¢Å“â€œ Ã¢â‚¬â€ **Estado:** ALINEADO

#### `num_talonario_ambito`
- **Entidad Java:** NumTalonarioAmbito Ã¢Å“â€œ Ã¢â‚¬â€ **Estado:** ALINEADO

#### `num_talonario_inspector`
- **Entidad Java:** NumTalonarioInspector Ã¢Å“â€œ Ã¢â‚¬â€ **Estado:** ALINEADO

#### `num_talonario_movimiento`
- **Entidad Java:** NumTalonarioMovimiento Ã¢Å“â€œ Ã¢â‚¬â€ **Estado:** ALINEADO

---

### SecciÃƒÂ³n 20 Ã¢â‚¬â€ QR y portal

#### `fal_acta_qr_acceso`
- **PK:** id BIGINT AUTO_INCREMENT
- **FK:** id_acta Ã¢â€ â€™ fal_acta
- **Campos:** token_qr VARCHAR(200), url_acceso VARCHAR(500), fh_generacion DATETIME(6), fh_vencimiento DATETIME(6) NULL, si_activo BOOLEAN
- **Entidad Java:** NO EXISTE
- **Estado InMemory:** FALTA_EN_INMEMORY Ã¢â‚¬â€ DIFERIBLE (baja prioridad) Ã¢â‚¬â€ Slice 8F-11K


---

## 3. Inventario canÃƒÂ³nico InMemory Ã¢â‚¬â€ Fase 2

### A. Entidades de dominio persistibles

| Clase Java | Tabla MariaDB | ID tipo Java | ID tipo MariaDB | Estado |
|---|---|---|---|---|
| `FalActa` | `fal_acta` | Long Ã¢Å“â€œ | BIGINT Ã¢Å“â€œ | PARCIAL (tipo_acta String, persona embebida, sin auditorÃƒÂ­a) |
| `FalActaEvento` | `fal_acta_evento` | String | verificar | PARCIAL |
| `FalActaSnapshot` | `fal_acta_snapshot` | Long (idActa) | BIGINT PK Ã¢Å“â€œ | ALINEADO |
| `FalActaEvidencia` | `fal_acta_evidencia` | Long Ã¢Å“â€œ | BIGINT Ã¢Å“â€œ | ALINEADO |
| `FalActaFallo` | `fal_acta_fallo` | **Long | ALINEADO id (8F-11B) + campos (8F-11F) |
| `FalActaApelacion` | `fal_acta_apelacion` | **Long | ALINEADO id (8F-11B) + campos (8F-11F) |
| `FalActaFirmezaCondena` | campos en `fal_acta_fallo` (D1) | String | Ã¢â‚¬â€ sin tabla propia | SEMANTICA_INCOMPATIBLE |
| `FalBloqueanteMaterial` | `fal_acta_bloqueante_cierre_material` | **String** | **BIGINT** Ã¢Å“â€” | IDENTIDAD_INCOMPATIBLE |
| `FalDependencia` | `fal_dependencia` | Long Ã¢Å“â€œ | BIGINT Ã¢Å“â€œ | ALINEADO |
| `FalDependenciaVersion` | `fal_dependencia_version` | Long+short | BIGINT+SMALLINT Ã¢Å“â€œ | ALINEADO |
| `FalDocumento` | `fal_documento` | Long Ã¢Å“â€œ | BIGINT Ã¢Å“â€œ | ALINEADO |
| `FalDocumentoFirma` | `fal_documento_firma` | Long Ã¢Å“â€œ | BIGINT Ã¢Å“â€œ | ALINEADO |
| `FalDocumentoFirmaReq` | `fal_documento_firma_req` | Long Ã¢Å“â€œ | BIGINT Ã¢Å“â€œ | ALINEADO |
| `FalDocumentoPlantilla` | `fal_documento_plantilla` | Long Ã¢Å“â€œ | BIGINT Ã¢Å“â€œ | ALINEADO |
| `FalDocumentoPlantillaFirmaReq` | `fal_documento_plantilla_firma_req` | Long+short | BIGINT+SMALLINT Ã¢Å“â€œ | ALINEADO |
| `FalDocumentoPlantillaContenido` | `fal_documento_plantilla_contenido` | Long Ã¢Å“â€œ | BIGINT Ã¢Å“â€œ | ALINEADO (8F-10-R2) |
| `FalDocumentoPlantillaDefault` | `fal_documento_plantilla_default` | Long Ã¢Å“â€œ | BIGINT Ã¢Å“â€œ | ALINEADO |
| `FalDocumentoRedaccion` | `fal_documento_redaccion` | Long Ã¢Å“â€œ | BIGINT Ã¢Å“â€œ | ALINEADO (8F-10-R2) |
| `FalFirmante` | `fal_firmante` | Long Ã¢Å“â€œ | BIGINT Ã¢Å“â€œ | ALINEADO |
| `FalFirmanteVersion` | `fal_firmante_version` | Long+short | BIGINT+SMALLINT Ã¢Å“â€œ | ALINEADO |
| `FalFirmanteVersionHabilitacion` | `fal_firmante_version_habilitacion` | Long Ã¢Å“â€œ | BIGINT Ã¢Å“â€œ | ALINEADO |
| `FalGestionExterna` | `fal_acta_gestion_externa` | **String** | **BIGINT** Ã¢Å“â€” | IDENTIDAD_INCOMPATIBLE |
| `FalInspector` | `fal_inspector` | Long Ã¢Å“â€œ | BIGINT Ã¢Å“â€œ | ALINEADO |
| `FalInspectorVersion` | `fal_inspector_version` | Long+short | BIGINT+SMALLINT Ã¢Å“â€œ | PARCIAL (faltan campos firma) |
| `FalNormativaFaltas` | `fal_normativa_faltas` | Long Ã¢Å“â€œ | BIGINT Ã¢Å“â€œ | ALINEADO |
| `FalArticuloNormativaFaltas` | `fal_articulo_normativa_faltas` | Long Ã¢Å“â€œ | BIGINT Ã¢Å“â€œ | ALINEADO |
| `FalDependenciaNormativa` | `fal_dependencia_normativa` | Long+Long | Ã¢â‚¬â€ Ã¢Å“â€œ | ALINEADO |
| `FalNotificacion` | `fal_notificacion` | **Long | ALINEADO (8F-11B) |
| `FalPagoVoluntario` | `fal_acta_obligacion_pago` (tipo=1) | String | BIGINT Ã¢Å“â€” | SEMANTICA_INCOMPATIBLE (D2 cerrada) |
| `FalPagoCondena` | `fal_acta_obligacion_pago` (tipo=2) | String | BIGINT Ã¢Å“â€” | SEMANTICA_INCOMPATIBLE (D2 cerrada) |
| `NumPolitica` | `num_politica` | Long Ã¢Å“â€œ | BIGINT Ã¢Å“â€œ | ALINEADO |
| `NumTalonario` | `num_talonario` | Long Ã¢Å“â€œ | BIGINT Ã¢Å“â€œ | ALINEADO |
| `NumTalonarioAmbito` | `num_talonario_ambito` | Ã¢â‚¬â€ compuesta | Ã¢â‚¬â€ compuesta Ã¢Å“â€œ | ALINEADO |
| `NumTalonarioInspector` | `num_talonario_inspector` | Long Ã¢Å“â€œ | BIGINT Ã¢Å“â€œ | ALINEADO |
| `NumTalonarioMovimiento` | `num_talonario_movimiento` | Long Ã¢Å“â€œ | BIGINT Ã¢Å“â€œ | ALINEADO |

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

**Repositorios pendientes de crear (22 entidades FALTA_EN_INMEMORY):** PersonaRepository, PersonaDomicilioRepository, ActaParalizacionRepository, ActaArchivoRepository, ObservacionRepository, NotificacionIntentoRepository, NotificacionAcuseRepository, LoteCorreoRepository, TarifarioUnidadRepository, MedidaPreventiva, ArticuloMedida, ActaArticuloInfringidoRepository, ActaValorizacionRepository, ActaValorizacionItemRepository, ActaDocumentoRepository, ActaApelacionDocumentoRepository, ActaObligacionPagoRepository (modelo real), ActaFormaPagoRepository, ActaPlanPagoRepository, ActaPagoMovimientoRepository, satÃƒÂ©lites (6), QrAccesoRepository.

### C. Enums de dominio Ã¢â‚¬â€ estado de cÃƒÂ³digo numÃƒÂ©rico

| Enum | CÃƒÂ³digo numÃƒÂ©rico | Columna MariaDB | Estado |
|---|---|---|---|
| `TipoActa` (TRANSITO, CONTRAVENCION, SUSTANCIAS_ALIMENTICIAS, COMERCIO) | **NO** | tipo_acta SMALLINT | ENUM_SIN_CODIGO Ã¢â‚¬â€ agregar 1-4 en 8F-11B |
| `EstadoRedaccionDocumento` (BORRADOR=1, CONFIRMADA=2, ANULADA=3) | **SÃƒÂ** | estado_redaccion SMALLINT | ALINEADO |
| `OrigenFirmezaCondena` (VENCIMIENTO_PLAZO_APELACION, APELACION_RECHAZADA) | **NO** | origen_firmeza SMALLINT NULL | ENUM_SIN_CODIGO Ã¢â‚¬â€ agregar 1=VENCIMIENTO, 2=RECHAZADA en 8F-11F |
| `ResultadoFinalActa` (0=SIN_RESULTADO_FINAL, 1=PAGO_VOLUNTARIO_PAGADO, 2=ABSUELTO, 3=CONDENA_FIRME, 4=ANULADO) | **SMALLINT** (pendiente implementaciÃƒÂ³n) | resultado_final SMALLINT NOT NULL | TIPO_INCOMPATIBLE Ã¢â‚¬â€ P1 CERRADA; implementar en 8F-11B |
| DemÃƒÂ¡s enums funcionales | verificar | columnas SMALLINT | verificar por enum en 8F-11B |

### D. Clases NO persistibles

| CategorÃƒÂ­a | Clases |
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

## 4. Matriz maestra campo por campo Ã¢â‚¬â€ Fase 3

### Leyenda de estados

| Estado | Significado |
|---|---|
| `ALINEADO` | Campo existe en ambos lados con tipo y semÃƒÂ¡ntica equivalente |
| `FALTA_EN_INMEMORY` | Campo existe en tabla MariaDB pero no en entidad Java |
| `FALTA_EN_MARIADB` | Campo existe en Java pero no hay tabla/columna MariaDB |
| `TIPO_INCOMPATIBLE` | Existe en ambos con tipo diferente |
| `NULABILIDAD_INCOMPATIBLE` | Difiere la nulabilidad |
| `IDENTIDAD_INCOMPATIBLE` | ID en Java es String/UUID, debe ser Long/BIGINT |
| `RELACION_INCOMPLETA` | FK o tabla pivot faltante en InMemory |
| `ENUM_SIN_CODIGO` | Enum sin cÃƒÂ³digo numÃƒÂ©rico para SMALLINT |
| `SEMANTICA_INCOMPATIBLE` | Modelado diferente (decisiÃƒÂ³n cerrada) |
| `SOLO_DEMO_TEST` | Solo para demo/test, no persistir |
| `SOLO_INFRAESTRUCTURA` | Infraestructura InMemory, no persistir |
| `NO_PERSISTIBLE` | Servicio, DTO, enum puro Java Ã¢â‚¬â€ sin tabla |

---

### Matriz: FalActa / fal_acta

| Campo MariaDB | Tipo MariaDB | Null | Campo Java | Tipo Java | Estado | AcciÃƒÂ³n | Slice |
|---|---|---|---|---|---|---|---|---|
| id | BIGINT AUTO_INCREMENT | NO | id | Long | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| uuid_tecnico | CHAR(36) | NO | uuidTecnico | String | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| nro_acta | VARCHAR(20) | YES | nroActa | String | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| tipo_acta | SMALLINT NOT NULL | NO | tipoActa | **TipoActa | ALINEADO (8F-11B) |
| id_talonario | BIGINT | YES | idTalonario | Long | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| nro_talonario_usado | INT | YES | nroTalonarioUsado | Integer | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| fecha_acta | DATE | NO | fechaActa | LocalDate | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| fecha_labrado | DATETIME(6) | NO | fechaLabrado | LocalDateTime | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| domicilio_hecho | VARCHAR(500) | YES | domicilioHecho | String | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| lat_infr | DOUBLE | YES | latInfr | Double | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| lon_infr | DOUBLE | YES | lonInfr | Double | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| resultado_firma_infractor | SMALLINT | NO | resultadoFirmaInfractor | ResultadoFirmaInfractor | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| bloque_actual | SMALLINT | NO | bloqueActual | BloqueActual | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| estado_procesal | SMALLINT | NO | estadoProcesal | EstadoProcesalActa | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| situacion_administrativa | SMALLINT | NO | situacionAdministrativa | SituacionAdministrativaActa | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| resultado_final | SMALLINT NOT NULL | NO | resultadoFinal | ResultadoFinalActa | ALINEADO (8F-11B) |
| id_persona | BIGINT | YES | Ã¢â‚¬â€ | Ã¢â‚¬â€ | FALTA_EN_INMEMORY | Agregar cuando FalPersona exista | 8F-11C |
| id_inspector | BIGINT | NO | idInspector | **Long | ALINEADO (8F-11B) |
| id_dependencia | BIGINT | NO | idDependencia | **Long | ALINEADO (8F-11B) |
| infractor_nombre | Ã¢â‚¬â€ (va a fal_persona) | Ã¢â‚¬â€ | infractorNombre | String | SEMANTICA_INCOMPATIBLE | Mover a FalPersona.nombre_completo | 8F-11C |
| infractor_documento | Ã¢â‚¬â€ (va a fal_persona) | Ã¢â‚¬â€ | infractorDocumento | String | SEMANTICA_INCOMPATIBLE | Mover a FalPersona.nro_documento | 8F-11C |
| domicilio_infractor | Ã¢â‚¬â€ (va a fal_persona_domicilio) | Ã¢â‚¬â€ | domicilioInfractor | String | SEMANTICA_INCOMPATIBLE | Mover a FalPersonaDomicilio.texto_libre | 8F-11C |
| fh_alta | DATETIME(6) | NO | Ã¢â‚¬â€ | Ã¢â‚¬â€ | ALINEADO (8F-11B) |
| id_user_alta | CHAR(36) | NO | Ã¢â‚¬â€ | Ã¢â‚¬â€ | ALINEADO (8F-11B) |
| fh_mod | DATETIME(6) | YES | Ã¢â‚¬â€ | Ã¢â‚¬â€ | ALINEADO (8F-11B) |
| id_user_mod | CHAR(36) | YES | Ã¢â‚¬â€ | Ã¢â‚¬â€ | ALINEADO (8F-11B) |

### Matriz: FalActaFallo / fal_acta_fallo

| Campo MariaDB | Tipo MariaDB | Null | Campo Java | Tipo Java | Estado | AcciÃƒÂ³n | Slice |
|---|---|---|---|---|---|---|---|---|
| id | BIGINT AUTO_INCREMENT | NO | id | **Long | ALINEADO (8F-11B) |
| id_acta | BIGINT | NO | actaId | Long | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| tipo_fallo | SMALLINT | NO | tipoFallo | TipoFalloActa | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| estado_fallo | SMALLINT | NO | estadoFallo | EstadoFalloActa | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| monto_condena | DECIMAL(14,2) | YES | montoCondena | BigDecimal | ALINEADO (simplificado) | Vincular a valorizacion_id en 8F-11D | 8F-11F |
| resultado_fallo | SMALLINT | YES | Ã¢â‚¬â€ | Ã¢â‚¬â€ | FALTA_EN_INMEMORY | Agregar enum ResultadoFalloActa o SMALLINT | 8F-11F |
| fundamentos | TEXT | YES | fundamentos | String | ALINEADO (D5) | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| documento_id | BIGINT | YES | documentoId | Long | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| valorizacion_id | BIGINT | YES | Ã¢â‚¬â€ | Ã¢â‚¬â€ | FALTA_EN_INMEMORY | Agregar Long valorizacionId | 8F-11F |
| fallo_reemplazado_id | BIGINT | YES | Ã¢â‚¬â€ | Ã¢â‚¬â€ | FALTA_EN_INMEMORY | Agregar Long falloReemplazadoId | 8F-11F |
| fecha_dictado | DATETIME(6) | NO | fechaDictado | LocalDateTime | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| fecha_notificacion | DATETIME(6) | YES | fechaNotificacion | LocalDateTime | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| fecha_resultado_final | DATETIME(6) | YES | fechaResultadoFinal | LocalDateTime | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| fh_firma | DATETIME(6) | YES | Ã¢â‚¬â€ | Ã¢â‚¬â€ | FALTA_EN_INMEMORY | Agregar LocalDateTime fhFirma | 8F-11F |
| fh_vto_apelacion | DATETIME(6) | YES | Ã¢â‚¬â€ | Ã¢â‚¬â€ | FALTA_EN_INMEMORY | Agregar LocalDateTime fhVtoApelacion | 8F-11F |
| si_activo | BOOLEAN | NO | siActivo | boolean | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| si_firme | BOOLEAN | NO | Ã¢â‚¬â€ | Ã¢â‚¬â€ | FALTA_EN_INMEMORY | Agregar boolean siFirme (D1) | 8F-11F |
| fh_firmeza | DATETIME(6) | YES | Ã¢â‚¬â€ | Ã¢â‚¬â€ | FALTA_EN_INMEMORY | Agregar LocalDateTime fhFirmeza (D1) | 8F-11F |
| origen_firmeza | SMALLINT | YES | Ã¢â‚¬â€ | Ã¢â‚¬â€ | FALTA_EN_INMEMORY | Agregar OrigenFirmezaCondena con cÃƒÂ³digo (D1) | 8F-11F |
| version_row | INT | NO | Ã¢â‚¬â€ | Ã¢â‚¬â€ | FALTA_EN_INMEMORY | Agregar int versionRow | 8F-11F |
| fh_alta | DATETIME(6) | NO | Ã¢â‚¬â€ | Ã¢â‚¬â€ | FALTA_EN_INMEMORY | AuditorÃƒÂ­a | 8F-11B |
| id_user_alta | CHAR(36) | NO | Ã¢â‚¬â€ | Ã¢â‚¬â€ | FALTA_EN_INMEMORY | AuditorÃƒÂ­a | 8F-11B |
| fh_mod | DATETIME(6) | YES | Ã¢â‚¬â€ | Ã¢â‚¬â€ | FALTA_EN_INMEMORY | AuditorÃƒÂ­a | 8F-11B |
| id_user_mod | CHAR(36) | YES | Ã¢â‚¬â€ | Ã¢â‚¬â€ | FALTA_EN_INMEMORY | AuditorÃƒÂ­a | 8F-11B |

### Matriz: FalActaApelacion / fal_acta_apelacion

| Campo MariaDB | Tipo MariaDB | Null | Campo Java | Tipo Java | Estado | AcciÃƒÂ³n | Slice |
|---|---|---|---|---|---|---|---|---|
| id | BIGINT AUTO_INCREMENT | NO | id | **Long | ALINEADO (8F-11B) |
| id_acta | BIGINT | NO | actaId | Long | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| fallo_id | BIGINT | NO | falloId | **String** | IDENTIDAD_INCOMPATIBLE | Cambiar a Long | 8F-11F |
| estado_apelacion | SMALLINT | NO | estadoApelacion | EstadoApelacionActa | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| fecha_presentacion | DATETIME(6) | NO | fechaPresentacion | LocalDateTime | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| canal_apelacion | SMALLINT | NO | Ã¢â‚¬â€ | Ã¢â‚¬â€ | FALTA_EN_INMEMORY | Agregar enum/SMALLINT canalApelacion | 8F-11F |
| tipo_presentacion | SMALLINT | NO | Ã¢â‚¬â€ | Ã¢â‚¬â€ | FALTA_EN_INMEMORY | Agregar enum/SMALLINT tipoPresentacion | 8F-11F |
| texto_apelacion | TEXT | YES | Ã¢â‚¬â€ | Ã¢â‚¬â€ | FALTA_EN_INMEMORY | Agregar String textoApelacion | 8F-11F |
| presentante | VARCHAR(300) | YES | presentante | String | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| fundamentos | TEXT | YES | fundamentos | String | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| observaciones | TEXT | YES | observaciones | String | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| si_activa | BOOLEAN | NO | siActiva | boolean | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| fecha_resolucion | DATETIME(6) | YES | fechaResolucion | LocalDateTime | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| fundamentos_resolucion | TEXT | YES | fundamentosResolucion | String | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| observaciones_resolucion | TEXT | YES | observacionesResolucion | String | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| documento_resolucion_id | BIGINT | YES | Ã¢â‚¬â€ | Ã¢â‚¬â€ | FALTA_EN_INMEMORY | Agregar Long documentoResolucionId | 8F-11F |
| version_row | INT | NO | Ã¢â‚¬â€ | Ã¢â‚¬â€ | FALTA_EN_INMEMORY | Agregar int versionRow | 8F-11F |
| fh_alta, id_user_alta, fh_mod, id_user_mod | DATETIME(6)/CHAR(36) | varios | Ã¢â‚¬â€ | Ã¢â‚¬â€ | ALINEADO (8F-11B) |

### Matriz: FalNotificacion / fal_notificacion

| Campo MariaDB | Tipo MariaDB | Null | Campo Java | Tipo Java | Estado | AcciÃƒÂ³n | Slice |
|---|---|---|---|---|---|---|---|---|
| id | BIGINT AUTO_INCREMENT | NO | id | **Long | ALINEADO (8F-11B) |
| id_acta | BIGINT | NO | idActa | Long | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| id_documento | BIGINT | NO | idDocumento | Long | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| tipo_docu_notificado | SMALLINT | NO | tipoDocumentoNotificado | TipoDocu | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| canal | VARCHAR(50) | NO | canal | String | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| fecha_envio | DATETIME(6) | NO | fechaEnvio | LocalDateTime | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| estado | SMALLINT | NO | estado | EstadoNotificacion | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| resultado | SMALLINT | YES | resultado | ResultadoNotificacion | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| fecha_resultado | DATETIME(6) | YES | fechaResultado | LocalDateTime | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |
| intentos | INT | NO | intentos | int | ALINEADO (simplificado) | Separar a fal_notificacion_intento en 8F-11I | 8F-11I |
| observaciones | TEXT | YES | observaciones | String | ALINEADO | Ã¢â‚¬â€ | Ã¢â‚¬â€ |

### Matrices alineadas (sin gaps de campo)

Las siguientes entidades estÃƒÂ¡n completamente alineadas; se omite el detalle de campo por campo:

| Entidad | Tabla | Verificado en |
|---|---|---|
| FalDocumentoPlantillaContenido | fal_documento_plantilla_contenido | 8F-10-R2 (ver secciÃƒÂ³n 2) |
| FalDocumentoPlantillaDefault | fal_documento_plantilla_default | 8F-10-R1 (ver secciÃƒÂ³n 2) |
| FalDocumentoRedaccion | fal_documento_redaccion | 8F-10-R2 (ver secciÃƒÂ³n 2) |
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

## 5. Gaps conocidos verificados Ã¢â‚¬â€ Fase 4

### GAP-1 CERRADO en 8F-10
Las tres tablas nuevas de 8F (fal_documento_plantilla_contenido, fal_documento_plantilla_default, fal_documento_redaccion) fueron incorporadas al modelo MariaDB en 8F-10. Las entidades Java fueron completamente alineadas en 8F-10-R2. **No hay gap.**

### GAP-2 APLICADO en 8F-10 (D1 cerrada)
origen_firmeza SMALLINT NULL incorporado en fal_acta_fallo del modelo MariaDB. **Pendiente en InMemory:** agregar si_firme, fh_firmeza, origen_firmeza (con cÃƒÂ³digo) a FalActaFallo Java Ã¢â‚¬â€ Slice 8F-11F.

### GAP-3 DECISION CERRADA D2
FalPagoVoluntario y FalPagoCondena Ã¢â€ â€™ fal_acta_obligacion_pago con tipo. **Pendiente:** implementar modelo real de pagos en 8F-11H.

### GAP-4 ACTIVO Ã¢â‚¬â€ FalActaParalizacion faltante
No existe FalActaParalizacion ni repo. D3 cerrada: implementar InMemory antes de JDBC. **Slice: 8F-11G.**

### GAP-5 ACTIVO Ã¢â‚¬â€ FalActaArchivo faltante
No existe FalActaArchivo ni repo. **Slice: 8F-11G.**

### GAP-6 ACTIVO Ã¢â‚¬â€ FalPersona / FalPersonaDomicilio faltantes
Datos de persona embebidos en FalActa. D6 cerrada: implementar InMemory antes de JDBC. **Slice: 8F-11C.**

### GAP-7 ACTIVO Ã¢â‚¬â€ ValorizaciÃƒÂ³n y artÃƒÂ­culos faltantes
No existen: FalTarifarioUnidadFaltas, FalActaArticuloInfringido, FalActaValorizacion, FalActaValorizacionItem. FalActaFallo.montoCondena es placeholder. **Slice: 8F-11D.**

### GAP-8 ELIMINADO (8F-11B)
Decision de diseno (8F-11B): `firma_storage_key`, `firma_hash`, `fh_firma_registrada` eliminados definitivamente del modelo MariaDB y del modelo Java. La firma maestra del inspector no se registra.

### GAP-9 ACTIVO Ã¢â‚¬â€ FalActaFallo campos faltantes
Faltan: valorizacion_id, resultado_fallo, fh_firma, fh_vto_apelacion, fallo_reemplazado_id, si_firme, fh_firmeza, origen_firmeza, version_row, auditorÃƒÂ­a. Ver matriz secciÃƒÂ³n 4. **Slice: 8F-11F.**

### GAP-10 ACTIVO Ã¢â‚¬â€ FalActaApelacion campos faltantes
Faltan: canal_apelacion, tipo_presentacion, texto_apelacion, documento_resolucion_id, version_row, auditorÃƒÂ­a; falloId debe ser Long. Ver matriz secciÃƒÂ³n 4. **Slice: 8F-11F.**

### GAP-11 DIFERIBLE Ã¢â‚¬â€ FalObservacion faltante
PAGAPR usa evento.descripcion transitoriamente. **Slice: 8F-11G.**

### GAP-12 DIFERIBLE Ã¢â‚¬â€ fal_acta_documento pivot faltante
FalDocumento.idActa cubre caso simple. **Slice: 8F-11J.**

### GAP-13 ACTIVO Ã¢â‚¬â€ Enums sin cÃƒÂ³digo numÃƒÂ©rico
- TipoActa: necesita TRANSITO=1, CONTRAVENCION=2, SUSTANCIAS_ALIMENTICIAS=3, COMERCIO=4 (D8 cerrada). **Slice: 8F-11B.**
- OrigenFirmezaCondena: necesita 1=VENCIMIENTO_PLAZO_APELACION, 2=APELACION_RECHAZADA (D1 cerrada). **Slice: 8F-11F.**

### GAP-14 ACTIVO Ã¢â‚¬â€ Identidades String en 5 entidades
FalActaFallo.id, FalActaApelacion.id, FalNotificacion.id, FalBloqueanteMaterial.id, FalGestionExterna.id Ã¢â‚¬â€ todos deben migrar a Long. **Slice: 8F-11B.**

### GAP-15 ACTIVO Ã¢â‚¬â€ AuditorÃƒÂ­a ausente en entidades clave
FalActa, FalActaFallo, FalActaApelacion, FalGestionExterna carecen de fh_alta, id_user_alta, fh_mod, id_user_mod. **Slice: 8F-11B.**

### GAP-16 ACTIVO Ã¢â‚¬â€ SatÃƒÂ©lites de acta faltantes
No existen: FalActaTransito, FalActaAlcoholemia, FalActaVehiculo, FalActaContravenciÃƒÂ³n, FalActaSustanciasAlimenticias, FalActaMedidaPreventiva. **Slice: 8F-11E.**

### GAP-17 DIFERIBLE Ã¢â‚¬â€ Notificaciones ciclo completo faltante
FalNotificacionIntento, FalNotificacionAcuse, FalLoteCorreo no implementados. **Slice: 8F-11I.**

### GAP-18 DIFERIBLE Ã¢â‚¬â€ QR portal
fal_acta_qr_acceso no implementado. Baja prioridad. **Slice: 8F-11K.**

---

## 6. Decisiones cerradas P1 y P2 - Fase 5

Ambas decisiones estÃƒÂ¡n cerradas. No reabrir.

---

### DecisiÃƒÂ³n P1 Ã¢â‚¬â€ ResultadoFinalActa: CERRADA

**DecisiÃƒÂ³n aprobada:** columna MariaDB `resultado_final SMALLINT NOT NULL`; enum Java con cÃƒÂ³digo explÃƒÂ­cito.

**No usar:** ordinal Java, `name()`, VARCHAR, CHAR(6).

**Valores definitivos:**

| CÃƒÂ³digo | Valor |
|---:|---|
| 0 | `SIN_RESULTADO_FINAL` |
| 1 | `PAGO_VOLUNTARIO_PAGADO` |
| 2 | `ABSUELTO` |
| 3 | `CONDENA_FIRME` |
| 4 | `ANULADO` |

**SemÃƒÂ¡ntica:**

- `ResultadoFinalActa` expresa la forma definitiva en que fue resuelta el acta, no el ÃƒÂºltimo estado transitorio.
- Una solicitud de pago voluntario pendiente conserva `SIN_RESULTADO_FINAL`.
- Solo el pago voluntario efectivamente acreditado produce `PAGO_VOLUNTARIO_PAGADO`.
- El pago posterior de una condena no cambia `CONDENA_FIRME`.
- El estado del pago vive en la obligaciÃƒÂ³n y sus movimientos.
- GestiÃƒÂ³n externa no es un resultado final.
- `PRESCRIPTO` no existe en el dominio aprobado.
- Archivo no equivale automÃƒÂ¡ticamente a resultado final.

**Valores eliminados del enum (no deben existir en la implementaciÃƒÂ³n Java final):**
- `PAGO_VOLUNTARIO_CONFIRMADO` Ã¢â‚¬â€ eliminado; reemplazado por `PAGO_VOLUNTARIO_PAGADO`
- `CONDENA_FIRME_PAGADA` Ã¢â‚¬â€ eliminado; el pago no cambia `ResultadoFinalActa`
- `FALLO_CONDENATORIO_PAGADO` Ã¢â‚¬â€ eliminado
- `FALLO_CONDENATORIO_GESTION_EXTERNA` Ã¢â‚¬â€ eliminado; gestiÃƒÂ³n externa no es resultado final
- `PRESCRIPTO` Ã¢â‚¬â€ eliminado; no existe en el dominio aprobado
**ImplementaciÃƒÂ³n Java:** Slice 8F-11B.

**Impacto en secciÃƒÂ³n 11.2:** La interpretaciÃƒÂ³n anterior de CHAR(6) queda reemplazada por esta decisiÃƒÂ³n. Ver correcciÃƒÂ³n en 11.2.

---

### DecisiÃƒÂ³n P2 Ã¢â‚¬â€ FalActaFirmezaCondena: CERRADA (OpciÃƒÂ³n B)

**DecisiÃƒÂ³n aprobada:** refactor en 8F-11F.

**Fuente de verdad jurÃƒÂ­dica:** la firmeza pertenece al fallo concreto y se persiste en `fal_acta_fallo`:
- `si_firme`
- `fh_firmeza`
- `origen_firmeza`

**Estado operativo del acta:** la situaciÃƒÂ³n de firmeza vigente debe proyectarse en `FalActaSnapshot`.

**En 8F-11F:**
- Completar `FalActaFallo` con siFirme, fhFirmeza, origenFirmeza.
- Refactorizar servicios para escribir la firmeza sobre el fallo.
- Eliminar `FalActaFirmezaCondena` como entidad persistible separada.
- Eliminar `FirmezaCondenaRepository`.
- Mantener proyecciÃƒÂ³n en snapshot.
- No duplicar segunda fuente de verdad en `FalActa`.

---

> No hay otras decisiones abiertas. D1 a D9 estÃƒÂ¡n cerradas y no deben reabrirse. P1 y P2 cerradas en 8F-11A-R1.
## 7. Plan finito de implementaciÃƒÂ³n Ã¢â‚¬â€ Fase 6

### 8F-11B Ã¢â‚¬â€ Identidades, enums, auditorÃƒÂ­a, versionRow y campos core
**Objetivo:** Eliminar todos los ids String, agregar cÃƒÂ³digos a enums core, agregar auditorÃƒÂ­a, corregir tipos de FK.

**Entidades afectadas:**
- FalActaFallo.id: String Ã¢â€ â€™ Long
- FalActaApelacion.id: String Ã¢â€ â€™ Long
- FalNotificacion.id: String Ã¢â€ â€™ Long (D7)
- FalBloqueanteMaterial.id: String Ã¢â€ â€™ Long
- FalGestionExterna.id: String Ã¢â€ â€™ Long
- FalActa.tipoActa: String Ã¢â€ â€™ enum TipoActa con cÃƒÂ³digo SMALLINT (D8) Ã¢â‚¬â€ valores 1Ã¢â‚¬â€œ4
- `ResultadoFinalActa`: SMALLINT 0Ã¢â‚¬â€œ4 definitivo (P1 CERRADA)
- FalActa.idInspector, idDependencia: String Ã¢â€ â€™ Long
- Campos de auditorÃƒÂ­a en FalActa, FalActaFallo, FalActaApelacion, FalGestionExterna
- `FalInspectorVersion`: campos de firma eliminados del modelo (GAP-8 ELIMINADO)
- `versionRow INT` en FalActa, FalDocumento, FalNotificacion, FalGestionExterna y otras entidades existentes que lo requieran

**Repositorios:** todos los mencionados arriba Ã¢â‚¬â€ actualizar AtomicLong counters
**Tests:** todos los que usen ids de fallo/apelaciÃƒÂ³n/notificaciÃƒÂ³n/bloqueante/gestiÃƒÂ³n como String
**DocumentaciÃƒÂ³n:** actualizar matriz 110 con ALINEADO para los campos corregidos
**Criterio de cierre:** Build >= 1509 tests, 0 failures; 0 entidades persistibles con id String (excepto uuid_tecnico en FalActa)

---

### 8F-11C Ã¢â‚¬â€ FalPersona y FalPersonaDomicilio
**Objetivo:** Implementar entidades de persona separadas de FalActa.

**Entidades a crear:** FalPersona (con tipo_persona, Id_Suj, Id_Bie), FalPersonaDomicilio (con modo_domicilio, refs geo)
**Repositorios a crear:** PersonaRepository + InMemory, PersonaDomicilioRepository + InMemory
**Refactor en FalActa:** agregar idPersona Long nullable; infractorNombre/infractorDocumento pueden mantenerse como campos de captura transitoria
**Dependencias:** 8F-11B (FalActa.idPersona como Long)
**Criterio de cierre:** FalPersona y FalPersonaDomicilio existen con repos; build verde

---

### 8F-11D Ã¢â‚¬â€ Normativa, tarifario, medidas y valorizaciÃƒÂ³n
**Objetivo:** Implementar el modelo de cÃƒÂ¡lculo de montos.

**Entidades a crear:** FalTarifarioUnidadFaltas, FalMedidaPreventiva, FalArticuloMedidaPreventiva, FalActaArticuloInfringido, FalActaValorizacion, FalActaValorizacionItem
**Repositorios:** 6 repos + InMemory
**Impacto:** FalActaFallo.montoCondena vinculado a FalActaValorizacion.id
**Criterio de cierre:** ValorizaciÃƒÂ³n calculada desde artÃƒÂ­culos; build verde

---

### 8F-11E Ã¢â‚¬â€ SatÃƒÂ©lites de acta y catÃƒÂ¡logos relacionados
**Objetivo:** Implementar datos especÃƒÂ­ficos de cada tipo de acta y catÃƒÂ¡logos de soporte.

**Entidades a crear Ã¢â‚¬â€ satÃƒÂ©lites:**
- FalActaTransito, FalActaTransitoAlcoholemia, FalActaVehiculo
- FalActaContravencion, FalActaSustanciasAlimenticias, FalActaMedidaPreventiva

**Entidades a crear Ã¢â‚¬â€ catÃƒÂ¡logos:**
- FalVehiculoMarca (id, descripcion, si_activo; CRUD; baja lÃƒÂ³gica)
- FalVehiculoModelo (id, marca_vehiculo_id, descripcion, si_activo; modelo pertenece a marca; sin descripciÃƒÂ³n duplicada por marca)
- FalRubroVersion (conservar versionado: row_hash, previous_row_hash, source_operation, close_operation, si_version_actual, valid_from, valid_to, synced_at; consultar por Id_Rub; recuperar versiÃƒÂ³n vigente)

**Repositorios:** 6 + 3 catÃƒÂ¡logos + InMemory
**Guardrail:** tipo_acta determina quÃƒÂ© satÃƒÂ©lite existe
**Dependencias:** 8F-11B (tipoActa correcto)
**Criterio de cierre:** Cada tipo tiene satÃƒÂ©lite; guardrail activo; catÃƒÂ¡logos con repos InMemory; build verde

---

### 8F-11F Ã¢â‚¬â€ Fallo, firmeza y apelaciÃƒÂ³n
**Objetivo:** Completar FalActaFallo y FalActaApelacion; refactorizar firmeza (P2 CERRADA OpciÃƒÂ³n B).

**Cambios en FalActaFallo:** id ya Long (8F-11B); agregar valorizacionId, resultadoFallo, fhFirma, fhVtoApelacion, falloReemplazadoId, siFirme, fhFirmeza, origenFirmeza, versionRow, auditorÃƒÂ­a
**Cambios en FalActaApelacion:** id ya Long (8F-11B); falloId Ã¢â€ â€™ Long; agregar canalApelacion, tipoPresentacion, textoApelacion, documentoResolucionId, versionRow, auditorÃƒÂ­a
**Nuevas entidades:** FalActaApelacionDocumento
**Firmeza (P2 CERRADA):** eliminar FalActaFirmezaCondena y FirmezaCondenaRepository; agregar siFirme, fhFirmeza, origenFirmeza (con cÃƒÂ³digo) a FalActaFallo; proyecciÃƒÂ³n en snapshot
**Dependencias:** 8F-11B (ids), 8F-11D (valorizacion_id)
**Criterio de cierre:** FalActaFallo con todos los campos del modelo; apelaciÃƒÂ³n completa; firmeza resuelta; build verde

---

### 8F-11G Ã¢â‚¬â€ ParalizaciÃƒÂ³n, archivo, motivos, observaciones y gestiÃƒÂ³n externa
**Objetivo:** Implementar ciclos de paralizaciÃƒÂ³n y archivo; crear observaciones; completar gestiÃƒÂ³n externa.

**Entidades a crear:** FalActaParalizacion, FalActaArchivo, FalMotivoArchivo (id, descripcion, si_activo; FK desde fal_acta_archivo; motivos inactivos vÃƒÂ¡lidos histÃƒÂ³ricamente), FalObservacion
**Repos:** 4 repos + InMemory
**Refactor:** PAGAPR escribe en FalObservacion en lugar de evento.descripcion
**Dependencias:** 8F-11B (ids Long en gestiÃƒÂ³n externa ya corregidos)
**Criterio de cierre:** Ciclo paralizaciÃƒÂ³n/cierre funciona; FalActaArchivo registra archivo; FalObservacion captura PAGAPR; FalMotivoArchivo con CRUD; build verde

---

### 8F-11H Ã¢â‚¬â€ Pagos reales unificados (D2 CERRADA)
**Objetivo:** Implementar modelo real de pagos unificado.

**Entidades a crear:** FalActaObligacionPago, FalActaFormaPago, FalActaPlanPagoRef, FalActaPagoMovimiento
**Enum aprobado:** OrigenObligacionPago (PAGO_VOLUNTARIO=1, CONDENA=2)
**Repos:** 4 repos + InMemory; PagoVoluntarioRepository y PagoCondenaRepository eliminados o wrappers transitorios
**MigraciÃƒÂ³n:** FalPagoVoluntario y FalPagoCondena ya no son entidades persistibles separadas
**Dependencias:** 8F-11D (valorizaciÃƒÂ³n)
**Criterio de cierre:** FalActaObligacionPago unifica ambos tipos de pago; ciclos de pago funcionan; build verde

---

### 8F-11I Ã¢â‚¬â€ Notificaciones completas
**Objetivo:** Ciclo completo de notificaciÃƒÂ³n con intentos y acuses.

**Entidades a crear:** FalNotificacionIntento, FalNotificacionAcuse, FalLoteCorreo
**Repos:** 3 repos + InMemory
**Dependencias:** 8F-11B (FalNotificacion.id Long)
**Criterio de cierre:** Ciclo con intentos y acuse funciona; build verde

---

### 8F-11J Ã¢â‚¬â€ Documentos y relaciones restantes
**Objetivo:** Completar relaciones documentales.

**Pendientes:** FalActaDocumento (pivot); FalDocumento campos faltantes (plantilla_id); no implementar PDF/storage real
**Dependencias:** 8F-11B
**Criterio de cierre:** FalDocumento completo; fal_acta_documento resuelta; build verde

---

### 8F-11K Ã¢â‚¬â€ Portal QR y auditorÃƒÂ­a final de paridad cero
**Objetivo:** Implementar QrAcceso; verificar 62 tablas a 0 gaps.

**Entidades a crear:** FalActaQrAcceso
**Actividades:**
- Recorrer la matriz 110 y confirmar: todos los estados son ALINEADO, SOLO_DEMO_TEST, SOLO_INFRAESTRUCTURA o NO_PERSISTIBLE
- RevisiÃƒÂ³n final de las 62 tablas Ã¢â‚¬â€ 0 gaps no justificados
- Actualizar 110 con estados finales
- Verificar guardrails: Test-Path docs\spec-as-source desde raÃƒÂ­z = FALSE; sin @Entity; sin JpaRepository; sin SQL
- Build final completo

**Criterio de cierre (paridad total):**
- 0 FALTA_EN_INMEMORY sin clasificaciÃƒÂ³n
- 0 IDENTIDAD_INCOMPATIBLE
- 0 TIPO_INCOMPATIBLE sin justificaciÃƒÂ³n
- Matriz 110 al 100% ALINEADO o clasificaciÃƒÂ³n justificada
- Build >= 1509 tests, 0 failures

---
## 8. Tabla de decisiones cerradas D1Ã¢â‚¬â€œD9 Ã¢â‚¬â€ referencia rÃƒÂ¡pida

No volver a mostrar estas decisiones como pendientes.

| # | DecisiÃƒÂ³n | DescripciÃƒÂ³n | Estado |
|---|---|---|---|
| D1 | Firmeza | FalActaFirmezaCondena sin tabla; campos en fal_acta_fallo (fh_firmeza, si_firme, origen_firmeza SMALLINT NULL) | CERRADA |
| D2 | Pagos | FalPagoVoluntario + FalPagoCondena Ã¢â€ â€™ fal_acta_obligacion_pago con tipo_obligacion | CERRADA |
| D3 | ParalizaciÃƒÂ³n | FalActaParalizacion: implementar InMemory antes de JDBC | CERRADA |
| D4 | JSON | Campos JSON documentales Ã¢â€ â€™ JSON nativo MariaDB 12.3.2 | CERRADA |
| D5 | Fundamentos fallo | FalActaFallo.fundamentos Ã¢â€ â€™ TEXT NULL directamente en fal_acta_fallo | CERRADA |
| D6 | Persona | FalPersona + FalPersonaDomicilio: implementar InMemory antes de JDBC | CERRADA |
| D7 | NotificaciÃƒÂ³n PK | FalNotificacion.id Ã¢â€ â€™ BIGINT AUTO_INCREMENT sin UUID | CERRADA |
| D8 | TipoActa | FalActa.tipoActa Ã¢â€ â€™ enum TipoActa con cÃƒÂ³digo SMALLINT | CERRADA |
| D9 | Prioridad default | fal_documento_plantilla_default.prioridad Ã¢â€ â€™ SMALLINT | CERRADA |
| GAP-1 | 3 tablas 8F | fal_documento_plantilla_contenido/default y fal_documento_redaccion en modelo MariaDB + Java alineado 8F-10-R2 | CERRADA |

---

## 9. Checklist de validaciÃƒÂ³n Ã¢â‚¬â€ 8F-11A (original) + 8F-11A-R1 (correcciÃƒÂ³n)

| ÃƒÂtem | Estado |
|------|--------|
| Build ejecutado: 1509 tests, 0 failures, BUILD SUCCESS | VERIFICADO |
| Test-Path docs\spec-as-source desde raÃƒÂ­z = FALSE (guardrail) | VERIFICADO |
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
| D1-D9 marcadas CERRADA en secciÃƒÂ³n 8 | COMPLETADO |
| 0 TODOs vagos sin slice asignado | COMPLETADO |
| **P1 CERRADA: ResultadoFinalActa SMALLINT 0Ã¢â‚¬â€œ4** | COMPLETADO (8F-11A-R1) |
| **P2 CERRADA: firmeza en fallo, refactor en 8F-11F** | COMPLETADO (8F-11A-R1) |
| **29 tablas FALTA_EN_INMEMORY enumeradas explÃƒÂ­citamente** | COMPLETADO (8F-11A-R1) |
| Roadmap finito 8F-11B a 8F-11K con 9 slices | COMPLETADO |
| SecciÃƒÂ³n 11.2 corregida (sin CHAR(6) incorrecto) | COMPLETADO (8F-11A-R1) |

---
## 10. Entrega final del slice Ã¢â‚¬â€ 8F-11A (original) + 8F-11A-R1 (correcciÃƒÂ³n documental)

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
| **SEMANTICA_INCOMPATIBLE** | 2 (FalPagoVoluntarioÃ¢â€ â€™D2, FalPagoCondenaÃ¢â€ â€™D2) + FalActaFirmezaCondenaÃ¢â€ â€™P2 CERRADA |
| **ENUM_SIN_CODIGO** | 0 (2 resueltos en 8F-11B) |
| **RELACION_INCOMPLETA** | 3 (fal_acta_documento, fal_acta_apelacion_documento, inspector firma FK) |
| **SOLO_DEMO_TEST** | 21 clases |
| **SOLO_INFRAESTRUCTURA** | 2 categorÃƒÂ­as |
| **NO_PERSISTIBLE** | 25+ (servicios, DTOs, enums puro Java) |
| **P1 CERRADA: ResultadoFinalActa** | SMALLINT NOT NULL; 5 valores (0=SIN_RESULTADO_FINAL Ã¢â‚¬Â¦ 4=ANULADO) |
| **P2 CERRADA: Firmeza** | siFirme/fhFirmeza/origenFirmeza en fal_acta_fallo; refactor en 8F-11F |
| **Modelo unificado de pagos** | FalActaObligacionPago + OrigenObligacionPago; implementar en 8F-11H |
| **4 catÃƒÂ¡logos confirmados** | VehiculoMarca, VehiculoModelo, RubroVersion (versionado), MotivoArchivo |
| **Roadmap de cierre** | 8F-11B a 8F-11K Ã¢â‚¬â€ 9 slices finitos |
| **Proximo slice recomendado** | **8F-11C** - FalPersona y FalPersonaDomicilio |

---

> **Criterio de cierre verificado (8F-11A-R1):** existen las 62 tablas inventariadas; las 29 tablas FALTA_EN_INMEMORY estÃƒÂ¡n enumeradas explÃƒÂ­citamente; P1 y P2 estÃƒÂ¡n cerradas; el roadmap cubre el 100% de las tablas persistibles (8F-11B a 8F-11K); el modelo MariaDB refleja `resultado_final SMALLINT NOT NULL`; build y guardrails verdes.

---
## 11. Correcciones post-auditorÃƒÂ­a Ã¢â‚¬â€ 8F-11A y 8F-11A-R1

### 11.1 Recuento de tablas corregido

El modelo MariaDB final tiene **62 tablas** en el dominio Faltas, no 55.

Las 7 tablas adicionales identificadas en la auditorÃƒÂ­a:

| Tabla adicional | ÃƒÂrea | Estado InMemory |
|---|---|---|
| `fal_vehiculo_marca` | CatÃƒÂ¡logo de vehÃƒÂ­culos Ã¢â‚¬â€ trÃƒÂ¡nsito | **ALINEADO** (8F-11E) |
| `fal_vehiculo_modelo` | CatÃƒÂ¡logo de vehÃƒÂ­culos Ã¢â‚¬â€ trÃƒÂ¡nsito | **ALINEADO** (8F-11E) |
| `fal_rubro_version` | CatÃƒÂ¡logo rubros versionado Ã¢â‚¬â€ contravenciÃƒÂ³n | **ALINEADO** (8F-11E) |
| `fal_motivo_archivo` | CatÃƒÂ¡logo motivo de archivo Ã¢â‚¬â€ administrable | FALTA_EN_INMEMORY Ã¢â‚¬â€ Slice 8F-11G |
| `stor_backend` | Storage Ã¢â‚¬â€ infraestructura tÃƒÂ©cnica | NO_PERSISTIBLE desde dominio Faltas |
| `stor_politica` | Storage Ã¢â‚¬â€ infraestructura tÃƒÂ©cnica | NO_PERSISTIBLE desde dominio Faltas |
| `stor_objeto` | Storage Ã¢â‚¬â€ referencia binarios | NO_PERSISTIBLE (storage_key en entidades de dominio) |

**Nota:** `fal_rubro_version` es una tabla de dominio Faltas con sincronizaciÃƒÂ³n externa (Informix). No es infraestructura tÃƒÂ©cnica. Requiere entidad `FalRubroVersion` con InMemory, respetando el modelo de versionado (`row_hash`, `previous_row_hash`, `source_operation`, `close_operation`, `si_version_actual`, `valid_from`, `valid_to`, `synced_at`).

Tablas geogrÃƒÂ¡ficas `geo_*` (geo_ign_*, geo_indec_*, geo_bahra_*, geo_malv_*): externas a Faltas, no administradas por este mÃƒÂ³dulo. Clasificadas NO_PERSISTIBLE a efectos de paridad Faltas.

---

### 11.2 DecisiÃƒÂ³n P1 Ã¢â‚¬â€ CERRADA (correcciÃƒÂ³n 8F-11A-R1)

**Ã¢Å¡Â  CorrecciÃƒÂ³n:** La secciÃƒÂ³n 11.2 original decÃƒÂ­a que `resultado_final` era `CHAR(6)` (incorrecto). Esa interpretaciÃƒÂ³n era incorrecta. La decisiÃƒÂ³n aprobada de dominio reemplaza esa definiciÃƒÂ³n.

**DecisiÃƒÂ³n definitiva aprobada:**

`resultado_final SMALLINT NOT NULL` en `fal_acta`.

**Por quÃƒÂ© no CHAR(6):** Aunque otros campos de estado del acta usan CHAR(4/6) como cÃƒÂ³digo corto de texto (`bloque_actual CHAR(4)`, `est_proc_act CHAR(4)`, `sit_adm_act CHAR(4)`), `resultado_final` es diferente en naturaleza: es el resultado final de resoluciÃƒÂ³n del expediente, con un dominio cerrado de 5 valores enteros, y se persiste con un cÃƒÂ³digo numÃƒÂ©rico explÃƒÂ­cito para garantizar estabilidad, eficiencia de ÃƒÂ­ndice y compatibilidad con la capa JDBC.

**Valores definitivos:**

| CÃƒÂ³digo SMALLINT | Constante Java |
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
- GestiÃƒÂ³n externa no es un resultado final.
- `PRESCRIPTO` no existe.
- Archivo no equivale automÃƒÂ¡ticamente a resultado final.

**Enum eliminado:** `PAGO_VOLUNTARIO_CONFIRMADO`, `CONDENA_FIRME_PAGADA`, `FALLO_CONDENATORIO_PAGADO`, `FALLO_CONDENATORIO_GESTION_EXTERNA`, `PRESCRIPTO` no deben existir en el enum final.

**ImplementaciÃƒÂ³n Java:** Slice 8F-11B.

---

### 11.3 Correcciones de tipo en FalActa

| Campo Java | Tipo Java actual | Tipo MariaDB real | CorrecciÃƒÂ³n |
|---|---|---|---|
| `tipoActa` | String | `tipo_acta SMALLINT` | TIPO_INCOMPATIBLE Ã¢â‚¬â€ agregar TipoActa enum con short 1Ã¢â‚¬â€œ4 |
| `bloqueActual` | BloqueActual (enum) | `bloque_actual CHAR(4)` | ALINEADO si el enum usa String CAPT/ENRI/etc. |
| `estadoProcesal` | EstadoProcesalActa | `est_proc_act CHAR(4)` | verificar cÃƒÂ³digos en 8F-11B |
| `situacionAdministrativa` | SituacionAdministrativaActa | `sit_adm_act CHAR(4)` | verificar cÃƒÂ³digos en 8F-11B |
| `resultadoFinal` | ResultadoFinalActa | `resultado_final SMALLINT NOT NULL` | TIPO_INCOMPATIBLE Ã¢â‚¬â€ P1 CERRADA; implementar short 0Ã¢â‚¬â€œ4 en 8F-11B |
| `resultadoFirmaInfractor` | ResultadoFirmaInfractor | `resultado_firma_infractor SMALLINT` | ALINEADO (short 1Ã¢â‚¬â€œ5 ya en enum) |

---

### 11.4 version_row en FalActa y aggregates

El modelo exige `version_row INT NOT NULL DEFAULT 0` en al menos:
`fal_acta`, `fal_documento`, `fal_notificacion`, `fal_acta_valorizacion`, `fal_acta_fallo`,
`fal_acta_apelacion`, `fal_acta_paralizacion`, `fal_acta_archivo`, `fal_acta_gestion_externa`,
`fal_acta_obligacion_pago`, `fal_acta_forma_pago`, `fal_acta_plan_pago_ref`, `num_talonario`.

**InMemory actual:** NumTalonario, FalActa, FalDocumento, FalNotificacion, FalGestionExterna, FalActaFallo, FalActaApelacion tienen `versionRow` implementado (8F-11B).

**Entidades existentes con versionRow (8F-11B CERRADO):** FalActa, FalDocumento, FalNotificacion, FalGestionExterna, FalActaFallo, FalActaApelacion.

**Entidades futuras que nacerÃƒÂ¡n con versionRow** (en cada slice de creaciÃƒÂ³n): FalActaValorizacion (8F-11D), FalActaParalizacion (8F-11G), FalActaArchivo (8F-11G), FalActaObligacionPago (8F-11H), FalActaFormaPago (8F-11H), FalActaPlanPagoRef (8F-11H).

---

### 11.5 FalPersona mÃƒÂ¡s compleja de lo documentado

`fal_persona` incluye:
- `tipo_persona` SMALLINT (FISICA=1, JURIDICA=2)
- `Id_Suj`, `Id_Bie` BIGINT Ã¢â‚¬â€ integraciÃƒÂ³n Ingresos
- `SujBieEstado` SMALLINT (sin cuenta / activa / inactiva)
- `fh_ult_mod`, `id_user_ult_mod` (auditorÃƒÂ­a completa)

`fal_persona_domicilio` es mÃƒÂ¡s compleja:
- `modo_domicilio` (MALVINAS_LOCAL / EXTERNO)
- refs geo versionadas Malvinas
- `origen_domicilio`, mÃƒÂºltiples FKs geo

Impacto en 8F-11C: `FalPersona` necesita campos de Ingresos y tipo_persona; `FalPersonaDomicilio` necesita modo_domicilio y refs geo simplificadas para InMemory.

---

### 11.6 Enums que ya tienen cÃƒÂ³digo short (confirmado en auditorÃƒÂ­a 8F-11A)

Con `short codigo()` implementado:
`AccionDocumental` (1Ã¢â‚¬â€œ11), `AlcanceTalonario` (1Ã¢â‚¬â€œ3), `ClaseNumeracion` (1Ã¢â‚¬â€œ2), `EstadoAsignacionTalonario` (1Ã¢â‚¬â€œ4), `EstadoDocu` (1Ã¢â‚¬â€œ7), `EstadoFirma` (1Ã¢â‚¬â€œ6), `EstadoFirmaReq` (1Ã¢â‚¬â€œ5), `EstadoNumeroTalonario` (1Ã¢â‚¬â€œ5), `EstadoRedaccionDocumento` (1Ã¢â‚¬â€œ3), `MomentoNumeracionDocu` (0Ã¢â‚¬â€œ4), `MotivoAnulacionTalonario` (1Ã¢â‚¬â€œ5), `ResultadoFirmaInfractor` (1Ã¢â‚¬â€œ5), `TipoDocu` (1Ã¢â‚¬â€œ12), `TipoEvidenciaActa` (6), `TipoFirma` (1Ã¢â‚¬â€œ4), `TipoFirmaReq` (0Ã¢â‚¬â€œ5), `TipoTalonario` (1Ã¢â‚¬â€œ2).

Con valores String directos (CHAR(4/6) en MariaDB Ã¢â‚¬â€ patrÃƒÂ³n correcto):
`BloqueActual`, `TipoEventoActa`, `EstadoProcesalActa`, `SituacionAdministrativaActa`.

Sin cÃƒÂ³digo confirmado (verificar en 8F-11B):
`EstadoApelacionActa`, `EstadoBloqueanteMaterial`, `EstadoFalloActa`, `EstadoGestionExterna`, `EstadoNotificacion`, `EstadoPagoCondena`, `EstadoPagoVoluntario`, `ModoReingresoGestionExterna`, `OrigenBloqueanteMaterial`, `ResultadoGestionExterna`, `ResultadoNotificacion`, `TipoActa` (pendiente SMALLINT 1Ã¢â‚¬â€œ4), `TipoGestionExterna`, `TipoUnidad`.

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

## ActualizaciÃƒÂ³n 8F-11D (2026-07-05)

### Tablas alineadas (6)
1. fal_tarifario_unidad_faltas Ã¢â€ â€™ FalTarifarioUnidadFaltas
2. fal_medida_preventiva Ã¢â€ â€™ FalMedidaPreventiva
3. fal_articulo_medida_preventiva Ã¢â€ â€™ FalArticuloMedidaPreventiva (PK compuesta via ArticuloMedidaPreventivaId)
4. fal_acta_articulo_infringido Ã¢â€ â€™ FalActaArticuloInfringido
5. fal_acta_valorizacion Ã¢â€ â€™ FalActaValorizacion (versionRow, optimistic locking)
6. fal_acta_valorizacion_item Ã¢â€ â€™ FalActaValorizacionItem

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
- **27 Ã¢â€ â€™ 21 tablas FALTA_EN_INMEMORY** (29 originales - 2 de 8F-11C - 6 de 8F-11D = 21)
- Tests: 1660 Ã¢â€ â€™ **1755** (8F-11D) Ã¢â€ â€™ **1785** (8F-11D-R1: +30 tests en ValorizacionInvariantesR1Test)
- PrÃƒÂ³ximo slice: **8F-11E**

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