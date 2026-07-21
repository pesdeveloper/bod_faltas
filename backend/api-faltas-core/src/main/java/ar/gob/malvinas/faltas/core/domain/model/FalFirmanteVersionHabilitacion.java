package ar.gob.malvinas.faltas.core.domain.model;

import java.time.LocalDateTime;

/**
 * Habilitacion documental de una version concreta de firmante.
 *
 * Corresponde a fal_firmante_version_habilitacion en el modelo MariaDB productivo.
 * Define que tipoDocu y que rolFirmaReq puede firmar esta version del firmante.
 *
 * PK funcional: idFirmante + verFirmante + tipoDocu + rolFirmaReq.
 * mecanismoFirmaReq es nullable: si es null no restringe mecanismo.
 * siActivo controla baja logica.
 *
 * tipo_docu SMALLINT -> short en Java (obligatorio, no nulo).
 * rol_firma_req SMALLINT -> short en Java (obligatorio, no nulo).
 * mecanismo_firma_req SMALLINT nullable -> Short en Java (nullable).
 *
 * Los enums correspondientes (tipo_docu, rol_firma_req, mecanismo_firma_req)
 * se alinearan en el Slice 8C junto con el modelo documental completo.
 *
 * Slice 8A-5.1: tipos alineados con MariaDB SMALLINT. Slice 9: reemplazar por JDBC.
 */
public class FalFirmanteVersionHabilitacion {

    private final Long idFirmante;
    private final int verFirmante;
    private final short tipoDocu;
    private final short rolFirmaReq;
    private final Short mecanismoFirmaReq;
    private boolean siActivo;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalFirmanteVersionHabilitacion(Long idFirmante, int verFirmante,
                                          short tipoDocu, short rolFirmaReq,
                                          Short mecanismoFirmaReq,
                                          LocalDateTime fhAlta, String idUserAlta) {
        this.idFirmante = idFirmante;
        this.verFirmante = verFirmante;
        this.tipoDocu = tipoDocu;
        this.rolFirmaReq = rolFirmaReq;
        this.mecanismoFirmaReq = mecanismoFirmaReq;
        this.siActivo = true;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public Long getIdFirmante() { return idFirmante; }
    public int getVerFirmante() { return verFirmante; }
    public short getTipoDocu() { return tipoDocu; }
    public short getRolFirmaReq() { return rolFirmaReq; }
    public Short getMecanismoFirmaReq() { return mecanismoFirmaReq; }

    public boolean isSiActivo() { return siActivo; }
    public void setSiActivo(boolean siActivo) { this.siActivo = siActivo; }

    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
}
