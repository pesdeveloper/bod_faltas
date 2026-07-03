package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Resultado final del circuito del acta, una vez concluido.
 *
 * SIN_RESULTADO_FINAL es el valor inicial: toda acta comienza sin resultado.
 * Los demas valores solo se asignan cuando el acta llega a un cierre con
 * resultado juridico claro (CERR o ARCH con causa determinada).
 *
 * No mezcla forma de pago, estado de obligacion ni situacion administrativa.
 * El resultado refleja la disposicion juridica final del expediente.
 *
 * ABSUELTO: fallo absolutorio notificado, acta cerrada (sin bloqueantes).
 * CONDENA_FIRME: condena firme declarada por vencimiento de plazo (PLAVNC+CONFIR) o apelacion rechazada (CONFIR).
 * CONDENA_FIRME_PAGADA: condena firme pagada y confirmada (PCOCNF+CIERRA). Slice 5.
 */
public enum ResultadoFinalActa {

    SIN_RESULTADO_FINAL,

    PAGO_VOLUNTARIO_CONFIRMADO,

    ABSUELTO,

    CONDENA_FIRME,

    CONDENA_FIRME_PAGADA,

    FALLO_CONDENATORIO_PAGADO,

    FALLO_CONDENATORIO_GESTION_EXTERNA,

    PRESCRIPTO,

    ANULADO
}
