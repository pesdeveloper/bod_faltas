package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.MotivoBajaArticuloInfringido;
import ar.gob.malvinas.faltas.core.domain.exception.ActaArticuloInfringidoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.ArticuloNormativaNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.NormativaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaArticuloInfringido;
import ar.gob.malvinas.faltas.core.domain.model.FalArticuloMedidaPreventiva;
import ar.gob.malvinas.faltas.core.domain.model.FalArticuloNormativaFaltas;
import ar.gob.malvinas.faltas.core.domain.model.FalNormativaFaltas;
import ar.gob.malvinas.faltas.core.repository.ActaArticuloInfringidoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ArticuloMedidaPreventivaRepository;
import ar.gob.malvinas.faltas.core.repository.NormativaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Imputación de artículos normativos a actas de faltas.
 * Solo registra qué se imputó. No guarda montos.
 * La corrección da de baja la fila anterior y crea una nueva.
 */
@Service
public class ActaArticuloInfringidoService {

    private final ActaArticuloInfringidoRepository articuloRepo;
    private final ActaRepository actaRepository;
    private final NormativaRepository normativaRepository;
    private final ArticuloMedidaPreventivaRepository articuloMedidaRepo;

    public ActaArticuloInfringidoService(
            ActaArticuloInfringidoRepository articuloRepo,
            ActaRepository actaRepository,
            NormativaRepository normativaRepository,
            ArticuloMedidaPreventivaRepository articuloMedidaRepo) {
        this.articuloRepo = articuloRepo;
        this.actaRepository = actaRepository;
        this.normativaRepository = normativaRepository;
        this.articuloMedidaRepo = articuloMedidaRepo;
    }

    public FalActaArticuloInfringido imputar(
            Long actaId,
            Long normativaId,
            Long articuloId,
            String idUser) {
        FalActa acta = actaRepository.buscarPorId(actaId)
                .orElseThrow(() -> new ActaNoEncontradaException(actaId));

        FalNormativaFaltas normativa = normativaRepository.findNormativaById(normativaId)
                .orElseThrow(() -> new NormativaNoEncontradaException(normativaId));

        if (!normativa.isSiActiva())
            throw new PrecondicionVioladaException("La normativa id=" + normativaId + " no está activa.");

        FalArticuloNormativaFaltas articulo = normativaRepository.findArticuloById(articuloId)
                .orElseThrow(() -> new ArticuloNormativaNoEncontradoException(articuloId));

        if (!articulo.getNormativaId().equals(normativaId))
            throw new PrecondicionVioladaException("El artículo id=" + articuloId + " no pertenece a la normativa id=" + normativaId);

        if (!articulo.isSiActivo())
            throw new PrecondicionVioladaException("El artículo id=" + articuloId + " no está activo.");

        LocalDate hoy = LocalDate.now();
        if (!normativa.esVigenteEn(hoy))
            throw new PrecondicionVioladaException("La normativa id=" + normativaId + " no está vigente en la fecha actual.");
        if (!articulo.esVigenteEn(hoy))
            throw new PrecondicionVioladaException("El artículo id=" + articuloId + " no está vigente en la fecha actual.");

        if (articuloRepo.existsActivo(actaId, articuloId))
            throw new PrecondicionVioladaException("Ya existe una imputación activa del artículo id=" + articuloId + " en el acta id=" + actaId);

        Long id = articuloRepo.nextId();
        FalActaArticuloInfringido imp = new FalActaArticuloInfringido(
                id, actaId, normativaId, articuloId, LocalDateTime.now(), idUser);
        return articuloRepo.save(imp);
    }

    public FalActaArticuloInfringido darDeBaja(
            Long articuloImputadoId,
            MotivoBajaArticuloInfringido motivo,
            String idUser) {
        FalActaArticuloInfringido imp = articuloRepo.findById(articuloImputadoId)
                .orElseThrow(() -> new ActaArticuloInfringidoNoEncontradoException(articuloImputadoId));
        imp.darDeBaja(motivo, LocalDateTime.now(), idUser);
        return articuloRepo.save(imp);
    }

    public FalActaArticuloInfringido corregir(
            Long articuloImputadoId,
            Long nuevaNormativaId,
            Long nuevoArticuloId,
            String idUser) {
        FalActaArticuloInfringido anterior = articuloRepo.findById(articuloImputadoId)
                .orElseThrow(() -> new ActaArticuloInfringidoNoEncontradoException(articuloImputadoId));

        if (!anterior.isSiActivo())
            throw new PrecondicionVioladaException("El artículo imputado id=" + articuloImputadoId + " ya está inactivo.");

        anterior.darDeBaja(MotivoBajaArticuloInfringido.CORRECCION_IMPUTACION, LocalDateTime.now(), idUser);
        articuloRepo.save(anterior);

        return imputar(anterior.getActaId(), nuevaNormativaId, nuevoArticuloId, idUser);
    }

    public List<FalActaArticuloInfringido> listarActivosPorActa(Long actaId) {
        return articuloRepo.findActivosByActaId(actaId);
    }

    public List<FalActaArticuloInfringido> listarTodosPorActa(Long actaId) {
        return articuloRepo.findByActaId(actaId);
    }

    public List<FalArticuloMedidaPreventiva> obtenerMedidasHabilitadas(Long actaId) {
        List<FalActaArticuloInfringido> activos = articuloRepo.findActivosByActaId(actaId);
        return activos.stream()
                .flatMap(a -> articuloMedidaRepo.findActivasByArticuloId(a.getArticuloId()).stream())
                .distinct()
                .toList();
    }
}
