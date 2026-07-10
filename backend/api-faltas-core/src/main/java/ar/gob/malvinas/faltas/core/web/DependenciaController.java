package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.command.CrearDependenciaCommand;
import ar.gob.malvinas.faltas.core.application.command.VersionarDependenciaCommand;
import ar.gob.malvinas.faltas.core.application.service.DependenciaService;
import ar.gob.malvinas.faltas.core.domain.exception.DependenciaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalDependencia;
import ar.gob.malvinas.faltas.core.domain.model.FalDependenciaVersion;
import ar.gob.malvinas.faltas.core.repository.DependenciaRepository;
import ar.gob.malvinas.faltas.core.web.dto.CrearDependenciaRequest;
import ar.gob.malvinas.faltas.core.web.dto.DependenciaResponse;
import ar.gob.malvinas.faltas.core.web.dto.DependenciaVersionResponse;
import ar.gob.malvinas.faltas.core.web.dto.VersionarDependenciaRequest;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/faltas/dependencias")
public class DependenciaController {

    private final DependenciaService dependenciaService;
    private final DependenciaRepository dependenciaRepository;

    private final FaltasClock faltasClock;

    public DependenciaController(DependenciaService dependenciaService,
                                  DependenciaRepository dependenciaRepository, FaltasClock faltasClock) {
        this.faltasClock = faltasClock;
        this.dependenciaService = dependenciaService;
        this.dependenciaRepository = dependenciaRepository;
    }

    @PostMapping
    public ResponseEntity<DependenciaResponse> crear(@RequestBody CrearDependenciaRequest req) {
        CrearDependenciaCommand cmd = new CrearDependenciaCommand(
                req.codDep(), req.nomDep(), req.idDepPadre(),
                req.tipoActa(), req.fhVigDesde(), req.idUserAlta());
        FalDependencia dep = dependenciaService.crear(cmd);
        FalDependenciaVersion version = dependenciaRepository
                .findVersionVigente(dep.getIdDep(), faltasClock.now().toLocalDate()).orElse(null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DependenciaResponse.de(dep, version));
    }

    @GetMapping
    public ResponseEntity<List<DependenciaResponse>> listar() {
        List<DependenciaResponse> respuestas = dependenciaService.listarActivas().stream()
                .map(dep -> {
                    FalDependenciaVersion version = dependenciaRepository
                            .findVersionVigente(dep.getIdDep(), faltasClock.now().toLocalDate()).orElse(null);
                    return DependenciaResponse.de(dep, version);
                })
                .toList();
        return ResponseEntity.ok(respuestas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DependenciaResponse> obtener(@PathVariable Long id) {
        FalDependencia dep = dependenciaService.obtener(id);
        FalDependenciaVersion version = dependenciaRepository
                .findVersionVigente(dep.getIdDep(), faltasClock.now().toLocalDate()).orElse(null);
        return ResponseEntity.ok(DependenciaResponse.de(dep, version));
    }

    @PutMapping("/{id}/versionar")
    public ResponseEntity<DependenciaVersionResponse> versionar(
            @PathVariable Long id,
            @RequestBody VersionarDependenciaRequest req) {
        VersionarDependenciaCommand cmd = new VersionarDependenciaCommand(
                id, req.nomDep(), req.idDepPadre(),
                req.tipoActa(), req.fhVigDesde(), req.idUserAlta());
        FalDependenciaVersion version = dependenciaService.versionar(cmd);
        return ResponseEntity.ok(DependenciaVersionResponse.de(version));
    }

}
