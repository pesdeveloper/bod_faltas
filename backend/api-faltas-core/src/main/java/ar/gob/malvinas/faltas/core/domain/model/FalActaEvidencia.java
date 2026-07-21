package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.TipoEvidenciaActa;

import java.time.LocalDateTime;

/**
 * Evidencia vinculada a un acta (fal_acta_evidencia en MariaDB).
 * Repositorio de evidencias del acta: documentos, multimedia y firma olografa del infractor.
 * HUMAN_DECISION_CLOSED — FULL-R1.2-CORRECCION-10
 *
 * FIRMA_OLOGRAFA_INFRACTOR: firma manuscrita o digitalizada del infractor (codigo 48).
 * No es firma institucional. No autoriza documentos.
 * No participa en FalDocumentoFirma, FalDocumentoFirmaReq, EstadoDocu ni EstadoFirmaReq.
 *
 * Campos fisicos aprobados (DECISION_DDL-EVID-01):
 *   fecha_registro DATETIME(6) NOT NULL = instante funcional de captura.
 *   hash_evid CHAR(64) NULL = SHA-256 hexadecimal opcional.
 *   fh_alta DATETIME(6) NOT NULL = instante tecnico de alta.
 *   id_user_alta CHAR(36) NOT NULL = actor que incorporo la evidencia.
 *
 * Usar un unico instante de FaltasClock cuando fechaRegistro y fhAlta
 * nazcan de la misma operacion.
 */
public class FalActaEvidencia {

    private static final java.util.regex.Pattern HEX_64 =
            java.util.regex.Pattern.compile("^[0-9a-fA-F]{64}$");

    private final Long id;
    private final Long idActa;
    private final TipoEvidenciaActa tipoEvid;
    private final String storageKey;
    private final LocalDateTime fechaRegistro;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;
    private final String hashEvid;

    public FalActaEvidencia(Long id, Long idActa, TipoEvidenciaActa tipoEvid,
                            String storageKey, LocalDateTime fechaRegistro,
                            LocalDateTime fhAlta, String idUserAlta, String hashEvid) {
        if (id == null) throw new IllegalArgumentException("id requerido");
        if (idActa == null) throw new IllegalArgumentException("idActa requerido");
        if (tipoEvid == null) throw new IllegalArgumentException("tipoEvid requerido");
        if (storageKey == null || storageKey.isBlank())
            throw new IllegalArgumentException("storageKey requerido");
        if (fechaRegistro == null) throw new IllegalArgumentException("fechaRegistro requerido");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta requerido");
        if (idUserAlta == null || idUserAlta.isBlank())
            throw new IllegalArgumentException("idUserAlta requerido");
        if (hashEvid != null && !HEX_64.matcher(hashEvid).matches())
            throw new IllegalArgumentException("hashEvid debe ser SHA-256 hexadecimal de 64 caracteres");
        this.id = id;
        this.idActa = idActa;
        this.tipoEvid = tipoEvid;
        this.storageKey = storageKey;
        this.fechaRegistro = fechaRegistro;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
        this.hashEvid = hashEvid;
    }

    public Long getId() { return id; }
    public Long getIdActa() { return idActa; }
    public TipoEvidenciaActa getTipoEvid() { return tipoEvid; }
    public String getStorageKey() { return storageKey; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
    public String getHashEvid() { return hashEvid; }
}
