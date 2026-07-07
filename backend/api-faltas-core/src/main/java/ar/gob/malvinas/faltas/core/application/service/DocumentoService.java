package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.EmitirDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.EmitirNumeroDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.FirmarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.GenerarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.GenerarDocumentoDesdePlantillaCommand;
import ar.gob.malvinas.faltas.core.application.command.NumerarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.EnviarAFirmaCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarFirmaDocumentalCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.result.NumeroDocumentoEmitidoResponse;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoFalloActa;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoFirma;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoFirmaReq;
import ar.gob.malvinas.faltas.core.domain.enums.MomentoNumeracionDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFirma;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFirmaReq;
import ar.gob.malvinas.faltas.core.domain.enums.ActorTipoEvento;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoPlantillaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.FirmanteNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.model.FalDependencia;
import ar.gob.malvinas.faltas.core.domain.model.FalDependenciaVersion;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoFirma;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoFirmaReq;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantillaFirmaReq;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantilla;
import ar.gob.malvinas.faltas.core.domain.model.FalFirmante;
import ar.gob.malvinas.faltas.core.domain.model.FalFirmanteVersion;
import ar.gob.malvinas.faltas.core.domain.model.FalFirmanteVersionHabilitacion;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.DependenciaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoFirmaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoFirmaReqRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.FirmanteRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.springframework.stereotype.Service;

import ar.gob.malvinas.faltas.core.application.command.ConvalidarFirmaEscaneadaCommand;
import ar.gob.malvinas.faltas.core.application.command.IncorporarDocumentoEscaneadoCommand;
import ar.gob.malvinas.faltas.core.application.result.ConvalidacionEscaneadaResultado;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Motor de proceso del circuito documental del expediente.
 *
 * Slice 8C-5A: numeracion documental reusable.
 * Slice 8C-6B-1: firma documental real con validacion de firmante/habilitacion/orden.
 * Slice 8C-6D-1: incorporacion de documento escaneado/adjunto y convalidacion de firma olografa.
 */
@Service
public class DocumentoService {

    private final ActaRepository actaRepository;
    private final DocumentoRepository documentoRepository;
    private final DocumentoFirmaRepository firmaRepository;
    private final ActaEventoRepository eventoRepository;
    private final ActaSnapshotRepository snapshotRepository;
    private final SnapshotRecalculador snapshotRecalculador;
    private final FalloActaRepository falloActaRepository;
    private final DocumentoPlantillaRepository documentoPlantillaRepository;
    private final TalonarioService talonarioService;
    private final DependenciaRepository dependenciaRepository;
    private final DocumentoFirmaReqRepository documentoFirmaReqRepository;
    private final FirmanteRepository firmanteRepository;

    public DocumentoService(
            ActaRepository actaRepository,
            DocumentoRepository documentoRepository,
            DocumentoFirmaRepository firmaRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            SnapshotRecalculador snapshotRecalculador,
            FalloActaRepository falloActaRepository,
            DocumentoPlantillaRepository documentoPlantillaRepository,
            TalonarioService talonarioService,
            DependenciaRepository dependenciaRepository,
            DocumentoFirmaReqRepository documentoFirmaReqRepository,
            FirmanteRepository firmanteRepository) {
        this.actaRepository = actaRepository;
        this.documentoRepository = documentoRepository;
        this.firmaRepository = firmaRepository;
        this.eventoRepository = eventoRepository;
        this.snapshotRepository = snapshotRepository;
        this.snapshotRecalculador = snapshotRecalculador;
        this.falloActaRepository = falloActaRepository;
        this.documentoPlantillaRepository = documentoPlantillaRepository;
        this.talonarioService = talonarioService;
        this.dependenciaRepository = dependenciaRepository;
        this.documentoFirmaReqRepository = documentoFirmaReqRepository;
        this.firmanteRepository = firmanteRepository;
    }

    // -------------------------------------------------------------------------
    // GenerarDocumento (flujo legacy)
    // -------------------------------------------------------------------------

    public ComandoResultado generarDocumento(GenerarDocumentoCommand cmd) {
        FalActa acta = actaRepository.buscarPorId(cmd.idActa())
                .orElseThrow(() -> new ActaNoEncontradaException(cmd.idActa()));

        if (acta.estaCerrada()) {
            throw new PrecondicionVioladaException("El acta esta cerrada. No se puede generar documento.");
        }

        Long idDoc = documentoRepository.nextId();
        FalDocumento doc = new FalDocumento(
                idDoc,
                acta.getId(),
                cmd.tipoDocumento(),
                LocalDateTime.now(),
                cmd.descripcion()
        );
        documentoRepository.guardar(doc);

        registrarEvento(acta.getId(), TipoEventoActa.DOCGEN, idDoc, null,
                null, "Documento generado: " + cmd.tipoDocumento());

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(acta.getId(), String.valueOf(idDoc),
                TipoEventoActa.DOCGEN.codigo(),
                "Documento generado. Tipo: " + cmd.tipoDocumento() + ". Estado: PENDIENTE_FIRMA");
    }

    // -------------------------------------------------------------------------
    // GenerarDocumentoDesdePlantilla (8C-3, actualizado 8C-5A)
    // -------------------------------------------------------------------------

    public FalDocumento generarDesdePlantilla(GenerarDocumentoDesdePlantillaCommand cmd) {
        if (cmd.idActa() == null) {
            throw new PrecondicionVioladaException("idActa es obligatorio");
        }
        if (cmd.plantillaId() == null) {
            throw new PrecondicionVioladaException("plantillaId es obligatorio");
        }
        if (cmd.idUserAlta() == null || cmd.idUserAlta().isBlank()) {
            throw new PrecondicionVioladaException("idUserAlta es obligatorio y no puede estar en blanco");
        }

        actaRepository.buscarPorId(cmd.idActa())
                .orElseThrow(() -> new ActaNoEncontradaException(cmd.idActa()));

        FalDocumentoPlantilla plantilla = documentoPlantillaRepository.buscarPorId(cmd.plantillaId())
                .orElseThrow(() -> new DocumentoPlantillaNoEncontradaException(cmd.plantillaId()));

        if (!plantilla.isSiActiva()) {
            throw new PrecondicionVioladaException(
                    "La plantilla no esta activa: " + cmd.plantillaId());
        }

        LocalDate hoy = LocalDate.now();
        if (plantilla.getFhVigDesde().isAfter(hoy)) {
            throw new PrecondicionVioladaException(
                    "La plantilla todavia no esta vigente. vigDesde: " + plantilla.getFhVigDesde());
        }
        if (plantilla.getFhVigHasta() != null && plantilla.getFhVigHasta().isBefore(hoy)) {
            throw new PrecondicionVioladaException(
                    "La plantilla esta vencida. vigHasta: " + plantilla.getFhVigHasta());
        }

        Long idDoc = documentoRepository.nextId();
        FalDocumento doc = new FalDocumento(
                idDoc,
                cmd.idActa(),
                plantilla.getTipoDocu(),
                LocalDateTime.now(),
                null,
                EstadoDocu.BORRADOR,
                plantilla.getTipoFirmaReq(),
                plantilla.getId());

        documentoRepository.guardar(doc);

        registrarEvento(cmd.idActa(), TipoEventoActa.DOCGEN, idDoc, null,
                cmd.idUserAlta(), "Documento generado desde plantilla: " + plantilla.getCodigo());

        actaRepository.buscarPorId(cmd.idActa()).ifPresent(acta -> {
            FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
            snapshotRepository.guardar(snap);
        });

        if (plantilla.isSiRequiereNumeracion()
                && plantilla.getMomentoNumeracionDocu() == MomentoNumeracionDocu.AL_CREAR) {
            doc = numerarDocumento(new NumerarDocumentoCommand(doc.getId(), cmd.idUserAlta()));
        }

        return doc;
    }

    // -------------------------------------------------------------------------
    // NumerarDocumento (8C-5A)
    // -------------------------------------------------------------------------

    public FalDocumento numerarDocumento(NumerarDocumentoCommand cmd) {
        if (cmd.documentoId() == null) {
            throw new PrecondicionVioladaException("documentoId es obligatorio para numerar.");
        }
        if (cmd.idUserOperacion() == null || cmd.idUserOperacion().isBlank()) {
            throw new PrecondicionVioladaException("idUserOperacion es obligatorio y no puede estar en blanco.");
        }

        FalDocumento doc = documentoRepository.buscarPorId(cmd.documentoId())
                .orElseThrow(() -> new DocumentoNoEncontradoException(cmd.documentoId()));

        if (doc.getNroDocu() != null) {
            throw new PrecondicionVioladaException(
                    "El documento ya tiene numero asignado: " + doc.getNroDocu()
                    + ". No se puede volver a numerar.");
        }

        if (doc.getPlantillaId() == null) {
            throw new PrecondicionVioladaException(
                    "El documento no tiene plantilla asociada. Solo se pueden numerar documentos generados desde plantilla.");
        }

        Long docPlantillaId = doc.getPlantillaId();
        FalDocumentoPlantilla plantilla = documentoPlantillaRepository.buscarPorId(docPlantillaId)
                .orElseThrow(() -> new DocumentoPlantillaNoEncontradaException(docPlantillaId));

        if (!plantilla.isSiRequiereNumeracion()) {
            throw new PrecondicionVioladaException(
                    "La plantilla no requiere numeracion (siRequiereNumeracion=false). Plantilla: " + docPlantillaId);
        }

        if (plantilla.getMomentoNumeracionDocu() == MomentoNumeracionDocu.NO_APLICA) {
            throw new PrecondicionVioladaException(
                    "La plantilla tiene momentoNumeracionDocu=NO_APLICA. "
                    + "Este documento no debe numerarse. Plantilla: " + doc.getPlantillaId());
        }

        FalActa acta = actaRepository.buscarPorId(doc.getIdActa())
                .orElseThrow(() -> new ActaNoEncontradaException(doc.getIdActa()));

        FalDependencia dependencia = dependenciaRepository.findById(acta.getIdDependencia())
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "No se encontro dependencia con codDep='" + acta.getIdDependencia()
                        + "'. No se puede determinar idDep para numeracion documental."));

        FalDependenciaVersion versionDep = dependenciaRepository
                .findVersionVigente(dependencia.getIdDep(), LocalDate.now())
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "No hay version vigente para la dependencia id=" + dependencia.getIdDep()
                        + ". No se puede determinar verDep para numeracion documental."));

        EmitirNumeroDocumentoCommand emitirCmd = new EmitirNumeroDocumentoCommand(
                dependencia.getIdDep(),
                (short) versionDep.getVerDep(),
                doc.getTipoDocu().codigo(),
                doc.getId(),
                LocalDateTime.now(),
                cmd.idUserOperacion()
        );

        NumeroDocumentoEmitidoResponse resultado = talonarioService.emitirNumeroDocumento(emitirCmd);

        doc.setNroDocu(resultado.nroDocu());
        doc.setIdTalonario(resultado.idTalonario());
        doc.setNroTalonarioUsado(resultado.nroTalonario());

        documentoRepository.guardar(doc);

        return doc;
    }

    // -------------------------------------------------------------------------
    // EnviarAFirma (8C-5B)
    // -------------------------------------------------------------------------

    public FalDocumento enviarAFirma(EnviarAFirmaCommand cmd) {
        if (cmd.documentoId() == null) {
            throw new PrecondicionVioladaException("documentoId es obligatorio para enviar a firma.");
        }
        if (cmd.idUserOperacion() == null || cmd.idUserOperacion().isBlank()) {
            throw new PrecondicionVioladaException("idUserOperacion es obligatorio y no puede estar en blanco.");
        }

        FalDocumento doc = documentoRepository.buscarPorId(cmd.documentoId())
                .orElseThrow(() -> new DocumentoNoEncontradoException(cmd.documentoId()));

        if (doc.getEstadoDocu() != EstadoDocu.BORRADOR) {
            throw new PrecondicionVioladaException(
                    "El documento debe estar en estado BORRADOR para enviar a firma. Estado actual: "
                    + doc.getEstadoDocu());
        }

        if (doc.getPlantillaId() == null) {
            throw new PrecondicionVioladaException(
                    "El documento no tiene plantilla asociada. Solo se pueden enviar a firma "
                    + "documentos generados desde plantilla.");
        }

        Long docPlantillaId = doc.getPlantillaId();
        FalDocumentoPlantilla plantilla = documentoPlantillaRepository.buscarPorId(docPlantillaId)
                .orElseThrow(() -> new DocumentoPlantillaNoEncontradaException(docPlantillaId));

        if (doc.getTipoFirmaReq() == TipoFirmaReq.NO_REQUIERE) {
            throw new PrecondicionVioladaException(
                    "El documento tiene tipoFirmaReq=NO_REQUIERE. "
                    + "Los documentos sin requisito de firma no pasan por estado PENDIENTE_FIRMA.");
        }

        MomentoNumeracionDocu momento = plantilla.getMomentoNumeracionDocu();

        if (momento == MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA) {
            doc = numerarDocumento(new NumerarDocumentoCommand(doc.getId(), cmd.idUserOperacion()));
        } else if (momento == MomentoNumeracionDocu.AL_CREAR && doc.getNroDocu() == null) {
            throw new PrecondicionVioladaException(
                    "Inconsistencia: momentoNumeracionDocu=AL_CREAR pero el documento no tiene nroDocu. "
                    + "El documento debio haber sido numerado automaticamente al crearse desde la plantilla.");
        }

        if (!documentoFirmaReqRepository.existePorDocumento(doc.getId())) {
            materializarFirmaReqDesdeInternal(doc, plantilla, cmd.idUserOperacion());
        }

        doc.setEstadoDocu(EstadoDocu.PENDIENTE_FIRMA);
        documentoRepository.guardar(doc);

        return doc;
    }

    private void materializarFirmaReqDesdeInternal(FalDocumento doc, FalDocumentoPlantilla plantilla, String idUser) {
        List<FalDocumentoPlantillaFirmaReq> activos = documentoPlantillaRepository
                .listarFirmaReqPorPlantilla(plantilla.getId())
                .stream().filter(FalDocumentoPlantillaFirmaReq::isSiActiva).toList();

        LocalDateTime ahora = LocalDateTime.now();
        for (FalDocumentoPlantillaFirmaReq req : activos) {
            Long id = documentoFirmaReqRepository.nextId();
            FalDocumentoFirmaReq firmaReq = new FalDocumentoFirmaReq(
                    id, doc.getId(), req.getSeqFirmaReq(), req.getRolFirmaReq(),
                    req.getMecanismoFirmaReq(), null, req.isSiObligatoria(), true, ahora, idUser);
            documentoFirmaReqRepository.guardar(firmaReq);
        }
    }

    // -------------------------------------------------------------------------
    // FirmarDocumento (flujo naive legacy - se mantiene para compatibilidad)
    // -------------------------------------------------------------------------

    public ComandoResultado firmarDocumento(FirmarDocumentoCommand cmd) {
        FalDocumento doc = documentoRepository.buscarPorId(cmd.idDocumento())
                .orElseThrow(() -> new DocumentoNoEncontradoException(cmd.idDocumento()));

        FalActa acta = actaRepository.buscarPorId(doc.getIdActa())
                .orElseThrow(() -> new ActaNoEncontradaException(doc.getIdActa()));

        if (acta.estaCerrada()) {
            throw new PrecondicionVioladaException("El acta esta cerrada. No se puede firmar documento.");
        }
        if (!doc.pendienteFirma()) {
            throw new PrecondicionVioladaException(
                    "El documento no esta en estado PENDIENTE_FIRMA. Estado actual: "
                            + doc.getEstadoDocu());
        }

        doc.setEstadoDocu(EstadoDocu.FIRMADO);
        documentoRepository.guardar(doc);

        TipoFirma tipoFirmaEnum;
        if (cmd.tipoFirma() != null) {
            try {
                tipoFirmaEnum = TipoFirma.valueOf(cmd.tipoFirma());
            } catch (IllegalArgumentException e) {
                tipoFirmaEnum = TipoFirma.SISTEMA;
            }
        } else {
            tipoFirmaEnum = TipoFirma.DIGITAL;
        }

        Long firmaId = firmaRepository.nextId();
        String firmante = cmd.firmante() != null ? cmd.firmante() : "SISTEMA";
        FalDocumentoFirma firma = new FalDocumentoFirma(
                firmaId,
                doc.getId(),
                (short) 1,
                0L,
                (short) 1,
                firmante,
                (short) 1,
                firmante,
                tipoFirmaEnum,
                EstadoFirma.FIRMADA,
                null,
                null,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                firmante
        );
        firmaRepository.guardar(firma);

        Long docId = doc.getId();
        falloActaRepository.buscarActivo(acta.getId()).ifPresent(fallo -> {
            if (docId.equals(fallo.getDocumentoId())) {
                fallo.setEstadoFallo(EstadoFalloActa.FIRMADO);
                falloActaRepository.guardar(fallo);
            }
        });

        registrarEvento(acta.getId(), TipoEventoActa.DOCFIR, doc.getId(), null,
                firmante, "Documento firmado por: " + firmante);

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(acta.getId(), String.valueOf(doc.getId()),
                TipoEventoActa.DOCFIR.codigo(),
                "Documento firmado. Estado: FIRMADO");
    }

    // -------------------------------------------------------------------------
    // RegistrarFirmaDocumental (8C-6B-1) - firma real con validacion completa
    // -------------------------------------------------------------------------

    public FalDocumentoFirma registrarFirmaDocumental(RegistrarFirmaDocumentalCommand cmd) {
        if (cmd.documentoId() == null) {
            throw new PrecondicionVioladaException("documentoId es obligatorio.");
        }
        if (cmd.seqFirmaReq() <= 0) {
            throw new PrecondicionVioladaException("seqFirmaReq debe ser mayor que cero.");
        }
        if (cmd.idFirmante() == null) {
            throw new PrecondicionVioladaException("idFirmante es obligatorio.");
        }
        if (cmd.tipoFirma() == null) {
            throw new PrecondicionVioladaException("tipoFirma es obligatorio.");
        }
        if (cmd.idUserFirma() == null || cmd.idUserFirma().isBlank()) {
            throw new PrecondicionVioladaException("idUserFirma es obligatorio y no puede estar en blanco.");
        }

        if (cmd.tipoFirma() == TipoFirma.DIGITAL) {
            if (cmd.hashDocumento() == null || cmd.hashDocumento().isBlank()) {
                throw new PrecondicionVioladaException("hashDocumento es obligatorio para firma DIGITAL.");
            }
            if (cmd.referenciaFirmaExt() == null || cmd.referenciaFirmaExt().isBlank()) {
                throw new PrecondicionVioladaException("referenciaFirmaExt es obligatorio para firma DIGITAL.");
            }
        }

        FalDocumento doc = documentoRepository.buscarPorId(cmd.documentoId())
                .orElseThrow(() -> new DocumentoNoEncontradoException(cmd.documentoId()));

        if (doc.getEstadoDocu() != EstadoDocu.PENDIENTE_FIRMA) {
            throw new PrecondicionVioladaException(
                    "El documento debe estar en estado PENDIENTE_FIRMA para registrar firma. Estado actual: "
                    + doc.getEstadoDocu());
        }

        if (doc.getPlantillaId() == null) {
            throw new PrecondicionVioladaException(
                    "El documento no tiene plantilla asociada. Solo se pueden firmar documentos generados desde plantilla.");
        }

        Long plantillaId = doc.getPlantillaId();
        FalDocumentoPlantilla plantilla = documentoPlantillaRepository.buscarPorId(plantillaId)
                .orElseThrow(() -> new DocumentoPlantillaNoEncontradaException(plantillaId));

        FalDocumentoFirmaReq req = documentoFirmaReqRepository
                .buscarPorDocumentoYSeq(cmd.documentoId(), cmd.seqFirmaReq())
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "No existe requisito de firma con seqFirmaReq=" + cmd.seqFirmaReq()
                        + " para el documento " + cmd.documentoId()));

        if (!req.isSiActiva()) {
            throw new PrecondicionVioladaException(
                    "El requisito de firma seqFirmaReq=" + cmd.seqFirmaReq() + " esta inactivo.");
        }

        if (req.getEstadoFirmaReq() != EstadoFirmaReq.PENDIENTE) {
            throw new PrecondicionVioladaException(
                    "El requisito de firma no esta en estado PENDIENTE. Estado actual: "
                    + req.getEstadoFirmaReq());
        }

        FalFirmante firmante = resolverFirmante(cmd.idFirmante());
        FalFirmanteVersion versionVigente = resolverVersionVigente(cmd.idFirmante());

        TipoDocu tipoDocuDoc = doc.getTipoDocu();
        FalFirmanteVersionHabilitacion habilitacion = resolverHabilitacion(
                cmd.idFirmante(), versionVigente.getVerFirmante(),
                tipoDocuDoc.codigo(), req.getRolFirmaReq());

        validarMecanismo(habilitacion, req.getMecanismoFirmaReq());
        validarOrdenFirma(cmd.documentoId(), req.getOrdenFirma());

        if (plantilla.isSiRequiereNumeracion()
                && plantilla.getMomentoNumeracionDocu() == MomentoNumeracionDocu.AL_FIRMAR
                && doc.getNroDocu() == null) {
            doc = numerarDocumento(new NumerarDocumentoCommand(doc.getId(), cmd.idUserFirma()));
        }

        LocalDateTime ahora = LocalDateTime.now();
        Long firmaId = firmaRepository.nextId();

        FalDocumentoFirma firma = new FalDocumentoFirma(
                firmaId,
                doc.getId(),
                cmd.seqFirmaReq(),
                cmd.idFirmante(),
                (short) versionVigente.getVerFirmante(),
                cmd.idUserFirma(),
                habilitacion.getRolFirmaReq(),
                firmante.getNomFirmante(),
                cmd.tipoFirma(),
                EstadoFirma.FIRMADA,
                cmd.hashDocumento(),
                cmd.referenciaFirmaExt(),
                cmd.storageKey(),
                null,
                ahora,
                ahora,
                cmd.idUserFirma()
        );
        firmaRepository.guardar(firma);

        req.marcarFirmado(firmaId, ahora, cmd.idFirmante(), (short) versionVigente.getVerFirmante());
        documentoFirmaReqRepository.guardar(req);

        List<FalDocumentoFirmaReq> todosReqs = documentoFirmaReqRepository.listarPorDocumento(doc.getId());
        boolean todosFirmados = todosReqs.stream()
                .filter(r -> r.isSiObligatoria() && r.isSiActiva())
                .allMatch(r -> r.getEstadoFirmaReq() == EstadoFirmaReq.FIRMADO);

        if (todosFirmados) {
            doc.marcarFirmado();
            documentoRepository.guardar(doc);

            FalActa acta = actaRepository.buscarPorId(doc.getIdActa()).orElse(null);
            if (acta != null) {
                Long docId = doc.getId();
                falloActaRepository.buscarActivo(acta.getId()).ifPresent(fallo -> {
                    if (docId.equals(fallo.getDocumentoId())) {
                        fallo.setEstadoFallo(EstadoFalloActa.FIRMADO);
                        falloActaRepository.guardar(fallo);
                    }
                });

                registrarEvento(acta.getId(), TipoEventoActa.DOCFIR, doc.getId(), null,
                        cmd.idUserFirma(), "Documento firmado por: " + firmante.getNomFirmante());

                FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
                snapshotRepository.guardar(snap);
            }
        }

        return firma;
    }

    // -------------------------------------------------------------------------
    // EmitirDocumento (8C-6C-1) - emision formal in-memory con storage/hash simulado
    // -------------------------------------------------------------------------

    /**
     * Emite formalmente un documento.
     *
     * Flujos:
     * - tipoFirmaReq = NO_REQUIERE: BORRADOR -> EMITIDO
     * - tipoFirmaReq != NO_REQUIERE: FIRMADO -> EMITIDO
     * - AL_EMITIR: numera automaticamente antes de consolidar storage/hash.
     * - siGeneraPdf = true: storageKey y hashDocu obligatorios.
     * - siGeneraPdf = false: storageKey y hashDocu pueden ser null.
     *
     * Slice 8C-6C-1.
     */
    public FalDocumento emitirDocumento(EmitirDocumentoCommand cmd) {
        FalDocumento doc = documentoRepository.buscarPorId(cmd.documentoId())
                .orElseThrow(() -> new DocumentoNoEncontradoException(cmd.documentoId()));

        if (doc.getPlantillaId() == null) {
            throw new PrecondicionVioladaException(
                    "El documento no tiene plantilla asociada. Solo se pueden emitir documentos generados desde plantilla.");
        }

        Long plantillaId = doc.getPlantillaId();
        FalDocumentoPlantilla plantilla = documentoPlantillaRepository.buscarPorId(plantillaId)
                .orElseThrow(() -> new DocumentoPlantillaNoEncontradaException(plantillaId));

        EstadoDocu estado = doc.getEstadoDocu();
        if (estado == EstadoDocu.EMITIDO) {
            throw new PrecondicionVioladaException("El documento ya esta en estado EMITIDO.");
        }
        if (estado == EstadoDocu.ANULADO) {
            throw new PrecondicionVioladaException("No se puede emitir un documento ANULADO.");
        }
        if (estado == EstadoDocu.REEMPLAZADO) {
            throw new PrecondicionVioladaException("No se puede emitir un documento REEMPLAZADO.");
        }

        TipoFirmaReq tipoFirmaReq = doc.getTipoFirmaReq();
        if (tipoFirmaReq == TipoFirmaReq.NO_REQUIERE) {
            if (estado != EstadoDocu.BORRADOR) {
                throw new PrecondicionVioladaException(
                        "Documento con tipoFirmaReq=NO_REQUIERE debe estar en BORRADOR para emitir. Estado actual: " + estado);
            }
        } else {
            if (estado == EstadoDocu.PENDIENTE_FIRMA) {
                throw new PrecondicionVioladaException(
                        "Documento en estado PENDIENTE_FIRMA no puede emitirse. Debe completar el proceso de firma primero.");
            }
            if (estado != EstadoDocu.FIRMADO) {
                throw new PrecondicionVioladaException(
                        "Documento con firma requerida debe estar en FIRMADO para emitir. Estado actual: " + estado);
            }
        }

        MomentoNumeracionDocu momento = plantilla.getMomentoNumeracionDocu();
        if (momento == MomentoNumeracionDocu.AL_EMITIR) {
            if (doc.getNroDocu() == null) {
                doc = numerarDocumento(new NumerarDocumentoCommand(doc.getId(), cmd.idUserOperacion()));
            }
        } else if (momento != MomentoNumeracionDocu.NO_APLICA) {
            if (doc.getNroDocu() == null) {
                throw new PrecondicionVioladaException(
                        "Inconsistencia: momentoNumeracionDocu=" + momento
                        + " pero el documento no tiene nroDocu asignado. "
                        + "El documento debio haberse numerado en el momento correspondiente.");
            }
        }

        if (plantilla.isSiGeneraPdf()) {
            if (cmd.storageKey() == null || cmd.storageKey().isBlank()) {
                throw new PrecondicionVioladaException(
                        "storageKey es obligatorio para documentos con siGeneraPdf=true.");
            }
            if (cmd.hashDocu() == null || cmd.hashDocu().isBlank()) {
                throw new PrecondicionVioladaException(
                        "hashDocu es obligatorio para documentos con siGeneraPdf=true.");
            }
        }

        LocalDateTime ahora = LocalDateTime.now();
        doc.marcarEmitido(cmd.storageKey(), cmd.hashDocu(), ahora);
        documentoRepository.guardar(doc);

        registrarEvento(doc.getIdActa(), TipoEventoActa.DOCEMI, doc.getId(), null,
                cmd.idUserOperacion(), "Documento emitido formalmente.");

        return doc;
    }

    // -------------------------------------------------------------------------
    // IncorporarDocumentoEscaneado (8C-6D-1) - adjunto externo
    // -------------------------------------------------------------------------

    /**
     * Incorpora un documento escaneado/adjunto externo al expediente.
     *
     * El documento externo queda en EstadoDocu.ADJUNTO.
     * storageKey, hashDocu y fhGeneracion son obligatorios.
     * No se numera, no se emite y no se firma digitalmente.
     *
     * Slice 8C-6D-1.
     */
    public FalDocumento incorporarDocumentoEscaneado(IncorporarDocumentoEscaneadoCommand cmd) {
        if (cmd.idActa() == null) {
            throw new PrecondicionVioladaException("idActa es obligatorio.");
        }
        if (cmd.tipoDocu() == null) {
            throw new PrecondicionVioladaException("tipoDocu es obligatorio.");
        }
        if (cmd.storageKey() == null || cmd.storageKey().isBlank()) {
            throw new PrecondicionVioladaException("storageKey es obligatorio.");
        }
        if (cmd.hashDocu() == null || cmd.hashDocu().isBlank()) {
            throw new PrecondicionVioladaException("hashDocu es obligatorio.");
        }
        if (cmd.idUserAlta() == null || cmd.idUserAlta().isBlank()) {
            throw new PrecondicionVioladaException("idUserAlta es obligatorio.");
        }

        actaRepository.buscarPorId(cmd.idActa())
                .orElseThrow(() -> new ActaNoEncontradaException(cmd.idActa()));

        if (cmd.plantillaId() != null) {
            documentoPlantillaRepository.buscarPorId(cmd.plantillaId())
                    .orElseThrow(() -> new DocumentoPlantillaNoEncontradaException(cmd.plantillaId()));
        }

        Long idDoc = documentoRepository.nextId();
        LocalDateTime ahora = LocalDateTime.now();
        FalDocumento doc = FalDocumento.crearAdjunto(
                idDoc, cmd.idActa(), cmd.tipoDocu(),
                cmd.storageKey(), cmd.hashDocu(), ahora, cmd.plantillaId());

        documentoRepository.guardar(doc);

        registrarEvento(cmd.idActa(), TipoEventoActa.DOCADJ, idDoc,
                null, cmd.idUserAlta(), "Documento escaneado incorporado. Tipo: " + cmd.tipoDocu());

        actaRepository.buscarPorId(cmd.idActa()).ifPresent(acta -> {
            FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
            snapshotRepository.guardar(snap);
        });

        return doc;
    }

    // -------------------------------------------------------------------------
    // ConvalidarFirmaEscaneada (8C-6D-1) - convalidacion olografa visible
    // -------------------------------------------------------------------------

    /**
     * Convalida la firma escaneada/olografa visible en un documento adjunto.
     *
     * Si seqFirmaReq es null: solo trazabilidad, evento DOCFIR, documento permanece ADJUNTO.
     * Si seqFirmaReq no es null: crea FalDocumentoFirma OLOGRAFA, marca requisito FIRMADO,
     *   si todos obligatorios activos firmados -> ADJUNTO -> FIRMADO.
     *
     * No genera firma digital. No recalcula hash. No emite automaticamente.
     * Slice 8C-6D-1.
     */
    public ConvalidacionEscaneadaResultado convalidarFirmaEscaneada(ConvalidarFirmaEscaneadaCommand cmd) {
        if (cmd.documentoId() == null) {
            throw new PrecondicionVioladaException("documentoId es obligatorio.");
        }
        if (cmd.idFirmante() == null) {
            throw new PrecondicionVioladaException("idFirmante es obligatorio.");
        }
        if (cmd.idUserFirma() == null || cmd.idUserFirma().isBlank()) {
            throw new PrecondicionVioladaException("idUserFirma es obligatorio y no puede estar en blanco.");
        }

        FalDocumento doc = documentoRepository.buscarPorId(cmd.documentoId())
                .orElseThrow(() -> new DocumentoNoEncontradoException(cmd.documentoId()));

        if (doc.getEstadoDocu() != EstadoDocu.ADJUNTO) {
            throw new PrecondicionVioladaException(
                    "El documento debe estar en estado ADJUNTO para convalidar firma escaneada. Estado actual: "
                    + doc.getEstadoDocu());
        }
        if (doc.getStorageKey() == null || doc.getStorageKey().isBlank()) {
            throw new PrecondicionVioladaException(
                    "El documento adjunto no tiene storageKey. No se puede convalidar.");
        }
        if (doc.getHashDocu() == null || doc.getHashDocu().isBlank()) {
            throw new PrecondicionVioladaException(
                    "El documento adjunto no tiene hashDocu. No se puede convalidar.");
        }

        FalFirmante firmante = resolverFirmante(cmd.idFirmante());
        FalFirmanteVersion versionVigente = resolverVersionVigente(cmd.idFirmante());

        if (cmd.seqFirmaReq() == null) {
            // Convalidacion simple: trazabilidad sin cumplir requisito.
            // No se crea FalDocumentoFirma (seqFirmaReq es primitivo, no puede ser null ni 0).
            registrarEvento(doc.getIdActa(), TipoEventoActa.DOCFIR, doc.getId(),
                    null, cmd.idUserFirma(),
                    "Convalidacion simple de firma escaneada por: " + firmante.getNomFirmante()
                    + " (sin requisito). Documento permanece en ADJUNTO.");
            return new ConvalidacionEscaneadaResultado(doc, null);
        }

        // Convalidacion que cumple requisito
        FalDocumentoFirmaReq req = documentoFirmaReqRepository
                .buscarPorDocumentoYSeq(cmd.documentoId(), cmd.seqFirmaReq())
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "No existe requisito de firma con seqFirmaReq=" + cmd.seqFirmaReq()
                        + " para el documento " + cmd.documentoId()));

        if (!req.isSiActiva()) {
            throw new PrecondicionVioladaException(
                    "El requisito de firma seqFirmaReq=" + cmd.seqFirmaReq() + " esta inactivo.");
        }
        if (req.getEstadoFirmaReq() != EstadoFirmaReq.PENDIENTE) {
            throw new PrecondicionVioladaException(
                    "El requisito de firma no esta en estado PENDIENTE. Estado actual: "
                    + req.getEstadoFirmaReq());
        }

        FalFirmanteVersionHabilitacion habilitacion = resolverHabilitacion(
                cmd.idFirmante(), versionVigente.getVerFirmante(),
                doc.getTipoDocu().codigo(), req.getRolFirmaReq());

        validarMecanismo(habilitacion, req.getMecanismoFirmaReq());
        validarOrdenFirma(cmd.documentoId(), req.getOrdenFirma());

        LocalDateTime ahora = LocalDateTime.now();
        Long firmaId = firmaRepository.nextId();

        FalDocumentoFirma firma = new FalDocumentoFirma(
                firmaId,
                doc.getId(),
                cmd.seqFirmaReq(),
                cmd.idFirmante(),
                (short) versionVigente.getVerFirmante(),
                cmd.idUserFirma(),
                habilitacion.getRolFirmaReq(),
                firmante.getNomFirmante(),
                TipoFirma.OLOGRAFA,
                EstadoFirma.FIRMADA,
                doc.getHashDocu(),
                cmd.referenciaFirmaExt(),
                doc.getStorageKey(),
                null,
                ahora,
                ahora,
                cmd.idUserFirma()
        );
        firmaRepository.guardar(firma);

        req.marcarFirmado(firmaId, ahora, cmd.idFirmante(), (short) versionVigente.getVerFirmante());
        documentoFirmaReqRepository.guardar(req);

        List<FalDocumentoFirmaReq> todosReqs = documentoFirmaReqRepository.listarPorDocumento(doc.getId());
        boolean todosFirmados = todosReqs.stream()
                .filter(r -> r.isSiObligatoria() && r.isSiActiva())
                .allMatch(r -> r.getEstadoFirmaReq() == EstadoFirmaReq.FIRMADO);

        if (todosFirmados) {
            doc.marcarFirmadoDesdeAdjunto();
            documentoRepository.guardar(doc);
        }

        registrarEvento(doc.getIdActa(), TipoEventoActa.DOCFIR, doc.getId(),
                null, cmd.idUserFirma(),
                "Firma escaneada convalidada (OLOGRAFA) por: " + firmante.getNomFirmante()
                + ". Requisito seq=" + cmd.seqFirmaReq() + " marcado FIRMADO.");

        actaRepository.buscarPorId(doc.getIdActa()).ifPresent(acta -> {
            FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
            snapshotRepository.guardar(snap);
        });

        return new ConvalidacionEscaneadaResultado(doc, firma);
    }

    // -------------------------------------------------------------------------
    // Consultas
    // -------------------------------------------------------------------------

    public List<FalDocumento> obtenerDocumentos(Long idActa) {
        actaRepository.buscarPorId(idActa)
                .orElseThrow(() -> new ActaNoEncontradaException(idActa));
        return documentoRepository.buscarPorActa(idActa);
    }

    // -------------------------------------------------------------------------
    // Helpers privados compartidos de validacion de firmante (8C-6D-1)
    // -------------------------------------------------------------------------

    private FalFirmante resolverFirmante(Long idFirmante) {
        return firmanteRepository.findById(idFirmante)
                .orElseThrow(() -> new FirmanteNoEncontradoException(idFirmante));
    }

    private FalFirmanteVersion resolverVersionVigente(Long idFirmante) {
        return firmanteRepository.findVersionVigente(idFirmante, LocalDate.now())
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "El firmante id=" + idFirmante + " no tiene version vigente."));
    }

    private FalFirmanteVersionHabilitacion resolverHabilitacion(
            Long idFirmante, int verFirmante, short tipoDocuCodigo, short rolFirmaReq) {
        return firmanteRepository.findHabilitacionActiva(idFirmante, verFirmante, tipoDocuCodigo, rolFirmaReq)
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "El firmante id=" + idFirmante
                        + " no tiene habilitacion activa para tipoDocu=" + tipoDocuCodigo
                        + " y rolFirmaReq=" + rolFirmaReq));
    }

    private void validarMecanismo(FalFirmanteVersionHabilitacion habilitacion, Short mecanismoFirmaReq) {
        if (mecanismoFirmaReq != null) {
            if (habilitacion.getMecanismoFirmaReq() == null
                    || !habilitacion.getMecanismoFirmaReq().equals(mecanismoFirmaReq)) {
                throw new PrecondicionVioladaException(
                        "El requisito exige mecanismoFirmaReq=" + mecanismoFirmaReq
                        + " pero la habilitacion del firmante tiene mecanismoFirmaReq="
                        + habilitacion.getMecanismoFirmaReq());
            }
        }
    }

    private void validarOrdenFirma(Long documentoId, Short ordenFirma) {
        if (ordenFirma != null) {
            short ordenActual = ordenFirma;
            List<FalDocumentoFirmaReq> todoReqs = documentoFirmaReqRepository.listarPorDocumento(documentoId);
            boolean hayAnteriorPendiente = todoReqs.stream()
                    .anyMatch(r -> r.isSiObligatoria()
                            && r.isSiActiva()
                            && r.getOrdenFirma() != null
                            && r.getOrdenFirma() < ordenActual
                            && r.getEstadoFirmaReq() != EstadoFirmaReq.FIRMADO);
            if (hayAnteriorPendiente) {
                throw new PrecondicionVioladaException(
                        "No se puede firmar el requisito con ordenFirma=" + ordenActual
                        + " porque hay requisitos obligatorios con orden menor pendientes de firma.");
            }
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
                .fhEvt(LocalDateTime.now())
                .idDocuRel(idDocuRel)
                .idNotifRel(idNotifRel)
                .idUserEvt(idUserEvt)
                .actorTipo(idUserEvt != null ? ActorTipoEvento.USUARIO_INTERNO : ActorTipoEvento.SISTEMA)
                .descripcionLegible(descripcionLegible)
                .build();
        eventoRepository.registrar(evento);
    }
}
