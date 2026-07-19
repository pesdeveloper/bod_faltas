package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoFormaPago;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoBajaFormaPago;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFormaPago;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Forma de pago de una obligacion de acta de faltas.
 * Mapeada a fal_acta_forma_pago en MariaDB.
 *
 * nroForma positivo y unico por obligacion.
 * Una forma vigente por obligacion.
 * CONTADO no tiene plan. PLAN_PAGO/REFINANCIACION exige plan.
 * Triple EM y triple PG completos o todos NULL.
 * Procesado no habilita cierre; confirmado puede habilitarlo.
 * El historial se preserva (siVigente=false en reemplazadas).
 */
public class FalActaFormaPago {

    private final Long id;
    private int versionRow;
    private final Long obligacionPagoId;
    private final short nroForma;
    private final TipoFormaPago tipoFormaPago;
    private EstadoFormaPago estadoFormaPago;
    private final BigDecimal montoForma;
    private String cmteEM;
    private Short prefEM;
    private Integer nroEM;
    private String cmtePG;
    private Short prefPG;
    private Integer nroPG;
    private Long formaReemplazadaId;
    private final LocalDateTime fhGeneracion;
    private LocalDateTime fhPagoProcesado;
    private LocalDateTime fhPagoConfirmado;
    private LocalDateTime fhBaja;
    private MotivoBajaFormaPago motivoBaja;
    private boolean siVigente;
    private boolean siExcluirEscaneo;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;
    private LocalDateTime fhVencimiento;

    public FalActaFormaPago(
            Long id,
            Long obligacionPagoId,
            short nroForma,
            TipoFormaPago tipoFormaPago,
            BigDecimal montoForma,
            LocalDateTime fhGeneracion,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (id == null) throw new IllegalArgumentException("id es obligatorio");
        if (obligacionPagoId == null) throw new IllegalArgumentException("obligacionPagoId es obligatorio");
        if (nroForma <= 0) throw new IllegalArgumentException("nroForma debe ser positivo");
        if (tipoFormaPago == null) throw new IllegalArgumentException("tipoFormaPago es obligatorio");
        if (montoForma == null || montoForma.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("montoForma no puede ser negativo");
        if (fhGeneracion == null) throw new IllegalArgumentException("fhGeneracion es obligatoria");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta es obligatoria");
        if (idUserAlta == null || idUserAlta.isBlank())
            throw new IllegalArgumentException("idUserAlta es obligatorio");
        this.id = id;
        this.versionRow = 0;
        this.obligacionPagoId = obligacionPagoId;
        this.nroForma = nroForma;
        this.tipoFormaPago = tipoFormaPago;
        this.estadoFormaPago = EstadoFormaPago.GENERADA;
        this.montoForma = montoForma.setScale(2, RoundingMode.HALF_UP);
        this.fhGeneracion = fhGeneracion;
        this.siVigente = true;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public Long getId() { return id; }
    public int getVersionRow() { return versionRow; }
    public void setVersionRow(int v) { this.versionRow = v; }
    public Long getObligacionPagoId() { return obligacionPagoId; }
    public short getNroForma() { return nroForma; }
    public TipoFormaPago getTipoFormaPago() { return tipoFormaPago; }
    public EstadoFormaPago getEstadoFormaPago() { return estadoFormaPago; }
    public void setEstadoFormaPago(EstadoFormaPago v) { this.estadoFormaPago = v; }
    public BigDecimal getMontoForma() { return montoForma; }

    public String getCmteEM() { return cmteEM; }
    public Short getPrefEM() { return prefEM; }
    public Integer getNroEM() { return nroEM; }
    public String getCmtePG() { return cmtePG; }
    public Short getPrefPG() { return prefPG; }
    public Integer getNroPG() { return nroPG; }

    public void setReferenciaEM(String cmteEM, Short prefEM, Integer nroEM) {
        if (cmteEM == null && prefEM == null && nroEM == null) {
            this.cmteEM = null; this.prefEM = null; this.nroEM = null;
            return;
        }
        if (cmteEM == null || prefEM == null || nroEM == null)
            throw new IllegalArgumentException("Triple EM debe ser completo o todo NULL");
        if (cmteEM.trim().length() != 2)
            throw new IllegalArgumentException("cmteEM debe tener exactamente 2 caracteres");
        if (prefEM < 0) throw new IllegalArgumentException("prefEM no puede ser negativo");
        if (nroEM < 0) throw new IllegalArgumentException("nroEM no puede ser negativo");
        this.cmteEM = cmteEM.trim();
        this.prefEM = prefEM;
        this.nroEM = nroEM;
    }

    public void setReferenciaPG(String cmtePG, Short prefPG, Integer nroPG) {
        if (cmtePG == null && prefPG == null && nroPG == null) {
            this.cmtePG = null; this.prefPG = null; this.nroPG = null;
            return;
        }
        if (cmtePG == null || prefPG == null || nroPG == null)
            throw new IllegalArgumentException("Triple PG debe ser completo o todo NULL");
        if (cmtePG.trim().length() != 2)
            throw new IllegalArgumentException("cmtePG debe tener exactamente 2 caracteres");
        if (prefPG < 0) throw new IllegalArgumentException("prefPG no puede ser negativo");
        if (nroPG < 0) throw new IllegalArgumentException("nroPG no puede ser negativo");
        this.cmtePG = cmtePG.trim();
        this.prefPG = prefPG;
        this.nroPG = nroPG;
    }

    public Long getFormaReemplazadaId() { return formaReemplazadaId; }
    public void setFormaReemplazadaId(Long v) { this.formaReemplazadaId = v; }
    public LocalDateTime getFhGeneracion() { return fhGeneracion; }
    public LocalDateTime getFhPagoProcesado() { return fhPagoProcesado; }
    public void setFhPagoProcesado(LocalDateTime v) { this.fhPagoProcesado = v; }
    public LocalDateTime getFhPagoConfirmado() { return fhPagoConfirmado; }
    public void setFhPagoConfirmado(LocalDateTime v) { this.fhPagoConfirmado = v; }
    public LocalDateTime getFhBaja() { return fhBaja; }
    public void setFhBaja(LocalDateTime v) { this.fhBaja = v; }
    public MotivoBajaFormaPago getMotivoBaja() { return motivoBaja; }
    public void setMotivoBaja(MotivoBajaFormaPago v) { this.motivoBaja = v; }
    public boolean isSiVigente() { return siVigente; }
    public void setSiVigente(boolean v) { this.siVigente = v; }
    public boolean isSiExcluirEscaneo() { return siExcluirEscaneo; }
    public void setSiExcluirEscaneo(boolean v) { this.siExcluirEscaneo = v; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
    public LocalDateTime getFhVencimiento() { return fhVencimiento; }
    public void setFhVencimiento(LocalDateTime v) { this.fhVencimiento = v; }

    public boolean estaPagada() {
        return estadoFormaPago == EstadoFormaPago.PAGADA;
    }
    public boolean esVigenteOPagada() {
        return estadoFormaPago == EstadoFormaPago.VIGENTE
                || estadoFormaPago == EstadoFormaPago.PAGADA;
    }
    public boolean esPlan() {
        return tipoFormaPago == TipoFormaPago.PLAN_PAGO
                || tipoFormaPago == TipoFormaPago.REFINANCIACION;
    }

    public FalActaFormaPago copia() {
        FalActaFormaPago c = new FalActaFormaPago(
                id, obligacionPagoId, nroForma, tipoFormaPago, montoForma,
                fhGeneracion, fhAlta, idUserAlta);
        c.versionRow = this.versionRow;
        c.estadoFormaPago = this.estadoFormaPago;
        c.cmteEM = this.cmteEM;
        c.prefEM = this.prefEM;
        c.nroEM = this.nroEM;
        c.cmtePG = this.cmtePG;
        c.prefPG = this.prefPG;
        c.nroPG = this.nroPG;
        c.formaReemplazadaId = this.formaReemplazadaId;
        c.fhPagoProcesado = this.fhPagoProcesado;
        c.fhPagoConfirmado = this.fhPagoConfirmado;
        c.fhBaja = this.fhBaja;
        c.motivoBaja = this.motivoBaja;
        c.siVigente = this.siVigente;
        c.siExcluirEscaneo = this.siExcluirEscaneo;
        c.fhVencimiento = this.fhVencimiento;
        return c;
    }
}
