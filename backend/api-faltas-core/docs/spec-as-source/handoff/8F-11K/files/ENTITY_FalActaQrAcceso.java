package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.CanalAccesoQr;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoAccesoQr;

import java.time.LocalDateTime;

/**
 * Registro de auditoria de un acceso valido realizado via codigo QR.
 *
 * Tabla: fal_acta_qr_acceso.
 * Semantica append-only: solo se inserta un registro si el token fue valido y
 * el acta pudo ser resuelta. Tokens invalidos, corruptos, con scope incorrecto,
 * actas inexistentes u otros errores NO producen filas en esta tabla.
 *
 * No almacena el token, el payload ni el hash del token.
 * No almacena datos personales del accedente.
 * IP y user-agent son tecnicos, no ciudadanos.
 */
public class FalActaQrAcceso {

    private static final int MAX_IP = 45;
    private static final int MAX_USER_AGENT = 255;

    private final Long id;
    private final Long actaId;
    private final LocalDateTime fhAcceso;
    private final CanalAccesoQr canalAcceso;
    private final String ipOrigen;
    private final String userAgent;
    private final ResultadoAccesoQr resultadoAcceso;
    private final LocalDateTime fhAlta;

    public FalActaQrAcceso(
            Long id,
            Long actaId,
            LocalDateTime fhAcceso,
            CanalAccesoQr canalAcceso,
            String ipOrigen,
            String userAgent,
            ResultadoAccesoQr resultadoAcceso,
            LocalDateTime fhAlta) {

        if (id == null) throw new IllegalArgumentException("id no puede ser null");
        if (actaId == null) throw new IllegalArgumentException("actaId no puede ser null");
        if (fhAcceso == null) throw new IllegalArgumentException("fhAcceso no puede ser null");
        if (canalAcceso == null) throw new IllegalArgumentException("canalAcceso no puede ser null");
        if (resultadoAcceso == null) throw new IllegalArgumentException("resultadoAcceso no puede ser null");
        if (fhAlta == null) throw new IllegalArgumentException("fhAlta no puede ser null");

        String ipValidada = validarIp(ipOrigen);
        String uaValidado = sanitizarUserAgent(userAgent);

        this.id = id;
        this.actaId = actaId;
        this.fhAcceso = fhAcceso;
        this.canalAcceso = canalAcceso;
        this.ipOrigen = ipValidada;
        this.userAgent = uaValidado;
        this.resultadoAcceso = resultadoAcceso;
        this.fhAlta = fhAlta;
    }

    private static String validarIp(String ip) {
        if (ip == null || ip.isBlank()) return null;
        if (ip.length() > MAX_IP)
            throw new IllegalArgumentException("ipOrigen excede " + MAX_IP + " caracteres: longitud=" + ip.length());
        String ipTrim = ip.trim();
        try {
            java.net.InetAddress.getByName(ipTrim);
        } catch (java.net.UnknownHostException e) {
            throw new IllegalArgumentException("ipOrigen no es una IP valida IPv4/IPv6: " + ipTrim);
        }
        return ipTrim;
    }

    private static String sanitizarUserAgent(String ua) {
        if (ua == null || ua.isBlank()) return null;
        String sanitizado = ua.replaceAll("[\\x00-\\x08\\x0b\\x0c\\x0e-\\x1f\\x7f]", "").trim();
        if (sanitizado.length() > MAX_USER_AGENT) {
            throw new IllegalArgumentException(
                    "userAgent sanitizado excede " + MAX_USER_AGENT + " caracteres: longitud=" + sanitizado.length()
                    + ". Truncar antes de invocar.");
        }
        return sanitizado.isEmpty() ? null : sanitizado;
    }

    public Long getId() { return id; }
    public Long getActaId() { return actaId; }
    public LocalDateTime getFhAcceso() { return fhAcceso; }
    public CanalAccesoQr getCanalAcceso() { return canalAcceso; }
    public String getIpOrigen() { return ipOrigen; }
    public String getUserAgent() { return userAgent; }
    public ResultadoAccesoQr getResultadoAcceso() { return resultadoAcceso; }
    public LocalDateTime getFhAlta() { return fhAlta; }

    public FalActaQrAcceso copia() {
        return new FalActaQrAcceso(id, actaId, fhAcceso, canalAcceso,
                ipOrigen, userAgent, resultadoAcceso, fhAlta);
    }
}
