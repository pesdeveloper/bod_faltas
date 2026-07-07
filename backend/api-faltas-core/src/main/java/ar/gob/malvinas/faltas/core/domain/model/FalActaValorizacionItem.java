package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.MotivoManualizacionValorizacion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoUnidadFaltas;
import ar.gob.malvinas.faltas.core.domain.enums.TipoValorizacionItem;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cálculo congelado de un artículo imputado dentro de una valorización.
 * Mapeada a fal_acta_valorizacion_item en MariaDB.
 *
 * Sin versionRow ni auditoría de usuario: inmutable una vez confirmada la valorización madre.
 * actaArticuloId es obligatorio salvo ajuste total/manual global.
 */
public class FalActaValorizacionItem {

    private final Long id;
    private final Long valorizacionId;
    private final Long actaArticuloId;
    private final TipoValorizacionItem tipoValorizacionItem;
    private TipoUnidadFaltas tipoUnidadBase;
    private BigDecimal cantidadUnidadesBase;
    private TipoUnidadFaltas tipoUnidadAplicada;
    private BigDecimal cantidadUnidadesAplicada;
    private BigDecimal valorUnidadAplicado;
    private final BigDecimal montoAplicado;
    private Long tarifarioUnidadId;
    private final boolean siManual;
    private MotivoManualizacionValorizacion motivoManual;
    private Long documentoId;
    private Long falloId;

    public FalActaValorizacionItem(
            Long id,
            Long valorizacionId,
            Long actaArticuloId,
            TipoValorizacionItem tipoValorizacionItem,
            BigDecimal montoAplicado,
            boolean siManual) {
        if (id == null) throw new IllegalArgumentException("id es obligatorio");
        if (valorizacionId == null) throw new IllegalArgumentException("valorizacionId es obligatorio");
        if (tipoValorizacionItem == null) throw new IllegalArgumentException("tipoValorizacionItem es obligatorio");
        if (montoAplicado == null || montoAplicado.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("montoAplicado no puede ser negativo");
        this.id = id;
        this.valorizacionId = valorizacionId;
        this.actaArticuloId = actaArticuloId;
        this.tipoValorizacionItem = tipoValorizacionItem;
        this.montoAplicado = montoAplicado.setScale(2, RoundingMode.HALF_UP);
        this.siManual = siManual;
    }

    public Long getId() { return id; }
    public Long getValorizacionId() { return valorizacionId; }
    public Long getActaArticuloId() { return actaArticuloId; }
    public TipoValorizacionItem getTipoValorizacionItem() { return tipoValorizacionItem; }

    public TipoUnidadFaltas getTipoUnidadBase() { return tipoUnidadBase; }
    public void setTipoUnidadBase(TipoUnidadFaltas v) { this.tipoUnidadBase = v; }

    public BigDecimal getCantidadUnidadesBase() { return cantidadUnidadesBase; }
    public void setCantidadUnidadesBase(BigDecimal v) {
        this.cantidadUnidadesBase = (v == null) ? null : v.setScale(4, RoundingMode.HALF_UP);
    }

    public TipoUnidadFaltas getTipoUnidadAplicada() { return tipoUnidadAplicada; }
    public void setTipoUnidadAplicada(TipoUnidadFaltas v) { this.tipoUnidadAplicada = v; }

    public BigDecimal getCantidadUnidadesAplicada() { return cantidadUnidadesAplicada; }
    public void setCantidadUnidadesAplicada(BigDecimal v) {
        this.cantidadUnidadesAplicada = (v == null) ? null : v.setScale(4, RoundingMode.HALF_UP);
    }

    public BigDecimal getValorUnidadAplicado() { return valorUnidadAplicado; }
    public void setValorUnidadAplicado(BigDecimal v) {
        this.valorUnidadAplicado = (v == null) ? null : v.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getMontoAplicado() { return montoAplicado; }

    public Long getTarifarioUnidadId() { return tarifarioUnidadId; }
    public void setTarifarioUnidadId(Long v) { this.tarifarioUnidadId = v; }

    public boolean isSiManual() { return siManual; }

    public MotivoManualizacionValorizacion getMotivoManual() { return motivoManual; }
    public void setMotivoManual(MotivoManualizacionValorizacion v) { this.motivoManual = v; }

    public Long getDocumentoId() { return documentoId; }
    public void setDocumentoId(Long v) { this.documentoId = v; }

    public Long getFalloId() { return falloId; }
    public void setFalloId(Long v) { this.falloId = v; }

    public FalActaValorizacionItem copia() {
        FalActaValorizacionItem c = new FalActaValorizacionItem(
                id, valorizacionId, actaArticuloId, tipoValorizacionItem, montoAplicado, siManual);
        c.tipoUnidadBase = this.tipoUnidadBase;
        c.cantidadUnidadesBase = this.cantidadUnidadesBase;
        c.tipoUnidadAplicada = this.tipoUnidadAplicada;
        c.cantidadUnidadesAplicada = this.cantidadUnidadesAplicada;
        c.valorUnidadAplicado = this.valorUnidadAplicado;
        c.tarifarioUnidadId = this.tarifarioUnidadId;
        c.motivoManual = this.motivoManual;
        c.documentoId = this.documentoId;
        c.falloId = this.falloId;
        return c;
    }
}
