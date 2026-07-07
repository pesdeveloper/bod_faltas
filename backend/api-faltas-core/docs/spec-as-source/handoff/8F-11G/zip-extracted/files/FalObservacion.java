package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.EntidadTipoObservada;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenObservacion;

import java.time.LocalDateTime;

/**
 * Observacion vinculada a una entidad de dominio (fal_observacion).
 *
 * Entidad polimorf\u00eda: entidadTipo + entidadId identifican el objeto observado.
 * No tiene versionRow (no requiere OCC).
 * No se borra fisicamente: se desactiva mediante siActiva=false.
 */
public class FalObservacion {

    private final Long id;
    private final EntidadTipoObservada entidadTipo;
    private final Long entidadId;
    private Integer tipoObs;
    private final String observacion;
    private OrigenObservacion origenObs;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;
    private boolean siActiva;

    public FalObservacion(
            Long id,
            EntidadTipoObservada entidadTipo,
            Long entidadId,
            Integer tipoObs,
            String observacion,
            OrigenObservacion origenObs,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (entidadTipo == null) throw new IllegalArgumentException("entidadTipo obligatorio");
        if (entidadId == null) throw new IllegalArgumentException("entidadId obligatorio");
        if (observacion == null || observacion.trim().isEmpty()) {
            throw new IllegalArgumentException("observacion no puede ser vacia");
        }
        String trimmed = observacion.trim();
        if (trimmed.length() > 1000) {
            throw new IllegalArgumentException("observacion supera 1000 caracteres: " + trimmed.length());
        }
        this.id = id;
        this.entidadTipo = entidadTipo;
        this.entidadId = entidadId;
        this.tipoObs = tipoObs;
        this.observacion = trimmed;
        this.origenObs = origenObs;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
        this.siActiva = true;
    }

    public Long getId() { return id; }
    public EntidadTipoObservada getEntidadTipo() { return entidadTipo; }
    public Long getEntidadId() { return entidadId; }
    public Integer getTipoObs() { return tipoObs; }
    public void setTipoObs(Integer tipoObs) { this.tipoObs = tipoObs; }
    public String getObservacion() { return observacion; }
    public OrigenObservacion getOrigenObs() { return origenObs; }
    public void setOrigenObs(OrigenObservacion origenObs) { this.origenObs = origenObs; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
    public boolean isSiActiva() { return siActiva; }
    public void setSiActiva(boolean siActiva) { this.siActiva = siActiva; }

    public FalObservacion copia() {
        FalObservacion c = new FalObservacion(id, entidadTipo, entidadId, tipoObs,
                observacion, origenObs, fhAlta, idUserAlta);
        c.siActiva = this.siActiva;
        return c;
    }
}
