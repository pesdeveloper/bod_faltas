package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
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
 * Escenario completo de ACTA-0022: pago voluntario + tres bloqueantes materiales.
 *
 * <p>Verifica que tras generar/firmar/cumplir cada bloqueante en forma
 * secuencial, los flags {@code resolucionBloqueante} y {@code cumplimientoMaterial}
 * se calculan por bloqueante de forma independiente, sin dead-end.
 *
 * <p>Secuencia principal:
 * <ol>
 *   <li>Confirmar pago voluntario (solicitud → informado → comprobante → confirmado).</li>
 *   <li>Generar → firmar → cumplir LEVANTAMIENTO_MEDIDA_PREVENTIVA.</li>
 *   <li>Generar → firmar resolución LIBERACION_RODADO (sin cumplir aún).</li>
 *   <li>Validar: {@code resolucionBloqueante=true} (ENTREGA_DOCUMENTACION sin resolutorio),
 *       {@code cumplimientoMaterial=true} (RODADO firmado, sin cumplimiento).</li>
 *   <li>Cumplir LIBERACION_RODADO.</li>
 *   <li>Validar: {@code resolucionBloqueante=true}, {@code cumplimientoMaterial=false}.</li>
 *   <li>Generar → firmar → cumplir ENTREGA_DOCUMENTACION.</li>
 *   <li>Validar: sin bloqueantes, {@code cierre=true}.</li>
 * </ol>
 */
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class Acta0022RecorridoMaterialesIT {

    private static final String B = "/api/prototipo";
    private static final String ID = "ACTA-0022";

    @Autowired
    private MockMvc mvc;

    /**
     * Reproduce el dead-end: tras firmar resolución de RODADO, ENTREGA_DOCUMENTACION
     * sigue sin resolutorio pero {@code resolucionBloqueante} no debe ser {@code false}.
     */
    @Test
    void acta0022_trasFirmaRodado_entregaDocumentacionSigueResoluble() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        // 1. Pago voluntario completo
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":9500.00}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-pago-informado"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/adjuntar-comprobante-pago-informado")
                        .param("nombreArchivo", "comprobante_0022.pdf"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/confirmar-pago-informado"))
                .andExpect(status().isOk());

        // ACTA-0022 empieza con 3 docs (00,01,02) → primer generado = DOC-0022-04

        // 2. Medida preventiva: resolutorio (DOC-0022-04) → firma → cumplimiento
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-resolucion-bloqueo-cierre")
                        .param("tipo", "LEVANTAMIENTO_MEDIDA_PREVENTIVA"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/firmar-documento/DOC-0022-04"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                        .param("tipo", "LEVANTAMIENTO_MEDIDA_PREVENTIVA"))
                .andExpect(status().isOk());

        // 3. Liberación rodado: resolutorio (DOC-0022-05) → firma (sin cumplir aún)
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-resolucion-bloqueo-cierre")
                        .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/firmar-documento/DOC-0022-05"))
                .andExpect(status().isOk());

        // 4. Validar: ENTREGA_DOCUMENTACION sin resolutorio → resolucionBloqueante=true
        //            RODADO firmado sin cumplimiento → cumplimientoMaterial=true
        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[?(@=='ENTREGA_DOCUMENTACION')]")
                        .isNotEmpty())
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[?(@=='LIBERACION_RODADO')]")
                        .isNotEmpty())
                .andExpect(jsonPath("$.accionesUi.resolucionBloqueante").value(true))
                .andExpect(jsonPath("$.accionesUi.cumplimientoMaterial").value(true));
    }

    /**
     * Tras cumplir LIBERACION_RODADO, solo ENTREGA_DOCUMENTACION queda pendiente
     * sin resolutorio: {@code resolucionBloqueante=true}, {@code cumplimientoMaterial=false}.
     */
    @Test
    void acta0022_trasCumplirRodado_entregaDocumentacionSoloResoluble() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":9500.00}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-pago-informado"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/adjuntar-comprobante-pago-informado")
                        .param("nombreArchivo", "comprobante_0022.pdf"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/confirmar-pago-informado"))
                .andExpect(status().isOk());

        // ACTA-0022: 3 docs pre-cargados → DOC-0022-04, DOC-0022-05

        // Medida preventiva completa
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-resolucion-bloqueo-cierre")
                        .param("tipo", "LEVANTAMIENTO_MEDIDA_PREVENTIVA"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/firmar-documento/DOC-0022-04"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                        .param("tipo", "LEVANTAMIENTO_MEDIDA_PREVENTIVA"))
                .andExpect(status().isOk());

        // Rodado: resolutorio → firma → cumplimiento
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-resolucion-bloqueo-cierre")
                        .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/firmar-documento/DOC-0022-05"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                        .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isOk());

        // Solo ENTREGA_DOCUMENTACION pendiente sin resolutorio
        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(1))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[0]").value("ENTREGA_DOCUMENTACION"))
                .andExpect(jsonPath("$.accionesUi.resolucionBloqueante").value(true))
                .andExpect(jsonPath("$.accionesUi.cumplimientoMaterial").value(false));
    }

    /**
     * Recorrido completo: pago + tres bloqueantes → cierre.
     */
    @Test
    void acta0022_recorridoCompleto_pagoYtresBloqueantesHastaCierre() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        // Pago
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":9500.00}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-pago-informado"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/adjuntar-comprobante-pago-informado")
                        .param("nombreArchivo", "comprobante_0022.pdf"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/confirmar-pago-informado"))
                .andExpect(status().isOk());

        // ACTA-0022: 3 docs pre-cargados → primer generado = DOC-0022-04

        // Medida preventiva (DOC-0022-04)
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-resolucion-bloqueo-cierre")
                        .param("tipo", "LEVANTAMIENTO_MEDIDA_PREVENTIVA"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/firmar-documento/DOC-0022-04"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                        .param("tipo", "LEVANTAMIENTO_MEDIDA_PREVENTIVA"))
                .andExpect(status().isOk());

        // Rodado (DOC-0022-05)
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-resolucion-bloqueo-cierre")
                        .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/firmar-documento/DOC-0022-05"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                        .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isOk());

        // Entrega documentación (DOC-0022-06)
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-resolucion-bloqueo-cierre")
                        .param("tipo", "ENTREGA_DOCUMENTACION"))
                .andExpect(status().isOk());

        // Validar acta en PENDIENTE_FIRMA con firmaPendiente=true
        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"))
                .andExpect(jsonPath("$.accionesUi.firmaPendiente").value(true));

        // Verificar documento
        mvc.perform(get(B + "/actas/" + ID + "/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.tipoDocumento=='DOC_RESTITUCION_DOCUMENTACION')].estadoDocumento")
                        .value("PENDIENTE_FIRMA"));

        // Firmar
        mvc.perform(post(B + "/actas/" + ID + "/acciones/firmar-documento/DOC-0022-06"))
                .andExpect(status().isOk());

        // Registrar entrega efectiva
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                        .param("tipo", "ENTREGA_DOCUMENTACION"))
                .andExpect(status().isOk());

        // Sin bloqueantes, cierre disponible
        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(true))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(0))
                .andExpect(jsonPath("$.accionesUi.cierre").value(true));

        // Cerrar
        mvc.perform(post(B + "/actas/" + ID + "/acciones/cerrar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("CERRADAS"));
    }
}
