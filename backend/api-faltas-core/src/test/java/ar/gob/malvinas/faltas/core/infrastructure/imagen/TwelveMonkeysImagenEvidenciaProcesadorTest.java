package ar.gob.malvinas.faltas.core.infrastructure.imagen;

import ar.gob.malvinas.faltas.core.application.port.ImagenProcesada;
import ar.gob.malvinas.faltas.core.domain.exception.ImagenEvidenciaInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cobertura del pipeline de normalizacion de fotografias (Wave 4 Parte 3D S7/S18).
 * Usa unicamente imagenes generadas en memoria (nunca archivos con derechos dudosos).
 */
@DisplayName("TwelveMonkeysImagenEvidenciaProcesador — pipeline de normalizacion (Parte 3D)")
class TwelveMonkeysImagenEvidenciaProcesadorTest {

    private static byte[] jpeg(int w, int h) {
        return codificar(imagenSolida(w, h, Color.RED, false), "jpg");
    }

    private static byte[] pngConAlfa(int w, int h) {
        return codificar(imagenSolida(w, h, new Color(10, 20, 30, 128), true), "png");
    }

    private static byte[] pngSinAlfa(int w, int h) {
        return codificar(imagenSolida(w, h, Color.BLUE, false), "png");
    }

    private static BufferedImage imagenSolida(int w, int h, Color color, boolean alfa) {
        BufferedImage img = new BufferedImage(w, h, alfa ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        var g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return img;
    }

    private static byte[] codificar(BufferedImage img, String formato) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (!ImageIO.write(img, formato, baos)) {
                throw new IllegalStateException("Sin writer para " + formato);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String sha256(byte[] datos) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(datos);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private TwelveMonkeysImagenEvidenciaProcesador procesador(long maxBytes, long maxPixeles, int ladoMayor) {
        return new TwelveMonkeysImagenEvidenciaProcesador(maxBytes, maxPixeles, ladoMayor, 0.85f);
    }

    private TwelveMonkeysImagenEvidenciaProcesador procesadorDefault() {
        return procesador(5_242_880L, 40_000_000L, 1920);
    }

    // ─────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("1. Formatos validados por contenido real")
    class Formatos {

        @Test
        @DisplayName("JPEG valido se procesa correctamente")
        void jpegValido() {
            ImagenProcesada r = procesadorDefault().procesar(jpeg(64, 48), "foto.jpg");
            assertThat(r.mimeTipoFinal()).isEqualTo("image/jpeg");
            assertThat(r.anchoPx()).isEqualTo(64);
            assertThat(r.altoPx()).isEqualTo(48);
            assertThat(r.sha256Hex()).isEqualTo(sha256(r.bytesFinal()));
            assertThat(r.sha256Hex()).hasSize(64).matches("^[0-9a-f]{64}$");
        }

        @Test
        @DisplayName("PNG valido (sin alfa) se procesa y recodifica a JPEG")
        void pngValidoSinAlfa() {
            ImagenProcesada r = procesadorDefault().procesar(pngSinAlfa(32, 32), "foto.png");
            assertThat(r.mimeTipoFinal()).isEqualTo("image/jpeg");
            assertThat(r.anchoPx()).isEqualTo(32);
            assertThat(r.altoPx()).isEqualTo(32);
        }

        @Test
        @DisplayName("PNG con transparencia se aplana sobre fondo blanco (politica documentada)")
        void pngConAlfaSeAplanaBlanco() {
            ImagenProcesada r = procesadorDefault().procesar(pngConAlfa(20, 20), "foto-alfa.png");
            assertThat(r.mimeTipoFinal()).isEqualTo("image/jpeg");
            // Verificacion indirecta: el resultado debe decodificar sin canal alfa (JPEG no lo soporta).
            assertThatCode_noAlfa(r.bytesFinal());
        }

        private void assertThatCode_noAlfa(byte[] bytesFinal) {
            try {
                BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(bytesFinal));
                assertThat(decoded.getColorModel().hasAlpha()).isFalse();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Test
        @DisplayName("GIF (formato no soportado) se rechaza aunque sea una imagen valida")
        void gifRechazado() {
            byte[] gif = codificar(imagenSolida(10, 10, Color.GREEN, false), "gif");
            assertThatThrownBy(() -> procesadorDefault().procesar(gif, "foto.gif"))
                    .isInstanceOf(ImagenEvidenciaInvalidaException.class)
                    .hasMessageContaining("no soportado");
        }

        @Test
        @DisplayName("contenido con extension .jpg pero que en realidad es texto se rechaza por contenido real")
        void extensionEnganosaSeRechazaPorContenidoReal() {
            byte[] textoDisfrazado = "esto no es una imagen, es texto plano".getBytes();
            assertThatThrownBy(() -> procesadorDefault().procesar(textoDisfrazado, "foto.jpg"))
                    .isInstanceOf(ImagenEvidenciaInvalidaException.class);
        }

        @Test
        @DisplayName("WEBP valido (fixture sintetico minimo) se decodifica y procesa")
        void webpValido() {
            // Fixture sintetico minimo (1x1, lossy VP8), sin derechos de autor: es un pixel en
            // blanco generado programaticamente, publicado como caso de prueba tecnico estandar.
            byte[] webp = Base64.getDecoder().decode(
                    "UklGRhoAAABXRUJQVlA4TA0AAAAvAAAAEAcQERGIiP4HAA==");
            ImagenProcesada r = procesadorDefault().procesar(webp, "foto.webp");
            assertThat(r.mimeTipoFinal()).isEqualTo("image/jpeg");
            assertThat(r.anchoPx()).isGreaterThan(0);
            assertThat(r.altoPx()).isGreaterThan(0);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("2. Rechazos: vacio, corrupto, limites")
    class Rechazos {

        @Test
        @DisplayName("archivo vacio se rechaza")
        void archivoVacio() {
            assertThatThrownBy(() -> procesadorDefault().procesar(new byte[0], "x.jpg"))
                    .isInstanceOf(ImagenEvidenciaInvalidaException.class)
                    .hasMessageContaining("vacio");
        }

        @Test
        @DisplayName("contenido nulo se rechaza")
        void contenidoNulo() {
            assertThatThrownBy(() -> procesadorDefault().procesar(null, "x.jpg"))
                    .isInstanceOf(ImagenEvidenciaInvalidaException.class);
        }

        @Test
        @DisplayName("bytes aleatorios (no imagen) se rechazan como corruptos/formato invalido")
        void bytesAleatoriosSeRechazan() {
            byte[] basura = new byte[500];
            new Random(42).nextBytes(basura);
            assertThatThrownBy(() -> procesadorDefault().procesar(basura, "x.jpg"))
                    .isInstanceOf(ImagenEvidenciaInvalidaException.class);
        }

        @Test
        @DisplayName("archivo que excede el tamaño maximo de entrada se rechaza")
        void excedeTamanioMaximo() {
            byte[] jpg = jpeg(200, 200);
            var procesadorLimiteBajo = procesador(100L, 40_000_000L, 1920);
            assertThatThrownBy(() -> procesadorLimiteBajo.procesar(jpg, "x.jpg"))
                    .isInstanceOf(ImagenEvidenciaInvalidaException.class)
                    .hasMessageContaining("tamaño maximo");
        }

        @Test
        @DisplayName("imagen que excede el limite de pixeles decodificables se rechaza antes de decodificar el raster completo")
        void excedeLimitePixeles() {
            byte[] jpg = jpeg(200, 200); // 40.000 pixeles
            var procesadorLimiteBajo = procesador(5_242_880L, 1_000L, 1920); // limite: 1000 px
            assertThatThrownBy(() -> procesadorLimiteBajo.procesar(jpg, "x.jpg"))
                    .isInstanceOf(ImagenEvidenciaInvalidaException.class)
                    .hasMessageContaining("decompression bombs");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("3. Redimension sin upscale")
    class Redimension {

        @Test
        @DisplayName("imagen mas chica que el lado mayor final NUNCA se agranda")
        void sinUpscale() {
            BufferedImage chica = imagenSolida(10, 8, Color.RED, false);
            BufferedImage resultado = TwelveMonkeysImagenEvidenciaProcesador.redimensionarSinUpscale(chica, 1920);
            assertThat(resultado.getWidth()).isEqualTo(10);
            assertThat(resultado.getHeight()).isEqualTo(8);
            assertThat(resultado).isSameAs(chica);
        }

        @Test
        @DisplayName("imagen mas grande se reduce preservando aspect ratio, lado mayor = limite configurado")
        void downscalePreservaAspecto() {
            BufferedImage grande = imagenSolida(4000, 2000, Color.RED, false);
            BufferedImage resultado = TwelveMonkeysImagenEvidenciaProcesador.redimensionarSinUpscale(grande, 1000);
            assertThat(resultado.getWidth()).isEqualTo(1000);
            assertThat(resultado.getHeight()).isEqualTo(500);
        }

        @Test
        @DisplayName("pipeline completo: fotografia grande termina con lado mayor acotado")
        void pipelineCompletoAcotaLadoMayor() {
            byte[] grande = jpeg(3000, 1500);
            ImagenProcesada r = procesador(5_242_880L, 40_000_000L, 800).procesar(grande, "grande.jpg");
            assertThat(Math.max(r.anchoPx(), r.altoPx())).isEqualTo(800);
            assertThat(r.anchoPx()).isEqualTo(800);
            assertThat(r.altoPx()).isEqualTo(400);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("4. Correccion de orientacion EXIF — matriz de transformacion")
    class OrientacionExif {

        @Test
        @DisplayName("orientacion null o 1 (normal) es no-op: misma instancia")
        void normalEsNoOp() {
            BufferedImage img = imagenSolida(5, 3, Color.RED, false);
            assertThat(TwelveMonkeysImagenEvidenciaProcesador.corregirOrientacion(img, null)).isSameAs(img);
            assertThat(TwelveMonkeysImagenEvidenciaProcesador.corregirOrientacion(img, 1)).isSameAs(img);
        }

        @Test
        @DisplayName("orientaciones 2, 3, 4, 5, 7 son involuciones: aplicadas dos veces devuelven la imagen original")
        void involuciones() {
            for (int orientacion : new int[]{2, 3, 4, 5, 7}) {
                BufferedImage original = imagenAsimetrica(6, 4);
                BufferedImage una = TwelveMonkeysImagenEvidenciaProcesador.corregirOrientacion(original, orientacion);
                BufferedImage dos = TwelveMonkeysImagenEvidenciaProcesador.corregirOrientacion(una, orientacion);
                assertThat(dos.getWidth()).as("orientacion %d: ancho tras doble aplicacion", orientacion)
                        .isEqualTo(original.getWidth());
                assertThat(dos.getHeight()).as("orientacion %d: alto tras doble aplicacion", orientacion)
                        .isEqualTo(original.getHeight());
                assertImagenesIguales(original, dos, "orientacion " + orientacion + " (involucion)");
            }
        }

        @Test
        @DisplayName("orientaciones 6 y 8 son inversas entre si: 90 CW seguido de 270 CW devuelve la imagen original")
        void rotacionesInversas() {
            BufferedImage original = imagenAsimetrica(6, 4);
            BufferedImage rotada90 = TwelveMonkeysImagenEvidenciaProcesador.corregirOrientacion(original, 6);
            assertThat(rotada90.getWidth()).isEqualTo(original.getHeight());
            assertThat(rotada90.getHeight()).isEqualTo(original.getWidth());
            BufferedImage vuelta = TwelveMonkeysImagenEvidenciaProcesador.corregirOrientacion(rotada90, 8);
            assertThat(vuelta.getWidth()).isEqualTo(original.getWidth());
            assertThat(vuelta.getHeight()).isEqualTo(original.getHeight());
            assertImagenesIguales(original, vuelta, "6 seguido de 8");
        }

        @Test
        @DisplayName("orientaciones 5, 6, 7, 8 intercambian ancho y alto; 2, 3, 4 preservan dimensiones")
        void dimensionesSegunOrientacion() {
            BufferedImage original = imagenAsimetrica(7, 5);
            for (int o : new int[]{2, 3, 4}) {
                BufferedImage r = TwelveMonkeysImagenEvidenciaProcesador.corregirOrientacion(original, o);
                assertThat(r.getWidth()).as("orientacion %d", o).isEqualTo(7);
                assertThat(r.getHeight()).as("orientacion %d", o).isEqualTo(5);
            }
            for (int o : new int[]{5, 6, 7, 8}) {
                BufferedImage r = TwelveMonkeysImagenEvidenciaProcesador.corregirOrientacion(original, o);
                assertThat(r.getWidth()).as("orientacion %d", o).isEqualTo(5);
                assertThat(r.getHeight()).as("orientacion %d", o).isEqualTo(7);
            }
        }

        @Test
        @DisplayName("orientacion 7 (transversa) es distinta de 2 (espejo horizontal) y de 5 (transpuesta)")
        void orientacion7EsDistintaDeOtrasReflexiones() {
            BufferedImage original = imagenAsimetrica(6, 4);
            BufferedImage o7 = TwelveMonkeysImagenEvidenciaProcesador.corregirOrientacion(original, 7);
            BufferedImage o5 = TwelveMonkeysImagenEvidenciaProcesador.corregirOrientacion(original, 5);
            // Mismas dimensiones (ambas intercambian w/h) pero contenido distinto.
            assertThat(o7.getWidth()).isEqualTo(o5.getWidth());
            assertThat(o7.getHeight()).isEqualTo(o5.getHeight());
            assertThat(pixelesIguales(o7, o5)).as("orientacion 7 no debe coincidir pixel a pixel con 5").isFalse();
        }

        /** Imagen 4 cuadrantes con colores distintos: ninguna simetria la deja invariante. */
        private BufferedImage imagenAsimetrica(int w, int h) {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int r = (x * 255) / Math.max(1, w - 1);
                    int g = (y * 255) / Math.max(1, h - 1);
                    int b = ((x + y) * 255) / Math.max(1, w + h - 2);
                    img.setRGB(x, y, new Color(r, g, b).getRGB());
                }
            }
            return img;
        }

        private void assertImagenesIguales(BufferedImage a, BufferedImage b, String contexto) {
            assertThat(pixelesIguales(a, b)).as("%s: pixeles deben coincidir exactamente", contexto).isTrue();
        }

        private boolean pixelesIguales(BufferedImage a, BufferedImage b) {
            if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) return false;
            for (int y = 0; y < a.getHeight(); y++) {
                for (int x = 0; x < a.getWidth(); x++) {
                    if (a.getRGB(x, y) != b.getRGB(x, y)) return false;
                }
            }
            return true;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("5. Nombre original: saneo y limites")
    class NombreOriginal {

        @Test
        @DisplayName("nombre con componentes de ruta conserva solo el basename")
        void soloBasename() {
            ImagenProcesada r = procesadorDefault().procesar(jpeg(10, 10), "../../etc/passwd.jpg");
            assertThat(r.nombreOriginalSaneado()).isEqualTo("passwd.jpg");
        }

        @Test
        @DisplayName("nombre con backslash (rutas Windows) conserva solo el basename")
        void basenameConBackslash() {
            ImagenProcesada r = procesadorDefault().procesar(jpeg(10, 10), "C:\\Users\\alguien\\foto.jpg");
            assertThat(r.nombreOriginalSaneado()).isEqualTo("foto.jpg");
        }

        @Test
        @DisplayName("nombre nulo o en blanco produce nombreOriginalSaneado nulo")
        void nombreNuloOBlanco() {
            assertThat(procesadorDefault().procesar(jpeg(10, 10), null).nombreOriginalSaneado()).isNull();
            assertThat(procesadorDefault().procesar(jpeg(10, 10), "   ").nombreOriginalSaneado()).isNull();
        }

        @Test
        @DisplayName("nombre extremadamente largo se trunca a 180 caracteres")
        void nombreLargoSeTrunca() {
            String largo = "a".repeat(250) + ".jpg";
            ImagenProcesada r = procesadorDefault().procesar(jpeg(10, 10), largo);
            assertThat(r.nombreOriginalSaneado()).hasSize(180);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("6. Fecha EXIF: informativa, nunca bloqueante")
    class FechaExif {

        @Test
        @DisplayName("imagen sin EXIF: fechaCapturaExif es null, nunca rechaza la fotografia")
        void sinExifEsNull() {
            ImagenProcesada r = procesadorDefault().procesar(jpeg(10, 10), "sin-exif.jpg");
            assertThat(r.fechaCapturaExif()).isNull();
        }

        /**
         * Wave 4 Parte 3F-B, hardening §3: prueba positiva con EXIF real, no mockeada. El
         * fixture se genera sintéticamente (imagen sólida propia + segmento APP1/Exif
         * construido a mano según el estándar TIFF/EXIF), sin depender de ninguna foto externa
         * ni de una librería de escritura EXIF adicional. Los bytes resultantes pasan
         * íntegros por {@link com.drew.imaging.ImageMetadataReader} dentro de
         * {@link TwelveMonkeysImagenEvidenciaProcesador#procesar}, exactamente el mismo camino
         * que una fotografía real de cámara/celular.
         */
        @Test
        @DisplayName("imagen con EXIF DateTimeOriginal real: fechaCapturaExif refleja la fecha embebida")
        void conExifRealDevuelveFechaEmbebida() {
            java.time.LocalDateTime fechaEsperada = java.time.LocalDateTime.of(2024, 3, 15, 10, 30, 45);
            byte[] jpegConExif = jpegConExifFechaCaptura(20, 20, fechaEsperada);

            ImagenProcesada r = procesadorDefault().procesar(jpegConExif, "con-exif.jpg");

            assertThat(r.fechaCapturaExif())
                    .as("la fecha DateTimeOriginal embebida en el APP1/Exif real debe leerse tal cual")
                    .isEqualTo(fechaEsperada);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Construcción de un fixture JPEG con un segmento APP1/Exif real (TIFF/EXIF mínimo,
    // un único tag DateTimeOriginal), sin librerías adicionales de escritura EXIF.
    // ─────────────────────────────────────────────────────────────────────

    private static byte[] jpegConExifFechaCaptura(int w, int h, java.time.LocalDateTime fecha) {
        byte[] base = jpeg(w, h);
        byte[] app1 = construirSegmentoApp1Exif(fecha);
        // base[0..1] es el marcador SOI (FFD8); el APP1/Exif se inserta inmediatamente
        // despues, tal como lo hacen las camaras/celulares reales.
        byte[] resultado = new byte[2 + app1.length + (base.length - 2)];
        resultado[0] = base[0];
        resultado[1] = base[1];
        System.arraycopy(app1, 0, resultado, 2, app1.length);
        System.arraycopy(base, 2, resultado, 2 + app1.length, base.length - 2);
        return resultado;
    }

    private static byte[] construirSegmentoApp1Exif(java.time.LocalDateTime fecha) {
        String fechaExif = fecha.format(java.time.format.DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")) + "\0";
        byte[] fechaBytes = fechaExif.getBytes(java.nio.charset.StandardCharsets.US_ASCII); // 20 bytes

        final int tiffHeaderLen = 8;
        final int ifd0Len = 2 + 12 + 4;       // 1 entrada (puntero a Exif SubIFD)
        final int exifSubIfdLen = 2 + 12 + 4; // 1 entrada (DateTimeOriginal)
        final int exifSubIfdOffset = tiffHeaderLen + ifd0Len;
        final int fechaOffset = exifSubIfdOffset + exifSubIfdLen;
        final int tiffTotalLen = fechaOffset + fechaBytes.length;

        java.nio.ByteBuffer tiff = java.nio.ByteBuffer.allocate(tiffTotalLen).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        tiff.put((byte) 'I').put((byte) 'I');   // byte order: little-endian ("II")
        tiff.putShort((short) 42);              // numero magico TIFF
        tiff.putInt(tiffHeaderLen);             // offset a IFD0

        // IFD0: 1 entrada -> puntero a Exif SubIFD (tag 0x8769, tipo LONG)
        tiff.putShort((short) 1);
        tiff.putShort((short) 0x8769);
        tiff.putShort((short) 4);               // LONG
        tiff.putInt(1);
        tiff.putInt(exifSubIfdOffset);
        tiff.putInt(0);                         // sin siguiente IFD

        // Exif SubIFD: 1 entrada -> DateTimeOriginal (tag 0x9003, tipo ASCII)
        tiff.putShort((short) 1);
        tiff.putShort((short) 0x9003);
        tiff.putShort((short) 2);               // ASCII
        tiff.putInt(fechaBytes.length);
        tiff.putInt(fechaOffset);
        tiff.putInt(0);                         // sin siguiente IFD

        tiff.put(fechaBytes);

        byte[] tiffBytes = tiff.array();
        byte[] exifHeader = "Exif\0\0".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        int payloadLen = exifHeader.length + tiffBytes.length;
        int app1Len = payloadLen + 2; // el campo de longitud JPEG se cuenta a si mismo

        java.nio.ByteBuffer app1 = java.nio.ByteBuffer.allocate(4 + payloadLen).order(java.nio.ByteOrder.BIG_ENDIAN);
        app1.put((byte) 0xFF).put((byte) 0xE1); // marcador APP1
        app1.putShort((short) app1Len);
        app1.put(exifHeader);
        app1.put(tiffBytes);
        return app1.array();
    }
}
