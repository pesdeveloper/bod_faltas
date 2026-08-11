package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActaEvidencia;

import java.util.List;
import java.util.Optional;

public interface ActaEvidenciaRepository {

    FalActaEvidencia guardar(FalActaEvidencia evidencia);

    List<FalActaEvidencia> listarPorActa(Long idActa);

    Long nextId();

    /**
     * Fotografias de la acta ({@code tipo_evid = FOTO}), ordenadas por
     * {@code orden} ascendente (Wave 4 Parte 3D). Distinto de
     * {@link #listarPorActa} porque ese metodo devuelve TODAS las evidencias
     * (cualquier tipo) en orden de alta, no solo fotografias ordenadas por
     * {@code orden} de negocio.
     */
    List<FalActaEvidencia> listarFotografiasPorActa(Long idActa);

    /**
     * Busca una fotografia puntual de la acta por id, solo si
     * {@code tipo_evid = FOTO}. Usado por el endpoint de descarga
     * (Parte 3D S14): nunca resuelve una evidencia no-FOTO por esta via.
     */
    Optional<FalActaEvidencia> buscarFotografiaPorActaYId(Long idActa, Long idEvidencia);

    /**
     * {@code MAX(orden)} entre las fotografias ya persistidas de la acta, o
     * {@code 0} si no tiene ninguna. Usado para calcular
     * {@code nuevoPrimerOrden = MAX(orden) + 1} en la carga posterior
     * (Parte 3D S5.6). El llamador es responsable de la exclusion mutua
     * (bloqueo de la fila del acta) antes de invocar este metodo: el metodo en
     * si no bloquea nada.
     */
    int maxOrdenFotografiasPorActa(Long idActa);

    /**
     * {@code true} si ya existe una fotografia con ese SHA-256 en la acta
     * (Parte 3D S5.10: duplicados dentro de la misma acta, nunca entre actas
     * distintas). El {@code UNIQUE(id_acta, hash_evid)} real es la autoridad
     * final ante concurrencia; esta consulta es la validacion temprana que
     * produce un rechazo funcional legible antes de tocar el motor.
     */
    boolean existeFotografiaConHash(Long idActa, String sha256Hex);
}
