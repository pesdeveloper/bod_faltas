package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.OrigenUltimaActualizacion;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEconomiaProyeccion;
import ar.gob.malvinas.faltas.core.domain.model.FalActaObligacionPago;
import ar.gob.malvinas.faltas.core.repository.ObligacionPagoRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProcesoNocturnoEconomicoService {

    private final ObligacionPagoRepository obligacionRepo;
    private final EconomiaProyeccionRecalculador recalculador;

    public ProcesoNocturnoEconomicoService(
            ObligacionPagoRepository obligacionRepo,
            EconomiaProyeccionRecalculador recalculador) {
        this.obligacionRepo = obligacionRepo;
        this.recalculador = recalculador;
    }

    public List<FalActaEconomiaProyeccion> ejecutarSincronizacionNocturna() {
        List<FalActaEconomiaProyeccion> resultados = new ArrayList<>();
        for (FalActaObligacionPago obl : obligacionRepo.findAllVigentes()) {
            FalActaEconomiaProyeccion p = recalculador.recalcular(
                    obl.getActaId(), OrigenUltimaActualizacion.SINCRONIZACION_NOCTURNA, "PROCESO_NOCTURNO");
            if (p != null) resultados.add(p);
        }
        return resultados;
    }
}
