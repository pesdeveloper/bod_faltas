package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.CriterioTarifario;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoValorizacion;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoManualizacionValorizacion;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenValorizacion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoUnidadFaltas;
import ar.gob.malvinas.faltas.core.domain.enums.TipoValorizacionActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoValorizacionItem;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.exception.ValorizacionNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaArticuloInfringido;
import ar.gob.malvinas.faltas.core.domain.model.FalActaValorizacion;
import ar.gob.malvinas.faltas.core.domain.model.FalActaValorizacionItem;
import ar.gob.malvinas.faltas.core.domain.model.FalArticuloNormativaFaltas;
import ar.gob.malvinas.faltas.core.domain.model.FalTarifarioUnidadFaltas;
import ar.gob.malvinas.faltas.core.repository.ActaArticuloInfringidoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaValorizacionItemRepository;
import ar.gob.malvinas.faltas.core.repository.ActaValorizacionRepository;
import ar.gob.malvinas.faltas.core.repository.NormativaRepository;
import ar.gob.malvinas.faltas.core.repository.TarifarioUnidadFaltasRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Cálculo, confirmación y gestión del historial de valorizaciones de acta.
 * La valorización es la fuente económica del acta.
 * Solo una vigente por acta+tipo en todo momento.
 */
@Service
public class ValorizacionService {

    private final ActaValorizacionRepository valorizacionRepo;
    private final ActaValorizacionItemRepository itemRepo;
    private final ActaArticuloInfringidoRepository articuloRepo;
    private final NormativaRepository normativaRepo;
    private final TarifarioUnidadFaltasRepository tarifarioRepo;
    private final ActaRepository actaRepo;

    public ValorizacionService(
            ActaValorizacionRepository valorizacionRepo,
            ActaValorizacionItemRepository itemRepo,
            ActaArticuloInfringidoRepository articuloRepo,
            NormativaRepository normativaRepo,
            TarifarioUnidadFaltasRepository tarifarioRepo,
            ActaRepository actaRepo) {
        this.valorizacionRepo = valorizacionRepo;
        this.itemRepo = itemRepo;
        this.articuloRepo = articuloRepo;
        this.normativaRepo = normativaRepo;
        this.tarifarioRepo = tarifarioRepo;
        this.actaRepo = actaRepo;
    }

    /** Calcula y guarda una valorización base preliminar a partir de artículos activos del acta. */
    public FalActaValorizacion calcularBasePreliminar(Long actaId, String idUser) {
        actaRepo.buscarPorId(actaId).orElseThrow(() -> new ActaNoEncontradaException(actaId));

        List<FalActaArticuloInfringido> activos = articuloRepo.findActivosByActaId(actaId);
        if (activos.isEmpty())
            throw new PrecondicionVioladaException("El acta id=" + actaId + " no tiene artículos imputados activos.");

        LocalDate hoy = LocalDate.now();
        List<FalActaValorizacionItem> items = new ArrayList<>();
        BigDecimal montoTotal = BigDecimal.ZERO;

        for (FalActaArticuloInfringido imp : activos) {
            Optional<FalArticuloNormativaFaltas> artOpt = normativaRepo.findArticuloById(imp.getArticuloId());
            if (artOpt.isEmpty()) continue;
            FalArticuloNormativaFaltas art = artOpt.get();

            BigDecimal cantidad = art.getCantidadUnidades();
            TipoUnidadFaltas tipoUnidad = mapTipoUnidad(art.getTipoUnidad());

            BigDecimal valorUnidad = BigDecimal.ZERO;
            Long tarifarioId = null;

            if (tipoUnidad != TipoUnidadFaltas.MONTO) {
                Optional<FalTarifarioUnidadFaltas> tarOpt = tarifarioRepo.findUltimoVigente(tipoUnidad, hoy);
                if (tarOpt.isPresent()) {
                    valorUnidad = tarOpt.get().getValorUnidad();
                    tarifarioId = tarOpt.get().getId();
                }
            } else {
                valorUnidad = BigDecimal.ONE;
            }

            BigDecimal monto = cantidad.multiply(valorUnidad).setScale(2, RoundingMode.HALF_UP);
            montoTotal = montoTotal.add(monto);

            Long itemId = itemRepo.nextId();
            FalActaValorizacionItem item = new FalActaValorizacionItem(
                    itemId, 0L, imp.getId(), TipoValorizacionItem.AUTOMATICA, monto, false);
            item.setTipoUnidadBase(tipoUnidad);
            item.setCantidadUnidadesBase(cantidad);
            item.setTipoUnidadAplicada(tipoUnidad);
            item.setCantidadUnidadesAplicada(cantidad);
            item.setValorUnidadAplicado(valorUnidad);
            item.setTarifarioUnidadId(tarifarioId);
            items.add(item);
        }

        Long valId = valorizacionRepo.nextId();
        FalActaValorizacion val = new FalActaValorizacion(
                valId, actaId, TipoValorizacionActa.INFRACCION_BASE, OrigenValorizacion.SISTEMA,
                CriterioTarifario.ULTIMO_VIGENTE, montoTotal, LocalDateTime.now(), idUser,
                LocalDateTime.now(), idUser);
        val.setMontoBaseArticulos(montoTotal);
        val.setTarifarioActualizado(true);
        FalActaValorizacion saved = valorizacionRepo.save(val);

        for (FalActaValorizacionItem item : items) {
            FalActaValorizacionItem itemConId = new FalActaValorizacionItem(
                    item.getId(), saved.getId(), item.getActaArticuloId(),
                    item.getTipoValorizacionItem(), item.getMontoAplicado(), item.isSiManual());
            itemConId.setTipoUnidadBase(item.getTipoUnidadBase());
            itemConId.setCantidadUnidadesBase(item.getCantidadUnidadesBase());
            itemConId.setTipoUnidadAplicada(item.getTipoUnidadAplicada());
            itemConId.setCantidadUnidadesAplicada(item.getCantidadUnidadesAplicada());
            itemConId.setValorUnidadAplicado(item.getValorUnidadAplicado());
            itemConId.setTarifarioUnidadId(item.getTarifarioUnidadId());
            itemRepo.save(itemConId);
        }

        return saved;
    }

    /** Confirma una valorización preliminar y la marca como vigente, reemplazando la anterior del mismo tipo. */
    public FalActaValorizacion confirmar(Long valorizacionId, String idUser) {
        FalActaValorizacion val = valorizacionRepo.findById(valorizacionId)
                .orElseThrow(() -> new ValorizacionNoEncontradaException(valorizacionId));

        if (val.getEstadoValorizacion() != EstadoValorizacion.PRELIMINAR)
            throw new PrecondicionVioladaException("Solo se puede confirmar una valorización PRELIMINAR. Estado actual: " + val.getEstadoValorizacion());

        List<FalActaValorizacionItem> items = itemRepo.findByValorizacionId(valorizacionId);
        boolean esAjusteTotalSinItems = val.getTipoValorizacionActa() == TipoValorizacionActa.AJUSTE_TOTAL
                && val.isSiSobrescribeTotal();
        if (items.isEmpty() && !esAjusteTotalSinItems)
            throw new PrecondicionVioladaException("La valorización id=" + valorizacionId + " no tiene ítems.");

        Optional<FalActaValorizacion> vigenteAnterior = valorizacionRepo
                .findVigenteByActaIdAndTipo(val.getActaId(), val.getTipoValorizacionActa());

        vigenteAnterior.ifPresent(anterior -> {
            anterior.setEstadoValorizacion(EstadoValorizacion.REEMPLAZADA);
            anterior.setSiVigente(false);
            valorizacionRepo.save(anterior);
        });

        val.setEstadoValorizacion(EstadoValorizacion.CONFIRMADA);
        val.setFhConfirmacion(LocalDateTime.now());
        val.setIdUserConfirmacion(idUser);
        val.setSiVigente(true);
        return valorizacionRepo.save(val);
    }

    /** Anula una valorización. No se puede reactivar. */
    public FalActaValorizacion anular(Long valorizacionId, String idUser) {
        FalActaValorizacion val = valorizacionRepo.findById(valorizacionId)
                .orElseThrow(() -> new ValorizacionNoEncontradaException(valorizacionId));

        if (val.getEstadoValorizacion() == EstadoValorizacion.ANULADA)
            throw new PrecondicionVioladaException("La valorización id=" + valorizacionId + " ya está anulada.");

        if (val.isSiVigente()) val.setSiVigente(false);
        val.setEstadoValorizacion(EstadoValorizacion.ANULADA);
        return valorizacionRepo.save(val);
    }

    /** Agrega un ítem manual a una valorización preliminar. */
    public FalActaValorizacionItem agregarItemManual(
            Long valorizacionId,
            Long actaArticuloId,
            BigDecimal monto,
            MotivoManualizacionValorizacion motivo,
            Long documentoId,
            String idUser) {
        FalActaValorizacion val = valorizacionRepo.findById(valorizacionId)
                .orElseThrow(() -> new ValorizacionNoEncontradaException(valorizacionId));

        if (val.getEstadoValorizacion() != EstadoValorizacion.PRELIMINAR)
            throw new PrecondicionVioladaException("Solo se pueden agregar ítems a una valorización PRELIMINAR.");

        if (monto == null || monto.compareTo(BigDecimal.ZERO) < 0)
            throw new PrecondicionVioladaException("El monto del ítem no puede ser negativo.");

        if (motivo == null)
            throw new PrecondicionVioladaException("El motivo es obligatorio para un ítem manual.");

        if (motivo == MotivoManualizacionValorizacion.OTRO_FUNDADO && documentoId == null)
            throw new PrecondicionVioladaException("OTRO_FUNDADO requiere documentoId hasta que 8F-11G implemente observaciones.");

        Long itemId = itemRepo.nextId();
        FalActaValorizacionItem item = new FalActaValorizacionItem(
                itemId, valorizacionId, actaArticuloId, TipoValorizacionItem.MANUAL, monto, true);
        item.setMotivoManual(motivo);
        item.setDocumentoId(documentoId);
        return itemRepo.save(item);
    }

    public Optional<FalActaValorizacion> consultarVigente(Long actaId, TipoValorizacionActa tipo) {
        return valorizacionRepo.findVigenteByActaIdAndTipo(actaId, tipo);
    }

    public List<FalActaValorizacion> listarHistorial(Long actaId) {
        return valorizacionRepo.findByActaId(actaId);
    }

    public List<FalActaValorizacionItem> listarItems(Long valorizacionId) {
        return itemRepo.findByValorizacionId(valorizacionId);
    }

    /** Selecciona la valorización operativa vigente del acta según prioridad. */
    public Optional<FalActaValorizacion> seleccionarOperativa(Long actaId) {
        // 1. condena confirmada/vigente
        Optional<FalActaValorizacion> condena = valorizacionRepo
                .findVigenteByActaIdAndTipo(actaId, TipoValorizacionActa.CONDENA);
        if (condena.isPresent()) return condena;

        // 2. pago voluntario confirmado/vigente
        Optional<FalActaValorizacion> pagVol = valorizacionRepo
                .findVigenteByActaIdAndTipo(actaId, TipoValorizacionActa.PAGO_VOLUNTARIO);
        if (pagVol.isPresent()) return pagVol;

        // 3. infracción base confirmada/vigente
        return valorizacionRepo.findVigenteByActaIdAndTipo(actaId, TipoValorizacionActa.INFRACCION_BASE);
    }

    private TipoUnidadFaltas mapTipoUnidad(ar.gob.malvinas.faltas.core.domain.enums.TipoUnidad tipoUnidad) {
        return switch (tipoUnidad) {
            case SALARIO -> TipoUnidadFaltas.SALARIO;
            case UNIDAD_FIJA -> TipoUnidadFaltas.UNIDAD_FIJA;
            case MONTO -> TipoUnidadFaltas.MONTO;
        };
    }
}
