package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.ConfirmarPagoCondenaCommand;
import ar.gob.malvinas.faltas.core.application.command.InformarPagoCondenaCommand;
import ar.gob.malvinas.faltas.core.application.command.ObservarPagoCondenaCommand;
import ar.gob.malvinas.faltas.core.application.port.BloqueantesMaterialesChecker;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoFalloActa;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoPagoCondena;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.ActorTipoEvento;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFalloActa;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.model.FalPagoCondena;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.PagoCondenaRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.springframework.stereotype.Service;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID; /**
 * Motor del circuito de pago de condena (Slice 5).
 *
 * Pago de condena aplica solo cuando resultadoFinal = CONDENA_FIRME.
 * Eventos: PCOINF -> PCOCNF (+ CIERRA si no hay bloqueantes) | PCOOBS (-> puede reinformar)
 *
 * PAGCON no existe como evento productivo.
 *
 * Confirmar pago de condena (Slice 7A):
 *   PCOCNF se registra siempre si las precondiciones se cumplen.
 *   Si no hay bloqueantes activos: tambien registra CIERRA y cierra el acta (CERRADA/CERR).
 *   Si hay bloqueantes activos: no registra CIERRA, acta queda ACTIVA/ANAL con CONDENA_FIRME_PAGADA.
 *
 * Deuda tecnica:
 *   - referenciaPago: dato temporal, reemplazar con integracion real de Ingresos/Tesoreria.
 *   - Comprobantes reales quedan para slice posterior.
 *   - Gestion externa: no implementada en este slice.
 *
 * Deuda tecnica:
 *   - BloqueantesMaterialesChecker: hoy NoOpBloqueantesMaterialesChecker (siempre false).
 *   - referenciaPago: dato temporal, reemplazar con integracion real de Ingresos/Tesoreria.
 *   - Comprobantes reales quedan para slice posterior.
 *   - Gestion externa: no implementada en este slice.
 */
@Service
public class PagoCondenaService {

    private final ActaRepository actaRepository;
    private final ActaEventoRepository eventoRepository;
    private final ActaSnapshotRepository snapshotRepository;
    private final FalloActaRepository falloActaRepository;
    private final PagoCondenaRepository pagoCondenaRepository;
    private final SnapshotRecalculador snapshotRecalculador;
    private final BloqueantesMaterialesChecker bloqueantesMaterialesChecker;
    private final FaltasClock faltasClock;

    public PagoCondenaService(
            ActaRepository actaRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            FalloActaRepository falloActaRepository,
            PagoCondenaRepository pagoCondenaRepository,
            SnapshotRecalculador snapshotRecalculador,
            BloqueantesMaterialesChecker bloqueantesMaterialesChecker,
            FaltasClock faltasClock) {
        this.faltasClock = faltasClock;
        this.actaRepository = actaRepository;
        this.eventoRepository = eventoRepository;
        this.snapshotRepository = snapshotRepository;
        this.falloActaRepository = falloActaRepository;
        this.pagoCondenaRepository = pagoCondenaRepository;
        this.snapshotRecalculador = snapshotRecalculador;
        this.bloqueantesMaterialesChecker = bloqueantesMaterialesChecker;
    }

    // -------------------------------------------------------------------------
    // InformarPagoCondena
    // -------------------------------------------------------------------------

    public ComandoResultado informar(InformarPagoCondenaCommand cmd) {
        FalActa acta = cargarActaOperativa(cmd.actaId());
        validarCondenaFirme(acta);
        validarFalloCondenatorioFirme(cmd.actaId());

        if (cmd.monto() == null || cmd.monto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PrecondicionVioladaException("El monto de pago de condena debe ser mayor a cero.");
        }
        if (cmd.referenciaPago() == null || cmd.referenciaPago().isBlank()) {
            throw new PrecondicionVioladaException("La referencia de pago es obligatoria.");
        }

        Optional<FalPagoCondena> pagoExistente = pagoCondenaRepository.buscarPorActa(cmd.actaId());
        if (pagoExistente.isPresent() && pagoExistente.get().estaConfirmado()) {
            throw new PrecondicionVioladaException(
                    "El pago de condena ya fue confirmado. No se puede reinformar.");
        }

        FalPagoCondena pago = pagoExistente.orElseGet(() ->
                new FalPagoCondena(UUID.randomUUID().toString(), cmd.actaId()));
        pago.setMonto(cmd.monto());
        pago.setReferenciaPago(cmd.referenciaPago());
        pago.setEstadoPagoCondena(EstadoPagoCondena.INFORMADO);
        pago.setFechaInforme(faltasClock.now());
        if (cmd.observaciones() != null) pago.setObservaciones(cmd.observaciones());
        pagoCondenaRepository.guardar(pago);

        registrarEvento(acta.getId(), TipoEventoActa.PCOINF, null, null, null,
                "Pago de condena informado. Referencia: " + cmd.referenciaPago()
                        + ". Monto: " + cmd.monto() + ". " + nvl(cmd.observaciones()));

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(acta.getId(), pago.getId(),
                TipoEventoActa.PCOINF.codigo(),
                "Pago de condena informado. Pendiente de confirmacion.");
    }

    // -------------------------------------------------------------------------
    // ConfirmarPagoCondena
    // Registra PCOCNF siempre. Registra CIERRA solo si no hay bloqueantes activos (Slice 7A).
    // -------------------------------------------------------------------------

    public ComandoResultado confirmar(ConfirmarPagoCondenaCommand cmd) {
        FalActa acta = cargarActaOperativa(cmd.actaId());
        validarCondenaFirme(acta);

        FalPagoCondena pago = cargarPagoRequerido(cmd.actaId());
        if (!pago.estaInformado()) {
            throw new PrecondicionVioladaException(
                    "Confirmar pago de condena requiere estado INFORMADO. Estado actual: "
                            + pago.getEstadoPagoCondena());
        }

        boolean hayBloqueantes = bloqueantesMaterialesChecker.tieneBloqueantesActivos(cmd.actaId());

        pago.setEstadoPagoCondena(EstadoPagoCondena.CONFIRMADO);
        pago.setFechaConfirmacion(faltasClock.now());
        if (cmd.observaciones() != null) pago.setObservaciones(cmd.observaciones());
        pagoCondenaRepository.guardar(pago);

        acta.setResultadoFinal(ResultadoFinalActa.CONDENA_FIRME_PAGADA);

        if (!hayBloqueantes) {
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.CERRADA);
            acta.setBloqueActual(BloqueActual.CERR);
        } else {
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.ACTIVA);
            acta.setBloqueActual(BloqueActual.ANAL);
        }
        actaRepository.guardar(acta);

        registrarEvento(acta.getId(), TipoEventoActa.PCOCNF, null, null, null,
                "Pago de condena confirmado. " + nvl(cmd.observaciones()));

        if (!hayBloqueantes) {
            registrarEvento(acta.getId(), TipoEventoActa.CIERRA, null, null, null,
                    "Acta cerrada por pago de condena confirmado.");
        }

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        String mensaje = hayBloqueantes
                ? "Pago de condena confirmado. Cierre pendiente por bloqueantes materiales activos. Resultado: CONDENA_FIRME_PAGADA."
                : "Pago de condena confirmado. Acta cerrada. Resultado: CONDENA_FIRME_PAGADA.";
        return ComandoResultado.de(acta.getId(), pago.getId(), TipoEventoActa.PCOCNF.codigo(), mensaje);
    }

    // -------------------------------------------------------------------------
    // ObservarPagoCondena
    // -------------------------------------------------------------------------

    public ComandoResultado observar(ObservarPagoCondenaCommand cmd) {
        FalActa acta = cargarActaOperativa(cmd.actaId());

        FalPagoCondena pago = cargarPagoRequerido(cmd.actaId());
        if (!pago.estaInformado()) {
            throw new PrecondicionVioladaException(
                    "Observar pago de condena requiere estado INFORMADO. Estado actual: "
                            + pago.getEstadoPagoCondena());
        }
        if (pago.estaConfirmado()) {
            throw new PrecondicionVioladaException(
                    "El pago de condena ya fue confirmado. No se puede observar.");
        }
        if (cmd.motivoObservacion() == null || cmd.motivoObservacion().isBlank()) {
            throw new PrecondicionVioladaException("El motivo de observacion es obligatorio.");
        }

        pago.setEstadoPagoCondena(EstadoPagoCondena.OBSERVADO);
        pago.setMotivoObservacion(cmd.motivoObservacion());
        pago.setFechaObservacion(faltasClock.now());
        if (cmd.observaciones() != null) pago.setObservaciones(cmd.observaciones());
        pagoCondenaRepository.guardar(pago);

        registrarEvento(acta.getId(), TipoEventoActa.PCOOBS, null, null, null,
                "Pago de condena observado. Motivo: " + cmd.motivoObservacion()
                        + ". " + nvl(cmd.observaciones()));

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(acta.getId(), pago.getId(),
                TipoEventoActa.PCOOBS.codigo(),
                "Pago de condena observado. Pendiente de correccion.");
    }

    // -------------------------------------------------------------------------
    // Consulta
    // -------------------------------------------------------------------------

    public Optional<FalPagoCondena> obtenerPago(Long actaId) {
        actaRepository.buscarPorId(actaId)
                .orElseThrow(() -> new ActaNoEncontradaException(actaId));
        return pagoCondenaRepository.buscarPorActa(actaId);
    }

    // -------------------------------------------------------------------------
    // Internos
    // -------------------------------------------------------------------------

    private FalActa cargarActaOperativa(Long actaId) {
        FalActa acta = actaRepository.buscarPorId(actaId)
                .orElseThrow(() -> new ActaNoEncontradaException(actaId));
        SituacionAdministrativaActa sit = acta.getSituacionAdministrativa();
        if (sit == SituacionAdministrativaActa.CERRADA)
            throw new PrecondicionVioladaException("El acta esta cerrada.");
        if (sit == SituacionAdministrativaActa.ANULADA)
            throw new PrecondicionVioladaException("El acta esta anulada.");
        if (sit == SituacionAdministrativaActa.ARCHIVADA)
            throw new PrecondicionVioladaException("El acta esta archivada.");
        if (sit == SituacionAdministrativaActa.PARALIZADA)
            throw new PrecondicionVioladaException("El acta esta paralizada.");
        return acta;
    }

    private void validarCondenaFirme(FalActa acta) {
        if (acta.getResultadoFinal() != ResultadoFinalActa.CONDENA_FIRME) {
            throw new PrecondicionVioladaException(
                    "El pago de condena solo aplica cuando resultadoFinal = CONDENA_FIRME. "
                            + "Resultado actual: " + acta.getResultadoFinal());
        }
    }

    private void validarFalloCondenatorioFirme(Long actaId) {
        FalActaFallo fallo = falloActaRepository.buscarActivo(actaId)
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "No existe fallo activo sobre el acta. No se puede iniciar pago de condena."));
        if (fallo.getTipoFallo() != TipoFalloActa.CONDENATORIO) {
            throw new PrecondicionVioladaException(
                    "El pago de condena requiere fallo condenatorio. Tipo actual: " + fallo.getTipoFallo());
        }
        if (fallo.getEstadoFallo() != EstadoFalloActa.FIRME) {
            throw new PrecondicionVioladaException(
                    "El pago de condena requiere fallo FIRME. Estado actual: "
                            + fallo.getEstadoFallo());
        }
        if (!fallo.isSiFirme()
                || fallo.getFhFirma() == null
                || fallo.getFhNotificacion() == null
                || fallo.getFhFirmeza() == null
                || fallo.getOrigenFirmeza() == null) {
            throw new PrecondicionVioladaException(
                    "El fallo FIRME presenta una firmeza inconsistente.");
        }
    }

    private FalPagoCondena cargarPagoRequerido(Long actaId) {
        return pagoCondenaRepository.buscarPorActa(actaId)
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "No existe pago de condena registrado para el acta: " + actaId
                                + ". Informe el pago primero."));
    }

    private void registrarEvento(Long idActa, TipoEventoActa tipo,
                                  Long idDocuRel, Long idNotifRel,
                                  String idUserEvt, String descripcionLegible) {
        FalActaEvento evento = FalActaEvento.builder()
                .actaId(idActa)
                .tipoEvt(tipo)
                .origenEvt(idUserEvt != null ? OrigenEvento.USUARIO_WEB : OrigenEvento.PROCESO_AUTOMATICO)
                .fhEvt(faltasClock.now())
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



