package ar.gob.malvinas.faltas.core.infrastructure.security;

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

    public SecurityConfig(JwtActorFilter jwtActorFilter) {
        this.jwtActorFilter = jwtActorFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/faltas/pagos/**").authenticated()
                        .requestMatchers("/api/faltas/documentos/*/numerar").authenticated()
                        .requestMatchers("/api/faltas/documentos/*/firmar-real").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/faltas/actas/labrar").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/faltas/actas/*/completar-captura").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/faltas/actas/*/enriquecer").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/faltas/actas/*/notificaciones/enviar").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/faltas/notificaciones/*/positiva").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/faltas/actas/*/firmeza/vencer-plazo-apelacion").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/faltas/actas/*/firmeza/por-apelacion-rechazada").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/faltas/actas/*/pago-condena/informar").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        (req, res, ex) -> res.sendError(HttpStatus.UNAUTHORIZED.value(), "Autenticacion Bearer requerida")))
                .addFilterBefore(jwtActorFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
