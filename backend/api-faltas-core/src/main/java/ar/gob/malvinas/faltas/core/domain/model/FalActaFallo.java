package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoFalloActa;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenFirmezaCondena;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFalloActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFalloActa;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fallo dictado sobre un acta de faltas (fal_acta_fallo).
 * Un acta puede tener historial de fallos; solo uno es vigente (siVigente = true).
 * La firmeza de condena vive en este mismo agregado (D1): no existe fal_acta_firmeza_condena.
 */
public class FalActaFallo {

    private final Long id;
    private int versionRow;
    private final Long actaId;
    private Long documentoId;
    private Long valorizacionId;
    private final TipoFalloActa tipoFallo;
    private ResultadoFalloActa resultadoFallo;
    private EstadoFalloActa estadoFallo;
    private BigDecimal montoCondena;
    private String fundamentos;
    private LocalDateTime fhDictado;
    private String idUserDictado;
    private LocalDateTime fhFirma;
    private LocalDateTime fhNotificacion;
    private LocalDate fhVtoApelacion;
    private LocalDateTime fhFirmeza;
    private OrigenFirmezaCondena origenFirmeza;
    private boolean siApelable;
    private boolean siFirme;
    private boolean siVigente;
    private Long falloReemplazadoId;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalActaFallo(
            Long id,
            Long actaId,
            TipoFalloActa tipoFallo,
            LocalDateTime fhDictado,
            LocalDateTime fhAlta,
            String idUserAlta) {
        this.id = id;
        this.actaId = actaId;
        this.tipoFallo = tipoFallo;
        this.fhDictado = fhDictado;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
        this.estadoFallo = EstadoFalloActa.PENDIENTE_FIRMA;
        this.siVigente = true;
        this.siApelable = (tipoFallo == TipoFalloActa.CONDENATORIO);
        this.siFirme = false;
        this.versionRow = 0;
        this.resultadoFallo = (tipoFallo == TipoFalloActa.ABSOLUTORIO)
                ? ResultadoFalloActa.ABSUELVE : ResultadoFalloActa.CONDENA;
    }

    // -----------------------------------------------------------------------
    // Getters y setters
    // -----------------------------------------------------------------------

    public Long getId() { return id; }
    public int getVersionRow() { return versionRow; }
    public void setVersionRow(int versionRow) { this.versionRow = versionRow; }
    public Long getActaId() { return actaId; }
    public Long getDocumentoId() { return documentoId; }
    public void setDocumentoId(Long documentoId) { this.documentoId = documentoId; }
    public Long getValorizacionId() { return valorizacionId; }
    public void setValorizacionId(Long valorizacionId) { this.valorizacionId = valorizacionId; }
    public TipoFalloActa getTipoFallo() { return tipoFallo; }
    public ResultadoFalloActa getResultadoFallo() { return resultadoFallo; }
    public void setResultadoFallo(ResultadoFalloActa resultadoFallo) { this.resultadoFallo = resultadoFallo; }
    public EstadoFalloActa getEstadoFallo() { return estadoFallo; }
    public void setEstadoFallo(EstadoFalloActa estadoFallo) { this.estadoFallo = estadoFallo; }
    public BigDecimal getMontoCondena() { return montoCondena; }
    public void setMontoCondena(BigDecimal montoCondena) { this.montoCondena = montoCondena; }
    public String getFundamentos() { return fundamentos; }
    public void setFundamentos(String fundamentos) { this.fundamentos = fundamentos; }
    public LocalDateTime getFhDictado() { return fhDictado; }
    public void setFhDictado(LocalDateTime fhDictado) { this.fhDictado = fhDictado; }
    public String getIdUserDictado() { return idUserDictado; }
    public void setIdUserDictado(String idUserDictado) { this.idUserDictado = idUserDictado; }
    public LocalDateTime getFhFirma() { return fhFirma; }
    public void setFhFirma(LocalDateTime fhFirma) { this.fhFirma = fhFirma; }
    public LocalDateTime getFhNotificacion() { return fhNotificacion; }
    public void setFhNotificacion(LocalDateTime fhNotificacion) { this.fhNotificacion = fhNotificacion; }
    public LocalDate getFhVtoApelacion() { return fhVtoApelacion; }
    public void setFhVtoApelacion(LocalDate fhVtoApelacion) { this.fhVtoApelacion = fhVtoApelacion; }
    public LocalDateTime getFhFirmeza() { return fhFirmeza; }
    public OrigenFirmezaCondena getOrigenFirmeza() { return origenFirmeza; }
    public boolean isSiApelable() { return siApelable; }
    public void setSiApelable(boolean siApelable) { this.siApelable = siApelable; }
    public boolean isSiFirme() { return siFirme; }
    public boolean isSiVigente() { return siVigente; }
    public void setSiVigente(boolean siVigente) { this.siVigente = siVigente; }
    public Long getFalloReemplazadoId() { return falloReemplazadoId; }
    public void setFalloReemplazadoId(Long falloReemplazadoId) { this.falloReemplazadoId = falloReemplazadoId; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }

    // -----------------------------------------------------------------------
    // Compatibilidad backward (nombres anteriores delegados a los canonicos)
    // -----------------------------------------------------------------------

    /** @deprecated Usar getFhDictado() */
    @Deprecated public LocalDateTime getFechaDictado() { return fhDictado; }
    /** @deprecated Usar getFhNotificacion() */
    @Deprecated public void setFechaNotificacion(java.time.LocalDateTime f) { this.fhNotificacion = f; }
    /** @deprecated Usar getFhNotificacion() */
    @Deprecated public LocalDateTime getFechaNotificacion() { return fhNotificacion; }
    /** @deprecated Usar isSiVigente() */
    @Deprecated public boolean isSiActivo() { return siVigente; }
    /** @deprecated Usar setSiVigente() */
    @Deprecated public void setSiActivo(boolean v) { this.siVigente = v; }

    // -----------------------------------------------------------------------
    // Operaciones de dominio
    // -----------------------------------------------------------------------

    public boolean esAbsolutorio() { return tipoFallo == TipoFalloActa.ABSOLUTORIO; }
    public boolean esCondenatorio() { return tipoFallo == TipoFalloActa.CONDENATORIO; }
    /**
     * Registra la firma del documento de fallo y pasa el estado a PENDIENTE_NOTIFICACION.
     * Debe llamarse cuando se completan todos los requisitos de firma obligatorios.
     */
    public void marcarPendienteNotificacion(LocalDateTime fhFirma) {
        if (fhFirma == null) throw new IllegalArgumentException("fhFirma requerida");
        if (this.estadoFallo != EstadoFalloActa.PENDIENTE_FIRMA) {
            throw new ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException(
                    "marcarPendienteNotificacion requiere estado PENDIENTE_FIRMA. Estado actual: " + this.estadoFallo);
        }
        this.fhFirma = fhFirma;
        this.estadoFallo = EstadoFalloActa.PENDIENTE_NOTIFICACION;
    }

    /**
     * Registra el resultado notificatorio positivo y pasa el estado a NOTIFICADO.
     */
    public void marcarNotificado(LocalDateTime fhNotificacion) {
        if (fhNotificacion == null) throw new IllegalArgumentException("fhNotificacion requerida");
        if (this.estadoFallo != EstadoFalloActa.PENDIENTE_NOTIFICACION) {
            throw new ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException(
                    "marcarNotificado requiere estado PENDIENTE_NOTIFICACION. Estado actual: " + this.estadoFallo);
        }
        if (this.fhFirma == null) {
            throw new ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException(
                    "marcarNotificado requiere fhFirma registrado.");
        }
        this.fhNotificacion = fhNotificacion;
        this.estadoFallo = EstadoFalloActa.NOTIFICADO;
    }

    /**
     * Declara la firmeza de condena en este fallo. Solo aplicable a condenatorios notificados.
     * Actualiza siFirme, fhFirmeza, origenFirmeza y estadoFallo = FIRME.
     */
    public void declararFirmeza(LocalDateTime fhFirmeza, OrigenFirmezaCondena origen) {
        if (fhFirmeza == null) throw new IllegalArgumentException("fhFirmeza requerida");
        if (origen == null) throw new IllegalArgumentException("origenFirmeza requerido");
        this.siFirme = true;
        this.fhFirmeza = fhFirmeza;
        this.origenFirmeza = origen;
        this.estadoFallo = EstadoFalloActa.FIRME;
    }

    /**
     * Marca este fallo como reemplazado (deja de ser vigente, pasa a estado REEMPLAZADO).
     */
    public void reemplazadoPor(Long nuevoFalloId) {
        this.siVigente = false;
        this.estadoFallo = EstadoFalloActa.REEMPLAZADO;
    }

    // -----------------------------------------------------------------------
    // Copia defensiva
    // -----------------------------------------------------------------------

    public FalActaFallo copia() {
        FalActaFallo c = new FalActaFallo(id, actaId, tipoFallo, fhDictado, fhAlta, idUserAlta);
        c.versionRow = this.versionRow;
        c.documentoId = this.documentoId;
        c.valorizacionId = this.valorizacionId;
        c.resultadoFallo = this.resultadoFallo;
        c.estadoFallo = this.estadoFallo;
        c.montoCondena = this.montoCondena;
        c.fundamentos = this.fundamentos;
        c.idUserDictado = this.idUserDictado;
        c.fhFirma = this.fhFirma;
        c.fhNotificacion = this.fhNotificacion;
        c.fhVtoApelacion = this.fhVtoApelacion;
        c.fhFirmeza = this.fhFirmeza;
        c.origenFirmeza = this.origenFirmeza;
        c.siApelable = this.siApelable;
        c.siFirme = this.siFirme;
        c.siVigente = this.siVigente;
        c.falloReemplazadoId = this.falloReemplazadoId;
        return c;
    }
}
