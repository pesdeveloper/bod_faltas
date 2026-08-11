package ar.gob.malvinas.faltas.core.application.demo;

import ar.gob.malvinas.faltas.core.domain.model.FalMotivoArchivo;
import ar.gob.malvinas.faltas.core.repository.MotivoArchivoRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Seed minimo del catalogo de motivos de archivo.
 * Slice 8F-11G.
 *
 * Motivos sembrados:
 *   PRESCRIP - prescripto sin cierre formal; permite reingreso=false; nulidad=false
 *   DESISTIM - infractor desiste; permite reingreso=false; nulidad=false
 *   ERRFORM  - error en el acta; permite reingreso=true; nulidad=false
 *   NULPROC  - nulidad juridica declarada; permite reingreso=false; nulidad=true
 *   ESPDOC   - documentacion pendiente; permite reingreso=true; nulidad=false
 *
 * Los codigos respetan el limite fisico de fal_motivo_archivo.cod_motivo_archivo
 * (VARCHAR(8)).
 *
 * Con el perfil jdbc este seeder no se registra: es dataset mock de demo y no debe
 * escribir en la base real.
 */
@Component
@Profile("!jdbc")
public class MotivoArchivoMockSeeder {

    static final LocalDateTime FH_ALTA = LocalDateTime.of(2024, 1, 1, 0, 0);
    static final String USER = "sistema";

    private final MotivoArchivoRepository repo;

    public MotivoArchivoMockSeeder(MotivoArchivoRepository repo) {
        this.repo = repo;
    }

    @PostConstruct
    public void seed() {
        sembrar("PRESCRIP", "Prescripcion",            false, false, false);
        sembrar("DESISTIM", "Desistimiento",           false, false, false);
        sembrar("ERRFORM",  "Error formal en el acta", false, true,  true);
        sembrar("NULPROC",  "Nulidad procesal",        true,  false, true);
        sembrar("ESPDOC",   "Espera de documentacion", false, true,  false);
    }

    private void sembrar(String cod, String nombre,
                         boolean nulidad, boolean reingreso, boolean reqObs) {
        if (repo.buscarPorCodigo(cod).isPresent()) return;
        FalMotivoArchivo m = new FalMotivoArchivo(null, cod, nombre,
                nulidad, reingreso, reqObs, true, FH_ALTA, USER);
        repo.guardar(m);
    }
}
