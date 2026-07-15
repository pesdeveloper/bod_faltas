# 110 — Matriz vigente de paridad InMemory / MariaDB (entrada a DDL/JDBC)

> **Estado documental:** PRE_DDL_PLAN
> **Autoridad DDL:** SUPPORTING
> Esta matriz no define reglas funcionales de dominio. Ante contradiccion con la
> spec normativa (`00-governance/`, `10-domain/`, `02-estados-bloques-eventos.md`,
> `03-comandos-precondiciones-efectos.md`, `04-snapshot-bandejas-acciones.md`,
> `20-application/fallo-command-contracts.md`), esa spec normativa prevalece.
> Este documento no reemplaza a `109-delta-modelo-mariadb-inmemory.md` ni es
> reemplazado por el: `109` describe los deltas fisicos transversales; `110` es
> la matriz vigente por agregado/puerto para el diseno de DDL/JDBC. Ninguno de
> los dos es fuente unica de verdad ni sustituye al otro; son complementarios.
> No contiene SQL/DDL definitivo ni nombres fisicos aprobados mas alla de los ya
> usados en la spec vigente. La historia de como se llego a este estado (fases,
> conteos de tests historicos, diarios de slice) permanece en Git.

---

## 0. Rol de este documento

- No define reglas funcionales; las reglas de dominio viven en `00-governance/`,
  `10-domain/`, `02`, `03`, `04` y `20-application/fallo-command-contracts.md`.
- No reemplaza a `109`; es complementario.
- No es la fuente unica de verdad del proyecto; los documentos normativos
  prevalecen ante cualquier contradiccion.
- Su unico proposito es servir de entrada compacta y verificable al diseno de
  DDL/JDBC: que agregados existen, con que identidad, que decisiones fisicas
  siguen abiertas.

---

## 1. Estado de readiness

| Aspecto | Estado |
|---|---|
| Contrato funcional (dominio, comandos, eventos, estados) | CERRADO |
| Modelo conceptual (agregados, entidades Java, puertos) | LISTO_PARA_DDL |
| Decisiones fisicas pendientes | Ver seccion 5 (`DECISION_DDL-*`) |

No se reporta un resumen numerico agregado de "paridad" derivado manualmente:
cada decision fisica pendiente esta identificada individualmente en la seccion
6 con su propio criterio de cierre.

---

## 2. Inventario canonico de tablas MariaDB objetivo (modelo logico)

Este inventario describe el modelo logico de tablas que la spec de dominio ya
supone (nombres, campos, FKs conceptuales). No es DDL ejecutable.

### Dependencias

#### `fal_dependencia`
- **Proposito:** unidades administrativas (organismos/dependencias).
- **PK:** `id BIGINT AUTO_INCREMENT`.
- **Campos:** `codigo_dependencia VARCHAR(20)`, `nombre VARCHAR(200)`, `tipo_acta SMALLINT`, `si_activo BOOLEAN`.
- **Auditoria:** `fh_alta`, `id_user_alta`.
- **Entidad Java:** `FalDependencia`. **Puerto:** `DependenciaRepository` / `InMemoryDependenciaRepository`.
- **Estado:** LISTO_PARA_DDL.

#### `fal_dependencia_version`
- **Proposito:** versiones de la dependencia con datos cambiantes.
- **PK:** `id_dependencia + version_dep` (compuesta). **FK:** `id_dependencia -> fal_dependencia`.
- **Campos:** `nombre_version`, `tipo_acta SMALLINT`, `si_activo`, `fh_vig_desde`, `fh_vig_hasta`.
- **Entidad Java:** `FalDependenciaVersion`. **Estado:** LISTO_PARA_DDL.

### Inspectores

#### `fal_inspector`
- **Proposito:** inspectores/agentes del organismo.
- **PK:** `id BIGINT AUTO_INCREMENT`. **Campos:** `legajo VARCHAR(20)`, `apellido`, `nombre`, `si_activo BOOLEAN`.
- **Entidad Java:** `FalInspector`. **Puerto:** `InspectorRepository` / `InMemoryInspectorRepository`. **Estado:** LISTO_PARA_DDL.

#### `fal_inspector_version`
- **Proposito:** version del inspector con datos del periodo.
- **PK:** `id_inspector + version_insp` (compuesta). **FK:** `id_inspector -> fal_inspector`.
- **Entidad Java:** `FalInspectorVersion`. **Estado:** LISTO_PARA_DDL.

### Firmantes

#### `fal_firmante`
- **PK:** `id BIGINT AUTO_INCREMENT`. **Entidad Java:** `FalFirmante`. **Puerto:** `FirmanteRepository` / `InMemoryFirmanteRepository`. **Estado:** LISTO_PARA_DDL.

#### `fal_firmante_version`
- **PK:** `id_firmante + ver_firmante` (compuesta). **FK:** `id_firmante -> fal_firmante`. **Entidad Java:** `FalFirmanteVersion`. **Estado:** LISTO_PARA_DDL.

#### `fal_firmante_version_habilitacion`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_firmante + ver_firmante -> fal_firmante_version`.
- **Campos:** `tipo_docu SMALLINT`, `rol_firma_req SMALLINT`, `mecanismo_firma_req SMALLINT`.
- **Entidad Java:** `FalFirmanteVersionHabilitacion`. **Estado:** LISTO_PARA_DDL.

### Personas y domicilios

#### `fal_persona`
- **Proposito:** maestro de personas (infractores, presentantes).
- **PK:** `id BIGINT AUTO_INCREMENT`. **Campos:** `nro_documento VARCHAR(20)`, `tipo_documento SMALLINT`, `nombre_completo VARCHAR(300)`, `fh_nacimiento DATE NULL`, `sexo SMALLINT NULL`, `si_identificado BOOLEAN`, `si_extranjero BOOLEAN`, `id_ign BIGINT NULL`, `id_indec BIGINT NULL`, `id_local BIGINT NULL`.
- **Auditoria:** `fh_alta`, `id_user_alta`, `fh_mod`, `id_user_mod`.
- **Entidad Java:** `FalPersona`. **Puerto:** `PersonaRepository` / `InMemoryPersonaRepository`.
- **Estado:** MODELO_CONCEPTUAL_CERRADO; DECISION_DDL_PENDIENTE (`DECISION_DDL-PERS-01`, unicidad fisica).
- **Clave natural candidata:** `(tipo_documento, nro_documento)` para personas identificadas; `DECISION_DDL-PERS-01` (ver seccion 5) sobre unicidad fisica.

#### `fal_persona_domicilio`
- **Proposito:** domicilios de personas. **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_persona -> fal_persona`.
- **Campos:** `tipo_domicilio SMALLINT`, `id_calle_version BIGINT NULL FK`, `nro_puerta VARCHAR(10) NULL`, `piso VARCHAR(5) NULL`, `dpto VARCHAR(5) NULL`, `localidad_id BIGINT NULL`, `texto_libre VARCHAR(500) NULL`, `si_principal TINYINT(1)`, `id_tca BIGINT NULL`, `id_loc BIGINT NULL`.
- **Entidad Java:** `FalPersonaDomicilio`. **Puerto:** `PersonaDomicilioRepository` / `InMemoryPersonaDomicilioRepository`. **Estado:** LISTO_PARA_DDL.

### Acta core

#### `fal_acta`
- **Proposito:** entidad central del expediente de faltas.
- **PK:** `id BIGINT AUTO_INCREMENT`. **UK:** `uuid_tecnico CHAR(36)`.
- **FK conceptuales:** `id_talonario -> num_talonario`, `id_inspector + ver_inspector -> fal_inspector_version`, `id_dependencia + ver_dependencia -> fal_dependencia_version`, `id_persona -> fal_persona`.
- **Campos:** `nro_acta VARCHAR(20) NULL`, `tipo_acta SMALLINT NOT NULL`, `id_talonario BIGINT NULL`, `nro_talonario_usado INT NULL`, `fecha_acta DATE NOT NULL`, `fecha_labrado DATETIME(6) NOT NULL`, `domicilio_hecho VARCHAR(500) NULL`, `lat_infr DOUBLE NULL`, `lon_infr DOUBLE NULL`, `resultado_firma_infractor SMALLINT NOT NULL`, `bloque_actual SMALLINT NOT NULL`, `estado_procesal SMALLINT NOT NULL`, `situacion_administrativa SMALLINT NOT NULL`, `resultado_final SMALLINT NOT NULL`, `id_persona BIGINT NULL FK`.
- **Auditoria:** `fh_alta`, `id_user_alta`, `fh_mod`, `id_user_mod`.
- **Entidad Java:** `FalActa` (id `Long`). **Puerto:** `ActaRepository` / `InMemoryActaRepository`.
- **versionRow/OCC:** SI (`versionRow` en `FalActa`).
- **Orden deterministico:** no aplica (entidad unica por expediente).
- **Estado:** LISTO_PARA_DDL.

### Eventos, snapshot, evidencias y observaciones

#### `fal_acta_evento`
- **Proposito:** log append-only de eventos del expediente.
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_acta -> fal_acta`.
- **Campos:** `tipo_evt SMALLINT NOT NULL`, `fecha_evento DATETIME(6)`, `orden_logico INT`, `id_documento BIGINT NULL`, `id_notificacion BIGINT NULL`, `id_operador CHAR(36)`, `descripcion TEXT NULL`, `payload JSON NULL`.
- **Entidad Java:** `FalActaEvento` (id `Long`). **Puerto:** `ActaEventoRepository` / `InMemoryActaEventoRepository`.
- **Orden deterministico:** por `id` autoincremental (desempate de `fecha_evento` igual); append-only, nunca se actualiza ni borra.
- **Estado:** LISTO_PARA_DDL.

#### `fal_acta_snapshot`
- **Proposito:** proyeccion operativa derivada del expediente (1:1 con acta), regenerable.
- **PK:** `id_acta BIGINT FK`. **Entidad Java:** `FalActaSnapshot` (`idActa Long`). **Puerto:** `ActaSnapshotRepository` / `InMemoryActaSnapshotRepository`. **Estado:** LISTO_PARA_DDL.

#### `fal_acta_evidencia`
- **PK:** `id BIGINT AUTO_INCREMENT`. **Entidad Java:** `FalActaEvidencia`. **Puerto:** `ActaEvidenciaRepository` / `InMemoryActaEvidenciaRepository`. **Estado:** LISTO_PARA_DDL.

#### `fal_observacion`
- **Proposito:** observaciones tipificadas por entidad.
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_acta -> fal_acta`.
- **Campos:** `entidad_tipo SMALLINT`, `entidad_id BIGINT`, `texto TEXT`, `id_user_alta`, `fh_alta`.
- **Entidad Java:** `FalObservacion`. **Puerto:** `ObservacionRepository` / `InMemoryObservacionRepository`. **Enum:** `EntidadTipoObservada` (22 codigos). **Estado:** LISTO_PARA_DDL.

### Normativa

#### `fal_dependencia_normativa`
- **Entidad Java:** `FalDependenciaNormativa`. **Estado:** LISTO_PARA_DDL.

#### `fal_normativa_faltas`
- **Entidad Java:** `FalNormativaFaltas`. **Puerto:** `NormativaRepository` / `InMemoryNormativaRepository`. **Estado:** LISTO_PARA_DDL.

#### `fal_articulo_normativa_faltas`
- **Entidad Java:** `FalArticuloNormativaFaltas`. **Estado:** LISTO_PARA_DDL.

#### `fal_tarifario_unidad_faltas`
- **Proposito:** valores unitarios por articulo para calculo de valorizacion.
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_articulo -> fal_articulo_normativa_faltas`.
- **Campos:** `fh_vig_desde DATE`, `fh_vig_hasta DATE NULL`, `valor_unitario DECIMAL(14,4)`.
- **Entidad Java:** `FalTarifarioUnidadFaltas`. **Puerto:** `TarifarioUnidadFaltasRepository` / `InMemoryTarifarioUnidadFaltasRepository`.
- **Unicidad fisica requerida:** sin solapamiento de rangos de vigencia activos por articulo (invariante ya aplicado en InMemory; DDL debe reforzarlo con constraint/verificacion transaccional).
- **Estado:** LISTO_PARA_DDL.

#### `fal_medida_preventiva`
- **Entidad Java:** `FalMedidaPreventiva` (version atomica; una activa por codigo). **Estado:** LISTO_PARA_DDL.

#### `fal_articulo_medida_preventiva`
- **Entidad Java:** `FalArticuloMedidaPreventiva` (PK compuesta via `ArticuloMedidaPreventivaId`; sin reactivacion silenciosa). **Estado:** LISTO_PARA_DDL.

### Articulos infringidos y valorizacion

#### `fal_acta_articulo_infringido`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_acta -> fal_acta`, `id_articulo -> fal_articulo_normativa_faltas`.
- **Unicidad fisica requerida:** UK activo `(id_acta, id_articulo)`.
- **Entidad Java:** `FalActaArticuloInfringido`. **Estado:** LISTO_PARA_DDL.

#### `fal_acta_valorizacion`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_acta -> fal_acta`.
- **Campos:** `monto_total DECIMAL(14,2)`, `fh_calculo DATETIME(6)`, `si_vigente BOOLEAN`.
- **Entidad Java:** `FalActaValorizacion` (`versionRow` presente). **Operacion atomica:** `confirmarVigenteAtomico` garantiza una sola valorizacion vigente CONFIRMADA por acta+tipo. **Estado:** LISTO_PARA_DDL.

#### `fal_acta_valorizacion_item`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_valorizacion -> fal_acta_valorizacion`.
- **Entidad Java:** `FalActaValorizacionItem` (inmutable post-confirmacion del padre). **Estado:** LISTO_PARA_DDL.

### Satelites del acta

Todas con PK `id_acta BIGINT` (1:1) o `id BIGINT AUTO_INCREMENT` segun el caso;
FK `id_acta -> fal_acta`. Un unico satelite valido por acta segun `tipo_acta`
(guardrail de dominio, no de esta matriz).

| Tabla | Entidad Java | Estado |
|---|---|---|
| `fal_acta_transito` | `FalActaTransito` | LISTO_PARA_DDL |
| `fal_acta_transito_alcoholemia` | `FalActaTransitoAlcoholemia` | LISTO_PARA_DDL |
| `fal_acta_vehiculo` | `FalActaVehiculo` | LISTO_PARA_DDL |
| `fal_acta_contravencion` | `FalActaContravencion` | LISTO_PARA_DDL |
| `fal_acta_sustancias_alimenticias` | `FalActaSustanciasAlimenticias` | LISTO_PARA_DDL |
| `fal_acta_medida_preventiva` | `FalActaMedidaPreventiva` | LISTO_PARA_DDL |

### Bloqueantes

#### `fal_acta_bloqueante_cierre_material`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_acta -> fal_acta`.
- **Campos:** `origen SMALLINT`, `estado SMALLINT`, `si_activo BOOLEAN`, `descripcion TEXT NULL`, `fh_alta DATETIME(6)`, `fh_cierre DATETIME(6) NULL`.
- **Entidad Java:** `FalBloqueanteMaterial` (id `Long`). **Puerto:** `BloqueanteMaterialRepository` / `InMemoryBloqueanteMaterialRepository`.
- **Indice de consulta:** `(id_acta, si_activo)` — usado por `existsActivoByActaId`.
- **versionRow/OCC:** no implementado aun en InMemory (`DECISION_DDL-BLOQ-01`, seccion 5).
- **Estado:** MODELO_CONCEPTUAL_CERRADO (identidad); DECISION_DDL_PENDIENTE (`DECISION_DDL-BLOQ-01`, OCC).

### Documentos, plantillas y firma

#### `fal_documento`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_acta -> fal_acta`, `id_plantilla -> fal_documento_plantilla`.
- **Campos:** `tipo_docu SMALLINT`, `nro_docu VARCHAR(30) NULL`, `estado_docu SMALLINT`, `storage_key VARCHAR(500) NULL`, `hash_docu VARCHAR(128) NULL`, `tipo_firma_req SMALLINT`, `fh_alta`, `id_user_alta`.
- **Entidad Java:** `FalDocumento`. **Puerto:** `DocumentoRepository` / `InMemoryDocumentoRepository`.
- **versionRow/OCC:** requerido para el endpoint de numeracion (`fal_documento.version_row`, ver `10-domain/firma-notificacion-fallo.md` y `03-comandos-precondiciones-efectos.md`); no implementado aun en InMemory (`DECISION_DDL-DOC-01`, seccion 5).
- **Estado:** MODELO_CONCEPTUAL_CERRADO (campos); DECISION_DDL_PENDIENTE (`DECISION_DDL-DOC-01`, OCC).

#### `fal_acta_documento`
- **Proposito:** tabla pivot relacion canonica acta-documento (pertenencia funcional).
- **PK:** `id_acta + id_documento` (compuesta, sin ID artificial).
- **Campos:** `rol_docu_acta SMALLINT`, `si_principal BOOLEAN`, `fh_alta DATETIME(6)`, `id_user_alta CHAR(36)`.
- **Entidad Java:** `FalActaDocumento` + `ActaDocumentoId` (value object). **Puerto:** `ActaDocumentoRepository` / `InMemoryActaDocumentoRepository` (thread-safe, `principalLock`).
- **Servicio:** `ActaDocumentoService`. **Enum:** `RolDocuActa` (12 valores, codigo `short` estable).
- **Snapshot:** `FalActaSnapshot.idDocuUlt` derivado via pivot (`FALLO > RESOLUCION > NOTIFICACION > ACTA_PRINCIPAL`).
- **Estado:** LISTO_PARA_DDL.

#### `fal_documento_firma`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_documento -> fal_documento`, `id_firmante + ver_firmante -> fal_firmante_version`.
- **Campos:** `estado_firma SMALLINT`, `seq_firma_req SMALLINT`, `tipo_firma SMALLINT`, `storage_key VARCHAR(500) NULL`, `hash_firma VARCHAR(128) NULL`, `fh_firma DATETIME(6) NULL`.
- **Entidad Java:** `FalDocumentoFirma`. **Idempotencia:** `referenciaFirmaExt` (ver `10-domain/firma-notificacion-fallo.md`). **Estado:** LISTO_PARA_DDL.

#### `fal_documento_firma_req`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_documento -> fal_documento`. **Entidad Java:** `FalDocumentoFirmaReq`. **Estado:** LISTO_PARA_DDL.

#### `fal_documento_plantilla`
- **PK:** `id BIGINT AUTO_INCREMENT`.
- **Campos:** `codigo VARCHAR(50)`, `nombre VARCHAR(200)`, `tipo_docu SMALLINT`, `accion_documental SMALLINT`, `tipo_acta SMALLINT NULL`, `tipo_firma_req SMALLINT`, `si_requiere_numeracion BOOLEAN`, `momento_numeracion_docu SMALLINT`, `si_notificable BOOLEAN`, `si_genera_pdf BOOLEAN`, `si_seleccionable BOOLEAN`, `si_activa BOOLEAN`, `fh_vig_desde DATE`, `fh_vig_hasta DATE NULL`, `fh_alta DATETIME(6)`, `id_user_alta CHAR(36)`.
- **Entidad Java:** `FalDocumentoPlantilla`. **Estado:** LISTO_PARA_DDL.

#### `fal_documento_plantilla_firma_req`
- **PK:** `id_plantilla + seq_firma_req` (compuesta). **Entidad Java:** `FalDocumentoPlantillaFirmaReq`. **Estado:** LISTO_PARA_DDL.

#### `fal_documento_plantilla_contenido`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_plantilla -> fal_documento_plantilla`.
- **Campos:** `version_contenido SMALLINT NOT NULL`, `titulo VARCHAR(200) NOT NULL`, `cuerpo_markdown TEXT NOT NULL`, `encabezado_markdown TEXT NULL`, `pie_markdown TEXT NULL`, `variables_declaradas_json JSON NOT NULL DEFAULT '[]'`, `si_activo BOOLEAN NOT NULL`, `fh_vig_desde DATETIME(6) NOT NULL`, `fh_vig_hasta DATETIME(6) NULL`, `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Entidad Java:** `FalDocumentoPlantillaContenido`. **Estado:** LISTO_PARA_DDL.

#### `fal_documento_plantilla_default`
- **PK:** `id BIGINT AUTO_INCREMENT`. **Indice de consulta:** `(accion_documental, tipo_acta, id_dependencia)`.
- **Campos:** `accion_documental SMALLINT NOT NULL`, `tipo_acta SMALLINT NULL` (NULL = generico), `tipo_docu SMALLINT NOT NULL`, `id_dependencia BIGINT NULL`, `ver_dependencia SMALLINT NULL`, `id_plantilla BIGINT NOT NULL FK`, `prioridad SMALLINT NOT NULL`, `fh_vig_desde DATETIME(6) NOT NULL`, `fh_vig_hasta DATETIME(6) NULL`, `si_activo BOOLEAN NOT NULL`, `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Entidad Java:** `FalDocumentoPlantillaDefault`. **Estado:** LISTO_PARA_DDL.

#### `fal_documento_redaccion`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_documento -> fal_documento`, `id_plantilla_contenido -> fal_documento_plantilla_contenido`, `redaccion_origen_id -> fal_documento_redaccion` (self). **UK:** `id_documento + nro_revision`.
- **Campos:** `nro_revision SMALLINT NOT NULL`, `redaccion_origen_id BIGINT NULL`, `estado_redaccion SMALLINT NOT NULL` (`BORRADOR=1, CONFIRMADA=2, ANULADA=3`), `contenido_base_markdown TEXT NOT NULL`, `contenido_editable_markdown TEXT NOT NULL`, `variables_snapshot_json JSON NOT NULL DEFAULT '{}'`, `variables_faltantes_json JSON NOT NULL DEFAULT '[]'`, `diagnostico_json JSON NOT NULL DEFAULT '{}'`, `recursos_snapshot_json JSON NULL`, campos de auditoria por hito (creacion/regeneracion/edicion/confirmacion/anulacion).
- **Entidad Java:** `FalDocumentoRedaccion`. **Servicios:** `DocumentoCombinacionService`, `DocumentoVariableContextBuilder`, `DocumentoVariableRegistry`, `DocumentoRedaccionService`. **Estado:** LISTO_PARA_DDL.

### Notificaciones

#### `fal_notificacion`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_acta -> fal_acta`, `id_documento -> fal_documento`.
- **Campos:** `tipo_docu_notificado SMALLINT`, `canal VARCHAR(50)`, `fecha_envio DATETIME(6)`, `estado SMALLINT`, `resultado SMALLINT NULL`, `fecha_resultado DATETIME(6) NULL`, `intentos INT`, `observaciones TEXT NULL`.
- **Entidad Java:** `FalNotificacion` (id `Long`). **Puerto:** `NotificacionRepository` / `InMemoryNotificacionRepository`.
- **versionRow/OCC:** no implementado aun en InMemory (`DECISION_DDL-NOTI-01`, seccion 5).
- **Estado:** MODELO_CONCEPTUAL_CERRADO (identidad y campos); DECISION_DDL_PENDIENTE (`DECISION_DDL-NOTI-01`, OCC).

#### `fal_notificacion_intento`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_notificacion -> fal_notificacion`.
- **Campos:** `nro_intento SMALLINT`, `canal VARCHAR(50)`, `fecha_intento DATETIME(6)`, `resultado SMALLINT`, `detalle TEXT NULL`.
- **Entidad Java:** `FalNotificacionIntento`. **Estado:** LISTO_PARA_DDL.

#### `fal_notificacion_acuse`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_notificacion -> fal_notificacion`. **Entidad Java:** `FalNotificacionAcuse`. **Estado:** LISTO_PARA_DDL.

#### `fal_lote_correo`
- **PK:** `id BIGINT AUTO_INCREMENT`. **Campos:** `tipo_lote SMALLINT`, `fecha_generacion DATETIME(6)`, `estado_lote SMALLINT`, `id_user_alta CHAR(36)`.
- **Entidad Java:** `FalLoteCorreo`. **Puerto:** `LoteCorreoRepository` / `InMemoryLoteCorreoRepository`.
- **Clave de idempotencia:** `loteCodigo` (unicidad fisica requerida, ver `109`, seccion 3). **Estado:** LISTO_PARA_DDL.

### Fallo y firmeza

#### `fal_acta_fallo`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK conceptuales:** `id_acta -> fal_acta`, `valorizacion_id -> fal_acta_valorizacion`, `fallo_reemplazado_id -> fal_acta_fallo` (self), `documento_id -> fal_documento`.
- **Campos:** `tipo_fallo SMALLINT NOT NULL`, `estado_fallo` (tipo pendiente: representacion: `DECISION_DDL-ENUM-01`; `EstadoFalloActa` es `NO_EXPLICIT_CODE`) `NOT NULL`, `monto_condena DECIMAL(14,2) NULL`, `resultado_fallo SMALLINT NULL`, `fundamentos TEXT NULL`, `documento_id BIGINT NULL`, `valorizacion_id BIGINT NULL`, `fallo_reemplazado_id BIGINT NULL`, `fecha_dictado DATETIME(6) NOT NULL`, `fecha_notificacion DATETIME(6) NULL`, `fecha_resultado_final DATETIME(6) NULL`, `fh_firma DATETIME(6) NULL`, `fh_vto_apelacion DATETIME(6) NULL`, `si_activo BOOLEAN NOT NULL`, `si_firme BOOLEAN NOT NULL DEFAULT FALSE`, `fh_firmeza DATETIME(6) NULL`, `origen_firmeza SMALLINT NULL` (`1=VENCIMIENTO_PLAZO_APELACION, 2=APELACION_RECHAZADA`), `version_row INT NOT NULL DEFAULT 0`.
- **Auditoria:** `fh_alta`, `id_user_alta`, `fh_mod`, `id_user_mod`.
- **Entidad Java:** `FalActaFallo` (id `Long`, `versionRow` presente). **Puerto:** `FalloActaRepository` / `InMemoryFalloActaRepository`.
- **Estado:** MODELO_CONCEPTUAL_CERRADO; DECISION_DDL_PENDIENTE (`DECISION_DDL-ENUM-01`, columna `estado_fallo`).

> **Nota de firmeza:** la firmeza de condena es inline en `fal_acta_fallo`
> (`si_firme`, `fh_firmeza`, `origen_firmeza`). No existe una tabla propia de
> firmeza.
> - `FalActaFirmezaCondena` esta eliminada del codigo Java vigente (no existe en `domain/model`; verificado contra el arbol de fuentes de este HEAD).
> - `FirmezaCondenaRepository` esta eliminado del codigo Java vigente (no existe en `repository/`; verificado contra el arbol de fuentes de este HEAD).
>
> Ver `20-application/fallo-command-contracts.md` (CMD-FALLO-005/006) para el
> contrato de firmeza vigente.

### Apelacion

#### `fal_acta_apelacion`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK conceptuales:** `id_acta -> fal_acta`, `fallo_id -> fal_acta_fallo`, `documento_resolucion_id -> fal_documento`.
- **Campos:** `fallo_id BIGINT NOT NULL`, `estado_apelacion` (tipo pendiente: representacion: `DECISION_DDL-ENUM-01`; `EstadoApelacionActa` es `NO_EXPLICIT_CODE`) `NOT NULL`, `fecha_presentacion DATETIME(6) NOT NULL`, `canal_apelacion SMALLINT NOT NULL`, `tipo_presentacion SMALLINT NOT NULL`, `texto_apelacion TEXT NULL`, `presentante VARCHAR(300) NULL`, `fundamentos TEXT NULL`, `observaciones TEXT NULL`, `si_activa BOOLEAN NOT NULL`, `fecha_resolucion DATETIME(6) NULL`, `fundamentos_resolucion TEXT NULL`, `observaciones_resolucion TEXT NULL`, `documento_resolucion_id BIGINT NULL`, `version_row INT NOT NULL DEFAULT 0`.
- **Auditoria:** `fh_alta`, `id_user_alta`, `fh_mod`, `id_user_mod`.
- **Entidad Java:** `FalActaApelacion` (id `Long`, `falloId Long`, `versionRow` presente). **Puerto:** `ApelacionActaRepository` / `InMemoryApelacionActaRepository`.
- **Nota de consulta (CMD-FALLO-006):** la apelacion relevante para declarar firmeza por apelacion rechazada se busca por `fallo_id`, no por "ultima apelacion del acta"; una apelacion historica de otro fallo no debe bloquear ni satisfacer la precondicion (ver `03-comandos-precondiciones-efectos.md`).
- **Estado:** MODELO_CONCEPTUAL_CERRADO; DECISION_DDL_PENDIENTE (`DECISION_DDL-ENUM-01`, columna `estado_apelacion`).

#### `fal_acta_apelacion_documento`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_apelacion -> fal_acta_apelacion`, `id_documento -> fal_documento`.
- **Entidad Java:** `FalActaApelacionDocumento`. **Estado:** LISTO_PARA_DDL.

### Paralizacion

#### `fal_acta_paralizacion`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_acta -> fal_acta`.
- **Campos:** `motivo SMALLINT NOT NULL`, `descripcion TEXT NULL`, `id_user_inicio CHAR(36) NOT NULL`, `fh_inicio DATETIME(6) NOT NULL`, `id_user_cierre CHAR(36) NULL`, `fh_cierre DATETIME(6) NULL`, `motivo_cierre SMALLINT NULL`, `si_activa BOOLEAN NOT NULL`.
- **Entidad Java:** `FalActaParalizacion`. **Puerto:** `ActaParalizacionRepository` / `InMemoryActaParalizacionRepository`. **Servicio:** `ParalizacionActaService`. **Estado:** LISTO_PARA_DDL.

### Archivo

#### `fal_acta_archivo`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_acta -> fal_acta`.
- **Campos:** `motivo_archivo SMALLINT NOT NULL`, `descripcion TEXT NULL`, `id_user_archivo CHAR(36) NOT NULL`, `fh_archivo DATETIME(6) NOT NULL`, `id_user_reingreso CHAR(36) NULL`, `fh_reingreso DATETIME(6) NULL`, `motivo_reingreso SMALLINT NULL`, `si_archivado BOOLEAN NOT NULL`.
- **Entidad Java:** `FalActaArchivo`. **Puerto:** `ActaArchivoRepository` / `InMemoryActaArchivoRepository`. **Servicio:** `ArchivoActaService`. **Estado:** LISTO_PARA_DDL.
- Nota de dominio: archivo no equivale automaticamente a `CERRADA` (ver `01-reglas-dominio-faltas` / glossary).

### Gestion externa

#### `fal_acta_gestion_externa`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_acta -> fal_acta`.
- **Entidad Java:** `FalGestionExterna` (id `Long`). **Puerto:** `GestionExternaRepository` / `InMemoryGestionExternaRepository`.
- **Invariante de vigencia:** a lo sumo una `FalGestionExterna` con `si_activa = true` por acta.
- **versionRow/OCC:** no implementado aun en InMemory (`DECISION_DDL-GEXT-01`, seccion 5).
- **Estado:** MODELO_CONCEPTUAL_CERRADO (identidad y campos); DECISION_DDL_PENDIENTE (`DECISION_DDL-GEXT-01`, OCC).

### Pagos

#### `fal_acta_obligacion_pago`
- **Entidad Java:** `FalActaObligacionPago` (id `Long`, `versionRow` presente). **Puerto:** `ObligacionPagoRepository` / `InMemoryObligacionPagoRepository`.
- **Enums:** `TipoObligacionPago`, `EstadoObligacionPago`.
- **Modelo (decision D2 cerrada):** unifica pago voluntario (`tipo=1`) y pago de condena (`tipo=2`); `FalPagoVoluntario` y `FalPagoCondena` (id `String`) son vistas de aplicacion sobre esta tabla unificada, no entidades fisicas separadas.
- **Estado:** MODELO_CONCEPTUAL_CERRADO; DECISION_DDL_PENDIENTE (`DECISION_DDL-PAGO-01`, identidad fisica de las vistas de aplicacion).

#### `fal_acta_forma_pago`
- **Entidad Java:** `FalActaFormaPago` (`versionRow` presente). **Enums:** `TipoFormaPago`, `EstadoFormaPago`. **Estado:** LISTO_PARA_DDL.

#### `fal_acta_plan_pago_ref`
- **Entidad Java:** `FalActaPlanPagoRef` (`versionRow` presente). **Enums:** `EstadoPlanPago`, `MotivoPlanCaidoCalculado`, `MotivoAptitudIntimacion`. **Estado:** LISTO_PARA_DDL.

#### `fal_acta_pago_movimiento`
- **Entidad Java:** `FalActaPagoMovimiento`, append-only. **Puerto:** `PagoMovimientoRepository` / `InMemoryPagoMovimientoRepository`.
- **Enums:** `TipoMovimientoPago`, `OrigenMovimiento`, `ClasificacionPago`, `EstadoConciliacionActual`, `OrigenUltimaActualizacion`.
- **Invariante:** vinculo canonico unico `movimientoOrigenId` para reversos; append-only, sin edicion de movimientos historicos.
- **Estado:** LISTO_PARA_DDL.

#### `fal_acta_economia_proyeccion`
- **Campo clave:** `importe_aplicado_total DECIMAL(14,2) NOT NULL DEFAULT 0`.
- **Entidad Java:** `FalActaEconomiaProyeccion` (`versionRow` presente). **Puerto:** `EconomiaProyeccionRepository` / `InMemoryEconomiaProyeccionRepository`.
- **Servicios:** `EconomiaProyeccionRecalculador`, `ProcesoNocturnoEconomicoService`. **Estado:** LISTO_PARA_DDL.

### Talonarios y numeracion

| Tabla | Entidad Java | Estado |
|---|---|---|
| `num_politica` | `NumPolitica` | LISTO_PARA_DDL |
| `num_talonario` | `NumTalonario` (`versionRow` presente) | LISTO_PARA_DDL |
| `num_talonario_ambito` | `NumTalonarioAmbito` | LISTO_PARA_DDL |
| `num_talonario_inspector` | `NumTalonarioInspector` | LISTO_PARA_DDL |
| `num_talonario_movimiento` | `NumTalonarioMovimiento` | LISTO_PARA_DDL |

#### Paridad de concurrencia/idempotencia para numeracion documental

Equivalencia entre InMemory vigente y la garantia productiva requerida en el
adapter MariaDB para `POST /api/faltas/documentos/{id}/numerar`:

| Aspecto | InMemory vigente | MariaDB requerido |
|---|---|---|
| Exclusion | metodo de instancia `synchronized` en `DocumentoService.numerarDocumentoParaFirmas` (monitor `this`, no estatico) | `SEQUENCE` asociada al talonario + transaccion + OCC |
| Alcance | serializa llamadas concurrentes sobre la misma instancia de `DocumentoService`; no es mecanismo multiinstancia | `NEXT VALUE FOR <secuencia>`, sin dependencia de locks JVM |
| Unicidad | verificacion previa en memoria | constraint unica `(id_talonario, nro_talonario)` en `num_talonario_movimiento` |
| Idempotencia | `nroDocu != null` -> devuelve numero existente, `yaEstabaNumerado = true` | verificar `nro_docu IS NOT NULL` antes de consumir otra `SEQUENCE`; recargar tras conflicto OCC |
| Evidencia InMemory | `DocumentoNumeracionFirmasTest` (17 casos) | — |

### QR y portal

#### `fal_acta_qr_acceso`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_acta -> fal_acta`.
- **Campos:** `token_qr VARCHAR(200)`, `url_acceso VARCHAR(500)`, `fh_generacion DATETIME(6)`, `fh_vencimiento DATETIME(6) NULL`, `si_activo BOOLEAN`.
- **Entidad Java:** `FalActaQrAcceso`. **Puerto:** `QrAccesoRepository` / `InMemoryQrAccesoRepository`. **Servicio:** `QrActaService`. **Proteccion de token:** `AesGcmQrTokenProtector`.
- **Estado:** LISTO_PARA_DDL.

### Catalogos de dominio adicionales

| Tabla | Entidad Java | Puerto | Estado |
|---|---|---|---|
| `fal_vehiculo_marca` | `FalVehiculoMarca` | `VehiculoMarcaRepository` / `InMemoryVehiculoMarcaRepository` | LISTO_PARA_DDL |
| `fal_vehiculo_modelo` | `FalVehiculoModelo` | `VehiculoModeloRepository` / `InMemoryVehiculoModeloRepository` | LISTO_PARA_DDL |
| `fal_rubro_version` | `FalRubroVersion` (versionado: `row_hash`, `previous_row_hash`, `source_operation`, `close_operation`, `si_version_actual`, `valid_from`, `valid_to`, `synced_at`) | `RubroVersionRepository` / `InMemoryRubroVersionRepository` | LISTO_PARA_DDL |
| `fal_motivo_archivo` | `FalMotivoArchivo` | `MotivoArchivoRepository` / `InMemoryMotivoArchivoRepository` | LISTO_PARA_DDL |

`fal_rubro_version` es una tabla de dominio Faltas con sincronizacion externa
(Informix); no es infraestructura tecnica.

### Calendario y excepciones

- **Entidad Java:** `FalDiaNoComputable`. **Puerto:** `DiaNoComputableRepository` / `InMemoryDiaNoComputableRepository`.
- **Fuente normativa:** `10-domain/calendario-plazos-administrativos.md`.
- **Clave de idempotencia:** fecha (dia no computable activo); unicidad fisica requerida sobre el subconjunto "activo" (ver `109`, seccion 3).
- **Estado:** LISTO_PARA_DDL.

### Tablas de infraestructura no persistidas por el dominio Faltas

`stor_backend`, `stor_politica`, `stor_objeto`: infraestructura de storage
tecnico; `NO_PERSISTIBLE` desde el dominio Faltas (el `storage_key` vive en las
entidades de dominio que lo requieren). Tablas geograficas `geo_*` (IGN, INDEC,
BAHRA, Malvinas): externas a Faltas, no administradas por este modulo.

---

## 3. Enums con relevancia de persistencia

No todo enum de dominio con relevancia de persistencia expone un codigo
explicito. Se distinguen tres categorias (ver `DECISION_DDL-ENUM-01` en la
seccion 5 y `102-slice-9-estrategia-jdbc-mariadb.md`, seccion 6):

- `EXPLICIT_NUMERIC_CODE`: el enum expone `codigo()` numerico (`short`).
  Persistencia candidata: `SMALLINT`.
- `EXPLICIT_STRING_CODE`: el enum expone `codigo()` de tipo `String` estable.
  Persistencia candidata: `CHAR`/`VARCHAR` exacta con constraint.
- `NO_EXPLICIT_CODE`: el enum no expone `codigo()`. Prohibido `ordinal()`.
  Prohibido `name()` sin decision explicita. Representacion fisica pendiente
  de `DECISION_DDL-ENUM-01`.

| Enum | Categoria | Notas |
|---|---|---|
| `TipoActa` (`TRANSITO=1, CONTRAVENCION=2, SUSTANCIAS_ALIMENTICIAS=3, COMERCIO=4`) | `EXPLICIT_NUMERIC_CODE` | — |
| `EstadoRedaccionDocumento` (`BORRADOR=1, CONFIRMADA=2, ANULADA=3`) | `EXPLICIT_NUMERIC_CODE` | — |
| `OrigenFirmezaCondena` (`VENCIMIENTO_PLAZO_APELACION=1, APELACION_RECHAZADA=2`) | `EXPLICIT_NUMERIC_CODE` | Persistido inline en `fal_acta_fallo.origen_firmeza` |
| `ResultadoFinalActa` (10 valores, 0–9) | `EXPLICIT_NUMERIC_CODE` | Ver seccion 4 (Decision P1); codigo 5 (`FALLO_CONDENATORIO_PAGADO`) es LEGACY_RESERVED, ver `DECISION_DDL-RF-005` |
| `ActorTipoEvento` (1–6) | `EXPLICIT_NUMERIC_CODE` | `fal_acta_evento.actor_tipo` |
| `OrigenEvento` (1–8) | `EXPLICIT_NUMERIC_CODE` | `fal_acta_evento.origen_evt` |
| `EstadoFalloActa` | `NO_EXPLICIT_CODE` | Sin `codigo()` verificado contra el enum Java vigente; representacion fisica pendiente de `DECISION_DDL-ENUM-01`; valores en `02-estados-bloques-eventos.md` |
| `EstadoApelacionActa` | `NO_EXPLICIT_CODE` | Sin `codigo()` verificado contra el enum Java vigente; representacion fisica pendiente de `DECISION_DDL-ENUM-01`; valores en `02-estados-bloques-eventos.md` |
| `EstadoPagoCondena` | `NO_EXPLICIT_CODE` | Sin `codigo()` verificado contra el enum Java vigente; representacion fisica pendiente de `DECISION_DDL-ENUM-01`; valores en `02-estados-bloques-eventos.md` |
| `ResultadoResolucionApelacion` y demas enums funcionales no listados arriba | Ver `02-estados-bloques-eventos.md` y `10-domain/lifecycle-states.md` | Fuente normativa de valores; esta matriz no repite la tabla ni asume categoria sin verificar el enum Java |
| `BloqueActual`, `TipoEventoActa`, `EstadoProcesalActa`, `SituacionAdministrativaActa` | `EXPLICIT_STRING_CODE` | Codigo `String` corto (`CHAR(4/6)`); patron correcto para estos cuatro; no aplican `SMALLINT` |

---

## 4. Decisiones fisicas ya cerradas

No reabrir. Referencia compacta; el detalle de cada decision permanece en Git.

| # | Decision | Descripcion | Estado |
|---|---|---|---|
| D1 | Firmeza | Firmeza inline en `fal_acta_fallo` (`fh_firmeza`, `si_firme`, `origen_firmeza SMALLINT NULL`); sin tabla propia | CERRADA |
| D2 | Pagos | `FalPagoVoluntario` + `FalPagoCondena` -> `fal_acta_obligacion_pago` con `tipo_obligacion` | CERRADA |
| D3 | Paralizacion | `FalActaParalizacion` implementada en InMemory | CERRADA |
| D4 | JSON | Campos JSON documentales -> JSON nativo MariaDB | CERRADA |
| D5 | Fundamentos fallo | `FalActaFallo.fundamentos` -> `TEXT NULL` directo en `fal_acta_fallo` | CERRADA |
| D6 | Persona | `FalPersona` + `FalPersonaDomicilio` implementadas en InMemory | CERRADA |
| D7 | Notificacion PK | `FalNotificacion.id` -> `BIGINT AUTO_INCREMENT` sin UUID | CERRADA |
| D8 | TipoActa | `FalActa.tipoActa` -> enum `TipoActa` con codigo `SMALLINT` | CERRADA |
| D9 | Prioridad default | `fal_documento_plantilla_default.prioridad` -> `SMALLINT` | CERRADA |
| P1 | `ResultadoFinalActa` | Columna `resultado_final SMALLINT NOT NULL`; 10 valores vigentes (0–9); ver seccion siguiente | CERRADA |
| P2 | Firmeza (identidad) | `FalActaFirmezaCondena` y `FirmezaCondenaRepository` eliminados del codigo Java vigente; firmeza inline en `FalActaFallo` | CERRADA |

### Decision P1 — `ResultadoFinalActa`: columna SMALLINT con codigo explicito

**Decision vigente:** columna MariaDB `resultado_final SMALLINT NOT NULL`; enum
Java con codigo explicito. No usar ordinal Java, `name()`, `VARCHAR` ni `CHAR`.

**Valores vigentes (10; fuente normativa: `00-governance/glossary.md` y
`10-domain/lifecycle-states.md`):**

| Codigo | Valor |
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

**Valor historico eliminado (no reintroducir):** `PAGO_VOLUNTARIO_CONFIRMADO`
(reemplazado por `PAGO_VOLUNTARIO_PAGADO`).

### Decision P2 — Firmeza de condena: identidad y persistencia

**Decision vigente:** la firmeza pertenece al fallo concreto y se persiste en
`fal_acta_fallo` (`si_firme`, `fh_firmeza`, `origen_firmeza`); proyectada en
`FalActaSnapshot`. No hay una segunda fuente de verdad en `FalActa`.
`FalActaFirmezaCondena` y `FirmezaCondenaRepository` estan eliminados del
codigo Java vigente (verificado contra el arbol de fuentes de este HEAD).

---

## 5. `DECISION_DDL` pendientes (fisicas, no funcionales)

Ninguna de estas decisiones reabre un contrato funcional. Todas son
exclusivamente de diseno fisico/DDL y no bloquean `READY_FOR_DDL`.

| ID | Tema | Alternativas | Criterio de cierre | Fuente normativa |
|---|---|---|---|---|
| `DECISION_DDL-RF-005` | Tratamiento fisico/migratorio del valor legacy `FALLO_CONDENATORIO_PAGADO` (codigo 5) de `ResultadoFinalActa` | (a) migrar datos historicos al valor canonico `CONDENA_FIRME_PAGADA`; (b) conservar en catalogo inactivo/legacy solo de lectura; (c) admitir temporalmente solo durante ventana de migracion | Se cierra al aprobar el DDL de `fal_acta.resultado_final` y la estrategia de migracion de datos historicos, si existieran | `02-estados-bloques-eventos.md`, `00-governance/glossary.md` |
| `DECISION_DDL-DOC-01` | `version_row` en `fal_documento` (OCC fisico para numeracion y estados documentales) | (a) agregar `versionRow` a `FalDocumento` antes del adapter JDBC; (b) confiar unicamente en `SEQUENCE` + transaccion sin OCC adicional | Se cierra al definir el mecanismo de concurrencia del adapter JDBC para `DocumentoService` | `109-delta-modelo-mariadb-inmemory.md` seccion 12, `03-comandos-precondiciones-efectos.md` |
| `DECISION_DDL-NOTI-01` | `version_row` en `fal_notificacion` | (a) agregar `versionRow`; (b) delegar exclusivamente en `ResultadoPositivoInMemoryMonitor`/equivalente transaccional | Se cierra al definir el adapter JDBC de `NotificacionService` | `109` seccion 12, `20-application/fallo-command-contracts.md` (CMD-FALLO-004) |
| `DECISION_DDL-GEXT-01` | `version_row` en `fal_acta_gestion_externa` | (a) agregar `versionRow`; (b) unicidad `si_activa=true` por acta como unico mecanismo | Se cierra al definir el adapter JDBC de gestion externa | `109` seccion 5 y 12, `03-comandos-precondiciones-efectos.md` |
| `DECISION_DDL-BLOQ-01` | `version_row` en `fal_acta_bloqueante_cierre_material` | (a) agregar `versionRow`; (b) mantener exclusion solo por invariante de repositorio | Se cierra al definir el adapter JDBC de bloqueantes materiales | `109` seccion 12, `03-comandos-precondiciones-efectos.md` |
| `DECISION_DDL-PERS-01` | Unicidad fisica de `fal_persona` sobre `(tipo_documento, nro_documento)` para personas identificadas | (a) `UNIQUE` simple; (b) `UNIQUE` parcial/condicional (`si_identificado=true`) segun soporte del motor | Se cierra al elegir motor/version MariaDB definitiva y su soporte de indices unicos parciales | `109` seccion 3 |
| `DECISION_DDL-PAGO-01` | Identidad fisica de `FalPagoVoluntario`/`FalPagoCondena` (actualmente `String`, vistas sobre `fal_acta_obligacion_pago`) | (a) mantener como vistas de aplicacion sin tabla propia; (b) exponer directamente `FalActaObligacionPago` en la capa de aplicacion | Se cierra al disenar la capa de acceso a datos de pagos en el adapter JDBC | `110` seccion 2 (Pagos), decision D2 |
| `DECISION_DDL-ENUM-01` | Representacion fisica de enums persistibles sin `codigo()` (categoria `NO_EXPLICIT_CODE`): como minimo `EstadoFalloActa`, `EstadoApelacionActa`, `EstadoPagoCondena` | (a) agregar codigo estable explicito en Java antes del adapter JDBC y usar `SMALLINT`; (b) aprobar codigo `String` estable y usar `CHAR`/`VARCHAR` con constraint | Se cierra con: inventario completo de enums persistibles sin `codigo()`; decision aprobada por enum o politica global; actualizacion del modelo Java si corresponde; DDL consistente con esa decision. Prohibido `ordinal()`, inferir numeros por posicion, o persistir `name()` sin esta decision | `109-delta-modelo-mariadb-inmemory.md` seccion 11, `102-slice-9-estrategia-jdbc-mariadb.md` seccion 6, seccion 3 de este documento |

---

## 6. Cierre

- Bloqueadores funcionales internos: ninguno.
- Decisiones fisicas pendientes: listadas en la seccion 5; ninguna reabre
  dominio, contratos de comando, eventos o estados.
- Integraciones externas (Ingresos/Tesoreria, Informix, geo, IDP): no bloquean
  el diseno inicial de DDL; ver `99-pendientes-siguientes-slices.md`.
- Gate formal `READY_FOR_DDL`: ver `101-auditoria-pre-jdbc-mariadb.md`.
