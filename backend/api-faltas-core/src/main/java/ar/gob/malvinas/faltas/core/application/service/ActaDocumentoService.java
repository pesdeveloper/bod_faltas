package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.RolDocuActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.ActaDocumentoId;
import ar.gob.malvinas.faltas.core.domain.model.FalActaDocumento;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.repository.ActaDocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRepository;
import org.springframework.stereotype.Service;

import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de la relacion canonica acta-documento.
 *
 * Toda asociacion de documento a expediente debe pasar por este servicio.
 * Garantiza:
 * - validacion de acta y documento existentes
 * - compatibilidad rol/tipo documental
 * - unicidad de principal por (acta, rol) cuando aplica
 * - operaciones atomicas de reemplazo de principal
 * - consultas canonicas de documentos por acta/rol
 */
@Service
public class ActaDocumentoService {

    private final ActaDocumentoRepository actaDocumentoRepository;
    private final ActaRepository actaRepository;
    private final DocumentoRepository documentoRepository;
    private final FaltasClock faltasClock;

    public ActaDocumentoService(
            ActaDocumentoRepository actaDocumentoRepository,
            ActaRepository actaRepository,
            DocumentoRepository documentoRepository,
            FaltasClock faltasClock) {
        this.actaDocumentoRepository = actaDocumentoRepository;
        this.actaRepository = actaRepository;
        this.documentoRepository = documentoRepository;
        this.faltasClock = faltasClock;
    }

    /**
     * Asocia un documento a un expediente con el rol indicado.
     * Si siPrincipal=true y el rol exige unicidad, desplaza el anterior principal.
     */
    public FalActaDocumento asociar(Long actaId, Long documentoId, RolDocuActa rol,
                                     boolean siPrincipal, String idUser) {
        validarActaExiste(actaId);
        FalDocumento doc = documentoRepository.buscarPorId(documentoId)
                .orElseThrow(() -> new DocumentoNoEncontradoException(documentoId));
        validarCompatibilidad(rol, doc.getTipoDocu());
        if (siPrincipal && !rol.admitePrincipal()) {
            throw new PrecondicionVioladaException(
                    "El rol " + rol + " no admite principalidad");
        }

        LocalDateTime ahora = faltasClock.now();
        if (siPrincipal) {
            return actaDocumentoRepository.asociarComoPrincipalAtomico(
                    actaId, documentoId, rol, idUser, ahora);
        }
        FalActaDocumento relacion = new FalActaDocumento(
                actaId, documentoId, rol, false, ahora, idUser);
        return actaDocumentoRepository.guardar(relacion);
    }

    /**
     * Asocia el documento como principal, desplazando el anterior si existe.
     */
    public FalActaDocumento asociarPrincipal(Long actaId, Long documentoId, RolDocuActa rol,
                                              String idUser) {
        validarActaExiste(actaId);
        FalDocumento doc = documentoRepository.buscarPorId(documentoId)
                .orElseThrow(() -> new DocumentoNoEncontradoException(documentoId));
        validarCompatibilidad(rol, doc.getTipoDocu());
        if (!rol.admitePrincipal()) {
            throw new PrecondicionVioladaException(
                    "El rol " + rol + " no admite principalidad");
        }
        return actaDocumentoRepository.asociarComoPrincipalAtomico(
                actaId, documentoId, rol, idUser, faltasClock.now());
    }

    /**
     * Reemplaza el documento principal actual del (acta, rol) por uno nuevo.
     * El anterior queda en el historial como no-principal.
     */
    public FalActaDocumento reemplazarPrincipal(Long actaId, Long documentoIdNuevo,
                                                 RolDocuActa rol, String idUser) {
        validarActaExiste(actaId);
        FalDocumento doc = documentoRepository.buscarPorId(documentoIdNuevo)
                .orElseThrow(() -> new DocumentoNoEncontradoException(documentoIdNuevo));
        validarCompatibilidad(rol, doc.getTipoDocu());
        if (!rol.admitePrincipal()) {
            throw new PrecondicionVioladaException(
                    "El rol " + rol + " no admite principalidad");
        }
        return actaDocumentoRepository.reemplazarPrincipalAtomico(
                actaId, documentoIdNuevo, rol, idUser, faltasClock.now());
    }

    public List<FalActaDocumento> consultarPorActa(Long actaId) {
        return actaDocumentoRepository.listarPorActa(actaId);
    }

    public List<FalActaDocumento> consultarPorActaYRol(Long actaId, RolDocuActa rol) {
        return actaDocumentoRepository.listarPorActaYRol(actaId, rol);
    }

    public List<FalActaDocumento> consultarPorDocumento(Long documentoId) {
        return actaDocumentoRepository.listarPorDocumento(documentoId);
    }

    public Optional<FalActaDocumento> consultarPrincipal(Long actaId, RolDocuActa rol) {
        return actaDocumentoRepository.buscarPrincipalPorActaYRol(actaId, rol);
    }

    public boolean perteneceAlActa(Long actaId, Long documentoId) {
        return actaDocumentoRepository.existe(actaId, documentoId);
    }

    public void validarPertenencia(Long actaId, Long documentoId) {
        if (!actaDocumentoRepository.existe(actaId, documentoId)) {
            throw new PrecondicionVioladaException(
                    "El documento " + documentoId + " no esta asociado al acta " + actaId);
        }
    }

    public void validarCompatibilidad(RolDocuActa rol, TipoDocu tipoDocu) {
        if (!rol.tiposPermitidos().contains(tipoDocu)) {
            throw new PrecondicionVioladaException(
                    "TipoDocu " + tipoDocu + " no es compatible con rol " + rol
                            + ". Tipos permitidos: " + rol.tiposPermitidos());
        }
    }

    /**
     * Resuelve el ultimo documento operativo relevante para el expediente.
     * Se usa para calcular idDocuUlt en el snapshot.
     * Prioridad: FALLO > RESOLUCION > NOTIFICACION > ACTA_PRINCIPAL.
     */
    public Optional<Long> resolverUltimoDocumentoOperativo(Long actaId) {
        for (RolDocuActa rol : List.of(
                RolDocuActa.FALLO,
                RolDocuActa.RESOLUCION,
                RolDocuActa.NOTIFICACION,
                RolDocuActa.ACTA_PRINCIPAL)) {
            Optional<FalActaDocumento> principal =
                    actaDocumentoRepository.buscarPrincipalPorActaYRol(actaId, rol);
            if (principal.isPresent()) {
                return Optional.of(principal.get().getDocumentoId());
            }
        }
        return Optional.empty();
    }

    private void validarActaExiste(Long actaId) {
        actaRepository.buscarPorId(actaId)
                .orElseThrow(() -> new ActaNoEncontradaException(actaId));
    }
}
