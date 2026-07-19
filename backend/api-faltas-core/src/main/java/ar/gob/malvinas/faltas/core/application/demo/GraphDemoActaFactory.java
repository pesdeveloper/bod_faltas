package ar.gob.malvinas.faltas.core.application.demo;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFalloActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFirmaReq;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.domain.model.FalNotificacion;
import ar.gob.malvinas.faltas.core.domain.model.FalPagoVoluntario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Factory de datos demo deterministicos para pruebas del graph demo in-memory.
 *
 * Todos los metodos son estaticos y devuelven objetos con datos fijos y representativos.
 * No se conecta a ninguna base de datos ni a infraestructura real.
 * Los objetos deben cargarse manualmente en los repositorios in-memory del test o contexto.
 *
 * Slice 8F-2.
 */
public final class GraphDemoActaFactory {

    public static final Long ACTA_DEMO_ID = 1L;
    public static final String NRO_ACTA_DEMO = "ACT-2024-00001";
    public static final Long INSPECTOR_DEMO = 1L;
    public static final Long DEPENDENCIA_DEMO = 1L;

    private static final LocalDate FECHA_ACTA_DEMO = LocalDate.of(2024, 3, 15);
    private static final LocalDateTime FH_LABRADO_DEMO = LocalDateTime.of(2024, 3, 15, 10, 30);
    private static final LocalDateTime FH_FALLO_DEMO = LocalDateTime.of(2024, 4, 10, 14, 0);
    private static final LocalDateTime FH_VENCIMIENTO_PAGO = LocalDateTime.of(2024, 5, 1, 23, 59);

    private GraphDemoActaFactory() {}

    /**
     * Crea un acta demo de transito con todos los datos necesarios para combinacion documental.
     */
    public static FalActa crearActaDemo(Long id) {
        FalActa acta = new FalActa(
                id,
                "uuid-demo-" + id,
                TipoActa.TRANSITO,
                DEPENDENCIA_DEMO,
                INSPECTOR_DEMO,
                FECHA_ACTA_DEMO,
                FH_LABRADO_DEMO,
                "Avenida Pioneros 2345, Malvinas Argentinas",
                "Conduccion sin revision tecnica obligatoria vigente",
                -34.5678,
                -58.1234,
                ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR,
                null,
                FH_LABRADO_DEMO,
                "SYS"
        );
        acta.setNroActa(NRO_ACTA_DEMO);
        acta.setInfractorNombre("Juan Carlos Perez");
        acta.setInfractorDocumento("12345678");
        return acta;
    }

    /**
     * Crea un fallo absolutorio demo.
     */
    public static FalActaFallo crearFalloAbsolutorioDemo(Long actaId) {
        FalActaFallo fallo = new FalActaFallo(1L, actaId, TipoFalloActa.ABSOLUTORIO, FH_FALLO_DEMO, FH_FALLO_DEMO, "SYS");
        fallo.setFundamentos("Se resuelve absolver al infractor por falta de merito suficiente.");
        return fallo;
    }

    /**
     * Crea un fallo condenatorio demo con monto de condena.
     */
    public static FalActaFallo crearFalloCondenatorioDemo(Long actaId) {
        return crearFalloCondenatorioDemo(actaId, 2L);
    }

    public static FalActaFallo crearFalloCondenatorioDemo(Long actaId, Long id) {
        FalActaFallo fallo = new FalActaFallo(id, actaId, TipoFalloActa.CONDENATORIO, FH_FALLO_DEMO, FH_FALLO_DEMO, "SYS");
        fallo.setMontoCondena(BigDecimal.valueOf(15000));
        fallo.setFundamentos("Se resuelve condenar al infractor al pago de multa de pesos quince mil.");
        return fallo;
    }

    /**
     * Crea un pago voluntario demo con monto fijado.
     */
    public static FalPagoVoluntario crearPagoVoluntarioDemo(Long actaId) {
        FalPagoVoluntario pago = new FalPagoVoluntario("PAGO-DEMO-" + actaId, actaId);
        pago.setMonto(BigDecimal.valueOf(15000));
        pago.setReferenciaPago("REF-2024-00001");
        pago.setFechaVencimiento(FH_VENCIMIENTO_PAGO);
        return pago;
    }

    /**
     * Crea una notificacion demo enviada por correo.
     */
    public static FalNotificacion crearNotificacionDemo(Long actaId, Long idDocumento, TipoDocu tipoDocu) {
        return new FalNotificacion(
                1L,
                actaId,
                idDocumento,
                tipoDocu,
                "CORREO",
                LocalDateTime.of(2024, 4, 20, 9, 0));
    }

    /**
     * Crea un documento demo en estado BORRADOR.
     */
    public static FalDocumento crearDocumentoDemo(Long id, Long actaId, TipoDocu tipoDocu, LocalDateTime ahora) {
        return new FalDocumento(
                id, actaId, tipoDocu,
                ahora,
                tipoDocu.name() + " - Demo",
                EstadoDocu.BORRADOR,
                TipoFirmaReq.NO_REQUIERE,
                null,
                ahora);
    }
}
