package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;

import java.time.LocalDateTime;

/**
 * Plantilla operativa por defecto para una accion documental.
 *
 * Regla de resolucion (via DocumentoPlantillaDefaultService):
 *   1. Filtrar activos y vigentes.
 *   2. Filtrar por accionDocumental, tipoActa (null=generico), idDependencia (null=generico).
 *   3. Ordenar por prioridad DESC. Tomar el maximo.
 *   4. Si empate de prioridad: PlantillaDefaultAmbiguaException.
 *   5. Si ninguno: PlantillaDefaultNoEncontradaException.
 *
 * Slice 8F-1.
 */
public class FalDocumentoPlantillaDefault {

    private final Long id;
    private final AccionDocumental accionDocumental;
    private final TipoActa tipoActa;
    private final TipoDocu tipoDocu;
    private final Long idDependencia;
    private final Short verDependencia;
    private final Long plantillaId;
    private final int prioridad;
    private final LocalDateTime fhVigDesde;
    private LocalDateTime fhVigHasta;
    private boolean siActivo;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalDocumentoPlantillaDefault(
            Long id, AccionDocumental accionDocumental, TipoActa tipoActa, TipoDocu tipoDocu,
            Long idDependencia, Short verDependencia, Long plantillaId, int prioridad,
            LocalDateTime fhVigDesde, LocalDateTime fhVigHasta, boolean siActivo,
            LocalDateTime fhAlta, String idUserAlta) {
        if (id == null) throw new IllegalArgumentException("id requerido");
        if (accionDocumental == null) throw new IllegalArgumentException("accionDocumental requerido");
        if (tipoDocu == null) throw new IllegalArgumentException("tipoDocu requerido");
        if (plantillaId == null) throw new IllegalArgumentException("plantillaId requerido");
        if (fhVigDesde == null) throw new IllegalArgumentException("fhVigDesde requerido");
        if (idUserAlta == null || idUserAlta.isBlank()) throw new IllegalArgumentException("idUserAlta requerido");
        if (fhVigHasta != null && fhVigHasta.isBefore(fhVigDesde))
            throw new IllegalArgumentException("fhVigHasta no puede ser anterior a fhVigDesde");

        this.id = id;
        this.accionDocumental = accionDocumental;
        this.tipoActa = tipoActa;
        this.tipoDocu = tipoDocu;
        this.idDependencia = idDependencia;
        this.verDependencia = verDependencia;
        this.plantillaId = plantillaId;
        this.prioridad = prioridad;
        this.fhVigDesde = fhVigDesde;
        this.fhVigHasta = fhVigHasta;
        this.siActivo = siActivo;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public boolean vigente(LocalDateTime en) {
        if (!siActivo) return false;
        if (en.isBefore(fhVigDesde)) return false;
        if (fhVigHasta != null && en.isAfter(fhVigHasta)) return false;
        return true;
    }

    public Long getId() { return id; }
    public AccionDocumental getAccionDocumental() { return accionDocumental; }
    public TipoActa getTipoActa() { return tipoActa; }
    public TipoDocu getTipoDocu() { return tipoDocu; }
    public Long getIdDependencia() { return idDependencia; }
    public Short getVerDependencia() { return verDependencia; }
    public Long getPlantillaId() { return plantillaId; }
    public int getPrioridad() { return prioridad; }
    public LocalDateTime getFhVigDesde() { return fhVigDesde; }
    public LocalDateTime getFhVigHasta() { return fhVigHasta; }
    public void setFhVigHasta(LocalDateTime v) { this.fhVigHasta = v; }
    public boolean isSiActivo() { return siActivo; }
    public void setSiActivo(boolean v) { this.siActivo = v; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
}
