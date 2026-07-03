package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActaEvidencia;

import java.util.List;

public interface ActaEvidenciaRepository {

    FalActaEvidencia guardar(FalActaEvidencia evidencia);

    List<FalActaEvidencia> listarPorActa(Long idActa);

    Long nextId();
}
