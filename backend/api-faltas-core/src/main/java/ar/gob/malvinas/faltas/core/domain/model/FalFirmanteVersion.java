package ar.gob.malvinas.faltas.core.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Version versionada de un firmante.
 *
 * Corresponde a fal_firmante_version en el modelo MariaDB productivo.
 * Congela nomFirmante, rolFirmante, cargoFirmante, idDep y verDep al versionar
 * para garantizar trazabilidad historica.
 *
 * rolFirmante es DESCRIPTIVO/institucional opcional. No define autorizacion documental.
 * La autorizacion real se expresa mediante FalFirmanteVersionHabilitacion.
 *
 * fhAlta y idUserAlta son campos de auditoria presentes en fal_firmante_version.
 *
 * Slice 8A-3: implementacion in-memory. Slice 9: reemplazar por JDBC.
 */
public class FalFirmanteVersion {

    private final Long idFirmante;
    private final int verFirmante;
    private final String idUser;
    private final String nomFirmante;
    private final String rolFirmante;
    private final String cargoFirmante;
    private final Long idDep;
    private final Integer verDep;
    private final LocalDate fhVigDesde;
    private LocalDate fhVigHasta;
    private boolean siActivo;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalFirmanteVersion(Long idFirmante, int verFirmante, String idUser,
                              String nomFirmante, String rolFirmante, String cargoFirmante,
                              Long idDep, Integer verDep, LocalDate fhVigDesde,
                              LocalDateTime fhAlta, String idUserAlta) {
        this.idFirmante = idFirmante;
        this.verFirmante = verFirmante;
        this.idUser = idUser;
        this.nomFirmante = nomFirmante;
        this.rolFirmante = rolFirmante;
        this.cargoFirmante = cargoFirmante;
        this.idDep = idDep;
        this.verDep = verDep;
        this.fhVigDesde = fhVigDesde;
        this.siActivo = true;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public Long getIdFirmante() { return idFirmante; }
    public int getVerFirmante() { return verFirmante; }
    public String getIdUser() { return idUser; }
    public String getNomFirmante() { return nomFirmante; }
    public String getRolFirmante() { return rolFirmante; }
    public String getCargoFirmante() { return cargoFirmante; }
    public Long getIdDep() { return idDep; }
    public Integer getVerDep() { return verDep; }
    public LocalDate getFhVigDesde() { return fhVigDesde; }

    public LocalDate getFhVigHasta() { return fhVigHasta; }
    public void setFhVigHasta(LocalDate fhVigHasta) { this.fhVigHasta = fhVigHasta; }

    public boolean isSiActivo() { return siActivo; }
    public void setSiActivo(boolean siActivo) { this.siActivo = siActivo; }

    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }

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
