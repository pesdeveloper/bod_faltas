package ar.gob.malvinas.faltas.core.infrastructure.security;

import ar.gob.malvinas.faltas.core.web.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtActorFilter jwtActorFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtActorFilter jwtActorFilter, ObjectMapper objectMapper) {
        this.jwtActorFilter = jwtActorFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ── Geo catalogos publicos (6 GET sin autenticacion) ──────────────────
                        .requestMatchers(HttpMethod.GET, "/api/geo/provincias").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/geo/unidades-territoriales").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/geo/localidades").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/geo/calles").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/geo/local/malvinas/localidades").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/geo/local/malvinas/calles").permitAll()
                        // ── Parcela geometria (provisional, solo geometria GeoJSON) ───────────
                        .requestMatchers(HttpMethod.GET, "/api/geo/local/malvinas/parcela/geom").permitAll()
                        // ── Portal ciudadano QR (token protegido por AES-GCM, no JWT) ─────────
                        .requestMatchers(HttpMethod.GET, "/portal/actas/qr/**").permitAll()
                        // ── Health demo: readiness del frontend (UP/DOWN, sin datos operativos)
                        //    Solo existe cuando faltas.demo.enabled=true. Sin wildcard en /demo/**
                        .requestMatchers(HttpMethod.GET, "/demo/health").permitAll()
                        // ── Parcela detalle: authenticated() hasta decision humana explicita ──
                        .requestMatchers(HttpMethod.GET, "/api/geo/local/malvinas/parcela/detalle").authenticated()
                        // ── Geocodificacion (4 POST Geo): requieren actor para auditoria ──────
                        .requestMatchers(HttpMethod.POST, "/api/geo/geocodificar").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/geo/reverse").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/geo/geocodificar-esquina").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/geo/geocodificar-entre-calles").authenticated()
                        // ── Operaciones de dominio Faltas (explicitamente autenticadas) ───────
                        .requestMatchers("/api/faltas/pagos/**").authenticated()
                        .requestMatchers("/api/faltas/documentos/*/numerar").authenticated()
                        .requestMatchers("/api/faltas/documentos/*/firmar-real").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/faltas/actas/labrar").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/faltas/actas/*/enriquecer").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/faltas/actas/*/notificaciones/enviar").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/faltas/notificaciones/*/positiva").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/faltas/actas/*/firmeza/vencer-plazo-apelacion").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/faltas/actas/*/firmeza/por-apelacion-rechazada").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/faltas/actas/*/pago-condena/informar").authenticated()
                        // ── Default: todo lo no listado exige autenticacion ──────────────────
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(HttpStatus.UNAUTHORIZED.value());
                    res.setContentType("application/json;charset=UTF-8");
                    objectMapper.writeValue(res.getWriter(),
                            ErrorResponse.of("ACTOR_NO_AUTENTICADO", "Autenticacion Bearer requerida"));
                }))
                .addFilterBefore(jwtActorFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
