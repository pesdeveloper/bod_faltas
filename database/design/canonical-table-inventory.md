# Inventario canónico de tablas — BOD Faltas MariaDB

> **Trabajo:** DDL-MARIADB-MANUAL-001-FULL-R1
> **Fuente normativa:** `50-persistence/mariadb-logical-model.md`, `50-persistence/ddl-decisions.md`
>
> Conteo: **65 tablas canónicas** = 1 PREEXISTING_CANONICAL_ADOPTED + 64 TO_CREATE.
> Excluidas del inventario: `fal_informix_sync_error`, `fal_informix_sync_run` (no son tablas del dominio Faltas).
> Excluidas del inventario: todas las `geo_*` (infraestructura geográfica externa).

## Leyenda de columnas

| Columna | Descripción |
|---|---|
| **#** | Orden topológico de creación |
| **Nombre físico** | Nombre de la tabla en MariaDB (lower_case snake_case) |
| **Estado** | `PREEXISTING_CANONICAL_ADOPTED` o `TO_CREATE` |
| **Familia** | Grupo funcional |
| **Java** | Entidad Java principal |
| **Puerto** | Repository/puerto canónico |
| **PK** | Clave primaria física |
| **FK salientes** | FKs que esta tabla emite (→ tabla destino) |
| **FK entrantes** | Tablas que referencian esta tabla (conocidas) |
| **Tipo** | `append-only` / `mutable` / `catálogo` / `proyección` |
| **OCC** | Control de concurrencia optimista (`version_row`) |
| **DECISION_DDL** | Decisiones físicas aplicables |
| **Slice físico** | Grupo de creación previsto |

---

## Grupo G1 — Catálogos y raíces sin dependencias externas

| # | Nombre físico | Estado | Familia | Java | Puerto | PK | FK salientes | FK entrantes (conocidas) | Tipo | OCC | DECISION_DDL | Slice |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | `fal_dependencia` | TO_CREATE | Catálogos internos | `FalDependencia` | `DependenciaRepository` | `id BIGINT AUTO_INCREMENT` | `id_dep_padre → fal_dependencia` (self, nullable, agregar vía ALTER) | `fal_dependencia_version`, `fal_firmante_version`, `fal_inspector` (lógica), `fal_acta`, `num_talonario_ambito`, `fal_dependencia_normativa`, `fal_documento_plantilla_default` | mutable | N | — | R2+ |
| 2 | `fal_inspector` | TO_CREATE | Catálogos internos | `FalInspector` | `InspectorRepository` | `id BIGINT AUTO_INCREMENT` | — | `fal_inspector_version`, `fal_acta` | mutable | N | — | R2+ |
| 3 | `fal_firmante` | TO_CREATE | Identidad/firmantes | `FalFirmante` | `FirmanteRepository` | `id BIGINT AUTO_INCREMENT` | — | `fal_firmante_version`, `fal_documento_firma` | mutable | N | — | R1 |
| 4 | `fal_persona` | TO_CREATE | Personas/domicilios | `FalPersona` | `PersonaRepository` | `id BIGINT AUTO_INCREMENT` | — | `fal_persona_domicilio`, `fal_acta`, `fal_acta_obligacion_pago` | mutable | N | `DECISION_DDL-PERS-01` (GAP-DDL-R1-PERSONA-LONGITUD-01 CERRADO) | R1 |
| 5 | `fal_vehiculo_marca` | TO_CREATE | Catálogos de dominio | `FalVehiculoMarca` | `VehiculoMarcaRepository` | `id BIGINT AUTO_INCREMENT` | — | `fal_vehiculo_modelo`, `fal_acta_vehiculo` | catálogo | N | — | R1 |
| 6 | `num_politica` | TO_CREATE | Talonarios/numeración | `NumPolitica` | `NumeracionRepository` | `id BIGINT AUTO_INCREMENT` | — | `num_talonario` | catálogo | N | — | R1 |
| 7 | `fal_dia_no_computable` | TO_CREATE | Calendario | `FalDiaNoComputable` | `DiaNoComputableRepository` | `id BIGINT AUTO_INCREMENT` | — | — | mutable | N | `DECISION_DDL-ENUM-01` | R1 |
| 8 | `fal_motivo_archivo` | TO_CREATE | Catálogos de dominio | `FalMotivoArchivo` | `MotivoArchivoRepository` | `id BIGINT AUTO_INCREMENT` | — | `fal_acta` | catálogo | N | — | R1 |
| 9 | `fal_normativa_faltas` | TO_CREATE | Normativa | `FalNormativaFaltas` | `NormativaRepository` | `id BIGINT AUTO_INCREMENT` | — | `fal_articulo_normativa_faltas`, `fal_dependencia_normativa` | catálogo | N | — | R2+ |
| 10 | `fal_medida_preventiva` | TO_CREATE | Normativa | `FalMedidaPreventiva` | — | `id BIGINT AUTO_INCREMENT` | — | `fal_articulo_medida_preventiva`, `fal_acta_medida_preventiva` | catálogo | N | — | R2+ |
| 11 | `fal_lote_correo` | TO_CREATE | Notificaciones | `FalLoteCorreo` | `LoteCorreoRepository` | `id BIGINT AUTO_INCREMENT` | — | — | mutable | N | — | R2+ |
| 12 | `fal_documento_plantilla` | TO_CREATE | Documentos | `FalDocumentoPlantilla` | — | `id BIGINT AUTO_INCREMENT` | — | `fal_documento_plantilla_firma_req`, `fal_documento_plantilla_contenido`, `fal_documento_plantilla_default`, `fal_documento` | catálogo | N | — | R2+ |
| 13 | `fal_rubro_version` | PREEXISTING_CANONICAL_ADOPTED | Catálogos de dominio | `FalRubroVersion` | `RubroVersionRepository` | (legacy, ver spec) | (legacy) | `fal_acta_contravencion`, `fal_acta_sustancias_alimenticias` | catálogo | N | `DECISION_DDL-BASELINE-01` | — |

## Grupo G2 — Versiones y catálogos secundarios (FK a G1)

| # | Nombre físico | Estado | Familia | Java | Puerto | PK | FK salientes | FK entrantes (conocidas) | Tipo | OCC | DECISION_DDL | Slice |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 14 | `fal_dependencia_version` | TO_CREATE | Catálogos internos | `FalDependenciaVersion` | `DependenciaRepository` | `id_dependencia + version_dep` (compuesta) | `id_dependencia → fal_dependencia` | `fal_firmante_version`, `fal_acta`, `fal_documento_plantilla_default` | mutable | N | — | R2+ |
| 15 | `fal_inspector_version` | TO_CREATE | Catálogos internos | `FalInspectorVersion` | `InspectorRepository` | `id_inspector + version_insp` (compuesta) | `id_inspector → fal_inspector` | `fal_acta`, `num_talonario_inspector` | mutable | N | — | R2+ |
| 16 | `fal_firmante_version` | TO_CREATE | Identidad/firmantes | `FalFirmanteVersion` | `FirmanteRepository` | `id_firmante + ver_firmante` (compuesta) | `id_firmante → fal_firmante`, `id_dep → fal_dependencia` (nullable) | `fal_firmante_version_habilitacion`, `fal_documento_firma` | mutable | N | — | R2+ |
| 17 | `fal_vehiculo_modelo` | TO_CREATE | Catálogos de dominio | `FalVehiculoModelo` | `VehiculoModeloRepository` | `id BIGINT AUTO_INCREMENT` | `marca_vehiculo_id → fal_vehiculo_marca` | `fal_acta_vehiculo` | catálogo | N | — | R1 |
| 18 | `num_talonario` | TO_CREATE | Talonarios/numeración | `NumTalonario` | `NumeracionRepository` | `id BIGINT AUTO_INCREMENT` | `politica_id → num_politica` | `num_talonario_ambito`, `num_talonario_inspector`, `num_talonario_movimiento`, `fal_acta` | mutable | Y | — | R2+ |
| 19 | `fal_dependencia_normativa` | TO_CREATE | Normativa | `FalDependenciaNormativa` | — | `id BIGINT AUTO_INCREMENT` | `id_dependencia → fal_dependencia`, `id_normativa → fal_normativa_faltas` | — | mutable | N | — | R2+ |
| 20 | `fal_articulo_normativa_faltas` | TO_CREATE | Normativa | `FalArticuloNormativaFaltas` | — | `id BIGINT AUTO_INCREMENT` | `id_normativa → fal_normativa_faltas` | `fal_tarifario_unidad_faltas`, `fal_articulo_medida_preventiva`, `fal_acta_articulo_infringido` | catálogo | N | — | R2+ |
| 21 | `fal_documento_plantilla_firma_req` | TO_CREATE | Documentos | `FalDocumentoPlantillaFirmaReq` | — | `id_plantilla + seq_firma_req` (compuesta) | `id_plantilla → fal_documento_plantilla` | — | catálogo | N | — | R2+ |
| 22 | `fal_documento_plantilla_contenido` | TO_CREATE | Documentos | `FalDocumentoPlantillaContenido` | — | `id BIGINT AUTO_INCREMENT` | `id_plantilla → fal_documento_plantilla` | `fal_documento_redaccion` | catálogo | N | — | R2+ |
| 23 | `fal_documento_plantilla_default` | TO_CREATE | Documentos | `FalDocumentoPlantillaDefault` | — | `id BIGINT AUTO_INCREMENT` | `id_plantilla → fal_documento_plantilla`, `id_dependencia → fal_dependencia` (nullable), `id_dep → fal_dependencia_version` (conceptual) | — | catálogo | N | — | R2+ |

## Grupo G3 — Habilitaciones y catálogos terciarios (FK a G1+G2)

| # | Nombre físico | Estado | Familia | Java | Puerto | PK | FK salientes | FK entrantes (conocidas) | Tipo | OCC | DECISION_DDL | Slice |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 24 | `fal_firmante_version_habilitacion` | TO_CREATE | Identidad/firmantes | `FalFirmanteVersionHabilitacion` | `FirmanteRepository` | `id_firmante + ver_firmante + tipo_docu + rol_firma_req` (compuesta) | `(id_firmante, ver_firmante) → fal_firmante_version` | — | catálogo | N | `DECISION_DDL-ENUM-01` (corrección PK compuesta) | R2+ |
| 25 | `fal_tarifario_unidad_faltas` | TO_CREATE | Normativa | `FalTarifarioUnidadFaltas` | `TarifarioUnidadFaltasRepository` | `id BIGINT AUTO_INCREMENT` | `id_articulo → fal_articulo_normativa_faltas` | — | catálogo | N | — | R2+ |
| 26 | `fal_articulo_medida_preventiva` | TO_CREATE | Normativa | `FalArticuloMedidaPreventiva` | — | PK compuesta vía `ArticuloMedidaPreventivaId` | `id_medida → fal_medida_preventiva`, `id_articulo → fal_articulo_normativa_faltas` | — | catálogo | N | — | R2+ |
| 27 | `num_talonario_ambito` | TO_CREATE | Talonarios/numeración | `NumTalonarioAmbito` | `NumeracionRepository` | `id BIGINT AUTO_INCREMENT` | `talonario_id → num_talonario`, `id_dep → fal_dependencia` (nullable) | — | mutable | N | — | R2+ |
| 28 | `num_talonario_inspector` | TO_CREATE | Talonarios/numeración | `NumTalonarioInspector` | `NumeracionRepository` | `id BIGINT AUTO_INCREMENT` | `id_talonario → num_talonario` | — | mutable | N | — | R2+ |

## Grupo G4 — Núcleo del acta (depende de G1+G2+G3)

| # | Nombre físico | Estado | Familia | Java | Puerto | PK | FK salientes | FK entrantes (conocidas) | Tipo | OCC | DECISION_DDL | Slice |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 29 | `fal_persona_domicilio` | TO_CREATE | Personas/domicilios | `FalPersonaDomicilio` | `PersonaDomicilioRepository` | `id BIGINT AUTO_INCREMENT` | `persona_id → fal_persona`, `acta_origen_id → fal_acta` (nullable; FK agregada vía ALTER post G4), `localidad_malvinas_version_id → geo_malv_localidad_version` (baseline), `calle_malvinas_version_id → geo_malv_calle_version` (baseline) | `fal_acta` (id_domicilio_infractor_act, id_domicilio_notif_act) | mutable | N | — | R2+ |
| 30 | `fal_acta` | TO_CREATE | Núcleo acta | `FalActa` | `ActaRepository` | `id BIGINT AUTO_INCREMENT` | `id_insp + ver_insp → fal_inspector_version` (conceptual), `id_dep + ver_dep → fal_dependencia_version` (conceptual), `id_persona_infractor → fal_persona`, `id_domicilio_infractor_act → fal_persona_domicilio` (nullable), `id_domicilio_notif_act → fal_persona_domicilio` (nullable), `id_motivo_archivo_actual → fal_motivo_archivo` (nullable), `localidad_infr_malvinas_version_id → geo_malv_localidad_version` (baseline), `calle_infr_malvinas_version_id → geo_malv_calle_version` (baseline), `id_talonario → num_talonario` (nullable) | prácticamente todas las tablas del dominio | mutable | Y | — | R2+ |

> **Nota ciclo G4:** `fal_acta` ↔ `fal_persona_domicilio`. Resolución: crear `fal_persona_domicilio` sin FK a `fal_acta`, crear `fal_acta` con FKs a `fal_persona_domicilio`, luego `ALTER TABLE fal_persona_domicilio ADD CONSTRAINT fk_pers_dom_acta_origen FOREIGN KEY (acta_origen_id) REFERENCES fal_acta(id)`.

## Grupos G5–G12 — Satélites, documentos, notificaciones, economía

| # | Nombre físico | Estado | Familia | Java | Puerto | PK | FK salientes (simplificado) | Tipo | OCC | DECISION_DDL | Slice |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 31 | `fal_acta_evidencia` | TO_CREATE | Núcleo acta | `FalActaEvidencia` | `ActaEvidenciaRepository` | `id BIGINT AUTO_INCREMENT` | `id_acta → fal_acta` | append-only | N | `DECISION_DDL-EVID-01` | R2+ |
| 32 | `fal_observacion` | TO_CREATE | Núcleo acta | `FalObservacion` | `ObservacionRepository` | `id BIGINT AUTO_INCREMENT` | `id_acta → fal_acta` | mutable | N | — | R2+ |
| 33 | `fal_acta_transito` | TO_CREATE | Satélites de acta | `FalActaTransito` | — | `acta_id BIGINT` (PK+FK 1:1) | `acta_id → fal_acta` | mutable | N | — | R2+ |
| 34 | `fal_acta_transito_alcoholemia` | TO_CREATE | Satélites de acta | `FalActaTransitoAlcoholemia` | — | `id BIGINT AUTO_INCREMENT` | `acta_id → fal_acta` | mutable | N | — | R2+ |
| 35 | `fal_acta_vehiculo` | TO_CREATE | Satélites de acta | `FalActaVehiculo` | — | `acta_id BIGINT` (PK+FK 1:1) | `acta_id → fal_acta`, `marca_vehiculo_id → fal_vehiculo_marca`, `modelo_vehiculo_id → fal_vehiculo_modelo` | mutable | N | — | R2+ |
| 36 | `fal_acta_contravencion` | TO_CREATE | Satélites de acta | `FalActaContravencion` | — | `acta_id BIGINT` (PK+FK 1:1) | `acta_id → fal_acta`, `rubro_id → fal_rubro_version` | mutable | N | — | R2+ |
| 37 | `fal_acta_sustancias_alimenticias` | TO_CREATE | Satélites de acta | `FalActaSustanciasAlimenticias` | — | `acta_id BIGINT` (PK+FK 1:1) | `acta_id → fal_acta`, `rubro_id → fal_rubro_version` | mutable | N | — | R2+ |
| 38 | `fal_acta_paralizacion` | TO_CREATE | Paralización | `FalActaParalizacion` | `ActaParalizacionRepository` | `id BIGINT AUTO_INCREMENT` | `id_acta → fal_acta` | mutable | N | — | R2+ |
| 39 | `fal_acta_archivo` | TO_CREATE | Archivo | `FalActaArchivo` | `ActaArchivoRepository` | `id BIGINT AUTO_INCREMENT` | `id_acta → fal_acta` | mutable | N | — | R2+ |
| 40 | `fal_acta_articulo_infringido` | TO_CREATE | Normativa/artículos | `FalActaArticuloInfringido` | — | `id BIGINT AUTO_INCREMENT` | `id_acta → fal_acta`, `id_articulo → fal_articulo_normativa_faltas` | mutable | N | — | R2+ |
| 41 | `fal_acta_qr_acceso` | TO_CREATE | QR/portal | `FalActaQrAcceso` | `QrAccesoRepository` | `id BIGINT AUTO_INCREMENT` | `acta_id → fal_acta` | append-only | N | — | R2+ |
| 42 | `fal_acta_gestion_externa` | TO_CREATE | Gestión externa | `FalGestionExterna` | `GestionExternaRepository` | `id BIGINT AUTO_INCREMENT` | `id_acta → fal_acta` | mutable | Y | `DECISION_DDL-GEXT-01` | R2+ |
| 43 | `fal_acta_valorizacion` | TO_CREATE | Normativa/valorización | `FalActaValorizacion` | — | `id BIGINT AUTO_INCREMENT` | `id_acta → fal_acta` | mutable | Y | — | R2+ |
| 44 | `fal_acta_medida_preventiva` | TO_CREATE | Satélites de acta | `FalActaMedidaPreventiva` | — | `id BIGINT AUTO_INCREMENT` | `acta_id → fal_acta`, `acta_articulo_id → fal_acta_articulo_infringido`, `medida_preventiva_id → fal_medida_preventiva` | mutable | N | — | R2+ |
| 45 | `fal_acta_bloqueante_cierre_material` | TO_CREATE | Núcleo acta | `FalBloqueanteMaterial` | `BloqueanteMaterialRepository` | `id BIGINT AUTO_INCREMENT` | `id_acta → fal_acta` | mutable | Y | `DECISION_DDL-BLOQ-01` | R2+ |
| 46 | `fal_documento` | TO_CREATE | Documentos | `FalDocumento` | `DocumentoRepository` | `id BIGINT AUTO_INCREMENT` | `id_acta → fal_acta`, `id_plantilla → fal_documento_plantilla` | mutable | Y | `DECISION_DDL-DOC-01` | R2+ |
| 47 | `fal_acta_valorizacion_item` | TO_CREATE | Normativa/valorización | `FalActaValorizacionItem` | — | `id BIGINT AUTO_INCREMENT` | `id_valorizacion → fal_acta_valorizacion` | append-only | N | — | R2+ |
| 48 | `fal_acta_documento` | TO_CREATE | Documentos | `FalActaDocumento` | `ActaDocumentoRepository` | `id_acta + id_documento` (compuesta) | `id_acta → fal_acta`, `id_documento → fal_documento` | mutable | N | — | R2+ |
| 49 | `fal_documento_firma_req` | TO_CREATE | Documentos/firma | `FalDocumentoFirmaReq` | `DocumentoRepository` | `id BIGINT AUTO_INCREMENT` | `id_documento → fal_documento` | mutable | N | — | R2+ |
| 50 | `fal_documento_redaccion` | TO_CREATE | Documentos | `FalDocumentoRedaccion` | `DocumentoRedaccionRepository` | `id BIGINT AUTO_INCREMENT` | `id_documento → fal_documento`, `id_plantilla_contenido → fal_documento_plantilla_contenido`, `redaccion_origen_id → fal_documento_redaccion` (self) | mutable | N | — | R2+ |
| 51 | `fal_notificacion` | TO_CREATE | Notificaciones | `FalNotificacion` | `NotificacionRepository` | `id BIGINT AUTO_INCREMENT` | `id_acta → fal_acta`, `id_documento → fal_documento` | mutable | Y | `DECISION_DDL-NOTI-01` | R2+ |
| 52 | `num_talonario_movimiento` | TO_CREATE | Talonarios/numeración | `NumTalonarioMovimiento` | `NumeracionRepository` | `id BIGINT AUTO_INCREMENT` | `id_talonario → num_talonario`, `acta_id → fal_acta` (nullable), `documento_id → fal_documento` (nullable) | append-only | N | — | R2+ |
| 53 | `fal_acta_fallo` | TO_CREATE | Fallo/firmeza | `FalActaFallo` | `FalloActaRepository` | `id BIGINT AUTO_INCREMENT` | `id_acta → fal_acta`, `valorizacion_id → fal_acta_valorizacion` (nullable), `fallo_reemplazado_id → fal_acta_fallo` (self), `documento_id → fal_documento` (nullable) | mutable | Y | `DECISION_DDL-ENUM-01`, `DECISION_DDL-RF-005` | R2+ |
| 54 | `fal_documento_firma` | TO_CREATE | Documentos/firma | `FalDocumentoFirma` | `DocumentoRepository` | `id BIGINT AUTO_INCREMENT` | `id_documento → fal_documento`, `(id_firmante, ver_firmante) → fal_firmante_version` | mutable | N | — | R2+ |
| 55 | `fal_notificacion_intento` | TO_CREATE | Notificaciones | `FalNotificacionIntento` | — | `id BIGINT AUTO_INCREMENT` | `id_notificacion → fal_notificacion` | mutable | N | — | R2+ |
| 56 | `fal_notificacion_acuse` | TO_CREATE | Notificaciones | `FalNotificacionAcuse` | — | `id BIGINT AUTO_INCREMENT` | `id_notificacion → fal_notificacion` | mutable | N | — | R2+ |
| 57 | `fal_acta_evento` | TO_CREATE | Núcleo acta | `FalActaEvento` | `ActaEventoRepository` | `id BIGINT AUTO_INCREMENT` | `acta_id → fal_acta`, `id_docu_rel → fal_documento` (nullable), `id_notif_rel → fal_notificacion` (nullable) | append-only | N | — | R2+ |
| 58 | `fal_acta_apelacion` | TO_CREATE | Apelación | `FalActaApelacion` | `ApelacionActaRepository` | `id BIGINT AUTO_INCREMENT` | `id_acta → fal_acta`, `fallo_id → fal_acta_fallo`, `documento_resolucion_id → fal_documento` (nullable), `self` | mutable | Y | `DECISION_DDL-ENUM-01` | R2+ |
| 59 | `fal_acta_obligacion_pago` | TO_CREATE | Economía/pagos | `FalActaObligacionPago` | `ObligacionPagoRepository` | `id BIGINT AUTO_INCREMENT` | `acta_id → fal_acta`, `persona_id → fal_persona`, `valorizacion_id → fal_acta_valorizacion` (nullable), `fallo_id → fal_acta_fallo` (nullable), `forma_pago_vigente_id → fal_acta_forma_pago` (nullable; FK agregada vía ALTER post G9), `obligacion_reemplazada_id → fal_acta_obligacion_pago` (self) | mutable | Y | `DECISION_DDL-PAGO-03`, `DECISION_DDL-SNAP-02` | R2+ |
| 60 | `fal_acta_snapshot` | TO_CREATE | Proyecciones/snapshot | `FalActaSnapshot` | `ActaSnapshotRepository` | `id_acta BIGINT` (PK+FK 1:1) | `id_acta → fal_acta`, `id_docu_ult → fal_documento` (nullable), `valorizacion_operativa_id → fal_acta_valorizacion` (nullable) | proyección | Y | `DECISION_DDL-SNAP-01`, `DECISION_DDL-SNAP-02` | R2+ |
| 61 | `fal_acta_apelacion_documento` | TO_CREATE | Apelación | `FalActaApelacionDocumento` | — | `id BIGINT AUTO_INCREMENT` | `id_apelacion → fal_acta_apelacion`, `id_documento → fal_documento` | mutable | N | — | R2+ |
| 62 | `fal_acta_forma_pago` | TO_CREATE | Economía/pagos | `FalActaFormaPago` | — | `id BIGINT AUTO_INCREMENT` | `obligacion_pago_id → fal_acta_obligacion_pago`, `forma_reemplazada_id → fal_acta_forma_pago` (self) | mutable | Y | `DECISION_DDL-FORMA-01`, `DECISION_DDL-PAGO-03` | R2+ |
| 63 | `fal_acta_plan_pago_ref` | TO_CREATE | Economía/pagos | `FalActaPlanPagoRef` | — | `id BIGINT AUTO_INCREMENT` | `forma_pago_id → fal_acta_forma_pago`, `obligacion_pago_id → fal_acta_obligacion_pago`, `plan_refinanciado_id → fal_acta_plan_pago_ref` (self) | mutable | Y | `DECISION_DDL-PLAN-01` | R2+ |
| 64 | `fal_acta_pago_movimiento` | TO_CREATE | Economía/pagos | `FalActaPagoMovimiento` | `PagoMovimientoRepository` | `id BIGINT AUTO_INCREMENT` | `obligacion_pago_id → fal_acta_obligacion_pago`, `forma_pago_id → fal_acta_forma_pago` (nullable), `plan_pago_ref_id → fal_acta_plan_pago_ref` (nullable), `movimiento_origen_id → fal_acta_pago_movimiento` (self) | append-only | N | `DECISION_DDL-MOV-01..04`, `DECISION_DDL-PAGO-MOV-01` | R2+ |
| 65 | `fal_acta_economia_proyeccion` | TO_CREATE | Economía/pagos | `FalActaEconomiaProyeccion` | `EconomiaProyeccionRepository` | `acta_id BIGINT` (PK+FK 1:1) | `acta_id → fal_acta`, `obligacion_vigente_id → fal_acta_obligacion_pago` (nullable), `forma_pago_vigente_id → fal_acta_forma_pago` (nullable), `plan_pago_vigente_id → fal_acta_plan_pago_ref` (nullable), `ultimo_movimiento_id_proyectado → fal_acta_pago_movimiento` (nullable) | proyección | Y | `DECISION_DDL-ECPR-01` | R2+ |

---

## Verificación del conteo

| Categoría | Cantidad |
|---|---|
| PREEXISTING_CANONICAL_ADOPTED | 1 (`fal_rubro_version`) |
| TO_CREATE | 64 |
| **Total canónico** | **65** |
| Excluidas (`fal_informix_sync_*`) | 2 (no canónicas) |
| Excluidas (`geo_*`) | múltiples (externas) |

## Tablas excluidas por no existir como tabla propia

- **`FalPagoVoluntario`** y **`FalPagoCondena`**: vistas de aplicación sobre `fal_acta_obligacion_pago`. No tienen tabla propia (`DECISION_DDL-PAGO-01` CERRADA).
- **`fal_acta_pago_resolucion`**: tabla eliminada del diseño (`DECISION_DDL-RF-005`, confirmado contra código Java).
- **`FalActaFirmezaCondena`**: eliminada del código Java; firmeza inline en `fal_acta_fallo` (`DECISION_DDL-RF-005`, P2 CERRADA).
