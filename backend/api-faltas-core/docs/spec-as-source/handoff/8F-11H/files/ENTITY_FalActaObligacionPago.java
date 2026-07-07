package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoObligacionPago;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoAptitudIntimacion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoObligacionPago;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Obligacion de pago de un acta de faltas.
 * Mapeada a fal_acta_obligacion_pago en MariaDB.
 *
 * Una sola obligacion vigente por acta en todo momento.
 * El historial se preserva (siVigente=false en registros anteriores).
 * versionRow controla concurrencia optimista.
 *
 * PAGO_VOLUNTARIO: nace de valorizacion confirmada, falloId NULL.
 * CONDENA: exige falloId y valorizacion coherente.
 *
 * Montos escala 2. Caches de mora y aptitud son auxiliares, no fuente primaria.
 */
public class FalActaObligacionPago {

    private final Long id;
    private int versionRow;
    private final Long actaId;
    private final Long personaId;
    private final TipoObligacionPago tipoObligacion;
    private Long valorizacionId;
    private Long falloId;
    private final BigDecimal montoOriginal;
    private EstadoObligacionPago estadoObligacion;
    private final LocalDateTime fhDeterminacion;
    private String idUserDeterminacion;
    private Long formaPagoVigenteId;
    private boolean siAptaIntimacion;
    private LocalDateTime fhAptaIntimacion;
    private MotivoAptitudIntimacion motivoAptaIntimacion;
    private Short cantDiasMora;
    private Short cantCuotasMora;
    private Short cantCuotasMoraConsec;
    private LocalDateTime fhCancelacion;
    private boolean siExcluirEscaneo;
    private LocalDateTime fhUltSyncIngresos;
    private boolean siVigente;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalActaObligacionPago(
            Long id,
            Long actaId,
            Long personaId,
            TipoObligacionPago tipoObligacion,
            BigDecimal montoOriginal,
            LocalDateTime fhDeterminacion,
            String idUserDeterminacion,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (id == null) throw new IllegalArgumentException("id es obligatorio");
        if (actaId == null) throw new IllegalArgumentException("actaId es obligatorio");
        if (personaId == null) throw new IllegalArgumentException("personaId es obligatorio");
        if (tipoObligacion == null) throw new IllegalArgumentException("tipoObligacion es obligatorio");
        if (montoOriginal == null || montoOriginal.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("montoOriginal no puede ser negativo");
        if (fhDeterminacion == null) throw new IllegalArgumentException("fhDeterminacion es obligatoria");
        if (idUserDeterminacion == null || idUserDeterminacion.isBlank())
            throw new IllegalArgumentException("idUserDeterminacion es obligatorio");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta es obligatoria");
        if (idUserAlta == null || idUserAlta.isBlank())
            throw new IllegalArgumentException("idUserAlta es obligatorio");
        this.id = id;
        this.versionRow = 0;
        this.actaId = actaId;
        this.personaId = personaId;
        this.tipoObligacion = tipoObligacion;
        this.montoOriginal = montoOriginal.setScale(2, RoundingMode.HALF_UP);
        this.estadoObligacion = EstadoObligacionPago.DETERMINADA;
        this.fhDeterminacion = fhDeterminacion;
        this.idUserDeterminacion = idUserDeterminacion;
        this.siVigente = true;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public Long getId() { return id; }
    public int getVersionRow() { return versionRow; }
    public void setVersionRow(int v) { this.versionRow = v; }
    public Long getActaId() { return actaId; }
    public Long getPersonaId() { return personaId; }
    public TipoObligacionPago getTipoObligacion() { return tipoObligacion; }
    public Long getValorizacionId() { return valorizacionId; }
    public void setValorizacionId(Long v) { this.valorizacionId = v; }
    public Long getFalloId() { return falloId; }
    public void setFalloId(Long v) { this.falloId = v; }
    public BigDecimal getMontoOriginal() { return montoOriginal; }
    public EstadoObligacionPago getEstadoObligacion() { return estadoObligacion; }
    public void setEstadoObligacion(EstadoObligacionPago v) { this.estadoObligacion = v; }
    public LocalDateTime getFhDeterminacion() { return fhDeterminacion; }
    public String getIdUserDeterminacion() { return idUserDeterminacion; }
    public void setIdUserDeterminacion(String v) { this.idUserDeterminacion = v; }
    public Long getFormaPagoVigenteId() { return formaPagoVigenteId; }
    public void setFormaPagoVigenteId(Long v) { this.formaPagoVigenteId = v; }
    public boolean isSiAptaIntimacion() { return siAptaIntimacion; }
    public void setSiAptaIntimacion(boolean v) { this.siAptaIntimacion = v; }
    public LocalDateTime getFhAptaIntimacion() { return fhAptaIntimacion; }
    public void setFhAptaIntimacion(LocalDateTime v) { this.fhAptaIntimacion = v; }
    public MotivoAptitudIntimacion getMotivoAptaIntimacion() { return motivoAptaIntimacion; }
    public void setMotivoAptaIntimacion(MotivoAptitudIntimacion v) { this.motivoAptaIntimacion = v; }
    public Short getCantDiasMora() { return cantDiasMora; }
    public void setCantDiasMora(Short v) {
        if (v != null && v < 0) throw new IllegalArgumentException("cantDiasMora no puede ser negativo");
        this.cantDiasMora = v;
    }
    public Short getCantCuotasMora() { return cantCuotasMora; }
    public void setCantCuotasMora(Short v) {
        if (v != null && v < 0) throw new IllegalArgumentException("cantCuotasMora no puede ser negativo");
        this.cantCuotasMora = v;
    }
    public Short getCantCuotasMoraConsec() { return cantCuotasMoraConsec; }
    public void setCantCuotasMoraConsec(Short v) {
        if (v != null && v < 0) throw new IllegalArgumentException("cantCuotasMoraConsec no puede ser negativo");
        this.cantCuotasMoraConsec = v;
    }
    public LocalDateTime getFhCancelacion() { return fhCancelacion; }
    public void setFhCancelacion(LocalDateTime v) { this.fhCancelacion = v; }
    public boolean isSiExcluirEscaneo() { return siExcluirEscaneo; }
    public void setSiExcluirEscaneo(boolean v) { this.siExcluirEscaneo = v; }
    public LocalDateTime getFhUltSyncIngresos() { return fhUltSyncIngresos; }
    public void setFhUltSyncIngresos(LocalDateTime v) { this.fhUltSyncIngresos = v; }
    public boolean isSiVigente() { return siVigente; }
    public void setSiVigente(boolean v) { this.siVigente = v; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }

    public boolean estaCancelada() {
        return estadoObligacion == EstadoObligacionPago.CANCELADA;
    }
    public boolean estaAnulada() {
        return estadoObligacion == EstadoObligacionPago.ANULADA;
    }
    public boolean esVoluntaria() {
        return tipoObligacion == TipoObligacionPago.PAGO_VOLUNTARIO;
    }
    public boolean esCondena() {
        return tipoObligacion == TipoObligacionPago.CONDENA;
    }

    public void cancelar(LocalDateTime fhCancelacion) {
        if (fhCancelacion == null) throw new IllegalArgumentException("fhCancelacion es obligatoria");
        if (this.estadoObligacion == EstadoObligacionPago.CANCELADA)
            throw new IllegalStateException("La obligacion ya esta cancelada.");
        if (this.estadoObligacion == EstadoObligacionPago.ANULADA)
            throw new IllegalStateException("No se puede cancelar una obligacion anulada.");
        this.estadoObligacion = EstadoObligacionPago.CANCELADA;
        this.fhCancelacion = fhCancelacion;
    }

    public void anular() {
        if (this.estadoObligacion == EstadoObligacionPago.ANULADA)
            throw new IllegalStateException("La obligacion ya esta anulada.");
        this.estadoObligacion = EstadoObligacionPago.ANULADA;
        this.siVigente = false;
    }

    public FalActaObligacionPago copia() {
        FalActaObligacionPago c = new FalActaObligacionPago(
                id, actaId, personaId, tipoObligacion, montoOriginal,
                fhDeterminacion, idUserDeterminacion, fhAlta, idUserAlta);
        c.versionRow = this.versionRow;
        c.valorizacionId = this.valorizacionId;
        c.falloId = this.falloId;
        c.estadoObligacion = this.estadoObligacion;
        c.formaPagoVigenteId = this.formaPagoVigenteId;
        c.siAptaIntimacion = this.siAptaIntimacion;
        c.fhAptaIntimacion = this.fhAptaIntimacion;
        c.motivoAptaIntimacion = this.motivoAptaIntimacion;
        c.cantDiasMora = this.cantDiasMora;
        c.cantCuotasMora = this.cantCuotasMora;
        c.cantCuotasMoraConsec = this.cantCuotasMoraConsec;
        c.fhCancelacion = this.fhCancelacion;
        c.siExcluirEscaneo = this.siExcluirEscaneo;
        c.fhUltSyncIngresos = this.fhUltSyncIngresos;
        c.siVigente = this.siVigente;
        return c;
    }
}
