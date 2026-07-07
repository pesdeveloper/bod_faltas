package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.exception.ActaVehiculoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.VehiculoMarcaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaVehiculo;
import ar.gob.malvinas.faltas.core.domain.model.FalVehiculoMarca;
import ar.gob.malvinas.faltas.core.domain.model.FalVehiculoModelo;
import ar.gob.malvinas.faltas.core.repository.ActaVehiculoRepository;
import ar.gob.malvinas.faltas.core.repository.VehiculoMarcaRepository;
import ar.gob.malvinas.faltas.core.repository.VehiculoModeloRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ActaVehiculoService {

    private final ActaVehiculoRepository vehiculoRepository;
    private final VehiculoMarcaRepository marcaRepository;
    private final VehiculoModeloRepository modeloRepository;

    public ActaVehiculoService(ActaVehiculoRepository vehiculoRepository,
                               VehiculoMarcaRepository marcaRepository,
                               VehiculoModeloRepository modeloRepository) {
        this.vehiculoRepository = vehiculoRepository;
        this.marcaRepository = marcaRepository;
        this.modeloRepository = modeloRepository;
    }

    public FalActaVehiculo registrarVehiculo(Long actaId, FalActaVehiculo vehiculo) {
        if (vehiculoRepository.existsByActaId(actaId))
            throw new IllegalStateException("Ya existe vehiculo para actaId=" + actaId);
        // Validar que el modelo (si normalizado) pertenezca a la marca informada
        validarRelacionMarcaModelo(vehiculo);
        return vehiculoRepository.guardar(vehiculo);
    }

    public FalActaVehiculo findByActaId(Long actaId) {
        return vehiculoRepository.findByActaId(actaId)
                .orElseThrow(() -> new ActaVehiculoNoEncontradoException(actaId));
    }

    public Optional<FalActaVehiculo> findByActaIdOpt(Long actaId) {
        return vehiculoRepository.findByActaId(actaId);
    }

    private void validarRelacionMarcaModelo(FalActaVehiculo vehiculo) {
        if (vehiculo.getModeloVehiculoId() != null) {
            if (vehiculo.getMarcaVehiculoId() == null)
                throw new IllegalArgumentException("marcaVehiculoId es obligatorio cuando se informa modeloVehiculoId");
            FalVehiculoModelo modelo = modeloRepository.findById(vehiculo.getModeloVehiculoId())
                    .orElseThrow(() -> new ar.gob.malvinas.faltas.core.domain.exception
                            .VehiculoModeloNoEncontradoException(vehiculo.getModeloVehiculoId()));
            if (!modelo.getMarcaVehiculoId().equals(vehiculo.getMarcaVehiculoId()))
                throw new IllegalArgumentException(
                        "El modelo " + vehiculo.getModeloVehiculoId() +
                        " no pertenece a la marca " + vehiculo.getMarcaVehiculoId());
        }
        if (vehiculo.getMarcaVehiculoId() != null) {
            FalVehiculoMarca marca = marcaRepository.findById(vehiculo.getMarcaVehiculoId())
                    .orElseThrow(() -> new VehiculoMarcaNoEncontradaException(vehiculo.getMarcaVehiculoId()));
            if (!marca.isSiActivo() && vehiculo.getModeloVehiculoId() == null) {
                // Marca inactiva puede usarse historicamente pero no como seleccion nueva sin modelo
            }
        }
    }
}
