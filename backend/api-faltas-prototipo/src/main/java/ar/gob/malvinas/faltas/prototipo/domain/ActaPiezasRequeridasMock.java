package ar.gob.malvinas.faltas.prototipo.domain;

import java.util.List;

/**
 * Soporte demo para actas que requieren producir una o más piezas no-fallo
 * (por ejemplo: NOTIFICACION_ACTA, MEDIDA_PREVENTIVA, RESOLUCION, NULIDAD,
 * RECTIFICACION) antes de poder salir de una bandeja operativa.
 * <p>
 * Estructura intencionalmente simple y explícita:
 * <ul>
 *   <li>{@code piezasRequeridas}: catálogo estático de piezas que la acta debe producir.</li>
 *   <li>{@code piezasGeneradas}: piezas ya producidas; subconjunto esperado de las requeridas.</li>
 * </ul>
 * Mientras falte al menos una pieza requerida por producir, la acta debe
 * permanecer en su bandeja operativa actual.
 */
public record ActaPiezasRequeridasMock(
        String actaId,
        List<String> piezasRequeridas,
        List<String> piezasGeneradas) {
}
