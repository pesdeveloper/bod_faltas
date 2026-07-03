package ar.gob.malvinas.faltas.core.web.dto;
import ar.gob.malvinas.faltas.core.domain.enums.TipoUnidad;
import ar.gob.malvinas.faltas.core.domain.model.FalArticuloNormativaFaltas;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
public record ArticuloNormativaFaltasResponse(
    Long id,
    Long normativaId,
    String codigoArticulo,
    int versionArticulo,
    String nombreArticulo,
    String descripcionArticulo,
    BigDecimal cantidadUnidades,
    TipoUnidad tipoUnidad,
    boolean siTienePagoVoluntario,
    BigDecimal cantidadUnidadesPagoVoluntario,
    TipoUnidad tipoUnidadPagoVoluntario,
    boolean siActivo,
    LocalDate fhVigDesde,
    LocalDate fhVigHasta,
    LocalDateTime fhAlta
) {
    public static ArticuloNormativaFaltasResponse de(FalArticuloNormativaFaltas a) {
        return new ArticuloNormativaFaltasResponse(
            a.getId(), a.getNormativaId(), a.getCodigoArticulo(), a.getVersionArticulo(),
            a.getNombreArticulo(), a.getDescripcionArticulo(),
            a.getCantidadUnidades(), a.getTipoUnidad(),
            a.isSiTienePagoVoluntario(), a.getCantidadUnidadesPagoVoluntario(),
            a.getTipoUnidadPagoVoluntario(), a.isSiActivo(),
            a.getFhVigDesde(), a.getFhVigHasta(), a.getFhAlta());
    }
}
