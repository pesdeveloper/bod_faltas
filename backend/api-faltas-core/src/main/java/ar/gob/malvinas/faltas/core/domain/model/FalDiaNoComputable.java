package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.OrigenDiaNoComputable;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDiaNoComputable;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class FalDiaNoComputable {

    private final Long id;
    private final LocalDate fecha;
    private final TipoDiaNoComputable tipo;
    private final String descripcion;
    private final OrigenDiaNoComputable origen;
    private final String referenciaExterna;
    private boolean siActivo;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;
    private LocalDateTime fhBaja;
    private String idUserBaja;

    public FalDiaNoComputable(
            Long id,
            LocalDate fecha,
            TipoDiaNoComputable tipo,
            String descripcion,
            OrigenDiaNoComputable origen,
            String referenciaExterna,
            LocalDateTime fhAlta,
            String idUserAlta) {

        if (id == null) throw new IllegalArgumentException("id es obligatorio");
        if (fecha == null) throw new IllegalArgumentException("fecha es obligatoria");
        if (tipo == null) throw new IllegalArgumentException("tipo es obligatorio");
        if (descripcion == null || descripcion.isBlank())
            throw new IllegalArgumentException("descripcion es obligatoria");
        String descNorm = descripcion.trim();
        if (descNorm.length() > 160)
            throw new IllegalArgumentException("descripcion max 160 caracteres");
        if (origen == null) throw new IllegalArgumentException("origen es obligatorio");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta es obligatoria");

        if (idUserAlta == null || idUserAlta.isBlank())
            throw new IllegalArgumentException("idUserAlta es obligatorio");
        String actorNorm = idUserAlta.trim();
        if (actorNorm.length() > 36)
            throw new IllegalArgumentException("idUserAlta max 36 caracteres");

        if (origen == OrigenDiaNoComputable.MANUAL) {
            if (referenciaExterna != null)
                throw new IllegalArgumentException("referenciaExterna debe ser null para origen MANUAL");
        } else if (origen == OrigenDiaNoComputable.SINCRONIZACION_EXTERNA) {
            if (referenciaExterna == null || referenciaExterna.isBlank())
                throw new IllegalArgumentException("referenciaExterna es obligatoria para SINCRONIZACION_EXTERNA");
            String refNorm = referenciaExterna.trim();
            if (refNorm.length() > 200)
                throw new IllegalArgumentException("referenciaExterna max 200 caracteres");
        }

        this.id = id;
        this.fecha = fecha;
        this.tipo = tipo;
        this.descripcion = descNorm;
        this.origen = origen;
        this.referenciaExterna = (origen == OrigenDiaNoComputable.SINCRONIZACION_EXTERNA)
                ? referenciaExterna.trim()
                : null;
        this.fhAlta = fhAlta;
        this.idUserAlta = actorNorm;
        this.siActivo = true;
        this.fhBaja = null;
        this.idUserBaja = null;
    }

    public void desactivar(LocalDateTime ahora, String actor) {
        if (!siActivo)
            throw new PrecondicionVioladaException("El dia no computable ya esta inactivo: id=" + id);
        if (ahora == null) throw new IllegalArgumentException("ahora es obligatorio");
        if (actor == null || actor.isBlank())
            throw new IllegalArgumentException("actor es obligatorio");
        String actorNorm = actor.trim();
        if (actorNorm.length() > 36)
            throw new IllegalArgumentException("actor max 36 caracteres");
        this.siActivo = false;
        this.fhBaja = ahora;
        this.idUserBaja = actorNorm;
    }

    public FalDiaNoComputable copia() {
        FalDiaNoComputable c = new FalDiaNoComputable(
                id, fecha, tipo, descripcion, origen,
                (origen == OrigenDiaNoComputable.SINCRONIZACION_EXTERNA) ? referenciaExterna : null,
                fhAlta, idUserAlta);
        c.siActivo = this.siActivo;
        c.fhBaja = this.fhBaja;
        c.idUserBaja = this.idUserBaja;
        return c;
    }

    public Long getId() { return id; }
    public LocalDate getFecha() { return fecha; }
    public TipoDiaNoComputable getTipo() { return tipo; }
    public String getDescripcion() { return descripcion; }
    public OrigenDiaNoComputable getOrigen() { return origen; }
    public String getReferenciaExterna() { return referenciaExterna; }
    public boolean isSiActivo() { return siActivo; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
    public LocalDateTime getFhBaja() { return fhBaja; }
    public String getIdUserBaja() { return idUserBaja; }
}
