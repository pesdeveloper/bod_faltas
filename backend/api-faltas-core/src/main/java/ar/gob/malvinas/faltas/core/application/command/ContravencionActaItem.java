package ar.gob.malvinas.faltas.core.application.command;

import ar.gob.malvinas.faltas.core.domain.enums.AmbitoCtv;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoNomenclaturaManual;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenNomenclatura;
import jakarta.validation.constraints.NotNull;

/**
 * Bloque tipado de nomenclatura catastral y rubro del acta de contravención/comercio,
 * agregado en Wave 4 Parte 3A. 1:1 con {@code fal_acta_contravencion}. Solo aplicable
 * cuando {@code tipoActa} es {@code CONTRAVENCION} o {@code COMERCIO}.
 */
public record ContravencionActaItem(
        @NotNull OrigenNomenclatura origenNomencl,
        boolean siNomenclaturaManual,
        MotivoNomenclaturaManual motivoNomenclaturaManual,
        Integer idSujI,
        Integer idBieI,
        Integer idSujC,
        Integer idBieC,
        Short circ,
        String secc,
        String frac,
        String mza,
        String parc,
        String ufun,
        String ucomp,
        Long rubroId,
        Integer idRub,
        AmbitoCtv ambitoCtv,
        String ambitoCtvTxt
) {}
