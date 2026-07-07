package ar.gob.malvinas.faltas.core.domain.model;

import java.time.LocalDateTime;

/**
 * Relación entre un artículo normativo y una medida preventiva.
 * Mapeada a fal_articulo_medida_preventiva en MariaDB.
 *
 * PK compuesta (articulo_id, medida_preventiva_id). Sin ID sintético.
 * Baja lógica: siActiva=false. No se borra físicamente.
 * Relaciones inactivas no habilitan nuevas aplicaciones.
 */
public class FalArticuloMedidaPreventiva {

    private final ArticuloMedidaPreventivaId id;
    private final boolean siObligatoria;
    private boolean siActiva;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalArticuloMedidaPreventiva(
            ArticuloMedidaPreventivaId id,
            boolean siObligatoria,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (id == null) throw new IllegalArgumentException("id es obligatorio");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta es obligatoria");
        if (idUserAlta == null || idUserAlta.isBlank()) throw new IllegalArgumentException("idUserAlta es obligatorio");
        this.id = id;
        this.siObligatoria = siObligatoria;
        this.siActiva = true;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public ArticuloMedidaPreventivaId getId() { return id; }
    public Long getArticuloId() { return id.articuloId(); }
    public Long getMedidaPreventivaId() { return id.medidaPreventivaId(); }
    public boolean isSiObligatoria() { return siObligatoria; }
    public boolean isSiActiva() { return siActiva; }
    public void setSiActiva(boolean siActiva) { this.siActiva = siActiva; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }

    public FalArticuloMedidaPreventiva copia() {
        FalArticuloMedidaPreventiva c = new FalArticuloMedidaPreventiva(id, siObligatoria, fhAlta, idUserAlta);
        c.siActiva = this.siActiva;
        return c;
    }
}
