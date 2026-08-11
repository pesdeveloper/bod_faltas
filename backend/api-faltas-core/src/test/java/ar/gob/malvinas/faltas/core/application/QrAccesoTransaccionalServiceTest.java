package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.service.QrAccesoTransaccionalService;
import ar.gob.malvinas.faltas.core.application.service.QrActaService;
import ar.gob.malvinas.faltas.core.domain.enums.CanalAccesoQr;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoAccesoQr;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaQrAcceso;
import ar.gob.malvinas.faltas.core.repository.QrAccesoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryFalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoCondenaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test unitario y determinista de {@code QrAccesoTransaccionalService.registrarConfirmado}
 * ({@code POST-B2-TX-CONSUMERS-01-R1}).
 *
 * <p>Antes de esta remediación, {@code registrarConfirmado} devolvía en
 * {@code AccesoQrResultado.idAcceso} el valor <b>provisional</b> de
 * {@code QrAccesoRepository.nextId()} en lugar del id real que devuelve
 * {@code QrAccesoRepository.registrar(...)}. Con el adaptador InMemory los dos valores
 * coinciden porque el InMemory conserva el id de entrada, de modo que el defecto era invisible
 * en tests unitarios y sólo se manifestaba con el adaptador JDBC, cuya PK la asigna
 * {@code AUTO_INCREMENT} y difiere del contador provisional.
 *
 * <p>Este test usa un {@link QrAccesoRepository} de prueba cuyo {@code nextId()} y
 * {@code registrar(...)} devuelven valores <b>deliberadamente distintos</b> (111 y 999) para que
 * cualquier regresión que vuelva a devolver el provisional falle de forma determinista, sin
 * depender de MariaDB.
 */
@DisplayName("POST-B2-TX-CONSUMERS-01-R1: QrAccesoTransaccionalService devuelve el id real persistido")
class QrAccesoTransaccionalServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 8, 4, 18, 0);
    private static final LocalDate FECHA = LocalDate.of(2026, 8, 4);

    private SnapshotRecalculador recalculadorSobre(InMemoryActaEventoRepository eventoRepo,
                                                    InMemoryActaSnapshotRepository snapshotRepo) {
        return new SnapshotRecalculador(
                eventoRepo, new InMemoryDocumentoRepository(), new InMemoryNotificacionRepository(),
                new InMemoryPagoVoluntarioRepository(), new InMemoryFalloActaRepository(),
                new InMemoryApelacionActaRepository(), new InMemoryPagoCondenaRepository(),
                FaltasClockTestSupport.FIXED, snapshotRepo);
    }

    private FalActa actaExistente(Long idActa) {
        return new FalActa(
                idActa, UUID.randomUUID().toString(), TipoActa.TRANSITO,
                1L, null, FECHA, AHORA, "domicilio de prueba", null, null, null,
                ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE,
                SituacionIdentificacionInfractor.AUSENTE, null, AHORA, "actor-test");
    }

    /**
     * Repositorio de prueba con {@code nextId()} y {@code registrar(...)} deliberadamente
     * distintos, para que el test sea inequívoco: 111 es el provisional que jamás debe llegar al
     * resultado, 999 es el id real que sí debe llegar.
     */
    private static final class QrAccesoRepositoryDeterminista implements QrAccesoRepository {
        private final Long idProvisional;
        private final FalActaQrAcceso persistidoSimulado;
        private FalActaQrAcceso ultimoRecibido;

        QrAccesoRepositoryDeterminista(Long idProvisional, FalActaQrAcceso persistidoSimulado) {
            this.idProvisional = idProvisional;
            this.persistidoSimulado = persistidoSimulado;
        }

        @Override
        public Long nextId() {
            return idProvisional;
        }

        @Override
        public FalActaQrAcceso registrar(FalActaQrAcceso acceso) {
            this.ultimoRecibido = acceso;
            return persistidoSimulado;
        }

        @Override
        public Optional<FalActaQrAcceso> buscarPorId(Long id) {
            return Optional.empty();
        }

        @Override
        public List<FalActaQrAcceso> listarPorActa(Long actaId) {
            return List.of();
        }

        @Override
        public int contarPorActa(Long actaId) {
            return 0;
        }
    }

    @Test
    @DisplayName("1. idAcceso es el id devuelto por registrar(...), no el provisional de nextId()")
    void idAccesoEsElIdRealPersistido() {
        long idProvisional = 111L;
        long idReal = 999L;
        Long idActa = 55L;

        FalActaQrAcceso persistidoSimulado = new FalActaQrAcceso(
                idReal, idActa, AHORA, CanalAccesoQr.PORTAL, null, null,
                ResultadoAccesoQr.VALIDO, AHORA);
        QrAccesoRepositoryDeterminista repoDeterminista =
                new QrAccesoRepositoryDeterminista(idProvisional, persistidoSimulado);

        InMemoryActaEventoRepository eventoRepo = new InMemoryActaEventoRepository();
        InMemoryActaSnapshotRepository snapshotRepo = new InMemoryActaSnapshotRepository();
        SnapshotRecalculador recalculador = recalculadorSobre(eventoRepo, snapshotRepo);

        QrAccesoTransaccionalService service =
                new QrAccesoTransaccionalService(repoDeterminista, eventoRepo, recalculador);

        QrActaService.AccesoQrResultado resultado = service.registrarConfirmado(
                actaExistente(idActa), CanalAccesoQr.PORTAL, null, null, "corr-r1", AHORA);

        assertThat(resultado.idAcceso())
                .as("el resultado debe llevar el id real que devolvio registrar(...), no el"
                        + " provisional de nextId()")
                .isEqualTo(idReal)
                .isNotEqualTo(idProvisional);
        assertThat(resultado.actaId()).isEqualTo(idActa);

        // El provisional SI debe usarse para construir el agregado antes de persistir: es el
        // valor que FalActaQrAcceso exige en su constructor porque el id no puede ser null.
        assertThat(repoDeterminista.ultimoRecibido.getId())
                .as("el agregado que se envia a registrar(...) lleva el id provisional")
                .isEqualTo(idProvisional);
    }
}
