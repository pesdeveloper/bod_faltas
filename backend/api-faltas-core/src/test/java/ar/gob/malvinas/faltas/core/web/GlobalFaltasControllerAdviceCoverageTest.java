package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.domain.exception.ActaArticuloInfringidoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.ActaContravencionNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.ActaDocumentoNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.ActaDocumentoYaExisteException;
import ar.gob.malvinas.faltas.core.domain.exception.ActaMedidaPreventivaAplicadaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.ActaSustanciasNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.ActaTransitoAlcoholemiaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.ActaTransitoNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.ActaVehiculoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.AcuseDuplicadoException;
import ar.gob.malvinas.faltas.core.domain.exception.ArticuloNormativaNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.BloqueanteMaterialNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.ConciliacionIncompatibleException;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.exception.DependenciaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoFirmaReqYaMaterializadaException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoPlantillaDuplicadaException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoPlantillaInvalidaException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoPlantillaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoRedaccionNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.DomicilioPersonaNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.FirmanteNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.FormaPagoNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.GestionExternaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.InspectorNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.LoteCodigoDuplicadoException;
import ar.gob.malvinas.faltas.core.domain.exception.LoteCorreoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.MedidaPreventivaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.MotivoArchivoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.MovimientoPagoDuplicadoException;
import ar.gob.malvinas.faltas.core.domain.exception.NormativaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.NotificacionAcuseNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.NotificacionIntentoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.NotificacionNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.ObligacionPagoNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PagoMovimientoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.PersonaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PlanPagoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.PlantillaContenidoAmbiguaException;
import ar.gob.malvinas.faltas.core.domain.exception.PlantillaContenidoNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PlantillaDefaultAmbiguaException;
import ar.gob.malvinas.faltas.core.domain.exception.PlantillaDefaultNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.exception.QrTokenInvalidoException;
import ar.gob.malvinas.faltas.core.domain.exception.ResolucionPagoAnteriorConflictoException;
import ar.gob.malvinas.faltas.core.domain.exception.RubroVersionNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.TarifarioNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.ValorizacionNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.VehiculoMarcaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.VehiculoModeloNoEncontradoException;
import ar.gob.malvinas.faltas.core.web.error.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que las 52 excepciones de dominio tengan mapping en GlobalFaltasControllerAdvice.
 */
@DisplayName("GlobalFaltasControllerAdvice — cobertura de las 52 excepciones de dominio")
class GlobalFaltasControllerAdviceCoverageTest {

    private final GlobalFaltasControllerAdvice advice = new GlobalFaltasControllerAdvice();

    private List<ExceptionMapping> catalogo() {
        return List.of(
            // 404 — ACTA
            m(new ActaNoEncontradaException(1L),                              "ACTA_NO_ENCONTRADA",   404),
            m(new ActaTransitoNoEncontradaException(1L),                      "ACTA_NO_ENCONTRADA",   404),
            m(new ActaContravencionNoEncontradaException(1L),                 "ACTA_NO_ENCONTRADA",   404),
            m(new ActaArticuloInfringidoNoEncontradoException(1L),            "ACTA_NO_ENCONTRADA",   404),
            m(new ActaMedidaPreventivaAplicadaNoEncontradaException(1L),      "ACTA_NO_ENCONTRADA",   404),
            m(new ActaSustanciasNoEncontradaException(1L),                    "ACTA_NO_ENCONTRADA",   404),
            m(new ActaTransitoAlcoholemiaNoEncontradaException(1L),           "ACTA_NO_ENCONTRADA",   404),
            m(new ActaVehiculoNoEncontradoException(1L),                      "ACTA_NO_ENCONTRADA",   404),
            m(new ActaDocumentoNoEncontradaException(1L, 2L),                 "ACTA_NO_ENCONTRADA",   404),
            // 404 — DOCUMENTO
            m(new DocumentoNoEncontradoException("doc-1"),                    "DOCUMENTO_NO_ENCONTRADO", 404),
            // 404 — PLANTILLA
            m(new DocumentoPlantillaNoEncontradaException("cod"),             "PLANTILLA_NO_ENCONTRADA", 404),
            m(new DocumentoRedaccionNoEncontradaException(1L),                "PLANTILLA_NO_ENCONTRADA", 404),
            m(new PlantillaContenidoNoEncontradaException(1L),                "PLANTILLA_NO_ENCONTRADA", 404),
            m(new PlantillaDefaultNoEncontradaException("msg"),               "PLANTILLA_NO_ENCONTRADA", 404),
            // 404 — PERSONA
            m(new PersonaNoEncontradaException(1L),                           "PERSONA_NO_ENCONTRADA",   404),
            m(new DomicilioPersonaNoEncontradoException(1L),                  "PERSONA_NO_ENCONTRADA",   404),
            // 404 — INSPECTOR / FIRMANTE
            m(new InspectorNoEncontradoException(1L),                         "INSPECTOR_NO_ENCONTRADO", 404),
            m(new FirmanteNoEncontradoException(1L),                          "INSPECTOR_NO_ENCONTRADO", 404),
            // 404 — DEPENDENCIA
            m(new DependenciaNoEncontradaException(1L),                       "DEPENDENCIA_NO_ENCONTRADA", 404),
            // 404 — NORMATIVA
            m(new NormativaNoEncontradaException(1L),                         "NORMATIVA_NO_ENCONTRADA", 404),
            m(new ArticuloNormativaNoEncontradoException(1L),                 "NORMATIVA_NO_ENCONTRADA", 404),
            // 404 — NOTIFICACION
            m(new NotificacionNoEncontradaException("notif-1"),               "NOTIFICACION_NO_ENCONTRADA", 404),
            m(new NotificacionAcuseNoEncontradoException(1L),                 "NOTIFICACION_NO_ENCONTRADA", 404),
            m(new NotificacionIntentoNoEncontradoException(1L),               "NOTIFICACION_NO_ENCONTRADA", 404),
            // 404 — PAGO
            m(new FormaPagoNoEncontradaException(1L),                         "PAGO_NO_ENCONTRADO", 404),
            m(new ObligacionPagoNoEncontradaException(1L),                    "PAGO_NO_ENCONTRADO", 404),
            m(new PagoMovimientoNoEncontradoException(1L),                    "PAGO_NO_ENCONTRADO", 404),
            m(new PlanPagoNoEncontradoException(1L),                          "PAGO_NO_ENCONTRADO", 404),
            m(new ValorizacionNoEncontradaException(1L),                      "PAGO_NO_ENCONTRADO", 404),
            // 404 — RECURSO
            m(new BloqueanteMaterialNoEncontradoException("bm-1"),            "RECURSO_NO_ENCONTRADO", 404),
            m(new GestionExternaNoEncontradaException(1L),                    "RECURSO_NO_ENCONTRADO", 404),
            m(new LoteCorreoNoEncontradoException(1L),                        "RECURSO_NO_ENCONTRADO", 404),
            m(new MedidaPreventivaNoEncontradaException("mp-1"),              "RECURSO_NO_ENCONTRADO", 404),
            m(new MotivoArchivoNoEncontradoException("ma-1"),                 "RECURSO_NO_ENCONTRADO", 404),
            m(new RubroVersionNoEncontradoException("rv-1"),                  "RECURSO_NO_ENCONTRADO", 404),
            m(new TarifarioNoEncontradoException(1L),                         "RECURSO_NO_ENCONTRADO", 404),
            m(new VehiculoMarcaNoEncontradaException("vm-1"),                 "RECURSO_NO_ENCONTRADO", 404),
            m(new VehiculoModeloNoEncontradoException("vmo-1"),               "RECURSO_NO_ENCONTRADO", 404),
            // 422 — PRECONDICION
            m(new PrecondicionVioladaException("estado invalido"),            "PRECONDICION_VIOLADA",  422),
            m(new DocumentoPlantillaInvalidaException("fallo"),               "PLANTILLA_INVALIDA",    422),
            m(new PlantillaContenidoAmbiguaException(1L, 3),                  "PLANTILLA_INVALIDA",    422),
            m(new PlantillaDefaultAmbiguaException("ambigua"),                "PLANTILLA_INVALIDA",    422),
            // 409 — CONFLICTO
            m(new ConcurrenciaConflictoException("Acta", 1L, 1, 2),          "CONFLICTO_CONCURRENCIA",    409),
            m(new MovimientoPagoDuplicadoException("ref-1"),                  "MOVIMIENTO_PAGO_DUPLICADO", 409),
            m(new ConciliacionIncompatibleException("incompatible"),          "CONCILIACION_INCOMPATIBLE", 409),
            m(new ResolucionPagoAnteriorConflictoException("conflicto"),      "RESOLUCION_PAGO_ANTERIOR_CONFLICTO", 409),
            m(new AcuseDuplicadoException(1L, 2L, "POSTAL"),                  "CONFLICTO_DUPLICADO",       409),
            m(new ActaDocumentoYaExisteException(1L, 2L),                     "CONFLICTO_DUPLICADO",       409),
            m(new DocumentoFirmaReqYaMaterializadaException(1L),              "CONFLICTO_DUPLICADO",       409),
            m(new DocumentoPlantillaDuplicadaException("codigo"),             "CONFLICTO_DUPLICADO",       409),
            m(new LoteCodigoDuplicadoException("lote-1"),                     "CONFLICTO_DUPLICADO",       409),
            // 400
            m(new QrTokenInvalidoException(),                                 "QR_TOKEN_INVALIDO",     400)
        );
    }

    @Test
    @DisplayName("Las 52 excepciones de dominio producen codigoError valido y status HTTP correcto")
    void todas_las_excepciones_producen_codigo_y_status_valido() {
        List<ExceptionMapping> lista = catalogo();
        assertThat(lista).as("Catalogo debe tener exactamente 52 entradas").hasSize(52);

        for (ExceptionMapping mapping : lista) {
            ResponseEntity<ErrorResponse> resp = invocarAdvice(mapping.ex());
            String nombre = mapping.ex().getClass().getSimpleName();

            assertThat(resp).as("Response para " + nombre).isNotNull();
            assertThat(resp.getBody()).as("Body para " + nombre).isNotNull();
            assertThat(resp.getBody().codigoError())
                    .as("codigoError para " + nombre)
                    .isEqualTo(mapping.codigoEsperado());
            assertThat(resp.getStatusCode().value())
                    .as("HTTP status para " + nombre)
                    .isEqualTo(mapping.statusEsperado());
        }
    }

    @Test
    @DisplayName("NOT_FOUND_CODES cubre las 38 excepciones de recurso no encontrado")
    void notFoundCodes_cubre_38_excepciones() {
        Map<Class<?>, String> codes = GlobalFaltasControllerAdvice.NOT_FOUND_CODES;
        assertThat(codes).hasSize(38);
        codes.forEach((clazz, codigo) ->
            assertThat(codigo).as("Codigo para " + clazz.getSimpleName())
                    .isNotBlank()
                    .matches("[A-Z0-9_]+")
        );
    }

    @Test
    @DisplayName("Todos los codigos del catalogo tienen formato A-Z0-9_")
    void todos_codigos_formato_valido() {
        for (ExceptionMapping mapping : catalogo()) {
            assertThat(mapping.codigoEsperado())
                    .as("Codigo para " + mapping.ex().getClass().getSimpleName())
                    .matches("[A-Z0-9_]+");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> invocarAdvice(RuntimeException ex) {
        if (GlobalFaltasControllerAdvice.NOT_FOUND_CODES.containsKey(ex.getClass())) {
            return advice.handleNotFound(ex);
        }
        if (ex instanceof PrecondicionVioladaException p) return advice.handlePrecondicion(p);
        if (ex instanceof DocumentoPlantillaInvalidaException
                || ex instanceof PlantillaContenidoAmbiguaException
                || ex instanceof PlantillaDefaultAmbiguaException) {
            return advice.handlePlantillaInvalida(ex);
        }
        if (ex instanceof ConcurrenciaConflictoException c) return advice.handleOcc(c);
        if (ex instanceof MovimientoPagoDuplicadoException mv) return advice.handleMovimientoDuplicado(mv);
        if (ex instanceof ConciliacionIncompatibleException ci) return advice.handleConciliacion(ci);
        if (ex instanceof ResolucionPagoAnteriorConflictoException rc) return advice.handleResolucionPagoAnteriorConflicto(rc);
        if (ex instanceof AcuseDuplicadoException
                || ex instanceof ActaDocumentoYaExisteException
                || ex instanceof DocumentoFirmaReqYaMaterializadaException
                || ex instanceof DocumentoPlantillaDuplicadaException
                || ex instanceof LoteCodigoDuplicadoException) {
            return advice.handleDuplicado(ex);
        }
        if (ex instanceof QrTokenInvalidoException q) return advice.handleQrToken(q);
        return advice.handleInterno(ex);
    }

    private static ExceptionMapping m(RuntimeException ex, String codigo, int status) {
        return new ExceptionMapping(ex, codigo, status);
    }

    private record ExceptionMapping(RuntimeException ex, String codigoEsperado, int statusEsperado) {}
}
