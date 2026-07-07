package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import org.springframework.stereotype.Service;

/**
 * Implementacion del checker de uso formal de domicilios.
 *
 * Actualmente verifica referencias en FalActa.idDomicilioInfractorAct y
 * FalActa.idDomicilioNotifAct.
 *
 * Slices futuros ampliaran este checker para notificaciones, documentos formales, etc.
 */
@Service
public class ActaRefPersonaDomicilioUsoChecker implements PersonaDomicilioUsoChecker {

    private final ActaRepository actaRepository;

    public ActaRefPersonaDomicilioUsoChecker(ActaRepository actaRepository) {
        this.actaRepository = actaRepository;
    }

    @Override
    public boolean estaUsadoFormalmente(Long domicilioId) {
        if (domicilioId == null) return false;
        return actaRepository.listarTodas().stream()
                .anyMatch(a -> domicilioId.equals(a.getIdDomicilioInfractorAct())
                        || domicilioId.equals(a.getIdDomicilioNotifAct()));
    }
}
