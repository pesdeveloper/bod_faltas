package ar.gob.malvinas.faltas.core.web.dto;
import ar.gob.malvinas.faltas.core.domain.enums.CategoriaInfraccion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoInfraccion;
import java.time.LocalDate;

/**
 * No incluye datos economicos (cantidadUnidades/tipoUnidad/pagoVoluntario): la
 * formula del articulo se crea por separado en {@code fal_tarifario_unidad_faltas}
 * (PROMPT-NOCTURNO-WAVE-04-PARTE-2 S7.3).
 */
public record CrearArticuloNormativaFaltasRequest(
    String codigoArticulo,
    int versionArticulo,
    String nombreArticulo,
    String descripcionArticulo,
    TipoInfraccion tipoInfraccion,
    CategoriaInfraccion categoriaInfraccion,
    boolean siAdmiteMedidaPreventiva,
    LocalDate fhVigDesde,
    String idUserAlta
) {}
