package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.AmbitoCtv;

import java.time.LocalDateTime;

/**
 * Datos especificos de acta de sustancias alimenticias/bromatologia.
 * Mapeada a fal_acta_sustancias_alimenticias en MariaDB.
 * 1:1 con fal_acta para circuito de sustancias/bromatologia.
 *
 * rubro_id + idRub deben informarse juntos cuando el rubro esta normalizado.
 * descripcionSustancias: texto libre tecnico sanitario, no observacion administrativa.
 */
public class FalActaSustanciasAlimenticias {

    private final Long actaId;
    private Long rubroId;
    private Integer idRub;
    private AmbitoCtv ambitoCtv;
    private String ambitoCtvTxt;
    private String descripcionSustancias;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalActaSustanciasAlimenticias(Long actaId, LocalDateTime fhAlta, String idUserAlta) {
        if (actaId == null) throw new IllegalArgumentException("actaId es obligatorio");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta es obligatoria");
        if (idUserAlta == null || idUserAlta.isBlank()) throw new IllegalArgumentException("idUserAlta es obligatorio");
        this.actaId = actaId;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public Long getActaId() { return actaId; }

    public Long getRubroId() { return rubroId; }
    public Integer getIdRub() { return idRub; }

    public void setRubro(Long rubroId, Integer idRub) {
        if (rubroId != null && idRub == null)
            throw new IllegalArgumentException("idRub es obligatorio cuando se informa rubroId");
        if (idRub != null && rubroId == null)
            throw new IllegalArgumentException("rubroId es obligatorio cuando se informa idRub");
        this.rubroId = rubroId;
        this.idRub = idRub;
    }

    public AmbitoCtv getAmbitoCtv() { return ambitoCtv; }
    public String getAmbitoCtvTxt() { return ambitoCtvTxt; }

    public void setAmbito(AmbitoCtv ambito, String ambitoTxt) {
        if (ambito == AmbitoCtv.OTRO && (ambitoTxt == null || ambitoTxt.isBlank()))
            throw new IllegalArgumentException("ambitoCtvTxt es obligatorio cuando ambitoCtv=OTRO");
        if (ambito != null && ambito != AmbitoCtv.OTRO && ambitoTxt != null)
            throw new IllegalArgumentException("ambitoCtvTxt debe ser null cuando ambitoCtv != OTRO");
        if (ambitoTxt != null && ambitoTxt.length() > 80)
            throw new IllegalArgumentException("ambitoCtvTxt max 80 caracteres");
        this.ambitoCtv = ambito;
        this.ambitoCtvTxt = ambitoTxt;
    }

    public String getDescripcionSustancias() { return descripcionSustancias; }
    public void setDescripcionSustancias(String descripcion) { this.descripcionSustancias = descripcion; }

    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }

    public FalActaSustanciasAlimenticias copia() {
        FalActaSustanciasAlimenticias c = new FalActaSustanciasAlimenticias(actaId, fhAlta, idUserAlta);
        c.rubroId = this.rubroId;
        c.idRub = this.idRub;
        c.ambitoCtv = this.ambitoCtv;
        c.ambitoCtvTxt = this.ambitoCtvTxt;
        c.descripcionSustancias = this.descripcionSustancias;
        return c;
    }
}
