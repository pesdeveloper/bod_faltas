package ar.gob.malvinas.faltas.core.web.dto;

/**
 * Item del timeline de eventos append-only del acta demo.
 *
 * Refleja un FalActaEvento real generado por el flujo de dominio.
 *
 * Slice 8F-7.
 */
public record DemoTimelineEventoDto(
        int orden,
        String eventoId,
        String tipoEvento,
        String descripcion,
        String fhEvento
) {}
