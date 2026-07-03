package ar.gob.malvinas.faltas.core.domain.model;
import java.time.LocalDate;
import java.time.LocalDateTime;
public class FalNormativaFaltas {
    private final Long id;
    private final String codigoNorma;
    private final int versionNorma;
    private final String nombreNorma;
    private String descripcionNorma;
    private boolean siActiva;
    private final LocalDate fhVigDesde;
    private LocalDate fhVigHasta;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;
    public FalNormativaFaltas(Long id, String codigoNorma, int versionNorma, String nombreNorma,
            LocalDate fhVigDesde, LocalDateTime fhAlta, String idUserAlta) {
        this.id = id; this.codigoNorma = codigoNorma; this.versionNorma = versionNorma;
        this.nombreNorma = nombreNorma; this.fhVigDesde = fhVigDesde;
        this.siActiva = true; this.fhAlta = fhAlta; this.idUserAlta = idUserAlta;
    }
    public Long getId() { return id; }
    public String getCodigoNorma() { return codigoNorma; }
    public int getVersionNorma() { return versionNorma; }
    public String getNombreNorma() { return nombreNorma; }
    public String getDescripcionNorma() { return descripcionNorma; }
    public void setDescripcionNorma(String v) { this.descripcionNorma = v; }
    public boolean isSiActiva() { return siActiva; }
    public void setSiActiva(boolean v) { this.siActiva = v; }
    public LocalDate getFhVigDesde() { return fhVigDesde; }
    public LocalDate getFhVigHasta() { return fhVigHasta; }
    public void setFhVigHasta(LocalDate v) { this.fhVigHasta = v; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
    public boolean esVigenteEn(LocalDate fecha) {
        if (!siActiva) return false;
        if (fhVigDesde.isAfter(fecha)) return false;
        if (fhVigHasta != null && !fecha.isBefore(fhVigHasta)) return false;
        return true;
    }
}
