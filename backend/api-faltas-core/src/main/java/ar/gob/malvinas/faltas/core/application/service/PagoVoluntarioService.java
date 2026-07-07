package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.ConfirmarPagoVoluntarioCommand;
import ar.gob.malvinas.faltas.core.application.command.FijarMontoPagoVoluntarioCommand;
import ar.gob.malvinas.faltas.core.application.command.InformarPagoVoluntarioCommand;
import ar.gob.malvinas.faltas.core.application.command.ObservarPagoVoluntarioCommand;
import ar.gob.malvinas.faltas.core.application.command.SolicitarPagoVoluntarioCommand;
import ar.gob.malvinas.faltas.core.application.command.VencerPagoVoluntarioCommand;
import ar.gob.malvinas.faltas.core.application.port.BloqueantesMaterialesChecker;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoPagoVoluntario;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.ActorTipoEvento;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.model.FalPagoVoluntario;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.PagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Motor de proceso del circuito de pago voluntario.
 *
 * Pago voluntario es un FLUJO con estados y eventos reales, no una accion unica.
 * Eventos: PAGVSO -> PAGVMF -> PAGINF -> PAGCNF + CIERRA | PAGOBS | PAGVVN
 *
 * PAGVOL no existe como evento productivo.
 * PAGO_INFORMADO no existe como estado productivo.
 *
 * Confirmacion de pago:
 *   Si no hay bloqueantes activos: registra PAGCNF (pago confirmado)
 *   y luego CIERRA (acta cerrada), en ese orden.
 *   Si hay bloqueantes activos: lanza PrecondicionVioladaException y no registra ningun evento.
 *
 * Deuda tecnica:
 *   - BloqueantesMaterialesChecker: hoy NoOpBloqueantesMaterialesChecker (siempre false).
 *     Reemplazar con motor real sin tocar servicios.
 *   - referenciaPago: dato temporal, reemplazar con Cmte_PG/Pref_PG/Nro_PG de Ingresos/Tesoreria.
 *   - PAGCMP: no se emite hasta que exista adjunto/evidencia real del comprobante.
 */
@Service
public class PagoVoluntarioService {

    private final ActaRepository actaRepository;
    private final ActaEventoRepository eventoRepository;
    private final ActaSnapshotRepository snapshotRepository;
    private final PagoVoluntarioRepository pagoVoluntarioRepository;
    private final SnapshotRecalculador snapshotRecalculador;
    private final BloqueantesMaterialesChecker bloqueantesMaterialesChecker;

    public PagoVoluntarioService(
            ActaRepository actaRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            PagoVoluntarioRepository pagoVoluntarioRepository,
            SnapshotRecalculador snapshotRecalculador,
            BloqueantesMaterialesChecker bloqueantesMaterialesChecker) {
        this.actaRepository = actaRepository;
        this.eventoRepository = eventoRepository;
        this.snapshotRepository = snapshotRepository;
        this.pagoVoluntarioRepository = pagoVoluntarioRepository;
        this.snapshotRecalculador = snapshotRecalculador;
        this.bloqueantesMaterialesChecker = bloqueantesMaterialesChecker;
    }

    // -------------------------------------------------------------------------
    // SolicitarPagoVoluntario
    // -------------------------------------------------------------------------

    public ComandoResultado solicitar(SolicitarPagoVoluntarioCommand cmd) {
        FalActa acta = cargarActaNoTerminada(cmd.actaId());

        BloqueActual bloque = acta.getBloqueActual();
        if (bloque != BloqueActual.ENRI && bloque != BloqueActual.ANAL) {
            throw new PrecondicionVioladaException(
                    "Solicitar pago voluntario requiere bloque ENRI o ANAL. Bloque actual: "
                            + bloque.codigo());
        }

        Optional<FalPagoVoluntario> pagoExistente = pagoVoluntarioRepository.buscarPorActa(cmd.actaId());
        if (pagoExistente.isPresent() && pagoExistente.get().estaConfirmado()) {
            throw new PrecondicionVioladaException("Ya existe un pago voluntario confirmado para este acta.");
        }

        FalPagoVoluntario pago = pagoExistente.orElseGet(() ->
                new FalPagoVoluntario(UUID.randomUUID().toString(), cmd.actaId()));
        pago.setEstadoPagoVoluntario(EstadoPagoVoluntario.SOLICITADO);
        pago.setFechaSolicitud(LocalDateTime.now());
        if (cmd.observaciones() != null) pago.setObservaciones(cmd.observaciones());
        pagoVoluntarioRepository.guardar(pago);

        if (bloque == BloqueActual.ENRI) {
            acta.setBloqueActual(BloqueActual.ANAL);
            actaRepository.guardar(acta);
        }

        registrarEvento(acta.getId(), TipoEventoActa.PAGVSO, null, null, null,
                "Pago voluntario solicitado. " + nvl(cmd.observaciones()));

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(acta.getId(), pago.getId(),
                TipoEventoActa.PAGVSO.codigo(), "Pago voluntario solicitado. Bloque: ANAL");
    }

    // -------------------------------------------------------------------------
    // FijarMontoPagoVoluntario
    // -------------------------------------------------------------------------

    public ComandoResultado fijarMonto(FijarMontoPagoVoluntarioCommand cmd) {
        FalActa acta = cargarActaNoTerminada(cmd.actaId());

        FalPagoVoluntario pago = cargarPagoRequerido(cmd.actaId());
        validarNoConfirmado(pago);
        validarNoVencido(pago);

        if (cmd.monto() == null || cmd.monto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PrecondicionVioladaException("El monto debe ser mayor a cero.");
        }
        if (pago.getEstadoPagoVoluntario() == EstadoPagoVoluntario.SIN_PAGO) {
            throw new PrecondicionVioladaException(
                    "No existe pago voluntario solicitado para fijar monto. Estado: "
                            + pago.getEstadoPagoVoluntario());
        }

        pago.setMonto(cmd.monto());
        pago.setEstadoPagoVoluntario(EstadoPagoVoluntario.MONTO_FIJADO);
        pago.setFechaMontoFijado(LocalDateTime.now());
        if (cmd.observaciones() != null) pago.setObservaciones(cmd.observaciones());
        pagoVoluntarioRepository.guardar(pago);

        registrarEvento(acta.getId(), TipoEventoActa.PAGVMF, null, null, null,
                "Monto fijado: " + cmd.monto() + ". " + nvl(cmd.observaciones()));

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(acta.getId(), pago.getId(),
                TipoEventoActa.PAGVMF.codigo(), "Monto fijado: " + cmd.monto());
    }

    // -------------------------------------------------------------------------
    // InformarPagoVoluntario
    // Nota: no emite PAGCMP porque aun no existe adjunto/evidencia real del comprobante.
    // PAGCMP se registrara en slice posterior de adjuntos/comprobantes reales.
    // -------------------------------------------------------------------------

    public ComandoResultado informar(InformarPagoVoluntarioCommand cmd) {
        FalActa acta = cargarActaNoTerminada(cmd.actaId());

        if (cmd.referenciaPago() == null || cmd.referenciaPago().isBlank()) {
            throw new PrecondicionVioladaException("La referencia de pago es obligatoria.");
        }

        FalPagoVoluntario pago = cargarPagoRequerido(cmd.actaId());
        validarNoConfirmado(pago);
        validarNoVencido(pago);

        EstadoPagoVoluntario estadoActual = pago.getEstadoPagoVoluntario();
        boolean estadoValido = estadoActual == EstadoPagoVoluntario.SOLICITADO
                || estadoActual == EstadoPagoVoluntario.MONTO_FIJADO
                || estadoActual == EstadoPagoVoluntario.OBSERVADO;
        if (!estadoValido) {
            throw new PrecondicionVioladaException(
                    "Informar pago requiere pago solicitado, con monto fijado u observado. Estado actual: "
                            + estadoActual);
        }

        pago.setReferenciaPago(cmd.referenciaPago());
        pago.setEstadoPagoVoluntario(EstadoPagoVoluntario.PENDIENTE_CONFIRMACION);
        pago.setFechaInforme(LocalDateTime.now());
        if (cmd.observaciones() != null) pago.setObservaciones(cmd.observaciones());
        pagoVoluntarioRepository.guardar(pago);

        registrarEvento(acta.getId(), TipoEventoActa.PAGINF, null, null, null,
                "Pago informado. Referencia: " + cmd.referenciaPago() + ". " + nvl(cmd.observaciones()));

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(acta.getId(), pago.getId(),
                TipoEventoActa.PAGINF.codigo(), "Pago informado. Pendiente de confirmacion.");
    }

    // -------------------------------------------------------------------------
    // ConfirmarPagoVoluntario
    // Registra PAGCNF y luego CIERRA si no hay bloqueantes activos.
    // Si hay bloqueantes: no registra ninguno de los dos eventos.
    // -------------------------------------------------------------------------

    public ComandoResultado confirmar(ConfirmarPagoVoluntarioCommand cmd) {
        FalActa acta = cargarActaNoTerminada(cmd.actaId());

        FalPagoVoluntario pago = cargarPagoRequerido(cmd.actaId());

        if (!pago.estaPendienteConfirmacion()) {
            throw new PrecondicionVioladaException(
                    "Confirmar pago requiere estado PENDIENTE_CONFIRMACION. Estado actual: "
                            + pago.getEstadoPagoVoluntario());
        }

        if (bloqueantesMaterialesChecker.tieneBloqueantesActivos(cmd.actaId())) {
            throw new PrecondicionVioladaException(
                    "No se puede confirmar el pago: existen bloqueantes materiales activos.");
        }

        pago.setEstadoPagoVoluntario(EstadoPagoVoluntario.CONFIRMADO);
        pago.setFechaConfirmacion(LocalDateTime.now());
        if (cmd.observaciones() != null) pago.setObservaciones(cmd.observaciones());
        pagoVoluntarioRepository.guardar(pago);

        acta.setResultadoFinal(ResultadoFinalActa.PAGO_VOLUNTARIO_PAGADO);
        acta.setSituacionAdministrativa(SituacionAdministrativaActa.CERRADA);
        acta.setBloqueActual(BloqueActual.CERR);
        actaRepository.guardar(acta);

        registrarEvento(acta.getId(), TipoEventoActa.PAGCNF, null, null, null,
                "Pago voluntario confirmado. " + nvl(cmd.observaciones()));

        registrarEvento(acta.getId(), TipoEventoActa.CIERRA, null, null, null,
                "Acta cerrada por pago voluntario confirmado.");

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(acta.getId(), pago.getId(),
                TipoEventoActa.PAGCNF.codigo(),
                "Pago voluntario confirmado. Acta cerrada. Resultado: PAGO_VOLUNTARIO_PAGADO.");
    }

    // -------------------------------------------------------------------------
    // ObservarPagoVoluntario
    // -------------------------------------------------------------------------

    public ComandoResultado observar(ObservarPagoVoluntarioCommand cmd) {
        FalActa acta = cargarActaNoTerminada(cmd.actaId());

        FalPagoVoluntario pago = cargarPagoRequerido(cmd.actaId());

        if (!pago.estaPendienteConfirmacion()) {
            throw new PrecondicionVioladaException(
                    "Observar pago requiere estado PENDIENTE_CONFIRMACION. Estado actual: "
                            + pago.getEstadoPagoVoluntario());
        }
        validarNoConfirmado(pago);

        if (cmd.motivoObservacion() == null || cmd.motivoObservacion().isBlank()) {
            throw new PrecondicionVioladaException("El motivo de observacion es obligatorio.");
        }

        pago.setEstadoPagoVoluntario(EstadoPagoVoluntario.OBSERVADO);
        pago.setMotivoObservacion(cmd.motivoObservacion());
        pago.setFechaObservacion(LocalDateTime.now());
        if (cmd.observaciones() != null) pago.setObservaciones(cmd.observaciones());
        pagoVoluntarioRepository.guardar(pago);

        registrarEvento(acta.getId(), TipoEventoActa.PAGOBS, null, null, null,
                "Pago observado. Motivo: " + cmd.motivoObservacion() + ". " + nvl(cmd.observaciones()));

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(acta.getId(), pago.getId(),
                TipoEventoActa.PAGOBS.codigo(),
                "Pago observado. Pendiente de correccion por el infractor.");
    }

    // -------------------------------------------------------------------------
    // VencerPagoVoluntario
    // -------------------------------------------------------------------------

    public ComandoResultado vencer(VencerPagoVoluntarioCommand cmd) {
        FalActa acta = cargarActaNoTerminada(cmd.actaId());

        FalPagoVoluntario pago = cargarPagoRequerido(cmd.actaId());
        validarNoConfirmado(pago);

        EstadoPagoVoluntario estadoActual = pago.getEstadoPagoVoluntario();
        boolean puedeVencer = estadoActual == EstadoPagoVoluntario.SOLICITADO
                || estadoActual == EstadoPagoVoluntario.MONTO_FIJADO
                || estadoActual == EstadoPagoVoluntario.PENDIENTE_CONFIRMACION
                || estadoActual == EstadoPagoVoluntario.OBSERVADO;
        if (!puedeVencer) {
            throw new PrecondicionVioladaException(
                    "No se puede vencer un pago en estado: " + estadoActual);
        }

        pago.setEstadoPagoVoluntario(EstadoPagoVoluntario.VENCIDO);
        pago.setFechaVencimiento(LocalDateTime.now());
        if (cmd.observaciones() != null) pago.setObservaciones(cmd.observaciones());
        pagoVoluntarioRepository.guardar(pago);

        acta.setBloqueActual(BloqueActual.ANAL);
        actaRepository.guardar(acta);

        registrarEvento(acta.getId(), TipoEventoActa.PAGVVN, null, null, null,
                "Pago voluntario vencido. Habilitado analisis/fallo. " + nvl(cmd.observaciones()));

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(acta.getId(), pago.getId(),
                TipoEventoActa.PAGVVN.codigo(),
                "Pago voluntario vencido. Bloque: ANAL. Habilitado analisis y fallo.");
    }

    // -------------------------------------------------------------------------
    // Consulta
    // -------------------------------------------------------------------------

    public Optional<FalPagoVoluntario> obtenerPago(Long actaId) {
        actaRepository.buscarPorId(actaId)
                .orElseThrow(() -> new ActaNoEncontradaException(actaId));
        return pagoVoluntarioRepository.buscarPorActa(actaId);
    }

    // -------------------------------------------------------------------------
    // Internos
    // -------------------------------------------------------------------------

    private FalActa cargarActaNoTerminada(Long actaId) {
        FalActa acta = actaRepository.buscarPorId(actaId)
                .orElseThrow(() -> new ActaNoEncontradaException(actaId));
        if (acta.estaCerrada()) {
            throw new PrecondicionVioladaException(
                    "El acta esta cerrada o anulada. No se pueden realizar acciones de pago.");
        }
        return acta;
    }

    private FalPagoVoluntario cargarPagoRequerido(Long actaId) {
        return pagoVoluntarioRepository.buscarPorActa(actaId)
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "No existe pago voluntario registrado para el acta: " + actaId));
    }

    private void validarNoConfirmado(FalPagoVoluntario pago) {
        if (pago.estaConfirmado()) {
            throw new PrecondicionVioladaException(
                    "El pago voluntario ya esta confirmado. No se puede modificar.");
        }
    }

    private void validarNoVencido(FalPagoVoluntario pago) {
        if (pago.estaVencido()) {
            throw new PrecondicionVioladaException(
                    "El pago voluntario esta vencido. No se puede modificar.");
        }
    }

    private void registrarEvento(Long idActa, TipoEventoActa tipo,
                                  Long idDocuRel, Long idNotifRel,
                                  String idUserEvt, String descripcionLegible) {
        FalActaEvento evento = FalActaEvento.builder()
                .actaId(idActa)
                .tipoEvt(tipo)
                .origenEvt(idUserEvt != null ? OrigenEvento.USUARIO_WEB : OrigenEvento.PROCESO_AUTOMATICO)
                .fhEvt(LocalDateTime.now())
                .idDocuRel(idDocuRel)
                .idNotifRel(idNotifRel)
                .idUserEvt(idUserEvt)
                .actorTipo(idUserEvt != null ? ActorTipoEvento.USUARIO_INTERNO : ActorTipoEvento.SISTEMA)
                .descripcionLegible(descripcionLegible)
                .build();
        eventoRepository.registrar(evento);
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }
}
