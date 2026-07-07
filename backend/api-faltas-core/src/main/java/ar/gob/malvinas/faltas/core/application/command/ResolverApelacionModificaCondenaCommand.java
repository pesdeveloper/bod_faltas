package ar.gob.malvinas.faltas.core.application.command;

import java.math.BigDecimal;

/**
 * Comando para resolver una apelacion con resultado MODIFICA_CONDENA.
 * Crea un fallo sustitutivo con el nuevo monto y enlaza falloReemplazadoId.
 */
public record ResolverApelacionModificaCondenaCommand(
        Long apelacionId,
        BigDecimal nuevoMontoCondena,
        String fundamentosResolucion,
        String idUserResolucion
) {}