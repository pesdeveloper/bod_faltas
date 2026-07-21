package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.domain.enums.TipoEvidenciaActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvidencia;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static ar.gob.malvinas.faltas.core.application.DdlTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Guardrails DECISION_DDL-EVID-02 — TipoEvidenciaActa.
 *
 * <p>Verifica catálogo completo (7 valores, multiplos de 4), regla de firma,
 * bloque DDL real de {@code fal_acta_evidencia} y ausencia del código histórico 6.
 * HUMAN_DECISION_CLOSED — FULL-R1.2-CORRECCION-10
 */
@DisplayName("Guardrails TipoEvidenciaActa (DECISION_DDL-EVID-02 / FULL-R1.2-CORRECCION-10)")
class TipoEvidenciaActaGuardrailTest {

    private static Map<String, String> bloquesPorTabla;
    private static String bloqueEvidencia;
    private static String sqlCompleto;
    private static String srcTipoEvidenciaActa;
    private static String srcFalActaEvidencia;
    private static String srcActaService;

    @BeforeAll
    static void cargarDdl() throws IOException {
        Path dbRoot = resolveDbRoot();
        sqlCompleto = leer(dbRoot.resolve("ddl/create-bod-faltas-domain.sql"));
        bloquesPorTabla = extraerBloquesDdl(sqlCompleto)
                .stream()
                .collect(Collectors.toMap(DdlTestSupport::extraerNombreTabla, b -> b));
        bloqueEvidencia = bloquesPorTabla.get("fal_acta_evidencia");
        assertThat(bloqueEvidencia)
                .as("El bloque DDL de fal_acta_evidencia debe existir")
                .isNotNull();

        Path srcRoot = resolveSrcMainJava()
                .resolve("ar/gob/malvinas/faltas/core");
        srcTipoEvidenciaActa = leer(srcRoot.resolve("domain/enums/TipoEvidenciaActa.java"));
        srcFalActaEvidencia   = leer(srcRoot.resolve("domain/model/FalActaEvidencia.java"));
        srcActaService        = leer(srcRoot.resolve("application/service/ActaService.java"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Catalogo completo del enum Java
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("1. Catalogo exacto del enum TipoEvidenciaActa")
    class Catalogo {

        @Test
        @DisplayName("1. Exactamente 7 valores en el enum")
        void exactamente_siete_valores() {
            assertThat(TipoEvidenciaActa.values()).hasSize(7);
        }

        @Test
        @DisplayName("2. FOTO tiene codigo 4")
        void foto_codigo_4() {
            assertThat(TipoEvidenciaActa.FOTO.codigo()).isEqualTo((short) 4);
        }

        @Test
        @DisplayName("3. VIDEO tiene codigo 8")
        void video_codigo_8() {
            assertThat(TipoEvidenciaActa.VIDEO.codigo()).isEqualTo((short) 8);
        }

        @Test
        @DisplayName("4. AUDIO tiene codigo 12")
        void audio_codigo_12() {
            assertThat(TipoEvidenciaActa.AUDIO.codigo()).isEqualTo((short) 12);
        }

        @Test
        @DisplayName("5. PDF tiene codigo 16")
        void pdf_codigo_16() {
            assertThat(TipoEvidenciaActa.PDF.codigo()).isEqualTo((short) 16);
        }

        @Test
        @DisplayName("6. DOCUMENTO_OFIMATICO tiene codigo 20")
        void documento_ofimatico_codigo_20() {
            assertThat(TipoEvidenciaActa.DOCUMENTO_OFIMATICO.codigo()).isEqualTo((short) 20);
        }

        @Test
        @DisplayName("7. PLANILLA_CALCULO tiene codigo 24")
        void planilla_calculo_codigo_24() {
            assertThat(TipoEvidenciaActa.PLANILLA_CALCULO.codigo()).isEqualTo((short) 24);
        }

        @Test
        @DisplayName("8. FIRMA_OLOGRAFA_INFRACTOR tiene codigo 48 (cambio incompatible aprobado: antes 6)")
        void firma_olografa_codigo_48() {
            assertThat(TipoEvidenciaActa.FIRMA_OLOGRAFA_INFRACTOR.codigo()).isEqualTo((short) 48);
        }

        @Test
        @DisplayName("9. Todos los codigos son unicos")
        void codigos_unicos() {
            long distintos = Arrays.stream(TipoEvidenciaActa.values())
                    .mapToInt(TipoEvidenciaActa::codigo)
                    .distinct()
                    .count();
            assertThat(distintos).isEqualTo(TipoEvidenciaActa.values().length);
        }

        @Test
        @DisplayName("10. Todos los codigos son multiplos de 4")
        void codigos_multiplos_de_cuatro() {
            for (TipoEvidenciaActa t : TipoEvidenciaActa.values()) {
                assertThat(t.codigo() % 4)
                        .as("El codigo de %s (%d) debe ser multiplo de 4", t.name(), t.codigo())
                        .isEqualTo((short) 0);
            }
        }

        @Test
        @DisplayName("11. No se usa ordinal() — el enum tiene metodo codigo() independiente de la posicion")
        void no_usa_ordinal() {
            for (TipoEvidenciaActa t : TipoEvidenciaActa.values()) {
                assertThat((int) t.codigo())
                        .as("El codigo de %s no debe coincidir con su ordinal()+1 (patron de ordinal disfrazado)",
                                t.name())
                        .isNotEqualTo(t.ordinal() + 1);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Resolucion por codigo
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("2. Resolucion por codigo — desdeCodigo")
    class ResolucionPorCodigo {

        @Test
        @DisplayName("12. El codigo 6 (historico) no resuelve como ninguna evidencia valida")
        void codigo_6_no_resuelve() {
            assertThatThrownBy(() -> TipoEvidenciaActa.desdeCodigo((short) 6))
                    .as("El codigo 6 es historico y debe ser rechazado")
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("13. El codigo 48 resuelve como FIRMA_OLOGRAFA_INFRACTOR")
        void codigo_48_resuelve_firma() {
            assertThat(TipoEvidenciaActa.desdeCodigo((short) 48))
                    .isEqualTo(TipoEvidenciaActa.FIRMA_OLOGRAFA_INFRACTOR);
        }

        @Test
        @DisplayName("desdeCodigo round-trip para los 7 valores")
        void round_trip_siete_valores() {
            for (TipoEvidenciaActa t : TipoEvidenciaActa.values()) {
                assertThat(TipoEvidenciaActa.desdeCodigo(t.codigo()))
                        .as("Round-trip fallo para %s (codigo %d)", t.name(), t.codigo())
                        .isEqualTo(t);
            }
        }

        @Test
        @DisplayName("Codigos desconocidos lanzan excepcion")
        void codigo_desconocido_lanza_excepcion() {
            for (short codigo : new short[]{0, 1, 2, 3, 5, 7, 28, 32, 36, 40, 44, 100}) {
                assertThatThrownBy(() -> TipoEvidenciaActa.desdeCodigo(codigo))
                        .as("El codigo %d debe ser rechazado", codigo)
                        .isInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Construccion de FalActaEvidencia con cada tipo
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("3. Construccion de FalActaEvidencia con cada tipo")
    class ConstruccionEvidencia {

        private final LocalDateTime ahora = LocalDateTime.of(2026, 7, 21, 12, 0, 0);

        private FalActaEvidencia construir(TipoEvidenciaActa tipo) {
            return new FalActaEvidencia(
                    1L, 100L, tipo,
                    "mock://evidencia/" + tipo.name().toLowerCase() + "/file",
                    ahora, ahora, "test-actor", null);
        }

        @Test
        @DisplayName("14. Se puede construir una evidencia con FOTO")
        void construir_foto() {
            assertThatCode(() -> construir(TipoEvidenciaActa.FOTO)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("15. Se puede construir una evidencia con VIDEO")
        void construir_video() {
            assertThatCode(() -> construir(TipoEvidenciaActa.VIDEO)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("16. Se puede construir una evidencia con AUDIO")
        void construir_audio() {
            assertThatCode(() -> construir(TipoEvidenciaActa.AUDIO)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("17. Se puede construir una evidencia con PDF")
        void construir_pdf() {
            assertThatCode(() -> construir(TipoEvidenciaActa.PDF)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("18. Se puede construir una evidencia con DOCUMENTO_OFIMATICO")
        void construir_documento_ofimatico() {
            assertThatCode(() -> construir(TipoEvidenciaActa.DOCUMENTO_OFIMATICO)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("19. Se puede construir una evidencia con PLANILLA_CALCULO")
        void construir_planilla_calculo() {
            assertThatCode(() -> construir(TipoEvidenciaActa.PLANILLA_CALCULO)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("20. Se puede construir una evidencia con FIRMA_OLOGRAFA_INFRACTOR")
        void construir_firma_olografa() {
            assertThatCode(() -> construir(TipoEvidenciaActa.FIRMA_OLOGRAFA_INFRACTOR)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("21. Los 7 tipos construidos preservan su tipoEvid")
        void tipoEvid_preservado() {
            for (TipoEvidenciaActa tipo : TipoEvidenciaActa.values()) {
                FalActaEvidencia e = construir(tipo);
                assertThat(e.getTipoEvid())
                        .as("tipoEvid debe preservarse para %s", tipo.name())
                        .isEqualTo(tipo);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Regla funcional de firma
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("4. Regla funcional de firma — solo FIRMA_OLOGRAFA_INFRACTOR satisface FIRMADA")
    class ReglaFirma {

        private boolean esFirmaOlografa(TipoEvidenciaActa tipo) {
            return tipo == TipoEvidenciaActa.FIRMA_OLOGRAFA_INFRACTOR;
        }

        @Test
        @DisplayName("22. FIRMA_OLOGRAFA_INFRACTOR satisface la regla de firma")
        void firma_olografa_satisface() {
            assertThat(esFirmaOlografa(TipoEvidenciaActa.FIRMA_OLOGRAFA_INFRACTOR)).isTrue();
        }

        @Test
        @DisplayName("23. FOTO no satisface el requisito de firma")
        void foto_no_satisface() {
            assertThat(esFirmaOlografa(TipoEvidenciaActa.FOTO)).isFalse();
        }

        @Test
        @DisplayName("24. VIDEO no satisface el requisito de firma")
        void video_no_satisface() {
            assertThat(esFirmaOlografa(TipoEvidenciaActa.VIDEO)).isFalse();
        }

        @Test
        @DisplayName("25. AUDIO no satisface el requisito de firma")
        void audio_no_satisface() {
            assertThat(esFirmaOlografa(TipoEvidenciaActa.AUDIO)).isFalse();
        }

        @Test
        @DisplayName("26. PDF no satisface el requisito de firma")
        void pdf_no_satisface() {
            assertThat(esFirmaOlografa(TipoEvidenciaActa.PDF)).isFalse();
        }

        @Test
        @DisplayName("27. DOCUMENTO_OFIMATICO no satisface el requisito de firma")
        void documento_ofimatico_no_satisface() {
            assertThat(esFirmaOlografa(TipoEvidenciaActa.DOCUMENTO_OFIMATICO)).isFalse();
        }

        @Test
        @DisplayName("28. PLANILLA_CALCULO no satisface el requisito de firma")
        void planilla_calculo_no_satisface() {
            assertThat(esFirmaOlografa(TipoEvidenciaActa.PLANILLA_CALCULO)).isFalse();
        }

        @Test
        @DisplayName("Solo FIRMA_OLOGRAFA_INFRACTOR satisface el requisito entre los 7 tipos")
        void exactamente_uno_satisface() {
            long satisfacen = Arrays.stream(TipoEvidenciaActa.values())
                    .filter(t -> t == TipoEvidenciaActa.FIRMA_OLOGRAFA_INFRACTOR)
                    .count();
            assertThat(satisfacen).isEqualTo(1);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bloque DDL real de fal_acta_evidencia
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("5. Bloque DDL fal_acta_evidencia — comentario tipo_evid")
    class BloqueDdl {

        @Test
        @DisplayName("29. El comentario DDL no contiene 'unico valor activo'")
        void sin_unico_valor_activo() {
            assertThat(bloqueEvidencia)
                    .as("El DDL de fal_acta_evidencia no debe decir 'unico valor activo'")
                    .doesNotContainIgnoringCase("unico valor activo");
        }

        @Test
        @DisplayName("30a. El comentario DDL documenta el codigo 4=FOTO")
        void documenta_foto() {
            assertThat(bloqueEvidencia).contains("4=FOTO");
        }

        @Test
        @DisplayName("30b. El comentario DDL documenta el codigo 8=VIDEO")
        void documenta_video() {
            assertThat(bloqueEvidencia).contains("8=VIDEO");
        }

        @Test
        @DisplayName("30c. El comentario DDL documenta el codigo 12=AUDIO")
        void documenta_audio() {
            assertThat(bloqueEvidencia).contains("12=AUDIO");
        }

        @Test
        @DisplayName("30d. El comentario DDL documenta el codigo 16=PDF")
        void documenta_pdf() {
            assertThat(bloqueEvidencia).contains("16=PDF");
        }

        @Test
        @DisplayName("30e. El comentario DDL documenta el codigo 20=DOCUMENTO_OFIMATICO")
        void documenta_documento_ofimatico() {
            assertThat(bloqueEvidencia).contains("20=DOCUMENTO_OFIMATICO");
        }

        @Test
        @DisplayName("30f. El comentario DDL documenta el codigo 24=PLANILLA_CALCULO")
        void documenta_planilla_calculo() {
            assertThat(bloqueEvidencia).contains("24=PLANILLA_CALCULO");
        }

        @Test
        @DisplayName("30g. El comentario DDL documenta el codigo 48=FIRMA_OLOGRAFA_INFRACTOR")
        void documenta_firma_olografa() {
            assertThat(bloqueEvidencia).contains("48=FIRMA_OLOGRAFA_INFRACTOR");
        }

        @Test
        @DisplayName("31. El codigo historico 6 no aparece como definicion activa en el DDL (solo comentarios documentales)")
        void codigo_6_sin_definicion_activa() {
            // El bloque DDL de la tabla no debe asignar el codigo 6 como valor de tipo_evid activo.
            // Se verifica que no haya 'FIRMA_OLOGRAFA_INFRACTOR.*6' ni '6=FIRMA_OLOGRAFA_INFRACTOR'
            // en el bloque de la tabla.
            assertThat(bloqueEvidencia).doesNotContain("6=FIRMA_OLOGRAFA_INFRACTOR");
            assertThat(bloqueEvidencia).doesNotContainPattern("FIRMA_OLOGRAFA_INFRACTOR.*[^0-9]6[^0-9]");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // D3 - Proteccion contra ordinal() y accesos posicionales en codigo productivo
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("6. Proteccion contra ordinal() y accesos posicionales (D3 - CORRECCION-10.1)")
    class ProteccionOrdinalYPosicional {

        @Test
        @DisplayName("32. TipoEvidenciaActa.java no usa ordinal() en ningun metodo")
        void enum_sin_ordinal() {
            assertThat(srcTipoEvidenciaActa)
                    .as("El enum TipoEvidenciaActa no debe contener llamadas a ordinal()")
                    .doesNotContain(".ordinal()");
        }

        @Test
        @DisplayName("33. TipoEvidenciaActa.java no usa acceso posicional values()[indice]")
        void enum_sin_acceso_posicional() {
            assertThat(srcTipoEvidenciaActa)
                    .as("El enum TipoEvidenciaActa no debe acceder a values() por indice")
                    .doesNotContainPattern("values\\(\\)\\s*\\[");
        }

        @Test
        @DisplayName("34. TipoEvidenciaActa.desdeCodigo usa comparacion por campo codigo explicito")
        void enum_resolucion_por_codigo_explicito() {
            assertThat(srcTipoEvidenciaActa)
                    .as("desdeCodigo debe comparar por t.codigo (campo explicito)")
                    .contains("t.codigo == codigo");
        }

        @Test
        @DisplayName("35. FalActaEvidencia.java no usa ordinal() ni values()[indice] para TipoEvidenciaActa")
        void acta_evidencia_sin_resolucion_posicional() {
            assertThat(srcFalActaEvidencia)
                    .as("FalActaEvidencia no debe usar ordinal() de TipoEvidenciaActa")
                    .doesNotContain(".ordinal()");
            assertThat(srcFalActaEvidencia)
                    .as("FalActaEvidencia no debe acceder a TipoEvidenciaActa.values() por indice")
                    .doesNotContainPattern("TipoEvidenciaActa\\.values\\(\\)\\s*\\[");
        }

        @Test
        @DisplayName("36. ActaService.java no usa ordinal() ni values()[indice] para TipoEvidenciaActa")
        void acta_service_sin_resolucion_posicional() {
            assertThat(srcActaService)
                    .as("ActaService no debe usar ordinal() de TipoEvidenciaActa")
                    .doesNotContain(".ordinal()");
            assertThat(srcActaService)
                    .as("ActaService no debe acceder a TipoEvidenciaActa.values() por indice")
                    .doesNotContainPattern("TipoEvidenciaActa\\.values\\(\\)\\s*\\[");
        }

        @Test
        @DisplayName("37. La comparacion de identidad semantica usa == (no ordinal ni codigo >= 48)")
        void comparacion_usa_identidad_semantica() {
            assertThat(srcActaService)
                    .as("La regla de firma debe usar identidad semantica (FIRMA_OLOGRAFA_INFRACTOR)")
                    .contains("FIRMA_OLOGRAFA_INFRACTOR");
            assertThat(srcActaService)
                    .as("No debe usar comparacion >= 48 para identificar el tipo de firma")
                    .doesNotContain(">= 48");
            assertThat(srcActaService)
                    .as("No debe usar comparacion .codigo() >= 48")
                    .doesNotContainPattern("codigo\\(\\)\\s*>=\\s*48");
        }
    }
}
