package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.exception.VehiculoMarcaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.model.FalVehiculoMarca;
import ar.gob.malvinas.faltas.core.repository.VehiculoMarcaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de catalogo de marcas de vehiculo.
 * Garantiza unicidad de codigo y nombre antes del alta.
 * La verificacion de unicidad se hace dentro de un bloque sincronizado via el repo.
 */
@Service
public class VehiculoMarcaService {

    private final VehiculoMarcaRepository marcaRepository;

    public VehiculoMarcaService(VehiculoMarcaRepository marcaRepository) {
        this.marcaRepository = marcaRepository;
    }

    /**
     * Alta de marca. codigo y nombre deben ser unicos.
     */
    public synchronized FalVehiculoMarca altaMarca(
            String codigo,
            String nombre,
            String idUserAlta) {
        String codigoNorm = (codigo != null) ? codigo.trim().toUpperCase() : null;
        String nombreTrim = (nombre != null) ? nombre.trim() : null;
        if (marcaRepository.findByCodigo(codigoNorm).isPresent())
            throw new IllegalStateException("Ya existe una marca con codigo: " + codigoNorm);
        if (marcaRepository.findByNombre(nombreTrim).isPresent())
            throw new IllegalStateException("Ya existe una marca con nombre: " + nombreTrim);
        Long id = marcaRepository.nextId();
        FalVehiculoMarca marca = new FalVehiculoMarca(id, codigo, nombre, LocalDateTime.now(), idUserAlta);
        return marcaRepository.guardar(marca);
    }

    public FalVehiculoMarca findById(Long id) {
        return marcaRepository.findById(id)
                .orElseThrow(() -> new VehiculoMarcaNoEncontradaException(id));
    }

    public Optional<FalVehiculoMarca> findByCodigo(String codigo) {
        return marcaRepository.findByCodigo(codigo);
    }

    public List<FalVehiculoMarca> findAllActivas() {
        return marcaRepository.findAllActivas();
    }

    public FalVehiculoMarca desactivar(Long id, String idUser) {
        FalVehiculoMarca marca = findById(id);
        marca.setSiActivo(false);
        return marcaRepository.guardar(marca);
    }
}
