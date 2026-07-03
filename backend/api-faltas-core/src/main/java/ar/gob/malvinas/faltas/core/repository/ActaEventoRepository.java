package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;

import java.util.List;

/**
 * Contrato de persistencia de eventos del acta.
 * Los eventos son append-only: nunca se modifican ni eliminan.
 */
public interface ActaEventoRepository {
    FalActaEvento registrar(FalActaEvento evento);
    List<FalActaEvento> buscarPorActa(Long idActa);
    int proximoOrdenLogico(Long idActa);
}

