package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalInspector;
import ar.gob.malvinas.faltas.core.domain.model.FalInspectorVersion;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de inspectores y sus versiones.
 * Reemplazable por implementacion MariaDB/JDBC sin tocar servicios (Slice 9).
 */
public interface InspectorRepository {

    FalInspector guardar(FalInspector inspector);

    Optional<FalInspector> findById(Long idInsp);

    boolean existsByIdUser(String idUser);

    List<FalInspector> findAllActivos();

    FalInspectorVersion guardarVersion(FalInspectorVersion version);

    List<FalInspectorVersion> findVersionesByInsp(Long idInsp);

    Optional<FalInspectorVersion> findVersionVigente(Long idInsp, LocalDate fecha);

    int maxVerInsp(Long idInsp);
}