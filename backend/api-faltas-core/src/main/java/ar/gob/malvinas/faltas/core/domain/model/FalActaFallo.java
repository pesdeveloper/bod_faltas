package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoFalloActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFalloActa;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fallo dictado sobre un acta de faltas.
 *
 * El fallo recorre su propio circuito: dictado -> firma -> notificacion.
 * No cierra el acta al ser dictado ni al ser firmado.
 * El absolutorio notificado sin bloqueantes cierra el acta.
 * El condenatorio notificado abre etapa post-fallo (apelacion / pago condena).
 *
 * documentoId se asigna al generar el documento de fallo en el mismo comando.
 * siActivo = false se reserva para anulacion (SIN_EFECTO), no implementada en este slice.
 */
public class FalActaFallo {

    private final String id;
    private final Long actaId;
    private final TipoFalloActa tipoFallo;
    private EstadoFalloActa estadoFallo;
    private BigDecimal montoCondena;
    private String fundamentos;
    private Long documentoId;
    private final LocalDateTime fechaDictado;
    private LocalDateTime fechaNotificacion;
    private LocalDateTime fechaResultadoFinal;
    private boolean siActivo;

    public FalActaFallo(
            String id,
            Long actaId,
            TipoFalloActa tipoFallo,
            LocalDateTime fechaDictado) {
        this.id = id;
        this.actaId = actaId;
        this.tipoFallo = tipoFallo;
        this.fechaDictado = fechaDictado;
        this.estadoFallo = EstadoFalloActa.PENDIENTE_FIRMA;
        this.siActivo = true;
    }

    public String getId() { return id; }
    public Long getActaId() { return actaId; }
    public TipoFalloActa getTipoFallo() { return tipoFallo; }

    public EstadoFalloActa getEstadoFallo() { return estadoFallo; }
    public void setEstadoFallo(EstadoFalloActa estadoFallo) { this.estadoFallo = estadoFallo; }

    public BigDecimal getMontoCondena() { return montoCondena; }
    public void setMontoCondena(BigDecimal montoCondena) { this.montoCondena = montoCondena; }

    public String getFundamentos() { return fundamentos; }
    public void setFundamentos(String fundamentos) { this.fundamentos = fundamentos; }

    public Long getDocumentoId() { return documentoId; }
    public void setDocumentoId(Long documentoId) { this.documentoId = documentoId; }

    public LocalDateTime getFechaDictado() { return fechaDictado; }

    public LocalDateTime getFechaNotificacion() { return fechaNotificacion; }
    public void setFechaNotificacion(LocalDateTime fechaNotificacion) { this.fechaNotificacion = fechaNotificacion; }

    public LocalDateTime getFechaResultadoFinal() { return fechaResultadoFinal; }
    public void setFechaResultadoFinal(LocalDateTime fechaResultadoFinal) {
        this.fechaResultadoFinal = fechaResultadoFinal;
    }

    public boolean isSiActivo() { return siActivo; }
    public void setSiActivo(boolean siActivo) { this.siActivo = siActivo; }

    public boolean esAbsolutorio() { return tipoFallo == TipoFalloActa.ABSOLUTORIO; }
    public boolean esCondenatorio() { return tipoFallo == TipoFalloActa.CONDENATORIO; }
}

