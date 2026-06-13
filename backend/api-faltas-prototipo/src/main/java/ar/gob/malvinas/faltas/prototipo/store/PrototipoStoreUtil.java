package ar.gob.malvinas.faltas.prototipo.store;

final class PrototipoStoreUtil {

    private PrototipoStoreUtil() {
    }

    /**
     * Extrae el sufijo numérico de un ID de acta.
     * "ACTA-2025-001" → "2025-001"; cualquier otro valor → se retorna tal cual.
     */
    static String sufijoActa(String actaId) {
        return actaId.startsWith("ACTA-") ? actaId.substring("ACTA-".length()) : actaId;
    }

    /**
     * Devuelve {@code primero} si no es null ni blank; de lo contrario devuelve {@code segundo}.
     */
    static String primerNoVacio(String primero, String segundo) {
        return primero != null && !primero.isBlank() ? primero : segundo;
    }
}
