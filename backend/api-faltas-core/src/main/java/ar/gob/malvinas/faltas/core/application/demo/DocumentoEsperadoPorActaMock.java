package ar.gob.malvinas.faltas.core.application.demo;

import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;

/**
 * Descriptor de un documento esperado para un acta mock funcional.
 *
 * Declara que tipo de documento deberia existir o generarse para el caso de uso cubierto
 * por una ActaMockFuncionalDefinicion especifica.
 *
 * No ejecuta nada. Solo declara la expectativa documental.
 *
 * Slice 8F-4B.
 */
public record DocumentoEsperadoPorActaMock(
        AccionDocumental accionDocumental,
        TipoDocu tipoDocu,
        boolean obligatorio,
        boolean requiereRedaccion,
        boolean requiereGeneracionMock,
        boolean requiereFirma,
        boolean requiereEmision,
        boolean requiereNotificacion,
        String momentoFuncional,
        String observacion
) {

    public static DocumentoEsperadoPorActaMock obligatorio(
            AccionDocumental accion,
            TipoDocu tipo,
            boolean requiereRedaccion,
            boolean requiereGeneracionMock,
            boolean requiereFirma,
            boolean requiereEmision,
            boolean requiereNotificacion,
            String momentoFuncional,
            String observacion) {
        return new DocumentoEsperadoPorActaMock(
                accion, tipo, true,
                requiereRedaccion, requiereGeneracionMock,
                requiereFirma, requiereEmision, requiereNotificacion,
                momentoFuncional, observacion);
    }

    public static DocumentoEsperadoPorActaMock opcional(
            AccionDocumental accion,
            TipoDocu tipo,
            boolean requiereRedaccion,
            boolean requiereGeneracionMock,
            boolean requiereFirma,
            boolean requiereEmision,
            boolean requiereNotificacion,
            String momentoFuncional,
            String observacion) {
        return new DocumentoEsperadoPorActaMock(
                accion, tipo, false,
                requiereRedaccion, requiereGeneracionMock,
                requiereFirma, requiereEmision, requiereNotificacion,
                momentoFuncional, observacion);
    }
}
