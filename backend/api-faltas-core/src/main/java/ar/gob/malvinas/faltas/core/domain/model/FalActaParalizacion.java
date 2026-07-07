package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.MotivoParalizacion;

import java.time.LocalDateTime;

/**
 * Ciclo de paralizacion de un acta (fal_acta_paralizacion).
 *
 * Un acta puede tener N ciclos historicos pero solo uno activo a la vez.
 * Cada paralizacion crea una nueva fila; la reactivacion cierra la fila activa.
 * Invariante: siActiva=true implica fhReactivacion=null e idUserReactivacion=null.
 */
public class FalActaParalizacion {

    private Long id;
    private int versionRow;
    private final Long actaId;
    private final MotivoParalizacion motivoParalizacion;
    private final LocalDateTime fhParalizacion;
    private final String idUserParalizacion;
    private LocalDateTime fhReactivacion;
    private String idUserReactivacion;
    private boolean siActiva;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;
    private LocalDateTime fhUltMod;
    private String idUserUltMod;

    public FalActaParalizacion(
            Long id,
            Long actaId,
            MotivoParalizacion motivoParalizacion,
            LocalDateTime fhParalizacion,
            String idUserParalizacion,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (actaId == null) throw new IllegalArgumentException("actaId obligatorio");
        if (motivoParalizacion == null) throw new IllegalArgumentException("motivoParalizacion obligatorio");
        if (fhParalizacion == null) throw new IllegalArgumentException("fhParalizacion obligatorio");
        this.id = id;
        this.actaId = actaId;
        this.motivoParalizacion = motivoParalizacion;
        this.fhParalizacion = fhParalizacion;
        this.idUserParalizacion = idUserParalizacion;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
        this.siActiva = true;
        this.versionRow = 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getVersionRow() { return versionRow; }
    public void setVersionRow(int versionRow) { this.versionRow = versionRow; }
    public Long getActaId() { return actaId; }
    public MotivoParalizacion getMotivoParalizacion() { return motivoParalizacion; }
    public LocalDateTime getFhParalizacion() { return fhParalizacion; }
    public String getIdUserParalizacion() { return idUserParalizacion; }
    public LocalDateTime getFhReactivacion() { return fhReactivacion; }
    public void setFhReactivacion(LocalDateTime fhReactivacion) { this.fhReactivacion = fhReactivacion; }
    public String getIdUserReactivacion() { return idUserReactivacion; }
    public void setIdUserReactivacion(String idUserReactivacion) { this.idUserReactivacion = idUserReactivacion; }
    public boolean isSiActiva() { return siActiva; }
    public void setSiActiva(boolean siActiva) { this.siActiva = siActiva; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
    public LocalDateTime getFhUltMod() { return fhUltMod; }
    public void setFhUltMod(LocalDateTime fhUltMod) { this.fhUltMod = fhUltMod; }
    public String getIdUserUltMod() { return idUserUltMod; }
    public void setIdUserUltMod(String idUserUltMod) { this.idUserUltMod = idUserUltMod; }

    public boolean estaActiva() {
        return siActiva;
    }

    public FalActaParalizacion copia() {
        FalActaParalizacion c = new FalActaParalizacion(id, actaId, motivoParalizacion,
                fhParalizacion, idUserParalizacion, fhAlta, idUserAlta);
        c.versionRow = this.versionRow;
        c.fhReactivacion = this.fhReactivacion;
        c.idUserReactivacion = this.idUserReactivacion;
        c.siActiva = this.siActiva;
        c.fhUltMod = this.fhUltMod;
        c.idUserUltMod = this.idUserUltMod;
        return c;
    }
}
