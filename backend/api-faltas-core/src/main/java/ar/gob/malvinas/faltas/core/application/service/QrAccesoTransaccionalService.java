package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.ActorTipoEvento;
import ar.gob.malvinas.faltas.core.domain.enums.CanalAccesoQr;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoAccesoQr;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaQrAcceso;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.QrAccesoRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Colaborador transaccional de {@code QrActaService.registrarAcceso}.
 *
 * <p>Creado en {@code POST-B2-TX-CONSUMERS-01}. Existe exclusivamente para resolver un
 * self-invocation real: {@code QrActaService.registrarAccesoConNotificacion} invoca
 * {@code registrarAcceso} <b>dentro del mismo bean</b>. Si la frontera transaccional se anotara
 * sobre {@code QrActaService.registrarAcceso}, esa llamada interna no atravesaria el proxy de
 * Spring y la anotacion quedaria neutralizada exactamente en el caso que mas importa proteger.
 * Extraer la escritura a un colaborador Spring separado, invocado siempre a traves de su propia
 * referencia inyectada, hace que la frontera sea efectiva sin importar quien la invoque.
 *
 * <h2>Frontera transaccional</h2>
 *
 * <p>Las tres escrituras siguientes ocurren dentro de una unica transaccion, con rollback total
 * ante cualquier fallo posterior a la primera:
 *
 * <pre>
 * 1. insertar fal_acta_qr_acceso
 * 2. insertar fal_acta_evento (QRACCA)
 * 3. insertar/actualizar fal_acta_snapshot
 * </pre>
 *
 * <p>Esta transaccion es independiente y anterior a cualquier notificacion: cuando este metodo
 * retorna, la transaccion ya hizo commit (o ya revirtio). {@code QrActaService.registrarAccesoConNotificacion}
 * invoca la colaboracion con {@code NotificacionIntentoService} <b>despues</b> de este retorno, nunca
 * dentro de la misma frontera ({@code DECISION-QR-ACCESO-NOTIFICACION-01}: ver
 * {@code 20-application/fallo-command-contracts.md}).
 */
@Service
public class QrAccesoTransaccionalService {

    private final QrAccesoRepository qrAccesoRepository;
    private final ActaEventoRepository eventoRepository;
    private final SnapshotRecalculador snapshotRecalculador;

    public QrAccesoTransaccionalService(
            QrAccesoRepository qrAccesoRepository,
            ActaEventoRepository eventoRepository,
            SnapshotRecalculador snapshotRecalculador) {
        if (qrAccesoRepository == null) {
            throw new IllegalArgumentException("qrAccesoRepository es obligatorio");
        }
        if (eventoRepository == null) {
            throw new IllegalArgumentException("eventoRepository es obligatorio");
        }
        if (snapshotRecalculador == null) {
            throw new IllegalArgumentException("snapshotRecalculador es obligatorio");
        }
        this.qrAccesoRepository = qrAccesoRepository;
        this.eventoRepository = eventoRepository;
        this.snapshotRecalculador = snapshotRecalculador;
    }

    /**
     * Registra el acceso QR, el evento {@code QRACCA} y recalcula el snapshot, en una unica
     * transaccion. Esta es la frontera real; {@code QrActaService.registrarAcceso} valida el
     * token y resuelve el acta <b>antes</b> de invocar este metodo, de modo que ninguna
     * validacion de formato o de firma corre dentro de la transaccion.
     *
     * @param acta       acta ya resuelta por el uuid tecnico del token
     * @param canal      canal de acceso
     * @param ipOrigen   IP del solicitante (nullable)
     * @param userAgent  user-agent sanitizado (nullable)
     * @param idCorrel   identificador de correlacion para la descripcion del evento
     * @param ahora      instante unico capturado por el llamador antes de mutar
     * @return el resultado seguro del acceso, sin token ni payload
     *
     * <h2>Identidad del resultado ({@code POST-B2-TX-CONSUMERS-01-R1})</h2>
     *
     * <p>{@code AccesoQrResultado.idAcceso()} es el id de la copia que devuelve
     * {@link QrAccesoRepository#registrar(FalActaQrAcceso)}, <b>no</b> el valor provisional de
     * {@link QrAccesoRepository#nextId()}. Bajo {@code JdbcQrAccesoRepository} la PK la asigna
     * {@code AUTO_INCREMENT} y difiere del contador provisional; devolver el provisional habria
     * expuesto un id que no corresponde a ninguna fila real de {@code fal_acta_qr_acceso}.
     */
    @Transactional
    public QrActaService.AccesoQrResultado registrarConfirmado(
            FalActa acta,
            CanalAccesoQr canal,
            String ipOrigen,
            String userAgent,
            String idCorrel,
            LocalDateTime ahora) {

        Long accId = qrAccesoRepository.nextId();
        FalActaQrAcceso acceso = new FalActaQrAcceso(
                accId, acta.getId(), ahora, canal, ipOrigen, userAgent,
                ResultadoAccesoQr.VALIDO, ahora);
        FalActaQrAcceso persistido = qrAccesoRepository.registrar(acceso);

        String idUser = "PORTAL";
        FalActaEvento evento = FalActaEvento.builder()
                .actaId(acta.getId())
                .tipoEvt(TipoEventoActa.QRACCA)
                .origenEvt(OrigenEvento.SISTEMA_QR)
                .fhEvt(ahora)
                .idUserEvt(idUser)
                .actorTipo(!"SYS".equals(idUser) ? ActorTipoEvento.USUARIO_INTERNO : ActorTipoEvento.SISTEMA)
                .descripcionLegible(
                        "Acceso QR registrado [correl=" + (idCorrel != null ? idCorrel : "?") + "]")
                .build();
        eventoRepository.registrar(evento);

        snapshotRecalculador.recalcularYGuardar(acta, ahora);

        return new QrActaService.AccesoQrResultado(
                persistido.getId(), acta.getId(), ahora, canal, ResultadoAccesoQr.VALIDO);
    }
}
