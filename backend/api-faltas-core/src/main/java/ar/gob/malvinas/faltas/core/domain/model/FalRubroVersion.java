package ar.gob.malvinas.faltas.core.domain.model;

import java.time.LocalDateTime;

/**
 * Version versionada de rubro comercial sincronizado desde Informix/Ingresos.
 * Mapeada a fal_rubro_version en MariaDB.
 *
 * No es el maestro primario de rubros. La fuente es informix.rubrocom.
 * Una sola version actual por Id_Rub (si_version_actual=true, valid_to=null).
 * Cambios de nombre o estado cierran version anterior y crean nueva.
 */
public class FalRubroVersion {

    private final Long rubroId;
    private final int idRub;
    private final String nombre;
    private String nombreNorm;
    private final short sidesabilitado;
    private final boolean siActivo;
    private final String rowHash;
    private String previousRowHash;
    private final String sourceOperation;
    private String closeOperation;
    private boolean siVersionActual;
    private final LocalDateTime validFrom;
    private LocalDateTime validTo;
    private final LocalDateTime syncedAt;

    public FalRubroVersion(
            Long rubroId,
            int idRub,
            String nombre,
            short sidesabilitado,
            String rowHash,
            String sourceOperation,
            LocalDateTime validFrom,
            LocalDateTime syncedAt) {
        if (rubroId == null) throw new IllegalArgumentException("rubroId es obligatorio");
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("nombre es obligatorio");
        if (nombre.length() > 120) throw new IllegalArgumentException("nombre max 120 caracteres");
        if (rowHash == null || rowHash.isBlank()) throw new IllegalArgumentException("rowHash es obligatorio");
        if (rowHash.length() > 128) throw new IllegalArgumentException("rowHash max 128 caracteres");
        if (sourceOperation == null || sourceOperation.isBlank()) throw new IllegalArgumentException("sourceOperation es obligatorio");
        if (sourceOperation.length() > 30) throw new IllegalArgumentException("sourceOperation max 30 caracteres");
        if (validFrom == null) throw new IllegalArgumentException("validFrom es obligatorio");
        if (syncedAt == null) throw new IllegalArgumentException("syncedAt es obligatorio");
        this.rubroId = rubroId;
        this.idRub = idRub;
        this.nombre = nombre;
        this.sidesabilitado = sidesabilitado;
        this.siActivo = (sidesabilitado == 0);
        this.rowHash = rowHash;
        this.sourceOperation = sourceOperation;
        this.siVersionActual = true;
        this.validFrom = validFrom;
        this.syncedAt = syncedAt;
    }

    public Long getRubroId() { return rubroId; }
    public int getIdRub() { return idRub; }
    public String getNombre() { return nombre; }
    public String getNombreNorm() { return nombreNorm; }
    public void setNombreNorm(String nombreNorm) {
        if (nombreNorm != null && nombreNorm.length() > 120)
            throw new IllegalArgumentException("nombreNorm max 120 caracteres");
        this.nombreNorm = nombreNorm;
    }
    public short getSidesabilitado() { return sidesabilitado; }
    public boolean isSiActivo() { return siActivo; }
    public String getRowHash() { return rowHash; }
    public String getPreviousRowHash() { return previousRowHash; }
    public void setPreviousRowHash(String previousRowHash) { this.previousRowHash = previousRowHash; }
    public String getSourceOperation() { return sourceOperation; }
    public String getCloseOperation() { return closeOperation; }
    public void setCloseOperation(String closeOperation) {
        if (closeOperation != null && closeOperation.length() > 30)
            throw new IllegalArgumentException("closeOperation max 30 caracteres");
        this.closeOperation = closeOperation;
    }
    public boolean isSiVersionActual() { return siVersionActual; }
    public void setSiVersionActual(boolean siVersionActual) { this.siVersionActual = siVersionActual; }
    public LocalDateTime getValidFrom() { return validFrom; }
    public LocalDateTime getValidTo() { return validTo; }
    public void setValidTo(LocalDateTime validTo) { this.validTo = validTo; }
    public LocalDateTime getSyncedAt() { return syncedAt; }

    public FalRubroVersion copia() {
        FalRubroVersion c = new FalRubroVersion(rubroId, idRub, nombre, sidesabilitado, rowHash, sourceOperation, validFrom, syncedAt);
        c.nombreNorm = this.nombreNorm;
        c.previousRowHash = this.previousRowHash;
        c.closeOperation = this.closeOperation;
        c.siVersionActual = this.siVersionActual;
        c.validTo = this.validTo;
        return c;
    }
}
