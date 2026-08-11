package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.OrigenIncorporacionEvidencia;
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
 *
 * <h2>Metadata de fotografias (DECISION_DDL-EVID-03, Wave 4 Parte 3D)</h2>
 *
 * <p>Diez columnas nuevas, todas NULL-ables a nivel fisico y EXCLUSIVAS de
 * {@code tipoEvid == FOTO}: {@code orden}, {@code descripcion},
 * {@code nombreOriginal}, {@code mimeTipo}, {@code tamanioBytes},
 * {@code anchoPx}, {@code altoPx}, {@code fechaCapturaExif},
 * {@code bloqueAlIncorporar}, {@code origenIncorporacion}.
 *
 * <p>La coherencia "FOTO implica esas siete columnas obligatorias pobladas
 * (orden, hash, mime, tamanio, ancho, alto, origen)" se valida <b>aqui, en
 * Java</b>, nunca con un CHECK fisico: {@code DECISION_DDL-SIN-CHECKS-CODIGOS-01}
 * retiro del DDL los CHECK de coherencia funcional entre campos y los dejo en
 * dominio/aplicacion (ver {@code database/diagnostics/verify-domain-schema.sql}
 * Seccion 12). {@code chk_evidencia_hash} sigue siendo la unica CHECK que
 * permanece en el motor para esta tabla.
 *
 * <p>El constructor de 8 argumentos (legacy) se preserva sin cambios de firma
 * ni de orden: es el que usan {@code ActaService} para evidencias no-FOTO,
 * {@code TipoEvidenciaActaGuardrailTest} y todo llamador preexistente. Delega
 * en el constructor completo con las diez columnas nuevas en {@code null},
 * que es exactamente lo que exige una evidencia no-FOTO.
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

    private final Integer orden;
    private final String descripcion;
    private final String nombreOriginal;
    private final String mimeTipo;
    private final Long tamanioBytes;
    private final Integer anchoPx;
    private final Integer altoPx;
    private final LocalDateTime fechaCapturaExif;
    private final String bloqueAlIncorporar;
    private final OrigenIncorporacionEvidencia origenIncorporacion;

    /**
     * Constructor legacy (DECISION_DDL-EVID-01), preservado sin cambios de firma.
     * Las diez columnas de fotografia quedan en {@code null}: correcto para
     * cualquier {@code tipoEvid} distinto de FOTO, y es el unico camino que
     * usan hoy {@code ActaService} y los guardrails existentes.
     */
    public FalActaEvidencia(Long id, Long idActa, TipoEvidenciaActa tipoEvid,
                            String storageKey, LocalDateTime fechaRegistro,
                            LocalDateTime fhAlta, String idUserAlta, String hashEvid) {
        this(id, idActa, tipoEvid, storageKey, fechaRegistro, fhAlta, idUserAlta, hashEvid,
                null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Constructor completo (DECISION_DDL-EVID-03). Para {@code tipoEvid == FOTO}
     * exige las siete columnas de fotografia obligatorias (orden, hash, mime,
     * tamanio, ancho, alto, origen); {@code descripcion}, {@code nombreOriginal},
     * {@code fechaCapturaExif} y {@code bloqueAlIncorporar} permanecen opcionales
     * incluso para FOTO. Para cualquier otro {@code tipoEvid} las diez columnas
     * de fotografia deben ser {@code null}: no existen fotografias parcialmente
     * tipadas ni evidencias no-FOTO con metadata de fotografia.
     */
    public FalActaEvidencia(Long id, Long idActa, TipoEvidenciaActa tipoEvid,
                            String storageKey, LocalDateTime fechaRegistro,
                            LocalDateTime fhAlta, String idUserAlta, String hashEvid,
                            Integer orden, String descripcion, String nombreOriginal,
                            String mimeTipo, Long tamanioBytes, Integer anchoPx, Integer altoPx,
                            LocalDateTime fechaCapturaExif, String bloqueAlIncorporar,
                            OrigenIncorporacionEvidencia origenIncorporacion) {
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

        boolean esFoto = tipoEvid == TipoEvidenciaActa.FOTO;
        if (esFoto) {
            if (orden == null) throw new IllegalArgumentException("orden requerido para tipoEvid=FOTO");
            if (orden < 1) throw new IllegalArgumentException("orden debe ser >= 1");
            if (hashEvid == null) throw new IllegalArgumentException("hashEvid (SHA-256) requerido para tipoEvid=FOTO");
            if (mimeTipo == null || mimeTipo.isBlank())
                throw new IllegalArgumentException("mimeTipo requerido para tipoEvid=FOTO");
            if (tamanioBytes == null || tamanioBytes <= 0)
                throw new IllegalArgumentException("tamanioBytes requerido y positivo para tipoEvid=FOTO");
            if (anchoPx == null || anchoPx <= 0)
                throw new IllegalArgumentException("anchoPx requerido y positivo para tipoEvid=FOTO");
            if (altoPx == null || altoPx <= 0)
                throw new IllegalArgumentException("altoPx requerido y positivo para tipoEvid=FOTO");
            if (origenIncorporacion == null)
                throw new IllegalArgumentException("origenIncorporacion requerido para tipoEvid=FOTO");
        } else {
            if (orden != null || mimeTipo != null || tamanioBytes != null || anchoPx != null
                    || altoPx != null || fechaCapturaExif != null || bloqueAlIncorporar != null
                    || origenIncorporacion != null || descripcion != null || nombreOriginal != null) {
                throw new IllegalArgumentException(
                        "Las columnas de fotografia deben ser null para tipoEvid=" + tipoEvid.name()
                                + " (exclusivas de FOTO, DECISION_DDL-EVID-03).");
            }
        }
        if (descripcion != null) {
            String normalizada = descripcion.strip().replaceAll("\\s+", " ");
            if (normalizada.isEmpty()) {
                descripcion = null;
            } else {
                if (normalizada.length() > 240) {
                    throw new IllegalArgumentException("descripcion no puede exceder 240 caracteres");
                }
                descripcion = normalizada;
            }
        }
        if (nombreOriginal != null && nombreOriginal.length() > 180) {
            throw new IllegalArgumentException("nombreOriginal no puede exceder 180 caracteres");
        }

        this.id = id;
        this.idActa = idActa;
        this.tipoEvid = tipoEvid;
        this.storageKey = storageKey;
        this.fechaRegistro = fechaRegistro;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
        this.hashEvid = hashEvid;
        this.orden = orden;
        this.descripcion = descripcion;
        this.nombreOriginal = nombreOriginal;
        this.mimeTipo = mimeTipo;
        this.tamanioBytes = tamanioBytes;
        this.anchoPx = anchoPx;
        this.altoPx = altoPx;
        this.fechaCapturaExif = fechaCapturaExif;
        this.bloqueAlIncorporar = bloqueAlIncorporar;
        this.origenIncorporacion = origenIncorporacion;
    }

    public Long getId() { return id; }
    public Long getIdActa() { return idActa; }
    public TipoEvidenciaActa getTipoEvid() { return tipoEvid; }
    public String getStorageKey() { return storageKey; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
    public String getHashEvid() { return hashEvid; }

    public Integer getOrden() { return orden; }
    public String getDescripcion() { return descripcion; }
    public String getNombreOriginal() { return nombreOriginal; }
    public String getMimeTipo() { return mimeTipo; }
    public Long getTamanioBytes() { return tamanioBytes; }
    public Integer getAnchoPx() { return anchoPx; }
    public Integer getAltoPx() { return altoPx; }
    public LocalDateTime getFechaCapturaExif() { return fechaCapturaExif; }
    public String getBloqueAlIncorporar() { return bloqueAlIncorporar; }
    public OrigenIncorporacionEvidencia getOrigenIncorporacion() { return origenIncorporacion; }

    public boolean esFotografia() {
        return tipoEvid == TipoEvidenciaActa.FOTO;
    }
}
