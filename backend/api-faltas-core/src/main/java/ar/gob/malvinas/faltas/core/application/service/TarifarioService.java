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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Gestión del catálogo de tarifario de unidades de faltas.
 * No permite superponer rangos activos para el mismo tipo.
 * Una fila referenciada no se modifica: se crea una nueva.
 */
@Service
public class TarifarioService {

    private final TarifarioUnidadFaltasRepository tarifarioRepo;
    private final FaltasClock faltasClock;

    public TarifarioService(TarifarioUnidadFaltasRepository tarifarioRepo,
            FaltasClock faltasClock) {
        this.faltasClock = faltasClock;
        this.tarifarioRepo = tarifarioRepo;
    }

    public FalTarifarioUnidadFaltas crear(
            TipoUnidadFaltas tipoUnidad,
            BigDecimal valorUnidad,
            LocalDate fhVigDesde,
            LocalDate fhVigHasta,
            String idUser) {
        if (tipoUnidad == null) throw new PrecondicionVioladaException("tipoUnidad es obligatorio.");
        if (valorUnidad == null || valorUnidad.compareTo(BigDecimal.ZERO) <= 0)
            throw new PrecondicionVioladaException("valorUnidad debe ser mayor que cero.");
        if (fhVigDesde == null) throw new PrecondicionVioladaException("fhVigDesde es obligatoria.");
        if (fhVigHasta != null && fhVigHasta.isBefore(fhVigDesde))
            throw new PrecondicionVioladaException("fhVigHasta no puede ser anterior a fhVigDesde.");

        verificarSinSuperposicion(tipoUnidad, fhVigDesde, fhVigHasta, null);

        Long id = tarifarioRepo.nextId();
        FalTarifarioUnidadFaltas t = new FalTarifarioUnidadFaltas(
                id, tipoUnidad, valorUnidad, fhVigDesde, faltasClock.now(), idUser);
        t.setFhVigHasta(fhVigHasta);
        return tarifarioRepo.save(t);
    }

    public FalTarifarioUnidadFaltas obtener(Long id) {
        return tarifarioRepo.findById(id).orElseThrow(() -> new TarifarioNoEncontradoException(id));
    }

    public List<FalTarifarioUnidadFaltas> listarTodos() {
        return tarifarioRepo.findAll();
    }

    public List<FalTarifarioUnidadFaltas> listarPorTipo(TipoUnidadFaltas tipoUnidad) {
        return tarifarioRepo.findByTipoUnidad(tipoUnidad);
    }

    public Optional<FalTarifarioUnidadFaltas> resolverUltimoVigente(TipoUnidadFaltas tipoUnidad, LocalDate fecha) {
        return tarifarioRepo.findUltimoVigente(tipoUnidad, fecha);
    }

    public FalTarifarioUnidadFaltas desactivar(Long id, String idUser) {
        FalTarifarioUnidadFaltas t = tarifarioRepo.findById(id)
                .orElseThrow(() -> new TarifarioNoEncontradoException(id));
        if (!t.isSiActiva()) throw new PrecondicionVioladaException("El tarifario id=" + id + " ya está inactivo.");
        t.setSiActiva(false);
        return tarifarioRepo.save(t);
    }

    private void verificarSinSuperposicion(TipoUnidadFaltas tipoUnidad, LocalDate desde, LocalDate hasta, Long excluirId) {
        List<FalTarifarioUnidadFaltas> existentes = tarifarioRepo.findByTipoUnidad(tipoUnidad);
        for (FalTarifarioUnidadFaltas e : existentes) {
            if (excluirId != null && excluirId.equals(e.getId())) continue;
            if (!e.isSiActiva()) continue;
            boolean solapa = solapan(desde, hasta, e.getFhVigDesde(), e.getFhVigHasta());
            if (solapa) {
                throw new PrecondicionVioladaException(
                        "Superposición de vigencia con tarifario id=" + e.getId()
                                + " para tipoUnidad=" + tipoUnidad);
            }
        }
    }

    private boolean solapan(LocalDate d1, LocalDate h1, LocalDate d2, LocalDate h2) {
        LocalDate fin1 = (h1 == null) ? LocalDate.MAX : h1;
        LocalDate fin2 = (h2 == null) ? LocalDate.MAX : h2;
        return !d1.isAfter(fin2.minusDays(1)) && !d2.isAfter(fin1.minusDays(1));
    }
}
