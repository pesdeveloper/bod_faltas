package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.store.PrototipoStore;
import static ar.gob.malvinas.faltas.prototipo.domain.PrototipoResultadoFinalHelper.resultadoFinalVigente;
import ar.gob.malvinas.faltas.prototipo.web.mapper.ActaDetalleMapper;
import ar.gob.malvinas.faltas.prototipo.web.dto.CerrabilidadResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.DictarFalloAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.DictarFalloCondenatorioAccionRequest;
import ar.gob.malvinas.faltas.prototipo.web.dto.MarcarResultadoAbsueltoAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarApelacionAccionRequest;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarApelacionAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarVencimientoPlazoApelacionAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ResolverApelacionAccionRequest;
import ar.gob.malvinas.faltas.prototipo.web.dto.ResolverApelacionAccionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/prototipo")
public class PrototipoFalloApelacionController {

    private final PrototipoStore store;

    public PrototipoFalloApelacionController(PrototipoStore store) {
        this.store = store;
    }

    // -------------------------------------------------------------------------
    // Resultado final
    // -------------------------------------------------------------------------

    /**
     * Resultado final mock: ABSUELTO. Solo desde {@code PENDIENTE_ANALISIS};
     * incompat con {@code PAGO_CONFIRMADO}. No implica cierre.
     */
    @PostMapping("/actas/{id}/acciones/marcar-resultado-final-absuelto")
    public MarcarResultadoAbsueltoAccionResponse marcarResultadoFinalAbsuelto(@PathVariable("id") String id) {
        PrototipoStore.MarcarResultadoAbsueltoResultado r = store.marcarResultadoAbsuelto(id);
        if (r.estado() == PrototipoStore.MarcarResultadoAbsueltoEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.MarcarResultadoAbsueltoEstado.CONFLICT) {
            throw conflictMarcarResultadoAbsuelto(id);
        }
        CerrabilidadResponse c = ActaDetalleMapper.mapCerrabilidad(store.getCerrabilidadActa(r.actaId()));
        return new MarcarResultadoAbsueltoAccionResponse(
                "OK",
                "Resultado final mock: ABSUELTO (no implica cierre automático).",
                r.actaId(),
                r.bandejaActual(),
                c);
    }

    // -------------------------------------------------------------------------
    // Fallo
    // -------------------------------------------------------------------------

    /**
     * Dicta fallo absolutorio desde {@code PENDIENTE_ANALISIS}: produce el
     * documento mock {@code FALLO_ABSOLUTORIO} en {@code PENDIENTE_FIRMA} y
     * mueve la acta a la bandeja {@code PENDIENTE_FIRMA}. No cambia
     * {@code resultadoFinal} todavía; eso ocurre al notificarse
     * positivamente el fallo (vía endpoint existente
     * {@code registrar-notificacion-positiva}). La firma del documento
     * reutiliza {@code firmar-documento/{documentoId}}.
     */
    @PostMapping("/actas/{id}/acciones/dictar-fallo-absolutorio")
    public DictarFalloAccionResponse dictarFalloAbsolutorio(@PathVariable("id") String id) {
        return mapDictarFallo(
                store.dictarFalloAbsolutorio(id),
                "Fallo absolutorio dictado; documento pendiente de firma.",
                null);
    }

    /**
     * Dicta fallo condenatorio desde {@code PENDIENTE_ANALISIS}: produce el
     * documento mock {@code FALLO_CONDENATORIO} en {@code PENDIENTE_FIRMA}
     * y mueve la acta a la bandeja {@code PENDIENTE_FIRMA}. No cambia
     * {@code resultadoFinal} todavía; al notificarse positivamente el fallo
     * se abre el plazo de apelación y el portal del infractor podrá ofrecer
     * la acción correspondiente.
     */
    @PostMapping("/actas/{id}/acciones/dictar-fallo-condenatorio")
    public DictarFalloAccionResponse dictarFalloCondenatorio(
            @PathVariable("id") String id,
            @RequestBody(required = false) DictarFalloCondenatorioAccionRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body requerido con montoCondena");
        }
        BigDecimal montoCondena = request.montoCondena();
        if (montoCondena == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "montoCondena requerido");
        }
        if (montoCondena.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "montoCondena debe ser mayor a cero");
        }
        return mapDictarFallo(
                store.dictarFalloCondenatorio(id, montoCondena),
                "Fallo condenatorio dictado; documento pendiente de firma.",
                montoCondena);
    }

    // -------------------------------------------------------------------------
    // Apelación y vencimiento de plazo
    // -------------------------------------------------------------------------

    /**
     * Registra el vencimiento mock del plazo de apelación (sin cálculo
     * real de días). Solo aplica si {@code resultadoFinal} es
     * {@code CONDENADO} y el plazo está abierto. Marca el resultado como
     * {@code CONDENA_FIRME}; el portal/infractor deja de habilitar la
     * presentación de apelación.
     */
    @PostMapping("/actas/{id}/acciones/registrar-vencimiento-plazo-apelacion")
    public RegistrarVencimientoPlazoApelacionAccionResponse registrarVencimientoPlazoApelacion(
            @PathVariable("id") String id) {
        PrototipoStore.RegistrarVencimientoPlazoApelacionResultado r =
                store.registrarVencimientoPlazoApelacion(id);
        if (r.estado() == PrototipoStore.RegistrarVencimientoPlazoApelacionEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.RegistrarVencimientoPlazoApelacionEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new RegistrarVencimientoPlazoApelacionAccionResponse(
                "OK",
                "Plazo de apelación vencido sin apelación; resultadoFinal CONDENA_FIRME.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.resultadoFinal());
    }

    /**
     * Registra la presentación mock de apelación/recurso mientras el plazo
     * está abierto. Cierra el plazo, conserva {@code resultadoFinal}
     * {@code CONDENADO} y no resuelve ni eleva el recurso.
     */
    @PostMapping("/actas/{id}/acciones/registrar-apelacion")
    public RegistrarApelacionAccionResponse registrarApelacion(
            @PathVariable("id") String id,
            @RequestBody(required = false) RegistrarApelacionAccionRequest request) {
        if (request == null || request.canal() == null || request.canal().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "canal requerido");
        }
        PrototipoStore.CanalPresentacionApelacionMock canal;
        try {
            canal = PrototipoStore.CanalPresentacionApelacionMock.valueOf(request.canal().trim());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "canal inválido: " + request.canal());
        }
        PrototipoStore.RegistrarApelacionResultado r = store.registrarApelacion(id, canal);
        if (r.estado() == PrototipoStore.RegistrarApelacionEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.RegistrarApelacionEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new RegistrarApelacionAccionResponse(
                "OK",
                "Apelación/recurso presentado; plazo de apelación cerrado; resultadoFinal CONDENADO.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.resultadoFinal(),
                r.canal());
    }

    /**
     * Resuelve mock de apelación/recurso ya presentado con plazo cerrado.
     * {@code RECHAZADA} confirma la condena ({@code CONDENA_FIRME});
     * {@code ACEPTADA_ABSUELVE} absuelve ({@code ABSUELTO}). No cierra el
     * acta ni deriva a gestión externa.
     */
    @PostMapping("/actas/{id}/acciones/resolver-apelacion")
    public ResolverApelacionAccionResponse resolverApelacion(
            @PathVariable("id") String id,
            @RequestBody(required = false) ResolverApelacionAccionRequest request) {
        if (request == null || request.resultado() == null || request.resultado().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resultado requerido");
        }
        PrototipoStore.ResultadoResolucionApelacionMock resultado;
        try {
            resultado =
                    PrototipoStore.ResultadoResolucionApelacionMock.valueOf(request.resultado().trim());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "resultado inválido: " + request.resultado());
        }
        PrototipoStore.ResolverApelacionResultado r = store.resolverApelacion(id, resultado);
        if (r.estado() == PrototipoStore.ResolverApelacionEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ResolverApelacionEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        String mensaje =
                resultado == PrototipoStore.ResultadoResolucionApelacionMock.RECHAZADA
                        ? "Apelación rechazada; condena confirmada; resultadoFinal CONDENA_FIRME."
                        : "Apelación acogida — absolución; resultadoFinal ABSUELTO.";
        return new ResolverApelacionAccionResponse(
                "OK",
                mensaje,
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.resultadoFinal(),
                r.resultadoResolucion());
    }

    // -------------------------------------------------------------------------
    // Helpers privados
    // -------------------------------------------------------------------------

    private DictarFalloAccionResponse mapDictarFallo(
            PrototipoStore.DictarFalloResultado r, String mensajeOk, BigDecimal montoCondena) {
        if (r.estado() == PrototipoStore.DictarFalloEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.DictarFalloEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new DictarFalloAccionResponse(
                "OK",
                mensajeOk,
                r.actaId(),
                r.documentoId(),
                r.tipoDocumento(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                montoCondena);
    }

    private ResponseStatusException conflictMarcarResultadoAbsuelto(String actaId) {
        PrototipoStore.ResultadoFinalCierreMock rf = resultadoFinalVigente(store, actaId);
        if (rf == PrototipoStore.ResultadoFinalCierreMock.PAGO_CONFIRMADO
                || rf == PrototipoStore.ResultadoFinalCierreMock.CONDENADO
                || rf == PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME) {
            return new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede marcar absolución directa (flujo viejo): resultado final vigente "
                            + rf.name()
                            + ".");
        }
        return new ResponseStatusException(HttpStatus.CONFLICT);
    }

}
