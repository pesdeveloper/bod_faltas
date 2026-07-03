package ar.gob.malvinas.faltas.core.application.command;
import java.time.LocalDate;
public record CrearNormativaFaltasCommand(
    String codigoNorma, int versionNorma, String nombreNorma,
    String descripcionNorma, LocalDate fhVigDesde, String idUserAlta) {}
