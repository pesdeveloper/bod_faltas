package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.ModoValorizacionItem;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaArticuloInfringido;
import ar.gob.malvinas.faltas.core.domain.model.FalActaValorizacion;
import ar.gob.malvinas.faltas.core.domain.model.FalActaValorizacionItem;
import ar.gob.malvinas.faltas.core.domain.model.FalArticuloNormativaFaltas;
import ar.gob.malvinas.faltas.core.domain.model.FalTarifarioUnidadFaltas;
import ar.gob.malvinas.faltas.core.domain.model.FalValorUnidadFaltas;
import ar.gob.malvinas.faltas.core.repository.ActaArticuloInfringidoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaValorizacionItemRepository;
import ar.gob.malvinas.faltas.core.repository.ActaValorizacionRepository;
import ar.gob.malvinas.faltas.core.repository.NormativaRepository;
import ar.gob.malvinas.faltas.core.repository.TarifarioUnidadFaltasRepository;
import ar.gob.malvinas.faltas.core.repository.ValorUnidadFaltasRepository;
import org.springframework.stereotype.Service;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Calculo y persistencia de la valorizacion de un acta.
 *
 * Rediseño PROMPT-NOCTURNO-WAVE-04-PARTE-2 S7: cabecera unica por acta
 * ({@link FalActaValorizacion}) con historial versionado por articulo
 * ({@link FalActaValorizacionItem}, hijo de {@link FalActaArticuloInfringido}).
 * El alta calcula pero nunca confirma (valorizacion no es obligacion de pago).
 */
@Service
public class ValorizacionService {

    private final ActaValorizacionRepository valorizacionRepo;
    private final ActaValorizacionItemRepository itemRepo;
    private final ActaArticuloInfringidoRepository articuloInfringidoRepo;
    private final NormativaRepository normativaRepo;
    private final TarifarioUnidadFaltasRepository tarifarioRepo;
    private final ValorUnidadFaltasRepository valorUnidadRepo;
    private final ActaRepository actaRepo;
    private final ValorizacionCalculador calculador;
    private final FaltasClock faltasClock;

    public ValorizacionService(
            ActaValorizacionRepository valorizacionRepo,
            ActaValorizacionItemRepository itemRepo,
            ActaArticuloInfringidoRepository articuloInfringidoRepo,
            NormativaRepository normativaRepo,
            TarifarioUnidadFaltasRepository tarifarioRepo,
            ValorUnidadFaltasRepository valorUnidadRepo,
            ActaRepository actaRepo,
            ValorizacionCalculador calculador,
            FaltasClock faltasClock) {
        this.valorizacionRepo = valorizacionRepo;
        this.itemRepo = itemRepo;
        this.articuloInfringidoRepo = articuloInfringidoRepo;
        this.normativaRepo = normativaRepo;
        this.tarifarioRepo = tarifarioRepo;
        this.valorUnidadRepo = valorUnidadRepo;
        this.actaRepo = actaRepo;
        this.calculador = calculador;
        this.faltasClock = faltasClock;
    }

    /**
     * Calcula automaticamente la version 1 del item de valorizacion de CADA articulo
     * activo del acta y la cabecera agregada. Idempotente respecto de items ya
     * calculados: un articulo que ya tiene item actual se omite (no se recalcula
     * silenciosamente dentro del alta).
     *
     * @return la cabecera (creada o actualizada), o {@code Optional.empty()} si el
     *         acta no tiene articulos activos (nada que valorizar).
     */
    public Optional<FalActaValorizacion> calcularInicial(Long actaId, String actor, LocalDateTime ahora) {
        actaRepo.buscarPorId(actaId).orElseThrow(() -> new ActaNoEncontradaException(actaId));
        List<FalActaArticuloInfringido> activos = articuloInfringidoRepo.findActivosByActaId(actaId);
        if (activos.isEmpty()) return Optional.empty();

        LocalDate hoy = ahora.toLocalDate();
        BigDecimal totalOrdinario = BigDecimal.ZERO;
        BigDecimal totalPagoVoluntario = null;
        boolean huboPagoVoluntario = false;

        for (FalActaArticuloInfringido imp : activos) {
            if (itemRepo.findActualByActaArticulo(imp.getId()).isPresent()) {
                continue; // ya calculado: no se recalcula silenciosamente
            }
            FalArticuloNormativaFaltas articulo = normativaRepo.findArticuloById(imp.getArticuloId())
                    .orElseThrow(() -> new PrecondicionVioladaException(
                            "El articulo id=" + imp.getArticuloId() + " referenciado por la imputacion id="
                                    + imp.getId() + " no existe en el catalogo. Integridad rota."));

            FalTarifarioUnidadFaltas tarifario = tarifarioRepo.findVigenteByArticulo(articulo.getId(), hoy)
                    .or(() -> tarifarioRepo.findActivaByArticulo(articulo.getId()))
                    .orElseThrow(() -> new PrecondicionVioladaException(
                            "No hay formula tarifaria activa para articuloId=" + articulo.getId()
                                    + ". Valorizacion no puede calcularse."));

            FalValorUnidadFaltas valorUnidad = resolverValorUnidad(tarifario, hoy);

            ValorizacionCalculador.ResultadoDimension ordinaria =
                    calculador.calcularOrdinarioAutomatico(tarifario, valorUnidad);
            totalOrdinario = totalOrdinario.add(ordinaria.monto());

            ValorizacionCalculador.ResultadoDimension pagoVoluntario = null;
            if (tarifario.isSiAdmitePagoVoluntario()) {
                pagoVoluntario = calculador.calcularPagoVoluntarioAutomatico(tarifario, valorUnidad);
                totalPagoVoluntario = (totalPagoVoluntario == null ? BigDecimal.ZERO : totalPagoVoluntario)
                        .add(pagoVoluntario.monto());
                huboPagoVoluntario = true;
            }

            Long itemId = itemRepo.nextId();
            FalActaValorizacionItem item = new FalActaValorizacionItem(
                    itemId, imp.getId(), 1, true,
                    tarifario.getId(), valorUnidad == null ? null : valorUnidad.getId(),
                    ModoValorizacionItem.AUTOMATICA, null, null,
                    tarifario.isSiAdmitePagoVoluntario() ? ModoValorizacionItem.AUTOMATICA : null, null, null,
                    null, null, ahora, actor);
            itemRepo.save(item);
        }

        Optional<FalActaValorizacion> existente = valorizacionRepo.findByActaId(actaId);
        BigDecimal totalPagoVolFinal = huboPagoVoluntario ? totalPagoVoluntario : null;
        if (existente.isPresent()) {
            FalActaValorizacion actual = existente.get();
            BigDecimal nuevoOrdinario = actual.getMontoTotalOrdinario().add(totalOrdinario);
            BigDecimal nuevoPagoVol = sumarNullable(actual.getMontoTotalPagoVoluntario(), totalPagoVolFinal);
            actual.recalcular(nuevoOrdinario, nuevoPagoVol, ahora);
            return Optional.of(valorizacionRepo.save(actual));
        }
        Long valId = valorizacionRepo.nextId();
        FalActaValorizacion header = new FalActaValorizacion(
                valId, actaId, totalOrdinario, totalPagoVolFinal, ahora, ahora, actor);
        return Optional.of(valorizacionRepo.save(header));
    }

    /**
     * Vuelve a AUTOMATICA usando la MISMA referencia historica ya usada por el item actual
     * ({@code tarifarioArticuloUsadoId}/{@code valorUnidadUsadoId}): recalcula sin saltar a
     * valores vigentes (Wave 4 PARTE-3A S14, "volver a automatico historico").
     */
    public FalActaValorizacionItem volverAAutomaticoHistorico(Long actaArticuloId, String actor, LocalDateTime ahora) {
        FalActaValorizacionItem actual = itemRepo.findActualByActaArticulo(actaArticuloId)
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "No hay item de valorizacion actual para actaArticuloId=" + actaArticuloId));
        FalTarifarioUnidadFaltas tarifario = tarifarioRepo.findById(actual.getTarifarioArticuloUsadoId())
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "El tarifario historico id=" + actual.getTarifarioArticuloUsadoId()
                                + " ya no existe. No se puede volver a automatico historico."));
        FalValorUnidadFaltas valorUnidad = actual.getValorUnidadUsadoId() == null ? null
                : valorUnidadRepo.findById(actual.getValorUnidadUsadoId())
                        .orElseThrow(() -> new PrecondicionVioladaException(
                                "El valor de unidad historico id=" + actual.getValorUnidadUsadoId()
                                        + " ya no existe. No se puede volver a automatico historico."));
        return recalcularConReferencia(actual, actaArticuloId, tarifario, valorUnidad,
                "VOLVER_A_AUTOMATICO_HISTORICO", actor, ahora);
    }

    /**
     * Crea una nueva version AUTOMATICA usando el tarifario y el valor de unidad activos/vigentes
     * HOY del articulo (pueden diferir de los historicos usados por el item actual). Wave 4
     * PARTE-3A S14, "actualizar a valores vigentes".
     */
    public FalActaValorizacionItem actualizarAValoresVigentes(Long actaArticuloId, String actor, LocalDateTime ahora) {
        FalActaValorizacionItem actual = itemRepo.findActualByActaArticulo(actaArticuloId)
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "No hay item de valorizacion actual para actaArticuloId=" + actaArticuloId));
        FalActaArticuloInfringido imputacion = articuloInfringidoRepo.findById(actaArticuloId)
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "La imputacion actaArticuloId=" + actaArticuloId + " no existe."));
        LocalDate hoy = ahora.toLocalDate();
        FalArticuloNormativaFaltas articulo = normativaRepo.findArticuloById(imputacion.getArticuloId())
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "El articulo id=" + imputacion.getArticuloId() + " no existe en el catalogo."));
        FalTarifarioUnidadFaltas tarifarioVigente = tarifarioRepo.findVigenteByArticulo(articulo.getId(), hoy)
                .or(() -> tarifarioRepo.findActivaByArticulo(articulo.getId()))
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "No hay formula tarifaria vigente para articuloId=" + articulo.getId()
                                + ". No se puede actualizar a valores vigentes."));
        FalValorUnidadFaltas valorUnidadVigente = resolverValorUnidad(tarifarioVigente, hoy);
        return recalcularConReferencia(actual, actaArticuloId, tarifarioVigente, valorUnidadVigente,
                "ACTUALIZAR_A_VALORES_VIGENTES", actor, ahora);
    }

    /**
     * Crea la siguiente version AUTOMATICA del item con la referencia (tarifario, valorUnidad)
     * indicada, y ajusta la cabecera del acta por la diferencia entre el monto anterior (con la
     * referencia que tenia el item actual) y el nuevo (con la referencia recibida). Nunca usa
     * {@code MAX(version)+1} sin proteccion: delega en {@code siguienteVersion} sobre la fila
     * actual, y el repositorio JDBC aplica OCC/UNIQUE de {@code (acta_articulo_id, si_actual)}.
     */
    private FalActaValorizacionItem recalcularConReferencia(
            FalActaValorizacionItem actual, Long actaArticuloId,
            FalTarifarioUnidadFaltas tarifarioNuevo, FalValorUnidadFaltas valorUnidadNuevo,
            String motivo, String actor, LocalDateTime ahora) {
        FalTarifarioUnidadFaltas tarifarioAnterior = tarifarioRepo.findById(actual.getTarifarioArticuloUsadoId())
                .orElse(tarifarioNuevo);
        FalValorUnidadFaltas valorUnidadAnterior = actual.getValorUnidadUsadoId() == null ? null
                : valorUnidadRepo.findById(actual.getValorUnidadUsadoId()).orElse(valorUnidadNuevo);

        ValorizacionCalculador.ResultadoDimension ordinariaAnterior =
                calculador.calcularOrdinarioAutomatico(tarifarioAnterior, valorUnidadAnterior);
        ValorizacionCalculador.ResultadoDimension pagoVolAnterior = tarifarioAnterior.isSiAdmitePagoVoluntario()
                ? calculador.calcularPagoVoluntarioAutomatico(tarifarioAnterior, valorUnidadAnterior) : null;

        ValorizacionCalculador.ResultadoDimension ordinariaNueva =
                calculador.calcularOrdinarioAutomatico(tarifarioNuevo, valorUnidadNuevo);
        ValorizacionCalculador.ResultadoDimension pagoVolNueva = tarifarioNuevo.isSiAdmitePagoVoluntario()
                ? calculador.calcularPagoVoluntarioAutomatico(tarifarioNuevo, valorUnidadNuevo) : null;

        Long nuevoId = itemRepo.nextId();
        FalActaValorizacionItem nueva = actual.siguienteVersion(
                nuevoId, tarifarioNuevo.getId(), valorUnidadNuevo == null ? null : valorUnidadNuevo.getId(),
                ModoValorizacionItem.AUTOMATICA, null, null,
                tarifarioNuevo.isSiAdmitePagoVoluntario() ? ModoValorizacionItem.AUTOMATICA : null, null, null,
                motivo, null, ahora, actor);
        itemRepo.marcarNoActual(actual.getId());
        FalActaValorizacionItem guardada = itemRepo.save(nueva);

        Long idActa = articuloInfringidoRepo.findById(actaArticuloId)
                .map(FalActaArticuloInfringido::getActaId)
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "La imputacion actaArticuloId=" + actaArticuloId + " no existe."));
        FalActaValorizacion header = valorizacionRepo.findByActaId(idActa)
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "No hay cabecera de valorizacion para actaId=" + idActa));
        BigDecimal nuevoOrdinario = header.getMontoTotalOrdinario()
                .subtract(ordinariaAnterior.monto())
                .add(ordinariaNueva.monto());
        BigDecimal nuevoPagoVol = ajustarPagoVoluntario(
                header.getMontoTotalPagoVoluntario(), pagoVolAnterior, pagoVolNueva);
        header.recalcular(nuevoOrdinario, nuevoPagoVol, ahora);
        valorizacionRepo.save(header);

        return guardada;
    }

    /** Ajusta el total de pago voluntario de la cabecera restando la contribucion anterior del
     * item y sumando la nueva, tolerando que cualquiera de las tres sea {@code null}. */
    private static BigDecimal ajustarPagoVoluntario(
            BigDecimal totalActual,
            ValorizacionCalculador.ResultadoDimension anterior,
            ValorizacionCalculador.ResultadoDimension nueva) {
        BigDecimal base = totalActual == null ? BigDecimal.ZERO : totalActual;
        if (anterior != null) base = base.subtract(anterior.monto());
        if (nueva != null) base = base.add(nueva.monto());
        if (anterior == null && nueva == null) return totalActual;
        return base;
    }

    private FalValorUnidadFaltas resolverValorUnidad(FalTarifarioUnidadFaltas tarifario, LocalDate hoy) {
        if (tarifario.getTipoUnidad() == ar.gob.malvinas.faltas.core.domain.enums.TipoUnidadFaltas.MONTO) {
            return null;
        }
        return valorUnidadRepo.findVigenteByTipo(tarifario.getTipoUnidad(), hoy)
                .or(() -> valorUnidadRepo.findActivoByTipo(tarifario.getTipoUnidad()))
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "No hay valor de unidad vigente para tipoUnidad=" + tarifario.getTipoUnidad()
                                + " en fecha=" + hoy + ". Valorizacion no puede calcularse."));
    }

    private static BigDecimal sumarNullable(BigDecimal a, BigDecimal b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.add(b);
    }

    /** Cabecera de valorizacion del acta, si existe. Usado por el snapshot. */
    public Optional<FalActaValorizacion> seleccionarOperativa(Long actaId) {
        return valorizacionRepo.findByActaId(actaId);
    }

    public List<FalActaValorizacionItem> listarItemsActuales(List<Long> actaArticuloIds) {
        return itemRepo.findActualesByActaArticuloIds(actaArticuloIds);
    }

    /**
     * Sobrescribe manualmente la dimension ordinaria y/o de pago voluntario de un
     * articulo, creando una nueva version del item (nunca sobrescribe la anterior).
     */
    public FalActaValorizacionItem manualizarItem(
            Long actaArticuloId,
            ModoValorizacionItem modoOrdinaria,
            BigDecimal cantidadManualOrdinaria,
            BigDecimal montoManualOrdinario,
            ModoValorizacionItem modoPagoVoluntario,
            BigDecimal cantidadManualPagoVoluntario,
            BigDecimal montoManualPagoVoluntario,
            String motivoCambio,
            String observacionCambio,
            String actor,
            LocalDateTime ahora) {
        FalActaValorizacionItem actual = itemRepo.findActualByActaArticulo(actaArticuloId)
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "No hay item de valorizacion actual para actaArticuloId=" + actaArticuloId));
        if (motivoCambio == null || motivoCambio.isBlank())
            throw new PrecondicionVioladaException("motivoCambio es obligatorio para manualizar un item.");

        Long nuevoId = itemRepo.nextId();
        FalActaValorizacionItem nueva = actual.siguienteVersion(
                nuevoId, actual.getTarifarioArticuloUsadoId(), actual.getValorUnidadUsadoId(),
                modoOrdinaria, cantidadManualOrdinaria, montoManualOrdinario,
                modoPagoVoluntario, cantidadManualPagoVoluntario, montoManualPagoVoluntario,
                motivoCambio, observacionCambio, ahora, actor);
        itemRepo.marcarNoActual(actual.getId());
        return itemRepo.save(nueva);
    }
}
