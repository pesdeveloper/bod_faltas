package ar.gob.malvinas.faltas.core.domain.model;

import java.time.LocalDate;

/**
 * Version vigente de un inspector.
 *
 * Corresponde a fal_inspector_version en el modelo MariaDB productivo.
 * Congela legajoInsp, nomInsp, idDep y verDep al momento de versionar para
 * garantizar trazabilidad historica aunque la dependencia o el inspector cambien.
 *
 * legajoInsp es INT en MariaDB (fal_inspector_version.legajo_insp INT).
 *
 * El tipoActa no vive aqui: se deriva consultando FalDependenciaVersion
 * usando idDep + verDep congelados en esta version.
 *
 * Slice 8A-2: implementacion in-memory. Slice 9: reemplazar por JDBC.
 */
public class FalInspectorVersion {

    private final Long idInsp;
    private final int verInsp;
    private final int legajoInsp;
    private final String nomInsp;
    private final Long idDep;
    private final int verDep;
    private final LocalDate fhVigDesde;
    private LocalDate fhVigHasta;
    private boolean siActivo;

    public FalInspectorVersion(Long idInsp, int verInsp, int legajoInsp, String nomInsp,
                               Long idDep, int verDep, LocalDate fhVigDesde) {
        this.idInsp = idInsp;
        this.verInsp = verInsp;
        this.legajoInsp = legajoInsp;
        this.nomInsp = nomInsp;
        this.idDep = idDep;
        this.verDep = verDep;
        this.fhVigDesde = fhVigDesde;
        this.siActivo = true;
    }

    public Long getIdInsp() { return idInsp; }
    public int getVerInsp() { return verInsp; }
    public int getLegajoInsp() { return legajoInsp; }
    public String getNomInsp() { return nomInsp; }
    public Long getIdDep() { return idDep; }
    public int getVerDep() { return verDep; }
    public LocalDate getFhVigDesde() { return fhVigDesde; }

    public LocalDate getFhVigHasta() { return fhVigHasta; }
    public void setFhVigHasta(LocalDate fhVigHasta) { this.fhVigHasta = fhVigHasta; }

    public boolean isSiActivo() { return siActivo; }
    public void setSiActivo(boolean siActivo) { this.siActivo = siActivo; }

    /**
     * Devuelve true si esta version es vigente para la fecha dada.
     */
    public boolean esVigenteEn(LocalDate fecha) {
        if (!siActivo) return false;
        if (fhVigDesde.isAfter(fecha)) return false;
        if (fhVigHasta != null && !fecha.isBefore(fhVigHasta)) return false;
        return true;
    }
}
