package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoFirmaReq;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;

import java.time.LocalDateTime;

/**
 * Snapshot de requisito de firma para un documento concreto generado desde plantilla.
 *
 * Alineado con fal_documento_firma_req MariaDB.
 * PK productiva compuesta: (id_docu, seq_firma_req).
 * Para in-memory se usa id sintetico generado por AtomicLong.
 *
 * Ciclo de vida del requisito controlado por EstadoFirmaReq (catalogo propio).
 * No representa una firma real (eso lo hace FalDocumentoFirma).
 * Cambios futuros en FalDocumentoPlantillaFirmaReq no alteran registros ya materializados.
 *
 * Slice 8C-4. Slice 8C-6B-1: agrega marcarFirmado.
 */
public class FalDocumentoFirmaReq {

    private final Long id;
    private final Long documentoId;
    private final short seqFirmaReq;
    private final short rolFirmaReq;
    private final Short mecanismoFirmaReq;
    private final Short ordenFirma;
    private EstadoFirmaReq estadoFirmaReq;
    private final boolean siObligatoria;
    private boolean siActiva;
    private Long idFirmanteAsig;
    private Short verFirmanteAsig;
    private LocalDateTime fhAsignacion;
    private LocalDateTime fhFirma;
    private Long idFirma;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalDocumentoFirmaReq(
            Long id,
            Long documentoId,
            short seqFirmaReq,
            short rolFirmaReq,
            Short mecanismoFirmaReq,
            Short ordenFirma,
            boolean siObligatoria,
            boolean siActiva,
            LocalDateTime fhAlta,
            String idUserAlta) {
        this.id = id;
        this.documentoId = documentoId;
        this.seqFirmaReq = seqFirmaReq;
        this.rolFirmaReq = rolFirmaReq;
        this.mecanismoFirmaReq = mecanismoFirmaReq;
        this.ordenFirma = ordenFirma;
        this.estadoFirmaReq = EstadoFirmaReq.PENDIENTE;
        this.siObligatoria = siObligatoria;
        this.siActiva = siActiva;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    /**
     * Marca el requisito como FIRMADO.
     * Solo puede pasar desde estado PENDIENTE.
     * Requiere idFirma y fhFirma no nulos.
     */
    public void marcarFirmado(Long idFirma, LocalDateTime fhFirma, Long idFirmanteAsig, short verFirmanteAsig) {
        if (estadoFirmaReq != EstadoFirmaReq.PENDIENTE) {
            throw new PrecondicionVioladaException(
                    "El requisito de firma no esta en estado PENDIENTE. Estado actual: " + estadoFirmaReq);
        }
        if (idFirma == null) throw new IllegalArgumentException("idFirma requerido para marcar como firmado");
        if (fhFirma == null) throw new IllegalArgumentException("fhFirma requerido para marcar como firmado");
        this.estadoFirmaReq = EstadoFirmaReq.FIRMADO;
        this.idFirma = idFirma;
        this.fhFirma = fhFirma;
        this.idFirmanteAsig = idFirmanteAsig;
        this.verFirmanteAsig = verFirmanteAsig;
    }

    public Long getId() { return id; }
    public Long getDocumentoId() { return documentoId; }
    public short getSeqFirmaReq() { return seqFirmaReq; }
    public short getRolFirmaReq() { return rolFirmaReq; }
    public Short getMecanismoFirmaReq() { return mecanismoFirmaReq; }
    public Short getOrdenFirma() { return ordenFirma; }
    public EstadoFirmaReq getEstadoFirmaReq() { return estadoFirmaReq; }
    public void setEstadoFirmaReq(EstadoFirmaReq estadoFirmaReq) { this.estadoFirmaReq = estadoFirmaReq; }
    public boolean isSiObligatoria() { return siObligatoria; }
    public boolean isSiActiva() { return siActiva; }
    public void setSiActiva(boolean siActiva) { this.siActiva = siActiva; }
    public Long getIdFirmanteAsig() { return idFirmanteAsig; }
    public void setIdFirmanteAsig(Long idFirmanteAsig) { this.idFirmanteAsig = idFirmanteAsig; }
    public Short getVerFirmanteAsig() { return verFirmanteAsig; }
    public void setVerFirmanteAsig(Short verFirmanteAsig) { this.verFirmanteAsig = verFirmanteAsig; }
    public LocalDateTime getFhAsignacion() { return fhAsignacion; }
    public void setFhAsignacion(LocalDateTime fhAsignacion) { this.fhAsignacion = fhAsignacion; }
    public LocalDateTime getFhFirma() { return fhFirma; }
    public void setFhFirma(LocalDateTime fhFirma) { this.fhFirma = fhFirma; }
    public Long getIdFirma() { return idFirma; }
    public void setIdFirma(Long idFirma) { this.idFirma = idFirma; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
}
