package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.CriterioTarifario;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoValorizacion;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenValorizacion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoUnidadFaltas;
import ar.gob.malvinas.faltas.core.domain.enums.TipoValorizacionActa;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Decisión económica global de un acta en un momento dado.
 * Mapeada a fal_acta_valorizacion en MariaDB.
 *
 * versionRow controla concurrencia optimista en la fila.
 * Solo una vigente por acta+tipo en todo momento.
 * No se borra ni se reactiva: historial inmutable.
 * Una preliminar nunca desplaza la confirmada vigente.
 */
public class FalActaValorizacion {

    private final Long id;
    private int versionRow;
    private final Long actaId;
    private EstadoValorizacion estadoValorizacion;
    private final TipoValorizacionActa tipoValorizacionActa;
    private final OrigenValorizacion origenValorizacion;
    private CriterioTarifario criterioTarifario;
    private boolean tarifarioActualizado;
    private BigDecimal montoBaseArticulos;
    private BigDecimal montoFinal;
    private TipoUnidadFaltas tipoUnidadFinal;
    private BigDecimal cantidadUnidadesFinal;
    private BigDecimal valorUnidadFinal;
    private Long tarifarioUnidadId;
    private boolean siSobrescribeTotal;
    private boolean siCongelaValor;
    private LocalDateTime fhCongelamiento;
    private Long falloId;
    private Long documentoId;
    private LocalDateTime fhValorizacion;
    private String idUserValorizacion;
    private LocalDateTime fhConfirmacion;
    private String idUserConfirmacion;
    private boolean siVigente;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalActaValorizacion(
            Long id,
            Long actaId,
            TipoValorizacionActa tipoValorizacionActa,
            OrigenValorizacion origenValorizacion,
            CriterioTarifario criterioTarifario,
            BigDecimal montoFinal,
            LocalDateTime fhValorizacion,
            String idUserValorizacion,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (id == null) throw new IllegalArgumentException("id es obligatorio");
        if (actaId == null) throw new IllegalArgumentException("actaId es obligatorio");
        if (tipoValorizacionActa == null) throw new IllegalArgumentException("tipoValorizacionActa es obligatorio");
        if (origenValorizacion == null) throw new IllegalArgumentException("origenValorizacion es obligatorio");
        if (criterioTarifario == null) throw new IllegalArgumentException("criterioTarifario es obligatorio");
        if (montoFinal == null || montoFinal.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("montoFinal no puede ser negativo");
        if (fhValorizacion == null) throw new IllegalArgumentException("fhValorizacion es obligatoria");
        if (idUserValorizacion == null || idUserValorizacion.isBlank())
            throw new IllegalArgumentException("idUserValorizacion es obligatorio");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta es obligatoria");
        if (idUserAlta == null || idUserAlta.isBlank()) throw new IllegalArgumentException("idUserAlta es obligatorio");
        this.id = id;
        this.versionRow = 0;
        this.actaId = actaId;
        this.estadoValorizacion = EstadoValorizacion.PRELIMINAR;
        this.tipoValorizacionActa = tipoValorizacionActa;
        this.origenValorizacion = origenValorizacion;
        this.criterioTarifario = criterioTarifario;
        this.montoFinal = montoFinal.setScale(2, RoundingMode.HALF_UP);
        this.fhValorizacion = fhValorizacion;
        this.idUserValorizacion = idUserValorizacion;
        this.siVigente = false;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public Long getId() { return id; }
    public int getVersionRow() { return versionRow; }
    public void setVersionRow(int versionRow) { this.versionRow = versionRow; }
    public Long getActaId() { return actaId; }
    public EstadoValorizacion getEstadoValorizacion() { return estadoValorizacion; }
    public void setEstadoValorizacion(EstadoValorizacion v) { this.estadoValorizacion = v; }
    public TipoValorizacionActa getTipoValorizacionActa() { return tipoValorizacionActa; }
    public OrigenValorizacion getOrigenValorizacion() { return origenValorizacion; }
    public CriterioTarifario getCriterioTarifario() { return criterioTarifario; }
    public void setCriterioTarifario(CriterioTarifario v) { this.criterioTarifario = v; }
    public boolean isTarifarioActualizado() { return tarifarioActualizado; }
    public void setTarifarioActualizado(boolean v) { this.tarifarioActualizado = v; }

    public BigDecimal getMontoBaseArticulos() { return montoBaseArticulos; }
    public void setMontoBaseArticulos(BigDecimal v) {
        this.montoBaseArticulos = (v == null) ? null : v.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getMontoFinal() { return montoFinal; }
    public void setMontoFinal(BigDecimal v) {
        if (v == null || v.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("montoFinal no puede ser negativo");
        this.montoFinal = v.setScale(2, RoundingMode.HALF_UP);
    }

    public TipoUnidadFaltas getTipoUnidadFinal() { return tipoUnidadFinal; }
    public void setTipoUnidadFinal(TipoUnidadFaltas v) { this.tipoUnidadFinal = v; }

    public BigDecimal getCantidadUnidadesFinal() { return cantidadUnidadesFinal; }
    public void setCantidadUnidadesFinal(BigDecimal v) {
        this.cantidadUnidadesFinal = (v == null) ? null : v.setScale(4, RoundingMode.HALF_UP);
    }

    public BigDecimal getValorUnidadFinal() { return valorUnidadFinal; }
    public void setValorUnidadFinal(BigDecimal v) {
        this.valorUnidadFinal = (v == null) ? null : v.setScale(2, RoundingMode.HALF_UP);
    }

    public Long getTarifarioUnidadId() { return tarifarioUnidadId; }
    public void setTarifarioUnidadId(Long v) { this.tarifarioUnidadId = v; }

    public boolean isSiSobrescribeTotal() { return siSobrescribeTotal; }
    public void setSiSobrescribeTotal(boolean v) { this.siSobrescribeTotal = v; }

    public boolean isSiCongelaValor() { return siCongelaValor; }
    public void setSiCongelaValor(boolean v) {
        this.siCongelaValor = v;
        if (!v) this.fhCongelamiento = null;
    }

    public LocalDateTime getFhCongelamiento() { return fhCongelamiento; }
    public void setFhCongelamiento(LocalDateTime v) {
        if (v != null && !siCongelaValor)
            throw new IllegalArgumentException("fhCongelamiento requiere siCongelaValor=true");
        this.fhCongelamiento = v;
    }

    public Long getFalloId() { return falloId; }
    public void setFalloId(Long v) { this.falloId = v; }
    public Long getDocumentoId() { return documentoId; }
    public void setDocumentoId(Long v) { this.documentoId = v; }

    public LocalDateTime getFhValorizacion() { return fhValorizacion; }
    public void setFhValorizacion(LocalDateTime v) { this.fhValorizacion = v; }
    public String getIdUserValorizacion() { return idUserValorizacion; }
    public void setIdUserValorizacion(String v) { this.idUserValorizacion = v; }

    public LocalDateTime getFhConfirmacion() { return fhConfirmacion; }
    public void setFhConfirmacion(LocalDateTime v) { this.fhConfirmacion = v; }
    public String getIdUserConfirmacion() { return idUserConfirmacion; }
    public void setIdUserConfirmacion(String v) { this.idUserConfirmacion = v; }

    public boolean isSiVigente() { return siVigente; }
    public void setSiVigente(boolean v) { this.siVigente = v; }

    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }

    public boolean estaConfirmada() { return estadoValorizacion == EstadoValorizacion.CONFIRMADA; }
    public boolean estaVigenteYConfirmada() { return siVigente && estaConfirmada(); }

    public FalActaValorizacion copia() {
        FalActaValorizacion c = new FalActaValorizacion(
                id, actaId, tipoValorizacionActa, origenValorizacion, criterioTarifario,
                montoFinal, fhValorizacion, idUserValorizacion, fhAlta, idUserAlta);
        c.versionRow = this.versionRow;
        c.estadoValorizacion = this.estadoValorizacion;
        c.tarifarioActualizado = this.tarifarioActualizado;
        c.montoBaseArticulos = this.montoBaseArticulos;
        c.tipoUnidadFinal = this.tipoUnidadFinal;
        c.cantidadUnidadesFinal = this.cantidadUnidadesFinal;
        c.valorUnidadFinal = this.valorUnidadFinal;
        c.tarifarioUnidadId = this.tarifarioUnidadId;
        c.siSobrescribeTotal = this.siSobrescribeTotal;
        c.siCongelaValor = this.siCongelaValor;
        c.fhCongelamiento = this.fhCongelamiento;
        c.falloId = this.falloId;
        c.documentoId = this.documentoId;
        c.fhConfirmacion = this.fhConfirmacion;
        c.idUserConfirmacion = this.idUserConfirmacion;
        c.siVigente = this.siVigente;
        return c;
    }
}
