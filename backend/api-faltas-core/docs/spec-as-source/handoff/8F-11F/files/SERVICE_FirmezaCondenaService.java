package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.DeclararCondenaFirmePorApelacionRechazadaCommand;
import ar.gob.malvinas.faltas.core.application.command.VencerPlazoApelacionCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoApelacionActa;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoFalloActa;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenFirmezaCondena;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoResolucionApelacion;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFalloActa;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaApelacion;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFirmezaCondena;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.ApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de firmeza de condena.
 * La firmeza vive dentro de FalActaFallo (D1): siFirme, fhFirmeza, origenFirmeza.
 * No usa FirmezaCondenaRepository (eliminado en 8F-11F).
 */
@Service
public class FirmezaCondenaService {

    private final ActaRepository actaRepository;
    private final FalloActaRepository falloActaRepository;
    private final ApelacionActaRepository apelacionActaRepository;
    private final ActaEventoRepository eventoRepository;
    private final ActaSnapshotRepository snapshotRepository;
    private final SnapshotRecalculador snapshotRecalculador;

    // Constructor principal (Spring)
    @org.springframework.beans.factory.annotation.Autowired
    public FirmezaCondenaService(
            ActaRepository actaRepository,
            FalloActaRepository falloActaRepository,
            ApelacionActaRepository apelacionActaRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            SnapshotRecalculador snapshotRecalculador) {
        this.actaRepository = actaRepository;
        this.falloActaRepository = falloActaRepository;
        this.apelacionActaRepository = apelacionActaRepository;
        this.eventoRepository = eventoRepository;
        this.snapshotRepository = snapshotRepository;
        this.snapshotRecalculador = snapshotRecalculador;
    }

    /**
     * Constructor de compatibilidad backward para tests del Slice 3D.
     * Acepta el firmezaRepo deprecado pero lo ignora (no persiste en el).
     */
    @SuppressWarnings("deprecation")
    public FirmezaCondenaService(
            ActaRepository actaRepository,
            FalloActaRepository falloActaRepository,
            ApelacionActaRepository apelacionActaRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            ar.gob.malvinas.faltas.core.repository.FirmezaCondenaRepository ignorado,
            SnapshotRecalculador snapshotRecalculador) {
        this(actaRepository, falloActaRepository, apelacionActaRepository,
                eventoRepository, snapshotRepository, snapshotRecalculador);
    }

    public ComandoResultado vencerPlazoApelacion(VencerPlazoApelacionCommand cmd) {
        FalActa acta = actaRepository.buscarPorId(cmd.actaId())
                .orElseThrow(() -> new ActaNoEncontradaException(cmd.actaId()));
        validarActaOperativa(acta);
        validarSinCondenaFirme(acta);
        FalActaFallo fallo = obtenerFalloCondenatorioNotificado(cmd.actaId());
        validarSinApelacion(cmd.actaId());

        registrarEvento(cmd.actaId(), TipoEventoActa.PLAVNC, null, null, null,
                "Plazo de apelacion vencido sin presentacion. " + nvl(cmd.observaciones()));
        registrarEvento(cmd.actaId(), TipoEventoActa.CONFIR, null, null, null,
                "Condena firme declarada por vencimiento de plazo de apelacion.");

        fallo.declararFirmeza(LocalDateTime.now(), OrigenFirmezaCondena.VENCIMIENTO_PLAZO_APELACION);
        falloActaRepository.guardar(fallo);

        acta.setResultadoFinal(ResultadoFinalActa.CONDENA_FIRME);
        actaRepository.guardar(acta);

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(cmd.actaId(), String.valueOf(fallo.getId()),
                TipoEventoActa.CONFIR.codigo(),
                "Condena firme declarada por vencimiento de plazo de apelacion.");
    }

    public ComandoResultado declararFirmePorApelacionRechazada(DeclararCondenaFirmePorApelacionRechazadaCommand cmd) {
        FalActa acta = actaRepository.buscarPorId(cmd.actaId())
                .orElseThrow(() -> new ActaNoEncontradaException(cmd.actaId()));
        validarActaOperativa(acta);
        FalActaFallo fallo = obtenerFalloCondenatorioNotificado(cmd.actaId());
        validarSinCondenaFirme(acta);

        FalActaApelacion apelacion = apelacionActaRepository.buscarUltima(cmd.actaId())
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "No existe apelacion sobre el acta. Para firmeza por vencimiento use VencerPlazoApelacionCommand."));

        EstadoApelacionActa estadoAp = apelacion.getEstadoApelacion();
        // Acepta RESUELTA+RECHAZADA (nuevo modelo) o RECHAZADA legacy
        boolean esRechazada = (estadoAp == EstadoApelacionActa.RESUELTA
                && apelacion.getResultadoResolucion() == ResultadoResolucionApelacion.RECHAZADA)
                || estadoAp == EstadoApelacionActa.RECHAZADA;
        if (estadoAp == EstadoApelacionActa.PRESENTADA || estadoAp == EstadoApelacionActa.EN_ANALISIS)
            throw new PrecondicionVioladaException(
                    "La apelacion esta en estado " + estadoAp + ". Debe resolverse primero antes de declarar firmeza.");
        if (!esRechazada)
            throw new PrecondicionVioladaException(
                    "La apelacion no esta RECHAZADA. Estado actual: " + estadoAp
                    + ". Solo la apelacion rechazada habilita este comando.");

        registrarEvento(cmd.actaId(), TipoEventoActa.CONFIR, null, null, null,
                "Condena firme declarada por apelacion rechazada. " + nvl(cmd.observaciones()));

        fallo.declararFirmeza(LocalDateTime.now(), OrigenFirmezaCondena.APELACION_RECHAZADA);
        falloActaRepository.guardar(fallo);

        acta.setResultadoFinal(ResultadoFinalActa.CONDENA_FIRME);
        actaRepository.guardar(acta);

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(cmd.actaId(), String.valueOf(fallo.getId()),
                TipoEventoActa.CONFIR.codigo(),
                "Condena firme declarada por apelacion rechazada.");
    }

    /** Retorna el DTO de firmeza activa (construido desde el fallo firme), si existe. */
    public Optional<FalActaFirmezaCondena> obtenerFirmezaActiva(Long actaId) {
        actaRepository.buscarPorId(actaId).orElseThrow(() -> new ActaNoEncontradaException(actaId));
        return falloActaRepository.findVigenteByActaId(actaId)
                .filter(FalActaFallo::isSiFirme)
                .map(f -> {
                    Long apelacionId = apelacionActaRepository.buscarUltima(actaId)
                            .map(FalActaApelacion::getId).orElse(null);
                    return FalActaFirmezaCondena.desdesFallo(f, apelacionId);
                });
    }

    private void validarActaOperativa(FalActa acta) {
        SituacionAdministrativaActa sit = acta.getSituacionAdministrativa();
        if (sit == SituacionAdministrativaActa.CERRADA) throw new PrecondicionVioladaException("El acta esta cerrada.");
        if (sit == SituacionAdministrativaActa.ANULADA) throw new PrecondicionVioladaException("El acta esta anulada.");
        if (sit == SituacionAdministrativaActa.ARCHIVADA) throw new PrecondicionVioladaException("El acta esta archivada.");
        if (sit == SituacionAdministrativaActa.PARALIZADA) throw new PrecondicionVioladaException("El acta esta paralizada.");
    }

    private void validarSinCondenaFirme(FalActa acta) {
        if (acta.getResultadoFinal() == ResultadoFinalActa.CONDENA_FIRME)
            throw new PrecondicionVioladaException("La condena ya fue declarada firme. No se puede repetir.");
    }

    private void validarSinApelacion(Long actaId) {
        Optional<FalActaApelacion> ultima = apelacionActaRepository.buscarUltima(actaId);
        if (ultima.isEmpty()) return;
        EstadoApelacionActa estado = ultima.get().getEstadoApelacion();
        switch (estado) {
            case PRESENTADA, EN_ANALISIS ->
                throw new PrecondicionVioladaException("Existe apelacion activa en estado " + estado + ". No se puede vencer el plazo.");
            case RESUELTA -> {
                ResultadoResolucionApelacion res = ultima.get().getResultadoResolucion();
                if (res == ResultadoResolucionApelacion.RECHAZADA)
                    throw new PrecondicionVioladaException("La apelacion fue rechazada. Use DeclararCondenaFirmePorApelacionRechazadaCommand.");
            }
            case RECHAZADA ->
                throw new PrecondicionVioladaException("La apelacion fue rechazada. Use DeclararCondenaFirmePorApelacionRechazadaCommand.");
            default ->
                throw new PrecondicionVioladaException("Existe apelacion sobre el acta (estado: " + estado + "). No aplica vencimiento de plazo.");
        }
    }

    private FalActaFallo obtenerFalloCondenatorioNotificado(Long actaId) {
        FalActaFallo fallo = falloActaRepository.findVigenteByActaId(actaId)
                .orElseThrow(() -> new PrecondicionVioladaException("No existe fallo vigente. No se puede declarar firmeza."));
        if (fallo.getTipoFallo() != TipoFalloActa.CONDENATORIO)
            throw new PrecondicionVioladaException("Solo fallo condenatorio puede quedar firme. Tipo: " + fallo.getTipoFallo());
        if (fallo.getEstadoFallo() != EstadoFalloActa.NOTIFICADO)
            throw new PrecondicionVioladaException("El fallo debe estar NOTIFICADO. Estado: " + fallo.getEstadoFallo());
        return fallo;
    }

    private void registrarEvento(Long idActa, TipoEventoActa tipo, String idDoc,
                                  String idNotif, String idOp, String descripcion) {
        int orden = eventoRepository.proximoOrdenLogico(idActa);
        FalActaEvento ev = new FalActaEvento(UUID.randomUUID().toString(), idActa, tipo,
                LocalDateTime.now(), orden, idDoc, idNotif, idOp, descripcion, null);
        eventoRepository.registrar(ev);
    }

    private static String nvl(String s) { return s != null ? s : ""; }
}