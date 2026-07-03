package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.command.AgregarHabilitacionFirmanteCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearFirmanteCommand;
import ar.gob.malvinas.faltas.core.application.command.DesactivarHabilitacionFirmanteCommand;
import ar.gob.malvinas.faltas.core.application.command.VersionarFirmanteCommand;
import ar.gob.malvinas.faltas.core.application.service.FirmanteService;
import ar.gob.malvinas.faltas.core.domain.exception.FirmanteNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalFirmante;
import ar.gob.malvinas.faltas.core.domain.model.FalFirmanteVersion;
import ar.gob.malvinas.faltas.core.domain.model.FalFirmanteVersionHabilitacion;
import ar.gob.malvinas.faltas.core.repository.FirmanteRepository;
import ar.gob.malvinas.faltas.core.web.dto.AgregarHabilitacionFirmanteRequest;
import ar.gob.malvinas.faltas.core.web.dto.DesactivarHabilitacionFirmanteRequest;
import ar.gob.malvinas.faltas.core.web.dto.CrearFirmanteRequest;
import ar.gob.malvinas.faltas.core.web.dto.FirmanteHabilitacionResponse;
import ar.gob.malvinas.faltas.core.web.dto.FirmanteResponse;
import ar.gob.malvinas.faltas.core.web.dto.FirmanteVersionResponse;
import ar.gob.malvinas.faltas.core.web.dto.VersionarFirmanteRequest;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/faltas/firmantes")
public class FirmanteController {

    private final FirmanteService firmanteService;
    private final FirmanteRepository firmanteRepository;

    public FirmanteController(FirmanteService firmanteService,
                               FirmanteRepository firmanteRepository) {
        this.firmanteService = firmanteService;
        this.firmanteRepository = firmanteRepository;
    }

    @PostMapping
    public ResponseEntity<FirmanteResponse> crear(@RequestBody CrearFirmanteRequest req) {
        CrearFirmanteCommand cmd = new CrearFirmanteCommand(
                req.idUser(), req.nomFirmante(), req.rolFirmante(), req.cargoFirmante(),
                req.idDep(), req.verDep(), req.fhVigDesde(), req.idUserAlta());
        FalFirmante firmante = firmanteService.crear(cmd);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(buildResponse(firmante));
    }

    @GetMapping
    public ResponseEntity<List<FirmanteResponse>> listar() {
        List<FirmanteResponse> respuestas = firmanteService.listarActivos().stream()
                .map(this::buildResponse)
                .toList();
        return ResponseEntity.ok(respuestas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FirmanteResponse> obtener(@PathVariable Long id) {
        FalFirmante firmante = firmanteService.obtener(id);
        return ResponseEntity.ok(buildResponse(firmante));
    }

    @PutMapping("/{id}/versionar")
    public ResponseEntity<FirmanteVersionResponse> versionar(
            @PathVariable Long id,
            @RequestBody VersionarFirmanteRequest req) {
        VersionarFirmanteCommand cmd = new VersionarFirmanteCommand(
                id, req.nomFirmante(), req.rolFirmante(), req.cargoFirmante(),
                req.idDep(), req.verDep(), req.fhVigDesde(), req.idUserAlta());
        FalFirmanteVersion version = firmanteService.versionar(cmd);
        List<FalFirmanteVersionHabilitacion> habs =
                firmanteRepository.findHabilitacionesByVersion(id, version.getVerFirmante());
        return ResponseEntity.ok(FirmanteVersionResponse.de(version, habs));
    }

    @PostMapping("/{id}/versiones/{ver}/habilitaciones")
    public ResponseEntity<FirmanteHabilitacionResponse> agregarHabilitacion(
            @PathVariable Long id,
            @PathVariable int ver,
            @RequestBody AgregarHabilitacionFirmanteRequest req) {
        AgregarHabilitacionFirmanteCommand cmd = new AgregarHabilitacionFirmanteCommand(
                id, ver, req.tipoDocu(), req.rolFirmaReq(), req.mecanismoFirmaReq(), req.idUserAlta());
        FalFirmanteVersionHabilitacion hab = firmanteService.agregarHabilitacion(cmd);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(FirmanteHabilitacionResponse.de(hab));
    }

    @GetMapping("/{id}/versiones/{ver}/habilitaciones")
    public ResponseEntity<List<FirmanteHabilitacionResponse>> listarHabilitaciones(
            @PathVariable Long id,
            @PathVariable int ver) {
        List<FirmanteHabilitacionResponse> result = firmanteService.listarHabilitaciones(id, ver)
                .stream()
                .map(FirmanteHabilitacionResponse::de)
                .toList();
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}/versiones/{ver}/habilitaciones/desactivar")
    public ResponseEntity<Void> desactivarHabilitacion(
            @PathVariable Long id,
            @PathVariable int ver,
            @RequestBody DesactivarHabilitacionFirmanteRequest req) {
        DesactivarHabilitacionFirmanteCommand cmd = new DesactivarHabilitacionFirmanteCommand(
                id, ver, req.tipoDocu(), req.rolFirmaReq(), req.idUserAlta());
        firmanteService.desactivarHabilitacion(cmd);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(FirmanteNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(FirmanteNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(PrecondicionVioladaException.class)
    public ResponseEntity<Map<String, String>> handlePrecondicion(PrecondicionVioladaException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", ex.getMessage()));
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private FirmanteResponse buildResponse(FalFirmante firmante) {
        List<FalFirmanteVersion> versiones =
                firmanteRepository.findVersionesByFirmante(firmante.getIdFirmante());
        Map<Integer, List<FalFirmanteVersionHabilitacion>> habsPorVer = versiones.stream()
                .collect(Collectors.toMap(
                        FalFirmanteVersion::getVerFirmante,
                        v -> firmanteRepository.findHabilitacionesByVersion(
                                firmante.getIdFirmante(), v.getVerFirmante())));
        return FirmanteResponse.de(firmante, versiones, habsPorVer);
    }
}