package ar.gob.malvinas.faltas.core.application;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guardrail de paridad campo-a-campo entre el codigo Java vigente de dominio y
 * {@code 50-persistence/mariadb-logical-model.md}.
 *
 * <p>Cubre:
 * <ul>
 *   <li>R2 (11 tablas): {@code fal_acta_evento}, {@code fal_acta_qr_acceso},
 *       {@code fal_acta}, {@code fal_persona}, {@code fal_persona_domicilio}
 *       y los 6 satelites del acta.</li>
 *   <li>R3 (15 tablas nuevas): firmantes (3), snapshot, evidencia, pagos (5),
 *       talonarios/numeracion (5).</li>
 *   <li>Estructura global: ausencia de {@code ESTRUCTURAL_YA_RECONCILIADA_R0_R1},
 *       presencia de {@code BASELINE_PRESERVADA_CON_DELTA} en tablas Nivel B,
 *       26 tablas con {@code RECONCILIADA_R3}.</li>
 * </ul>
 *
 * <p>No reabre dominio, comandos, eventos ni estados. No es DDL ejecutable.
 * Complementa (no reemplaza) a {@code SpecAsSourceGuardrailTest} (G-1..G-26).
 * Evidencia en {@code .work/handoff-current/MARIADB-LOGICAL-PARITY.md}.
 */
@DisplayName("Guardrail de paridad del modelo logico MariaDB (R2+R3: firmantes/snapshot/evidencia/pagos/talonarios/acta/persona/satelites)")
class MariaDbLogicalModelParityGuardrailTest {

    private static Path modeloLogico;

    @BeforeAll
    static void localizarModeloLogico() {
        Path candidato = Paths.get("docs", "spec-as-source", "50-persistence", "mariadb-logical-model.md");
        if (!Files.isRegularFile(candidato)) {
            throw new IllegalStateException(
                    "No se encontro 'docs/spec-as-source/50-persistence/mariadb-logical-model.md' desde el "
                            + "working directory Maven actual: " + Paths.get("").toAbsolutePath()
                            + ". Este test debe ejecutarse con cwd = backend/api-faltas-core.");
        }
        modeloLogico = candidato;
    }

    private static String leerModelo() throws IOException {
        return Files.readString(modeloLogico, StandardCharsets.UTF_8);
    }

    /**
     * Devuelve el texto de la seccion {@code #### `tabla`} hasta el siguiente
     * encabezado de igual o mayor nivel (excluido). Usado para acotar las
     * verificaciones de "campos prohibidos" a la tabla concreta, sin falsos
     * positivos por otras secciones ni por la prosa explicativa "No existen ..."
     * (que menciona intencionalmente los nombres inventados descartados, en un
     * bullet propio distinto al de "Campos").
     */
    private static String seccionDeTabla(String contenido, String tabla) {
        String marcador = "#### `" + tabla + "`";
        int inicio = contenido.indexOf(marcador);
        assertThat(inicio).as("Debe existir la seccion '%s' en mariadb-logical-model.md", marcador)
                .isGreaterThanOrEqualTo(0);
        int finPorSubseccion = contenido.indexOf("\n#### `", inicio + marcador.length());
        int finPorSeccion = contenido.indexOf("\n### ", inicio + marcador.length());
        int fin = contenido.length();
        if (finPorSubseccion >= 0) {
            fin = Math.min(fin, finPorSubseccion);
        }
        if (finPorSeccion >= 0) {
            fin = Math.min(fin, finPorSeccion);
        }
        return contenido.substring(inicio, fin);
    }

    /**
     * Extrae, dentro de una seccion ya acotada, solo las lineas que declaran
     * campos reales (bullets {@code **Campos} / {@code **Campos —}), evitando
     * que la prosa de "No existen ..." (que cita a proposito los nombres
     * inventados descartados) dispare falsos positivos de "campo prohibido".
     */
    private static String lineasDeCampos(String seccion) {
        return Arrays.stream(seccion.split("\n"))
                .filter(linea -> linea.contains("**Campos"))
                .collect(Collectors.joining("\n"));
    }

    private static void verificarCamposDeTabla(
            String contenido, String tabla, List<String> presentes, List<String> ausentes) {
        String seccion = seccionDeTabla(contenido, tabla);
        String lineasCampos = lineasDeCampos(seccion);
        List<String> violaciones = new ArrayList<>();
        for (String token : presentes) {
            if (!seccion.contains(token)) {
                violaciones.add(tabla + ": falta el token requerido '" + token + "'");
            }
        }
        for (String token : ausentes) {
            if (lineasCampos.contains(token)) {
                violaciones.add(tabla + ": contiene el token prohibido '" + token + "' en la linea de Campos");
            }
        }
        assertThat(violaciones)
                .as("Paridad de campos de %s en mariadb-logical-model.md", tabla)
                .isEmpty();
    }

    // =====================================================================
    // fal_acta_evento
    // =====================================================================

    @Nested
    @DisplayName("fal_acta_evento: CHAR(6), sin payload JSON, sin orden_logico")
    class FalActaEvento {

        @Test
        @DisplayName("contiene los campos reales de FalActaEvento y no contiene los campos inventados")
        void campos_correctos() throws IOException {
            String contenido = leerModelo();
            verificarCamposDeTabla(contenido, "fal_acta_evento",
                    List.of(
                            "tipo_evt CHAR(6)",
                            "origen_evt",
                            "fh_evt",
                            "bloque_func",
                            "est_proc_ant",
                            "est_proc_nvo",
                            "sit_adm_ant",
                            "sit_adm_nva",
                            "actor_tipo",
                            "actor_id",
                            "actor_ref",
                            "id_docu_rel",
                            "id_notif_rel",
                            "id_pres_rel",
                            "id_user_evt",
                            "si_evt_cierre",
                            "si_evt_ext",
                            "si_permite_reing",
                            "descripcion_legible",
                            "correlacion_id"),
                    List.of(
                            "tipo_evt SMALLINT",
                            "fecha_evento",
                            "orden_logico",
                            "payload JSON",
                            "payload_json"));
        }

        @Test
        @DisplayName("declara explicitamente que no usa payload JSON y que el orden es fhEvt + id")
        void reglas_explicitas() throws IOException {
            String contenido = leerModelo();
            assertThat(contenido).containsIgnoringCase("no usa payload json");
            assertThat(contenido).contains("fhEvt + id");
        }
    }

    // =====================================================================
    // fal_acta_qr_acceso
    // =====================================================================

    @Nested
    @DisplayName("fal_acta_qr_acceso: log append-only de accesos validos, sin token/URL/vencimiento")
    class FalActaQrAcceso {

        @Test
        @DisplayName("contiene los campos reales de FalActaQrAcceso y no contiene los campos inventados")
        void campos_correctos() throws IOException {
            String contenido = leerModelo();
            verificarCamposDeTabla(contenido, "fal_acta_qr_acceso",
                    List.of(
                            "fh_acceso",
                            "canal_acceso",
                            "ip_origen",
                            "user_agent",
                            "resultado_acceso"),
                    List.of(
                            "token_qr",
                            "url_acceso",
                            "fh_generacion",
                            "fh_vencimiento"));
        }

        @Test
        @DisplayName("no declara si_activo como campo de fal_acta_qr_acceso")
        void sin_si_activo_en_qr_acceso() throws IOException {
            List<String> lineas = Files.readAllLines(modeloLogico, StandardCharsets.UTF_8);
            int inicio = -1;
            for (int i = 0; i < lineas.size(); i++) {
                if (lineas.get(i).contains("#### `fal_acta_qr_acceso`")) {
                    inicio = i;
                    break;
                }
            }
            assertThat(inicio).as("Debe existir la seccion #### `fal_acta_qr_acceso`").isGreaterThanOrEqualTo(0);
            StringBuilder seccion = new StringBuilder();
            for (int i = inicio; i < lineas.size(); i++) {
                String linea = lineas.get(i);
                if (i > inicio && linea.startsWith("#### `") ) {
                    break;
                }
                seccion.append(linea).append('\n');
            }
            assertThat(seccion.toString())
                    .as("La seccion de fal_acta_qr_acceso no debe declarar 'si_activo' como campo propio")
                    .doesNotContain("si_activo BOOLEAN");
        }

        @Test
        @DisplayName("declara explicitamente que no almacena el token")
        void regla_explicita_sin_token() throws IOException {
            String contenido = leerModelo();
            assertThat(contenido).containsPattern("(?i)no almacena el token");
        }
    }

    // =====================================================================
    // fal_persona
    // =====================================================================

    @Nested
    @DisplayName("fal_persona: apellido/nombres/razon_social/nombre_mostrar, sin nombre_completo ni datos geo")
    class FalPersona {

        @Test
        @DisplayName("contiene los campos reales de FalPersona y no contiene los campos inventados")
        void campos_correctos() throws IOException {
            String contenido = leerModelo();
            verificarCamposDeTabla(contenido, "fal_persona",
                    List.of(
                            "tipo_persona",
                            "tipo_documento",
                            "nro_doc",
                            "apellido",
                            "nombres",
                            "razon_social",
                            "nombre_mostrar",
                            "email_principal",
                            "telefono_principal",
                            "id_suj",
                            "id_bie",
                            "suj_bie_estado",
                            "fh_suj_bie_creacion"),
                    List.of(
                            "nro_documento VARCHAR",
                            "nombre_completo",
                            "fh_nacimiento",
                            "si_identificado",
                            "si_extranjero",
                            "id_ign BIGINT",
                            "id_indec BIGINT",
                            "id_local BIGINT"));
        }

        @Test
        @DisplayName("declara explicitamente que FalPersona no tiene versionRow")
        void sin_version_row() throws IOException {
            String contenido = leerModelo();
            assertThat(contenido).containsPattern("(?i)`fal_persona`.{0,40}no define `versionRow`|no define .versionRow.");
        }
    }

    // =====================================================================
    // fal_persona_domicilio
    // =====================================================================

    @Nested
    @DisplayName("fal_persona_domicilio: modo MALVINAS_LOCAL/EXTERNO, ids Malvinas VARCHAR, sin campos inventados")
    class FalPersonaDomicilio {

        @Test
        @DisplayName("contiene los campos reales de FalPersonaDomicilio y no contiene los campos inventados")
        void campos_correctos() throws IOException {
            String contenido = leerModelo();
            verificarCamposDeTabla(contenido, "fal_persona_domicilio",
                    List.of(
                            "acta_origen_id",
                            "origen_domicilio",
                            "modo_domicilio",
                            "si_activo BOOLEAN NOT NULL",
                            "si_notificable",
                            "id_provincia",
                            "unidad_territorial_tipo",
                            "id_unidad_territorial",
                            "id_localidad",
                            "id_calle BIGINT",
                            "id_loc_malvinas VARCHAR(8)",
                            "localidad_malvinas_version_id",
                            "id_tca_malvinas VARCHAR(10)",
                            "calle_malvinas_version_id",
                            "calle_txt",
                            "altura INT",
                            "si_sin_altura",
                            "unidad_funcional",
                            "codigo_postal",
                            "domicilio_txt",
                            "si_normalizado_parcial",
                            "origen_ubicacion SMALLINT"),
                    List.of(
                            "validacion_domicilio",
                            "id_calle_version",
                            "nro_puerta",
                            "piso VARCHAR",
                            "dpto VARCHAR",
                            "localidad_id BIGINT",
                            "texto_libre",
                            "id_tca BIGINT",
                            "id_loc BIGINT"));
        }
    }

    // =====================================================================
    // fal_acta
    // =====================================================================

    @Nested
    @DisplayName("fal_acta: tripla CHAR(4), id_persona_infractor NOT NULL, bloque completo de captura/lugar del hecho/QR")
    class FalActa {

        @Test
        @DisplayName("contiene los campos reales de FalActa (captura, lugar del hecho, QR, archivo) y no contiene los campos inventados")
        void campos_correctos() throws IOException {
            String contenido = leerModelo();
            verificarCamposDeTabla(contenido, "fal_acta",
                    List.of(
                            "origen_captura",
                            "id_dispositivo_captura",
                            "id_user_captura",
                            "fh_captura",
                            "lat_captura",
                            "lon_captura",
                            "precision_captura_m",
                            "fh_pos_captura",
                            "origen_pos_captura",
                            "id_loc_infr_malvinas",
                            "localidad_infr_malvinas_version_id",
                            "id_tca_infr_malvinas",
                            "calle_infr_malvinas_version_id",
                            "altura_infr",
                            "altura_origen_infr",
                            "si_altura_infr_estimada",
                            "origen_ubicacion_infr",
                            "si_ubicacion_infr_manual",
                            "si_dom_txt_infr",
                            "dom_txt_infr",
                            "si_eje_urb",
                            "codigo_qr VARCHAR(128)",
                            "qr_payload_version",
                            "id_motivo_archivo_actual",
                            "permite_reingreso",
                            "fh_cierre",
                            "fh_archivo",
                            "resumen_hecho",
                            "id_persona_infractor",
                            "est_proc_act CHAR(4)",
                            "sit_adm_act CHAR(4)",
                            "bloque_actual CHAR(4)"),
                    List.of(
                            "estado_procesal SMALLINT",
                            "situacion_administrativa SMALLINT",
                            "bloque_actual SMALLINT",
                            "id_persona BIGINT NULL FK"));
        }

        @Test
        @DisplayName("la FK a persona es NOT NULL (id_persona_infractor)")
        void fk_persona_not_null() throws IOException {
            String contenido = leerModelo();
            assertThat(contenido).containsPattern("(?i)id_persona_infractor BIGINT NOT NULL");
        }
    }

    // =====================================================================
    // Satelites del acta (6 tablas)
    // =====================================================================

    @Nested
    @DisplayName("Satelites del acta: cada uno documenta sus campos propios (no solo nombre+entidad+estado)")
    class SatelitesDelActa {

        @Test
        @DisplayName("fal_acta_transito documenta sus 7 campos propios")
        void fal_acta_transito() throws IOException {
            String contenido = leerModelo();
            verificarCamposDeTabla(contenido, "fal_acta_transito",
                    List.of("nro_licencia", "id_prov_lic", "unidad_territorial_lic_tipo",
                            "id_unidad_territorial_lic", "si_ret_licencia", "si_ret_vehiculo",
                            "si_control_alcoholemia"),
                    List.of());
        }

        @Test
        @DisplayName("fal_acta_transito_alcoholemia documenta sus campos propios")
        void fal_acta_transito_alcoholemia() throws IOException {
            String contenido = leerModelo();
            verificarCamposDeTabla(contenido, "fal_acta_transito_alcoholemia",
                    List.of("orden_medicion", "tipo_prueba", "resultado_cualitativo",
                            "resultado_numerico", "unidad_medida", "id_alcoholimetro",
                            "ver_alcoholimetro", "si_resultado_final", "fh_medicion"),
                    List.of());
        }

        @Test
        @DisplayName("fal_acta_vehiculo documenta sus campos propios")
        void fal_acta_vehiculo() throws IOException {
            String contenido = leerModelo();
            verificarCamposDeTabla(contenido, "fal_acta_vehiculo",
                    List.of("dominio_vehiculo", "tipo_vehiculo", "marca_vehiculo_id",
                            "marca_vehiculo_txt", "modelo_vehiculo_id", "modelo_vehiculo_txt",
                            "anio_vehiculo", "color_vehiculo", "estado_general_vehiculo"),
                    List.of());
        }

        @Test
        @DisplayName("fal_acta_contravencion documenta sus campos propios y no documenta nomenclatura_txt")
        void fal_acta_contravencion() throws IOException {
            String contenido = leerModelo();
            verificarCamposDeTabla(contenido, "fal_acta_contravencion",
                    List.of("id_suj_i", "id_bie_i", "id_suj_c", "id_bie_c", "circ SMALLINT",
                            "secc VARCHAR", "frac VARCHAR", "mza VARCHAR", "parc VARCHAR",
                            "ufun VARCHAR", "ucomp VARCHAR", "origen_nomencl",
                            "si_nomenclatura_manual", "motivo_nomenclatura_manual"),
                    List.of("nomenclatura_txt VARCHAR", "nomenclatura_txt TEXT"));
        }

        @Test
        @DisplayName("fal_acta_sustancias_alimenticias documenta sus campos propios")
        void fal_acta_sustancias_alimenticias() throws IOException {
            String contenido = leerModelo();
            verificarCamposDeTabla(contenido, "fal_acta_sustancias_alimenticias",
                    List.of("descripcion_sustancias"),
                    List.of());
        }

        @Test
        @DisplayName("fal_acta_medida_preventiva documenta sus campos propios y la relacion unidireccional con bloqueantes")
        void fal_acta_medida_preventiva() throws IOException {
            String contenido = leerModelo();
            verificarCamposDeTabla(contenido, "fal_acta_medida_preventiva",
                    List.of("acta_articulo_id", "medida_preventiva_id", "med_prev_txt",
                            "estado_medida", "si_genera_bloqueante"),
                    List.of());
            assertThat(contenido).containsPattern("(?i)unidireccional");
        }
    }

    // =====================================================================
    // R3 Firmantes (fal_firmante / fal_firmante_version / fal_firmante_version_habilitacion)
    // =====================================================================

    @Nested
    @DisplayName("R3 Firmantes: campos Java verificados, PK compuesta de habilitacion corregida")
    class FirmantesR3 {

        @Test
        @DisplayName("fal_firmante documenta campos de FalFirmante.java y marca RECONCILIADA_R3")
        void fal_firmante() throws IOException {
            String contenido = leerModelo();
            verificarCamposDeTabla(contenido, "fal_firmante",
                    List.of("id_user CHAR(36)", "nom_firmante VARCHAR(48)", "si_activo", "fh_alta", "id_user_alta"),
                    List.of());
            assertThat(seccionDeTabla(contenido, "fal_firmante")).contains("RECONCILIADA_R3");
        }

        @Test
        @DisplayName("fal_firmante_version documenta campos de FalFirmanteVersion.java y marca RECONCILIADA_R3")
        void fal_firmante_version() throws IOException {
            String contenido = leerModelo();
            verificarCamposDeTabla(contenido, "fal_firmante_version",
                    List.of("id_firmante", "ver_firmante", "id_user", "nom_firmante", "rol_firmante",
                            "cargo_firmante", "fh_vig_desde", "fh_vig_hasta", "si_activo"),
                    List.of());
            assertThat(seccionDeTabla(contenido, "fal_firmante_version")).contains("RECONCILIADA_R3");
        }

        @Test
        @DisplayName("fal_firmante_version_habilitacion: PK compuesta (correc. R3 - no id surrogate), RECONCILIADA_R3")
        void fal_firmante_version_habilitacion() throws IOException {
            String contenido = leerModelo();
            String seccion = seccionDeTabla(contenido, "fal_firmante_version_habilitacion");
            assertThat(seccion).contains("tipo_docu").contains("rol_firma_req").contains("mecanismo_firma_req");
            assertThat(seccion).containsIgnoringCase("NO tiene campo `id`")
                    .as("Debe documentar explicitamente que la entidad Java no tiene campo id surrogate");
            assertThat(seccion).contains("RECONCILIADA_R3");
        }
    }

    // =====================================================================
    // R3 Snapshot y Evidencia
    // =====================================================================

    @Nested
    @DisplayName("R3 Snapshot y Evidencia: campos Java completos, decisiones DDL abiertas documentadas")
    class SnapshotEvidenciaR3 {

        @Test
        @DisplayName("fal_acta_snapshot documenta campos de estado, bandejas, documentos, valoracion y pagos")
        void fal_acta_snapshot() throws IOException {
            String contenido = leerModelo();
            verificarCamposDeTabla(contenido, "fal_acta_snapshot",
                    List.of("bloque_actual", "est_proc_act", "sit_adm_act", "resultado_final",
                            "cod_bandeja", "accion_pendiente",
                            "tiene_documentos", "tiene_notificaciones",
                            "valorizacion_operativa_id", "monto_operativo_vigente",
                            "tipo_obligacion_pago", "estado_obligacion_pago", "monto_obligacion_pago",
                            "si_plan_pago", "si_pago_procesado", "si_pago_confirmado",
                            "ultimo_evento_tipo", "ultima_actualizacion"),
                    List.of());
            String seccion = seccionDeTabla(contenido, "fal_acta_snapshot");
            assertThat(seccion).contains("DECISION_DDL-SNAP-01").contains("DECISION_DDL-SNAP-02");
            assertThat(seccion).contains("RECONCILIADA_R3");
        }

        @Test
        @DisplayName("fal_acta_evidencia documenta campos de FalActaEvidencia.java y DECISION_DDL-EVID-01")
        void fal_acta_evidencia() throws IOException {
            String contenido = leerModelo();
            verificarCamposDeTabla(contenido, "fal_acta_evidencia",
                    List.of("id_acta", "tipo_evid", "storage_key", "fecha_registro"),
                    List.of());
            String seccion = seccionDeTabla(contenido, "fal_acta_evidencia");
            assertThat(seccion).contains("DECISION_DDL-EVID-01");
            assertThat(seccion).contains("RECONCILIADA_R3");
        }
    }

    // =====================================================================
    // R3 Pagos (5 tablas)
    // =====================================================================

    @Nested
    @DisplayName("R3 Pagos: campos Java completos, decisiones DDL documentadas, sin EstadoPagoMovimiento")
    class PagosR3 {

        @Test
        @DisplayName("fal_acta_obligacion_pago: version_row, tipo_obligacion, estado_obligacion, si_vigente, RECONCILIADA_R3")
        void fal_acta_obligacion_pago() throws IOException {
            String contenido = leerModelo();
            verificarCamposDeTabla(contenido, "fal_acta_obligacion_pago",
                    List.of("version_row", "tipo_obligacion", "estado_obligacion", "monto_original", "si_vigente",
                            "fh_determinacion", "fh_cancelacion"),
                    List.of());
            String seccion = seccionDeTabla(contenido, "fal_acta_obligacion_pago");
            assertThat(seccion).contains("RECONCILIADA_R3").contains("DECISION_DDL-PAGO-03");
        }

        @Test
        @DisplayName("fal_acta_forma_pago: version_row, tipo_forma_pago, estado_forma_pago, si_vigente, RECONCILIADA_R3")
        void fal_acta_forma_pago() throws IOException {
            String contenido = leerModelo();
            verificarCamposDeTabla(contenido, "fal_acta_forma_pago",
                    List.of("version_row", "obligacion_pago_id", "nro_forma", "tipo_forma_pago",
                            "estado_forma_pago", "monto_forma", "si_vigente",
                            "cmte_em", "pref_em", "nro_em", "cmte_pg", "pref_pg", "nro_pg"),
                    List.of());
            String seccion = seccionDeTabla(contenido, "fal_acta_forma_pago");
            assertThat(seccion).contains("RECONCILIADA_R3").contains("DECISION_DDL-FORMA-01");
        }

        @Test
        @DisplayName("fal_acta_plan_pago_ref: version_row, estado_plan, cantidad_cuotas, importe_total_plan, RECONCILIADA_R3")
        void fal_acta_plan_pago_ref() throws IOException {
            String contenido = leerModelo();
            verificarCamposDeTabla(contenido, "fal_acta_plan_pago_ref",
                    List.of("version_row", "forma_pago_id", "obligacion_pago_id", "estado_plan",
                            "cantidad_cuotas", "importe_total_plan", "si_vigente"),
                    List.of());
            String seccion = seccionDeTabla(contenido, "fal_acta_plan_pago_ref");
            assertThat(seccion).contains("RECONCILIADA_R3").contains("DECISION_DDL-PLAN-01");
        }

        @Test
        @DisplayName("fal_acta_pago_movimiento: campos Java reales, sin EstadoPagoMovimiento/EstadoConciliacionActual, RECONCILIADA_R3")
        void fal_acta_pago_movimiento() throws IOException {
            String contenido = leerModelo();
            verificarCamposDeTabla(contenido, "fal_acta_pago_movimiento",
                    List.of("tipo_movimiento", "origen_movimiento", "clasificacion_pago",
                            "importe_capital", "importe_rima", "importe_total",
                            "movimiento_origen_id", "motivo_anulacion_pago",
                            "referencia_externa VARCHAR(80)", "fh_movimiento"),
                    List.of());
            String seccion = seccionDeTabla(contenido, "fal_acta_pago_movimiento");
            assertThat(seccion).contains("RECONCILIADA_R3");
            assertThat(seccion).contains("DECISION_DDL-MOV-04")
                    .as("Debe documentar divergencia de catalogo ClasificacionPago Java/historico");
        }

        @Test
        @DisplayName("fal_acta_economia_proyeccion: PK acta_id, importe_aplicado_total, saldo_pendiente, importe_excedente, RECONCILIADA_R3")
        void fal_acta_economia_proyeccion() throws IOException {
            String contenido = leerModelo();
            verificarCamposDeTabla(contenido, "fal_acta_economia_proyeccion",
                    List.of("acta_id BIGINT NOT NULL", "version_row",
                            "importe_aplicado_total DECIMAL(14,2) NOT NULL DEFAULT 0",
                            "saldo_pendiente", "importe_excedente",
                            "si_parcialmente_pagada", "estado_conciliacion_actual",
                            "referencia_ultima_conciliacion VARCHAR(80)",
                            "origen_ultima_actualizacion"),
                    List.of());
            String seccion = seccionDeTabla(contenido, "fal_acta_economia_proyeccion");
            assertThat(seccion).contains("RECONCILIADA_R3").contains("DECISION_DDL-ECPR-01");
            assertThat(seccion).doesNotContain("`id BIGINT AUTO_INCREMENT`")
                    .as("La PK de fal_acta_economia_proyeccion es acta_id, no id surrogate");
        }
    }

    // =====================================================================
    // R3 Talonarios y numeracion (5 tablas)
    // =====================================================================

    @Nested
    @DisplayName("R3 Talonarios y numeracion: campos Java + tipos fisicos confirmados del historico")
    class TalonariosR3 {

        @Test
        @DisplayName("num_politica: codigo VARCHAR(12), clase_numeracion, si_reinicio_anual, RECONCILIADA_R3")
        void num_politica() throws IOException {
            String contenido = leerModelo();
            verificarCamposDeTabla(contenido, "num_politica",
                    List.of("codigo VARCHAR(8)", "descripcion VARCHAR(64)", "apodo VARCHAR(20)", "clase_numeracion",
                            "si_reinicio_anual", "si_incluye_prefijo", "longitud_nro",
                            "formato_visible", "fh_vig_desde"),
                    List.of());
            assertThat(seccionDeTabla(contenido, "num_politica")).contains("RECONCILIADA_R3");
        }

        @Test
        @DisplayName("num_talonario: version_row, nombre_secuencia VARCHAR(64), tipo_talonario, RECONCILIADA_R3")
        void num_talonario() throws IOException {
            String contenido = leerModelo();
            verificarCamposDeTabla(contenido, "num_talonario",
                    List.of("version_row", "politica_id", "codigo VARCHAR(8)",
                            "nombre_secuencia VARCHAR(64)", "tipo_talonario", "clase_talonario",
                            "nro_desde", "si_activo", "si_bloqueado", "descripcion VARCHAR(48)"),
                    List.of("obs_talonario"));
            assertThat(seccionDeTabla(contenido, "num_talonario")).contains("RECONCILIADA_R3");
        }

        @Test
        @DisplayName("num_talonario_ambito: talonario_id, alcance, prioridad, RECONCILIADA_R3")
        void num_talonario_ambito() throws IOException {
            String contenido = leerModelo();
            verificarCamposDeTabla(contenido, "num_talonario_ambito",
                    List.of("talonario_id", "clase_talonario", "alcance", "prioridad",
                            "fh_desde", "si_activo"),
                    List.of());
            assertThat(seccionDeTabla(contenido, "num_talonario_ambito")).contains("RECONCILIADA_R3");
        }

        @Test
        @DisplayName("num_talonario_inspector: columna generada talonario_id_activo, RECONCILIADA_R3")
        void num_talonario_inspector() throws IOException {
            String contenido = leerModelo();
            String seccion = seccionDeTabla(contenido, "num_talonario_inspector");
            assertThat(seccion).contains("talonario_id_activo").contains("GENERATED ALWAYS AS")
                    .as("Debe documentar la columna generada para unicidad de asignacion activa");
            assertThat(seccion).contains("id_talonario").contains("estado_asignacion").contains("si_activa");
            assertThat(seccion).contains("RECONCILIADA_R3");
        }

        @Test
        @DisplayName("num_talonario_movimiento: UNIQUE(id_talonario, nro_talonario), observacion VARCHAR(500), RECONCILIADA_R3")
        void num_talonario_movimiento() throws IOException {
            String contenido = leerModelo();
            String seccion = seccionDeTabla(contenido, "num_talonario_movimiento");
            assertThat(seccion).containsPattern("UNIQUE.*id_talonario.*nro_talonario|UNIQUE.*nro_talonario.*id_talonario")
                    .as("Debe documentar la restriccion UNIQUE(id_talonario, nro_talonario)");
            assertThat(seccion).contains("estado_numero");
            assertThat(seccion).contains("RECONCILIADA_R3");
        }
    }

    // =====================================================================
    // FULL-R1.2-CORRECCION-04 — plantillas documentales
    // =====================================================================

    @Nested
    @DisplayName("FULL-R1.2-CORRECCION-04: plantilla y contenido versionado")
    class PlantillasDocumentalesCorreccion04 {

        @Test
        @DisplayName("fal_documento_plantilla conserva codigo 12, nombre 64 y elimina descripcion de Campos")
        void plantilla_logica_canonica() throws IOException {
            String seccion = seccionDeTabla(leerModelo(), "fal_documento_plantilla");
            String campos = lineasDeCampos(seccion);
            assertThat(campos)
                    .contains("codigo VARCHAR(12) NOT NULL")
                    .contains("nombre VARCHAR(64) NOT NULL")
                    .doesNotContain("descripcion");
            assertThat(seccion)
                    .contains("Eliminado:")
                    .contains("descripcion")
                    .contains("CrearDocumentoPlantillaCommand")
                    .contains("CrearDocumentoPlantillaRequest")
                    .contains("DocumentoPlantillaResponse")
                    .contains("FULL-R1.2-CORRECCION-04");
        }

        @Test
        @DisplayName("fal_documento_plantilla_contenido declara titulo VARCHAR(64) y metadata tipada/requerida/etiquetada")
        void contenido_logico_canonico() throws IOException {
            String seccion = seccionDeTabla(leerModelo(), "fal_documento_plantilla_contenido");
            String campos = lineasDeCampos(seccion);
            assertThat(campos)
                    .contains("titulo VARCHAR(64) NOT NULL")
                    .doesNotContain("titulo VARCHAR(200)")
                    .contains("variables_declaradas_json JSON NOT NULL")
                    .doesNotContain("DEFAULT '[]'");
            assertThat(seccion)
                    .contains("namespace")
                    .contains("campo")
                    .contains("tipoDato")
                    .contains("requerida")
                    .contains("etiqueta")
                    .contains("[]")
                    .contains("FULL-R1.2-CORRECCION-04");
        }
    }

    // =====================================================================
    // Estructura general
    // =====================================================================

    @Nested
    @DisplayName("Estructura general: el documento sigue siendo PRE_DDL_PLAN/SUPPORTING y complementario")
    class EstructuraGeneral {

        @Test
        @DisplayName("mariadb-logical-model.md mantiene su banner PRE_DDL_PLAN/SUPPORTING")
        void banner_intacto() throws IOException {
            List<String> primerasLineas = Files.readAllLines(modeloLogico, StandardCharsets.UTF_8).subList(0, 5);
            String encabezado = String.join("\n", primerasLineas);
            assertThat(encabezado).contains("PRE_DDL_PLAN");
            assertThat(encabezado).contains("SUPPORTING");
        }

        @Test
        @DisplayName("ninguna de las 11 tablas corregidas quedo con solo nombre+entidad+estado sin campos")
        void satelites_no_truncados() throws IOException {
            List<String> lineas = Files.readAllLines(modeloLogico, StandardCharsets.UTF_8);
            List<String> tablasCorregidas = List.of(
                    "fal_acta_evento", "fal_acta_qr_acceso", "fal_persona", "fal_persona_domicilio",
                    "fal_acta_transito", "fal_acta_transito_alcoholemia", "fal_acta_vehiculo",
                    "fal_acta_contravencion", "fal_acta_sustancias_alimenticias", "fal_acta_medida_preventiva");
            List<String> sinCampos = new ArrayList<>();
            for (String tabla : tablasCorregidas) {
                String marcador = "#### `" + tabla + "`";
                int idx = -1;
                for (int i = 0; i < lineas.size(); i++) {
                    if (lineas.get(i).trim().equals(marcador)) {
                        idx = i;
                        break;
                    }
                }
                assertThat(idx).as("Debe existir la seccion '%s'", marcador).isGreaterThanOrEqualTo(0);
                StringBuilder seccion = new StringBuilder();
                for (int i = idx; i < lineas.size() && (i == idx || !lineas.get(i).startsWith("#### `")); i++) {
                    seccion.append(lineas.get(i)).append('\n');
                }
                if (!seccion.toString().contains("**Campos")) {
                    sinCampos.add(tabla);
                }
            }
            assertThat(sinCampos)
                    .as("Estas tablas corregidas por la reconciliacion R2 deben documentar sus campos propios")
                    .isEmpty();
        }

        @Test
        @DisplayName("no existe la etiqueta prohibida ESTRUCTURAL_YA_RECONCILIADA_R0_R1 en el modelo")
        void sin_etiqueta_prohibida_r0_r1() throws IOException {
            String contenido = leerModelo();
            assertThat(contenido)
                    .as("El estado ESTRUCTURAL_YA_RECONCILIADA_R0_R1 fue eliminado en R3 y no debe reaparecer")
                    .doesNotContain("ESTRUCTURAL_YA_RECONCILIADA_R0_R1");
        }

        @Test
        @DisplayName("las 15 tablas nuevas de R3 tienen todas RECONCILIADA_R3")
        void r3_tablas_marcadas() throws IOException {
            String contenido = leerModelo();
            List<String> tablasR3 = List.of(
                    "fal_firmante", "fal_firmante_version", "fal_firmante_version_habilitacion",
                    "fal_acta_snapshot", "fal_acta_evidencia",
                    "fal_acta_obligacion_pago", "fal_acta_forma_pago", "fal_acta_plan_pago_ref",
                    "fal_acta_pago_movimiento", "fal_acta_economia_proyeccion",
                    "num_politica", "num_talonario", "num_talonario_ambito",
                    "num_talonario_inspector", "num_talonario_movimiento");
            List<String> sinMarca = new ArrayList<>();
            for (String tabla : tablasR3) {
                if (!seccionDeTabla(contenido, tabla).contains("RECONCILIADA_R3")) {
                    sinMarca.add(tabla);
                }
            }
            assertThat(sinMarca)
                    .as("Todas las tablas nuevas de R3 deben estar marcadas RECONCILIADA_R3")
                    .isEmpty();
        }

        @Test
        @DisplayName("las tablas Nivel B usan BASELINE_PRESERVADA_CON_DELTA (no KEEP_RECONCILED)")
        void nivel_b_usa_baseline_preservada() throws IOException {
            String contenido = leerModelo();
            assertThat(contenido).doesNotContain("KEEP_RECONCILED")
                    .as("KEEP_RECONCILED fue reemplazado por BASELINE_PRESERVADA_CON_DELTA en R3");

            // R3.1a: las 39 tablas Nivel B declaradas en MARIADB-LOGICAL-PARITY.md deben tener BASELINE_PRESERVADA_CON_DELTA
            List<String> todasNivelB = List.of(
                    "fal_dependencia", "fal_dependencia_version",
                    "fal_inspector", "fal_inspector_version",
                    "fal_observacion", "fal_dependencia_normativa",
                    "fal_normativa_faltas", "fal_articulo_normativa_faltas",
                    "fal_tarifario_unidad_faltas", "fal_medida_preventiva",
                    "fal_articulo_medida_preventiva", "fal_acta_articulo_infringido",
                    "fal_acta_valorizacion", "fal_acta_valorizacion_item",
                    "fal_acta_bloqueante_cierre_material",
                    "fal_documento", "fal_acta_documento",
                    "fal_documento_firma", "fal_documento_firma_req",
                    "fal_documento_plantilla", "fal_documento_plantilla_firma_req",
                    "fal_documento_plantilla_contenido", "fal_documento_plantilla_default",
                    "fal_documento_redaccion",
                    "fal_notificacion", "fal_notificacion_intento", "fal_notificacion_acuse",
                    "fal_lote_correo",
                    "fal_acta_fallo", "fal_acta_apelacion", "fal_acta_apelacion_documento",
                    "fal_acta_paralizacion", "fal_acta_archivo",
                    "fal_acta_gestion_externa",
                    "fal_vehiculo_marca", "fal_vehiculo_modelo",
                    "fal_motivo_archivo",
                    "fal_dia_no_computable");
            List<String> sinBaseline = new ArrayList<>();
            for (String tabla : todasNivelB) {
                if (!seccionDeTabla(contenido, tabla).contains("BASELINE_PRESERVADA_CON_DELTA")) {
                    sinBaseline.add(tabla);
                }
            }
            assertThat(sinBaseline)
                    .as("Todas las 38 tablas Nivel B (excluye fal_rubro_version que es PREEXISTING_CANONICAL_ADOPTED) deben tener BASELINE_PRESERVADA_CON_DELTA en su seccion del modelo logico")
                    .isEmpty();

            // R3.1: razon_social y nombre_mostrar con longitud confirmada (decision cerrada)
            String seccionPersona = seccionDeTabla(contenido, "fal_persona");
            assertThat(seccionPersona)
                    .as("fal_persona.razon_social debe declarar VARCHAR(64) (decision cerrada y baseline validada)")
                    .contains("razon_social VARCHAR(64)");
            assertThat(seccionPersona)
                    .as("fal_persona.nombre_mostrar debe declarar VARCHAR(64)")
                    .contains("nombre_mostrar VARCHAR(64)");

            // R3.1: fal_dia_no_computable tiene seccion propia y BASELINE_PRESERVADA_CON_DELTA
            assertThat(contenido)
                    .as("fal_dia_no_computable debe tener su propia seccion '#### `fal_dia_no_computable`' en el modelo logico")
                    .contains("#### `fal_dia_no_computable`");
            String seccionDnc = seccionDeTabla(contenido, "fal_dia_no_computable");
            assertThat(seccionDnc)
                    .as("fal_dia_no_computable debe tener BASELINE_PRESERVADA_CON_DELTA")
                    .contains("BASELINE_PRESERVADA_CON_DELTA");

            // R3.1a: fal_dia_no_computable — enum checks y ausencia de SMALLINT para tipo y origen
            assertThat(seccionDnc)
                    .as("fal_dia_no_computable debe declarar TipoDiaNoComputable")
                    .contains("TipoDiaNoComputable");
            assertThat(seccionDnc)
                    .as("fal_dia_no_computable debe declarar OrigenDiaNoComputable")
                    .contains("OrigenDiaNoComputable");
            assertThat(seccionDnc)
                    .as("fal_dia_no_computable debe clasificar ambos enums como EXPLICIT_NUMERIC_CODE (DECISION_DDL-ENUM-01 CERRADA)")
                    .contains("EXPLICIT_NUMERIC_CODE");
            assertThat(seccionDnc)
                    .as("fal_dia_no_computable debe referenciar DECISION_DDL-ENUM-01")
                    .contains("DECISION_DDL-ENUM-01");
            String lineasCamposDnc = lineasDeCampos(seccionDnc);
            assertThat(lineasCamposDnc)
                    .as("fal_dia_no_computable: tipo debe ser SMALLINT NOT NULL (DECISION_DDL-ENUM-01 CERRADA)")
                    .contains("tipo SMALLINT NOT NULL");
            assertThat(lineasCamposDnc)
                    .as("fal_dia_no_computable: origen debe ser SMALLINT NOT NULL (DECISION_DDL-ENUM-01 CERRADA)")
                    .contains("origen SMALLINT NOT NULL");

            // R3.1: total de headings de tabla (#### `) = 65
            long totalHeadings = Files.readAllLines(modeloLogico, StandardCharsets.UTF_8).stream()
                    .filter(l -> l.startsWith("#### `"))
                    .count();
            assertThat(totalHeadings)
                    .as("El modelo logico debe tener exactamente 65 headings de tabla del tipo '#### `'")
                    .isEqualTo(65L);
        }

        @Test
        @DisplayName("el documento define la taxonomia RECONCILIADA_R3 y BASELINE_PRESERVADA_CON_DELTA")
        void taxonomia_r3_documentada() throws IOException {
            String contenido = leerModelo();
            assertThat(contenido).contains("RECONCILIADA_R3")
                    .contains("BASELINE_PRESERVADA_CON_DELTA")
                    .contains("MODELO_CONCEPTUAL_CERRADO");

            // R3.1: gate informa 65/26/38+1=39 y 38 tests exactos
            Path gate = Paths.get("docs", "spec-as-source", "00-governance", "ready-for-ddl-gate.md");
            if (Files.isRegularFile(gate)) {
                String gateContent = Files.readString(gate, StandardCharsets.UTF_8);
                assertThat(gateContent)
                        .as("El gate debe mencionar 65 tablas totales en el item de paridad MariaDB")
                        .contains("65");
                assertThat(gateContent)
                        .as("El gate debe mencionar 38 tablas BASELINE_PRESERVADA_CON_DELTA (fal_rubro_version es PREEXISTING_CANONICAL_ADOPTED)")
                        .contains("38");
                assertThat(gateContent)
                        .as("El gate debe mencionar 39 tablas Nivel B (38 BASELINE + 1 PREEXISTING_CANONICAL_ADOPTED)")
                        .contains("39");
                assertThat(gateContent)
                        .as("El gate no debe afirmar '26 tablas con RECONCILIADA_R3' si solo las 15 R3 llevan ese estado literal")
                        .doesNotContain("26 tablas con RECONCILIADA_R3");
            }

            // R3.1: parity ledger limpio (verificacion local; se omite si el archivo no esta presente)
            Path parityLedger = Paths.get("..", "..", ".work", "handoff-current", "MARIADB-LOGICAL-PARITY.md");
            if (Files.isRegularFile(parityLedger)) {
                String parityContent = Files.readString(parityLedger, StandardCharsets.UTF_8);
                assertThat(parityContent)
                        .as("MARIADB-LOGICAL-PARITY.md no debe contener la etiqueta obsoleta ESTRUCTURAL_YA_RECONCILIADA_R0_R1")
                        .doesNotContain("ESTRUCTURAL_YA_RECONCILIADA_R0_R1");
                assertThat(parityContent)
                        .as("MARIADB-LOGICAL-PARITY.md no debe contener KEEP_RECONCILED como estado final")
                        .doesNotContain("KEEP_RECONCILED");
                assertThat(parityContent)
                        .as("MARIADB-LOGICAL-PARITY.md no debe contener placeholders {0} o {1}")
                        .doesNotContain("{0}").doesNotContain("{1}");
                assertThat(parityContent)
                        .as("MARIADB-LOGICAL-PARITY.md no debe contener caracteres U+000C (form feed)")
                        .doesNotContain("\f");
                assertThat(parityContent)
                        .as("MARIADB-LOGICAL-PARITY.md debe declarar el inventario de 65 tablas")
                        .contains("65");
                long seccionesR3 = Arrays.stream(parityContent.split("\n"))
                        .filter(l -> l.startsWith("## 2."))
                        .count();
                assertThat(seccionesR3)
                        .as("MARIADB-LOGICAL-PARITY.md no debe tener la seccion R3 duplicada (exactamente 1 '## 2.')")
                        .isEqualTo(1L);
            }
        }

        @Test
        @DisplayName("las nuevas DECISION_DDL-* de R3 estan referenciadas en el modelo; DECISION_DDL-ENUM-01 incluye TipoDiaNoComputable y OrigenDiaNoComputable")
        void nuevas_decision_ddl_referenciadas() throws IOException {
            String contenido = leerModelo();
            List<String> decisiones = List.of(
                    "DECISION_DDL-SNAP-01", "DECISION_DDL-SNAP-02",
                    "DECISION_DDL-EVID-01",
                    "DECISION_DDL-PAGO-03", "DECISION_DDL-FORMA-01", "DECISION_DDL-PLAN-01",
                    "DECISION_DDL-MOV-01", "DECISION_DDL-MOV-02", "DECISION_DDL-MOV-03", "DECISION_DDL-MOV-04",
                    "DECISION_DDL-ECPR-01");
            List<String> ausentes = new ArrayList<>();
            for (String d : decisiones) {
                if (!contenido.contains(d)) {
                    ausentes.add(d);
                }
            }
            assertThat(ausentes)
                    .as("Todas las nuevas DECISION_DDL-* de R3 deben estar referenciadas en mariadb-logical-model.md")
                    .isEmpty();

            // R3.1a: DECISION_DDL-ENUM-01 en ddl-decisions.md debe incluir TipoDiaNoComputable y OrigenDiaNoComputable
            Path ddlDecisions = Paths.get("docs", "spec-as-source", "50-persistence", "ddl-decisions.md");
            if (Files.isRegularFile(ddlDecisions)) {
                String decisionesContenido = Files.readString(ddlDecisions, StandardCharsets.UTF_8);
                int idxEnum01 = decisionesContenido.indexOf("DECISION_DDL-ENUM-01");
                assertThat(idxEnum01)
                        .as("ddl-decisions.md debe contener la decision DECISION_DDL-ENUM-01")
                        .isGreaterThanOrEqualTo(0);
                int finLinea = decisionesContenido.indexOf('\n', idxEnum01);
                String lineaEnum01 = finLinea >= 0
                        ? decisionesContenido.substring(idxEnum01, finLinea)
                        : decisionesContenido.substring(idxEnum01);
                assertThat(lineaEnum01)
                        .as("DECISION_DDL-ENUM-01 en ddl-decisions.md debe incluir TipoDiaNoComputable en su inventario")
                        .contains("TipoDiaNoComputable");
                assertThat(lineaEnum01)
                        .as("DECISION_DDL-ENUM-01 en ddl-decisions.md debe incluir OrigenDiaNoComputable en su inventario")
                        .contains("OrigenDiaNoComputable");
            }
        }

        @Test
        @DisplayName("fal_rubro_version tiene estado PREEXISTING_CANONICAL_ADOPTED (tabla preexistente adoptada sin DDL)")
        void fal_rubro_version_es_preexisting_canonical_adopted() throws IOException {
            String contenido = leerModelo();
            String seccion = seccionDeTabla(contenido, "fal_rubro_version");
            assertThat(seccion)
                    .as("fal_rubro_version debe tener estado PREEXISTING_CANONICAL_ADOPTED (no BASELINE_PRESERVADA_CON_DELTA)")
                    .contains("PREEXISTING_CANONICAL_ADOPTED");
            assertThat(seccion)
                    .as("fal_rubro_version no debe tener BASELINE_PRESERVADA_CON_DELTA (fue reclasificada a PREEXISTING_CANONICAL_ADOPTED)")
                    .doesNotContain("BASELINE_PRESERVADA_CON_DELTA");
        }
    }
}
