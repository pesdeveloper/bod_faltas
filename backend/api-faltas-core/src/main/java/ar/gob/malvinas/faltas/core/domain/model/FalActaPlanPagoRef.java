package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoPlanPago;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Referencia a plan de pago externo (Ingresos/Tesoreria) asociado a una forma de pago.
 * Mapeada a fal_acta_plan_pago_ref en MariaDB.
 *
 * idTdocPlan = 1 segun modelo vigente.
 * Par (idTdocPlan, idDocPlan) unico.
 * Un plan vigente por obligacion.
 * Cabecera estructural resumida; mora y cuotas dinamicas viven en proyeccion.
 */
public class FalActaPlanPagoRef {

    private final Long id;
    private int versionRow;
    private final Long formaPagoId;
    private final Long obligacionPagoId;
    private final short idTdocPlan;
    private final long idDocPlan;
    private EstadoPlanPago estadoPlan;
    private LocalDateTime fhGeneracionPlan;
    private final short cantidadCuotas;
    private final BigDecimal importeTotalPlan;
    private BigDecimal importeCuotaRegular;
    private LocalDateTime fhUltimoPago;
    private LocalDateTime fhFinalizacionPago;
    private LocalDateTime fhCancelacion;
    private LocalDateTime fhRefinanciacion;
    private Long planRefinanciadoId;
    private boolean siExcluirEscaneo;
    private LocalDateTime fhUltSyncIngresos;
    private boolean siVigente;

    public FalActaPlanPagoRef(
            Long id,
            Long formaPagoId,
            Long obligacionPagoId,
            short idTdocPlan,
            long idDocPlan,
            short cantidadCuotas,
            BigDecimal importeTotalPlan) {
        if (id == null) throw new IllegalArgumentException("id es obligatorio");
        if (formaPagoId == null) throw new IllegalArgumentException("formaPagoId es obligatorio");
        if (obligacionPagoId == null) throw new IllegalArgumentException("obligacionPagoId es obligatorio");
        if (cantidadCuotas <= 0) throw new IllegalArgumentException("cantidadCuotas debe ser positivo");
        if (importeTotalPlan == null || importeTotalPlan.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("importeTotalPlan no puede ser negativo");
        this.id = id;
        this.versionRow = 0;
        this.formaPagoId = formaPagoId;
        this.obligacionPagoId = obligacionPagoId;
        this.idTdocPlan = idTdocPlan;
        this.idDocPlan = idDocPlan;
        this.estadoPlan = EstadoPlanPago.ACTIVO;
        this.cantidadCuotas = cantidadCuotas;
        this.importeTotalPlan = importeTotalPlan.setScale(2, RoundingMode.HALF_UP);
        this.siVigente = true;
    }

    public Long getId() { return id; }
    public int getVersionRow() { return versionRow; }
    public void setVersionRow(int v) { this.versionRow = v; }
    public Long getFormaPagoId() { return formaPagoId; }
    public Long getObligacionPagoId() { return obligacionPagoId; }
    public short getIdTdocPlan() { return idTdocPlan; }
    public long getIdDocPlan() { return idDocPlan; }
    public EstadoPlanPago getEstadoPlan() { return estadoPlan; }
    public void setEstadoPlan(EstadoPlanPago v) { this.estadoPlan = v; }
    public LocalDateTime getFhGeneracionPlan() { return fhGeneracionPlan; }
    public void setFhGeneracionPlan(LocalDateTime v) { this.fhGeneracionPlan = v; }
    public short getCantidadCuotas() { return cantidadCuotas; }
    public BigDecimal getImporteTotalPlan() { return importeTotalPlan; }
    public BigDecimal getImporteCuotaRegular() { return importeCuotaRegular; }
    public void setImporteCuotaRegular(BigDecimal v) {
        if (v != null && v.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("importeCuotaRegular no puede ser negativo");
        this.importeCuotaRegular = (v == null) ? null : v.setScale(2, RoundingMode.HALF_UP);
    }
    public LocalDateTime getFhUltimoPago() { return fhUltimoPago; }
    public void setFhUltimoPago(LocalDateTime v) { this.fhUltimoPago = v; }
    public LocalDateTime getFhFinalizacionPago() { return fhFinalizacionPago; }
    public void setFhFinalizacionPago(LocalDateTime v) { this.fhFinalizacionPago = v; }
    public LocalDateTime getFhCancelacion() { return fhCancelacion; }
    public void setFhCancelacion(LocalDateTime v) { this.fhCancelacion = v; }
    public LocalDateTime getFhRefinanciacion() { return fhRefinanciacion; }
    public void setFhRefinanciacion(LocalDateTime v) { this.fhRefinanciacion = v; }
    public Long getPlanRefinanciadoId() { return planRefinanciadoId; }
    public void setPlanRefinanciadoId(Long v) { this.planRefinanciadoId = v; }
    public boolean isSiExcluirEscaneo() { return siExcluirEscaneo; }
    public void setSiExcluirEscaneo(boolean v) { this.siExcluirEscaneo = v; }
    public LocalDateTime getFhUltSyncIngresos() { return fhUltSyncIngresos; }
    public void setFhUltSyncIngresos(LocalDateTime v) { this.fhUltSyncIngresos = v; }
    public boolean isSiVigente() { return siVigente; }
    public void setSiVigente(boolean v) { this.siVigente = v; }

    public boolean estaActivo() { return estadoPlan == EstadoPlanPago.ACTIVO; }
    public boolean estaAnulado() { return estadoPlan == EstadoPlanPago.ANULADO; }
    public boolean estaRefinanciado() { return estadoPlan == EstadoPlanPago.REFINANCIADO; }

    public FalActaPlanPagoRef copia() {
        FalActaPlanPagoRef c = new FalActaPlanPagoRef(
                id, formaPagoId, obligacionPagoId, idTdocPlan, idDocPlan,
                cantidadCuotas, importeTotalPlan);
        c.versionRow = this.versionRow;
        c.estadoPlan = this.estadoPlan;
        c.fhGeneracionPlan = this.fhGeneracionPlan;
        c.importeCuotaRegular = this.importeCuotaRegular;
        c.fhUltimoPago = this.fhUltimoPago;
        c.fhFinalizacionPago = this.fhFinalizacionPago;
        c.fhFinalizacionPago = this.fhFinalizacionPago;
        c.fhCancelacion = this.fhCancelacion;
        c.fhRefinanciacion = this.fhRefinanciacion;
        c.planRefinanciadoId = this.planRefinanciadoId;
        c.siExcluirEscaneo = this.siExcluirEscaneo;
        c.fhUltSyncIngresos = this.fhUltSyncIngresos;
        c.siVigente = this.siVigente;
        return c;
    }
}
