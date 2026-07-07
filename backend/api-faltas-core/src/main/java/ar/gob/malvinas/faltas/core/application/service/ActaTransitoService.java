package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.TipoPruebaAlcoholemia;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoCualitativoAlcoholemia;
import ar.gob.malvinas.faltas.core.domain.enums.UnidadMedidaAlcoholemia;
import ar.gob.malvinas.faltas.core.domain.enums.UnidadTerritorialTipo;
import ar.gob.malvinas.faltas.core.domain.exception.ActaTransitoAlcoholemiaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.ActaTransitoNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaTransito;
import ar.gob.malvinas.faltas.core.domain.model.FalActaTransitoAlcoholemia;
import ar.gob.malvinas.faltas.core.repository.ActaTransitoAlcoholemiaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaTransitoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio de dominio para satelite fal_acta_transito y fal_acta_transito_alcoholemia.
 * Garantiza reglas de integracion entre el satelite de transito y sus mediciones.
 */
@Service
public class ActaTransitoService {

    private final ActaTransitoRepository transitoRepository;
    private final ActaTransitoAlcoholemiaRepository alcoholemiaRepository;

    public ActaTransitoService(ActaTransitoRepository transitoRepository,
                               ActaTransitoAlcoholemiaRepository alcoholemiaRepository) {
        this.transitoRepository = transitoRepository;
        this.alcoholemiaRepository = alcoholemiaRepository;
    }

    public FalActaTransito registrarTransito(Long actaId) {
        if (transitoRepository.existsByActaId(actaId))
            throw new IllegalStateException("Ya existe satelite de transito para actaId=" + actaId);
        FalActaTransito t = new FalActaTransito(actaId);
        return transitoRepository.guardar(t);
    }

    public FalActaTransito findByActaId(Long actaId) {
        return transitoRepository.findByActaId(actaId)
                .orElseThrow(() -> new ActaTransitoNoEncontradaException(actaId));
    }

    public FalActaTransito actualizarTransito(Long actaId, FalActaTransito updated) {
        findByActaId(actaId); // verifica existencia
        return transitoRepository.guardar(updated);
    }

    /**
     * Agrega una medicion de alcoholemia. orden_medicion debe ser unico por acta.
     */
    public FalActaTransitoAlcoholemia agregarMedicion(
            Long actaId,
            short ordenMedicion,
            TipoPruebaAlcoholemia tipoPrueba,
            ResultadoCualitativoAlcoholemia resultadoCualitativo,
            BigDecimal resultadoNumerico,
            UnidadMedidaAlcoholemia unidadMedida,
            Long idAlcoholimetro,
            Short verAlcoholimetro,
            LocalDateTime fhMedicion,
            String idUserAlta) {
        findByActaId(actaId); // satelite debe existir
        if (alcoholemiaRepository.existsOrdenByActaId(actaId, ordenMedicion))
            throw new IllegalStateException("Ya existe medicion con orden " + ordenMedicion + " para actaId=" + actaId);
        Long id = alcoholemiaRepository.nextId();
        FalActaTransitoAlcoholemia medicion = new FalActaTransitoAlcoholemia(
                id, actaId, ordenMedicion, tipoPrueba, LocalDateTime.now(), idUserAlta);
        medicion.setResultadoCualitativo(resultadoCualitativo);
        medicion.setResultadoNumerico(resultadoNumerico, unidadMedida);
        medicion.setAlcoholimetro(idAlcoholimetro, verAlcoholimetro);
        medicion.setFhMedicion(fhMedicion);
        return alcoholemiaRepository.guardar(medicion);
    }

    /**
     * Designa una medicion como resultado final de forma atomica.
     */
    public FalActaTransitoAlcoholemia marcarResultadoFinal(Long actaId, Long medicionId) {
        return alcoholemiaRepository.marcarResultadoFinalAtomicamente(actaId, medicionId);
    }

    public List<FalActaTransitoAlcoholemia> findMedicionesByActaId(Long actaId) {
        return alcoholemiaRepository.findByActaId(actaId);
    }

    /**
     * Verifica que si si_control_alcoholemia=true exista al menos una medicion.
     * Debe llamarse en confirmacion/cierre del labrado.
     */
    public void validarAlcoholemiaCompleta(Long actaId) {
        FalActaTransito t = findByActaId(actaId);
        if (t.isSiControlAlcoholemia()) {
            List<FalActaTransitoAlcoholemia> mediciones = alcoholemiaRepository.findByActaId(actaId);
            if (mediciones.isEmpty())
                throw new IllegalStateException(
                        "si_control_alcoholemia=true pero no hay mediciones registradas para actaId=" + actaId);
        }
    }
}
