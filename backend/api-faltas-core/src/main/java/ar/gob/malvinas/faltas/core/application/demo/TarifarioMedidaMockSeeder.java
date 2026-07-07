package ar.gob.malvinas.faltas.core.application.demo;

import ar.gob.malvinas.faltas.core.domain.enums.OrigenBloqueanteMaterial;
import ar.gob.malvinas.faltas.core.domain.enums.TipoUnidadFaltas;
import ar.gob.malvinas.faltas.core.domain.model.ArticuloMedidaPreventivaId;
import ar.gob.malvinas.faltas.core.domain.model.FalArticuloMedidaPreventiva;
import ar.gob.malvinas.faltas.core.domain.model.FalMedidaPreventiva;
import ar.gob.malvinas.faltas.core.domain.model.FalTarifarioUnidadFaltas;
import ar.gob.malvinas.faltas.core.repository.ArticuloMedidaPreventivaRepository;
import ar.gob.malvinas.faltas.core.repository.MedidaPreventivaRepository;
import ar.gob.malvinas.faltas.core.repository.TarifarioUnidadFaltasRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryArticuloMedidaPreventivaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryMedidaPreventivaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryTarifarioUnidadFaltasRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seed de tarifarios, medidas preventivas y relaciones artículo-medida para demo/test.
 * Slice 8F-11D.
 *
 * Tarifarios (IDs 1-7):
 *   SALARIO histórico (1), SALARIO vigente (2)
 *   UNIDAD_FIJA histórico (3), UNIDAD_FIJA vigente (4)
 *   MONTO vigente (5)
 *   SALARIO vigente adicional sin superposición (solo para test, inactivo: 6)
 *
 * Medidas preventivas (IDs 1-7):
 *   RETLIC v1 activa, RETROD v1 activa, SECVEH v1 activa, CLAUSURA v1 activa, CLAUSURA v2 activa
 *
 * Nota: los artículos normativos están sembrados por el seeder de normativa; aquí
 * se establecen relaciones con IDs conocidos del seed normativa (IDs 1 y 2 del seed de artículos).
 */
@Component
public class TarifarioMedidaMockSeeder {

    static final LocalDateTime FH_ALTA = LocalDateTime.of(2024, 1, 1, 0, 0);
    static final String USER = "sistema";

    private final TarifarioUnidadFaltasRepository tarifarioRepo;
    private final MedidaPreventivaRepository medidaRepo;
    private final ArticuloMedidaPreventivaRepository articuloMedidaRepo;

    public TarifarioMedidaMockSeeder(
            TarifarioUnidadFaltasRepository tarifarioRepo,
            MedidaPreventivaRepository medidaRepo,
            ArticuloMedidaPreventivaRepository articuloMedidaRepo) {
        this.tarifarioRepo = tarifarioRepo;
        this.medidaRepo = medidaRepo;
        this.articuloMedidaRepo = articuloMedidaRepo;
    }

    @PostConstruct
    public void cargar() {
        cargarTarifarios();
        cargarMedidas();
        cargarRelaciones();
    }

    private void cargarTarifarios() {
        List<FalTarifarioUnidadFaltas> lista = new ArrayList<>();

        // SALARIO: histórico cerrado (2022)
        FalTarifarioUnidadFaltas t1 = new FalTarifarioUnidadFaltas(1L,
                TipoUnidadFaltas.SALARIO, new BigDecimal("85000.00"),
                LocalDate.of(2022, 1, 1), FH_ALTA, USER);
        t1.setFhVigHasta(LocalDate.of(2022, 12, 31));
        t1.setSiActiva(false);
        lista.add(t1);

        // SALARIO: vigente actual (2023-presente)
        FalTarifarioUnidadFaltas t2 = new FalTarifarioUnidadFaltas(2L,
                TipoUnidadFaltas.SALARIO, new BigDecimal("120000.00"),
                LocalDate.of(2023, 1, 1), FH_ALTA, USER);
        lista.add(t2);

        // UNIDAD_FIJA: histórico cerrado (2022)
        FalTarifarioUnidadFaltas t3 = new FalTarifarioUnidadFaltas(3L,
                TipoUnidadFaltas.UNIDAD_FIJA, new BigDecimal("5000.00"),
                LocalDate.of(2022, 1, 1), FH_ALTA, USER);
        t3.setFhVigHasta(LocalDate.of(2022, 12, 31));
        t3.setSiActiva(false);
        lista.add(t3);

        // UNIDAD_FIJA: vigente actual (2023-presente)
        FalTarifarioUnidadFaltas t4 = new FalTarifarioUnidadFaltas(4L,
                TipoUnidadFaltas.UNIDAD_FIJA, new BigDecimal("8000.00"),
                LocalDate.of(2023, 1, 1), FH_ALTA, USER);
        lista.add(t4);

        // MONTO: vigente (valores directos en pesos)
        FalTarifarioUnidadFaltas t5 = new FalTarifarioUnidadFaltas(5L,
                TipoUnidadFaltas.MONTO, new BigDecimal("1.00"),
                LocalDate.of(2022, 1, 1), FH_ALTA, USER);
        lista.add(t5);

        ((InMemoryTarifarioUnidadFaltasRepository) tarifarioRepo).cargarSeed(lista);
    }

    private void cargarMedidas() {
        List<FalMedidaPreventiva> lista = new ArrayList<>();

        // RETLIC v1 - Retención de licencia, puede bloquear cierre
        FalMedidaPreventiva m1 = new FalMedidaPreventiva(1L, "RETLIC", (short) 1,
                "Retención de licencia de conducir", FH_ALTA, USER);
        m1.setSiPuedeBloquearCierre(true);
        m1.setTipoBloqueanteDefault(OrigenBloqueanteMaterial.DOCUMENTACION_RETENIDA);
        lista.add(m1);

        // RETROD v1 - Retención de rodado, puede bloquear cierre
        FalMedidaPreventiva m2 = new FalMedidaPreventiva(2L, "RETROD", (short) 1,
                "Retención de rodado/vehículo", FH_ALTA, USER);
        m2.setSiPuedeBloquearCierre(true);
        m2.setTipoBloqueanteDefault(OrigenBloqueanteMaterial.RODADO);
        lista.add(m2);

        // SECVEH v1 - Secuestro de vehículo, puede bloquear cierre
        FalMedidaPreventiva m3 = new FalMedidaPreventiva(3L, "SECVEH", (short) 1,
                "Secuestro de vehículo", FH_ALTA, USER);
        m3.setSiPuedeBloquearCierre(true);
        m3.setTipoBloqueanteDefault(OrigenBloqueanteMaterial.RODADO);
        lista.add(m3);

        // CLAUSURA v1 - histórica, inactiva
        FalMedidaPreventiva m4 = new FalMedidaPreventiva(4L, "CLAUSURA", (short) 1,
                "Clausura del local (versión inicial)", FH_ALTA, USER);
        m4.setSiActiva(false);
        lista.add(m4);

        // CLAUSURA v2 - activa, no bloquea cierre
        FalMedidaPreventiva m5 = new FalMedidaPreventiva(5L, "CLAUSURA", (short) 2,
                "Clausura del local comercial", FH_ALTA, USER);
        m5.setDescripcionDetalle("Clausura temporal o definitiva según resolución de la autoridad competente.");
        m5.setSiPuedeBloquearCierre(false);
        lista.add(m5);

        ((InMemoryMedidaPreventivaRepository) medidaRepo).cargarSeed(lista);
    }

    private void cargarRelaciones() {
        List<FalArticuloMedidaPreventiva> lista = new ArrayList<>();

        // Artículo 1 tiene medida RETLIC obligatoria y RETROD opcional
        // (IDs de artículos dependen del seed de normativa; usamos IDs 1 y 2)
        ArticuloMedidaPreventivaId id1 = new ArticuloMedidaPreventivaId(1L, 1L);
        FalArticuloMedidaPreventiva r1 = new FalArticuloMedidaPreventiva(id1, true, FH_ALTA, USER);
        lista.add(r1);

        ArticuloMedidaPreventivaId id2 = new ArticuloMedidaPreventivaId(1L, 2L);
        FalArticuloMedidaPreventiva r2 = new FalArticuloMedidaPreventiva(id2, false, FH_ALTA, USER);
        lista.add(r2);

        // Artículo 2 tiene medida SECVEH obligatoria
        ArticuloMedidaPreventivaId id3 = new ArticuloMedidaPreventivaId(2L, 3L);
        FalArticuloMedidaPreventiva r3 = new FalArticuloMedidaPreventiva(id3, true, FH_ALTA, USER);
        lista.add(r3);

        // Relación histórica inactiva (artículo 2, CLAUSURA v1=id4)
        ArticuloMedidaPreventivaId id4 = new ArticuloMedidaPreventivaId(2L, 4L);
        FalArticuloMedidaPreventiva r4 = new FalArticuloMedidaPreventiva(id4, false, FH_ALTA, USER);
        r4.setSiActiva(false);
        lista.add(r4);

        ((InMemoryArticuloMedidaPreventivaRepository) articuloMedidaRepo).cargarSeed(lista);
    }
}
