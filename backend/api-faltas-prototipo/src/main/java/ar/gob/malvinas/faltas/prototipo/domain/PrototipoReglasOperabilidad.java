package ar.gob.malvinas.faltas.prototipo.domain;

/**
 * Fachada pública con helpers semánticos de operabilidad de bandejas.
 *
 * <p>Define en un único lugar lógico qué bandejas son suspendidas, externas,
 * terminales o sin acciones internas. Accessible desde {@code web}, {@code
 * bandeja} y {@code store} sin romper el encapsulamiento package-private de
 * {@code PrototipoConstantes}.
 *
 * <p>Regla de no-duplicación: {@code PrototipoConstantes} delega sus helpers de
 * operabilidad en esta clase; no existen dos listas independientes de bandejas
 * prohibidas.
 *
 * <p>Solo helpers internos de backend. No afecta el contrato externo ni las
 * responses al portal infractor.
 */
public final class PrototipoReglasOperabilidad {

    private static final String ARCHIVO = "ARCHIVO";
    private static final String CERRADAS = "CERRADAS";
    private static final String GESTION_EXTERNA = "GESTION_EXTERNA";
    private static final String PARALIZADAS = "PARALIZADAS";

    private PrototipoReglasOperabilidad() {
    }

    /**
     * {@code true} si la bandeja está suspendida o es externa y por ello no
     * admite acciones internas normales: {@code PARALIZADAS} o
     * {@code GESTION_EXTERNA}.
     *
     * <p>Nota portal infractor: {@code PARALIZADAS} se expone como
     * {@code EN_TRAMITE} al infractor; no exponer el texto "acta paralizada".
     */
    public static boolean esBandejaSuspendidaOExterna(String bandeja) {
        return PARALIZADAS.equals(bandeja) || GESTION_EXTERNA.equals(bandeja);
    }

    /**
     * {@code true} si la bandeja es terminal: {@code CERRADAS} o
     * {@code ARCHIVO}. En bandejas terminales no aplican acciones internas
     * ni resolutorios incrementales.
     */
    public static boolean esBandejaTerminal(String bandeja) {
        return CERRADAS.equals(bandeja) || ARCHIVO.equals(bandeja);
    }

    /**
     * {@code true} si la bandeja no admite acciones internas normales: agrupa
     * {@link #esBandejaTerminal(String)} y
     * {@link #esBandejaSuspendidaOExterna(String)}.
     *
     * <p>Equivale a verificar que la bandeja es una de {@code CERRADAS},
     * {@code ARCHIVO}, {@code PARALIZADAS} o {@code GESTION_EXTERNA}.
     *
     * <p>Preferir este helper en lugar de listas locales de bandejas
     * prohibidas para evitar omitir alguna.
     */
    public static boolean esBandejaSinAccionesInternas(String bandeja) {
        return esBandejaTerminal(bandeja) || esBandejaSuspendidaOExterna(bandeja);
    }

    /** {@code true} si la bandeja es exactamente {@code ARCHIVO}. */
    public static boolean esBandejaArchivo(String bandeja) {
        return ARCHIVO.equals(bandeja);
    }

    /** {@code true} si la bandeja es exactamente {@code CERRADAS}. */
    public static boolean esBandejaCerrada(String bandeja) {
        return CERRADAS.equals(bandeja);
    }

    /** {@code true} si la bandeja es exactamente {@code GESTION_EXTERNA}. */
    public static boolean esBandejaGestionExterna(String bandeja) {
        return GESTION_EXTERNA.equals(bandeja);
    }
}
