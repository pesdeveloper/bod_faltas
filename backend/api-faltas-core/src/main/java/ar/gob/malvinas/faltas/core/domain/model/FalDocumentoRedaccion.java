package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoRedaccionDocumento;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;

import java.time.LocalDateTime;

/**
 * Redaccion documental editable de un documento concreto.
 *
 * Mientras esta en BORRADOR:
 *   - FalDocumento.storageKey debe ser null.
 *   - FalDocumento.hashDocu debe ser null.
 *   - FalDocumento.fhGeneracion debe ser null.
 *
 * El PDF final se genera SOLO al confirmar/enviar en un slice futuro.
 * No se guardan PDFs intermedios.
 *
 * Campos de revision (CORRECCION-07 — paridad DDL fal_documento_redaccion):
 *   - nroRevision: revision number; comienza en 1; nunca arbitrario (>= 1).
 *   - redaccionOrigenId: FK a la redaccion previa; null en primera revision.
 *   - fhAnulacion: timestamp de anulacion; null si no anulada.
 *   - idUserAnulacion: usuario que anulo; null si no anulada.
 *
 * OCC mediante versionRow se conserva integro.
 *
 * Slice 8F-1. Paridad DDL CORRECCION-07.
 */
public class FalDocumentoRedaccion {

    private final Long id;
    private final Long idDocumento;
    private final Long plantillaContenidoId;
    private final short nroRevision;              // nro_revision SMALLINT NOT NULL DEFAULT 1
    private final Long redaccionOrigenId;         // redaccion_origen_id BIGINT NULL
    private EstadoRedaccionDocumento estadoRedaccion;
    private String contenidoEditable;
    private final String variablesSnapshotJson;
    /** @deprecated Campo eliminado del DDL (HUMAN_DECISION_CLOSED). Los errores se resuelven antes de persistir. */
    @Deprecated
    private String variablesFaltantesJson;
    /** @deprecated Campo eliminado del DDL (HUMAN_DECISION_CLOSED). Los errores se resuelven antes de persistir. */
    @Deprecated
    private String diagnosticoJson;
    private int versionRow;                       // version_row INT NOT NULL DEFAULT 0
    private final LocalDateTime fhCreacion;
    private final String idUserCreacion;
    private LocalDateTime fhUltimaEdicion;
    private String idUserUltimaEdicion;
    private LocalDateTime fhConfirmacion;
    private String idUserConfirmacion;
    private LocalDateTime fhAnulacion;            // fh_anulacion DATETIME(6) NULL
    private String idUserAnulacion;               // id_user_anulacion CHAR(36) NULL

    /**
     * Constructor canonico.
     *
     * @param nroRevision      numero de revision; debe ser >= 1; primera redaccion = 1.
     * @param redaccionOrigenId FK a la redaccion previa; null en primera revision.
     */
    public FalDocumentoRedaccion(
            Long id, Long idDocumento, Long plantillaContenidoId,
            short nroRevision, Long redaccionOrigenId,
            EstadoRedaccionDocumento estadoRedaccion, String contenidoEditable,
            String variablesSnapshotJson, String variablesFaltantesJson, String diagnosticoJson,
            LocalDateTime fhCreacion, String idUserCreacion,
            LocalDateTime fhUltimaEdicion, String idUserUltimaEdicion,
            LocalDateTime fhConfirmacion, String idUserConfirmacion) {
        if (id == null) throw new IllegalArgumentException("id requerido");
        if (idDocumento == null) throw new IllegalArgumentException("idDocumento requerido");
        if (plantillaContenidoId == null) throw new IllegalArgumentException("plantillaContenidoId requerido");
        if (nroRevision < 1) throw new IllegalArgumentException("nroRevision debe ser >= 1; primera redaccion = 1");
        if (estadoRedaccion == null) throw new IllegalArgumentException("estadoRedaccion requerido");
        if (contenidoEditable == null) throw new IllegalArgumentException("contenidoEditable no puede ser null");
        if (fhCreacion == null) throw new IllegalArgumentException("fhCreacion requerido");
        if (idUserCreacion == null || idUserCreacion.isBlank())
            throw new IllegalArgumentException("idUserCreacion requerido");

        this.id = id;
        this.idDocumento = idDocumento;
        this.plantillaContenidoId = plantillaContenidoId;
        this.nroRevision = nroRevision;
        this.redaccionOrigenId = redaccionOrigenId;
        this.estadoRedaccion = estadoRedaccion;
        this.contenidoEditable = contenidoEditable;
        this.variablesSnapshotJson = variablesSnapshotJson;
        this.variablesFaltantesJson = variablesFaltantesJson;
        this.diagnosticoJson = diagnosticoJson;
        this.versionRow = 0;
        this.fhCreacion = fhCreacion;
        this.idUserCreacion = idUserCreacion;
        this.fhUltimaEdicion = fhUltimaEdicion;
        this.idUserUltimaEdicion = idUserUltimaEdicion;
        this.fhConfirmacion = fhConfirmacion;
        this.idUserConfirmacion = idUserConfirmacion;
        this.fhAnulacion = null;
        this.idUserAnulacion = null;
    }

    public boolean esBorrador() { return estadoRedaccion == EstadoRedaccionDocumento.BORRADOR; }
    public boolean estaConfirmada() { return estadoRedaccion == EstadoRedaccionDocumento.CONFIRMADA; }

    public Long getId() { return id; }
    public int getVersionRow() { return versionRow; }
    public void setVersionRow(int versionRow) { this.versionRow = versionRow; }
    public Long getIdDocumento() { return idDocumento; }
    public Long getPlantillaContenidoId() { return plantillaContenidoId; }
    public short getNroRevision() { return nroRevision; }
    public Long getRedaccionOrigenId() { return redaccionOrigenId; }
    public EstadoRedaccionDocumento getEstadoRedaccion() { return estadoRedaccion; }
    public void setEstadoRedaccion(EstadoRedaccionDocumento e) { this.estadoRedaccion = e; }
    public String getContenidoEditable() { return contenidoEditable; }
    public void setContenidoEditable(String v) { this.contenidoEditable = v; }
    public String getVariablesSnapshotJson() { return variablesSnapshotJson; }
    /** @deprecated Campo eliminado del DDL (HUMAN_DECISION_CLOSED). NO PERSISTIR. */
    @Deprecated
    public String getVariablesFaltantesJson() { return variablesFaltantesJson; }
    /** @deprecated Campo eliminado del DDL (HUMAN_DECISION_CLOSED). NO PERSISTIR. */
    @Deprecated
    public void setVariablesFaltantesJson(String v) { this.variablesFaltantesJson = v; }
    /** @deprecated Campo eliminado del DDL (HUMAN_DECISION_CLOSED). NO PERSISTIR. */
    @Deprecated
    public String getDiagnosticoJson() { return diagnosticoJson; }
    /** @deprecated Campo eliminado del DDL (HUMAN_DECISION_CLOSED). NO PERSISTIR. */
    @Deprecated
    public void setDiagnosticoJson(String v) { this.diagnosticoJson = v; }
    public LocalDateTime getFhCreacion() { return fhCreacion; }
    public String getIdUserCreacion() { return idUserCreacion; }
    public LocalDateTime getFhUltimaEdicion() { return fhUltimaEdicion; }
    public void setFhUltimaEdicion(LocalDateTime v) { this.fhUltimaEdicion = v; }
    public String getIdUserUltimaEdicion() { return idUserUltimaEdicion; }
    public void setIdUserUltimaEdicion(String v) { this.idUserUltimaEdicion = v; }
    public LocalDateTime getFhConfirmacion() { return fhConfirmacion; }
    public void setFhConfirmacion(LocalDateTime v) { this.fhConfirmacion = v; }
    public String getIdUserConfirmacion() { return idUserConfirmacion; }
    public void setIdUserConfirmacion(String v) { this.idUserConfirmacion = v; }
    public LocalDateTime getFhAnulacion() { return fhAnulacion; }
    public String getIdUserAnulacion() { return idUserAnulacion; }

    /**
     * Confirma la redaccion, transicionando desde BORRADOR o REABIERTA a CONFIRMADA.
     *
     * Reglas:
     *   - ANULADA no puede confirmarse.
     *   - CONFIRMADA no puede confirmarse dos veces.
     *   - El contenido editable no puede estar vacio.
     *   - No genera PDF; la generacion mock la orquesta el servicio.
     *
     * Slice 8F-3.
     */
    public void confirmar(LocalDateTime fhConfirmacion, String idUserConfirmacion) {
        if (estadoRedaccion == EstadoRedaccionDocumento.ANULADA) {
            throw new PrecondicionVioladaException(
                    "No se puede confirmar una redaccion ANULADA.");
        }
        if (estadoRedaccion == EstadoRedaccionDocumento.CONFIRMADA) {
            throw new PrecondicionVioladaException(
                    "La redaccion ya esta CONFIRMADA. No se puede confirmar dos veces.");
        }
        if (contenidoEditable == null || contenidoEditable.isBlank()) {
            throw new PrecondicionVioladaException(
                    "No se puede confirmar una redaccion con contenido editable vacio.");
        }
        this.estadoRedaccion = EstadoRedaccionDocumento.CONFIRMADA;
        this.fhConfirmacion = fhConfirmacion;
        this.idUserConfirmacion = idUserConfirmacion;
    }

    /**
     * Anula la redaccion.
     *
     * Reglas:
     *   - Solo puede anularse una redaccion en estado BORRADOR o CONFIRMADA.
     *   - ANULADA no puede anularse dos veces.
     *   - fhAnulacion, idUserAnulacion y motivoAnulacion son obligatorios.
     *   - Una redaccion CONFIRMADA permanece immutable despues de anulada
     *     (el contenido editable no se modifica).
     *
     * El motivoAnulacion no se persiste en esta entidad; se registra
     * en fal_observacion(DOCUMENTO) por el servicio coordinador.
     *
     * CORRECCION-07: paridad con fh_anulacion/id_user_anulacion del DDL.
     */
    public void anular(LocalDateTime fhAnulacion, String idUserAnulacion, String motivoAnulacion) {
        if (estadoRedaccion == EstadoRedaccionDocumento.ANULADA) {
            throw new PrecondicionVioladaException(
                    "La redaccion ya esta ANULADA. No se puede anular dos veces.");
        }
        if (fhAnulacion == null) {
            throw new PrecondicionVioladaException(
                    "fhAnulacion es obligatorio para anular una redaccion.");
        }
        if (idUserAnulacion == null || idUserAnulacion.isBlank()) {
            throw new PrecondicionVioladaException(
                    "idUserAnulacion es obligatorio para anular una redaccion.");
        }
        if (motivoAnulacion == null || motivoAnulacion.isBlank()) {
            throw new PrecondicionVioladaException(
                    "motivoAnulacion es obligatorio para anular una redaccion.");
        }
        this.estadoRedaccion = EstadoRedaccionDocumento.ANULADA;
        this.fhAnulacion = fhAnulacion;
        this.idUserAnulacion = idUserAnulacion;
    }

    public boolean esReabierta() { return estadoRedaccion == EstadoRedaccionDocumento.REABIERTA; }
    public boolean estaAnulada() { return estadoRedaccion == EstadoRedaccionDocumento.ANULADA; }

    public FalDocumentoRedaccion copia() {
        FalDocumentoRedaccion c = new FalDocumentoRedaccion(
                id, idDocumento, plantillaContenidoId,
                nroRevision, redaccionOrigenId,
                estadoRedaccion, contenidoEditable,
                variablesSnapshotJson, variablesFaltantesJson, diagnosticoJson,
                fhCreacion, idUserCreacion,
                fhUltimaEdicion, idUserUltimaEdicion,
                fhConfirmacion, idUserConfirmacion);
        c.versionRow = this.versionRow;
        c.fhAnulacion = this.fhAnulacion;
        c.idUserAnulacion = this.idUserAnulacion;
        return c;
    }
}
