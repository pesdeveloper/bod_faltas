package ar.gob.malvinas.faltas.core.domain.model;

import java.time.LocalDateTime;

/**
 * Firmante maestro del sistema de faltas.
 *
 * Corresponde a fal_firmante en el modelo MariaDB productivo.
 * El firmante no guarda certificados, firmas concretas ni permisos documentales.
 * La autorizacion documental real se define en FalFirmanteVersionHabilitacion.
 *
 * Slice 8A-3: implementacion in-memory. Slice 9: reemplazar por JDBC.
 */
public class FalFirmante {

    private final Long idFirmante;
    private final String idUser;
    private String nomFirmante;
    private boolean siActivo;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalFirmante(Long idFirmante, String idUser, String nomFirmante,
                       LocalDateTime fhAlta, String idUserAlta) {
        this.idFirmante = idFirmante;
        this.idUser = idUser;
        this.nomFirmante = nomFirmante;
        this.siActivo = true;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public Long getIdFirmante() { return idFirmante; }
    public String getIdUser() { return idUser; }

    public String getNomFirmante() { return nomFirmante; }
    public void setNomFirmante(String nomFirmante) { this.nomFirmante = nomFirmante; }

    public boolean isSiActivo() { return siActivo; }
    public void setSiActivo(boolean siActivo) { this.siActivo = siActivo; }

    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
}
