package ar.gob.malvinas.faltas.core.domain.model;

import java.time.LocalDateTime;

/**
 * Dependencia organica del sistema de faltas.
 *
 * Corresponde a fal_dependencia en el modelo MariaDB productivo.
 * La dependencia es el ambito organizacional al que pertenece un inspector
 * y que determina el tipo de acta que puede labrar.
 *
 * La estructura en detalle (tipo_acta, vigencias) vive en FalDependenciaVersion.
 *
 * Slice 8A-1: implementacion in-memory. Slice 9: reemplazar por JDBC.
 */
public class FalDependencia {

    private final Long idDep;
    private String codDep;
    private String nomDep;
    private Long idDepPadre;
    private boolean siActiva;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalDependencia(Long idDep, String nomDep, LocalDateTime fhAlta, String idUserAlta) {
        this.idDep = idDep;
        this.nomDep = nomDep;
        this.siActiva = true;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public Long getIdDep() { return idDep; }

    public String getCodDep() { return codDep; }
    public void setCodDep(String codDep) { this.codDep = codDep; }

    public String getNomDep() { return nomDep; }
    public void setNomDep(String nomDep) { this.nomDep = nomDep; }

    public Long getIdDepPadre() { return idDepPadre; }
    public void setIdDepPadre(Long idDepPadre) { this.idDepPadre = idDepPadre; }

    public boolean isSiActiva() { return siActiva; }
    public void setSiActiva(boolean siActiva) { this.siActiva = siActiva; }

    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
}
