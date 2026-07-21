package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.demo.DemoHealthService;
import ar.gob.malvinas.faltas.core.web.dto.DemoHealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de health/readiness para el frontend demo.
 *
 * GET /demo/health devuelve 200 UP cuando el modulo demo esta disponible.
 * No ejecuta efectos destructivos. No llama HTTP contra si mismo.
 * No invoca reset. No genera documentos.
 *
 * Slice 8F-8 - cierre GAP-8.
 */
@ConditionalOnProperty(name = "faltas.demo.enabled", havingValue = "true")
@RestController
@RequestMapping("/demo")
public class DemoHealthController {

    private final DemoHealthService healthService;

    public DemoHealthController(DemoHealthService healthService) {
        this.healthService = healthService;
    }

    /**
     * Evalua la readiness del modulo demo y devuelve el estado de cada check.
     *
     * @return 200 con DemoHealthResponse siempre que el modulo este iniciado.
     */
    @GetMapping("/health")
    public ResponseEntity<DemoHealthResponse> health() {
        return ResponseEntity.ok(healthService.evaluar());
    }
}
