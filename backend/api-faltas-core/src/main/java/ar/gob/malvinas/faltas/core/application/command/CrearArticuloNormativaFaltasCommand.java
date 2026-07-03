package ar.gob.malvinas.faltas.core.application.command;
import ar.gob.malvinas.faltas.core.domain.enums.TipoUnidad;
import java.math.BigDecimal;
import java.time.LocalDate;
public record CrearArticuloNormativaFaltasCommand(
    Long normativaId, String codigoArticulo, int versionArticulo, String nombreArticulo,
    String descripcionArticulo, BigDecimal cantidadUnidades, TipoUnidad tipoUnidad,
    boolean siTienePagoVoluntario, BigDecimal cantidadUnidadesPagoVoluntario,
    TipoUnidad tipoUnidadPagoVoluntario, LocalDate fhVigDesde, String idUserAlta) {}
