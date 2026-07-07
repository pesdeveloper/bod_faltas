package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActaSustanciasAlimenticias;

import java.util.Optional;

public interface ActaSustanciasAlimenticiasRepository {
    FalActaSustanciasAlimenticias guardar(FalActaSustanciasAlimenticias sustancias);
    Optional<FalActaSustanciasAlimenticias> findByActaId(Long actaId);
    boolean existsByActaId(Long actaId);
}
