package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.AccionPendiente;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.CodigoBandeja;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoProcesalActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;

import java.time.LocalDateTime;

/**
 * ProyecciÃ³n operativa resumida del expediente.
 *
 * Derivado y regenerable. No es fuente de verdad.
 * Se recalcula en cada transiciÃ³n de dominio.
 * Alimenta bandejas, filtros, badges y habilitaciÃ³n de acciones.
 */
public class FalActaSnapshot {

    private final Long idActa;
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
    private boolean bloqueadoNotificacion;

    private TipoEventoActa ultimoEventoTipo;
    private LocalDateTime ultimaActualizacion;

    public FalActaSnapshot(Long idActa) {
        this.idActa = idActa;
        this.ultimaActualizacion = LocalDateTime.now();
    }

    public Long getIdActa() { return idActa; }

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

    public boolean isBloqueadoNotificacion() { return bloqueadoNotificacion; }
    public void setBloqueadoNotificacion(boolean bloqueadoNotificacion) { this.bloqueadoNotificacion = bloqueadoNotificacion; }

    public TipoEventoActa getUltimoEventoTipo() { return ultimoEventoTipo; }
    public void setUltimoEventoTipo(TipoEventoActa ultimoEventoTipo) { this.ultimoEventoTipo = ultimoEventoTipo; }

    public LocalDateTime getUltimaActualizacion() { return ultimaActualizacion; }
    public void setUltimaActualizacion(LocalDateTime ultimaActualizacion) { this.ultimaActualizacion = ultimaActualizacion; }
}

