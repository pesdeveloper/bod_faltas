package ar.gob.malvinas.faltas.core.application.command;

import jakarta.validation.constraints.NotNull;

/**
 * Bloque tipado de una medida preventiva efectivamente aplicada al acta, agregada en Wave 4
 * Parte 3A. N medidas por acta ({@code fal_acta_medida_preventiva}).
 *
 * <p><b>Decision funcional final (Wave 4 Parte 3B §7, DECISION-MEDIDA-PREVENTIVA-LIBRE-01,
 * reemplaza {@code DECISION-ACTA-MEDIDA-PREVENTIVA-01}):</b> el inspector puede elegir
 * cualquier medida preventiva vigente del catálogo. Se valida únicamente existencia y
 * vigencia/actividad de {@code medidaPreventivaId} en {@code fal_medida_preventiva}, y
 * ausencia de duplicados exactos ({@code acta_id + medida_preventiva_id}) dentro de la misma
 * acta. {@code articuloId} se conserva como referencia histórica opcional (nunca obligatoria
 * ni validada contra {@code fal_articulo_medida_preventiva}): el camino canónico del alta ya
 * no la exige ni la usa para resolver la fila del artículo infringido.
 */
public record MedidaPreventivaActaItem(
        Long articuloId,
        @NotNull Long medidaPreventivaId,
        String medPrevTxt,
        boolean siGeneraBloqueante
) {}
