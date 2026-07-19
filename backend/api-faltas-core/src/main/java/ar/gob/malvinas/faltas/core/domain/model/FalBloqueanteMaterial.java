package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoBloqueanteMaterial;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenBloqueanteMaterial;

import java.time.LocalDateTime;

/**
 * Bloqueante material registrado sobre un acta.
 *
 * Un bloqueante activo (siActivo == true) impide el cierre administrativo del acta,
 * incluso cuando el resultado final es cerrable (CONDENA_FIRME, ABSUELTO, etc.).
 *
 * El cierre del acta queda pendiente hasta que todos los bloqueantes se resuelvan
 * (CUMPLIDO o ANULADO) y siActivo pase a false.
 *
 * Slice 7A: implementacion in-memory. Slice 9: reemplazar por JDBC.
 */
public class FalBloqueanteMaterial {

    private final Long id;
    private int versionRow;
    private final Long actaId;
    private OrigenBloqueanteMaterial origen;
    private EstadoBloqueanteMaterial estado;
    private boolean siActivo;
    private String descripcion;
    private LocalDateTime fechaAlta;
    private LocalDateTime fechaCierre;

    public FalBloqueanteMaterial(Long id, Long actaId, LocalDateTime fechaAlta) {
        this.id = id;
        this.actaId = actaId;
        this.estado = EstadoBloqueanteMaterial.PENDIENTE;
        this.siActivo = true;
        this.fechaAlta = fechaAlta;
        this.versionRow = 0;
    }

    public Long getId() { return id; }
    public int getVersionRow() { return versionRow; }
    public void setVersionRow(int versionRow) { this.versionRow = versionRow; }
    public Long getActaId() { return actaId; }

    public OrigenBloqueanteMaterial getOrigen() { return origen; }
    public void setOrigen(OrigenBloqueanteMaterial origen) { this.origen = origen; }

    public EstadoBloqueanteMaterial getEstado() { return estado; }
    public void setEstado(EstadoBloqueanteMaterial estado) { this.estado = estado; }

    public boolean isSiActivo() { return siActivo; }
    public void setSiActivo(boolean siActivo) { this.siActivo = siActivo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public LocalDateTime getFechaAlta() { return fechaAlta; }
    public void setFechaAlta(LocalDateTime fechaAlta) { this.fechaAlta = fechaAlta; }

    public LocalDateTime getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(LocalDateTime fechaCierre) { this.fechaCierre = fechaCierre; }

    public FalBloqueanteMaterial copia() {
        FalBloqueanteMaterial c = new FalBloqueanteMaterial(id, actaId, fechaAlta);
        c.versionRow = this.versionRow;
        c.origen = this.origen;
        c.estado = this.estado;
        c.siActivo = this.siActivo;
        c.descripcion = this.descripcion;
        c.fechaCierre = this.fechaCierre;
        return c;
    }
}

