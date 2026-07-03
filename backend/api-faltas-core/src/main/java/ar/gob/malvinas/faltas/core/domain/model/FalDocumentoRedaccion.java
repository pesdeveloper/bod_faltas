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
 * Slice 8F-1.
 */
public class FalDocumentoRedaccion {

    private final Long id;
    private final Long idDocumento;
    private final Long plantillaContenidoId;
    private EstadoRedaccionDocumento estadoRedaccion;
    private String contenidoEditable;
    private final String variablesSnapshotJson;
    private String variablesFaltantesJson;
    private String diagnosticoJson;
    private final LocalDateTime fhCreacion;
    private final String idUserCreacion;
    private LocalDateTime fhUltimaEdicion;
    private String idUserUltimaEdicion;
    private LocalDateTime fhConfirmacion;
    private String idUserConfirmacion;

    public FalDocumentoRedaccion(
            Long id, Long idDocumento, Long plantillaContenidoId,
            EstadoRedaccionDocumento estadoRedaccion, String contenidoEditable,
            String variablesSnapshotJson, String variablesFaltantesJson, String diagnosticoJson,
            LocalDateTime fhCreacion, String idUserCreacion,
            LocalDateTime fhUltimaEdicion, String idUserUltimaEdicion,
            LocalDateTime fhConfirmacion, String idUserConfirmacion) {
        if (id == null) throw new IllegalArgumentException("id requerido");
        if (idDocumento == null) throw new IllegalArgumentException("idDocumento requerido");
        if (plantillaContenidoId == null) throw new IllegalArgumentException("plantillaContenidoId requerido");
        if (estadoRedaccion == null) throw new IllegalArgumentException("estadoRedaccion requerido");
        if (contenidoEditable == null) throw new IllegalArgumentException("contenidoEditable no puede ser null");
        if (fhCreacion == null) throw new IllegalArgumentException("fhCreacion requerido");
        if (idUserCreacion == null || idUserCreacion.isBlank())
            throw new IllegalArgumentException("idUserCreacion requerido");

        this.id = id;
        this.idDocumento = idDocumento;
        this.plantillaContenidoId = plantillaContenidoId;
        this.estadoRedaccion = estadoRedaccion;
        this.contenidoEditable = contenidoEditable;
        this.variablesSnapshotJson = variablesSnapshotJson;
        this.variablesFaltantesJson = variablesFaltantesJson;
        this.diagnosticoJson = diagnosticoJson;
        this.fhCreacion = fhCreacion;
        this.idUserCreacion = idUserCreacion;
        this.fhUltimaEdicion = fhUltimaEdicion;
        this.idUserUltimaEdicion = idUserUltimaEdicion;
        this.fhConfirmacion = fhConfirmacion;
        this.idUserConfirmacion = idUserConfirmacion;
    }

    public boolean esBorrador() { return estadoRedaccion == EstadoRedaccionDocumento.BORRADOR; }
    public boolean estaConfirmada() { return estadoRedaccion == EstadoRedaccionDocumento.CONFIRMADA; }

    public Long getId() { return id; }
    public Long getIdDocumento() { return idDocumento; }
    public Long getPlantillaContenidoId() { return plantillaContenidoId; }
    public EstadoRedaccionDocumento getEstadoRedaccion() { return estadoRedaccion; }
    public void setEstadoRedaccion(EstadoRedaccionDocumento e) { this.estadoRedaccion = e; }
    public String getContenidoEditable() { return contenidoEditable; }
    public void setContenidoEditable(String v) { this.contenidoEditable = v; }
    public String getVariablesSnapshotJson() { return variablesSnapshotJson; }
    public String getVariablesFaltantesJson() { return variablesFaltantesJson; }
    public void setVariablesFaltantesJson(String v) { this.variablesFaltantesJson = v; }
    public String getDiagnosticoJson() { return diagnosticoJson; }
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

    public boolean esReabierta() { return estadoRedaccion == EstadoRedaccionDocumento.REABIERTA; }
    public boolean estaAnulada() { return estadoRedaccion == EstadoRedaccionDocumento.ANULADA; }
}



