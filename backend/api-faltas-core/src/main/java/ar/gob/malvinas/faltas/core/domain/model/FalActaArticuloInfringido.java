package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.MotivoBajaArticuloInfringido;

import java.time.LocalDateTime;

/**
 * Imputación de un artículo normativo a un acta de faltas.
 * Mapeada a fal_acta_articulo_infringido en MariaDB.
 *
 * Solo registra qué se imputó. No guarda montos, unidades ni tarifario.
 * Baja lógica preserva la historia. Corrección crea nueva fila.
 */
public class FalActaArticuloInfringido {

    private final Long id;
    private final Long actaId;
    private final Long normativaId;
    private final Long articuloId;
    private boolean siActivo;
    private MotivoBajaArticuloInfringido motivoBaja;
    private LocalDateTime fhBaja;
    private String idUserBaja;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalActaArticuloInfringido(
            Long id,
            Long actaId,
            Long normativaId,
            Long articuloId,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (id == null) throw new IllegalArgumentException("id es obligatorio");
        if (actaId == null) throw new IllegalArgumentException("actaId es obligatorio");
        if (normativaId == null) throw new IllegalArgumentException("normativaId es obligatorio");
        if (articuloId == null) throw new IllegalArgumentException("articuloId es obligatorio");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta es obligatoria");
        if (idUserAlta == null || idUserAlta.isBlank()) throw new IllegalArgumentException("idUserAlta es obligatorio");
        this.id = id;
        this.actaId = actaId;
        this.normativaId = normativaId;
        this.articuloId = articuloId;
        this.siActivo = true;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public Long getId() { return id; }
    public Long getActaId() { return actaId; }
    public Long getNormativaId() { return normativaId; }
    public Long getArticuloId() { return articuloId; }

    public boolean isSiActivo() { return siActivo; }

    public MotivoBajaArticuloInfringido getMotivoBaja() { return motivoBaja; }
    public LocalDateTime getFhBaja() { return fhBaja; }
    public String getIdUserBaja() { return idUserBaja; }

    public void darDeBaja(MotivoBajaArticuloInfringido motivo, LocalDateTime fhBaja, String idUserBaja) {
        if (!siActivo) throw new IllegalStateException("El artículo imputado ya está inactivo: id=" + id);
        if (motivo == null) throw new IllegalArgumentException("motivo es obligatorio para baja");
        if (fhBaja == null) throw new IllegalArgumentException("fhBaja es obligatoria");
        if (idUserBaja == null || idUserBaja.isBlank()) throw new IllegalArgumentException("idUserBaja es obligatorio");
        this.siActivo = false;
        this.motivoBaja = motivo;
        this.fhBaja = fhBaja;
        this.idUserBaja = idUserBaja;
    }

    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }

    public FalActaArticuloInfringido copia() {
        FalActaArticuloInfringido c = new FalActaArticuloInfringido(id, actaId, normativaId, articuloId, fhAlta, idUserAlta);
        c.siActivo = this.siActivo;
        c.motivoBaja = this.motivoBaja;
        c.fhBaja = this.fhBaja;
        c.idUserBaja = this.idUserBaja;
        return c;
    }
}
