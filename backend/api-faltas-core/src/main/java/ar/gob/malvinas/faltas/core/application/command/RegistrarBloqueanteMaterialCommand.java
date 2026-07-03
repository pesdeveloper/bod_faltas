package ar.gob.malvinas.faltas.core.application.command;

import ar.gob.malvinas.faltas.core.domain.enums.OrigenBloqueanteMaterial;

public record RegistrarBloqueanteMaterialCommand(
        Long actaId,
        OrigenBloqueanteMaterial origen
) {}
