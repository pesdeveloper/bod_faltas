package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.TipoEvidenciaActa;

import java.time.LocalDateTime;

/**
 * Evidencia vinculada a un acta (fal_acta_evidencia en MariaDB).
 * FIRMA_OLOGRAFA_INFRACTOR: imagen/captura de la firma olografa del infractor.
 * No es firma institucional. No autoriza documentos.
 * No participa en FalDocumentoFirma, FalDocumentoFirmaReq, EstadoDocu ni EstadoFirmaReq.
 */
public class FalActaEvidencia {

    private final Long id;
    private final Long idActa;
    private final TipoEvidenciaActa tipoEvid;
    private final String storageKey;
    private final LocalDateTime fechaRegistro;

    public FalActaEvidencia(Long id, Long idActa, TipoEvidenciaActa tipoEvid,
                            String storageKey, LocalDateTime fechaRegistro) {
        if (id == null) throw new IllegalArgumentException("id requerido");
        if (idActa == null) throw new IllegalArgumentException("idActa requerido");
        if (tipoEvid == null) throw new IllegalArgumentException("tipoEvid requerido");
        if (storageKey == null || storageKey.isBlank())
            throw new IllegalArgumentException("storageKey requerido");
        this.id = id;
        this.idActa = idActa;
        this.tipoEvid = tipoEvid;
        this.storageKey = storageKey;
        this.fechaRegistro = fechaRegistro != null ? fechaRegistro : LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getIdActa() { return idActa; }
    public TipoEvidenciaActa getTipoEvid() { return tipoEvid; }
    public String getStorageKey() { return storageKey; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
}
