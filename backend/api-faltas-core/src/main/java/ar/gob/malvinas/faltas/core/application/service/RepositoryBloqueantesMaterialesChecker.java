package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.port.BloqueantesMaterialesChecker;
import ar.gob.malvinas.faltas.core.repository.BloqueanteMaterialRepository;
import org.springframework.stereotype.Component;

/**
 * Implementacion real del checker de bloqueantes materiales.
 *
 * Consulta BloqueanteMaterialRepository para determinar si existen bloqueantes
 * activos sobre el acta. Reemplaza NoOpBloqueantesMaterialesChecker en produccion.
 *
 * Slice 7A: implementacion in-memory (via InMemoryBloqueanteMaterialRepository).
 * Slice 9: el repositorio sera reemplazado por implementacion JDBC sin tocar este checker.
 */
@Component
public class RepositoryBloqueantesMaterialesChecker implements BloqueantesMaterialesChecker {

    private final BloqueanteMaterialRepository bloqueanteMaterialRepository;

    public RepositoryBloqueantesMaterialesChecker(BloqueanteMaterialRepository bloqueanteMaterialRepository) {
        this.bloqueanteMaterialRepository = bloqueanteMaterialRepository;
    }

    @Override
    public boolean tieneBloqueantesActivos(Long actaId) {
        return bloqueanteMaterialRepository.existsActivoByActaId(actaId);
    }
}

