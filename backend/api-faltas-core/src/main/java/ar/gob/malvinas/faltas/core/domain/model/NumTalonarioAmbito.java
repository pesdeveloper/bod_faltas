package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.AlcanceTalonario;
import ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Ambito de uso de un talonario: regla que autoriza donde aplica el talonario.
 *
 * Puede ser GLOBAL (todas las dependencias), DEPENDENCIA (una especifica)
 * o TRANSVERSAL_DOCUMENTO (por tipo de documento, transversal a dependencias).
 *
 * La prioridad resuelve conflictos cuando varios ambitos aplican al mismo contexto.
 * Menor valor de prioridad = mayor prioridad (convencion a documentar en 8B-2).
 *
 * Alineado con num_talonario_ambito del modelo MariaDB productivo.
 *
 * Slice 8B-2: implementacion in-memory. Slice 9: persiste en num_talonario_ambito via JDBC.
 */
public class NumTalonarioAmbito {

    private final Long id;
    private final Long talonarioId;
    private final ClaseNumeracion claseTalonario;
    private final Short tipoDocu;
    private final Short tipoActa;
    private final Long idDep;
    private final Short verDep;
    private final AlcanceTalonario alcance;
    private final short prioridad;
    private final LocalDate fhDesde;
    private LocalDate fhHasta;
    private boolean siActivo;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public NumTalonarioAmbito(
            Long id,
            Long talonarioId,
            ClaseNumeracion claseTalonario,
            Short tipoDocu,
            Short tipoActa,
            Long idDep,
            Short verDep,
            AlcanceTalonario alcance,
            short prioridad,
            LocalDate fhDesde,
            LocalDate fhHasta,
            boolean siActivo,
            LocalDateTime fhAlta,
            String idUserAlta) {
        this.id = id;
        this.talonarioId = talonarioId;
        this.claseTalonario = claseTalonario;
        this.tipoDocu = tipoDocu;
        this.tipoActa = tipoActa;
        this.idDep = idDep;
        this.verDep = verDep;
        this.alcance = alcance;
        this.prioridad = prioridad;
        this.fhDesde = fhDesde;
        this.fhHasta = fhHasta;
        this.siActivo = siActivo;
        this.fhAlta = fhAlta;
        this.idUserAlta = idUserAlta;
    }

    public Long getId() { return id; }
    public Long getTalonarioId() { return talonarioId; }
    public ClaseNumeracion getClaseTalonario() { return claseTalonario; }
    public Short getTipoDocu() { return tipoDocu; }
    public Short getTipoActa() { return tipoActa; }
    public Long getIdDep() { return idDep; }
    public Short getVerDep() { return verDep; }
    public AlcanceTalonario getAlcance() { return alcance; }
    public short getPrioridad() { return prioridad; }
    public LocalDate getFhDesde() { return fhDesde; }
    public LocalDate getFhHasta() { return fhHasta; }
    public void setFhHasta(LocalDate fhHasta) { this.fhHasta = fhHasta; }
    public boolean isSiActivo() { return siActivo; }
    public void setSiActivo(boolean siActivo) { this.siActivo = siActivo; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }

    public boolean esVigenteEn(LocalDate fecha) {
        if (!siActivo) return false;
        if (fecha.isBefore(fhDesde)) return false;
        return fhHasta == null || !fecha.isAfter(fhHasta);
    }
}
