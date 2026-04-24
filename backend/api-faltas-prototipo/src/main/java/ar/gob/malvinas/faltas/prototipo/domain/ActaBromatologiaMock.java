package ar.gob.malvinas.faltas.prototipo.domain;

/**
 * Satélite mock mínimo para faltas de bromatología. El decomiso alimenticias
 * no se modela como medida preventiva de cierre; es dato de expediente
 * (mock).
 */
public record ActaBromatologiaMock(boolean decomisoSustanciasAlimenticias) {
}
