package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Valida que situacionPago general se normaliza correctamente cuando el pago
 * corresponde al circuito de condena, y que tipoPago refleja el origen.
 *
 * <p>Regla de dominio consolidada:
 * <ul>
 *   <li>{@code situacionPago} representa el estado general de pago del expediente.</li>
 *   <li>{@code situacionPagoCondena} representa el estado especifico del circuito de condena.</li>
 *   <li>{@code tipoPago} indica el circuito: VOLUNTARIO o CONDENA.</li>
 *   <li>Un expediente con {@code situacionPagoCondena=CONFIRMADO} no debe
 *       mostrarse como {@code situacionPago=SIN_PAGO}.</li>
 * </ul>
 *
 * <p>Nota de dominio: {@code consentir-condena} requiere {@code resultadoFinal=CONDENADO}
 * (plazo de apelacion aun abierto). Hace pasar el acta a {@code CONDENA_FIRME}.
 * {@code informar-pago-condena} requiere {@code resultadoFinal=CONDENA_FIRME}.
 *
 * <p>Usa ACTA-0029. El codigoQr del portal es QR-ACTA-0029-DEMO.
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class SituacionPagoGeneralNormalizadaIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0029";
    private static final String QR = "QR-ACTA-0029-DEMO";
    private static final String DOC_FALLO = "DOC-0029-02";
    private static final String MONTO_CONDENA = "{\"montoCondena\":1700}";

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void reset() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
    }

    // ── 1. PagoCondenaInformadoActualizaSituacionPagoGeneralIT ──────────────────

    @Test
    void pagoCondenaInformado_actualizaSituacionPagoGeneral() throws Exception {
        avanzarACondenaFirme();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/informar-pago-condena"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPago").value("PENDIENTE_CONFIRMACION"))
                .andExpect(jsonPath("$.tipoPago").value("CONDENA"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("INFORMADO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false));
    }

    // ── 2. PagoCondenaConfirmadoActualizaSituacionPagoGeneralIT ─────────────────

    @Test
    void pagoCondenaConfirmado_actualizaSituacionPagoGeneral() throws Exception {
        avanzarACondenaFirme();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/informar-pago-condena"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/confirmar-pago-condena"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPago").value("CONFIRMADO"))
                .andExpect(jsonPath("$.tipoPago").value("CONDENA"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("CONFIRMADO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(true));
    }

    // ── 3. PagoCondenaCerradaNoQuedaSinPagoIT ───────────────────────────────────
    //
    // consentir-condena requiere resultadoFinal=CONDENADO (plazo aun abierto).
    // Tras consentir, el acta pasa a CONDENA_FIRME. Luego el infractor paga.

    @Test
    void pagoCondenaCerrada_noQuedaSinPago() throws Exception {
        avanzarACondenado();

        // Infractor consiente condena desde portal: CONDENADO -> CONDENA_FIRME
        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/consentir-condena"))
                .andExpect(status().isOk());

        // Infractor informa pago de condena desde portal
        mvc.perform(post(B + "/infractor/actas/" + QR + "/acciones/pagar-condena"))
                .andExpect(status().isOk());

        // Direccion confirma pago
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/confirmar-pago-condena"))
                .andExpect(status().isOk());

        // Cerrar
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/cerrar-acta"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("CERRADAS"))
                .andExpect(jsonPath("$.situacionPago").value("CONFIRMADO"))
                .andExpect(jsonPath("$.tipoPago").value("CONDENA"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("CONFIRMADO"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.estaCerrada").value(true));
    }

    // ── 4. PagoVoluntarioSigueInformandoTipoVoluntarioIT ────────────────────────

    @Test
    void pagoVoluntario_informaTipoVoluntario() throws Exception {
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":2500}"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPago").value("SOLICITADO"))
                .andExpect(jsonPath("$.tipoPago").value("VOLUNTARIO"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("NO_APLICA"));
    }

    // ── 5. CondenaFirmePendienteNoSeMarcaComoPagoEnCursoIT ──────────────────────

    @Test
    void condenaFirmePendiente_noSeMarcaComoPagoEnCurso() throws Exception {
        avanzarACondenaFirme();

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPago").value("SIN_PAGO"))
                .andExpect(jsonPath("$.tipoPago").value("NO_APLICA"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("PENDIENTE"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false));
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    /** Avanza a CONDENA_FIRME (plazo de apelacion vencido). */
    private void avanzarACondenaFirme() throws Exception {
        avanzarACondenado();
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-vencimiento-plazo-apelacion"))
                .andExpect(status().isOk());
    }

    /** Avanza a CONDENADO (fallo firmado y notificado, plazo aun abierto). */
    private void avanzarACondenado() throws Exception {
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/dictar-fallo-condenatorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MONTO_CONDENA))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/firmar-documento/" + DOC_FALLO))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-notificacion-positiva"))
                .andExpect(status().isOk());
    }
}
