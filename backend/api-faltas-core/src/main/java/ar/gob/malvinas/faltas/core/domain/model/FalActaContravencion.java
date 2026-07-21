package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.OrigenNomenclatura;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoNomenclaturaManual;
import ar.gob.malvinas.faltas.core.domain.enums.AmbitoCtv;

import java.time.LocalDateTime;

/**
 * Datos de nomenclatura catastral y rubro del acta de contravencion/comercio.
 * Mapeada a fal_acta_contravencion en MariaDB.
 * 1:1 con fal_acta. Solo para tipo_acta=CONTRAVENCION o COMERCIO.
 *
 * La nomenclatura se instancia historicamente: no se actualiza si Catastro cambia.
 * nomenclatura_resumen se proyecta en fal_acta_snapshot; no es campo de esta tabla.
 */
public class FalActaContravencion {

    private final Long actaId;
    private Integer idSujI;
    private Integer idBieI;
    private Integer idSujC;
    private Integer idBieC;
    private Short circ;
    private String secc;
    private String frac;
    private String mza;
    private String parc;
    private String ufun;
    private String ucomp;
    private final OrigenNomenclatura origenNomencl;
    private final boolean siNomenclaturaManual;
    private MotivoNomenclaturaManual motivoNomenclaturaManual;
    private Long rubroId;
    private Integer idRub;
    private AmbitoCtv ambitoCtv;
    private String ambito_ctv_txt;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalActaContravencion(
            Long actaId,
            OrigenNomenclatura origenNomencl,
            boolean siNomenclaturaManual,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (actaId == null) throw new IllegalArgumentException("actaId es obligatorio");
        if (origenNomencl == null) throw new IllegalArgumentException("origenNomencl es obligatorio");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta es obligatoria");
        if (idUserAlta == null || idUserAlta.isBlank()) throw new IllegalArgumentException("idUserAlta es obligatorio");
        if (siNomenclaturaManual && origenNomencl != OrigenNomenclatura.MANUAL_EXCEPCIONAL)
            throw new IllegalArgumentException("si_nomenclatura_manual=true requiere origenNomencl=MANUAL_EXCEPCIONAL");
        if (!siNomenclaturaManual && origenNomencl == OrigenNomenclatura.MANUAL_EXCEPCIONAL)
            throw new IllegalArgumentException("origenNomencl=MANUAL_EXCEPCIONAL requiere si_nomenclatura_manual=true");
        this.actaId = actaId;
        this.origenNomencl = origenNomencl;
        this.siNomenclaturaManual = siNomenclaturaManual;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public Long getActaId() { return actaId; }

    public Integer getIdSujI() { return idSujI; }
    public Integer getIdBieI() { return idBieI; }

    public void setSujetoInmueble(Integer idSujI, Integer idBieI) {
        if (siNomenclaturaManual && idBieI != null)
            throw new IllegalArgumentException("idBieI debe ser null en carga manual excepcional");
        if (idSujI != null && (idSujI < 1 || idSujI > 255))
            throw new IllegalArgumentException(
                    "idSujI debe estar entre 1 y 255; valor: " + idSujI);
        if (idBieI != null && (idBieI < 1 || idBieI > 9_999_999))
            throw new IllegalArgumentException(
                    "idBieI debe estar entre 1 y 9.999.999; valor: " + idBieI);
        this.idSujI = idSujI;
        this.idBieI = idBieI;
    }

    public Integer getIdSujC() { return idSujC; }
    public Integer getIdBieC() { return idBieC; }

    public void setSujetoComercio(Integer idSujC, Integer idBieC) {
        if ((idSujC == null) != (idBieC == null))
            throw new IllegalArgumentException("Id_Suj_c e Id_Bie_c deben informarse juntos");
        if (idSujC != null && (idSujC < 1 || idSujC > 255))
            throw new IllegalArgumentException(
                    "idSujC debe estar entre 1 y 255; valor: " + idSujC);
        if (idBieC != null && (idBieC < 1 || idBieC > 9_999_999))
            throw new IllegalArgumentException(
                    "idBieC debe estar entre 1 y 9.999.999; valor: " + idBieC);
        this.idSujC = idSujC;
        this.idBieC = idBieC;
    }

    public Short getCirc() { return circ; }
    public void setCirc(Short circ) { this.circ = circ; }

    public String getSecc() { return secc; }
    public void setSecc(String secc) {
        if (secc != null && secc.length() > 2) throw new IllegalArgumentException("secc: CHAR(2) max");
        this.secc = secc;
    }

    public String getFrac() { return frac; }
    public void setFrac(String frac) {
        if (frac != null && frac.length() > 7) throw new IllegalArgumentException("frac max 7 caracteres");
        this.frac = frac;
    }

    public String getMza() { return mza; }
    public void setMza(String mza) {
        if (mza != null && mza.length() > 7) throw new IllegalArgumentException("mza max 7 caracteres");
        this.mza = mza;
    }

    public String getParc() { return parc; }
    public void setParc(String parc) {
        if (parc != null && parc.length() > 7) throw new IllegalArgumentException("parc max 7 caracteres");
        this.parc = parc;
    }

    public String getUfun() { return ufun; }
    public void setUfun(String ufun) {
        if (ufun != null && ufun.length() > 7) throw new IllegalArgumentException("ufun max 7 caracteres");
        this.ufun = ufun;
    }

    public String getUcomp() { return ucomp; }
    public void setUcomp(String ucomp) {
        if (ucomp != null && ucomp.length() > 20) throw new IllegalArgumentException("ucomp max 20 caracteres");
        this.ucomp = ucomp;
    }

    public OrigenNomenclatura getOrigenNomencl() { return origenNomencl; }
    public boolean isSiNomenclaturaManual() { return siNomenclaturaManual; }

    public MotivoNomenclaturaManual getMotivoNomenclaturaManual() { return motivoNomenclaturaManual; }
    public void setMotivoNomenclaturaManual(MotivoNomenclaturaManual motivo) {
        if (siNomenclaturaManual && motivo == null)
            throw new IllegalArgumentException("motivoNomenclaturaManual es obligatorio cuando siNomenclaturaManual=true");
        this.motivoNomenclaturaManual = motivo;
    }

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
    public String getAmbito_ctv_txt() { return ambito_ctv_txt; }

    public void setAmbito(AmbitoCtv ambito, String ambitoTxt) {
        if (ambito == AmbitoCtv.OTRO && (ambitoTxt == null || ambitoTxt.isBlank()))
            throw new IllegalArgumentException("ambito_ctv_txt es obligatorio cuando ambitoCtv=OTRO");
        if (ambito != AmbitoCtv.OTRO && ambitoTxt != null)
            throw new IllegalArgumentException("ambito_ctv_txt debe ser null cuando ambitoCtv != OTRO");
        if (ambitoTxt != null && ambitoTxt.length() > 80)
            throw new IllegalArgumentException("ambito_ctv_txt max 80 caracteres");
        this.ambitoCtv = ambito;
        this.ambito_ctv_txt = ambitoTxt;
    }

    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }

    /** Genera el texto resumido de nomenclatura para proyectar en snapshot. */
    public String generarNomenclaturaResumen() {
        if (idBieI != null) return "CVP:" + idBieI + (idBieC != null ? "/COM:" + idBieC : "");
        StringBuilder sb = new StringBuilder();
        if (circ != null) sb.append("C").append(circ);
        if (secc != null) sb.append(" S").append(secc);
        if (frac != null) sb.append(" F").append(frac);
        if (mza != null) sb.append(" M").append(mza);
        if (parc != null) sb.append(" P").append(parc);
        if (ufun != null) sb.append(" UF").append(ufun);
        String base = sb.toString().trim();
        return base.isEmpty() ? (siNomenclaturaManual ? "MANUAL" : null) : base;
    }

    public FalActaContravencion copia() {
        FalActaContravencion c = new FalActaContravencion(actaId, origenNomencl, siNomenclaturaManual, fhAlta, idUserAlta);
        c.idSujI = this.idSujI;
        c.idBieI = this.idBieI;
        c.idSujC = this.idSujC;
        c.idBieC = this.idBieC;
        c.circ = this.circ;
        c.secc = this.secc;
        c.frac = this.frac;
        c.mza = this.mza;
        c.parc = this.parc;
        c.ufun = this.ufun;
        c.ucomp = this.ucomp;
        c.motivoNomenclaturaManual = this.motivoNomenclaturaManual;
        c.rubroId = this.rubroId;
        c.idRub = this.idRub;
        c.ambitoCtv = this.ambitoCtv;
        c.ambito_ctv_txt = this.ambito_ctv_txt;
        return c;
    }
}
