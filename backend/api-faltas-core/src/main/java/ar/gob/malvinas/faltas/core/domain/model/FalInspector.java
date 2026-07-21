package ar.gob.malvinas.faltas.core.domain.model;

import java.time.LocalDateTime;

/**
 * Inspector organico del sistema de faltas.
 *
 * Corresponde a fal_inspector en el modelo MariaDB productivo.
 * El inspector pertenece a una dependencia versionada que determina el tipo de acta
 * que puede labrar. El tipoActa no vive en esta clase: se deriva desde
 * FalDependenciaVersion.tipoActa consultando idDep + verDep de la version vigente.
 *
 * legajoInsp es INT en MariaDB (fal_inspector.legajo_insp INT NOT NULL, > 0).
 * idUser es CHAR(36) NOT NULL UNIQUE en MariaDB (UUID del usuario IDP).
 * nomInsp es VARCHAR(120) NOT NULL en MariaDB.
 * No existen campos 'apellido' ni 'nombre' en esta tabla (corrección FULL-R1).
 *
 * Slice 8A-2: implementacion in-memory. Slice 9: reemplazar por JDBC.
 */
public class FalInspector {

    private final Long idInsp;
    private final String idUser;
    private int legajoInsp;
    private String nomInsp;
    private boolean siActivo;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalInspector(Long idInsp, String idUser, int legajoInsp, String nomInsp,
                        LocalDateTime fhAlta, String idUserAlta) {
        this.idInsp = idInsp;
        this.idUser = idUser;
        this.legajoInsp = legajoInsp;
        this.nomInsp = nomInsp;
        this.siActivo = true;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public Long getIdInsp() { return idInsp; }
    public String getIdUser() { return idUser; }

    public int getLegajoInsp() { return legajoInsp; }
    public void setLegajoInsp(int legajoInsp) { this.legajoInsp = legajoInsp; }

    public String getNomInsp() { return nomInsp; }
    public void setNomInsp(String nomInsp) { this.nomInsp = nomInsp; }

    public boolean isSiActivo() { return siActivo; }
    public void setSiActivo(boolean siActivo) { this.siActivo = siActivo; }

    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
}
