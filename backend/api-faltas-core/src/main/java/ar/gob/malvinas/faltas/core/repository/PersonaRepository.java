package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.enums.TipoDocumentoPersona;
import ar.gob.malvinas.faltas.core.domain.model.FalPersona;

import java.util.List;
import java.util.Optional;

/**
 * Port de acceso a personas del sistema de faltas.
 * No elimina fisicamente personas.
 */
public interface PersonaRepository {

    Long nextId();

    FalPersona guardar(FalPersona persona);

    Optional<FalPersona> buscarPorId(Long id);

    List<FalPersona> listarTodas();

    List<FalPersona> buscarPorTipoDocYNroDoc(TipoDocumentoPersona tipoDoc, String nroDoc);

    List<FalPersona> buscarPorIdSujBie(Long idSuj, Long idBie);
}
