package ar.gob.malvinas.faltas.core.application.command;

/** Comando para pasar una apelacion PRESENTADA al estado EN_ANALISIS. */
public record PasarApelacionAAnalisisCommand(
        Long apelacionId,
        String idUserOperacion
) {}