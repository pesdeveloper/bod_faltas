package ar.gob.malvinas.faltas.core.domain.model;

import java.time.LocalDateTime;

/**
 * Requisito de firma de una plantilla documental.
 *
 * Alineado con fal_documento_plantilla_firma_req MariaDB.
 * Este NO es un requisito de firma de un documento concreto (FalDocumentoFirmaReq, slice 8C-4).
 */
public class FalDocumentoPlantillaFirmaReq {

    private final Long id;
    private final Long plantillaId;
    private final short seqFirmaReq;
    private final short rolFirmaReq;
    private final Short mecanismoFirmaReq;
    private final boolean siObligatoria;
    private boolean siActiva;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalDocumentoPlantillaFirmaReq(
            Long id,
            Long plantillaId,
            short seqFirmaReq,
            short rolFirmaReq,
            Short mecanismoFirmaReq,
            boolean siObligatoria,
            boolean siActiva,
            LocalDateTime fhAlta,
            String idUserAlta) {
        this.id = id;
        this.plantillaId = plantillaId;
        this.seqFirmaReq = seqFirmaReq;
        this.rolFirmaReq = rolFirmaReq;
        this.mecanismoFirmaReq = mecanismoFirmaReq;
        this.siObligatoria = siObligatoria;
        this.siActiva = siActiva;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public Long getId() { return id; }
    public Long getPlantillaId() { return plantillaId; }
    public short getSeqFirmaReq() { return seqFirmaReq; }
    public short getRolFirmaReq() { return rolFirmaReq; }
    public Short getMecanismoFirmaReq() { return mecanismoFirmaReq; }
    public boolean isSiObligatoria() { return siObligatoria; }
    public boolean isSiActiva() { return siActiva; }
    public void setSiActiva(boolean siActiva) { this.siActiva = siActiva; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
}