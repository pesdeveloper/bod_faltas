package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;
import ar.gob.malvinas.faltas.prototipo.store.PrototipoStore;
import ar.gob.malvinas.faltas.prototipo.web.dto.PrototipoActaBusquedaResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Búsqueda global liviana de actas sobre el dataset demo en memoria.
 *
 * <p>Criterios de matching (prioridad descendente):
 * <ol>
 *   <li>Exact match case-insensitive sobre {@code actaId} o {@code numeroActa} raw.</li>
 *   <li>Exact match sobre versión normalizada (uppercase, sin guiones ni espacios).</li>
 *   <li>Q puramente numérico y dígitos de {@code actaId} son exactamente iguales.</li>
 *   <li>Versión normalizada de {@code actaId} o {@code numeroActa} empieza con {@code q} normalizado.</li>
 *   <li>Q puramente numérico y los dígitos de {@code actaId} contienen los dígitos de {@code q}.</li>
 *   <li>Versión normalizada de {@code actaId} o {@code numeroActa} contiene {@code q} normalizado.</li>
 *   <li>Match sobre {@code infractorNombre} o {@code infractorDocumento} (subcadena).</li>
 * </ol>
 *
 * <p>Ordenamiento: score ascendente (mejor primero), luego {@code numeroActa} ascendente.
 * Resultado limitado a {@link #MAX_RESULTADOS}.
 *
 * <p>No muta estado del store; segura para llamadas concurrentes de lectura.
 */
@Component
class ActaBusquedaHelper {

    static final int MAX_RESULTADOS = 50;

    private final PrototipoStore store;

    ActaBusquedaHelper(PrototipoStore store) {
        this.store = store;
    }

    /**
     * Ejecuta la búsqueda global para la query {@code q}.
     *
     * @param q texto libre de búsqueda; si es null o blank devuelve lista vacía
     * @return lista de resultados livianos, vacía si no hay matches
     */
    List<PrototipoActaBusquedaResponse> buscar(String q) {
        if (q == null || q.isBlank()) {
            return List.of();
        }

        String qTrim = q.trim();
        String qUpper = qTrim.toUpperCase();
        String qNorm = normalizar(qTrim);
        String qDigits = extraerDigitos(qTrim);
        boolean qEsPuramenteNumerico = !qNorm.isEmpty() && qNorm.matches("[0-9]+");

        if (qNorm.isEmpty()) {
            return List.of();
        }

        List<EntradaMatch> matches = new ArrayList<>();

        for (ActaMock acta : store.getActas().values()) {
            int score = calcularScore(acta, qUpper, qNorm, qDigits, qEsPuramenteNumerico);
            if (score > 0) {
                matches.add(new EntradaMatch(acta, score));
            }
        }

        return matches.stream()
                .sorted(Comparator.comparingInt(EntradaMatch::score)
                        .thenComparing(e -> e.acta().numeroActa()))
                .limit(MAX_RESULTADOS)
                .map(this::toResponse)
                .toList();
    }

    private int calcularScore(
            ActaMock acta,
            String qUpper,
            String qNorm,
            String qDigits,
            boolean qEsPuramenteNumerico) {

        String actaIdUpper = acta.id().toUpperCase();
        String numeroActaUpper = acta.numeroActa().toUpperCase();

        // Score 1: exact match case-insensitive (raw)
        if (actaIdUpper.equals(qUpper) || numeroActaUpper.equals(qUpper)) {
            return 1;
        }

        // Score 2: exact match normalizado (sin guiones/espacios)
        String actaIdNorm = normalizar(acta.id());
        String numeroActaNorm = normalizar(acta.numeroActa());
        if (actaIdNorm.equals(qNorm) || numeroActaNorm.equals(qNorm)) {
            return 2;
        }

        // Score 3: q puramente numérico y dígitos de actaId son exactamente iguales
        if (qEsPuramenteNumerico) {
            String actaDigits = extraerDigitos(acta.id());
            if (actaDigits.equals(qDigits)) {
                return 3;
            }
        }

        // Score 4: startsWith normalizado
        if (actaIdNorm.startsWith(qNorm) || numeroActaNorm.startsWith(qNorm)) {
            return 4;
        }

        // Score 5: q puramente numérico y los dígitos de actaId contienen los dígitos de q
        if (qEsPuramenteNumerico && !qDigits.isEmpty()) {
            String actaDigits = extraerDigitos(acta.id());
            if (actaDigits.contains(qDigits)) {
                return 5;
            }
        }

        // Score 6: contains normalizado (general)
        if (actaIdNorm.contains(qNorm) || numeroActaNorm.contains(qNorm)) {
            return 6;
        }

        // Score 7: match sobre infractorNombre o infractorDocumento
        // Incluye búsqueda por nombre/DNI del infractor si los datos están cargados.
        // Mínimo 2 caracteres para evitar falsos positivos.
        if (qUpper.length() >= 2) {
            String nombre = acta.infractorNombre();
            if (nombre != null && nombre.toUpperCase().contains(qUpper)) {
                return 7;
            }
        }
        if (qEsPuramenteNumerico && qDigits.length() >= 3) {
            String doc = acta.infractorDocumento();
            if (doc != null && doc.contains(qDigits)) {
                return 7;
            }
        }

        return 0;
    }

    private PrototipoActaBusquedaResponse toResponse(EntradaMatch e) {
        ActaMock acta = e.acta();
        String actaId = acta.id();

        PrototipoStore.CerrabilidadActaVista cv = store.getCerrabilidadActa(actaId);
        String resultadoFinal = (cv != null && cv.resultadoFinal() != null)
                ? cv.resultadoFinal().name()
                : "SIN_RESULTADO_FINAL";
        boolean cerrable = cv != null && cv.cerrable();

        return new PrototipoActaBusquedaResponse(
                actaId,
                acta.numeroActa(),
                acta.bandejaActual(),
                BandejaNombres.nombre(acta.bandejaActual()),
                store.getDependenciaDemo(actaId).orElse(null),
                acta.estadoProcesoActual(),
                acta.situacionAdministrativaActual(),
                resultadoFinal,
                store.getSituacionPago(actaId).name(),
                store.getSituacionPagoCondena(actaId).name(),
                store.getMontoCondena(actaId),
                store.getAccionPendiente(actaId),
                store.getTipoGestionExterna(actaId),
                cerrable);
    }

    /**
     * Normaliza un texto para comparación flexible: uppercase, elimina todo
     * lo que no sea letra o dígito (guiones, espacios, puntos, etc.).
     */
    static String normalizar(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    /**
     * Extrae solo los dígitos de un texto (p. ej. {@code "ACTA-0018"} → {@code "0018"}).
     */
    static String extraerDigitos(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("[^0-9]", "");
    }

    private record EntradaMatch(ActaMock acta, int score) {
    }
}
