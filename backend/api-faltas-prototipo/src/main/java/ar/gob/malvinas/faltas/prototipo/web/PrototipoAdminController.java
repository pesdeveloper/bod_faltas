package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.store.MockDataFactory;
import ar.gob.malvinas.faltas.prototipo.store.PrototipoStore;
import ar.gob.malvinas.faltas.prototipo.web.dto.ActaDetalleResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.CrearActaMockDemoRequest;
import ar.gob.malvinas.faltas.prototipo.web.mapper.ActaDetalleMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Endpoints administrativos y de demo del prototipo.
 *
 * <p>Rutas públicas mantenidas exactamente:
 * <ul>
 *   <li>{@code GET  /api/prototipo/health}
 *   <li>{@code POST /api/prototipo/reset}
 *   <li>{@code POST /api/prototipo/actas/mock}
 * </ul>
 */
@RestController
@RequestMapping("/api/prototipo")
public class PrototipoAdminController {

    private final PrototipoStore store;
    private final MockDataFactory mockDataFactory;
    private final ActaDetalleMapper actaDetalleMapper;

    public PrototipoAdminController(
            PrototipoStore store,
            MockDataFactory mockDataFactory,
            ActaDetalleMapper actaDetalleMapper) {
        this.store = store;
        this.mockDataFactory = mockDataFactory;
        this.actaDetalleMapper = actaDetalleMapper;
    }

    @GetMapping("/health")
    public PrototipoHealthResponse health() {
        return new PrototipoHealthResponse(
                "UP",
                "api-faltas-prototipo",
                "mock-en-memoria",
                store.getActas().size());
    }

    @PostMapping("/reset")
    public PrototipoResetResponse reset() {
        mockDataFactory.loadInitialData(store);
        return new PrototipoResetResponse(
                "OK",
                "Escenario mock reinicializado",
                store.getActas().size());
    }

    /**
     * Alta mínima de acta mock en vivo (demo), numeración {@code ACTA-DEMO-nnnn}.
     * Consistente con anclas y proyección material del prototipo in-memory.
     */
    @PostMapping("/actas/mock")
    @ResponseStatus(HttpStatus.CREATED)
    public ActaDetalleResponse crearActaMockDemo(@RequestBody CrearActaMockDemoRequest request) {
        PrototipoStore.CrearActaMockDemoResultado r = store.crearActaMockDemo(request);
        if (r.estado() == PrototipoStore.CrearActaMockDemoEstado.BAD_REQUEST) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, r.mensaje() != null ? r.mensaje() : "Solicitud inválida (demo).");
        }
        return actaDetalleMapper.map(r.acta());
    }
}
