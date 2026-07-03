package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.command.CrearInspectorCommand;
import ar.gob.malvinas.faltas.core.application.command.VersionarInspectorCommand;
import ar.gob.malvinas.faltas.core.application.service.InspectorService;
import ar.gob.malvinas.faltas.core.application.service.TalonarioService;
import ar.gob.malvinas.faltas.core.domain.exception.InspectorNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalInspector;
import ar.gob.malvinas.faltas.core.domain.model.FalInspectorVersion;
import ar.gob.malvinas.faltas.core.repository.InspectorRepository;
import ar.gob.malvinas.faltas.core.web.dto.CrearInspectorRequest;
import ar.gob.malvinas.faltas.core.web.dto.InspectorResponse;
import ar.gob.malvinas.faltas.core.web.dto.InspectorVersionResponse;
import ar.gob.malvinas.faltas.core.web.dto.TalonarioInspectorResponse;
import ar.gob.malvinas.faltas.core.web.dto.VersionarInspectorRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/faltas/inspectores")
public class InspectorController {

    private final InspectorService inspectorService;
    private final InspectorRepository inspectorRepository;
    private final TalonarioService talonarioService;

    public InspectorController(InspectorService inspectorService,
                               InspectorRepository inspectorRepository,
                               TalonarioService talonarioService) {
        this.inspectorService = inspectorService;
        this.inspectorRepository = inspectorRepository;
        this.talonarioService = talonarioService;
    }

    @PostMapping
    public ResponseEntity<InspectorResponse> crear(@RequestBody CrearInspectorRequest req) {
        CrearInspectorCommand cmd = new CrearInspectorCommand(
                req.idUser(), req.legajoInsp(), req.nomInsp(),
                req.idDep(), req.verDep(), req.fhVigDesde(), req.idUserAlta());
        FalInspector insp = inspectorService.crear(cmd);
        List<FalInspectorVersion> versiones = inspectorRepository
                .findVersionesByInsp(insp.getIdInsp());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(InspectorResponse.de(insp, versiones));
    }

    @GetMapping
    public ResponseEntity<List<InspectorResponse>> listar() {
        List<InspectorResponse> respuestas = inspectorService.listarActivos().stream()
                .map(insp -> {
                    List<FalInspectorVersion> versiones = inspectorRepository
                            .findVersionesByInsp(insp.getIdInsp());
                    return InspectorResponse.de(insp, versiones);
                })
                .toList();
        return ResponseEntity.ok(respuestas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InspectorResponse> obtener(@PathVariable Long id) {
        FalInspector insp = inspectorService.obtener(id);
        List<FalInspectorVersion> versiones = inspectorRepository
                .findVersionesByInsp(insp.getIdInsp());
        return ResponseEntity.ok(InspectorResponse.de(insp, versiones));
    }

    @PutMapping("/{id}/versionar")
    public ResponseEntity<InspectorVersionResponse> versionar(
            @PathVariable Long id,
            @RequestBody VersionarInspectorRequest req) {
        VersionarInspectorCommand cmd = new VersionarInspectorCommand(
                id, req.legajoInsp(), req.nomInsp(),
                req.idDep(), req.verDep(), req.fhVigDesde(), req.idUserAlta());
        FalInspectorVersion version = inspectorService.versionar(cmd);
        return ResponseEntity.ok(InspectorVersionResponse.de(version));
    }

    @GetMapping("/{idInsp}/versiones/{verInsp}/talonarios")
    public ResponseEntity<List<TalonarioInspectorResponse>> listarTalonariosPorInspectorVersion(
            @PathVariable Long idInsp,
            @PathVariable short verInsp) {
        List<TalonarioInspectorResponse> respuestas =
                talonarioService.listarAsignacionesPorInspectorVersion(idInsp, verInsp)
                        .stream().map(TalonarioInspectorResponse::de).toList();
        return ResponseEntity.ok(respuestas);
    }

    @ExceptionHandler(InspectorNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(InspectorNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(PrecondicionVioladaException.class)
    public ResponseEntity<Map<String, String>> handlePrecondicion(PrecondicionVioladaException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", ex.getMessage()));
    }
}