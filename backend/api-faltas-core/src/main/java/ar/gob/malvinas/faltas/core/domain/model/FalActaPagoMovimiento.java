package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.ClasificacionPago;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoAnulacionPago;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenConfirmacion;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenMovimiento;
import ar.gob.malvinas.faltas.core.domain.enums.TipoMovimientoPago;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

public class FalActaPagoMovimiento {

    private final Long id;
    private final Long obligacionPagoId;
    private final Long formaPagoId;
    private final Long planPagoRefId;
    private final TipoMovimientoPago tipoMovimiento;
    private final OrigenMovimiento origenMovimiento;
    private final OrigenConfirmacion origenConfirmacion;
    private final Long evidenciaDocumentoId;
    private final ClasificacionPago clasificacionPago;
    private final Short nroCuota;
    private final BigDecimal importeCapital;
    private final BigDecimal importeRima;
    private final BigDecimal importeTotal;
    private final String cmteEM;
    private final Short prefEM;
    private final Integer nroEM;
    private final String cmtePG;
    private final Short prefPG;
    private final Integer nroPG;
    private final Long idCierre;
    private final Long idOpe;
    private final Long movimientoOrigenId;
    private final MotivoAnulacionPago motivoAnulacionPago;
    private final LocalDateTime fhPagoProcesado;
    private final LocalDateTime fhPagoConfirmado;
    private final String referenciaExterna;
    private final LocalDateTime fhMovimiento;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    private FalActaPagoMovimiento(Builder b) {
        this.id = b.id;
        this.obligacionPagoId = b.obligacionPagoId;
        this.formaPagoId = b.formaPagoId;
        this.planPagoRefId = b.planPagoRefId;
        this.tipoMovimiento = b.tipoMovimiento;
        this.origenMovimiento = b.origenMovimiento;
        this.origenConfirmacion = b.origenConfirmacion;
        this.evidenciaDocumentoId = b.evidenciaDocumentoId;
        this.clasificacionPago = b.clasificacionPago;
        this.nroCuota = b.nroCuota;
        this.importeCapital = b.importeCapital;
        this.importeRima = b.importeRima;
        this.importeTotal = b.importeTotal;
        this.cmteEM = b.cmteEM;
        this.prefEM = b.prefEM;
        this.nroEM = b.nroEM;
        this.cmtePG = b.cmtePG;
        this.prefPG = b.prefPG;
        this.nroPG = b.nroPG;
        this.idCierre = b.idCierre;
        this.idOpe = b.idOpe;
        this.movimientoOrigenId = b.movimientoOrigenId;
        this.motivoAnulacionPago = b.motivoAnulacionPago;
        this.fhPagoProcesado = b.fhPagoProcesado;
        this.fhPagoConfirmado = b.fhPagoConfirmado;
        this.referenciaExterna = b.referenciaExterna;
        this.fhMovimiento = b.fhMovimiento;
        this.fhAlta = b.fhAlta;
        this.idUserAlta = b.idUserAlta;
    }

    public Long getId() { return id; }
    public Long getObligacionPagoId() { return obligacionPagoId; }
    public Long getFormaPagoId() { return formaPagoId; }
    public Long getPlanPagoRefId() { return planPagoRefId; }
    public TipoMovimientoPago getTipoMovimiento() { return tipoMovimiento; }
    public OrigenMovimiento getOrigenMovimiento() { return origenMovimiento; }
    public OrigenConfirmacion getOrigenConfirmacion() { return origenConfirmacion; }
    public Long getEvidenciaDocumentoId() { return evidenciaDocumentoId; }
    public ClasificacionPago getClasificacionPago() { return clasificacionPago; }
    public Short getNroCuota() { return nroCuota; }
    public BigDecimal getImporteCapital() { return importeCapital; }
    public BigDecimal getImporteRima() { return importeRima; }
    public BigDecimal getImporteTotal() { return importeTotal; }
    public String getCmteEM() { return cmteEM; }
    public Short getPrefEM() { return prefEM; }
    public Integer getNroEM() { return nroEM; }
    public String getCmtePG() { return cmtePG; }
    public Short getPrefPG() { return prefPG; }
    public Integer getNroPG() { return nroPG; }
    public Long getIdCierre() { return idCierre; }
    public Long getIdOpe() { return idOpe; }
    public Long getMovimientoOrigenId() { return movimientoOrigenId; }
    public MotivoAnulacionPago getMotivoAnulacionPago() { return motivoAnulacionPago; }
    public LocalDateTime getFhPagoProcesado() { return fhPagoProcesado; }
    public LocalDateTime getFhPagoConfirmado() { return fhPagoConfirmado; }
    public String getReferenciaExterna() { return referenciaExterna; }
    public LocalDateTime getFhMovimiento() { return fhMovimiento; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }

    public boolean esAnulacion() {
        return tipoMovimiento == TipoMovimientoPago.PAGO_REVERTIDO
                || tipoMovimiento == TipoMovimientoPago.EMISION_ANULADA;
    }

    public boolean payloadEquivalenteA(FalActaPagoMovimiento otro) {
        return mismoPayloadIdempotente(otro);
    }

    public boolean mismoPayloadIdempotente(FalActaPagoMovimiento otro) {
        if (otro == null) return false;
        return Objects.equals(obligacionPagoId, otro.obligacionPagoId)
                && tipoMovimiento == otro.tipoMovimiento
                && origenMovimiento == otro.origenMovimiento
                && origenConfirmacion == otro.origenConfirmacion
                && Objects.equals(evidenciaDocumentoId, otro.evidenciaDocumentoId)
                && clasificacionPago == otro.clasificacionPago
                && Objects.equals(formaPagoId, otro.formaPagoId)
                && Objects.equals(planPagoRefId, otro.planPagoRefId)
                && Objects.equals(nroCuota, otro.nroCuota)
                && eq(importeCapital, otro.importeCapital)
                && eq(importeRima, otro.importeRima)
                && eq(importeTotal, otro.importeTotal)
                && Objects.equals(cmteEM, otro.cmteEM)
                && Objects.equals(prefEM, otro.prefEM)
                && Objects.equals(nroEM, otro.nroEM)
                && Objects.equals(cmtePG, otro.cmtePG)
                && Objects.equals(prefPG, otro.prefPG)
                && Objects.equals(nroPG, otro.nroPG)
                && Objects.equals(idCierre, otro.idCierre)
                && Objects.equals(idOpe, otro.idOpe)
                && Objects.equals(movimientoOrigenId, otro.movimientoOrigenId)
                && motivoAnulacionPago == otro.motivoAnulacionPago
                && Objects.equals(fhPagoProcesado, otro.fhPagoProcesado)
                && Objects.equals(fhPagoConfirmado, otro.fhPagoConfirmado)
                && Objects.equals(referenciaExterna, otro.referenciaExterna)
                && Objects.equals(fhMovimiento, otro.fhMovimiento);
    }

    private static boolean eq(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.compareTo(b) == 0;
    }

    public FalActaPagoMovimiento copia() {
        return new Builder(id, obligacionPagoId, tipoMovimiento, origenMovimiento, fhMovimiento, fhAlta, idUserAlta)
                .formaPagoId(formaPagoId)
                .planPagoRefId(planPagoRefId)
                .origenConfirmacion(origenConfirmacion)
                .evidenciaDocumentoId(evidenciaDocumentoId)
                .clasificacionPago(clasificacionPago)
                .nroCuota(nroCuota)
                .importes(importeCapital, importeRima, importeTotal)
                .referenciaEM(cmteEM, prefEM, nroEM)
                .referenciaPG(cmtePG, prefPG, nroPG)
                .idCierre(idCierre)
                .idOpe(idOpe)
                .movimientoOrigenId(movimientoOrigenId)
                .motivoAnulacionPago(motivoAnulacionPago)
                .fhPagoProcesado(fhPagoProcesado)
                .fhPagoConfirmado(fhPagoConfirmado)
                .referenciaExterna(referenciaExterna)
                .build();
    }

    public static class Builder {
        private final Long id;
        private final Long obligacionPagoId;
        private final TipoMovimientoPago tipoMovimiento;
        private final OrigenMovimiento origenMovimiento;
        private final LocalDateTime fhMovimiento;
        private final LocalDateTime fhAlta;
        private final String idUserAlta;
        private Long formaPagoId;
        private Long planPagoRefId;
        private OrigenConfirmacion origenConfirmacion;
        private Long evidenciaDocumentoId;
        private ClasificacionPago clasificacionPago = ClasificacionPago.NORMAL;
        private Short nroCuota;
        private BigDecimal importeCapital;
        private BigDecimal importeRima;
        private BigDecimal importeTotal;
        private String cmteEM;
        private Short prefEM;
        private Integer nroEM;
        private String cmtePG;
        private Short prefPG;
        private Integer nroPG;
        private Long idCierre;
        private Long idOpe;
        private Long movimientoOrigenId;
        private MotivoAnulacionPago motivoAnulacionPago;
        private LocalDateTime fhPagoProcesado;
        private LocalDateTime fhPagoConfirmado;
        private String referenciaExterna;

        public Builder(Long id, Long obligacionPagoId, TipoMovimientoPago tipoMovimiento,
                       OrigenMovimiento origenMovimiento, LocalDateTime fhMovimiento,
                       LocalDateTime fhAlta, String idUserAlta) {
            if (id == null) throw new IllegalArgumentException("id es obligatorio");
            if (obligacionPagoId == null) throw new IllegalArgumentException("obligacionPagoId es obligatorio");
            if (tipoMovimiento == null) throw new IllegalArgumentException("tipoMovimiento es obligatorio");
            if (origenMovimiento == null) throw new IllegalArgumentException("origenMovimiento es obligatorio");
            if (fhMovimiento == null) throw new IllegalArgumentException("fhMovimiento es obligatoria");
            if (fhAlta == null) throw new IllegalArgumentException("fhAlta es obligatoria");
            if (idUserAlta == null || idUserAlta.isBlank())
                throw new IllegalArgumentException("idUserAlta es obligatorio");
            this.id = id;
            this.obligacionPagoId = obligacionPagoId;
            this.tipoMovimiento = tipoMovimiento;
            this.origenMovimiento = origenMovimiento;
            this.fhMovimiento = fhMovimiento;
            this.fhAlta = fhAlta;
            this.idUserAlta = idUserAlta;
        }

        public Builder formaPagoId(Long v) { this.formaPagoId = v; return this; }
        public Builder planPagoRefId(Long v) { this.planPagoRefId = v; return this; }
        public Builder origenConfirmacion(OrigenConfirmacion v) { this.origenConfirmacion = v; return this; }
        public Builder evidenciaDocumentoId(Long v) { this.evidenciaDocumentoId = v; return this; }
        public Builder clasificacionPago(ClasificacionPago v) { this.clasificacionPago = v; return this; }
        public Builder nroCuota(Short v) { this.nroCuota = v; return this; }
        public Builder importes(BigDecimal capital, BigDecimal rima, BigDecimal total) {
            if (capital != null && capital.compareTo(BigDecimal.ZERO) < 0)
                throw new IllegalArgumentException("importeCapital no puede ser negativo");
            if (rima != null && rima.compareTo(BigDecimal.ZERO) < 0)
                throw new IllegalArgumentException("importeRima no puede ser negativo");
            if (total != null && total.compareTo(BigDecimal.ZERO) < 0)
                throw new IllegalArgumentException("importeTotal no puede ser negativo");
            if (capital != null && rima != null && total != null) {
                BigDecimal suma = capital.add(rima);
                if (suma.compareTo(total.setScale(2, RoundingMode.HALF_UP)) != 0)
                    throw new IllegalArgumentException(
                            "importeTotal debe ser igual a capital + rima: " + capital + " + " + rima + " != " + total);
            }
            this.importeCapital = (capital == null) ? null : capital.setScale(2, RoundingMode.HALF_UP);
            this.importeRima = (rima == null) ? null : rima.setScale(2, RoundingMode.HALF_UP);
            this.importeTotal = (total == null) ? null : total.setScale(2, RoundingMode.HALF_UP);
            return this;
        }
        public Builder referenciaEM(String cmteEM, Short prefEM, Integer nroEM) {
            if (cmteEM == null && prefEM == null && nroEM == null) return this;
            if (cmteEM == null || prefEM == null || nroEM == null)
                throw new IllegalArgumentException("Triple EM debe ser completo o todo NULL");
            if (cmteEM.trim().length() != 2)
                throw new IllegalArgumentException("cmteEM debe tener exactamente 2 caracteres");
            this.cmteEM = cmteEM.trim(); this.prefEM = prefEM; this.nroEM = nroEM;
            return this;
        }
        public Builder referenciaPG(String cmtePG, Short prefPG, Integer nroPG) {
            if (cmtePG == null && prefPG == null && nroPG == null) return this;
            if (cmtePG == null || prefPG == null || nroPG == null)
                throw new IllegalArgumentException("Triple PG debe ser completo o todo NULL");
            if (cmtePG.trim().length() != 2)
                throw new IllegalArgumentException("cmtePG debe tener exactamente 2 caracteres");
            this.cmtePG = cmtePG.trim(); this.prefPG = prefPG; this.nroPG = nroPG;
            return this;
        }
        public Builder idCierre(Long v) { this.idCierre = v; return this; }
        public Builder idOpe(Long v) { this.idOpe = v; return this; }
        public Builder movimientoOrigenId(Long v) { this.movimientoOrigenId = v; return this; }
        public Builder motivoAnulacionPago(MotivoAnulacionPago v) { this.motivoAnulacionPago = v; return this; }
        public Builder fhPagoProcesado(LocalDateTime v) { this.fhPagoProcesado = v; return this; }
        public Builder fhPagoConfirmado(LocalDateTime v) { this.fhPagoConfirmado = v; return this; }
        public Builder referenciaExterna(String v) {
            if (v != null && v.length() > 80)
                throw new IllegalArgumentException("referenciaExterna max 80 caracteres");
            this.referenciaExterna = v;
            return this;
        }
        public FalActaPagoMovimiento build() { return new FalActaPagoMovimiento(this); }
    }
}
