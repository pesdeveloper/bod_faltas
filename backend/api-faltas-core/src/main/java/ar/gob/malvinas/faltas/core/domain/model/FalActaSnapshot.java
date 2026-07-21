package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.AccionPendiente;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.CodigoBandeja;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoProcesalActa;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoValorizacion;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoParalizacion;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoValorizacionActa;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Proyeccion operativa resumida del expediente.
 * Derivado y regenerable. No es fuente de verdad.
 * Se recalcula en cada transicion de dominio.
 * Alimenta bandejas, filtros, badges y habilitacion de acciones.
 *
 * Este snapshot NO transporta economia de pagos (DECISION_DDL-SNAP-02).
 * Las lecturas economicas salen de FalActaEconomiaProyeccion.
 * SnapshotRecalculador.proyectarPagos es no-op.
 * monto_operativo_vigente permanece como valorizacion UX del acta,
 * no como dato economico de pagos.
 *
 * versionRow controla concurrencia optimista (DECISION_DDL-SNAP-01).
 */
public class FalActaSnapshot {

    private final Long idActa;
    private int versionRow;
    private BloqueActual bloqueActual;
    private EstadoProcesalActa estadoProcesal;
    private SituacionAdministrativaActa situacionAdministrativa;
    private ResultadoFinalActa resultadoFinal;
    private CodigoBandeja codBandeja;
    private String subBandeja;
    private AccionPendiente accionPendiente;

    private boolean tieneDocumentos;
    private boolean tieneDocsPendientesFirma;
    private boolean tieneDocsListosParaNotificar;
    private boolean tieneNotificaciones;
    private boolean notificacionEnCurso;
    private boolean bloqueadoCierre;
    private Long idDocuUlt;
    private boolean bloqueadoNotificacion;

    private Long valorizacionOperativaId;
    private EstadoValorizacion estadoValorizacionOperativa;
    private TipoValorizacionActa tipoValorizacionOperativa;
    private BigDecimal montoOperativoVigente;
    private boolean siMontoConfirmado;

    private TipoEventoActa ultimoEventoTipo;
    private LocalDateTime ultimaActualizacion;

    // Campos de satelites - 8F-11E
    private String licenciaProvinciaTxt;
    private String licenciaUnidadTxt;
    private String nomenclaturaResumen;
    private Integer idBieI;
    private Integer idBieC;

    // Campos de paralizacion y archivo - 8F-11G
    private MotivoParalizacion motivoParalizacionAct;

    public FalActaSnapshot(Long idActa) {
        this.idActa = idActa;
        this.versionRow = 0;
        this.ultimaActualizacion = null;
    }

    public Long getIdActa() { return idActa; }

    public int getVersionRow() { return versionRow; }
    public void setVersionRow(int versionRow) { this.versionRow = versionRow; }

    public BloqueActual getBloqueActual() { return bloqueActual; }
    public void setBloqueActual(BloqueActual bloqueActual) { this.bloqueActual = bloqueActual; }

    public EstadoProcesalActa getEstadoProcesal() { return estadoProcesal; }
    public void setEstadoProcesal(EstadoProcesalActa estadoProcesal) { this.estadoProcesal = estadoProcesal; }

    public SituacionAdministrativaActa getSituacionAdministrativa() { return situacionAdministrativa; }
    public void setSituacionAdministrativa(SituacionAdministrativaActa situacionAdministrativa) {
        this.situacionAdministrativa = situacionAdministrativa;
    }

    public ResultadoFinalActa getResultadoFinal() { return resultadoFinal; }
    public void setResultadoFinal(ResultadoFinalActa resultadoFinal) { this.resultadoFinal = resultadoFinal; }

    public CodigoBandeja getCodBandeja() { return codBandeja; }
    public void setCodBandeja(CodigoBandeja codBandeja) { this.codBandeja = codBandeja; }

    public String getSubBandeja() { return subBandeja; }
    public void setSubBandeja(String subBandeja) { this.subBandeja = subBandeja; }

    public AccionPendiente getAccionPendiente() { return accionPendiente; }
    public void setAccionPendiente(AccionPendiente accionPendiente) { this.accionPendiente = accionPendiente; }

    public boolean isTieneDocumentos() { return tieneDocumentos; }
    public void setTieneDocumentos(boolean tieneDocumentos) { this.tieneDocumentos = tieneDocumentos; }

    public boolean isTieneDocsPendientesFirma() { return tieneDocsPendientesFirma; }
    public void setTieneDocsPendientesFirma(boolean tieneDocsPendientesFirma) {
        this.tieneDocsPendientesFirma = tieneDocsPendientesFirma;
    }

    public boolean isTieneDocsListosParaNotificar() { return tieneDocsListosParaNotificar; }
    public void setTieneDocsListosParaNotificar(boolean tieneDocsListosParaNotificar) {
        this.tieneDocsListosParaNotificar = tieneDocsListosParaNotificar;
    }

    public boolean isTieneNotificaciones() { return tieneNotificaciones; }
    public void setTieneNotificaciones(boolean tieneNotificaciones) { this.tieneNotificaciones = tieneNotificaciones; }

    public boolean isNotificacionEnCurso() { return notificacionEnCurso; }
    public void setNotificacionEnCurso(boolean notificacionEnCurso) { this.notificacionEnCurso = notificacionEnCurso; }

    public boolean isBloqueadoCierre() { return bloqueadoCierre; }
    public void setBloqueadoCierre(boolean bloqueadoCierre) { this.bloqueadoCierre = bloqueadoCierre; }

    public Long getIdDocuUlt() { return idDocuUlt; }
    public void setIdDocuUlt(Long idDocuUlt) { this.idDocuUlt = idDocuUlt; }

    public boolean isBloqueadoNotificacion() { return bloqueadoNotificacion; }
    public void setBloqueadoNotificacion(boolean bloqueadoNotificacion) { this.bloqueadoNotificacion = bloqueadoNotificacion; }

    public TipoEventoActa getUltimoEventoTipo() { return ultimoEventoTipo; }
    public void setUltimoEventoTipo(TipoEventoActa ultimoEventoTipo) { this.ultimoEventoTipo = ultimoEventoTipo; }

    public LocalDateTime getUltimaActualizacion() { return ultimaActualizacion; }
    public void setUltimaActualizacion(LocalDateTime ultimaActualizacion) { this.ultimaActualizacion = ultimaActualizacion; }

    public Long getValorizacionOperativaId() { return valorizacionOperativaId; }
    public void setValorizacionOperativaId(Long valorizacionOperativaId) { this.valorizacionOperativaId = valorizacionOperativaId; }

    public EstadoValorizacion getEstadoValorizacionOperativa() { return estadoValorizacionOperativa; }
    public void setEstadoValorizacionOperativa(EstadoValorizacion estadoValorizacionOperativa) { this.estadoValorizacionOperativa = estadoValorizacionOperativa; }

    public TipoValorizacionActa getTipoValorizacionOperativa() { return tipoValorizacionOperativa; }
    public void setTipoValorizacionOperativa(TipoValorizacionActa tipoValorizacionOperativa) { this.tipoValorizacionOperativa = tipoValorizacionOperativa; }

    public BigDecimal getMontoOperativoVigente() { return montoOperativoVigente; }
    public void setMontoOperativoVigente(BigDecimal montoOperativoVigente) { this.montoOperativoVigente = montoOperativoVigente; }

    public boolean isSiMontoConfirmado() { return siMontoConfirmado; }
    public void setSiMontoConfirmado(boolean siMontoConfirmado) { this.siMontoConfirmado = siMontoConfirmado; }

    public String getLicenciaProvinciaTxt() { return licenciaProvinciaTxt; }
    public void setLicenciaProvinciaTxt(String licenciaProvinciaTxt) { this.licenciaProvinciaTxt = licenciaProvinciaTxt; }

    public String getLicenciaUnidadTxt() { return licenciaUnidadTxt; }
    public void setLicenciaUnidadTxt(String licenciaUnidadTxt) { this.licenciaUnidadTxt = licenciaUnidadTxt; }

    public String getNomenclaturaResumen() { return nomenclaturaResumen; }
    public void setNomenclaturaResumen(String nomenclaturaResumen) { this.nomenclaturaResumen = nomenclaturaResumen; }

    public Integer getIdBieI() { return idBieI; }
    public void setIdBieI(Integer idBieI) {
        if (idBieI != null && (idBieI < 1 || idBieI > 9_999_999))
            throw new IllegalArgumentException(
                    "idBieI debe estar entre 1 y 9.999.999; valor: " + idBieI);
        this.idBieI = idBieI;
    }

    public Integer getIdBieC() { return idBieC; }
    public void setIdBieC(Integer idBieC) {
        if (idBieC != null && (idBieC < 1 || idBieC > 9_999_999))
            throw new IllegalArgumentException(
                    "idBieC debe estar entre 1 y 9.999.999; valor: " + idBieC);
        this.idBieC = idBieC;
    }

    public MotivoParalizacion getMotivoParalizacionAct() { return motivoParalizacionAct; }
    public void setMotivoParalizacionAct(MotivoParalizacion motivoParalizacionAct) { this.motivoParalizacionAct = motivoParalizacionAct; }

    public FalActaSnapshot copia() {
        FalActaSnapshot c = new FalActaSnapshot(idActa);
        c.versionRow = this.versionRow;
        c.bloqueActual = this.bloqueActual;
        c.estadoProcesal = this.estadoProcesal;
        c.situacionAdministrativa = this.situacionAdministrativa;
        c.resultadoFinal = this.resultadoFinal;
        c.codBandeja = this.codBandeja;
        c.subBandeja = this.subBandeja;
        c.accionPendiente = this.accionPendiente;
        c.tieneDocumentos = this.tieneDocumentos;
        c.tieneDocsPendientesFirma = this.tieneDocsPendientesFirma;
        c.tieneDocsListosParaNotificar = this.tieneDocsListosParaNotificar;
        c.tieneNotificaciones = this.tieneNotificaciones;
        c.notificacionEnCurso = this.notificacionEnCurso;
        c.bloqueadoCierre = this.bloqueadoCierre;
        c.idDocuUlt = this.idDocuUlt;
        c.bloqueadoNotificacion = this.bloqueadoNotificacion;
        c.valorizacionOperativaId = this.valorizacionOperativaId;
        c.estadoValorizacionOperativa = this.estadoValorizacionOperativa;
        c.tipoValorizacionOperativa = this.tipoValorizacionOperativa;
        c.montoOperativoVigente = this.montoOperativoVigente;
        c.siMontoConfirmado = this.siMontoConfirmado;
        c.ultimoEventoTipo = this.ultimoEventoTipo;
        c.ultimaActualizacion = this.ultimaActualizacion;
        c.licenciaProvinciaTxt = this.licenciaProvinciaTxt;
        c.licenciaUnidadTxt = this.licenciaUnidadTxt;
        c.nomenclaturaResumen = this.nomenclaturaResumen;
        c.idBieI = this.idBieI;
        c.idBieC = this.idBieC;
        c.motivoParalizacionAct = this.motivoParalizacionAct;
        return c;
    }
}
