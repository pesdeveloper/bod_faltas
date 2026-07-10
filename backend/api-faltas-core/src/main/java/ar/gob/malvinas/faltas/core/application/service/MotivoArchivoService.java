package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.exception.MotivoArchivoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalMotivoArchivo;
import ar.gob.malvinas.faltas.core.repository.MotivoArchivoRepository;
import org.springframework.stereotype.Service;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de administracion del catalogo de motivos de archivo.
 *
 * El catalogo es abierto (no enum cerrado).
 * La unicidad de codMotivoArchivo es concurrente.
 * Motivos inactivos no son seleccionables para nuevos archivos.
 */
@Service
public class MotivoArchivoService {

    private final MotivoArchivoRepository repo;
    private final FaltasClock faltasClock;

    public MotivoArchivoService(MotivoArchivoRepository repo,
            FaltasClock faltasClock) {
        this.faltasClock = faltasClock;
        this.repo = repo;
    }

    public FalMotivoArchivo crear(String codigo, String nombre, String descripcion,
                                   boolean siNulidad, boolean siPermiteReingreso,
                                   boolean siRequiereObservacion, String idUserAlta) {
        Long id = repo.nextId();
        FalMotivoArchivo motivo = new FalMotivoArchivo(id, codigo, nombre, descripcion,
                siNulidad, siPermiteReingreso, siRequiereObservacion, true,
                faltasClock.now(), idUserAlta);
        return repo.guardar(motivo);
    }

    public FalMotivoArchivo buscarActivoPorId(Long id) {
        FalMotivoArchivo motivo = repo.buscarPorId(id)
                .orElseThrow(() -> new MotivoArchivoNoEncontradoException(id));
        if (!motivo.isSiActivo()) {
            throw new PrecondicionVioladaException(
                    "El motivo de archivo esta inactivo y no puede usarse en nuevos archivos: " + id);
        }
        return motivo;
    }

    public FalMotivoArchivo buscarPorId(Long id) {
        return repo.buscarPorId(id).orElseThrow(() -> new MotivoArchivoNoEncontradoException(id));
    }

    public Optional<FalMotivoArchivo> buscarPorCodigo(String codigo) {
        return repo.buscarPorCodigo(codigo);
    }

    public List<FalMotivoArchivo> listarActivos() {
        return repo.listarActivos();
    }

    public List<FalMotivoArchivo> listarTodos() {
        return repo.listarTodos();
    }

    public FalMotivoArchivo darDeBaja(Long id, String idUserMod) {
        FalMotivoArchivo motivo = repo.buscarPorId(id)
                .orElseThrow(() -> new MotivoArchivoNoEncontradoException(id));
        motivo.setSiActivo(false);
        motivo.setFhUltMod(faltasClock.now());
        motivo.setIdUserUltMod(idUserMod);
        return repo.actualizarAtomicamente(motivo);
    }

    public FalMotivoArchivo actualizar(Long id, String nombre, String descripcion,
                                        boolean siNulidad, boolean siPermiteReingreso,
                                        boolean siRequiereObservacion, String idUserMod) {
        FalMotivoArchivo motivo = repo.buscarPorId(id)
                .orElseThrow(() -> new MotivoArchivoNoEncontradoException(id));
        motivo.setNombre(nombre);
        motivo.setDescripcion(descripcion);
        motivo.setSiNulidad(siNulidad);
        motivo.setSiPermiteReingreso(siPermiteReingreso);
        motivo.setSiRequiereObservacion(siRequiereObservacion);
        motivo.setFhUltMod(faltasClock.now());
        motivo.setIdUserUltMod(idUserMod);
        return repo.actualizarAtomicamente(motivo);
    }
}
