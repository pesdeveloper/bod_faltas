package ar.gob.malvinas.faltas.core.infrastructure.storage;

import ar.gob.malvinas.faltas.core.domain.exception.EvidenciaArchivoNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cobertura del adapter filesystem de evidencias (Wave 4 Parte 3D S10/S18):
 * temporal/final, promocion atomica, no-sobrescritura, compensacion
 * idempotente, path traversal.
 */
@DisplayName("FilesystemEvidenciaAlmacenamientoAdapter — filesystem real, directorio temporal aislado")
class FilesystemEvidenciaAlmacenamientoAdapterTest {

    private Path raiz;
    private FilesystemEvidenciaAlmacenamientoAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        raiz = Files.createTempDirectory("faltas-fs-adapter-test-");
        adapter = new FilesystemEvidenciaAlmacenamientoAdapter(
                raiz.toString(), raiz.toString(), new MockEnvironment());
    }

    @Nested
    @DisplayName("1. Ciclo temporal -> final")
    class CicloTemporalFinal {

        @Test
        @DisplayName("escribirTemporal crea el archivo con el contenido exacto")
        void escribirTemporalCreaArchivo() throws IOException {
            byte[] contenido = "hola mundo".getBytes();
            String clave = adapter.escribirTemporal(contenido);
            Path esperado = raiz.resolve("temporal").resolve(clave);
            assertThat(Files.isRegularFile(esperado)).isTrue();
            assertThat(Files.readAllBytes(esperado)).isEqualTo(contenido);
        }

        @Test
        @DisplayName("promover mueve el archivo de temporal a final y el temporal deja de existir")
        void promoverMueveArchivo() throws IOException {
            byte[] contenido = "contenido final".getBytes();
            String temp = adapter.escribirTemporal(contenido);
            String finalKey = adapter.generarClaveFinal("jpg");
            adapter.promover(temp, finalKey);

            assertThat(Files.exists(raiz.resolve("temporal").resolve(temp))).isFalse();
            Path destino = raiz.resolve("final").resolve(finalKey);
            assertThat(Files.isRegularFile(destino)).isTrue();
            assertThat(Files.readAllBytes(destino)).isEqualTo(contenido);
            assertThat(adapter.existeFinal(finalKey)).isTrue();
            assertThat(adapter.leerFinal(finalKey)).isEqualTo(contenido);
        }

        @Test
        @DisplayName("generarClaveFinal produce claves distintas y con la extension pedida")
        void clavesDistintasConExtension() {
            String a = adapter.generarClaveFinal("jpg");
            String b = adapter.generarClaveFinal("jpg");
            assertThat(a).isNotEqualTo(b);
            assertThat(a).endsWith(".jpg");
        }

        @Test
        @DisplayName("promover con temporal inexistente falla explicitamente")
        void promoverTemporalInexistenteFalla() {
            assertThatThrownBy(() -> adapter.promover("no-existe.tmp", adapter.generarClaveFinal("jpg")))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("2. No sobrescritura silenciosa")
    class NoSobrescritura {

        @Test
        @DisplayName("promover a una clave final que ya existe falla, no sobrescribe")
        void promoverAClaveExistenteFalla() throws IOException {
            String temp1 = adapter.escribirTemporal("uno".getBytes());
            String finalKey = adapter.generarClaveFinal("jpg");
            adapter.promover(temp1, finalKey);

            String temp2 = adapter.escribirTemporal("dos".getBytes());
            assertThatThrownBy(() -> adapter.promover(temp2, finalKey))
                    .isInstanceOf(IllegalStateException.class);

            // El contenido original permanece intacto (no fue sobrescrito).
            assertThat(adapter.leerFinal(finalKey)).isEqualTo("uno".getBytes());
        }
    }

    @Nested
    @DisplayName("3. Compensacion idempotente")
    class CompensacionIdempotente {

        @Test
        @DisplayName("eliminarTemporal sobre una clave inexistente no lanza (idempotente)")
        void eliminarTemporalInexistenteNoLanza() {
            adapter.eliminarTemporal("no-existe.tmp");
            adapter.eliminarTemporal("no-existe.tmp"); // dos veces, sigue sin lanzar
        }

        @Test
        @DisplayName("eliminarFinal sobre una clave inexistente no lanza (idempotente)")
        void eliminarFinalInexistenteNoLanza() {
            adapter.eliminarFinal("no-existe.jpg");
            adapter.eliminarFinal("no-existe.jpg");
        }

        @Test
        @DisplayName("eliminarFinal despues de promover deja de existir el archivo")
        void eliminarFinalTrasPromoverBorraElArchivo() {
            String temp = adapter.escribirTemporal("x".getBytes());
            String finalKey = adapter.generarClaveFinal("jpg");
            adapter.promover(temp, finalKey);
            assertThat(adapter.existeFinal(finalKey)).isTrue();

            adapter.eliminarFinal(finalKey);
            assertThat(adapter.existeFinal(finalKey)).isFalse();
        }
    }

    @Nested
    @DisplayName("4. Lectura de archivo inexistente")
    class LecturaInexistente {

        @Test
        @DisplayName("leerFinal sobre una clave inexistente lanza EvidenciaArchivoNoEncontradoException")
        void leerFinalInexistenteLanza404() {
            assertThatThrownBy(() -> adapter.leerFinal("jamas-existio.jpg"))
                    .isInstanceOf(EvidenciaArchivoNoEncontradoException.class);
        }

        @Test
        @DisplayName("existeFinal sobre clave nula o en blanco devuelve false, no lanza")
        void existeFinalConClaveInvalida() {
            assertThat(adapter.existeFinal(null)).isFalse();
            assertThat(adapter.existeFinal("")).isFalse();
        }
    }

    @Nested
    @DisplayName("5. Path traversal")
    class PathTraversal {

        @Test
        @DisplayName("leerFinal con clave que intenta escapar la raiz (../) es rechazada")
        void pathTraversalEnLecturaEsRechazado() {
            assertThatThrownBy(() -> adapter.leerFinal("../../../../etc/passwd"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("traversal");
        }

        @Test
        @DisplayName("existeFinal con clave de path traversal devuelve false en lugar de escapar la raiz")
        void pathTraversalEnExisteEsSeguro() {
            assertThat(adapter.existeFinal("../../../../etc/passwd")).isFalse();
        }

        @Test
        @DisplayName("las claves generadas por el adapter nunca contienen separadores de ruta")
        void clavesGeneradasSinSeparadores() {
            String clave = adapter.generarClaveFinal("jpg");
            assertThat(clave).doesNotContain("/").doesNotContain("\\").doesNotContain("..");
        }
    }

    @Nested
    @DisplayName("6. Raiz configurable")
    class RaizConfigurable {

        @Test
        @DisplayName("la raiz crea los subdirectorios temporal/ y final/ al construirse")
        void creaSubdirectorios() {
            assertThat(Files.isDirectory(raiz.resolve("temporal"))).isTrue();
            assertThat(Files.isDirectory(raiz.resolve("final"))).isTrue();
        }

        @Test
        @DisplayName("root-dir en blanco rechaza al construir")
        void rootDirEnBlancoRechaza() {
            assertThatThrownBy(() -> new FilesystemEvidenciaAlmacenamientoAdapter(
                    " ", " ", new MockEnvironment()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
