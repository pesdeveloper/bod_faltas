package ar.gob.malvinas.faltas.core.web.dto;

/**
 * Bloque de estado documental/plantillas en el health demo.
 * Slice 8F-8.
 */
public record DemoHealthDocumentosDto(
        boolean ready,
        int totalPlantillasMock,
        boolean graphDisponible,
        boolean storageReal
) {}
