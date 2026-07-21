package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.EntidadTipoObservada;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenObservacion;

import java.time.LocalDateTime;

/**
 * Observacion vinculada a una entidad de dominio (fal_observacion).
 *
 * Entidad polimorfica: entidadTipo + entidadId identifican el objeto observado.
 * No tiene versionRow (es append-only; no se edita).
 * Baja logica mediante siActiva=false + fhBaja + idUserBaja.
 *
 * idActaContexto: FK opcional a fal_acta. Cuando se informa, las observaciones
 * se eliminan en cascada al borrar fisicamente el acta.
 *
 * Longitud maxima: 512 caracteres (HUMAN_DECISION_CLOSED SPEC-MODEL-DDL-CLOSURE-001).
 */
public class FalObservacion {

    private final Long id;
    private final EntidadTipoObservada entidadTipo;
    private final Long entidadId;
    /** @deprecated Campo redundante con entidadTipo. NO PERSISTIR en MariaDB (HUMAN_DECISION_CLOSED). */
    @Deprecated
    private Integer tipoObs;
    private final String observacion;
    private final OrigenObservacion origenObs;
    private final Long idActaContexto;
    private boolean siActiva;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;
    private LocalDateTime fhBaja;
    private String idUserBaja;

    /** Constructor de compatibilidad (sin idActaContexto). */
    public FalObservacion(
            Long id,
            EntidadTipoObservada entidadTipo,
            Long entidadId,
            Integer tipoObs,
            String observacion,
            OrigenObservacion origenObs,
            LocalDateTime fhAlta,
            String idUserAlta) {
        this(id, entidadTipo, entidadId, tipoObs, observacion, origenObs, null, fhAlta, idUserAlta);
    }

    public FalObservacion(
            Long id,
            EntidadTipoObservada entidadTipo,
            Long entidadId,
            Integer tipoObs,
            String observacion,
            OrigenObservacion origenObs,
            Long idActaContexto,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (entidadTipo == null) throw new IllegalArgumentException("entidadTipo obligatorio");
        if (entidadId == null) throw new IllegalArgumentException("entidadId obligatorio");
        if (observacion == null || observacion.trim().isEmpty()) {
            throw new IllegalArgumentException("observacion no puede ser vacia");
        }
        String trimmed = observacion.trim();
        if (trimmed.length() > 512) {
            throw new IllegalArgumentException("observacion supera 512 caracteres: " + trimmed.length());
        }
        this.id = id;
        this.entidadTipo = entidadTipo;
        this.entidadId = entidadId;
        this.tipoObs = tipoObs;
        this.observacion = trimmed;
        this.origenObs = origenObs;
        this.idActaContexto = idActaContexto;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
        this.siActiva = true;
        this.fhBaja = null;
        this.idUserBaja = null;
    }

    public Long getId() { return id; }
    public EntidadTipoObservada getEntidadTipo() { return entidadTipo; }
    public Long getEntidadId() { return entidadId; }
    /** @deprecated Campo redundante con entidadTipo. NO PERSISTIR en MariaDB. */
    @Deprecated
    public Integer getTipoObs() { return tipoObs; }
    /** @deprecated Campo redundante con entidadTipo. NO PERSISTIR en MariaDB. */
    @Deprecated
    public void setTipoObs(Integer tipoObs) { this.tipoObs = tipoObs; }
    public String getObservacion() { return observacion; }
    public OrigenObservacion getOrigenObs() { return origenObs; }
    public Long getIdActaContexto() { return idActaContexto; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
    public boolean isSiActiva() { return siActiva; }
    public LocalDateTime getFhBaja() { return fhBaja; }
    public String getIdUserBaja() { return idUserBaja; }

    public void desactivar(LocalDateTime fhBaja, String idUserBaja) {
        if (!siActiva) throw new IllegalStateException("La observacion ya esta inactiva: id=" + id);
        if (fhBaja == null) throw new IllegalArgumentException("fhBaja requerida");
        if (idUserBaja == null || idUserBaja.isBlank()) throw new IllegalArgumentException("idUserBaja requerido");
        this.siActiva = false;
        this.fhBaja = fhBaja;
        this.idUserBaja = idUserBaja.trim();
    }

    /** @deprecated Usar desactivar(fhBaja, idUserBaja). Solo InMemory puede invocar esto directamente. */
    @Deprecated
    public void setSiActiva(boolean siActiva) { this.siActiva = siActiva; }

    public FalObservacion copia() {
        FalObservacion c = new FalObservacion(id, entidadTipo, entidadId, tipoObs,
                observacion, origenObs, idActaContexto, fhAlta, idUserAlta);
        c.siActiva = this.siActiva;
        c.fhBaja = this.fhBaja;
        c.idUserBaja = this.idUserBaja;
        return c;
    }
}
