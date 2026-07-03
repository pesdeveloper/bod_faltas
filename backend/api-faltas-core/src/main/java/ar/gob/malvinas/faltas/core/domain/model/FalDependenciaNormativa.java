package ar.gob.malvinas.faltas.core.domain.model;
import java.time.LocalDateTime;
public class FalDependenciaNormativa {
    private final Long idDep;
    private final int verDep;
    private final Long normativaId;
    private boolean siActiva;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;
    public FalDependenciaNormativa(Long idDep, int verDep, Long normativaId,
            LocalDateTime fhAlta, String idUserAlta) {
        this.idDep = idDep; this.verDep = verDep; this.normativaId = normativaId;
        this.siActiva = true; this.fhAlta = fhAlta; this.idUserAlta = idUserAlta;
    }
    public Long getIdDep() { return idDep; }
    public int getVerDep() { return verDep; }
    public Long getNormativaId() { return normativaId; }
    public boolean isSiActiva() { return siActiva; }
    public void setSiActiva(boolean v) { this.siActiva = v; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
}
