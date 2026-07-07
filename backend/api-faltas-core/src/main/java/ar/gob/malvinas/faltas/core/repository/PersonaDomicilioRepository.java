package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.enums.TipoDomicilio;
import ar.gob.malvinas.faltas.core.domain.model.FalPersonaDomicilio;

import java.util.List;
import java.util.Optional;

/**
 * Port de acceso a domicilios de personas.
 * No elimina fisicamente: baja logica via siActivo=false.
 */
public interface PersonaDomicilioRepository {

    Long nextId();

    FalPersonaDomicilio guardar(FalPersonaDomicilio domicilio);

    Optional<FalPersonaDomicilio> buscarPorId(Long id);

    List<FalPersonaDomicilio> buscarPorPersonaId(Long personaId);

    List<FalPersonaDomicilio> buscarActivosPorPersonaId(Long personaId);

    List<FalPersonaDomicilio> buscarNotificablesPorPersonaId(Long personaId);

    Optional<FalPersonaDomicilio> buscarPrincipalActivo(Long personaId, TipoDomicilio tipoDomicilio);
}
