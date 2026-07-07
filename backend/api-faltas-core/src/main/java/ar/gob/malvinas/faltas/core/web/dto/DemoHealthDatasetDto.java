package ar.gob.malvinas.faltas.core.web.dto;

/**
 * Bloque de estado del dataset funcional en el health demo.
 * Slice 8F-8.
 */
public record DemoHealthDatasetDto(
        boolean ready,
        int totalActasMock,
        boolean coberturaCompleta,
        boolean detalleDisponible
) {}