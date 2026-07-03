package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;

import java.time.LocalDateTime;

/**
 * Proceso notificatorio de un documento del expediente.
 *
 * Toda notificacion recae sobre un documento del expediente.
 * Puede tener multiples intentos. El estado refleja el resultado acumulado.
 * El resultado operativo modifica la bandeja y el snapshot del acta.
 */
public class FalNotificacion {

    private final String id;
    private final Long idActa;
    private final Long idDocumento;
    private final TipoDocu tipoDocumentoNotificado;
    private final String canal;
    private final LocalDateTime fechaEnvio;
    private EstadoNotificacion estado;
    private ResultadoNotificacion resultado;
    private LocalDateTime fechaResultado;
    private int intentos;
    private String observaciones;

    public FalNotificacion(
            String id,
            Long idActa,
            Long idDocumento,
            TipoDocu tipoDocumentoNotificado,
            String canal,
            LocalDateTime fechaEnvio) {
        this.id = id;
        this.idActa = idActa;
        this.idDocumento = idDocumento;
        this.tipoDocumentoNotificado = tipoDocumentoNotificado;
        this.canal = canal;
        this.fechaEnvio = fechaEnvio;
        this.estado = EstadoNotificacion.EN_PROCESO;
        this.intentos = 1;
    }

    public String getId() { return id; }
    public Long getIdActa() { return idActa; }
    public Long getIdDocumento() { return idDocumento; }
    public TipoDocu getTipoDocumentoNotificado() { return tipoDocumentoNotificado; }
    public String getCanal() { return canal; }
    public LocalDateTime getFechaEnvio() { return fechaEnvio; }

    public EstadoNotificacion getEstado() { return estado; }
    public void setEstado(EstadoNotificacion estado) { this.estado = estado; }

    public ResultadoNotificacion getResultado() { return resultado; }
    public void setResultado(ResultadoNotificacion resultado) { this.resultado = resultado; }

    public LocalDateTime getFechaResultado() { return fechaResultado; }
    public void setFechaResultado(LocalDateTime fechaResultado) { this.fechaResultado = fechaResultado; }

    public int getIntentos() { return intentos; }
    public void incrementarIntento() { this.intentos++; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public boolean tieneResultadoPositivo() {
        return resultado == ResultadoNotificacion.POSITIVO;
    }

    public boolean estaEnProceso() {
        return estado == EstadoNotificacion.EN_PROCESO
                || estado == EstadoNotificacion.PENDIENTE_ENVIO;
    }
}
