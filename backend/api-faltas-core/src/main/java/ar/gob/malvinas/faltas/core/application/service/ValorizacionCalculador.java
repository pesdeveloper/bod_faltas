package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.ModoValorizacionItem;
import ar.gob.malvinas.faltas.core.domain.enums.TipoUnidadFaltas;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalTarifarioUnidadFaltas;
import ar.gob.malvinas.faltas.core.domain.model.FalValorUnidadFaltas;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Componente reusable de calculo de valorizacion. No persiste nada.
 *
 * Calcula el monto ordinario y de pago voluntario de UN articulo a partir de
 * su formula ({@link FalTarifarioUnidadFaltas}) y, si corresponde, el valor de
 * unidad vigente ({@link FalValorUnidadFaltas}). Resuelve el modo AUTOMATICO y
 * los modos manuales (por unidades o por monto).
 *
 * Se reutiliza desde el alta integral (Wave 4 PARTE-2) y queda preparado para
 * la vista previa de valorizacion y el recalculo en ENRI (Wave 5), sin
 * incrustar la formula dentro de {@code ActaService} (PROMPT-NOCTURNO-WAVE-04-PARTE-2 S7.9).
 */
@Component
public class ValorizacionCalculador {

    /** Resultado del calculo de una dimension (ordinaria o pago voluntario) de un articulo. */
    public record ResultadoDimension(BigDecimal monto, BigDecimal cantidadUnidadesAplicada, BigDecimal valorUnidadAplicado) {}

    /**
     * Calcula la dimension ordinaria en modo AUTOMATICO a partir de la formula del articulo.
     * Para SALARIO/UNIDAD_FIJA requiere {@code valorUnidad} vigente. Para MONTO, {@code valorUnidad}
     * puede ser null (el monto fijo ya es el valor final).
     */
    public ResultadoDimension calcularOrdinarioAutomatico(FalTarifarioUnidadFaltas tarifario, FalValorUnidadFaltas valorUnidad) {
        return calcularAutomatico(tarifario.getTipoUnidad(), tarifario.getCantidadUnidadesOrdinaria(),
                tarifario.getMontoFijoOrdinario(), valorUnidad);
    }

    /**
     * Calcula la dimension de pago voluntario en modo AUTOMATICO. Falla si el articulo no admite
     * pago voluntario: el llamador debe verificar {@code siAdmitePagoVoluntario} antes de invocar.
     */
    public ResultadoDimension calcularPagoVoluntarioAutomatico(FalTarifarioUnidadFaltas tarifario, FalValorUnidadFaltas valorUnidad) {
        if (!tarifario.isSiAdmitePagoVoluntario()) {
            throw new PrecondicionVioladaException(
                    "El tarifario id=" + tarifario.getId() + " no admite pago voluntario.");
        }
        return calcularAutomatico(tarifario.getTipoUnidad(), tarifario.getCantidadUnidadesPagoVoluntario(),
                tarifario.getMontoFijoPagoVoluntario(), valorUnidad);
    }

    private ResultadoDimension calcularAutomatico(
            TipoUnidadFaltas tipoUnidad, BigDecimal cantidadUnidades, BigDecimal montoFijo, FalValorUnidadFaltas valorUnidad) {
        if (tipoUnidad == TipoUnidadFaltas.MONTO) {
            return new ResultadoDimension(escala2(montoFijo), null, null);
        }
        if (valorUnidad == null) {
            throw new PrecondicionVioladaException(
                    "No hay valor de unidad vigente para tipoUnidad=" + tipoUnidad + ". Valorizacion no puede calcularse.");
        }
        if (valorUnidad.getTipoUnidad() != tipoUnidad) {
            throw new IllegalArgumentException("valorUnidad.tipoUnidad no coincide con tarifario.tipoUnidad");
        }
        BigDecimal monto = cantidadUnidades.multiply(valorUnidad.getValorUnidad()).setScale(2, RoundingMode.HALF_UP);
        return new ResultadoDimension(monto, cantidadUnidades, valorUnidad.getValorUnidad());
    }

    /** Calcula una dimension MANUAL_MONTO: el monto es el informado directamente. */
    public ResultadoDimension calcularManualMonto(BigDecimal montoManual) {
        if (montoManual == null || montoManual.compareTo(BigDecimal.ZERO) <= 0)
            throw new PrecondicionVioladaException("montoManual debe ser mayor a cero.");
        return new ResultadoDimension(escala2(montoManual), null, null);
    }

    /**
     * Calcula una dimension MANUAL_UNIDADES: cantidad informada por el usuario x valor de unidad
     * vigente (no aplica a MONTO: para MONTO el modo manual correcto es MANUAL_MONTO).
     */
    public ResultadoDimension calcularManualUnidades(BigDecimal cantidadManual, FalValorUnidadFaltas valorUnidad) {
        if (cantidadManual == null || cantidadManual.compareTo(BigDecimal.ZERO) <= 0)
            throw new PrecondicionVioladaException("cantidadManual debe ser mayor a cero.");
        if (valorUnidad == null)
            throw new PrecondicionVioladaException("valorUnidad es obligatorio para MANUAL_UNIDADES.");
        BigDecimal monto = cantidadManual.multiply(valorUnidad.getValorUnidad()).setScale(2, RoundingMode.HALF_UP);
        return new ResultadoDimension(monto, cantidadManual, valorUnidad.getValorUnidad());
    }

    /** Resuelve una dimension segun el modo, delegando en el metodo de calculo correspondiente. */
    public ResultadoDimension calcularOrdinaria(
            ModoValorizacionItem modo, FalTarifarioUnidadFaltas tarifario, FalValorUnidadFaltas valorUnidad,
            BigDecimal cantidadManual, BigDecimal montoManual) {
        return switch (modo) {
            case AUTOMATICA -> calcularOrdinarioAutomatico(tarifario, valorUnidad);
            case MANUAL_UNIDADES -> calcularManualUnidades(cantidadManual, valorUnidad);
            case MANUAL_MONTO -> calcularManualMonto(montoManual);
        };
    }

    public ResultadoDimension calcularPagoVoluntario(
            ModoValorizacionItem modo, FalTarifarioUnidadFaltas tarifario, FalValorUnidadFaltas valorUnidad,
            BigDecimal cantidadManual, BigDecimal montoManual) {
        return switch (modo) {
            case AUTOMATICA -> calcularPagoVoluntarioAutomatico(tarifario, valorUnidad);
            case MANUAL_UNIDADES -> calcularManualUnidades(cantidadManual, valorUnidad);
            case MANUAL_MONTO -> calcularManualMonto(montoManual);
        };
    }

    private static BigDecimal escala2(BigDecimal v) { return v == null ? null : v.setScale(2, RoundingMode.HALF_UP); }
}
