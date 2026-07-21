package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.exception.PlantillaDefaultAmbiguaException;
import ar.gob.malvinas.faltas.core.domain.exception.PlantillaDefaultNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantillaDefault;
import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaDefaultRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio de resolucion de plantillas operativas por defecto.
 *
 * Algoritmo:
 *   1. buscarDefaultsVigentes para la accion.
 *   2. Ordenar por prioridad DESC.
 *   3. Verificar que no haya empate en la prioridad maxima.
 *   4. Retornar el de mayor prioridad.
 *
 * Slice 8F-1.
 */
@Service
public class DocumentoPlantillaDefaultService {

    private final DocumentoPlantillaDefaultRepository repository;

    public DocumentoPlantillaDefaultService(DocumentoPlantillaDefaultRepository repository) {
        this.repository = repository;
    }

    public FalDocumentoPlantillaDefault registrar(FalDocumentoPlantillaDefault d) {
        return repository.guardar(d);
    }

    /**
     * Resuelve el default mas especifico para el contexto dado.
     *
     * @throws PlantillaDefaultNoEncontradaException si no hay ninguno aplicable
     * @throws PlantillaDefaultAmbiguaException       si hay empate de prioridad
     */
    public FalDocumentoPlantillaDefault resolverDefault(
            AccionDocumental accionDocumental,
            TipoActa tipoActa,
            Long idDependencia,
            LocalDateTime en) {
        if (accionDocumental == null) throw new IllegalArgumentException("accionDocumental requerido");

        List<FalDocumentoPlantillaDefault> candidatos =
                repository.buscarDefaultsVigentes(accionDocumental, tipoActa, idDependencia, en);

        if (candidatos.isEmpty()) {
            throw new PlantillaDefaultNoEncontradaException(
                    "No hay plantilla default activa y vigente para accion=" + accionDocumental
                            + ", tipoActa=" + tipoActa + ", idDependencia=" + idDependencia);
        }

        int maxPrioridad = candidatos.stream()
                .mapToInt(FalDocumentoPlantillaDefault::getPrioridad)
                .max()
                .orElseThrow();

        List<FalDocumentoPlantillaDefault> maximos = candidatos.stream()
                .filter(d -> d.getPrioridad() == maxPrioridad)
                .toList();

        if (maximos.size() > 1) {
            throw new PlantillaDefaultAmbiguaException(
                    "Ambiguedad: " + maximos.size() + " defaults con prioridad=" + maxPrioridad
                            + " para accion=" + accionDocumental);
        }

        return maximos.get(0);
    }

    public List<FalDocumentoPlantillaDefault> listar() {
        return repository.listar();
    }
}
