package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalRubroVersion;

import java.util.List;
import java.util.Optional;

public interface RubroVersionRepository {
    Long nextId();
    FalRubroVersion guardar(FalRubroVersion version);
    Optional<FalRubroVersion> findByRubroId(Long rubroId);
    Optional<FalRubroVersion> findActualByIdRub(int idRub);
    List<FalRubroVersion> findAllActualesActivas();
    List<FalRubroVersion> findByIdRub(int idRub);
    /**
     * Operacion atomica de sincronizacion: cierra la version actual de idRub (si existe)
     * y guarda la nueva version como actual.
     */
    FalRubroVersion sincronizarAtomicamente(FalRubroVersion nuevaVersion);
}
