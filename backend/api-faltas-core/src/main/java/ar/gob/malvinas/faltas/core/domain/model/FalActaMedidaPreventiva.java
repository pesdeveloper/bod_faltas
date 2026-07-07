package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoMedidaAplicada;

import java.time.LocalDateTime;

/**
 * Medida preventiva efectivamente aplicada al acta.
 * Mapeada a fal_acta_medida_preventiva en MariaDB.
 *
 * Referencia articulo infringido activo de la misma acta.
 * La relacion con bloqueantes es unidireccional (no se guarda bloqueante_id).
 * Si genera bloqueante, el bloqueante apunta a esta medida por su id.
 * Historial se preserva: no se borra fisicamente.
 */
public class FalActaMedidaPreventiva {

    private final Long id;
    private final Long actaId;
    private final Long actaArticuloId;
    private final Long medidaPreventivaId;
    private String medPrevTxt;
    private EstadoMedidaAplicada estadoMedida;
    private final boolean siGeneraBloqueante;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalActaMedidaPreventiva(
            Long id,
            Long actaId,
            Long actaArticuloId,
            Long medidaPreventivaId,
            boolean siGeneraBloqueante,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (id == null) throw new IllegalArgumentException("id es obligatorio");
        if (actaId == null) throw new IllegalArgumentException("actaId es obligatorio");
        if (actaArticuloId == null) throw new IllegalArgumentException("actaArticuloId es obligatorio");
        if (medidaPreventivaId == null) throw new IllegalArgumentException("medidaPreventivaId es obligatorio");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta es obligatoria");
        if (idUserAlta == null || idUserAlta.isBlank()) throw new IllegalArgumentException("idUserAlta es obligatorio");
        this.id = id;
        this.actaId = actaId;
        this.actaArticuloId = actaArticuloId;
        this.medidaPreventivaId = medidaPreventivaId;
        this.siGeneraBloqueante = siGeneraBloqueante;
        this.estadoMedida = EstadoMedidaAplicada.APLICADA;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public Long getId() { return id; }
    public Long getActaId() { return actaId; }
    public Long getActaArticuloId() { return actaArticuloId; }
    public Long getMedidaPreventivaId() { return medidaPreventivaId; }

    public String getMedPrevTxt() { return medPrevTxt; }
    public void setMedPrevTxt(String medPrevTxt) {
        if (medPrevTxt != null && medPrevTxt.length() > 255)
            throw new IllegalArgumentException("medPrevTxt max 255 caracteres");
        this.medPrevTxt = medPrevTxt;
    }

    public EstadoMedidaAplicada getEstadoMedida() { return estadoMedida; }

    public void transicionarEstado(EstadoMedidaAplicada nuevoEstado) {
        if (nuevoEstado == null) throw new IllegalArgumentException("nuevoEstado es obligatorio");
        if (this.estadoMedida == EstadoMedidaAplicada.ANULADA || this.estadoMedida == EstadoMedidaAplicada.CUMPLIDA)
            throw new IllegalStateException("No se puede transicionar desde estado terminal: " + this.estadoMedida);
        this.estadoMedida = nuevoEstado;
    }

    public boolean isSiGeneraBloqueante() { return siGeneraBloqueante; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }

    /** Indica si la medida esta activa (no resuelta). */
    public boolean isActiva() {
        return estadoMedida == EstadoMedidaAplicada.APLICADA;
    }

    public FalActaMedidaPreventiva copia() {
        FalActaMedidaPreventiva c = new FalActaMedidaPreventiva(id, actaId, actaArticuloId, medidaPreventivaId, siGeneraBloqueante, fhAlta, idUserAlta);
        c.medPrevTxt = this.medPrevTxt;
        c.estadoMedida = this.estadoMedida;
        return c;
    }
}
