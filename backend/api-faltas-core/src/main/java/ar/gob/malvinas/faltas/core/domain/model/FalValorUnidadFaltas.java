package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.TipoUnidadFaltas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Valor monetario historico y vigente de una unidad economica (SALARIO/UNIDAD_FIJA).
 * Mapeada a {@code fal_valor_unidad_faltas} en MariaDB.
 *
 * {@code MONTO} no tiene fila aqui: su valor vive directamente en
 * {@code fal_tarifario_unidad_faltas.monto_fijo_*}.
 *
 * Nueva tabla del rediseño de valorizacion (PROMPT-NOCTURNO-WAVE-04-PARTE-2 S7.2).
 * No tiene {@code idUserModificacion}: es una fila historica inmutable salvo por el
 * cierre de vigencia (baja logica de la fila anterior al activar una nueva).
 */
public class FalValorUnidadFaltas {

    private final Long id;
    private final TipoUnidadFaltas tipoUnidad;
    private final BigDecimal valorUnidad;
    private final LocalDate fhVigDesde;
    private LocalDate fhVigHasta;
    private boolean siActiva;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalValorUnidadFaltas(
            Long id,
            TipoUnidadFaltas tipoUnidad,
            BigDecimal valorUnidad,
            LocalDate fhVigDesde,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (id == null) throw new IllegalArgumentException("id es obligatorio");
        if (tipoUnidad == null) throw new IllegalArgumentException("tipoUnidad es obligatorio");
        if (tipoUnidad == TipoUnidadFaltas.MONTO)
            throw new IllegalArgumentException("MONTO no tiene fila en fal_valor_unidad_faltas");
        if (valorUnidad == null || valorUnidad.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("valorUnidad debe ser mayor que cero");
        if (fhVigDesde == null) throw new IllegalArgumentException("fhVigDesde es obligatoria");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta es obligatoria");
        if (idUserAlta == null || idUserAlta.isBlank()) throw new IllegalArgumentException("idUserAlta es obligatorio");
        this.id = id;
        this.tipoUnidad = tipoUnidad;
        this.valorUnidad = valorUnidad.setScale(4, RoundingMode.HALF_UP);
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

    /**
     * Vigencia HISTORICA por rango de fechas, independiente de {@code siActiva}.
     * Para el valor OPERATIVO actual usar el repositorio (lookup por {@code si_activa}).
     */
    public boolean esVigenteEn(LocalDate fecha) {
        if (fhVigDesde.isAfter(fecha)) return false;
        return fhVigHasta == null || fecha.isBefore(fhVigHasta);
    }

    public FalValorUnidadFaltas copia() {
        FalValorUnidadFaltas c = new FalValorUnidadFaltas(id, tipoUnidad, valorUnidad, fhVigDesde, fhAlta, idUserAlta);
        c.fhVigHasta = this.fhVigHasta;
        c.siActiva = this.siActiva;
        return c;
    }
}
