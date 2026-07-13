package ar.gob.malvinas.faltas.core.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de autenticacion Bearer JWT.
 *
 * Valida criptograficamente el token usando JwtDecoder (HS256 en desarrollo/test,
 * reemplazable por JWKS/OpenIddict en staging/prod sin modificar este filtro).
 *
 * Flujo:
 *   1. Extraer Bearer del header Authorization.
 *   2. Decodificar y validar con jwtDecoder (firma, alg, exp, nbf, issuer, audience).
 *   3. Extraer sub; rechazar si ausente o vacio.
 *   4. Establecer ActorContext y SecurityContext.
 *   5. Continuar cadena.
 *   6. Limpiar contextos en finally.
 *
 * Cualquier error de validacion JWT devuelve HTTP 401 sin exponer detalle interno.
 * alg=none es rechazado por el decoder.
 *
 * FIX-FALLO-NOTI-01-R2: reemplaza parser manual Base64 por JwtDecoder real.
 */
@Component
public class JwtActorFilter extends OncePerRequestFilter {

    static final String RUTA_PROTEGIDA = "/api/faltas/pagos";
    static final String SUFIJO_NUMERAR_DOCU = "/numerar";
    static final String SUFIJO_FIRMAR_REAL = "/firmar-real";
    static final String SUFIJO_ENVIAR_NOTIF = "/notificaciones/enviar";
    static final String PREFIJO_NOTIF_POSITIVA = "/api/faltas/notificaciones/";
    static final String SUFIJO_NOTIF_POSITIVA = "/positiva";
    static final String PREFIJO_ACTAS = "/api/faltas/actas/";
    static final String SUFIJO_VENCER_PLAZO = "/firmeza/vencer-plazo-apelacion";
    static final String SUFIJO_APELACION_RECHAZADA = "/firmeza/por-apelacion-rechazada";

    private final JwtDecoder jwtDecoder;

    public JwtActorFilter(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
        try {
            String bearer = extractBearer(request);
            if (bearer != null) {
                Jwt jwt;
                try {
                    jwt = jwtDecoder.decode(bearer);
                } catch (JwtException e) {
                    response.sendError(HttpStatus.UNAUTHORIZED.value(), "Bearer token invalido");
                    return;
                }
                String sub = jwt.getSubject();
                if (sub == null || sub.isBlank()) {
                    response.sendError(HttpStatus.UNAUTHORIZED.value(), "Bearer token invalido: sub ausente");
                    return;
                }
                ActorContext ctx;
                try {
                    ctx = new ActorContext(sub);
                } catch (IllegalArgumentException e) {
                    response.sendError(HttpStatus.UNAUTHORIZED.value(), "Bearer token invalido: sub no aceptado");
                    return;
                }
                ActorContextHolder.set(ctx);
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(sub, null, java.util.List.of()));
            }
            if (ActorContextHolder.get() == null && esRutaProtegida(request)) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Autenticacion Bearer requerida");
                return;
            }
            chain.doFilter(request, response);
        } finally {
            ActorContextHolder.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private static String extractBearer(HttpServletRequest request) {
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }

    private static boolean esRutaProtegida(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) return false;
        if (uri.startsWith(RUTA_PROTEGIDA)) return true;
        if (uri.endsWith(SUFIJO_NUMERAR_DOCU)) return true;
        if (uri.endsWith(SUFIJO_FIRMAR_REAL)) return true;
        if (uri.endsWith(SUFIJO_ENVIAR_NOTIF)) return true;
        if (uri.startsWith(PREFIJO_NOTIF_POSITIVA) && uri.endsWith(SUFIJO_NOTIF_POSITIVA)) return true;
        if (uri.startsWith(PREFIJO_ACTAS) && uri.endsWith(SUFIJO_VENCER_PLAZO)
                && "POST".equalsIgnoreCase(request.getMethod())) return true;
        if (uri.startsWith(PREFIJO_ACTAS) && uri.endsWith(SUFIJO_APELACION_RECHAZADA)
                && "POST".equalsIgnoreCase(request.getMethod())) return true;
        return false;
    }
}
