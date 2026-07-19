package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.result.DocumentoRenderizadoMock;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoRedaccionDocumento;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoRedaccion;
import org.springframework.stereotype.Service;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

/**
 * Renderer mock de documentos PDF.
 *
 * Genera una representacion textual deterministica del contenido redactado,
 * sin usar librerias PDF reales (no iText, no PDFBox, no OpenPDF).
 * No escribe archivos fisicos. No accede a storage real.
 *
 * El resultado incluye:
 *   - Marca clara de PDF MOCK
 *   - idDocumento, idRedaccion
 *   - fecha/hora de generacion
 *   - tipo documental
 *   - contenido editable confirmado
 *   - hash SHA-256 deterministico del contenido mock
 *   - storageKey esquema mock://
 *
 * Solo puede renderizar redacciones CONFIRMADAS.
 * No modifica FalDocumento directamente (renderer puro).
 *
 * Slice 8F-3.
 */
@Service
public class DocumentoPdfMockRenderer {

    static final String MIME_TYPE = "application/x-faltas-pdf-mock";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private final FaltasClock faltasClock;

    public DocumentoPdfMockRenderer(FaltasClock faltasClock) {
        this.faltasClock = faltasClock;
    }

    public DocumentoRenderizadoMock renderizar(FalDocumento documento, FalDocumentoRedaccion redaccion) {
        if (documento == null) throw new IllegalArgumentException("documento requerido");
        if (redaccion == null) throw new IllegalArgumentException("redaccion requerida");
        if (redaccion.getEstadoRedaccion() != EstadoRedaccionDocumento.CONFIRMADA) {
            throw new PrecondicionVioladaException(
                    "Solo se puede renderizar una redaccion CONFIRMADA. Estado actual: "
                    + redaccion.getEstadoRedaccion());
        }

        LocalDateTime fhGeneracion = faltasClock.now();
        String contenidoMock = buildContenidoMock(documento, redaccion, fhGeneracion);
        String hashMock = calcularHash(contenidoMock);
        long sizeBytes = contenidoMock.getBytes(StandardCharsets.UTF_8).length;
        String storageKeyMock = buildStorageKey(documento.getId(), redaccion.getId());
        String nombreArchivo = "documento-" + documento.getId()
                + "-redaccion-" + redaccion.getId() + ".mock.pdf";

        return new DocumentoRenderizadoMock(
                documento.getId(),
                redaccion.getId(),
                nombreArchivo,
                MIME_TYPE,
                storageKeyMock,
                hashMock,
                contenidoMock,
                fhGeneracion,
                sizeBytes);
    }

    private String buildContenidoMock(
            FalDocumento documento,
            FalDocumentoRedaccion redaccion,
            LocalDateTime fhGeneracion) {
        String tipoDocu = documento.getTipoDocu() != null
                ? documento.getTipoDocu().name()
                : "DESCONOCIDO";
        return "[PDF MOCK - SISTEMA DE FALTAS]\n" +
               "===============================\n" +
               "Documento ID   : " + documento.getId() + "\n" +
               "Redaccion ID   : " + redaccion.getId() + "\n" +
               "Tipo documental: " + tipoDocu + "\n" +
               "Generado       : " + fhGeneracion.format(FMT) + "\n" +
               "Confirmado por : " + redaccion.getIdUserConfirmacion() + "\n" +
               "===============================\n" +
               redaccion.getContenidoEditable() + "\n" +
               "===============================\n" +
               "[FIN PDF MOCK]";
    }

    public static String calcularHash(String contenido) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(contenido.getBytes(StandardCharsets.UTF_8));
            return "sha256-mock-" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    public static String buildStorageKey(Long documentoId, Long redaccionId) {
        return "mock://documentos/" + documentoId
                + "/redacciones/" + redaccionId
                + "/documento-final.pdf";
    }
}
