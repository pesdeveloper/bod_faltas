package ar.gob.malvinas.faltas.core.infrastructure;

import ar.gob.malvinas.faltas.core.domain.enums.AccionPendiente;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.CodigoBandeja;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoProcesalActa;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoValorizacionActa;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoParalizacion;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoValorizacionActa;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.infrastructure.jdbc.JdbcActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * IT de {@link JdbcActaSnapshotRepository} sobre {@code fal_acta_snapshot}.
 *
 * <p>Adaptador creado en WAVE 1A2-B2. Antes de este slice el snapshot del alta quedaba en memoria
 * aunque el acta fuera a MariaDB: un rollback dejaba una proyeccion operativa viva de un acta
 * inexistente, y esa proyeccion es lo que alimenta bandejas.
 *
 * <p>El foco esta en el contrato OCC de {@code DECISION_DDL-SNAP-01} y en el mapeo de las 30
 * columnas. La verificacion del rollback conjunto con el resto del alta vive en
 * {@code ActaLabrarTransaccionalIT}.
 */
@SpringBootTest
@ActiveProfiles("jdbc")
@EnabledIfEnvironmentVariable(named = "FALTAS_DB_URL", matches = ".+")
@DisplayName("IT WAVE-1A2-B2: JdbcActaSnapshotRepository sobre fal_acta_snapshot")
class JdbcActaSnapshotRepositoryIT {

    private static final String MARCA = "WAVE1A2B2S_";
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 8, 4, 13, 0, 0, 654_321_000);
    private static final LocalDate F0 = LocalDate.of(2026, 8, 4);

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private PlatformTransactionManager txManager;

    @Autowired
    private ActaSnapshotRepository puerto;

    @Autowired
    private ActaRepository actas;

    private String actor;

    @BeforeEach
    void nuevoActor() {
        actor = UUID.randomUUID().toString();
    }

    // =====================================================================
    // TOPOLOGIA
    // =====================================================================

    @Test
    @DisplayName("1. Con perfil jdbc el puerto del snapshot lo resuelve el adaptador JDBC")
    void puertoResueltoPorJdbc() {
        assertThat(ctx.getBeanNamesForType(ActaSnapshotRepository.class)).hasSize(1);
        assertThat(puerto).isInstanceOf(JdbcActaSnapshotRepository.class);
        assertThat(ctx.getBeanNamesForType(InMemoryActaSnapshotRepository.class))
                .as("el InMemory queda fuera del perfil jdbc")
                .isEmpty();
    }

    // =====================================================================
    // ALTA: PK = id_acta, version_row = 0
    // =====================================================================

    /**
     * {@code id_acta} es a la vez PK y clave ajena: la proyeccion es uno a uno con el acta y no
     * tiene identidad propia que generar.
     */
    @Test
    @DisplayName("2. El alta usa id_acta como PK y deja version_row en 0")
    void altaConVersionCero() {
        enEscenario(idActa -> {
            FalActaSnapshot nuevo = new FalActaSnapshot(idActa);
            nuevo.setBloqueActual(BloqueActual.ENRI);
            nuevo.setUltimaActualizacion(T0);

            FalActaSnapshot persistido = puerto.guardar(nuevo);

            assertThat(persistido.getVersionRow()).isZero();
            assertThat(versionCruda(idActa)).isZero();
            assertThat(puerto.buscarPorActa(idActa)).isPresent();
            return null;
        });
    }

    /** Las 30 columnas fisicas tienen destino en un campo del modelo y vuelven intactas. */
    @Test
    @DisplayName("3. Round-trip de las 30 columnas con todos los campos informados")
    void roundTripDeLas30Columnas() {
        enEscenario(idActa -> {
            FalActaSnapshot original = new FalActaSnapshot(idActa);
            original.setBloqueActual(BloqueActual.ENRI);
            original.setEstadoProcesal(EstadoProcesalActa.EN_TRAMITE);
            original.setSituacionAdministrativa(SituacionAdministrativaActa.ACTIVA);
            original.setResultadoFinal(ResultadoFinalActa.SIN_RESULTADO_FINAL);
            original.setCodBandeja(CodigoBandeja.PENDIENTE_ANALISIS);
            original.setSubBandeja(MARCA + "sub");
            original.setAccionPendiente(AccionPendiente.NINGUNA);
            original.setTieneDocumentos(true);
            original.setTieneDocsPendientesFirma(true);
            original.setTieneDocsListosParaNotificar(true);
            original.setTieneNotificaciones(true);
            original.setNotificacionEnCurso(true);
            original.setBloqueadoCierre(true);
            original.setBloqueadoNotificacion(true);
            original.setEstadoValorizacionOperativa(EstadoValorizacionActa.PRELIMINAR);
            original.setTipoValorizacionOperativa(TipoValorizacionActa.INFRACCION_BASE);
            original.setMontoOperativoVigente(new BigDecimal("123456789012.34"));
            original.setSiMontoConfirmado(true);
            original.setLicenciaProvinciaTxt(MARCA + "BUENOS AIRES");
            original.setLicenciaUnidadTxt(MARCA + "UNIDAD");
            original.setNomenclaturaResumen(MARCA + "4-S-M7C-P10");
            original.setIdBieI(1234567);
            original.setIdBieC(7654321);
            original.setMotivoParalizacionAct(MotivoParalizacion.values()[0]);
            original.setUltimoEventoTipo(TipoEventoActa.ACTLAB);
            original.setUltimaActualizacion(T0);

            puerto.guardar(original);
            FalActaSnapshot leido = puerto.buscarPorActa(idActa).orElseThrow();

            assertThat(leido.getIdActa()).isEqualTo(idActa);
            assertThat(leido.getVersionRow()).isZero();
            assertThat(leido.getBloqueActual()).isEqualTo(BloqueActual.ENRI);
            assertThat(leido.getEstadoProcesal()).isEqualTo(EstadoProcesalActa.EN_TRAMITE);
            assertThat(leido.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ACTIVA);
            assertThat(leido.getResultadoFinal()).isEqualTo(ResultadoFinalActa.SIN_RESULTADO_FINAL);
            assertThat(leido.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_ANALISIS);
            assertThat(leido.getSubBandeja()).isEqualTo(MARCA + "sub");
            assertThat(leido.getAccionPendiente()).isEqualTo(AccionPendiente.NINGUNA);
            assertThat(leido.isTieneDocumentos()).isTrue();
            assertThat(leido.isTieneDocsPendientesFirma()).isTrue();
            assertThat(leido.isTieneDocsListosParaNotificar()).isTrue();
            assertThat(leido.isTieneNotificaciones()).isTrue();
            assertThat(leido.isNotificacionEnCurso()).isTrue();
            assertThat(leido.isBloqueadoCierre()).isTrue();
            assertThat(leido.isBloqueadoNotificacion()).isTrue();
            assertThat(leido.getIdDocuUlt()).as("sin documento vigente queda NULL").isNull();
            assertThat(leido.getValorizacionOperativaId()).isNull();
            assertThat(leido.getEstadoValorizacionOperativa()).isEqualTo(EstadoValorizacionActa.PRELIMINAR);
            assertThat(leido.getTipoValorizacionOperativa()).isEqualTo(TipoValorizacionActa.INFRACCION_BASE);
            assertThat(leido.getMontoOperativoVigente())
                    .as("DECIMAL(14,2) conserva la escala")
                    .isEqualByComparingTo("123456789012.34");
            assertThat(leido.isSiMontoConfirmado()).isTrue();
            assertThat(leido.getLicenciaProvinciaTxt()).isEqualTo(MARCA + "BUENOS AIRES");
            assertThat(leido.getLicenciaUnidadTxt()).isEqualTo(MARCA + "UNIDAD");
            assertThat(leido.getNomenclaturaResumen()).isEqualTo(MARCA + "4-S-M7C-P10");
            assertThat(leido.getIdBieI()).isEqualTo(1234567);
            assertThat(leido.getIdBieC()).isEqualTo(7654321);
            assertThat(leido.getMotivoParalizacionAct())
                    .isEqualTo(MotivoParalizacion.values()[0]);
            assertThat(leido.getUltimoEventoTipo()).isEqualTo(TipoEventoActa.ACTLAB);
            assertThat(leido.getUltimaActualizacion())
                    .as("DATETIME(6) conserva microsegundos")
                    .isEqualTo(T0);
            return null;
        });
    }

    @Test
    @DisplayName("4. Round-trip con todos los campos opcionales nulos")
    void roundTripConNulos() {
        enEscenario(idActa -> {
            puerto.guardar(new FalActaSnapshot(idActa));
            FalActaSnapshot leido = puerto.buscarPorActa(idActa).orElseThrow();

            assertThat(leido.getBloqueActual()).isNull();
            assertThat(leido.getEstadoProcesal()).isNull();
            assertThat(leido.getSituacionAdministrativa()).isNull();
            assertThat(leido.getResultadoFinal()).isNull();
            assertThat(leido.getCodBandeja()).isNull();
            assertThat(leido.getSubBandeja()).isNull();
            assertThat(leido.getAccionPendiente()).isNull();
            assertThat(leido.getMontoOperativoVigente()).isNull();
            assertThat(leido.getUltimoEventoTipo()).isNull();
            assertThat(leido.getUltimaActualizacion()).isNull();
            assertThat(leido.isTieneDocumentos()).as("los booleanos NOT NULL arrancan en false").isFalse();
            return null;
        });
    }

    // =====================================================================
    // OCC
    // =====================================================================

    @Test
    @DisplayName("5. La actualizacion con la version correcta incrementa exactamente uno")
    void updateOccIncrementaUno() {
        enEscenario(idActa -> {
            FalActaSnapshot snap = new FalActaSnapshot(idActa);
            snap.setUltimaActualizacion(T0);
            puerto.guardar(snap);

            snap.setCodBandeja(CodigoBandeja.PENDIENTE_ANALISIS);
            FalActaSnapshot actualizado = puerto.guardar(snap);

            assertThat(actualizado.getVersionRow()).isEqualTo(1);
            assertThat(versionCruda(idActa)).isEqualTo(1);
            assertThat(puerto.buscarPorActa(idActa).orElseThrow().getCodBandeja())
                    .isEqualTo(CodigoBandeja.PENDIENTE_ANALISIS);
            return null;
        });
    }

    @Test
    @DisplayName("6. Tres actualizaciones sucesivas dejan version_row en 3")
    void actualizacionesSucesivas() {
        enEscenario(idActa -> {
            FalActaSnapshot snap = new FalActaSnapshot(idActa);
            puerto.guardar(snap);
            for (int i = 0; i < 3; i++) {
                snap.setSubBandeja(MARCA + i);
                puerto.guardar(snap);
            }
            assertThat(snap.getVersionRow()).isEqualTo(3);
            assertThat(versionCruda(idActa)).isEqualTo(3);
            return null;
        });
    }

    /**
     * Una version obsoleta es un conflicto y no una escritura silenciosa. Es la razon por la que el
     * adaptador no usa un upsert de motor: {@code REPLACE} o {@code ON DUPLICATE KEY UPDATE}
     * escribirian la fila cualquiera fuera la version almacenada y anularian el OCC.
     */
    @Test
    @DisplayName("7. Una version obsoleta produce conflicto y no escribe")
    void versionObsoletaEsConflicto() {
        enEscenario(idActa -> {
            FalActaSnapshot snap = new FalActaSnapshot(idActa);
            puerto.guardar(snap);
            snap.setSubBandeja(MARCA + "primera");
            puerto.guardar(snap);

            FalActaSnapshot conVersionVieja = new FalActaSnapshot(idActa);
            conVersionVieja.setVersionRow(0);
            conVersionVieja.setSubBandeja(MARCA + "no debe escribirse");

            assertThatThrownBy(() -> puerto.guardar(conVersionVieja))
                    .isInstanceOf(ConcurrenciaConflictoException.class)
                    .hasMessageContaining("FalActaSnapshot")
                    .hasMessageContaining("version almacenada=1")
                    .hasMessageContaining("version entrante=0");

            assertThat(puerto.buscarPorActa(idActa).orElseThrow().getSubBandeja())
                    .as("la escritura conflictiva no dejo rastro")
                    .isEqualTo(MARCA + "primera");
            assertThat(versionCruda(idActa)).isEqualTo(1);
            return null;
        });
    }

    /**
     * Un snapshot recien construido llega con {@code versionRow = 0}, que es tambien la version de
     * la fila recien insertada: no es un alta duplicada, es la primera actualizacion. Es
     * exactamente lo que hace {@code SnapshotRecalculador}, que lee el snapshot existente para
     * preservar su version y, si no existe, construye uno nuevo.
     *
     * <p>Este caso fija la distincion que un discriminador basado en el cero se pierde. Con el, la
     * primera actualizacion de todo snapshot fallaria como duplicado de clave.
     */
    @Test
    @DisplayName("8. Un guardar con version 0 sobre una fila en version 0 es la primera actualizacion")
    void versionCeroSobreFilaEnCeroEsActualizacion() {
        enEscenario(idActa -> {
            puerto.guardar(new FalActaSnapshot(idActa));
            assertThat(versionCruda(idActa)).isZero();

            FalActaSnapshot desdeLaLectura = puerto.buscarPorActa(idActa).orElseThrow();
            assertThat(desdeLaLectura.getVersionRow()).isZero();
            desdeLaLectura.setSubBandeja(MARCA + "primera actualizacion");

            FalActaSnapshot actualizado = puerto.guardar(desdeLaLectura);

            assertThat(actualizado.getVersionRow()).isEqualTo(1);
            assertThat(versionCruda(idActa)).isEqualTo(1);
            assertThat(puerto.buscarPorActa(idActa).orElseThrow().getSubBandeja())
                    .isEqualTo(MARCA + "primera actualizacion");
            return null;
        });
    }

    /**
     * Con una version por encima de la almacenada tampoco se inserta ni se escribe: es conflicto.
     * Es el caso simetrico del anterior y lo que impide que un llamador "adelante" la version.
     */
    @Test
    @DisplayName("8b. Una version adelantada sobre una fila existente es conflicto")
    void versionAdelantadaEsConflicto() {
        enEscenario(idActa -> {
            puerto.guardar(new FalActaSnapshot(idActa));

            FalActaSnapshot adelantado = new FalActaSnapshot(idActa);
            adelantado.setVersionRow(7);
            adelantado.setSubBandeja(MARCA + "no debe escribirse");

            assertThatThrownBy(() -> puerto.guardar(adelantado))
                    .isInstanceOf(ConcurrenciaConflictoException.class)
                    .hasMessageContaining("version almacenada=0")
                    .hasMessageContaining("version entrante=7");
            assertThat(versionCruda(idActa)).isZero();
            assertThat(puerto.buscarPorActa(idActa).orElseThrow().getSubBandeja()).isNull();
            return null;
        });
    }

    @Test
    @DisplayName("9. Una actualizacion sobre una proyeccion inexistente es conflicto")
    void updateSobreInexistenteEsConflicto() {
        enEscenario(idActa -> {
            FalActaSnapshot inexistente = new FalActaSnapshot(idActa);
            inexistente.setVersionRow(5);

            assertThatThrownBy(() -> puerto.guardar(inexistente))
                    .isInstanceOf(ConcurrenciaConflictoException.class)
                    .hasMessageContaining("version entrante=5");
            assertThat(puerto.buscarPorActa(idActa)).isEmpty();
            return null;
        });
    }

    /**
     * El SET lista las 28 columnas mutables completas: llevar una columna nullable a NULL se
     * persiste. Es lo que hace que una bandeja que deja de aplicar se limpie de verdad en lugar de
     * quedar residual.
     */
    @Test
    @DisplayName("10. La actualizacion persiste el paso de un valor a NULL")
    void updateLimpiaValoresANulo() {
        enEscenario(idActa -> {
            FalActaSnapshot snap = new FalActaSnapshot(idActa);
            snap.setCodBandeja(CodigoBandeja.PENDIENTE_ANALISIS);
            snap.setSubBandeja(MARCA + "con valor");
            snap.setMontoOperativoVigente(new BigDecimal("100.00"));
            puerto.guardar(snap);

            snap.setCodBandeja(null);
            snap.setSubBandeja(null);
            snap.setMontoOperativoVigente(null);
            puerto.guardar(snap);

            FalActaSnapshot leido = puerto.buscarPorActa(idActa).orElseThrow();
            assertThat(leido.getCodBandeja()).isNull();
            assertThat(leido.getSubBandeja()).isNull();
            assertThat(leido.getMontoOperativoVigente()).isNull();
            return null;
        });
    }

    // =====================================================================
    // LECTURA E INTEGRIDAD
    // =====================================================================

    @Test
    @DisplayName("11. Un acta sin proyeccion devuelve vacio, y un id nulo tambien")
    void sinProyeccionVacio() {
        enEscenario(idActa -> {
            assertThat(puerto.buscarPorActa(idActa)).isEmpty();
            assertThat(puerto.buscarPorActa(null))
                    .as("un id nulo no identifica ninguna proyeccion y no consulta")
                    .isEmpty();
            return null;
        });
    }

    @Test
    @DisplayName("12. Una clave ajena violada propaga DataIntegrityViolationException")
    void claveAjenaVioladaPropaga() {
        enEscenario(idActa -> {
            FalActaSnapshot huerfano = new FalActaSnapshot(999_999_999L);
            assertThatThrownBy(() -> puerto.guardar(huerfano))
                    .isInstanceOf(DataIntegrityViolationException.class);
            return null;
        });
    }

    @Test
    @DisplayName("13. Un snapshot nulo se rechaza antes de tocar el motor")
    void snapshotNuloRechazado() {
        assertThatThrownBy(() -> puerto.guardar(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("snapshot es obligatorio");
    }

    /**
     * El codigo fisico persistido es el canonico de cada enum, no su {@code ordinal()}. Se verifica
     * leyendo la columna cruda: un mapeo por ordinal daria valores distintos y el round-trip por si
     * solo no lo detectaria.
     */
    @Test
    @DisplayName("14. Los enums se persisten por su codigo canonico y no por ordinal")
    void enumsPorCodigoCanonico() {
        enEscenario(idActa -> {
            FalActaSnapshot snap = new FalActaSnapshot(idActa);
            snap.setBloqueActual(BloqueActual.ENRI);
            snap.setCodBandeja(CodigoBandeja.PENDIENTE_ANALISIS);
            snap.setResultadoFinal(ResultadoFinalActa.SIN_RESULTADO_FINAL);
            snap.setUltimoEventoTipo(TipoEventoActa.ACTLAB);
            puerto.guardar(snap);

            var fila = jdbc.sql("""
                    SELECT bloque_actual, cod_bandeja, resultado_final, ultimo_evento_tipo
                      FROM fal_acta_snapshot WHERE id_acta = :id
                    """)
                    .param("id", idActa)
                    .query((rs, n) -> java.util.List.of(
                            rs.getString("bloque_actual"),
                            rs.getString("cod_bandeja"),
                            rs.getShort("resultado_final"),
                            rs.getString("ultimo_evento_tipo")))
                    .single();

            assertThat(fila.get(0)).isEqualTo(BloqueActual.ENRI.codigo());
            assertThat(fila.get(1)).isEqualTo(CodigoBandeja.PENDIENTE_ANALISIS.codigo());
            assertThat(fila.get(2)).isEqualTo(ResultadoFinalActa.SIN_RESULTADO_FINAL.codigo());
            assertThat(fila.get(3)).isEqualTo(TipoEventoActa.ACTLAB.codigo());
            return null;
        });
    }

    // =====================================================================
    // PARIDAD CON EL ORACULO
    // =====================================================================

    /**
     * Paridad del contrato OCC con el adaptador InMemory: mismas versiones, misma excepcion y mismo
     * mensaje en las tres ramas.
     */
    @Test
    @DisplayName("15. Paridad OCC con el oraculo InMemory en alta, update y conflicto")
    void paridadOccConElOraculo() {
        enEscenario(idActa -> {
            ActaSnapshotRepository enMemoria = new InMemoryActaSnapshotRepository();

            FalActaSnapshot paraJdbc = new FalActaSnapshot(idActa);
            FalActaSnapshot paraMemoria = new FalActaSnapshot(idActa);

            assertThat(puerto.guardar(paraJdbc).getVersionRow())
                    .isEqualTo(enMemoria.guardar(paraMemoria).getVersionRow());

            assertThat(puerto.guardar(paraJdbc).getVersionRow())
                    .as("el primer update deja version 1 en los dos")
                    .isEqualTo(enMemoria.guardar(paraMemoria).getVersionRow());

            FalActaSnapshot obsoletoJdbc = new FalActaSnapshot(idActa);
            obsoletoJdbc.setVersionRow(0);
            FalActaSnapshot obsoletoMemoria = new FalActaSnapshot(idActa);
            obsoletoMemoria.setVersionRow(0);
            Throwable deJdbc = org.assertj.core.api.Assertions
                    .catchThrowable(() -> puerto.guardar(obsoletoJdbc));
            Throwable deMemoria = org.assertj.core.api.Assertions
                    .catchThrowable(() -> enMemoria.guardar(obsoletoMemoria));

            assertThat(deJdbc).isInstanceOf(ConcurrenciaConflictoException.class);
            assertThat(deMemoria).isInstanceOf(ConcurrenciaConflictoException.class);
            assertThat(deJdbc.getMessage())
                    .as("mismo mensaje en los dos adaptadores")
                    .isEqualTo(deMemoria.getMessage());
            return null;
        });
    }

    // =====================================================================
    // SOPORTE
    // =====================================================================

    private int versionCruda(Long idActa) {
        return jdbc.sql("SELECT version_row FROM fal_acta_snapshot WHERE id_acta = :id")
                .param("id", idActa).query(Integer.class).single();
    }

    /** Escenario dentro de una transaccion que se revierte siempre. */
    private <T> T enEscenario(Function<Long, T> escenario) {
        return new TransactionTemplate(txManager).execute(status -> {
            try {
                return escenario.apply(insertarActa());
            } finally {
                status.setRollbackOnly();
            }
        });
    }

    private Long insertarActa() {
        jdbc.sql("""
                INSERT INTO fal_dependencia (nom_dep, fh_alta, id_user_alta)
                VALUES (:nom, :fhAlta, :usuario)
                """)
                .param("nom", MARCA + "DEP")
                .param("fhAlta", T0)
                .param("usuario", actor)
                .update();
        Long idDep = jdbc.sql("SELECT MAX(id) FROM fal_dependencia WHERE nom_dep = :nom")
                .param("nom", MARCA + "DEP").query(Long.class).single();
        jdbc.sql("""
                INSERT INTO fal_dependencia_version
                    (id_dep, ver_dep, tipo_acta, fh_vig_desde, fh_alta, id_user_alta)
                VALUES (:idDep, 1, 1, :vigDesde, :fhAlta, :usuario)
                """)
                .param("idDep", idDep)
                .param("vigDesde", LocalDate.of(2026, 1, 1))
                .param("fhAlta", T0)
                .param("usuario", actor)
                .update();

        FalActa acta = new FalActa(
                null, UUID.randomUUID().toString(), TipoActa.TRANSITO, idDep, null,
                F0, T0, MARCA + "domicilio", null, null, null,
                ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE,
                SituacionIdentificacionInfractor.AUSENTE, null, T0, actor);
        acta.setCodigoQr(MARCA + UUID.randomUUID());
        return actas.guardar(acta).getId();
    }
}
