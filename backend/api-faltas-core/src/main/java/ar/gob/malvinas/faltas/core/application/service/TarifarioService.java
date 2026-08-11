package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.TipoUnidadFaltas;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.exception.TarifarioNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.model.FalTarifarioUnidadFaltas;
import ar.gob.malvinas.faltas.core.repository.TarifarioUnidadFaltasRepository;
import org.springframework.stereotype.Service;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Gestión de la formula tarifaria por articulo (Wave 4 PARTE-2 S7.3).
 * Como máximo una fórmula activa por artículo (UK real en el DDL).
 * Un cambio de fórmula cierra la vigencia de la anterior y crea una nueva.
 */
@Service
public class TarifarioService {

    private final TarifarioUnidadFaltasRepository tarifarioRepo;
    private final FaltasClock faltasClock;

    public TarifarioService(TarifarioUnidadFaltasRepository tarifarioRepo, FaltasClock faltasClock) {
        this.faltasClock = faltasClock;
        this.tarifarioRepo = tarifarioRepo;
    }

    public FalTarifarioUnidadFaltas crearParaArticulo(
            Long articuloId,
            TipoUnidadFaltas tipoUnidad,
            BigDecimal cantidadUnidadesOrdinaria,
            BigDecimal montoFijoOrdinario,
            boolean siAdmitePagoVoluntario,
            BigDecimal cantidadUnidadesPagoVoluntario,
            BigDecimal montoFijoPagoVoluntario,
            LocalDate fhVigDesde,
            String idUser) {
        if (articuloId == null) throw new PrecondicionVioladaException("articuloId es obligatorio.");
        if (tipoUnidad == null) throw new PrecondicionVioladaException("tipoUnidad es obligatorio.");
        if (fhVigDesde == null) throw new PrecondicionVioladaException("fhVigDesde es obligatoria.");

        tarifarioRepo.findActivaByArticulo(articuloId).ifPresent(existente -> {
            throw new PrecondicionVioladaException(
                    "Ya existe una formula activa (id=" + existente.getId() + ") para el articuloId=" + articuloId
                            + ". Usar cambiarFormula para reemplazarla.");
        });

        Long id = tarifarioRepo.nextId();
        FalTarifarioUnidadFaltas t = new FalTarifarioUnidadFaltas(
                id, articuloId, tipoUnidad, cantidadUnidadesOrdinaria, montoFijoOrdinario,
                siAdmitePagoVoluntario, cantidadUnidadesPagoVoluntario, montoFijoPagoVoluntario,
                fhVigDesde, faltasClock.now(), idUser);
        return tarifarioRepo.save(t);
    }

    /** Cierra la vigencia de la fórmula activa actual y crea una nueva activa. */
    public FalTarifarioUnidadFaltas cambiarFormula(
            Long articuloId,
            TipoUnidadFaltas tipoUnidad,
            BigDecimal cantidadUnidadesOrdinaria,
            BigDecimal montoFijoOrdinario,
            boolean siAdmitePagoVoluntario,
            BigDecimal cantidadUnidadesPagoVoluntario,
            BigDecimal montoFijoPagoVoluntario,
            LocalDate fhVigDesde,
            String idUser) {
        Optional<FalTarifarioUnidadFaltas> activaActual = tarifarioRepo.findActivaByArticulo(articuloId);
        activaActual.ifPresent(actual -> {
            if (!fhVigDesde.isAfter(actual.getFhVigDesde()))
                throw new PrecondicionVioladaException(
                        "fhVigDesde de la nueva formula debe ser posterior a la vigencia actual (" + actual.getFhVigDesde() + ").");
            actual.setFhVigHasta(fhVigDesde);
            actual.setSiActiva(false);
            tarifarioRepo.save(actual);
        });

        Long id = tarifarioRepo.nextId();
        FalTarifarioUnidadFaltas nueva = new FalTarifarioUnidadFaltas(
                id, articuloId, tipoUnidad, cantidadUnidadesOrdinaria, montoFijoOrdinario,
                siAdmitePagoVoluntario, cantidadUnidadesPagoVoluntario, montoFijoPagoVoluntario,
                fhVigDesde, faltasClock.now(), idUser);
        return tarifarioRepo.save(nueva);
    }

    public FalTarifarioUnidadFaltas obtener(Long id) {
        return tarifarioRepo.findById(id).orElseThrow(() -> new TarifarioNoEncontradoException(id));
    }

    public List<FalTarifarioUnidadFaltas> listarPorArticulo(Long articuloId) {
        return tarifarioRepo.findAllByArticulo(articuloId);
    }

    public Optional<FalTarifarioUnidadFaltas> resolverActiva(Long articuloId) {
        return tarifarioRepo.findActivaByArticulo(articuloId);
    }

    public Optional<FalTarifarioUnidadFaltas> resolverVigente(Long articuloId, LocalDate fecha) {
        return tarifarioRepo.findVigenteByArticulo(articuloId, fecha);
    }

    public FalTarifarioUnidadFaltas desactivar(Long id, String idUser) {
        FalTarifarioUnidadFaltas t = tarifarioRepo.findById(id)
                .orElseThrow(() -> new TarifarioNoEncontradoException(id));
        if (!t.isSiActiva()) throw new PrecondicionVioladaException("El tarifario id=" + id + " ya está inactivo.");
        t.setSiActiva(false);
        return tarifarioRepo.save(t);
    }
}
