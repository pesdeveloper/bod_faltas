package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.domain.enums.AccionPendiente;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoFirma;
import ar.gob.malvinas.faltas.core.domain.enums.MomentoNumeracionDocu;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFirmaReq;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Guardrails de enum: nombres prohibidos, nombres obligatorios y restriccion de valores.
 *
 * No duplica tests de FlujoCompletoTest ni ApelacionActaTest ni FirmezaCondenaTest.
 * Consolida los guardrails especificos del micro-slice de cierre semantico y
 * del micro-slice pre-Slice 6 (gestion externa).
 *
 * Prohibiciones documentadas:
 * - PAGCON no existe (pago condena: PCOINF / PCOCNF / PCOOBS)
 * - D3_DOCUMENTAL no es bloque (ya en FlujoCompletoTest, aqui se agrega DOCUMENTAL)
 * - Eventos genericos y aliases legacy no existen
 * - GESTIONAR_CONDENA_FIRME no existe en AccionPendiente (eliminado, usar GESTIONAR_PAGO_CONDENA)
 * - DRVEXT no existe en TipoEventoActa (prohibido; eventos correctos: EXTDER/EXTRET/PAGAPR)
 *
 * Valores obligatorios:
 * - PCOINF, PCOCNF, PCOOBS existen como eventos de pago condena (Slice 5)
 * - BloqueActual solo contiene CAPT, ENRI, NOTI, ANAL, GEXT, ARCH, CERR
 * - GESTIONAR_PAGO_CONDENA existe en AccionPendiente
 * - EXTDER, EXTRET, PAGAPR existen en TipoEventoActa (reservados para Slice 6)
 */
@DisplayName("Guardrails de enums: nombres prohibidos y obligatorios")
class EnumGuardrailTest {

    // =========================================================================
    // Pago condena: PAGCON prohibido, PCOINF/PCOCNF/PCOOBS obligatorios
    // =========================================================================

    @Nested
    @DisplayName("Pago de condena: eventos correctos para Slice 5")
    class PagoCondenaEventosTests {

        @Test
        @DisplayName("PAGCON no existe como evento productivo")
        void pagcon_no_existe() {
            assertThatThrownBy(() -> TipoEventoActa.deCodigo("PAGCON"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("PAGCON");
        }

        @Test
        @DisplayName("PCOINF existe y resuelve correctamente (pago condena informado)")
        void pcoinf_existe() {
            TipoEventoActa pcoinf = TipoEventoActa.deCodigo("PCOINF");
            assertThat(pcoinf).isEqualTo(TipoEventoActa.PCOINF);
            assertThat(pcoinf.codigo()).isEqualTo("PCOINF");
        }

        @Test
        @DisplayName("PCOCNF existe y resuelve correctamente (pago condena confirmado)")
        void pcocnf_existe() {
            TipoEventoActa pcocnf = TipoEventoActa.deCodigo("PCOCNF");
            assertThat(pcocnf).isEqualTo(TipoEventoActa.PCOCNF);
            assertThat(pcocnf.codigo()).isEqualTo("PCOCNF");
        }

        @Test
        @DisplayName("PCOOBS existe y resuelve correctamente (pago condena observado)")
        void pcoobs_existe() {
            TipoEventoActa pcoobs = TipoEventoActa.deCodigo("PCOOBS");
            assertThat(pcoobs).isEqualTo(TipoEventoActa.PCOOBS);
            assertThat(pcoobs.codigo()).isEqualTo("PCOOBS");
        }
    }

    // =========================================================================
    // BloqueActual: valores exactos, ninguno legacy
    // =========================================================================

    @Nested
    @DisplayName("BloqueActual: valores exactos y prohibiciones adicionales")
    class BloqueActualGuardrailTests {

        @Test
        @DisplayName("BloqueActual contiene exactamente los 7 bloques productivos")
        void bloque_actual_contiene_exactamente_7_bloques_productivos() {
            Set<String> codigos = Arrays.stream(BloqueActual.values())
                    .map(BloqueActual::codigo)
                    .collect(Collectors.toSet());
            assertThat(codigos).containsExactlyInAnyOrder(
                    "CAPT", "ENRI", "NOTI", "ANAL", "GEXT", "ARCH", "CERR");
        }

        @Test
        @DisplayName("BloqueActual.deCodigo rechaza DOCUMENTAL")
        void bloque_documental_rechazado() {
            assertThatThrownBy(() -> BloqueActual.deCodigo("DOCUMENTAL"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("BloqueActual.deCodigo rechaza D2_ENRIQUECIMIENTO (alias legacy prohibido)")
        void bloque_d2_enriquecimiento_rechazado() {
            assertThatThrownBy(() -> BloqueActual.deCodigo("D2_ENRIQUECIMIENTO"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("BloqueActual.deCodigo rechaza D4_NOTIFICACION (alias legacy prohibido)")
        void bloque_d4_notificacion_rechazado() {
            assertThatThrownBy(() -> BloqueActual.deCodigo("D4_NOTIFICACION"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("BloqueActual.deCodigo rechaza D5_ANALISIS (alias legacy prohibido)")
        void bloque_d5_analisis_rechazado() {
            assertThatThrownBy(() -> BloqueActual.deCodigo("D5_ANALISIS"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // =========================================================================
    // Eventos prohibidos: aliases genericos y legacy
    // =========================================================================

    @Nested
    @DisplayName("TipoEventoActa: eventos prohibidos")
    class EventosProhibidosTests {

        @Test
        @DisplayName("PAGVOL no existe (pago voluntario tiene 7 eventos especificos)")
        void pagvol_no_existe() {
            assertThatThrownBy(() -> TipoEventoActa.deCodigo("PAGVOL"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("PAGVOL");
        }

        @Test
        @DisplayName("ACTCER no existe (el cierre usa CIERRA)")
        void actcer_no_existe() {
            assertThatThrownBy(() -> TipoEventoActa.deCodigo("ACTCER"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ACTCER");
        }

        @Test
        @DisplayName("FALLO no existe como evento generico (usar FALABS o FALCON)")
        void fallo_generico_no_existe() {
            assertThatThrownBy(() -> TipoEventoActa.deCodigo("FALLO"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("FALLO");
        }

        @Test
        @DisplayName("APELACION no existe como evento generico (usar APEPRE/APERAZ/APEABS)")
        void apelacion_generica_no_existe() {
            assertThatThrownBy(() -> TipoEventoActa.deCodigo("APELACION"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("APELACION");
        }

        @Test
        @DisplayName("PASE_BANDEJA no existe (es proyeccion operativa, no evento de dominio)")
        void pase_bandeja_no_existe() {
            assertThatThrownBy(() -> TipoEventoActa.deCodigo("PASE_BANDEJA"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("PASE_DEMO no existe (era evento demo descartado)")
        void pase_demo_no_existe() {
            assertThatThrownBy(() -> TipoEventoActa.deCodigo("PASE_DEMO"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("ACTCER no aparece en ningun valor del enum TipoEventoActa")
        void actcer_no_aparece_en_enum() {
            for (TipoEventoActa t : TipoEventoActa.values()) {
                assertThat(t.codigo()).isNotEqualTo("ACTCER");
                assertThat(t.name()).isNotEqualTo("ACTCER");
            }
        }

        @Test
        @DisplayName("PAGCON no aparece en ningun valor del enum TipoEventoActa")
        void pagcon_no_aparece_en_enum() {
            for (TipoEventoActa t : TipoEventoActa.values()) {
                assertThat(t.codigo()).isNotEqualTo("PAGCON");
                assertThat(t.name()).isNotEqualTo("PAGCON");
            }
        }

        @Test
        @DisplayName("DRVEXT no existe como evento productivo (prohibido; usar EXTDER/EXTRET/PAGAPR)")
        void drvext_no_existe() {
            assertThatThrownBy(() -> TipoEventoActa.deCodigo("DRVEXT"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("DRVEXT");
        }

        @Test
        @DisplayName("DRVEXT no aparece en ningun valor del enum TipoEventoActa")
        void drvext_no_aparece_en_enum() {
            for (TipoEventoActa t : TipoEventoActa.values()) {
                assertThat(t.codigo()).isNotEqualTo("DRVEXT");
                assertThat(t.name()).isNotEqualTo("DRVEXT");
            }
        }
    }

    // =========================================================================
    // Eventos obligatorios vigentes
    // =========================================================================

    @Nested
    @DisplayName("TipoEventoActa: eventos obligatorios vigentes")
    class EventosObligatoriosTests {

        @Test
        @DisplayName("CIERRA existe y es el evento de cierre definitivo")
        void cierra_existe() {
            TipoEventoActa cierra = TipoEventoActa.deCodigo("CIERRA");
            assertThat(cierra).isEqualTo(TipoEventoActa.CIERRA);
            assertThat(cierra.codigo()).isEqualTo("CIERRA");
        }

        @Test
        @DisplayName("APEPRE existe como evento de apelacion presentada")
        void apepre_existe() {
            assertThat(TipoEventoActa.deCodigo("APEPRE")).isEqualTo(TipoEventoActa.APEPRE);
        }

        @Test
        @DisplayName("APERAZ existe como evento de apelacion rechazada")
        void aperaz_existe() {
            assertThat(TipoEventoActa.deCodigo("APERAZ")).isEqualTo(TipoEventoActa.APERAZ);
        }

        @Test
        @DisplayName("APEABS existe como evento de apelacion aceptada-absolucion")
        void apeabs_existe() {
            assertThat(TipoEventoActa.deCodigo("APEABS")).isEqualTo(TipoEventoActa.APEABS);
        }
    }

    // =========================================================================
    // Gestion externa: guardrails pre-Slice 6
    // EXTDER / EXTRET / PAGAPR son los eventos correctos (reservados para Slice 6)
    // DRVEXT esta prohibido
    // =========================================================================

    @Nested
    @DisplayName("Gestion externa: guardrails pre-Slice 6")
    class GestionExternaGuardrailTests {

        @Test
        @DisplayName("EXTDER existe y resuelve correctamente (derivar a gestion externa)")
        void extder_existe() {
            TipoEventoActa extder = TipoEventoActa.deCodigo("EXTDER");
            assertThat(extder).isEqualTo(TipoEventoActa.EXTDER);
            assertThat(extder.codigo()).isEqualTo("EXTDER");
        }

        @Test
        @DisplayName("EXTRET existe y resuelve correctamente (reingresar desde gestion externa)")
        void extret_existe() {
            TipoEventoActa extret = TipoEventoActa.deCodigo("EXTRET");
            assertThat(extret).isEqualTo(TipoEventoActa.EXTRET);
            assertThat(extret.codigo()).isEqualTo("EXTRET");
        }

        @Test
        @DisplayName("PAGAPR existe y resuelve correctamente (pago externo por apremio)")
        void pagapr_existe() {
            TipoEventoActa pagapr = TipoEventoActa.deCodigo("PAGAPR");
            assertThat(pagapr).isEqualTo(TipoEventoActa.PAGAPR);
            assertThat(pagapr.codigo()).isEqualTo("PAGAPR");
        }
    }

    // =========================================================================
    // AccionPendiente: GESTIONAR_CONDENA_FIRME eliminado, GESTIONAR_PAGO_CONDENA vigente
    // =========================================================================

    @Nested
    @DisplayName("AccionPendiente: GESTIONAR_CONDENA_FIRME eliminado")
    class AccionPendienteGuardrailTests {

        @Test
        @DisplayName("GESTIONAR_CONDENA_FIRME no existe en AccionPendiente")
        void gestionar_condena_firme_no_existe() {
            for (AccionPendiente ap : AccionPendiente.values()) {
                assertThat(ap.name()).isNotEqualTo("GESTIONAR_CONDENA_FIRME");
            }
        }

        @Test
        @DisplayName("GESTIONAR_PAGO_CONDENA existe en AccionPendiente (valor correcto para condena firme)")
        void gestionar_pago_condena_existe() {
            AccionPendiente gpc = AccionPendiente.GESTIONAR_PAGO_CONDENA;
            assertThat(gpc.name()).isEqualTo("GESTIONAR_PAGO_CONDENA");
        }
    }

    // =========================================================================
    // Catalogos documentales 8C-1
    // =========================================================================

    @Nested
    @DisplayName("Catalogos documentales 8C-1: codigos y valores")
    class CatalogosDocumentalesTests {

        @Test
        @DisplayName("TipoDocu.desdeCodigo(1) = ACTA_INFRACCION")
        void tipo_docu_codigo_1_es_acta_infraccion() {
            assertThat(TipoDocu.desdeCodigo((short) 1)).isEqualTo(TipoDocu.ACTA_INFRACCION);
        }

        @Test
        @DisplayName("TipoDocu.desdeCodigo(12) = OTRO")
        void tipo_docu_codigo_12_es_otro() {
            assertThat(TipoDocu.desdeCodigo((short) 12)).isEqualTo(TipoDocu.OTRO);
        }

        @Test
        @DisplayName("TipoDocu tiene exactamente 12 valores")
        void tipo_docu_tiene_12_valores() {
            assertThat(TipoDocu.values()).hasSize(12);
        }

        @Test
        @DisplayName("TipoDocu codigo invalido lanza IllegalArgumentException")
        void tipo_docu_codigo_invalido_falla() {
            assertThatThrownBy(() -> TipoDocu.desdeCodigo((short) 99))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("EstadoDocu.desdeCodigo(4) = FIRMADO")
        void estado_docu_codigo_4_es_firmado() {
            assertThat(EstadoDocu.desdeCodigo((short) 4)).isEqualTo(EstadoDocu.FIRMADO);
        }

        @Test
        @DisplayName("EstadoDocu tiene exactamente 7 valores")
        void estado_docu_tiene_7_valores() {
            assertThat(EstadoDocu.values()).hasSize(7);
        }

        @Test
        @DisplayName("EstadoDocu contiene ANULADO y REEMPLAZADO")
        void estado_docu_contiene_anulado_y_reemplazado() {
            assertThat(EstadoDocu.ANULADO).isNotNull();
            assertThat(EstadoDocu.REEMPLAZADO).isNotNull();
        }

        @Test
        @DisplayName("EstadoFirma tiene exactamente 6 valores")
        void estado_firma_tiene_6_valores() {
            assertThat(EstadoFirma.values()).hasSize(6);
        }

        @Test
        @DisplayName("EstadoFirma.desdeCodigo(3) = FIRMADA")
        void estado_firma_codigo_3_es_firmada() {
            assertThat(EstadoFirma.desdeCodigo((short) 3)).isEqualTo(EstadoFirma.FIRMADA);
        }

        @Test
        @DisplayName("TipoFirmaReq.desdeCodigo(5) = FIRMA_MULTIPLE")
        void tipo_firma_req_codigo_5_es_firma_multiple() {
            assertThat(TipoFirmaReq.desdeCodigo((short) 5)).isEqualTo(TipoFirmaReq.FIRMA_MULTIPLE);
        }

        @Test
        @DisplayName("TipoFirmaReq.desdeCodigo(0) = NO_REQUIERE")
        void tipo_firma_req_codigo_0_es_no_requiere() {
            assertThat(TipoFirmaReq.desdeCodigo((short) 0)).isEqualTo(TipoFirmaReq.NO_REQUIERE);
        }

        @Test
        @DisplayName("FIRMA_MIXTA no existe en TipoFirmaReq")
        void firma_mixta_no_existe_en_tipo_firma_req() {
            for (TipoFirmaReq t : TipoFirmaReq.values()) {
                assertThat(t.name()).isNotEqualTo("FIRMA_MIXTA");
            }
        }

        @Test
        @DisplayName("AccionDocumental.desdeCodigo(4) = EMITIR_FALLO")
        void accion_documental_codigo_4_es_emitir_fallo() {
            assertThat(AccionDocumental.desdeCodigo((short) 4)).isEqualTo(AccionDocumental.EMITIR_FALLO);
        }

        @Test
        @DisplayName("AccionDocumental tiene exactamente 11 valores")
        void accion_documental_tiene_11_valores() {
            assertThat(AccionDocumental.values()).hasSize(11);
        }

        @Test
        @DisplayName("MomentoNumeracionDocu.desdeCodigo(3) = AL_ENVIAR_A_FIRMA")
        void momento_numeracion_docu_codigo_3_es_al_enviar_a_firma() {
            assertThat(MomentoNumeracionDocu.desdeCodigo((short) 3))
                    .isEqualTo(MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA);
        }

        @Test
        @DisplayName("MomentoNumeracionDocu tiene exactamente 5 valores")
        void momento_numeracion_docu_tiene_5_valores() {
            assertThat(MomentoNumeracionDocu.values()).hasSize(5);
        }

        @Test
        @DisplayName("ResultadoFirmaInfractor.desdeCodigo(2) = SE_NIEGA_A_FIRMAR")
        void resultado_firma_infractor_codigo_2_es_se_niega_a_firmar() {
            assertThat(ResultadoFirmaInfractor.desdeCodigo((short) 2))
                    .isEqualTo(ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR);
        }

        @Test
        @DisplayName("ResultadoFirmaInfractor tiene exactamente 5 valores")
        void resultado_firma_infractor_tiene_5_valores() {
            assertThat(ResultadoFirmaInfractor.values()).hasSize(5);
        }

        @Test
        @DisplayName("ResultadoFirmaInfractor codigo invalido lanza IllegalArgumentException")
        void resultado_firma_infractor_codigo_invalido_falla() {
            assertThatThrownBy(() -> ResultadoFirmaInfractor.desdeCodigo((short) 99))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
