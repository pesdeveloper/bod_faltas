package ar.gob.malvinas.faltas.core.application.result;

/**
 * Resultado de un paso individual dentro de la ejecucion funcional de un caso de uso.
 *
 * Representa la ejecucion de un comando/servicio dentro del flujo del runner.
 *
 * Slice 8F-4C.
 */
public record PasoFuncionalResultado(
        int orden,
        String nombrePaso,
        boolean exitoso,
        String detalle,
        String eventoGenerado,
        String estadoPosterior
) {

    public static PasoFuncionalResultado ok(
            int orden, String nombre, String evento, String estado) {
        return new PasoFuncionalResultado(orden, nombre, true, null, evento, estado);
    }

    public static PasoFuncionalResultado ok(
            int orden, String nombre, String evento, String estado, String detalle) {
        return new PasoFuncionalResultado(orden, nombre, true, detalle, evento, estado);
    }

    public static PasoFuncionalResultado fallido(
            int orden, String nombre, String motivoFallo) {
        return new PasoFuncionalResultado(orden, nombre, false, motivoFallo, null, null);
    }
}
