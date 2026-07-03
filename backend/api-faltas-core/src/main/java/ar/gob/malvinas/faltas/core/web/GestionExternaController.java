package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.command.DerivarGestionExternaCommand;
import ar.gob.malvinas.faltas.core.application.command.ReingresarDesdeGestionExternaCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarPagoExternoGestionCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.GestionExternaService;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.GestionExternaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalGestionExterna;
import ar.gob.malvinas.faltas.core.web.dto.DerivarGestionExternaRequest;
import ar.gob.malvinas.faltas.core.web.dto.ReingresarDesdeGestionExternaRequest;
import ar.gob.malvinas.faltas.core.web.dto.RegistrarPagoExternoGestionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/faltas/actas")
public class GestionExternaController {

    private final GestionExternaService gestionExternaService;

    public GestionExternaController(GestionExternaService gestionExternaService) {
        this.gestionExternaService = gestionExternaService;
    }

    @PostMapping("/{id}/gestion-externa/derivar")
    public ResponseEntity<ComandoResultado> derivar(
            @PathVariable Long id,
            @RequestBody DerivarGestionExternaRequest req) {
        DerivarGestionExternaCommand cmd = new DerivarGestionExternaCommand(
                id, req.tipoGestionExterna(), req.motivoDerivacion(), req.observaciones());
        return ResponseEntity.ok(gestionExternaService.derivar(cmd));
    }

    @PostMapping("/{id}/gestion-externa/reingresar")
    public ResponseEntity<ComandoResultado> reingresar(
            @PathVariable Long id,
            @RequestBody ReingresarDesdeGestionExternaRequest req) {
        ReingresarDesdeGestionExternaCommand cmd = new ReingresarDesdeGestionExternaCommand(
                id,
                req.modoReingresoGestionExterna(),
                req.motivoReingreso(),
                req.resultadoGestionExterna(),
                req.observaciones(),
                req.montoResultado());
        return ResponseEntity.ok(gestionExternaService.reingresar(cmd));
    }


    @PostMapping("/{id}/gestion-externa/pago-externo")
    public ResponseEntity<ComandoResultado> registrarPagoExterno(
            @PathVariable Long id,
            @RequestBody RegistrarPagoExternoGestionRequest req) {
        RegistrarPagoExternoGestionCommand cmd = new RegistrarPagoExternoGestionCommand(
                id, req.observaciones());
        return ResponseEntity.ok(gestionExternaService.registrarPagoExternoGestion(cmd));
    }
    @GetMapping("/{id}/gestion-externa")
    public ResponseEntity<FalGestionExterna> obtener(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(gestionExternaService.obtenerGestionActiva(id));
        } catch (GestionExternaNoEncontradaException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @ExceptionHandler(ActaNoEncontradaException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ActaNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(PrecondicionVioladaException.class)
    public ResponseEntity<Map<String, String>> handlePrecondicion(PrecondicionVioladaException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", ex.getMessage()));
    }
}
