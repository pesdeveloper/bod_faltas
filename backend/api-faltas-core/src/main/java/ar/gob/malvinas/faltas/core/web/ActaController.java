package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.command.CompletarCapturaCommand;
import ar.gob.malvinas.faltas.core.application.command.EnriquecerActaCommand;
import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvidencia;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.web.dto.CompletarCapturaRequest;
import ar.gob.malvinas.faltas.core.web.dto.EnriquecerActaRequest;
import ar.gob.malvinas.faltas.core.web.dto.LabrarActaRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/faltas/actas")
public class ActaController {

    private final ActaService actaService;
    private final ActaEventoRepository eventoRepository;

    public ActaController(ActaService actaService, ActaEventoRepository eventoRepository) {
        this.actaService = actaService;
        this.eventoRepository = eventoRepository;
    }

    @PostMapping("/labrar")
    public ResponseEntity<ComandoResultado> labrar(@Valid @RequestBody LabrarActaRequest req) {
        LabrarActaCommand cmd = new LabrarActaCommand(
                req.tipoActa(), req.idDependencia(), req.idInspector(),
                req.fechaActa(), req.domicilioHecho(), req.domicilioInfractor(),
                req.observaciones(), req.latInfr(), req.lonInfr(),
                req.infractorNombre(), req.infractorDocumento(),
                req.resultadoFirmaInfractor(), req.evidenciasActa()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(actaService.labrar(cmd));
    }

    @PostMapping("/{id}/completar-captura")
    public ResponseEntity<ComandoResultado> completarCaptura(
            @PathVariable Long id,
            @RequestBody(required = false) CompletarCapturaRequest req) {
        CompletarCapturaCommand cmd = new CompletarCapturaCommand(
                id, req != null ? req.observaciones() : null);
        return ResponseEntity.ok(actaService.completarCaptura(cmd));
    }

    @PostMapping("/{id}/enriquecer")
    public ResponseEntity<ComandoResultado> enriquecer(
            @PathVariable Long id,
            @RequestBody(required = false) EnriquecerActaRequest req) {
        EnriquecerActaCommand cmd = new EnriquecerActaCommand(
                id, req != null ? req.observaciones() : null);
        return ResponseEntity.ok(actaService.enriquecer(cmd));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FalActa> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(actaService.obtenerActa(id));
    }

    @GetMapping("/{id}/snapshot")
    public ResponseEntity<FalActaSnapshot> snapshot(@PathVariable Long id) {
        return ResponseEntity.ok(actaService.obtenerSnapshot(id));
    }

    @GetMapping("/{id}/timeline")
    public ResponseEntity<List<FalActaEvento>> timeline(@PathVariable Long id) {
        actaService.obtenerActa(id);
        return ResponseEntity.ok(eventoRepository.buscarPorActa(id));
    }

    @GetMapping("/{id}/evidencias")
    public ResponseEntity<List<FalActaEvidencia>> evidencias(@PathVariable Long id) {
        actaService.obtenerActa(id);
        return ResponseEntity.ok(actaService.listarEvidencias(id));
    }

}