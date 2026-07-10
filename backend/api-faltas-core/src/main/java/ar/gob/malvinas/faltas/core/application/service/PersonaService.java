package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.SujBieEstado;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocumentoPersona;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPersona;
import ar.gob.malvinas.faltas.core.domain.exception.PersonaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.model.FalPersona;
import ar.gob.malvinas.faltas.core.repository.PersonaRepository;
import org.springframework.stereotype.Service;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Casos de uso de gestion de personas.
 *
 * Invariantes que este servicio hace cumplir:
 * - FISICA: razonSocial debe ser null.
 * - JURIDICA: apellido y nombres deben ser null.
 * - tipoDoc y nroDoc se informan juntos o ambos null.
 * - nroDoc no puede ser vacio ni mayor a 20 caracteres.
 * - nombreMostrar max 64 caracteres.
 * - emailPrincipal max 160 caracteres.
 * - telefonoPrincipal max 20 caracteres.
 * - Para Faltas, idSuj debe ser 20 cuando se informa.
 * - idBie no puede existir sin idSuj.
 * - ACTIVA: idSuj=20, idBie!=null, fhSujBieCreacion!=null.
 * - SIN_CUENTA e inexistente: idSuj=null, idBie=null, fhSujBieCreacion=null.
 */
@Service
public class PersonaService {

    private final PersonaRepository personaRepository;
    private final FaltasClock faltasClock;

    public PersonaService(PersonaRepository personaRepository,
            FaltasClock faltasClock) {
        this.faltasClock = faltasClock;
        this.personaRepository = personaRepository;
    }

    /**
     * Crea una persona nueva con validaciones de dominio.
     * Calcula nombreMostrar si no se informa uno valido.
     */
    public FalPersona crear(
            TipoPersona tipoPersona,
            TipoDocumentoPersona tipoDoc,
            String nroDoc,
            String apellido,
            String nombres,
            String razonSocial,
            String nombreMostrar,
            String emailPrincipal,
            String telefonoPrincipal,
            String idUserAlta) {

        LocalDateTime ahora = faltasClock.now();
        Long id = personaRepository.nextId();

        FalPersona persona = new FalPersona(id, tipoPersona, ahora, idUserAlta);

        aplicarDocumento(persona, tipoDoc, nroDoc);
        aplicarDatosTipo(persona, tipoPersona, apellido, nombres, razonSocial);
        aplicarNombreMostrar(persona, nombreMostrar);
        aplicarContacto(persona, emailPrincipal, telefonoPrincipal);

        validarIdentificacion(persona);

        return personaRepository.guardar(persona);
    }

    /**
     * Crea una persona minima a partir de un numero de documento (sin tipo) y nombre opcional.
     * Para compatibilidad con flujos legacy que aun no informan idPersonaInfractor.
     */
    public FalPersona crearMinimal(String nroDoc, String nombreMostrar, String idUserAlta) {
        LocalDateTime ahora = faltasClock.now();
        Long id = personaRepository.nextId();
        FalPersona persona = new FalPersona(id, TipoPersona.FISICA, ahora, idUserAlta);
        if (nroDoc != null && !nroDoc.isBlank()) {
            String nroDocNorm = nroDoc.strip().replaceAll("[^0-9a-zA-Z\\-]", "");
            if (nroDocNorm.length() > 20) nroDocNorm = nroDocNorm.substring(0, 20);
            persona.setNroDoc(nroDocNorm.isBlank() ? null : nroDocNorm);
        }
        if (nombreMostrar != null && !nombreMostrar.isBlank()) {
            String nm = nombreMostrar.strip();
            persona.setNombreMostrar(nm.length() > 64 ? nm.substring(0, 64) : nm);
        }
        return personaRepository.guardar(persona);
    }

    public FalPersona obtener(Long id) {
        return personaRepository.buscarPorId(id)
                .orElseThrow(() -> new PersonaNoEncontradaException(id));
    }

    public Optional<FalPersona> buscarPorId(Long id) {
        return personaRepository.buscarPorId(id);
    }

    public List<FalPersona> buscarPorDocumento(TipoDocumentoPersona tipoDoc, String nroDoc) {
        return personaRepository.buscarPorTipoDocYNroDoc(tipoDoc, nroDoc);
    }

    /**
     * Modifica datos personales.
     * Recalcula nombreMostrar si cambio alguno de los datos estructurados.
     */
    public FalPersona modificar(
            Long id,
            TipoDocumentoPersona tipoDoc,
            String nroDoc,
            String apellido,
            String nombres,
            String razonSocial,
            String nombreMostrar,
            String emailPrincipal,
            String telefonoPrincipal,
            String idUserMod) {

        FalPersona persona = obtener(id);
        aplicarDocumento(persona, tipoDoc, nroDoc);
        aplicarDatosTipo(persona, persona.getTipoPersona(), apellido, nombres, razonSocial);
        aplicarNombreMostrar(persona, nombreMostrar);
        aplicarContacto(persona, emailPrincipal, telefonoPrincipal);
        validarIdentificacion(persona);

        persona.setFhUltMod(faltasClock.now());
        persona.setIdUserUltMod(idUserMod);

        return personaRepository.guardar(persona);
    }

    /**
     * Actualiza el vinculo con Ingresos.
     * Valida invariantes de Id_Suj / Id_Bie segun SujBieEstado.
     */
    public FalPersona actualizarVinculoIngresos(
            Long id,
            SujBieEstado nuevoEstado,
            Long idSuj,
            Long idBie,
            LocalDateTime fhSujBieCreacion,
            String idUserMod) {

        FalPersona persona = obtener(id);

        if (nuevoEstado == null) {
            if (idSuj != null || idBie != null || fhSujBieCreacion != null)
                throw new IllegalArgumentException("Si sujBieEstado es null, idSuj/idBie/fhSujBieCreacion deben ser null");
        } else {
            switch (nuevoEstado) {
                case SIN_CUENTA:
                    if (idSuj != null || idBie != null || fhSujBieCreacion != null)
                        throw new IllegalArgumentException("SIN_CUENTA no admite idSuj, idBie ni fhSujBieCreacion");
                    break;
                case ACTIVA:
                    if (!Long.valueOf(20).equals(idSuj))
                        throw new IllegalArgumentException("ACTIVA requiere idSuj=20");
                    if (idBie == null)
                        throw new IllegalArgumentException("ACTIVA requiere idBie");
                    if (fhSujBieCreacion == null)
                        throw new IllegalArgumentException("ACTIVA requiere fhSujBieCreacion");
                    break;
                case PENDIENTE_CREACION:
                    if (fhSujBieCreacion != null)
                        throw new IllegalArgumentException("PENDIENTE_CREACION no debe tener fhSujBieCreacion definitiva");
                    break;
                default:
                    break;
            }
        }

        if (idBie != null && idSuj == null)
            throw new IllegalArgumentException("idBie no puede existir sin idSuj");
        if (idSuj != null && !Long.valueOf(20).equals(idSuj))
            throw new IllegalArgumentException("Para Faltas, idSuj debe ser 20");

        persona.setSujBieEstado(nuevoEstado);
        persona.setIdSuj(idSuj);
        persona.setIdBie(idBie);
        persona.setFhSujBieCreacion(fhSujBieCreacion);
        persona.setFhUltMod(faltasClock.now());
        persona.setIdUserUltMod(idUserMod);

        return personaRepository.guardar(persona);
    }

    // --- Helpers privados ---

    private void aplicarDocumento(FalPersona persona, TipoDocumentoPersona tipoDoc, String nroDoc) {
        if (tipoDoc == null && nroDoc == null) {
            persona.setTipoDoc(null);
            persona.setNroDoc(null);
            return;
        }
        if (tipoDoc == null || nroDoc == null)
            throw new IllegalArgumentException("tipoDoc y nroDoc deben informarse juntos o ambos null");
        String docNorm = nroDoc.strip().replaceAll("\\s+", "");
        if (docNorm.isBlank()) throw new IllegalArgumentException("nroDoc no puede ser vacio");
        if (docNorm.length() > 20) throw new IllegalArgumentException("nroDoc supera 20 caracteres");
        persona.setTipoDoc(tipoDoc);
        persona.setNroDoc(docNorm);
    }

    private void aplicarDatosTipo(FalPersona persona, TipoPersona tipo, String apellido, String nombres, String razonSocial) {
        if (tipo == TipoPersona.FISICA) {
            if (razonSocial != null)
                throw new IllegalArgumentException("FISICA no debe tener razonSocial");
            persona.setApellido(apellido);
            persona.setNombres(nombres);
            persona.setRazonSocial(null);
        } else {
            if (apellido != null || nombres != null)
                throw new IllegalArgumentException("JURIDICA no debe tener apellido ni nombres");
            persona.setApellido(null);
            persona.setNombres(null);
            persona.setRazonSocial(razonSocial);
        }
    }

    private void aplicarNombreMostrar(FalPersona persona, String nombreMostrar) {
        String calculado = persona.calcularNombreMostrar();
        String nm = (calculado != null) ? calculado : nombreMostrar;
        if (nm != null) {
            nm = nm.strip();
            if (nm.length() > 64) nm = nm.substring(0, 64);
            persona.setNombreMostrar(nm.isBlank() ? null : nm);
        } else {
            if (nombreMostrar != null && !nombreMostrar.isBlank()) {
                String raw = nombreMostrar.strip();
                persona.setNombreMostrar(raw.length() > 64 ? raw.substring(0, 64) : raw);
            }
        }
    }

    private void aplicarContacto(FalPersona persona, String email, String telefono) {
        if (email != null) {
            String e = email.strip();
            if (e.length() > 160) throw new IllegalArgumentException("emailPrincipal supera 160 caracteres");
            persona.setEmailPrincipal(e.isBlank() ? null : e);
        }
        if (telefono != null) {
            String t = telefono.strip();
            if (t.length() > 20) throw new IllegalArgumentException("telefonoPrincipal supera 20 caracteres");
            persona.setTelefonoPrincipal(t.isBlank() ? null : t);
        }
    }

    private void validarIdentificacion(FalPersona persona) {
        boolean tieneId = persona.getNombreMostrar() != null
                || persona.getApellido() != null
                || persona.getNombres() != null
                || persona.getRazonSocial() != null
                || persona.getNroDoc() != null
                || persona.getIdSuj() != null;
        if (!tieneId)
            throw new IllegalArgumentException("La persona debe tener al menos una forma de identificacion");
    }
}
