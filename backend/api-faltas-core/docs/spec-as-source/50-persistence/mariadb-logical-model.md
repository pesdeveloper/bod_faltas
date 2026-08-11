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
- **Campos:** `cod_dep VARCHAR(8) NULL` (único donde informado, UNIQUE permisivo en NULL), `nom_dep VARCHAR(48) NOT NULL`, `id_dep_padre BIGINT NULL` (FK auto-referencial; agr. vía ALTER), `si_activa BOOLEAN NOT NULL`.
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Nota:** `tipo_acta` NO vive en el maestro; vive en `fal_dependencia_version`. Nombres prohibidos: `codigo_dependencia`, `id_dependencia_padre`, `nombre VARCHAR(120/200)`.
- **Regla autorreferencial:** `id_dep_padre IS NULL OR id_dep_padre <> id`. Enforcement en aplicación (`FalDependencia.setIdDepPadre`), no `CHECK` físico: MariaDB no admite `CHECK` sobre columnas `AUTO_INCREMENT` (`ERROR 1901`). Ver `ddl-decisions.md` → `DECISION_DDL-DEP-01`.
- **Entidad Java:** `FalDependencia`. **Puerto:** `DependenciaRepository` / `InMemoryDependenciaRepository`.
- **Estado:** HUMAN_DECISION_CLOSED (SPEC-MODEL-DDL-CLOSURE-001): cod_dep VARCHAR(8), nom_dep VARCHAR(48), regla autorreferencial / BASELINE_PRESERVADA_CON_DELTA.
- **Enmienda (`MARIADB-BASELINE-EXEC-001`, 2026-08-02):** el `CHECK chk_dep_no_self` originalmente decidido en `SPEC-MODEL-DDL-CLOSURE-001` no es ejecutable en MariaDB 12.3.2 (`ERROR 1901: Function or expression 'AUTO_INCREMENT' cannot be used in the CHECK clause of `id``), detectado al ejecutar `create-bod-faltas-domain.sql` contra `sb_faltas_db`. La regla funcional no cambia; su enforcement se desplaza al dominio (mismo patrón ya usado en `DECISION_DDL-PAGO-03` para `obligacionReemplazadaId`). Redacción original preservada arriba salvo el cambio de mecanismo físico.

#### `fal_dependencia_version`
- **Propósito:** versiones de la dependencia con datos cambiantes.
- **PK:** `(id_dep, ver_dep)` (compuesta). **FK:** `id_dep -> fal_dependencia(id)`.
- **Campos:** `nom_dep VARCHAR(48) NULL`, `id_dep_padre BIGINT NULL`, `ver_dep_padre SMALLINT NULL`, `tipo_acta SMALLINT NOT NULL` (define el único tipo de acta que puede labrar esta dependencia), `fh_vig_desde DATE NOT NULL`, `fh_vig_hasta DATE NULL`, `si_activa BOOLEAN NOT NULL`.
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **FK histórica:** `(id_dep_padre, ver_dep_padre) -> fal_dependencia_version(id_dep, ver_dep)`; padre y versión: ambos NULL o ambos NOT NULL.
- **Entidad Java:** `FalDependenciaVersion`. **Estado:** HUMAN_DECISION_CLOSED (SPEC-MODEL-DDL-CLOSURE-001): PK renombrada a id_dep/ver_dep, nom_dep VARCHAR(48) / BASELINE_PRESERVADA_CON_DELTA.

### Inspectores

#### `fal_inspector`
- **Propósito:** inspectores/agentes del organismo habilitados para labrar actas.
- **PK:** `id BIGINT AUTO_INCREMENT`.
- **Campos:** `id_user CHAR(36) NOT NULL UNIQUE` (UUID IDP, único por inspector), `legajo_insp INT NOT NULL` (numérico estricto; > 0; con índice UNIQUE), `nom_insp VARCHAR(36) NOT NULL`, `si_activo BOOLEAN NOT NULL`.
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **CHECK:** `legajo_insp > 0`.
- **Nota:** los campos `legajo VARCHAR(20)`, `apellido`, `nombre` son INCORRECTOS. La definición canónica usa `legajo_insp INT` y `nom_insp VARCHAR(36)`.
- **Entidad Java:** `FalInspector`. **Puerto:** `InspectorRepository` / `InMemoryInspectorRepository`. **Estado:** HUMAN_DECISION_CLOSED (SPEC-MODEL-DDL-CLOSURE-001): nom_insp VARCHAR(36), legajo_insp INT / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_inspector_version`
- **Propósito:** versión del inspector con datos del período.
- **PK:** `(id_insp, ver_insp)` (compuesta). **FK:** `id_insp -> fal_inspector(id)`.
- **Campos:** `legajo_insp INT NOT NULL` (tipo exacto coincide con fal_inspector.legajo_insp), `nom_insp VARCHAR(36) NOT NULL`, `id_dep BIGINT NULL`, `ver_dep SMALLINT NULL`, `fh_vig_desde DATE NOT NULL`, `fh_vig_hasta DATE NULL`, `si_activo BOOLEAN NOT NULL`.
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **FK:** `(id_dep, ver_dep) -> fal_dependencia_version(id_dep, ver_dep)`; ambos NULL o ambos NOT NULL.
- **Entidad Java:** `FalInspectorVersion`. **Estado:** HUMAN_DECISION_CLOSED (SPEC-MODEL-DDL-CLOSURE-001): nom_insp VARCHAR(36), PK id_insp/ver_insp, tipos FK exactos / BASELINE_PRESERVADA_CON_DELTA.

### Firmantes

#### `fal_firmante`
- **Propósito:** maestro de firmantes/autorizados para firma; gestiona el padrón de usuarios habilitados para firmar documentos.
- **PK:** `id BIGINT AUTO_INCREMENT`.
- **Campos:** `id_user CHAR(36) NOT NULL UNIQUE` (usuario IDP, único por firmante), `nom_firmante VARCHAR(48) NOT NULL` (nombre visible actual; histórico vive en versiones), `si_activo BOOLEAN NOT NULL`.
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Entidad Java:** `FalFirmante` (id `Long`). **Puerto:** `FirmanteRepository` / `InMemoryFirmanteRepository`.
- **No** guardar certificados, binarios, firmas concretas ni metadata pesada. Baja lógica vía `si_activo`; no borrado físico si firmó documentos.
- **Estado:** HUMAN_DECISION_CLOSED (SPEC-MODEL-DDL-CLOSURE-001): nom_firmante VARCHAR(48) / BASELINE_PRESERVADA_CON_DELTA / RECONCILIADA_R3.

#### `fal_firmante_version`
- **Propósito:** versionado histórico de roles, cargo, dependencia y vigencia del firmante; congela el contexto vigente en cada cambio relevante.
- **PK:** `(id_firmante, ver_firmante)` (compuesta; `ver_firmante SMALLINT`, inicia en 1). **FK:** `id_firmante -> fal_firmante`.
- **Campos:** `id_user CHAR(36) NOT NULL` (snapshot del IDP), `nom_firmante VARCHAR(48) NOT NULL` (snapshot histórico; mismo ancho que fal_firmante.nom_firmante para representar el mismo nombre sin truncar), `rol_firmante VARCHAR(64) NULL` (rol descriptivo/institucional; opcional), `cargo_firmante VARCHAR(64) NULL`, `id_dep BIGINT NULL FK` (→ `fal_dependencia`; nullable si no aplica), `ver_dep SMALLINT NULL` (obligatorio si hay `id_dep`), `fh_vig_desde DATE NOT NULL`, `fh_vig_hasta DATE NULL` (NULL si vigente), `si_activo BOOLEAN NOT NULL`.
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Entidad Java:** `FalFirmanteVersion` (PK compuesta). **Puerto:** acceso vía `FirmanteRepository`.
- **Reglas:** `rol_firmante` y `cargo_firmante` son descriptivos/institucionales. `tipo_firma` NO va en esta tabla. Al versionar: versión anterior cierra `fh_vig_hasta` + `si_activo = false`.
- **Estado:** HUMAN_DECISION_CLOSED (SPEC-MODEL-DDL-CLOSURE-001): nom_firmante VARCHAR(48), rol_firmante VARCHAR(64), cargo_firmante VARCHAR(64) / BASELINE_PRESERVADA_CON_DELTA / RECONCILIADA_R3.

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
- **Campos nombres:** `tipo_persona SMALLINT NOT NULL` (1=FISICA, 2=JURIDICA), `apellido VARCHAR(24) NULL` (persona física), `nombres VARCHAR(36) NULL` (persona física), `razon_social VARCHAR(64) NULL` (persona jurídica), `nombre_mostrar VARCHAR(64) NULL` (calculado).
- **Campos documento — esquema ejecutado, coincide con el modelo objetivo (`DECISION_DDL-PERS-01`):** `tipo_documento SMALLINT NULL` (TipoDocumentoPersona: 1=DNI, 2=CUIT, 3=CUIL, 4=PASAPORTE, 5=DNI_EXTRANJERO, 9=OTRO), `prefijo_cuit_cuil TINYINT UNSIGNED NULL` (BETWEEN 10 AND 99 cuando se informa), `nro_doc VARCHAR(12) NULL` con forma canónica, `digito_verificador TINYINT UNSIGNED NULL`. El documento es opcional en conjunto y `nro_doc` admite documentos alfanuméricos. El nombre físico canónico es `tipo_documento`; `tipo_doc` está prohibido como nombre de columna.
- **Campos documento — antecedente histórico, ya no vigente:** hasta el 2026-08-03 el esquema ejecutado declaraba `tipo_documento SMALLINT NOT NULL` y `nro_doc INT UNSIGNED NOT NULL` con `CHECK (nro_doc BETWEEN 1 AND 99999999)`, de modo que una persona sin documento y un documento alfanumérico no eran representables. `WAVE 1A2-B1.5B0-B` aplicó el `ALTER` de `DECISION_DDL-PERS-01` (`database/ddl/alter-fal-persona-documento-opcional-001-forward.sql`) y los dos planos convergieron: ya no hay remediación pendiente sobre el documento de esta tabla.
- **Coherencia documento:** DNI/PASAPORTE/DNI_EXTRANJERO/OTRO: prefijo NULL, digito NULL. CUIT/CUIL: prefijo dos dígitos significativos, `nro_doc` con el cuerpo central de ocho dígitos, digito 0–9; los tres obligatorios juntos. Sin documento: `tipo_documento`, `nro_doc`, `prefijo_cuit_cuil` y `digito_verificador` los cuatro NULL (`DECISION_DDL-PERS-01`).
- **Campos contacto:** `email_principal VARCHAR(160) NULL`, `telefono_principal VARCHAR(20) NULL`.
- **Campos rubro/ingresos:** `id_suj TINYINT UNSIGNED NULL` (código tipo de sujeto en Ingresos Municipales; catálogo abierto: 1=INMUEBLE, 2=COMERCIO, 3=RODADO, 18=CEMENTERIO, 20=FALTAS, 99=VARIOS; rango físico 1-255; no CHECK IN), `id_bie MEDIUMINT UNSIGNED NULL` (id del bien/cuenta dentro del tipo de sujeto; rango físico 1-9999999; requiere id_suj), `suj_bie_estado SMALLINT NULL`, `fh_suj_bie_creacion DATETIME(6) NULL`. Java usa `Integer` para ambos. `HUMAN_DECISION_CLOSED — FULL-R1.2-CORRECCION-10`
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`, `fh_ult_mod DATETIME(6) NULL` (HUMAN_DECISION_CLOSED CORRECCION-07: nullable hasta primera modificacion), `id_user_ult_mod CHAR(36) NULL`.
- **Unicidad — esquema ejecutado, coincide con el modelo objetivo (`DECISION_DDL-PERS-01`):** columna generada `doc_key VARCHAR(12) STORED` con expresión `CASE WHEN tipo_documento IS NULL THEN NULL WHEN tipo_documento IN (2,3) THEN CONCAT(prefijo_cuit_cuil, nro_doc, digito_verificador) ELSE nro_doc END`, sin `COALESCE` ni `LPAD`, con `UNIQUE (tipo_documento, doc_key)`. Sin documento la clave es NULL y MariaDB admite ilimitadas filas con clave NULL: así coexisten las personas identificadas parcialmente sin documento. Consecuencia deliberada del UNIQUE compuesto, verificada en el smoke físico del `ALTER`: los mismos once dígitos coexisten una vez como `CUIT` y otra como `CUIL`.
- **Unicidad — antecedente histórico, ya no vigente:** hasta el 2026-08-03 `doc_key` era `VARCHAR(20) STORED` con expresión `CASE WHEN tipo_documento IN (2,3) THEN CONCAT(LPAD(COALESCE(prefijo_cuit_cuil,0),2,0), LPAD(nro_doc,8,0), COALESCE(digito_verificador,0)) ELSE CONCAT(tipo_documento,'-',nro_doc) END`, que rellenaba componentes ausentes y repetía `tipo_documento` dentro de la clave.
- **CHECK físicos del documento:** `chk_fal_persona_doc_coherente`, `chk_fal_persona_doc_tipo`, `chk_fal_persona_doc_cuit`, `chk_fal_persona_doc_no_cuit`, `chk_fal_persona_nro_doc_canonico`, `chk_fal_persona_nro_doc_dni`, `chk_fal_persona_prefijo` (10..99) y `chk_fal_persona_digito`. Cláusulas exactas en [`ddl-decisions.md`](ddl-decisions.md) §7 de `DECISION_DDL-PERS-01`. `chk_fal_persona_nro_doc` (`BETWEEN 1 AND 99999999`) ya no existe: era aritmético sobre una columna que pasó a ser texto. Los `CHECK` verifican estructura, no aritmética: el módulo 11 del dígito verificador sigue sin exigirse (`D-PERS-CUIT-DV-01`, abierta).
- **versionRow/OCC:** NO — `fal_persona` no define `versionRow` (`DECISION_DDL-PERS-01`).
- **Prohibido:** `observacion`; un único `nro_doc` que absorba los once dígitos de CUIT/CUIL y elimine `prefijo_cuit_cuil` / `digito_verificador`; apellido/nombres > 24/36; `tipo_doc` como nombre físico; persona centinela o documento inventado para representar la ausencia de documento.
- **Entidad Java:** `FalPersona`. **Puerto:** `PersonaRepository` / `InMemoryPersonaRepository`.
- **Estado:** HUMAN_DECISION_CLOSED (SPEC-MODEL-DDL-CLOSURE-001): documento estructurado, apellido VARCHAR(24), nombres VARCHAR(36) / BASELINE_PRESERVADA_CON_DELTA. La estructura de tres componentes para CUIT/CUIL se conserva; el tipo físico de `nro_doc` y la obligatoriedad del documento quedan redefinidos por `DECISION_DDL-PERS-01`, que es la única autoridad vigente sobre el documento de `fal_persona` y declara obsoletas tanto `INT UNSIGNED NOT NULL` como los nombres `tipo_doc` / `UNIQUE(tipo_doc, nro_doc)`. `DDL_EXECUTED` — `WAVE 1A2-B1.5B0-B`, 2026-08-03: el esquema ejecutado coincide con el modelo objetivo.

#### `fal_persona_domicilio`
- **Propósito:** domicilios de personas (infractor, notificación); no es el lugar del hecho (eso vive en `fal_acta`). **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `persona_id -> fal_persona`, `acta_origen_id -> fal_acta` (NULL, acta donde nació el domicilio).
- **Campos:** `tipo_domicilio SMALLINT NOT NULL`, `origen_domicilio SMALLINT NOT NULL` (CONTENIDO: OrigenDomicilio enum; USO: identifica el circuito que generó este domicilio; REGLA: debe informarse siempre; VALORES: leer de OrigenDomicilio.java), `modo_domicilio SMALLINT NOT NULL` (MALVINAS_LOCAL/EXTERNO), `si_activo BOOLEAN NOT NULL`, `si_notificable BOOLEAN NOT NULL`, `si_principal BOOLEAN NOT NULL`, `id_provincia SMALLINT NULL`, `unidad_territorial_tipo SMALLINT NULL`, `id_unidad_territorial INT NULL`, `id_localidad BIGINT NULL` (solo modo EXTERNO), `id_calle BIGINT NULL` (solo modo EXTERNO), `id_loc_malvinas VARCHAR(8) NULL`, `localidad_malvinas_version_id BIGINT NULL FK` (→ `geo_malv_localidad_version.localidad_version_id`), `id_tca_malvinas VARCHAR(10) NULL`, `calle_malvinas_version_id BIGINT NULL FK` (→ `geo_malv_calle_version.calle_version_id`), `calle_txt VARCHAR(48) NULL`, `altura INT NULL`, `si_sin_altura BOOLEAN NOT NULL`, `unidad_funcional VARCHAR(20) NULL`, `codigo_postal VARCHAR(10) NULL`, `domicilio_txt VARCHAR(196) NULL` (cache legible, mutable), `si_normalizado_parcial BOOLEAN NOT NULL`, `lat DECIMAL(10,7) NULL`, `lon DECIMAL(10,7) NULL`, `origen_ubicacion SMALLINT NULL`.
- **Eliminado:** `validacion_domicilio` — no reemplazar por otro texto ambiguo.
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`, `fh_ult_mod DATETIME(6) NULL` (HUMAN_DECISION_CLOSED CORRECCION-07: nullable hasta primera modificacion), `id_user_ult_mod CHAR(36) NULL`.
- **Entidad Java:** `FalPersonaDomicilio`. **Puerto:** `PersonaDomicilioRepository` / `InMemoryPersonaDomicilioRepository`.
- **versionRow/OCC:** NO. Baja lógica vía `si_activo=false`, nunca borrado físico.
- **Estado:** HUMAN_DECISION_CLOSED (SPEC-MODEL-DDL-CLOSURE-001): calle_txt VARCHAR(48), domicilio_txt VARCHAR(196), sin validacion_domicilio, FK GEO canónicas / BASELINE_PRESERVADA_CON_DELTA.

### Acta core

#### `fal_acta`
- **Propósito:** agregado raíz del expediente de faltas. Guarda identidad administrativa, estado actual (tripla canónica), captura, lugar del hecho, QR y numeración.
- **PK:** `id BIGINT AUTO_INCREMENT`. **UK:** `id_tecnico CHAR(36)`, `codigo_qr VARCHAR(128)`.
- **FK conceptuales:** `id_talonario -> num_talonario`, `id_insp + ver_insp -> fal_inspector_version`, `id_dep + ver_dep -> fal_dependencia_version`, `id_persona_infractor -> fal_persona`, `id_domicilio_infractor_act -> fal_persona_domicilio`, `id_domicilio_notif_act -> fal_persona_domicilio`, `localidad_infr_malvinas_version_id -> geo_malv_localidad_version`, `calle_infr_malvinas_version_id -> geo_malv_calle_version`, `id_motivo_archivo_actual -> fal_motivo_archivo`.
- **Campos — identidad y numeración:** `nro_acta VARCHAR(30) NULL`, `id_talonario BIGINT NULL`, `nro_talonario_usado INT NULL`.
- **Campos — tipo y captura:** `tipo_acta SMALLINT NOT NULL`, `origen_captura SMALLINT NOT NULL`, `id_dispositivo_captura VARCHAR(80) NULL`, `id_user_captura CHAR(36) NULL`, `fh_captura DATETIME(6) NOT NULL`, `lat_captura DECIMAL(10,7) NULL`, `lon_captura DECIMAL(10,7) NULL`, `precision_captura_m DECIMAL(8,2) NULL`, `fh_pos_captura DATETIME(6) NULL`, `origen_pos_captura SMALLINT NULL`.
- **Campos — fecha funcional:** `fecha_acta DATE NOT NULL`, `fecha_labrado DATETIME(6) NOT NULL`.
- **Campos — dependencia e inspector (versionados):** `id_dep BIGINT NOT NULL`, `ver_dep SMALLINT NOT NULL`, `id_insp BIGINT NULL`, `ver_insp SMALLINT NULL` (obligatoria si hay inspector).
- **Campos — infractor y domicilios:** `situacion_identificacion_infractor SMALLINT NOT NULL` sin default (`SituacionIdentificacionInfractor`: 1=IDENTIFICADO_COMPLETO, 2=IDENTIFICADO_PARCIAL, 3=PRESENTE_SE_NIEGA, 4=PRESENTE_NO_APORTA_DATOS, 5=AUSENTE, 6=DATOS_INSUFICIENTES), `id_persona_infractor BIGINT NULL` (hay sujeto identificado exactamente en las situaciones 1 y 2; en 3..6 la columna queda NULL y nunca una persona centinela), `id_domicilio_infractor_act BIGINT NULL`, `id_domicilio_notif_act BIGINT NULL`. Nombre/documento/domicilio del infractor NO se almacenan en `fal_acta`: se obtienen de `fal_persona`/`fal_persona_domicilio` vía estas FK; el snapshot los proyecta como vista derivada.
- **Campos — resumen del hecho:** `resumen_hecho VARCHAR(1000) NULL`, `domicilio_hecho VARCHAR(196) NULL` (texto libre del lugar del hecho).
- **Campos — lugar del hecho (nomenclatura local Malvinas):** `id_loc_infr_malvinas VARCHAR(8) NULL`, `localidad_infr_malvinas_version_id BIGINT NULL`, `id_tca_infr_malvinas VARCHAR(10) NULL`, `calle_infr_malvinas_version_id BIGINT NULL`.
- **Campos — lugar del hecho (altura):** `altura_infr INT NULL`, `altura_origen_infr SMALLINT NULL`, `si_altura_infr_estimada BOOLEAN NOT NULL DEFAULT FALSE`.
- **Campos — lugar del hecho (coordenadas finales):** `lat_infr DECIMAL(10,7) NULL`, `lon_infr DECIMAL(10,7) NULL` (ver nota de precisión física), `origen_ubicacion_infr SMALLINT NULL`, `si_ubicacion_infr_manual BOOLEAN NOT NULL DEFAULT FALSE`.
- **Campos — texto libre del lugar del hecho:** `si_dom_txt_infr BOOLEAN NOT NULL DEFAULT FALSE`, `dom_txt_infr VARCHAR(196) NULL`.
- **Campos — contexto geográfico:** `si_eje_urb BOOLEAN NOT NULL DEFAULT FALSE`.
- **Campos — QR:** `codigo_qr VARCHAR(128) NOT NULL`, `qr_payload_version SMALLINT NOT NULL DEFAULT 0`.
- **Nota QR:** 128 caracteres permiten el token firmado completo formato `QR0.<uuid-acta>.<version>.<firma-hmac-base64url>`. No guardar imagen QR, payload masivo, domicilio, datos personales ni URL completa arbitraria.
- **Nota QR — algoritmo y momento de generación (`DECISION_DDL-ACTA-QR-01`):** la firma es `HMAC-SHA-256` sobre el contenido canónico `QR0.<id_tecnico>.<version>`, en Base64 URL-safe sin relleno; el token es determinístico para el mismo `id_tecnico`, versión y clave, y se genera antes de la primera escritura del acta a partir de `id_tecnico`, nunca de `id_acta`. El ancho de 128 queda ratificado, no reversado. Detalle en [`ddl-decisions.md`](ddl-decisions.md).
- **Campos — estado actual (tripla canónica):** `bloque_actual CHAR(4) NOT NULL`, `est_proc_act CHAR(4) NOT NULL`, `sit_adm_act CHAR(4) NOT NULL`.
- **Campos — resultado y cierre:** `resultado_final SMALLINT NOT NULL`, `resultado_firma_infractor SMALLINT NOT NULL`, `id_motivo_archivo_actual BIGINT NULL`, `permite_reingreso BOOLEAN NULL`, `fh_cierre DATETIME(6) NULL` (funcional), `fh_archivo DATETIME(6) NULL` (funcional).
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`, `fh_ult_mod DATETIME(6) NULL` (HUMAN_DECISION_CLOSED CORRECCION-07: nullable hasta primera modificacion), `id_user_ult_mod CHAR(36) NULL`.
- **Entidad Java:** `FalActa` (id `Long`). **Puerto:** `ActaRepository` / `InMemoryActaRepository`.
- **versionRow/OCC:** SI (`versionRow` en `FalActa`, default 0).
- **Orden determinístico:** no aplica (entidad única por expediente).
- **No existen** `estado_procesal`/`situacion_administrativa` como `SMALLINT`, ni `id_persona` como nombre de FK: los nombres reales son `est_proc_act`/`sit_adm_act`, y ambos junto con `bloque_actual` son **CHAR(4)** (código `String` estable del enum, no `SMALLINT`); la FK a persona se llama `id_persona_infractor` y es **NULL**.
- **CHECK de la tabla:** cuatro. `chk_acta_tipo`, `chk_acta_qr_ver`, `chk_acta_situacion_infractor CHECK (situacion_identificacion_infractor BETWEEN 1 AND 6)` y `chk_acta_infractor_coherente CHECK ((situacion_identificacion_infractor IN (1,2) AND id_persona_infractor IS NOT NULL) OR (situacion_identificacion_infractor IN (3,4,5,6) AND id_persona_infractor IS NULL))`. La FK `fk_acta_persona_infractor` conserva `ON DELETE RESTRICT ON UPDATE RESTRICT` y el índice `ix_fal_acta_persona` se mantiene.
- **Remediación física aplicada (`DECISION_DDL-ACTA-INFR-01`):** `DDL_EXECUTED` — `WAVE 1A2-B1.5B1`, 2026-08-03. `fal_acta` pasó de 56 a **57 columnas**: se agregó `situacion_identificacion_infractor SMALLINT NOT NULL` sin default, `id_persona_infractor` pasó de `BIGINT NOT NULL` a `BIGINT NULL` y se sumaron los dos `CHECK` de la línea anterior, de modo que un infractor ausente, que se niega a identificarse, que no aporta datos o con datos insuficientes se representa con la FK en NULL en vez de con una persona centinela, que queda prohibida. El backfill clasificó por documento de la persona vinculada (1 con `tipo_documento` y `nro_doc` informados, 2 con ambos NULL) y no dejó filas sin clasificar. Paquete: `database/ddl/alter-fal-acta-situacion-infractor-001-forward.sql` y su rollback, con precheck, postcheck y rollback-precheck en `database/diagnostics/`. Semántica del enum en [`../10-domain/states-events-catalogs.md`](../10-domain/states-events-catalogs.md); orden de migración, backfill y rollback en [`ddl-decisions.md`](ddl-decisions.md).
- **Nota de precisión física:** las cuatro coordenadas de `fal_acta` son `DECIMAL(10,7)`, el mismo tipo que `fal_persona_domicilio.lat`/`.lon` (`DECISION_DDL-COORD-01`). Tipo Java `Double`. `precision_captura_m` es una distancia en metros, no una coordenada: `DECIMAL(8,2)`.
- **Estado:** HUMAN_DECISION_CLOSED (SPEC-MODEL-DDL-CLOSURE-001): ver_dep/ver_insp SMALLINT (tipo exacto FK), codigo_qr VARCHAR(128), domicilio_hecho VARCHAR(196), dom_txt_infr VARCHAR(196) / BASELINE_PRESERVADA_CON_DELTA.

### Eventos, snapshot, evidencias y observaciones

#### `fal_acta_evento`
- **Propósito:** log append-only de eventos del expediente (timeline de hechos reales de dominio).
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `acta_id -> fal_acta`, `id_docu_rel -> fal_documento` (NULL), `id_notif_rel -> fal_notificacion` (NULL).
- **Campos:** `tipo_evt CHAR(6) NOT NULL` (código estable del enum `TipoEventoActa`, **no** `SMALLINT`), `origen_evt SMALLINT NOT NULL`, `fh_evt DATETIME(6) NOT NULL`, `bloque_func CHAR(4) NULL`, `est_proc_ant CHAR(4) NULL`, `est_proc_nvo CHAR(4) NULL`, `sit_adm_ant CHAR(4) NULL`, `sit_adm_nva CHAR(4) NULL`, `actor_tipo SMALLINT NULL`, `actor_id CHAR(36) NULL`, `id_docu_rel BIGINT NULL`, `id_notif_rel BIGINT NULL`, `id_pres_rel BIGINT NULL`, `id_user_evt CHAR(36) NULL`, `si_evt_cierre BOOLEAN NOT NULL`, `si_evt_ext BOOLEAN NOT NULL`, `si_permite_reing BOOLEAN NOT NULL`, `correlacion_id CHAR(36) NULL`.
- **Eliminado:** `actor_ref VARCHAR(80)` — identidad estructurada vía actor_tipo + actor_id. `descripcion_legible VARCHAR(255)` — reconstruible desde tipo_evento + actor + timestamp; no persistir.
- **Cambiado:** `correlacion_id` es `CHAR(36)` (UUID); no `VARCHAR(60)`.
- **Entidad Java:** `FalActaEvento` (id `Long`, inmutable). **Puerto:** `ActaEventoRepository` / `InMemoryActaEventoRepository`.
- **Orden determinístico:** por `fhEvt + id` (`fh_evt + id` en DDL); append-only, nunca se actualiza ni borra. No existe `orden_logico`. No usa payload JSON.
- **Estado:** HUMAN_DECISION_CLOSED (SPEC-MODEL-DDL-CLOSURE-001): sin actor_ref, sin descripcion_legible, correlacion_id CHAR(36) / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_acta_snapshot`
- **Propósito:** proyección operativa resumida del expediente (1:1 con acta), derivada y regenerable. Alimenta bandejas, filtros, badges y habilitación de acciones. No es fuente de verdad; se recalcula en cada transición de dominio.
- **PK:** `id_acta BIGINT NOT NULL FK` (→ `fal_acta`).
- **Campos — control de concurrencia:** `version_row INT NOT NULL DEFAULT 0` (OCC; `DECISION_DDL-SNAP-01` CERRADA).
- **Campos — estado procesal:** `bloque_actual CHAR(4) NULL` (`BloqueActual`), `est_proc_act CHAR(4) NULL` (`EstadoProcesalActa`), `sit_adm_act CHAR(4) NULL` (`SituacionAdministrativaActa`), `resultado_final SMALLINT NULL` (`ResultadoFinalActa`, `EXPLICIT_NUMERIC_CODE`).
- **Campos — bandejas y acciones:** `cod_bandeja VARCHAR(50) NULL` (`CodigoBandeja`, `EXPLICIT_STRING_CODE`; longitud física confirmada: max 50 caracteres; `DECISION_DDL-SNAP-01` CERRADA), `sub_bandeja VARCHAR(80) NULL`, `accion_pendiente VARCHAR(50) NULL` (`AccionPendiente`, `EXPLICIT_STRING_CODE`; longitud física confirmada: max 50 caracteres; **no** `CHAR(6)`).
- **Campos — documentos y notificaciones:** `tiene_documentos BOOLEAN NOT NULL DEFAULT FALSE`, `tiene_docs_pendientes_firma BOOLEAN NOT NULL DEFAULT FALSE`, `tiene_docs_listos_para_notificar BOOLEAN NOT NULL DEFAULT FALSE`, `tiene_notificaciones BOOLEAN NOT NULL DEFAULT FALSE`, `notificacion_en_curso BOOLEAN NOT NULL DEFAULT FALSE`, `bloqueado_cierre BOOLEAN NOT NULL DEFAULT FALSE`, `id_docu_ult BIGINT NULL FK` (→ `fal_documento`), `bloqueado_notificacion BOOLEAN NOT NULL DEFAULT FALSE`.
- **Campos — valorización operativa:** `valorizacion_operativa_id BIGINT NULL FK` (→ `fal_acta_valorizacion`), `estado_valorizacion_operativa SMALLINT NULL` (`EstadoValorizacion`), `tipo_valorizacion_operativa SMALLINT NULL` (`TipoValorizacionActa`), `monto_operativo_vigente DECIMAL(14,2) NULL` (valorización UX del acta; no es estado económico de pagos), `si_monto_confirmado BOOLEAN NOT NULL DEFAULT FALSE`.
- **Campos — satélites de acta:** `licencia_provincia_txt VARCHAR(64) NULL`, `licencia_unidad_txt VARCHAR(80) NULL`, `nomenclatura_resumen VARCHAR(120) NULL`, `id_bie_i MEDIUMINT UNSIGNED NULL` (proyección de fal_acta_contravencion.id_bie_i), `id_bie_c MEDIUMINT UNSIGNED NULL` (proyección de fal_acta_contravencion.id_bie_c). Java usa `Integer`. `HUMAN_DECISION_CLOSED — FULL-R1.2-CORRECCION-10`
- **Campos — paralización:** `motivo_paralizacion_act SMALLINT NULL` (`MotivoParalizacion`, `EXPLICIT_NUMERIC_CODE`).
- **Campos — control técnico:** `ultimo_evento_tipo CHAR(6) NULL` (`TipoEventoActa`, `EXPLICIT_STRING_CODE`), `ultima_actualizacion DATETIME(6) NULL`.
- **Entidad Java:** `FalActaSnapshot` (PK: `idActa Long`; campos verificados). **Puerto:** `ActaSnapshotRepository` / `InMemoryActaSnapshotRepository`.
- **versionRow/OCC:** IMPLEMENTADO (`DECISION_DDL-SNAP-01` CERRADA). `InMemoryActaSnapshotRepository.guardar()` auto-incrementa `versionRow`.
- **Economía de pagos:** **NO** transportada en snapshot (`DECISION_DDL-SNAP-02` CERRADA). Los campos históricos `tipo_obligacion_pago`, `estado_obligacion_pago`, `monto_obligacion_pago`, `tipo_forma_pago_vigente`, `estado_forma_pago_vigente`, `si_plan_pago`, `estado_plan_pago`, `cant_cuotas_plan`, `valor_cuota_plan`, `cant_cuotas_pagadas`, `cant_cuotas_mora`, `cant_cuotas_mora_consec`, `cant_dias_mora`, `si_apta_intimacion`, `motivo_apta_intimacion`, `si_pago_procesado`, `si_pago_confirmado`, `fh_ult_sync_ingresos` han sido **eliminados** de `FalActaSnapshot.java` y de esta tabla. Las lecturas económicas salen de `fal_acta_economia_proyeccion`. `SnapshotRecalculador.proyectarPagos()` es no-op confirmado.
- **Campos históricos excluidos (DECISION_DDL-SNAP-02 CERRADA):** `fh_acta`, `nro_acta`, `tipo_acta`, `id_dep`/`ver_dep`, `id_insp`/`ver_insp`, `id_persona_infractor`, `nombre_infractor`, `doc_infractor_txt`, domicilios/textos/ubicación del hecho, `esta_cerrada`, `si_visible_bandeja`, `prioridad`, bloqueantes separados, gestión externa duplicada, fechas de vencimiento duplicadas, `id_evt_ult`, `id_notif_ult`, `fh_snapshot`, `rebuild_id`, `fh_ult_mod`, `id_user_ult_mod`.
- **Estado:** LISTO_PARA_DDL / RECONCILIADA_R3.

#### `fal_acta_evidencia`
- **Propósito:** repositorio de evidencias del acta: documentos, multimedia y firma ológrafa del infractor. No es firma institucional; no participa en `FalDocumentoFirma` ni `FalDocumentoFirmaReq`. `HUMAN_DECISION_CLOSED — FULL-R1.2-CORRECCION-10`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_acta -> fal_acta`.
- **Campos (verificados contra Java; `DECISION_DDL-EVID-01` CERRADA):** `id_acta BIGINT NOT NULL`, `tipo_evid SMALLINT NOT NULL` (`TipoEvidenciaActa`, `EXPLICIT_NUMERIC_CODE`; catálogo aprobado: `FOTO=4, VIDEO=8, AUDIO=12, PDF=16, DOCUMENTO_OFIMATICO=20, PLANILLA_CALCULO=24, FIRMA_OLOGRAFA_INFRACTOR=48`; códigos 28-44 reservados), `storage_key VARCHAR(255) NOT NULL` (no guardar binario), `fecha_registro DATETIME(6) NOT NULL` (instante funcional de captura/incorporación), `hash_evid CHAR(64) NULL` (SHA-256 hexadecimal opcional; si informado debe ser exactamente 64 caracteres hex), `fh_alta DATETIME(6) NOT NULL` (instante técnico de alta), `id_user_alta CHAR(36) NOT NULL` (actor técnico/funcional que incorporó la evidencia).
- **Campos excluidos (DECISION_DDL-EVID-01 CERRADA):** `nombre_archivo`, `mime_type`, `orden_evid` — pertenecen al metadato del storage, no al registro de dominio. No se agrega `fh_captura` adicional: `fecha_registro` y `fh_alta` cubren las semánticas funcional y técnica. Estos cuatro campos del histórico 2026-06-23 quedan excluidos.
- **Entidad Java:** `FalActaEvidencia` (id `Long`, inmutable; campos: `id`, `idActa`, `tipoEvid`, `storageKey`, `fechaRegistro`, `hashEvid`, `fhAlta`, `idUserAlta`). Constructor exige `fechaRegistro`, `fhAlta` y `idUserAlta` not-null; `hashEvid` nullable con validación de 64 caracteres hex si presente. **Puerto:** `ActaEvidenciaRepository` / `InMemoryActaEvidenciaRepository`.
- **Append-only:** SÍ (inmutable post-creación).
- **Estado:** LISTO_PARA_DDL / RECONCILIADA_R3 / ACTUALIZADA_CORRECCION-10.

#### `fal_observacion`
- **Propósito:** registro polimórfico append-only de notas libres vinculadas a cualquier entidad observable del dominio. Ver spec completa en `40-domain/observaciones.md`.
- **PK:** `id BIGINT AUTO_INCREMENT`.
- **Campos:** `entidad_tipo SMALLINT NOT NULL` (EntidadTipoObservada, 22 códigos), `entidad_id BIGINT NOT NULL`, `id_acta_contexto BIGINT NULL FK` (→ fal_acta, ON DELETE CASCADE), `origen_observacion SMALLINT NOT NULL` (OrigenObservacion: 1=USUARIO, 2=SISTEMA, 3=INTEGRACION), `observacion VARCHAR(512) NOT NULL`, `si_activa BOOLEAN NOT NULL DEFAULT TRUE`, `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`, `fh_baja DATETIME(6) NULL`, `id_user_baja CHAR(36) NULL`.
- **CHECK:** `CHAR_LENGTH(TRIM(observacion)) BETWEEN 1 AND 512`; baja coherente (si_activa=TRUE ⟺ fh_baja/id_user_baja NULL; si_activa=FALSE ⟺ ambos NOT NULL).
- **Índices:** `(entidad_tipo, entidad_id, si_activa, fh_alta)`, `(id_acta_contexto, fh_alta)`.
- **No tiene** `version_row`: no se edita; corrección vía baja lógica + nueva inserción.
- **Triggers:** 21 triggers AFTER DELETE (uno por tabla observable, excepto fal_acta que usa CASCADE). Ver `40-domain/observaciones.md` sección 5.
- **Entidad Java:** `FalObservacion`. **Puerto:** `ObservacionRepository` / `InMemoryObservacionRepository`. **Enums:** `EntidadTipoObservada` (22 códigos), `OrigenObservacion` (3 códigos).
- **Estado:** HUMAN_DECISION_CLOSED (SPEC-MODEL-DDL-CLOSURE-001): spec completa en observaciones.md / BASELINE_PRESERVADA_CON_DELTA.

### Normativa

#### `fal_dependencia_normativa`
- **Entidad Java:** `FalDependenciaNormativa`. **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_normativa_faltas`
- **PK:** `id BIGINT AUTO_INCREMENT`.
- **Campos:** `codigo_norma VARCHAR(8) NOT NULL`, `version_norma SMALLINT NOT NULL DEFAULT 1` (versión funcional; una fila por versión, historia reconstruible), `nombre_norma VARCHAR(64) NOT NULL`, `tipo_norma SMALLINT NOT NULL`, `numero_norma INT NULL`, `anio_norma SMALLINT NULL`, `fecha_promulgacion DATE NULL`, `fecha_publicacion DATE NULL`, `si_activa BOOLEAN NOT NULL`, `fh_vig_desde DATE NOT NULL`, `fh_vig_hasta DATE NULL`, `version_row INT NOT NULL DEFAULT 0` (OCC de la fila; distinto de `version_norma`), `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Restricción:** `UNIQUE(codigo_norma, version_norma)`. UK condicional sobre columna generada `codigo_norma_activo = IF(si_activa, codigo_norma, NULL)` para impedir dos versiones activas simultáneas del mismo código.
- **Regla de corrección (Wave 4, PROMPT-NOCTURNO-WAVE-04-PARTE-2 §6):** si la fila todavía no tiene relaciones materiales (ningún `fal_dependencia_normativa`/`fal_articulo_normativa_faltas` que la referencien de forma no trivial), una corrección por error puede actualizar la misma versión con OCC (`version_row`); si ya tiene relaciones, no se modifica el significado histórico: se cierra `fh_vig_hasta`/`si_activa=false` de la versión actual y se crea una nueva fila con `version_norma+1`.
- **Entidad Java:** `FalNormativaFaltas`. **Puerto:** `NormativaRepository` / `InMemoryNormativaRepository` / `JdbcNormativaRepository`.
- **Estado:** RECONCILIADA_WAVE4 (PROMPT-NOCTURNO-WAVE-04-PARTE-2 §6): versionado real con `version_norma`+`version_row` reemplaza el modelo previo sin persistencia; codigo_norma VARCHAR(8), nombre_norma VARCHAR(64) / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_articulo_normativa_faltas`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_normativa -> fal_normativa_faltas`.
- **Campos:** `id_normativa BIGINT NOT NULL`, `codigo_articulo VARCHAR(16) NOT NULL`, `version_articulo SMALLINT NOT NULL DEFAULT 1` (versión funcional del artículo), `nombre_articulo VARCHAR(64) NOT NULL`, `descripcion_articulo VARCHAR(128) NULL` (no usar TEXT para descripción breve), `tipo_infraccion SMALLINT NOT NULL`, `categoria_infraccion SMALLINT NULL`, `si_admite_medida_preventiva BOOLEAN NOT NULL DEFAULT FALSE`, `si_activo BOOLEAN NOT NULL`, `fh_vig_desde DATE NOT NULL`, `fh_vig_hasta DATE NULL`, `version_row INT NOT NULL DEFAULT 0` (OCC de la fila), `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Restricción:** `UNIQUE(id_normativa, codigo_articulo, version_articulo)`. UK condicional sobre columna generada `codigo_articulo_activo` para impedir dos versiones activas simultáneas del mismo código dentro de la misma normativa.
- **No contiene** `cantidad_unidades` ni `tipo_unidad`: la fórmula económica del artículo (unidades, montos fijos, pago voluntario) vive exclusivamente en `fal_tarifario_unidad_faltas`, que es POR ARTÍCULO (PROMPT-NOCTURNO-WAVE-04-PARTE-2 §7.3). Esto cierra la incompatibilidad estructural documentada en `ACTA-ALTA-INTEGRAL-JDBC-MASTER-20260804-224853/wave-04-jdbc-alta-integral/CHECKPOINT.md`§3.
- **Regla de corrección:** igual que `fal_normativa_faltas` — corrección sin relaciones vía OCC en la misma fila; con relaciones (p. ej. `fal_acta_articulo_infringido` que la referencia), nueva versión.
- **Entidad Java:** `FalArticuloNormativaFaltas`.
- **Estado:** RECONCILIADA_WAVE4 (PROMPT-NOCTURNO-WAVE-04-PARTE-2 §6/§7): versionado real, campos económicos removidos a favor del tarifario por artículo; codigo_articulo VARCHAR(16), nombre_articulo VARCHAR(64), descripcion_articulo VARCHAR(128) / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_tarifario_unidad_faltas`
- **Propósito:** representa la FÓRMULA del artículo (no solo un valor unitario): cantidad/monto ordinario y de pago voluntario, para el cálculo de valorización. Es POR ARTÍCULO, no por tipo de unidad global (PROMPT-NOCTURNO-WAVE-04-PARTE-2 §7.3).
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_articulo -> fal_articulo_normativa_faltas`.
- **Campos:** `id_articulo BIGINT NOT NULL`, `tipo_unidad SMALLINT NOT NULL` (`TipoUnidadFaltas`: SALARIO/UNIDAD_FIJA/MONTO), `cantidad_unidades_ordinaria DECIMAL(10,4) NULL` (obligatoria si `tipo_unidad` ≠ MONTO), `cantidad_unidades_pago_voluntario DECIMAL(10,4) NULL` (obligatoria si `si_admite_pago_voluntario=true` y `tipo_unidad` ≠ MONTO), `monto_fijo_ordinario DECIMAL(14,2) NULL` (obligatorio si `tipo_unidad=MONTO`), `monto_fijo_pago_voluntario DECIMAL(14,2) NULL` (obligatorio si `si_admite_pago_voluntario=true` y `tipo_unidad=MONTO`), `si_admite_pago_voluntario BOOLEAN NOT NULL DEFAULT FALSE`, `fh_vig_desde DATE NOT NULL`, `fh_vig_hasta DATE NULL`, `si_activa BOOLEAN NOT NULL DEFAULT TRUE`, `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Entidad Java:** `FalTarifarioUnidadFaltas`. **Puerto:** `TarifarioUnidadFaltasRepository` / `InMemoryTarifarioUnidadFaltasRepository` / `JdbcTarifarioUnidadFaltasRepository`.
- **Unicidad física requerida:** columna generada `id_articulo_activo = IF(si_activa, id_articulo, NULL)` con `UNIQUE(id_articulo_activo)`: como máximo una tarifa activa por artículo. Índices `(id_articulo, si_activa)` (lookup normal) y `(id_articulo, fh_vig_desde, fh_vig_hasta)` (lookup histórico).
- **Regla de cambio de fórmula:** cerrar vigencia (`fh_vig_hasta`, `si_activa=false`) de la tarifa anterior y crear una nueva fila activa; la histórica se conserva.
- **Estado:** RECONCILIADA_WAVE4 (PROMPT-NOCTURNO-WAVE-04-PARTE-2 §7.3): reemplaza el modelo previo (clave por `TipoUnidadFaltas` global, sin persistencia real) por tarifario por artículo con ordinaria/pago-voluntario / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_valor_unidad_faltas`
- **Propósito:** valor monetario histórico de las unidades económicas `SALARIO` y `UNIDAD_FIJA` (PROMPT-NOCTURNO-WAVE-04-PARTE-2 §7.2). `MONTO` no tiene fila aquí: su valor vive directamente en `fal_tarifario_unidad_faltas.monto_fijo_*`.
- **PK:** `id BIGINT AUTO_INCREMENT`.
- **Campos:** `tipo_unidad SMALLINT NOT NULL` (`TipoUnidadFaltas`: solo SALARIO/UNIDAD_FIJA en la práctica), `valor_unidad DECIMAL(14,4) NOT NULL`, `fh_vig_desde DATE NOT NULL`, `fh_vig_hasta DATE NULL`, `si_activa BOOLEAN NOT NULL DEFAULT TRUE`, `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`. **No** `id_user_modificacion` (decisión cerrada del prompt).
- **Entidad Java:** `FalValorUnidadFaltas` (nueva). **Puerto:** `ValorUnidadFaltasRepository` / `InMemoryValorUnidadFaltasRepository` / `JdbcValorUnidadFaltasRepository`.
- **Unicidad física requerida:** columna generada `tipo_unidad_activo = IF(si_activa, tipo_unidad, NULL)` con `UNIQUE(tipo_unidad_activo)`: una sola fila activa por tipo, permite lookup directo del valor operativo actual por índice. Índices `(tipo_unidad, si_activa)` y `(tipo_unidad, fh_vig_desde, fh_vig_hasta)`.
- **Estado:** NUEVA_WAVE4 (PROMPT-NOCTURNO-WAVE-04-PARTE-2 §7.2) / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_medida_preventiva`
- **Entidad Java:** `FalMedidaPreventiva` (versión atómica; una activa por código). **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_articulo_medida_preventiva`
- **Entidad Java:** `FalArticuloMedidaPreventiva` (PK compuesta vía `ArticuloMedidaPreventivaId`; sin reactivación silenciosa). **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

### Artículos infringidos y valorización

#### `fal_acta_articulo_infringido`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_acta -> fal_acta`, `id_articulo -> fal_articulo_normativa_faltas`.
- **Campos:** `si_activo BOOLEAN NOT NULL DEFAULT TRUE`, `motivo_baja SMALLINT NULL`, `version_row INT NOT NULL DEFAULT 0` (OCC, PROMPT-NOCTURNO-WAVE-04-PARTE-2 §7.4), `fh_baja DATETIME(6) NULL`, `id_user_baja CHAR(36) NULL`, `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Unicidad física requerida:** UK activo `(id_acta, id_articulo)` vía columnas generadas.
- **Eliminado:** `observaciones TEXT` (centralizada en `fal_observacion(ARTICULO_INFRINGIDO)`) y `cantidad_unidades` (la cantidad efectivamente aplicada, manual u ordinaria, vive en `fal_acta_valorizacion_item`, no en el artículo infringido).
- **No persiste** valorización económica detallada ni puntero inverso al ítem actual (`fal_acta_valorizacion_item` apunta hacia acá, nunca al revés: sin FK circular).
- **Entidad Java:** `FalActaArticuloInfringido` (con `versionRow`, `fhBaja`/`idUserBaja` reales, sin fallback de fecha/usuario de alta como aproximación de baja).
- **Estado:** RECONCILIADA_WAVE4 (PROMPT-NOCTURNO-WAVE-04-PARTE-2 §7.4): OCC y auditoría de baja real agregadas, cantidad_unidades removida / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_acta_valorizacion`
- **Propósito:** cabecera única por acta con el total económico actual (PROMPT-NOCTURNO-WAVE-04-PARTE-2 §7.8). Reemplaza el modelo anterior de múltiples filas por acta+tipo con bandera de vigencia.
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_acta -> fal_acta`.
- **Restricción:** `UNIQUE(id_acta)` — una sola cabecera por acta.
- **Campos:** `version_row INT NOT NULL DEFAULT 0` (OCC; la cabecera es mutable), `monto_total_ordinario DECIMAL(14,2) NOT NULL DEFAULT 0`, `monto_total_pago_voluntario DECIMAL(14,2) NULL`, `estado_valorizacion SMALLINT NOT NULL` (`EstadoValorizacionActa`: PRELIMINAR/CONFIRMADA), `si_confirmada BOOLEAN NOT NULL DEFAULT FALSE`, `fh_calculo DATETIME(6) NOT NULL`, `fh_confirmacion DATETIME(6) NULL`, `id_user_confirmacion CHAR(36) NULL`.
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **No copia** los ~26 campos del modelo Java anterior (`origenValorizacion`, `criterioTarifario`, `tarifarioActualizado`, `tipoUnidadFinal`, `cantidadUnidadesFinal`, `valorUnidadFinal`, `tarifarioUnidadId`, `siSobrescribeTotal`, `siCongelaValor`, `fhCongelamiento`, `falloId`, `documentoId`, etc.): esos campos solo tenían sentido para un único tipo/unidad/tarifario global, incapaz de representar artículos heterogéneos con ítems propios. El detalle por artículo vive en `fal_acta_valorizacion_item`.
- **Entidad Java:** `FalActaValorizacion` (rediseñada). **Alta:** calcula la valorización inicial pero **no** crea obligación de pago (`si_confirmada=false`, `estado_valorizacion=PRELIMINAR`).
- **Estado:** RECONCILIADA_WAVE4 (PROMPT-NOCTURNO-WAVE-04-PARTE-2 §7.8): cabecera única por acta, campos recortados a los funcionalmente vigentes / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_acta_valorizacion_item`
- **Propósito:** historial económico versionado de CADA artículo imputado (hijo de `fal_acta_articulo_infringido`, no de la cabecera). `fal_acta` → `fal_acta_articulo_infringido` → `fal_acta_valorizacion_item` (versión 1, versión 2, ..., versión actual) (PROMPT-NOCTURNO-WAVE-04-PARTE-2 §7.5).
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_acta_articulo -> fal_acta_articulo_infringido`, `id_tarifario_articulo_usado -> fal_tarifario_unidad_faltas`, `id_valor_unidad_usado -> fal_valor_unidad_faltas` (NULL si `tipo_unidad=MONTO`).
- **Campos:** `version_item INT NOT NULL` (historial funcional, NO es OCC), `si_actual BOOLEAN NOT NULL DEFAULT TRUE`, `id_tarifario_articulo_usado BIGINT NOT NULL`, `id_valor_unidad_usado BIGINT NULL`, `modo_valorizacion_ordinaria SMALLINT NOT NULL` (`ModoValorizacionItem`: AUTOMATICA/MANUAL_UNIDADES/MANUAL_MONTO), `cantidad_manual_ordinaria DECIMAL(10,4) NULL`, `monto_manual_ordinario DECIMAL(14,2) NULL`, `modo_valorizacion_pago_voluntario SMALLINT NULL`, `cantidad_manual_pago_voluntario DECIMAL(10,4) NULL`, `monto_manual_pago_voluntario DECIMAL(14,2) NULL`, `motivo_cambio VARCHAR(255) NULL` (obligatorio si algún modo es manual), `observacion_cambio VARCHAR(500) NULL`, `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **No duplica** obligatoriamente el valor monetario automático: la reconstrucción usa `id_tarifario_articulo_usado` + `id_valor_unidad_usado`. Sin `version_row` (los ítems son inmutables una vez creados; cada cambio crea una fila nueva, nunca se sobrescribe una versión histórica).
- **Unicidad física requerida:** columna generada `id_acta_articulo_actual = IF(si_actual, id_acta_articulo, NULL)` con `UNIQUE(id_acta_articulo_actual)`: como máximo un ítem actual por artículo imputado. `UNIQUE(id_acta_articulo, version_item)` para el historial.
- **Entidad Java:** `FalActaValorizacionItem` (rediseñada; inmutable, sin `copia()` mutante). **Estado:** RECONCILIADA_WAVE4 (PROMPT-NOCTURNO-WAVE-04-PARTE-2 §7.5) / BASELINE_PRESERVADA_CON_DELTA.

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
- **Propósito:** nomenclatura catastral y rubro del acta de contravención/comercio, instanciada históricamente al labrar. **PK+FK:** `acta_id BIGINT` (1:1, solo `tipo_acta=CONTRAVENCION`/`COMERCIO`).
- **Campos — identificadores:** `id_suj_i TINYINT UNSIGNED NULL` (código tipo de sujeto en Ingresos Municipales; CHECK: BETWEEN 1 AND 255; catálogo abierto), `id_bie_i MEDIUMINT UNSIGNED NULL` (CHECK: BETWEEN 1 AND 9999999), `id_suj_c TINYINT UNSIGNED NULL` (código tipo de sujeto en Ingresos Municipales; CHECK: BETWEEN 1 AND 255; catálogo abierto), `id_bie_c MEDIUMINT UNSIGNED NULL` (CHECK: BETWEEN 1 AND 9999999).
- **Campos — nomenclatura catastral (tipos canónicos GEO-SPEC-001):** `circ SMALLINT NULL`, `secc VARCHAR(7) NULL`, `quinta VARCHAR(7) NULL` (quinta catastral; origen: `geo_qmat_parcela.quint` o `fal_inmueble.quinta`; ausente en Java/DDL actual — ver nota de delta), `frac VARCHAR(7) NULL`, `mza VARCHAR(7) NULL`, `parc VARCHAR(7) NULL`, `ufun VARCHAR(7) NULL`, `ucomp VARCHAR(3) NULL` (unidad complementaria; 3 caracteres suficientes). **`chacra` excluido de todo el modelo.**
- **Nota de delta (nomenclatura):** `secc` pasa de `VARCHAR(2)` a `VARCHAR(7)`; `ucomp` pasa de `VARCHAR(20)` a `VARCHAR(3)`; `quinta VARCHAR(7)` es campo nuevo. Java y DDL actuales tienen los valores anteriores; el ajuste es parte del trabajo de implementación pendiente (ver [`../../90-roadmap/current-roadmap.md`](../90-roadmap/current-roadmap.md)).
- **Campos — comercio no registrado (GEO-SPEC-001):** `si_comercio_no_registrado BOOLEAN NOT NULL DEFAULT FALSE`, `comercio_no_reg_nombre_fantasia VARCHAR(64) NULL` (obligatorio si `si_comercio_no_registrado=true`), `comercio_no_reg_titular VARCHAR(64) NULL` (opcional). Ausentes en Java/DDL actual — ver nota de delta. No agregar `comercio_no_reg_observaciones` ni campo libre adicional; las notas libres van a `fal_observacion(ACTA)`.
- **Campos — rubro y ámbito:** `origen_nomencl SMALLINT NOT NULL`, `si_nomenclatura_manual BOOLEAN NOT NULL`, `motivo_nomenclatura_manual SMALLINT NULL`, `rubro_id BIGINT NULL FK` (→ `fal_rubro_version.rubro_id`), `id_rub INT NULL`, `ambito_ctv SMALLINT NULL`, `ambito_ctv_txt VARCHAR(48) NULL` (obligatorio solo si `ambito_ctv=OTRO`).
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Entidad Java:** `FalActaContravencion` (idSujI/idBieI/idSujC/idBieC son `Integer`). **No existe** `nomenclatura_txt` como columna propia.
- **Invariantes de par inmueble/comercio:** `id_suj_i`/`id_bie_i` ambos nulos o ambos informados; todo comercio (registrado o no registrado) obliga el par inmueble; `id_suj_c`/`id_bie_c` ambos nulos o ambos informados; comercio registrado y `si_comercio_no_registrado=true` son mutuamente excluyentes; comercio registrado exige campos manuales nulos; comercio no registrado exige `id_suj_c`/`id_bie_c` nulos.
- **Reglas de comercio no registrado:** `si_comercio_no_registrado=true` → `id_suj_c`/`id_bie_c` null; `id_suj_i`/`id_bie_i` y nomenclatura obligatorios; nombre fantasía obligatorio; titular opcional. `si_comercio_no_registrado=false` → `comercio_no_reg_nombre_fantasia`/`comercio_no_reg_titular` null. Al cambiar de modo, datos incompatibles se limpian.
- **Observación de comercio no registrado:** se persiste en `fal_observacion(ACTA)` con `entidad_id=acta_id`, `id_acta_contexto=acta_id`, `OrigenObservacion.USUARIO`; creación dentro de la misma transacción funcional; sin columna embebida en `fal_acta_contravencion`. Ver spec completa en [`../../60-geo/geodomicilios.md`](../60-geo/geodomicilios.md).
- **Estado:** HUMAN_DECISION_CLOSED (SPEC-MODEL-DDL-CLOSURE-001 / FULL-R1.2-CORRECCION-10 / GEO-SPEC-001): tipos de nomenclatura canónicos cerrados (secc VARCHAR(7), quinta VARCHAR(7), ucomp VARCHAR(3)); comercio no registrado productivo; chacra excluido / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_acta_sustancias_alimenticias`
- **Propósito:** datos de actas de sustancias alimenticias/bromatología. **PK+FK:** `acta_id BIGINT` (1:1).
- **Campos:** `rubro_id BIGINT NULL FK` (→ `fal_rubro_version.rubro_id`), `id_rub INT NULL`, `ambito_ctv SMALLINT NULL`, `ambito_ctv_txt VARCHAR(48) NULL`, `descripcion_sustancias TEXT NULL`.
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Entidad Java:** `FalActaSustanciasAlimenticias`.
- **Estado:** HUMAN_DECISION_CLOSED (SPEC-MODEL-DDL-CLOSURE-001): ambito_ctv_txt VARCHAR(48) / BASELINE_PRESERVADA_CON_DELTA.

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
- **Campos:** `tipo_docu SMALLINT NOT NULL`, `nro_docu VARCHAR(30) NULL`, `estado_docu SMALLINT NOT NULL`, `storage_key VARCHAR(196) NULL` (clave técnica generada por backend; formato: faltas/actas/<uuid-acta>/documentos/<uuid-documento>.pdf; no es URL pública; no contiene binario), `hash_docu VARCHAR(128) NULL`, `tipo_firma_req SMALLINT NOT NULL`, `fh_generacion DATETIME(6) NULL`, `id_talonario BIGINT NULL FK`, `nro_talonario_usado INT NULL`.
- **Eliminado:** `descripcion` — derivable desde plantilla; nombre comunica uso.
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **versionRow/OCC:** `version_row INT NOT NULL DEFAULT 0` (`DECISION_DDL-DOC-01` CERRADA).
- **Entidad Java:** `FalDocumento`. **Puerto:** `DocumentoRepository` / `InMemoryDocumentoRepository`.
- **Estado:** HUMAN_DECISION_CLOSED (SPEC-MODEL-DDL-CLOSURE-001): storage_key VARCHAR(196), sin descripcion / BASELINE_PRESERVADA_CON_DELTA.
- **Integración EDA con Firmas (`DEC-EDA-014`/`024`):** `storage_key`/`hash_docu` son la única fuente de verdad del PDF vigente del documento en cualquier momento de su ciclo de firma (no se crea tabla 1:1 adicional); `hash_docu` se actualiza con `hashSalida` de cada firma válida. Columna física nueva: `revision_artefacto INT NOT NULL DEFAULT 0` (revisión del único PDF vigente; `0`=sin artefacto preparado; incrementa en 1 en cada preparación inicial y en cada firma válida aplicada; `CHECK (revision_artefacto >= 0)`). Ver `10-domain/firma-notificacion-fallo.md`, `firma-eda-physical-model.md` §3.

#### `fal_acta_documento`
- **Propósito:** tabla pivot relación canónica acta-documento (pertenencia funcional).
- **PK:** `id_acta + id_documento` (compuesta, sin ID artificial).
- **Campos:** `rol_docu_acta SMALLINT`, `si_principal BOOLEAN`, `fh_alta DATETIME(6)`, `id_user_alta CHAR(36)`.
- **Entidad Java:** `FalActaDocumento` + `ActaDocumentoId` (value object). **Puerto:** `ActaDocumentoRepository` / `InMemoryActaDocumentoRepository` (thread-safe, `principalLock`).
- **Servicio:** `ActaDocumentoService`. **Enum:** `RolDocuActa` (12 valores, código `short` estable).
- **Snapshot:** `FalActaSnapshot.idDocuUlt` derivado vía pivot (`FALLO > RESOLUCION > NOTIFICACION > ACTA_PRINCIPAL`).
- **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_documento_firma`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_documento -> fal_documento`, `(id_firmante, ver_firmante) -> fal_firmante_version`.
- **Campos:** `estado_firma SMALLINT NOT NULL`, `seq_firma_req SMALLINT NOT NULL`, `tipo_firma SMALLINT NOT NULL`, `storage_key VARCHAR(196) NULL` (mismo contrato que fal_documento.storage_key: clave técnica interna), `hash_firma VARCHAR(128) NULL`, `referencia_firma_ext VARCHAR(64) NULL` (id de transacción/solicitud/sobre del proveedor externo; no es la firma ni el certificado; NULL para firma interna), `fh_firma DATETIME(6) NULL`, `id_firmante BIGINT NULL FK`, `ver_firmante SMALLINT NULL`, `id_user_firma CHAR(36) NULL`, `nombre_firmante VARCHAR(48) NULL`, `mensaje_error VARCHAR(512) NULL`.
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Entidad Java:** `FalDocumentoFirma`. **Idempotencia:** `referenciaFirmaExt`.
- **Estado:** HUMAN_DECISION_CLOSED (SPEC-MODEL-DDL-CLOSURE-001): storage_key VARCHAR(196), referencia_firma_ext VARCHAR(64) / BASELINE_PRESERVADA_CON_DELTA.
- **Integración EDA con Firmas (`DEC-EDA-013`/`021`/`023`/`029`):** `hash_firma` es el hash de **entrada** de esa firma (el PDF vigente exacto que Firmas recibió y firmó, antes de aplicarla). Columnas físicas nuevas y definitivas, `PHYSICAL_MODEL_COMPLETE`/`DDL_READY`: `hash_salida VARCHAR(128) NULL`, `revision_entrada SMALLINT NULL`, `revision_salida SMALLINT NULL` (encadenamiento de hashes); actor técnico M2M (`actor_tecnico_tipo`, `actor_tecnico_id`, `actor_tecnico_app`, `actor_tecnico_key_fingerprint`, `correlation_id`, `event_id`, `received_at`); metadata PKI mínima (`perfil_firma`, `algoritmo_hash VARCHAR(16) NULL` — nunca `NOT NULL DEFAULT`, no se inventa un algoritmo retroactivo, `DEC-EDA-029` —, `resultado_firma_id`, `evidencia_id`, `certificado_serial`, `certificado_thumbprint`, `certificado_subject`, `certificado_issuer`, `certificado_vigencia_desde/hasta`, `resultado_cadena`, `fuente_revocacion`, `resultado_revocacion`, `tsa_nombre`, `tsa_instante`, `resultado_validacion` — catálogo `1=VALIDO, 2=INVALIDO, 3=VALIDO_CON_OBSERVACIONES_PERMITIDAS` —, `version_validador`). `CHECK chk_firma_eda_completo` condicional: si `resultado_firma_id IS NOT NULL`, exige informado el conjunto mínimo de columnas EDA (nunca para filas históricas/legacy sin `resultado_firma_id`). Definición completa y DDL en `firma-eda-physical-model.md` §2. `storage_key` de esta tabla no es fuente de verdad de ninguna ubicación física por firma; en el circuito EDA queda `NULL`/sin uso — la única referencia física vigente es siempre `fal_documento.storage_key` (`DEC-EDA-015`). Ver `10-domain/firma-notificacion-fallo.md`.

> **Nota de alcance (`DEC-EDA-024`):** `fal_documento_firma_resultado` (staging
> temporal e idempotente del candidato firmado cargado por `PUT
> .../resultados/{resultadoFirmaId}`, previo e independiente del efecto de
> dominio que aplica exclusivamente el consumidor de `DocumentoFirmado`) es
> una tabla nueva de este cierre, fuera del inventario canónico de 65 tablas
> de `database/design/canonical-table-inventory.md`. Su definición física
> completa (columnas, catálogo `estado_resultado`, índices, checks) vive
> únicamente en `firma-eda-physical-model.md` §4, igual que
> `fal_outbox`/`bus_evento`/`fal_bus_checkpoint`/`fal_inbox`/`fal_inbox_error`;
> no se agrega como entrada `#### \`...\`` a esta sección para no alterar el
> conteo cerrado de 65 encabezados de tabla del baseline canónico.

#### `fal_documento_firma_req`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_documento -> fal_documento`. **Entidad Java:** `FalDocumentoFirmaReq`. **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.
- **Integración EDA con Firmas (`DEC-EDA-016`/`020`):** lifecycle completo y columnas físicas nuevas y definitivas, `LOGICAL_MODEL_COMPLETE`/`PHYSICAL_MODEL_COMPLETE`/`DDL_READY`: `version_row INT NOT NULL DEFAULT 0` (OCC, nueva en esta tabla); catálogo `estado_firma_req` extendido de forma aditiva a 9 valores (`6=CANCELACION_SOLICITADA`, `7=CANCELADO`, `8=RECHAZADO`, `9=RECHAZADO_CONTRATO`, sin alterar los 5 valores existentes); `solicitud_firma_id CHAR(36) NULL`, `revision_solicitud SMALLINT NOT NULL DEFAULT 0`, `fh_solicitud`/`fh_aceptacion`/`fh_inicio_firma`/`vence_en`/`fh_terminal DATETIME(6) NULL`, `motivo_terminal_firma_req SMALLINT NULL`, `detalle_terminal_firma_req VARCHAR(255) NULL`. Definición completa, índices, checks y backfill en `firma-eda-physical-model.md` §1. Ver también `10-domain/firma-notificacion-fallo.md`, "Lifecycle único de solicitud/requisito de firma".

#### `fal_documento_plantilla`
- **PK:** `id BIGINT AUTO_INCREMENT`.
- **Campos:** `codigo VARCHAR(12) NOT NULL UNIQUE`, `nombre VARCHAR(64) NOT NULL` (debe comunicar uso e intención), `tipo_docu SMALLINT NOT NULL`, `accion_documental SMALLINT NOT NULL`, `tipo_acta SMALLINT NULL`, `tipo_firma_req SMALLINT NOT NULL`, `si_requiere_numeracion BOOLEAN NOT NULL`, `momento_numeracion_docu SMALLINT NOT NULL`, `si_notificable BOOLEAN NOT NULL`, `si_genera_pdf BOOLEAN NOT NULL`, `si_seleccionable BOOLEAN NOT NULL`, `si_activa BOOLEAN NOT NULL`, `fh_vig_desde DATE NOT NULL`, `fh_vig_hasta DATE NULL`.
- **Eliminado:** `descripcion` — nombre VARCHAR(64) comunica uso.
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Entidad Java:** `FalDocumentoPlantilla` (sin campo, constructor ni accessors de `descripcion`). **Estado:** HUMAN_DECISION_CLOSED (FULL-R1.2-CORRECCION-04): codigo VARCHAR(12), nombre VARCHAR(64), sin descripcion / BASELINE_PRESERVADA_CON_DELTA.
- **Contrato Java/API:** `CrearDocumentoPlantillaCommand`, `CrearDocumentoPlantillaRequest` y `DocumentoPlantillaResponse` no exponen `descripcion`; controller y service no la transportan ni la descartan silenciosamente.

#### `fal_documento_plantilla_firma_req`
- **PK:** `id_plantilla + seq_firma_req` (compuesta). **Entidad Java:** `FalDocumentoPlantillaFirmaReq`. **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_documento_plantilla_contenido`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_plantilla -> fal_documento_plantilla`.
- **Campos:** `version_contenido SMALLINT NOT NULL`, `titulo VARCHAR(64) NOT NULL`, `cuerpo_markdown TEXT NOT NULL`, `encabezado_markdown TEXT NULL`, `pie_markdown TEXT NULL`, `variables_declaradas_json JSON NOT NULL`, `si_activo BOOLEAN NOT NULL`, `fh_vig_desde DATETIME(6) NOT NULL`, `fh_vig_hasta DATETIME(6) NULL`, `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Contrato de `variables_declaradas_json`:** arreglo de metadatos declarativos; usar exactamente `[]` cuando la plantilla no declara variables. Cada elemento es un objeto y debe contener `namespace`, `campo`, `tipoDato`, `requerida` y `etiqueta`. `tipoDato` tipa el valor esperado, `requerida` es un booleano explícito y `etiqueta` es el nombre legible para UI/diagnóstico. No contiene valores resueltos ni reemplaza `variables_snapshot_json` de la redacción.
- **Entidad Java:** `FalDocumentoPlantillaContenido` (`MAX_TITULO_LENGTH = 64`; metadatos documentados sin incorporar parser JSON). **Estado:** HUMAN_DECISION_CLOSED (FULL-R1.2-CORRECCION-04): titulo VARCHAR(64), variables declaradas tipadas/requeridas/etiquetadas / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_documento_plantilla_default`
- **PK:** `id BIGINT AUTO_INCREMENT`. **Índice de consulta:** `(accion_documental, tipo_acta, id_dependencia)`.
- **Campos:** `accion_documental SMALLINT NOT NULL`, `tipo_acta SMALLINT NULL` (NULL = genérico), `tipo_docu SMALLINT NOT NULL`, `id_dependencia BIGINT NULL`, `ver_dependencia SMALLINT NULL`, `id_plantilla BIGINT NOT NULL FK`, `prioridad SMALLINT NOT NULL`, `fh_vig_desde DATETIME(6) NOT NULL`, `fh_vig_hasta DATETIME(6) NULL`, `si_activo BOOLEAN NOT NULL`, `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Entidad Java:** `FalDocumentoPlantillaDefault`. **Estado:** LISTO_PARA_DDL / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_documento_redaccion`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_documento -> fal_documento`, `id_plantilla_contenido -> fal_documento_plantilla_contenido`, `redaccion_origen_id -> fal_documento_redaccion` (self). **UK:** `id_documento + nro_revision`.
- **Campos:** `nro_revision SMALLINT NOT NULL`, `redaccion_origen_id BIGINT NULL`, `estado_redaccion SMALLINT NOT NULL` (`BORRADOR=1, CONFIRMADA=2, ANULADA=3`), `contenido_editable_markdown TEXT NOT NULL`, `variables_snapshot_json JSON NOT NULL DEFAULT '{}'`, campos de auditoría por hito (creación/confirmación/anulación).
- **Eliminado:** `contenido_base_markdown` — no duplicar cuerpo de plantilla. `variables_faltantes_json`, `diagnostico_json`, `recursos_snapshot_json` — errores se resuelven antes de confirmar; no persistir estado transitorio de validación.
- **Reglas:** no se confirma una redacción con variables faltantes; `contenido_editable_markdown` conserva cambios manuales; `variables_snapshot_json` congela los valores utilizados.
- **versionRow/OCC:** `version_row INT NOT NULL DEFAULT 0` (`versionRow` en `FalDocumentoRedaccion`, default 0). **Repositorio OCC:** `InMemoryDocumentoRedaccionRepository` con semántica copy-on-read/write.
- **Entidad Java:** `FalDocumentoRedaccion` (`versionRow` presente). **Servicios:** `DocumentoCombinacionService`, `DocumentoVariableContextBuilder`, `DocumentoVariableRegistry`, `DocumentoRedaccionService`.
- **Estado:** HUMAN_DECISION_CLOSED (SPEC-MODEL-DDL-CLOSURE-001): sin contenido_base_markdown/variables_faltantes_json/diagnostico_json/recursos_snapshot_json; OCC confirmado / BASELINE_PRESERVADA_CON_DELTA.

### Notificaciones

#### `fal_notificacion`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_acta -> fal_acta`, `id_documento -> fal_documento`.
- **Campos:** `tipo_docu_notificado SMALLINT`, `canal SMALLINT NULL` (CanalNotificacion: 1=CORREO_POSTAL, 2=NOTIFICADOR_MUNICIPAL, 3=PRESENCIAL, 4=PORTAL_INFRACTOR, 5=EMAIL; NULL en estado PENDIENTE_ENVIO; NOT NULL tras iniciarEnvio), `fecha_envio DATETIME(6)`, `estado SMALLINT`, `resultado SMALLINT NULL`, `fecha_resultado DATETIME(6) NULL`, `intentos INT`.
- **Eliminado:** `observaciones TEXT` — centralizar en fal_observacion(NOTIFICACION).
- **Entidad Java:** `FalNotificacion` (id `Long`). **Puerto:** `NotificacionRepository` / `InMemoryNotificacionRepository`.
- **versionRow/OCC:** `version_row INT NOT NULL DEFAULT 0` (`DECISION_DDL-NOTI-01` CERRADA).
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`, `fh_ult_mod DATETIME(6) NULL` (HUMAN_DECISION_CLOSED CORRECCION-07: nullable hasta primera modificacion), `id_user_ult_mod CHAR(36) NULL`.
- **Estado:** HUMAN_DECISION_CLOSED (SPEC-MODEL-DDL-CLOSURE-001): canal SMALLINT, sin observaciones / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_notificacion_intento`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_notificacion -> fal_notificacion`.
- **Campos:** `nro_intento SMALLINT NOT NULL`, `canal_notif SMALLINT NOT NULL` (CanalNotificacion: 1=CORREO_POSTAL, 2=NOTIFICADOR_MUNICIPAL, 3=PRESENCIAL, 4=PORTAL_INFRACTOR, 5=EMAIL; nombre canonico SPEC-MODEL-DDL-CLOSURE-001; DDL y Java usan canal_notif), `estado_intento SMALLINT NOT NULL`, `resultado_intento SMALLINT NULL`, `domicilio_notif_id BIGINT NULL FK`, `destino_digital VARCHAR(120) NULL`, `lote_id BIGINT NULL FK`, `referencia_externa VARCHAR(80) NULL`, `fh_intento DATETIME(6) NOT NULL`, `fh_resultado DATETIME(6) NULL`.
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`, `fh_ult_mod DATETIME(6) NULL` (HUMAN_DECISION_CLOSED CORRECCION-07: nullable hasta primera modificacion), `id_user_ult_mod CHAR(36) NULL`.
- **Entidad Java:** `FalNotificacionIntento`. **Estado:** HUMAN_DECISION_CLOSED (SPEC-MODEL-DDL-CLOSURE-001): canal SMALLINT (no VARCHAR) / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_notificacion_acuse`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `id_notificacion -> fal_notificacion`, `intento_id -> fal_notificacion_intento` (NULL).
- **Campos:** `tipo_acuse SMALLINT NOT NULL`, `canal SMALLINT NOT NULL` (CanalNotificacion: 1=CORREO_POSTAL, 2=NOTIFICADOR_MUNICIPAL, 3=PRESENCIAL, 4=PORTAL_INFRACTOR, 5=EMAIL), `estado_acuse SMALLINT NOT NULL`, `storage_key VARCHAR(196) NULL`, `fh_acuse DATETIME(6) NULL`.
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Entidad Java:** `FalNotificacionAcuse`. **Estado:** HUMAN_DECISION_CLOSED (SPEC-MODEL-DDL-CLOSURE-001): canal SMALLINT, storage_key VARCHAR(196) / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_lote_correo`
- **PK:** `id BIGINT AUTO_INCREMENT`.
- **Campos:** `lote_codigo CHAR(20) NOT NULL UNIQUE` (formato LC-YYYYMMDD-NNNNNNNN; generado por backend; inmutable), `tipo_lote SMALLINT NOT NULL`, `fecha_generacion DATETIME(6) NOT NULL`, `estado_lote SMALLINT NOT NULL`.
- **Auditoría:** `id_user_alta CHAR(36) NOT NULL`, `fh_alta DATETIME(6) NOT NULL`.
- **CHECK lote_codigo:** `lote_codigo REGEXP '^LC-[0-9]{8}-[0-9]{8}$'`.
- **Entidad Java:** `FalLoteCorreo`. **Puerto:** `LoteCorreoRepository` / `InMemoryLoteCorreoRepository`.
- **Estado:** HUMAN_DECISION_CLOSED (SPEC-MODEL-DDL-CLOSURE-001): lote_codigo CHAR(20) formato LC-YYYYMMDD-NNNNNNNN / BASELINE_PRESERVADA_CON_DELTA.

### Fallo y firmeza

#### `fal_acta_fallo`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK conceptuales:** `id_acta -> fal_acta`, `valorizacion_id -> fal_acta_valorizacion`, `fallo_reemplazado_id -> fal_acta_fallo` (self), `documento_id -> fal_documento`.
- **Campos:** `tipo_fallo SMALLINT NOT NULL`, `estado_fallo SMALLINT NOT NULL` (`EstadoFalloActa`, `EXPLICIT_NUMERIC_CODE`; códigos: `PENDIENTE_FIRMA=1, PENDIENTE_NOTIFICACION=2, NOTIFICADO=3, FIRME=4, REEMPLAZADO=5, SIN_EFECTO=6`; `DECISION_DDL-ENUM-01` CERRADA), `monto_condena DECIMAL(14,2) NULL`, `resultado_fallo SMALLINT NULL`, `fundamentos TEXT NULL`, `documento_id BIGINT NULL`, `valorizacion_id BIGINT NULL`, `fallo_reemplazado_id BIGINT NULL`, `fh_dictado DATETIME(6) NOT NULL`, `id_user_dictado CHAR(36) NULL`, `fh_firma DATETIME(6) NULL`, `fh_notificacion DATETIME(6) NULL`, `fh_vto_apelacion DATE NULL`, `fh_firmeza DATETIME(6) NULL`, `origen_firmeza SMALLINT NULL` (`1=VENCIMIENTO_PLAZO_APELACION, 2=APELACION_RECHAZADA`), `si_apelable BOOLEAN NOT NULL DEFAULT FALSE`, `si_firme BOOLEAN NOT NULL DEFAULT FALSE`, `si_vigente BOOLEAN NOT NULL DEFAULT TRUE`, `version_row INT NOT NULL DEFAULT 0`.
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Entidad Java:** `FalActaFallo` (id `Long`, `versionRow` presente). **Puerto:** `FalloActaRepository` / `InMemoryFalloActaRepository`.
- **Mapeo de nombres históricos → canónicos:** `fecha_dictado` → `fh_dictado` · `fecha_notificacion` → `fh_notificacion` · `si_activo` → `si_vigente` · `fh_vto_apelacion DATETIME(6)` → `fh_vto_apelacion DATE` · campos `fecha_resultado_final`, `fh_mod`, `id_user_mod` eliminados (no existen en Java ni DDL).
- **Estado:** HUMAN_DECISION_CLOSED (SPEC-MODEL-DDL-CLOSURE-001): fh_dictado/fh_notificacion/si_vigente/fh_vto_apelacion DATE/si_apelable confirmados / BASELINE_PRESERVADA_CON_DELTA.

> **Nota de firmeza:** la firmeza de condena es inline en `fal_acta_fallo`
> (`si_firme`, `fh_firmeza`, `origen_firmeza`). No existe una tabla propia de
> firmeza. `FalActaFirmezaCondena` y `FirmezaCondenaRepository` están
> eliminados del código Java vigente (verificado contra el árbol de fuentes).
> Ver `../20-application/fallo-command-contracts.md` (CMD-FALLO-005/006) para
> el contrato de firmeza vigente.

### Apelación

#### `fal_acta_apelacion`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK conceptuales:** `id_acta -> fal_acta`, `fallo_id -> fal_acta_fallo`, `documento_resolucion_id -> fal_documento`.
- **Campos:** `fallo_id BIGINT NOT NULL`, `estado_apelacion SMALLINT NOT NULL` (EstadoApelacionActa; códigos: PRESENTADA=1, EN_ANALISIS=2, RECHAZADA=3, ACEPTADA_ABSUELVE=4, RESUELTA=5, SIN_EFECTO=6), `fecha_presentacion DATETIME(6) NOT NULL`, `canal_apelacion SMALLINT NOT NULL`, `tipo_presentacion SMALLINT NOT NULL`, `presentante VARCHAR(64) NULL`, `texto_apelacion TEXT NULL`, `fundamentos TEXT NULL`, `si_activa BOOLEAN NOT NULL`, `fecha_resolucion DATETIME(6) NULL`, `resultado_resolucion SMALLINT NULL`, `fundamentos_resolucion TEXT NULL`, `documento_resolucion_id BIGINT NULL`, `version_row INT NOT NULL DEFAULT 0`.
- **Eliminado:** `observaciones TEXT`, `observaciones_resolucion TEXT` — centralizar en fal_observacion(APELACION).
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`, `fh_mod DATETIME(6) NULL`, `id_user_mod CHAR(36) NULL`.
- **Entidad Java:** `FalActaApelacion` (id `Long`, `falloId Long`, `versionRow` presente). **Puerto:** `ApelacionActaRepository` / `InMemoryApelacionActaRepository`.
- **Estado:** HUMAN_DECISION_CLOSED (SPEC-MODEL-DDL-CLOSURE-001): presentante VARCHAR(64), sin observaciones/observaciones_resolucion, conserva texto_apelacion + fundamentos + fundamentos_resolucion / BASELINE_PRESERVADA_CON_DELTA.

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
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `obligacion_pago_id -> fal_acta_obligacion_pago`, `forma_pago_id -> fal_acta_forma_pago` (NULL), `plan_pago_ref_id -> fal_acta_plan_pago_ref` (NULL), `movimiento_origen_id -> fal_acta_pago_movimiento` (self, NULL).
- **Campos — identificadores y tipo:** `obligacion_pago_id BIGINT NOT NULL`, `forma_pago_id BIGINT NULL`, `plan_pago_ref_id BIGINT NULL`, `tipo_movimiento SMALLINT NOT NULL`, `origen_movimiento SMALLINT NOT NULL`, `origen_confirmacion SMALLINT NULL`, `evidencia_documento_id BIGINT NULL`, `clasificacion_pago SMALLINT NOT NULL DEFAULT 1`, `nro_cuota SMALLINT NULL`.
- **Campos — importes:** `importe_capital DECIMAL(14,2) NULL`, `importe_rima DECIMAL(14,2) NULL`, `importe_total DECIMAL(14,2) NULL`.
- **Campos — referencias EM/PG:** `cmte_em CHAR(2) NULL`, `pref_em SMALLINT NULL`, `nro_em INT NULL`, `cmte_pg CHAR(2) NULL`, `pref_pg SMALLINT NULL`, `nro_pg INT NULL`.
- **Campos — contexto:** `id_cierre BIGINT NULL`, `id_ope BIGINT NULL`, `movimiento_origen_id BIGINT NULL FK self`, `motivo_anulacion_pago SMALLINT NULL`.
- **Eliminado:** `motivo_aplicacion_pago_anterior VARCHAR(500)` — texto libre no estructurado. Idempotencia via movimiento_origen_id + estructuras cerradas. Notas humanas a fal_observacion(MOVIMIENTO_PAGO).
- **Campos — fechas y referencia:** `fh_pago_procesado DATETIME(6) NULL`, `fh_pago_confirmado DATETIME(6) NULL`, `referencia_externa VARCHAR(80) NULL`, `fh_movimiento DATETIME(6) NOT NULL`.
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Entidad Java:** `FalActaPagoMovimiento` (id `Long`, sin `versionRow`). **Puerto:** `PagoMovimientoRepository` / `InMemoryPagoMovimientoRepository`.
- **Decisiones:** `DECISION_DDL-MOV-01` (tipos de importes DECIMAL(14,2) nullable), `DECISION_DDL-MOV-02` (referencias EM/PG cerradas), `DECISION_DDL-MOV-03` (movimiento_origen_id self-FK para idempotencia), `DECISION_DDL-MOV-04` (ClasificacionPago SMALLINT NOT NULL DEFAULT 1; divergencia entre Java y histórico: DEFAULT 1 = CUENTA_CORRIENTE; closed).
- **Estado:** HUMAN_DECISION_CLOSED (SPEC-MODEL-DDL-CLOSURE-001): sin motivo_aplicacion_pago_anterior / BASELINE_PRESERVADA_CON_DELTA / RECONCILIADA_R3.

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
- **Campos:** `codigo VARCHAR(8) NOT NULL UNIQUE`, `apodo VARCHAR(20) NOT NULL` (nombre corto operativo), `descripcion VARCHAR(64) NOT NULL`, `clase_numeracion SMALLINT NOT NULL` (`ClaseNumeracion`; ACTA=1/DOCUMENTO=2), `si_reinicio_anual BOOLEAN NOT NULL`, `si_incluye_prefijo BOOLEAN NOT NULL`, `prefijo VARCHAR(10) NULL`, `si_incluye_anio BOOLEAN NOT NULL`, `formato_anio SMALLINT NULL` (2 o 4 dígitos), `si_incluye_serie BOOLEAN NOT NULL`, `longitud_nro SMALLINT NULL`, `formato_visible VARCHAR(60) NOT NULL` (ej. `{PREF}-{ANIO}-{SERIE}-{NRO}`), `si_activa BOOLEAN NOT NULL`, `fh_vig_desde DATE NOT NULL`, `fh_vig_hasta DATE NULL` (NULL si vigente).
- **CHECK:** `clase_numeracion IN (1, 2)` (restaurado desde R1.3).
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`, `fh_ult_mod DATETIME(6) NULL` (HUMAN_DECISION_CLOSED CORRECCION-07: nullable hasta primera modificacion), `id_user_ult_mod CHAR(36) NULL`.
- **Entidad Java:** `NumPolitica` (id `Long`, sin `versionRow`). **Puerto:** `NumeracionRepository` / `InMemoryNumeracionRepository`.
- **Estado:** HUMAN_DECISION_CLOSED (SPEC-MODEL-DDL-CLOSURE-001): codigo VARCHAR(8), apodo VARCHAR(20), descripcion VARCHAR(64) / BASELINE_PRESERVADA_CON_DELTA / RECONCILIADA_R3.

#### `num_talonario`
- **Propósito:** talonario concreto (electrónico o manual físico).
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `politica_id -> num_politica`.
- **Campos:** `version_row INT NOT NULL DEFAULT 0` (OCC), `politica_id BIGINT NOT NULL`, `codigo VARCHAR(8) NOT NULL UNIQUE`, `descripcion VARCHAR(48) NOT NULL`, `tipo_talonario SMALLINT NOT NULL` (`TipoTalonario`; ELECTRONICO/MANUAL_FISICO), `clase_talonario SMALLINT NOT NULL` (`ClaseNumeracion`), `anio SMALLINT NULL` (obligatorio si política reinicia anual), `serie VARCHAR(12) NULL`, `nro_desde INT NOT NULL`, `nro_hasta INT NULL` (NULL si sin límite operativo), `nombre_secuencia VARCHAR(64) NOT NULL UNIQUE` (identificador lógico heredado; ningún objeto `SEQUENCE` de MariaDB se crea ni se consulta con él — ver decisión de numeración electrónica más abajo), `ultimo_nro_emitido INT NOT NULL DEFAULT 0` (contador transaccional real del correlativo electrónico), `si_activo BOOLEAN NOT NULL`, `si_bloqueado BOOLEAN NOT NULL DEFAULT FALSE`, `cod_desbloqueo VARCHAR(64) NULL`.
- **Eliminado:** `obs_talonario` — notas humanas van a `fal_observacion`.
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Decisión de numeración electrónica (PROMPT-NOCTURNO-WAVE-04-PARTE-2 §8.3):** el correlativo oficial del acta NO usa `SEQUENCE` de MariaDB (`NEXT VALUE FOR`). La transacción del alta hace todo el trabajo (validaciones, persistencia de raíz, satélites, valorización, evidencias) **antes** de tocar el talonario; recién en una sección crítica breve al final se ejecuta `SELECT ultimo_nro_emitido FROM num_talonario WHERE id=:id FOR UPDATE`, se calcula `siguiente = GREATEST(ultimo_nro_emitido, nro_desde-1)+1`, se valida contra `nro_hasta`, se hace `UPDATE ... SET ultimo_nro_emitido=:siguiente` y se asigna `nro_acta`/`id_talonario`/`nro_talonario_usado` al acta. Un rollback posterior a la sección crítica revierte el `UPDATE` junto con el resto de la transacción: el número queda libre y reutilizable, no hay número "quemado". La numeración electrónica de actas **no** inserta fila en `num_talonario_movimiento` (reservado al circuito de talonario manual físico) ni usa `num_talonario_inspector`.
- **Entidad Java:** `NumTalonario` (`versionRow` presente). **Puerto:** `TalonarioRepository` / `InMemoryTalonarioRepository` / `JdbcTalonarioRepository`. Método: `estaOperativo()` = `siActivo && !siBloqueado`.
- **Estado:** RECONCILIADA_WAVE4 (PROMPT-NOCTURNO-WAVE-04-PARTE-2 §8.3): agregado `ultimo_nro_emitido` como contador transaccional real; `nombre_secuencia` se conserva como identificador lógico heredado sin objeto `SEQUENCE` real; codigo VARCHAR(8), descripcion VARCHAR(48), nombre_secuencia VARCHAR(64), cod_desbloqueo VARCHAR(64), sin obs_talonario / BASELINE_PRESERVADA_CON_DELTA / RECONCILIADA_R3.

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
- **Campos:** `id_talonario BIGINT NOT NULL`, `nro_talonario INT NOT NULL`, `estado_numero SMALLINT NOT NULL` (`EstadoNumeroTalonario`; USADO=1/ANULADO=2/DEVUELTO_SIN_USAR=3/RENDIDO=4/JUSTIFICADO=5), `motivo_anulacion SMALLINT NULL` (`MotivoAnulacionTalonario`; obligatorio si estado=ANULADO), `acta_id BIGINT NULL` (obligatorio si fue usado para acta), `documento_id BIGINT NULL` (obligatorio si fue usado para documento), `id_dep BIGINT NULL`, `ver_dep SMALLINT NULL`, `id_insp BIGINT NULL`, `ver_insp SMALLINT NULL`, `fh_movimiento DATETIME(6) NOT NULL`, `id_user_movimiento CHAR(36) NOT NULL`.
- **Eliminado:** `observacion VARCHAR(500)` — centralizar en fal_observacion(TALONARIO_MOVIMIENTO).
- **Nota:** `NumTalonarioMovimiento` no tiene `fh_alta`/`id_user_alta` separados; usa `fh_movimiento`/`id_user_movimiento`.
- **Entidad Java:** `NumTalonarioMovimiento` (id `Long`, sin `versionRow`; append-only: no `set*`).
- **Estado:** HUMAN_DECISION_CLOSED (SPEC-MODEL-DDL-CLOSURE-001): sin observacion, tipos FK exactos (ver_dep/ver_insp SMALLINT) / BASELINE_PRESERVADA_CON_DELTA / RECONCILIADA_R3.

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
- **Propósito:** log append-only de auditoría de accesos realizados vía código QR (válidos e inválidos). Registra fecha, IP/origen técnico, user-agent y resultado.
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `acta_id -> fal_acta`.
- **Campos:** `acta_id BIGINT NOT NULL`, `fh_acceso DATETIME(6) NOT NULL`, `ip_origen VARCHAR(45) NULL` (validado IPv4/IPv6), `user_agent VARCHAR(255) NULL` (sanitizado), `resultado_acceso_qr SMALLINT NOT NULL` (ResultadoAccesoQr: leer códigos del enum canónico; NO inventar valores).
- **Eliminado:** `canal_acceso_qr` — mientras el único canal sea WEB no aporta variabilidad; no persistir constante sin variabilidad funcional.
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`.
- **Entidad Java:** `FalActaQrAcceso` (inmutable). **Puerto:** `QrAccesoRepository` / `InMemoryQrAccesoRepository`. **Servicio:** `QrActaService`. **Protección de token:** `AesGcmQrTokenProtector`.
- **No almacena el token QR** ni URL, fecha de generación ni fecha de vencimiento: esta tabla es un log append-only de accesos realizados. Los tokens se generan por `AesGcmQrTokenProtector` y nunca se persisten.
- **No existen** `token_qr`, `url_acceso`, `fh_generacion`, `fh_vencimiento` ni `si_activo`: esta tabla es un log append-only de accesos.
- **Append-only:** SÍ.
- **Estado:** HUMAN_DECISION_CLOSED (SPEC-MODEL-DDL-CLOSURE-001): sin canal_acceso_qr, resultado_acceso_qr SMALLINT enum canónico / BASELINE_PRESERVADA_CON_DELTA.

### Catálogos de dominio adicionales

#### `fal_vehiculo_marca`
- **PK:** `id BIGINT AUTO_INCREMENT`.
- **Campos:** `codigo VARCHAR(12) NOT NULL UNIQUE`, `nombre VARCHAR(24) NOT NULL`, `si_activa BOOLEAN NOT NULL`, `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Regla:** `UNIQUE` funcional por `codigo`; no usar `UNIQUE(nombre)` como reemplazo del código.
- **Entidad Java:** `FalVehiculoMarca`. **Puerto:** `VehiculoMarcaRepository` / `InMemoryVehiculoMarcaRepository`.
- **Estado:** HUMAN_DECISION_CLOSED (SPEC-MODEL-DDL-CLOSURE-001): nombre VARCHAR(24), conserva codigo/UNIQUE/flags/auditoría R1.3 / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_vehiculo_modelo`
- **PK:** `id BIGINT AUTO_INCREMENT`. **FK:** `marca_vehiculo_id -> fal_vehiculo_marca`.
- **Campos:** `marca_vehiculo_id BIGINT NOT NULL`, `codigo VARCHAR(12) NOT NULL`, `nombre VARCHAR(24) NOT NULL`, `si_activo BOOLEAN NOT NULL`, `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`.
- **Restricción:** `UNIQUE(marca_vehiculo_id, codigo)` — la longitud de `nombre` coincide con el fallback de `fal_acta_vehiculo`.
- **Entidad Java:** `FalVehiculoModelo`. **Puerto:** `VehiculoModeloRepository` / `InMemoryVehiculoModeloRepository`.
- **Estado:** HUMAN_DECISION_CLOSED (SPEC-MODEL-DDL-CLOSURE-001): nombre VARCHAR(24), conserva codigo/UNIQUE(marca,codigo)/FK/flags/auditoría / BASELINE_PRESERVADA_CON_DELTA.

#### `fal_rubro_version`
- **Entidad Java:** `FalRubroVersion`. Tabla de dominio Faltas con sincronización externa (Informix); no es infraestructura técnica.
- **Campos versionados (histórico 2026-06-23):** `row_hash`, `previous_row_hash`, `source_operation`, `close_operation`, `si_version_actual`, `valid_from`, `valid_to`, `synced_at`.
- **Puerto:** `RubroVersionRepository` / `InMemoryRubroVersionRepository`.
- **Estado:** PREEXISTING_CANONICAL_ADOPTED — tabla preexistente en el baseline; el DDL del dominio la adopta sin crearla, alterarla ni administrar sus datos. Unica tabla Faltas preexistente de las 65 canonicas (ver `50-persistence/ddl-execution-and-test-seeding.md`).

#### `fal_motivo_archivo`
- **PK:** `id BIGINT AUTO_INCREMENT`.
- **Campos:** `cod_motivo_archivo VARCHAR(8) NOT NULL UNIQUE`, `nombre VARCHAR(64) NOT NULL`, `si_nulidad BOOLEAN NOT NULL`, `si_permite_reingreso BOOLEAN NOT NULL`, `si_requiere_observacion BOOLEAN NOT NULL`, `si_activo BOOLEAN NOT NULL`, `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`, `fh_ult_mod DATETIME(6) NULL` (HUMAN_DECISION_CLOSED CORRECCION-07: nullable hasta primera modificacion), `id_user_ult_mod CHAR(36) NULL`.
- **Entidad Java:** `FalMotivoArchivo`. **Puerto:** `MotivoArchivoRepository` / `InMemoryMotivoArchivoRepository`.
- **Estado:** HUMAN_DECISION_CLOSED (SPEC-MODEL-DDL-CLOSURE-001): cod_motivo_archivo VARCHAR(8), nombre VARCHAR(64), conserva si_nulidad/si_permite_reingreso/si_requiere_observacion/auditoría / BASELINE_PRESERVADA_CON_DELTA.

### Calendario y excepciones

#### `fal_dia_no_computable`
- **Propósito:** registro de excepciones locales al calendario administrativo (feriados, asuetos). Las reglas fijas (domingo, 1-ene, 1-may) no se persisten; ver `../10-domain/calendario-plazos-administrativos.md`.
- **PK:** `id BIGINT AUTO_INCREMENT`.
- **Campos:** `fecha DATE NOT NULL`, `tipo SMALLINT NOT NULL` (`TipoDiaNoComputable`; FERIADO=1/ASUETO_ADMINISTRATIVO=2/OTRO=3), `descripcion VARCHAR(64) NOT NULL`, `origen SMALLINT NOT NULL` (`OrigenDiaNoComputable`; MANUAL=1/SINCRONIZACION_EXTERNA=2), `referencia_externa VARCHAR(200) NULL` (obligatoria para SINCRONIZACION_EXTERNA), `si_activo BOOLEAN NOT NULL DEFAULT TRUE`.
- **Auditoría:** `fh_alta DATETIME(6) NOT NULL`, `id_user_alta CHAR(36) NOT NULL`, `fh_baja DATETIME(6) NULL`, `id_user_baja CHAR(36) NULL`.
- **CHECK:** `tipo IN (1, 2, 3)`; `origen IN (1, 2)`; `origen = 2 implies referencia_externa IS NOT NULL`.
- **UK/unicidad:** columna generada `fecha_activa DATE GENERATED ALWAYS AS (CASE WHEN si_activo THEN fecha ELSE NULL END) STORED` con `UNIQUE(fecha_activa)` — un solo día activo por fecha.
- **OCC/versionRow:** no aplica; desactivación vía `si_activo=false`.
- **Nombres correctos:** `tipo` y `origen` (no `tipo_dia` ni `origen_dia`).
- **Enums:** `TipoDiaNoComputable` y `OrigenDiaNoComputable` usan estrategia EXPLICIT_NUMERIC_CODE (`DECISION_DDL-ENUM-01` CERRADA): SMALLINT en DDL, código numérico explícito en Java.
- **Entidad Java:** `FalDiaNoComputable`. **Puertos:** `DiaNoComputableRepository` / `InMemoryDiaNoComputableRepository`. **Fuente normativa:** `../10-domain/calendario-plazos-administrativos.md`.
- **Estado:** HUMAN_DECISION_CLOSED (SPEC-MODEL-DDL-CLOSURE-001): descripcion VARCHAR(64), nombres canónicos tipo/origen / BASELINE_PRESERVADA_CON_DELTA.

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
