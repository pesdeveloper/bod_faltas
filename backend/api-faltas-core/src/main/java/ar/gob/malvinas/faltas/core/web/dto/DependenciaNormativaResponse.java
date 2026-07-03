package ar.gob.malvinas.faltas.core.web.dto;
import ar.gob.malvinas.faltas.core.domain.model.FalDependenciaNormativa;
import java.time.LocalDateTime;
public record DependenciaNormativaResponse(
    Long idDep,
    int verDep,
    Long normativaId,
    boolean siActiva,
    LocalDateTime fhAlta
) {
    public static DependenciaNormativaResponse de(FalDependenciaNormativa r) {
        return new DependenciaNormativaResponse(
            r.getIdDep(), r.getVerDep(), r.getNormativaId(),
            r.isSiActiva(), r.getFhAlta());
    }
}
