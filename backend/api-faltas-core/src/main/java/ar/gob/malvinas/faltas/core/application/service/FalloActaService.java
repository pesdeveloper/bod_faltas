package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.DictarFalloAbsolutorioCommand;
import ar.gob.malvinas.faltas.core.application.command.DictarFalloCondenatorioCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoPagoVoluntario;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFalloActa;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.PagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Motor de proceso del fallo del expediente de faltas.
 *
 * Dictar fallo absolutorio o condenatorio crea un FalActaFallo,
 * genera el documento correspondiente (tipo ACTO_ADMINISTRATIVO) y registra
 * los eventos FALABS/FALCON y DOCGEN.
 * La distincion absolutorio/condenatorio vive en FalActaFallo.tipoFallo.
 * NO cierra el acta. El cierre ocurre solo despues de notificacion positiva absolutoria
 * sin bloqueantes (responsabilidad de NotificacionService).
 *
 * El condenatorio notificado queda pendiente para slices futuros:
 * apelacion, firmeza, pago condena.
 */
@Service
public class FalloActaService {

    private final ActaRepository actaRepository;
    private final ActaEventoRepository eventoRepository;
    private final ActaSnapshotRepository snapshotRepository;
    private final DocumentoRepository documentoRepository;
    private final FalloActaRepository falloActaRepository;
    private final PagoVoluntarioRepository pagoVoluntarioRepository;
    private final SnapshotRecalculador snapshotRecalculador;

    public FalloActaService(
            ActaRepository actaRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            DocumentoRepository documentoRepository,
            FalloActaRepository falloActaRepository,
            PagoVoluntarioRepository pagoVoluntarioRepository,
            SnapshotRecalculador snapshotRecalculador) {
        this.actaRepository = actaRepository;
        this.eventoRepository = eventoRepository;
        this.snapshotRepository = snapshotRepository;
        this.documentoRepository = documentoRepository;
        this.falloActaRepository = falloActaRepository;
        this.pagoVoluntarioRepository = pagoVoluntarioRepository;
        this.snapshotRecalculador = snapshotRecalculador;
    }

    // -------------------------------------------------------------------------
    // DictarFalloAbsolutorio
    // -------------------------------------------------------------------------

    public ComandoResultado dictarAbsolutorio(DictarFalloAbsolutorioCommand cmd) {
        FalActa acta = cargarActaHabilitada(cmd.actaId());

        validarPrecondicionesFallo(acta, cmd.actaId());

        String idFallo = UUID.randomUUID().toString();
        FalActaFallo fallo = new FalActaFallo(
                idFallo, acta.getId(), TipoFalloActa.ABSOLUTORIO, LocalDateTime.now());
        fallo.setFundamentos(cmd.fundamentos());

        Long idDoc = documentoRepository.nextId();
        FalDocumento doc = new FalDocumento(
                idDoc, acta.getId(), TipoDocu.ACTO_ADMINISTRATIVO,
                LocalDateTime.now(), "Fallo absolutorio");
        doc.setStorageKey("storage/" + acta.getId() + "/fallo/" + idDoc);
        documentoRepository.guardar(doc);

        fallo.setDocumentoId(idDoc);
        falloActaRepository.guardar(fallo);

        registrarEvento(acta.getId(), TipoEventoActa.FALABS, String.valueOf(idDoc), null, null,
                "Fallo absolutorio dictado. " + nvl(cmd.observaciones()));
        registrarEvento(acta.getId(), TipoEventoActa.DOCGEN, String.valueOf(idDoc), null, null,
                "Documento ACTO_ADMINISTRATIVO (fallo absolutorio) generado.");

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(acta.getId(), idFallo,
                TipoEventoActa.FALABS.codigo(),
                "Fallo absolutorio dictado. Documento pendiente de firma.");
    }

    // -------------------------------------------------------------------------
    // DictarFalloCondenatorio
    // -------------------------------------------------------------------------

    public ComandoResultado dictarCondenatorio(DictarFalloCondenatorioCommand cmd) {
        FalActa acta = cargarActaHabilitada(cmd.actaId());

        if (cmd.montoCondena() == null || cmd.montoCondena().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PrecondicionVioladaException(
                    "El monto de condena debe ser mayor a cero.");
        }

        validarPrecondicionesFallo(acta, cmd.actaId());

        String idFallo = UUID.randomUUID().toString();
        FalActaFallo fallo = new FalActaFallo(
                idFallo, acta.getId(), TipoFalloActa.CONDENATORIO, LocalDateTime.now());
        fallo.setMontoCondena(cmd.montoCondena());
        fallo.setFundamentos(cmd.fundamentos());

        Long idDoc = documentoRepository.nextId();
        FalDocumento doc = new FalDocumento(
                idDoc, acta.getId(), TipoDocu.ACTO_ADMINISTRATIVO,
                LocalDateTime.now(), "Fallo condenatorio");
        doc.setStorageKey("storage/" + acta.getId() + "/fallo/" + idDoc);
        documentoRepository.guardar(doc);

        fallo.setDocumentoId(idDoc);
        falloActaRepository.guardar(fallo);

        registrarEvento(acta.getId(), TipoEventoActa.FALCON, String.valueOf(idDoc), null, null,
                "Fallo condenatorio dictado. Monto: " + cmd.montoCondena()
                        + ". " + nvl(cmd.observaciones()));
        registrarEvento(acta.getId(), TipoEventoActa.DOCGEN, String.valueOf(idDoc), null, null,
                "Documento ACTO_ADMINISTRATIVO (fallo condenatorio) generado.");

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(acta.getId(), idFallo,
                TipoEventoActa.FALCON.codigo(),
                "Fallo condenatorio dictado. Monto: " + cmd.montoCondena()
                        + ". Documento pendiente de firma.");
    }

    // -------------------------------------------------------------------------
    // Consulta
    // -------------------------------------------------------------------------

    public Optional<FalActaFallo> obtenerFallo(Long actaId) {
        actaRepository.buscarPorId(actaId)
                .orElseThrow(() -> new ActaNoEncontradaException(actaId));
        return falloActaRepository.buscarActivo(actaId);
    }

    // -------------------------------------------------------------------------
    // Internos
    // -------------------------------------------------------------------------

    private FalActa cargarActaHabilitada(Long actaId) {
        FalActa acta = actaRepository.buscarPorId(actaId)
                .orElseThrow(() -> new ActaNoEncontradaException(actaId));
        if (acta.estaCerrada()) {
            throw new PrecondicionVioladaException(
                    "El acta esta cerrada o anulada. No se puede dictar fallo.");
        }
        if (acta.estaParalizada()) {
            throw new PrecondicionVioladaException(
                    "El acta esta paralizada. No se puede dictar fallo.");
        }
        return acta;
    }

    private void validarPrecondicionesFallo(FalActa acta, Long actaId) {
        if (acta.getBloqueActual() != BloqueActual.ANAL) {
            throw new PrecondicionVioladaException(
                    "Dictar fallo requiere bloque ANAL. Bloque actual: "
                            + acta.getBloqueActual().codigo());
        }

        pagoVoluntarioRepository.buscarPorActa(actaId).ifPresent(pago -> {
            if (pago.getEstadoPagoVoluntario() == EstadoPagoVoluntario.CONFIRMADO) {
                throw new PrecondicionVioladaException(
                        "No se puede dictar fallo: existe pago voluntario confirmado.");
            }
        });

        falloActaRepository.buscarActivo(actaId).ifPresent(falloExistente -> {
            throw new PrecondicionVioladaException(
                    "Ya existe un fallo activo para este acta. Estado: "
                            + falloExistente.getEstadoFallo());
        });
    }

    private void registrarEvento(Long idActa, TipoEventoActa tipo,
                                  String idDocumento, String idNotificacion,
                                  String idOperador, String descripcion) {
        int orden = eventoRepository.proximoOrdenLogico(idActa);
        FalActaEvento evento = new FalActaEvento(
                UUID.randomUUID().toString(),
                idActa,
                tipo,
                LocalDateTime.now(),
                orden,
                idDocumento,
                idNotificacion,
                idOperador,
                descripcion,
                null
        );
        eventoRepository.registrar(evento);
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }
}
