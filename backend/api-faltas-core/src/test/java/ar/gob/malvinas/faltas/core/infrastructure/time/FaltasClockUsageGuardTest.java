package ar.gob.malvinas.faltas.core.infrastructure.time;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class FaltasClockUsageGuardTest {

    private static final Path MAIN_JAVA = resolveMainJava();

    /** Files allowed to contain direct clock construction: only the canonical implementation. */
    private static final String ALLOWLIST_CLOCK_IMPL = "FaltasClock.java";

    /** Files allowed to contain new FaltasClock(): canonical impl + documented composition roots. */
    private static final Set<String> ALLOWLIST_NEW_FALTAS_CLOCK = Set.of(
            "FaltasClock.java",
            "CasoUsoFuncionalRunner.java",
            "AesGcmQrTokenProtector.java"
    );

    private static final List<Pattern> FORBIDDEN_DIRECT = List.of(
            Pattern.compile("\\bLocalDateTime\\.now\\s*\\("),
            Pattern.compile("\\bLocalDate\\.now\\s*\\("),
            Pattern.compile("\\bInstant\\.now\\s*\\(\\s*\\)"),
            Pattern.compile("\\bZonedDateTime\\.now\\s*\\("),
            Pattern.compile("\\bOffsetDateTime\\.now\\s*\\("),
            Pattern.compile("\\bnew\\s+Date\\s*\\("),
            Pattern.compile("\\bSystem\\.currentTimeMillis\\s*\\("),
            Pattern.compile("\\bClock\\.system(DefaultZone|UTC)?\\s*\\(")
    );

    private static final Pattern NEW_FALTAS_CLOCK = Pattern.compile("\\bnew\\s+FaltasClock\\s*\\(");

    private static Path resolveMainJava() {
        Path relative = Paths.get("src/main/java");
        if (Files.isDirectory(relative)) {
            return relative;
        }
        // Resolve from the project root when run from a different working directory
        Path fromClassPath = Paths.get(
                FaltasClockUsageGuardTest.class.getProtectionDomain()
                        .getCodeSource().getLocation().getPath())
                .resolve("../../../../src/main/java").normalize();
        if (Files.isDirectory(fromClassPath)) {
            return fromClassPath;
        }
        return relative; // fall through - test will report missing directory
    }

    @Test
    void mainJava_existe_el_directorio_fuente() {
        assertThat(MAIN_JAVA)
                .as("src/main/java must exist and be a directory. Working dir: %s", Paths.get("").toAbsolutePath())
                .isDirectory();
    }

    @Test
    void mainJava_noUsosDirectosDeRelojFueraDeAllowlist() throws IOException {
        assertThat(MAIN_JAVA).as("src/main/java directory must exist").isDirectory();
        List<String> violations = new ArrayList<>();
        try (var stream = Files.walk(MAIN_JAVA)) {
            stream.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.getFileName().toString().equals(ALLOWLIST_CLOCK_IMPL))
                    .forEach(p -> scanForDirectClock(p, violations));
        }
        assertThat(violations)
                .as("Usos temporales directos prohibidos en src/main/java")
                .isEmpty();
    }

    @Test
    void mainJava_noNewFaltasClockFueraDeAllowlist() throws IOException {
        assertThat(MAIN_JAVA).as("src/main/java directory must exist").isDirectory();
        List<String> violations = new ArrayList<>();
        try (var stream = Files.walk(MAIN_JAVA)) {
            stream.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !ALLOWLIST_NEW_FALTAS_CLOCK.contains(p.getFileName().toString()))
                    .forEach(p -> scanForNewFaltasClock(p, violations));
        }
        assertThat(violations)
                .as("new FaltasClock() fuera de allowlist en src/main/java")
                .isEmpty();
    }

    private static void scanForDirectClock(Path file, List<String> violations) {
        try {
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (isComment(line)) continue;
                for (Pattern pattern : FORBIDDEN_DIRECT) {
                    if (pattern.matcher(line).find()) {
                        violations.add(file + ":" + (i + 1) + " -> " + line.trim());
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading: " + file, e);
        }
    }

    private static void scanForNewFaltasClock(Path file, List<String> violations) {
        try {
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (isComment(line)) continue;
                if (NEW_FALTAS_CLOCK.matcher(line).find()) {
                    violations.add(file + ":" + (i + 1) + " -> " + line.trim());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading: " + file, e);
        }
    }

    private static boolean isComment(String line) {
        String t = line.trim();
        return t.startsWith("//") || t.startsWith("*") || t.startsWith("/*");
    }
}
