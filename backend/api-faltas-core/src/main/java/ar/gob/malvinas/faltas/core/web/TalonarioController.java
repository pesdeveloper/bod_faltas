package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.command.AnularNumeroTalonarioCommand;
import ar.gob.malvinas.faltas.core.application.command.JustificarNumeroTalonarioCommand;
import ar.gob.malvinas.faltas.core.application.command.DevolverNumeroSinUsarCommand;
import ar.gob.malvinas.faltas.core.application.command.CerrarAsignacionTalonarioInspectorCommand;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonarioMovimiento;
import ar.gob.malvinas.faltas.core.web.dto.AnularNumeroTalonarioRequest;
import ar.gob.malvinas.faltas.core.web.dto.JustificarNumeroTalonarioRequest;
import ar.gob.malvinas.faltas.core.web.dto.DevolverNumeroSinUsarRequest;
import ar.gob.malvinas.faltas.core.web.dto.CerrarAsignacionTalonarioInspectorRequest;
import ar.gob.malvinas.faltas.core.web.dto.CierreAsignacionTalonarioResponse;
import ar.gob.malvinas.faltas.core.application.command.AsignarTalonarioInspectorCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearPoliticaNumeracionCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearTalonarioAmbitoCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearTalonarioCommand;
import ar.gob.malvinas.faltas.core.application.command.DevolverTalonarioInspectorCommand;
import ar.gob.malvinas.faltas.core.application.command.EmitirNumeroActaCommand;
import ar.gob.malvinas.faltas.core.application.result.NumeroActaEmitidoResponse;
import ar.gob.malvinas.faltas.core.application.service.TalonarioService;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.NumPolitica;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonario;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonarioAmbito;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonarioInspector;
import ar.gob.malvinas.faltas.core.web.dto.AsignarTalonarioInspectorRequest;
import ar.gob.malvinas.faltas.core.web.dto.CrearPoliticaNumeracionRequest;
import ar.gob.malvinas.faltas.core.web.dto.CrearTalonarioAmbitoRequest;
import ar.gob.malvinas.faltas.core.web.dto.CrearTalonarioRequest;
import ar.gob.malvinas.faltas.core.web.dto.DevolverTalonarioInspectorRequest;
import ar.gob.malvinas.faltas.core.web.dto.EmitirNumeroActaRequest;
import ar.gob.malvinas.faltas.core.web.dto.PoliticaNumeracionResponse;
import ar.gob.malvinas.faltas.core.web.dto.TalonarioAmbitoResponse;
import ar.gob.malvinas.faltas.core.web.dto.TalonarioInspectorResponse;
import ar.gob.malvinas.faltas.core.web.dto.TalonarioMovimientoResponse;
import ar.gob.malvinas.faltas.core.web.dto.TalonarioResponse;
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
@RequestMapping("/api/faltas/talonarios")
public class TalonarioController {

    private final TalonarioService talonarioService;

    public TalonarioController(TalonarioService talonarioService) {
        this.talonarioService = talonarioService;
    }

    @PostMapping("/politicas")
    public ResponseEntity<PoliticaNumeracionResponse> crearPolitica(
            @RequestBody CrearPoliticaNumeracionRequest req) {
        CrearPoliticaNumeracionCommand cmd = new CrearPoliticaNumeracionCommand(
                req.codigo(), req.descripcion(), req.claseNumeracion(),
                req.siReinicioAnual(), req.siIncluyePrefijo(), req.prefijo(),
                req.siIncluyeAnio(), req.formatoAnio(), req.siIncluyeSerie(),
                req.longitudNro(), req.formatoVisible(), req.siActiva(),
                req.fhVigDesde(), req.fhVigHasta(), req.idUserAlta());
        NumPolitica politica = talonarioService.crearPolitica(cmd);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PoliticaNumeracionResponse.de(politica));
    }

    @GetMapping("/politicas")
    public ResponseEntity<List<PoliticaNumeracionResponse>> listarPoliticas() {
        List<PoliticaNumeracionResponse> respuestas = talonarioService.listarPoliticasActivas()
                .stream().map(PoliticaNumeracionResponse::de).toList();
        return ResponseEntity.ok(respuestas);
    }

    @GetMapping("/politicas/{id}")
    public ResponseEntity<PoliticaNumeracionResponse> obtenerPolitica(@PathVariable Long id) {
        NumPolitica politica = talonarioService.obtenerPolitica(id);
        return ResponseEntity.ok(PoliticaNumeracionResponse.de(politica));
    }

    @PostMapping
    public ResponseEntity<TalonarioResponse> crearTalonario(
            @RequestBody CrearTalonarioRequest req) {
        CrearTalonarioCommand cmd = new CrearTalonarioCommand(
                req.politicaId(), req.codigo(), req.descripcion(),
                req.tipoTalonario(), req.claseTalonario(),
                req.anio(), req.serie(), req.nroDesde(), req.nroHasta(),
                req.nombreSecuencia(), req.siActivo(), req.siBloqueado(),
                req.codDesbloqueo(), req.obsTalonario(), req.idUserAlta());
        NumTalonario talonario = talonarioService.crearTalonario(cmd);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TalonarioResponse.de(talonario));
    }

    @GetMapping
    public ResponseEntity<List<TalonarioResponse>> listarTalonarios() {
        List<TalonarioResponse> respuestas = talonarioService.listarTalonariosActivos()
                .stream().map(TalonarioResponse::de).toList();
        return ResponseEntity.ok(respuestas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TalonarioResponse> obtenerTalonario(@PathVariable Long id) {
        NumTalonario talonario = talonarioService.obtenerTalonario(id);
        return ResponseEntity.ok(TalonarioResponse.de(talonario));
    }

    @PostMapping("/{id}/ambitos")
    public ResponseEntity<TalonarioAmbitoResponse> crearAmbito(
            @PathVariable Long id,
            @RequestBody CrearTalonarioAmbitoRequest req) {
        CrearTalonarioAmbitoCommand cmd = new CrearTalonarioAmbitoCommand(
                id, req.claseTalonario(), req.tipoDocu(), req.tipoActa(),
                req.idDep(), req.verDep(), req.alcance(), req.prioridad(),
                req.fhDesde(), req.fhHasta(), req.siActivo(), req.idUserAlta());
        NumTalonarioAmbito ambito = talonarioService.crearAmbito(cmd);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TalonarioAmbitoResponse.de(ambito));
    }

    @GetMapping("/{id}/ambitos")
    public ResponseEntity<List<TalonarioAmbitoResponse>> listarAmbitos(@PathVariable Long id) {
        List<TalonarioAmbitoResponse> respuestas = talonarioService.listarAmbitosPorTalonario(id)
                .stream().map(TalonarioAmbitoResponse::de).toList();
        return ResponseEntity.ok(respuestas);
    }

    @PostMapping("/{idTalonario}/asignaciones-inspector")
    public ResponseEntity<TalonarioInspectorResponse> asignarInspector(
            @PathVariable Long idTalonario,
            @RequestBody AsignarTalonarioInspectorRequest req) {
        if (req.idInsp() == null) {
            throw new PrecondicionVioladaException("idInsp es obligatorio.");
        }
        if (req.verInsp() == null) {
            throw new PrecondicionVioladaException("verInsp es obligatorio.");
        }
        AsignarTalonarioInspectorCommand cmd = new AsignarTalonarioInspectorCommand(
                idTalonario, req.idInsp(), req.verInsp(), req.fhEntrega(), req.idUserEntrega());
        NumTalonarioInspector asignacion = talonarioService.asignarTalonarioInspector(cmd);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TalonarioInspectorResponse.de(asignacion));
    }

    @GetMapping("/{idTalonario}/asignaciones-inspector")
    public ResponseEntity<List<TalonarioInspectorResponse>> listarAsignacionesPorTalonario(
            @PathVariable Long idTalonario) {
        List<TalonarioInspectorResponse> respuestas =
                talonarioService.listarAsignacionesPorTalonario(idTalonario)
                        .stream().map(TalonarioInspectorResponse::de).toList();
        return ResponseEntity.ok(respuestas);
    }

    @GetMapping("/asignaciones-inspector/activas")
    public ResponseEntity<List<TalonarioInspectorResponse>> listarAsignacionesActivas() {
        List<TalonarioInspectorResponse> respuestas =
                talonarioService.listarAsignacionesActivas()
                        .stream().map(TalonarioInspectorResponse::de).toList();
        return ResponseEntity.ok(respuestas);
    }

    @PutMapping("/asignaciones-inspector/{idAsignacion}/devolver")
    public ResponseEntity<TalonarioInspectorResponse> devolverAsignacion(
            @PathVariable Long idAsignacion,
            @RequestBody DevolverTalonarioInspectorRequest req) {
        DevolverTalonarioInspectorCommand cmd = new DevolverTalonarioInspectorCommand(
                idAsignacion, req.fhDevolucion(), req.idUserDevolucion());
        NumTalonarioInspector asignacion = talonarioService.devolverTalonarioInspector(cmd);
        return ResponseEntity.ok(TalonarioInspectorResponse.de(asignacion));
    }

    // -------------------------------------------------------------------------
    // Numeracion de actas (Slice 8B-4)
    // -------------------------------------------------------------------------

    @PostMapping("/numeracion/actas/emitir")
    public ResponseEntity<NumeroActaEmitidoResponse> emitirNumeroActa(
            @RequestBody EmitirNumeroActaRequest req) {
        EmitirNumeroActaCommand cmd = new EmitirNumeroActaCommand(
                req.idDep(), req.verDep(), req.tipoActa(),
                req.idInsp(), req.verInsp(),
                req.actaId(), req.fhMovimiento(), req.idUserMovimiento());
        NumeroActaEmitidoResponse resultado = talonarioService.emitirNumeroActa(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

    @GetMapping("/{idTalonario}/movimientos")
    public ResponseEntity<List<TalonarioMovimientoResponse>> listarMovimientos(
            @PathVariable Long idTalonario) {
        List<TalonarioMovimientoResponse> respuestas =
                talonarioService.listarMovimientosPorTalonario(idTalonario)
                        .stream().map(TalonarioMovimientoResponse::de).toList();
        return ResponseEntity.ok(respuestas);
    }


    // -------------------------------------------------------------------------
    // Control de numeros de talonarios manuales fisicos (Slice 8B-6)
    // -------------------------------------------------------------------------

    @PostMapping("/{idTalonario}/numeros/{nroTalonario}/anular")
    public ResponseEntity<TalonarioMovimientoResponse> anularNumero(
            @PathVariable Long idTalonario,
            @PathVariable int nroTalonario,
            @RequestBody AnularNumeroTalonarioRequest req) {
        AnularNumeroTalonarioCommand cmd = new AnularNumeroTalonarioCommand(
                idTalonario, nroTalonario,
                req.motivoAnulacion(), req.observacion(),
                req.idDep(), req.verDep(), req.idInsp(), req.verInsp(),
                req.fhMovimiento(), req.idUserMovimiento());
        NumTalonarioMovimiento movimiento = talonarioService.anularNumeroTalonario(cmd);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TalonarioMovimientoResponse.de(movimiento));
    }

    @PostMapping("/{idTalonario}/numeros/{nroTalonario}/justificar")
    public ResponseEntity<TalonarioMovimientoResponse> justificarNumero(
            @PathVariable Long idTalonario,
            @PathVariable int nroTalonario,
            @RequestBody JustificarNumeroTalonarioRequest req) {
        JustificarNumeroTalonarioCommand cmd = new JustificarNumeroTalonarioCommand(
                idTalonario, nroTalonario,
                req.observacion(),
                req.idDep(), req.verDep(), req.idInsp(), req.verInsp(),
                req.fhMovimiento(), req.idUserMovimiento());
        NumTalonarioMovimiento movimiento = talonarioService.justificarNumeroTalonario(cmd);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TalonarioMovimientoResponse.de(movimiento));
    }

    @PostMapping("/{idTalonario}/numeros/{nroTalonario}/devolver-sin-usar")
    public ResponseEntity<TalonarioMovimientoResponse> devolverNumeroSinUsar(
            @PathVariable Long idTalonario,
            @PathVariable int nroTalonario,
            @RequestBody DevolverNumeroSinUsarRequest req) {
        DevolverNumeroSinUsarCommand cmd = new DevolverNumeroSinUsarCommand(
                idTalonario, nroTalonario,
                req.observacion(), req.idInsp(), req.verInsp(),
                req.fhMovimiento(), req.idUserMovimiento());
        NumTalonarioMovimiento movimiento = talonarioService.devolverNumeroSinUsar(cmd);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TalonarioMovimientoResponse.de(movimiento));
    }

    @PutMapping("/asignaciones-inspector/{idAsignacion}/cerrar")
    public ResponseEntity<CierreAsignacionTalonarioResponse> cerrarAsignacion(
            @PathVariable Long idAsignacion,
            @RequestBody CerrarAsignacionTalonarioInspectorRequest req) {
        CerrarAsignacionTalonarioInspectorCommand cmd = new CerrarAsignacionTalonarioInspectorCommand(
                idAsignacion, req.fhCierre(), req.idUserCierre(), req.observacion());
        CierreAsignacionTalonarioResponse resultado = talonarioService.cerrarAsignacionTalonarioInspector(cmd);
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/{idTalonario}/numeros/faltantes")
    public ResponseEntity<List<Integer>> listarNumerosFaltantes(@PathVariable Long idTalonario) {
        List<Integer> faltantes = talonarioService.listarNumerosFaltantes(idTalonario);
        return ResponseEntity.ok(faltantes);
    }
    @ExceptionHandler(PrecondicionVioladaException.class)
    public ResponseEntity<Map<String, String>> handlePrecondicion(PrecondicionVioladaException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", ex.getMessage()));
    }
}
