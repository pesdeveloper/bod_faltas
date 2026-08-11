package ar.gob.malvinas.faltas.core.infrastructure;

import ar.gob.malvinas.faltas.core.application.command.AlcoholemiaMedicionItem;
import ar.gob.malvinas.faltas.core.application.command.ArticuloInfringidoItem;
import ar.gob.malvinas.faltas.core.application.command.ContravencionActaItem;
import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.application.command.MedidaPreventivaActaItem;
import ar.gob.malvinas.faltas.core.application.command.SustanciasAlimenticiasActaItem;
import ar.gob.malvinas.faltas.core.application.command.TransitoActaItem;
import ar.gob.malvinas.faltas.core.application.command.VehiculoActaItem;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.application.service.TalonarioService;
import ar.gob.malvinas.faltas.core.domain.enums.AmbitoCtv;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoGeneralVehiculo;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenNomenclatura;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocumentoPersona;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPruebaAlcoholemia;
import ar.gob.malvinas.faltas.core.domain.enums.TipoVehiculo;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * IT de Wave 4 PARTE-3A: numeración electrónica end-to-end, concurrencia real (≥10 altas
 * simultáneas), rollback sin consumo de número, idempotencia secuencial y concurrente por
 * {@code id_tecnico}, y wiring real de los seis satélites completos del alta integral
 * (vehículo, tránsito, alcoholemia, contravención, sustancias alimenticias, medida
 * preventiva) contra MariaDB real.
 *
 * <p>Sigue el mismo patrón de decoradores por {@code BeanPostProcessor} que
 * {@code ActaLabrarSatelitesWave4IT} / {@code ActaLabrarTransaccionalIT}: falla en el punto
 * elegido para probar que una escritura ya realizada (el incremento del contador) se
 * revierte, no solo que no llegó a ocurrir.
 */
@SpringBootTest
@ActiveProfiles("jdbc")
@EnabledIfEnvironmentVariable(named = "FALTAS_DB_URL", matches = ".+")
@Import(ActaAltaIntegralParte3AIT.DecoradorFalloPostNumeracion.class)
@DisplayName("IT WAVE-4 PARTE-3A: numeración, concurrencia, rollback, idempotencia, satélites")
class ActaAltaIntegralParte3AIT {

    private static final String MARCA = "WAVE4P3A_";
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 8, 6, 9, 0, 0);
    private static final LocalDate F0 = LocalDate.of(2026, 8, 6);

    enum Etapa { NINGUNA, POST_NUMERACION }

    static volatile Etapa fallaEn = Etapa.NINGUNA;
    private static final String FALLO = "FALLO_INYECTADO_WAVE4_P3A";

    @Autowired
    private ActaService actaService;

    @Autowired
    private TalonarioService talonarioService;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private ApplicationContext ctx;

    private Long idDependencia;
    private Long idInspector;
    private Long idDependenciaContravencion;
    private Long idDependenciaSustancias;
    private Long idNormativa;
    private Long idArticulo;
    private Long idMedidaPreventiva;
    private Long idTalonario;

    @BeforeEach
    void setUp() {
        fallaEn = Etapa.NINGUNA;
        ActorContextHolder.set(new ActorContext(UUID.randomUUID().toString()));
        insertarDependenciaEInspector();
        idDependenciaContravencion = insertarDependenciaAdicional(TipoActa.CONTRAVENCION);
        idDependenciaSustancias = insertarDependenciaAdicional(TipoActa.SUSTANCIAS_ALIMENTICIAS);
        insertarNormativaArticuloYTarifario();
        insertarMedidaPreventivaYRelacion();
        insertarTalonarioElectronico();
    }

    /**
     * Dependencia adicional del slice, habilitada exclusivamente para {@code tipoActa}: cada
     * dependencia versionada define un único tipo de acta que puede labrar (comentario de
     * {@code FalDependenciaVersion}), así que contravención y sustancias alimenticias
     * necesitan su propia dependencia además de la de tránsito usada por defecto.
     */
    private Long insertarDependenciaAdicional(TipoActa tipoActa) {
        String nombre = MARCA + "DEP_" + tipoActa.name();
        jdbc.sql("INSERT INTO fal_dependencia (nom_dep, fh_alta, id_user_alta) VALUES (:nom, :fhAlta, :usuario)")
                .param("nom", nombre).param("fhAlta", T0).param("usuario", actor()).update();
        Long id = jdbc.sql("SELECT MAX(id) FROM fal_dependencia WHERE nom_dep = :nom")
                .param("nom", nombre).query(Long.class).single();
        jdbc.sql("""
                INSERT INTO fal_dependencia_version (id_dep, ver_dep, tipo_acta, fh_vig_desde, fh_alta, id_user_alta)
                VALUES (:idDep, 1, :tipoActa, :vigDesde, :fhAlta, :usuario)
                """).param("idDep", id).param("tipoActa", tipoActa.codigo())
                .param("vigDesde", LocalDate.of(2026, 1, 1)).param("fhAlta", T0).param("usuario", actor()).update();
        return id;
    }

    @AfterEach
    void limpiar() {
        fallaEn = Etapa.NINGUNA;
        ActorContextHolder.clear();
        borrarResiduos();
    }

    // =====================================================================
    // Seed del slice (propio, aislado por marca; nunca toca geo_*/maestros ajenos)
    // =====================================================================

    private void insertarDependenciaEInspector() {
        jdbc.sql("""
                INSERT INTO fal_dependencia (nom_dep, fh_alta, id_user_alta)
                VALUES (:nom, :fhAlta, :usuario)
                """).param("nom", MARCA + "DEP").param("fhAlta", T0).param("usuario", actor()).update();
        idDependencia = jdbc.sql("SELECT MAX(id) FROM fal_dependencia WHERE nom_dep = :nom")
                .param("nom", MARCA + "DEP").query(Long.class).single();
        jdbc.sql("""
                INSERT INTO fal_dependencia_version (id_dep, ver_dep, tipo_acta, fh_vig_desde, fh_alta, id_user_alta)
                VALUES (:idDep, 1, :tipoActa, :vigDesde, :fhAlta, :usuario)
                """).param("idDep", idDependencia).param("tipoActa", TipoActa.TRANSITO.codigo())
                .param("vigDesde", LocalDate.of(2026, 1, 1)).param("fhAlta", T0).param("usuario", actor()).update();

        jdbc.sql("""
                INSERT INTO fal_inspector (id_user, legajo_insp, nom_insp, fh_alta, id_user_alta)
                VALUES (:idUser, :legajo, :nom, :fhAlta, :usuario)
                """).param("idUser", UUID.randomUUID().toString()).param("legajo", (int) (Math.random() * 900000) + 1)
                .param("nom", MARCA + "INSP").param("fhAlta", T0).param("usuario", actor()).update();
        idInspector = jdbc.sql("SELECT MAX(id) FROM fal_inspector WHERE nom_insp = :nom")
                .param("nom", MARCA + "INSP").query(Long.class).single();
        jdbc.sql("""
                INSERT INTO fal_inspector_version (id_insp, ver_insp, legajo_insp, nom_insp, id_dep, ver_dep,
                    fh_vig_desde, fh_alta, id_user_alta)
                VALUES (:idInsp, 1, :legajo, :nom, :idDep, 1, :vigDesde, :fhAlta, :usuario)
                """).param("idInsp", idInspector).param("legajo", (int) (Math.random() * 900000) + 1)
                .param("nom", MARCA + "INSP").param("idDep", idDependencia)
                .param("vigDesde", LocalDate.of(2026, 1, 1)).param("fhAlta", T0).param("usuario", actor()).update();
    }

    private void insertarNormativaArticuloYTarifario() {
        String codigoNorma = (MARCA + UUID.randomUUID()).substring(0, 8);
        jdbc.sql("""
                INSERT INTO fal_normativa_faltas (codigo_norma, nombre_norma, tipo_norma, fh_vig_desde, fh_alta, id_user_alta)
                VALUES (:codigo, :nombre, 1, :fhVigDesde, :fhAlta, :usuario)
                """).param("codigo", codigoNorma).param("nombre", MARCA + "NORMATIVA")
                .param("fhVigDesde", T0.toLocalDate()).param("fhAlta", T0).param("usuario", actor()).update();
        idNormativa = jdbc.sql("SELECT MAX(id) FROM fal_normativa_faltas WHERE nombre_norma = :n")
                .param("n", MARCA + "NORMATIVA").query(Long.class).single();

        jdbc.sql("""
                INSERT INTO fal_articulo_normativa_faltas
                    (id_normativa, codigo_articulo, nombre_articulo, tipo_infraccion, fh_vig_desde, fh_alta, id_user_alta)
                VALUES (:idNormativa, :codigo, :nombre, 1, :fhVigDesde, :fhAlta, :usuario)
                """).param("idNormativa", idNormativa).param("codigo", "ART" + (int) (Math.random() * 1_000_000))
                .param("nombre", MARCA + "ARTICULO").param("fhVigDesde", T0.toLocalDate())
                .param("fhAlta", T0).param("usuario", actor()).update();
        idArticulo = jdbc.sql("SELECT MAX(id) FROM fal_articulo_normativa_faltas WHERE nombre_articulo = :n")
                .param("n", MARCA + "ARTICULO").query(Long.class).single();

        // Formula MONTO: no depende de fal_valor_unidad_faltas para que ValorizacionService
        // pueda calcular sin seed adicional.
        jdbc.sql("""
                INSERT INTO fal_tarifario_unidad_faltas
                    (id_articulo, tipo_unidad, monto_fijo_ordinario, si_admite_pago_voluntario, fh_vig_desde, fh_alta, id_user_alta)
                VALUES (:idArticulo, 3, 100.00, FALSE, :fhVigDesde, :fhAlta, :usuario)
                """).param("idArticulo", idArticulo).param("fhVigDesde", T0.toLocalDate())
                .param("fhAlta", T0).param("usuario", actor()).update();
    }

    private void insertarMedidaPreventivaYRelacion() {
        String codigo = ("MP" + UUID.randomUUID()).substring(0, 12);
        jdbc.sql("""
                INSERT INTO fal_medida_preventiva (codigo, version_medida, descripcion, fh_alta, id_user_alta)
                VALUES (:codigo, 1, :descripcion, :fhAlta, :usuario)
                """).param("codigo", codigo).param("descripcion", MARCA + "MEDIDA")
                .param("fhAlta", T0).param("usuario", actor()).update();
        idMedidaPreventiva = jdbc.sql("SELECT MAX(id) FROM fal_medida_preventiva WHERE codigo = :c")
                .param("c", codigo).query(Long.class).single();
        jdbc.sql("""
                INSERT INTO fal_articulo_medida_preventiva (id_articulo, id_medida, si_obligatoria, fh_alta, id_user_alta)
                VALUES (:idArticulo, :idMedida, FALSE, :fhAlta, :usuario)
                """).param("idArticulo", idArticulo).param("idMedida", idMedidaPreventiva)
                .param("fhAlta", T0).param("usuario", actor()).update();
    }

    /** Codigo corto unico (<= 8 caracteres, VARCHAR(8) real de num_politica/num_talonario). */
    private static String codigoCorto(String prefijo) {
        String base36 = Long.toString(System.nanoTime(), 36).toUpperCase();
        String sufijo = base36.substring(Math.max(0, base36.length() - (8 - prefijo.length())));
        return prefijo + sufijo;
    }

    private void insertarTalonarioElectronico() {
        var politica = talonarioService.crearPolitica(new ar.gob.malvinas.faltas.core.application.command.CrearPoliticaNumeracionCommand(
                codigoCorto("P"), "Politica ACTA electronica " + MARCA,
                ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion.ACTA,
                false, false, null, false, null, false, (short) 6,
                "{NRO}", true, LocalDate.of(2026, 1, 1), null, actor()));
        var talonario = talonarioService.crearTalonario(new ar.gob.malvinas.faltas.core.application.command.CrearTalonarioCommand(
                politica.getId(), codigoCorto("T"), "Talonario electronico " + MARCA,
                ar.gob.malvinas.faltas.core.domain.enums.TipoTalonario.ELECTRONICO,
                ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion.ACTA,
                null, null, 1, null,
                (MARCA + "SEQ_" + UUID.randomUUID()).substring(0, 24), true, false, null, null, actor()));
        idTalonario = talonario.getId();
        talonarioService.crearAmbito(new ar.gob.malvinas.faltas.core.application.command.CrearTalonarioAmbitoCommand(
                idTalonario, ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion.ACTA, null,
                TipoActa.TRANSITO.codigo(), idDependencia, (short) 1,
                ar.gob.malvinas.faltas.core.domain.enums.AlcanceTalonario.DEPENDENCIA,
                (short) 1, LocalDate.of(2026, 1, 1), null, true, actor()));
    }

    private void borrarResiduos() {
        List<Long> actas = jdbc.sql("SELECT id FROM fal_acta WHERE id_dep IN (:idDep, :idDep2, :idDep3)")
                .param("idDep", idDependencia).param("idDep2", idDependenciaContravencion)
                .param("idDep3", idDependenciaSustancias).query(Long.class).list();
        for (Long idActa : actas) {
            jdbc.sql("DELETE FROM fal_acta_medida_preventiva WHERE acta_id = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta_transito_alcoholemia WHERE acta_id = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta_transito WHERE acta_id = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta_vehiculo WHERE acta_id = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta_contravencion WHERE acta_id = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta_sustancias_alimenticias WHERE acta_id = :id").param("id", idActa).update();
            jdbc.sql("""
                    DELETE FROM fal_acta_valorizacion_item WHERE id_acta_articulo IN (
                        SELECT id FROM fal_acta_articulo_infringido WHERE id_acta = :id)
                    """).param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta_articulo_infringido WHERE id_acta = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta_snapshot WHERE id_acta = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta_valorizacion WHERE id_acta = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta_evidencia WHERE id_acta = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta_evento WHERE acta_id = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_observacion WHERE entidad_tipo = 1 AND entidad_id = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta WHERE id = :id").param("id", idActa).update();
        }
        jdbc.sql("""
                DELETE FROM fal_persona_domicilio WHERE persona_id IN (
                    SELECT id FROM fal_persona WHERE apellido LIKE :marca OR nombre_mostrar LIKE :marca)
                """).param("marca", MARCA + "%").update();
        jdbc.sql("DELETE FROM fal_persona WHERE apellido LIKE :marca OR nombre_mostrar LIKE :marca")
                .param("marca", MARCA + "%").update();
        if (idTalonario != null) {
            jdbc.sql("DELETE FROM num_talonario_movimiento WHERE id_talonario = :id").param("id", idTalonario).update();
            jdbc.sql("DELETE FROM num_talonario_ambito WHERE talonario_id = :id").param("id", idTalonario).update();
            jdbc.sql("DELETE FROM num_talonario WHERE id = :id").param("id", idTalonario).update();
        }
        if (idArticulo != null) {
            jdbc.sql("DELETE FROM fal_articulo_medida_preventiva WHERE id_articulo = :id").param("id", idArticulo).update();
            jdbc.sql("DELETE FROM fal_tarifario_unidad_faltas WHERE id_articulo = :id").param("id", idArticulo).update();
            jdbc.sql("DELETE FROM fal_articulo_normativa_faltas WHERE id = :id").param("id", idArticulo).update();
        }
        if (idMedidaPreventiva != null) {
            jdbc.sql("DELETE FROM fal_medida_preventiva WHERE id = :id").param("id", idMedidaPreventiva).update();
        }
        if (idNormativa != null) {
            jdbc.sql("DELETE FROM fal_normativa_faltas WHERE id = :id").param("id", idNormativa).update();
        }
        jdbc.sql("DELETE FROM fal_inspector_version WHERE id_insp = :id").param("id", idInspector).update();
        jdbc.sql("DELETE FROM fal_inspector WHERE id = :id").param("id", idInspector).update();
        for (Long dep : List.of(idDependencia, idDependenciaContravencion, idDependenciaSustancias)) {
            jdbc.sql("DELETE FROM fal_dependencia_version WHERE id_dep = :idDep").param("idDep", dep).update();
            jdbc.sql("DELETE FROM fal_dependencia WHERE id = :idDep").param("idDep", dep).update();
        }
    }

    private static String actor() { return ActorContextHolder.subOr("SYS"); }

    /** Bloque transito minimo, sin control de alcoholemia (Wave 4 Parte 3C, opt-in eliminado). */
    private static TransitoActaItem transitoMinimo() {
        return new TransitoActaItem(null, null, null, null, false, false, false, null);
    }

    private LabrarActaCommand comandoBase(String nroDoc, String idTecnico) {
        return new LabrarActaCommand(
                TipoActa.TRANSITO, idDependencia, idInspector, F0,
                MARCA + "domicilio del hecho", null, null, null, null,
                MARCA + "TITULAR", TipoDocumentoPersona.DNI, nroDoc, null,
                ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE,
                SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO,
                null, idTecnico, null,
                List.of(new ArticuloInfringidoItem(idNormativa, idArticulo)),
                null, transitoMinimo(), null, null, null);
    }

    private LabrarActaCommand comandoConSatelites(String nroDoc, String idTecnico) {
        VehiculoActaItem vehiculo = new VehiculoActaItem(
                "AB123CD", TipoVehiculo.AUTO, null, MARCA + "MARCA", null, MARCA + "MODELO",
                (short) 2020, "Rojo", EstadoGeneralVehiculo.BUENO);
        TransitoActaItem transito = new TransitoActaItem(
                MARCA + "LIC", null, null, null, false, false, true,
                List.of(new AlcoholemiaMedicionItem(
                        (short) 1, TipoPruebaAlcoholemia.ALCOHOLIMETRO, null, null, null, null, null, true, T0)));
        MedidaPreventivaActaItem medida = new MedidaPreventivaActaItem(
                idArticulo, idMedidaPreventiva, MARCA + "MEDIDA TXT", false);
        return new LabrarActaCommand(
                TipoActa.TRANSITO, idDependencia, idInspector, F0,
                MARCA + "domicilio del hecho", null, null, null, null,
                MARCA + "TITULAR", TipoDocumentoPersona.DNI, nroDoc, null,
                ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE,
                SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO,
                null, idTecnico, null,
                List.of(new ArticuloInfringidoItem(idNormativa, idArticulo)),
                vehiculo, transito, null, null, List.of(medida));
    }

    private static String nroDocDePrueba() {
        return String.valueOf(70_000_000 + (int) (Math.random() * 900_000));
    }

    /**
     * Crea un talonario ACTA ELECTRONICO activo dedicado, con ambito DEPENDENCIA para
     * {@code idDep}, y devuelve su id. El llamador es responsable de limpiarlo (ambito +
     * talonario + politica) en su propio bloque {@code finally} con
     * {@link #borrarTalonarioDedicado(Long)}.
     */
    private Long crearTalonarioDedicado(Long idDep, short tipoActaCodigo, int nroDesde, Integer nroHasta) {
        var politica = talonarioService.crearPolitica(new ar.gob.malvinas.faltas.core.application.command.CrearPoliticaNumeracionCommand(
                codigoCorto("PD"), "Politica dedicada " + MARCA,
                ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion.ACTA,
                false, false, null, false, null, false, (short) 6,
                "{NRO}", true, LocalDate.of(2026, 1, 1), null, actor()));
        var talonario = talonarioService.crearTalonario(new ar.gob.malvinas.faltas.core.application.command.CrearTalonarioCommand(
                politica.getId(), codigoCorto("TD"), "Talonario dedicado " + MARCA,
                ar.gob.malvinas.faltas.core.domain.enums.TipoTalonario.ELECTRONICO,
                ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion.ACTA,
                null, null, nroDesde, nroHasta,
                (MARCA + "SEQD_" + UUID.randomUUID()).substring(0, 24), true, false, null, null, actor()));
        talonarioService.crearAmbito(new ar.gob.malvinas.faltas.core.application.command.CrearTalonarioAmbitoCommand(
                talonario.getId(), ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion.ACTA, null,
                tipoActaCodigo, idDep, (short) 1,
                ar.gob.malvinas.faltas.core.domain.enums.AlcanceTalonario.DEPENDENCIA,
                (short) 1, LocalDate.of(2026, 1, 1), null, true, actor()));
        return talonario.getId();
    }

    /**
     * Crea un segundo talonario ACTA ELECTRONICO activo, aplicable a la misma dependencia que
     * otro ya existente, insertando su ambito con SQL crudo en vez de {@code TalonarioService
     * .crearAmbito}: a proposito evita el guardrail transaccional de
     * {@code DECISION-NUM-UNICO-ACTIVO-POR-DEPENDENCIA-01} (test 12a) para poder demostrar que,
     * si esa inconsistencia llegara a existir por otra via, {@code ActaService.labrar} igual la
     * detecta y rechaza (test 12b, defensa en profundidad).
     */
    private Long crearTalonarioInconsistentePorSqlCrudo(Long idDep, short tipoActaCodigo) {
        var politica = talonarioService.crearPolitica(new ar.gob.malvinas.faltas.core.application.command.CrearPoliticaNumeracionCommand(
                codigoCorto("PX"), "Politica inconsistente " + MARCA,
                ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion.ACTA,
                false, false, null, false, null, false, (short) 6,
                "{NRO}", true, LocalDate.of(2026, 1, 1), null, actor()));
        var talonario = talonarioService.crearTalonario(new ar.gob.malvinas.faltas.core.application.command.CrearTalonarioCommand(
                politica.getId(), codigoCorto("TX"), "Talonario inconsistente " + MARCA,
                ar.gob.malvinas.faltas.core.domain.enums.TipoTalonario.ELECTRONICO,
                ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion.ACTA,
                null, null, 1, null,
                (MARCA + "SEQX_" + UUID.randomUUID()).substring(0, 24), true, false, null, null, actor()));
        jdbc.sql("""
                INSERT INTO num_talonario_ambito
                    (talonario_id, clase_talonario, tipo_docu, tipo_acta, id_dep, ver_dep,
                     alcance, prioridad, fh_desde, fh_hasta, si_activo, fh_alta, id_user_alta)
                VALUES (:talonarioId, 1, NULL, :tipoActa, :idDep, 1, 2, 1, :fhDesde, NULL, TRUE, :fhAlta, :usuario)
                """)
                .param("talonarioId", talonario.getId())
                .param("tipoActa", tipoActaCodigo)
                .param("idDep", idDep)
                .param("fhDesde", LocalDate.of(2026, 1, 1))
                .param("fhAlta", T0)
                .param("usuario", actor())
                .update();
        return talonario.getId();
    }

    private void borrarTalonarioDedicado(Long idTal) {
        jdbc.sql("DELETE FROM num_talonario_ambito WHERE talonario_id = :id").param("id", idTal).update();
        Long idPol = jdbc.sql("SELECT politica_id FROM num_talonario WHERE id = :id")
                .param("id", idTal).query(Long.class).single();
        jdbc.sql("DELETE FROM num_talonario WHERE id = :id").param("id", idTal).update();
        jdbc.sql("DELETE FROM num_politica WHERE id = :id").param("id", idPol).update();
    }

    private Integer ultimoNroEmitido() {
        return jdbc.sql("SELECT ultimo_nro_emitido FROM num_talonario WHERE id = :id")
                .param("id", idTalonario).query(Integer.class).single();
    }

    private long contarMovimientosManuales() {
        return jdbc.sql("SELECT COUNT(*) FROM num_talonario_movimiento WHERE id_talonario = :id")
                .param("id", idTalonario).query(Long.class).single();
    }

    private long contarActasDelSlice() {
        return jdbc.sql("SELECT COUNT(*) FROM fal_acta WHERE id_dep = :idDep")
                .param("idDep", idDependencia).query(Long.class).single();
    }

    // =====================================================================
    @Nested
    @DisplayName("Wiring real de los seis satélites completos")
    class SatelitesWiring {

        @Test
        @DisplayName("1. Alta de tránsito con vehículo, alcoholemia y medida preventiva persiste cada satélite en su tabla")
        void altaConVehiculoAlcoholemiaYMedidaPreventiva() {
            ComandoResultado res = actaService.labrar(comandoConSatelites(nroDocDePrueba(), null));
            Long idActa = res.idActa();

            assertThat(jdbc.sql("SELECT COUNT(*) FROM fal_acta_vehiculo WHERE acta_id = :id")
                    .param("id", idActa).query(Long.class).single()).isEqualTo(1L);
            assertThat(jdbc.sql("SELECT dominio_vehiculo FROM fal_acta_vehiculo WHERE acta_id = :id")
                    .param("id", idActa).query(String.class).single()).isEqualTo("AB123CD");

            assertThat(jdbc.sql("SELECT COUNT(*) FROM fal_acta_transito WHERE acta_id = :id")
                    .param("id", idActa).query(Long.class).single()).isEqualTo(1L);
            assertThat(jdbc.sql("SELECT si_control_alcoholemia FROM fal_acta_transito WHERE acta_id = :id")
                    .param("id", idActa).query(Boolean.class).single()).isTrue();

            assertThat(jdbc.sql("SELECT COUNT(*) FROM fal_acta_transito_alcoholemia WHERE acta_id = :id")
                    .param("id", idActa).query(Long.class).single()).isEqualTo(1L);
            assertThat(jdbc.sql("SELECT si_resultado_final FROM fal_acta_transito_alcoholemia WHERE acta_id = :id")
                    .param("id", idActa).query(Boolean.class).single()).isTrue();

            List<Long> actaArticuloIds = jdbc.sql("SELECT id FROM fal_acta_articulo_infringido WHERE id_acta = :id")
                    .param("id", idActa).query(Long.class).list();
            assertThat(actaArticuloIds).hasSize(1);
            // Wave 4 Parte 3B §7 (DECISION-MEDIDA-PREVENTIVA-LIBRE-01): la medida preventiva ya
            // no referencia el articulo infringido (acta_articulo_id queda NULL).
            assertThat(jdbc.sql("""
                    SELECT COUNT(*) FROM fal_acta_medida_preventiva
                     WHERE acta_id = :id AND acta_articulo_id IS NULL AND medida_preventiva_id = :med
                    """).param("id", idActa).param("med", idMedidaPreventiva)
                    .query(Long.class).single()).isEqualTo(1L);
        }

        @Test
        @DisplayName("2. Medida preventiva inexistente en el catalogo rechaza el alta sin escribir nada")
        void medidaPreventivaInexistenteRechazaElAlta() {
            // Wave 4 Parte 3B §7 (DECISION-MEDIDA-PREVENTIVA-LIBRE-01): la decision final es que
            // el inspector puede elegir cualquier medida preventiva vigente del catalogo, sin
            // relacion previa con el articulo. Ya no existe "relacion no permitida"; el unico
            // rechazo posible por catalogo es medida inexistente o inactiva.
            Long idMedidaNoRelacionada = jdbc.sql("SELECT MAX(id) + 1 FROM fal_medida_preventiva").query(Long.class).single();
            // La matriz de aplicabilidad (Wave 4 Parte 3B §6) exige el bloque 'transito' para
            // tipoActa=TRANSITO en cuanto el comando informa cualquier satelite tipado
            // (medidasPreventivas en este caso): se informa un transito minimo valido para que
            // el rechazo bajo prueba sea especificamente el de catalogo de medida preventiva,
            // no el de la matriz.
            TransitoActaItem transitoMinimo = new TransitoActaItem(
                    null, null, null, null, false, false, false, null);
            LabrarActaCommand cmd = new LabrarActaCommand(
                    TipoActa.TRANSITO, idDependencia, idInspector, F0,
                    MARCA + "dom", null, null, null, null,
                    MARCA + "TITULAR", TipoDocumentoPersona.DNI, nroDocDePrueba(), null,
                    ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE,
                    SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO,
                    null, null, null,
                    List.of(new ArticuloInfringidoItem(idNormativa, idArticulo)),
                    null, transitoMinimo, null, null,
                    List.of(new MedidaPreventivaActaItem(idArticulo, idMedidaNoRelacionada, null, false)));

            Throwable error = catchThrowable(() -> actaService.labrar(cmd));

            assertThat(error).isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("no existe");
            assertThat(contarActasDelSlice()).isZero();
        }

        @Test
        @DisplayName("3. Alta de contravención persiste el satélite fuera del circuito de tránsito")
        void altaDeContravencionPersisteSatelite() {
            // DECISION-ACTA-TALONARIO-REQUIRED-01 (Wave 4 Parte 3C §7): idDependenciaContravencion
            // no tiene talonario por defecto (lo reserva NumeracionReglasFinales.
            // talonarioAgotadoRechazaElAlta para su propio talonario dedicado de rango 1); este
            // test necesita el suyo, exclusivo y limpiado en su propio finally.
            Long idTalDedicado = crearTalonarioDedicado(
                    idDependenciaContravencion, TipoActa.CONTRAVENCION.codigo(), 1, null);
            try {
                // Componentes en orden: origenNomencl, siNomenclaturaManual, motivoNomenclaturaManual,
                // idSujI, idBieI, idSujC, idBieC, circ, secc, frac, mza, parc, ufun, ucomp,
                // rubroId, idRub, ambitoCtv, ambitoCtvTxt (18 en total).
                ContravencionActaItem contravencion = new ContravencionActaItem(
                        OrigenNomenclatura.MANUAL_EXCEPCIONAL, true,
                        ar.gob.malvinas.faltas.core.domain.enums.MotivoNomenclaturaManual.CONTINGENCIA_OPERATIVA,
                        null, null, null, null,
                        null, null, null, null, null, null, null,
                        null, null,
                        AmbitoCtv.COMERCIO, null);
                LabrarActaCommand cmd = new LabrarActaCommand(
                        TipoActa.CONTRAVENCION, idDependenciaContravencion, null, F0,
                        MARCA + "dom", null, null, null, null,
                        MARCA + "TITULAR", TipoDocumentoPersona.DNI, nroDocDePrueba(), null,
                        ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE,
                        SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO,
                        null, null, null, List.of(),
                        null, null, contravencion, null, null);

                ComandoResultado res = actaService.labrar(cmd);

                assertThat(jdbc.sql("SELECT origen_nomencl, si_nomenclatura_manual FROM fal_acta_contravencion WHERE acta_id = :id")
                        .param("id", res.idActa())
                        .query((rs, n) -> List.of(rs.getShort("origen_nomencl"), rs.getBoolean("si_nomenclatura_manual")))
                        .single()).isEqualTo(List.of((short) OrigenNomenclatura.MANUAL_EXCEPCIONAL.codigo(), true));
            } finally {
                jdbc.sql("DELETE FROM fal_acta_contravencion WHERE acta_id IN (SELECT id FROM fal_acta WHERE id_dep = :d)")
                        .param("d", idDependenciaContravencion).update();
                jdbc.sql("DELETE FROM fal_acta_valorizacion WHERE id_acta IN (SELECT id FROM fal_acta WHERE id_dep = :d)")
                        .param("d", idDependenciaContravencion).update();
                jdbc.sql("DELETE FROM fal_acta_evento WHERE acta_id IN (SELECT id FROM fal_acta WHERE id_dep = :d)")
                        .param("d", idDependenciaContravencion).update();
                jdbc.sql("DELETE FROM fal_acta_snapshot WHERE id_acta IN (SELECT id FROM fal_acta WHERE id_dep = :d)")
                        .param("d", idDependenciaContravencion).update();
                jdbc.sql("DELETE FROM fal_acta WHERE id_dep = :d").param("d", idDependenciaContravencion).update();
                borrarTalonarioDedicado(idTalDedicado);
            }
        }

        @Test
        @DisplayName("4. Alta de sustancias alimenticias persiste el satélite")
        void altaDeSustanciasAlimenticiasPersisteSatelite() {
            // Idem test 3: idDependenciaSustancias no tiene talonario por defecto (lo reserva
            // NumeracionReglasFinales.dosTalonariosActivosRechazanElAlta para crear 2 propios).
            Long idTalDedicado = crearTalonarioDedicado(
                    idDependenciaSustancias, TipoActa.SUSTANCIAS_ALIMENTICIAS.codigo(), 1, null);
            try {
                SustanciasAlimenticiasActaItem sustancias = new SustanciasAlimenticiasActaItem(
                        null, null, AmbitoCtv.LOCAL, null, MARCA + "DESCRIPCION SUSTANCIAS");
                LabrarActaCommand cmd = new LabrarActaCommand(
                        TipoActa.SUSTANCIAS_ALIMENTICIAS, idDependenciaSustancias, null, F0,
                        MARCA + "dom", null, null, null, null,
                        MARCA + "TITULAR", TipoDocumentoPersona.DNI, nroDocDePrueba(), null,
                        ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE,
                        SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO,
                        null, null, null, List.of(),
                        null, null, null, sustancias, null);

                ComandoResultado res = actaService.labrar(cmd);

                assertThat(jdbc.sql("SELECT descripcion_sustancias FROM fal_acta_sustancias_alimenticias WHERE acta_id = :id")
                        .param("id", res.idActa()).query(String.class).single())
                        .isEqualTo(MARCA + "DESCRIPCION SUSTANCIAS");
            } finally {
                jdbc.sql("DELETE FROM fal_acta_sustancias_alimenticias WHERE acta_id IN (SELECT id FROM fal_acta WHERE id_dep = :d)")
                        .param("d", idDependenciaSustancias).update();
                jdbc.sql("DELETE FROM fal_acta_valorizacion WHERE id_acta IN (SELECT id FROM fal_acta WHERE id_dep = :d)")
                        .param("d", idDependenciaSustancias).update();
                jdbc.sql("DELETE FROM fal_acta_evento WHERE acta_id IN (SELECT id FROM fal_acta WHERE id_dep = :d)")
                        .param("d", idDependenciaSustancias).update();
                jdbc.sql("DELETE FROM fal_acta_snapshot WHERE id_acta IN (SELECT id FROM fal_acta WHERE id_dep = :d)")
                        .param("d", idDependenciaSustancias).update();
                jdbc.sql("DELETE FROM fal_acta WHERE id_dep = :d").param("d", idDependenciaSustancias).update();
                borrarTalonarioDedicado(idTalDedicado);
            }
        }

        @Test
        @DisplayName("5. Fallo al guardar vehículo revierte acta, artículo y persona (rollback total)")
        void falloEnVehiculoRevierteTodo() {
            // Reusa el mecanismo de fallo post-numeracion en otra etapa: aqui se verifica que
            // el satelite vehiculo participa de la MISMA transaccion que el nucleo, mediante
            // una violacion de integridad real (medida preventiva inexistente) en el mismo
            // comando que informa vehiculo: si vehiculo violara la atomicidad, quedaria
            // persistido a pesar del rechazo posterior.
            Long idMedidaInexistente = jdbc.sql("SELECT MAX(id) + 1000 FROM fal_medida_preventiva").query(Long.class).single();
            VehiculoActaItem vehiculo = new VehiculoActaItem(
                    "ZZ999ZZ", TipoVehiculo.MOTO, null, MARCA + "MARCA2", null, null, null, null, null);
            LabrarActaCommand cmd = new LabrarActaCommand(
                    TipoActa.TRANSITO, idDependencia, idInspector, F0,
                    MARCA + "dom", null, null, null, null,
                    MARCA + "TITULAR", TipoDocumentoPersona.DNI, nroDocDePrueba(), null,
                    ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE,
                    SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO,
                    null, null, null,
                    List.of(new ArticuloInfringidoItem(idNormativa, idArticulo)),
                    vehiculo, transitoMinimo(), null, null,
                    List.of(new MedidaPreventivaActaItem(idArticulo, idMedidaInexistente, null, false)));

            Throwable error = catchThrowable(() -> actaService.labrar(cmd));

            assertThat(error).isInstanceOf(PrecondicionVioladaException.class);
            assertThat(contarActasDelSlice()).as("cero fal_acta: rollback total").isZero();
            assertThat(jdbc.sql("SELECT COUNT(*) FROM fal_acta_vehiculo WHERE dominio_vehiculo = 'ZZ999ZZ'")
                    .query(Long.class).single()).as("el vehiculo tambien se revierte").isZero();
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Numeración electrónica end-to-end")
    class NumeracionEndToEnd {

        @Test
        @DisplayName("6. Alta electrónica recibe nro_acta e id_talonario, sin fila en num_talonario_movimiento")
        void altaElectronicaRecibeNumeroEndToEnd() {
            Integer antes = ultimoNroEmitido();

            ComandoResultado res = actaService.labrar(comandoBase(nroDocDePrueba(), null));

            var fila = jdbc.sql("SELECT nro_acta, id_talonario, nro_talonario_usado FROM fal_acta WHERE id = :id")
                    .param("id", res.idActa())
                    .query((rs, n) -> List.of(rs.getObject("nro_acta"), rs.getObject("id_talonario"), rs.getObject("nro_talonario_usado")))
                    .single();
            assertThat(fila.get(0)).as("nro_acta no nulo").isNotNull();
            assertThat(fila.get(1)).as("id_talonario asignado").isEqualTo(idTalonario);
            assertThat(((Number) fila.get(2)).intValue()).isEqualTo(antes + 1);
            assertThat(ultimoNroEmitido()).isEqualTo(antes + 1);
            assertThat(contarMovimientosManuales()).as("circuito electronico: 0 num_talonario_movimiento").isZero();
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Concurrencia real de numeración (≥10 altas simultáneas)")
    class ConcurrenciaNumeracion {

        @Test
        @DisplayName("7. Diez altas concurrentes sobre el mismo talonario reciben números únicos y correlativos")
        void diezAltasConcurrentesRecibenNumerosUnicos() throws InterruptedException {
            int hilos = 10;
            Integer antes = ultimoNroEmitido();
            ExecutorService exec = Executors.newFixedThreadPool(hilos);
            CountDownLatch listos = new CountDownLatch(hilos);
            CountDownLatch arranquen = new CountDownLatch(1);
            CountDownLatch fin = new CountDownLatch(hilos);
            List<Integer> numeros = new CopyOnWriteArrayList<>();
            List<Long> idsActa = new CopyOnWriteArrayList<>();
            List<Throwable> errores = new CopyOnWriteArrayList<>();

            try {
                for (int i = 0; i < hilos; i++) {
                    String nroDoc = String.valueOf(71_000_000 + i * 1237 + (int) (Math.random() * 900));
                    exec.submit(() -> {
                        ActorContextHolder.set(new ActorContext(UUID.randomUUID().toString()));
                        listos.countDown();
                        try {
                            arranquen.await(10, TimeUnit.SECONDS);
                            ComandoResultado res = actaService.labrar(comandoBase(nroDoc, null));
                            idsActa.add(res.idActa());
                            Integer nro = jdbc.sql("SELECT nro_talonario_usado FROM fal_acta WHERE id = :id")
                                    .param("id", res.idActa()).query(Integer.class).single();
                            numeros.add(nro);
                        } catch (Throwable t) {
                            errores.add(t);
                        } finally {
                            ActorContextHolder.clear();
                            fin.countDown();
                        }
                    });
                }
                assertThat(listos.await(10, TimeUnit.SECONDS)).isTrue();
                arranquen.countDown();
                assertThat(fin.await(30, TimeUnit.SECONDS)).as("las 10 altas terminaron a tiempo").isTrue();
            } finally {
                exec.shutdownNow();
            }

            assertThat(errores).as("ninguna alta concurrente fallo: %s", errores).isEmpty();
            assertThat(idsActa).as("10 actas confirmadas").hasSize(hilos);
            assertThat(Set.copyOf(idsActa)).as("10 ids de acta distintos").hasSize(hilos);
            assertThat(numeros).as("10 numeros de talonario").hasSize(hilos);
            assertThat(Set.copyOf(numeros)).as("10 numeros unicos, ningun duplicado").hasSize(hilos);
            assertThat(numeros).as("correlatividad exacta respecto del contador inicial")
                    .containsExactlyInAnyOrderElementsOf(
                            IntStream.rangeClosed(antes + 1, antes + hilos).boxed().collect(Collectors.toList()));
            assertThat(ultimoNroEmitido()).as("contador final = inicial + 10").isEqualTo(antes + hilos);
            assertThat(contarMovimientosManuales()).as("cero num_talonario_movimiento tras el lote concurrente").isZero();

            long actlabPorActa = idsActa.stream()
                    .mapToLong(id -> jdbc.sql("SELECT COUNT(*) FROM fal_acta_evento WHERE acta_id = :id AND tipo_evt = 'ACTLAB'")
                            .param("id", id).query(Long.class).single())
                    .filter(c -> c == 1L).count();
            assertThat(actlabPorActa).as("un ACTLAB por acta").isEqualTo(hilos);
            long snapshotPorActa = idsActa.stream()
                    .mapToLong(id -> jdbc.sql("SELECT COUNT(*) FROM fal_acta_snapshot WHERE id_acta = :id")
                            .param("id", id).query(Long.class).single())
                    .filter(c -> c == 1L).count();
            assertThat(snapshotPorActa).as("un snapshot por acta").isEqualTo(hilos);
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Rollback después del incremento: no consume número")
    class RollbackSinConsumo {

        @Test
        @DisplayName("8. Fallo inyectado después de numerar (antes de ACTLAB) revierte el contador y no deja residuos")
        void falloDespuesDeNumerarRevierteContadorSinResiduos() {
            Integer antes = ultimoNroEmitido();
            fallaEn = Etapa.POST_NUMERACION;
            String nroDoc = nroDocDePrueba();

            Throwable error = catchThrowable(() -> actaService.labrar(comandoBase(nroDoc, null)));

            assertThat(error).hasMessageContaining(FALLO);
            assertThat(contarActasDelSlice()).as("cero fal_acta").isZero();
            assertThat(ultimoNroEmitido()).as("el contador vuelve exactamente al valor anterior").isEqualTo(antes);
            assertThat(contarMovimientosManuales()).as("cero num_talonario_movimiento").isZero();
            assertThat(jdbc.sql("SELECT COUNT(*) FROM fal_acta_snapshot WHERE id_acta IN "
                            + "(SELECT id FROM fal_acta WHERE id_dep = :idDep)")
                    .param("idDep", idDependencia).query(Long.class).single())
                    .as("cero snapshot: el evento nunca llego a registrarse").isZero();

            fallaEn = Etapa.NINGUNA;
            ComandoResultado ok = actaService.labrar(comandoBase(nroDocDePrueba(), null));
            Integer nroSiguiente = jdbc.sql("SELECT nro_talonario_usado FROM fal_acta WHERE id = :id")
                    .param("id", ok.idActa()).query(Integer.class).single();
            assertThat(nroSiguiente).as("la siguiente alta valida recibe el numero no consumido").isEqualTo(antes + 1);
            assertThat(ultimoNroEmitido()).isEqualTo(antes + 1);
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Reglas finales de talonario (Wave 4 Parte 3B §9)")
    class NumeracionReglasFinales {

        @Test
        @DisplayName("11. Talonario agotado (nroHasta superado) rechaza toda el alta, sin persistir nada")
        void talonarioAgotadoRechazaElAlta() {
            // Talonario dedicado con rango de exactamente 1 numero, para la dependencia de
            // CONTRAVENCION (aislado del talonario principal de TRANSITO).
            Long idTalDedicado = crearTalonarioDedicado(
                    idDependenciaContravencion, TipoActa.CONTRAVENCION.codigo(), 9_000_000, 9_000_000);
            try {
                ContravencionActaItem contravencion = new ContravencionActaItem(
                        OrigenNomenclatura.MANUAL_EXCEPCIONAL, true,
                        ar.gob.malvinas.faltas.core.domain.enums.MotivoNomenclaturaManual.CONTINGENCIA_OPERATIVA,
                        null, null, null, null, null, null, null, null, null, null, null,
                        null, null, AmbitoCtv.COMERCIO, null);
                LabrarActaCommand cmd1 = new LabrarActaCommand(
                        TipoActa.CONTRAVENCION, idDependenciaContravencion, null, F0,
                        MARCA + "dom", null, null, null, null,
                        MARCA + "TITULAR", TipoDocumentoPersona.DNI, nroDocDePrueba(), null,
                        ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE,
                        SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO,
                        null, null, null, List.of(),
                        null, null, contravencion, null, null);
                ComandoResultado primera = actaService.labrar(cmd1);
                assertThat(jdbc.sql("SELECT nro_talonario_usado FROM fal_acta WHERE id = :id")
                        .param("id", primera.idActa()).query(Integer.class).single()).isEqualTo(9_000_000);

                long actasAntesDelAgotamiento = contarActasDelSlice();
                LabrarActaCommand cmd2 = new LabrarActaCommand(
                        TipoActa.CONTRAVENCION, idDependenciaContravencion, null, F0,
                        MARCA + "dom2", null, null, null, null,
                        MARCA + "TITULAR2", TipoDocumentoPersona.DNI, nroDocDePrueba(), null,
                        ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE,
                        SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO,
                        null, null, null, List.of(),
                        null, null, contravencion, null, null);

                Throwable error = catchThrowable(() -> actaService.labrar(cmd2));

                assertThat(error).isInstanceOf(PrecondicionVioladaException.class)
                        .hasMessageContaining("agotado");
                assertThat(contarActasDelSlice())
                        .as("talonario agotado: cero actas nuevas, la del alta previa se preserva")
                        .isEqualTo(actasAntesDelAgotamiento);
                assertThat(jdbc.sql("SELECT ultimo_nro_emitido FROM num_talonario WHERE id = :id")
                        .param("id", idTalDedicado).query(Integer.class).single())
                        .as("el contador no avanza mas alla del rango agotado").isEqualTo(9_000_000);
            } finally {
                jdbc.sql("DELETE FROM fal_acta_contravencion WHERE acta_id IN (SELECT id FROM fal_acta WHERE id_dep = :d)")
                        .param("d", idDependenciaContravencion).update();
                jdbc.sql("DELETE FROM fal_acta_valorizacion WHERE id_acta IN (SELECT id FROM fal_acta WHERE id_dep = :d)")
                        .param("d", idDependenciaContravencion).update();
                jdbc.sql("DELETE FROM fal_acta_evento WHERE acta_id IN (SELECT id FROM fal_acta WHERE id_dep = :d)")
                        .param("d", idDependenciaContravencion).update();
                jdbc.sql("DELETE FROM fal_acta_snapshot WHERE id_acta IN (SELECT id FROM fal_acta WHERE id_dep = :d)")
                        .param("d", idDependenciaContravencion).update();
                jdbc.sql("DELETE FROM fal_acta WHERE id_dep = :d").param("d", idDependenciaContravencion).update();
                borrarTalonarioDedicado(idTalDedicado);
            }
        }

        @Test
        @DisplayName("12a. Crear un segundo talonario ACTA ELECTRONICO activo para la misma dependencia se rechaza en la escritura")
        void segundoTalonarioActivoSeRechazaAlCrearlo() {
            // DECISION-NUM-UNICO-ACTIVO-POR-DEPENDENCIA-01 (Wave 4 Parte 3C §8): la exclusividad
            // se previene en el camino de escritura, no solo se detecta al labrar. El primer
            // talonario dedicado se crea sin problema; el segundo, aplicable a la misma
            // dependencia/tipoActa/vigencia, debe rechazarse en crearAmbito (TalonarioService),
            // antes de que exista ninguna inconsistencia que labrar() pueda ver.
            Long idTal1 = crearTalonarioDedicado(
                    idDependenciaSustancias, TipoActa.SUSTANCIAS_ALIMENTICIAS.codigo(), 1, null);
            try {
                Throwable error = catchThrowable(() -> crearTalonarioDedicado(
                        idDependenciaSustancias, TipoActa.SUSTANCIAS_ALIMENTICIAS.codigo(), 1, null));

                assertThat(error).isInstanceOf(PrecondicionVioladaException.class)
                        .hasMessageContaining("Maximo un talonario electronico activo por dependencia");
                assertThat(jdbc.sql("""
                        SELECT COUNT(*) FROM num_talonario_ambito a JOIN num_talonario t ON a.talonario_id = t.id
                         WHERE a.id_dep = :dep AND a.si_activo = TRUE AND t.si_activo = TRUE
                           AND t.clase_talonario = 1 AND t.tipo_talonario = 1
                        """).param("dep", idDependenciaSustancias).query(Long.class).single())
                        .as("exactamente un talonario electronico activo aplicable, el rechazado nunca se creo")
                        .isEqualTo(1L);
            } finally {
                borrarTalonarioDedicado(idTal1);
            }
        }

        @Test
        @DisplayName("12b. Si dos talonarios ACTA ELECTRONICO activos llegaran a coexistir (dato inconsistente), labrar() los detecta y rechaza el alta")
        void dosTalonariosActivosPreexistentesRechazanElAlta() {
            // Defensa en profundidad: aunque crearAmbito ya bloquea este estado en la escritura
            // (test 12a), este caso demuestra que si la inconsistencia llegara a existir por
            // cualquier otra via (migracion, correccion manual, dato heredado), ActaService
            // .labrar() sigue rechazando el alta en vez de numerar con ambiguedad. Se inserta el
            // segundo ambito por SQL crudo, deliberadamente evitando el guardrail del caso de uso.
            Long idTal1 = crearTalonarioDedicado(
                    idDependenciaSustancias, TipoActa.SUSTANCIAS_ALIMENTICIAS.codigo(), 1, null);
            Long idTal2 = crearTalonarioInconsistentePorSqlCrudo(
                    idDependenciaSustancias, TipoActa.SUSTANCIAS_ALIMENTICIAS.codigo());
            try {
                SustanciasAlimenticiasActaItem sustancias = new SustanciasAlimenticiasActaItem(
                        null, null, AmbitoCtv.LOCAL, null, MARCA + "DOS_TALONARIOS");
                LabrarActaCommand cmd = new LabrarActaCommand(
                        TipoActa.SUSTANCIAS_ALIMENTICIAS, idDependenciaSustancias, null, F0,
                        MARCA + "dom", null, null, null, null,
                        MARCA + "TITULAR", TipoDocumentoPersona.DNI, nroDocDePrueba(), null,
                        ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE,
                        SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO,
                        null, null, null, List.of(),
                        null, null, null, sustancias, null);

                Throwable error = catchThrowable(() -> actaService.labrar(cmd));

                assertThat(error).isInstanceOf(PrecondicionVioladaException.class)
                        .hasMessageContaining("Inconsistencia");
                assertThat(jdbc.sql("SELECT COUNT(*) FROM fal_acta WHERE id_dep = :d")
                        .param("d", idDependenciaSustancias).query(Long.class).single())
                        .as("cero actas: la inconsistencia rechaza el alta completa").isZero();
            } finally {
                borrarTalonarioDedicado(idTal1);
                borrarTalonarioDedicado(idTal2);
            }
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Exclusividad concurrente de talonario electronico por dependencia (Wave 4 Parte 3C §8.3)")
    class ExclusividadTalonarioConcurrente {

        /** Crea politica+talonario ELECTRONICO/ACTA (siempre exitoso, codigos unicos) sin ambito. */
        private Long crearTalonarioSinAmbito() {
            var politica = talonarioService.crearPolitica(new ar.gob.malvinas.faltas.core.application.command.CrearPoliticaNumeracionCommand(
                    codigoCorto("PC"), "Politica concurrente " + MARCA,
                    ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion.ACTA,
                    false, false, null, false, null, false, (short) 6,
                    "{NRO}", true, LocalDate.of(2026, 1, 1), null, actor()));
            var talonario = talonarioService.crearTalonario(new ar.gob.malvinas.faltas.core.application.command.CrearTalonarioCommand(
                    politica.getId(), codigoCorto("TC"), "Talonario concurrente " + MARCA,
                    ar.gob.malvinas.faltas.core.domain.enums.TipoTalonario.ELECTRONICO,
                    ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion.ACTA,
                    null, null, 1, null,
                    (MARCA + "SEQC_" + UUID.randomUUID()).substring(0, 24), true, false, null, null, actor()));
            return talonario.getId();
        }

        /**
         * Dos hilos intentan, al mismo tiempo, activar un ambito ACTA ELECTRONICO DEPENDENCIA
         * aplicable a la MISMA dependencia (idDependenciaContravencion, sin talonario propio por
         * defecto). Cada hilo ya tiene su propio talonario/politica creados de antemano (sin
         * ambito): solo compiten por el paso protegido por
         * {@code DECISION-NUM-UNICO-ACTIVO-POR-DEPENDENCIA-01}. Resultado exigido: exactamente
         * una activacion exitosa, la otra rechazada, y el estado final en MariaDB muestra
         * exactamente un talonario activo aplicable (no "last write wins": la perdedora nunca
         * llega a existir como fila activa).
         */
        @Test
        @DisplayName("13. Dos activaciones concurrentes de talonario ACTA ELECTRONICO para la misma dependencia: exactamente una exitosa")
        void dosActivacionesConcurrentesDejanExactamenteUnaActiva() throws InterruptedException {
            int hilos = 2;
            Long idTal1 = crearTalonarioSinAmbito();
            Long idTal2 = crearTalonarioSinAmbito();
            List<Long> talonarios = List.of(idTal1, idTal2);

            ExecutorService exec = Executors.newFixedThreadPool(hilos);
            CountDownLatch listos = new CountDownLatch(hilos);
            CountDownLatch arranquen = new CountDownLatch(1);
            CountDownLatch fin = new CountDownLatch(hilos);
            List<Long> exitosos = new CopyOnWriteArrayList<>();
            List<Throwable> errores = new CopyOnWriteArrayList<>();

            try {
                for (int i = 0; i < hilos; i++) {
                    Long idTal = talonarios.get(i);
                    exec.submit(() -> {
                        ActorContextHolder.set(new ActorContext(UUID.randomUUID().toString()));
                        listos.countDown();
                        try {
                            arranquen.await(10, TimeUnit.SECONDS);
                            talonarioService.crearAmbito(new ar.gob.malvinas.faltas.core.application.command.CrearTalonarioAmbitoCommand(
                                    idTal, ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion.ACTA, null,
                                    TipoActa.CONTRAVENCION.codigo(), idDependenciaContravencion, (short) 1,
                                    ar.gob.malvinas.faltas.core.domain.enums.AlcanceTalonario.DEPENDENCIA,
                                    (short) 1, LocalDate.of(2026, 1, 1), null, true, actor()));
                            exitosos.add(idTal);
                        } catch (Throwable t) {
                            errores.add(t);
                        } finally {
                            ActorContextHolder.clear();
                            fin.countDown();
                        }
                    });
                }
                assertThat(listos.await(10, TimeUnit.SECONDS)).isTrue();
                arranquen.countDown();
                assertThat(fin.await(30, TimeUnit.SECONDS)).as("ambos hilos terminaron a tiempo").isTrue();
            } finally {
                exec.shutdownNow();
            }

            try {
                assertThat(exitosos).as("exactamente una activacion exitosa").hasSize(1);
                assertThat(errores).as("exactamente un rechazo, nunca last-write-wins").hasSize(1);
                assertThat(errores.get(0)).isInstanceOf(PrecondicionVioladaException.class)
                        .hasMessageContaining("Maximo un talonario electronico activo por dependencia");

                Long activosFinal = jdbc.sql("""
                        SELECT COUNT(*) FROM num_talonario_ambito a JOIN num_talonario t ON a.talonario_id = t.id
                         WHERE a.id_dep = :dep AND a.si_activo = TRUE AND t.si_activo = TRUE
                           AND t.clase_talonario = 1 AND t.tipo_talonario = 1
                        """).param("dep", idDependenciaContravencion).query(Long.class).single();
                assertThat(activosFinal)
                        .as("estado final verificado por consulta: exactamente un talonario activo aplicable")
                        .isEqualTo(1L);
            } finally {
                for (Long idTal : talonarios) {
                    borrarTalonarioDedicado(idTal);
                }
            }
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Idempotencia secuencial y concurrente por id_tecnico")
    class Idempotencia {

        @Test
        @DisplayName("9. Reintento secuencial con el mismo id_tecnico devuelve la misma acta/número/QR sin duplicar nada")
        void reintentoSecuencialMismoIdTecnicoDevuelveLaMismaActa() {
            String idTecnico = UUID.randomUUID().toString();
            String nroDoc = nroDocDePrueba();

            ComandoResultado r1 = actaService.labrar(comandoBase(nroDoc, idTecnico));
            ComandoResultado r2 = actaService.labrar(comandoBase(nroDoc, idTecnico));

            assertThat(r2.idActa()).isEqualTo(r1.idActa());
            var fila1 = jdbc.sql("SELECT nro_acta, id_talonario, codigo_qr FROM fal_acta WHERE id = :id")
                    .param("id", r1.idActa())
                    .query((rs, n) -> List.of(rs.getObject("nro_acta"), rs.getObject("id_talonario"), rs.getString("codigo_qr")))
                    .single();
            assertThat(contarActasDelSlice()).as("una sola acta a pesar de 2 invocaciones").isEqualTo(1L);
            assertThat(jdbc.sql("SELECT COUNT(*) FROM fal_acta_articulo_infringido WHERE id_acta = :id")
                    .param("id", r1.idActa()).query(Long.class).single()).as("un solo articulo").isEqualTo(1L);
            assertThat(jdbc.sql("SELECT COUNT(*) FROM fal_acta_evento WHERE acta_id = :id AND tipo_evt = 'ACTLAB'")
                    .param("id", r1.idActa()).query(Long.class).single()).as("un solo ACTLAB").isEqualTo(1L);
            assertThat(fila1.get(0)).isNotNull();
        }

        @Test
        @DisplayName("9b. Reintento con el mismo id_tecnico y payload materialmente distinto (incluye evidencias distintas) devuelve la acta original sin ninguna modificacion (Wave 4 Parte 3B §11)")
        void reintentoConPayloadYEvidenciasDistintasNoModificaNada() {
            String idTecnico = UUID.randomUUID().toString();
            String nroDocOriginal = nroDocDePrueba();

            LabrarActaCommand original = new LabrarActaCommand(
                    TipoActa.TRANSITO, idDependencia, idInspector, F0,
                    MARCA + "domicilio-ORIGINAL", null, MARCA + "obs-original", null, null,
                    MARCA + "TITULAR-ORIGINAL", TipoDocumentoPersona.DNI, nroDocOriginal, null,
                    ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE,
                    SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO,
                    List.of(new ar.gob.malvinas.faltas.core.application.command.EvidenciaActaItem(
                            ar.gob.malvinas.faltas.core.domain.enums.TipoEvidenciaActa.PDF, "storage/original-1.jpg")),
                    idTecnico, null,
                    List.of(new ArticuloInfringidoItem(idNormativa, idArticulo)),
                    null, transitoMinimo(), null, null, null);

            ComandoResultado r1 = actaService.labrar(original);

            // Payload materialmente diferente: otro documento, otro domicilio, otra
            // observacion, y evidencias distintas de las originales. Wave 4 Parte 3D: FOTO ya
            // no se acepta en el contrato generico evidenciasActa (exige el pipeline dedicado
            // de fotografias), de modo que este fixture generico usa PDF.
            LabrarActaCommand reintentoDistinto = new LabrarActaCommand(
                    TipoActa.TRANSITO, idDependencia, idInspector, F0,
                    MARCA + "domicilio-DISTINTO-EN-REINTENTO", null, MARCA + "obs-DISTINTA-EN-REINTENTO", null, null,
                    MARCA + "TITULAR-DISTINTO", TipoDocumentoPersona.DNI, nroDocDePrueba(), null,
                    ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE,
                    SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO,
                    List.of(new ar.gob.malvinas.faltas.core.application.command.EvidenciaActaItem(
                                    ar.gob.malvinas.faltas.core.domain.enums.TipoEvidenciaActa.PDF, "storage/distinta-1.jpg"),
                            new ar.gob.malvinas.faltas.core.application.command.EvidenciaActaItem(
                                    ar.gob.malvinas.faltas.core.domain.enums.TipoEvidenciaActa.PDF, "storage/distinta-2.jpg")),
                    idTecnico, null,
                    List.of(new ArticuloInfringidoItem(idNormativa, idArticulo)),
                    null, transitoMinimo(), null, null, null);

            ComandoResultado r2 = actaService.labrar(reintentoDistinto);

            assertThat(r2.idActa()).as("misma acta original, no se crea una nueva").isEqualTo(r1.idActa());
            assertThat(contarActasDelSlice()).as("una sola acta a pesar del payload distinto").isEqualTo(1L);

            // domicilio_hecho SI es columna fisica de fal_acta; infractorNombre/infractorDocumento
            // son campos de compatibilidad que JdbcActaRepository nunca persiste (viven en
            // fal_persona via id_persona_infractor), y observaciones vive en fal_observacion
            // (DECISION_OBS-01), nunca en una columna de fal_acta.
            String domicilioActual = jdbc.sql("SELECT domicilio_hecho FROM fal_acta WHERE id = :id")
                    .param("id", r1.idActa()).query(String.class).single();
            assertThat(domicilioActual).as("domicilio_hecho conserva el valor ORIGINAL")
                    .isEqualTo(MARCA + "domicilio-ORIGINAL");

            List<String> observacionesPersistidas = jdbc.sql("""
                    SELECT observacion FROM fal_observacion
                     WHERE entidad_tipo = 1 AND entidad_id = :id AND id_acta_contexto IS NULL
                    """).param("id", r1.idActa()).query(String.class).list();
            assertThat(observacionesPersistidas)
                    .as("una sola observacion inicial, la ORIGINAL; el reintento no agrega ni reemplaza nada")
                    .containsExactly(MARCA + "obs-original");

            List<String> storageKeys = jdbc.sql("SELECT storage_key FROM fal_acta_evidencia WHERE id_acta = :id")
                    .param("id", r1.idActa()).query(String.class).list();
            assertThat(storageKeys).as("las evidencias/fotografias originales son las UNICAS persistidas")
                    .containsExactly("storage/original-1.jpg");
            assertThat(storageKeys).as("las evidencias del reintento NUNCA se procesan ni persisten")
                    .doesNotContain("storage/distinta-1.jpg", "storage/distinta-2.jpg");

            assertThat(jdbc.sql("SELECT COUNT(*) FROM fal_acta_evento WHERE acta_id = :id AND tipo_evt = 'ACTLAB'")
                    .param("id", r1.idActa()).query(Long.class).single())
                    .as("un solo ACTLAB: el reintento no emite un evento nuevo").isEqualTo(1L);
        }

        /**
         * Reintenta {@code actaService.labrar} un numero acotado de veces cuando MariaDB
         * detecta un deadlock real entre transacciones concurrentes que compiten por el mismo
         * {@code id_tecnico} ({@code CannotAcquireLockException}/{@code TransientDataAccessException}).
         *
         * <p>Un deadlock (a diferencia de una violacion UNIQUE simple, que {@code ActaService.labrar}
         * ya resuelve internamente marcando rollback-only y devolviendo la acta ganadora, sin
         * reintento) revierte la TRANSACCION COMPLETA a nivel del motor: no hay forma segura de
         * continuar dentro de la misma transaccion Spring ya invalidada sin reintroducir
         * {@code REQUIRES_NEW}/{@code NESTED}, prohibidos por {@code DECISION_OBS-TX-01}. El
         * contrato correcto -- documentado por MariaDB para {@code ERROR 1213}/deadlocks: "try
         * restarting transaction" -- es que el LLAMADOR reintente la misma solicitud idempotente;
         * el pre-check por {@code id_tecnico} al reintentar encuentra la fila ya comprometida por
         * la transaccion ganadora y devuelve el mismo resultado sin crear una segunda acta.
         */
        private ComandoResultado labrarConReintentoPorDeadlock(LabrarActaCommand cmd) {
            for (int intento = 1; intento <= 5; intento++) {
                try {
                    return actaService.labrar(cmd);
                } catch (org.springframework.dao.TransientDataAccessException deadlockOLockTimeout) {
                    if (intento == 5) throw deadlockOLockTimeout;
                }
            }
            throw new IllegalStateException("inalcanzable");
        }

        @Test
        @DisplayName("10. Reintento concurrente con el mismo id_tecnico crea una sola acta (UNIQUE de MariaDB como autoridad final)")
        void reintentoConcurrenteMismoIdTecnicoCreaUnaSolaActa() throws InterruptedException {
            String idTecnico = UUID.randomUUID().toString();
            int hilos = 3;
            ExecutorService exec = Executors.newFixedThreadPool(hilos);
            CountDownLatch listos = new CountDownLatch(hilos);
            CountDownLatch arranquen = new CountDownLatch(1);
            CountDownLatch fin = new CountDownLatch(hilos);
            List<Long> idsActa = new CopyOnWriteArrayList<>();
            List<Throwable> errores = new CopyOnWriteArrayList<>();

            try {
                for (int i = 0; i < hilos; i++) {
                    String nroDoc = nroDocDePrueba();
                    exec.submit(() -> {
                        ActorContextHolder.set(new ActorContext(UUID.randomUUID().toString()));
                        listos.countDown();
                        try {
                            arranquen.await(10, TimeUnit.SECONDS);
                            ComandoResultado res = labrarConReintentoPorDeadlock(comandoBase(nroDoc, idTecnico));
                            idsActa.add(res.idActa());
                        } catch (Throwable t) {
                            errores.add(t);
                        } finally {
                            ActorContextHolder.clear();
                            fin.countDown();
                        }
                    });
                }
                assertThat(listos.await(10, TimeUnit.SECONDS)).isTrue();
                arranquen.countDown();
                assertThat(fin.await(30, TimeUnit.SECONDS)).isTrue();
            } finally {
                exec.shutdownNow();
            }

            assertThat(errores).as("ningun hilo propaga la violacion UNIQUE cruda: %s", errores).isEmpty();
            assertThat(idsActa).hasSize(hilos);
            assertThat(Set.copyOf(idsActa)).as("todas las respuestas apuntan a la MISMA acta").hasSize(1);
            assertThat(jdbc.sql("SELECT COUNT(*) FROM fal_acta WHERE id_tecnico = :uuid")
                    .param("uuid", idTecnico).query(Long.class).single())
                    .as("una sola fila fal_acta para este id_tecnico").isEqualTo(1L);
            Long idGanadora = idsActa.get(0);
            assertThat(jdbc.sql("SELECT COUNT(*) FROM fal_acta_articulo_infringido WHERE id_acta = :id")
                    .param("id", idGanadora).query(Long.class).single()).isEqualTo(1L);
            assertThat(jdbc.sql("SELECT COUNT(*) FROM fal_acta_evento WHERE acta_id = :id AND tipo_evt = 'ACTLAB'")
                    .param("id", idGanadora).query(Long.class).single()).as("un solo ACTLAB, no uno por hilo").isEqualTo(1L);
            assertThat(jdbc.sql("SELECT COUNT(*) FROM fal_acta_snapshot WHERE id_acta = :id")
                    .param("id", idGanadora).query(Long.class).single()).isEqualTo(1L);
            assertThat(contarMovimientosManuales()).isZero();
        }
    }

    // =====================================================================
    // DECORADOR DE FALLO POST-NUMERACION
    // =====================================================================

    @TestConfiguration
    static class DecoradorFalloPostNumeracion {
        @Bean
        static org.springframework.beans.factory.config.BeanPostProcessor fallosPostNumeracionWave4P3A() {
            return new org.springframework.beans.factory.config.BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String nombre) {
                    if (bean instanceof ActaEventoRepository r) return new EventoConFalloPostNumeracion(r);
                    return bean;
                }
            };
        }
    }

    private record EventoConFalloPostNumeracion(ActaEventoRepository delegado) implements ActaEventoRepository {
        @Override
        public FalActaEvento registrar(FalActaEvento evento) {
            if (fallaEn == Etapa.POST_NUMERACION) {
                throw new IllegalStateException(FALLO + " despues de numerar, antes de ACTLAB");
            }
            return delegado.registrar(evento);
        }
        @Override public List<FalActaEvento> buscarPorActa(Long idActa) { return delegado.buscarPorActa(idActa); }
        @Override public java.util.Optional<FalActaEvento> buscarPorId(Long id) { return delegado.buscarPorId(id); }
        @Override public boolean existeCorrelacion(Long idActa, String correlacionId) {
            return delegado.existeCorrelacion(idActa, correlacionId);
        }
    }
}
