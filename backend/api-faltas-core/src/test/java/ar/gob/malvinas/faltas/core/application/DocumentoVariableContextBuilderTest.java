package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.combinacion.DocumentoVariableContextBuilder;
import ar.gob.malvinas.faltas.core.application.demo.GraphDemoActaFactory;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.domain.model.FalNotificacion;
import ar.gob.malvinas.faltas.core.domain.model.FalPagoVoluntario;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Micro-slice 8F-2: DocumentoVariableContextBuilder")
class DocumentoVariableContextBuilderTest {

    private FalActa actaCompleta;
    private FalDocumento documentoDemo;

    @BeforeEach
    void setUp() {
        actaCompleta = GraphDemoActaFactory.crearActaDemo(1L);
        documentoDemo = GraphDemoActaFactory.crearDocumentoDemo(10L, 1L, TipoDocu.ACTO_ADMINISTRATIVO, FaltasClockTestSupport.FIXED.now());
        documentoDemo.setNroDocu("RES-2024-00010");
    }

    @Nested
    @DisplayName("Contexto base desde acta")
    class ContextoBase {

        @Test
        @DisplayName("1. Construye contexto basico desde acta con datos completos")
        void construye_contexto_basico() {
            Map<String, Object> ctx = DocumentoVariableContextBuilder.buildDesdeActa(actaCompleta, documentoDemo, FaltasClockTestSupport.FIXED.now());
            assertThat(ctx).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("2. Incluye acta.nroActa cuando el acta tiene numero")
        void incluye_nro_acta() {
            Map<String, Object> ctx = DocumentoVariableContextBuilder.buildDesdeActa(actaCompleta, null, FaltasClockTestSupport.FIXED.now());
            assertThat(ctx).containsKey("acta.nroActa");
            assertThat(ctx.get("acta.nroActa")).isEqualTo(GraphDemoActaFactory.NRO_ACTA_DEMO);
        }

        @Test
        @DisplayName("3. Incluye acta.fechaLabrado cuando el acta tiene fecha")
        void incluye_fecha_labrado() {
            Map<String, Object> ctx = DocumentoVariableContextBuilder.buildDesdeActa(actaCompleta, null, FaltasClockTestSupport.FIXED.now());
            assertThat(ctx).containsKey("acta.fechaLabrado");
            assertThat(ctx.get("acta.fechaLabrado")).isNotNull();
        }

        @Test
        @DisplayName("4. Incluye infractor.nombreCompleto")
        void incluye_nombre_infractor() {
            Map<String, Object> ctx = DocumentoVariableContextBuilder.buildDesdeActa(actaCompleta, null, FaltasClockTestSupport.FIXED.now());
            assertThat(ctx).containsKey("infractor.nombreCompleto");
            assertThat(ctx.get("infractor.nombreCompleto")).isEqualTo("Juan Carlos Perez");
        }

        @Test
        @DisplayName("5. Incluye infractor.documento")
        void incluye_documento_infractor() {
            Map<String, Object> ctx = DocumentoVariableContextBuilder.buildDesdeActa(actaCompleta, null, FaltasClockTestSupport.FIXED.now());
            assertThat(ctx).containsKey("infractor.documento");
            assertThat(ctx.get("infractor.documento")).isEqualTo("12345678");
        }

        @Test
        @DisplayName("6. Incluye domicilioInfractor.texto")
        void incluye_domicilio_infractor() {
            Map<String, Object> ctx = DocumentoVariableContextBuilder.buildDesdeActa(actaCompleta, null, FaltasClockTestSupport.FIXED.now());
            assertThat(ctx).containsKey("domicilioInfractor.texto");
        }

        @Test
        @DisplayName("7. Incluye domicilioInfraccion.texto")
        void incluye_domicilio_infraccion() {
            Map<String, Object> ctx = DocumentoVariableContextBuilder.buildDesdeActa(actaCompleta, null, FaltasClockTestSupport.FIXED.now());
            assertThat(ctx).containsKey("domicilioInfraccion.texto");
        }

        @Test
        @DisplayName("8. Incluye ubicacion cuando lat/lon estan presentes")
        void incluye_ubicacion() {
            Map<String, Object> ctx = DocumentoVariableContextBuilder.buildDesdeActa(actaCompleta, null, FaltasClockTestSupport.FIXED.now());
            assertThat(ctx).containsKey("ubicacion.lat");
            assertThat(ctx).containsKey("ubicacion.lon");
        }

        @Test
        @DisplayName("9. Incluye infraccion.descripcion desde observaciones del acta")
        void incluye_infraccion_descripcion() {
            Map<String, Object> ctx = DocumentoVariableContextBuilder.buildDesdeActa(actaCompleta, null, FaltasClockTestSupport.FIXED.now());
            assertThat(ctx).containsKey("infraccion.descripcion");
        }

        @Test
        @DisplayName("10. Incluye sistema.municipioNombre siempre")
        void incluye_municipio_nombre() {
            Map<String, Object> ctx = DocumentoVariableContextBuilder.buildDesdeActa(actaCompleta, null, FaltasClockTestSupport.FIXED.now());
            assertThat(ctx).containsKey("sistema.municipioNombre");
            assertThat(ctx.get("sistema.municipioNombre")).isEqualTo("Malvinas Argentinas");
        }

        @Test
        @DisplayName("11. Incluye sistema.fechaActual siempre")
        void incluye_fecha_actual() {
            Map<String, Object> ctx = DocumentoVariableContextBuilder.buildDesdeActa(actaCompleta, null, FaltasClockTestSupport.FIXED.now());
            assertThat(ctx).containsKey("sistema.fechaActual");
            assertThat(ctx.get("sistema.fechaActual")).isNotNull();
        }

        @Test
        @DisplayName("12. Incluye documento.nroDocu cuando el documento tiene numero")
        void incluye_nro_docu() {
            Map<String, Object> ctx = DocumentoVariableContextBuilder.buildDesdeActa(actaCompleta, documentoDemo, FaltasClockTestSupport.FIXED.now());
            assertThat(ctx).containsKey("documento.nroDocu");
            assertThat(ctx.get("documento.nroDocu")).isEqualTo("RES-2024-00010");
        }

        @Test
        @DisplayName("13. Incluye licencia.municipioEmisor (mock deterministico)")
        void incluye_licencia_municipio() {
            Map<String, Object> ctx = DocumentoVariableContextBuilder.buildDesdeActa(actaCompleta, null, FaltasClockTestSupport.FIXED.now());
            assertThat(ctx).containsKey("licencia.municipioEmisor");
        }

        @Test
        @DisplayName("14. Incluye nomenclatura.manzana y nomenclatura.parcela (mock deterministico)")
        void incluye_nomenclatura() {
            Map<String, Object> ctx = DocumentoVariableContextBuilder.buildDesdeActa(actaCompleta, null, FaltasClockTestSupport.FIXED.now());
            assertThat(ctx).containsKey("nomenclatura.manzana");
            assertThat(ctx).containsKey("nomenclatura.parcela");
        }

        @Test
        @DisplayName("15. No incluye acta.nroActa cuando es null")
        void no_incluye_nro_acta_si_null() {
            FalActa actaSinNumero = new FalActa(
                    99L, "uuid-sin-nro", "TRANSITO", "DEP-01", "INS-001",
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), FaltasClockTestSupport.FIXED.now(),
                    "Calle X", "Calle Y", null, null, null,
                    "Infractor Test", "99999999",
                    ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor.FIRMADA);
            Map<String, Object> ctx = DocumentoVariableContextBuilder.buildDesdeActa(actaSinNumero, null, FaltasClockTestSupport.FIXED.now());
            assertThat(ctx).doesNotContainKey("acta.nroActa");
        }

        @Test
        @DisplayName("16. Las claves siguen el formato namespace.campo")
        void claves_en_formato_namespace_campo() {
            Map<String, Object> ctx = DocumentoVariableContextBuilder.buildDesdeActa(actaCompleta, null, FaltasClockTestSupport.FIXED.now());
            ctx.keySet().forEach(clave ->
                assertThat(clave).matches("[a-z][a-zA-Z0-9]*\\.[a-z][a-zA-Z0-9]*"));
        }
    }

    @Nested
    @DisplayName("Variables de fallo")
    class VariablesFallo {

        @Test
        @DisplayName("17. Incluye fallo.tipo cuando fallo no es null")
        void incluye_fallo_tipo() {
            FalActaFallo fallo = GraphDemoActaFactory.crearFalloAbsolutorioDemo(1L);
            Map<String, Object> ctx = DocumentoVariableContextBuilder.buildDesdeActa(
                    actaCompleta, null, fallo, null, null, FaltasClockTestSupport.FIXED.now());
            assertThat(ctx).containsKey("fallo.tipo");
            assertThat(ctx.get("fallo.tipo")).isEqualTo("ABSOLUTORIO");
        }

        @Test
        @DisplayName("18. Incluye fallo.monto para fallo condenatorio")
        void incluye_fallo_monto() {
            FalActaFallo fallo = GraphDemoActaFactory.crearFalloCondenatorioDemo(1L);
            Map<String, Object> ctx = DocumentoVariableContextBuilder.buildDesdeActa(
                    actaCompleta, null, fallo, null, null, FaltasClockTestSupport.FIXED.now());
            assertThat(ctx).containsKey("fallo.monto");
            assertThat(ctx.get("fallo.monto")).isEqualTo(BigDecimal.valueOf(15000));
        }

        @Test
        @DisplayName("19. Incluye fallo.fechaDictado")
        void incluye_fallo_fecha_dictado() {
            FalActaFallo fallo = GraphDemoActaFactory.crearFalloAbsolutorioDemo(1L);
            Map<String, Object> ctx = DocumentoVariableContextBuilder.buildDesdeActa(
                    actaCompleta, null, fallo, null, null, FaltasClockTestSupport.FIXED.now());
            assertThat(ctx).containsKey("fallo.fechaDictado");
        }

        @Test
        @DisplayName("20. No incluye fallo.* cuando fallo es null")
        void no_incluye_fallo_si_null() {
            Map<String, Object> ctx = DocumentoVariableContextBuilder.buildDesdeActa(
                    actaCompleta, null, null, null, null, FaltasClockTestSupport.FIXED.now());
            assertThat(ctx).doesNotContainKey("fallo.tipo");
            assertThat(ctx).doesNotContainKey("fallo.monto");
        }
    }

    @Nested
    @DisplayName("Variables de pago")
    class VariablesPago {

        @Test
        @DisplayName("21. Incluye pago.monto cuando pago no es null")
        void incluye_pago_monto() {
            FalPagoVoluntario pago = GraphDemoActaFactory.crearPagoVoluntarioDemo(1L);
            Map<String, Object> ctx = DocumentoVariableContextBuilder.buildDesdeActa(
                    actaCompleta, null, null, pago, null, FaltasClockTestSupport.FIXED.now());
            assertThat(ctx).containsKey("pago.monto");
        }

        @Test
        @DisplayName("22. Incluye pago.estado cuando pago no es null")
        void incluye_pago_estado() {
            FalPagoVoluntario pago = GraphDemoActaFactory.crearPagoVoluntarioDemo(1L);
            Map<String, Object> ctx = DocumentoVariableContextBuilder.buildDesdeActa(
                    actaCompleta, null, null, pago, null, FaltasClockTestSupport.FIXED.now());
            assertThat(ctx).containsKey("pago.estado");
        }

        @Test
        @DisplayName("23. No incluye pago.* cuando pago es null")
        void no_incluye_pago_si_null() {
            Map<String, Object> ctx = DocumentoVariableContextBuilder.buildDesdeActa(
                    actaCompleta, null, null, null, null, FaltasClockTestSupport.FIXED.now());
            assertThat(ctx).doesNotContainKey("pago.monto");
        }
    }

    @Nested
    @DisplayName("Variables de notificacion")
    class VariablesNotificacion {

        @Test
        @DisplayName("24. Incluye notificacion.canal cuando notificacion no es null")
        void incluye_notificacion_canal() {
            FalNotificacion notif = GraphDemoActaFactory.crearNotificacionDemo(
                    1L, 10L, TipoDocu.NOTIFICACION_ACTA);
            Map<String, Object> ctx = DocumentoVariableContextBuilder.buildDesdeActa(
                    actaCompleta, null, null, null, notif, FaltasClockTestSupport.FIXED.now());
            assertThat(ctx).containsKey("notificacion.canal");
            assertThat(ctx.get("notificacion.canal")).isEqualTo("CORREO");
        }

        @Test
        @DisplayName("25. No incluye notificacion.* cuando notificacion es null")
        void no_incluye_notificacion_si_null() {
            Map<String, Object> ctx = DocumentoVariableContextBuilder.buildDesdeActa(
                    actaCompleta, null, null, null, null, FaltasClockTestSupport.FIXED.now());
            assertThat(ctx).doesNotContainKey("notificacion.canal");
        }
    }
}
