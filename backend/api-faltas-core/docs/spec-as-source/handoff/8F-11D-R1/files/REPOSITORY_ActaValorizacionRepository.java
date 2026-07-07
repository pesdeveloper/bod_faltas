package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.enums.TipoValorizacionActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaValorizacion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Port de acceso a valorizaciones de acta.
 * Optimistic locking real: save() compara versionRow.
 * Solo una vigente CONFIRMADA por acta+tipo en todo momento.
 */
public interface ActaValorizacionRepository {

    Long nextId();

    /**
     * Alta si no existe; update con control optimista si ya existe.
     * Guarda no puede:
     *   - establecer siVigente=true con estado != CONFIRMADA;
     *   - introducir una segunda vigente para el mismo acta+tipo.
     * Para transicion atomica PRELIMINAR->CONFIRMADA usar confirmarVigenteAtomico.
     */
    FalActaValorizacion save(FalActaValorizacion valorizacion);

    Optional<FalActaValorizacion> findById(Long id);

    List<FalActaValorizacion> findByActaId(Long actaId);

    List<FalActaValorizacion> findByActaIdAndTipo(Long actaId, TipoValorizacionActa tipo);

    /**
     * Devuelve la vigente CONFIRMADA para el acta+tipo, si existe.
     * Falla con IllegalStateException si por corrupcion hubiera mas de una.
     */
    Optional<FalActaValorizacion> findVigenteByActaIdAndTipo(Long actaId, TipoValorizacionActa tipo);

    List<FalActaValorizacion> findAll();

    /**
     * Confirma y activa una valorizacion candidata de forma atomica,
     * garantizando exactamente una vigente por acta+tipo al finalizar.
     *
     * Lanza ConcurrenciaConflictoException si:
     *   - la candidata cambio (version != esperada);
     *   - la vigente anterior cambio (version != esperada o ID distinto);
     *   - si no se esperaba vigente previa pero ya hay una (otro confirmo concurrentemente).
     *
     * @param candidataId                   ID de la valorizacion PRELIMINAR a confirmar
     * @param versionRowCandidataEsperada   version esperada de la candidata (OCC)
     * @param vigenteAnteriorId             ID de la vigente que se reemplaza, null si no habia vigente
     * @param versionRowVigenteEsperada     version esperada de la vigente anterior, null si no habia
     * @param fhConfirmacion                timestamp de confirmacion
     * @param idUserConfirmacion            usuario confirmador
     */
    FalActaValorizacion confirmarVigenteAtomico(
            Long candidataId,
            int versionRowCandidataEsperada,
            Long vigenteAnteriorId,
            Integer versionRowVigenteEsperada,
            LocalDateTime fhConfirmacion,
            String idUserConfirmacion);
}
