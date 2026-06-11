package ar.gob.malvinas.faltas.prototipo.domain;

public enum ResultadoNotificacion {
    SIN_RESULTADO,
    POSITIVA,
    NEGATIVA,
    VENCIDA,
    /**
     * La pieza fue notificada positivamente por otro canal (portal
     * infractor); esta notificación alternativa queda formalmente sin
     * efecto. No es un fallo de entrega: se conserva solo como traza.
     */
    SUPERADA_POR_PORTAL
}
