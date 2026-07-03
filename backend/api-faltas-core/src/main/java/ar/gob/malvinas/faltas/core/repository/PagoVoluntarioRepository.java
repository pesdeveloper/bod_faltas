package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalPagoVoluntario;

import java.util.Optional;

/**
 * Contrato de persistencia del pago voluntario.
 * Reemplazable por implementacion MariaDB/JDBC sin tocar servicios.
 */
public interface PagoVoluntarioRepository {
    FalPagoVoluntario guardar(FalPagoVoluntario pago);
    Optional<FalPagoVoluntario> buscarPorActa(Long actaId);
}
