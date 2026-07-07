package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.ActorTipoEvento;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Helper de cierre diferido (Slice 7C).
 *
 * Centraliza la logica de evaluacion y emision de cierre para reutilizarla
 * en BloqueanteMaterialService cuando se resuelve el ultimo bloqueante activo.
 *
 * Responsabilidades:
 *   - esResultadoCerrable(): evalua si el resultado final del acta habilita el cierre.
 *   - yaTieneCierre(): evita emitir CIERRA duplicado si ya fue registrado.
 *   - emitirCierre(): cambia estado del acta, registra evento CIERRA y recalcula snapshot.
 *
 * No reemplaza la logica de cierre inmediato en PagoCondenaService, PagoVoluntarioService,
 * NotificacionService ni GestionExternaService. Solo agrega el camino diferido.
 *
 * Cerrables reconocidos:
 *   PAGO_VOLUNTARIO_PAGADO, ABSUELTO, CONDENA_FIRME_PAGADA.
 *
 * Slice 9: reemplazable por implementacion JDBC sin tocar dominio.
 */
@Component
public class CierreActaHelper {

    private final ActaRepository actaRepository;
    private final ActaEventoRepository eventoRepository;
    private final ActaSnapshotRepository snapshotRepository;
    private final SnapshotRecalculador snapshotRecalculador;

    public CierreActaHelper(
            ActaRepository actaRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            SnapshotRecalculador snapshotRecalculador) {
        this.actaRepository = actaRepository;
        this.eventoRepository = eventoRepository;
        this.snapshotRepository = snapshotRepository;
        this.snapshotRecalculador = snapshotRecalculador;
    }

    /**
     * Retorna true si el resultado final del acta es cerrable de forma diferida.
     *
     * Solo los resultados que ya implican conclusion juridica y pago/absolucion
     * habilitan el cierre diferido. CONDENA_FIRME no es cerrable por si solo:
     * requiere pago o gestion externa adicional.
     */
    public boolean esResultadoCerrable(ResultadoFinalActa resultado) {
        return resultado == ResultadoFinalActa.PAGO_VOLUNTARIO_PAGADO
                || resultado == ResultadoFinalActa.ABSUELTO
                || resultado == ResultadoFinalActa.CONDENA_FIRME_PAGADA;
    }

    /**
     * Retorna true si ya existe un evento CIERRA registrado para el acta.
     * Previene emision duplicada de CIERRA en cierre diferido.
     */
    public boolean yaTieneCierre(Long actaId) {
        return eventoRepository.buscarPorActa(actaId).stream()
                .anyMatch(e -> e.tipoEvt() == TipoEventoActa.CIERRA);
    }

    /**
     * Emite el cierre del acta: cambia estado a CERRADA/CERR, registra evento CIERRA
     * y recalcula snapshot. Solo llamar si todas las precondiciones ya fueron verificadas.
     */
    public void emitirCierre(FalActa acta, String motivo) {
        acta.setSituacionAdministrativa(SituacionAdministrativaActa.CERRADA);
        acta.setBloqueActual(BloqueActual.CERR);
        actaRepository.guardar(acta);

        FalActaEvento evento = FalActaEvento.builder()
                .actaId(acta.getId())
                .tipoEvt(TipoEventoActa.CIERRA)
                .origenEvt(OrigenEvento.PROCESO_AUTOMATICO)
                .fhEvt(LocalDateTime.now())
                .actorTipo(ActorTipoEvento.SISTEMA)
                .siEvtCierre(true)
                .descripcionLegible(motivo)
                .build();
        eventoRepository.registrar(evento);

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);
    }
}
