package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.ResolverPagoObligacionAnteriorCommand;
import ar.gob.malvinas.faltas.core.domain.enums.ClasificacionPago;
import ar.gob.malvinas.faltas.core.domain.enums.MovimientoRegistroResult;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoMovimientoPago;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.ObligacionPagoNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PagoMovimientoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.exception.ResolucionPagoAnteriorConflictoException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEconomiaProyeccion;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaObligacionPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPagoMovimiento;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.EconomiaProyeccionRepository;
import ar.gob.malvinas.faltas.core.repository.ObligacionPagoRepository;
import ar.gob.malvinas.faltas.core.repository.PagoMovimientoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Resuelve administrativamente un pago aplicado a una obligacion anterior
 * (ResolverPagoObligacionAnteriorCommand). Ver
 * backend/api-faltas-core/docs/spec-as-source/03-comandos-precondiciones-efectos.md
 * (contrato completo del comando) y
 * backend/api-faltas-core/docs/spec-as-source/02-estados-bloques-eventos.md
 * (PAGANT/PAGRES) para el modelo canonico.
 *
 * No existe fal_acta_pago_resolucion ni obligacion por diferencia: el unico
 * efecto de esta resolucion es un movimiento de aplicacion (PAGO_CONFIRMADO,
 * clasificacionPago=NORMAL) contra la obligacion vigente, enlazado al
 * movimiento original (PAGANT) via movimientoOrigenId, mas el evento PAGRES.
 * La unicidad de la aplicacion se garantiza sin tabla propia: a lo mas un
 * movimiento puede declarar movimientoOrigenId=R.id.
 *
 * Orden canonico de resolver() (CMD-ORDER-002 y unicidad de instante real):
 * (1) cmd no nulo, (2) resolver actor, (3) actor no blank, (4) actaId
 * obligatorio, (5) movimientoPagoId obligatorio, (6) motivo normalizado,
 * (7) cargar recursos (acta, movimiento original, obligacion origen),
 * (8) validar elegibilidad completa, (9) resolver retry, (10) capturar el
 * unico instante real de FaltasClock, (11) primera mutacion. nextId() no se
 * consume antes de completar 1-9.
 *
 * Concurrencia InMemory: el metodo resolver() es synchronized a nivel de esta
 * instancia de servicio (singleton de Spring), lo que serializa todas las
 * resoluciones dentro de este proceso. No es una garantia de exclusion mutua
 * entre instancias/nodos; ese requisito fisico queda documentado como
 * pendiente de MariaDB en 109/110-*.md (bloqueo/aislamiento a nivel de fila
 * via versionRow + transaccion, no via monitor de aplicacion).
 */
@Service
public class ResolverPagoObligacionAnteriorService {

    private final ActaRepository actaRepo;
    private final PagoMovimientoRepository movimientoRepo;
    private final ObligacionPagoRepository obligacionRepo;
    private final EconomiaProyeccionRepository proyeccionRepo;
    private final PagoIntegracionService integracionService;
    private final ActaEventoRepository eventoRepo;
    private final FaltasClock clock;

    public ResolverPagoObligacionAnteriorService(
            ActaRepository actaRepo,
            PagoMovimientoRepository movimientoRepo,
            ObligacionPagoRepository obligacionRepo,
            EconomiaProyeccionRepository proyeccionRepo,
            PagoIntegracionService integracionService,
            ActaEventoRepository eventoRepo,
            FaltasClock clock) {
        this.actaRepo = actaRepo;
        this.movimientoRepo = movimientoRepo;
        this.obligacionRepo = obligacionRepo;
        this.proyeccionRepo = proyeccionRepo;
        this.integracionService = integracionService;
        this.eventoRepo = eventoRepo;
        this.clock = clock;
    }

    public synchronized ResolverPagoObligacionAnteriorResultado resolver(ResolverPagoObligacionAnteriorCommand cmd) {
        // (1) cmd no nulo
        if (cmd == null) throw new PrecondicionVioladaException("cmd es obligatorio");

        // (2) resolver actor
        String actor = ActorContextHolder.subOr(cmd.idUser());

        // (3) actor no blank
        if (actor == null || actor.isBlank()) throw new PrecondicionVioladaException("actor es obligatorio");

        // (4) actaId obligatorio
        if (cmd.actaId() == null) throw new PrecondicionVioladaException("actaId es obligatorio");

        // (5) movimientoPagoId obligatorio
        if (cmd.movimientoPagoId() == null) throw new PrecondicionVioladaException("movimientoPagoId es obligatorio");

        // (6) motivo normalizado
        String motivoNormalizado = normalizarMotivo(cmd.motivo());

        // (7) cargar recursos
        actaRepo.buscarPorId(cmd.actaId())
                .orElseThrow(() -> new ActaNoEncontradaException(cmd.actaId()));

        FalActaPagoMovimiento original = movimientoRepo.findById(cmd.movimientoPagoId())
                .orElseThrow(() -> new PagoMovimientoNoEncontradoException(cmd.movimientoPagoId()));

        FalActaObligacionPago obligacionOrigen = obligacionRepo.findById(original.getObligacionPagoId())
                .orElseThrow(() -> new ObligacionPagoNoEncontradaException(original.getObligacionPagoId()));

        // (8) validar elegibilidad completa
        if (original.getTipoMovimiento() != TipoMovimientoPago.PAGO_CONFIRMADO
                || original.getClasificacionPago() != ClasificacionPago.OBLIGACION_ANTERIOR) {
            throw new PrecondicionVioladaException(
                    "El movimiento " + cmd.movimientoPagoId()
                            + " no esta clasificado como pago aplicado a obligacion anterior");
        }
        if (original.getMovimientoOrigenId() != null) {
            throw new PrecondicionVioladaException(
                    "El movimiento " + cmd.movimientoPagoId() + " es un movimiento derivado (movimientoOrigenId="
                            + original.getMovimientoOrigenId() + ") y no puede resolverse directamente");
        }
        if (!obligacionOrigen.getActaId().equals(cmd.actaId())) {
            throw new PrecondicionVioladaException(
                    "El movimiento " + cmd.movimientoPagoId() + " no pertenece al acta " + cmd.actaId());
        }
        if (original.getImporteTotal() == null || original.getImporteTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PrecondicionVioladaException(
                    "El movimiento " + cmd.movimientoPagoId() + " no tiene un importeTotal valido para aplicar");
        }
        if (obligacionOrigen.isSiVigente()) {
            throw new PrecondicionVioladaException(
                    "La obligacion origen " + obligacionOrigen.getId()
                            + " es vigente: no admite resolucion de pago anterior");
        }

        // (9) resolver retry
        List<FalActaPagoMovimiento> aplicacionesPrevias = movimientoRepo.findByMovimientoOrigenId(original.getId());
        if (!aplicacionesPrevias.isEmpty()) {
            return resolverReintento(cmd.actaId(), original, aplicacionesPrevias.get(0), obligacionOrigen, motivoNormalizado);
        }

        // (10) capturar el unico instante real
        LocalDateTime ahora = clock.now();

        // (11) primera mutacion
        FalActaObligacionPago obligacionVigente = obligacionRepo.findVigenteByActaId(cmd.actaId())
                .orElseThrow(() -> new ObligacionPagoNoEncontradaException(
                        "No existe obligacion vigente en el acta " + cmd.actaId() + " para aplicar el pago"));

        BigDecimal importe = original.getImporteTotal();

        Long movAplicadoId = movimientoRepo.nextId();
        FalActaPagoMovimiento movimientoAplicado = new FalActaPagoMovimiento.Builder(
                movAplicadoId, obligacionVigente.getId(), TipoMovimientoPago.PAGO_CONFIRMADO,
                original.getOrigenMovimiento(), ahora, ahora, actor)
                .origenConfirmacion(original.getOrigenConfirmacion())
                .clasificacionPago(ClasificacionPago.NORMAL)
                .importes(null, null, importe)
                .movimientoOrigenId(original.getId())
                .motivoAplicacionPagoAnterior(motivoNormalizado)
                .referenciaExterna("RESOL-" + original.getId())
                .build();
        RegistroMovimientoOutcome outcome = movimientoRepo.append(movimientoAplicado);
        if (outcome.resultado() != MovimientoRegistroResult.CREATED) {
            throw new IllegalStateException(
                    "No se pudo registrar el movimiento de aplicacion para el pago anterior movimientoPagoId="
                            + cmd.movimientoPagoId());
        }

        integracionService.recalcularEstados(
                obligacionVigente.getId(), obligacionVigente.getFormaPagoVigenteId(), null, ahora, actor);
        emitirEventoPagres(cmd.actaId(), original, obligacionVigente.getId(), motivoNormalizado, actor, ahora);

        FalActaEconomiaProyeccion proyeccion = proyeccionRepo.findByActaId(cmd.actaId()).orElse(null);
        BigDecimal saldoResultante = proyeccion != null ? proyeccion.getSaldoPendiente() : BigDecimal.ZERO;
        BigDecimal importeExcedente = proyeccion != null ? proyeccion.getImporteExcedente() : BigDecimal.ZERO;

        return new ResolverPagoObligacionAnteriorResultado(
                original, outcome.movimiento(), obligacionOrigen.getId(), obligacionVigente.getId(),
                importe, saldoResultante, importeExcedente, motivoNormalizado, actor, ahora);
    }

    /**
     * Reintento sobre un movimiento original ya resuelto: la aplicacion es
     * unica por movimientoOrigenId. Compatible (misma obligacion aplicada y
     * motivo equivalente) devuelve el resultado historico de la primera
     * ejecucion (actor, fecha y motivo de aplicacionPrevia) sin efectos
     * nuevos; incompatible (distinta obligacion destino o motivo) es un
     * conflicto (409). La autorizacion para reintentar sigue evaluandose con
     * el actor autenticado actual (validado en el paso 3 de resolver()); solo
     * cambia que datos historicos se reportan en el resultado.
     */
    private ResolverPagoObligacionAnteriorResultado resolverReintento(
            Long actaId,
            FalActaPagoMovimiento original,
            FalActaPagoMovimiento aplicacionPrevia,
            FalActaObligacionPago obligacionOrigen,
            String motivoNormalizado) {
        boolean obligacionDestinoCambio = obligacionRepo.findVigenteByActaId(actaId)
                .map(vigenteActual -> !vigenteActual.getId().equals(aplicacionPrevia.getObligacionPagoId()))
                .orElse(true);
        if (obligacionDestinoCambio) {
            throw new ResolucionPagoAnteriorConflictoException(
                    "El movimiento " + original.getId()
                            + " ya fue resuelto contra una obligacion que ya no es la vigente del acta " + actaId);
        }
        String motivoPrevio = aplicacionPrevia.getMotivoAplicacionPagoAnterior();
        if (!Objects.equals(motivoPrevio, motivoNormalizado)) {
            throw new ResolucionPagoAnteriorConflictoException(
                    "El movimiento " + original.getId() + " ya fue resuelto con un motivo distinto");
        }

        FalActaEconomiaProyeccion proyeccion = proyeccionRepo.findByActaId(actaId).orElse(null);
        BigDecimal saldoResultante = proyeccion != null ? proyeccion.getSaldoPendiente() : BigDecimal.ZERO;
        BigDecimal importeExcedente = proyeccion != null ? proyeccion.getImporteExcedente() : BigDecimal.ZERO;
        BigDecimal importeAplicado = aplicacionPrevia.getImporteTotal() != null
                ? aplicacionPrevia.getImporteTotal() : BigDecimal.ZERO;

        return new ResolverPagoObligacionAnteriorResultado(
                original, aplicacionPrevia, obligacionOrigen.getId(), aplicacionPrevia.getObligacionPagoId(),
                importeAplicado, saldoResultante, importeExcedente,
                aplicacionPrevia.getMotivoAplicacionPagoAnterior(), aplicacionPrevia.getIdUserAlta(),
                aplicacionPrevia.getFhMovimiento());
    }

    private void emitirEventoPagres(
            Long actaId, FalActaPagoMovimiento movimientoOriginal, Long obligacionAplicadaId,
            String motivoNormalizado, String actor, LocalDateTime ahora) {
        String correlacionId = "PAGRES-" + movimientoOriginal.getId();
        FalActaEvento evento = FalActaEvento.builder()
                .actaId(actaId)
                .tipoEvt(TipoEventoActa.PAGRES)
                .origenEvt(OrigenEvento.USUARIO_WEB)
                .fhEvt(ahora)
                .idUserEvt(actor)
                .descripcionLegible("Pago anterior resuelto administrativamente movOrigen=" + movimientoOriginal.getId()
                        + " obligAplicada=" + obligacionAplicadaId + " motivo=" + motivoNormalizado)
                .correlacionId(correlacionId)
                .build();
        eventoRepo.registrar(evento);
    }

    private static String normalizarMotivo(String motivo) {
        return motivo == null ? "" : motivo.trim();
    }
}
