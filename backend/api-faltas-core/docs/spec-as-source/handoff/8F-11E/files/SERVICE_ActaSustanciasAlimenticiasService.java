package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.exception.ActaSustanciasNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.RubroVersionNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSustanciasAlimenticias;
import ar.gob.malvinas.faltas.core.domain.model.FalRubroVersion;
import ar.gob.malvinas.faltas.core.repository.ActaSustanciasAlimenticiasRepository;
import ar.gob.malvinas.faltas.core.repository.RubroVersionRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ActaSustanciasAlimenticiasService {

    private final ActaSustanciasAlimenticiasRepository sustanciasRepository;
    private final RubroVersionRepository rubroRepository;

    public ActaSustanciasAlimenticiasService(ActaSustanciasAlimenticiasRepository sustanciasRepository,
                                             RubroVersionRepository rubroRepository) {
        this.sustanciasRepository = sustanciasRepository;
        this.rubroRepository = rubroRepository;
    }

    public FalActaSustanciasAlimenticias registrar(Long actaId, FalActaSustanciasAlimenticias sustancias) {
        if (sustanciasRepository.existsByActaId(actaId))
            throw new IllegalStateException("Ya existe registro de sustancias para actaId=" + actaId);
        validarRubro(sustancias);
        return sustanciasRepository.guardar(sustancias);
    }

    public FalActaSustanciasAlimenticias findByActaId(Long actaId) {
        return sustanciasRepository.findByActaId(actaId)
                .orElseThrow(() -> new ActaSustanciasNoEncontradaException(actaId));
    }

    public Optional<FalActaSustanciasAlimenticias> findByActaIdOpt(Long actaId) {
        return sustanciasRepository.findByActaId(actaId);
    }

    private void validarRubro(FalActaSustanciasAlimenticias s) {
        if (s.getRubroId() != null && s.getIdRub() != null) {
            FalRubroVersion version = rubroRepository.findByRubroId(s.getRubroId())
                    .orElseThrow(() -> new RubroVersionNoEncontradoException(s.getRubroId()));
            if (version.getIdRub() != s.getIdRub())
                throw new IllegalArgumentException(
                        "rubroId=" + s.getRubroId() + " no corresponde a Id_Rub=" + s.getIdRub());
        }
    }
}
