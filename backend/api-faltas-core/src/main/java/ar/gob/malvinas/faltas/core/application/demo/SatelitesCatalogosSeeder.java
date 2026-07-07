package ar.gob.malvinas.faltas.core.application.demo;

import ar.gob.malvinas.faltas.core.domain.enums.AmbitoCtv;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenNomenclatura;
import ar.gob.malvinas.faltas.core.domain.enums.TipoVehiculo;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoGeneralVehiculo;
import ar.gob.malvinas.faltas.core.domain.model.FalVehiculoMarca;
import ar.gob.malvinas.faltas.core.domain.model.FalVehiculoModelo;
import ar.gob.malvinas.faltas.core.domain.model.FalRubroVersion;
import ar.gob.malvinas.faltas.core.repository.RubroVersionRepository;
import ar.gob.malvinas.faltas.core.repository.VehiculoMarcaRepository;
import ar.gob.malvinas.faltas.core.repository.VehiculoModeloRepository;
import ar.gob.malvinas.faltas.core.application.service.RubroVersionService;
import ar.gob.malvinas.faltas.core.application.service.VehiculoMarcaService;
import ar.gob.malvinas.faltas.core.application.service.VehiculoModeloService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Seeder de datos funcionales para satelites de acta y catalogos de vehiculo/rubro.
 * Se ejecuta al inicio de la aplicacion demo.
 *
 * Datos minimos:
 * - Marcas y modelos de vehiculo
 * - Rubros versionados (activos e inactivos)
 */
@Component
public class SatelitesCatalogosSeeder {

    private final VehiculoMarcaService marcaService;
    private final VehiculoModeloService modeloService;
    private final RubroVersionService rubroService;
    private final VehiculoMarcaRepository marcaRepository;

    public SatelitesCatalogosSeeder(
            VehiculoMarcaService marcaService,
            VehiculoModeloService modeloService,
            RubroVersionService rubroService,
            VehiculoMarcaRepository marcaRepository) {
        this.marcaService = marcaService;
        this.modeloService = modeloService;
        this.rubroService = rubroService;
        this.marcaRepository = marcaRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        seedMarcasModelos();
        seedRubros();
    }

    private void seedMarcasModelos() {
        // Marcas activas
        FalVehiculoMarca fiat = marcaService.altaMarca("FIAT", "Fiat", "SEEDER");
        modeloService.altaModelo(fiat.getId(), "CRONOS", "Cronos", "SEEDER");
        modeloService.altaModelo(fiat.getId(), "PALIO", "Palio", "SEEDER");
        modeloService.altaModelo(fiat.getId(), "SIENA", "Siena", "SEEDER");

        FalVehiculoMarca ford = marcaService.altaMarca("FORD", "Ford", "SEEDER");
        modeloService.altaModelo(ford.getId(), "FIESTA", "Fiesta", "SEEDER");
        modeloService.altaModelo(ford.getId(), "FOCUS", "Focus", "SEEDER");
        modeloService.altaModelo(ford.getId(), "RANGER", "Ranger", "SEEDER");

        FalVehiculoMarca toyota = marcaService.altaMarca("TOYOTA", "Toyota", "SEEDER");
        modeloService.altaModelo(toyota.getId(), "COROLLA", "Corolla", "SEEDER");
        modeloService.altaModelo(toyota.getId(), "HILUX", "Hilux", "SEEDER");

        FalVehiculoMarca honda = marcaService.altaMarca("HONDA", "Honda", "SEEDER");
        modeloService.altaModelo(honda.getId(), "CIVIC", "Civic", "SEEDER");
        modeloService.altaModelo(honda.getId(), "CR-V", "CR-V", "SEEDER");

        // Marca inactiva (para test de referencia historica)
        FalVehiculoMarca renault = marcaService.altaMarca("RENAULT", "Renault", "SEEDER");
        modeloService.altaModelo(renault.getId(), "LAGUNA", "Laguna", "SEEDER");
        marcaService.desactivar(renault.getId(), "SEEDER");
    }

    private void seedRubros() {
        // Rubros activos
        rubroService.sincronizar(101, "Kiosco", (short) 0, "SEEDER");
        rubroService.sincronizar(102, "Panaderia", (short) 0, "SEEDER");
        rubroService.sincronizar(103, "Carniceria", (short) 0, "SEEDER");
        rubroService.sincronizar(201, "Deposito de mercaderias", (short) 0, "SEEDER");
        rubroService.sincronizar(301, "Restaurante", (short) 0, "SEEDER");

        // Rubro deshabilitado (sidesabilitado != 0)
        rubroService.sincronizar(999, "Actividad no habilitada", (short) 1, "SEEDER");
    }
}
