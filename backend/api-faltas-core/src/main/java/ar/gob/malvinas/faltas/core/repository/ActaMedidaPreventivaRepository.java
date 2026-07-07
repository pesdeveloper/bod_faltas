package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActaMedidaPreventiva;

import java.util.List;
import java.util.Optional;

public interface ActaMedidaPreventivaRepository {
    Long nextId();
    FalActaMedidaPreventiva guardar(FalActaMedidaPreventiva medida);
    Optional<FalActaMedidaPreventiva> findById(Long id);
    List<FalActaMedidaPreventiva> findByActaId(Long actaId);
    List<FalActaMedidaPreventiva> findActivasByActaId(Long actaId);
}
