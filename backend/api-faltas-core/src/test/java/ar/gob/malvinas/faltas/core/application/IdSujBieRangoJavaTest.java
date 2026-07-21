package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.domain.enums.OrigenNomenclatura;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPersona;
import ar.gob.malvinas.faltas.core.domain.model.FalActaContravencion;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.model.FalPersona;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Paridad Java/InMemory–DDL para rangos de id_suj e id_bie.
 *
 * <p>Verifica que los setters del dominio rechacen valores fuera del rango fisico
 * definido en el DDL (TINYINT UNSIGNED 1-255 e MEDIUMINT UNSIGNED 1-9999999),
 * cerrando el hallazgo D2 de FULL-R1.2-CORRECCION-10.
 *
 * HUMAN_DECISION_CLOSED - FULL-R1.2-CORRECCION-10.1
 */
@DisplayName("Paridad Java/DDL rangos id_suj/id_bie (D2 - CORRECCION-10.1)")
class IdSujBieRangoJavaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 7, 21, 12, 0);

    private FalPersona persona() {
        return new FalPersona(1L, TipoPersona.FISICA, AHORA, "test");
    }

    private FalActaContravencion contravencion() {
        return new FalActaContravencion(1L, OrigenNomenclatura.CATASTRO, false, AHORA, "test");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FalPersona - id_suj
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("FalPersona.setIdSuj - rango 1-255")
    class FalPersonaIdSuj {

        @Test
        @DisplayName("1. Acepta null (sin vinculo Ingresos)")
        void acepta_null() {
            assertThatCode(() -> persona().setIdSuj(null)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("2. Acepta frontera inferior: idSuj=1")
        void acepta_uno() {
            FalPersona p = persona();
            p.setIdSuj(1);
            assertThat(p.getIdSuj()).isEqualTo(1);
        }

        @Test
        @DisplayName("3. Acepta frontera superior: idSuj=255")
        void acepta_doscientos_cincuenta_y_cinco() {
            FalPersona p = persona();
            p.setIdSuj(255);
            assertThat(p.getIdSuj()).isEqualTo(255);
        }

        @Test
        @DisplayName("4. Rechaza frontera inferior invalida: idSuj=0")
        void rechaza_cero() {
            assertThatThrownBy(() -> persona().setIdSuj(0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("idSuj");
        }

        @Test
        @DisplayName("5. Rechaza frontera superior invalida: idSuj=256")
        void rechaza_doscientos_cincuenta_y_seis() {
            assertThatThrownBy(() -> persona().setIdSuj(256))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("idSuj");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FalPersona - id_bie
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("FalPersona.setIdBie - rango 1-9999999 / requiere idSuj")
    class FalPersonaIdBie {

        @Test
        @DisplayName("6. Acepta null (sin bien informado)")
        void acepta_null() {
            FalPersona p = persona();
            p.setIdSuj(20);
            assertThatCode(() -> p.setIdBie(null)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("7. Acepta frontera inferior: idBie=1 con idSuj previo")
        void acepta_uno() {
            FalPersona p = persona();
            p.setIdSuj(20);
            p.setIdBie(1);
            assertThat(p.getIdBie()).isEqualTo(1);
        }

        @Test
        @DisplayName("8. Acepta frontera superior: idBie=9999999 con idSuj previo")
        void acepta_nueve_millones_novecientos_noventa_y_nueve_mil_novecientos_noventa_y_nueve() {
            FalPersona p = persona();
            p.setIdSuj(20);
            p.setIdBie(9_999_999);
            assertThat(p.getIdBie()).isEqualTo(9_999_999);
        }

        @Test
        @DisplayName("9. Rechaza frontera inferior invalida: idBie=0")
        void rechaza_cero() {
            FalPersona p = persona();
            p.setIdSuj(20);
            assertThatThrownBy(() -> p.setIdBie(0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("idBie");
        }

        @Test
        @DisplayName("10. Rechaza frontera superior invalida: idBie=10000000")
        void rechaza_diez_millones() {
            FalPersona p = persona();
            p.setIdSuj(20);
            assertThatThrownBy(() -> p.setIdBie(10_000_000))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("idBie");
        }

        @Test
        @DisplayName("11. Rechaza idBie informado sin idSuj")
        void rechaza_sin_idSuj() {
            FalPersona p = persona();
            assertThatThrownBy(() -> p.setIdBie(100))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("idSuj");
        }

        @Test
        @DisplayName("12. La copia preserva idSuj e idBie validos")
        void copia_preserva_valores() {
            FalPersona p = persona();
            p.setIdSuj(1);
            p.setIdBie(9_999_999);
            FalPersona c = p.copia();
            assertThat(c.getIdSuj()).isEqualTo(1);
            assertThat(c.getIdBie()).isEqualTo(9_999_999);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FalActaContravencion - inmueble (id_suj_i / id_bie_i)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("FalActaContravencion.setSujetoInmueble - rangos id_suj_i/id_bie_i")
    class FalActaContravencionInmueble {

        @Test
        @DisplayName("13. Acepta idSujI=1, idBieI=1 (fronteras inferiores validas)")
        void acepta_fronteras_inferiores() {
            FalActaContravencion ctv = contravencion();
            ctv.setSujetoInmueble(1, 1);
            assertThat(ctv.getIdSujI()).isEqualTo(1);
            assertThat(ctv.getIdBieI()).isEqualTo(1);
        }

        @Test
        @DisplayName("14. Acepta idSujI=255, idBieI=9999999 (fronteras superiores validas)")
        void acepta_fronteras_superiores() {
            FalActaContravencion ctv = contravencion();
            ctv.setSujetoInmueble(255, 9_999_999);
            assertThat(ctv.getIdSujI()).isEqualTo(255);
            assertThat(ctv.getIdBieI()).isEqualTo(9_999_999);
        }

        @Test
        @DisplayName("15. Rechaza idSujI=0")
        void rechaza_idSujI_cero() {
            assertThatThrownBy(() -> contravencion().setSujetoInmueble(0, 100))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("idSujI");
        }

        @Test
        @DisplayName("16. Rechaza idSujI=256")
        void rechaza_idSujI_doscientos_cincuenta_y_seis() {
            assertThatThrownBy(() -> contravencion().setSujetoInmueble(256, 100))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("idSujI");
        }

        @Test
        @DisplayName("17. Rechaza idBieI=0")
        void rechaza_idBieI_cero() {
            assertThatThrownBy(() -> contravencion().setSujetoInmueble(1, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("idBieI");
        }

        @Test
        @DisplayName("18. Rechaza idBieI=10000000")
        void rechaza_idBieI_diez_millones() {
            assertThatThrownBy(() -> contravencion().setSujetoInmueble(1, 10_000_000))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("idBieI");
        }

        @Test
        @DisplayName("19. La copia preserva idSujI e idBieI validos")
        void copia_preserva_valores() {
            FalActaContravencion ctv = contravencion();
            ctv.setSujetoInmueble(1, 9_999_999);
            FalActaContravencion c = ctv.copia();
            assertThat(c.getIdSujI()).isEqualTo(1);
            assertThat(c.getIdBieI()).isEqualTo(9_999_999);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FalActaContravencion - comercio (id_suj_c / id_bie_c)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("FalActaContravencion.setSujetoComercio - rangos id_suj_c/id_bie_c")
    class FalActaContravencionComercio {

        @Test
        @DisplayName("20. Acepta idSujC=2, idBieC=1 (frontera inferior valida)")
        void acepta_fronteras_inferiores() {
            FalActaContravencion ctv = contravencion();
            ctv.setSujetoComercio(2, 1);
            assertThat(ctv.getIdSujC()).isEqualTo(2);
            assertThat(ctv.getIdBieC()).isEqualTo(1);
        }

        @Test
        @DisplayName("21. Acepta idSujC=255, idBieC=9999999 (fronteras superiores validas)")
        void acepta_fronteras_superiores() {
            FalActaContravencion ctv = contravencion();
            ctv.setSujetoComercio(255, 9_999_999);
            assertThat(ctv.getIdSujC()).isEqualTo(255);
            assertThat(ctv.getIdBieC()).isEqualTo(9_999_999);
        }

        @Test
        @DisplayName("22. Rechaza idSujC=0")
        void rechaza_idSujC_cero() {
            assertThatThrownBy(() -> contravencion().setSujetoComercio(0, 100))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("idSujC");
        }

        @Test
        @DisplayName("23. Rechaza idSujC=256")
        void rechaza_idSujC_doscientos_cincuenta_y_seis() {
            assertThatThrownBy(() -> contravencion().setSujetoComercio(256, 100))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("idSujC");
        }

        @Test
        @DisplayName("24. Rechaza idBieC=0")
        void rechaza_idBieC_cero() {
            assertThatThrownBy(() -> contravencion().setSujetoComercio(2, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("idBieC");
        }

        @Test
        @DisplayName("25. Rechaza idBieC=10000000")
        void rechaza_idBieC_diez_millones() {
            assertThatThrownBy(() -> contravencion().setSujetoComercio(2, 10_000_000))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("idBieC");
        }

        @Test
        @DisplayName("26. La copia preserva idSujC e idBieC validos")
        void copia_preserva_valores() {
            FalActaContravencion ctv = contravencion();
            ctv.setSujetoComercio(2, 9_999_999);
            FalActaContravencion c = ctv.copia();
            assertThat(c.getIdSujC()).isEqualTo(2);
            assertThat(c.getIdBieC()).isEqualTo(9_999_999);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FalActaSnapshot - id_bie_i / id_bie_c
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("FalActaSnapshot.setIdBieI / setIdBieC - rango 1-9999999")
    class FalActaSnapshotRango {

        @Test
        @DisplayName("27. Acepta idBieI=1 (frontera inferior valida)")
        void acepta_idBieI_uno() {
            FalActaSnapshot snap = new FalActaSnapshot(1L);
            snap.setIdBieI(1);
            assertThat(snap.getIdBieI()).isEqualTo(1);
        }

        @Test
        @DisplayName("28. Acepta idBieI=9999999 (frontera superior valida)")
        void acepta_idBieI_maximo() {
            FalActaSnapshot snap = new FalActaSnapshot(1L);
            snap.setIdBieI(9_999_999);
            assertThat(snap.getIdBieI()).isEqualTo(9_999_999);
        }

        @Test
        @DisplayName("29. Rechaza idBieI=0")
        void rechaza_idBieI_cero() {
            assertThatThrownBy(() -> new FalActaSnapshot(1L).setIdBieI(0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("idBieI");
        }

        @Test
        @DisplayName("30. Rechaza idBieI=10000000")
        void rechaza_idBieI_diez_millones() {
            assertThatThrownBy(() -> new FalActaSnapshot(1L).setIdBieI(10_000_000))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("idBieI");
        }

        @Test
        @DisplayName("31. Acepta idBieC=1 (frontera inferior valida)")
        void acepta_idBieC_uno() {
            FalActaSnapshot snap = new FalActaSnapshot(1L);
            snap.setIdBieC(1);
            assertThat(snap.getIdBieC()).isEqualTo(1);
        }

        @Test
        @DisplayName("32. Acepta idBieC=9999999 (frontera superior valida)")
        void acepta_idBieC_maximo() {
            FalActaSnapshot snap = new FalActaSnapshot(1L);
            snap.setIdBieC(9_999_999);
            assertThat(snap.getIdBieC()).isEqualTo(9_999_999);
        }

        @Test
        @DisplayName("33. Rechaza idBieC=0")
        void rechaza_idBieC_cero() {
            assertThatThrownBy(() -> new FalActaSnapshot(1L).setIdBieC(0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("idBieC");
        }

        @Test
        @DisplayName("34. Rechaza idBieC=10000000")
        void rechaza_idBieC_diez_millones() {
            assertThatThrownBy(() -> new FalActaSnapshot(1L).setIdBieC(10_000_000))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("idBieC");
        }

        @Test
        @DisplayName("35. La copia preserva idBieI e idBieC validos")
        void copia_preserva_valores() {
            FalActaSnapshot snap = new FalActaSnapshot(1L);
            snap.setIdBieI(1);
            snap.setIdBieC(9_999_999);
            FalActaSnapshot c = snap.copia();
            assertThat(c.getIdBieI()).isEqualTo(1);
            assertThat(c.getIdBieC()).isEqualTo(9_999_999);
        }
    }
}
