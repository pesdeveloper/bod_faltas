package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.TipoUnidadFaltas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Valor monetario de una unidad de faltas por tipo y vigencia temporal.
 * Mapeada a fal_tarifario_unidad_faltas en MariaDB.
 *
 * Un tarifario referenciado no se modifica funcionalmente: se crea una nueva fila.
 * No se permiten rangos activos superpuestos para el mismo tipo de unidad.
 */
public class FalTarifarioUnidadFaltas {

    private final Long id;
    private final TipoUnidadFaltas tipoUnidad;
    private final BigDecimal valorUnidad;
    private final LocalDate fhVigDesde;
    private LocalDate fhVigHasta;
    private boolean siActiva;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalTarifarioUnidadFaltas(
            Long id,
            TipoUnidadFaltas tipoUnidad,
            BigDecimal valorUnidad,
            LocalDate fhVigDesde,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (id == null) throw new IllegalArgumentException("id es obligatorio");
        if (tipoUnidad == null) throw new IllegalArgumentException("tipoUnidad es obligatorio");
        if (valorUnidad == null || valorUnidad.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("valorUnidad debe ser mayor que cero");
        if (fhVigDesde == null) throw new IllegalArgumentException("fhVigDesde es obligatoria");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta es obligatoria");
        if (idUserAlta == null || idUserAlta.isBlank()) throw new IllegalArgumentException("idUserAlta es obligatorio");
        this.id = id;
        this.tipoUnidad = tipoUnidad;
        this.valorUnidad = valorUnidad.setScale(2, RoundingMode.HALF_UP);
        this.fhVigDesde = fhVigDesde;
        this.siActiva = true;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public Long getId() { return id; }
    public TipoUnidadFaltas getTipoUnidad() { return tipoUnidad; }
    public BigDecimal getValorUnidad() { return valorUnidad; }
    public LocalDate getFhVigDesde() { return fhVigDesde; }

    public LocalDate getFhVigHasta() { return fhVigHasta; }
    public void setFhVigHasta(LocalDate fhVigHasta) {
        if (fhVigHasta != null && fhVigHasta.isBefore(fhVigDesde))
            throw new IllegalArgumentException("fhVigHasta no puede ser anterior a fhVigDesde");
        this.fhVigHasta = fhVigHasta;
    }

    public boolean isSiActiva() { return siActiva; }
    public void setSiActiva(boolean siActiva) { this.siActiva = siActiva; }

    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }

    public boolean esVigenteEn(LocalDate fecha) {
        if (!siActiva) return false;
        if (fhVigDesde.isAfter(fecha)) return false;
        if (fhVigHasta != null && !fecha.isBefore(fhVigHasta)) return false;
        return true;
    }

    public FalTarifarioUnidadFaltas copia() {
        FalTarifarioUnidadFaltas c = new FalTarifarioUnidadFaltas(id, tipoUnidad, valorUnidad, fhVigDesde, fhAlta, idUserAlta);
        c.fhVigHasta = this.fhVigHasta;
        c.siActiva = this.siActiva;
        return c;
    }
}
