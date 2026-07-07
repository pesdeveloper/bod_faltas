package ar.gob.malvinas.faltas.core.snapshot;

import ar.gob.malvinas.faltas.core.application.service.ValorizacionService;
import ar.gob.malvinas.faltas.core.domain.enums.AccionPendiente;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.CodigoBandeja;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoApelacionActa;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoFalloActa;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoPagoCondena;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoPagoVoluntario;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaApelacion;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.model.FalActaValorizacion;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.domain.model.FalNotificacion;
import ar.gob.malvinas.faltas.core.domain.model.FalPagoCondena;
import ar.gob.malvinas.faltas.core.domain.model.FalPagoVoluntario;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.NotificacionRepository;
import ar.gob.malvinas.faltas.core.repository.PagoCondenaRepository;
import ar.gob.malvinas.faltas.core.repository.PagoVoluntarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Recalculador del snapshot operativo del expediente.
 *
 * Deriva bandeja, sub-bandeja, accion pendiente y flags operativos
 * a partir del estado actual del acta, sus documentos, notificaciones, pago voluntario,
 * pago condena, fallo activo, apelacion y valorizacion operativa vigente.
 *
 * El snapshot no es fuente de verdad. Puede regenerarse en cualquier momento.
 *
 * Routing de bandeja (por orden de prioridad):
 *   1. Situacion administrativa transversal (CERRADA, ANULADA, ARCHIVADA, PARALIZADA, EN_GESTION_EXTERNA).
 *   2. ResultadoFinal ABSUELTO con acta aun ACTIVA: PENDIENTE_ANALISIS / NINGUNA.
 *   2b. ResultadoFinal CONDENA_FIRME con acta ACTIVA: routing por estado pago condena.
 *   3. Bloque CERR, ARCH, GEXT.
 *   4. Bloque ANAL: evalua en orden pago voluntario, apelacion, fallo.
 *   5. Bloque NOTI: evalua notificaciones.
 *   6. Bloque CAPT/ENRI: evalua documentos.
 */
@Component
public class SnapshotRecalculador {

    private final ActaEventoRepository eventoRepository;
    private final DocumentoRepository documentoRepository;
    private final NotificacionRepository notificacionRepository;
    private final PagoVoluntarioRepository pagoVoluntarioRepository;
    private final FalloActaRepository falloActaRepository;
    private final ApelacionActaRepository apelacionActaRepository;
    private final PagoCondenaRepository pagoCondenaRepository;
    private ValorizacionService valorizacionService;

    /**
     * Constructor usado por Spring (inyecta ValorizacionService).
     * El @Autowired en este constructor garantiza que Spring lo use cuando el bean
     * ValorizacionService esta disponible (contexto completo).
     */
    @Autowired
    public SnapshotRecalculador(
            ActaEventoRepository eventoRepository,
            DocumentoRepository documentoRepository,
            NotificacionRepository notificacionRepository,
            PagoVoluntarioRepository pagoVoluntarioRepository,
            FalloActaRepository falloActaRepository,
            ApelacionActaRepository apelacionActaRepository,
            PagoCondenaRepository pagoCondenaRepository,
            ValorizacionService valorizacionService) {
        this.eventoRepository = eventoRepository;
        this.documentoRepository = documentoRepository;
        this.notificacionRepository = notificacionRepository;
        this.pagoVoluntarioRepository = pagoVoluntarioRepository;
        this.falloActaRepository = falloActaRepository;
        this.apelacionActaRepository = apelacionActaRepository;
        this.pagoCondenaRepository = pagoCondenaRepository;
        this.valorizacionService = valorizacionService;
    }

    /**
     * Constructor de compatibilidad hacia atras para tests que no prueban valorizacion en snapshot.
     * Cuando valorizacionService=null, los cinco campos de valorizacion quedan en null/false.
     */
    public SnapshotRecalculador(
            ActaEventoRepository eventoRepository,
            DocumentoRepository documentoRepository,
            NotificacionRepository notificacionRepository,
            PagoVoluntarioRepository pagoVoluntarioRepository,
            FalloActaRepository falloActaRepository,
            ApelacionActaRepository apelacionActaRepository,
            PagoCondenaRepository pagoCondenaRepository) {
        this(eventoRepository, documentoRepository, notificacionRepository,
                pagoVoluntarioRepository, falloActaRepository, apelacionActaRepository,
                pagoCondenaRepository, null);
    }

    public FalActaSnapshot recalcular(FalActa acta) {
        FalActaSnapshot snap = new FalActaSnapshot(acta.getId());
        snap.setBloqueActual(acta.getBloqueActual());
        snap.setEstadoProcesal(acta.getEstadoProcesal());
        snap.setSituacionAdministrativa(acta.getSituacionAdministrativa());
        snap.setResultadoFinal(acta.getResultadoFinal());
        snap.setUltimaActualizacion(LocalDateTime.now());

        List<FalDocumento> docs = documentoRepository.buscarPorActa(acta.getId());
        List<FalNotificacion> notifs = notificacionRepository.buscarPorActa(acta.getId());
        List<FalActaEvento> eventos = eventoRepository.buscarPorActa(acta.getId());
        Optional<FalPagoVoluntario> pagoOpt = pagoVoluntarioRepository.buscarPorActa(acta.getId());
        Optional<FalActaFallo> falloOpt = falloActaRepository.buscarActivo(acta.getId());
        Optional<FalActaApelacion> ultimaApelacionOpt = apelacionActaRepository.buscarUltima(acta.getId());
        Optional<FalPagoCondena> pagoCondenaOpt = pagoCondenaRepository.buscarPorActa(acta.getId());

        boolean tieneDocumentos = !docs.isEmpty();
        boolean tieneDocsPendientesFirma = docs.stream()
                .anyMatch(d -> d.getEstadoDocu() == ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu.PENDIENTE_FIRMA);
        boolean tieneDocsFirmados = docs.stream()
                .anyMatch(d -> d.getEstadoDocu() == ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu.FIRMADO);
        boolean todosDocsFirmados = tieneDocumentos && docs.stream()
                .allMatch(d -> d.getEstadoDocu() == ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu.FIRMADO
                        || d.getEstadoDocu() == ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu.EMITIDO
                        || d.getEstadoDocu() == ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu.ADJUNTO);
        boolean tieneNotificaciones = !notifs.isEmpty();
        boolean notificacionEnCurso = notifs.stream()
                .anyMatch(n -> n.getEstado() == EstadoNotificacion.EN_PROCESO
                        || n.getEstado() == EstadoNotificacion.PENDIENTE_ENVIO);
        boolean tieneNotificacionPositiva = notifs.stream()
                .anyMatch(n -> n.getEstado() == EstadoNotificacion.CON_ACUSE_POSITIVO);

        snap.setTieneDocumentos(tieneDocumentos);
        snap.setTieneDocsPendientesFirma(tieneDocsPendientesFirma);
        snap.setTieneDocsListosParaNotificar(tieneDocsFirmados);
        snap.setTieneNotificaciones(tieneNotificaciones);
        snap.setNotificacionEnCurso(notificacionEnCurso);

        if (!eventos.isEmpty()) {
            snap.setUltimoEventoTipo(eventos.get(eventos.size() - 1).tipoEvt());
        }

        // REINGRESO_PARA_REVISION emite EXTRET igual que REINGRESO_SIN_PAGO.
        boolean esReingresoParaRevision = snap.getUltimoEventoTipo() == TipoEventoActa.EXTRET
                && eventos.stream()
                        .filter(e -> e.tipoEvt() == TipoEventoActa.EXTRET)
                        .reduce((a, b) -> b)
                        .map(e -> e.descripcion() != null
                                && e.descripcion().contains("REINGRESO_PARA_REVISION"))
                        .orElse(false);

        boolean tuvoPagoExterno = eventos.stream()
                .anyMatch(e -> e.tipoEvt() == TipoEventoActa.PAGAPR);

        // Proyectar valorización operativa (solo cuando el servicio esta disponible)
        proyectarValorizacion(snap, acta.getId());

        derivarBandejaYAccion(snap, acta, pagoOpt, falloOpt, ultimaApelacionOpt,
                pagoCondenaOpt,
                tieneDocumentos, tieneDocsPendientesFirma,
                todosDocsFirmados, tieneDocsFirmados, tieneNotificaciones,
                notificacionEnCurso, tieneNotificacionPositiva, esReingresoParaRevision,
                tuvoPagoExterno);

        return snap;
    }

    /**
     * Proyecta los cinco campos de valorizacion operativa en el snapshot.
     * Si no hay servicio disponible o no hay vigente, deja los campos en null/false.
     */
    private void proyectarValorizacion(FalActaSnapshot snap, Long actaId) {
        if (valorizacionService == null) {
            snap.setValorizacionOperativaId(null);
            snap.setEstadoValorizacionOperativa(null);
            snap.setTipoValorizacionOperativa(null);
            snap.setMontoOperativoVigente(null);
            snap.setSiMontoConfirmado(false);
            return;
        }
        Optional<FalActaValorizacion> operativaOpt = valorizacionService.seleccionarOperativa(actaId);
        if (operativaOpt.isPresent()) {
            FalActaValorizacion op = operativaOpt.get();
            snap.setValorizacionOperativaId(op.getId());
            snap.setEstadoValorizacionOperativa(op.getEstadoValorizacion());
            snap.setTipoValorizacionOperativa(op.getTipoValorizacionActa());
            snap.setMontoOperativoVigente(op.getMontoFinal());
            snap.setSiMontoConfirmado(true);
        } else {
            snap.setValorizacionOperativaId(null);
            snap.setEstadoValorizacionOperativa(null);
            snap.setTipoValorizacionOperativa(null);
            snap.setMontoOperativoVigente(null);
            snap.setSiMontoConfirmado(false);
        }
    }

    private void derivarBandejaYAccion(
            FalActaSnapshot snap,
            FalActa acta,
            Optional<FalPagoVoluntario> pagoOpt,
            Optional<FalActaFallo> falloOpt,
            Optional<FalActaApelacion> ultimaApelacionOpt,
            Optional<FalPagoCondena> pagoCondenaOpt,
            boolean tieneDocumentos,
            boolean tieneDocsPendientesFirma,
            boolean todosDocsFirmados,
            boolean tieneDocsFirmados,
            boolean tieneNotificaciones,
            boolean notificacionEnCurso,
            boolean tieneNotificacionPositiva,
            boolean esReingresoParaRevision,
            boolean tuvoPagoExterno) {

        switch (acta.getSituacionAdministrativa()) {
            case CERRADA, ANULADA -> {
                snap.setCodBandeja(CodigoBandeja.CERRADAS);
                snap.setAccionPendiente(AccionPendiente.NINGUNA);
                return;
            }
            case ARCHIVADA -> {
                snap.setCodBandeja(CodigoBandeja.ARCHIVO);
                snap.setAccionPendiente(AccionPendiente.NINGUNA);
                return;
            }
            case PARALIZADA -> {
                snap.setCodBandeja(CodigoBandeja.PARALIZADAS);
                snap.setAccionPendiente(AccionPendiente.NINGUNA);
                return;
            }
            case EN_GESTION_EXTERNA -> {
                snap.setCodBandeja(CodigoBandeja.GESTION_EXTERNA);
                snap.setAccionPendiente(AccionPendiente.NINGUNA);
                return;
            }
            default -> { /* ACTIVA: continuar */ }
        }

        if (acta.getResultadoFinal() == ResultadoFinalActa.ABSUELTO) {
            snap.setCodBandeja(CodigoBandeja.PENDIENTE_ANALISIS);
            snap.setAccionPendiente(AccionPendiente.NINGUNA);
            return;
        }

        if (acta.getResultadoFinal() == ResultadoFinalActa.CONDENA_FIRME
                && acta.getSituacionAdministrativa() == SituacionAdministrativaActa.ACTIVA
                && pagoCondenaOpt.isPresent()
                && pagoCondenaOpt.get().getEstadoPagoCondena() == EstadoPagoCondena.CONFIRMADO) {
            snap.setCodBandeja(CodigoBandeja.PENDIENTE_ANALISIS);
            snap.setAccionPendiente(AccionPendiente.NINGUNA);
            return;
        }

        if (acta.getResultadoFinal() == ResultadoFinalActa.CONDENA_FIRME) {
            if (esReingresoParaRevision) {
                snap.setCodBandeja(CodigoBandeja.PENDIENTE_ANALISIS);
                snap.setAccionPendiente(AccionPendiente.DICTAR_FALLO);
                return;
            }
            if (pagoCondenaOpt.isPresent()) {
                EstadoPagoCondena estadoPC = pagoCondenaOpt.get().getEstadoPagoCondena();
                if (estadoPC == EstadoPagoCondena.INFORMADO) {
                    snap.setCodBandeja(CodigoBandeja.PENDIENTE_CONFIRMACION_PAGO_CONDENA);
                    snap.setAccionPendiente(AccionPendiente.CONFIRMAR_PAGO_CONDENA);
                    return;
                }
                if (estadoPC == EstadoPagoCondena.OBSERVADO) {
                    snap.setCodBandeja(CodigoBandeja.PENDIENTE_PAGO_CONDENA);
                    snap.setAccionPendiente(AccionPendiente.CORREGIR_PAGO_CONDENA);
                    return;
                }
            }
            if (tuvoPagoExterno) {
                snap.setCodBandeja(CodigoBandeja.PENDIENTE_ANALISIS);
                snap.setAccionPendiente(AccionPendiente.NINGUNA);
                return;
            }
            snap.setCodBandeja(CodigoBandeja.PENDIENTE_PAGO_CONDENA);
            snap.setAccionPendiente(AccionPendiente.GESTIONAR_PAGO_CONDENA);
            return;
        }

        BloqueActual bloque = acta.getBloqueActual();

        if (bloque == BloqueActual.CERR) {
            snap.setCodBandeja(CodigoBandeja.CERRADAS);
            snap.setAccionPendiente(AccionPendiente.NINGUNA);
            return;
        }
        if (bloque == BloqueActual.ARCH) {
            snap.setCodBandeja(CodigoBandeja.ARCHIVO);
            snap.setAccionPendiente(AccionPendiente.NINGUNA);
            return;
        }
        if (bloque == BloqueActual.GEXT) {
            snap.setCodBandeja(CodigoBandeja.GESTION_EXTERNA);
            snap.setAccionPendiente(AccionPendiente.NINGUNA);
            return;
        }

        if (bloque == BloqueActual.ANAL) {
            if (pagoOpt.isPresent()) {
                EstadoPagoVoluntario estadoPago = pagoOpt.get().getEstadoPagoVoluntario();
                if (estadoPago == EstadoPagoVoluntario.PENDIENTE_CONFIRMACION) {
                    snap.setCodBandeja(CodigoBandeja.PENDIENTE_CONFIRMACION_PAGO);
                    snap.setAccionPendiente(AccionPendiente.CONFIRMAR_PAGO);
                    return;
                }
                if (estadoPago == EstadoPagoVoluntario.OBSERVADO) {
                    snap.setCodBandeja(CodigoBandeja.PENDIENTE_ANALISIS);
                    snap.setAccionPendiente(AccionPendiente.CORREGIR_PAGO);
                    return;
                }
            }

            if (ultimaApelacionOpt.isPresent()) {
                EstadoApelacionActa estadoAp = ultimaApelacionOpt.get().getEstadoApelacion();
                if (estadoAp == EstadoApelacionActa.PRESENTADA) {
                    snap.setCodBandeja(CodigoBandeja.CON_APELACION);
                    snap.setAccionPendiente(AccionPendiente.RESOLVER_APELACION);
                    return;
                }
                if (estadoAp == EstadoApelacionActa.RECHAZADA) {
                    snap.setCodBandeja(CodigoBandeja.PENDIENTE_ANALISIS);
                    snap.setAccionPendiente(AccionPendiente.DECLARAR_CONDENA_FIRME);
                    return;
                }
            }

            if (falloOpt.isPresent()) {
                EstadoFalloActa estadoFallo = falloOpt.get().getEstadoFallo();
                if (estadoFallo == EstadoFalloActa.DICTADO
                        || estadoFallo == EstadoFalloActa.PENDIENTE_FIRMA) {
                    snap.setCodBandeja(CodigoBandeja.PENDIENTE_FIRMA);
                    snap.setAccionPendiente(AccionPendiente.FIRMAR_DOCUMENTO);
                    return;
                }
                if (estadoFallo == EstadoFalloActa.FIRMADO
                        || estadoFallo == EstadoFalloActa.PENDIENTE_NOTIFICACION) {
                    snap.setCodBandeja(CodigoBandeja.PENDIENTE_NOTIFICACION);
                    snap.setAccionPendiente(AccionPendiente.ENVIAR_NOTIFICACION);
                    return;
                }
                if (estadoFallo == EstadoFalloActa.NOTIFICADO) {
                    snap.setCodBandeja(CodigoBandeja.PENDIENTES_FALLO);
                    snap.setAccionPendiente(AccionPendiente.NINGUNA);
                    return;
                }
            }

            if (pagoOpt.isPresent()
                    && pagoOpt.get().getEstadoPagoVoluntario() == EstadoPagoVoluntario.VENCIDO) {
                snap.setCodBandeja(CodigoBandeja.PENDIENTE_ANALISIS);
                snap.setAccionPendiente(AccionPendiente.DICTAR_FALLO);
                return;
            }

            snap.setCodBandeja(CodigoBandeja.PENDIENTE_ANALISIS);
            snap.setAccionPendiente(AccionPendiente.DICTAR_FALLO);
            return;
        }

        if (bloque == BloqueActual.NOTI) {
            if (notificacionEnCurso) {
                snap.setCodBandeja(CodigoBandeja.EN_NOTIFICACION);
                snap.setAccionPendiente(AccionPendiente.EVALUAR_NOTIFICACION);
            } else if (tieneNotificaciones && tieneNotificacionPositiva) {
                snap.setCodBandeja(CodigoBandeja.PENDIENTE_ANALISIS);
                snap.setAccionPendiente(AccionPendiente.DICTAR_FALLO);
            } else if (tieneNotificaciones) {
                snap.setCodBandeja(CodigoBandeja.PENDIENTE_ANALISIS);
                snap.setAccionPendiente(AccionPendiente.DECIDIR_REINTENTO_O_GESTION);
            } else {
                snap.setCodBandeja(CodigoBandeja.PENDIENTE_NOTIFICACION);
                snap.setAccionPendiente(AccionPendiente.ENVIAR_NOTIFICACION);
            }
            return;
        }

        if (!tieneDocumentos) {
            if (bloque == BloqueActual.CAPT) {
                snap.setCodBandeja(CodigoBandeja.ACTAS_EN_ENRIQUECIMIENTO);
                snap.setAccionPendiente(AccionPendiente.COMPLETAR_CAPTURA);
            } else {
                snap.setCodBandeja(CodigoBandeja.ACTAS_EN_ENRIQUECIMIENTO);
                snap.setAccionPendiente(AccionPendiente.GENERAR_DOCUMENTO);
            }
            return;
        }

        if (tieneDocsPendientesFirma) {
            snap.setCodBandeja(CodigoBandeja.PENDIENTE_FIRMA);
            snap.setAccionPendiente(AccionPendiente.FIRMAR_DOCUMENTO);
            return;
        }

        if (todosDocsFirmados) {
            snap.setCodBandeja(CodigoBandeja.PENDIENTE_NOTIFICACION);
            snap.setAccionPendiente(AccionPendiente.ENVIAR_NOTIFICACION);
            return;
        }

        snap.setCodBandeja(CodigoBandeja.PENDIENTE_PREPARACION_DOCUMENTAL);
        snap.setAccionPendiente(AccionPendiente.GENERAR_DOCUMENTO);
    }
}
