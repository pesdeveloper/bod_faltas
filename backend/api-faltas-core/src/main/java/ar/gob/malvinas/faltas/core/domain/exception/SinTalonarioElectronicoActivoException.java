package ar.gob.malvinas.faltas.core.domain.exception;

/**
 * Se lanza cuando no existe ningun talonario ACTA ELECTRONICO activo compatible con la
 * dependencia consultada ({@code DECISION-NUM-UNICO-ACTIVO-01}, Wave 4 Parte 3B §9.2: "0
 * activos").
 *
 * <p>Subtipo dedicado de {@link PrecondicionVioladaException} que identifica este caso
 * especifico frente a una inconsistencia real (2+ talonarios activos) o a un talonario
 * agotado. Desde Wave 4 Parte 3C ({@code DECISION-ACTA-TALONARIO-REQUIRED-01}, prompt §7), los
 * tres casos rechazan el alta completa por igual: {@code ActaService.labrar} ya no captura
 * este subtipo para tolerar un alta sin numerar. No existe alta canonica con
 * {@code nro_acta = NULL} por falta de configuracion electronica.
 */
public class SinTalonarioElectronicoActivoException extends PrecondicionVioladaException {
    public SinTalonarioElectronicoActivoException(String mensaje) {
        super(mensaje);
    }
}
