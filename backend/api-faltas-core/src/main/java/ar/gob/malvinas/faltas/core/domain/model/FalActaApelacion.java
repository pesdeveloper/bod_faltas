package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoApelacionActa;

import java.time.LocalDateTime;

/**
 * Apelacion sobre el fallo condenatorio de un acta.
 *
 * Una apelacion activa (siActiva=true) es el recurso presentado por el infractor
 * contra el fallo condenatorio notificado.
 *
 * Solo puede existir una apelacion por acta (activa o resuelta).
 * El fallo al que aplica se referencia por falloId.
 *
 * Slice 3C: agrega campos de resolucion (fechaResolucion, fundamentosResolucion,
 * observacionesResolucion) usados en APERAZ y APEABS.
 */
public class FalActaApelacion {

    private final String id;
    private final Long actaId;
    private final String falloId;
    private EstadoApelacionActa estadoApelacion;
    private final LocalDateTime fechaPresentacion;
    private String presentante;
    private String fundamentos;
    private String observaciones;
    private boolean siActiva;
    private LocalDateTime fechaResolucion;
    private String fundamentosResolucion;
    private String observacionesResolucion;

    public FalActaApelacion(
            String id,
            Long actaId,
            String falloId,
            EstadoApelacionActa estadoApelacion,
            LocalDateTime fechaPresentacion,
            String presentante,
            String fundamentos,
            String observaciones,
            boolean siActiva) {
        this.id = id;
        this.actaId = actaId;
        this.falloId = falloId;
        this.estadoApelacion = estadoApelacion;
        this.fechaPresentacion = fechaPresentacion;
        this.presentante = presentante;
        this.fundamentos = fundamentos;
        this.observaciones = observaciones;
        this.siActiva = siActiva;
    }

    public String getId() { return id; }
    public Long getActaId() { return actaId; }
    public String getFalloId() { return falloId; }
    public EstadoApelacionActa getEstadoApelacion() { return estadoApelacion; }
    public void setEstadoApelacion(EstadoApelacionActa estadoApelacion) { this.estadoApelacion = estadoApelacion; }
    public LocalDateTime getFechaPresentacion() { return fechaPresentacion; }
    public String getPresentante() { return presentante; }
    public void setPresentante(String presentante) { this.presentante = presentante; }
    public String getFundamentos() { return fundamentos; }
    public void setFundamentos(String fundamentos) { this.fundamentos = fundamentos; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public boolean isSiActiva() { return siActiva; }
    public void setSiActiva(boolean siActiva) { this.siActiva = siActiva; }
    public LocalDateTime getFechaResolucion() { return fechaResolucion; }
    public void setFechaResolucion(LocalDateTime fechaResolucion) { this.fechaResolucion = fechaResolucion; }
    public String getFundamentosResolucion() { return fundamentosResolucion; }
    public void setFundamentosResolucion(String fundamentosResolucion) { this.fundamentosResolucion = fundamentosResolucion; }
    public String getObservacionesResolucion() { return observacionesResolucion; }
    public void setObservacionesResolucion(String observacionesResolucion) { this.observacionesResolucion = observacionesResolucion; }
}

