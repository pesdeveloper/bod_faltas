package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.NotificarMovimientoPagoCommand;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoAnulacionPago;
import ar.gob.malvinas.faltas.core.domain.enums.MovimientoRegistroResult;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenMovimiento;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenUltimaActualizacion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoMovimientoPago;
import ar.gob.malvinas.faltas.core.domain.enums.TipoObligacionPago;
import ar.gob.malvinas.faltas.core.domain.exception.ConciliacionIncompatibleException;
import ar.gob.malvinas.faltas.core.domain.exception.MovimientoPagoDuplicadoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEconomiaProyeccion;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaObligacionPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPagoMovimiento;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.EconomiaProyeccionRepository;
import ar.gob.malvinas.faltas.core.repository.ObligacionPagoRepository;
import ar.gob.malvinas.faltas.core.repository.PagoMovimientoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class PagoEconomicoService {

    private final PagoIntegracionService integracionService;
    private final PagoMovimientoService movimientoService;
    private final EconomiaProyeccionRecalculador recalculador;
    private final ObligacionPagoRepository obligacionRepo;
    private final PagoMovimientoRepository movimientoRepo;
    private final EconomiaProyeccionRepository proyeccionRepo;
    private final ActaEventoRepository eventoRepo;
    private final FaltasClock clock;
    private final TesoreriaConciliacionInput tesoreriaInput;

    public PagoEconomicoService(
            PagoIntegracionService integracionService,
            PagoMovimientoService movimientoService,
            EconomiaProyeccionRecalculador recalculador,
            ObligacionPagoRepository obligacionRepo,
            PagoMovimientoRepository movimientoRepo,
            EconomiaProyeccionRepository proyeccionRepo,
            ActaEventoRepository eventoRepo,
            FaltasClock clock,
            TesoreriaConciliacionInput tesoreriaInput) {
        this.integracionService = integracionService;
        this.movimientoService = movimientoService;
        this.recalculador = recalculador;
        this.obligacionRepo = obligacionRepo;
        this.movimientoRepo = movimientoRepo;
        this.proyeccionRepo = proyeccionRepo;
        this.eventoRepo = eventoRepo;
        this.clock = clock;
        this.tesoreriaInput = tesoreriaInput;
    }

    public FalActaPagoMovimiento notificarMovimiento(NotificarMovimientoPagoCommand cmd) {
        if (cmd.tipoMovimiento() == TipoMovimientoPago.PAGO_REVERTIDO) {
            throw new PrecondicionVioladaException(
                    "PAGO_REVERTIDO no puede notificarse por el endpoint generico. Usar la operacion especifica de reverso.");
        }
        RegistroMovimientoOutcome outcome = integracionService.notificarMovimiento(cmd);
        if (outcome.resultado() == MovimientoRegistroResult.CONFLICT) {
            throw new MovimientoPagoDuplicadoException(cmd.referenciaExterna() != null ? cmd.referenciaExterna() : "conflicto");
        }
        if (outcome.resultado() == MovimientoRegistroResult.CREATED) {
            emitirEventoMovimiento(cmd);
        }
        return outcome.movimiento();
    }

    /**
     * Operacion atomica de alto nivel para reverso de movimiento.
     * Garantiza: 1 movimiento PAGO_REVERTIDO, 1 evento PAGREV, recalculo de estados.
     * Idempotente: reintento con misma referencia y mismo payload no duplica nada.
     */
    public FalActaPagoMovimiento revertirMovimiento(
            Long movimientoOriginalId,
            MotivoAnulacionPago motivo,
            String referenciaExterna,
            OrigenMovimiento origenMovimiento,
            String idUser) {
        FalActaPagoMovimiento original = movimientoRepo.findById(movimientoOriginalId)
                .orElseThrow(() -> new PrecondicionVioladaException("Movimiento no encontrado: " + movimientoOriginalId));

        RegistroMovimientoOutcome outcome = movimientoService.revertir(
                movimientoOriginalId, motivo, referenciaExterna, origenMovimiento, idUser);

        if (outcome.resultado() == MovimientoRegistroResult.CONFLICT) {
            throw new MovimientoPagoDuplicadoException(
                    referenciaExterna != null ? referenciaExterna : "conflicto-reverso");
        }

        if (outcome.resultado() == MovimientoRegistroResult.CREATED) {
            emitirEventoPagrev(original.getObligacionPagoId(), idUser, referenciaExterna);
            integracionService.recalcularEstados(
                    original.getObligacionPagoId(),
                    original.getFormaPagoId(),
                    original.getPlanPagoRefId());
        }

        return outcome.movimiento();
    }

    public FalActaEconomiaProyeccion conciliarMovimiento(Long movimientoId, String referenciaConciliacion) {
        FalActaPagoMovimiento m = movimientoRepo.findById(movimientoId)
                .orElseThrow(() -> new IllegalArgumentException("Movimiento no encontrado"));
        if (m.getTipoMovimiento() != TipoMovimientoPago.PAGO_CONFIRMADO) {
            throw new IllegalArgumentException("Solo se concilian pagos confirmados");
        }
        FalActaObligacionPago obl = obligacionRepo.findById(m.getObligacionPagoId()).orElseThrow();
        FalActaEconomiaProyeccion p = proyeccionRepo.findByActaId(obl.getActaId())
                .orElseGet(() -> recalculador.recalcular(obl.getActaId(), OrigenUltimaActualizacion.TIEMPO_REAL, "SISTEMA"));

        // Idempotencia: usar containsKey para distinguir no-conciliado vs conciliado con null
        Map<Long, String> snapshotActual = tesoreriaInput.snapshotActual();
        if (snapshotActual.containsKey(movimientoId)) {
            String prevRef = snapshotActual.get(movimientoId);
            if (Objects.equals(prevRef, referenciaConciliacion)) {
                // Reintento identico: devolver proyeccion actual sin mutar fechas ni estado
                return p;
            }
            throw new ConciliacionIncompatibleException("Conciliacion incompatible para movimiento " + movimientoId);
        }
        Map<Long, String> snapshotNuevo = new HashMap<>(snapshotActual);
        snapshotNuevo.put(movimientoId, referenciaConciliacion);
        tesoreriaInput.reemplazarEstadoAbsoluto(snapshotNuevo);

        String actor = ActorContextHolder.subOr("SISTEMA");
        LocalDateTime ahora = clock.now();
        p.setFhUltimaConciliacion(ahora);
        p.setReferenciaUltimaConciliacion(referenciaConciliacion);
        p.setIdUserUltMod(actor);
        proyeccionRepo.save(p);

        int eventosAntes = eventoRepo.buscarPorActa(obl.getActaId()).size();
        FalActaEconomiaProyeccion recalculada = recalculador.recalcular(obl.getActaId(), OrigenUltimaActualizacion.TIEMPO_REAL, actor);
        int eventosDespues = eventoRepo.buscarPorActa(obl.getActaId()).size();
        if (eventosDespues != eventosAntes) {
            throw new IllegalStateException("Conciliacion no debe generar eventos");
        }
        FalActaPagoMovimiento despues = movimientoRepo.findById(movimientoId).orElseThrow();
        if (!despues.payloadEquivalenteA(m)) {
            throw new IllegalStateException("Conciliacion no debe mutar el movimiento");
        }
        return recalculada;
    }

    private void emitirEventoMovimiento(NotificarMovimientoPagoCommand cmd) {
        FalActaObligacionPago obl = obligacionRepo.findById(cmd.obligacionPagoId()).orElseThrow();
        TipoEventoActa tipo = mapearEvento(cmd.tipoMovimiento(), obl.getTipoObligacion());
        if (tipo == null) return;
        String actor = ActorContextHolder.subOr(cmd.idUser());
        FalActaEvento evento = FalActaEvento.builder()
                .actaId(obl.getActaId())
                .tipoEvt(tipo)
                .origenEvt(OrigenEvento.INTEGRACION)
                .fhEvt(clock.now())
                .idUserEvt(actor)
                .descripcionLegible("Movimiento economico " + cmd.tipoMovimiento() + " ref=" + cmd.referenciaExterna())
                .correlacionId(cmd.referenciaExterna())
                .build();
        eventoRepo.registrar(evento);
    }

    private void emitirEventoPagrev(Long obligacionPagoId, String idUser, String correlacionId) {
        FalActaObligacionPago obl = obligacionRepo.findById(obligacionPagoId).orElseThrow();
        String actor = ActorContextHolder.subOr(idUser);
        FalActaEvento evento = FalActaEvento.builder()
                .actaId(obl.getActaId())
                .tipoEvt(TipoEventoActa.PAGREV)
                .origenEvt(OrigenEvento.INTEGRACION)
                .fhEvt(clock.now())
                .idUserEvt(actor)
                .descripcionLegible("Reverso de movimiento de pago ref=" + correlacionId)
                .correlacionId(correlacionId)
                .build();
        eventoRepo.registrar(evento);
    }

    private TipoEventoActa mapearEvento(TipoMovimientoPago tipo, TipoObligacionPago tipoObligacion) {
        return switch (tipo) {
            case DEUDA_EMITIDA, PAGO_PROCESADO -> null;
            case PAGO_CONFIRMADO -> tipoObligacion == TipoObligacionPago.CONDENA
                    ? TipoEventoActa.PCOCNF : TipoEventoActa.PAGCNF;
            case PAGO_REVERTIDO -> TipoEventoActa.PAGREV;
            case EMISION_ANULADA -> TipoEventoActa.EMIANU;
        };
    }

    public void recalcularEstadosEstructurales(Long obligacionPagoId, Long formaPagoId, Long planPagoRefId) {
        integracionService.recalcularEstados(obligacionPagoId, formaPagoId, planPagoRefId);
    }
}