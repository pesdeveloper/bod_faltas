package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.TipoPruebaAlcoholemia;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoCualitativoAlcoholemia;
import ar.gob.malvinas.faltas.core.domain.enums.UnidadMedidaAlcoholemia;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Medicion de alcoholemia vinculada a un acta de transito.
 * Mapeada a fal_acta_transito_alcoholemia en MariaDB.
 *
 * N mediciones por acta. orden_medicion positivo y unico por acta.
 * Si hay resultado_numerico, unidad_medida es obligatoria.
 * id_alcoholimetro y ver_alcoholimetro deben informarse juntos.
 * Solo una medicion final por acta (si_resultado_final).
 */
public class FalActaTransitoAlcoholemia {

    private final Long id;
    private final Long actaId;
    private final short ordenMedicion;
    private final TipoPruebaAlcoholemia tipoPrueba;
    private ResultadoCualitativoAlcoholemia resultadoCualitativo;
    private BigDecimal resultadoNumerico;
    private UnidadMedidaAlcoholemia unidadMedida;
    private Long idAlcoholimetro;
    private Short verAlcoholimetro;
    private boolean siResultadoFinal;
    private LocalDateTime fhMedicion;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalActaTransitoAlcoholemia(
            Long id,
            Long actaId,
            short ordenMedicion,
            TipoPruebaAlcoholemia tipoPrueba,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (id == null) throw new IllegalArgumentException("id es obligatorio");
        if (actaId == null) throw new IllegalArgumentException("actaId es obligatorio");
        if (ordenMedicion <= 0) throw new IllegalArgumentException("ordenMedicion debe ser positivo");
        if (tipoPrueba == null) throw new IllegalArgumentException("tipoPrueba es obligatorio");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta es obligatoria");
        if (idUserAlta == null || idUserAlta.isBlank()) throw new IllegalArgumentException("idUserAlta es obligatorio");
        this.id = id;
        this.actaId = actaId;
        this.ordenMedicion = ordenMedicion;
        this.tipoPrueba = tipoPrueba;
        this.siResultadoFinal = false;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public Long getId() { return id; }
    public Long getActaId() { return actaId; }
    public short getOrdenMedicion() { return ordenMedicion; }
    public TipoPruebaAlcoholemia getTipoPrueba() { return tipoPrueba; }

    public ResultadoCualitativoAlcoholemia getResultadoCualitativo() { return resultadoCualitativo; }
    public void setResultadoCualitativo(ResultadoCualitativoAlcoholemia resultadoCualitativo) {
        this.resultadoCualitativo = resultadoCualitativo;
    }

    public BigDecimal getResultadoNumerico() { return resultadoNumerico; }
    public UnidadMedidaAlcoholemia getUnidadMedida() { return unidadMedida; }

    public void setResultadoNumerico(BigDecimal resultadoNumerico, UnidadMedidaAlcoholemia unidadMedida) {
        if (resultadoNumerico != null && unidadMedida == null)
            throw new IllegalArgumentException("unidadMedida es obligatoria cuando se informa resultadoNumerico");
        if (resultadoNumerico != null && resultadoNumerico.scale() > 2)
            throw new IllegalArgumentException("resultadoNumerico: escala maxima 2 decimales");
        this.resultadoNumerico = resultadoNumerico;
        this.unidadMedida = (resultadoNumerico != null) ? unidadMedida : null;
    }

    public Long getIdAlcoholimetro() { return idAlcoholimetro; }
    public Short getVerAlcoholimetro() { return verAlcoholimetro; }

    public void setAlcoholimetro(Long idAlcoholimetro, Short verAlcoholimetro) {
        if ((idAlcoholimetro == null) != (verAlcoholimetro == null))
            throw new IllegalArgumentException("idAlcoholimetro y verAlcoholimetro deben informarse juntos");
        this.idAlcoholimetro = idAlcoholimetro;
        this.verAlcoholimetro = verAlcoholimetro;
    }

    public boolean isSiResultadoFinal() { return siResultadoFinal; }
    public void setSiResultadoFinal(boolean siResultadoFinal) { this.siResultadoFinal = siResultadoFinal; }

    public LocalDateTime getFhMedicion() { return fhMedicion; }
    public void setFhMedicion(LocalDateTime fhMedicion) { this.fhMedicion = fhMedicion; }

    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }

    public FalActaTransitoAlcoholemia copia() {
        FalActaTransitoAlcoholemia c = new FalActaTransitoAlcoholemia(id, actaId, ordenMedicion, tipoPrueba, fhAlta, idUserAlta);
        c.resultadoCualitativo = this.resultadoCualitativo;
        c.resultadoNumerico = this.resultadoNumerico;
        c.unidadMedida = this.unidadMedida;
        c.idAlcoholimetro = this.idAlcoholimetro;
        c.verAlcoholimetro = this.verAlcoholimetro;
        c.siResultadoFinal = this.siResultadoFinal;
        c.fhMedicion = this.fhMedicion;
        return c;
    }
}
