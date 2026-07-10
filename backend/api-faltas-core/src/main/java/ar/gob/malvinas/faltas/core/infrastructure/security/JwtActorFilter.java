package ar.gob.malvinas.faltas.core.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class JwtActorFilter extends OncePerRequestFilter {

    static final String RUTA_PROTEGIDA = "/api/faltas/pagos";
    static final String SUFIJO_NUMERAR_DOCU = "/numerar";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
            String sub = null;
            if (auth != null && auth.startsWith("Bearer ")) {
                sub = extractSub(auth.substring(7).trim());
                if (sub == null || sub.isBlank() || sub.length() > 36) {
                    response.sendError(HttpStatus.UNAUTHORIZED.value(), "Bearer token invalido o sub ausente");
                    return;
                }
                ActorContextHolder.set(new ActorContext(sub));
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(sub, null, java.util.List.of()));
            }
            if (sub == null && esRutaProtegida(request)) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Autenticacion Bearer requerida");
                return;
            }
            chain.doFilter(request, response);
        } finally {
            ActorContextHolder.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private static boolean esRutaProtegida(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) return false;
        if (uri.startsWith(RUTA_PROTEGIDA)) return true;
        // Protege POST /api/faltas/documentos/{id}/numerar para integracion con Firmas
        if (uri.endsWith(SUFIJO_NUMERAR_DOCU)) return true;
        return false;
    }

    static String extractSub(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) return null;
            String payload = new String(Base64.getUrlDecoder().decode(pad(parts[1])), StandardCharsets.UTF_8);
            int idx = payload.indexOf("\"sub\"");
            if (idx < 0) return null;
            int colon = payload.indexOf(':', idx);
            int q1 = payload.indexOf('"', colon + 1);
            int q2 = payload.indexOf('"', q1 + 1);
            if (q1 < 0 || q2 < 0) return null;
            return payload.substring(q1 + 1, q2);
        } catch (Exception e) {
            return null;
        }
    }

    private static String pad(String b64) {
        int mod = b64.length() % 4;
        if (mod == 0) return b64;
        return b64 + "====".substring(mod);
    }
}