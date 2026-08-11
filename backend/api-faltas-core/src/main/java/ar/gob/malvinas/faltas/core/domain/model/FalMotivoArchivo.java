package ar.gob.malvinas.faltas.core.domain.model;

import java.time.LocalDateTime;

/**
 * Motivo de archivo administrable (fal_motivo_archivo).
 *
 * Catalogo abierto, no enum cerrado.
 * codMotivoArchivo es unico y estable.
 * Baja logica: si_activo=false. No se borra.
 * No tiene versionRow.
 * Los snapshots de motivo en cada ciclo de archivo son inmutables.
 *
 * Limites alineados al DDL de fal_motivo_archivo (spec-as-source
 * 50-persistence/mariadb-logical-model.md, HUMAN_DECISION_CLOSED
 * SPEC-MODEL-DDL-CLOSURE-001): cod_motivo_archivo VARCHAR(8), nombre VARCHAR(64).
 * La tabla no tiene columna descripcion y el modelo no la representa.
 */
public class FalMotivoArchivo {

    private Long id;
    private String codMotivoArchivo;
    private String nombre;
    private boolean siNulidad;
    private boolean siPermiteReingreso;
    private boolean siRequiereObservacion;
    private boolean siActivo;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;
    private LocalDateTime fhUltMod;
    private String idUserUltMod;

    public FalMotivoArchivo(
            Long id,
            String codMotivoArchivo,
            String nombre,
            boolean siNulidad,
            boolean siPermiteReingreso,
            boolean siRequiereObservacion,
            boolean siActivo,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (codMotivoArchivo == null || codMotivoArchivo.trim().isEmpty()) {
            throw new IllegalArgumentException("codMotivoArchivo obligatorio");
        }
        if (codMotivoArchivo.length() > 8) {
            throw new IllegalArgumentException("codMotivoArchivo supera 8 caracteres");
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("nombre obligatorio");
        }
        if (nombre.length() > 64) {
            throw new IllegalArgumentException("nombre supera 64 caracteres");
        }
        this.id = id;
        this.codMotivoArchivo = codMotivoArchivo.trim().toUpperCase();
        this.nombre = nombre.trim();
        this.siNulidad = siNulidad;
        this.siPermiteReingreso = siPermiteReingreso;
        this.siRequiereObservacion = siRequiereObservacion;
        this.siActivo = siActivo;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodMotivoArchivo() { return codMotivoArchivo; }
    public void setCodMotivoArchivo(String codMotivoArchivo) { this.codMotivoArchivo = codMotivoArchivo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public boolean isSiNulidad() { return siNulidad; }
    public void setSiNulidad(boolean siNulidad) { this.siNulidad = siNulidad; }
    public boolean isSiPermiteReingreso() { return siPermiteReingreso; }
    public void setSiPermiteReingreso(boolean siPermiteReingreso) { this.siPermiteReingreso = siPermiteReingreso; }
    public boolean isSiRequiereObservacion() { return siRequiereObservacion; }
    public void setSiRequiereObservacion(boolean siRequiereObservacion) { this.siRequiereObservacion = siRequiereObservacion; }
    public boolean isSiActivo() { return siActivo; }
    public void setSiActivo(boolean siActivo) { this.siActivo = siActivo; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
    public LocalDateTime getFhUltMod() { return fhUltMod; }
    public void setFhUltMod(LocalDateTime fhUltMod) { this.fhUltMod = fhUltMod; }
    public String getIdUserUltMod() { return idUserUltMod; }
    public void setIdUserUltMod(String idUserUltMod) { this.idUserUltMod = idUserUltMod; }

    public FalMotivoArchivo copia() {
        FalMotivoArchivo c = new FalMotivoArchivo(id, codMotivoArchivo, nombre,
                siNulidad, siPermiteReingreso, siRequiereObservacion, siActivo, fhAlta, idUserAlta);
        c.fhUltMod = this.fhUltMod;
        c.idUserUltMod = this.idUserUltMod;
        return c;
    }
}
