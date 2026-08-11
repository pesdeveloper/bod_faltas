package ar.gob.malvinas.faltas.core.infrastructure;

import ar.gob.malvinas.faltas.core.domain.enums.ActorTipoEvento;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoProcesalActa;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenCaptura;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.infrastructure.jdbc.JdbcActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEventoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * IT del repositorio piloto JDBC de dominio: ActaEventoRepository sobre
 * fal_acta_evento (WAVE 0B).
 *
 * Ejecuta contra el sandbox real sb_faltas_db mediante Failsafe:
 *   mvn -P integration-tests verify
 *
 * Disciplina de datos:
 * - Todo escenario corre dentro de una transaccion que se revierte siempre. El
 *   metodo enEscenario(...) marca rollbackOnly antes de devolver, de modo que ni
 *   un escenario exitoso deja filas.
 * - El padre minimo de fal_acta se construye en la propia transaccion respetando
 *   el DDL real (fal_dependencia -> fal_dependencia_version -> fal_persona ->
 *   fal_acta). No se usan datos preexistentes del sandbox.
 * - Los datos llevan marca WAVE0B_TEST_ y documentos en un rango propio.
 * - No hay TRUNCATE ni DELETE: la limpieza es el rollback.
 */
@SpringBootTest
@ActiveProfiles("jdbc")
@EnabledIfEnvironmentVariable(named = "FALTAS_DB_URL", matches = ".+")
@DisplayName("IT WAVE-0B: JdbcActaEventoRepository sobre fal_acta_evento")
class JdbcActaEventoRepositoryIT {

    private static final String MARCA = "WAVE0B_TEST_";

    /** Instante base con precision de microsegundos: DATETIME(6) no guarda nanosegundos. */
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 8, 2, 10, 15, 30, 123_456_000);

    @Autowired
    private ActaEventoRepository repositorio;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private PlatformTransactionManager txManager;

    // =====================================================================
    // TOPOLOGIA
    // =====================================================================

    @Test
    @DisplayName("1. Con perfil jdbc el puerto lo resuelve el adaptador JDBC")
    void puertoResueltoPorAdaptadorJdbc() {
        assertThat(repositorio).isInstanceOf(JdbcActaEventoRepository.class);
    }

    // =====================================================================
    // ESCRITURA Y LECTURA
    // =====================================================================

    @Test
    @DisplayName("2. registrar asigna el id de AUTO_INCREMENT y lo devuelve en el evento")
    void registrarAsignaId() {
        enEscenario(actaId -> {
            FalActaEvento guardado = repositorio.registrar(eventoMinimo(actaId, TipoEventoActa.ACTLAB, T0));

            assertThat(guardado.getId()).isNotNull().isPositive();
            assertThat(guardado.actaId()).isEqualTo(actaId);
            assertThat(guardado.tipoEvt()).isEqualTo(TipoEventoActa.ACTLAB);
            return null;
        });
    }

    @Test
    @DisplayName("3. Round-trip completo: todos los campos persistidos se recuperan iguales")
    void roundTripDeTodosLosCampos() {
        enEscenario(actaId -> {
            String actorId = UUID.randomUUID().toString();
            String userEvt = UUID.randomUUID().toString();
            String correlacion = UUID.randomUUID().toString();

            FalActaEvento original = FalActaEvento.builder()
                    .actaId(actaId)
                    .tipoEvt(TipoEventoActa.DOCFIR)
                    .origenEvt(OrigenEvento.INTEGRACION)
                    .fhEvt(T0)
                    .bloqueFunc(BloqueActual.NOTI)
                    .estProcAnt(EstadoProcesalActa.EN_TRAMITE)
                    .estProcNvo(EstadoProcesalActa.CONCLUIDO)
                    .sitAdmAnt(SituacionAdministrativaActa.ACTIVA)
                    .sitAdmNva(SituacionAdministrativaActa.CERRADA)
                    .actorTipo(ActorTipoEvento.INTEGRACION)
                    .actorId(actorId)
                    .idPresRel(987_654L)
                    .idUserEvt(userEvt)
                    .siEvtCierre(true)
                    .siEvtExt(true)
                    .siPermiteReing(true)
                    .correlacionId(correlacion)
                    .build();

            Long id = repositorio.registrar(original).getId();
            FalActaEvento leido = repositorio.buscarPorId(id).orElseThrow();

            assertThat(leido.getId()).isEqualTo(id);
            assertThat(leido.actaId()).isEqualTo(actaId);
            assertThat(leido.tipoEvt()).isEqualTo(TipoEventoActa.DOCFIR);
            assertThat(leido.origenEvt()).isEqualTo(OrigenEvento.INTEGRACION);
            assertThat(leido.fhEvt()).isEqualTo(T0);
            assertThat(leido.bloqueFunc()).isEqualTo(BloqueActual.NOTI);
            assertThat(leido.estProcAnt()).isEqualTo(EstadoProcesalActa.EN_TRAMITE);
            assertThat(leido.estProcNvo()).isEqualTo(EstadoProcesalActa.CONCLUIDO);
            assertThat(leido.sitAdmAnt()).isEqualTo(SituacionAdministrativaActa.ACTIVA);
            assertThat(leido.sitAdmNva()).isEqualTo(SituacionAdministrativaActa.CERRADA);
            assertThat(leido.actorTipo()).isEqualTo(ActorTipoEvento.INTEGRACION);
            assertThat(leido.actorId()).isEqualTo(actorId);
            assertThat(leido.idPresRel()).isEqualTo(987_654L);
            assertThat(leido.idUserEvt()).isEqualTo(userEvt);
            assertThat(leido.siEvtCierre()).isTrue();
            assertThat(leido.siEvtExt()).isTrue();
            assertThat(leido.siPermiteReing()).isTrue();
            assertThat(leido.correlacionId()).isEqualTo(correlacion);
            return null;
        });
    }

    @Test
    @DisplayName("4. Los campos opcionales se preservan como NULL, sin defaults inventados")
    void nullsOpcionalesSePreservan() {
        enEscenario(actaId -> {
            Long id = repositorio.registrar(eventoMinimo(actaId, TipoEventoActa.ACTENR, T0)).getId();
            FalActaEvento leido = repositorio.buscarPorId(id).orElseThrow();

            assertThat(leido.bloqueFunc()).isNull();
            assertThat(leido.estProcAnt()).isNull();
            assertThat(leido.estProcNvo()).isNull();
            assertThat(leido.sitAdmAnt()).isNull();
            assertThat(leido.sitAdmNva()).isNull();
            assertThat(leido.actorTipo()).isNull();
            assertThat(leido.actorId()).isNull();
            assertThat(leido.idDocuRel()).isNull();
            assertThat(leido.idNotifRel()).isNull();
            assertThat(leido.idPresRel()).isNull();
            assertThat(leido.idUserEvt()).isNull();
            assertThat(leido.correlacionId()).isNull();
            assertThat(leido.siEvtCierre()).isFalse();
            assertThat(leido.siEvtExt()).isFalse();
            assertThat(leido.siPermiteReing()).isFalse();
            return null;
        });
    }

    @Test
    @DisplayName("5. tipo_evt se almacena como el codigo CHAR(6) del enum, no como ordinal ni name")
    void tipoEventoSeGuardaComoCodigoChar6() {
        enEscenario(actaId -> {
            Long id = repositorio.registrar(eventoMinimo(actaId, TipoEventoActa.PCOCNF, T0)).getId();

            String crudo = jdbc.sql("SELECT tipo_evt FROM fal_acta_evento WHERE id = :id")
                    .param("id", id)
                    .query(String.class)
                    .single();

            assertThat(crudo).isEqualTo("PCOCNF");
            assertThat(crudo).isEqualTo(TipoEventoActa.PCOCNF.codigo());
            assertThat(crudo).hasSize(6);
            assertThat(crudo).isNotEqualTo(String.valueOf(TipoEventoActa.PCOCNF.ordinal()));
            return null;
        });
    }

    @Test
    @DisplayName("6. Los 3 enums numericos se guardan por codigo() SMALLINT, no por ordinal")
    void enumsNumericosSeGuardanPorCodigo() {
        enEscenario(actaId -> {
            FalActaEvento evento = FalActaEvento.builder()
                    .actaId(actaId)
                    .tipoEvt(TipoEventoActa.NOTENV)
                    // codigo 6, ordinal 5: si el adaptador usara ordinal el valor no coincidiria
                    .origenEvt(OrigenEvento.SERVICIO_NOTIFICACION)
                    // codigo 6, ordinal 5
                    .actorTipo(ActorTipoEvento.NOTIFICADOR)
                    .fhEvt(T0)
                    .build();

            Long id = repositorio.registrar(evento).getId();

            var fila = jdbc.sql("SELECT origen_evt, actor_tipo FROM fal_acta_evento WHERE id = :id")
                    .param("id", id)
                    .query((rs, n) -> new short[] { rs.getShort("origen_evt"), rs.getShort("actor_tipo") })
                    .single();

            assertThat(fila[0]).isEqualTo(OrigenEvento.SERVICIO_NOTIFICACION.codigo());
            assertThat(fila[1]).isEqualTo(ActorTipoEvento.NOTIFICADOR.codigo());
            assertThat(fila[0]).isNotEqualTo((short) OrigenEvento.SERVICIO_NOTIFICACION.ordinal());
            return null;
        });
    }

    @Test
    @DisplayName("7. Todos los valores de TipoEventoActa hacen round-trip por codigo")
    void todosLosTiposDeEventoHacenRoundTrip() {
        enEscenario(actaId -> {
            for (TipoEventoActa tipo : TipoEventoActa.values()) {
                Long id = repositorio.registrar(eventoMinimo(actaId, tipo, T0)).getId();
                assertThat(repositorio.buscarPorId(id).orElseThrow().tipoEvt())
                        .as("round-trip de %s", tipo.codigo())
                        .isEqualTo(tipo);
            }
            assertThat(repositorio.buscarPorActa(actaId)).hasSize(TipoEventoActa.values().length);
            return null;
        });
    }

    // =====================================================================
    // CONSULTAS DEL PUERTO
    // =====================================================================

    @Test
    @DisplayName("8. buscarPorActa devuelve lista vacia para un acta sin eventos")
    void buscarPorActaVaciaSinEventos() {
        enEscenario(actaId -> {
            assertThat(repositorio.buscarPorActa(actaId)).isEmpty();
            return null;
        });
    }

    @Test
    @DisplayName("9. buscarPorId devuelve Optional vacio para un id inexistente")
    void buscarPorIdInexistente() {
        enEscenario(actaId -> {
            assertThat(repositorio.buscarPorId(-1L)).isEmpty();
            return null;
        });
    }

    @Test
    @DisplayName("10. El orden del timeline es fh_evt + id, identico al adaptador InMemory")
    void ordenDelTimelineCoincideConInMemory() {
        enEscenario(actaId -> {
            // Se insertan desordenados a proposito y con dos eventos en el mismo instante,
            // para que el desempate por id quede ejercitado.
            List<FalActaEvento> entrada = List.of(
                    eventoMinimo(actaId, TipoEventoActa.NOTPOS, T0.plusMinutes(30)),
                    eventoMinimo(actaId, TipoEventoActa.ACTLAB, T0),
                    eventoMinimo(actaId, TipoEventoActa.DOCFIR, T0.plusMinutes(10)),
                    eventoMinimo(actaId, TipoEventoActa.DOCGEN, T0.plusMinutes(10)),
                    eventoMinimo(actaId, TipoEventoActa.ACTENR, T0.plusMinutes(5)));

            InMemoryActaEventoRepository enMemoria = new InMemoryActaEventoRepository();
            entrada.forEach(e -> {
                repositorio.registrar(e);
                enMemoria.registrar(e);
            });

            List<TipoEventoActa> ordenJdbc = repositorio.buscarPorActa(actaId).stream()
                    .map(FalActaEvento::tipoEvt).toList();
            List<TipoEventoActa> ordenMemoria = enMemoria.buscarPorActa(actaId).stream()
                    .map(FalActaEvento::tipoEvt).toList();

            assertThat(ordenJdbc).containsExactly(
                    TipoEventoActa.ACTLAB,
                    TipoEventoActa.ACTENR,
                    TipoEventoActa.DOCFIR,
                    TipoEventoActa.DOCGEN,
                    TipoEventoActa.NOTPOS);
            assertThat(ordenJdbc)
                    .as("paridad de orden entre el adaptador JDBC y el InMemory")
                    .isEqualTo(ordenMemoria);

            List<LocalDateTime> fechas = repositorio.buscarPorActa(actaId).stream()
                    .map(FalActaEvento::fhEvt).toList();
            assertThat(fechas).isSorted();
            return null;
        });
    }

    @Test
    @DisplayName("11. Los eventos quedan aislados por acta")
    void aislamientoEntreActas() {
        enEscenario(actaA -> {
            Long actaB = insertarPadreMinimo();

            repositorio.registrar(eventoMinimo(actaA, TipoEventoActa.ACTLAB, T0));
            repositorio.registrar(eventoMinimo(actaA, TipoEventoActa.ACTENR, T0.plusMinutes(1)));
            repositorio.registrar(eventoMinimo(actaB, TipoEventoActa.ACTLAB, T0));

            assertThat(repositorio.buscarPorActa(actaA)).hasSize(2);
            assertThat(repositorio.buscarPorActa(actaB)).hasSize(1);
            assertThat(repositorio.buscarPorActa(actaA))
                    .allSatisfy(e -> assertThat(e.actaId()).isEqualTo(actaA));
            return null;
        });
    }

    @Test
    @DisplayName("12. existeCorrelacion distingue presente, ausente, null y blanco")
    void existeCorrelacionCubreTodosLosCasos() {
        enEscenario(actaId -> {
            String correlacion = UUID.randomUUID().toString();
            repositorio.registrar(FalActaEvento.builder()
                    .actaId(actaId)
                    .tipoEvt(TipoEventoActa.DOCFIR)
                    .fhEvt(T0)
                    .correlacionId(correlacion)
                    .build());

            assertThat(repositorio.existeCorrelacion(actaId, correlacion)).isTrue();
            assertThat(repositorio.existeCorrelacion(actaId, UUID.randomUUID().toString())).isFalse();
            assertThat(repositorio.existeCorrelacion(actaId, null)).isFalse();
            assertThat(repositorio.existeCorrelacion(actaId, "   ")).isFalse();

            // La correlacion es por acta: la misma marca en otra acta no cuenta.
            Long otraActa = insertarPadreMinimo();
            assertThat(repositorio.existeCorrelacion(otraActa, correlacion)).isFalse();
            return null;
        });
    }

    // =====================================================================
    // APPEND-ONLY E INTEGRIDAD
    // =====================================================================

    @Test
    @DisplayName("13. Append-only: dos registros de igual contenido generan dos filas distintas")
    void appendOnlyNoHaceUpsert() {
        enEscenario(actaId -> {
            FalActaEvento plantilla = eventoMinimo(actaId, TipoEventoActa.ACTLAB, T0);

            Long primero = repositorio.registrar(plantilla).getId();
            Long segundo = repositorio.registrar(plantilla).getId();

            assertThat(segundo).isNotEqualTo(primero);
            assertThat(repositorio.buscarPorActa(actaId)).hasSize(2);
            return null;
        });
    }

    @Test
    @DisplayName("14. Un id preexistente en el evento de entrada se ignora: lo asigna la base")
    void idDeEntradaSeIgnora() {
        enEscenario(actaId -> {
            FalActaEvento conIdFalso = FalActaEvento.builder()
                    .id(999_999_999L)
                    .actaId(actaId)
                    .tipoEvt(TipoEventoActa.ACTLAB)
                    .fhEvt(T0)
                    .build();

            FalActaEvento guardado = repositorio.registrar(conIdFalso);

            assertThat(guardado.getId()).isNotEqualTo(999_999_999L);
            assertThat(repositorio.buscarPorId(999_999_999L)).isEmpty();
            return null;
        });
    }

    @Test
    @DisplayName("15. Un acta_id inexistente viola la FK y propaga DataIntegrityViolationException")
    void fkInvalidaPropagaExcepcion() {
        TransactionTemplate tx = new TransactionTemplate(txManager);
        tx.execute(status -> {
            try {
                assertThatThrownBy(() -> repositorio.registrar(
                        eventoMinimo(-999_999L, TipoEventoActa.ACTLAB, T0)))
                        .isInstanceOf(DataIntegrityViolationException.class);
                return null;
            } finally {
                status.setRollbackOnly();
            }
        });
    }

    // =====================================================================
    // TRANSACCION Y ROLLBACK
    // =====================================================================

    @Test
    @DisplayName("16. Rollback real: la fila es visible dentro de la transaccion y desaparece al revertir")
    void rollbackRealDejaLaBaseSinRastro() {
        long[] ids = new long[2]; // [0] = id del evento, [1] = id del acta

        TransactionTemplate tx = new TransactionTemplate(txManager);
        tx.execute(status -> {
            Long actaId = insertarPadreMinimo();
            Long eventoId = repositorio.registrar(eventoMinimo(actaId, TipoEventoActa.ACTLAB, T0)).getId();

            // Dentro de la transaccion la fila existe y es legible.
            assertThat(repositorio.buscarPorId(eventoId)).isPresent();
            assertThat(repositorio.buscarPorActa(actaId)).hasSize(1);

            ids[0] = eventoId;
            ids[1] = actaId;
            status.setRollbackOnly();
            return null;
        });

        // Fuera de la transaccion revertida no debe quedar ni el evento ni el acta padre.
        assertThat(contarPorId("fal_acta_evento", ids[0]))
                .as("el evento %s no debe sobrevivir al rollback", ids[0])
                .isZero();
        assertThat(contarPorId("fal_acta", ids[1]))
                .as("el acta padre %s no debe sobrevivir al rollback", ids[1])
                .isZero();
    }

    @Test
    @DisplayName("17. Cerrada la suite, el adaptador no dejo filas propias en fal_acta_evento")
    void sinResiduosDeLaMarcaDelSlice() {
        Integer residuos = jdbc.sql("""
                        SELECT COUNT(*) FROM fal_acta_evento e
                        JOIN fal_acta a ON a.id = e.acta_id
                        WHERE a.codigo_qr LIKE :marca
                        """)
                .param("marca", MARCA + "%")
                .query(Integer.class)
                .single();

        assertThat(residuos)
                .as("ninguna fila con marca %s debe quedar fuera de una transaccion revertida", MARCA)
                .isZero();
    }

    // =====================================================================
    // SOPORTE
    // =====================================================================

    /**
     * Ejecuta el escenario dentro de una transaccion que siempre se revierte, con
     * un padre minimo de fal_acta ya disponible.
     */
    private <T> T enEscenario(Function<Long, T> escenario) {
        TransactionTemplate tx = new TransactionTemplate(txManager);
        return tx.execute(status -> {
            try {
                return escenario.apply(insertarPadreMinimo());
            } finally {
                status.setRollbackOnly();
            }
        });
    }

    /**
     * Inserta la cadena minima que el DDL exige para poder referenciar un acta:
     * fal_dependencia -> fal_dependencia_version -> fal_persona -> fal_acta.
     *
     * Solo se completan las columnas NOT NULL sin default; el resto queda en NULL.
     * doc_key de fal_persona es STORED GENERATED y por eso no se informa.
     */
    private Long insertarPadreMinimo() {
        LocalDateTime ahora = T0;
        String usuario = UUID.randomUUID().toString();

        Long idDep = insertarYObtenerId("""
                INSERT INTO fal_dependencia (nom_dep, fh_alta, id_user_alta)
                VALUES (:nom, :fhAlta, :usuario)
                """, spec -> spec
                .param("nom", MARCA + "DEP")
                .param("fhAlta", ahora)
                .param("usuario", usuario));

        short verDep = 1;
        jdbc.sql("""
                        INSERT INTO fal_dependencia_version
                            (id_dep, ver_dep, tipo_acta, fh_vig_desde, fh_alta, id_user_alta)
                        VALUES (:idDep, :verDep, :tipoActa, :vigDesde, :fhAlta, :usuario)
                        """)
                .param("idDep", idDep)
                .param("verDep", verDep)
                .param("tipoActa", (short) 1)
                .param("vigDesde", LocalDate.of(2026, 1, 1))
                .param("fhAlta", ahora)
                .param("usuario", usuario)
                .update();

        Long idPersona = insertarYObtenerId("""
                INSERT INTO fal_persona
                    (tipo_persona, tipo_documento, nro_doc, apellido, nombres, fh_alta, id_user_alta)
                VALUES (:tipoPersona, :tipoDoc, :nroDoc, :apellido, :nombres, :fhAlta, :usuario)
                """, spec -> spec
                .param("tipoPersona", (short) 1)
                .param("tipoDoc", (short) 1)
                .param("nroDoc", nroDocumentoDePrueba())
                .param("apellido", "WAVE0B")
                .param("nombres", "TEST")
                .param("fhAlta", ahora)
                .param("usuario", usuario));

        return insertarYObtenerId("""
                INSERT INTO fal_acta (
                    id_tecnico, tipo_acta, origen_captura, fh_captura,
                    fecha_acta, fecha_labrado, id_dep, ver_dep,
                    situacion_identificacion_infractor, id_persona_infractor,
                    codigo_qr, bloque_actual, est_proc_act, sit_adm_act,
                    resultado_firma_infractor, fh_alta, id_user_alta
                ) VALUES (
                    :idTecnico, :tipoActa, :origenCaptura, :fhCaptura,
                    :fechaActa, :fechaLabrado, :idDep, :verDep,
                    :situacionIdentificacion, :idPersona,
                    :codigoQr, :bloque, :estProc, :sitAdm,
                    :resultadoFirma, :fhAlta, :usuario
                )
                """, spec -> spec
                .param("idTecnico", UUID.randomUUID().toString())
                .param("tipoActa", (short) 1)
                .param("origenCaptura", OrigenCaptura.WEB_OPERADOR.codigo())
                .param("fhCaptura", ahora)
                .param("fechaActa", ahora.toLocalDate())
                .param("fechaLabrado", ahora)
                .param("idDep", idDep)
                .param("verDep", verDep)
                .param("situacionIdentificacion",
                        SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO.codigo())
                .param("idPersona", idPersona)
                .param("codigoQr", MARCA + UUID.randomUUID())
                .param("bloque", BloqueActual.ENRI.codigo())
                .param("estProc", EstadoProcesalActa.EN_TRAMITE.codigo())
                .param("sitAdm", SituacionAdministrativaActa.ACTIVA.codigo())
                .param("resultadoFirma", ResultadoFirmaInfractor.FIRMADA.codigo())
                .param("fhAlta", ahora)
                .param("usuario", usuario));
    }

    private Long insertarYObtenerId(String sql,
                                    Function<JdbcClient.StatementSpec, JdbcClient.StatementSpec> parametros) {
        KeyHolder claves = new GeneratedKeyHolder();
        parametros.apply(jdbc.sql(sql)).update(claves);
        Number id = claves.getKey();
        return Optional.ofNullable(id)
                .map(Number::longValue)
                .orElseThrow(() -> new IllegalStateException("La fixture no obtuvo el id generado"));
    }

    /**
     * nro_doc es int unsigned con CHECK entre 1 y 99999999 y participa del UNIQUE
     * (tipo_documento, doc_key). Se usa un valor aleatorio del rango alto para no
     * chocar con datos funcionales; de todos modos la fila se revierte.
     */
    private static int nroDocumentoDePrueba() {
        return 90_000_000 + (int) (Math.random() * 9_000_000);
    }

    private int contarPorId(String tabla, long id) {
        Integer total = jdbc.sql("SELECT COUNT(*) FROM " + tabla + " WHERE id = :id")
                .param("id", id)
                .query(Integer.class)
                .single();
        return total == null ? 0 : total;
    }

    private static FalActaEvento eventoMinimo(Long actaId, TipoEventoActa tipo, LocalDateTime fh) {
        return FalActaEvento.builder()
                .actaId(actaId)
                .tipoEvt(tipo)
                .fhEvt(fh)
                .build();
    }
}
