package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.CriterioTarifario;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoValorizacion;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenValorizacion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoUnidadFaltas;
import ar.gob.malvinas.faltas.core.domain.enums.TipoValorizacionActa;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Decision economica global de un acta en un momento dado.
 * Mapeada a fal_acta_valorizacion en MariaDB.
 *
 * versionRow controla concurrencia optimista en la fila.
 * Solo una vigente (CONFIRMADA) por acta+tipo en todo momento.
 * No se borra ni se reactiva: historial inmutable.
 * Una preliminar nunca desplaza la confirmada vigente.
 *
 * Semantica de fila: una nueva decision/calculo economico genera nueva fila PRELIMINAR;
 * la confirmacion solo cambia el estado de la misma fila bajo optimistic locking
 * via confirmarVigenteAtomico en el repository.
 */
public class FalActaValorizacion {

    private final Long id;
    private int versionRow;
    private final Long actaId;
    private EstadoValorizacion estadoValorizacion;
    private final TipoValorizacionActa tipoValorizacionActa;
    private final OrigenValorizacion origenValorizacion;
    private CriterioTarifario criterioTarifario;
    private boolean tarifarioActualizado;
    private BigDecimal montoBaseArticulos;
    private BigDecimal montoFinal;
    private TipoUnidadFaltas tipoUnidadFinal;
    private BigDecimal cantidadUnidadesFinal;
    private BigDecimal valorUnidadFinal;
    private Long tarifarioUnidadId;
    private boolean siSobrescribeTotal;
    private boolean siCongelaValor;
    private LocalDateTime fhCongelamiento;
    private Long falloId;
    private Long documentoId;
    private LocalDateTime fhValorizacion;
    private String idUserValorizacion;
    private LocalDateTime fhConfirmacion;
    private String idUserConfirmacion;
    private boolean siVigente;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalActaValorizacion(
            Long id,
            Long actaId,
            TipoValorizacionActa tipoValorizacionActa,
            OrigenValorizacion origenValorizacion,
            CriterioTarifario criterioTarifario,
            BigDecimal montoFinal,
            LocalDateTime fhValorizacion,
            String idUserValorizacion,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (id == null) throw new IllegalArgumentException("id es obligatorio");
        if (actaId == null) throw new IllegalArgumentException("actaId es obligatorio");
        if (tipoValorizacionActa == null) throw new IllegalArgumentException("tipoValorizacionActa es obligatorio");
        if (origenValorizacion == null) throw new IllegalArgumentException("origenValorizacion es obligatorio");
        if (criterioTarifario == null) throw new IllegalArgumentException("criterioTarifario es obligatorio");
        if (montoFinal == null || montoFinal.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("montoFinal no puede ser negativo");
        if (fhValorizacion == null) throw new IllegalArgumentException("fhValorizacion es obligatoria");
        if (idUserValorizacion == null || idUserValorizacion.isBlank())
            throw new IllegalArgumentException("idUserValorizacion es obligatorio");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta es obligatoria");
        if (idUserAlta == null || idUserAlta.isBlank()) throw new IllegalArgumentException("idUserAlta es obligatorio");
        this.id = id;
        this.versionRow = 0;
        this.actaId = actaId;
        this.estadoValorizacion = EstadoValorizacion.PRELIMINAR;
        this.tipoValorizacionActa = tipoValorizacionActa;
        this.origenValorizacion = origenValorizacion;
        this.criterioTarifario = criterioTarifario;
        this.montoFinal = montoFinal.setScale(2, RoundingMode.HALF_UP);
        this.fhValorizacion = fhValorizacion;
        this.idUserValorizacion = idUserValorizacion;
        this.siVigente = false;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    // ---- Getters administrativos ----

    public Long getId() { return id; }
    public int getVersionRow() { return versionRow; }
    public void setVersionRow(int versionRow) { this.versionRow = versionRow; }
    public Long getActaId() { return actaId; }
    public EstadoValorizacion getEstadoValorizacion() { return estadoValorizacion; }
    public void setEstadoValorizacion(EstadoValorizacion v) { this.estadoValorizacion = v; }
    public TipoValorizacionActa getTipoValorizacionActa() { return tipoValorizacionActa; }
    public OrigenValorizacion getOrigenValorizacion() { return origenValorizacion; }

    // ---- Guard de mutabilidad economica ----

    private void verificarMutable() {
        if (estadoValorizacion != EstadoValorizacion.PRELIMINAR)
            throw new IllegalStateException(
                    "No se puede modificar el contenido economico de una valorizacion " + estadoValorizacion);
    }

    // ---- Getters/setters de campos economicos (guardados por verificarMutable) ----

    public CriterioTarifario getCriterioTarifario() { return criterioTarifario; }
    public void setCriterioTarifario(CriterioTarifario v) { verificarMutable(); this.criterioTarifario = v; }

    public boolean isTarifarioActualizado() { return tarifarioActualizado; }
    public void setTarifarioActualizado(boolean v) { verificarMutable(); this.tarifarioActualizado = v; }

    public BigDecimal getMontoBaseArticulos() { return montoBaseArticulos; }
    public void setMontoBaseArticulos(BigDecimal v) {
        verificarMutable();
        this.montoBaseArticulos = (v == null) ? null : v.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getMontoFinal() { return montoFinal; }
    public void setMontoFinal(BigDecimal v) {
        verificarMutable();
        if (v == null || v.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("montoFinal no puede ser negativo");
        this.montoFinal = v.setScale(2, RoundingMode.HALF_UP);
    }

    public TipoUnidadFaltas getTipoUnidadFinal() { return tipoUnidadFinal; }
    public void setTipoUnidadFinal(TipoUnidadFaltas v) { verificarMutable(); this.tipoUnidadFinal = v; }

    public BigDecimal getCantidadUnidadesFinal() { return cantidadUnidadesFinal; }
    public void setCantidadUnidadesFinal(BigDecimal v) {
        verificarMutable();
        this.cantidadUnidadesFinal = (v == null) ? null : v.setScale(4, RoundingMode.HALF_UP);
    }

    public BigDecimal getValorUnidadFinal() { return valorUnidadFinal; }
    public void setValorUnidadFinal(BigDecimal v) {
        verificarMutable();
        this.valorUnidadFinal = (v == null) ? null : v.setScale(2, RoundingMode.HALF_UP);
    }

    public Long getTarifarioUnidadId() { return tarifarioUnidadId; }
    public void setTarifarioUnidadId(Long v) { verificarMutable(); this.tarifarioUnidadId = v; }

    public boolean isSiSobrescribeTotal() { return siSobrescribeTotal; }
    public void setSiSobrescribeTotal(boolean v) { verificarMutable(); this.siSobrescribeTotal = v; }

    public boolean isSiCongelaValor() { return siCongelaValor; }
    public void setSiCongelaValor(boolean v) {
        verificarMutable();
        this.siCongelaValor = v;
        if (!v) this.fhCongelamiento = null;
    }

    public LocalDateTime getFhCongelamiento() { return fhCongelamiento; }
    public void setFhCongelamiento(LocalDateTime v) {
        verificarMutable();
        if (v != null && !siCongelaValor)
            throw new IllegalArgumentException("fhCongelamiento requiere siCongelaValor=true");
        this.fhCongelamiento = v;
    }

    public Long getFalloId() { return falloId; }
    public void setFalloId(Long v) { verificarMutable(); this.falloId = v; }

    public Long getDocumentoId() { return documentoId; }
    public void setDocumentoId(Long v) { verificarMutable(); this.documentoId = v; }

    public LocalDateTime getFhValorizacion() { return fhValorizacion; }
    public void setFhValorizacion(LocalDateTime v) { verificarMutable(); this.fhValorizacion = v; }
    public String getIdUserValorizacion() { return idUserValorizacion; }
    public void setIdUserValorizacion(String v) { verificarMutable(); this.idUserValorizacion = v; }

    // ---- Getters/setters de campos de transicion de estado ----

    public LocalDateTime getFhConfirmacion() { return fhConfirmacion; }
    public void setFhConfirmacion(LocalDateTime v) { this.fhConfirmacion = v; }
    public String getIdUserConfirmacion() { return idUserConfirmacion; }
    public void setIdUserConfirmacion(String v) { this.idUserConfirmacion = v; }

    public boolean isSiVigente() { return siVigente; }
    public void setSiVigente(boolean v) { this.siVigente = v; }

    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }

    // ---- Helpers de estado ----

    public boolean estaConfirmada() { return estadoValorizacion == EstadoValorizacion.CONFIRMADA; }
    public boolean estaVigenteYConfirmada() { return siVigente && estaConfirmada(); }

    // ---- Metodos de dominio para transiciones de estado ----

    /**
     * PRELIMINAR -> CONFIRMADA.
     * La confirmacion cambia el estado de la misma fila; una nueva decision genera nueva fila PRELIMINAR.
     */
    public void marcarConfirmada(LocalDateTime fhConfirmacion, String idUserConfirmacion) {
        if (estadoValorizacion != EstadoValorizacion.PRELIMINAR)
            throw new IllegalStateException(
                    "Solo se puede confirmar desde PRELIMINAR. Estado actual: " + estadoValorizacion);
        if (fhConfirmacion == null) throw new IllegalArgumentException("fhConfirmacion es obligatoria");
        if (idUserConfirmacion == null || idUserConfirmacion.isBlank())
            throw new IllegalArgumentException("idUserConfirmacion es obligatorio");
        this.estadoValorizacion = EstadoValorizacion.CONFIRMADA;
        this.siVigente = true;
        this.fhConfirmacion = fhConfirmacion;
        this.idUserConfirmacion = idUserConfirmacion;
    }

    /**
     * CONFIRMADA -> REEMPLAZADA. Ocurre al confirmar nueva candidata para el mismo acta+tipo.
     */
    public void marcarReemplazada() {
        if (estadoValorizacion != EstadoValorizacion.CONFIRMADA)
            throw new IllegalStateException(
                    "Solo se puede reemplazar desde CONFIRMADA. Estado actual: " + estadoValorizacion);
        this.estadoValorizacion = EstadoValorizacion.REEMPLAZADA;
        this.siVigente = false;
    }

    /**
     * Anulacion. Valida desde PRELIMINAR o CONFIRMADA. No reversible.
     */
    public void marcarAnulada() {
        if (estadoValorizacion == EstadoValorizacion.ANULADA)
            throw new IllegalStateException("La valorizacion id=" + id + " ya esta anulada.");
        this.estadoValorizacion = EstadoValorizacion.ANULADA;
        this.siVigente = false;
    }

    // ---- copia() usa asignacion directa de campos para bypassear guards ----

    public FalActaValorizacion copia() {
        FalActaValorizacion c = new FalActaValorizacion(
                id, actaId, tipoValorizacionActa, origenValorizacion, criterioTarifario,
                montoFinal, fhValorizacion, idUserValorizacion, fhAlta, idUserAlta);
        c.versionRow = this.versionRow;
        c.estadoValorizacion = this.estadoValorizacion;
        c.tarifarioActualizado = this.tarifarioActualizado;
        c.montoBaseArticulos = this.montoBaseArticulos;
        c.tipoUnidadFinal = this.tipoUnidadFinal;
        c.cantidadUnidadesFinal = this.cantidadUnidadesFinal;
        c.valorUnidadFinal = this.valorUnidadFinal;
        c.tarifarioUnidadId = this.tarifarioUnidadId;
        c.siSobrescribeTotal = this.siSobrescribeTotal;
        c.siCongelaValor = this.siCongelaValor;
        c.fhCongelamiento = this.fhCongelamiento;
        c.falloId = this.falloId;
        c.documentoId = this.documentoId;
        c.fhConfirmacion = this.fhConfirmacion;
        c.idUserConfirmacion = this.idUserConfirmacion;
        c.siVigente = this.siVigente;
        return c;
    }
}
