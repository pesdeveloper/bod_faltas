package ar.gob.malvinas.faltas.core.domain.exception;

import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;

public class PlantillaDefaultNoEncontradaException extends RuntimeException {
    public PlantillaDefaultNoEncontradaException(AccionDocumental accion) {
        super("No se encontro plantilla default activa y vigente para accion: " + accion);
    }
    public PlantillaDefaultNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
