package ar.gob.malvinas.faltas.prototipo.web.dto;

/**
 * Acciones operativas disponibles en la vista Dirección/Admin para un acta
 * dado su estado administrativo actual. Calculadas en el backend para que el
 * frontend no reifique lógica de negocio. Solo expone el subconjunto de flags
 * necesarios en el slice actual; se extiende por slice.
 *
 * <p>Regla de archivo: en bandeja ARCHIVO con permiteReingreso=true, la única
 * acción interna disponible es {@code archivoReingreso=true}; el resto de
 * acciones operativas queda apagado mientras el acta esté archivada.
 *
 * <p>{@code consentirCondenaYRegistrarPago}: acción combinada de Dirección para
 * cuando el infractor se presenta presencialmente, consiente la condena y
 * registra el pago sin necesidad de esperar el vencimiento del plazo. Solo
 * disponible cuando resultadoFinal=CONDENADO, montoCondena&gt;0,
 * situacionPagoCondena=NO_APLICA, situacionPago=SIN_PAGO y no hay apelación
 * presentada pendiente.
 *
 * <p>{@code pagoVoluntario}: Dirección puede iniciar/fijar pago voluntario para
 * este acta. Falso si ya hay pago en curso, fallo dictado o resultado final
 * incompatible con pago voluntario.
 *
 * <p>{@code vencimientoPagoVoluntario}: Dirección puede registrar que el
 * plazo/oportunidad de pago voluntario venció sin pago. Solo disponible cuando
 * {@code situacionPago=SOLICITADO}, {@code resultadoFinal=SIN_RESULTADO_FINAL}
 * y no hay fallo dictado. Tras ejecutarse, el acta queda con
 * {@code situacionPago=VENCIDO} y {@code falloFondo=true}.
 *
 * <p>{@code falloFondo}: Dirección puede dictar fallo de fondo (absolutorio o
 * condenatorio). Disponible cuando el acta está en bandeja interna operable,
 * {@code resultadoFinal=SIN_RESULTADO_FINAL}, no hay fallo dictado y no hay
 * pago voluntario activo en curso ({@code situacionPago} no es {@code SOLICITADO},
 * {@code PAGO_INFORMADO} ni {@code PENDIENTE_CONFIRMACION}).
 *
 * <p>{@code cumplimientoMaterial}: al menos un pendiente material está listo
 * para ejecutarse (existe habilitante: pago confirmado, absolución o condena
 * firme con pago confirmado). Falso si no hay habilitante aunque existan
 * bloqueantes materiales pendientes.
 *
 * <p>{@code resolucionBloqueante}: puede registrarse el documento resolutorio
 * de al menos un pendiente material (misma condición habilitante que
 * {@code cumplimientoMaterial}).
 *
 * <p>{@code cierre}: el acta puede cerrarse ahora (existe habilitante, sin
 * pendientes materiales ni bloqueantes). Equivale a {@code cerrable} pero
 * expuesto como acción UI explícita.
 *
 * <p>{@code enviarANotificacion}: Dirección puede enviar el acta desde
 * enriquecimiento a la bandeja de notificación. Solo disponible en
 * {@code ACTAS_EN_ENRIQUECIMIENTO} con estadoProceso {@code EN_CURSO} y
 * situaciónAdministrativa {@code ACTIVA}.
 *
 * <p>{@code anularActa}: Dirección puede anular el acta y archivarla por
 * nulidad. Solo disponible en {@code ACTAS_EN_ENRIQUECIMIENTO} activa.
 * El acta queda en ARCHIVO con motivoArchivo=NULIDAD y permiteReingreso=true.
 */
public record AccionesUiResponse(
        boolean archivoReingreso,
        boolean consentirCondenaYRegistrarPago,
        boolean pagoVoluntario,
        boolean vencimientoPagoVoluntario,
        boolean falloFondo,
        boolean cumplimientoMaterial,
        boolean resolucionBloqueante,
        boolean cierre,
        boolean enviarANotificacion,
        boolean anularActa) {}
