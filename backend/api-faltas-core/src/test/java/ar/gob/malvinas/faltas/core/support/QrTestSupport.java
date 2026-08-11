package ar.gob.malvinas.faltas.core.support;

import ar.gob.malvinas.faltas.core.application.port.QrTokenProtector;
import ar.gob.malvinas.faltas.core.application.service.QrAccesoTransaccionalService;
import ar.gob.malvinas.faltas.core.application.service.QrActaService;
import ar.gob.malvinas.faltas.core.infrastructure.qr.HmacSha256QrTokenProtector;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.QrAccesoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryQrAccesoRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;

/**
 * Clave y colaboradores QR para tests.
 *
 * <p>Las claves son <b>TEST_ONLY</b>: literales sinteticos fijos, escritos en el codigo de
 * prueba a proposito. No provienen del entorno local, no son secretos y no deben usarse en
 * ningun ambiente. Son fijas porque el token canonico es deterministico y los tests verifican
 * exactamente esa propiedad.
 *
 * <p>No hay keyring ni version de clave: {@code DECISION-QR-INMUTABLE-01} fija una unica
 * clave estable y la version del token es la del formato del payload.
 */
public final class QrTestSupport {

    /** TEST_ONLY. Clave de firma usada por la mayoria de los tests. 46 bytes UTF-8. */
    public static final String SECRETO_TEST_ONLY = "TEST_ONLY-qr-signing-key-primary-000000000000";

    /** TEST_ONLY. Clave distinta, para verificar que otra clave no valida los tokens. */
    public static final String SECRETO_ALTERNATIVO_TEST_ONLY = "TEST_ONLY-qr-signing-key-alternative-0000000";

    /** TEST_ONLY. Clave mas corta que el minimo tecnico, para verificar el fail-closed. */
    public static final String SECRETO_DEMASIADO_CORTO_TEST_ONLY = "TEST_ONLY-corto";

    public static QrTokenProtector protector() {
        return new HmacSha256QrTokenProtector(SECRETO_TEST_ONLY);
    }

    public static QrTokenProtector protectorAlternativo() {
        return new HmacSha256QrTokenProtector(SECRETO_ALTERNATIVO_TEST_ONLY);
    }

    /**
     * {@link QrActaService} listo para usar, sobre los mismos repositorios del test. El
     * repositorio de accesos QR es propio de esta instancia porque los tests que no verifican
     * accesos no lo necesitan.
     *
     * <p>{@code snapshotRepository} no participa directamente de {@code QrActaService} desde
     * {@code POST-B2-TX-CONSUMERS-01} (la escritura del snapshot la resuelve
     * {@code snapshotRecalculador} dentro del colaborador transaccional); se conserva como
     * parametro para no modificar la firma que ya usan otros helpers de test.
     */
    public static QrActaService qrActaService(
            ActaRepository actaRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            SnapshotRecalculador snapshotRecalculador,
            FaltasClock faltasClock) {
        QrAccesoRepository qrAccesoRepository = new InMemoryQrAccesoRepository();
        QrAccesoTransaccionalService transaccional = new QrAccesoTransaccionalService(
                qrAccesoRepository, eventoRepository, snapshotRecalculador);
        return new QrActaService(
                actaRepository, qrAccesoRepository, transaccional, protector(), faltasClock);
    }

    private QrTestSupport() {}
}
