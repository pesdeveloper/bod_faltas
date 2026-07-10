package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.ClasificacionPago;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoAnulacionPago;
import ar.gob.malvinas.faltas.core.domain.enums.MovimientoRegistroResult;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenConfirmacion;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenMovimiento;
import ar.gob.malvinas.faltas.core.domain.enums.TipoMovimientoPago;
import ar.gob.malvinas.faltas.core.domain.exception.ObligacionPagoNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPagoMovimiento;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import ar.gob.malvinas.faltas.core.repository.ObligacionPagoRepository;
import ar.gob.malvinas.faltas.core.repository.PagoMovimientoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class PagoMovimientoService {

    private final PagoMovimientoRepository movimientoRepo;
    private final ObligacionPagoRepository obligacionRepo;
    private final FaltasClock clock;

    private static final java.util.Set<TipoMovimientoPago> TIPOS_REVERSIBLES =
            java.util.EnumSet.of(TipoMovimientoPago.PAGO_CONFIRMADO);

    public PagoMovimientoService(
            PagoMovimientoRepository movimientoRepo,
            ObligacionPagoRepository obligacionRepo,
            FaltasClock clock) {
        this.movimientoRepo = movimientoRepo;
        this.obligacionRepo = obligacionRepo;
        this.clock = clock;
    }

    public RegistroMovimientoOutcome registrar(
            Long obligacionPagoId,
            Long formaPagoId,
            Long planPagoRefId,
            TipoMovimientoPago tipoMovimiento,
            OrigenMovimiento origenMovimiento,
            OrigenConfirmacion origenConfirmacion,
            Long evidenciaDocumentoId,
            ClasificacionPago clasificacionPago,
            Short nroCuota,
            BigDecimal importeCapital,
            BigDecimal importeRima,
            BigDecimal importeTotal,
            String cmteEM, Short prefEM, Integer nroEM,
            String cmtePG, Short prefPG, Integer nroPG,
            Long idCierre,
            Long idOpe,
            LocalDateTime fhPagoProcesado,
            LocalDateTime fhPagoConfirmado,
            String referenciaExterna,
            LocalDateTime fhMovimiento,
            String idUser) {
        obligacionRepo.findById(obligacionPagoId)
                .orElseThrow(() -> new ObligacionPagoNoEncontradaException(obligacionPagoId));
        if (origenMovimiento == null) throw new PrecondicionVioladaException("origenMovimiento es obligatorio");
        if (nroCuota != null && planPagoRefId == null)
            throw new PrecondicionVioladaException("nroCuota requiere planPagoRefId");

        String actor = ActorContextHolder.subOr(idUser);

        // Idempotencia temprana: verificar existente ANTES de consumir nextId()
        Optional<FalActaPagoMovimiento> existentePorRef = (referenciaExterna == null)
                ? Optional.empty()
                : movimientoRepo.findByOrigenAndReferenciaExterna(origenMovimiento, referenciaExterna);
        LocalDateTime fhAlta = existentePorRef.map(FalActaPagoMovimiento::getFhAlta).orElseGet(clock::now);
        LocalDateTime fhMov = fhMovimiento != null
                ? fhMovimiento
                : existentePorRef.map(FalActaPagoMovimiento::getFhMovimiento).orElse(fhAlta);

        if (existentePorRef.isPresent()) {
            FalActaPagoMovimiento prev = existentePorRef.get();
            // Comparar payload usando ID del existente (payloadEquivalenteA ignora ID)
            FalActaPagoMovimiento candidate = new FalActaPagoMovimiento.Builder(
                    prev.getId(), obligacionPagoId, tipoMovimiento, origenMovimiento, fhMov, prev.getFhAlta(), actor)
                    .formaPagoId(formaPagoId)
                    .planPagoRefId(planPagoRefId)
                    .origenConfirmacion(origenConfirmacion)
                    .evidenciaDocumentoId(evidenciaDocumentoId)
                    .clasificacionPago(clasificacionPago != null ? clasificacionPago : ClasificacionPago.NORMAL)
                    .nroCuota(nroCuota)
                    .importes(importeCapital, importeRima, importeTotal)
                    .referenciaEM(cmteEM, prefEM, nroEM)
                    .referenciaPG(cmtePG, prefPG, nroPG)
                    .idCierre(idCierre)
                    .idOpe(idOpe)
                    .fhPagoProcesado(fhPagoProcesado)
                    .fhPagoConfirmado(fhPagoConfirmado)
                    .referenciaExterna(referenciaExterna)
                    .build();
            if (prev.payloadEquivalenteA(candidate)) {
                return new RegistroMovimientoOutcome(MovimientoRegistroResult.ALREADY_EXISTS, prev);
            }
            return new RegistroMovimientoOutcome(MovimientoRegistroResult.CONFLICT, prev);
        }

        // Solo consumir nextId cuando es realmente nuevo
        Long nuevoId = movimientoRepo.nextId();
        FalActaPagoMovimiento m = new FalActaPagoMovimiento.Builder(nuevoId, obligacionPagoId, tipoMovimiento, origenMovimiento, fhMov, fhAlta, actor)
                .formaPagoId(formaPagoId)
                .planPagoRefId(planPagoRefId)
                .origenConfirmacion(origenConfirmacion)
                .evidenciaDocumentoId(evidenciaDocumentoId)
                .clasificacionPago(clasificacionPago != null ? clasificacionPago : ClasificacionPago.NORMAL)
                .nroCuota(nroCuota)
                .importes(importeCapital, importeRima, importeTotal)
                .referenciaEM(cmteEM, prefEM, nroEM)
                .referenciaPG(cmtePG, prefPG, nroPG)
                .idCierre(idCierre)
                .idOpe(idOpe)
                .fhPagoProcesado(fhPagoProcesado)
                .fhPagoConfirmado(fhPagoConfirmado)
                .referenciaExterna(referenciaExterna)
                .build();
        return movimientoRepo.append(m);
    }

    public RegistroMovimientoOutcome revertir(
            Long movimientoOriginalId,
            MotivoAnulacionPago motivo,
            String referenciaExterna,
            OrigenMovimiento origenMovimiento,
            String idUser) {
        FalActaPagoMovimiento original = movimientoRepo.findById(movimientoOriginalId)
                .orElseThrow(() -> new PrecondicionVioladaException("Movimiento no encontrado: " + movimientoOriginalId));

        if (motivo == null)
            throw new PrecondicionVioladaException("El motivo de reverso es obligatorio");

        if (referenciaExterna == null || referenciaExterna.isBlank())
            throw new PrecondicionVioladaException("referenciaExterna es obligatoria para reverso tecnico idempotente");

        if (!TIPOS_REVERSIBLES.contains(original.getTipoMovimiento()))
            throw new PrecondicionVioladaException(
                    "Tipo de movimiento no reversible: " + original.getTipoMovimiento());

        OrigenMovimiento origenEfectivo = origenMovimiento != null ? origenMovimiento : OrigenMovimiento.TESORERIA;

        // Idempotencia: verificar reverso previo ANTES de consumir nextId
        FalActaPagoMovimiento reversoPrevio = movimientoRepo.findByObligacionPagoId(original.getObligacionPagoId()).stream()
                .filter(mv -> mv.getTipoMovimiento() == TipoMovimientoPago.PAGO_REVERTIDO
                        && movimientoOriginalId.equals(mv.getMovimientoOrigenId()))
                .findFirst()
                .orElse(null);
        if (reversoPrevio != null) {
            if (Objects.equals(reversoPrevio.getReferenciaExterna(), referenciaExterna)) {
                // Misma referencia: verificar compatibilidad semantica del payload
                if (reversoPrevio.getMotivoAnulacionPago() != motivo
                        || reversoPrevio.getOrigenMovimiento() != origenEfectivo) {
                    throw new PrecondicionVioladaException(
                            "Reverso ref=" + referenciaExterna + " ya existe con motivo u origen incompatible");
                }
                return new RegistroMovimientoOutcome(MovimientoRegistroResult.ALREADY_EXISTS, reversoPrevio);
            }
            throw new PrecondicionVioladaException(
                    "El movimiento id=" + movimientoOriginalId + " ya fue revertido");
        }

        // Solo consumir nextId cuando es realmente nuevo
        String actor = ActorContextHolder.subOr(idUser);
        LocalDateTime ahora = clock.now();
        Long nuevoId = movimientoRepo.nextId();
        FalActaPagoMovimiento reverso = new FalActaPagoMovimiento.Builder(
                nuevoId, original.getObligacionPagoId(), TipoMovimientoPago.PAGO_REVERTIDO,
                origenEfectivo, ahora, ahora, actor)
                .formaPagoId(original.getFormaPagoId())
                .planPagoRefId(original.getPlanPagoRefId())
                .importes(original.getImporteCapital(), original.getImporteRima(), original.getImporteTotal())
                .movimientoOrigenId(movimientoOriginalId)
                .motivoAnulacionPago(motivo)
                .referenciaExterna(referenciaExterna)
                .build();
        return movimientoRepo.append(reverso);
    }

    public List<FalActaPagoMovimiento> buscarPorObligacion(Long obligacionPagoId) {
        return movimientoRepo.findByObligacionPagoId(obligacionPagoId);
    }

    public Optional<FalActaPagoMovimiento> buscarPorOrigenYReferencia(OrigenMovimiento origen, String refExterna) {
        return movimientoRepo.findByOrigenAndReferenciaExterna(origen, refExterna);
    }
}