package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.DerivarGestionExternaCommand;
import ar.gob.malvinas.faltas.core.application.command.ReingresarDesdeGestionExternaCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarPagoExternoGestionCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoGestionExterna;
import ar.gob.malvinas.faltas.core.domain.enums.ModoReingresoGestionExterna;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoGestionExterna;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.ActorTipoEvento;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.GestionExternaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.model.FalGestionExterna;
import ar.gob.malvinas.faltas.core.domain.model.FalPagoCondena;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.application.port.BloqueantesMaterialesChecker;
import ar.gob.malvinas.faltas.core.repository.GestionExternaRepository;
import ar.gob.malvinas.faltas.core.repository.PagoCondenaRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.springframework.stereotype.Service;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Motor del circuito de gestion externa.
 *
 * Slice 6A: derivacion (EXTDER).
 * Slice 6B: reingreso (EXTRET) con modos REINGRESO_PARA_REVISION y REINGRESO_SIN_PAGO.
 * Slice 6C: pago externo (PAGAPR) desde gestion externa activa.
 * Slice 6D-1: validacion de pares resultado/modo habilitados:
 *   - SIN_PAGO + REINGRESO_SIN_PAGO
 *   - SIN_CAMBIOS + REINGRESO_PARA_REVISION
 * Slice 6D-2: reingreso con dictamen externo:
 *   - ABSUELVE + REINGRESO_PARA_NUEVO_FALLO (vuelve a ANAL; no genera fallo automatico)
 *   - CONFIRMA_CONDENA + REINGRESO_CON_DICTAMEN (mantiene CONDENA_FIRME; vuelve a ANAL)
 *   - MODIFICA_MONTO + REINGRESO_CON_DICTAMEN (registra monto; vuelve a ANAL; sin nuevo fallo automatico)
 * Reservados: REINGRESO_PARA_CIERRE, REINGRESO_CON_PAGO.
 */
@Service
public class GestionExternaService {

    private final ActaRepository actaRepository;
    private final ActaEventoRepository eventoRepository;
    private final ActaSnapshotRepository snapshotRepository;
    private final PagoCondenaRepository pagoCondenaRepository;
    private final GestionExternaRepository gestionExternaRepository;
    private final SnapshotRecalculador snapshotRecalculador;
    private final BloqueantesMaterialesChecker bloqueantesChecker;
    private final FaltasClock faltasClock;

    public GestionExternaService(
            ActaRepository actaRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            PagoCondenaRepository pagoCondenaRepository,
            GestionExternaRepository gestionExternaRepository,
            SnapshotRecalculador snapshotRecalculador,
            BloqueantesMaterialesChecker bloqueantesChecker,
            FaltasClock faltasClock) {
        this.faltasClock = faltasClock;
        this.actaRepository = actaRepository;
        this.eventoRepository = eventoRepository;
        this.snapshotRepository = snapshotRepository;
        this.pagoCondenaRepository = pagoCondenaRepository;
        this.gestionExternaRepository = gestionExternaRepository;
        this.snapshotRecalculador = snapshotRecalculador;
        this.bloqueantesChecker = bloqueantesChecker;
    }

    // -------------------------------------------------------------------------
    // Derivar (Slice 6A)
    // -------------------------------------------------------------------------

    public ComandoResultado derivar(DerivarGestionExternaCommand cmd) {
        FalActa acta = actaRepository.buscarPorId(cmd.actaId())
                .orElseThrow(() -> new ActaNoEncontradaException(cmd.actaId()));

        validarSituacionPermiteDerivacion(acta);
        validarNoExisteGestionActiva(cmd.actaId());
        validarTipoGestion(cmd);
        validarMotivoDerivacion(cmd);
        validarResultadoHabilitaDerivacion(acta);
        validarPagoCondenaNoConfirmado(cmd.actaId());

        BloqueActual bloqueOrigen = acta.getBloqueActual();
        SituacionAdministrativaActa situacionOrigen = acta.getSituacionAdministrativa();

        Optional<FalActaSnapshot> snapActual = snapshotRepository.buscarPorActa(cmd.actaId());

        LocalDateTime ahora = faltasClock.now();
        Long gestionId = gestionExternaRepository.nextId();
        FalGestionExterna gestion = new FalGestionExterna(gestionId, cmd.actaId(), ahora, "SYS");
        gestion.setTipoGestionExterna(cmd.tipoGestionExterna());
        gestion.setEstadoGestionExterna(EstadoGestionExterna.DERIVADA);
        gestion.setResultadoGestionExterna(ResultadoGestionExterna.SIN_RESULTADO);
        gestion.setModoReingresoGestionExterna(null);
        gestion.setMotivoDerivacion(cmd.motivoDerivacion());
        if (cmd.observaciones() != null) gestion.setObservacionesDerivacion(cmd.observaciones());
        gestion.setFechaDerivacion(ahora);
        gestion.setSiActiva(true);

        gestion.setBloqueOrigen(bloqueOrigen);
        gestion.setSituacionAdministrativaOrigen(situacionOrigen);
        snapActual.ifPresent(s -> {
            gestion.setCodigoBandejaOrigen(s.getCodBandeja());
            gestion.setAccionPendienteOrigen(s.getAccionPendiente());
        });

        acta.setBloqueActual(BloqueActual.GEXT);
        acta.setSituacionAdministrativa(SituacionAdministrativaActa.EN_GESTION_EXTERNA);
        actaRepository.guardar(acta);
        gestionExternaRepository.guardar(gestion);

        registrarEvento(acta.getId(), TipoEventoActa.EXTDER, null, null, null,
                "Acta derivada a gestion externa. Tipo: " + cmd.tipoGestionExterna()
                        + ". Motivo: " + cmd.motivoDerivacion()
                        + nvl(cmd.observaciones() != null ? ". " + cmd.observaciones() : null));

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(acta.getId(), String.valueOf(gestion.getId()),
                TipoEventoActa.EXTDER.codigo(),
                "Acta derivada a gestion externa. Estado: DERIVADA. Tipo: " + cmd.tipoGestionExterna());
    }

    // -------------------------------------------------------------------------
    // Reingresar (Slice 6B)
    // -------------------------------------------------------------------------

    public ComandoResultado reingresar(ReingresarDesdeGestionExternaCommand cmd) {
        FalActa acta = actaRepository.buscarPorId(cmd.actaId())
                .orElseThrow(() -> new ActaNoEncontradaException(cmd.actaId()));

        validarModoReingresoNoNulo(cmd);
        validarMotivoReingreso(cmd);
        validarActaEnGestionExternaParaReingreso(acta);
        validarBloqueActualEsGext(acta);
        validarModoHabilitadoEnSlice6B(cmd);
        validarModoRequiereResultadoExplicito(cmd);
        validarParResultadoModo(cmd);

        FalGestionExterna gestion = gestionExternaRepository.buscarActiva(cmd.actaId())
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "No existe gestion externa activa para el acta: " + cmd.actaId()
                                + ". No se puede reingresar."));

        validarGestionActivaEstadoPermiteReingreso(gestion);

        if (cmd.modoReingresoGestionExterna() == ModoReingresoGestionExterna.REINGRESO_SIN_PAGO) {
            validarResultadoFinalCondenaFirmeParaReingreso(acta);
        }
        validarCondenaFirmeParaModosDictamen(cmd, acta);
        validarMontoParaModificaMonto(cmd);

        // Cerrar gestion externa activa preservando trazabilidad del ciclo externo
        gestion.setSiActiva(false);
        gestion.setEstadoGestionExterna(EstadoGestionExterna.REINGRESADA);
        gestion.setModoReingresoGestionExterna(cmd.modoReingresoGestionExterna());
        gestion.setMotivoReingreso(cmd.motivoReingreso());
        gestion.setFechaReingreso(faltasClock.now());
        if (cmd.resultadoGestionExterna() != null) {
            gestion.setResultadoGestionExterna(cmd.resultadoGestionExterna());
        }
        if (cmd.observaciones() != null) {
            gestion.setObservacionesReingreso(cmd.observaciones());
        }
        if (cmd.montoResultado() != null) {
            gestion.setMontoResultado(cmd.montoResultado());
        }
        gestionExternaRepository.guardar(gestion);

        // Retorna a ANAL / ACTIVA en todos los modos de dictamen
        if (cmd.resultadoGestionExterna() == ResultadoGestionExterna.CONFIRMA_CONDENA) {
            acta.setResultadoFinal(ResultadoFinalActa.CONDENA_FIRME);
        }
        acta.setSituacionAdministrativa(SituacionAdministrativaActa.ACTIVA);
        acta.setBloqueActual(BloqueActual.ANAL);
        actaRepository.guardar(acta);

        registrarEvento(acta.getId(), TipoEventoActa.EXTRET, null, null, null,
                "Acta reingresada desde gestion externa. Modo: " + cmd.modoReingresoGestionExterna()
                        + ". Motivo: " + cmd.motivoReingreso()
                        + nvl(cmd.observaciones() != null ? ". " + cmd.observaciones() : null));

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(acta.getId(), String.valueOf(gestion.getId()),
                TipoEventoActa.EXTRET.codigo(),
                "Acta reingresada desde gestion externa. Modo: " + cmd.modoReingresoGestionExterna());
    }


    // -------------------------------------------------------------------------
    // Registrar pago externo (Slice 6C)
    // -------------------------------------------------------------------------

    /**
     * Registra el pago externo de una gestion externa activa (PAGAPR).
     *
     * Aplica a cualquier tipo de gestion externa: apremio, juzgado de paz u otra.
     *
     * Si pasan las precondiciones, siempre registra PAGAPR y cierra la gestion externa.
     * Los bloqueantes solo determinan si se emite o no CIERRA:
     *   - sin bloqueantes: PAGAPR + CIERRA, acta CERRADA/CERR.
     *   - con bloqueantes: solo PAGAPR, acta ACTIVA/ANAL (transitorio hasta Slice 7).
     *
     * Las observaciones opcionales se incluyen en FalActaEvento.descripcion como puente
     * transitorio. En Slice 9/JDBC deben registrarse en fal_observacion (entidad_tipo=GESTION_EXTERNA).
     */
    public ComandoResultado registrarPagoExternoGestion(RegistrarPagoExternoGestionCommand cmd) {
        FalActa acta = actaRepository.buscarPorId(cmd.actaId())
                .orElseThrow(() -> new ActaNoEncontradaException(cmd.actaId()));

        validarActaPermitePagoExterno(acta);
        validarBloqueActualEsGextParaPago(acta);
        validarResultadoFinalEsCondenaFirme(acta);

        FalGestionExterna gestion = gestionExternaRepository.buscarActiva(cmd.actaId())
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "No existe gestion externa activa para el acta: " + cmd.actaId()
                                + ". No se puede registrar pago externo."));

        validarGestionActivaParaPagoExterno(gestion);
        validarPagoCondenaNoConfirmadoParaPagoExterno(cmd.actaId());

        // Cerrar la gestion externa con resultado de pago externo
        gestion.setSiActiva(false);
        gestion.setEstadoGestionExterna(EstadoGestionExterna.CERRADA_EXTERNA);
        gestion.setResultadoGestionExterna(ResultadoGestionExterna.PAGO_REGISTRADO);
        gestion.setFechaCierreGestionExterna(faltasClock.now());
        gestionExternaRepository.guardar(gestion);

        // Asignar resultado final (siempre, independiente de bloqueantes)
        acta.setResultadoFinal(ResultadoFinalActa.CONDENA_FIRME_PAGADA);

        String descEvento = "Pago externo registrado en gestion externa."
                + " Tipo: " + gestion.getTipoGestionExterna()
                + nvl(cmd.observaciones() != null ? ". " + cmd.observaciones() : null);

        registrarEvento(acta.getId(), TipoEventoActa.PAGAPR, null, null, null, descEvento);

        boolean hayBloqueantes = bloqueantesChecker.tieneBloqueantesActivos(acta.getId());

        if (!hayBloqueantes) {
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.CERRADA);
            acta.setBloqueActual(BloqueActual.CERR);
            actaRepository.guardar(acta);
            registrarEvento(acta.getId(), TipoEventoActa.CIERRA, null, null, null,
                    "Acta cerrada por pago externo confirmado en gestion externa.");
        } else {
            // Pago registrado pero bloqueantes impiden cierre: acta vuelve a ANAL/ACTIVA.
            // Estado transitorio hasta Slice 7 (motor real de bloqueantes).
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.ACTIVA);
            acta.setBloqueActual(BloqueActual.ANAL);
            actaRepository.guardar(acta);
        }

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        String estado = hayBloqueantes ? "CONDENA_FIRME_PAGADA (bloqueantes activos - ACTIVA/ANAL)" : "CERRADA";
        return ComandoResultado.de(acta.getId(), String.valueOf(gestion.getId()),
                TipoEventoActa.PAGAPR.codigo(),
                "Pago externo registrado en gestion externa. Estado acta: " + estado);
    }
    // -------------------------------------------------------------------------
    // Consulta
    // -------------------------------------------------------------------------

    public FalGestionExterna obtenerGestionActiva(Long actaId) {
        actaRepository.buscarPorId(actaId)
                .orElseThrow(() -> new ActaNoEncontradaException(actaId));
        return gestionExternaRepository.buscarActiva(actaId)
                .orElseThrow(() -> new GestionExternaNoEncontradaException(actaId));
    }

    // -------------------------------------------------------------------------
    // Validaciones - derivar
    // -------------------------------------------------------------------------

    private void validarSituacionPermiteDerivacion(FalActa acta) {
        SituacionAdministrativaActa sit = acta.getSituacionAdministrativa();
        if (sit == SituacionAdministrativaActa.CERRADA) {
            throw new PrecondicionVioladaException("El acta esta cerrada. No se puede derivar.");
        }
        if (sit == SituacionAdministrativaActa.ANULADA) {
            throw new PrecondicionVioladaException("El acta esta anulada. No se puede derivar.");
        }
        if (sit == SituacionAdministrativaActa.ARCHIVADA) {
            throw new PrecondicionVioladaException("El acta esta archivada. No se puede derivar.");
        }
        if (sit == SituacionAdministrativaActa.PARALIZADA) {
            throw new PrecondicionVioladaException("El acta esta paralizada. No se puede derivar.");
        }
        if (sit == SituacionAdministrativaActa.EN_GESTION_EXTERNA
                || acta.getBloqueActual() == BloqueActual.GEXT) {
            throw new PrecondicionVioladaException(
                    "El acta ya se encuentra en gestion externa (GEXT).");
        }
    }

    private void validarNoExisteGestionActiva(Long actaId) {
        if (gestionExternaRepository.existeActiva(actaId)) {
            throw new PrecondicionVioladaException(
                    "Ya existe una gestion externa activa para el acta: " + actaId);
        }
    }

    private void validarTipoGestion(DerivarGestionExternaCommand cmd) {
        if (cmd.tipoGestionExterna() == null) {
            throw new PrecondicionVioladaException("El tipo de gestion externa es obligatorio.");
        }
    }

    private void validarMotivoDerivacion(DerivarGestionExternaCommand cmd) {
        if (cmd.motivoDerivacion() == null || cmd.motivoDerivacion().isBlank()) {
            throw new PrecondicionVioladaException("El motivo de derivacion es obligatorio.");
        }
    }

    private void validarResultadoHabilitaDerivacion(FalActa acta) {
        ResultadoFinalActa resultado = acta.getResultadoFinal();
        if (resultado == ResultadoFinalActa.ABSUELTO) {
            throw new PrecondicionVioladaException(
                    "El acta tiene resultado ABSUELTO. No aplica derivacion a gestion externa.");
        }
        if (resultado == ResultadoFinalActa.PAGO_VOLUNTARIO_PAGADO) {
            throw new PrecondicionVioladaException(
                    "El acta tiene resultado PAGO_VOLUNTARIO_PAGADO. No aplica derivacion a gestion externa.");
        }
        if (resultado == ResultadoFinalActa.CONDENA_FIRME_PAGADA) {
            throw new PrecondicionVioladaException(
                    "El acta tiene resultado CONDENA_FIRME_PAGADA. La condena ya fue pagada, no se puede derivar.");
        }
        if (resultado != ResultadoFinalActa.CONDENA_FIRME) {
            throw new PrecondicionVioladaException(
                    "La derivacion a gestion externa requiere resultadoFinal = CONDENA_FIRME. "
                            + "Resultado actual: " + resultado);
        }
    }

    private void validarPagoCondenaNoConfirmado(Long actaId) {
        Optional<FalPagoCondena> pagoOpt = pagoCondenaRepository.buscarPorActa(actaId);
        if (pagoOpt.isPresent() && pagoOpt.get().estaConfirmado()) {
            throw new PrecondicionVioladaException(
                    "El pago de condena ya fue confirmado. No se puede derivar a gestion externa.");
        }
    }

    // -------------------------------------------------------------------------
    // Validaciones - reingresar
    // -------------------------------------------------------------------------

    private void validarModoReingresoNoNulo(ReingresarDesdeGestionExternaCommand cmd) {
        if (cmd.modoReingresoGestionExterna() == null) {
            throw new PrecondicionVioladaException("El modo de reingreso es obligatorio.");
        }
    }

    private void validarMotivoReingreso(ReingresarDesdeGestionExternaCommand cmd) {
        if (cmd.motivoReingreso() == null || cmd.motivoReingreso().isBlank()) {
            throw new PrecondicionVioladaException(
                    "El motivo de reingreso es obligatorio y no puede estar vacio.");
        }
    }

    private void validarActaEnGestionExternaParaReingreso(FalActa acta) {
        SituacionAdministrativaActa sit = acta.getSituacionAdministrativa();
        if (sit == SituacionAdministrativaActa.CERRADA) {
            throw new PrecondicionVioladaException("El acta esta cerrada. No se puede reingresar.");
        }
        if (sit == SituacionAdministrativaActa.ANULADA) {
            throw new PrecondicionVioladaException("El acta esta anulada. No se puede reingresar.");
        }
        if (sit == SituacionAdministrativaActa.ARCHIVADA) {
            throw new PrecondicionVioladaException("El acta esta archivada. No se puede reingresar.");
        }
        if (sit != SituacionAdministrativaActa.EN_GESTION_EXTERNA) {
            throw new PrecondicionVioladaException(
                    "El acta no esta en gestion externa (situacion actual: " + sit
                            + "). Solo se puede reingresar desde EN_GESTION_EXTERNA.");
        }
    }

    private void validarBloqueActualEsGext(FalActa acta) {
        if (acta.getBloqueActual() != BloqueActual.GEXT) {
            throw new PrecondicionVioladaException(
                    "El acta no esta en bloque GEXT (bloque actual: " + acta.getBloqueActual()
                            + "). Solo se puede reingresar desde GEXT.");
        }
    }

    /**
     * Valida que el modo de reingreso no sea uno permanentemente reservado.
     *
     * Desde Slice 6D-2 estan habilitados:
     *   REINGRESO_PARA_REVISION, REINGRESO_SIN_PAGO (Slice 6B/6D-1),
     *   REINGRESO_PARA_NUEVO_FALLO (requiere ABSUELVE, Slice 6D-2),
     *   REINGRESO_CON_DICTAMEN (requiere CONFIRMA_CONDENA o MODIFICA_MONTO, Slice 6D-2).
     *
     * Siguen reservados: REINGRESO_PARA_CIERRE, REINGRESO_CON_PAGO.
     */
    private void validarModoHabilitadoEnSlice6B(ReingresarDesdeGestionExternaCommand cmd) {
        ModoReingresoGestionExterna modo = cmd.modoReingresoGestionExterna();
        if (modo == ModoReingresoGestionExterna.REINGRESO_PARA_CIERRE) {
            throw new PrecondicionVioladaException(
                    "El modo REINGRESO_PARA_CIERRE no esta habilitado. "
                            + "Queda reservado para un slice posterior.");
        }
        if (modo == ModoReingresoGestionExterna.REINGRESO_CON_PAGO) {
            throw new PrecondicionVioladaException(
                    "El modo REINGRESO_CON_PAGO no esta habilitado. "
                            + "Queda reservado para un slice posterior.");
        }
    }

    /**
     * Valida coherencia del par resultado/modo cuando resultadoGestionExterna es no nulo.
     *
     * Pares habilitados en Slice 6D-1:
     *   - SIN_PAGO + REINGRESO_SIN_PAGO
     *   - SIN_CAMBIOS + REINGRESO_PARA_REVISION
     *
     * Pares habilitados en Slice 6D-2:
     *   - ABSUELVE + REINGRESO_PARA_NUEVO_FALLO
     *   - CONFIRMA_CONDENA + REINGRESO_CON_DICTAMEN
     *   - MODIFICA_MONTO + REINGRESO_CON_DICTAMEN
     *
     * PAGO_REGISTRADO es asignado automaticamente por PAGAPR; no se puede informar via reingreso.
     * SIN_RESULTADO es el estado inicial; no se puede informar al reingresar.
     *
     * Si resultadoGestionExterna es null, no se valida el par (modos sin resultado explicito usan la ruta clasica).
     * REINGRESO_PARA_NUEVO_FALLO y REINGRESO_CON_DICTAMEN requieren resultado explicito
     * (validado por validarModoRequiereResultadoExplicito antes de este metodo).
     */
    private void validarParResultadoModo(ReingresarDesdeGestionExternaCommand cmd) {
        ResultadoGestionExterna resultado = cmd.resultadoGestionExterna();
        if (resultado == null) {
            return;
        }
        ModoReingresoGestionExterna modo = cmd.modoReingresoGestionExterna();

        if (resultado == ResultadoGestionExterna.PAGO_REGISTRADO) {
            throw new PrecondicionVioladaException(
                    "PAGO_REGISTRADO es asignado automaticamente por el comando PAGAPR. "
                            + "No puede informarse mediante reingreso manual.");
        }
        if (resultado == ResultadoGestionExterna.SIN_RESULTADO) {
            throw new PrecondicionVioladaException(
                    "SIN_RESULTADO es el estado inicial al derivar. No puede informarse al reingresar.");
        }
        if (resultado == ResultadoGestionExterna.SIN_PAGO
                && modo != ModoReingresoGestionExterna.REINGRESO_SIN_PAGO) {
            throw new PrecondicionVioladaException(
                    "El resultado SIN_PAGO requiere modo REINGRESO_SIN_PAGO. Modo informado: " + modo);
        }
        if (resultado == ResultadoGestionExterna.SIN_CAMBIOS
                && modo != ModoReingresoGestionExterna.REINGRESO_PARA_REVISION) {
            throw new PrecondicionVioladaException(
                    "El resultado SIN_CAMBIOS requiere modo REINGRESO_PARA_REVISION. Modo informado: " + modo);
        }
        if (resultado == ResultadoGestionExterna.ABSUELVE
                && modo != ModoReingresoGestionExterna.REINGRESO_PARA_NUEVO_FALLO) {
            throw new PrecondicionVioladaException(
                    "El resultado ABSUELVE requiere modo REINGRESO_PARA_NUEVO_FALLO. Modo informado: " + modo);
        }
        if (resultado == ResultadoGestionExterna.CONFIRMA_CONDENA
                && modo != ModoReingresoGestionExterna.REINGRESO_CON_DICTAMEN) {
            throw new PrecondicionVioladaException(
                    "El resultado CONFIRMA_CONDENA requiere modo REINGRESO_CON_DICTAMEN. Modo informado: " + modo);
        }
        if (resultado == ResultadoGestionExterna.MODIFICA_MONTO
                && modo != ModoReingresoGestionExterna.REINGRESO_CON_DICTAMEN) {
            throw new PrecondicionVioladaException(
                    "El resultado MODIFICA_MONTO requiere modo REINGRESO_CON_DICTAMEN. Modo informado: " + modo);
        }
    }


    /**
     * Valida que los modos REINGRESO_PARA_NUEVO_FALLO y REINGRESO_CON_DICTAMEN
     * siempre vengan acompanados de un resultado explicito no nulo.
     * Estos modos son el resultado de un dictamen externo, por lo que el resultado es obligatorio.
     */
    private void validarModoRequiereResultadoExplicito(ReingresarDesdeGestionExternaCommand cmd) {
        ModoReingresoGestionExterna modo = cmd.modoReingresoGestionExterna();
        if ((modo == ModoReingresoGestionExterna.REINGRESO_PARA_NUEVO_FALLO
                || modo == ModoReingresoGestionExterna.REINGRESO_CON_DICTAMEN)
                && cmd.resultadoGestionExterna() == null) {
            throw new PrecondicionVioladaException(
                    "El modo " + modo + " requiere resultado explicito no nulo. "
                            + "REINGRESO_PARA_NUEVO_FALLO requiere ABSUELVE. "
                            + "REINGRESO_CON_DICTAMEN requiere CONFIRMA_CONDENA o MODIFICA_MONTO.");
        }
    }

    /**
     * Valida que el acta tenga resultadoFinal = CONDENA_FIRME cuando se usa
     * un modo de dictamen externo (REINGRESO_PARA_NUEVO_FALLO o REINGRESO_CON_DICTAMEN).
     * El dictamen externo presupone que existia una condena firme que fue cuestionada externamente.
     */
    private void validarCondenaFirmeParaModosDictamen(ReingresarDesdeGestionExternaCommand cmd, FalActa acta) {
        ModoReingresoGestionExterna modo = cmd.modoReingresoGestionExterna();
        if (modo == ModoReingresoGestionExterna.REINGRESO_PARA_NUEVO_FALLO
                || modo == ModoReingresoGestionExterna.REINGRESO_CON_DICTAMEN) {
            if (acta.getResultadoFinal() != ResultadoFinalActa.CONDENA_FIRME) {
                throw new PrecondicionVioladaException(
                        "El modo " + modo + " requiere resultadoFinal = CONDENA_FIRME. "
                                + "Resultado actual: " + acta.getResultadoFinal());
            }
        }
    }

    /**
     * Valida que cuando el resultado sea MODIFICA_MONTO el montoResultado sea no nulo y mayor a cero.
     * El campo montoResultado corresponde a monto_resultado en fal_acta_gestion_externa (MariaDB).
     */
    private void validarMontoParaModificaMonto(ReingresarDesdeGestionExternaCommand cmd) {
        if (cmd.resultadoGestionExterna() != ResultadoGestionExterna.MODIFICA_MONTO) {
            return;
        }
        if (cmd.montoResultado() == null) {
            throw new PrecondicionVioladaException(
                    "MODIFICA_MONTO requiere montoResultado no nulo. "
                            + "El monto externo informado por la gestion externa es obligatorio.");
        }
        if (cmd.montoResultado().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PrecondicionVioladaException(
                    "MODIFICA_MONTO requiere montoResultado mayor a cero. "
                            + "Monto informado: " + cmd.montoResultado());
        }
    }

        private void validarGestionActivaEstadoPermiteReingreso(FalGestionExterna gestion) {
        if (!gestion.estaActivaParaReingreso()) {
            throw new PrecondicionVioladaException(
                    "La gestion externa no esta en estado valido para reingreso. "
                            + "Estado actual: " + gestion.getEstadoGestionExterna()
                            + ". Se requiere DERIVADA o EN_CURSO.");
        }
    }

    private void validarResultadoFinalCondenaFirmeParaReingreso(FalActa acta) {
        if (acta.getResultadoFinal() != ResultadoFinalActa.CONDENA_FIRME) {
            throw new PrecondicionVioladaException(
                    "El modo REINGRESO_SIN_PAGO requiere resultadoFinal = CONDENA_FIRME. "
                            + "Resultado actual: " + acta.getResultadoFinal());
        }
    }


    // -------------------------------------------------------------------------
    // Validaciones - registrar pago externo (Slice 6C)
    // -------------------------------------------------------------------------

    private void validarActaPermitePagoExterno(FalActa acta) {
        SituacionAdministrativaActa sit = acta.getSituacionAdministrativa();
        if (sit == SituacionAdministrativaActa.CERRADA) {
            throw new PrecondicionVioladaException("El acta esta cerrada. No se puede registrar pago externo.");
        }
        if (sit == SituacionAdministrativaActa.ANULADA) {
            throw new PrecondicionVioladaException("El acta esta anulada. No se puede registrar pago externo.");
        }
        if (sit == SituacionAdministrativaActa.ARCHIVADA) {
            throw new PrecondicionVioladaException("El acta esta archivada. No se puede registrar pago externo.");
        }
        if (sit != SituacionAdministrativaActa.EN_GESTION_EXTERNA) {
            throw new PrecondicionVioladaException(
                    "El acta no esta en gestion externa (situacion actual: " + sit
                            + "). Solo se puede registrar pago externo desde EN_GESTION_EXTERNA.");
        }
    }

    private void validarBloqueActualEsGextParaPago(FalActa acta) {
        if (acta.getBloqueActual() != BloqueActual.GEXT) {
            throw new PrecondicionVioladaException(
                    "El acta no esta en bloque GEXT (bloque actual: " + acta.getBloqueActual()
                            + "). Solo se puede registrar pago externo desde GEXT.");
        }
    }

    private void validarResultadoFinalEsCondenaFirme(FalActa acta) {
        if (acta.getResultadoFinal() != ResultadoFinalActa.CONDENA_FIRME) {
            throw new PrecondicionVioladaException(
                    "El registro de pago externo requiere resultadoFinal = CONDENA_FIRME. "
                            + "Resultado actual: " + acta.getResultadoFinal());
        }
    }

    private void validarGestionActivaParaPagoExterno(FalGestionExterna gestion) {
        if (!gestion.estaActivaParaReingreso()) {
            throw new PrecondicionVioladaException(
                    "La gestion externa no esta en estado valido para registrar pago externo. "
                            + "Estado actual: " + gestion.getEstadoGestionExterna()
                            + ". Se requiere DERIVADA o EN_CURSO.");
        }
    }

    private void validarPagoCondenaNoConfirmadoParaPagoExterno(Long actaId) {
        Optional<FalPagoCondena> pagoOpt = pagoCondenaRepository.buscarPorActa(actaId);
        if (pagoOpt.isPresent() && pagoOpt.get().estaConfirmado()) {
            throw new PrecondicionVioladaException(
                    "El pago de condena interno ya fue confirmado. No se puede registrar pago externo.");
        }
    }
    // -------------------------------------------------------------------------
    // Internos
    // -------------------------------------------------------------------------

    private void registrarEvento(Long idActa, TipoEventoActa tipo,
                                  Long idDocuRel, Long idNotifRel,
                                  String idUserEvt, String descripcionLegible) {
        FalActaEvento evento = FalActaEvento.builder()
                .actaId(idActa)
                .tipoEvt(tipo)
                .origenEvt(idUserEvt != null ? OrigenEvento.USUARIO_WEB : OrigenEvento.PROCESO_AUTOMATICO)
                .fhEvt(faltasClock.now())
                .idDocuRel(idDocuRel)
                .idNotifRel(idNotifRel)
                .idUserEvt(idUserEvt)
                .actorTipo(idUserEvt != null ? ActorTipoEvento.USUARIO_INTERNO : ActorTipoEvento.SISTEMA)
                .descripcionLegible(descripcionLegible)
                .build();
        eventoRepository.registrar(evento);
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }
}
