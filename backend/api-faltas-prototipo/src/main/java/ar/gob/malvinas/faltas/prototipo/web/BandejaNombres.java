package ar.gob.malvinas.faltas.prototipo.web;

import java.util.Map;

/**
 * Etiquetas demo para códigos de bandeja conocidos del escenario mock.
 */
final class BandejaNombres {

    private static final Map<String, String> POR_CODIGO = Map.ofEntries(
            Map.entry("ACTAS_EN_ENRIQUECIMIENTO", "Actas en enriquecimiento"),
            Map.entry("PENDIENTE_PREPARACION_DOCUMENTAL", "Pendiente preparación documental"),
            Map.entry("PENDIENTE_FIRMA", "Pendiente firma"),
            Map.entry("PENDIENTE_NOTIFICACION", "Pendiente notificación"),
            Map.entry("EN_NOTIFICACION", "En notificación"),
            Map.entry("PENDIENTE_ANALISIS", "Pendiente análisis"),
            Map.entry("ARCHIVO", "Archivo"),
            Map.entry("CERRADAS", "Cerradas"));

    private BandejaNombres() {
    }

    static String nombre(String codigo) {
        return POR_CODIGO.getOrDefault(codigo, codigo);
    }
}
