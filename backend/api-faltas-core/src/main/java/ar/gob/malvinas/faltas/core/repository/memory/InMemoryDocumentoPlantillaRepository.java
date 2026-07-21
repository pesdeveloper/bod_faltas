package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantilla;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantillaFirmaReq;
import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryDocumentoPlantillaRepository implements DocumentoPlantillaRepository, ResettableInMemoryRepository {

    private final Map<Long, FalDocumentoPlantilla> plantillas = new ConcurrentHashMap<>();
    private final Map<Long, FalDocumentoPlantillaFirmaReq> firmaReqs = new ConcurrentHashMap<>();
    private final AtomicLong plantillaIdCounter = new AtomicLong(1);
    private final AtomicLong firmaReqIdCounter = new AtomicLong(1);

    @Override
    public Long nextPlantillaId() {
        return plantillaIdCounter.getAndIncrement();
    }

    @Override
    public Long nextFirmaReqId() {
        return firmaReqIdCounter.getAndIncrement();
    }

    @Override
    public FalDocumentoPlantilla guardar(FalDocumentoPlantilla plantilla) {
        plantillas.put(plantilla.getId(), plantilla);
        return plantilla;
    }

    @Override
    public Optional<FalDocumentoPlantilla> buscarPorId(Long id) {
        return Optional.ofNullable(plantillas.get(id));
    }

    @Override
    public Optional<FalDocumentoPlantilla> buscarPorCodigo(String codigo) {
        return plantillas.values().stream()
                .filter(p -> p.getCodigo().equals(codigo))
                .findFirst();
    }

    @Override
    public List<FalDocumentoPlantilla> listar() {
        return List.copyOf(plantillas.values());
    }

    @Override
    public List<FalDocumentoPlantilla> buscarPorAccion(AccionDocumental accionDocumental) {
        return plantillas.values().stream()
                .filter(p -> p.getAccionDocumental() == accionDocumental)
                .toList();
    }

    @Override
    public List<FalDocumentoPlantilla> buscarActivasPorAccion(AccionDocumental accionDocumental) {
        return plantillas.values().stream()
                .filter(p -> p.getAccionDocumental() == accionDocumental && p.isSiActiva())
                .toList();
    }

    @Override
    public FalDocumentoPlantillaFirmaReq guardarFirmaReq(FalDocumentoPlantillaFirmaReq req) {
        firmaReqs.put(req.getId(), req);
        return req;
    }

    @Override
    public List<FalDocumentoPlantillaFirmaReq> listarFirmaReqPorPlantilla(Long plantillaId) {
        return firmaReqs.values().stream()
                .filter(r -> plantillaId.equals(r.getPlantillaId()))
                .toList();
    }

    @Override
    public Optional<FalDocumentoPlantillaFirmaReq> buscarFirmaReqPorId(Long id) {
        return Optional.ofNullable(firmaReqs.get(id));
    }

    @Override
    public void reset() { plantillas.clear(); firmaReqs.clear(); }

    @Override
    public String nombre() { return "plantillas"; }

    @Override
    public int size() { return plantillas.size(); }
}
