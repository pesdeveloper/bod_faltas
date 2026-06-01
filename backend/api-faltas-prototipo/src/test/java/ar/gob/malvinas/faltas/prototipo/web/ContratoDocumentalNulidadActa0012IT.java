package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato documental ACTA-0012: pieza única NULIDAD.
 *
 * <p>Recorrido completo: PENDIENTES_RESOLUCION_REDACCION →
 * {@code generar-nulidad} → PENDIENTE_FIRMA →
 * {@code firmar-documento/DOC-0012-02} → CERRADAS.
 *
 * <p>Semilla: {@code MockDataFactory#cargarActa0012}.
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class ContratoDocumentalNulidadActa0012IT {

    private static final String B = "/api/prototipo";
    private static final String ID = "ACTA-0012";
    /**
     * ID del documento NULIDAD generado por {@code generar-nulidad}: el acta
     * ya tiene DOC-0012-01 (INFORME_VICIO_FORMAL), por lo que el siguiente
     * es DOC-0012-02.
     */
    private static final String DOC_NULIDAD = "DOC-0012-02";

    @Autowired
    private MockMvc mvc;

    @Test
    void estadoInicial_piezaNulidadRequerida_generadasVacio_bandejaRedaccion() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTES_RESOLUCION_REDACCION"))
                .andExpect(jsonPath("$.piezasRequeridas[0]").value("NULIDAD"))
                .andExpect(jsonPath("$.piezasGeneradas.length()").value(0));
    }

    @Test
    void generarNulidad_pasaAPendienteFirma() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/generar-nulidad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_FIRMA_PIEZAS"));
    }

    @Test
    void documentosTrasgenerarNulidad_existeNulidadPendienteFirma() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/generar-nulidad"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID + "/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].id").value(DOC_NULIDAD))
                .andExpect(jsonPath("$[1].tipoDocumento").value("NULIDAD"))
                .andExpect(jsonPath("$[1].estadoDocumento").value("PENDIENTE_FIRMA"));
    }

    @Test
    void firmarNulidad_cierreInvalidante_pasaACerradas() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/generar-nulidad"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/firmar-documento/" + DOC_NULIDAD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("CERRADAS"));

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("CERRADAS"))
                .andExpect(jsonPath("$.estaCerrada").value(true));
    }

    @Test
    void flujoCompleto_deRedaccionACerrada_documentoNulidadFirmado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTES_RESOLUCION_REDACCION"))
                .andExpect(jsonPath("$.piezasRequeridas[0]").value("NULIDAD"))
                .andExpect(jsonPath("$.piezasGeneradas.length()").value(0));

        mvc.perform(post(B + "/actas/" + ID + "/acciones/generar-nulidad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_FIRMA_PIEZAS"));

        mvc.perform(get(B + "/actas/" + ID + "/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].id").value(DOC_NULIDAD))
                .andExpect(jsonPath("$[1].tipoDocumento").value("NULIDAD"))
                .andExpect(jsonPath("$[1].estadoDocumento").value("PENDIENTE_FIRMA"));

        mvc.perform(post(B + "/actas/" + ID + "/acciones/firmar-documento/" + DOC_NULIDAD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("CERRADAS"));

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("CERRADAS"))
                .andExpect(jsonPath("$.estaCerrada").value(true));

        mvc.perform(get(B + "/actas/" + ID + "/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].tipoDocumento").value("NULIDAD"))
                .andExpect(jsonPath("$[1].estadoDocumento").value("FIRMADO"));
    }
}
