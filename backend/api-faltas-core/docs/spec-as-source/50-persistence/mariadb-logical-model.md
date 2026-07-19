# Modelo lógico MariaDB — inventario canónico de tablas (entrada a DDL/JDBC)

> **Estado documental:** PRE_DDL_PLAN
> **Autoridad DDL:** SUPPORTING
> Esta matriz no define reglas funcionales de dominio. Ante contradicción con la
> spec normativa (`00-governance/`, `10-domain/`, `10-domain/states-events-catalogs.md`,
> `20-application/command-contracts.md`, `30-projections/snapshot-bandejas-acciones.md`,
> `20-application/fallo-command-contracts.md`), esa spec normativa prevalece.
> Este documento no reemplaza a `inmemory-mariadb-deltas.md` ni a
> `ddl-decisions.md`; son complementarios: este documento es el inventario
> lógico por agregado (qué tablas/campos existen); `inmemory-mariadb-deltas.md`
> describe los deltas físicos transversales (identidad, unicidad, concurrencia,
> etc.); `ddl-decisions.md` es el registro único de decisiones físicas
> cerradas (`DECISION_DDL-*`). Ninguno de los tres es fuente única
> de verdad ni sustituye a los otros. No contiene SQL/DDL definitivo ni nombres
> físicos aprobados más allá de los ya usados en la spec vigente. La historia
> de cómo se llegó a este estado (fases, diarios de slice, modelos MariaDB
> preliminares de 2026-06-23 y anteriores) permanece en Git.

## 0. Rol de este documento

- No define reglas funcionales; las reglas de dominio viven en `00-governance/`,
  `10-domain/`, `20-application/` y `30-projections/`.
- No reemplaza a `inmemory-mariadb-deltas.md` ni a `ddl-decisions.md`; es complementario a ambos.
- No es la fuente única de verdad del proyecto; los documentos normativos
  prevalecen ante cualquier contradicción.
- Su único propósito es servir de entrada compacta y verificable al diseño de
  DDL/JDBC: qué agregados existen, con qué identidad, qué decisiones físicas
  cerradas condicionan cada tabla (ver `ddl-decisions.md`).
- **Reemplaza y reconcilia** el modelo físico preliminar descrito en los
  documentos históricos `MODELO_MARIADB_FALTAS_FINAL_PRODUCTIVO_COMPLETO_2026-06-23_CORREGIDO.md`
  y `DELTA_MODELO_MARIADB_DESDE_IMPLEMENTACION_IN_MEMORY.md` (eliminados de
  este árbol; historia en Git). Ninguna tabla, catálogo o regla de esos
  documentos se reintroduce aquí si contradice el código Java vigente o la
  spec normativa. En particular: no existe `fal_acta_pago_resolucion` (ver
  sección "Pagos" más abajo) y no existe seguimiento de cuotas individuales
  del plan de pago ni conciliación histórica separada (ver `fal_acta_plan_pago_ref`
  y `fal_acta_economia_proyeccion` más abajo); ambas conclusiones están
  verificadas contra el código Java vigente, no contra el diseño preliminar.

## 1. Estado de readiness

| Aspecto | Estado |
|---|---|
| Contrato funcional (dominio, comandos, eventos, estados) | CERRADO |
| Modelo conceptual (agregados, entidades Java, puertos) | LISTO_PARA_DDL |
| Decisiones físicas pendientes | **0** — todas las `DECISION_DDL-*` cerradas en el slice `SPEC-AS-SOURCE-CLEAN-ROOM-Y-DDL-CLOSURE-001` (ver `ddl-decisions.md`) |

No se reporta un resumen numérico agregado de "paridad" derivado manualmente:
cada decisión física relevante está identificada individualmente en
`ddl-decisions.md` con su propio criterio de cierre.

## 2. Inventario canónico de tablas MariaDB objetivo (modelo lógico)

Este inventario describe el modelo lógico de tablas que la spec de dominio ya
supone (nombres, campos, FKs conceptuales). No es DDL ejecutable.

**Taxonomía de estados de reconciliación R3:**
- `RECONCILIADA_R3` — reconciliación profunda completada en R3: Java ↔ histórico 2026-06-23 ↔ spec vigente, campo por campo; deltas documentados; todas las `DECISION_DDL-*` emitidas y cerradas.
- `BASELINE_PRESERVADA_CON_DELTA` — tabla presente y verificada contra el código Java vigente; fuente: `MODELO_MARIADB_FALTAS_FINAL_PRODUCTIVO_COMPLETO_2026-06-23_CORREGIDO.md` (ref. Git `HEAD:docs/faltas/...`); delta: sin deltas estructurales detectados en R3 para esta tabla; reglas físicas: preservadas del histórico; no hay decisiones pendientes.
- `LISTO_PARA_DDL` — condición de readiness del modelo conceptual (campos Java verificados; sin bloqueo funcional).
- ~~`MODELO_CONCEPTUAL_CERRADO`~~ — categoría eliminada: todas las `DECISION_DDL-*` fueron cerradas en el slice `SPEC-AS-SOURCE-CLEAN-ROOM-Y-DDL-CLOSURE-001`.

### Dependencias

#### `fal_dependencia`
- **Propósito:** unidades administrativas (organismos/dependencias).
- **PK:** `id BIGINT AUTO_INCREMENT`.
- **Campos:** `codigo_dependencia VARCHAR(20)`, `nombre VARCHAR(200)`, `tipo_acta SMALLINT`, `si_activo BOOLEAN`.
- **Auditoría:** `fh_alta`, `id_user_alta`.
- **Entidad Java:** `FalDependencia`. **Puerto:** `DependenciaRepository` / `InMemoryDependenciaRepository`.
- **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_dependencia_version`
- **Propósito:** versiones de la dependencia con datos cambiantes.
- **PK:** `id_dependencia + version_dep` (compuesta). **FK:** `id_dependencia -> fal_dependencia`.
- **Campos:** `nombre_version`, `tipo_acta SMALLINT`, `si_activo`, `fh_vig_desde`, `fh_vig_hasta`.
- **Entidad Java:** `FalDependenciaVersion`. **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

### Inspectores

#### `fal_inspector`
- **Propósito:** inspectores/agentes del organismo.
- **PK:** `id BIGINT AUTO_INCREMENT`. **Campos:** `legajo VARCHAR(20)`, `apellido`, `nombre`, `si_activo BOOLEAN`.
- **Entidad Java:** `FalInspector`. **Puerto:** `InspectorRepository` / `InMemoryInspectorRepository`. **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_inspector_version`
- **Propósito:** versión del inspector con datos del período.
- **PK:** `id_inspector + version_insp` (compuesta). **FK:** `id_inspector -> fal_inspector`.
- **Entidad Java:** `FalInspectorVersion`. **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.
- **Nota histórica:** los campos de firma maestra del inspector fueron eliminados del modelo (decisión previa a este slice); la firma no se registra en esta tabla.

### Firmantes

#### `fal_firmante`
- **Propósito:** maestro de firmantes/autorizados para firma; gestiona el padrón de usuarios habilitados para firmar documentos.
- **PK:** `id BIGINT AUTO_INCREMENT`.
- **Campos:** `id_user CHAR(36) NOT NULL UNIQUE` (usuario IDP, único por firmante), `nom_firmante VARCHAR(128) NOT NULL` (nombre visible actual; histórico vive en versiones), `si_activo BOOLEAN NOT NULL`.
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Entidad Java:** `FalFirmante` (id `Long`). **Puerto:** `FirmanteRepository` / `InMemoryFirmanteRepository`.
- **No** guardar certificados, binarios, firmas concretas ni metadata pesada. Baja lógica vía `si_activo`; no borrado físico si firmó documentos.
- **Reconciliación triple:** Java ↔ histórico 2026-06-23 ↔ spec vigente. Sin discrepancias. Tipos confirmados: `id_user CHAR(36)`, `nom_firmante VARCHAR(128)`, `si_activo BOOLEAN`, auditoría `DATETIME(6)` / `CHAR(36)`.
- **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA / RECONCILIADA_R3.

#### `fal_firmante_version`
- **Propósito:** versionado histórico de roles, cargo, dependencia y vigencia del firmante; congela el contexto vigente en cada cambio relevante.
- **PK:** `id_firmante + ver_firmante` (compuesta; `ver_firmante SMALLINT`, inicia en 1). **FK:** `id_firmante -> fal_firmante`.
- **Campos:** `id_user CHAR(36) NOT NULL` (snapshot del IDP al crear la versión), `nom_firmante VARCHAR(128) NOT NULL` (snapshot histórico), `rol_firmante VARCHAR(40) NULL` (descriptivo opcional; no define autorización documental), `cargo_firmante VARCHAR(128) NULL`, `id_dep BIGINT NULL FK` (→ `fal_dependencia`; nullable si no aplica), `ver_dep SMALLINT NULL` (obligatorio si hay `id_dep`), `fh_vig_desde DATE NOT NULL`, `fh_vig_hasta DATE NULL` (NULL si vigente), `si_activo BOOLEAN NOT NULL`.
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Entidad Java:** `FalFirmanteVersion` (PK compuesta; campos: `idFirmante Long`, `verFirmante int`, `idUser String`, `nomFirmante String`, `rolFirmante String`, `cargoFirmante String`, `idDep Long`, `verDep Short`, `fhVigDesde LocalDate`, `fhVigHasta LocalDate`, `siActivo boolean`, auditoría). **Puerto:** no listado aparte (acceso vía `FirmanteRepository`).
- **Reglas:** `rol_firmante` es descriptivo/institucional; la autorización documental concreta va en `fal_firmante_version_habilitacion`. `tipo_firma` NO va en esta tabla. Al versionar: versión anterior cierra `fh_vig_hasta` + `si_activo = false`.
- **Reconciliación triple:** Java ↔ histórico 2026-06-23 ↔ spec vigente. Sin discrepancias. Todos los campos con tipos confirmados del histórico (CHAR/VARCHAR/DATE/BOOLEAN/DATETIME(6)).
- **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA / RECONCILIADA_R3.

#### `fal_firmante_version_habilitacion`
- **Propósito:** define qué tipos de documento y qué roles de firma puede satisfacer una versión concreta de firmante; es la fuente de autorización documental por firmante.
- **PK:** `id_firmante + ver_firmante + tipo_docu + rol_firma_req` (compuesta). **FK:** `(id_firmante, ver_firmante) -> fal_firmante_version`.
- **Campos:** `tipo_docu SMALLINT NOT NULL` (tipo de documento habilitado; compatible con `fal_documento.tipo_docu`), `rol_firma_req SMALLINT NOT NULL` (rol requerido que puede satisfacer; compatible con `fal_documento_firma_req.rol_firma_req`), `mecanismo_firma_req SMALLINT NULL` (restringe mecanismo solo si informado; NULL = sin restricción), `si_activo BOOLEAN NOT NULL` (baja lógica).
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Entidad Java:** `FalFirmanteVersionHabilitacion` (campos: `idFirmante Long`, `verFirmante int`, `tipoDocu short`, `rolFirmaReq short`, `mecanismoFirmaReq Short`, `siActivo boolean`, auditoría). **NO tiene campo `id` propio**: la PK es completamente compuesta.
- **Reglas:** vigencia temporal se toma de `fal_firmante_version`; sin PK surrogate adicional.
- **Reconciliación triple:** Java ↔ histórico 2026-06-23 ↔ spec vigente. **CORRECCIÓN R3:** la spec anterior afirmaba erróneamente `PK: id BIGINT AUTO_INCREMENT`; `FalFirmanteVersionHabilitacion.java` no tiene campo `id`; el histórico 2026-06-23 (sección 1.9) confirma PK compuesta `(id_firmante, ver_firmante, tipo_docu, rol_firma_req)`. Corrección sin impacto funcional.
- **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA / RECONCILIADA_R3.

### Personas y domicilios

#### `fal_persona`
- **Propósito:** maestro de personas (infractores, presentantes).
- **PK:** `id BIGINT AUTO_INCREMENT`.
- **Campos:** `tipo_persona SMALLINT NOT NULL` (`FISICA`/`JURIDICA`), `tipo_doc SMALLINT NULL`, `nro_doc VARCHAR(20) NULL`, `apellido VARCHAR NULL` (persona física), `nombres VARCHAR NULL` (persona física), `razon_social VARCHAR(64) NULL` (persona jurídica), `nombre_mostrar VARCHAR(64) NULL` (calculado), `email_principal VARCHAR(160) NULL`, `telefono_principal VARCHAR(20) NULL`, `id_suj BIGINT NULL` (sujeto/rubro de ingresos), `id_bie BIGINT NULL` (cuenta/bien en ingresos), `suj_bie_estado SMALLINT NULL`, `fh_suj_bie_creacion DATETIME(6) NULL`.
- **Auditoría:** `fh_alta`, `id_user_alta`, `fh_ult_mod`, `id_user_ult_mod`.
- **Entidad Java:** `FalPersona`. **Puerto:** `PersonaRepository` / `InMemoryPersonaRepository`.
- **versionRow/OCC:** NO — `FalPersona` no define `versionRow` (verificado contra el código Java vigente); no aplica OCC (`DECISION_DDL-PERS-01` CERRADA).
- **Estado:** LISTO_PARA_DDL / RECONCILIADA_R3.
- **Unicidad física:** UNIQUE(`tipo_doc, nro_doc`) WHERE `tipo_doc IS NOT NULL AND nro_doc IS NOT NULL`; personas no identificadas sin restricción (`DECISION_DDL-PERS-01` CERRADA).
- **No existen** `nombre_completo`, `fh_nacimiento`, `sexo`, `si_identificado`, `si_extranjero`, `id_ign`, `id_indec`, `id_local` como columnas de `fal_persona`: no tienen respaldo en `FalPersona.java`. El nombre para mostrar se calcula desde `apellido`+`nombres` o `razon_social` (método `calcularNombreMostrar()`), nunca se persiste un único campo `nombre_completo`.

#### `fal_persona_domicilio`
- **Propósito:** domicilios de personas (infractor, notificación); no es el lugar del hecho (eso vive en `fal_acta`). **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `persona_id -> fal_persona`, `acta_origen_id -> fal_acta` (NULL, acta donde nació el domicilio).
- **Campos:** `tipo_domicilio SMALLINT NOT NULL`, `origen_domicilio SMALLINT NOT NULL`, `modo_domicilio SMALLINT NOT NULL` (`MALVINAS_LOCAL`/`EXTERNO`), `si_activo BOOLEAN NOT NULL`, `si_notificable BOOLEAN NOT NULL`, `si_principal BOOLEAN NOT NULL`, `id_provincia SMALLINT NULL`, `unidad_territorial_tipo SMALLINT NULL`, `id_unidad_territorial INT NULL`, `id_localidad BIGINT NULL` (solo modo `EXTERNO`), `id_calle BIGINT NULL` (solo modo `EXTERNO`), `id_loc_malvinas VARCHAR(8) NULL` (solo modo `MALVINAS_LOCAL`; preserva ceros iniciales, no convertir a número), `localidad_malvinas_version_id BIGINT NULL FK` (solo modo `MALVINAS_LOCAL`), `id_tca_malvinas VARCHAR(10) NULL` (solo modo `MALVINAS_LOCAL`; preserva ceros iniciales, no convertir a número), `calle_malvinas_version_id BIGINT NULL FK` (solo modo `MALVINAS_LOCAL`), `calle_txt VARCHAR NULL`, `altura INT NULL`, `si_sin_altura BOOLEAN NOT NULL`, `unidad_funcional VARCHAR NULL`, `codigo_postal VARCHAR NULL`, `domicilio_txt VARCHAR NULL` (cache legible, mutable), `validacion_domicilio VARCHAR NULL`, `si_normalizado_parcial BOOLEAN NOT NULL`, `lat DECIMAL NULL`, `lon DECIMAL NULL`, `origen_ubicacion SMALLINT NULL`.
- **Auditoría:** `fh_alta`, `id_user_alta`, `fh_ult_mod`, `id_user_ult_mod`.
- **Entidad Java:** `FalPersonaDomicilio`. **Puerto:** `PersonaDomicilioRepository` / `InMemoryPersonaDomicilioRepository`.
- **versionRow/OCC:** NO — `FalPersonaDomicilio` no define `versionRow` (verificado contra el código Java vigente); baja lógica vía `si_activo=false`, nunca borrado físico.
- **No existen** `id_calle_version` (BIGINT único), `nro_puerta`/`piso`/`dpto`, `localidad_id`, `texto_libre`, ni `id_tca`/`id_loc` como `BIGINT`: el modelo real distingue `id_calle` (BIGINT, modo `EXTERNO`) de `id_tca_malvinas` (**VARCHAR**, modo `MALVINAS_LOCAL`), usa `altura`+`unidad_funcional` en lugar de `nro_puerta`/`piso`/`dpto`, `id_localidad` en lugar de `localidad_id`, y `domicilio_txt` en lugar de `texto_libre`.
- **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

### Acta core

#### `fal_acta`
- **Propósito:** agregado raíz del expediente de faltas. Guarda identidad administrativa, estado actual (tripla canónica), captura, lugar del hecho, QR y numeración.
- **PK:** `id BIGINT AUTO_INCREMENT`. **UK:** `uuid_tecnico CHAR(36)`, `codigo_qr VARCHAR(512)`.
- **FK conceptuales:** `id_talonario -> num_talonario`, `id_insp + ver_insp -> fal_inspector_version`, `id_dep + ver_dep -> fal_dependencia_version`, `id_persona_infractor -> fal_persona`, `id_domicilio_infractor_act -> fal_persona_domicilio`, `id_domicilio_notif_act -> fal_persona_domicilio`, `localidad_infr_malvinas_version_id -> geo_malv_localidad_version`, `calle_infr_malvinas_version_id -> geo_malv_calle_version`, `id_motivo_archivo_actual -> fal_motivo_archivo`.
- **Campos — identidad y numeración:** `nro_acta VARCHAR(30) NULL`, `id_talonario BIGINT NULL`, `nro_talonario_usado INT NULL`.
- **Campos — tipo y captura:** `tipo_acta SMALLINT NOT NULL`, `origen_captura SMALLINT NOT NULL`, `id_dispositivo_captura VARCHAR(80) NULL`, `id_user_captura CHAR(36) NULL`, `fh_captura DATETIME(6) NOT NULL`, `lat_captura DOUBLE NULL`, `lon_captura DOUBLE NULL`, `precision_captura_m DOUBLE NULL`, `fh_pos_captura DATETIME(6) NULL`, `origen_pos_captura SMALLINT NULL`.
- **Campos — fecha funcional:** `fecha_acta DATE NOT NULL`, `fecha_labrado DATETIME(6) NOT NULL`.
- **Campos — dependencia e inspector (versionados):** `id_dep BIGINT NOT NULL`, `ver_dep INT NOT NULL`, `id_insp BIGINT NULL`, `ver_insp INT NULL` (obligatoria si hay inspector).
- **Campos — infractor y domicilios:** `id_persona_infractor BIGINT NOT NULL` (toda acta tiene sujeto técnico), `id_domicilio_infractor_act BIGINT NULL`, `id_domicilio_notif_act BIGINT NULL`. Nombre/documento/domicilio del infractor NO se almacenan en `fal_acta`: se obtienen de `fal_persona`/`fal_persona_domicilio` vía estas FK; el snapshot los proyecta como vista derivada.
- **Campos — resumen del hecho:** `resumen_hecho VARCHAR(1000) NULL`, `domicilio_hecho VARCHAR(500) NULL` (compatibilidad; el lugar del hecho normalizado vive en los campos siguientes).
- **Campos — lugar del hecho (nomenclatura local Malvinas):** `id_loc_infr_malvinas VARCHAR(8) NULL`, `localidad_infr_malvinas_version_id BIGINT NULL`, `id_tca_infr_malvinas VARCHAR(10) NULL`, `calle_infr_malvinas_version_id BIGINT NULL`.
- **Campos — lugar del hecho (altura):** `altura_infr INT NULL`, `altura_origen_infr SMALLINT NULL`, `si_altura_infr_estimada BOOLEAN NOT NULL DEFAULT FALSE`.
- **Campos — lugar del hecho (coordenadas finales):** `lat_infr DOUBLE NULL`, `lon_infr DOUBLE NULL` (precisión física candidata a `DECISION_DDL`, ver nota), `origen_ubicacion_infr SMALLINT NULL`, `si_ubicacion_infr_manual BOOLEAN NOT NULL DEFAULT FALSE`.
- **Campos — texto libre del lugar del hecho:** `si_dom_txt_infr BOOLEAN NOT NULL DEFAULT FALSE`, `dom_txt_infr VARCHAR(255) NULL`.
- **Campos — contexto geográfico:** `si_eje_urb BOOLEAN NOT NULL DEFAULT FALSE`.
- **Campos — QR:** `codigo_qr VARCHAR(512) NOT NULL`, `qr_payload_version SMALLINT NOT NULL DEFAULT 0`.
- **Campos — estado actual (tripla canónica):** `bloque_actual CHAR(4) NOT NULL`, `est_proc_act CHAR(4) NOT NULL`, `sit_adm_act CHAR(4) NOT NULL`.
- **Campos — resultado y cierre:** `resultado_final SMALLINT NOT NULL`, `resultado_firma_infractor SMALLINT NOT NULL`, `id_motivo_archivo_actual BIGINT NULL`, `permite_reingreso BOOLEAN NULL`, `fh_cierre DATETIME(6) NULL` (funcional), `fh_archivo DATETIME(6) NULL` (funcional).
- **Auditoría:** `fh_alta`, `id_user_alta`, `fh_ult_mod`, `id_user_ult_mod`.
- **Entidad Java:** `FalActa` (id `Long`). **Puerto:** `ActaRepository` / `InMemoryActaRepository`.
- **versionRow/OCC:** SI (`versionRow` en `FalActa`, default 0).
- **Orden determinístico:** no aplica (entidad única por expediente).
- **No existen** `estado_procesal`/`situacion_administrativa` como `SMALLINT`, ni `id_persona` como nombre de FK: los nombres reales son `est_proc_act`/`sit_adm_act`, y ambos junto con `bloque_actual` son **CHAR(4)** (código `String` estable del enum, no `SMALLINT`); la FK a persona se llama `id_persona_infractor` y es **NOT NULL**.
- **Nota de precisión física:** el modelo histórico (`MODELO_MARIADB_FALTAS_FINAL_PRODUCTIVO_COMPLETO_2026-06-23_CORREGIDO.md`, eliminado; historia en Git) proponía `lat_infr`/`lon_infr` como `DECIMAL(12,8)`; el tipo Java vigente es `Double`. La elección final de tipo físico (`DOUBLE` vs `DECIMAL(12,8)`) es una decisión de diseño DDL, no un gap funcional; no se abre un nuevo `DECISION_DDL-*` en este slice porque no bloquea `READY_FOR_DDL` ni reabre dominio.
- **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

### Eventos, snapshot, evidencias y observaciones

#### `fal_acta_evento`
- **Propósito:** log append-only de eventos del expediente (timeline de hechos reales de dominio).
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `acta_id -> fal_acta`, `id_docu_rel -> fal_documento` (NULL), `id_notif_rel -> fal_notificacion` (NULL).
- **Campos:** `tipo_evt CHAR(6) NOT NULL` (código estable del enum `TipoEventoActa`, **no** `SMALLINT`), `origen_evt SMALLINT NOT NULL`, `fh_evt DATETIME(6) NOT NULL`, `bloque_func CHAR(4) NULL`, `est_proc_ant CHAR(4) NULL`, `est_proc_nvo CHAR(4) NULL`, `sit_adm_ant CHAR(4) NULL`, `sit_adm_nva CHAR(4) NULL`, `actor_tipo SMALLINT NULL`, `actor_id CHAR(36) NULL`, `actor_ref VARCHAR(80) NULL`, `id_docu_rel BIGINT NULL`, `id_notif_rel BIGINT NULL`, `id_pres_rel BIGINT NULL`, `id_user_evt CHAR(36) NULL`, `si_evt_cierre BOOLEAN NOT NULL`, `si_evt_ext BOOLEAN NOT NULL`, `si_permite_reing BOOLEAN NOT NULL`, `descripcion_legible VARCHAR(255) NULL`, `correlacion_id VARCHAR(60) NULL`.
- **Entidad Java:** `FalActaEvento` (id `Long`, inmutable). **Puerto:** `ActaEventoRepository` / `InMemoryActaEventoRepository`.
- **Orden determinístico:** por `fh_evt + id` (desempate por `id` autoincremental); append-only, nunca se actualiza ni borra. **No existe** un campo `orden_logico`: el propio Javadoc de `FalActaEvento` declara explícitamente "el orden del timeline es fhEvt + id; no se usa campo ordenLogico".
- **No usa payload JSON:** el detalle estructurado vive en la tabla funcional correspondiente y el evento conserva la FK (`id_docu_rel`, `id_notif_rel`, `id_pres_rel`). Confirmado por el Javadoc de `FalActaEvento` ("No se usa payload JSON") y por el modelo histórico (`MODELO_MARIADB_FALTAS_FINAL_PRODUCTIVO_COMPLETO_2026-06-23_CORREGIDO.md`, eliminado; historia en Git: "La tabla no guarda payload libre" / "No usar `payload_json`"). No existen `id_documento`, `id_notificacion`, `id_operador`, `descripcion` ni `fecha_evento` con esos nombres: los nombres reales son `id_docu_rel`, `id_notif_rel`, (`actor_id`/`actor_ref`/`id_user_evt` en lugar de un único `id_operador`), `descripcion_legible` y `fh_evt`.
- **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_acta_snapshot`
- **Propósito:** proyección operativa resumida del expediente (1:1 con acta), derivada y regenerable. Alimenta bandejas, filtros, badges y habilitación de acciones. No es fuente de verdad; se recalcula en cada transición de dominio.
- **PK:** `id_acta BIGINT NOT NULL FK` (→ `fal_acta`).
- **Campos — control de concurrencia:** `version_row INT NOT NULL DEFAULT 0` (OCC; `DECISION_DDL-SNAP-01` CERRADA).
- **Campos — estado procesal:** `bloque_actual CHAR(4) NULL` (`BloqueActual`), `est_proc_act CHAR(4) NULL` (`EstadoProcesalActa`), `sit_adm_act CHAR(4) NULL` (`SituacionAdministrativaActa`), `resultado_final SMALLINT NULL` (`ResultadoFinalActa`, `EXPLICIT_NUMERIC_CODE`).
- **Campos — bandejas y acciones:** `cod_bandeja VARCHAR(50) NULL` (`CodigoBandeja`, `EXPLICIT_STRING_CODE`; longitud física confirmada: max 50 caracteres; `DECISION_DDL-SNAP-01` CERRADA), `sub_bandeja VARCHAR(80) NULL`, `accion_pendiente VARCHAR(50) NULL` (`AccionPendiente`, `EXPLICIT_STRING_CODE`; longitud física confirmada: max 50 caracteres; **no** `CHAR(6)`).
- **Campos — documentos y notificaciones:** `tiene_documentos BOOLEAN NOT NULL DEFAULT FALSE`, `tiene_docs_pendientes_firma BOOLEAN NOT NULL DEFAULT FALSE`, `tiene_docs_listos_para_notificar BOOLEAN NOT NULL DEFAULT FALSE`, `tiene_notificaciones BOOLEAN NOT NULL DEFAULT FALSE`, `notificacion_en_curso BOOLEAN NOT NULL DEFAULT FALSE`, `bloqueado_cierre BOOLEAN NOT NULL DEFAULT FALSE`, `id_docu_ult BIGINT NULL FK` (→ `fal_documento`), `bloqueado_notificacion BOOLEAN NOT NULL DEFAULT FALSE`.
- **Campos — valorización operativa:** `valorizacion_operativa_id BIGINT NULL FK` (→ `fal_acta_valorizacion`), `estado_valorizacion_operativa SMALLINT NULL` (`EstadoValorizacion`), `tipo_valorizacion_operativa SMALLINT NULL` (`TipoValorizacionActa`), `monto_operativo_vigente DECIMAL(14,2) NULL` (valorización UX del acta; no es estado económico de pagos), `si_monto_confirmado BOOLEAN NOT NULL DEFAULT FALSE`.
- **Campos — satélites de acta:** `licencia_provincia_txt VARCHAR(64) NULL`, `licencia_unidad_txt VARCHAR(80) NULL`, `nomenclatura_resumen VARCHAR(120) NULL`, `id_bie_i BIGINT NULL`, `id_bie_c BIGINT NULL`.
- **Campos — paralización:** `motivo_paralizacion_act SMALLINT NULL` (`MotivoParalizacion`, `EXPLICIT_NUMERIC_CODE`).
- **Campos — control técnico:** `ultimo_evento_tipo CHAR(6) NULL` (`TipoEventoActa`, `EXPLICIT_STRING_CODE`), `ultima_actualizacion DATETIME(6) NULL`.
- **Entidad Java:** `FalActaSnapshot` (PK: `idActa Long`; campos verificados). **Puerto:** `ActaSnapshotRepository` / `InMemoryActaSnapshotRepository`.
- **versionRow/OCC:** IMPLEMENTADO (`DECISION_DDL-SNAP-01` CERRADA). `InMemoryActaSnapshotRepository.guardar()` auto-incrementa `versionRow`.
- **Economía de pagos:** **NO** transportada en snapshot (`DECISION_DDL-SNAP-02` CERRADA). Los campos históricos `tipo_obligacion_pago`, `estado_obligacion_pago`, `monto_obligacion_pago`, `tipo_forma_pago_vigente`, `estado_forma_pago_vigente`, `si_plan_pago`, `estado_plan_pago`, `cant_cuotas_plan`, `valor_cuota_plan`, `cant_cuotas_pagadas`, `cant_cuotas_mora`, `cant_cuotas_mora_consec`, `cant_dias_mora`, `si_apta_intimacion`, `motivo_apta_intimacion`, `si_pago_procesado`, `si_pago_confirmado`, `fh_ult_sync_ingresos` han sido **eliminados** de `FalActaSnapshot.java` y de esta tabla. Las lecturas económicas salen de `fal_acta_economia_proyeccion`. `SnapshotRecalculador.proyectarPagos()` es no-op confirmado.
- **Campos históricos excluidos (DECISION_DDL-SNAP-02 CERRADA):** `fh_acta`, `nro_acta`, `tipo_acta`, `id_dep`/`ver_dep`, `id_insp`/`ver_insp`, `id_persona_infractor`, `nombre_infractor`, `doc_infractor_txt`, domicilios/textos/ubicación del hecho, `esta_cerrada`, `si_visible_bandeja`, `prioridad`, bloqueantes separados, gestión externa duplicada, fechas de vencimiento duplicadas, `id_evt_ult`, `id_notif_ult`, `fh_snapshot`, `rebuild_id`, `fh_ult_mod`, `id_user_ult_mod`.
- **Estado:** LISTO_PARA_DDL / RECONCILIADA_R3.

#### `fal_acta_evidencia`
- **Propósito:** evidencias físicas vinculadas al acta (imágenes, capturas). Actualmente solo `FIRMA_OLOGRAFA_INFRACTOR` (código 6). No es firma institucional; no participa en `FalDocumentoFirma` ni `FalDocumentoFirmaReq`.
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_acta -> fal_acta`.
- **Campos (verificados contra Java; `DECISION_DDL-EVID-01` CERRADA):** `id_acta BIGINT NOT NULL`, `tipo_evid SMALLINT NOT NULL` (`TipoEvidenciaActa`, `EXPLICIT_NUMERIC_CODE`; único valor activo: `FIRMA_OLOGRAFA_INFRACTOR = 6`), `storage_key VARCHAR(255) NOT NULL` (no guardar binario), `fecha_registro DATETIME(6) NOT NULL` (instante funcional de captura/incorporación), `hash_evid CHAR(64) NULL` (SHA-256 hexadecimal opcional; si informado debe ser exactamente 64 caracteres hex), `fh_alta DATETIME(6) NOT NULL` (instante técnico de alta), `id_user_alta CHAR(36) NOT NULL` (actor técnico/funcional que incorporó la evidencia).
- **Campos excluidos (DECISION_DDL-EVID-01 CERRADA):** `nombre_archivo`, `mime_type`, `orden_evid` — pertenecen al metadato del storage, no al registro de dominio. No se agrega `fh_captura` adicional: `fecha_registro` y `fh_alta` cubren las semánticas funcional y técnica. Estos cuatro campos del histórico 2026-06-23 quedan excluidos.
- **Entidad Java:** `FalActaEvidencia` (id `Long`, inmutable; campos: `id`, `idActa`, `tipoEvid`, `storageKey`, `fechaRegistro`, `hashEvid`, `fhAlta`, `idUserAlta`). Constructor exige `fechaRegistro`, `fhAlta` y `idUserAlta` not-null; `hashEvid` nullable con validación de 64 caracteres hex si presente. **Puerto:** `ActaEvidenciaRepository` / `InMemoryActaEvidenciaRepository`.
- **Append-only:** SÍ (inmutable post-creación).
- **Estado:** LISTO_PARA_DDL / RECONCILIADA_R3.

#### `fal_observacion`
- **Propósito:** observaciones tipificadas por entidad.
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_acta -> fal_acta`.
- **Campos:** `entidad_tipo SMALLINT`, `entidad_id BIGINT`, `texto TEXT`, `id_user_alta`, `fh_alta`.
- **Entidad Java:** `FalObservacion`. **Puerto:** `ObservacionRepository` / `InMemoryObservacionRepository`. **Enum:** `EntidadTipoObservada` (22 códigos). **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

### Normativa

#### `fal_dependencia_normativa`
- **Entidad Java:** `FalDependenciaNormativa`. **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_normativa_faltas`
- **Entidad Java:** `FalNormativaFaltas`. **Puerto:** `NormativaRepository` / `InMemoryNormativaRepository`. **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_articulo_normativa_faltas`
- **Entidad Java:** `FalArticuloNormativaFaltas`. **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_tarifario_unidad_faltas`
- **Propósito:** valores unitarios por artículo para cálculo de valorización.
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_articulo -> fal_articulo_normativa_faltas`.
- **Campos:** `fh_vig_desde DATE`, `fh_vig_hasta DATE NULL`, `valor_unitario DECIMAL(14,4)`.
- **Entidad Java:** `FalTarifarioUnidadFaltas`. **Puerto:** `TarifarioUnidadFaltasRepository` / `InMemoryTarifarioUnidadFaltasRepository`.
- **Unicidad física requerida:** sin solapamiento de rangos de vigencia activos por artículo (invariante ya aplicado en InMemory; DDL debe reforzarlo con constraint/verificación transaccional).
- **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_medida_preventiva`
- **Entidad Java:** `FalMedidaPreventiva` (versión atómica; una activa por código). **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_articulo_medida_preventiva`
- **Entidad Java:** `FalArticuloMedidaPreventiva` (PK compuesta vía `ArticuloMedidaPreventivaId`; sin reactivación silenciosa). **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

### Artículos infringidos y valorización

#### `fal_acta_articulo_infringido`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_acta -> fal_acta`, `id_articulo -> fal_articulo_normativa_faltas`.
- **Unicidad física requerida:** UK activo `(id_acta, id_articulo)`.
- **Entidad Java:** `FalActaArticuloInfringido`. **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_acta_valorizacion`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_acta -> fal_acta`.
- **Campos:** `monto_total DECIMAL(14,2)`, `fh_calculo DATETIME(6)`, `si_vigente BOOLEAN`.
- **Entidad Java:** `FalActaValorizacion` (`versionRow` presente). **Operación atómica:** `confirmarVigenteAtomico` garantiza una sola valorización vigente CONFIRMADA por acta+tipo. **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_acta_valorizacion_item`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_valorizacion -> fal_acta_valorizacion`.
- **Entidad Java:** `FalActaValorizacionItem` (inmutable post-confirmación del padre). **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

### Satélites del acta

Todas con PK `acta_id BIGINT` (1:1) o `id BIGINT AUTO_INCREMENT` según el
caso; FK `acta_id -> fal_acta`. Un único satélite válido por acta según
`tipo_acta` (guardrail de dominio, no de este documento).

#### `fal_acta_transito`
- **Propósito:** datos específicos de actas de tránsito. **PK+FK:** `acta_id BIGINT` (1:1, solo `tipo_acta=TRANSITO`).
- **Campos:** `nro_licencia VARCHAR(20) NULL`, `id_prov_lic SMALLINT NULL`, `unidad_territorial_lic_tipo SMALLINT NULL`, `id_unidad_territorial_lic INT NULL`, `si_ret_licencia BOOLEAN NOT NULL`, `si_ret_vehiculo BOOLEAN NOT NULL`, `si_control_alcoholemia BOOLEAN NOT NULL`.
- **Entidad Java:** `FalActaTransito`. **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_acta_transito_alcoholemia`
- **Propósito:** mediciones de alcoholemia vinculadas al acta de tránsito (no es tipo de acta; satélite eventual). **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `acta_id -> fal_acta` (N mediciones por acta).
- **Campos:** `orden_medicion SMALLINT NOT NULL` (positivo, único por acta), `tipo_prueba SMALLINT NOT NULL`, `resultado_cualitativo SMALLINT NULL`, `resultado_numerico DECIMAL(4,2) NULL` (escala máx. 2), `unidad_medida SMALLINT NULL` (obligatoria si se informa `resultado_numerico`), `id_alcoholimetro BIGINT NULL`, `ver_alcoholimetro SMALLINT NULL` (obligatoria junto con `id_alcoholimetro`), `si_resultado_final BOOLEAN NOT NULL DEFAULT FALSE` (solo una final por acta), `fh_medicion DATETIME(6) NULL`.
- **Auditoría:** `fh_alta`, `id_user_alta`.
- **Entidad Java:** `FalActaTransitoAlcoholemia`. **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_acta_vehiculo`
- **Propósito:** datos del vehículo vinculado al acta. **PK+FK:** `acta_id BIGINT` (1:1; un vehículo por acta por ahora).
- **Campos:** `dominio_vehiculo VARCHAR(10) NULL`, `tipo_vehiculo SMALLINT NULL`, `marca_vehiculo_id BIGINT NULL FK`, `marca_vehiculo_txt VARCHAR(24) NULL` (fallback si no existe en catálogo), `modelo_vehiculo_id BIGINT NULL FK`, `modelo_vehiculo_txt VARCHAR(24) NULL`, `anio_vehiculo SMALLINT NULL`, `color_vehiculo VARCHAR(24) NULL`, `estado_general_vehiculo SMALLINT NULL`.
- **Auditoría:** `fh_alta`, `id_user_alta`.
- **Entidad Java:** `FalActaVehiculo`. **FK:** `marca_vehiculo_id -> fal_vehiculo_marca`, `modelo_vehiculo_id -> fal_vehiculo_modelo`. **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_acta_contravencion`
- **Propósito:** nomenclatura catastral y rubro del acta de contravención/comercio, instanciada históricamente al labrar (no se actualiza si Catastro cambia después). **PK+FK:** `acta_id BIGINT` (1:1, solo `tipo_acta=CONTRAVENCION`/`COMERCIO`).
- **Campos:** `id_suj_i BIGINT NULL`, `id_bie_i BIGINT NULL` (cuenta inmueble/CVP; NULL solo si no hay cuenta disponible o carga manual/excepcional), `id_suj_c BIGINT NULL`, `id_bie_c BIGINT NULL` (cuenta comercio; juntos), `circ SMALLINT NULL`, `secc VARCHAR(2) NULL`, `frac VARCHAR(7) NULL`, `mza VARCHAR(7) NULL`, `parc VARCHAR(7) NULL`, `ufun VARCHAR(7) NULL`, `ucomp VARCHAR(20) NULL`, `origen_nomencl SMALLINT NOT NULL`, `si_nomenclatura_manual BOOLEAN NOT NULL`, `motivo_nomenclatura_manual SMALLINT NULL` (obligatorio si manual), `rubro_id BIGINT NULL FK` (junto con `id_rub`), `id_rub INT NULL` (referencia externa `informix.rubrocom.id_rub`), `ambito_ctv SMALLINT NULL`, `ambito_ctv_txt VARCHAR(80) NULL` (obligatorio solo si `ambito_ctv=OTRO`).
- **Auditoría:** `fh_alta`, `id_user_alta`.
- **Entidad Java:** `FalActaContravencion`. **FK:** `rubro_id -> fal_rubro_version`. **No existe** `nomenclatura_txt` como columna propia: el resumen legible se proyecta en `fal_acta_snapshot.nomenclatura_resumen`. **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_acta_sustancias_alimenticias`
- **Propósito:** datos de actas de sustancias alimenticias/bromatología. **PK+FK:** `acta_id BIGINT` (1:1, solo circuito sustancias/bromatología).
- **Campos:** `rubro_id BIGINT NULL FK` (junto con `id_rub`), `id_rub INT NULL`, `ambito_ctv SMALLINT NULL`, `ambito_ctv_txt VARCHAR(80) NULL` (obligatorio solo si `ambito_ctv=OTRO`), `descripcion_sustancias TEXT NULL`.
- **Auditoría:** `fh_alta`, `id_user_alta`.
- **Entidad Java:** `FalActaSustanciasAlimenticias`. **FK:** `rubro_id -> fal_rubro_version`. **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_acta_medida_preventiva`
- **Propósito:** medida preventiva efectivamente aplicada al acta (no es catálogo). **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `acta_id -> fal_acta` (1 acta : N medidas).
- **Campos:** `acta_articulo_id BIGINT NOT NULL FK` (artículo infringido que habilitó la medida), `medida_preventiva_id BIGINT NOT NULL FK`, `med_prev_txt VARCHAR(255) NULL`, `estado_medida SMALLINT NOT NULL` (default `APLICADA`; sin transición desde estado terminal `ANULADA`/`CUMPLIDA`), `si_genera_bloqueante BOOLEAN NOT NULL`.
- **Auditoría:** `fh_alta`, `id_user_alta`.
- **Entidad Java:** `FalActaMedidaPreventiva`. **FK:** `acta_articulo_id -> fal_acta_articulo_infringido`, `medida_preventiva_id -> fal_medida_preventiva`. **Relación con bloqueantes:** unidireccional (el bloqueante referencia la medida vía `fal_acta_bloqueante_cierre_material.medida_preventiva_acta_id`; esta tabla no guarda `bloqueante_id`, para evitar ciclo de FK). **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

### Bloqueantes

#### `fal_acta_bloqueante_cierre_material`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_acta -> fal_acta`.
- **Campos:** `origen SMALLINT`, `estado SMALLINT`, `si_activo BOOLEAN`, `descripcion TEXT NULL`, `fh_alta DATETIME(6)`, `fh_cierre DATETIME(6) NULL`.
- **Entidad Java:** `FalBloqueanteMaterial` (id `Long`). **Puerto:** `BloqueanteMaterialRepository` / `InMemoryBloqueanteMaterialRepository`.
- **Índice de consulta:** `(id_acta, si_activo)` — usado por `existsActivoByActaId`.
- **versionRow/OCC:** implementado en `InMemoryBloqueanteMaterialRepository.guardar()` (modifica in-place); DDL: `fal_acta_bloqueante_cierre_material.version_row INT NOT NULL DEFAULT 0` (`DECISION_DDL-BLOQ-01` CERRADA).
- **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA / RECONCILIADA_R3.

### Documentos, plantillas y firma

#### `fal_documento`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_acta -> fal_acta`, `id_plantilla -> fal_documento_plantilla`.
- **Campos:** `tipo_docu SMALLINT`, `nro_docu VARCHAR(30) NULL`, `estado_docu SMALLINT`, `storage_key VARCHAR(500) NULL`, `hash_docu VARCHAR(128) NULL`, `tipo_firma_req SMALLINT`, `fh_alta`, `id_user_alta`.
- **Entidad Java:** `FalDocumento`. **Puerto:** `DocumentoRepository` / `InMemoryDocumentoRepository`.
- **versionRow/OCC:** implementado en `InMemoryDocumentoRepository.guardar()` (modifica in-place) y en `FalDocumento.versionRow`; DDL: `fal_documento.version_row INT NOT NULL DEFAULT 0` (`DECISION_DDL-DOC-01` CERRADA).
- **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA / RECONCILIADA_R3.

#### `fal_acta_documento`
- **Propósito:** tabla pivot relación canónica acta-documento (pertenencia funcional).
- **PK:** `id_acta + id_documento` (compuesta, sin ID artificial).
- **Campos:** `rol_docu_acta SMALLINT`, `si_principal BOOLEAN`, `fh_alta DATETIME(6)`, `id_user_alta CHAR(36)`.
- **Entidad Java:** `FalActaDocumento` + `ActaDocumentoId` (value object). **Puerto:** `ActaDocumentoRepository` / `InMemoryActaDocumentoRepository` (thread-safe, `principalLock`).
- **Servicio:** `ActaDocumentoService`. **Enum:** `RolDocuActa` (12 valores, código `short` estable).
- **Snapshot:** `FalActaSnapshot.idDocuUlt` derivado vía pivot (`FALLO > RESOLUCION > NOTIFICACION > ACTA_PRINCIPAL`).
- **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_documento_firma`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_documento -> fal_documento`, `id_firmante + ver_firmante -> fal_firmante_version`.
- **Campos:** `estado_firma SMALLINT`, `seq_firma_req SMALLINT`, `tipo_firma SMALLINT`, `storage_key VARCHAR(500) NULL`, `hash_firma VARCHAR(128) NULL`, `fh_firma DATETIME(6) NULL`.
- **Entidad Java:** `FalDocumentoFirma`. **Idempotencia:** `referenciaFirmaExt` (ver `../10-domain/firma-notificacion-fallo.md`). **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_documento_firma_req`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_documento -> fal_documento`. **Entidad Java:** `FalDocumentoFirmaReq`. **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_documento_plantilla`
- **PK:** `id BIGINT AUTO_INCREMENT`.
- **Campos:** `codigo VARCHAR(50)`, `nombre VARCHAR(200)`, `tipo_docu SMALLINT`, `accion_documental SMALLINT`, `tipo_acta SMALLINT NULL`, `tipo_firma_req SMALLINT`, `si_requiere_numeracion BOOLEAN`, `momento_numeracion_docu SMALLINT`, `si_notificable BOOLEAN`, `si_genera_pdf BOOLEAN`, `si_seleccionable BOOLEAN`, `si_activa BOOLEAN`, `fh_vig_desde DATE`, `fh_vig_hasta DATE NULL`, `fh_alta DATETIME(6)`, `id_user_alta CHAR(36)`.
- **Entidad Java:** `FalDocumentoPlantilla`. **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_documento_plantilla_firma_req`
- **PK:** `id_plantilla + seq_firma_req` (compuesta). **Entidad Java:** `FalDocumentoPlantillaFirmaReq`. **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_documento_plantilla_contenido`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_plantilla -> fal_documento_plantilla`.
- **Campos:** `version_contenido SMALLINT NOT NULL`, `titulo VARCHAR(200) NOT NULL`, `cuerpo_markdown TEXT NOT NULL`, `encabezado_markdown TEXT NULL`, `pie_markdown TEXT NULL`, `variables_declaradas_json JSON NOT NULL DEFAULT '[]'`, `si_activo BOOLEAN NOT NULL`, `fh_vig_desde DATETIME(6) NOT NULL`, `fh_vig_hasta DATETIME(6) NULL`, `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Entidad Java:** `FalDocumentoPlantillaContenido`. **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_documento_plantilla_default`
- **PK:** `id BIGINT AUTO_INCREMENT`. **Índice de consulta:** `(accion_documental, tipo_acta, id_dependencia)`.
- **Campos:** `accion_documental SMALLINT NOT NULL`, `tipo_acta SMALLINT NULL` (NULL = genérico), `tipo_docu SMALLINT NOT NULL`, `id_dependencia BIGINT NULL`, `ver_dependencia SMALLINT NULL`, `id_plantilla BIGINT NOT NULL FK`, `prioridad SMALLINT NOT NULL`, `fh_vig_desde DATETIME(6) NOT NULL`, `fh_vig_hasta DATETIME(6) NULL`, `si_activo BOOLEAN NOT NULL`, `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Entidad Java:** `FalDocumentoPlantillaDefault`. **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_documento_redaccion`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_documento -> fal_documento`, `id_plantilla_contenido -> fal_documento_plantilla_contenido`, `redaccion_origen_id -> fal_documento_redaccion` (self). **UK:** `id_documento + nro_revision`.
- **Campos:** `nro_revision SMALLINT NOT NULL`, `redaccion_origen_id BIGINT NULL`, `estado_redaccion SMALLINT NOT NULL` (`BORRADOR=1, CONFIRMADA=2, ANULADA=3`), `contenido_base_markdown TEXT NOT NULL`, `contenido_editable_markdown TEXT NOT NULL`, `variables_snapshot_json JSON NOT NULL DEFAULT '{}'`, `variables_faltantes_json JSON NOT NULL DEFAULT '[]'`, `diagnostico_json JSON NOT NULL DEFAULT '{}'`, `recursos_snapshot_json JSON NULL`, campos de auditoría por hito (creación/regeneración/edición/confirmación/anulación).
- **Entidad Java:** `FalDocumentoRedaccion`. **Servicios:** `DocumentoCombinacionService`, `DocumentoVariableContextBuilder`, `DocumentoVariableRegistry`, `DocumentoRedaccionService`. **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

### Notificaciones

#### `fal_notificacion`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_acta -> fal_acta`, `id_documento -> fal_documento`.
- **Campos:** `tipo_docu_notificado SMALLINT`, `canal VARCHAR(50)`, `fecha_envio DATETIME(6)`, `estado SMALLINT`, `resultado SMALLINT NULL`, `fecha_resultado DATETIME(6) NULL`, `intentos INT`, `observaciones TEXT NULL`.
- **Entidad Java:** `FalNotificacion` (id `Long`). **Puerto:** `NotificacionRepository` / `InMemoryNotificacionRepository`.
- **versionRow/OCC:** implementado en `InMemoryNotificacionRepository.guardar()` (modifica in-place); DDL: `fal_notificacion.version_row INT NOT NULL DEFAULT 0` (`DECISION_DDL-NOTI-01` CERRADA).
- **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA / RECONCILIADA_R3.

#### `fal_notificacion_intento`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_notificacion -> fal_notificacion`.
- **Campos:** `nro_intento SMALLINT`, `canal VARCHAR(50)`, `fecha_intento DATETIME(6)`, `resultado SMALLINT`, `detalle TEXT NULL`.
- **Entidad Java:** `FalNotificacionIntento`. **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_notificacion_acuse`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_notificacion -> fal_notificacion`. **Entidad Java:** `FalNotificacionAcuse`. **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_lote_correo`
- **PK:** `id BIGINT AUTO_INCREMENT`. **Campos:** `tipo_lote SMALLINT`, `fecha_generacion DATETIME(6)`, `estado_lote SMALLINT`, `id_user_alta CHAR(36)`.
- **Entidad Java:** `FalLoteCorreo`. **Puerto:** `LoteCorreoRepository` / `InMemoryLoteCorreoRepository`.
- **Clave de idempotencia:** `loteCodigo` (unicidad física requerida, ver `inmemory-mariadb-deltas.md`, sección de unicidades). **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

### Fallo y firmeza

#### `fal_acta_fallo`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK conceptuales:** `id_acta -> fal_acta`, `valorizacion_id -> fal_acta_valorizacion`, `fallo_reemplazado_id -> fal_acta_fallo` (self), `documento_id -> fal_documento`.
- **Campos:** `tipo_fallo SMALLINT NOT NULL`, `estado_fallo SMALLINT NOT NULL` (`EstadoFalloActa`, `EXPLICIT_NUMERIC_CODE`; códigos: `PENDIENTE_FIRMA=1, PENDIENTE_NOTIFICACION=2, NOTIFICADO=3, FIRME=4, REEMPLAZADO=5, SIN_EFECTO=6`; `DECISION_DDL-ENUM-01` CERRADA), `monto_condena DECIMAL(14,2) NULL`, `resultado_fallo SMALLINT NULL`, `fundamentos TEXT NULL`, `documento_id BIGINT NULL`, `valorizacion_id BIGINT NULL`, `fallo_reemplazado_id BIGINT NULL`, `fecha_dictado DATETIME(6) NOT NULL`, `fecha_notificacion DATETIME(6) NULL`, `fecha_resultado_final DATETIME(6) NULL`, `fh_firma DATETIME(6) NULL`, `fh_vto_apelacion DATETIME(6) NULL`, `si_activo BOOLEAN NOT NULL`, `si_firme BOOLEAN NOT NULL DEFAULT FALSE`, `fh_firmeza DATETIME(6) NULL`, `origen_firmeza SMALLINT NULL` (`1=VENCIMIENTO_PLAZO_APELACION, 2=APELACION_RECHAZADA`), `version_row INT NOT NULL DEFAULT 0`.
- **Auditoría:** `fh_alta`, `id_user_alta`, `fh_mod`, `id_user_mod`.
- **Entidad Java:** `FalActaFallo` (id `Long`, `versionRow` presente). **Puerto:** `FalloActaRepository` / `InMemoryFalloActaRepository`.
- **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA / RECONCILIADA_R3.

> **Nota de firmeza:** la firmeza de condena es inline en `fal_acta_fallo`
> (`si_firme`, `fh_firmeza`, `origen_firmeza`). No existe una tabla propia de
> firmeza. `FalActaFirmezaCondena` y `FirmezaCondenaRepository` están
> eliminados del código Java vigente (verificado contra el árbol de fuentes).
> Ver `../20-application/fallo-command-contracts.md` (CMD-FALLO-005/006) para
> el contrato de firmeza vigente.

### Apelación

#### `fal_acta_apelacion`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK conceptuales:** `id_acta -> fal_acta`, `fallo_id -> fal_acta_fallo`, `documento_resolucion_id -> fal_documento`.
- **Campos:** `fallo_id BIGINT NOT NULL`, `estado_apelacion SMALLINT NOT NULL` (`EstadoApelacionActa`, `EXPLICIT_NUMERIC_CODE`; códigos: `PRESENTADA=1, EN_ANALISIS=2, RECHAZADA=3, ACEPTADA_ABSUELVE=4, RESUELTA=5, SIN_EFECTO=6`; `DECISION_DDL-ENUM-01` CERRADA), `fecha_presentacion DATETIME(6) NOT NULL`, `canal_apelacion SMALLINT NOT NULL`, `tipo_presentacion SMALLINT NOT NULL`, `texto_apelacion TEXT NULL`, `presentante VARCHAR(300) NULL`, `fundamentos TEXT NULL`, `observaciones TEXT NULL`, `si_activa BOOLEAN NOT NULL`, `fecha_resolucion DATETIME(6) NULL`, `fundamentos_resolucion TEXT NULL`, `observaciones_resolucion TEXT NULL`, `documento_resolucion_id BIGINT NULL`, `version_row INT NOT NULL DEFAULT 0`.
- **Auditoría:** `fh_alta`, `id_user_alta`, `fh_mod`, `id_user_mod`.
- **Entidad Java:** `FalActaApelacion` (id `Long`, `falloId Long`, `versionRow` presente). **Puerto:** `ApelacionActaRepository` / `InMemoryApelacionActaRepository`.
- **Nota de consulta (CMD-FALLO-006):** la apelación relevante para declarar firmeza por apelación rechazada se busca por `fallo_id`, no por "última apelación del acta"; una apelación histórica de otro fallo no debe bloquear ni satisfacer la precondición (ver `../20-application/command-contracts.md`).
- **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA / RECONCILIADA_R3.

#### `fal_acta_apelacion_documento`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_apelacion -> fal_acta_apelacion`, `id_documento -> fal_documento`.
- **Entidad Java:** `FalActaApelacionDocumento`. **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

### Paralización

#### `fal_acta_paralizacion`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_acta -> fal_acta`.
- **Campos:** `motivo SMALLINT NOT NULL`, `descripcion TEXT NULL`, `id_user_inicio CHAR(36) NOT NULL`, `fh_inicio DATETIME(6) NOT NULL`, `id_user_cierre CHAR(36) NULL`, `fh_cierre DATETIME(6) NULL`, `motivo_cierre SMALLINT NULL`, `si_activa BOOLEAN NOT NULL`.
- **Entidad Java:** `FalActaParalizacion`. **Puerto:** `ActaParalizacionRepository` / `InMemoryActaParalizacionRepository`. **Servicio:** `ParalizacionActaService`. **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

### Archivo

#### `fal_acta_archivo`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_acta -> fal_acta`.
- **Campos:** `motivo_archivo SMALLINT NOT NULL`, `descripcion TEXT NULL`, `id_user_archivo CHAR(36) NOT NULL`, `fh_archivo DATETIME(6) NOT NULL`, `id_user_reingreso CHAR(36) NULL`, `fh_reingreso DATETIME(6) NULL`, `motivo_reingreso SMALLINT NULL`, `si_archivado BOOLEAN NOT NULL`.
- **Entidad Java:** `FalActaArchivo`. **Puerto:** `ActaArchivoRepository` / `InMemoryActaArchivoRepository`. **Servicio:** `ArchivoActaService`. **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.
- Nota de dominio: archivo no equivale automáticamente a `CERRADA` (ver `../00-governance/glossary.md` y `../10-domain/lifecycle-states.md`).

### Gestión externa

#### `fal_acta_gestion_externa`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_acta -> fal_acta`.
- **Entidad Java:** `FalGestionExterna` (id `Long`). **Puerto:** `GestionExternaRepository` / `InMemoryGestionExternaRepository`.
- **Invariante de vigencia:** a lo sumo una `FalGestionExterna` con `si_activa = true` por acta.
- **versionRow/OCC:** implementado en `InMemoryGestionExternaRepository.guardar()` (modifica in-place); DDL: `fal_acta_gestion_externa.version_row INT NOT NULL DEFAULT 0` (`DECISION_DDL-GEXT-01` CERRADA).
- **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA / RECONCILIADA_R3.

### Pagos

> **Confirmación explícita: NO existe `fal_acta_pago_resolucion`.** Un diseño
> preliminar (2026-06-23, documento histórico eliminado; historia en Git)
> proponía esa tabla con un catálogo `tipo_resolucion` para resolver pagos
> aplicados a obligaciones anteriores. Ese diseño quedó superado:
> `ResolverPagoObligacionAnteriorCommand` (evento `PAGRES`) no crea tabla ni
> obligación/valorización por diferencia; aplica el pago original completo
> contra la obligación vigente mediante un movimiento en
> `fal_acta_pago_movimiento` (`movimientoOrigenId`), verificado contra
> `ResolverPagoObligacionAnteriorService` y `ResolverPagoObligacionAnteriorTest`
> (código Java vigente). No reintroducir `fal_acta_pago_resolucion` ni
> `tipo_resolucion` sin una decisión de dominio explícita que reabra este
> alcance.

#### `fal_acta_obligacion_pago`
- **Propósito:** cabecera de la obligación de pago (estado jurídico/administrativo, monto original y referencias estructurales). No almacena saldos, conciliación, mora ni flags de reapertura.
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `acta_id -> fal_acta`, `persona_id -> fal_persona`, `valorizacion_id -> fal_acta_valorizacion` (NULL), `fallo_id -> fal_acta_fallo` (NULL), `forma_pago_vigente_id -> fal_acta_forma_pago` (NULL).
- **Campos (verificados contra Java):** `version_row INT NOT NULL DEFAULT 0` (OCC), `acta_id BIGINT NOT NULL`, `persona_id BIGINT NOT NULL`, `tipo_obligacion SMALLINT NOT NULL` (`TipoObligacionPago`, código 1=PAGO_VOLUNTARIO/2=CONDENA), `valorizacion_id BIGINT NULL`, `fallo_id BIGINT NULL`, `monto_original DECIMAL(14,2) NOT NULL` (escala 2), `estado_obligacion SMALLINT NOT NULL` (`EstadoObligacionPago`, códigos 1–6; catálogo cerrado), `fh_determinacion DATETIME(6) NOT NULL`, `id_user_determinacion CHAR(36) NULL`, `forma_pago_vigente_id BIGINT NULL`, `fh_cancelacion DATETIME(6) NULL`, `si_excluir_escaneo BOOLEAN NOT NULL DEFAULT FALSE`, `fh_ult_sync_ingresos DATETIME(6) NULL`, `si_vigente BOOLEAN NOT NULL`, `origen_obligacion SMALLINT NOT NULL DEFAULT 1` (`OrigenObligacionPago`, `EXPLICIT_NUMERIC_CODE`; `FALTAS=1, APREMIO=2, JUZGADO=3`; default `FALTAS`; `DECISION_DDL-PAGO-03` CERRADA), `obligacion_reemplazada_id BIGINT NULL FK self` (nullable; apunta a la obligación reemplazada; `DECISION_DDL-PAGO-03` CERRADA).
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Columna generada (DDL):** `acta_id_vigente BIGINT NULL GENERATED ALWAYS AS (CASE WHEN si_vigente THEN acta_id ELSE NULL END)` con `UNIQUE` — unicidad de obligación vigente por acta.
- **Entidad Java:** `FalActaObligacionPago` (id `Long`, `versionRow` presente). **Puerto:** `ObligacionPagoRepository` / `InMemoryObligacionPagoRepository`.
- **Enums:** `TipoObligacionPago` (`EXPLICIT_NUMERIC_CODE`), `EstadoObligacionPago` (`EXPLICIT_NUMERIC_CODE`; catálogo: DETERMINADA=1, PENDIENTE_FORMA_PAGO=2, CON_FORMA_PAGO_VIGENTE=3, CANCELADA_POR_PAGO=4, REEMPLAZADA=5, DEJADA_SIN_EFECTO=6), `OrigenObligacionPago` (`EXPLICIT_NUMERIC_CODE`; `DECISION_DDL-PAGO-03` CERRADA).
- **Modelo (decisión cerrada):** unifica pago voluntario (`tipo=1`) y pago de condena (`tipo=2`); `FalPagoVoluntario` y `FalPagoCondena` (id `String`) son vistas de aplicación sobre esta tabla unificada, no entidades físicas separadas.
- **Resolución de pago aplicado a obligación anterior:** no agrega ningún campo a esta tabla. `ResolverPagoObligacionAnteriorCommand` (evento `PAGRES`) nunca crea una obligación nueva por diferencia: aplica el pago original completo, sin recortar, contra la obligación vigente existente mediante un movimiento en `fal_acta_pago_movimiento` (ver esa tabla). El saldo resultante se deriva siempre de `fal_acta_economia_proyeccion`; un excedente (pago mayor al saldo pendiente) es informativo, no una obligación.
- **Reconciliación triple:** Java ↔ histórico 2026-06-23 ↔ spec vigente. **Deltas resueltos:** (a) `origen_obligacion SMALLINT NOT NULL` — incorporado a Java y DDL (`DECISION_DDL-PAGO-03` CERRADA); (b) `obligacion_reemplazada_id BIGINT NULL` — incorporado a Java y DDL (`DECISION_DDL-PAGO-03` CERRADA); (c) `fh_ult_sync_ingresos` — en Java vigente, incluido en DDL; (d) columna generada `acta_id_vigente` — DDL-only, no en Java.
- **Estado:** LISTO_PARA_DDL / RECONCILIADA_R3.

#### `fal_acta_forma_pago`
- **Propósito:** instrumento de pago vigente/histórico de una obligación (recibo al cobro, plan de pago, refinanciación). El historial se preserva (`si_vigente=false` en reemplazadas).
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `obligacion_pago_id -> fal_acta_obligacion_pago`, `forma_reemplazada_id -> fal_acta_forma_pago` (self, NULL).
- **Campos (verificados contra Java):** `version_row INT NOT NULL DEFAULT 0` (OCC), `obligacion_pago_id BIGINT NOT NULL`, `nro_forma SMALLINT NOT NULL` (positivo, único por obligación), `tipo_forma_pago SMALLINT NOT NULL` (`TipoFormaPago`; catálogo: RECIBO_AL_COBRO=1/PLAN_PAGO=2/REFINANCIACION=3), `estado_forma_pago SMALLINT NOT NULL` (`EstadoFormaPago`; catálogo cerrado, 6 valores), `monto_forma DECIMAL(14,2) NOT NULL` (escala 2; inmutable mientras vigente), `cmte_em CHAR(2) NULL`, `pref_em SMALLINT NULL`, `nro_em INT NULL` (terna completa o totalmente NULL), `cmte_pg CHAR(2) NULL`, `pref_pg SMALLINT NULL`, `nro_pg INT NULL` (terna completa o totalmente NULL), `forma_reemplazada_id BIGINT NULL`, `fh_generacion DATETIME(6) NOT NULL`, `fh_pago_procesado DATETIME(6) NULL`, `fh_pago_confirmado DATETIME(6) NULL`, `fh_baja DATETIME(6) NULL`, `motivo_baja SMALLINT NULL` (`MotivoBajaFormaPago`, `EXPLICIT_NUMERIC_CODE`), `si_vigente BOOLEAN NOT NULL`, `si_excluir_escaneo BOOLEAN NOT NULL DEFAULT FALSE`, `fh_vencimiento DATETIME(6) NULL` (informativo; no rechaza pagos fuera de término; `DECISION_DDL-FORMA-01` CERRADA).
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Columnas generadas (DDL):** `obligacion_pago_id_vigente BIGINT NULL GENERATED ALWAYS AS (CASE WHEN si_vigente THEN obligacion_pago_id ELSE NULL END) UNIQUE`, `tipo_forma_pago_vigente SMALLINT NULL GENERATED ALWAYS AS (CASE WHEN si_vigente THEN tipo_forma_pago ELSE NULL END)`.
- **Entidad Java:** `FalActaFormaPago` (`versionRow` presente). **Enums:** `TipoFormaPago` (`EXPLICIT_NUMERIC_CODE`), `EstadoFormaPago` (`EXPLICIT_NUMERIC_CODE`), `MotivoBajaFormaPago` (`EXPLICIT_NUMERIC_CODE`).
- **Reconciliación triple:** Java ↔ histórico 2026-06-23 ↔ spec vigente. **Delta resuelto:** `fh_vencimiento DATETIME(6) NULL` — incorporado a Java y DDL (`DECISION_DDL-FORMA-01` CERRADA). Columnas generadas son DDL-only.
- **Estado:** LISTO_PARA_DDL / RECONCILIADA_R3.

#### `fal_acta_plan_pago_ref`
- **Propósito:** cabecera estructural resumida del plan de pago externo (Ingresos/Tesorería). Sin tabla de cuotas; el cronograma se consulta en Ingresos.
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `forma_pago_id -> fal_acta_forma_pago`, `obligacion_pago_id -> fal_acta_obligacion_pago`, `plan_refinanciado_id -> fal_acta_plan_pago_ref` (self, NULL).
- **Campos (verificados contra Java):** `version_row INT NOT NULL DEFAULT 0` (OCC), `forma_pago_id BIGINT NOT NULL`, `obligacion_pago_id BIGINT NOT NULL`, `id_tdoc_plan SMALLINT NOT NULL` (tipo doc plan externo; valor 1 según modelo vigente), `id_doc_plan BIGINT NOT NULL` (ID plan Ingresos), `estado_plan SMALLINT NOT NULL` (`EstadoPlanPago`; catálogo: ACTIVO=1/FINALIZADO_POR_PAGO=2/ANULADO=3/REFINANCIADO=4; nunca CAIDO/CUMPLIDO), `fh_generacion_plan DATETIME(6) NULL`, `cantidad_cuotas SMALLINT NOT NULL`, `importe_total_plan DECIMAL(14,2) NOT NULL` (escala 2), `importe_cuota_regular DECIMAL(14,2) NULL`, `fh_ultimo_pago DATETIME(6) NULL` (en Java, **ausente en histórico**: ver `DECISION_DDL-PLAN-01`), `fh_finalizacion_pago DATETIME(6) NULL`, `fh_cancelacion DATETIME(6) NULL`, `fh_refinanciacion DATETIME(6) NULL`, `plan_refinanciado_id BIGINT NULL`, `si_excluir_escaneo BOOLEAN NOT NULL DEFAULT FALSE`, `fh_ult_sync_ingresos DATETIME(6) NULL` (en Java, **ausente en histórico**: ver `DECISION_DDL-PLAN-01`), `si_vigente BOOLEAN NOT NULL`.
- **Columna generada (DDL):** `obligacion_pago_id_vigente BIGINT NULL GENERATED ALWAYS AS (CASE WHEN si_vigente THEN obligacion_pago_id ELSE NULL END) UNIQUE`.
- **Entidad Java:** `FalActaPlanPagoRef` (`versionRow` presente). **Enums:** `EstadoPlanPago` (`EXPLICIT_NUMERIC_CODE`).
- **Nota:** no existe tabla de cuotas (`fal_acta_plan_pago_cuota`); el seguimiento fino de cuotas descripto en un diseño histórico preliminar (2026-06-23, eliminado; historia en Git) no se implementó en el código Java vigente.
- **Reconciliación triple:** Java ↔ histórico 2026-06-23 ↔ spec vigente. **Deltas resueltos:** `fh_ultimo_pago` y `fh_ult_sync_ingresos` — en Java vigente, incluidos en DDL (`DECISION_DDL-PLAN-01` CERRADA). Columna generada `obligacion_pago_id_vigente` es DDL-only.
- **Estado:** LISTO_PARA_DDL / RECONCILIADA_R3.

#### `fal_acta_pago_movimiento`
- **Propósito:** registro append-only de cada hecho económico real (emisión, pago procesado, pago confirmado, reverso, anulación de emisión, pago aplicado a obligación anterior). Tabla inmutable post-inserción; no tiene `versionRow`.
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `obligacion_pago_id -> fal_acta_obligacion_pago`, `forma_pago_id -> fal_acta_forma_pago` (NULL), `plan_pago_ref_id -> fal_acta_plan_pago_ref` (NULL), `movimiento_origen_id -> fal_acta_pago_movimiento` (self, NULL — reversos y resoluciones PAGRES).
- **Campos — identificadores y tipo:** `obligacion_pago_id BIGINT NOT NULL`, `forma_pago_id BIGINT NULL`, `plan_pago_ref_id BIGINT NULL`, `tipo_movimiento SMALLINT NOT NULL` (`TipoMovimientoPago`; DEUDA_EMITIDA=1/PAGO_PROCESADO=2/PAGO_CONFIRMADO=3/PAGO_REVERTIDO=4/EMISION_ANULADA=5), `origen_movimiento SMALLINT NOT NULL` (`OrigenMovimiento`; catálogo cerrado 8 valores), `origen_confirmacion SMALLINT NULL` (`OrigenConfirmacion`; catálogo cerrado 6 valores), `evidencia_documento_id BIGINT NULL`, `clasificacion_pago SMALLINT NOT NULL DEFAULT 1` (`ClasificacionPago`; **catálogo Java vigente**: NORMAL=1/DUPLICADO_REAL=2/EXCEDENTE=3/OBLIGACION_ANTERIOR=4 — difiere del histórico 2026-06-23 que tenía 3 valores y códigos distintos; ver `DECISION_DDL-MOV-04`), `nro_cuota SMALLINT NULL`.
- **Campos — importes:** `importe_capital DECIMAL(14,2) NULL`, `importe_rima DECIMAL(14,2) NULL`, `importe_total DECIMAL(14,2) NULL` (invariante Java: si los tres informados, `capital + rima == total`).
- **Campos — referencias EM/PG:** `cmte_em CHAR(2) NULL`, `pref_em SMALLINT NULL`, `nro_em INT NULL` (terna EM completa o NULL total), `cmte_pg CHAR(2) NULL`, `pref_pg SMALLINT NULL`, `nro_pg INT NULL` (terna PG completa o NULL total; obligatoria en `PAGO_CONFIRMADO` original sin `movimiento_origen_id`).
- **Campos — contexto operativo:** `id_cierre BIGINT NULL`, `id_ope BIGINT NULL`, `movimiento_origen_id BIGINT NULL FK self` (reversos: apunta al movimiento original; PAGRES: apunta al `PAGANT` original; unicidad de aplicación garantizada por columna generada nullable + `UNIQUE` — ver `DECISION_DDL-PAGO-MOV-01` CERRADA), `motivo_anulacion_pago SMALLINT NULL` (`MotivoAnulacionPago`; CONTRACARGO=1/ANULACION_TESORERIA=2/ERROR_OPERATIVO=3/DUPLICADO=4/REVERSION_MEDIO_PAGO=5/OTRO=6), `motivo_aplicacion_pago_anterior VARCHAR(500) NULL` (fuente de verdad para idempotencia de PAGRES; nunca se deriva de `descripcionLegible` del evento; longitud: 500 caracteres máximo, trim obligatorio; `DECISION_DDL-MOV-03` CERRADA).
- **Campos — fechas y referencia:** `fh_pago_procesado DATETIME(6) NULL`, `fh_pago_confirmado DATETIME(6) NULL`, `referencia_externa VARCHAR(80) NULL` (longitud confirmada por validación Java: max 80 caracteres).
- **Campos — fecha funcional:** `fh_movimiento DATETIME(6) NOT NULL` (inmutable; fecha efectiva del hecho económico).
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL` (Java Builder valida NOT NULL y non-blank; histórico 2026-06-23 erróneamente lo mostraba nullable).
- **Entidad Java:** `FalActaPagoMovimiento` (id `Long`, **no tiene `versionRow`**: append-only estricto). **Puerto:** `PagoMovimientoRepository` / `InMemoryPagoMovimientoRepository`.
- **Enums:** `TipoMovimientoPago` (`EXPLICIT_NUMERIC_CODE`), `OrigenMovimiento` (`EXPLICIT_NUMERIC_CODE`), `OrigenConfirmacion` (`EXPLICIT_NUMERIC_CODE`), `ClasificacionPago` (`EXPLICIT_NUMERIC_CODE`), `MotivoAnulacionPago` (`EXPLICIT_NUMERIC_CODE`).
- **Invariante:** vínculo canónico único `movimientoOrigenId` para reversos y para el movimiento de aplicación creado al resolver un pago anterior; append-only, sin edición de movimientos históricos.
- **`ClasificacionPago.OBLIGACION_ANTERIOR`:** un `PAGO_CONFIRMADO` registrado contra una obligación con `siVigente=false` se clasifica siempre así (derivado del estado de la obligación destino, nunca elegido por el actor) y emite `PAGANT` en lugar de `PAGCNF`/`PCOCNF`. No es un valor que el actor pueda declarar contra una obligación vigente (rechazado con 422). Ver `../10-domain/states-events-catalogs.md`.
- **Campos de recibo `cmtePG`/`prefPG`/`nroPG`:** terna completa o totalmente `NULL` (no parcial); obligatoria (completa) en todo movimiento `PAGO_CONFIRMADO` original (`movimientoOrigenId == null`); el movimiento de aplicación PAGRES puede tenerla `NULL`. `origenMovimiento + cmtePG + prefPG + nroPG` es la clave de negocio de un recibo físico; ver `DECISION_DDL-PAGO-MOV-01`.
- **Resolución de pago aplicado a obligación anterior (`PAGRES`), sin tabla propia:** el movimiento de aplicación creado por `ResolverPagoObligacionAnteriorCommand` es un `FalActaPagoMovimiento` más: `tipoMovimiento=PAGO_CONFIRMADO`, `clasificacionPago=NORMAL`, `movimientoOrigenId` = id del `PAGANT` original, `importeTotal` igual al importe total del `PAGANT` (sin recortar contra el saldo pendiente), `motivoAplicacionPagoAnterior` con el motivo normalizado. Un reintento compatible devuelve los datos históricos de la primera aplicación. Ver `../20-application/command-contracts.md`.
- **Reconciliación triple:** Java ↔ histórico 2026-06-23 ↔ spec vigente. **Deltas resueltos:**
  - `tipo_vencimiento_pago SMALLINT NULL` — excluido (`DECISION_DDL-MOV-01` CERRADA; derivable de `fh_movimiento` + `fal_acta_forma_pago.fh_vencimiento`).
  - `fh_recepcion_tecnica DATETIME(6) NULL` — excluido (`DECISION_DDL-MOV-02` CERRADA; `fh_alta` cubre la semántica técnica).
  - `motivo_aplicacion_pago_anterior VARCHAR(500)` — incorporado a Java y DDL (`DECISION_DDL-MOV-03` CERRADA).
  - `ClasificacionPago` catálogo: autoritativo Java 4 valores (`DECISION_DDL-MOV-04` CERRADA).
  - `movimiento_origen_id` (Java) generaliza `movimiento_anulado_id` (histórico); misma columna, uso ampliado.
  - `id_user_alta` nullabilidad: histórico decía nullable, Java valida NOT NULL → DDL usa NOT NULL.
- **Estado:** LISTO_PARA_DDL / RECONCILIADA_R3.

#### `fal_acta_economia_proyeccion`
- **Propósito:** proyección económica actual por acta (1:1); mutable, regenerable, optimizada para consultas. No es fuente histórica ni jurídica. Incluye importes, saldos, conciliación actual, mora, plan caído calculado, aptitud de intimación y flags de pago.
- **PK:** `acta_id BIGINT NOT NULL` (también FK → `fal_acta`). **FK adicionales:** `obligacion_vigente_id -> fal_acta_obligacion_pago` (NULL), `forma_pago_vigente_id -> fal_acta_forma_pago` (NULL), `plan_pago_vigente_id -> fal_acta_plan_pago_ref` (NULL), `ultimo_movimiento_id_proyectado -> fal_acta_pago_movimiento` (NULL).
- **Campos — control técnico:** `version_row INT NOT NULL DEFAULT 0` (OCC).
- **Campos — referencias vigentes:** `obligacion_vigente_id BIGINT NULL`, `forma_pago_vigente_id BIGINT NULL`, `plan_pago_vigente_id BIGINT NULL`.
- **Campos — estado actual (copias regenerables):** `tipo_obligacion SMALLINT NULL` (`TipoObligacionPago`), `estado_obligacion SMALLINT NULL` (`EstadoObligacionPago`), `monto_obligacion_vigente DECIMAL(14,2) NULL`, `tipo_forma_pago SMALLINT NULL` (`TipoFormaPago`), `estado_forma_pago SMALLINT NULL` (`EstadoFormaPago`), `estado_plan SMALLINT NULL` (`EstadoPlanPago`), `cantidad_cuotas SMALLINT NULL`, `importe_cuota_regular DECIMAL(14,2) NULL`.
- **Campos — mora y seguimiento de cuotas:** `cantidad_cuotas_pagadas SMALLINT NULL`, `cantidad_cuotas_vencidas SMALLINT NULL`, `cantidad_cuotas_en_mora SMALLINT NULL`, `cantidad_cuotas_mora_consec SMALLINT NULL`, `dias_mora_max SMALLINT NULL`.
- **Campos — importes calculados:** `importe_pago_procesado DECIMAL(14,2) NULL`, `importe_confirmado_evidencia_pendiente DECIMAL(14,2) NULL`, `importe_confirmado_tesoreria DECIMAL(14,2) NULL`, `importe_observado_tesoreria DECIMAL(14,2) NULL`, `importe_aplicado_total DECIMAL(14,2) NOT NULL DEFAULT 0` (neto; base para cancelación y saldo), `importe_revertido DECIMAL(14,2) NULL`, `saldo_pendiente DECIMAL(14,2) NULL` (`= MAX(0, monto - aplicadoNeto)`), `importe_excedente DECIMAL(14,2) NOT NULL DEFAULT 0` (**en Java, ausente en histórico** — ver delta), `si_parcialmente_pagada BOOLEAN NOT NULL DEFAULT FALSE` (**en Java, ausente en histórico** — ver delta), `importe_vencido_plan DECIMAL(14,2) NULL`.
- **Campos — conciliación actual:** `estado_conciliacion_actual SMALLINT NOT NULL DEFAULT 1` (`EstadoConciliacionActual`; NO_APLICA=1/PENDIENTE_TESORERIA=2/CONCILIADO_TESORERIA=3/OBSERVADO_TESORERIA=4; default `NO_APLICA`), `si_conciliacion_pendiente BOOLEAN NOT NULL DEFAULT FALSE`, `fh_ultima_conciliacion DATETIME(6) NULL`, `referencia_ultima_conciliacion VARCHAR(80) NULL` (longitud confirmada por histórico 2026-06-23).
- **Campos — flags de pago:** `si_pago_procesado BOOLEAN NOT NULL DEFAULT FALSE`, `si_pago_confirmado BOOLEAN NOT NULL DEFAULT FALSE`.
- **Campos — plan caído calculado:** `si_plan_caido_calculado BOOLEAN NOT NULL DEFAULT FALSE`, `fh_desde_plan_caido_calculado DATETIME(6) NULL`, `motivo_plan_caido_calculado SMALLINT NULL` (`MotivoPlanCaidoCalculado`; CUOTAS_EN_MORA=1/MORA_CONSECUTIVA=2/ANTIGUEDAD_MORA=3/REGLA_INGRESOS=4/COMBINADA=5).
- **Campos — aptitud de intimación:** `si_apta_intimacion BOOLEAN NOT NULL DEFAULT FALSE`, `fh_apta_intimacion DATETIME(6) NULL`, `motivo_apta_intimacion SMALLINT NULL` (`MotivoAptitudIntimacion`).
- **Campo — reapertura:** `si_reapertura_requerida BOOLEAN NOT NULL DEFAULT FALSE` (reverso posterior al cierre; no reabre automáticamente).
- **Campos — watermarks técnicos:** `ultimo_movimiento_id_proyectado BIGINT NULL FK`, `fh_corte_economico DATETIME(6) NULL` (puede ser NULL antes del primer cálculo; `DECISION_DDL-ECPR-01` CERRADA), `fh_ultima_sincronizacion DATETIME(6) NULL`, `origen_ultima_actualizacion SMALLINT NOT NULL DEFAULT 1` (`OrigenUltimaActualizacion`; TIEMPO_REAL=1/SINCRONIZACION_NOCTURNA=2/REBUILD=3/CORRECCION_CONTROLADA=4).
- **Campos — última modificación:** `fh_ult_mod DATETIME(6) NOT NULL` (audit obligatorio; `DECISION_DDL-ECPR-01` CERRADA; la aplicación suministra desde `FaltasClock`; `InMemoryEconomiaProyeccionRepository.save()` lanza si es null), `id_user_ult_mod CHAR(36) NULL`.
- **Campo derivado (no físico):** `si_cancelada` — no es columna propia; se deriva de `estado_obligacion = CANCELADA_POR_PAGO` en código.
- **Entidad Java:** `FalActaEconomiaProyeccion` (`actaId Long` PK; `versionRow` presente; 47 campos verificados). **Puerto:** `EconomiaProyeccionRepository` / `InMemoryEconomiaProyeccionRepository`. **Servicios:** `EconomiaProyeccionRecalculador`, `ProcesoNocturnoEconomicoService`.
- **Nota:** no existe `fal_acta_pago_conciliacion`; esta tabla es mutable/regenerable, no fuente histórica.
- **Reconciliación triple:** Java ↔ histórico 2026-06-23 ↔ spec vigente. **Deltas detectados:**
  - `importe_excedente DECIMAL(14,2) NOT NULL DEFAULT 0` — **en Java**, ausente en histórico 2026-06-23; incluir en DDL.
  - `si_parcialmente_pagada BOOLEAN NOT NULL DEFAULT FALSE` — **en Java**, ausente en histórico; incluir en DDL.
  - `fh_corte_economico` nullabilidad: histórico dice NOT NULL, Java es nullable → `DECISION_DDL-ECPR-01`.
  - `fh_ult_mod` nullabilidad: histórico dice NOT NULL, Java es nullable → `DECISION_DDL-ECPR-01`.
  - `referencia_ultima_conciliacion VARCHAR(80)` — confirmada por histórico; sin discrepancia.
  - Todos los demás campos coinciden en tipo y nullabilidad con el histórico 2026-06-23.
- **Estado:** LISTO_PARA_DDL / RECONCILIADA_R3.

#### Paridad de concurrencia/idempotencia para resolución de pago anterior

No existe `fal_acta_pago_resolucion`. Equivalencia entre InMemory vigente y
la garantía productiva requerida en el adapter MariaDB para
`POST /api/faltas/pagos/resolver-pago-anterior`:

| Aspecto | InMemory vigente | MariaDB requerido |
|---|---|---|
| Exclusión | método de instancia `synchronized` en `ResolverPagoObligacionAnteriorService.resolver` (monitor `this`, no estático) | `versionRow` de `FalActaObligacionPago` + transacción; unicidad física de aplicación (`DECISION_DDL-PAGO-MOV-01` CERRADA) |
| Alcance | serializa llamadas concurrentes sobre la misma instancia del servicio; no es mecanismo multiinstancia | OCC + `UNIQUE`, sin dependencia de locks JVM |
| Unicidad | verificación previa en memoria (`PagoMovimientoRepository.findByMovimientoOrigenId`) | constraint única parcial/condicional sobre `movimiento_origen_id` en `fal_acta_pago_movimiento`; columna generada nullable + `UNIQUE` (`DECISION_DDL-PAGO-MOV-01` CERRADA) |
| Idempotencia | reintento con el mismo `movimientoPagoId`, misma obligación aplicada y motivo equivalente (comparado contra el campo estructurado `motivoAplicacionPagoAnterior`, nunca parseado de `descripcionLegible`) devuelve el mismo resultado sin crear movimiento ni evento adicional, reportando actor/motivo/fecha **históricos** de la primera aplicación; distinta obligación o motivo es conflicto (409) | verificar existencia por `movimiento_origen_id` antes de aplicar; recargar tras conflicto OCC en la obligación vigente; el resultado reportado debe leerse del registro histórico, no de la solicitud de reintento |
| Evidencia InMemory | `ResolverPagoObligacionAnteriorTest` | — |

### Talonarios y numeración

#### `num_politica`
- **Propósito:** define composición del número visible y comportamiento de reinicio anual. Un talonario pertenece a una política.
- **PK:** `id BIGINT AUTO_INCREMENT`.
- **Campos (verificados Java + histórico 2026-06-23):** `codigo VARCHAR(12) NOT NULL UNIQUE`, `descripcion VARCHAR(120) NOT NULL`, `clase_numeracion SMALLINT NOT NULL` (`ClaseNumeracion`; ACTA=1/DOCUMENTO=2), `si_reinicio_anual BOOLEAN NOT NULL`, `si_incluye_prefijo BOOLEAN NOT NULL`, `prefijo VARCHAR(10) NULL`, `si_incluye_anio BOOLEAN NOT NULL`, `formato_anio SMALLINT NULL` (2 o 4 dígitos), `si_incluye_serie BOOLEAN NOT NULL`, `longitud_nro SMALLINT NULL` (en Java: `Short`; histórico SÍ nullable), `formato_visible VARCHAR(60) NOT NULL` (ej. `{PREF}-{ANIO}-{SERIE}-{NRO}`), `si_activa BOOLEAN NOT NULL`, `fh_vig_desde DATE NOT NULL`, `fh_vig_hasta DATE NULL` (NULL si vigente).
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Entidad Java:** `NumPolitica` (id `Long`, sin `versionRow`). **Puerto:** `NumeracionRepository` / `InMemoryNumeracionRepository`.
- **Reconciliación triple:** Java ↔ histórico 2026-06-23 ↔ spec vigente. Sin discrepancias. Todos los tipos confirmados. `longitud_nro` nullable confirmado por Java (`Short`; puede ser null) y histórico.
- **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA / RECONCILIADA_R3.

#### `num_talonario`
- **Propósito:** talonario concreto (electrónico o manual físico); referencia a `SEQUENCE` nativa de MariaDB mediante `nombre_secuencia`.
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `politica_id -> num_politica`.
- **Campos (verificados Java + histórico 2026-06-23):** `version_row INT NOT NULL DEFAULT 0` (OCC), `politica_id BIGINT NOT NULL`, `codigo VARCHAR(12) NOT NULL UNIQUE`, `descripcion VARCHAR(120) NOT NULL`, `tipo_talonario SMALLINT NOT NULL` (`TipoTalonario`; ELECTRONICO/MANUAL_FISICO), `clase_talonario SMALLINT NOT NULL` (`ClaseNumeracion`), `anio SMALLINT NULL` (obligatorio si política reinicia anual), `serie VARCHAR(12) NULL`, `nro_desde INT NOT NULL`, `nro_hasta INT NULL` (NULL si sin límite operativo), `nombre_secuencia VARCHAR(64) NOT NULL UNIQUE` (nombre del objeto `SEQUENCE` real; uno por talonario electrónico; `NOCACHE` recomendado), `si_activo BOOLEAN NOT NULL`, `si_bloqueado BOOLEAN NOT NULL DEFAULT FALSE`, `cod_desbloqueo VARCHAR(64) NULL`, `obs_talonario VARCHAR(255) NULL`.
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Entidad Java:** `NumTalonario` (`versionRow` presente). **Puerto:** `NumeracionRepository` / `InMemoryNumeracionRepository`. Método: `estaOperativo()` = `siActivo && !siBloqueado`.
- **Reconciliación triple:** Java ↔ histórico 2026-06-23 ↔ spec vigente. Sin discrepancias. Todos los tipos confirmados: VARCHAR(12) para `codigo`/`serie`, VARCHAR(64) para `nombre_secuencia`/`cod_desbloqueo`, VARCHAR(255) para `obs_talonario`.
- **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA / RECONCILIADA_R3.

#### `num_talonario_ambito`
- **Propósito:** regla que autoriza dónde aplica un talonario (alcance global, por dependencia o por tipo documental transversal).
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `talonario_id -> num_talonario`, `id_dep -> fal_dependencia` (NULL).
- **Campos (verificados Java + histórico 2026-06-23):** `talonario_id BIGINT NOT NULL`, `clase_talonario SMALLINT NOT NULL` (`ClaseNumeracion`), `tipo_docu SMALLINT NULL` (obligatorio si clase DOCUMENTO), `tipo_acta SMALLINT NULL`, `id_dep BIGINT NULL`, `ver_dep SMALLINT NULL` (obligatorio si hay `id_dep`), `alcance SMALLINT NOT NULL` (`AlcanceTalonario`; GLOBAL=1/DEPENDENCIA=2/TRANSVERSAL_DOCUMENTO=3), `prioridad SMALLINT NOT NULL` (menor valor = mayor prioridad), `fh_desde DATE NOT NULL`, `fh_hasta DATE NULL` (NULL si vigente), `si_activo BOOLEAN NOT NULL`.
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Entidad Java:** `NumTalonarioAmbito` (id `Long`, sin `versionRow`). **Puerto:** `NumeracionRepository`.
- **Reconciliación triple:** Java ↔ histórico 2026-06-23 ↔ spec vigente. Sin discrepancias. Todos los tipos confirmados.
- **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA / RECONCILIADA_R3.

#### `num_talonario_inspector`
- **Propósito:** asignación/rendición de talonarios manuales físicos a inspectores. Solo talonarios `MANUAL_FISICO` pueden asignarse.
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_talonario -> num_talonario`.
- **Campos (verificados Java + histórico 2026-06-23):** `id_talonario BIGINT NOT NULL`, `id_insp BIGINT NOT NULL`, `ver_insp SMALLINT NOT NULL`, `fh_entrega DATETIME(6) NOT NULL`, `id_user_entrega CHAR(36) NOT NULL`, `fh_devolucion DATETIME(6) NULL`, `id_user_devolucion CHAR(36) NULL`, `estado_asignacion SMALLINT NOT NULL` (`EstadoAsignacionTalonario`; ENTREGADO=1/DEVUELTO=2/CERRADO=3/OBSERVADO=4), `si_activa BOOLEAN NOT NULL`.
- **Columna generada (DDL):** `talonario_id_activo BIGINT NULL GENERATED ALWAYS AS (CASE WHEN si_activa THEN id_talonario ELSE NULL END) UNIQUE` — unicidad de asignación activa por talonario manual; confirmada por Javadoc de `NumTalonarioInspector` (`talonarioIdActivo` simula esta columna en InMemory).
- **Entidad Java:** `NumTalonarioInspector` (id `Long`, sin `versionRow`; campo `talonarioIdActivo` simula columna generada).
- **Reconciliación triple:** Java ↔ histórico 2026-06-23 ↔ spec vigente. Sin discrepancias. Todos los tipos confirmados. Columna generada `talonario_id_activo` es DDL-only (equivalente InMemory en Java).
- **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA / RECONCILIADA_R3.

#### `num_talonario_movimiento`
- **Propósito:** registro operativo único de cada número de talonario. Fila única por `id_talonario + nro_talonario`; no se borran ni actualizan.
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_talonario -> num_talonario`, `acta_id -> fal_acta` (NULL), `documento_id -> fal_documento` (NULL).
- **Restricción:** `UNIQUE(id_talonario, nro_talonario)` — confirmada en Javadoc de `NumTalonarioMovimiento`.
- **Campos (verificados Java + histórico 2026-06-23):** `id_talonario BIGINT NOT NULL`, `nro_talonario INT NOT NULL`, `estado_numero SMALLINT NOT NULL` (`EstadoNumeroTalonario`; USADO=1/ANULADO=2/DEVUELTO_SIN_USAR=3/RENDIDO=4/JUSTIFICADO=5), `motivo_anulacion SMALLINT NULL` (`MotivoAnulacionTalonario`; obligatorio si estado=ANULADO), `observacion VARCHAR(500) NULL` (longitud confirmada en histórico 2026-06-23), `acta_id BIGINT NULL` (obligatorio si fue usado para acta), `documento_id BIGINT NULL` (obligatorio si fue usado para documento), `id_dep BIGINT NULL`, `ver_dep SMALLINT NULL`, `id_insp BIGINT NULL`, `ver_insp SMALLINT NULL`, `fh_movimiento DATETIME(6) NOT NULL`, `id_user_movimiento CHAR(36) NOT NULL`.
- **Nota:** `NumTalonarioMovimiento` no tiene `fh_alta`/`id_user_alta` separados; usa `fh_movimiento`/`id_user_movimiento`.
- **Entidad Java:** `NumTalonarioMovimiento` (id `Long`, sin `versionRow`; append-only: no `set*`).
- **Reconciliación triple:** Java ↔ histórico 2026-06-23 ↔ spec vigente. Sin discrepancias. `observacion VARCHAR(500)` confirmada por histórico. Todos los tipos confirmados.
- **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA / RECONCILIADA_R3.

#### Paridad de concurrencia/idempotencia para numeración documental

Equivalencia entre InMemory vigente y la garantía productiva requerida en el
adapter MariaDB para `POST /api/faltas/documentos/{id}/numerar`:

| Aspecto | InMemory vigente | MariaDB requerido |
|---|---|---|
| Exclusión | método de instancia `synchronized` en `DocumentoService.numerarDocumentoParaFirmas` (monitor `this`, no estático) | `SEQUENCE` asociada al talonario + transacción + OCC |
| Alcance | serializa llamadas concurrentes sobre la misma instancia de `DocumentoService`; no es mecanismo multiinstancia | `NEXT VALUE FOR <secuencia>`, sin dependencia de locks JVM |
| Unicidad | verificación previa en memoria | constraint única `(id_talonario, nro_talonario)` en `num_talonario_movimiento` |
| Idempotencia | `nroDocu != null` -> devuelve número existente, `yaEstabaNumerado = true` | verificar `nro_docu IS NOT NULL` antes de consumir otra `SEQUENCE`; recargar tras conflicto OCC |
| Evidencia InMemory | `DocumentoNumeracionFirmasTest` (17 casos) | — |

### QR y portal

#### `fal_acta_qr_acceso`
- **Propósito:** log append-only de auditoría de accesos **válidos** realizados vía código QR. Solo se inserta una fila cuando el token fue válido y el acta pudo resolverse; tokens inválidos, corruptos, con scope incorrecto o actas inexistentes **no** producen filas.
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `acta_id -> fal_acta`.
- **Campos:** `fh_acceso DATETIME(6) NOT NULL`, `canal_acceso SMALLINT NOT NULL`, `ip_origen VARCHAR(45) NULL` (validado IPv4/IPv6), `user_agent VARCHAR(255) NULL` (sanitizado), `resultado_acceso SMALLINT NOT NULL`.
- **Auditoría:** `fh_alta`.
- **Entidad Java:** `FalActaQrAcceso` (inmutable). **Puerto:** `QrAccesoRepository` / `InMemoryQrAccesoRepository`. **Servicio:** `QrActaService`. **Protección de token:** `AesGcmQrTokenProtector`.
- **No existen** `token_qr`, `url_acceso`, `fh_generacion`, `fh_vencimiento` ni `si_activo`: esta tabla no almacena el token, el payload ni el hash del token (Javadoc de `FalActaQrAcceso`: "No almacena el token, el payload ni el hash del token"), y no es una fila mutable "activa": es un log append-only de accesos ya resueltos. El token en sí y su emisión/vigencia son responsabilidad de `codigo_qr`/`qr_payload_version` en `fal_acta` (ver sección "Acta core") y de `AesGcmQrTokenProtector`, no de esta tabla.
- **Append-only:** SÍ.
- **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

### Catálogos de dominio adicionales

#### `fal_vehiculo_marca`
- **Entidad Java:** `FalVehiculoMarca`. **Puerto:** `VehiculoMarcaRepository` / `InMemoryVehiculoMarcaRepository`.
- **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_vehiculo_modelo`
- **Entidad Java:** `FalVehiculoModelo`. **Puerto:** `VehiculoModeloRepository` / `InMemoryVehiculoModeloRepository`.
- **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_rubro_version`
- **Entidad Java:** `FalRubroVersion`. Tabla de dominio Faltas con sincronización externa (Informix); no es infraestructura técnica.
- **Campos versionados (histórico 2026-06-23):** `row_hash`, `previous_row_hash`, `source_operation`, `close_operation`, `si_version_actual`, `valid_from`, `valid_to`, `synced_at`.
- **Puerto:** `RubroVersionRepository` / `InMemoryRubroVersionRepository`.
- **Estado:** PREEXISTING_CANONICAL_ADOPTED — tabla preexistente en el baseline; el DDL del dominio la adopta sin crearla, alterarla ni administrar sus datos. Unica tabla Faltas preexistente de las 65 canonicas (ver `50-persistence/ddl-execution-and-test-seeding.md`).

#### `fal_motivo_archivo`
- **Entidad Java:** `FalMotivoArchivo`. **Puerto:** `MotivoArchivoRepository` / `InMemoryMotivoArchivoRepository`.
- **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

### Calendario y excepciones

#### `fal_dia_no_computable`
- **Propósito:** registro de excepciones locales al calendario administrativo (feriados, asuetos). Las reglas fijas (domingo, 1-ene, 1-may) no se persisten; ver `../10-domain/calendario-plazos-administrativos.md`.
- **PK:** `id BIGINT AUTO_INCREMENT`.
- **Campos (verificados contra Java):** `fecha DATE NOT NULL`, `tipo SMALLINT NOT NULL` (`TipoDiaNoComputable`, `EXPLICIT_NUMERIC_CODE`; `FERIADO=1, ASUETO_ADMINISTRATIVO=2, OTRO=3`; `DECISION_DDL-ENUM-01` CERRADA), `descripcion VARCHAR(160) NOT NULL` (max 160 validado en constructor Java), `origen SMALLINT NOT NULL` (`OrigenDiaNoComputable`, `EXPLICIT_NUMERIC_CODE`; `MANUAL=1, SINCRONIZACION_EXTERNA=2`; `DECISION_DDL-ENUM-01` CERRADA), `referencia_externa VARCHAR(200) NULL` (obligatoria para SINCRONIZACION_EXTERNA, null para MANUAL; max 200 validado), `si_activo BOOLEAN NOT NULL DEFAULT TRUE`.
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL` (max 36 validado), `fh_baja DATETIME(6) NULL`, `id_user_baja CHAR(36) NULL` (max 36; se establece al desactivar).
- **OCC/versionRow:** no aplica; las excepciones se desactivan (`si_activo=false`) pero nunca se editan campos sustantivos.
- **UK/unicidad:** unicidad física requerida sobre `fecha` con `si_activo=TRUE` (implementación candidata: columna generada o índice parcial; ver `inmemory-mariadb-deltas.md`, sección de unicidades).
- **Fuente:** código Java vigente (`FalDiaNoComputable.java`). Sin contraparte en modelo histórico 2026-06-23 (tabla ausente en ese baseline; no hay deltas que reportar).
- **Entidad Java:** `FalDiaNoComputable`. **Puertos:** `DiaNoComputableRepository` / `InMemoryDiaNoComputableRepository`. **Fuente normativa:** `../10-domain/calendario-plazos-administrativos.md`.
- **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA / RECONCILIADA_R3.

### Tablas de infraestructura no persistidas por el dominio Faltas

`stor_backend`, `stor_politica`, `stor_objeto`: infraestructura de storage
técnico; `NO_PERSISTIBLE` desde el dominio Faltas (el `storage_key` vive en las
entidades de dominio que lo requieren). Tablas geográficas `geo_*` (IGN, INDEC,
BAHRA, Malvinas): externas a Faltas, no administradas por este módulo.

## 3. Enums con relevancia de persistencia

No todo enum de dominio con relevancia de persistencia expone un código
explícito. Se distinguen tres categorías (ver `DECISION_DDL-ENUM-01` en
`ddl-decisions.md` y `jdbc-strategy.md`, sección de enums):

- `EXPLICIT_NUMERIC_CODE`: el enum expone `codigo()` numérico (`short`).
  Persistencia candidata: `SMALLINT`.
- `EXPLICIT_STRING_CODE`: el enum expone `codigo()` de tipo `String` estable.
  Persistencia candidata: `CHAR`/`VARCHAR` exacta con constraint.
- `NO_EXPLICIT_CODE`: el enum no expone `codigo()`. Prohibido `ordinal()`.
  Prohibido `name()` sin decisión explícita. **0 enums en esta categoría** tras el
  cierre de `DECISION_DDL-ENUM-01`.

| Enum | Categoría | Notas |
|---|---|---|
| `TipoActa` (`TRANSITO=1, CONTRAVENCION=2, SUSTANCIAS_ALIMENTICIAS=3, COMERCIO=4`) | `EXPLICIT_NUMERIC_CODE` | — |
| `EstadoRedaccionDocumento` (`BORRADOR=1, CONFIRMADA=2, ANULADA=3`) | `EXPLICIT_NUMERIC_CODE` | — |
| `OrigenFirmezaCondena` (`VENCIMIENTO_PLAZO_APELACION=1, APELACION_RECHAZADA=2`) | `EXPLICIT_NUMERIC_CODE` | Persistido inline en `fal_acta_fallo.origen_firmeza` |
| `ResultadoFinalActa` (10 valores, 0–9) | `EXPLICIT_NUMERIC_CODE` | Ver sección 4 (Decisión P1); código 5 (`FALLO_CONDENATORIO_PAGADO`) es LEGACY_RESERVED, ver `DECISION_DDL-RF-005` |
| `ActorTipoEvento` (1–6) | `EXPLICIT_NUMERIC_CODE` | `fal_acta_evento.actor_tipo` |
| `OrigenEvento` (1–8) | `EXPLICIT_NUMERIC_CODE` | `fal_acta_evento.origen_evt` |
| `EstadoFalloActa` (`PENDIENTE_FIRMA=1, PENDIENTE_NOTIFICACION=2, NOTIFICADO=3, FIRME=4, REEMPLAZADO=5, SIN_EFECTO=6`) | `EXPLICIT_NUMERIC_CODE` | `DECISION_DDL-ENUM-01` CERRADA; `codigo()` expuesto en enum Java |
| `EstadoApelacionActa` (`PRESENTADA=1, EN_ANALISIS=2, RECHAZADA=3, ACEPTADA_ABSUELVE=4, RESUELTA=5, SIN_EFECTO=6`) | `EXPLICIT_NUMERIC_CODE` | `DECISION_DDL-ENUM-01` CERRADA; `codigo()` expuesto en enum Java |
| `EstadoPagoCondena` (`NO_APLICA=1, PENDIENTE=2, INFORMADO=3, CONFIRMADO=4, OBSERVADO=5`) | `EXPLICIT_NUMERIC_CODE` | `DECISION_DDL-ENUM-01` CERRADA; `codigo()` expuesto en enum Java |
| `TipoDiaNoComputable` (`FERIADO=1, ASUETO_ADMINISTRATIVO=2, OTRO=3`) | `EXPLICIT_NUMERIC_CODE` | `DECISION_DDL-ENUM-01` CERRADA; `codigo()` expuesto en enum Java |
| `OrigenDiaNoComputable` (`MANUAL=1, SINCRONIZACION_EXTERNA=2`) | `EXPLICIT_NUMERIC_CODE` | `DECISION_DDL-ENUM-01` CERRADA; `codigo()` expuesto en enum Java |
| `ResultadoResolucionApelacion` y demás enums funcionales no listados arriba | Ver `../10-domain/states-events-catalogs.md` y `../10-domain/lifecycle-states.md` | Fuente normativa de valores; este documento no repite la tabla ni asume categoría sin verificar el enum Java |
| `BloqueActual`, `TipoEventoActa`, `EstadoProcesalActa`, `SituacionAdministrativaActa` | `EXPLICIT_STRING_CODE` | Código `String` corto (`CHAR(4/6)`); patrón correcto para estos cuatro; no aplican `SMALLINT` |

## 4. Decisiones físicas ya cerradas

No reabrir. Referencia compacta; el detalle de cada decisión permanece en Git.

| # | Decisión | Descripción | Estado |
|---|---|---|---|
| D1 | Firmeza | Firmeza inline en `fal_acta_fallo` (`fh_firmeza`, `si_firme`, `origen_firmeza SMALLINT NULL`); sin tabla propia | CERRADA |
| D2 | Pagos | `FalPagoVoluntario` + `FalPagoCondena` -> `fal_acta_obligacion_pago` con `tipo_obligacion` | CERRADA |
| D3 | Paralización | `FalActaParalizacion` implementada en InMemory | CERRADA |
| D4 | JSON | Campos JSON documentales -> JSON nativo MariaDB | CERRADA |
| D5 | Fundamentos fallo | `FalActaFallo.fundamentos` -> `TEXT NULL` directo en `fal_acta_fallo` | CERRADA |
| D6 | Persona | `FalPersona` + `FalPersonaDomicilio` implementadas en InMemory | CERRADA |
| D7 | Notificación PK | `FalNotificacion.id` -> `BIGINT AUTO_INCREMENT` sin UUID | CERRADA |
| D8 | TipoActa | `FalActa.tipoActa` -> enum `TipoActa` con código `SMALLINT` | CERRADA |
| D9 | Prioridad default | `fal_documento_plantilla_default.prioridad` -> `SMALLINT` | CERRADA |
| D10 | Resolución de pago anterior | NO existe `fal_acta_pago_resolucion`; ver sección "Pagos" de este documento (confirmación explícita contra el código Java vigente) | CERRADA |
| P1 | `ResultadoFinalActa` | Columna `resultado_final SMALLINT NOT NULL`; 10 valores vigentes (0–9); ver sección siguiente | CERRADA |
| P2 | Firmeza (identidad) | `FalActaFirmezaCondena` y `FirmezaCondenaRepository` eliminados del código Java vigente; firmeza inline en `FalActaFallo` | CERRADA |

### Decisión P1 — `ResultadoFinalActa`: columna SMALLINT con código explícito

**Decisión vigente:** columna MariaDB `resultado_final SMALLINT NOT NULL`; enum
Java con código explícito. No usar ordinal Java, `name()`, `VARCHAR` ni `CHAR`.

**Valores vigentes (10; fuente normativa: `00-governance/glossary.md` y
`../10-domain/lifecycle-states.md`):**

| Código | Valor |
|---:|---|
| 0 | `SIN_RESULTADO_FINAL` |
| 1 | `PAGO_VOLUNTARIO_PAGADO` |
| 2 | `ABSUELTO` |
| 3 | `CONDENA_FIRME` |
| 4 | `CONDENA_FIRME_PAGADA` |
| 5 | `FALLO_CONDENATORIO_PAGADO` (LEGACY_RESERVED; ver `DECISION_DDL-RF-005`) |
| 6 | `FALLO_CONDENATORIO_GESTION_EXTERNA` |
| 7 | `PRESCRIPTO` |
| 8 | `ANULADO` |
| 9 | `NULIDAD` |

**Valor histórico eliminado (no reintroducir):** `PAGO_VOLUNTARIO_CONFIRMADO`
(reemplazado por `PAGO_VOLUNTARIO_PAGADO`).

### Decisión P2 — Firmeza de condena: identidad y persistencia

**Decisión vigente:** la firmeza pertenece al fallo concreto y se persiste en
`fal_acta_fallo` (`si_firme`, `fh_firmeza`, `origen_firmeza`); proyectada en
`FalActaSnapshot`. No hay una segunda fuente de verdad en `FalActa`.
`FalActaFirmezaCondena` y `FirmezaCondenaRepository` están eliminados del
código Java vigente (verificado contra el árbol de fuentes de este HEAD).

## 5. Cierre

- Bloqueadores funcionales internos: ninguno.
- Decisiones físicas abiertas: 0. Ver `ddl-decisions.md`; ninguna reabre
  dominio, contratos de comando, eventos o estados.
- Integraciones externas (Ingresos/Tesorería, Informix, geo, IDP): no bloquean
  el diseño inicial de DDL; ver `../90-roadmap/current-roadmap.md`.
- Gate formal `READY_FOR_DDL`: ver `../00-governance/ready-for-ddl-gate.md`.
