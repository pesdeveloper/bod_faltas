package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalDependencia;
import ar.gob.malvinas.faltas.core.domain.model.FalDependenciaVersion;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de dependencias y sus versiones.
 * Reemplazable por implementacion MariaDB/JDBC sin tocar servicios (Slice 9).
 */
public interface DependenciaRepository {

    FalDependencia guardar(FalDependencia dependencia);

    Optional<FalDependencia> findById(Long idDep);

    List<FalDependencia> findAllActivas();

    boolean existsByCodDep(String codDep);

    Optional<FalDependencia> findByCodDep(String codDep);

    FalDependenciaVersion guardarVersion(FalDependenciaVersion version);

    List<FalDependenciaVersion> findVersionesByDep(Long idDep);

    Optional<FalDependenciaVersion> findVersionVigente(Long idDep, LocalDate fecha);

    int maxVerDep(Long idDep);
}
