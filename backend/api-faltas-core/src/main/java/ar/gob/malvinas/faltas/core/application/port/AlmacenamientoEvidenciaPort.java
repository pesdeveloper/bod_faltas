package ar.gob.malvinas.faltas.core.application.port;

/**
 * Puerto de almacenamiento fisico de archivos de evidencia (Wave 4 Parte 3D).
 *
 * <p>Abstrae el filesystem: ningun caso de uso ni servicio de aplicacion depende
 * directamente de {@code java.nio.file}. La unica implementacion productiva es
 * un adapter filesystem configurable ({@code FilesystemEvidenciaAlmacenamientoAdapter}).
 * Fakes/InMemory de este puerto son admitidos solo en {@code src/test}
 * (Parte 3D S9: prohibido agregar nuevos adapters productivos InMemory).
 *
 * <h2>Contrato de dos fases (temporal -&gt; final)</h2>
 *
 * <pre>
 * 1. escribirTemporal   -&gt; graba bytes en un area temporal, fuera de toda
 *                          clave visible/servible; no requiere transaccion.
 * 2. promover           -&gt; mueve el archivo temporal a su clave final,
 *                          atomicamente cuando el filesystem lo permite.
 *                          Se invoca DENTRO de la transaccion del caso de uso,
 *                          antes del commit: si el commit no llega a ocurrir
 *                          (rollback posterior), el llamador debe compensar
 *                          con {@link #eliminarFinal}.
 * 3. eliminarTemporal / eliminarFinal -&gt; compensacion idempotente. Eliminar
 *                          una clave que ya no existe no es un error.
 * </pre>
 *
 * <p>Las claves ({@code String}) son siempre relativas a la raiz configurada,
 * opacas (UUID, nunca datos de negocio ni el nombre de archivo del cliente) e
 * inmunes a path traversal porque el propio adapter las genera: ningun
 * llamador construye una clave a partir de un valor de entrada.
 */
public interface AlmacenamientoEvidenciaPort {

    /**
     * Genera una clave temporal nueva y escribe {@code contenido} bajo ella,
     * con fsync antes de devolver el control (Parte 3D S10: "fsync/close
     * adecuado segun plataforma").
     *
     * @return clave temporal opaca, nunca {@code null}
     */
    String escribirTemporal(byte[] contenido);

    /**
     * Genera una clave final nueva (opaca, relativa, sin datos sensibles) para
     * la extension dada (sin el punto, p.ej. {@code "jpg"}). No escribe nada:
     * solo reserva el nombre que {@link #promover} usara como destino.
     */
    String generarClaveFinal(String extension);

    /**
     * Promueve el archivo temporal a la clave final, atomicamente cuando el
     * filesystem lo permite (mismo volumen: {@code ATOMIC_MOVE}); si no lo
     * permite, copia + fsync + borrado del temporal, nunca deja el original
     * temporal y el destino coexistiendo con exito parcial silencioso.
     *
     * <p>No sobrescribe un destino existente: {@code claveFinal} es siempre
     * una clave nueva generada por {@link #generarClaveFinal}, de modo que una
     * colision indicaria un error de programacion, nunca una operacion valida.
     *
     * @throws IllegalStateException si el archivo temporal no existe o si el
     *         destino ya existe
     */
    void promover(String claveTemporal, String claveFinal);

    /**
     * Compensacion idempotente: elimina el archivo temporal si existe. No
     * lanza si ya fue eliminado o promovido.
     */
    void eliminarTemporal(String claveTemporal);

    /**
     * Compensacion idempotente: elimina el archivo final si existe. Se usa
     * exclusivamente para revertir una promocion cuya transaccion de metadata
     * no llego a confirmar (rollback posterior a la promocion).
     */
    void eliminarFinal(String claveFinal);

    /**
     * Lee el contenido completo del archivo final. Los archivos de evidencia
     * fotografica estan acotados por los limites de tamaño del pipeline (Parte
     * 3D S6): cargar el contenido completo en memoria es aceptable a esa
     * escala.
     *
     * @throws ar.gob.malvinas.faltas.core.domain.exception.EvidenciaArchivoNoEncontradoException
     *         si la clave no corresponde a ningun archivo final existente
     */
    byte[] leerFinal(String claveFinal);

    /**
     * @return {@code true} si existe un archivo final bajo esa clave.
     */
    boolean existeFinal(String claveFinal);
}
