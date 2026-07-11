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
 *
 * Ciclo de cola notificatoria:
 *   preparar() -> PENDIENTE_ENVIO -> iniciarEnvio() -> EN_PROCESO -> resultado
 */
public class FalNotificacion {

    private final Long id;
    private final Long idActa;
    private final Long idDocumento;
    private final TipoDocu tipoDocumentoNotificado;
    private String canal;
    private LocalDateTime fechaEnvio;
    private EstadoNotificacion estado;
    private ResultadoNotificacion resultado;
    private LocalDateTime fechaResultado;
    private int intentos;
    private String observaciones;
    private LocalDateTime fhUltMod;
    private String idUserUltMod;
    private LocalDateTime fhAlta;
    private String idUserAlta;

    // -------------------------------------------------------------------------
    // Constructores de envio directo (mantener compatibilidad)
    // -------------------------------------------------------------------------

    public FalNotificacion(
            Long id,
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

    public FalNotificacion(
            Long id,
            Long idActa,
            Long idDocumento,
            TipoDocu tipoDocumentoNotificado,
            String canal,
            LocalDateTime fechaEnvio,
            LocalDateTime fhAlta,
            String idUserAlta) {
        this(id, idActa, idDocumento, tipoDocumentoNotificado, canal, fechaEnvio);
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    // -------------------------------------------------------------------------
    // Factory para cola notificatoria (PENDIENTE_ENVIO)
    // -------------------------------------------------------------------------

    /**
     * Prepara una notificacion en estado PENDIENTE_ENVIO lista para ser procesada
     * por un lote de correo o por envio directo.
     *
     * Estado inicial: PENDIENTE_ENVIO, resultado null, intentos 0, canal null, fechaEnvio null.
     * No emite NOTENV. No es un envio real.
     */
    public static FalNotificacion preparar(
            Long id,
            Long idActa,
            Long idDocumento,
            TipoDocu tipoDocumentoNotificado,
            LocalDateTime fhAlta,
            String idUserAlta) {
        if (id == null) throw new IllegalArgumentException("id requerido");
        if (idActa == null) throw new IllegalArgumentException("idActa requerido");
        if (idDocumento == null) throw new IllegalArgumentException("idDocumento requerido");
        if (tipoDocumentoNotificado == null) throw new IllegalArgumentException("tipoDocumentoNotificado requerido");
        FalNotificacion n = new FalNotificacion(id, idActa, idDocumento, tipoDocumentoNotificado, null, null);
        n.estado = EstadoNotificacion.PENDIENTE_ENVIO;
        n.intentos = 0;
        n.fhAlta = fhAlta;
        n.idUserAlta = idUserAlta;
        return n;
    }

    // -------------------------------------------------------------------------
    // Operaciones de dominio
    // -------------------------------------------------------------------------

    /**
     * Inicia el envio real de una notificacion preparada (PENDIENTE_ENVIO -> EN_PROCESO).
     * Completa canal, fecha de envio, incrementa intentos y registra la modificacion.
     */
    public void iniciarEnvio(String canal, LocalDateTime fechaEnvio, LocalDateTime fhUltMod, String actor) {
        if (canal == null || canal.isBlank()) throw new IllegalArgumentException("canal requerido");
        if (fechaEnvio == null) throw new IllegalArgumentException("fechaEnvio requerida");
        if (this.estado != EstadoNotificacion.PENDIENTE_ENVIO) {
            throw new ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException(
                    "iniciarEnvio requiere estado PENDIENTE_ENVIO. Estado actual: " + this.estado);
        }
        this.canal = canal;
        this.fechaEnvio = fechaEnvio;
        this.estado = EstadoNotificacion.EN_PROCESO;
        this.intentos++;
        this.fhUltMod = fhUltMod;
        this.idUserUltMod = actor;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public Long getId() { return id; }
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

    public LocalDateTime getFhUltMod() { return fhUltMod; }
    public void setFhUltMod(LocalDateTime fhUltMod) { this.fhUltMod = fhUltMod; }

    public String getIdUserUltMod() { return idUserUltMod; }
    public void setIdUserUltMod(String idUserUltMod) { this.idUserUltMod = idUserUltMod; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }

    public boolean tieneResultadoPositivo() {
        return resultado == ResultadoNotificacion.POSITIVO;
    }

    public boolean estaEnProceso() {
        return estado == EstadoNotificacion.EN_PROCESO
                || estado == EstadoNotificacion.PENDIENTE_ENVIO;
    }

    public boolean estaActiva() {
        return estado != EstadoNotificacion.SIN_EFECTO;
    }
}
