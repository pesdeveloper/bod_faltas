package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoConciliacionActual;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoFormaPago;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoObligacionPago;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoPlanPago;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoAptitudIntimacion;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoPlanCaidoCalculado;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenUltimaActualizacion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFormaPago;
import ar.gob.malvinas.faltas.core.domain.enums.TipoObligacionPago;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public class FalActaEconomiaProyeccion {

    private final Long actaId;
    private int versionRow;
    private Long obligacionVigenteId;
    private Long formaPagoVigenteId;
    private Long planPagoVigenteId;
    private TipoObligacionPago tipoObligacion;
    private EstadoObligacionPago estadoObligacion;
    private BigDecimal montoObligacionVigente;
    private TipoFormaPago tipoFormaPago;
    private EstadoFormaPago estadoFormaPago;
    private EstadoPlanPago estadoPlan;
    private Short cantidadCuotas;
    private BigDecimal importeCuotaRegular;
    private Short cantidadCuotasPagadas;
    private Short cantidadCuotasVencidas;
    private Short cantidadCuotasEnMora;
    private Short cantidadCuotasMoraConsec;
    private Short diasMoraMax;
    private BigDecimal importePagoProcesado;
    private BigDecimal importeConfirmadoEvidenciaPendiente;
    private BigDecimal importeConfirmadoTesoreria;
    private BigDecimal importeObservadoTesoreria;
    private BigDecimal importeAplicadoTotal;
    private BigDecimal importeRevertido;
    private BigDecimal saldoPendiente;
    private BigDecimal importeExcedente;
    private boolean siParcialmentePagada;
    private BigDecimal importeVencidoPlan;
    private EstadoConciliacionActual estadoConciliacionActual;
    private boolean siConciliacionPendiente;
    private LocalDateTime fhUltimaConciliacion;
    private String referenciaUltimaConciliacion;
    private boolean siPagoProcesado;
    private boolean siPagoConfirmado;
    private boolean siPlanCaidoCalculado;
    private LocalDateTime fhDesdePlanCaidoCalculado;
    private MotivoPlanCaidoCalculado motivoPlanCaidoCalculado;
    private boolean siAptaIntimacion;
    private LocalDateTime fhAptaIntimacion;
    private MotivoAptitudIntimacion motivoAptaIntimacion;
    private boolean siReaperturaRequerida;
    private Long ultimoMovimientoIdProyectado;
    private LocalDateTime fhCorteEconomico;
    private LocalDateTime fhUltimaSincronizacion;
    private OrigenUltimaActualizacion origenUltimaActualizacion;
    private LocalDateTime fhUltMod;
    private String idUserUltMod;

    public FalActaEconomiaProyeccion(Long actaId) {
        if (actaId == null) throw new IllegalArgumentException("actaId requerido");
        this.actaId = actaId;
        this.versionRow = 0;
        this.importeAplicadoTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.importeExcedente = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.estadoConciliacionActual = EstadoConciliacionActual.NO_APLICA;
        this.origenUltimaActualizacion = OrigenUltimaActualizacion.TIEMPO_REAL;
    }

    public Long getActaId() { return actaId; }
    public int getVersionRow() { return versionRow; }
    public void setVersionRow(int v) { this.versionRow = v; }
    public Long getObligacionVigenteId() { return obligacionVigenteId; }
    public void setObligacionVigenteId(Long v) { this.obligacionVigenteId = v; }
    public Long getFormaPagoVigenteId() { return formaPagoVigenteId; }
    public void setFormaPagoVigenteId(Long v) { this.formaPagoVigenteId = v; }
    public Long getPlanPagoVigenteId() { return planPagoVigenteId; }
    public void setPlanPagoVigenteId(Long v) { this.planPagoVigenteId = v; }
    public TipoObligacionPago getTipoObligacion() { return tipoObligacion; }
    public void setTipoObligacion(TipoObligacionPago v) { this.tipoObligacion = v; }
    public EstadoObligacionPago getEstadoObligacion() { return estadoObligacion; }
    public void setEstadoObligacion(EstadoObligacionPago v) { this.estadoObligacion = v; }
    public BigDecimal getMontoObligacionVigente() { return montoObligacionVigente; }
    public void setMontoObligacionVigente(BigDecimal v) { this.montoObligacionVigente = scale(v); }
    public TipoFormaPago getTipoFormaPago() { return tipoFormaPago; }
    public void setTipoFormaPago(TipoFormaPago v) { this.tipoFormaPago = v; }
    public EstadoFormaPago getEstadoFormaPago() { return estadoFormaPago; }
    public void setEstadoFormaPago(EstadoFormaPago v) { this.estadoFormaPago = v; }
    public EstadoPlanPago getEstadoPlan() { return estadoPlan; }
    public void setEstadoPlan(EstadoPlanPago v) { this.estadoPlan = v; }
    public Short getCantidadCuotas() { return cantidadCuotas; }
    public void setCantidadCuotas(Short v) { this.cantidadCuotas = v; }
    public BigDecimal getImporteCuotaRegular() { return importeCuotaRegular; }
    public void setImporteCuotaRegular(BigDecimal v) { this.importeCuotaRegular = scale(v); }
    public Short getCantidadCuotasPagadas() { return cantidadCuotasPagadas; }
    public void setCantidadCuotasPagadas(Short v) { this.cantidadCuotasPagadas = v; }
    public Short getCantidadCuotasVencidas() { return cantidadCuotasVencidas; }
    public void setCantidadCuotasVencidas(Short v) { this.cantidadCuotasVencidas = v; }
    public Short getCantidadCuotasEnMora() { return cantidadCuotasEnMora; }
    public void setCantidadCuotasEnMora(Short v) { this.cantidadCuotasEnMora = v; }
    public Short getCantidadCuotasMoraConsec() { return cantidadCuotasMoraConsec; }
    public void setCantidadCuotasMoraConsec(Short v) { this.cantidadCuotasMoraConsec = v; }
    public Short getDiasMoraMax() { return diasMoraMax; }
    public void setDiasMoraMax(Short v) { this.diasMoraMax = v; }
    public BigDecimal getImportePagoProcesado() { return importePagoProcesado; }
    public void setImportePagoProcesado(BigDecimal v) { this.importePagoProcesado = scale(v); }
    public BigDecimal getImporteConfirmadoEvidenciaPendiente() { return importeConfirmadoEvidenciaPendiente; }
    public void setImporteConfirmadoEvidenciaPendiente(BigDecimal v) { this.importeConfirmadoEvidenciaPendiente = scale(v); }
    public BigDecimal getImporteConfirmadoTesoreria() { return importeConfirmadoTesoreria; }
    public void setImporteConfirmadoTesoreria(BigDecimal v) { this.importeConfirmadoTesoreria = scale(v); }
    public BigDecimal getImporteObservadoTesoreria() { return importeObservadoTesoreria; }
    public void setImporteObservadoTesoreria(BigDecimal v) { this.importeObservadoTesoreria = scale(v); }
    public BigDecimal getImporteAplicadoTotal() { return importeAplicadoTotal; }
    public void setImporteAplicadoTotal(BigDecimal v) { this.importeAplicadoTotal = scale(v); }
    public BigDecimal getImporteRevertido() { return importeRevertido; }
    public void setImporteRevertido(BigDecimal v) { this.importeRevertido = scale(v); }
    public BigDecimal getSaldoPendiente() { return saldoPendiente; }
    public void setSaldoPendiente(BigDecimal v) { this.saldoPendiente = scale(v); }
    public BigDecimal getImporteExcedente() { return importeExcedente; }
    public void setImporteExcedente(BigDecimal v) { this.importeExcedente = scale(v); }
    public boolean isSiParcialmentePagada() { return siParcialmentePagada; }
    public void setSiParcialmentePagada(boolean v) { this.siParcialmentePagada = v; }
    /** Derivado de estadoObligacion; no es un dato de entrada independiente. */
    public boolean isSiCancelada() { return estadoObligacion == EstadoObligacionPago.CANCELADA_POR_PAGO; }
    public BigDecimal getImporteVencidoPlan() { return importeVencidoPlan; }
    public void setImporteVencidoPlan(BigDecimal v) { this.importeVencidoPlan = scale(v); }
    public EstadoConciliacionActual getEstadoConciliacionActual() { return estadoConciliacionActual; }
    public void setEstadoConciliacionActual(EstadoConciliacionActual v) { this.estadoConciliacionActual = v; }
    public boolean isSiConciliacionPendiente() { return siConciliacionPendiente; }
    public void setSiConciliacionPendiente(boolean v) { this.siConciliacionPendiente = v; }
    public LocalDateTime getFhUltimaConciliacion() { return fhUltimaConciliacion; }
    public void setFhUltimaConciliacion(LocalDateTime v) { this.fhUltimaConciliacion = v; }
    public String getReferenciaUltimaConciliacion() { return referenciaUltimaConciliacion; }
    public void setReferenciaUltimaConciliacion(String v) { this.referenciaUltimaConciliacion = v; }
    public boolean isSiPagoProcesado() { return siPagoProcesado; }
    public void setSiPagoProcesado(boolean v) { this.siPagoProcesado = v; }
    public boolean isSiPagoConfirmado() { return siPagoConfirmado; }
    public void setSiPagoConfirmado(boolean v) { this.siPagoConfirmado = v; }
    public boolean isSiPlanCaidoCalculado() { return siPlanCaidoCalculado; }
    public void setSiPlanCaidoCalculado(boolean v) { this.siPlanCaidoCalculado = v; }
    public LocalDateTime getFhDesdePlanCaidoCalculado() { return fhDesdePlanCaidoCalculado; }
    public void setFhDesdePlanCaidoCalculado(LocalDateTime v) { this.fhDesdePlanCaidoCalculado = v; }
    public MotivoPlanCaidoCalculado getMotivoPlanCaidoCalculado() { return motivoPlanCaidoCalculado; }
    public void setMotivoPlanCaidoCalculado(MotivoPlanCaidoCalculado v) { this.motivoPlanCaidoCalculado = v; }
    public boolean isSiAptaIntimacion() { return siAptaIntimacion; }
    public void setSiAptaIntimacion(boolean v) { this.siAptaIntimacion = v; }
    public LocalDateTime getFhAptaIntimacion() { return fhAptaIntimacion; }
    public void setFhAptaIntimacion(LocalDateTime v) { this.fhAptaIntimacion = v; }
    public MotivoAptitudIntimacion getMotivoAptaIntimacion() { return motivoAptaIntimacion; }
    public void setMotivoAptaIntimacion(MotivoAptitudIntimacion v) { this.motivoAptaIntimacion = v; }
    public boolean isSiReaperturaRequerida() { return siReaperturaRequerida; }
    public void setSiReaperturaRequerida(boolean v) { this.siReaperturaRequerida = v; }
    public Long getUltimoMovimientoIdProyectado() { return ultimoMovimientoIdProyectado; }
    public void setUltimoMovimientoIdProyectado(Long v) { this.ultimoMovimientoIdProyectado = v; }
    public LocalDateTime getFhCorteEconomico() { return fhCorteEconomico; }
    public void setFhCorteEconomico(LocalDateTime v) { this.fhCorteEconomico = v; }
    public LocalDateTime getFhUltimaSincronizacion() { return fhUltimaSincronizacion; }
    public void setFhUltimaSincronizacion(LocalDateTime v) { this.fhUltimaSincronizacion = v; }
    public OrigenUltimaActualizacion getOrigenUltimaActualizacion() { return origenUltimaActualizacion; }
    public void setOrigenUltimaActualizacion(OrigenUltimaActualizacion v) { this.origenUltimaActualizacion = v; }
    public LocalDateTime getFhUltMod() { return fhUltMod; }
    public void setFhUltMod(LocalDateTime v) { this.fhUltMod = v; }
    public String getIdUserUltMod() { return idUserUltMod; }
    public void setIdUserUltMod(String v) { this.idUserUltMod = v; }

    public FalActaEconomiaProyeccion copia() {
        FalActaEconomiaProyeccion c = new FalActaEconomiaProyeccion(actaId);
        c.versionRow = versionRow;
        c.obligacionVigenteId = obligacionVigenteId;
        c.formaPagoVigenteId = formaPagoVigenteId;
        c.planPagoVigenteId = planPagoVigenteId;
        c.tipoObligacion = tipoObligacion;
        c.estadoObligacion = estadoObligacion;
        c.montoObligacionVigente = montoObligacionVigente;
        c.tipoFormaPago = tipoFormaPago;
        c.estadoFormaPago = estadoFormaPago;
        c.estadoPlan = estadoPlan;
        c.cantidadCuotas = cantidadCuotas;
        c.importeCuotaRegular = importeCuotaRegular;
        c.cantidadCuotasPagadas = cantidadCuotasPagadas;
        c.cantidadCuotasVencidas = cantidadCuotasVencidas;
        c.cantidadCuotasEnMora = cantidadCuotasEnMora;
        c.cantidadCuotasMoraConsec = cantidadCuotasMoraConsec;
        c.diasMoraMax = diasMoraMax;
        c.importePagoProcesado = importePagoProcesado;
        c.importeConfirmadoEvidenciaPendiente = importeConfirmadoEvidenciaPendiente;
        c.importeConfirmadoTesoreria = importeConfirmadoTesoreria;
        c.importeObservadoTesoreria = importeObservadoTesoreria;
        c.importeAplicadoTotal = importeAplicadoTotal;
        c.importeRevertido = importeRevertido;
        c.saldoPendiente = saldoPendiente;
        c.importeExcedente = importeExcedente;
        c.siParcialmentePagada = siParcialmentePagada;
        c.importeVencidoPlan = importeVencidoPlan;
        c.estadoConciliacionActual = estadoConciliacionActual;
        c.siConciliacionPendiente = siConciliacionPendiente;
        c.fhUltimaConciliacion = fhUltimaConciliacion;
        c.referenciaUltimaConciliacion = referenciaUltimaConciliacion;
        c.siPagoProcesado = siPagoProcesado;
        c.siPagoConfirmado = siPagoConfirmado;
        c.siPlanCaidoCalculado = siPlanCaidoCalculado;
        c.fhDesdePlanCaidoCalculado = fhDesdePlanCaidoCalculado;
        c.motivoPlanCaidoCalculado = motivoPlanCaidoCalculado;
        c.siAptaIntimacion = siAptaIntimacion;
        c.fhAptaIntimacion = fhAptaIntimacion;
        c.motivoAptaIntimacion = motivoAptaIntimacion;
        c.siReaperturaRequerida = siReaperturaRequerida;
        c.ultimoMovimientoIdProyectado = ultimoMovimientoIdProyectado;
        c.fhCorteEconomico = fhCorteEconomico;
        c.fhUltimaSincronizacion = fhUltimaSincronizacion;
        c.origenUltimaActualizacion = origenUltimaActualizacion;
        c.fhUltMod = fhUltMod;
        c.idUserUltMod = idUserUltMod;
        return c;
    }

    private static BigDecimal scale(BigDecimal v) {
        return v == null ? null : v.setScale(2, RoundingMode.HALF_UP);
    }
}
