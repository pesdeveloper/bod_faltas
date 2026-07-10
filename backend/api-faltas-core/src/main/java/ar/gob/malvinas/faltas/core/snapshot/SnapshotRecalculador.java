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
import ar.gob.malvinas.faltas.core.domain.model.FalActaParalizacion;
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
import ar.gob.malvinas.faltas.core.domain.model.FalActaContravencion;
import ar.gob.malvinas.faltas.core.domain.model.FalActaTransito;
import ar.gob.malvinas.faltas.core.repository.ActaParalizacionRepository;
import ar.gob.malvinas.faltas.core.repository.ActaContravencionRepository;
import ar.gob.malvinas.faltas.core.repository.ActaTransitoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaDocumentoRepository;
import ar.gob.malvinas.faltas.core.domain.enums.RolDocuActa;
import ar.gob.malvinas.faltas.core.repository.ObligacionPagoRepository;
import ar.gob.malvinas.faltas.core.repository.FormaPagoRepository;
import ar.gob.malvinas.faltas.core.repository.PlanPagoRefRepository;
import ar.gob.malvinas.faltas.core.repository.PagoMovimientoRepository;
import ar.gob.malvinas.faltas.core.domain.model.FalActaObligacionPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFormaPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPlanPagoRef;
import ar.gob.malvinas.faltas.core.application.service.PagoMovimientoReducer;
import ar.gob.malvinas.faltas.core.repository.EconomiaProyeccionRepository;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEconomiaProyeccion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
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
    private final FaltasClock faltasClock;
    private ValorizacionService valorizacionService;

    // Repositorios de satelites - 8F-11E (optional, no rompen tests anteriores)
    @Autowired(required = false)
    private ActaTransitoRepository actaTransitoRepository;
    @Autowired(required = false)
    private ActaParalizacionRepository actaParalizacionRepository;

    @Autowired(required = false)
    private ActaContravencionRepository actaContravencionRepository;

    // Pivot acta-documento - 8F-11J (optional, no rompen tests anteriores)
    @Autowired(required = false)
    private ActaDocumentoRepository actaDocumentoRepository;

    // Repositorios de pagos - 8F-11H (optional, no rompen tests anteriores)
    @Autowired(required = false)
    private ObligacionPagoRepository obligacionPagoRepository;
    @Autowired(required = false)
    private FormaPagoRepository formaPagoRepository;
    @Autowired(required = false)
    private PlanPagoRefRepository planPagoRefRepository;
    @Autowired(required = false)
    private PagoMovimientoRepository pagoMovimientoRepository;
    @Autowired(required = false)
    private PagoMovimientoReducer pagoMovimientoReducer;

    @Autowired(required = false)
    private EconomiaProyeccionRepository economiaProyeccionRepository;

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
            ValorizacionService valorizacionService,
            FaltasClock faltasClock) {
        this.eventoRepository = eventoRepository;
        this.documentoRepository = documentoRepository;
        this.notificacionRepository = notificacionRepository;
        this.pagoVoluntarioRepository = pagoVoluntarioRepository;
        this.falloActaRepository = falloActaRepository;
        this.apelacionActaRepository = apelacionActaRepository;
        this.pagoCondenaRepository = pagoCondenaRepository;
        this.valorizacionService = valorizacionService;
        this.faltasClock = faltasClock;
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
            PagoCondenaRepository pagoCondenaRepository,
            FaltasClock faltasClock) {
        this(eventoRepository, documentoRepository, notificacionRepository,
                pagoVoluntarioRepository, falloActaRepository, apelacionActaRepository,
                pagoCondenaRepository, null, faltasClock);
    }

    public FalActaSnapshot recalcular(FalActa acta) {
        FalActaSnapshot snap = new FalActaSnapshot(acta.getId());
        snap.setBloqueActual(acta.getBloqueActual());
        snap.setEstadoProcesal(acta.getEstadoProcesal());
        snap.setSituacionAdministrativa(acta.getSituacionAdministrativa());
        snap.setResultadoFinal(acta.getResultadoFinal());
        snap.setUltimaActualizacion(faltasClock.now());

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
                        .map(e -> e.descripcionLegible() != null
                                && e.descripcionLegible().contains("REINGRESO_PARA_REVISION"))
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

        proyectarSatelites(snap, acta.getId());
        proyectarPagos(snap, acta.getId());
        proyectarParalizacion(snap, acta.getId());
        proyectarPivot(snap, acta.getId());

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

        if (acta.getResultadoFinal() == ResultadoFinalActa.CONDENA_FIRME_PAGADA) {
            snap.setCodBandeja(CodigoBandeja.PENDIENTE_ANALISIS);
            snap.setAccionPendiente(AccionPendiente.NINGUNA);
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
                if (estadoAp == EstadoApelacionActa.RECHAZADA
                        || (estadoAp == EstadoApelacionActa.RESUELTA
                            && ultimaApelacionOpt.get().getResultadoResolucion()
                               == ar.gob.malvinas.faltas.core.domain.enums.ResultadoResolucionApelacion.RECHAZADA)) {
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

    /**
     * Proyecta campos satelite en el snapshot: transito y contravencion.
     * Requiere actaTransitoRepository y actaContravencionRepository inyectados (optional).
     * Si no estan disponibles, no hace nada (compatibilidad hacia atras).
     */
    /**
     * Proyecta idDocuUlt en el snapshot desde el pivot acta-documento.
     * Prioridad: FALLO > NOTIFICACION > ACTA_PRINCIPAL.
     * Si actaDocumentoRepository no esta disponible, no hace nada.
     */
    private void proyectarPivot(FalActaSnapshot snap, Long actaId) {
        if (actaDocumentoRepository == null) return;
        for (RolDocuActa rol : new RolDocuActa[]{RolDocuActa.FALLO, RolDocuActa.NOTIFICACION, RolDocuActa.ACTA_PRINCIPAL}) {
            java.util.Optional<ar.gob.malvinas.faltas.core.domain.model.FalActaDocumento> principal =
                    actaDocumentoRepository.buscarPrincipalPorActaYRol(actaId, rol);
            if (principal.isPresent()) {
                snap.setIdDocuUlt(principal.get().getDocumentoId());
                return;
            }
        }
    }

    private void proyectarSatelites(FalActaSnapshot snap, Long actaId) {
        if (actaTransitoRepository != null) {
            actaTransitoRepository.findByActaId(actaId).ifPresent(t -> {
                if (t.getIdProvLic() != null) {
                    snap.setLicenciaProvinciaTxt("Prov-" + t.getIdProvLic());
                }
                if (t.getUnidadTerritorialLicTipo() != null) {
                    snap.setLicenciaUnidadTxt(t.getUnidadTerritorialLicTipo().name());
                }
            });
        }
        if (actaContravencionRepository != null) {
            actaContravencionRepository.findByActaId(actaId).ifPresent(ctv -> {
                snap.setNomenclaturaResumen(ctv.generarNomenclaturaResumen());
                snap.setIdBieI(ctv.getIdBieI());
                snap.setIdBieC(ctv.getIdBieC());
            });
        }
    }
    /**
     * Proyecta motivoParalizacionAct desde el ciclo activo de paralizacion.
     * NULL cuando no hay paralizacion activa.
     */
    private void proyectarParalizacion(FalActaSnapshot snap, Long actaId) {
        if (actaParalizacionRepository != null) {
            actaParalizacionRepository.buscarActivaPorActa(actaId).ifPresent(ciclo ->
                    snap.setMotivoParalizacionAct(ciclo.getMotivoParalizacion()));
        }
    }
    /**
     * Proyecta campos de pagos en el snapshot desde el nuevo modelo de obligacion/forma/plan.
     * Solo actua cuando los repositorios de pagos estan disponibles (Spring context completo).
     * Tests sin estos repositorios no se ven afectados.
     */
    private void proyectarPagos(FalActaSnapshot snap, Long actaId) {
        // Economia operativa: consultar FalActaEconomiaProyeccion; snapshot no duplica importes ni flags de pago.
    }

}