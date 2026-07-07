package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoPlanPago;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoAptitudIntimacion;

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
 * Caches de cuotas/mora son auxiliares; la verdad es en Ingresos.
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
    private Short cantidadCuotasPagadas;
    private Short cantidadCuotasVencidas;
    private Short cantidadCuotasEnMora;
    private Short cantidadCuotasMoraConsec;
    private Short diasMoraMax;
    private LocalDateTime fhUltimoPago;
    private LocalDateTime fhCaida;
    private LocalDateTime fhCancelacion;
    private LocalDateTime fhRefinanciacion;
    private Long planRefinanciadoId;
    private boolean siAptoIntimacion;
    private LocalDateTime fhAptoIntimacion;
    private MotivoAptitudIntimacion motivoAptaIntimacion;
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
    public Short getCantidadCuotasPagadas() { return cantidadCuotasPagadas; }
    public void setCantidadCuotasPagadas(Short v) {
        if (v != null && v < 0) throw new IllegalArgumentException("cantidadCuotasPagadas no puede ser negativo");
        this.cantidadCuotasPagadas = v;
    }
    public Short getCantidadCuotasVencidas() { return cantidadCuotasVencidas; }
    public void setCantidadCuotasVencidas(Short v) { this.cantidadCuotasVencidas = v; }
    public Short getCantidadCuotasEnMora() { return cantidadCuotasEnMora; }
    public void setCantidadCuotasEnMora(Short v) { this.cantidadCuotasEnMora = v; }
    public Short getCantidadCuotasMoraConsec() { return cantidadCuotasMoraConsec; }
    public void setCantidadCuotasMoraConsec(Short v) { this.cantidadCuotasMoraConsec = v; }
    public Short getDiasMoraMax() { return diasMoraMax; }
    public void setDiasMoraMax(Short v) { this.diasMoraMax = v; }
    public LocalDateTime getFhUltimoPago() { return fhUltimoPago; }
    public void setFhUltimoPago(LocalDateTime v) { this.fhUltimoPago = v; }
    public LocalDateTime getFhCaida() { return fhCaida; }
    public void setFhCaida(LocalDateTime v) { this.fhCaida = v; }
    public LocalDateTime getFhCancelacion() { return fhCancelacion; }
    public void setFhCancelacion(LocalDateTime v) { this.fhCancelacion = v; }
    public LocalDateTime getFhRefinanciacion() { return fhRefinanciacion; }
    public void setFhRefinanciacion(LocalDateTime v) { this.fhRefinanciacion = v; }
    public Long getPlanRefinanciadoId() { return planRefinanciadoId; }
    public void setPlanRefinanciadoId(Long v) { this.planRefinanciadoId = v; }
    public boolean isSiAptoIntimacion() { return siAptoIntimacion; }
    public void setSiAptoIntimacion(boolean v) { this.siAptoIntimacion = v; }
    public LocalDateTime getFhAptoIntimacion() { return fhAptoIntimacion; }
    public void setFhAptoIntimacion(LocalDateTime v) { this.fhAptoIntimacion = v; }
    public MotivoAptitudIntimacion getMotivoAptaIntimacion() { return motivoAptaIntimacion; }
    public void setMotivoAptaIntimacion(MotivoAptitudIntimacion v) { this.motivoAptaIntimacion = v; }
    public boolean isSiExcluirEscaneo() { return siExcluirEscaneo; }
    public void setSiExcluirEscaneo(boolean v) { this.siExcluirEscaneo = v; }
    public LocalDateTime getFhUltSyncIngresos() { return fhUltSyncIngresos; }
    public void setFhUltSyncIngresos(LocalDateTime v) { this.fhUltSyncIngresos = v; }
    public boolean isSiVigente() { return siVigente; }
    public void setSiVigente(boolean v) { this.siVigente = v; }

    public boolean estaActivo() { return estadoPlan == EstadoPlanPago.ACTIVO; }
    public boolean estaCaido() { return estadoPlan == EstadoPlanPago.CAIDO; }
    public boolean estaCancelado() { return estadoPlan == EstadoPlanPago.CANCELADO; }
    public boolean estaRefinanciado() { return estadoPlan == EstadoPlanPago.REFINANCIADO; }

    public FalActaPlanPagoRef copia() {
        FalActaPlanPagoRef c = new FalActaPlanPagoRef(
                id, formaPagoId, obligacionPagoId, idTdocPlan, idDocPlan,
                cantidadCuotas, importeTotalPlan);
        c.versionRow = this.versionRow;
        c.estadoPlan = this.estadoPlan;
        c.fhGeneracionPlan = this.fhGeneracionPlan;
        c.importeCuotaRegular = this.importeCuotaRegular;
        c.cantidadCuotasPagadas = this.cantidadCuotasPagadas;
        c.cantidadCuotasVencidas = this.cantidadCuotasVencidas;
        c.cantidadCuotasEnMora = this.cantidadCuotasEnMora;
        c.cantidadCuotasMoraConsec = this.cantidadCuotasMoraConsec;
        c.diasMoraMax = this.diasMoraMax;
        c.fhUltimoPago = this.fhUltimoPago;
        c.fhCaida = this.fhCaida;
        c.fhCancelacion = this.fhCancelacion;
        c.fhRefinanciacion = this.fhRefinanciacion;
        c.planRefinanciadoId = this.planRefinanciadoId;
        c.siAptoIntimacion = this.siAptoIntimacion;
        c.fhAptoIntimacion = this.fhAptoIntimacion;
        c.motivoAptaIntimacion = this.motivoAptaIntimacion;
        c.siExcluirEscaneo = this.siExcluirEscaneo;
        c.fhUltSyncIngresos = this.fhUltSyncIngresos;
        c.siVigente = this.siVigente;
        return c;
    }
}
