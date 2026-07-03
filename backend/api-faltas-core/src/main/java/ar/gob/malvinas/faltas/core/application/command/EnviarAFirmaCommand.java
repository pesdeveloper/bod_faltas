package ar.gob.malvinas.faltas.core.application.command;

/**
 * Comando para enviar un documento a firma.
 *
 * Transicion BORRADOR -> PENDIENTE_FIRMA.
 * Si momentoNumeracionDocu = AL_ENVIAR_A_FIRMA, numera el documento primero.
 * Si momentoNumeracionDocu = AL_CREAR y el documento no tiene nroDocu, falla por inconsistencia.
 * Materializa firma_req desde plantilla si no existia.
 *
 * Slice 8C-5B.
 */
public record EnviarAFirmaCommand(
        Long documentoId,
        String idUserOperacion
) {}