package ar.gob.malvinas.faltas.prototipo.web.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado liviano de búsqueda global de actas con coincidencias explicadas.
 *
 * <p>Incluye los campos mínimos necesarios para que la UI pueda:
 * <ul>
 *   <li>Presentar el número visible del acta.</li>
 *   <li>Navegar a la bandeja correcta.</li>
 *   <li>Mostrar estado operativo básico sin cargar el detalle completo.</li>
 *   <li>Explicar por qué el acta aparece en los resultados ({@code matches}).</li>
 * </ul>
 *
 * @param actaId             identificador interno, p. ej. {@code ACTA-0030}
 * @param acta               número visible, p. ej. {@code A-2026-0030}
 * @param bandeja            código de bandeja actual, p. ej. {@code PENDIENTE_ANALISIS}
 * @param bandejaLabel       etiqueta legible de la bandeja para UI
 * @param dependencia        dependencia demo si existe, o {@code null}
 * @param estadoProceso      estado de proceso actual del acta
 * @param situacionAdministrativa situación administrativa actual
 * @param resultadoFinal     resultado final de cierre, p. ej. {@code SIN_RESULTADO_FINAL}
 * @param situacionPago      situación de pago voluntario
 * @param situacionPagoCondena situación de pago de condena firme
 * @param montoCondena       monto de condena si aplica, o {@code null}
 * @param accionPendiente    marca operativa dentro de la bandeja, o {@code null}
 * @param tipoGestionExterna tipo de gestión externa si aplica, o {@code null}
 * @param cerrable           indica si el acta puede cerrarse en este momento
 * @param score              puntaje de relevancia de la búsqueda (mayor = más relevante)
 * @param scoreLabel         etiqueta legible del score: {@code ALTA}, {@code MEDIA} o {@code BAJA}
 * @param matches            lista de campos que produjeron coincidencia, no vacía
 */
public record PrototipoActaBusquedaResponse(
        String actaId,
        String acta,
        String bandeja,
        String bandejaLabel,
        String dependencia,
        String estadoProceso,
        String situacionAdministrativa,
        String resultadoFinal,
        String situacionPago,
        String situacionPagoCondena,
        BigDecimal montoCondena,
        String accionPendiente,
        String tipoGestionExterna,
        boolean cerrable,
        int score,
        String scoreLabel,
        List<PrototipoActaBusquedaMatchResponse> matches) {
}
