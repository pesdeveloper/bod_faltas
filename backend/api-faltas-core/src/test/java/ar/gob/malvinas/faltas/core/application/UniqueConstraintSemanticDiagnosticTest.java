package ar.gob.malvinas.faltas.core.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guardrail estático (sin conexión MariaDB) del algoritmo de identidad semántica
 * introducido en la Sección 10 de {@code verify-domain-schema.sql}
 * (MARIADB-BASELINE-DIAGNOSTIC-FIX-001).
 *
 * <p><b>Contexto del falso positivo cerrado:</b> el criterio anterior exigía que el nombre
 * del constraint UNIQUE comenzara por {@code uq_<nombre_completo_de_tabla>}. La convención real
 * del DDL es {@code uq_<abreviatura>_*} (ej. {@code uq_fal_dep_cod} para {@code fal_dependencia}),
 * por lo que 25 de 33 UNIQUE reales eran reportadas como {@code UNIQUE_INESPERADA} sin ser un
 * defecto físico. El criterio nuevo compara identidad semántica: tabla + columnas + orden.
 *
 * <p>Esta clase reproduce en Java, de forma fiel y sin ambigüedad, el mismo algoritmo que la
 * Sección 10 aplica en SQL (agrupar por tabla+columnas, marcar como {@code OK} la primera
 * aparición de cada forma si está en la lista esperada, {@code UNIQUE_INESPERADA} en caso
 * contrario o si es una repetición de una forma ya contabilizada, y {@code UNIQUE_FALTANTE}
 * para toda forma esperada sin ninguna aparición real). Sirve para demostrar de forma
 * confiable y reproducible, sin depender de una base MariaDB real, que el criterio nuevo:
 *
 * <ul>
 *   <li>acepta el nombre físico real (abreviado) cuando la forma (tabla+columnas) es la esperada;</li>
 *   <li>acepta la semántica esperada aunque el nombre no siga ningún patrón fijo;</li>
 *   <li>sigue rechazando un UNIQUE adicional no autorizado;</li>
 *   <li>sigue rechazando un UNIQUE con columnas (u orden) equivocado;</li>
 *   <li>sigue rechazando un UNIQUE faltante;</li>
 *   <li>rechaza un UNIQUE duplicado (misma tabla+columnas, distinto nombre).</li>
 * </ul>
 *
 * <p>La prueba material equivalente contra {@code sb_faltas_db} (33/33 {@code OK}, 0
 * {@code UNIQUE_INESPERADA}, 0 {@code UNIQUE_FALTANTE}) está documentada en la evidencia de
 * {@code MARIADB-BASELINE-DIAGNOSTIC-FIX-001} (carpeta {@code 03-fix/}), fuera del repositorio.
 */
@DisplayName("Guardrail semántico: diagnóstico UNIQUE por identidad (tabla+columnas), no por nombre")
class UniqueConstraintSemanticDiagnosticTest {

    /** UNIQUE esperada por spec-as-source/DDL: identidad semántica, sin nombre físico. */
    record Esperada(String tabla, String columnas) {}

    /** UNIQUE real reportada por {@code information_schema} (o simulada en estos tests). */
    record Real(String tabla, String nombre, String columnas) {}

    record Diagnostico(String tabla, String nombre, String columnas, String resultado) {}

    /**
     * Reproduce exactamente el algoritmo de la Sección 10 corregida:
     * <pre>
     * reales_rank: ROW_NUMBER() OVER (PARTITION BY tabla, columnas ORDER BY nombre)
     * OK                si rn_forma = 1 Y existe esperada con misma (tabla, columnas)
     * UNIQUE_INESPERADA si rn_forma > 1 (duplicada) O no existe esperada con esa forma
     * UNIQUE_FALTANTE   por cada esperada sin ninguna real con esa forma
     * </pre>
     */
    private static List<Diagnostico> diagnosticar(List<Esperada> esperadas, List<Real> reales) {
        List<Diagnostico> salida = new ArrayList<>();

        Map<String, List<Real>> porForma = new LinkedHashMap<>();
        for (Real r : reales) {
            porForma.computeIfAbsent(r.tabla() + "|" + r.columnas(), k -> new ArrayList<>()).add(r);
        }

        for (Map.Entry<String, List<Real>> entry : porForma.entrySet()) {
            List<Real> grupo = new ArrayList<>(entry.getValue());
            grupo.sort(Comparator.comparing(Real::nombre));
            boolean formaEsperada = esperadas.stream()
                    .anyMatch(e -> (e.tabla() + "|" + e.columnas()).equals(entry.getKey()));
            for (int i = 0; i < grupo.size(); i++) {
                Real r = grupo.get(i);
                String resultado = (i > 0 || !formaEsperada) ? "UNIQUE_INESPERADA" : "OK";
                salida.add(new Diagnostico(r.tabla(), r.nombre(), r.columnas(), resultado));
            }
        }

        for (Esperada e : esperadas) {
            boolean presente = reales.stream()
                    .anyMatch(r -> r.tabla().equals(e.tabla()) && r.columnas().equals(e.columnas()));
            if (!presente) {
                salida.add(new Diagnostico(e.tabla(), null, e.columnas(), "UNIQUE_FALTANTE"));
            }
        }
        return salida;
    }

    private static String resultadoPara(List<Diagnostico> diagnosticos, String tabla, String nombre) {
        return diagnosticos.stream()
                .filter(d -> d.tabla().equals(tabla) && java.util.Objects.equals(d.nombre(), nombre))
                .findFirst()
                .map(Diagnostico::resultado)
                .orElseThrow(() -> new AssertionError("No se encontró diagnóstico para " + tabla + "/" + nombre));
    }

    @Nested
    @DisplayName("Requisitos de guardrail (prompt §9, MARIADB-BASELINE-DIAGNOSTIC-FIX-001)")
    class RequisitosGuardrail {

        @Test
        @DisplayName("el nombre real abreviado (uq_fal_dep_cod) es aceptado cuando la forma coincide")
        void nombreRealValidoEsAceptado() {
            List<Esperada> esperadas = List.of(new Esperada("fal_dependencia", "cod_dep"));
            List<Real> reales = List.of(new Real("fal_dependencia", "uq_fal_dep_cod", "cod_dep"));

            List<Diagnostico> resultado = diagnosticar(esperadas, reales);

            assertThat(resultadoPara(resultado, "fal_dependencia", "uq_fal_dep_cod")).isEqualTo("OK");
        }

        @Test
        @DisplayName("la semántica esperada (tabla+columnas) es aceptada aunque el nombre no siga ningún patrón fijo")
        void semanticaEsperadaEsAceptadaSinPatronDeNombre() {
            List<Esperada> esperadas = List.of(
                    new Esperada("fal_documento_firma_req", "id_documento,seq_firma_req"));
            // Nombre real deliberadamente sin relación textual con la tabla completa
            List<Real> reales = List.of(
                    new Real("fal_documento_firma_req", "uq_firma_req_doc_seq", "id_documento,seq_firma_req"));

            List<Diagnostico> resultado = diagnosticar(esperadas, reales);

            assertThat(resultadoPara(resultado, "fal_documento_firma_req", "uq_firma_req_doc_seq"))
                    .isEqualTo("OK");
        }

        @Test
        @DisplayName("un UNIQUE adicional no autorizado sigue siendo rechazado")
        void uniqueAdicionalNoAutorizadoSigueRechazado() {
            List<Esperada> esperadas = List.of(new Esperada("fal_dependencia", "cod_dep"));
            List<Real> reales = List.of(
                    new Real("fal_dependencia", "uq_fal_dep_cod", "cod_dep"),
                    // UNIQUE adicional no declarado en spec/DDL (columna distinta, no autorizada)
                    new Real("fal_dependencia", "uq_fal_dep_nom_extra", "nom_dep"));

            List<Diagnostico> resultado = diagnosticar(esperadas, reales);

            assertThat(resultadoPara(resultado, "fal_dependencia", "uq_fal_dep_cod")).isEqualTo("OK");
            assertThat(resultadoPara(resultado, "fal_dependencia", "uq_fal_dep_nom_extra"))
                    .isEqualTo("UNIQUE_INESPERADA");
        }

        @Test
        @DisplayName("un UNIQUE con columnas u orden equivocado sigue siendo rechazado")
        void uniqueConColumnasEquivocadasSigueRechazado() {
            List<Esperada> esperadas = List.of(
                    new Esperada("fal_persona", "tipo_documento,doc_key"));
            // Mismo nombre físico esperado, pero orden de columnas invertido: identidad distinta
            List<Real> reales = List.of(
                    new Real("fal_persona", "uq_fal_persona_tipo_doc_key", "doc_key,tipo_documento"));

            List<Diagnostico> resultado = diagnosticar(esperadas, reales);

            assertThat(resultadoPara(resultado, "fal_persona", "uq_fal_persona_tipo_doc_key"))
                    .as("El orden de columnas es parte de la identidad semántica: invertirlo debe rechazarse")
                    .isEqualTo("UNIQUE_INESPERADA");
        }

        @Test
        @DisplayName("un UNIQUE faltante sigue siendo rechazado (UNIQUE_FALTANTE)")
        void uniqueFaltanteSigueSiendoDetectado() {
            List<Esperada> esperadas = List.of(
                    new Esperada("fal_acta", "codigo_qr"),
                    new Esperada("fal_acta", "id_tecnico"));
            // Falta uq_fal_acta_codigo_qr por completo
            List<Real> reales = List.of(
                    new Real("fal_acta", "uq_fal_acta_id_tecnico", "id_tecnico"));

            List<Diagnostico> resultado = diagnosticar(esperadas, reales);

            assertThat(resultadoPara(resultado, "fal_acta", null)).isEqualTo("UNIQUE_FALTANTE");
            assertThat(resultadoPara(resultado, "fal_acta", "uq_fal_acta_id_tecnico")).isEqualTo("OK");
        }

        @Test
        @DisplayName("un UNIQUE duplicado (misma tabla+columnas, otro nombre) es rechazado")
        void uniqueDuplicadoEsRechazado() {
            List<Esperada> esperadas = List.of(new Esperada("fal_dependencia", "cod_dep"));
            List<Real> reales = List.of(
                    new Real("fal_dependencia", "uq_fal_dep_cod", "cod_dep"),
                    // Duplicado físico de la misma forma (no debería existir, pero si existiera
                    // por error, la segunda aparición (orden alfabético) debe rechazarse)
                    new Real("fal_dependencia", "uq_fal_dep_cod_dup", "cod_dep"));

            List<Diagnostico> resultado = diagnosticar(esperadas, reales);

            assertThat(resultadoPara(resultado, "fal_dependencia", "uq_fal_dep_cod")).isEqualTo("OK");
            assertThat(resultadoPara(resultado, "fal_dependencia", "uq_fal_dep_cod_dup"))
                    .isEqualTo("UNIQUE_INESPERADA");
        }
    }

    @Nested
    @DisplayName("Reproducción del caso completo verificado materialmente contra sb_faltas_db")
    class CasoCompletoBaseline {

        /**
         * Las 33 formas (tabla, columnas) declaradas en {@code create-bod-faltas-domain.sql} y
         * verificadas materialmente contra {@code sb_faltas_db} (RUN-20260802-051800): coinciden
         * 1:1 con las 33 UNIQUE reales instaladas. Aquí se reproduce esa comparación íntegramente
         * en Java para blindar el algoritmo sin requerir conexión MariaDB en cada corrida de test.
         */
        @Test
        @DisplayName("33 UNIQUE esperadas == 33 UNIQUE reales (mismos nombres físicos abreviados que antes eran falsos positivos)")
        void las33UniqueBaselineSonTodasOk() {
            List<Esperada> esperadas = List.of(
                    new Esperada("fal_dependencia", "cod_dep"),
                    new Esperada("fal_inspector", "id_user"),
                    new Esperada("fal_inspector", "legajo_insp"),
                    new Esperada("fal_firmante", "id_user"),
                    new Esperada("fal_persona", "tipo_documento,doc_key"),
                    new Esperada("fal_vehiculo_marca", "codigo"),
                    new Esperada("num_politica", "codigo"),
                    new Esperada("fal_dia_no_computable", "fecha_activa"),
                    new Esperada("fal_motivo_archivo", "cod_motivo_archivo"),
                    new Esperada("fal_normativa_faltas", "codigo_norma"),
                    new Esperada("fal_medida_preventiva", "codigo"),
                    new Esperada("fal_lote_correo", "lote_codigo"),
                    new Esperada("fal_documento_plantilla", "codigo"),
                    new Esperada("fal_vehiculo_modelo", "marca_vehiculo_id,codigo"),
                    new Esperada("num_talonario", "codigo"),
                    new Esperada("num_talonario", "nombre_secuencia"),
                    new Esperada("fal_articulo_normativa_faltas", "id_normativa,codigo_articulo"),
                    new Esperada("fal_documento_plantilla_contenido", "id_plantilla,version_contenido"),
                    new Esperada("num_talonario_inspector", "talonario_id_activo"),
                    new Esperada("fal_acta", "id_tecnico"),
                    new Esperada("fal_acta", "codigo_qr"),
                    new Esperada("fal_acta_transito_alcoholemia", "acta_id,orden_medicion"),
                    new Esperada("fal_acta_articulo_infringido", "id_acta_activo,id_art_activo"),
                    new Esperada("fal_documento_firma_req", "id_documento,seq_firma_req"),
                    new Esperada("fal_documento_redaccion", "id_documento,nro_revision"),
                    new Esperada("num_talonario_movimiento", "id_talonario,nro_talonario"),
                    new Esperada("fal_notificacion_intento", "id_notificacion,nro_intento"),
                    new Esperada("fal_acta_obligacion_pago", "acta_id_vigente"),
                    new Esperada("fal_acta_forma_pago", "obligacion_pago_id_vigente"),
                    new Esperada("fal_acta_forma_pago", "obligacion_pago_id,nro_forma"),
                    new Esperada("fal_acta_plan_pago_ref", "id_tdoc_plan,id_doc_plan"),
                    new Esperada("fal_acta_plan_pago_ref", "obligacion_pago_id_vigente"),
                    new Esperada("fal_acta_pago_movimiento", "movimiento_origen_id_aplicacion"));

            // Reales: mismas 33 formas, con los nombres físicos reales verificados en
            // sb_faltas_db (25 de ellos NO seguían el patrón uq_<tabla_completa>_* anterior).
            List<Real> reales = List.of(
                    new Real("fal_dependencia", "uq_fal_dep_cod", "cod_dep"),
                    new Real("fal_inspector", "uq_fal_insp_id_user", "id_user"),
                    new Real("fal_inspector", "uq_fal_insp_legajo", "legajo_insp"),
                    new Real("fal_firmante", "uq_fal_firmante_id_user", "id_user"),
                    new Real("fal_persona", "uq_fal_persona_tipo_doc_key", "tipo_documento,doc_key"),
                    new Real("fal_vehiculo_marca", "uq_fal_veh_marca_codigo", "codigo"),
                    new Real("num_politica", "uq_num_politica_codigo", "codigo"),
                    new Real("fal_dia_no_computable", "uq_fal_dia_nc_fecha_activa", "fecha_activa"),
                    new Real("fal_motivo_archivo", "uq_fal_motivo_arch_cod", "cod_motivo_archivo"),
                    new Real("fal_normativa_faltas", "uq_fal_norm_codigo", "codigo_norma"),
                    new Real("fal_medida_preventiva", "uq_fal_med_prev_codigo", "codigo"),
                    new Real("fal_lote_correo", "uq_fal_lote_codigo", "lote_codigo"),
                    new Real("fal_documento_plantilla", "uq_fal_docu_plantilla_codigo", "codigo"),
                    new Real("fal_vehiculo_modelo", "uq_fal_veh_modelo_marca_codigo", "marca_vehiculo_id,codigo"),
                    new Real("num_talonario", "uq_num_talonario_codigo", "codigo"),
                    new Real("num_talonario", "uq_num_talonario_secuencia", "nombre_secuencia"),
                    new Real("fal_articulo_normativa_faltas", "uq_fal_art_norm_cod", "id_normativa,codigo_articulo"),
                    new Real("fal_documento_plantilla_contenido", "uq_fal_plantilla_cont_ver", "id_plantilla,version_contenido"),
                    new Real("num_talonario_inspector", "uq_tal_insp_activo", "talonario_id_activo"),
                    new Real("fal_acta", "uq_fal_acta_id_tecnico", "id_tecnico"),
                    new Real("fal_acta", "uq_fal_acta_codigo_qr", "codigo_qr"),
                    new Real("fal_acta_transito_alcoholemia", "uq_acta_alcoholemia_orden", "acta_id,orden_medicion"),
                    new Real("fal_acta_articulo_infringido", "uq_acta_art_activo", "id_acta_activo,id_art_activo"),
                    new Real("fal_documento_firma_req", "uq_firma_req_doc_seq", "id_documento,seq_firma_req"),
                    new Real("fal_documento_redaccion", "uq_redaccion_doc_rev", "id_documento,nro_revision"),
                    new Real("num_talonario_movimiento", "uq_tal_mov_nro", "id_talonario,nro_talonario"),
                    new Real("fal_notificacion_intento", "uq_noti_intento_nro", "id_notificacion,nro_intento"),
                    new Real("fal_acta_obligacion_pago", "uq_oblig_pago_vigente", "acta_id_vigente"),
                    new Real("fal_acta_forma_pago", "uq_forma_pago_vigente", "obligacion_pago_id_vigente"),
                    new Real("fal_acta_forma_pago", "uq_forma_pago_nro", "obligacion_pago_id,nro_forma"),
                    new Real("fal_acta_plan_pago_ref", "uq_plan_pago_ext", "id_tdoc_plan,id_doc_plan"),
                    new Real("fal_acta_plan_pago_ref", "uq_plan_pago_vigente", "obligacion_pago_id_vigente"),
                    new Real("fal_acta_pago_movimiento", "uq_pago_mov_pagres_unicidad", "movimiento_origen_id_aplicacion"));

            List<Diagnostico> resultado = diagnosticar(esperadas, reales);

            assertThat(resultado).hasSize(33);
            assertThat(resultado)
                    .as("Las 33 UNIQUE reales del baseline instalado deben resultar OK — 0 UNIQUE_INESPERADA, "
                            + "0 UNIQUE_FALTANTE (verificado materialmente en RUN-20260802-051800)")
                    .allSatisfy(d -> assertThat(d.resultado()).isEqualTo("OK"));
        }
    }
}
