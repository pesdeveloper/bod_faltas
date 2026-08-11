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

    /**
     * Idempotente desde Wave 4 (alta integral JDBC): antes de esta wave, el seeder solo corria
     * contra {@code InMemoryVehiculoMarcaRepository} (reiniciado por cada contexto de test), de
     * modo que reejecutarlo nunca chocaba con una marca ya existente. Con
     * {@code JdbcVehiculoMarcaRepository} activo bajo el perfil {@code jdbc}, la persistencia es
     * real y sobrevive entre recargas del contexto de Spring dentro de la misma corrida de
     * Failsafe: sin este guard, la segunda carga del contexto fallaba con
     * "Ya existe una marca con codigo: FIAT". Se aplica {@code altaMarcaSiNoExiste}/
     * {@code altaModeloSiNoExiste} en lugar de tocar {@code VehiculoMarcaService.altaMarca}, que
     * debe seguir rechazando duplicados reales.
     */
    private void seedMarcasModelos() {
        // Marcas activas
        FalVehiculoMarca fiat = altaMarcaSiNoExiste("FIAT", "Fiat");
        altaModeloSiNoExiste(fiat.getId(), "CRONOS", "Cronos");
        altaModeloSiNoExiste(fiat.getId(), "PALIO", "Palio");
        altaModeloSiNoExiste(fiat.getId(), "SIENA", "Siena");

        FalVehiculoMarca ford = altaMarcaSiNoExiste("FORD", "Ford");
        altaModeloSiNoExiste(ford.getId(), "FIESTA", "Fiesta");
        altaModeloSiNoExiste(ford.getId(), "FOCUS", "Focus");
        altaModeloSiNoExiste(ford.getId(), "RANGER", "Ranger");

        FalVehiculoMarca toyota = altaMarcaSiNoExiste("TOYOTA", "Toyota");
        altaModeloSiNoExiste(toyota.getId(), "COROLLA", "Corolla");
        altaModeloSiNoExiste(toyota.getId(), "HILUX", "Hilux");

        FalVehiculoMarca honda = altaMarcaSiNoExiste("HONDA", "Honda");
        altaModeloSiNoExiste(honda.getId(), "CIVIC", "Civic");
        altaModeloSiNoExiste(honda.getId(), "CR-V", "CR-V");

        // Marca inactiva (para test de referencia historica)
        FalVehiculoMarca renault = altaMarcaSiNoExiste("RENAULT", "Renault");
        altaModeloSiNoExiste(renault.getId(), "LAGUNA", "Laguna");
        if (renault.isSiActivo()) {
            marcaService.desactivar(renault.getId(), "SEEDER");
        }
    }

    private FalVehiculoMarca altaMarcaSiNoExiste(String codigo, String nombre) {
        return marcaRepository.findByCodigo(codigo)
                .orElseGet(() -> marcaService.altaMarca(codigo, nombre, "SEEDER"));
    }

    private void altaModeloSiNoExiste(Long marcaId, String codigo, String nombre) {
        if (modeloService.findByMarcaAndCodigo(marcaId, codigo).isEmpty()) {
            modeloService.altaModelo(marcaId, codigo, nombre, "SEEDER");
        }
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
