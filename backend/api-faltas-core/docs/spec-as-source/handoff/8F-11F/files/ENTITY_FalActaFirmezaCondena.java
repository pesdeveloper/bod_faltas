package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.OrigenFirmezaCondena;

import java.time.LocalDateTime;

/**
 * Vista/DTO de firmeza de condena construida desde los campos de FalActaFallo.
 * No es una entidad persistible; no tiene repository propio (D1 aprobado en Slice 8F-10).
 * La firmeza vive en fal_acta_fallo (siFirme, fhFirmeza, origenFirmeza).
 */
public class FalActaFirmezaCondena {

    private final Long id;
    private final Long actaId;
    private final Long falloId;
    private final Long apelacionId;
    private final OrigenFirmezaCondena origenFirmeza;
    private final LocalDateTime fechaFirmeza;
    private final String observaciones;
    private final boolean siActiva;

    public FalActaFirmezaCondena(
            Long id,
            Long actaId,
            Long falloId,
            Long apelacionId,
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

    /** Construye el DTO a partir del fallo vigente firme. */
    public static FalActaFirmezaCondena desdesFallo(FalActaFallo fallo, Long apelacionId) {
        return new FalActaFirmezaCondena(
                fallo.getId(),
                fallo.getActaId(),
                fallo.getId(),
                apelacionId,
                fallo.getOrigenFirmeza(),
                fallo.getFhFirmeza(),
                null,
                fallo.isSiFirme());
    }

    public Long getId() { return id; }
    public Long getActaId() { return actaId; }
    public Long getFalloId() { return falloId; }
    public Long getApelacionId() { return apelacionId; }
    public OrigenFirmezaCondena getOrigenFirmeza() { return origenFirmeza; }
    public LocalDateTime getFechaFirmeza() { return fechaFirmeza; }
    public String getObservaciones() { return observaciones; }
    public boolean isSiActiva() { return siActiva; }
}