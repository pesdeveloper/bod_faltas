package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.command.MaterializarFirmaReqDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.service.DocumentoFirmaReqService;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoFirmaReqYaMaterializadaException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoPlantillaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoFirmaReq;
import ar.gob.malvinas.faltas.core.web.dto.DocumentoFirmaReqResponse;
import ar.gob.malvinas.faltas.core.web.dto.MaterializarFirmaReqDocumentoRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Endpoints REST para requisitos de firma de documentos concretos.
 *
 * POST /api/faltas/documentos/{documentoId}/firma-req/materializar
 * GET  /api/faltas/documentos/{documentoId}/firma-req
 * GET  /api/faltas/documentos/firma-req/{id}
 *
 * Slice 8C-4.
 */
@RestController
@RequestMapping("/api/faltas/documentos")
public class DocumentoFirmaReqController {

    private final DocumentoFirmaReqService service;

    public DocumentoFirmaReqController(DocumentoFirmaReqService service) {
        this.service = service;
    }

    @PostMapping("/{documentoId}/firma-req/materializar")
    public ResponseEntity<List<DocumentoFirmaReqResponse>> materializar(
            @PathVariable Long documentoId,
            @Valid @RequestBody MaterializarFirmaReqDocumentoRequest req) {
        MaterializarFirmaReqDocumentoCommand cmd =
                new MaterializarFirmaReqDocumentoCommand(documentoId, req.idUserAlta());
        List<FalDocumentoFirmaReq> resultado = service.materializarDesdePlantilla(cmd);
        List<DocumentoFirmaReqResponse> response = resultado.stream()
                .map(DocumentoFirmaReqResponse::from)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{documentoId}/firma-req")
    public ResponseEntity<List<DocumentoFirmaReqResponse>> listar(
            @PathVariable Long documentoId) {
        List<FalDocumentoFirmaReq> lista = service.listarPorDocumento(documentoId);
        List<DocumentoFirmaReqResponse> response = lista.stream()
                .map(DocumentoFirmaReqResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/firma-req/{id}")
    public ResponseEntity<DocumentoFirmaReqResponse> obtener(@PathVariable Long id) {
        FalDocumentoFirmaReq req = service.obtener(id);
        return ResponseEntity.ok(DocumentoFirmaReqResponse.from(req));
    }

    @ExceptionHandler(DocumentoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleDocNotFound(DocumentoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(DocumentoPlantillaNoEncontradaException.class)
    public ResponseEntity<Map<String, String>> handlePlantillaNotFound(DocumentoPlantillaNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(DocumentoFirmaReqYaMaterializadaException.class)
    public ResponseEntity<Map<String, String>> handleYaMaterializada(DocumentoFirmaReqYaMaterializadaException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(PrecondicionVioladaException.class)
    public ResponseEntity<Map<String, String>> handlePrecondicion(PrecondicionVioladaException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", ex.getMessage()));
    }
}
