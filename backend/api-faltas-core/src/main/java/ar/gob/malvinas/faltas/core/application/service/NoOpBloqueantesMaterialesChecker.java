package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.port.BloqueantesMaterialesChecker;

/**
 * Implementacion stub del checker de bloqueantes materiales para uso en tests.
 * Devuelve siempre false: ningun acta tiene bloqueantes activos.
 *
 * No es @Component. No se inyecta en produccion.
 * En produccion se usa RepositoryBloqueantesMaterialesChecker (@Component, Slice 7A).
 * En tests: instanciar directamente con new, o usar lambda actaId -> true/false.
 */
public class NoOpBloqueantesMaterialesChecker implements BloqueantesMaterialesChecker {

    @Override
    public boolean tieneBloqueantesActivos(Long actaId) {
        return false;
    }
}
