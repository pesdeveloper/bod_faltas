package ar.gob.malvinas.faltas.core.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Normativa de faltas, versionada historicamente.
 * Mapeada a {@code fal_normativa_faltas} en MariaDB.
 *
 * {@code versionNorma} es la version FUNCIONAL (historia reconstruible: una
 * corrección por error sin relaciones materiales puede reutilizar la misma
 * fila via OCC; con relaciones, se cierra vigencia y se crea nueva version).
 * {@code versionRow} es OCC de la fila, distinta de {@code versionNorma}
 * (PROMPT-NOCTURNO-WAVE-04-PARTE-2 S6).
 */
public class FalNormativaFaltas {
    private final Long id;
    private final String codigoNorma;
    private final int versionNorma;
    private final String nombreNorma;
    private String descripcionNorma;
    private boolean siActiva;
    private final LocalDate fhVigDesde;
    private LocalDate fhVigHasta;
    private int versionRow;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalNormativaFaltas(Long id, String codigoNorma, int versionNorma, String nombreNorma,
            LocalDate fhVigDesde, LocalDateTime fhAlta, String idUserAlta) {
        if (id == null) throw new IllegalArgumentException("id es obligatorio");
        if (codigoNorma == null || codigoNorma.isBlank()) throw new IllegalArgumentException("codigoNorma es obligatorio");
        if (versionNorma < 1) throw new IllegalArgumentException("versionNorma debe ser >= 1");
        if (nombreNorma == null || nombreNorma.isBlank()) throw new IllegalArgumentException("nombreNorma es obligatorio");
        if (fhVigDesde == null) throw new IllegalArgumentException("fhVigDesde es obligatoria");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta es obligatoria");
        if (idUserAlta == null || idUserAlta.isBlank()) throw new IllegalArgumentException("idUserAlta es obligatorio");
        this.id = id; this.codigoNorma = codigoNorma; this.versionNorma = versionNorma;
        this.nombreNorma = nombreNorma; this.fhVigDesde = fhVigDesde;
        this.siActiva = true; this.versionRow = 0; this.fhAlta = fhAlta; this.idUserAlta = idUserAlta;
    }
    public Long getId() { return id; }
    public String getCodigoNorma() { return codigoNorma; }
    public int getVersionNorma() { return versionNorma; }
    public String getNombreNorma() { return nombreNorma; }
    public String getDescripcionNorma() { return descripcionNorma; }
    public void setDescripcionNorma(String v) { this.descripcionNorma = v; }
    public boolean isSiActiva() { return siActiva; }
    public void setSiActiva(boolean v) { this.siActiva = v; }
    public LocalDate getFhVigDesde() { return fhVigDesde; }
    public LocalDate getFhVigHasta() { return fhVigHasta; }
    public void setFhVigHasta(LocalDate v) { this.fhVigHasta = v; }
    public int getVersionRow() { return versionRow; }
    public void setVersionRow(int v) { this.versionRow = v; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
    public boolean esVigenteEn(LocalDate fecha) {
        if (!siActiva) return false;
        if (fhVigDesde.isAfter(fecha)) return false;
        return fhVigHasta == null || fecha.isBefore(fhVigHasta);
    }

    public FalNormativaFaltas copia() {
        FalNormativaFaltas c = new FalNormativaFaltas(id, codigoNorma, versionNorma, nombreNorma, fhVigDesde, fhAlta, idUserAlta);
        c.descripcionNorma = this.descripcionNorma;
        c.siActiva = this.siActiva;
        c.fhVigHasta = this.fhVigHasta;
        c.versionRow = this.versionRow;
        return c;
    }
}
