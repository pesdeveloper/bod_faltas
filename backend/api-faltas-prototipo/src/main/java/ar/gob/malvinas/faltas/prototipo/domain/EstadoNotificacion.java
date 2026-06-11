package ar.gob.malvinas.faltas.prototipo.domain;

public enum EstadoNotificacion {
    PENDIENTE_PREPARACION,
    LISTA_PARA_ENVIO,
    ENVIADA,
    ENTREGADA,
    NEGATIVA,
    VENCIDA,
    /**
     * Notificación que dejó de ser operativa sin haberse concretado
     * (p. ej. superada por una notificación positiva de portal). No debe
     * generar acciones pendientes ni entrar en circuitos de envío.
     */
    SIN_EFECTO
}
