package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.AgregarFirmaReqPlantillaCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearDocumentoPlantillaCommand;
import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.enums.MomentoNumeracionDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFirmaReq;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoPlantillaDuplicadaException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoPlantillaInvalidaException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoPlantillaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantilla;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantillaFirmaReq;
import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaRepository;
import org.springframework.stereotype.Service;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio de administracion de plantillas documentales in-memory.
 *
 * Centraliza validaciones de dominio.
 * No genera documentos concretos.
 * No usa talonarios.
 * No implementa FalDocumentoFirmaReq.
 */
@Service
public class DocumentoPlantillaService {

    private final DocumentoPlantillaRepository repository;
    private final FaltasClock faltasClock;

    public DocumentoPlantillaService(DocumentoPlantillaRepository repository,
            FaltasClock faltasClock) {
        this.faltasClock = faltasClock;
        this.repository = repository;
    }

    public FalDocumentoPlantilla crear(CrearDocumentoPlantillaCommand cmd) {
        if (cmd.codigo() == null || cmd.codigo().isBlank()) {
            throw new DocumentoPlantillaInvalidaException("El codigo de la plantilla es obligatorio.");
        }
        if (cmd.nombre() == null || cmd.nombre().isBlank()) {
            throw new DocumentoPlantillaInvalidaException("El nombre de la plantilla es obligatorio.");
        }
        if (cmd.tipoDocu() == null) {
            throw new DocumentoPlantillaInvalidaException("El tipo de documento es obligatorio.");
        }
        if (cmd.accionDocumental() == null) {
            throw new DocumentoPlantillaInvalidaException("La accion documental es obligatoria.");
        }
        if (cmd.tipoFirmaReq() == null) {
            throw new DocumentoPlantillaInvalidaException("El tipo de firma requerida es obligatorio.");
        }
        if (cmd.momentoNumeracionDocu() == null) {
            throw new DocumentoPlantillaInvalidaException("El momento de numeracion es obligatorio.");
        }
        if (cmd.fhVigDesde() == null) {
            throw new DocumentoPlantillaInvalidaException("La fecha de inicio de vigencia es obligatoria.");
        }
        if (cmd.idUserAlta() == null || cmd.idUserAlta().isBlank()) {
            throw new DocumentoPlantillaInvalidaException("El idUserAlta es obligatorio.");
        }

        validarCoherenciaNumeracion(cmd.siRequiereNumeracion(), cmd.momentoNumeracionDocu());

        if (cmd.fhVigHasta() != null && cmd.fhVigHasta().isBefore(cmd.fhVigDesde())) {
            throw new DocumentoPlantillaInvalidaException(
                    "fhVigHasta no puede ser anterior a fhVigDesde.");
        }

        repository.buscarPorCodigo(cmd.codigo()).ifPresent(p -> {
            throw new DocumentoPlantillaDuplicadaException(cmd.codigo());
        });

        Long id = repository.nextPlantillaId();
        FalDocumentoPlantilla plantilla = new FalDocumentoPlantilla(
                id,
                cmd.codigo(),
                cmd.nombre(),
                cmd.tipoDocu(),
                cmd.accionDocumental(),
                cmd.tipoActa(),
                cmd.tipoFirmaReq(),
                cmd.siRequiereNumeracion(),
                cmd.momentoNumeracionDocu(),
                cmd.siNotificable(),
                cmd.siGeneraPdf(),
                cmd.siSeleccionable(),
                false,
                cmd.fhVigDesde(),
                cmd.fhVigHasta(),
                faltasClock.now(),
                cmd.idUserAlta()
        );
        return repository.guardar(plantilla);
    }

    public FalDocumentoPlantillaFirmaReq agregarFirmaReq(AgregarFirmaReqPlantillaCommand cmd) {
        repository.buscarPorId(cmd.plantillaId())
                .orElseThrow(() -> new DocumentoPlantillaNoEncontradaException(cmd.plantillaId()));

        if (cmd.seqFirmaReq() <= 0) {
            throw new DocumentoPlantillaInvalidaException(
                    "seqFirmaReq debe ser mayor que cero.");
        }
        if (cmd.rolFirmaReq() <= 0) {
            throw new DocumentoPlantillaInvalidaException(
                    "rolFirmaReq debe ser mayor que cero.");
        }

        if (cmd.siActiva()) {
            boolean seqDuplicada = repository.listarFirmaReqPorPlantilla(cmd.plantillaId())
                    .stream()
                    .anyMatch(r -> r.isSiActiva() && r.getSeqFirmaReq() == cmd.seqFirmaReq());
            if (seqDuplicada) {
                throw new DocumentoPlantillaInvalidaException(
                        "Ya existe un requisito activo con seqFirmaReq=" + cmd.seqFirmaReq()
                                + " en la plantilla " + cmd.plantillaId());
            }
        }

        Long id = repository.nextFirmaReqId();
        FalDocumentoPlantillaFirmaReq req = new FalDocumentoPlantillaFirmaReq(
                id,
                cmd.plantillaId(),
                cmd.seqFirmaReq(),
                cmd.rolFirmaReq(),
                cmd.mecanismoFirmaReq(),
                cmd.siObligatoria(),
                cmd.siActiva(),
                faltasClock.now(),
                cmd.idUserAlta()
        );
        return repository.guardarFirmaReq(req);
    }

    public FalDocumentoPlantilla activar(Long plantillaId) {
        FalDocumentoPlantilla plantilla = repository.buscarPorId(plantillaId)
                .orElseThrow(() -> new DocumentoPlantillaNoEncontradaException(plantillaId));

        if (plantilla.getFhVigHasta() != null
                && plantilla.getFhVigHasta().isBefore(faltasClock.now().toLocalDate())) {
            throw new DocumentoPlantillaInvalidaException(
                    "No se puede activar una plantilla cuya vigencia ya vencio (fhVigHasta="
                            + plantilla.getFhVigHasta() + ").");
        }

        validarCoherenciaNumeracion(plantilla.isSiRequiereNumeracion(), plantilla.getMomentoNumeracionDocu());

        List<FalDocumentoPlantillaFirmaReq> reqs = repository.listarFirmaReqPorPlantilla(plantillaId);
        long obligatoriasActivas = reqs.stream()
                .filter(r -> r.isSiActiva() && r.isSiObligatoria())
                .count();

        TipoFirmaReq tipo = plantilla.getTipoFirmaReq();

        if (tipo == TipoFirmaReq.NO_REQUIERE) {
            if (obligatoriasActivas > 0) {
                throw new DocumentoPlantillaInvalidaException(
                        "La plantilla NO_REQUIERE no puede tener requisitos obligatorios activos.");
            }
        } else if (tipo == TipoFirmaReq.FIRMA_MULTIPLE) {
            if (obligatoriasActivas < 2) {
                throw new DocumentoPlantillaInvalidaException(
                        "La plantilla FIRMA_MULTIPLE requiere al menos dos requisitos obligatorios activos. Tiene: "
                                + obligatoriasActivas);
            }
        } else {
            if (obligatoriasActivas < 1) {
                throw new DocumentoPlantillaInvalidaException(
                        "La plantilla " + tipo + " requiere al menos un requisito obligatorio activo.");
            }
        }

        plantilla.setSiActiva(true);
        return repository.guardar(plantilla);
    }

    public FalDocumentoPlantilla desactivar(Long plantillaId) {
        FalDocumentoPlantilla plantilla = repository.buscarPorId(plantillaId)
                .orElseThrow(() -> new DocumentoPlantillaNoEncontradaException(plantillaId));
        plantilla.setSiActiva(false);
        return repository.guardar(plantilla);
    }

    public FalDocumentoPlantilla obtener(Long plantillaId) {
        return repository.buscarPorId(plantillaId)
                .orElseThrow(() -> new DocumentoPlantillaNoEncontradaException(plantillaId));
    }

    public List<FalDocumentoPlantilla> listar() {
        return repository.listar();
    }

    public List<FalDocumentoPlantilla> listarPorAccion(AccionDocumental accionDocumental) {
        return repository.buscarPorAccion(accionDocumental);
    }

    public List<FalDocumentoPlantilla> listarActivasPorAccion(AccionDocumental accionDocumental) {
        return repository.buscarActivasPorAccion(accionDocumental);
    }

    public List<FalDocumentoPlantillaFirmaReq> listarFirmaReq(Long plantillaId) {
        repository.buscarPorId(plantillaId)
                .orElseThrow(() -> new DocumentoPlantillaNoEncontradaException(plantillaId));
        return repository.listarFirmaReqPorPlantilla(plantillaId);
    }

    private void validarCoherenciaNumeracion(boolean siRequiereNumeracion,
                                              MomentoNumeracionDocu momento) {
        if (!siRequiereNumeracion && momento != MomentoNumeracionDocu.NO_APLICA) {
            throw new DocumentoPlantillaInvalidaException(
                    "Si siRequiereNumeracion=false, momentoNumeracionDocu debe ser NO_APLICA. Valor recibido: "
                            + momento);
        }
        if (siRequiereNumeracion && momento == MomentoNumeracionDocu.NO_APLICA) {
            throw new DocumentoPlantillaInvalidaException(
                    "Si siRequiereNumeracion=true, momentoNumeracionDocu no puede ser NO_APLICA.");
        }
    }
}
