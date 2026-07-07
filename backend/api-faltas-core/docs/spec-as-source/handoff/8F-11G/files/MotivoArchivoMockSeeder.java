package ar.gob.malvinas.faltas.core.application.demo;

import ar.gob.malvinas.faltas.core.domain.model.FalMotivoArchivo;
import ar.gob.malvinas.faltas.core.repository.MotivoArchivoRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Seed minimo del catalogo de motivos de archivo.
 * Slice 8F-11G.
 *
 * Motivos sembrados:
 *   PRESCRIPCION          - prescripto sin cierre formal; permite reingreso=false; nulidad=false
 *   DESISTIMIENTO         - infractor desiste; permite reingreso=false; nulidad=false
 *   ERROR_FORMAL          - error en el acta; permite reingreso=true; nulidad=false
 *   NULIDAD_PROCESAL      - nulidad juridica declarada; permite reingreso=false; nulidad=true
 *   ESPERA_DOCUMENTACION  - documentacion pendiente; permite reingreso=true; nulidad=false
 */
@Component
public class MotivoArchivoMockSeeder {

    static final LocalDateTime FH_ALTA = LocalDateTime.of(2024, 1, 1, 0, 0);
    static final String USER = "sistema";

    private final MotivoArchivoRepository repo;

    public MotivoArchivoMockSeeder(MotivoArchivoRepository repo) {
        this.repo = repo;
    }

    @PostConstruct
    public void seed() {
        sembrar("PRESCRIPCION",        "Prescripcion",                  "Acta prescripta sin cierre formal",
                false, false, false);
        sembrar("DESISTIMIENTO",       "Desistimiento",                 "Infractor desiste del tramite",
                false, false, false);
        sembrar("ERROR_FORMAL",        "Error formal en el acta",       "Error en datos del acta que impide proseguir",
                false, true, true);
        sembrar("NULIDAD_PROCESAL",    "Nulidad procesal",              "Nulidad juridica declarada por defecto procesal",
                true, false, true);
        sembrar("ESPERA_DOCUMENTACION","Espera de documentacion",       "Archivo temporal por documentacion pendiente",
                false, true, false);
    }

    private void sembrar(String cod, String nombre, String desc,
                         boolean nulidad, boolean reingreso, boolean reqObs) {
        if (repo.buscarPorCodigo(cod).isPresent()) return;
        Long id = repo.nextId();
        FalMotivoArchivo m = new FalMotivoArchivo(id, cod, nombre, desc,
                nulidad, reingreso, reqObs, true, FH_ALTA, USER);
        repo.guardar(m);
    }
}