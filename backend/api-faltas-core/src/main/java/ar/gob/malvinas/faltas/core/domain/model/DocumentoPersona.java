package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.TipoDocumentoPersona;

import java.util.Locale;

/**
 * Forma canonica del documento de identidad de una persona.
 *
 * <p>Es la unica autoridad de normalizacion documental del dominio
 * ({@code DECISION_DDL-PERS-01}). Toda entrada de documento pasa por
 * {@link #normalizar(TipoDocumentoPersona, String)} antes de llegar a
 * {@link FalPersona}, de modo que lo que se persiste y lo que se busca tienen
 * exactamente la misma forma.
 *
 * <p>Correspondencia fisica con {@code fal_persona}:
 * <pre>
 * tipoDoc            -&gt; tipo_documento      SMALLINT NULL
 * nroDoc             -&gt; nro_doc             VARCHAR(12) NULL
 * prefijoCuitCuil    -&gt; prefijo_cuit_cuil   TINYINT UNSIGNED NULL
 * digitoVerificador  -&gt; digito_verificador  TINYINT UNSIGNED NULL
 * </pre>
 *
 * <p>El documento es opcional en conjunto: o los cuatro componentes son nulos, o
 * {@code tipoDoc} y {@code nroDoc} estan informados. La persona sin documento es un
 * estado modelado y valido, exigido por
 * {@code SituacionIdentificacionInfractor.IDENTIFICADO_PARCIAL}.
 *
 * <p>CUIT y CUIL se almacenan descompuestos y nunca como los once digitos completos
 * en {@code nroDoc}. Un CUIT/CUIL estructuralmente invalido se rechaza: no se degrada
 * a DNI, no se inventa prefijo ni digito y no se rellena ningun componente. La
 * verificacion aritmetica del digito por modulo 11 no se exige todavia
 * ({@code D-PERS-CUIT-DV-01}).
 *
 * <p>Los mensajes de error describen la longitud y la clase de caracteres, nunca el
 * valor recibido: un documento es un dato personal y no viaja a logs ni a respuestas
 * de error.
 */
public record DocumentoPersona(
        TipoDocumentoPersona tipoDoc,
        String nroDoc,
        Integer prefijoCuitCuil,
        Integer digitoVerificador) {

    /** Ancho fisico de {@code nro_doc}. */
    public static final int LONGITUD_MAXIMA_NRO_DOC = 12;

    /** Cuerpo central del CUIT/CUIL: ancho fijo, los ceros iniciales son significativos. */
    public static final int LONGITUD_CUERPO_CUIT_CUIL = 8;

    /** Los once digitos del CUIT/CUIL: prefijo + cuerpo + digito verificador. */
    public static final int LONGITUD_CUIT_CUIL_COMPLETO = 11;

    /** Limite del DNI, heredado del CHECK numerico del esquema ejecutado. */
    public static final int LONGITUD_MAXIMA_DNI = 8;

    /** El prefijo tiene dos digitos significativos: es lo que permite componer doc_key sin relleno. */
    public static final int PREFIJO_CUIT_CUIL_MINIMO = 10;
    public static final int PREFIJO_CUIT_CUIL_MAXIMO = 99;

    private static final String SEPARADORES_SIN_VALOR_DE_IDENTIDAD = "[\\s.\\-/]";

    private static final DocumentoPersona AUSENTE = new DocumentoPersona(null, null, null, null);

    public DocumentoPersona {
        if ((tipoDoc == null) != (nroDoc == null)) {
            throw new IllegalArgumentException("tipoDoc y nroDoc deben informarse juntos o ambos null");
        }
        if (tipoDoc == null) {
            if (prefijoCuitCuil != null || digitoVerificador != null) {
                throw new IllegalArgumentException(
                        "Sin documento, prefijoCuitCuil y digitoVerificador deben ser null");
            }
        } else if (esCuitCuil(tipoDoc)) {
            validarCuitCuil(tipoDoc, nroDoc, prefijoCuitCuil, digitoVerificador);
        } else {
            validarNoCuitCuil(tipoDoc, nroDoc, prefijoCuitCuil, digitoVerificador);
        }
    }

    /** Documento ausente: los cuatro componentes nulos. */
    public static DocumentoPersona ausente() {
        return AUSENTE;
    }

    /**
     * Normaliza un valor recibido a su forma canonica segun el tipo documental.
     *
     * @param tipoDoc       tipo documental, o {@code null} si no se conoce
     * @param valorRecibido documento tal como lo aporto el llamador, con o sin separadores
     * @return la forma canonica, o {@link #ausente()} si no hay documento
     * @throws IllegalArgumentException si el par esta incompleto o el valor no es canonizable
     */
    public static DocumentoPersona normalizar(TipoDocumentoPersona tipoDoc, String valorRecibido) {
        boolean sinTipo = tipoDoc == null;
        boolean sinValor = valorRecibido == null || valorRecibido.isBlank();
        if (sinTipo && sinValor) {
            return AUSENTE;
        }
        if (sinTipo || sinValor) {
            throw new IllegalArgumentException("tipoDoc y nroDoc deben informarse juntos o ambos null");
        }

        String limpio = limpiar(valorRecibido);
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("nroDoc no puede ser vacio");
        }
        if (esCuitCuil(tipoDoc)) {
            return normalizarCuitCuil(tipoDoc, limpio);
        }
        if (tipoDoc == TipoDocumentoPersona.DNI) {
            return normalizarDni(tipoDoc, limpio);
        }
        return normalizarAlfanumerico(tipoDoc, limpio);
    }

    /** CUIT y CUIL, identificados por constante del enum y nunca por ordinal ni por codigo literal. */
    public static boolean esCuitCuil(TipoDocumentoPersona tipoDoc) {
        return tipoDoc == TipoDocumentoPersona.CUIT || tipoDoc == TipoDocumentoPersona.CUIL;
    }

    public boolean esCuitCuil() {
        return esCuitCuil(tipoDoc);
    }

    public boolean estaAusente() {
        return tipoDoc == null;
    }

    /**
     * Forma visible del documento. Para CUIT/CUIL reconstruye {@code XX-XXXXXXXX-X} desde
     * los tres componentes; el formato visual no se almacena.
     */
    public String nroDocVisible() {
        if (nroDoc == null) {
            return null;
        }
        if (esCuitCuil()) {
            return String.format("%02d-%s-%d", prefijoCuitCuil, nroDoc, digitoVerificador);
        }
        return nroDoc;
    }

    // --- Normalizacion por tipo ---

    private static String limpiar(String valor) {
        return valor.strip()
                .replaceAll(SEPARADORES_SIN_VALOR_DE_IDENTIDAD, "")
                .toUpperCase(Locale.ROOT);
    }

    private static DocumentoPersona normalizarDni(TipoDocumentoPersona tipoDoc, String limpio) {
        exigirSoloDigitos(tipoDoc, limpio);
        String sinCerosIniciales = limpio.replaceFirst("^0+", "");
        if (sinCerosIniciales.isEmpty()) {
            throw new IllegalArgumentException("DNI no puede ser cero");
        }
        return new DocumentoPersona(tipoDoc, sinCerosIniciales, null, null);
    }

    private static DocumentoPersona normalizarCuitCuil(TipoDocumentoPersona tipoDoc, String limpio) {
        exigirSoloDigitos(tipoDoc, limpio);
        if (limpio.length() != LONGITUD_CUIT_CUIL_COMPLETO) {
            throw new IllegalArgumentException(tipoDoc.name() + " requiere exactamente "
                    + LONGITUD_CUIT_CUIL_COMPLETO + " digitos; " + descripcionSegura(limpio));
        }
        return new DocumentoPersona(
                tipoDoc,
                limpio.substring(2, 2 + LONGITUD_CUERPO_CUIT_CUIL),
                Integer.valueOf(limpio.substring(0, 2)),
                Integer.valueOf(limpio.substring(2 + LONGITUD_CUERPO_CUIT_CUIL)));
    }

    private static DocumentoPersona normalizarAlfanumerico(TipoDocumentoPersona tipoDoc, String limpio) {
        if (!limpio.matches("[0-9A-Z]+")) {
            throw new IllegalArgumentException(tipoDoc.name()
                    + " admite solo digitos y letras sin acentos; " + descripcionSegura(limpio));
        }
        return new DocumentoPersona(tipoDoc, limpio, null, null);
    }

    private static void exigirSoloDigitos(TipoDocumentoPersona tipoDoc, String limpio) {
        if (!limpio.matches("[0-9]+")) {
            throw new IllegalArgumentException(tipoDoc.name()
                    + " admite solo digitos; " + descripcionSegura(limpio));
        }
    }

    // --- Invariantes ---

    private static void validarCuitCuil(TipoDocumentoPersona tipoDoc, String nroDoc,
                                        Integer prefijo, Integer digito) {
        if (prefijo == null || digito == null) {
            throw new IllegalArgumentException(tipoDoc.name()
                    + " requiere prefijoCuitCuil y digitoVerificador informados");
        }
        if (!nroDoc.matches("[0-9]{" + LONGITUD_CUERPO_CUIT_CUIL + "}")) {
            throw new IllegalArgumentException(tipoDoc.name() + " requiere un cuerpo central de "
                    + LONGITUD_CUERPO_CUIT_CUIL + " digitos; " + descripcionSegura(nroDoc));
        }
        if (prefijo < PREFIJO_CUIT_CUIL_MINIMO || prefijo > PREFIJO_CUIT_CUIL_MAXIMO) {
            throw new IllegalArgumentException("prefijoCuitCuil debe estar entre "
                    + PREFIJO_CUIT_CUIL_MINIMO + " y " + PREFIJO_CUIT_CUIL_MAXIMO);
        }
        if (digito < 0 || digito > 9) {
            throw new IllegalArgumentException("digitoVerificador debe estar entre 0 y 9");
        }
    }

    private static void validarNoCuitCuil(TipoDocumentoPersona tipoDoc, String nroDoc,
                                          Integer prefijo, Integer digito) {
        if (prefijo != null || digito != null) {
            throw new IllegalArgumentException("prefijoCuitCuil y digitoVerificador solo aplican a "
                    + TipoDocumentoPersona.CUIT.name() + " y " + TipoDocumentoPersona.CUIL.name()
                    + "; tipo recibido: " + tipoDoc.name());
        }
        if (!nroDoc.matches("[0-9A-Z]{1," + LONGITUD_MAXIMA_NRO_DOC + "}")) {
            throw new IllegalArgumentException("nroDoc canonico admite 1 a "
                    + LONGITUD_MAXIMA_NRO_DOC + " digitos o letras mayusculas; "
                    + descripcionSegura(nroDoc));
        }
        if (tipoDoc == TipoDocumentoPersona.DNI
                && !nroDoc.matches("[1-9][0-9]{0," + (LONGITUD_MAXIMA_DNI - 1) + "}")) {
            throw new IllegalArgumentException("DNI canonico admite 1 a " + LONGITUD_MAXIMA_DNI
                    + " digitos sin ceros iniciales; " + descripcionSegura(nroDoc));
        }
    }

    /** Describe un valor rechazado sin reproducirlo: un documento es un dato personal. */
    private static String descripcionSegura(String valor) {
        return "longitud recibida: " + valor.length();
    }
}
