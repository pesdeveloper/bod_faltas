package ar.gob.malvinas.faltas.core.infrastructure.storage;

import ar.gob.malvinas.faltas.core.application.port.AlmacenamientoEvidenciaPort;
import ar.gob.malvinas.faltas.core.domain.exception.EvidenciaArchivoNoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Adapter filesystem productivo de {@link AlmacenamientoEvidenciaPort} (Wave 4
 * Parte 3D S10). Raiz configurable por {@code faltas.storage.evidencias.root-dir}
 * (property, nunca hardcodeada); dos subdirectorios bajo la raiz:
 *
 * <pre>
 * temporal/  archivos en preparacion, nunca servidos ni referenciados por metadata
 * final/     archivos promovidos, referenciados por fal_acta_evidencia.storage_key
 * </pre>
 *
 * <p>Las claves son siempre {@code UUID.randomUUID()} generadas por este
 * adapter: ningun llamador aporta el nombre de archivo ni ningun fragmento de
 * el, de modo que no existe entrada de cliente en la ruta fisica (inmune a
 * path traversal por construccion, no por sanitizacion posterior).
 */
@Component
public class FilesystemEvidenciaAlmacenamientoAdapter implements AlmacenamientoEvidenciaPort {

    private static final Logger log = LoggerFactory.getLogger(FilesystemEvidenciaAlmacenamientoAdapter.class);

    /**
     * Perfiles bajo los cuales se permite el default relativo de conveniencia de
     * {@code faltas.storage.evidencias.root-dir} sin configuración explícita adicional (Wave 4
     * Parte 3E-B §9): {@code local}/{@code dev} (ambientes de desarrollo), {@code test}
     * (exclusivo de {@code src/test}) y {@code jdbc} en sí — combo productivo legítimo y
     * ampliamente usado por la suite de integración real contra MariaDB (prompt §13.2 "jdbc
     * arranca"), no un ambiente de despliegue con requisitos operativos propios. El proyecto no
     * define hoy ningún otro perfil de ambiente productivo (p. ej. "staging"/"prod"): la regla
     * estricta de más abajo sólo se activa si en el futuro se combina "jdbc" con un perfil
     * distinto de estos, no se inventa aquí ese nombre.
     */
    static final List<String> PERFILES_SIN_REQUISITO_DE_RUTA_EXPLICITA = List.of("jdbc", "local", "dev", "test");

    private final Path raizTemporal;
    private final Path raizFinal;

    public FilesystemEvidenciaAlmacenamientoAdapter(
            @Value("${faltas.storage.evidencias.root-dir:./faltas-data/evidencias}") String rootDir,
            @Value("${faltas.storage.evidencias.root-dir:#{null}}") String rootDirSinDefault,
            Environment environment) {
        validarRootDirProductivo(rootDir, rootDirSinDefault, environment.getActiveProfiles());
        Path raiz = Path.of(rootDir).toAbsolutePath().normalize();
        this.raizTemporal = raiz.resolve("temporal");
        this.raizFinal = raiz.resolve("final");
        try {
            Files.createDirectories(raizTemporal);
            Files.createDirectories(raizFinal);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo preparar la raiz de almacenamiento de evidencias: " + raiz, e);
        }
    }

    /**
     * Hardening de {@code faltas.storage.evidencias.root-dir} (Wave 4 Parte 3E-B §9).
     *
     * <ul>
     *   <li>Vacía: siempre rechazada, en cualquier ambiente.</li>
     *   <li>{@code classpath:...}: siempre rechazada — la raíz de evidencias nunca puede vivir
     *       dentro del artefacto empaquetado.</li>
     *   <li>Exactamente igual a la raíz cruda de temporales del sistema operativo
     *       ({@code java.io.tmpdir}): siempre rechazada. No afecta a los tests que usan
     *       {@code Files.createTempDirectory(...)}: esa API crea un subdirectorio único
     *       <b>dentro</b> de esa raíz, nunca igual a la raíz misma.</li>
     *   <li>Fuera de {@code local}/{@code dev}/{@code test}: exige que la property haya sido
     *       configurada explícitamente (no vale el default relativo de conveniencia). Esta
     *       condición es vacía hoy: el proyecto no define todavía ningún perfil de ambiente
     *       productivo distinto de {@code local}/{@code dev} (ver
     *       {@code StorageRootDirGuardrailTest} para la certificación de la regla en sí, con un
     *       perfil hipotético).</li>
     * </ul>
     */
    static void validarRootDirProductivo(String rootDir, String rootDirSinDefault, String[] perfilesActivos) {
        if (rootDir == null || rootDir.isBlank()) {
            throw new IllegalArgumentException("faltas.storage.evidencias.root-dir es obligatorio");
        }
        if (rootDir.regionMatches(true, 0, "classpath:", 0, "classpath:".length())) {
            throw new IllegalArgumentException(
                    "faltas.storage.evidencias.root-dir no puede ser una ruta classpath: " + rootDir);
        }
        String tmpdirCrudo = System.getProperty("java.io.tmpdir");
        if (tmpdirCrudo != null) {
            Path raizSolicitada = Path.of(rootDir).toAbsolutePath().normalize();
            Path raizTemporalSistema = Path.of(tmpdirCrudo).toAbsolutePath().normalize();
            if (raizSolicitada.equals(raizTemporalSistema)) {
                throw new IllegalArgumentException(
                        "faltas.storage.evidencias.root-dir no puede ser la raiz temporal cruda del"
                                + " sistema operativo (" + raizTemporalSistema + "); usar un"
                                + " subdirectorio explicito.");
            }
        }
        boolean hayPerfilProductivoEstrictoNoDefinidoTodavia = Arrays.stream(perfilesActivos)
                .map(p -> p.toLowerCase(Locale.ROOT))
                .anyMatch(p -> !PERFILES_SIN_REQUISITO_DE_RUTA_EXPLICITA.contains(p));
        if (hayPerfilProductivoEstrictoNoDefinidoTodavia && rootDirSinDefault == null) {
            throw new IllegalArgumentException(
                    "faltas.storage.evidencias.root-dir debe configurarse explicitamente fuera de"
                            + " los perfiles " + PERFILES_SIN_REQUISITO_DE_RUTA_EXPLICITA
                            + "; no se permite el default relativo de conveniencia en ese ambiente."
                            + " Perfiles activos: " + Arrays.toString(perfilesActivos) + ".");
        }
    }

    @Override
    public String escribirTemporal(byte[] contenido) {
        if (contenido == null) {
            throw new IllegalArgumentException("contenido es obligatorio");
        }
        String clave = UUID.randomUUID() + ".tmp";
        Path destino = raizTemporal.resolve(clave);
        escribirConFsync(destino, contenido);
        return clave;
    }

    @Override
    public String generarClaveFinal(String extension) {
        if (extension == null || extension.isBlank()) {
            throw new IllegalArgumentException("extension es obligatoria");
        }
        String ext = extension.startsWith(".") ? extension.substring(1) : extension;
        if (!ext.matches("[a-zA-Z0-9]{1,10}")) {
            throw new IllegalArgumentException("extension invalida: " + extension);
        }
        return UUID.randomUUID() + "." + ext.toLowerCase();
    }

    @Override
    public void promover(String claveTemporal, String claveFinal) {
        Path origen = resolverDentroDe(raizTemporal, claveTemporal);
        Path destino = resolverDentroDe(raizFinal, claveFinal);
        if (!Files.isRegularFile(origen)) {
            throw new IllegalStateException("Archivo temporal inexistente para promover: " + claveTemporal);
        }
        if (Files.exists(destino)) {
            throw new IllegalStateException("La clave final ya existe (colision inesperada): " + claveFinal);
        }
        try {
            Files.move(origen, destino, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException noAtomico) {
            log.warn("Move atomico no soportado por el filesystem (raiz temporal/final en volumenes "
                    + "distintos); usando copia + fsync + borrado del temporal para {}", claveFinal);
            promoverSinMoveAtomico(origen, destino);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo promover " + claveTemporal + " -> " + claveFinal, e);
        }
    }

    private void promoverSinMoveAtomico(Path origen, Path destino) {
        try {
            Files.copy(origen, destino, (CopyOption) StandardCopyOption.COPY_ATTRIBUTES);
            fsyncArchivo(destino);
            Files.delete(origen);
        } catch (IOException e) {
            // Compensacion inmediata: un destino a medio escribir no debe sobrevivir.
            try {
                Files.deleteIfExists(destino);
            } catch (IOException limpiezaFallida) {
                log.error("Fallo de cleanup tras promocion fallida sin move atomico, destino={}",
                        destino, limpiezaFallida);
            }
            throw new IllegalStateException("No se pudo promover sin move atomico: " + origen + " -> " + destino, e);
        }
    }

    @Override
    public void eliminarTemporal(String claveTemporal) {
        eliminarIdempotente(raizTemporal, claveTemporal);
    }

    @Override
    public void eliminarFinal(String claveFinal) {
        eliminarIdempotente(raizFinal, claveFinal);
    }

    private void eliminarIdempotente(Path raiz, String clave) {
        if (clave == null || clave.isBlank()) {
            return;
        }
        try {
            Path destino = resolverDentroDe(raiz, clave);
            boolean borrado = Files.deleteIfExists(destino);
            if (!borrado) {
                log.debug("Compensacion idempotente: {} ya no existia bajo {}", clave, raiz);
            }
        } catch (IOException e) {
            // Fallo de cleanup observable, nunca silenciado (Parte 3D S11).
            log.error("Fallo de cleanup (no silenciado) eliminando clave={} bajo raiz={}", clave, raiz, e);
        }
    }

    @Override
    public byte[] leerFinal(String claveFinal) {
        Path origen = resolverDentroDe(raizFinal, claveFinal);
        if (!Files.isRegularFile(origen)) {
            throw new EvidenciaArchivoNoEncontradoException(
                    "Archivo final de evidencia no encontrado para la clave informada.");
        }
        try {
            return Files.readAllBytes(origen);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el archivo final: " + claveFinal, e);
        }
    }

    @Override
    public boolean existeFinal(String claveFinal) {
        if (claveFinal == null || claveFinal.isBlank()) {
            return false;
        }
        try {
            return Files.isRegularFile(resolverDentroDe(raizFinal, claveFinal));
        } catch (IllegalArgumentException clavesInvalida) {
            return false;
        }
    }

    /**
     * Resuelve {@code clave} contra {@code raiz} y verifica que el resultado
     * normalizado siga siendo descendiente directo de la raiz (defensa en
     * profundidad contra path traversal, aunque las claves de este adapter
     * son siempre UUID generados por el; esta verificacion cubre tambien una
     * clave corrupta o manipulada que llegara por error desde otra capa).
     */
    private static Path resolverDentroDe(Path raiz, String clave) {
        if (clave == null || clave.isBlank()) {
            throw new IllegalArgumentException("clave es obligatoria");
        }
        Path candidato = raiz.resolve(clave).normalize();
        if (!candidato.startsWith(raiz)) {
            throw new IllegalArgumentException("Clave fuera de la raiz permitida (path traversal): " + clave);
        }
        return candidato;
    }

    private static void escribirConFsync(Path destino, byte[] contenido) {
        try (RandomAccessFile raf = new RandomAccessFile(destino.toFile(), "rw")) {
            raf.write(contenido);
            raf.getFD().sync();
        } catch (IOException e) {
            try {
                Files.deleteIfExists(destino);
            } catch (IOException limpiezaFallida) {
                log.error("Fallo de cleanup tras escritura temporal fallida, destino={}", destino, limpiezaFallida);
            }
            throw new IllegalStateException("No se pudo escribir el archivo temporal: " + destino, e);
        }
    }

    private static void fsyncArchivo(Path destino) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(destino.toFile(), "rw")) {
            raf.getFD().sync();
        }
    }
}
