package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.service.TalonarioElectronicoDevSeeder;
import ar.gob.malvinas.faltas.core.web.dto.SeedLocalDevResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint local/dev del seeder de talonario ACTA ELECTRONICO (Wave 4 Parte 3C §11).
 *
 * <p>{@code POST /demo/dev/seed-talonario-electronico}. Solo disponible cuando
 * {@code faltas.seed.local-dev.enabled=true} (default {@code false}, fail-closed: sin la
 * property, 404). Requiere ademas {@code app.persistence.mode=jdbc}: si el contexto corre en
 * memoria, {@link TalonarioElectronicoDevSeeder} rechaza ejecutar (ver su Javadoc).
 *
 * <p>No es un endpoint productivo. No borra ni corrige datos existentes: solo asegura, sin
 * duplicar, el minimo necesario para que el alta integral pueda numerar en un ambiente local/dev
 * recien provisto.
 *
 * <p>{@code @Profile({"local", "dev"})} (Wave 4 Parte 3E §15): fuera de esos dos perfiles este
 * controller no se registra, sin importar el valor de {@code faltas.seed.local-dev.enabled}.
 * Es un segundo gate estructural, independiente de la property.
 */
@RestController
@RequestMapping("/demo")
@Profile({"local", "dev"})
public class SeedLocalDevController {

    @Value("${faltas.seed.local-dev.enabled:false}")
    private boolean seedEnabled;

    private final TalonarioElectronicoDevSeeder seeder;

    public SeedLocalDevController(TalonarioElectronicoDevSeeder seeder) {
        this.seeder = seeder;
    }

    @PostMapping("/dev/seed-talonario-electronico")
    public ResponseEntity<SeedLocalDevResponse> seedTalonarioElectronico() {
        if (!seedEnabled) {
            return ResponseEntity.notFound().build();
        }
        TalonarioElectronicoDevSeeder.ResultadoSeederLocalDev resultado = seeder.asegurar();
        List<String> pasos = resultado.pasos().stream()
                .map(p -> p.estado() + " " + p.entidad() + " id=" + p.id())
                .toList();
        return ResponseEntity.ok(new SeedLocalDevResponse(
                resultado.idDependencia(), resultado.idInspector(), resultado.idTalonario(), pasos));
    }
}
