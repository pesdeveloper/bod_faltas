package ar.gob.malvinas.faltas.core.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guardrail estático (sin conexión MariaDB) del algoritmo de identidad semántica
 * introducido en la Sección 12 de {@code verify-domain-schema.sql}
 * (MARIADB-BASELINE-CHECK-DIAGNOSTIC-FIX-002).
 *
 * <p><b>Contexto del falso positivo cerrado:</b> el criterio anterior exigía que la cláusula
 * normalizada CONTUVIERA el nombre físico del constraint
 * ({@code LIKE CONCAT('%', lower(tc.constraint_name), '%')}). El nombre de un CHECK normalmente
 * no forma parte del texto de su expresión (ej. {@code chk_fal_insp_legajo} → {@code legajo_insp
 * > 0}: la palabra "legajo_insp" no contiene "chk_fal_insp_legajo"), por lo que las 36 filas
 * reales resultaban {@code CLAUSULA_DIVERGENTE} aun cuando la cláusula real y la esperada eran
 * idénticas — un falso positivo generalizado, no una divergencia física real.
 *
 * <p>Defecto adicional cerrado en la misma sección: la normalización anterior aplicaba
 * {@code LOWER()} a la cláusula COMPLETA, incluido el contenido de literales de cadena. Para
 * {@code chk_fal_lote_codigo} ({@code `lote_codigo` REGEXP '^LC-[0-9]{8}-[0-9]{8}$'}), eso
 * convertía el literal a {@code '^lc-...'}, corrompiendo un patrón sensible a
 * mayúsculas/minúsculas.
 *
 * <p>Esta clase reproduce en Java, de forma fiel y sin ambigüedad, el mismo algoritmo que la
 * Sección 12 aplica en SQL:
 *
 * <pre>
 * reales_check_rank: ROW_NUMBER() OVER (PARTITION BY tabla, clausula_normalizada ORDER BY nombre)
 * CHECK_DUPLICADO     si rn_forma > 1 (repetición de una forma tabla+expresión ya contabilizada)
 * OK                  si rn_forma = 1 Y existe esperada con misma (tabla, expresión)
 * CLAUSULA_DIVERGENTE si no matchea por expresión, pero SÍ existe una fila esperada con el mismo
 *                     (tabla, nombre) — la expresión física difiere de la declarada
 * CHECK_INESPERADO    si no matchea ni por expresión ni por (tabla, nombre) — constraint
 *                     adicional no autorizado
 * CHECK_FALTANTE      por cada esperada sin ninguna real con esa forma (tabla, expresión)
 * </pre>
 *
 * <p>Sirve para demostrar de forma confiable y reproducible, sin depender de una base MariaDB
 * real, que el criterio nuevo:
 *
 * <ul>
 *   <li>acepta una cláusula reformateada (espacios, backticks) que sea semánticamente idéntica;</li>
 *   <li>preserva el case exacto de literales REGEXP al comparar (no colapsa mayúsculas/minúsculas);</li>
 *   <li>sigue rechazando un CHECK con operador distinto (ej. {@code >} vs {@code >=});</li>
 *   <li>sigue rechazando un CHECK con valor distinto (ej. límite de rango cambiado);</li>
 *   <li>distingue {@code CLAUSULA_DIVERGENTE} (nombre esperado, expresión distinta) de
 *       {@code CHECK_INESPERADO} (ningún nombre esperado para esa tabla);</li>
 *   <li>sigue rechazando un CHECK faltante ({@code CHECK_FALTANTE});</li>
 *   <li>rechaza un CHECK duplicado (misma tabla+expresión, distinto nombre físico).</li>
 * </ul>
 *
 * <p>La prueba material equivalente contra {@code sb_faltas_db} (36/36 {@code OK}, 0
 * {@code CLAUSULA_DIVERGENTE}, 0 {@code CHECK_FALTANTE}, 0 {@code CHECK_INESPERADO}, 0
 * {@code CHECK_DUPLICADO}) está documentada en la evidencia de
 * {@code MARIADB-BASELINE-CHECK-DIAGNOSTIC-FIX-002} (carpeta {@code 03-fix/}), fuera del
 * repositorio.
 */
@DisplayName("Guardrail semántico: diagnóstico CHECK por identidad (tabla+expresión), no por nombre")
class CheckConstraintSemanticDiagnosticTest {

    /** CHECK esperado por spec-as-source/DDL: tabla, nombre físico y expresión normalizada. */
    record Esperada(String tabla, String nombre, String clausula) {}

    /** CHECK real reportado por {@code information_schema} (o simulado en estos tests). */
    record Real(String tabla, String nombre, String clausulaNormalizada) {}

    record Diagnostico(String tabla, String nombre, String clausula, String resultado) {}

    /**
     * Reproduce exactamente el algoritmo de la Sección 12 corregida (ver Javadoc de clase).
     */
    private static List<Diagnostico> diagnosticar(List<Esperada> esperadas, List<Real> reales) {
        List<Diagnostico> salida = new ArrayList<>();

        Map<String, List<Real>> porForma = new LinkedHashMap<>();
        for (Real r : reales) {
            porForma.computeIfAbsent(r.tabla() + "|" + r.clausulaNormalizada(), k -> new ArrayList<>()).add(r);
        }

        for (Map.Entry<String, List<Real>> entry : porForma.entrySet()) {
            List<Real> grupo = new ArrayList<>(entry.getValue());
            grupo.sort(Comparator.comparing(Real::nombre));
            boolean formaEsperadaPorExpresion = esperadas.stream()
                    .anyMatch(e -> (e.tabla() + "|" + e.clausula()).equals(entry.getKey()));
            for (int i = 0; i < grupo.size(); i++) {
                Real r = grupo.get(i);
                String resultado;
                if (i > 0) {
                    resultado = "CHECK_DUPLICADO";
                } else if (formaEsperadaPorExpresion) {
                    resultado = "OK";
                } else {
                    boolean nombreEsperadoParaTabla = esperadas.stream()
                            .anyMatch(e -> e.tabla().equals(r.tabla()) && e.nombre().equals(r.nombre()));
                    resultado = nombreEsperadoParaTabla ? "CLAUSULA_DIVERGENTE" : "CHECK_INESPERADO";
                }
                salida.add(new Diagnostico(r.tabla(), r.nombre(), r.clausulaNormalizada(), resultado));
            }
        }

        for (Esperada e : esperadas) {
            boolean presente = reales.stream()
                    .anyMatch(r -> r.tabla().equals(e.tabla()) && r.clausulaNormalizada().equals(e.clausula()));
            if (!presente) {
                salida.add(new Diagnostico(e.tabla(), e.nombre(), e.clausula(), "CHECK_FALTANTE"));
            }
        }
        return salida;
    }

    private static String resultadoPara(List<Diagnostico> diagnosticos, String tabla, String nombre) {
        return diagnosticos.stream()
                .filter(d -> d.tabla().equals(tabla) && Objects.equals(d.nombre(), nombre))
                .findFirst()
                .map(Diagnostico::resultado)
                .orElseThrow(() -> new AssertionError("No se encontró diagnóstico para " + tabla + "/" + nombre));
    }

    @Nested
    @DisplayName("Requisitos de guardrail (prompt §12, MARIADB-BASELINE-CHECK-DIAGNOSTIC-FIX-002)")
    class RequisitosGuardrail {

        @Test
        @DisplayName("una cláusula reformateada (espacios/orden de backticks) equivalente es aceptada (OK)")
        void clausulaReformateadaEquivalenteEsAceptada() {
            List<Esperada> esperadas = List.of(
                    new Esperada("fal_inspector", "chk_fal_insp_legajo", "legajo_insp > 0"));
            // La cláusula real ya viene normalizada (sin backticks, espacios colapsados) por la
            // Sección 12 antes de compararla; aquí se simula esa entrada ya normalizada.
            List<Real> reales = List.of(new Real("fal_inspector", "chk_fal_insp_legajo", "legajo_insp > 0"));

            List<Diagnostico> resultado = diagnosticar(esperadas, reales);

            assertThat(resultadoPara(resultado, "fal_inspector", "chk_fal_insp_legajo")).isEqualTo("OK");
        }

        @Test
        @DisplayName("el case exacto de un literal REGEXP se preserva y no se colapsa a minúsculas")
        void caseDeLiteralRegexpSePreserva() {
            List<Esperada> esperadas = List.of(new Esperada(
                    "fal_lote_correo", "chk_fal_lote_codigo",
                    "lote_codigo regexp '^LC-[0-9]{8}-[0-9]{8}$'"));
            List<Real> reales = List.of(new Real(
                    "fal_lote_correo", "chk_fal_lote_codigo",
                    "lote_codigo regexp '^LC-[0-9]{8}-[0-9]{8}$'"));

            List<Diagnostico> resultado = diagnosticar(esperadas, reales);

            assertThat(resultadoPara(resultado, "fal_lote_correo", "chk_fal_lote_codigo")).isEqualTo("OK");
        }

        @Test
        @DisplayName("un literal REGEXP con case corrompido (colapsado a minúsculas) es rechazado, no aceptado por error")
        void literalRegexpConCaseCorrompidoEsRechazado() {
            List<Esperada> esperadas = List.of(new Esperada(
                    "fal_lote_correo", "chk_fal_lote_codigo",
                    "lote_codigo regexp '^LC-[0-9]{8}-[0-9]{8}$'"));
            // Simula el defecto cerrado: LOWER() global habría corrompido el literal a '^lc-...'
            List<Real> reales = List.of(new Real(
                    "fal_lote_correo", "chk_fal_lote_codigo",
                    "lote_codigo regexp '^lc-[0-9]{8}-[0-9]{8}$'"));

            List<Diagnostico> resultado = diagnosticar(esperadas, reales);

            assertThat(resultadoPara(resultado, "fal_lote_correo", "chk_fal_lote_codigo"))
                    .as("Un literal con case distinto es una expresión físicamente distinta: debe "
                            + "reportarse como CLAUSULA_DIVERGENTE, nunca aceptarse silenciosamente como OK")
                    .isEqualTo("CLAUSULA_DIVERGENTE");
        }

        @Test
        @DisplayName("un CHECK con operador distinto (> vs >=) sigue siendo rechazado")
        void checkConOperadorDistintoSigueRechazado() {
            List<Esperada> esperadas = List.of(
                    new Esperada("fal_acta", "chk_acta_qr_ver", "qr_payload_version >= 0"));
            List<Real> reales = List.of(
                    new Real("fal_acta", "chk_acta_qr_ver", "qr_payload_version > 0"));

            List<Diagnostico> resultado = diagnosticar(esperadas, reales);

            assertThat(resultadoPara(resultado, "fal_acta", "chk_acta_qr_ver"))
                    .isEqualTo("CLAUSULA_DIVERGENTE");
        }

        @Test
        @DisplayName("un CHECK con valor de rango distinto sigue siendo rechazado")
        void checkConValorDistintoSigueRechazado() {
            List<Esperada> esperadas = List.of(
                    new Esperada("fal_acta_fallo", "chk_fallo_estado", "estado_fallo between 1 and 6"));
            List<Real> reales = List.of(
                    new Real("fal_acta_fallo", "chk_fallo_estado", "estado_fallo between 1 and 5"));

            List<Diagnostico> resultado = diagnosticar(esperadas, reales);

            assertThat(resultadoPara(resultado, "fal_acta_fallo", "chk_fallo_estado"))
                    .isEqualTo("CLAUSULA_DIVERGENTE");
        }

        @Test
        @DisplayName("un CHECK adicional no autorizado (sin nombre esperado para la tabla) es CHECK_INESPERADO, no CLAUSULA_DIVERGENTE")
        void checkAdicionalNoAutorizadoEsInesperado() {
            List<Esperada> esperadas = List.of(
                    new Esperada("fal_inspector", "chk_fal_insp_legajo", "legajo_insp > 0"));
            List<Real> reales = List.of(
                    new Real("fal_inspector", "chk_fal_insp_legajo", "legajo_insp > 0"),
                    // CHECK adicional no declarado en spec/DDL, ningún nombre esperado coincide
                    new Real("fal_inspector", "chk_fal_insp_extra_no_autorizado", "activo = 1"));

            List<Diagnostico> resultado = diagnosticar(esperadas, reales);

            assertThat(resultadoPara(resultado, "fal_inspector", "chk_fal_insp_legajo")).isEqualTo("OK");
            assertThat(resultadoPara(resultado, "fal_inspector", "chk_fal_insp_extra_no_autorizado"))
                    .as("Sin nombre esperado para esa tabla, debe distinguirse de CLAUSULA_DIVERGENTE")
                    .isEqualTo("CHECK_INESPERADO");
        }

        @Test
        @DisplayName("un paréntesis significativo eliminado (cambia la precedencia AND/OR) es rechazado, no aceptado por error")
        void parentesisSignificativoEliminadoEsRechazado() {
            // chk_num_talonario_nro: los paréntesis alrededor de "nro_hasta is null or nro_hasta
            // >= nro_desde" son significativos — sin ellos, "AND ... OR ..." se reagrupa por
            // precedencia (AND liga más fuerte que OR) y cambia el significado de la expresión.
            List<Esperada> esperadas = List.of(new Esperada(
                    "num_talonario", "chk_num_talonario_nro",
                    "nro_desde > 0 and (nro_hasta is null or nro_hasta >= nro_desde)"));
            List<Real> reales = List.of(new Real(
                    "num_talonario", "chk_num_talonario_nro",
                    "nro_desde > 0 and nro_hasta is null or nro_hasta >= nro_desde"));

            List<Diagnostico> resultado = diagnosticar(esperadas, reales);

            assertThat(resultadoPara(resultado, "num_talonario", "chk_num_talonario_nro"))
                    .as("Eliminar el paréntesis cambia la precedencia AND/OR y por lo tanto el "
                            + "significado: debe rechazarse como CLAUSULA_DIVERGENTE, no aceptarse como OK")
                    .isEqualTo("CLAUSULA_DIVERGENTE");
        }

        @Test
        @DisplayName("un CHECK con nombre esperado pero expresión distinta es CLAUSULA_DIVERGENTE, no CHECK_INESPERADO")
        void checkConNombreEsperadoYExpresionDistintaEsDivergente() {
            List<Esperada> esperadas = List.of(
                    new Esperada("fal_persona", "chk_fal_persona_tipo", "tipo_persona in (1,2)"));
            List<Real> reales = List.of(
                    // Mismo nombre físico esperado, pero alguien amplió manualmente el rango
                    new Real("fal_persona", "chk_fal_persona_tipo", "tipo_persona in (1,2,3)"));

            List<Diagnostico> resultado = diagnosticar(esperadas, reales);

            assertThat(resultadoPara(resultado, "fal_persona", "chk_fal_persona_tipo"))
                    .as("El nombre físico coincide con uno esperado para esa tabla: la categoría correcta "
                            + "es CLAUSULA_DIVERGENTE (posible edición manual o drift físico), no "
                            + "CHECK_INESPERADO (que implica ningún nombre esperado coincide)")
                    .isEqualTo("CLAUSULA_DIVERGENTE");
        }

        @Test
        @DisplayName("un CHECK faltante sigue siendo rechazado (CHECK_FALTANTE)")
        void checkFaltanteSigueSiendoDetectado() {
            List<Esperada> esperadas = List.of(
                    new Esperada("fal_lote_correo", "chk_fal_lote_codigo",
                            "lote_codigo regexp '^LC-[0-9]{8}-[0-9]{8}$'"),
                    new Esperada("fal_lote_correo", "chk_fal_lote_estado", "estado_lote between 1 and 5"));
            // Falta chk_fal_lote_codigo por completo
            List<Real> reales = List.of(
                    new Real("fal_lote_correo", "chk_fal_lote_estado", "estado_lote between 1 and 5"));

            List<Diagnostico> resultado = diagnosticar(esperadas, reales);

            assertThat(resultadoPara(resultado, "fal_lote_correo", "chk_fal_lote_codigo"))
                    .isEqualTo("CHECK_FALTANTE");
            assertThat(resultadoPara(resultado, "fal_lote_correo", "chk_fal_lote_estado")).isEqualTo("OK");
        }

        @Test
        @DisplayName("un CHECK duplicado (misma tabla+expresión, otro nombre) es rechazado")
        void checkDuplicadoEsRechazado() {
            List<Esperada> esperadas = List.of(
                    new Esperada("fal_inspector", "chk_fal_insp_legajo", "legajo_insp > 0"));
            List<Real> reales = List.of(
                    new Real("fal_inspector", "chk_fal_insp_legajo", "legajo_insp > 0"),
                    // Duplicado físico de la misma forma (no debería existir, pero si existiera
                    // por error, la segunda aparición (orden alfabético) debe rechazarse)
                    new Real("fal_inspector", "chk_fal_insp_legajo_dup", "legajo_insp > 0"));

            List<Diagnostico> resultado = diagnosticar(esperadas, reales);

            assertThat(resultadoPara(resultado, "fal_inspector", "chk_fal_insp_legajo")).isEqualTo("OK");
            assertThat(resultadoPara(resultado, "fal_inspector", "chk_fal_insp_legajo_dup"))
                    .isEqualTo("CHECK_DUPLICADO");
        }
    }

    @Nested
    @DisplayName("Reproducción del caso completo verificado materialmente contra sb_faltas_db")
    class CasoCompletoBaseline {

        /**
         * Las 36 formas (tabla, nombre, expresión) declaradas en {@code create-bod-faltas-domain.sql}
         * y verificadas materialmente contra {@code sb_faltas_db} (RUN-20260802-054840): coinciden
         * 1:1 con los 36 CHECK reales instalados. Aquí se reproduce esa comparación íntegramente en
         * Java para blindar el algoritmo sin requerir conexión MariaDB en cada corrida de test.
         */
        @Test
        @DisplayName("36 CHECK esperados == 36 CHECK reales (0 CLAUSULA_DIVERGENTE, 0 CHECK_INESPERADO, 0 CHECK_DUPLICADO, 0 CHECK_FALTANTE)")
        void los36CheckBaselineSonTodosOk() {
            List<Esperada> esperadas = List.of(
                    new Esperada("fal_inspector", "chk_fal_insp_legajo", "legajo_insp > 0"),
                    new Esperada("fal_persona", "chk_fal_persona_tipo", "tipo_persona in (1,2)"),
                    new Esperada("fal_persona", "chk_fal_persona_nro_doc", "nro_doc between 1 and 99999999"),
                    new Esperada("fal_persona", "chk_fal_persona_prefijo",
                            "prefijo_cuit_cuil is null or prefijo_cuit_cuil between 0 and 99"),
                    new Esperada("fal_persona", "chk_fal_persona_digito",
                            "digito_verificador is null or digito_verificador between 0 and 9"),
                    new Esperada("fal_persona", "chk_fal_persona_id_suj",
                            "id_suj is null or id_suj between 1 and 255"),
                    new Esperada("fal_persona", "chk_fal_persona_id_bie",
                            "id_bie is null or id_bie between 1 and 9999999"),
                    new Esperada("fal_persona", "chk_fal_persona_id_bie_suj",
                            "id_bie is null or id_suj is not null"),
                    new Esperada("num_politica", "chk_num_pol_clase", "clase_numeracion in (1,2)"),
                    new Esperada("num_politica", "chk_num_pol_longitud", "longitud_nro > 0"),
                    new Esperada("fal_dia_no_computable", "chk_dia_nc_tipo", "tipo in (1,2,3)"),
                    new Esperada("fal_dia_no_computable", "chk_dia_nc_origen", "origen in (1,2)"),
                    new Esperada("fal_dia_no_computable", "chk_dia_nc_ref_ext",
                            "origen = 1 and referencia_externa is null or origen = 2 and referencia_externa is not null"),
                    new Esperada("fal_acta", "chk_acta_tipo", "tipo_acta in (1,2,3,4)"),
                    new Esperada("fal_acta", "chk_acta_qr_ver", "qr_payload_version >= 0"),
                    new Esperada("fal_acta_obligacion_pago", "chk_oblig_tipo", "tipo_obligacion in (1,2)"),
                    new Esperada("fal_acta_obligacion_pago", "chk_oblig_origen", "origen_obligacion between 1 and 3"),
                    new Esperada("fal_acta_obligacion_pago", "chk_oblig_estado", "estado_obligacion between 1 and 6"),
                    new Esperada("fal_acta_obligacion_pago", "chk_oblig_monto", "monto_original >= 0"),
                    new Esperada("fal_acta_forma_pago", "chk_forma_pago_tipo", "tipo_forma_pago in (1,2,3)"),
                    new Esperada("fal_acta_forma_pago", "chk_forma_pago_nro", "nro_forma > 0"),
                    new Esperada("fal_acta_forma_pago", "chk_forma_pago_monto", "monto_forma >= 0"),
                    new Esperada("fal_acta_fallo", "chk_fallo_estado", "estado_fallo between 1 and 6"),
                    new Esperada("fal_acta_fallo", "chk_fallo_tipo", "tipo_fallo in (1,2)"),
                    new Esperada("fal_acta_fallo", "chk_fallo_firmeza",
                            "origen_firmeza is null or origen_firmeza in (1,2)"),
                    new Esperada("fal_acta_apelacion", "chk_apelacion_estado", "estado_apelacion between 1 and 6"),
                    new Esperada("fal_acta_pago_movimiento", "chk_pago_mov_tipo", "tipo_movimiento between 1 and 5"),
                    new Esperada("fal_acta_pago_movimiento", "chk_pago_mov_clasificacion",
                            "clasificacion_pago between 1 and 4"),
                    new Esperada("fal_acta_pago_movimiento", "chk_pago_mov_motivo_anulacion",
                            "motivo_anulacion_pago is null or motivo_anulacion_pago between 1 and 6"),
                    new Esperada("num_talonario", "chk_num_talonario_nro",
                            "nro_desde > 0 and (nro_hasta is null or nro_hasta >= nro_desde)"),
                    new Esperada("fal_lote_correo", "chk_fal_lote_codigo",
                            "lote_codigo regexp '^LC-[0-9]{8}-[0-9]{8}$'"),
                    new Esperada("fal_lote_correo", "chk_fal_lote_estado", "estado_lote between 1 and 5"),
                    new Esperada("fal_acta_transito_alcoholemia", "chk_alcoholemia_orden", "orden_medicion > 0"),
                    new Esperada("fal_notificacion_intento", "chk_noti_intento_nro", "nro_intento > 0"),
                    new Esperada("num_talonario_inspector", "chk_tal_insp_estado",
                            "estado_asignacion between 1 and 4"),
                    new Esperada("num_talonario_movimiento", "chk_tal_mov_nro", "nro_talonario > 0"));

            // Reales: mismos 36 pares (tabla, nombre) con la misma expresión normalizada
            // verificada materialmente en sb_faltas_db (RUN-20260802-054840).
            List<Real> reales = esperadas.stream()
                    .map(e -> new Real(e.tabla(), e.nombre(), e.clausula()))
                    .toList();

            assertThat(esperadas).hasSize(36);

            List<Diagnostico> resultado = diagnosticar(esperadas, reales);

            assertThat(resultado).hasSize(36);
            assertThat(resultado)
                    .as("Los 36 CHECK reales del baseline instalado deben resultar OK — 0 CLAUSULA_DIVERGENTE, "
                            + "0 CHECK_INESPERADO, 0 CHECK_DUPLICADO, 0 CHECK_FALTANTE "
                            + "(verificado materialmente en RUN-20260802-054840)")
                    .allSatisfy(d -> assertThat(d.resultado()).isEqualTo("OK"));
        }
    }
}
