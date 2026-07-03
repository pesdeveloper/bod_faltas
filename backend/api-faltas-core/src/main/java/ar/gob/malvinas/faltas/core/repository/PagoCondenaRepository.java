package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalPagoCondena;

import java.util.Optional;

/**
 * Contrato de persistencia del pago de condena.
 * Reemplazable por implementacion MariaDB/JDBC sin tocar servicios.
 */
public interface PagoCondenaRepository {
    FalPagoCondena guardar(FalPagoCondena pago);
    Optional<FalPagoCondena> buscarPorActa(Long actaId);
}

