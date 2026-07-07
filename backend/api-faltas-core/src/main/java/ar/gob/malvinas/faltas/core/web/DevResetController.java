package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.demo.DevInMemoryResetService;
import ar.gob.malvinas.faltas.core.web.dto.DevResetResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint dev/test de reset in-memory.
 *
 * POST /demo/dev/reset
 *
 * Solo disponible cuando faltas.demo.reset.enabled=true.
 * Si no esta habilitado, devuelve 404.
 *
 * No es un endpoint productivo.
 * No toca JDBC ni MariaDB.
 * No escribe archivos.
 * No borra datos reales.
 *
 * Property de habilitacion: faltas.demo.reset.enabled=true
 * Valor por defecto: false
 *
 * Slice 8F-5.
 */
@RestController
@RequestMapping("/demo")
public class DevResetController {

    @Value("${faltas.demo.reset.enabled:false}")
    private boolean resetEnabled;

    private final DevInMemoryResetService resetService;

    public DevResetController(DevInMemoryResetService resetService) {
        this.resetService = resetService;
    }

    /**
     * Limpia todos los repositorios in-memory y resembrada plantillas mock.
     * Disponible solo cuando faltas.demo.reset.enabled=true.
     */
    @PostMapping("/dev/reset")
    public ResponseEntity<DevResetResponse> resetInMemory() {
        if (!resetEnabled) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(resetService.ejecutarReset());
    }
}
