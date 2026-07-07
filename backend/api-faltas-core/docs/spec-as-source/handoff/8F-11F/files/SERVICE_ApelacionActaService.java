package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.PasarApelacionAAnalisisCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarApelacionCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarDocumentoApelacionCommand;
import ar.gob.malvinas.faltas.core.application.command.ResolverApelacionAceptaAbsuelveCommand;
import ar.gob.malvinas.faltas.core.application.command.ResolverApelacionModificaCondenaCommand;
import ar.gob.malvinas.faltas.core.application.command.ResolverApelacionNulidadCommand;
import ar.gob.malvinas.faltas.core.application.command.ResolverApelacionRechazadaCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.CanalApelacion;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoApelacionActa;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoFalloActa;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoProcesalActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoResolucionApelacion;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPresentacion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFalloActa;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaApelacion;
import ar.gob.malvinas.faltas.core.domain.model.FalActaApelacionDocumento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.ApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.ApelacionDocumentoRepository;
import ar.gob.malvinas.faltas.core.application.port.BloqueantesMaterialesChecker;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ApelacionActaService {

    private final ActaRepository actaRepository;
    private final FalloActaRepository falloActaRepository;
    private final ApelacionActaRepository apelacionActaRepository;
    private final ApelacionDocumentoRepository apelacionDocumentoRepository;
    private final ActaEventoRepository eventoRepository;
    private final ActaSnapshotRepository snapshotRepository;
    private final SnapshotRecalculador snapshotRecalculador;
    private final BloqueantesMaterialesChecker bloqueantesChecker;

    @Autowired
    public ApelacionActaService(
            ActaRepository actaRepository,
            FalloActaRepository falloActaRepository,
            ApelacionActaRepository apelacionActaRepository,
            ApelacionDocumentoRepository apelacionDocumentoRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            SnapshotRecalculador snapshotRecalculador,
            BloqueantesMaterialesChecker bloqueantesChecker) {
        this.actaRepository = actaRepository;
        this.falloActaRepository = falloActaRepository;
        this.apelacionActaRepository = apelacionActaRepository;
        this.apelacionDocumentoRepository = apelacionDocumentoRepository;
        this.eventoRepository = eventoRepository;
        this.snapshotRepository = snapshotRepository;
        this.snapshotRecalculador = snapshotRecalculador;
        this.bloqueantesChecker = bloqueantesChecker;
    }

    /** Constructor de compatibilidad backward (sin ApelacionDocumentoRepository). */
    public ApelacionActaService(
            ActaRepository actaRepository,
            FalloActaRepository falloActaRepository,
            ApelacionActaRepository apelacionActaRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            SnapshotRecalculador snapshotRecalculador,
            BloqueantesMaterialesChecker bloqueantesChecker) {
        this(actaRepository, falloActaRepository, apelacionActaRepository,
                null, eventoRepository, snapshotRepository, snapshotRecalculador, bloqueantesChecker);
    }

    // -----------------------------------------------------------------------
    // Registrar apelacion
    // -----------------------------------------------------------------------

    public ComandoResultado registrarApelacion(RegistrarApelacionCommand cmd) {
        FalActa acta = cargarActaAbierta(cmd.actaId());
        FalActaFallo fallo = obtenerFalloCondenatorioNotificado(cmd.actaId());

        if (apelacionActaRepository.buscarActiva(cmd.actaId()).isPresent())
            throw new PrecondicionVioladaException("Ya existe una apelacion activa para este acta.");

        validarContenidoApelacion(cmd.tipoPresentacion(), cmd.textoApelacion());

        LocalDateTime ahora = LocalDateTime.now();
        Long idApelacion = apelacionActaRepository.nextId();
        FalActaApelacion apelacion = new FalActaApelacion(
                idApelacion, cmd.actaId(), fallo.getId(),
                cmd.canalApelacion() != null ? cmd.canalApelacion() : CanalApelacion.PRESENCIAL,
                cmd.tipoPresentacion() != null ? cmd.tipoPresentacion() : TipoPresentacion.TEXTO,
                cmd.textoApelacion(),
                ahora, cmd.idUserRegistro(), ahora, nvlStr(cmd.idUserRegistro()));
        apelacionActaRepository.guardar(apelacion);

        registrarEvento(cmd.actaId(), TipoEventoActa.APEPRE, null, null, null,
                "Apelacion presentada. Canal: " + apelacion.getCanalApelacion()
                + ". Tipo: " + apelacion.getTipoPresentacion());

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(cmd.actaId(), String.valueOf(idApelacion),
                TipoEventoActa.APEPRE.codigo(), "Apelacion registrada.");
    }

    // -----------------------------------------------------------------------
    // Pasar a analisis
    // -----------------------------------------------------------------------

    public ComandoResultado pasarAAnalisis(PasarApelacionAAnalisisCommand cmd) {
        FalActaApelacion apelacion = apelacionActaRepository.findById(cmd.apelacionId())
                .orElseThrow(() -> new PrecondicionVioladaException("Apelacion no encontrada: " + cmd.apelacionId()));
        FalActa acta = cargarActaAbierta(apelacion.getActaId());

        if (apelacion.getEstadoApelacion() != EstadoApelacionActa.PRESENTADA)
            throw new PrecondicionVioladaException("Solo apelacion PRESENTADA puede pasar a EN_ANALISIS. Estado: " + apelacion.getEstadoApelacion());

        apelacion.setEstadoApelacion(EstadoApelacionActa.EN_ANALISIS);
        apelacion.setFhUltMod(LocalDateTime.now());
        apelacion.setIdUserUltMod(cmd.idUserOperacion());
        apelacionActaRepository.guardar(apelacion);

        registrarEvento(apelacion.getActaId(), TipoEventoActa.APEANL, null, null, null,
                "Apelacion pasada a EN_ANALISIS.");

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(apelacion.getActaId(), String.valueOf(apelacion.getId()),
                TipoEventoActa.APEANL.codigo(), "Apelacion en analisis.");
    }

    // -----------------------------------------------------------------------
    // Registrar documento de apelacion
    // -----------------------------------------------------------------------

    public ComandoResultado registrarDocumento(RegistrarDocumentoApelacionCommand cmd) {
        if (apelacionDocumentoRepository == null)
            throw new UnsupportedOperationException("ApelacionDocumentoRepository no disponible.");
        FalActaApelacion apelacion = apelacionActaRepository.findById(cmd.apelacionId())
                .orElseThrow(() -> new PrecondicionVioladaException("Apelacion no encontrada: " + cmd.apelacionId()));

        if (apelacion.getEstadoApelacion() != EstadoApelacionActa.PRESENTADA
                && apelacion.getEstadoApelacion() != EstadoApelacionActa.EN_ANALISIS)
            throw new PrecondicionVioladaException("Solo se pueden agregar documentos a apelacion PRESENTADA o EN_ANALISIS.");

        LocalDateTime ahora = LocalDateTime.now();
        Long idDoc = apelacionDocumentoRepository.nextId();
        FalActaApelacionDocumento doc = new FalActaApelacionDocumento(
                idDoc, cmd.apelacionId(), cmd.tipoDocApelacion(), cmd.origenPresentacion(),
                cmd.documentoId(), cmd.storageKey(), cmd.nombreArchivo(), cmd.mimeType(),
                cmd.tamanioBytes(), ahora, ahora, nvlStr(cmd.idUserAlta()));
        apelacionDocumentoRepository.guardar(doc);

        return ComandoResultado.de(apelacion.getActaId(), String.valueOf(idDoc),
                "DOCAMP", "Documento de apelacion registrado.");
    }

    // -----------------------------------------------------------------------
    // Resoluciones
    // -----------------------------------------------------------------------

    public ComandoResultado resolverRechazada(ResolverApelacionRechazadaCommand cmd) {
        FalActa acta = cargarActaAbierta(cmd.actaId());
        obtenerFalloCondenatorioNotificado(cmd.actaId());
        FalActaApelacion apelacion = obtenerApelacionParaResolucion(cmd.actaId());

        apelacion.resolver(ResultadoResolucionApelacion.RECHAZADA,
                LocalDateTime.now(), "SYS", null);
        apelacionActaRepository.guardar(apelacion);

        registrarEvento(cmd.actaId(), TipoEventoActa.APERAZ, null, null, null,
                "Apelacion rechazada. " + nvl(cmd.fundamentosResolucion()));

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(cmd.actaId(), String.valueOf(apelacion.getId()),
                TipoEventoActa.APERAZ.codigo(), "Apelacion rechazada. Pendiente declaracion de firmeza.");
    }

    public ComandoResultado resolverAceptaAbsuelve(ResolverApelacionAceptaAbsuelveCommand cmd) {
        FalActa acta = cargarActaAbierta(cmd.actaId());
        obtenerFalloCondenatorioNotificado(cmd.actaId());
        FalActaApelacion apelacion = obtenerApelacionParaResolucion(cmd.actaId());

        apelacion.resolver(ResultadoResolucionApelacion.ACEPTADA_ABSUELVE,
                LocalDateTime.now(), "SYS", null);
        apelacionActaRepository.guardar(apelacion);

        acta.setResultadoFinal(ResultadoFinalActa.ABSUELTO);

        registrarEvento(cmd.actaId(), TipoEventoActa.APEABS, null, null, null,
                "Apelacion aceptada - absolucion en segunda instancia. " + nvl(cmd.fundamentosResolucion()));

        boolean cerrar = !bloqueantesChecker.tieneBloqueantesActivos(cmd.actaId());
        if (cerrar) {
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.CERRADA);
            acta.setBloqueActual(BloqueActual.CERR);
            acta.setEstadoProcesal(EstadoProcesalActa.CONCLUIDO);
            actaRepository.guardar(acta);
            registrarEvento(cmd.actaId(), TipoEventoActa.CIERRA, null, null, null,
                    "Acta cerrada por absolucion en apelacion.");
        } else {
            actaRepository.guardar(acta);
        }

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        String msg = cerrar ? "Apelacion aceptada. Infractor absuelto. Acta cerrada."
                : "Apelacion aceptada. Infractor absuelto. Cierre pendiente por bloqueantes.";
        return ComandoResultado.de(cmd.actaId(), String.valueOf(apelacion.getId()),
                TipoEventoActa.APEABS.codigo(), msg);
    }

    public ComandoResultado resolverModificaCondena(ResolverApelacionModificaCondenaCommand cmd) {
        FalActaApelacion apelacion = apelacionActaRepository.findById(cmd.apelacionId())
                .orElseThrow(() -> new PrecondicionVioladaException("Apelacion no encontrada: " + cmd.apelacionId()));
        FalActa acta = cargarActaAbierta(apelacion.getActaId());

        FalActaFallo falloViejo = obtenerFalloCondenatorioNotificado(apelacion.getActaId());
        if (cmd.nuevoMontoCondena() == null || cmd.nuevoMontoCondena().compareTo(BigDecimal.ZERO) < 0)
            throw new PrecondicionVioladaException("nuevoMontoCondena requerido y >= 0.");

        apelacion.resolver(ResultadoResolucionApelacion.MODIFICA_CONDENA,
                LocalDateTime.now(), cmd.idUserResolucion(), null);
        apelacionActaRepository.guardar(apelacion);

        // Crear fallo sustitutivo
        Long idNuevoFallo = falloActaRepository.nextId();
        FalActaFallo falloNuevo = new FalActaFallo(idNuevoFallo, apelacion.getActaId(),
                TipoFalloActa.CONDENATORIO, LocalDateTime.now(), LocalDateTime.now(),
                nvlStr(cmd.idUserResolucion()));
        falloNuevo.setMontoCondena(cmd.nuevoMontoCondena());
        falloNuevo.setFundamentos(cmd.fundamentosResolucion());
        falloNuevo.setFalloReemplazadoId(falloViejo.getId());
        falloNuevo.setEstadoFallo(EstadoFalloActa.NOTIFICADO);
        falloNuevo.setDocumentoId(falloViejo.getDocumentoId());
        falloActaRepository.guardarComoVigente(falloNuevo);

        registrarEvento(apelacion.getActaId(), TipoEventoActa.APEMCO, null, null, null,
                "Apelacion aceptada - condena modificada. Nuevo monto: " + cmd.nuevoMontoCondena());
        registrarEvento(apelacion.getActaId(), TipoEventoActa.FALRMP, null, null, null,
                "Fallo " + falloViejo.getId() + " reemplazado por " + idNuevoFallo);

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(apelacion.getActaId(), String.valueOf(idNuevoFallo),
                TipoEventoActa.APEMCO.codigo(),
                "Condena modificada. Nuevo monto: " + cmd.nuevoMontoCondena());
    }

    public ComandoResultado resolverNulidad(ResolverApelacionNulidadCommand cmd) {
        FalActaApelacion apelacion = apelacionActaRepository.findById(cmd.apelacionId())
                .orElseThrow(() -> new PrecondicionVioladaException("Apelacion no encontrada: " + cmd.apelacionId()));
        FalActa acta = cargarActaAbierta(apelacion.getActaId());

        FalActaFallo falloViejo = falloActaRepository.findVigenteByActaId(apelacion.getActaId())
                .orElseThrow(() -> new PrecondicionVioladaException("No existe fallo vigente."));

        apelacion.resolver(ResultadoResolucionApelacion.NULIDAD,
                LocalDateTime.now(), cmd.idUserResolucion(), null);
        apelacionActaRepository.guardar(apelacion);

        // Desactivar fallo anterior
        falloViejo.reemplazadoPor(null);
        falloActaRepository.guardar(falloViejo);

        acta.setResultadoFinal(ResultadoFinalActa.NULIDAD);
        actaRepository.guardar(acta);

        registrarEvento(apelacion.getActaId(), TipoEventoActa.APENUL, null, null, null,
                "Apelacion resuelta con NULIDAD. " + nvl(cmd.fundamentosResolucion()));

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(apelacion.getActaId(), String.valueOf(apelacion.getId()),
                TipoEventoActa.APENUL.codigo(), "Nulidad declarada.");
    }

    // -----------------------------------------------------------------------
    // Consultas
    // -----------------------------------------------------------------------

    public Optional<FalActaApelacion> obtenerApelacionActiva(Long actaId) {
        actaRepository.buscarPorId(actaId).orElseThrow(() -> new ActaNoEncontradaException(actaId));
        return apelacionActaRepository.buscarActiva(actaId);
    }

    public List<FalActaApelacionDocumento> listarDocumentosApelacion(Long apelacionId) {
        if (apelacionDocumentoRepository == null) return List.of();
        return apelacionDocumentoRepository.findByApelacionId(apelacionId);
    }

    // -----------------------------------------------------------------------
    // Helpers privados
    // -----------------------------------------------------------------------

    private FalActa cargarActaAbierta(Long actaId) {
        FalActa acta = actaRepository.buscarPorId(actaId)
                .orElseThrow(() -> new ActaNoEncontradaException(actaId));
        SituacionAdministrativaActa sit = acta.getSituacionAdministrativa();
        if (sit == SituacionAdministrativaActa.CERRADA) throw new PrecondicionVioladaException("El acta esta cerrada.");
        if (sit == SituacionAdministrativaActa.ANULADA) throw new PrecondicionVioladaException("El acta esta anulada.");
        if (sit == SituacionAdministrativaActa.ARCHIVADA) throw new PrecondicionVioladaException("El acta esta archivada.");
        if (sit == SituacionAdministrativaActa.PARALIZADA) throw new PrecondicionVioladaException("El acta esta paralizada.");
        return acta;
    }

    private FalActaFallo obtenerFalloCondenatorioNotificado(Long actaId) {
        FalActaFallo fallo = falloActaRepository.findVigenteByActaId(actaId)
                .orElseThrow(() -> new PrecondicionVioladaException("No existe fallo vigente. No se puede operar."));
        if (fallo.getTipoFallo() != TipoFalloActa.CONDENATORIO)
            throw new PrecondicionVioladaException("Solo fallo condenatorio admite apelacion. Tipo: " + fallo.getTipoFallo());
        if (fallo.getEstadoFallo() != EstadoFalloActa.NOTIFICADO)
            throw new PrecondicionVioladaException("El fallo debe estar NOTIFICADO. Estado: " + fallo.getEstadoFallo());
        return fallo;
    }

    private FalActaApelacion obtenerApelacionParaResolucion(Long actaId) {
        FalActaApelacion apelacion = apelacionActaRepository.buscarActiva(actaId)
                .orElseThrow(() -> new PrecondicionVioladaException("No existe apelacion activa para este acta."));
        if (apelacion.getEstadoApelacion() != EstadoApelacionActa.PRESENTADA
                && apelacion.getEstadoApelacion() != EstadoApelacionActa.EN_ANALISIS)
            throw new PrecondicionVioladaException("La apelacion no esta en estado resoluble. Estado: " + apelacion.getEstadoApelacion());
        return apelacion;
    }

    private void validarContenidoApelacion(TipoPresentacion tipo, String texto) {
        if (tipo == null) return;
        switch (tipo) {
            case TEXTO, MIXTA -> {
                if (texto == null || texto.isBlank())
                    throw new PrecondicionVioladaException("tipoPresentacion " + tipo + " requiere textoApelacion no vacio.");
            }
            case DOCUMENTOS -> { /* documentos se agregan con RegistrarDocumentoApelacionCommand */ }
        }
    }

    private void registrarEvento(Long idActa, TipoEventoActa tipo, String idDoc,
                                  String idNotif, String idOp, String descripcion) {
        int orden = eventoRepository.proximoOrdenLogico(idActa);
        FalActaEvento ev = new FalActaEvento(UUID.randomUUID().toString(), idActa, tipo,
                LocalDateTime.now(), orden, idDoc, idNotif, idOp, descripcion, null);
        eventoRepository.registrar(ev);
    }

    private static String nvl(String s) { return s != null ? s : ""; }
    private static String nvlStr(String s) { return (s != null && !s.isBlank()) ? s : "SYS"; }
}