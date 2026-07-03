package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.OrigenFirmezaCondena;

import java.time.LocalDateTime;

/**
 * Registro de firmeza de condena sobre un acta.
 *
 * Una condena firme se origina por dos vias (OrigenFirmezaCondena):
 *   - VENCIMIENTO_PLAZO_APELACION: vencio el plazo sin que el infractor apelara.
 *   - APELACION_RECHAZADA: el infractor apelo y la apelacion fue rechazada.
 *
 * Garantiza trazabilidad de la firmeza mas alla de los eventos PLAVNC/CONFIR.
 * El campo apelacionId es null para el camino de vencimiento de plazo.
 *
 * siActiva: reservado para futura anulacion. Siempre true en Slice 3D.
 */
public class FalActaFirmezaCondena {

    private final String id;
    private final Long actaId;
    private final String falloId;
    private final String apelacionId;
    private final OrigenFirmezaCondena origenFirmeza;
    private final LocalDateTime fechaFirmeza;
    private final String observaciones;
    private boolean siActiva;

    public FalActaFirmezaCondena(
            String id,
            Long actaId,
            String falloId,
            String apelacionId,
            OrigenFirmezaCondena origenFirmeza,
            LocalDateTime fechaFirmeza,
            String observaciones,
            boolean siActiva) {
        this.id = id;
        this.actaId = actaId;
        this.falloId = falloId;
        this.apelacionId = apelacionId;
        this.origenFirmeza = origenFirmeza;
        this.fechaFirmeza = fechaFirmeza;
        this.observaciones = observaciones;
        this.siActiva = siActiva;
    }

    public String getId() { return id; }
    public Long getActaId() { return actaId; }
    public String getFalloId() { return falloId; }
    public String getApelacionId() { return apelacionId; }
    public OrigenFirmezaCondena getOrigenFirmeza() { return origenFirmeza; }
    public LocalDateTime getFechaFirmeza() { return fechaFirmeza; }
    public String getObservaciones() { return observaciones; }
    public boolean isSiActiva() { return siActiva; }
    public void setSiActiva(boolean siActiva) { this.siActiva = siActiva; }
}
