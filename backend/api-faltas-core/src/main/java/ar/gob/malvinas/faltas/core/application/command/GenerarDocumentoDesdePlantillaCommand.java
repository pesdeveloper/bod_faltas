package ar.gob.malvinas.faltas.core.application.command;

public record GenerarDocumentoDesdePlantillaCommand(
        Long idActa,
        Long plantillaId,
        String idUserAlta) {}
