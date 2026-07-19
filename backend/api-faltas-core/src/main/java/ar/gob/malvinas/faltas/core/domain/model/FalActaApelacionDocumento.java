package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.OrigenPresentacion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocumentoApelacion;

import java.time.LocalDateTime;

/**
 * Documento, escrito, adjunto o evidencia presentada dentro de una apelacion (fal_acta_apelacion_documento).
 * Cada fila requiere al menos documentoId o storageKey; no se persisten filas vacias.
 * Los documentos de resolucion van en FalActaApelacion.documentoResolucionId.
 */
public class FalActaApelacionDocumento {

    private final Long id;
    private final Long apelacionId;
    private final TipoDocumentoApelacion tipoDocApelacion;
    private final OrigenPresentacion origenPresentacion;
    private Long documentoId;
    private String storageKey;
    private String nombreArchivo;
    private Short mimeType;
    private Long tamanioBytes;
    private final LocalDateTime fhPresentacion;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalActaApelacionDocumento(
            Long id,
            Long apelacionId,
            TipoDocumentoApelacion tipoDocApelacion,
            OrigenPresentacion origenPresentacion,
            Long documentoId,
            String storageKey,
            String nombreArchivo,
            Short mimeType,
            Long tamanioBytes,
            LocalDateTime fhPresentacion,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (id == null) throw new IllegalArgumentException("id requerido");
        if (apelacionId == null) throw new IllegalArgumentException("apelacionId requerido");
        if (tipoDocApelacion == null) throw new IllegalArgumentException("tipoDocApelacion requerido");
        if (origenPresentacion == null) throw new IllegalArgumentException("origenPresentacion requerido");
        if (documentoId == null && (storageKey == null || storageKey.isBlank()))
            throw new IllegalArgumentException("Se requiere documentoId o storageKey no vacio");
        if (storageKey != null && storageKey.length() > 255)
            throw new IllegalArgumentException("storageKey supera 255 caracteres");
        if (nombreArchivo != null && nombreArchivo.length() > 120)
            throw new IllegalArgumentException("nombreArchivo supera 120 caracteres");
        if (tamanioBytes != null && tamanioBytes < 0)
            throw new IllegalArgumentException("tamanioBytes no puede ser negativo");
        if (fhPresentacion == null) throw new IllegalArgumentException("fhPresentacion requerida");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta requerida");
        if (idUserAlta == null || idUserAlta.isBlank()) throw new IllegalArgumentException("idUserAlta requerido");
        this.id = id;
        this.apelacionId = apelacionId;
        this.tipoDocApelacion = tipoDocApelacion;
        this.origenPresentacion = origenPresentacion;
        this.documentoId = documentoId;
        this.storageKey = storageKey;
        this.nombreArchivo = nombreArchivo;
        this.mimeType = mimeType;
        this.tamanioBytes = tamanioBytes;
        this.fhPresentacion = fhPresentacion;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public Long getId() { return id; }
    public Long getApelacionId() { return apelacionId; }
    public TipoDocumentoApelacion getTipoDocApelacion() { return tipoDocApelacion; }
    public OrigenPresentacion getOrigenPresentacion() { return origenPresentacion; }
    public Long getDocumentoId() { return documentoId; }
    public String getStorageKey() { return storageKey; }
    public String getNombreArchivo() { return nombreArchivo; }
    public Short getMimeType() { return mimeType; }
    public Long getTamanioBytes() { return tamanioBytes; }
    public LocalDateTime getFhPresentacion() { return fhPresentacion; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }

    /** Copia defensiva (entidad inmutable post-creacion). */
    public FalActaApelacionDocumento copia() {
        return new FalActaApelacionDocumento(
                id, apelacionId, tipoDocApelacion, origenPresentacion,
                documentoId, storageKey, nombreArchivo, mimeType, tamanioBytes,
                fhPresentacion, fhAlta, idUserAlta);
    }
}
