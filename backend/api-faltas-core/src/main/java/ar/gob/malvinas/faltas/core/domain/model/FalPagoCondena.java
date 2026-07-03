package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoPagoCondena;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad de pago de condena asociada a un acta de faltas.
 *
 * Representa el flujo de pago de la condena firme: desde la informacion del pago
 * hasta la confirmacion o la observacion. No es un acto unico.
 *
 * Eventos: PCOINF -> PCOCNF + CIERRA | PCOOBS (-> puede reinformar)
 *
 * PAGCON no existe como evento productivo.
 *
 * Nota: referenciaPago es un dato temporal del slice. En produccion sera reemplazado
 * por la integracion real con el sistema de Ingresos/Tesoreria.
 * Deuda tecnica: slice posterior de integracion con valoracion y comprobantes reales.
 */
public class FalPagoCondena {

    private final String id;
    private final Long actaId;
    private EstadoPagoCondena estadoPagoCondena;
    private BigDecimal monto;
    private String referenciaPago;
    private String observaciones;
    private String motivoObservacion;

    private LocalDateTime fechaInforme;
    private LocalDateTime fechaConfirmacion;
    private LocalDateTime fechaObservacion;

    public FalPagoCondena(String id, Long actaId) {
        this.id = id;
        this.actaId = actaId;
        this.estadoPagoCondena = EstadoPagoCondena.PENDIENTE;
    }

    public String getId() { return id; }
    public Long getActaId() { return actaId; }

    public EstadoPagoCondena getEstadoPagoCondena() { return estadoPagoCondena; }
    public void setEstadoPagoCondena(EstadoPagoCondena estadoPagoCondena) {
        this.estadoPagoCondena = estadoPagoCondena;
    }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public String getReferenciaPago() { return referenciaPago; }
    public void setReferenciaPago(String referenciaPago) { this.referenciaPago = referenciaPago; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getMotivoObservacion() { return motivoObservacion; }
    public void setMotivoObservacion(String motivoObservacion) { this.motivoObservacion = motivoObservacion; }

    public LocalDateTime getFechaInforme() { return fechaInforme; }
    public void setFechaInforme(LocalDateTime fechaInforme) { this.fechaInforme = fechaInforme; }

    public LocalDateTime getFechaConfirmacion() { return fechaConfirmacion; }
    public void setFechaConfirmacion(LocalDateTime fechaConfirmacion) { this.fechaConfirmacion = fechaConfirmacion; }

    public LocalDateTime getFechaObservacion() { return fechaObservacion; }
    public void setFechaObservacion(LocalDateTime fechaObservacion) { this.fechaObservacion = fechaObservacion; }

    public boolean estaConfirmado() {
        return estadoPagoCondena == EstadoPagoCondena.CONFIRMADO;
    }

    public boolean estaInformado() {
        return estadoPagoCondena == EstadoPagoCondena.INFORMADO;
    }

    public boolean estaObservado() {
        return estadoPagoCondena == EstadoPagoCondena.OBSERVADO;
    }
}

