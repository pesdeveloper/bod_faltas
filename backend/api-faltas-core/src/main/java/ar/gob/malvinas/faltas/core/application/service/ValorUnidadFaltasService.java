package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.TipoUnidadFaltas;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalValorUnidadFaltas;
import ar.gob.malvinas.faltas.core.repository.ValorUnidadFaltasRepository;
import org.springframework.stereotype.Service;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Gestión del valor monetario historico/vigente de SALARIO/UNIDAD_FIJA
 * (Wave 4 PARTE-2 S7.2). Como máximo un valor activo por tipo.
 */
@Service
public class ValorUnidadFaltasService {

    private final ValorUnidadFaltasRepository repo;
    private final FaltasClock faltasClock;

    public ValorUnidadFaltasService(ValorUnidadFaltasRepository repo, FaltasClock faltasClock) {
        this.repo = repo;
        this.faltasClock = faltasClock;
    }

    public FalValorUnidadFaltas crear(TipoUnidadFaltas tipoUnidad, BigDecimal valorUnidad, LocalDate fhVigDesde, String idUser) {
        if (tipoUnidad == TipoUnidadFaltas.MONTO)
            throw new PrecondicionVioladaException("MONTO no tiene valor de unidad: usa monto_fijo_* en el tarifario.");
        repo.findActivoByTipo(tipoUnidad).ifPresent(existente -> {
            throw new PrecondicionVioladaException(
                    "Ya existe un valor activo (id=" + existente.getId() + ") para tipoUnidad=" + tipoUnidad
                            + ". Usar actualizarValor para reemplazarlo.");
        });
        Long id = repo.nextId();
        FalValorUnidadFaltas v = new FalValorUnidadFaltas(id, tipoUnidad, valorUnidad, fhVigDesde, faltasClock.now(), idUser);
        return repo.save(v);
    }

    /** Cierra la vigencia del valor activo actual y crea uno nuevo. */
    public FalValorUnidadFaltas actualizarValor(TipoUnidadFaltas tipoUnidad, BigDecimal nuevoValor, LocalDate fhVigDesde, String idUser) {
        Optional<FalValorUnidadFaltas> activo = repo.findActivoByTipo(tipoUnidad);
        activo.ifPresent(actual -> {
            if (!fhVigDesde.isAfter(actual.getFhVigDesde()))
                throw new PrecondicionVioladaException(
                        "fhVigDesde del nuevo valor debe ser posterior a la vigencia actual (" + actual.getFhVigDesde() + ").");
            actual.setFhVigHasta(fhVigDesde);
            actual.setSiActiva(false);
            repo.save(actual);
        });
        Long id = repo.nextId();
        FalValorUnidadFaltas nuevo = new FalValorUnidadFaltas(id, tipoUnidad, nuevoValor, fhVigDesde, faltasClock.now(), idUser);
        return repo.save(nuevo);
    }

    public Optional<FalValorUnidadFaltas> resolverActivo(TipoUnidadFaltas tipoUnidad) {
        return repo.findActivoByTipo(tipoUnidad);
    }

    public Optional<FalValorUnidadFaltas> resolverVigente(TipoUnidadFaltas tipoUnidad, LocalDate fecha) {
        return repo.findVigenteByTipo(tipoUnidad, fecha);
    }

    public List<FalValorUnidadFaltas> listarPorTipo(TipoUnidadFaltas tipoUnidad) {
        return repo.findAllByTipo(tipoUnidad);
    }
}
