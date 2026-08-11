package ar.gob.malvinas.faltas.core.web.dto;
import ar.gob.malvinas.faltas.core.domain.enums.CategoriaInfraccion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoInfraccion;
import ar.gob.malvinas.faltas.core.domain.model.FalArticuloNormativaFaltas;
import java.time.LocalDate;
import java.time.LocalDateTime;
public record ArticuloNormativaFaltasResponse(
    Long id,
    Long normativaId,
    String codigoArticulo,
    int versionArticulo,
    String nombreArticulo,
    String descripcionArticulo,
    TipoInfraccion tipoInfraccion,
    CategoriaInfraccion categoriaInfraccion,
    boolean siAdmiteMedidaPreventiva,
    boolean siActivo,
    LocalDate fhVigDesde,
    LocalDate fhVigHasta,
    LocalDateTime fhAlta
) {
    public static ArticuloNormativaFaltasResponse de(FalArticuloNormativaFaltas a) {
        return new ArticuloNormativaFaltasResponse(
            a.getId(), a.getNormativaId(), a.getCodigoArticulo(), a.getVersionArticulo(),
            a.getNombreArticulo(), a.getDescripcionArticulo(),
            a.getTipoInfraccion(), a.getCategoriaInfraccion(), a.isSiAdmiteMedidaPreventiva(),
            a.isSiActivo(), a.getFhVigDesde(), a.getFhVigHasta(), a.getFhAlta());
    }
}
