package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.CanalNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoNotificacion;

import java.time.LocalDateTime;

/**
 * Representa cada intento concreto realizado dentro de una notificacion.
 * Una notificacion puede tener N intentos; cada reintento crea una nueva fila.
 * Invariante: (notificacionId, nroIntento) es unico dentro del sistema.
 */
public class FalNotificacionIntento {

    private final Long id;
    private final Long notificacionId;
    private final short nroIntento;
    private final CanalNotificacion canalNotif;
    private EstadoNotificacion estadoIntento;
    private ResultadoNotificacion resultadoIntento;
    private final Long domicilioNotifId;
    private final String destinoDigital;
    private Long loteId;
    private String referenciaExterna;
    private final LocalDateTime fhIntento;
    private LocalDateTime fhResultado;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;
    private LocalDateTime fhUltMod;
    private String idUserUltMod;

    public FalNotificacionIntento(
            Long id,
            Long notificacionId,
            short nroIntento,
            CanalNotificacion canalNotif,
            Long domicilioNotifId,
            String destinoDigital,
            Long loteId,
            String referenciaExterna,
            LocalDateTime fhIntento,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (id == null) throw new IllegalArgumentException("id no puede ser null");
        if (notificacionId == null) throw new IllegalArgumentException("notificacionId no puede ser null");
        if (nroIntento < 1) throw new IllegalArgumentException("nroIntento debe ser >= 1, fue: " + nroIntento);
        if (canalNotif == null) throw new IllegalArgumentException("canalNotif no puede ser null");
        if (fhIntento == null) throw new IllegalArgumentException("fhIntento no puede ser null");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta no puede ser null");
        if (idUserAlta == null || idUserAlta.isBlank()) throw new IllegalArgumentException("idUserAlta no puede ser nulo/blanco");
        if (destinoDigital != null && destinoDigital.length() > 120)
            throw new IllegalArgumentException("destinoDigital excede 120 caracteres");
        if (referenciaExterna != null && referenciaExterna.length() > 80)
            throw new IllegalArgumentException("referenciaExterna excede 80 caracteres");
        this.id = id;
        this.notificacionId = notificacionId;
        this.nroIntento = nroIntento;
        this.canalNotif = canalNotif;
        this.domicilioNotifId = domicilioNotifId;
        this.destinoDigital = destinoDigital != null ? destinoDigital.trim() : null;
        this.loteId = loteId;
        this.referenciaExterna = referenciaExterna;
        this.fhIntento = fhIntento;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
        this.estadoIntento = EstadoNotificacion.EN_PROCESO;
        this.resultadoIntento = null;
    }

    public Long getId() { return id; }
    public Long getNotificacionId() { return notificacionId; }
    public short getNroIntento() { return nroIntento; }
    public CanalNotificacion getCanalNotif() { return canalNotif; }

    public EstadoNotificacion getEstadoIntento() { return estadoIntento; }
    public void setEstadoIntento(EstadoNotificacion estadoIntento) { this.estadoIntento = estadoIntento; }

    public ResultadoNotificacion getResultadoIntento() { return resultadoIntento; }
    public void setResultadoIntento(ResultadoNotificacion resultadoIntento) { this.resultadoIntento = resultadoIntento; }

    public Long getDomicilioNotifId() { return domicilioNotifId; }
    public String getDestinoDigital() { return destinoDigital; }

    public Long getLoteId() { return loteId; }
    public void setLoteId(Long loteId) { this.loteId = loteId; }

    public String getReferenciaExterna() { return referenciaExterna; }
    public void setReferenciaExterna(String referenciaExterna) {
        if (referenciaExterna != null && referenciaExterna.length() > 80)
            throw new IllegalArgumentException("referenciaExterna excede 80 caracteres");
        this.referenciaExterna = referenciaExterna;
    }

    public LocalDateTime getFhIntento() { return fhIntento; }

    public LocalDateTime getFhResultado() { return fhResultado; }
    public void setFhResultado(LocalDateTime fhResultado) { this.fhResultado = fhResultado; }

    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }

    public LocalDateTime getFhUltMod() { return fhUltMod; }
    public void setFhUltMod(LocalDateTime fhUltMod) { this.fhUltMod = fhUltMod; }

    public String getIdUserUltMod() { return idUserUltMod; }
    public void setIdUserUltMod(String idUserUltMod) { this.idUserUltMod = idUserUltMod; }

    public boolean tieneResultado() { return resultadoIntento != null; }

    public boolean esPositivo() {
        return resultadoIntento == ResultadoNotificacion.POSITIVO;
    }

    public boolean esNegativo() {
        return resultadoIntento == ResultadoNotificacion.NEGATIVO;
    }

    public boolean esVencido() {
        return resultadoIntento == ResultadoNotificacion.VENCIDO;
    }

    public boolean esSuperadoPorPortal() {
        return resultadoIntento == ResultadoNotificacion.SUPERADA_POR_PORTAL;
    }

    public FalNotificacionIntento copia() {
        FalNotificacionIntento c = new FalNotificacionIntento(
                id, notificacionId, nroIntento, canalNotif,
                domicilioNotifId, destinoDigital, loteId, referenciaExterna,
                fhIntento, fhAlta, idUserAlta);
        c.estadoIntento = this.estadoIntento;
        c.resultadoIntento = this.resultadoIntento;
        c.fhResultado = this.fhResultado;
        c.fhUltMod = this.fhUltMod;
        c.idUserUltMod = this.idUserUltMod;
        return c;
    }
}
