package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;

import java.time.LocalDate;

/**
 * Version vigente de una dependencia organica.
 *
 * Corresponde a fal_dependencia_version en el modelo MariaDB productivo.
 * Una dependencia puede tener multiples versiones historicas; la vigente
 * es la que tiene siActiva=true y cuya fhVigDesde <= hoy < fhVigHasta (o fhVigHasta nula).
 *
 * El campo tipoActa define el unico tipo de acta que puede labrar esa dependencia.
 * El modelo congela nomDep e idDepPadre al momento de versionar para garantizar
 * trazabilidad historica aunque la dependencia padre cambie.
 *
 * Slice 8A-1: implementacion in-memory. Slice 9: reemplazar por JDBC.
 */
public class FalDependenciaVersion {

    private final Long idDep;
    private final int verDep;
    private final String nomDep;
    private final Long idDepPadre;
    private final Integer verDepPadre;
    private final TipoActa tipoActa;
    private final LocalDate fhVigDesde;
    private LocalDate fhVigHasta;
    private boolean siActiva;

    public FalDependenciaVersion(
            Long idDep,
            int verDep,
            String nomDep,
            Long idDepPadre,
            Integer verDepPadre,
            TipoActa tipoActa,
            LocalDate fhVigDesde) {
        this.idDep = idDep;
        this.verDep = verDep;
        this.nomDep = nomDep;
        this.idDepPadre = idDepPadre;
        this.verDepPadre = verDepPadre;
        this.tipoActa = tipoActa;
        this.fhVigDesde = fhVigDesde;
        this.siActiva = true;
    }

    public Long getIdDep() { return idDep; }
    public int getVerDep() { return verDep; }
    public String getNomDep() { return nomDep; }
    public Long getIdDepPadre() { return idDepPadre; }
    public Integer getVerDepPadre() { return verDepPadre; }
    public TipoActa getTipoActa() { return tipoActa; }
    public LocalDate getFhVigDesde() { return fhVigDesde; }

    public LocalDate getFhVigHasta() { return fhVigHasta; }
    public void setFhVigHasta(LocalDate fhVigHasta) { this.fhVigHasta = fhVigHasta; }

    public boolean isSiActiva() { return siActiva; }
    public void setSiActiva(boolean siActiva) { this.siActiva = siActiva; }

    /**
     * Devuelve true si esta version es vigente para la fecha dada.
     * Vigente: siActiva && fhVigDesde <= fecha && (fhVigHasta nula || fecha < fhVigHasta)
     */
    public boolean esVigenteEn(LocalDate fecha) {
        if (!siActiva) return false;
        if (fhVigDesde.isAfter(fecha)) return false;
        if (fhVigHasta != null && !fecha.isBefore(fhVigHasta)) return false;
        return true;
    }
}
