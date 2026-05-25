package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class PostCondenaFirmeIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0006";
    private static final String MONTO_CONDENA_JSON = "{\"montoCondena\":2100}";

    @Autowired
    private MockMvc mvc;

    @Test
    void despuesDeVencimiento_condenaFirmeTieneSalidasOperativasSinPagoVoluntario() throws Exception {
        avanzarACondenaFirme();

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.montoCondena").value(2100))
                .andExpect(jsonPath("$.montoPagoVoluntario").value(nullValue()))
                .andExpect(jsonPath("$.situacionPago").value("SIN_PAGO"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("PENDIENTE"))
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles").isEmpty())
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles[0]").value("INFORMAR"))
                .andExpect(
                        jsonPath("$.accionesGestionExternaDisponibles[*]")
                                .value(containsInAnyOrder("APREMIO", "JUZGADO_DE_PAZ")))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.cerrabilidad.motivoNoCerrable")
                        .value("Condena firme pendiente de pago o derivación externa."));
    }

    @Test
    void informarYConfirmarPagoCondena_habilitaCierreSinComprobantes() throws Exception {
        avanzarACondenaFirme();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/informar-pago-condena"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPagoCondena").value("INFORMADO"))
                .andExpect(jsonPath("$.em").doesNotExist())
                .andExpect(jsonPath("$.rc").doesNotExist())
                .andExpect(jsonPath("$.cmte").doesNotExist())
                .andExpect(jsonPath("$.pref").doesNotExist())
                .andExpect(jsonPath("$.nro").doesNotExist());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPagoCondena").value("INFORMADO"))
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles[*]")
                        .value(containsInAnyOrder("CONFIRMAR", "OBSERVAR")))
                .andExpect(jsonPath("$.accionesGestionExternaDisponibles").isEmpty())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.cerrabilidad.motivoNoCerrable")
                        .value("Pago de condena informado pendiente de confirmación."));

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("PAGO_CONDENA_INFORMADO")));

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/confirmar-pago-condena"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPagoCondena").value("CONFIRMADO"));

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("CONFIRMADO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(true))
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles").isEmpty())
                .andExpect(jsonPath("$.accionesGestionExternaDisponibles").isEmpty())
                .andExpect(jsonPath("$.emisionDeuda").doesNotExist())
                .andExpect(jsonPath("$.recibo").doesNotExist())
                .andExpect(jsonPath("$.comprobante").doesNotExist());

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("PAGO_CONDENA_CONFIRMADO")))
                .andExpect(jsonPath("$[*].tipoEvento", not(hasItem("EM"))))
                .andExpect(jsonPath("$[*].tipoEvento", not(hasItem("RC"))))
                .andExpect(jsonPath("$[*].tipoEvento", not(hasItem("CMTE"))));
    }

    @Test
    void cerrarDespuesDePagoCondenaConfirmado_conservaEstadoDeCondena() throws Exception {
        avanzarACondenaFirme();
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/informar-pago-condena"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/confirmar-pago-condena"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/cerrar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("CERRADAS"));

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("CERRADAS"))
                .andExpect(jsonPath("$.montoCondena").value(2100))
                .andExpect(jsonPath("$.situacionPagoCondena").value("CONFIRMADO"))
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles").isEmpty())
                .andExpect(jsonPath("$.accionesGestionExternaDisponibles").isEmpty())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false));
    }

    @Test
    void observarPagoCondena_vuelveAPermitirInformarYNoCierra() throws Exception {
        avanzarACondenaFirme();
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/informar-pago-condena"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/observar-pago-condena"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPagoCondena").value("OBSERVADO"));

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPagoCondena").value("OBSERVADO"))
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles[0]").value("INFORMAR"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false));

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("PAGO_CONDENA_OBSERVADO")));
    }

    @Test
    void rechazosControladosPagoCondenaYCierre() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/informar-pago-condena"))
                .andExpect(status().isConflict());

        avanzarACondenaFirmeSinReset();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/confirmar-pago-condena"))
                .andExpect(status().isConflict());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/observar-pago-condena"))
                .andExpect(status().isConflict());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/cerrar-acta"))
                .andExpect(status().isConflict());
    }

    @Test
    void derivacionExternaDesdeCondenaFirme_conservaCondenaYMontoSinComprobantes() throws Exception {
        avanzarACondenaFirme();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/derivar-a-apremio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("GESTION_EXTERNA"))
                .andExpect(jsonPath("$.tipoGestionExterna").value("APREMIO"));

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("GESTION_EXTERNA"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.montoCondena").value(2100))
                .andExpect(jsonPath("$.situacionPagoCondena").value("PENDIENTE"))
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles").isEmpty())
                .andExpect(jsonPath("$.em").doesNotExist())
                .andExpect(jsonPath("$.EM").doesNotExist())
                .andExpect(jsonPath("$.rc").doesNotExist())
                .andExpect(jsonPath("$.RC").doesNotExist())
                .andExpect(jsonPath("$.cmte").doesNotExist())
                .andExpect(jsonPath("$.pref").doesNotExist())
                .andExpect(jsonPath("$.nro").doesNotExist())
                .andExpect(jsonPath("$.emisionDeuda").doesNotExist())
                .andExpect(jsonPath("$.recibo").doesNotExist())
                .andExpect(jsonPath("$.comprobante").doesNotExist());
    }

    private void avanzarACondenaFirme() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        avanzarACondenaFirmeSinReset();
    }

    private void avanzarACondenaFirmeSinReset() throws Exception {
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/dictar-fallo-condenatorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MONTO_CONDENA_JSON))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/firmar-documento/DOC-0006-02"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-notificacion-positiva"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-vencimiento-plazo-apelacion"))
                .andExpect(status().isOk());
    }
}
