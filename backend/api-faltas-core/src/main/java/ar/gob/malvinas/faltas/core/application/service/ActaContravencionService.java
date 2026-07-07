package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.exception.ActaContravencionNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaContravencion;
import ar.gob.malvinas.faltas.core.domain.model.FalRubroVersion;
import ar.gob.malvinas.faltas.core.repository.ActaContravencionRepository;
import ar.gob.malvinas.faltas.core.repository.RubroVersionRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ActaContravencionService {

    private final ActaContravencionRepository contravencionRepository;
    private final RubroVersionRepository rubroRepository;

    public ActaContravencionService(ActaContravencionRepository contravencionRepository,
                                    RubroVersionRepository rubroRepository) {
        this.contravencionRepository = contravencionRepository;
        this.rubroRepository = rubroRepository;
    }

    public FalActaContravencion registrar(Long actaId, FalActaContravencion contravencion) {
        if (contravencionRepository.existsByActaId(actaId))
            throw new IllegalStateException("Ya existe contravencion para actaId=" + actaId);
        validarRubro(contravencion);
        return contravencionRepository.guardar(contravencion);
    }

    public FalActaContravencion findByActaId(Long actaId) {
        return contravencionRepository.findByActaId(actaId)
                .orElseThrow(() -> new ActaContravencionNoEncontradaException(actaId));
    }

    public Optional<FalActaContravencion> findByActaIdOpt(Long actaId) {
        return contravencionRepository.findByActaId(actaId);
    }

    private void validarRubro(FalActaContravencion ctv) {
        if (ctv.getRubroId() != null && ctv.getIdRub() != null) {
            FalRubroVersion version = rubroRepository.findByRubroId(ctv.getRubroId())
                    .orElseThrow(() -> new ar.gob.malvinas.faltas.core.domain.exception
                            .RubroVersionNoEncontradoException(ctv.getRubroId()));
            if (version.getIdRub() != ctv.getIdRub())
                throw new IllegalArgumentException(
                        "rubroId=" + ctv.getRubroId() + " no corresponde a Id_Rub=" + ctv.getIdRub());
        }
    }
}
