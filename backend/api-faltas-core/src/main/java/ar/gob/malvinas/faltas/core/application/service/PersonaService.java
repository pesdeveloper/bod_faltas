package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.query.CampoOrdenPersona;
import ar.gob.malvinas.faltas.core.application.query.FiltroPersona;
import ar.gob.malvinas.faltas.core.application.query.PaginaResultado;
import ar.gob.malvinas.faltas.core.application.query.PaginaSolicitada;
import ar.gob.malvinas.faltas.core.domain.enums.SujBieEstado;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocumentoPersona;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPersona;
import ar.gob.malvinas.faltas.core.domain.exception.PersonaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.model.DocumentoPersona;
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
 * - el documento se normaliza y valida en DocumentoPersona, unica autoridad del dominio.
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

        FalPersona persona = new FalPersona(null, tipoPersona, ahora, idUserAlta);

        aplicarDocumento(persona, tipoDoc, nroDoc);
        aplicarDatosTipo(persona, tipoPersona, apellido, nombres, razonSocial);
        aplicarNombreMostrar(persona, nombreMostrar);
        aplicarContacto(persona, emailPrincipal, telefonoPrincipal);

        validarIdentificacion(persona);

        return personaRepository.guardar(persona);
    }

    /**
     * Crea una persona minima cuando el llamador no conoce el tipo documental.
     *
     * <p>El texto documental recibido **no** se estructura como documento de
     * `fal_persona`: inferir `DNI` porque el valor parece numerico seria inventar
     * identificacion, y `DECISION_DDL-PERS-01` lo prohibe. El documento queda ausente y
     * el texto original permanece en el agregado que lo recibio.
     *
     * <p>Solo se crea la persona si existe otra forma real de identificacion. Sin ninguna,
     * no hay persona: la ausencia de sujeto identificado se expresa con
     * `id_persona_infractor` NULL, nunca con una fila vacia.
     *
     * @param textoDocumentoSinTipo documento tal como lo aporto el llamador, sin tipo
     *        documental. Deliberadamente no se persiste en Persona: sin tipo no hay
     *        documento canonizable. Quien lo aporta es responsable de conservarlo.
     * @return la persona persistida, u {@code Optional.empty()} si no hay datos suficientes
     */
    public Optional<FalPersona> crearMinimal(String textoDocumentoSinTipo, String nombreMostrar,
            String idUserAlta) {
        LocalDateTime ahora = faltasClock.now();
        FalPersona persona = new FalPersona(null, TipoPersona.FISICA, ahora, idUserAlta);
        if (nombreMostrar != null && !nombreMostrar.isBlank()) {
            persona.setNombreMostrar(recortarNombreMostrar(nombreMostrar.strip()));
        }
        if (!persona.tieneIdentificacionMinima()) {
            return Optional.empty();
        }
        return Optional.of(personaRepository.guardar(persona));
    }

    public FalPersona obtener(Long id) {
        return personaRepository.buscarPorId(id)
                .orElseThrow(() -> new PersonaNoEncontradaException(id));
    }

    public Optional<FalPersona> buscarPorId(Long id) {
        return personaRepository.buscarPorId(id);
    }

    /**
     * Busca por documento normalizando la entrada con la misma funcion canonica que se usa
     * para persistir: no se guarda de una forma y se busca de otra.
     *
     * <p>Un valor que no es canonizable para el tipo indicado no puede coincidir con
     * ninguna fila, porque toda fila persistida guarda la forma canonica. La busqueda
     * devuelve lista vacia en lugar de fallar: es una lectura total, no una validacion de
     * alta. La misma entrada si es rechazada por `crear` y `modificar`.
     */
    public List<FalPersona> buscarPorDocumento(TipoDocumentoPersona tipoDoc, String nroDoc) {
        DocumentoPersona documento;
        try {
            documento = DocumentoPersona.normalizar(tipoDoc, nroDoc);
        } catch (IllegalArgumentException noCanonizable) {
            return List.of();
        }
        return personaRepository.buscarPorTipoDocYNroDoc(documento.tipoDoc(), documento.nroDoc());
    }

    /**
     * Caso de uso de consulta paginada de personas ({@code DECISION-LIST-PAGE-SORT-01}).
     *
     * <p>Es la unica lectura de coleccion de personas que la aplicacion ofrece. No existe una
     * variante ilimitada: {@code PersonaRepository.listarTodas} es una operacion interna del
     * puerto y este servicio no la expone.
     *
     * <p>Argumentos nulos significan "sin filtro" y "primera pagina con los valores por
     * defecto": {@code page = 0}, {@code size = 50} y el orden predeterminado de Persona.
     *
     * <p>A diferencia de {@link #buscarPorDocumento}, el filtro <b>no</b> normaliza el numero de
     * documento: recibe la forma canonica que ya se persiste. Normalizar aca duplicaria la
     * autoridad de {@code DocumentoPersona} dentro de un contrato de consulta, y un filtro que
     * corrige silenciosamente su entrada devolveria una pagina distinta de la pedida sin que el
     * llamador pueda notarlo.
     *
     * @throws ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException
     *         si la pagina o el orden solicitados violan el contrato
     */
    public PaginaResultado<FalPersona> listarPersonas(
            FiltroPersona filtro, PaginaSolicitada<CampoOrdenPersona> pagina) {
        return personaRepository.listarPagina(
                filtro == null ? FiltroPersona.ninguno() : filtro,
                pagina == null ? PaginaSolicitada.primeraPagina() : pagina);
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
            Integer idSuj,
            Integer idBie,
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
                    if (!Integer.valueOf(20).equals(idSuj))
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
        if (idSuj != null && !Integer.valueOf(20).equals(idSuj))
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

    /**
     * Unica via de aplicacion del documento. Delega en {@link DocumentoPersona}, que es la
     * autoridad de normalizacion del dominio: no hay regex de documento en este servicio.
     * Cambiar el documento reemplaza los cuatro componentes, de modo que pasar de CUIT a
     * otro tipo, o quitar el documento, no deja prefijo ni digito residuales.
     */
    private void aplicarDocumento(FalPersona persona, TipoDocumentoPersona tipoDoc, String nroDoc) {
        persona.setDocumento(DocumentoPersona.normalizar(tipoDoc, nroDoc));
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
            persona.setNombreMostrar(nm.isBlank() ? null : recortarNombreMostrar(nm));
        } else {
            if (nombreMostrar != null && !nombreMostrar.isBlank()) {
                persona.setNombreMostrar(recortarNombreMostrar(nombreMostrar.strip()));
            }
        }
    }

    /** `nombre_mostrar` es VARCHAR(64) y es un texto de presentacion, no una identidad. */
    private static String recortarNombreMostrar(String nombreMostrar) {
        return nombreMostrar.length() > 64 ? nombreMostrar.substring(0, 64) : nombreMostrar;
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
        if (!persona.tieneIdentificacionMinima())
            throw new IllegalArgumentException("La persona debe tener al menos una forma de identificacion");
    }
}
