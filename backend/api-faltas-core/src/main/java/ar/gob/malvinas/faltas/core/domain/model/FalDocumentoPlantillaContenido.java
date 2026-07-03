package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.FormatoPlantillaContenido;

import java.time.LocalDateTime;

/**
 * Contenido versionado de una plantilla documental.
 *
 * Una plantilla funcional puede tener multiples versiones de contenido.
 * Solo una version activa y vigente debe resolver para una fecha dada.
 * El cuerpoTemplate contiene variables en la forma {{namespace.campo}}.
 *
 * Slice 8F-1.
 */
public class FalDocumentoPlantillaContenido {

    private final Long id;
    private final Long plantillaId;
    private final short versionContenido;
    private final FormatoPlantillaContenido formato;
    private final String titulo;
    private final String cuerpoTemplate;
    private final String encabezadoTemplate;
    private final String pieTemplate;
    private final String variablesDeclaradasJson;
    private boolean siActivo;
    private final LocalDateTime fhVigDesde;
    private LocalDateTime fhVigHasta;
    private final LocalDateTime fhAlta;
    private final String idUserAlta;

    public FalDocumentoPlantillaContenido(
            Long id, Long plantillaId, short versionContenido,
            FormatoPlantillaContenido formato, String titulo, String cuerpoTemplate,
            String encabezadoTemplate, String pieTemplate, String variablesDeclaradasJson,
            boolean siActivo, LocalDateTime fhVigDesde, LocalDateTime fhVigHasta,
            LocalDateTime fhAlta, String idUserAlta) {
        if (id == null) throw new IllegalArgumentException("id requerido");
        if (plantillaId == null) throw new IllegalArgumentException("plantillaId requerido");
        if (formato == null) throw new IllegalArgumentException("formato requerido");
        if (titulo == null || titulo.isBlank()) throw new IllegalArgumentException("titulo requerido");
        if (cuerpoTemplate == null || cuerpoTemplate.isBlank())
            throw new IllegalArgumentException("cuerpoTemplate no puede ser blank");
        if (fhVigDesde == null) throw new IllegalArgumentException("fhVigDesde requerido");
        if (idUserAlta == null || idUserAlta.isBlank()) throw new IllegalArgumentException("idUserAlta requerido");
        if (fhVigHasta != null && fhVigHasta.isBefore(fhVigDesde))
            throw new IllegalArgumentException("fhVigHasta no puede ser anterior a fhVigDesde");

        this.id = id;
        this.plantillaId = plantillaId;
        this.versionContenido = versionContenido;
        this.formato = formato;
        this.titulo = titulo;
        this.cuerpoTemplate = cuerpoTemplate;
        this.encabezadoTemplate = encabezadoTemplate;
        this.pieTemplate = pieTemplate;
        this.variablesDeclaradasJson = variablesDeclaradasJson;
        this.siActivo = siActivo;
        this.fhVigDesde = fhVigDesde;
        this.fhVigHasta = fhVigHasta;
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
    public Long getPlantillaId() { return plantillaId; }
    public short getVersionContenido() { return versionContenido; }
    public FormatoPlantillaContenido getFormato() { return formato; }
    public String getTitulo() { return titulo; }
    public String getCuerpoTemplate() { return cuerpoTemplate; }
    public String getEncabezadoTemplate() { return encabezadoTemplate; }
    public String getPieTemplate() { return pieTemplate; }
    public String getVariablesDeclaradasJson() { return variablesDeclaradasJson; }
    public boolean isSiActivo() { return siActivo; }
    public void setSiActivo(boolean v) { this.siActivo = v; }
    public LocalDateTime getFhVigDesde() { return fhVigDesde; }
    public LocalDateTime getFhVigHasta() { return fhVigHasta; }
    public void setFhVigHasta(LocalDateTime v) { this.fhVigHasta = v; }
    public LocalDateTime getFhAlta() { return fhAlta; }
    public String getIdUserAlta() { return idUserAlta; }
}