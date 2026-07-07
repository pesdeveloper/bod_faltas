package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoAcuse;
import ar.gob.malvinas.faltas.core.domain.enums.TipoAcuse;

import java.time.LocalDateTime;

/**
 * Evidencia/constancia documental de una notificacion.
 * El acuse documenta/valida; no reemplaza el resultado operativo del intento.
 * No almacena binarios: el archivo vive en storage mediante storageKey.
 */
public class FalNotificacionAcuse {

    private final Long id;
    private final Long notificacionId;
    private final Long intentoId;
    private final TipoAcuse tipoAcuse;
    private EstadoAcuse estadoAcuse;
    private String storageKey;
    private LocalDateTime fhAcuse;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalNotificacionAcuse(
            Long id,
            Long notificacionId,
            Long intentoId,
            TipoAcuse tipoAcuse,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (id == null) throw new IllegalArgumentException("id no puede ser null");
        if (notificacionId == null) throw new IllegalArgumentException("notificacionId no puede ser null");
        if (tipoAcuse == null) throw new IllegalArgumentException("tipoAcuse no puede ser null");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta no puede ser null");
        if (idUserAlta == null || idUserAlta.isBlank()) throw new IllegalArgumentException("idUserAlta no puede ser nulo/blanco");
        this.id = id;
        this.notificacionId = notificacionId;
        this.intentoId = intentoId;
        this.tipoAcuse = tipoAcuse;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
        this.estadoAcuse = EstadoAcuse.PENDIENTE;
    }

    public Long getId() { return id; }
    public Long getNotificacionId() { return notificacionId; }
    public Long getIntentoId() { return intentoId; }
    public TipoAcuse getTipoAcuse() { return tipoAcuse; }

    public EstadoAcuse getEstadoAcuse() { return estadoAcuse; }
    public void setEstadoAcuse(EstadoAcuse estadoAcuse) { this.estadoAcuse = estadoAcuse; }

    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) {
        if (storageKey != null && storageKey.length() > 255)
            throw new IllegalArgumentException("storageKey excede 255 caracteres");
        this.storageKey = storageKey;
    }

    public LocalDateTime getFhAcuse() { return fhAcuse; }
    public void setFhAcuse(LocalDateTime fhAcuse) { this.fhAcuse = fhAcuse; }

    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }

    public boolean estaValidado() { return estadoAcuse == EstadoAcuse.VALIDADO; }
    public boolean estaAnulado() { return estadoAcuse == EstadoAcuse.ANULADO; }
    public boolean estaActivo() { return estadoAcuse != EstadoAcuse.ANULADO; }

    public FalNotificacionAcuse copia() {
        FalNotificacionAcuse c = new FalNotificacionAcuse(id, notificacionId, intentoId, tipoAcuse, fhAlta, idUserAlta);
        c.estadoAcuse = this.estadoAcuse;
        c.storageKey = this.storageKey;
        c.fhAcuse = this.fhAcuse;
        return c;
    }
}
