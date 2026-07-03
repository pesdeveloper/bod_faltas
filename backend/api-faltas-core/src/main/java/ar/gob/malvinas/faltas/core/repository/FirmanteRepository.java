package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalFirmante;
import ar.gob.malvinas.faltas.core.domain.model.FalFirmanteVersion;
import ar.gob.malvinas.faltas.core.domain.model.FalFirmanteVersionHabilitacion;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de firmantes, versiones y habilitaciones.
 * Reemplazable por implementacion MariaDB/JDBC sin tocar servicios (Slice 9).
 */
public interface FirmanteRepository {

    FalFirmante guardar(FalFirmante firmante);

    Optional<FalFirmante> findById(Long idFirmante);

    Optional<FalFirmante> findByIdUser(String idUser);

    boolean existsByIdUser(String idUser);

    List<FalFirmante> findAllActivos();

    FalFirmanteVersion guardarVersion(FalFirmanteVersion version);

    List<FalFirmanteVersion> findVersionesByFirmante(Long idFirmante);

    Optional<FalFirmanteVersion> findVersionVigente(Long idFirmante, LocalDate fecha);

    Optional<FalFirmanteVersion> findVersionByFirmanteAndVer(Long idFirmante, int verFirmante);

    int maxVerFirmante(Long idFirmante);

    FalFirmanteVersionHabilitacion guardarHabilitacion(FalFirmanteVersionHabilitacion habilitacion);

    List<FalFirmanteVersionHabilitacion> findHabilitacionesByVersion(Long idFirmante, int verFirmante);

    Optional<FalFirmanteVersionHabilitacion> findHabilitacionActiva(Long idFirmante, int verFirmante,
                                                                     Short tipoDocu, Short rolFirmaReq);

    boolean existeHabilitacionActiva(Long idFirmante, int verFirmante,
                                     Short tipoDocu, Short rolFirmaReq);
}