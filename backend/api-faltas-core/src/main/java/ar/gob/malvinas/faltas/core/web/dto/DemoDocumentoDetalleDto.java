package ar.gob.malvinas.faltas.core.web.dto;

/**
 * Item de documento asociado al acta demo.
 *
 * Refleja un FalDocumento real generado por el flujo de dominio.
 * Guardrail: storageKey debe ser mock:// o storage:// cuando existe.
 * hashDocu debe tener prefijo sha256-mock- cuando existe.
 *
 * Slice 8F-7.
 */
public record DemoDocumentoDetalleDto(
        Long documentoId,
        String tipoDocu,
        String estadoDocu,
        String storageKey,
        String hashDocu,
        boolean mock,
        String fhGeneracion,
        Long plantillaId,
        String descripcion
) {}
