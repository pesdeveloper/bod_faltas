package ar.gob.malvinas.faltas.core.web.dto;
import ar.gob.malvinas.faltas.core.domain.model.FalNormativaFaltas;
import java.time.LocalDate;
import java.time.LocalDateTime;
public record NormativaFaltasResponse(
    Long id,
    String codigoNorma,
    int versionNorma,
    String nombreNorma,
    String descripcionNorma,
    boolean siActiva,
    LocalDate fhVigDesde,
    LocalDate fhVigHasta,
    LocalDateTime fhAlta
) {
    public static NormativaFaltasResponse de(FalNormativaFaltas n) {
        return new NormativaFaltasResponse(
            n.getId(), n.getCodigoNorma(), n.getVersionNorma(),
            n.getNombreNorma(), n.getDescripcionNorma(), n.isSiActiva(),
            n.getFhVigDesde(), n.getFhVigHasta(), n.getFhAlta());
    }
}
