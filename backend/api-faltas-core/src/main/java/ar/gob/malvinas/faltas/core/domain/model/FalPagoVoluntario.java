package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoPagoVoluntario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad de pago voluntario asociada a un acta de faltas.
 *
 * Representa el flujo de pago voluntario: desde la solicitud hasta la confirmacion
 * o el vencimiento. No es un acto unico.
 *
 * Nota: referenciaPago es un dato temporal del slice. En produccion sera reemplazado
 * por la integracion real con el sistema de Ingresos/Tesoreria (Cmte_PG / Pref_PG / Nro_PG).
 * Deuda tecnica: slice posterior de integracion con valoracion y comprobantes reales.
 */
public class FalPagoVoluntario {

    private final String id;
    private final Long actaId;
    private EstadoPagoVoluntario estadoPagoVoluntario;
    private BigDecimal monto;
    private String referenciaPago;
    private String observaciones;
    private String motivoObservacion;

    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaMontoFijado;
    private LocalDateTime fechaInforme;
    private LocalDateTime fechaConfirmacion;
    private LocalDateTime fechaObservacion;
    private LocalDateTime fechaVencimiento;

    public FalPagoVoluntario(String id, Long actaId) {
        this.id = id;
        this.actaId = actaId;
        this.estadoPagoVoluntario = EstadoPagoVoluntario.SIN_PAGO;
    }

    public String getId() { return id; }
    public Long getActaId() { return actaId; }

    public EstadoPagoVoluntario getEstadoPagoVoluntario() { return estadoPagoVoluntario; }
    public void setEstadoPagoVoluntario(EstadoPagoVoluntario estadoPagoVoluntario) {
        this.estadoPagoVoluntario = estadoPagoVoluntario;
    }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public String getReferenciaPago() { return referenciaPago; }
    public void setReferenciaPago(String referenciaPago) { this.referenciaPago = referenciaPago; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getMotivoObservacion() { return motivoObservacion; }
    public void setMotivoObservacion(String motivoObservacion) { this.motivoObservacion = motivoObservacion; }

    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDateTime fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }

    public LocalDateTime getFechaMontoFijado() { return fechaMontoFijado; }
    public void setFechaMontoFijado(LocalDateTime fechaMontoFijado) { this.fechaMontoFijado = fechaMontoFijado; }

    public LocalDateTime getFechaInforme() { return fechaInforme; }
    public void setFechaInforme(LocalDateTime fechaInforme) { this.fechaInforme = fechaInforme; }

    public LocalDateTime getFechaConfirmacion() { return fechaConfirmacion; }
    public void setFechaConfirmacion(LocalDateTime fechaConfirmacion) { this.fechaConfirmacion = fechaConfirmacion; }

    public LocalDateTime getFechaObservacion() { return fechaObservacion; }
    public void setFechaObservacion(LocalDateTime fechaObservacion) { this.fechaObservacion = fechaObservacion; }

    public LocalDateTime getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDateTime fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    public boolean estaConfirmado() {
        return estadoPagoVoluntario == EstadoPagoVoluntario.CONFIRMADO;
    }

    public boolean estaVencido() {
        return estadoPagoVoluntario == EstadoPagoVoluntario.VENCIDO;
    }

    public boolean estaPendienteConfirmacion() {
        return estadoPagoVoluntario == EstadoPagoVoluntario.PENDIENTE_CONFIRMACION;
    }
}
