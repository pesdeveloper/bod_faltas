package ar.gob.malvinas.faltas.core.support;

import ar.gob.malvinas.faltas.core.application.command.CrearPoliticaNumeracionCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearTalonarioAmbitoCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearTalonarioCommand;
import ar.gob.malvinas.faltas.core.application.service.TalonarioService;
import ar.gob.malvinas.faltas.core.domain.enums.AlcanceTalonario;
import ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoTalonario;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonario;

import java.time.LocalDate;

/**
 * Utilidad de seed compartida para IT reales contra MariaDB que invocan
 * {@code ActaService.labrar} (Wave 4 Parte 3C §10).
 *
 * <p>Desde {@code DECISION-ACTA-TALONARIO-REQUIRED-01} el alta rechaza completa cuando la
 * dependencia no tiene exactamente un talonario ACTA ELECTRONICO activo aplicable. Los IT reales
 * que labran actas deben seedear ese talonario explícitamente antes de invocar el caso de uso;
 * ya no hay tolerancia de "0 activos".
 *
 * <p>El ámbito se crea siempre con {@code alcance=DEPENDENCIA}, nunca {@code GLOBAL}: cada IT
 * seedea su propia dependencia nueva por método/clase (PK autoincremental, nunca reutilizada), de
 * modo que un ámbito por dependencia es automáticamente único y aislado, sin colisionar entre
 * tests ni entre clases de IT distintas, y sin competir con
 * {@code DECISION-NUM-UNICO-ACTIVO-POR-DEPENDENCIA-01} (máximo un talonario ELECTRONICO activo
 * por dependencia): cada dependencia de test es una fila nueva, nunca comparte ámbito con otra.
 *
 * <p>{@code codigo} (num_politica/num_talonario, {@code VARCHAR(8)}) y {@code nombreSecuencia}
 * ({@code VARCHAR(64)}, único) se derivan del PK autoincremental de la dependencia: nunca se
 * repite entre ejecuciones, incluso en corridas sucesivas de la suite completa.
 */
public final class TalonarioElectronicoITSeedSupport {

    private TalonarioElectronicoITSeedSupport() {}

    /**
     * Crea política + talonario ELECTRONICO + ámbito DEPENDENCIA, exactamente uno, activo, vigente
     * desde {@code vigenciaDesde} sin fecha de fin, aplicable a cualquier {@code tipoActa}
     * (tipoActa del ámbito {@code null}).
     *
     * @return el id del talonario creado.
     */
    public static Long seedTalonarioElectronicoActivo(
            TalonarioService talonarioService, Long idDependencia, short verDependencia,
            LocalDate vigenciaDesde) {
        String sufijo = sufijoCorto(idDependencia);
        var politica = talonarioService.crearPolitica(new CrearPoliticaNumeracionCommand(
                ("P" + sufijo), "Politica IT dependencia " + idDependencia,
                ClaseNumeracion.ACTA, false, false, null, false, null, false, (short) 6,
                "{NRO}", true, vigenciaDesde, null, "IT_SEED"));
        NumTalonario talonario = talonarioService.crearTalonario(new CrearTalonarioCommand(
                politica.getId(), ("T" + sufijo), "Talonario IT dependencia " + idDependencia,
                TipoTalonario.ELECTRONICO, ClaseNumeracion.ACTA, null, null, 1, null,
                "SEQ_IT_DEP_" + idDependencia, true, false, null, null, "IT_SEED"));
        talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                talonario.getId(), ClaseNumeracion.ACTA, null, null,
                idDependencia, verDependencia,
                AlcanceTalonario.DEPENDENCIA, (short) 1, vigenciaDesde, null, true, "IT_SEED"));
        return talonario.getId();
    }

    /** Hasta 7 caracteres en base36, suficientes para caber en columnas VARCHAR(8) con el prefijo. */
    private static String sufijoCorto(Long idDependencia) {
        String base36 = Long.toString(idDependencia, 36).toUpperCase(java.util.Locale.ROOT);
        return base36.length() > 7 ? base36.substring(base36.length() - 7) : base36;
    }
}
