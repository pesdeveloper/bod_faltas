package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;
import ar.gob.malvinas.faltas.prototipo.store.PrototipoStore;
import ar.gob.malvinas.faltas.prototipo.web.dto.PrototipoActaBusquedaMatchResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.PrototipoActaBusquedaResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Búsqueda global explicable de actas sobre el dataset demo en memoria.
 *
 * <p>Reglas de matching por tipo de criterio:
 * <ul>
 *   <li><b>Numérico puro &lt; 7 dígitos</b>: solo número de acta (y expediente externo si aplica).</li>
 *   <li><b>Numérico puro ≥ 7 dígitos</b>: número de acta y documento (DOC/CUIT/CUIL).</li>
 *   <li><b>Solo letras</b>: nombre/apellido del infractor.</li>
 *   <li><b>Alfanumérico plausible como dominio</b>: patente/matrícula vehicular.</li>
 * </ul>
 *
 * <p>Cada resultado incluye la lista {@code matches} explicando qué campo produjo la
 * coincidencia, junto con un {@code score} y {@code scoreLabel} de relevancia.
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
     * @return lista de resultados explicados, vacía si no hay matches
     */
    List<PrototipoActaBusquedaResponse> buscar(String q) {
        if (q == null || q.isBlank()) {
            return List.of();
        }

        String qTrim = q.trim();
        String qNorm = normalizar(qTrim);

        if (qNorm.isEmpty()) {
            return List.of();
        }

        List<EntradaMatch> candidates = new ArrayList<>();

        for (ActaMock acta : store.getActas().values()) {
            List<PrototipoActaBusquedaMatchResponse> matches = calcularMatches(acta, qTrim, qNorm);
            if (!matches.isEmpty()) {
                int score = matches.stream()
                        .mapToInt(m -> m.score() != null ? m.score() : 0)
                        .max()
                        .orElse(0);
                candidates.add(new EntradaMatch(acta, score, matches));
            }
        }

        return candidates.stream()
                .sorted(Comparator.comparingInt(EntradaMatch::score).reversed()
                        .thenComparing(e -> e.acta().numeroActa()))
                .limit(MAX_RESULTADOS)
                .map(this::toResponse)
                .toList();
    }

    // ─── Cálculo de matches por acta ────────────────────────────────────────

    private List<PrototipoActaBusquedaMatchResponse> calcularMatches(
            ActaMock acta, String qTrim, String qNorm) {

        List<PrototipoActaBusquedaMatchResponse> matches = new ArrayList<>();

        // Número de acta: siempre se intenta
        matchNumeroActa(acta, qTrim, qNorm, matches);

        // Documento: solo si q es numérico puro con 7+ dígitos
        if (puedeBuscarDocumento(qTrim)) {
            matchDocumento(acta, qTrim, matches);
        }

        // Nombre/apellido: solo si q contiene letras y NO contiene dígitos
        if (puedeBuscarNombre(qTrim)) {
            matchNombre(acta, qTrim, matches);
        }

        // Dominio vehicular: solo si q parece patente/matrícula plausible
        if (esDominioVehicularPlausible(qTrim)) {
            matchDominio(acta, qTrim, matches);
        }

        return matches;
    }

    private void matchNumeroActa(
            ActaMock acta, String qTrim, String qNorm,
            List<PrototipoActaBusquedaMatchResponse> out) {

        String actaId = acta.id();
        String numeroActa = acta.numeroActa();
        String actaIdNorm = normalizar(actaId);
        String numeroActaNorm = normalizar(numeroActa);

        int score = 0;
        String fragmento;

        // Exact match raw (case-insensitive)
        if (actaId.equalsIgnoreCase(qTrim) || numeroActa.equalsIgnoreCase(qTrim)) {
            score = 100;
            fragmento = encontrarFragmento(qTrim, numeroActa);
        }
        // Exact match normalizado
        else if (actaIdNorm.equals(qNorm) || numeroActaNorm.equals(qNorm)) {
            score = 100;
            fragmento = encontrarFragmento(qTrim, numeroActa);
        }
        // Starts-with normalizado
        else if (actaIdNorm.startsWith(qNorm) || numeroActaNorm.startsWith(qNorm)) {
            score = 90;
            fragmento = encontrarFragmento(qTrim, numeroActa);
        } else if (esNumericoPuro(qTrim)) {
            String qDigits = extraerDigitos(qTrim);
            String actaDigits = extraerDigitos(actaId);
            if (actaDigits.equals(qDigits)) {
                // Dígitos exactos con ceros: q="0030" → ACTA-0030
                score = 90;
                fragmento = encontrarFragmento(qTrim, numeroActa);
            } else if (coincideNumeroActaExactoNumerico(qTrim, actaId)) {
                // Exacto numérico sin ceros: q="30" → ACTA-0030 (30==30), pero no ACTA-0130 (30≠130)
                score = 90;
                fragmento = encontrarFragmento(qTrim, numeroActa);
            } else if (actaDigits.contains(qDigits)) {
                // Contiene como substring: q="30" → ACTA-0130
                score = 75;
                fragmento = encontrarFragmento(qTrim, numeroActa);
            } else {
                fragmento = null;
            }
        } else if (actaIdNorm.contains(qNorm) || numeroActaNorm.contains(qNorm)) {
            score = 75;
            fragmento = encontrarFragmento(qTrim, numeroActa);
        } else {
            fragmento = null;
        }

        if (score > 0) {
            out.add(new PrototipoActaBusquedaMatchResponse(
                    "NUMERO_ACTA", "Número de acta", numeroActa, fragmento, score));
        }
    }

    private void matchDocumento(
            ActaMock acta, String qTrim,
            List<PrototipoActaBusquedaMatchResponse> out) {

        String doc = acta.infractorDocumento();
        if (doc == null) return;

        String qDigits = extraerDigitos(qTrim);
        String docDigits = extraerDigitos(doc);
        if (docDigits.isEmpty()) return;

        int score = 0;
        if (docDigits.equals(qDigits)) {
            score = 100;
        } else if (docDigits.contains(qDigits)) {
            score = 75;
        }

        if (score > 0) {
            String label = inferirLabelDocumento(doc);
            out.add(new PrototipoActaBusquedaMatchResponse(
                    "DOCUMENTO_INFRACTOR", label, doc, qTrim, score));
        }
    }

    private void matchNombre(
            ActaMock acta, String qTrim,
            List<PrototipoActaBusquedaMatchResponse> out) {

        String nombre = acta.infractorNombre();
        if (nombre == null) return;

        if (nombre.toLowerCase().contains(qTrim.toLowerCase())) {
            out.add(new PrototipoActaBusquedaMatchResponse(
                    "NOMBRE_INFRACTOR", "Infractor", nombre,
                    encontrarFragmento(qTrim, nombre), 60));
        }
    }

    private void matchDominio(
            ActaMock acta, String qTrim,
            List<PrototipoActaBusquedaMatchResponse> out) {

        String patente = store.getPatenteVehiculo(acta.id()).orElse(null);
        if (patente == null) return;

        String patenteNorm = normalizar(patente);
        String qNorm = normalizar(qTrim);

        int score = 0;
        if (patenteNorm.equals(qNorm)) {
            score = 100;
        } else if (patenteNorm.contains(qNorm)) {
            score = 75;
        }

        if (score > 0) {
            out.add(new PrototipoActaBusquedaMatchResponse(
                    "DOMINIO", "Dominio", patente,
                    encontrarFragmento(qTrim, patente), score));
        }
    }

    // ─── Construcción del DTO de respuesta ──────────────────────────────────

    private PrototipoActaBusquedaResponse toResponse(EntradaMatch e) {
        ActaMock acta = e.acta();
        String actaId = acta.id();

        PrototipoStore.CerrabilidadActaVista cv = store.getCerrabilidadActa(actaId);
        String resultadoFinal = (cv != null && cv.resultadoFinal() != null)
                ? cv.resultadoFinal().name()
                : "SIN_RESULTADO_FINAL";
        boolean cerrable = cv != null && cv.cerrable();

        int score = e.score();

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
                cerrable,
                score,
                calcularScoreLabel(score),
                e.matches());
    }

    // ─── Helpers de clasificación de query ──────────────────────────────────

    /** {@code true} si q es puramente numérico. */
    static boolean esNumericoPuro(String s) {
        if (s == null || s.isEmpty()) return false;
        return s.chars().allMatch(Character::isDigit);
    }

    /** {@code true} si q contiene al menos una letra. */
    static boolean contieneLetras(String s) {
        return s != null && s.chars().anyMatch(Character::isLetter);
    }

    /** {@code true} si q contiene al menos un dígito. */
    static boolean contieneNumeros(String s) {
        return s != null && s.chars().anyMatch(Character::isDigit);
    }

    /**
     * {@code true} si se puede usar q para búsqueda por documento:
     * q debe ser numérico puro y tener al menos 7 dígitos.
     */
    static boolean puedeBuscarDocumento(String q) {
        if (q == null) return false;
        String t = q.trim();
        return esNumericoPuro(t) && t.length() >= 7;
    }

    /**
     * {@code true} si se puede usar q para búsqueda por nombre/apellido:
     * q debe contener letras y NO contener dígitos.
     */
    static boolean puedeBuscarNombre(String q) {
        return contieneLetras(q) && !contieneNumeros(q);
    }

    /**
     * {@code true} si q parece una patente/matrícula vehicular plausible.
     *
     * <p>Condiciones (sobre la forma normalizada):
     * <ul>
     *   <li>Longitud entre 5 y 10 caracteres.</li>
     *   <li>Al menos una letra.</li>
     *   <li>Al menos un dígito.</li>
     * </ul>
     *
     * <p>Ejemplos aceptables: {@code ABC123}, {@code AB123CD}, {@code ABC-123},
     * {@code DIP1234}, {@code AB1234}.
     * <p>Ejemplos NO aceptables: {@code AB1} (muy corto), {@code HERRERA} (sin dígitos),
     * {@code 1234567} (sin letras), {@code CALLE118} (letras + dígitos pero texto libre).
     */
    static boolean esDominioVehicularPlausible(String q) {
        if (q == null) return false;
        String norm = normalizar(q);
        if (norm.length() < 5 || norm.length() > 10) return false;
        return contieneLetras(norm) && contieneNumeros(norm);
    }

    // ─── Helpers de scoring y fragmento ─────────────────────────────────────

    private static String calcularScoreLabel(int score) {
        if (score >= 85) return "ALTA";
        if (score >= 60) return "MEDIA";
        return "BAJA";
    }

    /**
     * Devuelve {@code q} si aparece en {@code valor} (case-insensitive), o {@code null} si no.
     * Permite que la UI resalte el fragmento exacto dentro del valor mostrado.
     */
    private static String encontrarFragmento(String q, String valor) {
        if (q == null || valor == null) return null;
        return valor.toLowerCase().contains(q.toLowerCase()) ? q : null;
    }

    private static String inferirLabelDocumento(String doc) {
        if (doc == null) return "Documento";
        String upper = doc.trim().toUpperCase();
        if (upper.startsWith("CUIT")) return "CUIT";
        if (upper.startsWith("CUIL")) return "CUIL";
        return "DOC";
    }

    // ─── Helpers de número de acta ──────────────────────────────────────────

    /**
     * Extrae el último bloque numérico contiguo de {@code valor}.
     * Ejemplo: {@code "ACTA-0030"} → {@code "0030"}, {@code "A-2026-0130"} → {@code "0130"}.
     */
    static String ultimoBloqueNumerico(String valor) {
        if (valor == null || valor.isEmpty()) return "";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[0-9]+").matcher(valor);
        String ultimo = "";
        while (m.find()) {
            ultimo = m.group();
        }
        return ultimo;
    }

    /**
     * Elimina ceros a la izquierda de {@code s}, preservando al menos un dígito.
     * Ejemplo: {@code "0030"} → {@code "30"}, {@code "0"} → {@code "0"}.
     */
    static String normalizarSinCerosIzquierda(String s) {
        if (s == null || s.isEmpty()) return "0";
        String stripped = s.replaceAll("^0+", "");
        return stripped.isEmpty() ? "0" : stripped;
    }

    /**
     * {@code true} si {@code q} (numérico puro) coincide exactamente con el número
     * operativo del último bloque numérico de {@code actaId}, ambos sin ceros a la izquierda.
     *
     * <p>Ejemplos:
     * <ul>
     *   <li>q="{@code 30}", actaId="{@code ACTA-0030}" → {@code true} (30 == 30)</li>
     *   <li>q="{@code 30}", actaId="{@code ACTA-0130}" → {@code false} (30 ≠ 130)</li>
     *   <li>q="{@code 24}", actaId="{@code ACTA-0024}" → {@code true} (24 == 24)</li>
     * </ul>
     */
    static boolean coincideNumeroActaExactoNumerico(String q, String actaId) {
        if (!esNumericoPuro(q)) return false;
        String ultimoBloque = ultimoBloqueNumerico(actaId);
        if (ultimoBloque.isEmpty()) return false;
        return normalizarSinCerosIzquierda(q).equals(normalizarSinCerosIzquierda(ultimoBloque));
    }

    // ─── Normalización ──────────────────────────────────────────────────────

    /**
     * Normaliza texto para comparación flexible:
     * uppercase y conserva solo letras/dígitos (elimina guiones, espacios, puntos, etc.).
     */
    static String normalizar(String s) {
        if (s == null) return "";
        return s.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    /**
     * Extrae solo los dígitos de un texto (p. ej. {@code "ACTA-0018"} → {@code "0018"}).
     */
    static String extraerDigitos(String s) {
        if (s == null) return "";
        return s.replaceAll("[^0-9]", "");
    }

    // ─── Tipos internos ─────────────────────────────────────────────────────

    private record EntradaMatch(
            ActaMock acta,
            int score,
            List<PrototipoActaBusquedaMatchResponse> matches) {
    }
}
