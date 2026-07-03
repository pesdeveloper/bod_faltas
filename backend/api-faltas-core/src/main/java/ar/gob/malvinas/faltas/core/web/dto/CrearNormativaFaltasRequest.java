package ar.gob.malvinas.faltas.core.web.dto;
import java.time.LocalDate;
public record CrearNormativaFaltasRequest(
    String codigoNorma,
    int versionNorma,
    String nombreNorma,
    String descripcionNorma,
    LocalDate fhVigDesde,
    String idUserAlta
) {}
