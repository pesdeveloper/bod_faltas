package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Registrar vencimiento de pago voluntario: evita que una solicitud con monto
 * fijado paralice indefinidamente el expediente y habilita continuar a fallo.
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class RegistrarVencimientoPagoVoluntarioIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0024";
    private static final String MONTO_4500 = "{\"monto\":4500}";

    @Autowired
    private MockMvc mvc;

    @Test
    void pagoVoluntarioSolicitadoSinMontoNoMuestraVencimiento() throws Exception {
        prepararPagoVoluntarioSolicitadoSinMonto();

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_REVISION"))
                .andExpect(jsonPath("$.situacionPago").value("SOLICITADO"))
                .andExpect(jsonPath("$.montoPagoVoluntario").value(nullValue()))
                .andExpect(jsonPath("$.accionPendiente").value("EVALUAR_PAGO_VOLUNTARIO"))
                .andExpect(jsonPath("$.accionesUi.pagoVoluntario").value(true))
                .andExpect(jsonPath("$.accionesUi.vencimientoPagoVoluntario").value(false))
                .andExpect(jsonPath("$.accionesUi.falloFondo").value(false));
    }

    @Test
    void pagoVoluntarioSolicitadoMuestraVencimiento() throws Exception {
        prepararPagoVoluntarioSolicitadoSinMonto();
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":3500}"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.situacionPago").value("SOLICITADO"))
                .andExpect(jsonPath("$.montoPagoVoluntario").value(3500))
                .andExpect(jsonPath("$.accionPendiente").doesNotExist())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("SIN_RESULTADO_FINAL"))
                .andExpect(jsonPath("$.accionesUi.pagoVoluntario").value(true))
                .andExpect(jsonPath("$.accionesUi.vencimientoPagoVoluntario").value(true))
                .andExpect(jsonPath("$.accionesUi.falloFondo").value(false))
                .andExpect(jsonPath("$.accionesUi.cumplimientoMaterial").value(false))
                .andExpect(jsonPath("$.accionesUi.resolucionBloqueante").value(false));
    }

    @Test
    void acta0024JsonRealVencidaConBloqueantesMaterialesHabilitaFalloFondo() throws Exception {
        prepararPagoVoluntarioSolicitadoConMonto();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-vencimiento-pago-voluntario"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje")
                        .value("Vencimiento de pago voluntario registrado. El tramite puede continuar a fallo."))
                .andExpect(jsonPath("$.situacionPago").value("VENCIDO"))
                .andExpect(jsonPath("$.montoPagoVoluntario").value(4500));

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_REVISION"))
                .andExpect(jsonPath("$.situacionAdministrativaActual").value("ACTIVA"))
                .andExpect(jsonPath("$.situacionPago").value("VENCIDO"))
                .andExpect(jsonPath("$.montoPagoVoluntario").value(4500))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("SIN_RESULTADO_FINAL"))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(2))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[0]")
                        .value("ENTREGA_DOCUMENTACION"))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[1]")
                        .value("LIBERACION_RODADO"))
                .andExpect(jsonPath("$.accionPendiente").doesNotExist())
                .andExpect(jsonPath("$.accionesUi.falloFondo").value(true))
                .andExpect(jsonPath("$.accionesUi.pagoVoluntario").value(false))
                .andExpect(jsonPath("$.accionesUi.vencimientoPagoVoluntario").value(false))
                .andExpect(jsonPath("$.accionesUi.cumplimientoMaterial").value(false))
                .andExpect(jsonPath("$.accionesUi.resolucionBloqueante").value(false))
                .andExpect(jsonPath("$.accionesUi.cierre").value(false))
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles", not(hasItem("SOLICITAR"))))
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles", not(hasItem("INFORMAR"))));

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("PAGO_VOLUNTARIO_VENCIDO")));
    }

    @Test
    void registrarVencimientoPermiteDictarFallo() throws Exception {
        prepararPagoVoluntarioSolicitadoConMonto();
        registrarVencimiento();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/dictar-fallo-condenatorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoCondena\":35000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoDocumento").value("FALLO_CONDENATORIO"))
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"))
                .andExpect(jsonPath("$.montoCondena").value(35000));

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoCondena").value(35000))
                .andExpect(jsonPath("$.montoPagoVoluntario").value(4500))
                .andExpect(jsonPath("$.situacionPago").value("VENCIDO"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("SIN_RESULTADO_FINAL"));

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("FALLO_CONDENATORIO_DICTADO")));
    }

    @Test
    void registrarVencimientoRechazaSiPagoConfirmado() throws Exception {
        prepararPagoVoluntarioSolicitadoConMonto();
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-pago-informado"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/adjuntar-comprobante-pago-informado")
                        .param("nombreArchivo", "comprobante_0024.pdf"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/confirmar-pago-informado"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-vencimiento-pago-voluntario"))
                .andExpect(status().isConflict());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPago").value("CONFIRMADO"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("PAGO_CONFIRMADO"));
    }

    @Test
    void registrarVencimientoRechazaEstadosNoOperables() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        assertVencimientoRechazado("ACTA-0007", "ARCHIVO");
        assertVencimientoRechazado("ACTA-0008", "CERRADAS");
        assertVencimientoRechazado("ACTA-0017", "GESTION_EXTERNA");
        assertVencimientoRechazado("ACTA-0029", "PENDIENTE_ANALISIS");
        assertVencimientoRechazado("ACTA-0030", "PENDIENTE_ANALISIS");
        assertVencimientoRechazado("ACTA-0021", "PENDIENTE_ANALISIS");
    }

    @Test
    void materialesSiguenNoEjecutablesTrasVencimiento() throws Exception {
        prepararPagoVoluntarioSolicitadoConMonto();
        registrarVencimiento();

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accionesUi.cumplimientoMaterial").value(false))
                .andExpect(jsonPath("$.accionesUi.resolucionBloqueante").value(false))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(2))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[0]")
                        .value("ENTREGA_DOCUMENTACION"))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[1]")
                        .value("LIBERACION_RODADO"));

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                        .param("tipo", "ENTREGA_DOCUMENTACION"))
                .andExpect(status().isConflict());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                        .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isConflict());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(2));
    }

    @Test
    void noHabilitaPagoVoluntarioDespuesDeVencido() throws Exception {
        prepararPagoVoluntarioSolicitadoConMonto();
        registrarVencimiento();

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accionesUi.pagoVoluntario").value(false))
                .andExpect(jsonPath("$.accionesUi.vencimientoPagoVoluntario").value(false))
                .andExpect(jsonPath("$.accionesUi.falloFondo").value(true))
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles").isEmpty());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MONTO_4500))
                .andExpect(status().isConflict());
    }

    private void prepararPagoVoluntarioSolicitadoConMonto() throws Exception {
        prepararPagoVoluntarioSolicitadoSinMonto();
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/fijar-monto-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MONTO_4500))
                .andExpect(status().isOk());
    }

    private void prepararPagoVoluntarioSolicitadoSinMonto() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/enviar-a-notificacion"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-notificacion-positiva"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/infractor/actas/QR-ACTA-0024-DEMO/acciones/solicitar-pago-voluntario"))
                .andExpect(status().isOk());
    }

    private void registrarVencimiento() throws Exception {
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-vencimiento-pago-voluntario"))
                .andExpect(status().isOk());
    }

    private void assertVencimientoRechazado(String actaId, String bandeja) throws Exception {
        mvc.perform(get(B + "/actas/" + actaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value(bandeja));

        mvc.perform(post(B + "/actas/" + actaId + "/acciones/registrar-vencimiento-pago-voluntario"))
                .andExpect(status().isConflict());
    }
}
