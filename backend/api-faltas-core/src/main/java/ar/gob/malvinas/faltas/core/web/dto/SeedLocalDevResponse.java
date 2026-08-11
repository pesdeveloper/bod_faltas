package ar.gob.malvinas.faltas.core.web.dto;

import java.util.List;

/**
 * Respuesta de {@code POST /demo/dev/seed-talonario-electronico} (Wave 4 Parte 3C §11).
 *
 * <p>Solo para uso en local/dev, con {@code app.persistence.mode=jdbc}. No es un contrato
 * productivo: es la forma de verificar por HTTP el resultado del seeder sin necesitar acceso
 * directo a la base.
 */
public record SeedLocalDevResponse(
        Long idDependencia,
        Long idInspector,
        Long idTalonario,
        List<String> pasos
) {}
