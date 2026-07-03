package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Politica de numeracion de actas o documentos.
 *
 * Define composicion del numero visible y comportamiento de reinicio anual.
 * Alineado con num_politica del modelo MariaDB productivo.
 *
 * Slice 8B-2: implementacion in-memory. Slice 9: persiste en num_politica via JDBC.
 */
public class NumPolitica {

    private final Long id;
    private final String codigo;
    private final String descripcion;
    private final ClaseNumeracion claseNumeracion;
    private final boolean siReinicioAnual;
    private final boolean siIncluyePrefijo;
    private final String prefijo;
    private final boolean siIncluyeAnio;
    private final Short formatoAnio;
    private final boolean siIncluyeSerie;
    private final Short longitudNro;
    private final String formatoVisible;
    private boolean siActiva;
    private final LocalDate fhVigDesde;
    private LocalDate fhVigHasta;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public NumPolitica(
            Long id,
            String codigo,
            String descripcion,
            ClaseNumeracion claseNumeracion,
            boolean siReinicioAnual,
            boolean siIncluyePrefijo,
            String prefijo,
            boolean siIncluyeAnio,
            Short formatoAnio,
            boolean siIncluyeSerie,
            Short longitudNro,
            String formatoVisible,
            boolean siActiva,
            LocalDate fhVigDesde,
            LocalDate fhVigHasta,
            LocalDateTime fhAlta,
            String idUserAlta) {
        this.id = id;
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.claseNumeracion = claseNumeracion;
        this.siReinicioAnual = siReinicioAnual;
        this.siIncluyePrefijo = siIncluyePrefijo;
        this.prefijo = prefijo;
        this.siIncluyeAnio = siIncluyeAnio;
        this.formatoAnio = formatoAnio;
        this.siIncluyeSerie = siIncluyeSerie;
        this.longitudNro = longitudNro;
        this.formatoVisible = formatoVisible;
        this.siActiva = siActiva;
        this.fhVigDesde = fhVigDesde;
        this.fhVigHasta = fhVigHasta;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getDescripcion() { return descripcion; }
    public ClaseNumeracion getClaseNumeracion() { return claseNumeracion; }
    public boolean isSiReinicioAnual() { return siReinicioAnual; }
    public boolean isSiIncluyePrefijo() { return siIncluyePrefijo; }
    public String getPrefijo() { return prefijo; }
    public boolean isSiIncluyeAnio() { return siIncluyeAnio; }
    public Short getFormatoAnio() { return formatoAnio; }
    public boolean isSiIncluyeSerie() { return siIncluyeSerie; }
    public Short getLongitudNro() { return longitudNro; }
    public String getFormatoVisible() { return formatoVisible; }
    public boolean isSiActiva() { return siActiva; }
    public void setSiActiva(boolean siActiva) { this.siActiva = siActiva; }
    public LocalDate getFhVigDesde() { return fhVigDesde; }
    public LocalDate getFhVigHasta() { return fhVigHasta; }
    public void setFhVigHasta(LocalDate fhVigHasta) { this.fhVigHasta = fhVigHasta; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
}
