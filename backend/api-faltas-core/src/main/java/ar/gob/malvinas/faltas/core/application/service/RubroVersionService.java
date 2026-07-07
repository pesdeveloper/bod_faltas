package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.exception.RubroVersionNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.model.FalRubroVersion;
import ar.gob.malvinas.faltas.core.repository.RubroVersionRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de sincronizacion y consulta del catalogo de versiones de rubro.
 * Una sola version actual por Id_Rub, gestionada atomicamente.
 */
@Service
public class RubroVersionService {

    private final RubroVersionRepository rubroRepository;

    public RubroVersionService(RubroVersionRepository rubroRepository) {
        this.rubroRepository = rubroRepository;
    }

    /**
     * Sincroniza un rubro desde Informix/Ingresos.
     * Si el hash es identico a la version actual, no crea nueva version (UNCHANGED).
     * Si hay cambio, cierra la version anterior y crea una nueva.
     */
    public FalRubroVersion sincronizar(int idRub, String nombre, short sidesabilitado, String idUser) {
        String hash = calcularHash(idRub, nombre, sidesabilitado);
        LocalDateTime ahora = LocalDateTime.now();
        Long rubroId = rubroRepository.nextId();
        FalRubroVersion nueva = new FalRubroVersion(
                rubroId, idRub, nombre, sidesabilitado, hash, "INSERT", ahora, ahora);
        return rubroRepository.sincronizarAtomicamente(nueva);
    }

    public FalRubroVersion findByRubroId(Long rubroId) {
        return rubroRepository.findByRubroId(rubroId)
                .orElseThrow(() -> new RubroVersionNoEncontradoException(rubroId));
    }

    public Optional<FalRubroVersion> findActualByIdRub(int idRub) {
        return rubroRepository.findActualByIdRub(idRub);
    }

    public List<FalRubroVersion> findAllActualesActivas() {
        return rubroRepository.findAllActualesActivas();
    }

    /** Valida que el rubroId exista (historicamente valido, no necesariamente actual). */
    public FalRubroVersion validarRubroId(Long rubroId) {
        return findByRubroId(rubroId);
    }

    /** Valida coherencia entre rubroId e idRub. */
    public void validarCoherenciaRubroIdExterno(Long rubroId, int idRub) {
        FalRubroVersion version = findByRubroId(rubroId);
        if (version.getIdRub() != idRub)
            throw new IllegalArgumentException(
                    "rubroId=" + rubroId + " no corresponde a Id_Rub=" + idRub +
                    " (esperado Id_Rub=" + version.getIdRub() + ")");
    }

    private String calcularHash(int idRub, String nombre, short sidesabilitado) {
        try {
            String input = idRub + "|" + nombre + "|" + sidesabilitado;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Error calculando hash de rubro", e);
        }
    }
}
