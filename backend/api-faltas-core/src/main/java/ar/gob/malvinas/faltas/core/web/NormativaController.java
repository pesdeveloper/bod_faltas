package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.command.CrearArticuloNormativaFaltasCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearNormativaFaltasCommand;
import ar.gob.malvinas.faltas.core.application.command.VincularDependenciaNormativaCommand;
import ar.gob.malvinas.faltas.core.application.service.NormativaService;
import ar.gob.malvinas.faltas.core.domain.exception.ArticuloNormativaNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.DependenciaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.NormativaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalArticuloNormativaFaltas;
import ar.gob.malvinas.faltas.core.domain.model.FalDependenciaNormativa;
import ar.gob.malvinas.faltas.core.domain.model.FalNormativaFaltas;
import ar.gob.malvinas.faltas.core.web.dto.ArticuloNormativaFaltasResponse;
import ar.gob.malvinas.faltas.core.web.dto.CrearArticuloNormativaFaltasRequest;
import ar.gob.malvinas.faltas.core.web.dto.CrearNormativaFaltasRequest;
import ar.gob.malvinas.faltas.core.web.dto.DependenciaNormativaResponse;
import ar.gob.malvinas.faltas.core.web.dto.NormativaFaltasResponse;
import ar.gob.malvinas.faltas.core.web.dto.VincularDependenciaNormativaRequest;
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
@RequestMapping("/api/faltas")
public class NormativaController {

    private final NormativaService normativaService;

    public NormativaController(NormativaService normativaService) {
        this.normativaService = normativaService;
    }

    @PostMapping("/normativas")
    public ResponseEntity<NormativaFaltasResponse> crearNormativa(
            @RequestBody CrearNormativaFaltasRequest req) {
        FalNormativaFaltas n = normativaService.crearNormativa(new CrearNormativaFaltasCommand(
                req.codigoNorma(), req.versionNorma(), req.nombreNorma(),
                req.descripcionNorma(), req.fhVigDesde(), req.idUserAlta()));
        return ResponseEntity.status(HttpStatus.CREATED).body(NormativaFaltasResponse.de(n));
    }

    @GetMapping("/normativas")
    public ResponseEntity<List<NormativaFaltasResponse>> listarNormativas() {
        return ResponseEntity.ok(normativaService.listarNormativasActivas()
                .stream().map(NormativaFaltasResponse::de).toList());
    }

    @GetMapping("/normativas/{id}")
    public ResponseEntity<NormativaFaltasResponse> obtenerNormativa(@PathVariable Long id) {
        return ResponseEntity.ok(NormativaFaltasResponse.de(normativaService.obtenerNormativa(id)));
    }

    @PostMapping("/normativas/{normativaId}/articulos")
    public ResponseEntity<ArticuloNormativaFaltasResponse> crearArticulo(
            @PathVariable Long normativaId,
            @RequestBody CrearArticuloNormativaFaltasRequest req) {
        FalArticuloNormativaFaltas a = normativaService.crearArticulo(
                new CrearArticuloNormativaFaltasCommand(
                        normativaId, req.codigoArticulo(), req.versionArticulo(),
                        req.nombreArticulo(), req.descripcionArticulo(),
                        req.cantidadUnidades(), req.tipoUnidad(),
                        req.siTienePagoVoluntario(), req.cantidadUnidadesPagoVoluntario(),
                        req.tipoUnidadPagoVoluntario(), req.fhVigDesde(), req.idUserAlta()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ArticuloNormativaFaltasResponse.de(a));
    }

    @GetMapping("/normativas/{normativaId}/articulos")
    public ResponseEntity<List<ArticuloNormativaFaltasResponse>> listarArticulos(
            @PathVariable Long normativaId) {
        return ResponseEntity.ok(normativaService.listarArticulosByNormativa(normativaId)
                .stream().map(ArticuloNormativaFaltasResponse::de).toList());
    }

    @PostMapping("/dependencias/{idDep}/versiones/{verDep}/normativas")
    public ResponseEntity<DependenciaNormativaResponse> vincular(
            @PathVariable Long idDep,
            @PathVariable int verDep,
            @RequestBody VincularDependenciaNormativaRequest req) {
        FalDependenciaNormativa rel = normativaService.vincularDependenciaNormativa(
                new VincularDependenciaNormativaCommand(
                        idDep, verDep, req.normativaId(), req.idUserAlta()));
        return ResponseEntity.status(HttpStatus.CREATED).body(DependenciaNormativaResponse.de(rel));
    }

    @GetMapping("/dependencias/{idDep}/versiones/{verDep}/normativas")
    public ResponseEntity<List<DependenciaNormativaResponse>> listarNormativasDeDependencia(
            @PathVariable Long idDep,
            @PathVariable int verDep) {
        return ResponseEntity.ok(normativaService.listarNormativasByDepVersion(idDep, verDep)
                .stream().map(DependenciaNormativaResponse::de).toList());
    }

}
