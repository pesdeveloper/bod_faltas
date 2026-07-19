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
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Calculo, confirmacion y gestion del historial de valorizaciones de acta.
 * La valorizacion es la fuente economica del acta.
 * Solo una vigente CONFIRMADA por acta+tipo en todo momento.
 * La confirmacion opera sobre la misma fila PRELIMINAR via confirmarVigenteAtomico.
 */
@Service
public class ValorizacionService {

    private final ActaValorizacionRepository valorizacionRepo;
    private final ActaValorizacionItemRepository itemRepo;
    private final ActaArticuloInfringidoRepository articuloRepo;
    private final NormativaRepository normativaRepo;
    private final TarifarioUnidadFaltasRepository tarifarioRepo;
    private final ActaRepository actaRepo;
    private final FaltasClock faltasClock;

    public ValorizacionService(
            ActaValorizacionRepository valorizacionRepo,
            ActaValorizacionItemRepository itemRepo,
            ActaArticuloInfringidoRepository articuloRepo,
            NormativaRepository normativaRepo,
            TarifarioUnidadFaltasRepository tarifarioRepo,
            ActaRepository actaRepo,
            FaltasClock faltasClock) {
        this.faltasClock = faltasClock;
        this.valorizacionRepo = valorizacionRepo;
        this.itemRepo = itemRepo;
        this.articuloRepo = articuloRepo;
        this.normativaRepo = normativaRepo;
        this.tarifarioRepo = tarifarioRepo;
        this.actaRepo = actaRepo;
    }

    /**
     * Calcula y guarda una valorizacion base preliminar a partir de articulos activos del acta.
     * Falla con PrecondicionVioladaException si un articulo activo no existe en el catalogo.
     * Falla con PrecondicionVioladaException si no hay tarifario vigente para SALARIO o UNIDAD_FIJA.
     */
    public FalActaValorizacion calcularBasePreliminar(Long actaId, String idUser) {
        actaRepo.buscarPorId(actaId).orElseThrow(() -> new ActaNoEncontradaException(actaId));

        List<FalActaArticuloInfringido> activos = articuloRepo.findActivosByActaId(actaId);
        if (activos.isEmpty())
            throw new PrecondicionVioladaException(
                    "El acta id=" + actaId + " no tiene articulos imputados activos.");

        LocalDateTime ahora = faltasClock.now();
        LocalDate hoy = ahora.toLocalDate();
        List<FalActaValorizacionItem> items = new ArrayList<>();
        BigDecimal montoTotal = BigDecimal.ZERO;

        for (FalActaArticuloInfringido imp : activos) {
            FalArticuloNormativaFaltas art = normativaRepo.findArticuloById(imp.getArticuloId())
                    .orElseThrow(() -> new PrecondicionVioladaException(
                            "El articulo id=" + imp.getArticuloId()
                                    + " referenciado por la imputacion id=" + imp.getId()
                                    + " no existe en el catalogo. Integridad rota."));

            BigDecimal cantidad = art.getCantidadUnidades();
            TipoUnidadFaltas tipoUnidad = mapTipoUnidad(art.getTipoUnidad());

            BigDecimal valorUnidad;
            Long tarifarioId = null;

            if (tipoUnidad != TipoUnidadFaltas.MONTO) {
                FalTarifarioUnidadFaltas tar = tarifarioRepo.findUltimoVigente(tipoUnidad, hoy)
                        .orElseThrow(() -> new PrecondicionVioladaException(
                                "No hay tarifario vigente para tipoUnidad=" + tipoUnidad
                                        + " en fecha=" + hoy + ". Valorizacion no puede calcularse."));
                valorUnidad = tar.getValorUnidad();
                tarifarioId = tar.getId();
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
                CriterioTarifario.ULTIMO_VIGENTE, montoTotal, ahora, idUser,
                ahora, idUser);
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

    /**
     * Confirma una valorizacion preliminar y la marca como vigente de forma atomica,
     * reemplazando la anterior del mismo tipo si existia.
     */
    public FalActaValorizacion confirmar(Long valorizacionId, String idUser) {
        FalActaValorizacion val = valorizacionRepo.findById(valorizacionId)
                .orElseThrow(() -> new ValorizacionNoEncontradaException(valorizacionId));

        if (val.getEstadoValorizacion() != EstadoValorizacion.PRELIMINAR)
            throw new PrecondicionVioladaException(
                    "Solo se puede confirmar una valorizacion PRELIMINAR. Estado actual: "
                            + val.getEstadoValorizacion());

        List<FalActaValorizacionItem> items = itemRepo.findByValorizacionId(valorizacionId);
        boolean esAjusteTotalSinItems = val.getTipoValorizacionActa() == TipoValorizacionActa.AJUSTE_TOTAL
                && val.isSiSobrescribeTotal();
        if (items.isEmpty() && !esAjusteTotalSinItems)
            throw new PrecondicionVioladaException(
                    "La valorizacion id=" + valorizacionId + " no tiene items.");

        // Validar consistencia cabecera <-> items cuando no es sobrescritura total
        if (!val.isSiSobrescribeTotal() && !items.isEmpty()) {
            BigDecimal sumaItems = items.stream()
                    .map(FalActaValorizacionItem::getMontoAplicado)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal montoFinalNorm = val.getMontoFinal().setScale(2, RoundingMode.HALF_UP);
            if (montoFinalNorm.compareTo(sumaItems) != 0) {
                throw new PrecondicionVioladaException(
                        "montoFinal=" + montoFinalNorm
                                + " no coincide con la suma de items=" + sumaItems
                                + " para valorizacion id=" + valorizacionId);
            }
        }

        // Leer vigente anterior para la operacion atomica
        Optional<FalActaValorizacion> vigenteAnterior = valorizacionRepo
                .findVigenteByActaIdAndTipo(val.getActaId(), val.getTipoValorizacionActa());

        return valorizacionRepo.confirmarVigenteAtomico(
                val.getId(),
                val.getVersionRow(),
                vigenteAnterior.map(FalActaValorizacion::getId).orElse(null),
                vigenteAnterior.map(v -> (Integer) v.getVersionRow()).orElse(null),
                faltasClock.now(),
                idUser);
    }

    /** Anula una valorizacion. No se puede reactivar. */
    public FalActaValorizacion anular(Long valorizacionId, String idUser) {
        FalActaValorizacion val = valorizacionRepo.findById(valorizacionId)
                .orElseThrow(() -> new ValorizacionNoEncontradaException(valorizacionId));

        val.marcarAnulada();
        return valorizacionRepo.save(val);
    }

    /** Agrega un item manual a una valorizacion preliminar. */
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
            throw new PrecondicionVioladaException(
                    "Solo se pueden agregar items a una valorizacion PRELIMINAR.");

        if (monto == null || monto.compareTo(BigDecimal.ZERO) < 0)
            throw new PrecondicionVioladaException("El monto del item no puede ser negativo.");

        if (motivo == null)
            throw new PrecondicionVioladaException("El motivo es obligatorio para un item manual.");

        if (motivo == MotivoManualizacionValorizacion.OTRO_FUNDADO && documentoId == null)
            throw new PrecondicionVioladaException(
                    "OTRO_FUNDADO requiere documentoId hasta que 8F-11G implemente observaciones.");

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

    /**
     * Selecciona la valorizacion operativa vigente (CONFIRMADA+vigente) del acta segun prioridad:
     *   1. CONDENA
     *   2. PAGO_VOLUNTARIO
     *   3. INFRACCION_BASE
     * findVigenteByActaIdAndTipo ya garantiza que solo devuelve CONFIRMADA+siVigente.
     */
    public Optional<FalActaValorizacion> seleccionarOperativa(Long actaId) {
        Optional<FalActaValorizacion> condena = valorizacionRepo
                .findVigenteByActaIdAndTipo(actaId, TipoValorizacionActa.CONDENA);
        if (condena.isPresent()) return condena;

        Optional<FalActaValorizacion> pagVol = valorizacionRepo
                .findVigenteByActaIdAndTipo(actaId, TipoValorizacionActa.PAGO_VOLUNTARIO);
        if (pagVol.isPresent()) return pagVol;

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
