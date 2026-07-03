package ar.gob.malvinas.faltas.core.application.combinacion;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Motor de combinacion de documentos.
 *
 * Detecta variables {{namespace.campo}} en un template y las reemplaza con valores del contexto.
 *
 * Reglas de seguridad:
 *   - No usa SpEL, eval, ScriptEngine, Groovy, MVEL ni JavaScript.
 *   - No permite expresiones arbitrarias, llamadas a metodos ni acceso reflexivo.
 *   - Solo reemplaza variables con el formato exacto {{namespace.campo}}.
 *
 * Politica de faltantes:
 *   - Desconocida (no en registry): registrada en variablesDesconocidas, no reemplazada.
 *   - Requerida faltante: registrada en variablesFaltantes, no reemplazada.
 *   - Opcional faltante: reemplazada por cadena vacia.
 *
 * Slice 8F-1.
 */
@Service
public class DocumentoCombinacionService {

    private static final Pattern VAR_PATTERN = Pattern.compile(
            "\\{\\{\\s*([a-z][a-zA-Z0-9]*(?:\\.[a-z][a-zA-Z0-9]*)+)\\s*\\}\\}");

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final DocumentoVariableRegistry registry;

    public DocumentoCombinacionService(DocumentoVariableRegistry registry) {
        this.registry = registry;
    }

    public DocumentoCombinacionResultado combinar(String template, Map<String, Object> contexto) {
        if (template == null) throw new IllegalArgumentException("template no puede ser null");
        if (contexto == null) throw new IllegalArgumentException("contexto no puede ser null");

        Set<String> variablesUsadas = new LinkedHashSet<>();
        Set<String> variablesFaltantes = new LinkedHashSet<>();
        Set<String> variablesDesconocidas = new LinkedHashSet<>();

        StringBuffer sb = new StringBuffer();
        Matcher matcher = VAR_PATTERN.matcher(template);

        while (matcher.find()) {
            String nombreVar = matcher.group(1);
            variablesUsadas.add(nombreVar);

            if (!registry.estaRegistrada(nombreVar)) {
                variablesDesconocidas.add(nombreVar);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }

            if (contexto.containsKey(nombreVar)) {
                Object valor = contexto.get(nombreVar);
                if (valor == null) {
                    if (registry.esRequerida(nombreVar)) {
                        variablesFaltantes.add(nombreVar);
                        matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
                    } else {
                        matcher.appendReplacement(sb, "");
                    }
                } else {
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(formatear(valor)));
                }
            } else {
                if (registry.esRequerida(nombreVar)) {
                    variablesFaltantes.add(nombreVar);
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
                } else {
                    matcher.appendReplacement(sb, "");
                }
            }
        }
        matcher.appendTail(sb);

        if (variablesFaltantes.isEmpty() && variablesDesconocidas.isEmpty()) {
            return DocumentoCombinacionResultado.exitoso(sb.toString(), Set.copyOf(variablesUsadas));
        }
        return DocumentoCombinacionResultado.conProblemas(
                sb.toString(), Set.copyOf(variablesUsadas),
                Set.copyOf(variablesFaltantes), Set.copyOf(variablesDesconocidas));
    }

    private String formatear(Object valor) {
        if (valor instanceof LocalDate d) return d.format(FMT_FECHA);
        if (valor instanceof LocalDateTime dt) return dt.format(FMT_FECHA_HORA);
        return valor.toString();
    }
}