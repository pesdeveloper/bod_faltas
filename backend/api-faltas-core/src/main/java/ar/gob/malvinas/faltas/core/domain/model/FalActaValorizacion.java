package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoValorizacionActa;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Cabecera unica por acta con el total economico actual.
 * Mapeada a {@code fal_acta_valorizacion} en MariaDB.
 *
 * Rediseño PROMPT-NOCTURNO-WAVE-04-PARTE-2 S7.8: reemplaza el modelo anterior
 * de multiples filas por acta+tipo con bandera de vigencia (nunca persistido
 * via JDBC — incompatibilidad documentada en CHECKPOINT.md de Wave 4 §3) por
 * una cabecera unica y mutable con OCC. El detalle por articulo vive en
 * {@link FalActaValorizacionItem}.
 *
 * El alta calcula esta cabecera pero NUNCA la confirma ({@code siConfirmada=false},
 * {@code estadoValorizacion=PRELIMINAR}): valorizacion no equivale a obligacion de
 * pago. La confirmacion es una operacion posterior fuera del alcance del alta.
 */
public class FalActaValorizacion {

    private final Long id;
    private int versionRow;
    private final Long actaId;
    private BigDecimal montoTotalOrdinario;
    private BigDecimal montoTotalPagoVoluntario;
    private EstadoValorizacionActa estadoValorizacion;
    private boolean siConfirmada;
    private LocalDateTime fhCalculo;
    private LocalDateTime fhConfirmacion;
    private String idUserConfirmacion;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalActaValorizacion(
            Long id,
            Long actaId,
            BigDecimal montoTotalOrdinario,
            BigDecimal montoTotalPagoVoluntario,
            LocalDateTime fhCalculo,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (id == null) throw new IllegalArgumentException("id es obligatorio");
        if (actaId == null) throw new IllegalArgumentException("actaId es obligatorio");
        if (fhCalculo == null) throw new IllegalArgumentException("fhCalculo es obligatoria");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta es obligatoria");
        if (idUserAlta == null || idUserAlta.isBlank()) throw new IllegalArgumentException("idUserAlta es obligatorio");
        this.id = id;
        this.versionRow = 0;
        this.actaId = actaId;
        this.montoTotalOrdinario = montoTotalOrdinario == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : montoTotalOrdinario.setScale(2, RoundingMode.HALF_UP);
        this.montoTotalPagoVoluntario = montoTotalPagoVoluntario == null ? null
                : montoTotalPagoVoluntario.setScale(2, RoundingMode.HALF_UP);
        this.estadoValorizacion = EstadoValorizacionActa.PRELIMINAR;
        this.siConfirmada = false;
        this.fhCalculo = fhCalculo;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public Long getId() { return id; }
    public int getVersionRow() { return versionRow; }
    public void setVersionRow(int v) { this.versionRow = v; }
    public Long getActaId() { return actaId; }

    public BigDecimal getMontoTotalOrdinario() { return montoTotalOrdinario; }
    public BigDecimal getMontoTotalPagoVoluntario() { return montoTotalPagoVoluntario; }

    public EstadoValorizacionActa getEstadoValorizacion() { return estadoValorizacion; }
    public boolean isSiConfirmada() { return siConfirmada; }
    public LocalDateTime getFhCalculo() { return fhCalculo; }
    public LocalDateTime getFhConfirmacion() { return fhConfirmacion; }
    public String getIdUserConfirmacion() { return idUserConfirmacion; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }

    /** Recalcula los totales (p. ej. tras agregar/quitar un articulo). No confirma. */
    public void recalcular(BigDecimal montoTotalOrdinario, BigDecimal montoTotalPagoVoluntario, LocalDateTime fhCalculo) {
        if (siConfirmada)
            throw new IllegalStateException("No se puede recalcular una valorizacion confirmada. id=" + id);
        this.montoTotalOrdinario = montoTotalOrdinario == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : montoTotalOrdinario.setScale(2, RoundingMode.HALF_UP);
        this.montoTotalPagoVoluntario = montoTotalPagoVoluntario == null ? null
                : montoTotalPagoVoluntario.setScale(2, RoundingMode.HALF_UP);
        this.fhCalculo = fhCalculo;
    }

    /** PRELIMINAR -> CONFIRMADA. Fuera del alcance del alta (prevista para ENRI, Wave 5). */
    public void confirmar(LocalDateTime fhConfirmacion, String idUserConfirmacion) {
        if (estadoValorizacion != EstadoValorizacionActa.PRELIMINAR)
            throw new IllegalStateException("Solo se puede confirmar desde PRELIMINAR. Estado actual: " + estadoValorizacion);
        if (fhConfirmacion == null) throw new IllegalArgumentException("fhConfirmacion es obligatoria");
        if (idUserConfirmacion == null || idUserConfirmacion.isBlank())
            throw new IllegalArgumentException("idUserConfirmacion es obligatorio");
        this.estadoValorizacion = EstadoValorizacionActa.CONFIRMADA;
        this.siConfirmada = true;
        this.fhConfirmacion = fhConfirmacion;
        this.idUserConfirmacion = idUserConfirmacion;
    }

    public FalActaValorizacion copia() {
        FalActaValorizacion c = new FalActaValorizacion(
                id, actaId, montoTotalOrdinario, montoTotalPagoVoluntario, fhCalculo, fhAlta, idUserAlta);
        c.versionRow = this.versionRow;
        c.estadoValorizacion = this.estadoValorizacion;
        c.siConfirmada = this.siConfirmada;
        c.fhConfirmacion = this.fhConfirmacion;
        c.idUserConfirmacion = this.idUserConfirmacion;
        return c;
    }
}
