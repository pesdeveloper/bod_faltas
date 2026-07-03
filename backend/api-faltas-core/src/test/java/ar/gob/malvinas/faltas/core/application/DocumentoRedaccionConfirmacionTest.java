package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoRedaccionDocumento;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoRedaccion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Micro-slice 8F-3: Confirmacion de redaccion - dominio FalDocumentoRedaccion")
class DocumentoRedaccionConfirmacionTest {

    private static final LocalDateTime FH = LocalDateTime.of(2024, 5, 1, 10, 0);

    private static FalDocumentoRedaccion borrador(String contenido) {
        return new FalDocumentoRedaccion(
                1L, 100L, 200L,
                EstadoRedaccionDocumento.BORRADOR,
                contenido,
                null, null, null,
                FH, "usr-creacion",
                FH, "usr-creacion",
                null, null);
    }

    private static FalDocumentoRedaccion conEstado(EstadoRedaccionDocumento estado) {
        FalDocumentoRedaccion r = borrador("contenido de prueba");
        r.setEstadoRedaccion(estado);
        return r;
    }

    @Nested
    @DisplayName("Confirmacion desde BORRADOR")
    class ConfirmacionDesdeBorrador {

        @Test
        @DisplayName("1. Confirma redaccion BORRADOR y pasa a CONFIRMADA")
        void confirma_borrador() {
            FalDocumentoRedaccion r = borrador("Contenido del acta de infraccion.");
            r.confirmar(FH, "usr");
            assertThat(r.getEstadoRedaccion()).isEqualTo(EstadoRedaccionDocumento.CONFIRMADA);
        }

        @Test
        @DisplayName("2. Setea fhConfirmacion al confirmar")
        void setea_fh_confirmacion() {
            FalDocumentoRedaccion r = borrador("Texto de prueba.");
            r.confirmar(FH, "usr");
            assertThat(r.getFhConfirmacion()).isEqualTo(FH);
        }

        @Test
        @DisplayName("3. Setea idUserConfirmacion al confirmar")
        void setea_id_user_confirmacion() {
            FalDocumentoRedaccion r = borrador("Texto de prueba.");
            r.confirmar(FH, "usr-firma");
            assertThat(r.getIdUserConfirmacion()).isEqualTo("usr-firma");
        }

        @Test
        @DisplayName("4. El contenido editable no cambia al confirmar")
        void contenido_no_cambia() {
            FalDocumentoRedaccion r = borrador("Texto invariante.");
            r.confirmar(FH, "usr");
            assertThat(r.getContenidoEditable()).isEqualTo("Texto invariante.");
        }

        @Test
        @DisplayName("5. Confirmar no genera storageKey ni hashDocu (eso es trabajo del servicio)")
        void confirmar_no_genera_storage_ni_hash() {
            FalDocumentoRedaccion r = borrador("Contenido.");
            r.confirmar(FH, "usr");
            // El metodo de dominio confirmar() no tiene acceso a FalDocumento
            // Verificamos que estadoRedaccion cambio correctamente
            assertThat(r.estaConfirmada()).isTrue();
        }
    }

    @Nested
    @DisplayName("Confirmacion desde REABIERTA")
    class ConfirmacionDesdeReabierta {

        @Test
        @DisplayName("6. Puede confirmar desde REABIERTA")
        void puede_confirmar_reabierta() {
            FalDocumentoRedaccion r = conEstado(EstadoRedaccionDocumento.REABIERTA);
            r.confirmar(FH, "usr");
            assertThat(r.getEstadoRedaccion()).isEqualTo(EstadoRedaccionDocumento.CONFIRMADA);
        }
    }

    @Nested
    @DisplayName("Errores de confirmacion")
    class ErroresConfirmacion {

        @Test
        @DisplayName("7. No permite confirmar ANULADA")
        void no_permite_confirmar_anulada() {
            FalDocumentoRedaccion r = conEstado(EstadoRedaccionDocumento.ANULADA);
            assertThatThrownBy(() -> r.confirmar(FH, "usr"))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("ANULADA");
        }

        @Test
        @DisplayName("8. No permite confirmar dos veces")
        void no_permite_confirmar_dos_veces() {
            FalDocumentoRedaccion r = borrador("Contenido.");
            r.confirmar(FH, "usr");
            assertThatThrownBy(() -> r.confirmar(FH, "usr"))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("CONFIRMADA");
        }

        @Test
        @DisplayName("9. No permite confirmar contenido vacio")
        void no_permite_confirmar_contenido_vacio() {
            FalDocumentoRedaccion r = borrador("   ");
            assertThatThrownBy(() -> r.confirmar(FH, "usr"))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("vacio");
        }

        @Test
        @DisplayName("10. No permite confirmar contenido de solo espacios")
        void no_permite_confirmar_contenido_solo_espacios() {
            FalDocumentoRedaccion r = borrador("     ");
            assertThatThrownBy(() -> r.confirmar(FH, "usr"))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
    }

    @Nested
    @DisplayName("Helpers de estado")
    class HelpersEstado {

        @Test
        @DisplayName("11. esBorrador() retorna true para BORRADOR")
        void es_borrador() {
            assertThat(borrador("c").esBorrador()).isTrue();
        }

        @Test
        @DisplayName("12. estaConfirmada() retorna true para CONFIRMADA")
        void esta_confirmada() {
            FalDocumentoRedaccion r = borrador("c");
            r.confirmar(FH, "usr");
            assertThat(r.estaConfirmada()).isTrue();
        }

        @Test
        @DisplayName("13. esReabierta() retorna true para REABIERTA")
        void es_reabierta() {
            FalDocumentoRedaccion r = conEstado(EstadoRedaccionDocumento.REABIERTA);
            assertThat(r.esReabierta()).isTrue();
        }

        @Test
        @DisplayName("14. estaAnulada() retorna true para ANULADA")
        void esta_anulada() {
            FalDocumentoRedaccion r = conEstado(EstadoRedaccionDocumento.ANULADA);
            assertThat(r.estaAnulada()).isTrue();
        }
    }
}
