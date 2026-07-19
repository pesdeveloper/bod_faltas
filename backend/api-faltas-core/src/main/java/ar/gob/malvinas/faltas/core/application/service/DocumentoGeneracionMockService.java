package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.ConfirmarRedaccionYGenerarDocumentoMockCommand;
import ar.gob.malvinas.faltas.core.application.result.DocumentoGeneracionMockResponse;
import ar.gob.malvinas.faltas.core.application.result.DocumentoRenderizadoMock;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoRedaccionDocumento;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoRedaccionNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoRedaccion;
import ar.gob.malvinas.faltas.core.repository.DocumentoRedaccionRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRepository;
import org.springframework.stereotype.Service;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;

import java.time.LocalDateTime;

/**
 * Servicio de generacion final simulada de documentos.
 *
 * Orquesta el flujo:
 *   BORRADOR/REABIERTA -> confirmar redaccion -> render mock -> setear metadatos en FalDocumento
 *
 * Guardrails:
 *   - No emite el documento automaticamente.
 *   - No envia a firma.
 *   - No notifica.
 *   - No escribe archivos fisicos.
 *   - No usa storage real.
 *   - storageKey/hashDocu/fhGeneracion se setean con valores mock deterministicos.
 *   - Solo permite generar desde BORRADOR o REABIERTA (no ANULADA, no ya CONFIRMADA).
 *
 * Slice 8F-3.
 */
@Service
public class DocumentoGeneracionMockService {

    private final DocumentoRedaccionRepository redaccionRepository;
    private final DocumentoRepository documentoRepository;
    private final DocumentoPdfMockRenderer renderer;
    private final FaltasClock faltasClock;

    public DocumentoGeneracionMockService(
            DocumentoRedaccionRepository redaccionRepository,
            DocumentoRepository documentoRepository,
            DocumentoPdfMockRenderer renderer,
            FaltasClock faltasClock) {
        this.faltasClock = faltasClock;
        this.redaccionRepository = redaccionRepository;
        this.documentoRepository = documentoRepository;
        this.renderer = renderer;
    }

    /**
     * Confirma una redaccion BORRADOR o REABIERTA y genera la salida mock final.
     *
     * Setea storageKey, hashDocu y fhGeneracion en FalDocumento con valores simulados.
     * No emite, no firma, no notifica.
     */
    public DocumentoGeneracionMockResponse confirmarYGenerarMockPdf(
            ConfirmarRedaccionYGenerarDocumentoMockCommand cmd) {

        FalDocumentoRedaccion redaccion = redaccionRepository.buscarPorId(cmd.redaccionId())
                .orElseThrow(() -> new DocumentoRedaccionNoEncontradaException(cmd.redaccionId()));

        FalDocumento documento = documentoRepository.buscarPorId(redaccion.getIdDocumento())
                .orElseThrow(() -> new DocumentoNoEncontradoException(redaccion.getIdDocumento()));

        validarEstadoParaGeneracion(redaccion);

        LocalDateTime fhOperacion = cmd.fhOperacion() != null
                ? cmd.fhOperacion()
                : faltasClock.now();

        redaccion.confirmar(fhOperacion, cmd.idUserOperacion());

        DocumentoRenderizadoMock renderizado = renderer.renderizar(documento, redaccion);

        documento.setStorageKey(renderizado.storageKeyMock());
        documento.setHashDocu(renderizado.hashMock());
        documento.setFhGeneracion(renderizado.fhGeneracion());

        redaccionRepository.guardar(redaccion);
        documentoRepository.guardar(documento);

        return new DocumentoGeneracionMockResponse(
                documento.getId(),
                redaccion.getId(),
                redaccion.getEstadoRedaccion(),
                documento.getStorageKey(),
                documento.getHashDocu(),
                documento.getFhGeneracion(),
                renderizado.nombreArchivo(),
                renderizado.mimeType(),
                renderizado.sizeBytes(),
                true);
    }

    private void validarEstadoParaGeneracion(FalDocumentoRedaccion redaccion) {
        EstadoRedaccionDocumento estado = redaccion.getEstadoRedaccion();
        if (estado == EstadoRedaccionDocumento.ANULADA) {
            throw new PrecondicionVioladaException(
                    "No se puede generar desde una redaccion ANULADA.");
        }
        if (estado == EstadoRedaccionDocumento.CONFIRMADA) {
            throw new PrecondicionVioladaException(
                    "La redaccion ya esta CONFIRMADA. No se puede generar dos veces sin reabrir.");
        }
    }
}
