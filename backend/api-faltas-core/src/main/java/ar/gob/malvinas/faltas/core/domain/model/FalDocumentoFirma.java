package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoFirma;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFirma;

import java.time.LocalDateTime;

/**
 * Registro de firma documental alineado con fal_documento_firma MariaDB.
 *
 * id: Long (PK BIGINT AUTO_INCREMENT).
 * idDocumento: Long FK a fal_documento.
 * El acta es derivable desde FalDocumento.idActa; no se duplica aqui.
 *
 * Slice 8C-6B-1: refactor de modelo naive (String id, firmante String naive)
 * al modelo alineado con MariaDB.
 */
public class FalDocumentoFirma {

    private final Long id;
    private final Long idDocumento;
    private final short seqFirmaReq;
    private final Long idFirmante;
    private final short verFirmante;
    private final String idUserFirma;
    private final short rolFirmante;
    private final String nombreFirmante;
    private final TipoFirma tipoFirma;
    private final EstadoFirma estadoFirma;
    private final String hashDocumento;
    private final String referenciaFirmaExt;
    private final String storageKey;
    private final String mensajeError;
    private final LocalDateTime fhFirma;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalDocumentoFirma(
            Long id,
            Long idDocumento,
            short seqFirmaReq,
            Long idFirmante,
            short verFirmante,
            String idUserFirma,
            short rolFirmante,
            String nombreFirmante,
            TipoFirma tipoFirma,
            EstadoFirma estadoFirma,
            String hashDocumento,
            String referenciaFirmaExt,
            String storageKey,
            String mensajeError,
            LocalDateTime fhFirma,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (id == null) throw new IllegalArgumentException("id de firma requerido");
        if (idDocumento == null) throw new IllegalArgumentException("idDocumento requerido");
        this.id = id;
        this.idDocumento = idDocumento;
        this.seqFirmaReq = seqFirmaReq;
        this.idFirmante = idFirmante;
        this.verFirmante = verFirmante;
        this.idUserFirma = idUserFirma;
        this.rolFirmante = rolFirmante;
        this.nombreFirmante = nombreFirmante;
        this.tipoFirma = tipoFirma;
        this.estadoFirma = estadoFirma;
        this.hashDocumento = hashDocumento;
        this.referenciaFirmaExt = referenciaFirmaExt;
        this.storageKey = storageKey;
        this.mensajeError = mensajeError;
        this.fhFirma = fhFirma;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public Long getId() { return id; }
    public Long getIdDocumento() { return idDocumento; }
    public short getSeqFirmaReq() { return seqFirmaReq; }
    public Long getIdFirmante() { return idFirmante; }
    public short getVerFirmante() { return verFirmante; }
    public String getIdUserFirma() { return idUserFirma; }
    public short getRolFirmante() { return rolFirmante; }
    public String getNombreFirmante() { return nombreFirmante; }
    public TipoFirma getTipoFirma() { return tipoFirma; }
    public EstadoFirma getEstadoFirma() { return estadoFirma; }
    public String getHashDocumento() { return hashDocumento; }
    public String getReferenciaFirmaExt() { return referenciaFirmaExt; }
    public String getStorageKey() { return storageKey; }
    public String getMensajeError() { return mensajeError; }
    public LocalDateTime getFhFirma() { return fhFirma; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
}
