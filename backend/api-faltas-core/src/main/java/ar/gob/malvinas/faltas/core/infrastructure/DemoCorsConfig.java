package ar.gob.malvinas.faltas.core.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuracion CORS minima para endpoints demo y API.
 *
 * Habilitada para permitir que un frontend Angular en modo dev
 * (tipicamente localhost:4200) consuma la API sin bloqueos del navegador.
 *
 * Configurable via: faltas.demo.cors.allowed-origins (default: *)
 *
 * No es seguridad de produccion.
 * Configurar origins reales en ambientes no-dev.
 *
 * Slice 8F-6.
 */
@Configuration
public class DemoCorsConfig implements WebMvcConfigurer {

    @Value("${faltas.demo.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/demo/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);

        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}