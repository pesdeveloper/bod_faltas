package ar.gob.malvinas.faltas.core.application.demo;

import ar.gob.malvinas.faltas.core.application.result.DatasetFuncionalCoberturaResultado;
import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaRepository;
import ar.gob.malvinas.faltas.core.web.dto.DemoHealthDatasetDto;
import ar.gob.malvinas.faltas.core.web.dto.DemoHealthDocumentosDto;
import ar.gob.malvinas.faltas.core.web.dto.DemoHealthEndpointDto;
import ar.gob.malvinas.faltas.core.web.dto.DemoHealthResetDto;
import ar.gob.malvinas.faltas.core.web.dto.DemoHealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de evaluacion de demo-readiness.
 *
 * Evalua checks internos sin llamadas HTTP contra si mismo:
 * - Dataset funcional: conteo y disponibilidad de detallePath.
 * - Plantillas documentales: conteo via repositorio in-memory.
 * - Reset dev/test: solo informa si esta habilitado o no (no ejecuta reset).
 * - Endpoints demo: lista estatica de endpoints conocidos y su estado.
 *
 * Todos los checks son de solo lectura. Ningun check ejecuta flujos de dominio.
 *
 * Slice 8F-8 - cierre GAP-8.
 */
@Service
public class DemoHealthService {

    private static final String VERSION_DEMO = "8F-8";
    private static final int TOTAL_ACTAS_ESPERADAS = 31;
    private static final int TOTAL_PLANTILLAS_ESPERADAS = 8;

    private final DocumentoPlantillaRepository plantillaRepository;

    @Value("${faltas.demo.reset.enabled:false}")
    private boolean resetEnabled;

    public DemoHealthService(DocumentoPlantillaRepository plantillaRepository) {
        this.plantillaRepository = plantillaRepository;
    }

    public DemoHealthResponse evaluar() {
        List<String> warnings = new ArrayList<>();

        DemoHealthDatasetDto datasetDto = evaluarDataset(warnings);
        DemoHealthDocumentosDto documentosDto = evaluarDocumentos(warnings);
        DemoHealthResetDto resetDto = buildResetDto();
        List<DemoHealthEndpointDto> endpoints = buildEndpoints();

        boolean demoReady = datasetDto.ready() && documentosDto.ready();

        return new DemoHealthResponse(
                "UP",
                demoReady,
                LocalDateTime.now().toString(),
                VERSION_DEMO,
                datasetDto,
                documentosDto,
                resetDto,
                endpoints,
                warnings
        );
    }

    private DemoHealthDatasetDto evaluarDataset(List<String> warnings) {
        DatasetFuncionalCoberturaResultado cobertura = DatasetFuncionalDominioCatalog.calcularCobertura();
        int total = cobertura.totalActasMock();
        boolean coberturaCompleta = cobertura.coberturaCompletaSegunDominioActual();

        boolean detalleDisponible = !cobertura.actas().isEmpty()
                && cobertura.actas().get(0).detallePath() != null
                && cobertura.actas().get(0).detallePath().startsWith("/demo/actas/");

        if (total != TOTAL_ACTAS_ESPERADAS) {
            warnings.add("dataset: totalActasMock=" + total + " (esperado " + TOTAL_ACTAS_ESPERADAS + ")");
        }
        if (!coberturaCompleta) {
            warnings.add("dataset: cobertura incompleta segun dominio actual");
        }

        boolean ready = total >= TOTAL_ACTAS_ESPERADAS && detalleDisponible;
        return new DemoHealthDatasetDto(ready, total, coberturaCompleta, detalleDisponible);
    }

    private DemoHealthDocumentosDto evaluarDocumentos(List<String> warnings) {
        int totalPlantillas = plantillaRepository.listar().size();

        if (totalPlantillas < TOTAL_PLANTILLAS_ESPERADAS) {
            warnings.add("documentos: totalPlantillasMock=" + totalPlantillas + " (esperado " + TOTAL_PLANTILLAS_ESPERADAS + ")");
        }

        boolean ready = totalPlantillas >= TOTAL_PLANTILLAS_ESPERADAS;
        return new DemoHealthDocumentosDto(ready, totalPlantillas, true, false);
    }

    private DemoHealthResetDto buildResetDto() {
        return new DemoHealthResetDto(
                "/demo/dev/reset",
                resetEnabled,
                !resetEnabled
        );
    }

    private List<DemoHealthEndpointDto> buildEndpoints() {
        return List.of(
                new DemoHealthEndpointDto("GET", "/demo/documentos/graph", true,
                        "Graph documental con todos los casos de plantilla/documento mock"),
                new DemoHealthEndpointDto("GET", "/demo/actas/dataset-funcional", true,
                        "Dataset funcional completo con 31 actas mock y detallePath"),
                new DemoHealthEndpointDto("GET", "/demo/actas/{codigo}", true,
                        "Drill-down individual: instancia real con timeline y documentos"),
                new DemoHealthEndpointDto("POST", "/demo/dev/reset", true,
                        "Reset dev/test de repositorios in-memory (protegido por property)"),
                new DemoHealthEndpointDto("GET", "/demo/health", true,
                        "Health/readiness demo para frontend Angular")
        );
    }
}