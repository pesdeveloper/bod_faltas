package ar.gob.malvinas.faltas.core.application.combinacion;

import ar.gob.malvinas.faltas.core.domain.enums.DocumentoVariableNamespace;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDatoVariableDocumento;
import ar.gob.malvinas.faltas.core.domain.model.DocumentoVariableDefinicion;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registro in-memory de variables documentales disponibles para combinacion.
 *
 * Variables requeridas: acta.fechaLabrado, infractor.nombreCompleto, infractor.documento.
 * Variables opcionales: fallo.*, pago.*, notificacion.* y el resto.
 *
 * No se persiste como tabla. No se crea seed.
 *
 * Slice 8F-1: 15 variables base.
 * Slice 8F-2: +11 variables de fallo, pago, notificacion, infraccion. Total 26 variables.
 */
@Component
public class DocumentoVariableRegistry {

    private final Map<String, DocumentoVariableDefinicion> variables;

    public DocumentoVariableRegistry() {
        List<DocumentoVariableDefinicion> lista = List.of(
                // --- acta ---
                def("acta.nroActa", DocumentoVariableNamespace.ACTA, TipoDatoVariableDocumento.TEXTO, false, "ACT-2024-00001", "acta.nroActa"),
                def("acta.fechaLabrado", DocumentoVariableNamespace.ACTA, TipoDatoVariableDocumento.FECHA_HORA, true, "15/03/2024 10:30", "acta.fechaLabrado"),
                // --- infractor ---
                def("infractor.nombreCompleto", DocumentoVariableNamespace.INFRACTOR, TipoDatoVariableDocumento.TEXTO, true, "Juan Perez", "acta.infractorNombre"),
                def("infractor.documento", DocumentoVariableNamespace.INFRACTOR, TipoDatoVariableDocumento.TEXTO, true, "12345678", "acta.infractorDocumento"),
                // --- domicilios ---
                def("domicilioInfractor.texto", DocumentoVariableNamespace.DOMICILIO_INFRACTOR, TipoDatoVariableDocumento.TEXTO, false, "Belgrano 200", "acta.domicilioInfractor"),
                def("domicilioInfraccion.texto", DocumentoVariableNamespace.DOMICILIO_INFRACCION, TipoDatoVariableDocumento.TEXTO, false, "Av. Pioneros 500", "acta.domicilioHecho"),
                // --- ubicacion ---
                def("ubicacion.lat", DocumentoVariableNamespace.UBICACION, TipoDatoVariableDocumento.NUMERO, false, "-34.5678", "acta.latInfr"),
                def("ubicacion.lon", DocumentoVariableNamespace.UBICACION, TipoDatoVariableDocumento.NUMERO, false, "-58.1234", "acta.lonInfr"),
                // --- nomenclatura (mock deterministico) ---
                def("nomenclatura.manzana", DocumentoVariableNamespace.NOMENCLATURA, TipoDatoVariableDocumento.TEXTO, false, "12", null),
                def("nomenclatura.parcela", DocumentoVariableNamespace.NOMENCLATURA, TipoDatoVariableDocumento.TEXTO, false, "4B", null),
                // --- licencia (mock deterministico) ---
                def("licencia.municipioEmisor", DocumentoVariableNamespace.LICENCIA, TipoDatoVariableDocumento.TEXTO, false, "Malvinas Argentinas", null),
                // --- infraccion ---
                def("infraccion.descripcion", DocumentoVariableNamespace.INFRACCION, TipoDatoVariableDocumento.TEXTO, false, "Licencia vencida", "acta.observaciones"),
                // --- documento ---
                def("documento.nroDocu", DocumentoVariableNamespace.DOCUMENTO, TipoDatoVariableDocumento.TEXTO, false, "RES-2024-00010", "documento.nroDocu"),
                // --- sistema ---
                def("sistema.municipioNombre", DocumentoVariableNamespace.SISTEMA, TipoDatoVariableDocumento.TEXTO, false, "Malvinas Argentinas", "sistema"),
                def("sistema.fechaActual", DocumentoVariableNamespace.SISTEMA, TipoDatoVariableDocumento.FECHA_HORA, false, "02/07/2026 18:00", "sistema"),
                // --- fallo (slice 8F-2, opcionales) ---
                def("fallo.tipo", DocumentoVariableNamespace.FALLO, TipoDatoVariableDocumento.TEXTO, false, "CONDENATORIO", "fallo.tipoFallo"),
                def("fallo.monto", DocumentoVariableNamespace.FALLO, TipoDatoVariableDocumento.NUMERO, false, "15000.00", "fallo.montoCondena"),
                def("fallo.fundamentos", DocumentoVariableNamespace.FALLO, TipoDatoVariableDocumento.TEXTO, false, "Por los fundamentos expuestos", "fallo.fundamentos"),
                def("fallo.fechaDictado", DocumentoVariableNamespace.FALLO, TipoDatoVariableDocumento.FECHA_HORA, false, "10/04/2024 14:00", "fallo.fechaDictado"),
                // --- pago (slice 8F-2, opcionales) ---
                def("pago.monto", DocumentoVariableNamespace.PAGO, TipoDatoVariableDocumento.NUMERO, false, "15000.00", "pago.monto"),
                def("pago.referenciaPago", DocumentoVariableNamespace.PAGO, TipoDatoVariableDocumento.TEXTO, false, "REF-2024-00001", "pago.referenciaPago"),
                def("pago.estado", DocumentoVariableNamespace.PAGO, TipoDatoVariableDocumento.TEXTO, false, "MONTO_FIJADO", "pago.estadoPagoVoluntario"),
                def("pago.fechaVencimiento", DocumentoVariableNamespace.PAGO, TipoDatoVariableDocumento.FECHA_HORA, false, "01/05/2024 23:59", "pago.fechaVencimiento"),
                // --- notificacion (slice 8F-2, opcionales) ---
                def("notificacion.canal", DocumentoVariableNamespace.NOTIFICACION, TipoDatoVariableDocumento.TEXTO, false, "CORREO", "notificacion.canal"),
                def("notificacion.fechaEnvio", DocumentoVariableNamespace.NOTIFICACION, TipoDatoVariableDocumento.FECHA_HORA, false, "20/04/2024 09:00", "notificacion.fechaEnvio")
        );
        this.variables = lista.stream()
                .collect(Collectors.toUnmodifiableMap(DocumentoVariableDefinicion::nombre, Function.identity()));
    }

    public Optional<DocumentoVariableDefinicion> buscarPorNombre(String nombre) {
        return Optional.ofNullable(variables.get(nombre));
    }

    public boolean estaRegistrada(String nombre) {
        return variables.containsKey(nombre);
    }

    public boolean esRequerida(String nombre) {
        return variables.containsKey(nombre) && variables.get(nombre).requerida();
    }

    public List<DocumentoVariableDefinicion> listar() {
        return List.copyOf(variables.values());
    }

    private static DocumentoVariableDefinicion def(
            String nombre, DocumentoVariableNamespace ns, TipoDatoVariableDocumento tipo,
            boolean requerida, String ejemplo, String path) {
        return new DocumentoVariableDefinicion(nombre, nombre, ns, tipo, requerida, ejemplo, path);
    }
}
