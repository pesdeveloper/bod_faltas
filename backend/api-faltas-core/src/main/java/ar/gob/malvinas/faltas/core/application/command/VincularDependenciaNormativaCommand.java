package ar.gob.malvinas.faltas.core.application.command;
public record VincularDependenciaNormativaCommand(Long idDep, int verDep, Long normativaId, String idUserAlta) {}
