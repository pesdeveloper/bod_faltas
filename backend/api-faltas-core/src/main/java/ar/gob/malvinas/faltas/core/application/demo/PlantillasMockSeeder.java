package ar.gob.malvinas.faltas.core.application.demo;

import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.enums.FormatoPlantillaContenido;
import ar.gob.malvinas.faltas.core.domain.enums.MomentoNumeracionDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFirmaReq;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantilla;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantillaContenido;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantillaDefault;
import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaContenidoRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaDefaultRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Seeder de plantillas mock para los 8 casos operativos principales.
 *
 * Carga en los repositorios in-memory:
 *   - 8 FalDocumentoPlantilla (funcional)
 *   - 8 FalDocumentoPlantillaContenido (contenido con variables reales)
 *   - 8 FalDocumentoPlantillaDefault (regla de resolucion por accion documental)
 *
 * Casos cubiertos:
 *   1. EMITIR_FALLO              -> ACTO_ADMINISTRATIVO
 *   2. EMITIR_NOTIFICACION_ACTA  -> NOTIFICACION_ACTA
 *   3. EMITIR_NOTIFICACION_FALLO -> NOTIFICACION_ACTO_ADMINISTRATIVO
 *   4. EMITIR_INTIMACION_PAGO    -> INTIMACION_PAGO
 *   5. EMITIR_MEDIDA_PREVENTIVA  -> MEDIDA_PREVENTIVA
 *   6. EMITIR_CONSTANCIA         -> CONSTANCIA
 *   7. EMITIR_ANEXO              -> ANEXO
 *   8. EMITIR_RESOLUTORIO_BLOQUEANTE -> RESOLUTORIO_BLOQUEANTE
 *
 * IDs reservados para demo: plantilla 1001-1008, contenido 2001-2008, default 3001-3008.
 *
 * Guardrails:
 *   - No genera PDF.
 *   - No crea tablas ni scripts SQL.
 *   - No usa JPA/Hibernate.
 *   - Solo variables registradas en DocumentoVariableRegistry.
 *
 * Slice 8F-2.
 */
@Component
public class PlantillasMockSeeder {

    private static final LocalDate VIG_DESDE = LocalDate.of(2024, 1, 1);
    private static final LocalDateTime FH_ALTA = LocalDateTime.of(2024, 1, 1, 0, 0);
    private static final String USER_SISTEMA = "sistema";

    private final DocumentoPlantillaRepository plantillaRepo;
    private final DocumentoPlantillaContenidoRepository contenidoRepo;
    private final DocumentoPlantillaDefaultRepository defaultRepo;

    public PlantillasMockSeeder(
            DocumentoPlantillaRepository plantillaRepo,
            DocumentoPlantillaContenidoRepository contenidoRepo,
            DocumentoPlantillaDefaultRepository defaultRepo) {
        this.plantillaRepo = plantillaRepo;
        this.contenidoRepo = contenidoRepo;
        this.defaultRepo = defaultRepo;
    }

    @PostConstruct
    public void seed() {
        seedar(plantillaRepo, contenidoRepo, defaultRepo);
    }

    /**
     * Metodo estatico invocable desde tests con repositorios locales.
     */
    public static void seedar(
            DocumentoPlantillaRepository plantillaRepo,
            DocumentoPlantillaContenidoRepository contenidoRepo,
            DocumentoPlantillaDefaultRepository defaultRepo) {

        seedCaso(plantillaRepo, contenidoRepo, defaultRepo,
                1001L, 2001L, 3001L,
                "TMPL-FALLO-001",
                "Plantilla de Fallo Administrativo",
                TipoDocu.ACTO_ADMINISTRATIVO,
                AccionDocumental.EMITIR_FALLO,
                10,
                templateFallo());

        seedCaso(plantillaRepo, contenidoRepo, defaultRepo,
                1002L, 2002L, 3002L,
                "TMPL-NOTIF-ACTA-001",
                "Plantilla de Notificacion de Acta",
                TipoDocu.NOTIFICACION_ACTA,
                AccionDocumental.EMITIR_NOTIFICACION_ACTA,
                10,
                templateNotificacionActa());

        seedCaso(plantillaRepo, contenidoRepo, defaultRepo,
                1003L, 2003L, 3003L,
                "TMPL-NOTIF-FALLO-001",
                "Plantilla de Notificacion de Fallo",
                TipoDocu.NOTIFICACION_ACTO_ADMINISTRATIVO,
                AccionDocumental.EMITIR_NOTIFICACION_FALLO,
                10,
                templateNotificacionFallo());

        seedCaso(plantillaRepo, contenidoRepo, defaultRepo,
                1004L, 2004L, 3004L,
                "TMPL-INTIM-PAGO-001",
                "Plantilla de Intimacion de Pago",
                TipoDocu.INTIMACION_PAGO,
                AccionDocumental.EMITIR_INTIMACION_PAGO,
                10,
                templateIntimacionPago());

        seedCaso(plantillaRepo, contenidoRepo, defaultRepo,
                1005L, 2005L, 3005L,
                "TMPL-MED-PREV-001",
                "Plantilla de Medida Preventiva",
                TipoDocu.MEDIDA_PREVENTIVA,
                AccionDocumental.EMITIR_MEDIDA_PREVENTIVA,
                10,
                templateMedidaPreventiva());

        seedCaso(plantillaRepo, contenidoRepo, defaultRepo,
                1006L, 2006L, 3006L,
                "TMPL-CONST-001",
                "Plantilla de Constancia",
                TipoDocu.CONSTANCIA,
                AccionDocumental.EMITIR_CONSTANCIA,
                10,
                templateConstancia());

        seedCaso(plantillaRepo, contenidoRepo, defaultRepo,
                1007L, 2007L, 3007L,
                "TMPL-ANEXO-001",
                "Plantilla de Anexo Documental",
                TipoDocu.ANEXO,
                AccionDocumental.EMITIR_ANEXO,
                10,
                templateAnexo());

        seedCaso(plantillaRepo, contenidoRepo, defaultRepo,
                1008L, 2008L, 3008L,
                "TMPL-RES-BLOQ-001",
                "Plantilla de Resolutorio Bloqueante",
                TipoDocu.RESOLUTORIO_BLOQUEANTE,
                AccionDocumental.EMITIR_RESOLUTORIO_BLOQUEANTE,
                10,
                templateResolutorioBloqueante());
    }

    private static void seedCaso(
            DocumentoPlantillaRepository plantillaRepo,
            DocumentoPlantillaContenidoRepository contenidoRepo,
            DocumentoPlantillaDefaultRepository defaultRepo,
            Long plantillaId, Long contenidoId, Long defaultId,
            String codigo, String nombre,
            TipoDocu tipoDocu, AccionDocumental accion,
            int prioridad, String cuerpoTemplate) {

        FalDocumentoPlantilla plantilla = new FalDocumentoPlantilla(
                plantillaId, codigo, nombre,
                "Plantilla mock demo - " + accion.name(),
                tipoDocu, accion, null,
                TipoFirmaReq.FIRMA_AUTORIDAD,
                true, MomentoNumeracionDocu.AL_EMITIR,
                false, false, true, true,
                VIG_DESDE, null,
                FH_ALTA, USER_SISTEMA);
        plantillaRepo.guardar(plantilla);

        LocalDateTime vigDesde = FH_ALTA;
        FalDocumentoPlantillaContenido contenido = new FalDocumentoPlantillaContenido(
                contenidoId, plantillaId, (short) 1,
                FormatoPlantillaContenido.TEXTO_PLANO,
                nombre, cuerpoTemplate,
                null, null, null,
                true, vigDesde, null,
                FH_ALTA, USER_SISTEMA);
        contenidoRepo.guardar(contenido);

        LocalDateTime defaultVigDesde = FH_ALTA;
        FalDocumentoPlantillaDefault plantillaDefault = new FalDocumentoPlantillaDefault(
                defaultId, accion, null, tipoDocu,
                null, null, plantillaId,
                prioridad, defaultVigDesde, null, true,
                FH_ALTA, USER_SISTEMA);
        defaultRepo.guardar(plantillaDefault);
    }

    // --- Templates ---

    private static String templateFallo() {
        return
            "RESOLUCION DE FALLO\n" +
            "Municipalidad de {{sistema.municipioNombre}}\n" +
            "\n" +
            "Fecha: {{acta.fechaLabrado}}\n" +
            "Acta Nro: {{acta.nroActa}}\n" +
            "\n" +
            "Infractor: {{infractor.nombreCompleto}} - DNI {{infractor.documento}}\n" +
            "Domicilio: {{domicilioInfractor.texto}}\n" +
            "\n" +
            "Infraccion registrada en: {{domicilioInfraccion.texto}}\n" +
            "Descripcion: {{infraccion.descripcion}}\n" +
            "\n" +
            "Tipo de fallo: {{fallo.tipo}}\n" +
            "Fecha de dictado: {{fallo.fechaDictado}}\n" +
            "Fundamentos: {{fallo.fundamentos}}\n" +
            "\n" +
            "Expedido en {{sistema.municipioNombre}}, {{sistema.fechaActual}}.";
    }

    private static String templateNotificacionActa() {
        return
            "NOTIFICACION DE ACTA DE INFRACCION\n" +
            "Municipalidad de {{sistema.municipioNombre}}\n" +
            "\n" +
            "Sr./Sra. {{infractor.nombreCompleto}} - DNI {{infractor.documento}}\n" +
            "Domicilio: {{domicilioInfractor.texto}}\n" +
            "\n" +
            "Se le notifica que el {{acta.fechaLabrado}} le fue labrada el Acta Nro {{acta.nroActa}}\n" +
            "por infraccion cometida en: {{domicilioInfraccion.texto}}\n" +
            "\n" +
            "Descripcion de la infraccion: {{infraccion.descripcion}}\n" +
            "\n" +
            "Fecha de notificacion: {{sistema.fechaActual}}\n" +
            "Municipio: {{sistema.municipioNombre}}";
    }

    private static String templateNotificacionFallo() {
        return
            "NOTIFICACION DE RESOLUCION DE FALLO\n" +
            "Municipalidad de {{sistema.municipioNombre}}\n" +
            "\n" +
            "Sr./Sra. {{infractor.nombreCompleto}} - DNI {{infractor.documento}}\n" +
            "Domicilio: {{domicilioInfractor.texto}}\n" +
            "\n" +
            "Se le notifica la Resolucion de Fallo recaida en el Acta Nro {{acta.nroActa}}\n" +
            "Tipo de fallo: {{fallo.tipo}}\n" +
            "Fecha de dictado: {{fallo.fechaDictado}}\n" +
            "\n" +
            "Tiene derecho a interponer recurso de apelacion dentro del plazo legal.\n" +
            "\n" +
            "Fecha de notificacion: {{sistema.fechaActual}}\n" +
            "Municipio: {{sistema.municipioNombre}}";
    }

    private static String templateIntimacionPago() {
        return
            "INTIMACION DE PAGO\n" +
            "Municipalidad de {{sistema.municipioNombre}}\n" +
            "\n" +
            "Sr./Sra. {{infractor.nombreCompleto}} - DNI {{infractor.documento}}\n" +
            "Domicilio: {{domicilioInfractor.texto}}\n" +
            "\n" +
            "En virtud del fallo recaido en el Acta Nro {{acta.nroActa}},\n" +
            "se intima al pago de la multa correspondiente.\n" +
            "\n" +
            "Monto: {{pago.monto}}\n" +
            "Referencia de pago: {{pago.referenciaPago}}\n" +
            "Vencimiento: {{pago.fechaVencimiento}}\n" +
            "\n" +
            "Fecha: {{sistema.fechaActual}}\n" +
            "Municipio: {{sistema.municipioNombre}}";
    }

    private static String templateMedidaPreventiva() {
        return
            "MEDIDA PREVENTIVA\n" +
            "Municipalidad de {{sistema.municipioNombre}}\n" +
            "\n" +
            "Fecha: {{acta.fechaLabrado}}\n" +
            "Acta Nro: {{acta.nroActa}}\n" +
            "\n" +
            "Infractor: {{infractor.nombreCompleto}} - DNI {{infractor.documento}}\n" +
            "Domicilio del hecho: {{domicilioInfraccion.texto}}\n" +
            "Descripcion: {{infraccion.descripcion}}\n" +
            "\n" +
            "Se dispone la aplicacion de medida preventiva conforme normativa vigente.\n" +
            "\n" +
            "Expedido en {{sistema.municipioNombre}}, {{sistema.fechaActual}}.";
    }

    private static String templateConstancia() {
        return
            "CONSTANCIA\n" +
            "Municipalidad de {{sistema.municipioNombre}}\n" +
            "\n" +
            "Por la presente se deja constancia que {{infractor.nombreCompleto}},\n" +
            "DNI {{infractor.documento}}, domiciliado en {{domicilioInfractor.texto}},\n" +
            "registra el Acta Nro {{acta.nroActa}} labrada el {{acta.fechaLabrado}}\n" +
            "por infraccion en: {{domicilioInfraccion.texto}}\n" +
            "\n" +
            "Expedido en {{sistema.municipioNombre}}, {{sistema.fechaActual}}.";
    }

    private static String templateAnexo() {
        return
            "ANEXO DOCUMENTAL\n" +
            "Municipalidad de {{sistema.municipioNombre}}\n" +
            "\n" +
            "Acta Nro: {{acta.nroActa}}\n" +
            "Infractor: {{infractor.nombreCompleto}} - DNI {{infractor.documento}}\n" +
            "\n" +
            "El presente anexo forma parte integrante del expediente de faltas\n" +
            "correspondiente al Acta labrada el {{acta.fechaLabrado}}.\n" +
            "\n" +
            "Descripcion de la infraccion: {{infraccion.descripcion}}\n" +
            "\n" +
            "Expedido en {{sistema.municipioNombre}}, {{sistema.fechaActual}}.";
    }

    private static String templateResolutorioBloqueante() {
        return
            "RESOLUTORIO DE TRABA BLOQUEANTE\n" +
            "Municipalidad de {{sistema.municipioNombre}}\n" +
            "\n" +
            "Fecha: {{acta.fechaLabrado}}\n" +
            "Acta Nro: {{acta.nroActa}}\n" +
            "\n" +
            "Infractor: {{infractor.nombreCompleto}} - DNI {{infractor.documento}}\n" +
            "Domicilio: {{domicilioInfractor.texto}}\n" +
            "\n" +
            "Por medio del presente resolutorio se instruye sobre la situacion de traba\n" +
            "material que impide la prosecucion del expediente de faltas.\n" +
            "Descripcion: {{infraccion.descripcion}}\n" +
            "\n" +
            "Expedido en {{sistema.municipioNombre}}, {{sistema.fechaActual}}.";
    }
}
