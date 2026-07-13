package ar.gob.malvinas.faltas.core.infrastructure.config;

import ar.gob.malvinas.faltas.core.application.port.ConfiguracionPlazosAdministrativos;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPlazoAdministrativo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuracion global de plazos administrativos, externalizable via application.yml.
 *
 * Prefijo: faltas.plazos
 * Propiedad: apelacion-dias-computables (default 30, rango 1..3650)
 *
 * El valor 30 es el default canonico del dominio. El unico lugar productivo autorizado
 * para contener este valor es esta clase y application.yml.
 */
@Component
@ConfigurationProperties(prefix = "faltas.plazos")
@Validated
public class PlazosAdministrativosProperties implements ConfiguracionPlazosAdministrativos {

    @Min(1)
    @Max(3650)
    private int apelacionDiasComputables = 30;

    @Override
    public int cantidadDiasComputables(TipoPlazoAdministrativo tipo) {
        if (tipo == null) throw new IllegalArgumentException("tipo es obligatorio");
        return switch (tipo) {
            case APELACION_FALLO -> apelacionDiasComputables;
            default -> throw new IllegalArgumentException("Tipo de plazo no configurado: " + tipo);
        };
    }

    public int getApelacionDiasComputables() { return apelacionDiasComputables; }

    public void setApelacionDiasComputables(int apelacionDiasComputables) {
        this.apelacionDiasComputables = apelacionDiasComputables;
    }
}
