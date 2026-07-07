package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de eventos del acta.
 *
 * Los eventos son append-only: nunca se modifican ni eliminan.
 * El orden del timeline es por fhEvt + id; no se usa ordenLogico.
 * El id es Long auto-generado por la implementacion (BIGINT AUTO_INCREMENT).
 *
 * Garantias de la implementacion:
 * - append() asigna id monotonamente creciente y retorna el evento con id.
 * - buscarPorActa() retorna eventos ordenados por fhEvt + id.
 * - No existe metodo de update, delete ni reordenamiento.
 */
public interface ActaEventoRepository {

    /** Registra un nuevo evento (append-only). Asigna id y retorna el evento con id. */
    FalActaEvento registrar(FalActaEvento evento);

    /** Lista todos los eventos del acta ordenados por fhEvt + id (timeline). */
    List<FalActaEvento> buscarPorActa(Long idActa);

    /** Busca un evento por su id tecnico. */
    Optional<FalActaEvento> buscarPorId(Long id);

    /**
     * Verifica si ya existe un evento con el mismo correlacionId para la acta.
     * Usado para idempotencia en comandos externos.
     */
    boolean existeCorrelacion(Long idActa, String correlacionId);
}