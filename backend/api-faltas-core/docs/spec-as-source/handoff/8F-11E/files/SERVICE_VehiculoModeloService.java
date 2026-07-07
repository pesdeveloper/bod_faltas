package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.exception.VehiculoMarcaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.VehiculoModeloNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.model.FalVehiculoModelo;
import ar.gob.malvinas.faltas.core.repository.VehiculoMarcaRepository;
import ar.gob.malvinas.faltas.core.repository.VehiculoModeloRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class VehiculoModeloService {

    private final VehiculoModeloRepository modeloRepository;
    private final VehiculoMarcaRepository marcaRepository;

    public VehiculoModeloService(VehiculoModeloRepository modeloRepository, VehiculoMarcaRepository marcaRepository) {
        this.modeloRepository = modeloRepository;
        this.marcaRepository = marcaRepository;
    }

    /**
     * Alta de modelo. codigo y nombre deben ser unicos dentro de la marca.
     * La marca debe existir.
     */
    public synchronized FalVehiculoModelo altaModelo(
            Long marcaId,
            String codigo,
            String nombre,
            String idUserAlta) {
        marcaRepository.findById(marcaId)
                .orElseThrow(() -> new VehiculoMarcaNoEncontradaException(marcaId));
        String codigoNorm = (codigo != null) ? codigo.trim().toUpperCase() : null;
        String nombreTrim = (nombre != null) ? nombre.trim() : null;
        if (modeloRepository.findByMarcaAndCodigo(marcaId, codigoNorm).isPresent())
            throw new IllegalStateException("Ya existe un modelo con codigo " + codigoNorm + " para la marca " + marcaId);
        if (modeloRepository.findByMarcaAndNombre(marcaId, nombreTrim).isPresent())
            throw new IllegalStateException("Ya existe un modelo con nombre " + nombreTrim + " para la marca " + marcaId);
        Long id = modeloRepository.nextId();
        FalVehiculoModelo modelo = new FalVehiculoModelo(id, marcaId, codigo, nombre, LocalDateTime.now(), idUserAlta);
        return modeloRepository.guardar(modelo);
    }

    public FalVehiculoModelo findById(Long id) {
        return modeloRepository.findById(id)
                .orElseThrow(() -> new VehiculoModeloNoEncontradoException(id));
    }

    public Optional<FalVehiculoModelo> findByMarcaAndCodigo(Long marcaId, String codigo) {
        return modeloRepository.findByMarcaAndCodigo(marcaId, codigo);
    }

    public List<FalVehiculoModelo> findActivasByMarca(Long marcaId) {
        return modeloRepository.findActivasByMarca(marcaId);
    }

    /**
     * Valida que el modelo pertenezca a la marca informada.
     */
    public void validarModeloPerteneceMarca(Long modeloId, Long marcaId) {
        FalVehiculoModelo modelo = findById(modeloId);
        if (!modelo.getMarcaVehiculoId().equals(marcaId))
            throw new IllegalArgumentException("El modelo " + modeloId + " no pertenece a la marca " + marcaId);
    }

    public FalVehiculoModelo desactivar(Long id, String idUser) {
        FalVehiculoModelo modelo = findById(id);
        modelo.setSiActivo(false);
        return modeloRepository.guardar(modelo);
    }
}
