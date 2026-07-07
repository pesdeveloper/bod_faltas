package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.UnidadTerritorialTipo;

/**
 * Datos especificos de acta de transito.
 * Mapeada a fal_acta_transito en MariaDB.
 * 1:1 con fal_acta. Solo para tipo_acta=TRANSITO.
 */
public class FalActaTransito {

    private final Long actaId;
    private String nroLicencia;
    private Short idProvLic;
    private UnidadTerritorialTipo unidadTerritorialLicTipo;
    private Integer idUnidadTerritorialLic;
    private boolean siRetLicencia;
    private boolean siRetVehiculo;
    private boolean siControlAlcoholemia;

    public FalActaTransito(Long actaId) {
        if (actaId == null) throw new IllegalArgumentException("actaId es obligatorio");
        this.actaId = actaId;
    }

    public Long getActaId() { return actaId; }

    public String getNroLicencia() { return nroLicencia; }
    public void setNroLicencia(String nroLicencia) {
        if (nroLicencia != null && nroLicencia.length() > 20)
            throw new IllegalArgumentException("nroLicencia max 20 caracteres");
        this.nroLicencia = nroLicencia;
    }

    public Short getIdProvLic() { return idProvLic; }
    public UnidadTerritorialTipo getUnidadTerritorialLicTipo() { return unidadTerritorialLicTipo; }
    public Integer getIdUnidadTerritorialLic() { return idUnidadTerritorialLic; }

    /**
     * Informa la provincia, tipo y unidad territorial de la licencia de conducir.
     * idProvLic e idUnidadTerritorialLic deben informarse juntos o ambos null.
     * Si se informa la unidad, el tipo es obligatorio.
     */
    public void setLicenciaTerritorio(Short idProvLic, UnidadTerritorialTipo tipo, Integer idUnidadTerritorialLic) {
        if ((idProvLic == null) != (idUnidadTerritorialLic == null))
            throw new IllegalArgumentException("idProvLic e idUnidadTerritorialLic deben informarse juntos");
        if (idUnidadTerritorialLic != null && tipo == null)
            throw new IllegalArgumentException("unidadTerritorialLicTipo es obligatorio cuando se informa unidad territorial");
        this.idProvLic = idProvLic;
        this.unidadTerritorialLicTipo = tipo;
        this.idUnidadTerritorialLic = idUnidadTerritorialLic;
    }

    public boolean isSiRetLicencia() { return siRetLicencia; }
    public void setSiRetLicencia(boolean siRetLicencia) { this.siRetLicencia = siRetLicencia; }

    public boolean isSiRetVehiculo() { return siRetVehiculo; }
    public void setSiRetVehiculo(boolean siRetVehiculo) { this.siRetVehiculo = siRetVehiculo; }

    public boolean isSiControlAlcoholemia() { return siControlAlcoholemia; }
    public void setSiControlAlcoholemia(boolean siControlAlcoholemia) { this.siControlAlcoholemia = siControlAlcoholemia; }

    public FalActaTransito copia() {
        FalActaTransito c = new FalActaTransito(actaId);
        c.nroLicencia = this.nroLicencia;
        c.idProvLic = this.idProvLic;
        c.unidadTerritorialLicTipo = this.unidadTerritorialLicTipo;
        c.idUnidadTerritorialLic = this.idUnidadTerritorialLic;
        c.siRetLicencia = this.siRetLicencia;
        c.siRetVehiculo = this.siRetVehiculo;
        c.siControlAlcoholemia = this.siControlAlcoholemia;
        return c;
    }
}
