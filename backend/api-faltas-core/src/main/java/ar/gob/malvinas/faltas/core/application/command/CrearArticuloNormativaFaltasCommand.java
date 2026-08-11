package ar.gob.malvinas.faltas.core.application.command;
import ar.gob.malvinas.faltas.core.domain.enums.CategoriaInfraccion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoInfraccion;
import java.time.LocalDate;

/**
 * Sin datos economicos: la formula del articulo (cantidad/monto, ordinaria y
 * pago voluntario) se crea por separado con {@code CrearTarifarioArticuloCommand}
 * sobre {@code fal_tarifario_unidad_faltas} (PROMPT-NOCTURNO-WAVE-04-PARTE-2 S7.3).
 */
public record CrearArticuloNormativaFaltasCommand(
    Long normativaId, String codigoArticulo, int versionArticulo, String nombreArticulo,
    String descripcionArticulo, TipoInfraccion tipoInfraccion, CategoriaInfraccion categoriaInfraccion,
    boolean siAdmiteMedidaPreventiva, LocalDate fhVigDesde, String idUserAlta) {}
