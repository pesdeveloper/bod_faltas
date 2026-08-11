package ar.gob.malvinas.faltas.core.infrastructure.imagen;

import ar.gob.malvinas.faltas.core.application.port.ImagenEvidenciaProcesador;
import ar.gob.malvinas.faltas.core.application.port.ImagenProcesada;
import ar.gob.malvinas.faltas.core.domain.exception.ImagenEvidenciaInvalidaException;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

/**
 * Implementacion productiva de {@link ImagenEvidenciaProcesador} (Wave 4 Parte
 * 3D S7).
 *
 * <h2>Formato final y decisiones tecnicas (documentadas por exigencia del prompt)</h2>
 * <ul>
 *   <li><b>Formato final:</b> siempre JPEG. Es el unico formato en el que
 *       {@code javax.imageio} incluye codificador nativo en el JDK sin
 *       dependencias adicionales, y es el mas compacto para fotografias reales
 *       (sin transparencia util). No se conserva el formato de entrada.</li>
 *   <li><b>Calidad:</b> configurable, {@code faltas.fotografias.calidad-jpeg}
 *       (default 0.85), via {@code JPEGImageWriteParam.setCompressionQuality}.</li>
 *   <li><b>Dimensiones maximas:</b> {@code faltas.fotografias.lado-mayor-final-px}
 *       (default 1920). El lado mayor se recorta a ese valor preservando
 *       aspect ratio; nunca se hace upscale de una imagen mas chica.</li>
 *   <li><b>Transparencia (PNG/WEBP):</b> JPEG no soporta canal alfa. Toda
 *       imagen con transparencia se aplana sobre fondo blanco antes de
 *       codificar. Es una decision de producto (fotografias de actas, no
 *       arte con transparencia funcional) documentada aca por no existir una
 *       decision previa en la spec.</li>
 *   <li><b>Perfiles de color:</b> se decodifica con el {@code ColorModel} que
 *       entrega el reader de {@code javax.imageio} (conversion a sRGB
 *       implicita de la plataforma); no se preserva ningun perfil ICC
 *       embebido, consistente con "eliminar metadata no necesaria".</li>
 *   <li><b>Librerias:</b>
 *     <ul>
 *       <li>JPEG y PNG: decoder/encoder nativos de {@code javax.imageio} (JDK 21),
 *           sin dependencia adicional.</li>
 *       <li>WEBP: {@code com.twelvemonkeys.imageio:imageio-webp} (BSD-3-Clause,
 *           TwelveMonkeys) — unico plugin de decodificacion WEBP disponible
 *           para {@code javax.imageio} en el JDK estandar. Solo decodifica: el
 *           formato final sigue siendo JPEG.</li>
 *       <li>EXIF (orientacion + fecha de captura): {@code com.drewnoakes:metadata-extractor}
 *           (Apache-2.0) — libreria Java de facto para EXIF, se usa
 *           exclusivamente para <b>leer</b> el EXIF de entrada antes de
 *           descartarlo; el archivo final nunca lleva EXIF.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h2>Proteccion contra decompression bombs</h2>
 * <p>Antes de decodificar el raster completo, se lee {@code width}/{@code height}
 * del {@link ImageReader} (operacion barata, no decodifica pixeles) y se
 * rechaza si {@code width * height} excede {@code faltas.fotografias.max-pixeles-decodificables}.
 * Solo despues de esa verificacion se invoca {@code reader.read(0)}.
 */
@Component
public class TwelveMonkeysImagenEvidenciaProcesador implements ImagenEvidenciaProcesador {

    private static final Set<String> FORMATOS_ACEPTADOS = Set.of("JPEG", "PNG", "WEBP");

    private final long maxBytesEntrada;
    private final long maxPixelesDecodificables;
    private final int ladoMayorFinalPx;
    private final float calidadJpeg;

    public TwelveMonkeysImagenEvidenciaProcesador(
            @Value("${faltas.fotografias.max-bytes-entrada:5242880}") long maxBytesEntrada,
            @Value("${faltas.fotografias.max-pixeles-decodificables:40000000}") long maxPixelesDecodificables,
            @Value("${faltas.fotografias.lado-mayor-final-px:1920}") int ladoMayorFinalPx,
            @Value("${faltas.fotografias.calidad-jpeg:0.85}") float calidadJpeg) {
        this.maxBytesEntrada = maxBytesEntrada;
        this.maxPixelesDecodificables = maxPixelesDecodificables;
        this.ladoMayorFinalPx = ladoMayorFinalPx;
        this.calidadJpeg = calidadJpeg;
    }

    @Override
    public ImagenProcesada procesar(byte[] contenidoOriginal, String nombreOriginalDeclarado) {
        if (contenidoOriginal == null || contenidoOriginal.length == 0) {
            throw new ImagenEvidenciaInvalidaException("El archivo esta vacio.");
        }
        if (contenidoOriginal.length > maxBytesEntrada) {
            throw new ImagenEvidenciaInvalidaException(
                    "El archivo excede el tamaño maximo permitido de " + maxBytesEntrada + " bytes.");
        }

        ImageReader reader = localizarReaderValido(contenidoOriginal);
        BufferedImage decodificada;
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(contenidoOriginal))) {
            reader.setInput(iis, true, true);
            verificarLimitePixeles(reader);
            decodificada = reader.read(0, defaultReadParam(reader));
        } catch (ImagenEvidenciaInvalidaException limiteExcedido) {
            throw limiteExcedido;
        } catch (IOException | RuntimeException e) {
            throw new ImagenEvidenciaInvalidaException("No se pudo decodificar la imagen: contenido corrupto o invalido.", e);
        } finally {
            reader.dispose();
        }
        if (decodificada == null) {
            throw new ImagenEvidenciaInvalidaException("El decoder no produjo una imagen valida.");
        }

        Integer orientacionExif = null;
        LocalDateTime fechaCaptura = null;
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(contenidoOriginal));
            orientacionExif = leerOrientacion(metadata);
            fechaCaptura = leerFechaCaptura(metadata);
        } catch (ImageProcessingException | IOException | RuntimeException exifNoDisponible) {
            // EXIF es informativo: ausente o invalido nunca rechaza la fotografia (Parte 3D S5.7).
        }

        BufferedImage corregida = corregirOrientacion(decodificada, orientacionExif);
        BufferedImage redimensionada = redimensionarSinUpscale(corregida, ladoMayorFinalPx);
        BufferedImage aplanada = aplanarSobreFondoBlancoSiTieneAlfa(redimensionada);

        byte[] bytesFinal = recomprimirJpeg(aplanada, calidadJpeg);
        String sha256 = sha256Hex(bytesFinal);
        String nombreSaneado = sanearNombreOriginal(nombreOriginalDeclarado);

        return new ImagenProcesada(
                bytesFinal, "image/jpeg", aplanada.getWidth(), aplanada.getHeight(),
                sha256, fechaCaptura, nombreSaneado);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Validacion de formato real (magic bytes / decodificacion), no extension
    // ─────────────────────────────────────────────────────────────────────

    private ImageReader localizarReaderValido(byte[] contenido) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(contenido))) {
            if (iis == null) {
                throw new ImagenEvidenciaInvalidaException("No se pudo interpretar el contenido como imagen.");
            }
            Iterator<ImageReader> candidatos = ImageIO.getImageReaders(iis);
            while (candidatos.hasNext()) {
                ImageReader candidato = candidatos.next();
                String formato = candidato.getFormatName();
                if (formato != null && FORMATOS_ACEPTADOS.contains(formato.toUpperCase(Locale.ROOT))) {
                    return candidato;
                }
                candidato.dispose();
            }
        } catch (IOException e) {
            throw new ImagenEvidenciaInvalidaException("No se pudo abrir el contenido para deteccion de formato.", e);
        }
        throw new ImagenEvidenciaInvalidaException(
                "Formato no soportado. Solo se aceptan JPEG, PNG y WEBP validados por contenido real.");
    }

    private static ImageReadParam defaultReadParam(ImageReader reader) {
        try {
            return reader.getDefaultReadParam();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void verificarLimitePixeles(ImageReader reader) throws IOException {
        int ancho = reader.getWidth(0);
        int alto = reader.getHeight(0);
        if (ancho <= 0 || alto <= 0) {
            throw new ImagenEvidenciaInvalidaException("Dimensiones de imagen invalidas.");
        }
        long pixeles = (long) ancho * (long) alto;
        if (pixeles > maxPixelesDecodificables) {
            throw new ImagenEvidenciaInvalidaException(
                    "La imagen declara " + ancho + "x" + alto + " (" + pixeles
                            + " pixeles), por encima del limite seguro de decodificacion de "
                            + maxPixelesDecodificables + " pixeles (proteccion contra decompression bombs).");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // EXIF: orientacion + fecha de captura (informativa, nunca autoritativa)
    // ─────────────────────────────────────────────────────────────────────

    private static Integer leerOrientacion(Metadata metadata) {
        ExifIFD0Directory dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        if (dir == null || !dir.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
            return null;
        }
        try {
            return dir.getInt(ExifIFD0Directory.TAG_ORIENTATION);
        } catch (com.drew.metadata.MetadataException e) {
            return null;
        }
    }

    private static LocalDateTime leerFechaCaptura(Metadata metadata) {
        ExifSubIFDDirectory dir = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        if (dir == null) {
            return null;
        }
        Date fecha = dir.getDateOriginal();
        if (fecha == null) {
            return null;
        }
        return LocalDateTime.ofInstant(fecha.toInstant(), ZoneId.systemDefault());
    }

    /**
     * Aplica la correccion de orientacion EXIF estandar (valores 1..8). Sin
     * tag de orientacion o valor 1 (normal): no-op. Dimensiones se
     * intercambian en las cuatro rotaciones de 90/270 grados (5, 6, 7, 8).
     */
    /** Visibilidad de paquete a proposito: testeada directamente (matriz de transformacion EXIF). */
    static BufferedImage corregirOrientacion(BufferedImage origen, Integer orientacion) {
        if (orientacion == null || orientacion == 1) {
            return origen;
        }
        int w = origen.getWidth();
        int h = origen.getHeight();
        AffineTransform t = new AffineTransform();
        int destW = w;
        int destH = h;
        switch (orientacion) {
            case 2 -> { t.translate(w, 0); t.scale(-1, 1); }
            case 3 -> { t.translate(w, h); t.rotate(Math.PI); }
            case 4 -> { t.translate(0, h); t.scale(1, -1); }
            case 5 -> { destW = h; destH = w; t.rotate(Math.PI / 2); t.scale(1, -1); }
            case 6 -> { destW = h; destH = w; t.translate(h, 0); t.rotate(Math.PI / 2); }
            case 7 -> { destW = h; destH = w; t.scale(-1, 1); t.translate(-h, 0); t.translate(0, w); t.rotate(3 * Math.PI / 2); }
            case 8 -> { destW = h; destH = w; t.translate(0, w); t.rotate(3 * Math.PI / 2); }
            default -> { return origen; }
        }
        BufferedImage destino = new BufferedImage(destW, destH, tipoConAlfaSeguro(origen));
        Graphics2D g = destino.createGraphics();
        aplicarHintsCalidad(g);
        g.drawImage(origen, t, null);
        g.dispose();
        return destino;
    }

    /** Visibilidad de paquete a proposito: testeada directamente. */
    static BufferedImage redimensionarSinUpscale(BufferedImage origen, int ladoMayorFinalPx) {
        int w = origen.getWidth();
        int h = origen.getHeight();
        int ladoMayor = Math.max(w, h);
        if (ladoMayor <= ladoMayorFinalPx) {
            return origen; // nunca upscale
        }
        double factor = ladoMayorFinalPx / (double) ladoMayor;
        int nuevoW = Math.max(1, Math.round((float) (w * factor)));
        int nuevoH = Math.max(1, Math.round((float) (h * factor)));
        BufferedImage destino = new BufferedImage(nuevoW, nuevoH, tipoConAlfaSeguro(origen));
        Graphics2D g = destino.createGraphics();
        aplicarHintsCalidad(g);
        g.drawImage(origen, 0, 0, nuevoW, nuevoH, null);
        g.dispose();
        return destino;
    }

    private static BufferedImage aplanarSobreFondoBlancoSiTieneAlfa(BufferedImage origen) {
        if (!origen.getColorModel().hasAlpha()) {
            return origen;
        }
        BufferedImage destino = new BufferedImage(origen.getWidth(), origen.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = destino.createGraphics();
        aplicarHintsCalidad(g);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, origen.getWidth(), origen.getHeight());
        g.drawImage(origen, 0, 0, null);
        g.dispose();
        return destino;
    }

    private static int tipoConAlfaSeguro(BufferedImage origen) {
        return origen.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
    }

    private static void aplicarHintsCalidad(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    private static byte[] recomprimirJpeg(BufferedImage imagen, float calidad) {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("No hay ImageWriter JPEG disponible en el JDK.");
        }
        ImageWriter writer = writers.next();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(Math.max(0.0f, Math.min(1.0f, calidad)));
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(baos)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(imagen, null, null), param);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new ImagenEvidenciaInvalidaException("No se pudo recomprimir la imagen a JPEG.", e);
        } finally {
            writer.dispose();
        }
    }

    private static String sha256Hex(byte[] datos) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(datos);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM.", e);
        }
    }

    /**
     * Sanea el nombre de archivo declarado por el cliente: solo el basename
     * (sin componentes de ruta), sin caracteres de control, truncado a 180
     * caracteres (VARCHAR(180) fisico). Nunca se usa como ruta real.
     */
    private static String sanearNombreOriginal(String declarado) {
        if (declarado == null || declarado.isBlank()) {
            return null;
        }
        String basename = declarado.replace('\\', '/');
        int barra = basename.lastIndexOf('/');
        if (barra >= 0) {
            basename = basename.substring(barra + 1);
        }
        basename = basename.replaceAll("[\\p{Cntrl}]", "").strip();
        if (basename.isEmpty()) {
            return null;
        }
        return basename.length() > 180 ? basename.substring(0, 180) : basename;
    }
}
