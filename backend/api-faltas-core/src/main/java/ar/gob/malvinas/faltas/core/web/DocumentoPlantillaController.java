package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.command.AgregarFirmaReqPlantillaCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearDocumentoPlantillaCommand;
import ar.gob.malvinas.faltas.core.application.service.DocumentoPlantillaService;
import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoPlantillaDuplicadaException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoPlantillaInvalidaException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoPlantillaNoEncontradaException;
import ar.gob.malvinas.faltas.core.web.dto.AgregarFirmaReqPlantillaRequest;
import ar.gob.malvinas.faltas.core.web.dto.CrearDocumentoPlantillaRequest;
import ar.gob.malvinas.faltas.core.web.dto.DocumentoPlantillaFirmaReqResponse;
import ar.gob.malvinas.faltas.core.web.dto.DocumentoPlantillaResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/faltas/documentos/plantillas")
public class DocumentoPlantillaController {

    private final DocumentoPlantillaService service;

    public DocumentoPlantillaController(DocumentoPlantillaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<DocumentoPlantillaResponse> crear(
            @Valid @RequestBody CrearDocumentoPlantillaRequest req) {
        CrearDocumentoPlantillaCommand cmd = new CrearDocumentoPlantillaCommand(
                req.codigo(), req.nombre(),
                req.tipoDocu(), req.accionDocumental(), req.tipoActa(),
                req.tipoFirmaReq(), req.siRequiereNumeracion(), req.momentoNumeracionDocu(),
                req.siNotificable(), req.siGeneraPdf(), req.siSeleccionable(),
                req.fhVigDesde(), req.fhVigHasta(), req.idUserAlta());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DocumentoPlantillaResponse.from(service.crear(cmd)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentoPlantillaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(DocumentoPlantillaResponse.from(service.obtener(id)));
    }

    @GetMapping
    public ResponseEntity<List<DocumentoPlantillaResponse>> listar(
            @RequestParam(required = false) AccionDocumental accionDocumental,
            @RequestParam(required = false, defaultValue = "false") boolean soloActivas) {
        List<DocumentoPlantillaResponse> result;
        if (accionDocumental != null && soloActivas) {
            result = service.listarActivasPorAccion(accionDocumental)
                    .stream().map(DocumentoPlantillaResponse::from).toList();
        } else if (accionDocumental != null) {
            result = service.listarPorAccion(accionDocumental)
                    .stream().map(DocumentoPlantillaResponse::from).toList();
        } else {
            result = service.listar()
                    .stream().map(DocumentoPlantillaResponse::from).toList();
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/firma-req")
    public ResponseEntity<DocumentoPlantillaFirmaReqResponse> agregarFirmaReq(
            @PathVariable Long id,
            @Valid @RequestBody AgregarFirmaReqPlantillaRequest req) {
        AgregarFirmaReqPlantillaCommand cmd = new AgregarFirmaReqPlantillaCommand(
                id, req.seqFirmaReq(), req.rolFirmaReq(),
                req.mecanismoFirmaReq(), req.siObligatoria(), req.siActiva(), req.idUserAlta());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DocumentoPlantillaFirmaReqResponse.from(service.agregarFirmaReq(cmd)));
    }

    @GetMapping("/{id}/firma-req")
    public ResponseEntity<List<DocumentoPlantillaFirmaReqResponse>> listarFirmaReq(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                service.listarFirmaReq(id)
                        .stream().map(DocumentoPlantillaFirmaReqResponse::from).toList());
    }

    @PostMapping("/{id}/activar")
    public ResponseEntity<DocumentoPlantillaResponse> activar(@PathVariable Long id) {
        return ResponseEntity.ok(DocumentoPlantillaResponse.from(service.activar(id)));
    }

    @PostMapping("/{id}/desactivar")
    public ResponseEntity<DocumentoPlantillaResponse> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(DocumentoPlantillaResponse.from(service.desactivar(id)));
    }

}
