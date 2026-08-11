package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Frontera transaccional de un único intento de alta (Wave 4 Parte 3E-B §3.4, §10).
 *
 * <p>Endurece la garantía de "transacción nueva por intento" del retry
 * ({@link ActaAltaTransientRetryService}) para que no dependa de que el llamador esté libre de
 * transacción ambiente. Hasta Parte 3E-B, esa garantía surgía únicamente de que el orquestador
 * no llevaba {@code @Transactional} propia: si {@code ActaService.labrar} (propagación
 * {@code REQUIRED}, sin cambios en esta parte) se invocaba desde una transacción ya abierta,
 * habría <b>reutilizado</b> esa transacción ambiente en los tres intentos posibles, en vez de
 * abrir una nueva por intento.
 *
 * <p>Este bean es la pieza intermedia: {@code @Transactional(REQUIRES_NEW)} en
 * {@link #intentar(LabrarActaCommand)} suspende cualquier transacción ambiente (si la hay) y abre
 * una nueva antes de que {@code ActaService.labrar} la herede por {@code REQUIRED}. Al ser un
 * bean Spring distinto de {@link ActaAltaTransientRetryService} y de {@link ActaService}, el
 * proxy transaccional de Spring intercepta la llamada en los dos saltos (no hay
 * self-invocation): la garantía es efectiva sin importar desde dónde se invoque el orquestador.
 */
@Service
public class ActaAltaIntentoTransaccionalService {

    private final ActaService actaService;

    public ActaAltaIntentoTransaccionalService(ActaService actaService) {
        this.actaService = actaService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ComandoResultado intentar(LabrarActaCommand comando) {
        return actaService.labrar(comando);
    }
}
