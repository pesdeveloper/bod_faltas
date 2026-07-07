package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.CompletarCapturaCommand;
import ar.gob.malvinas.faltas.core.application.command.EnriquecerActaCommand;
import ar.gob.malvinas.faltas.core.application.command.EvidenciaActaItem;
import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEvidenciaActa;
import ar.gob.malvinas.faltas.core.domain.enums.ActorTipoEvento;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvidencia;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPersona;
import ar.gob.malvinas.faltas.core.domain.model.FalPersona;
import ar.gob.malvinas.faltas.core.repository.ActaEvidenciaRepository;
import ar.gob.malvinas.faltas.core.repository.PersonaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ActaService {

    private final ActaRepository actaRepository;
    private final ActaEventoRepository eventoRepository;
    private final ActaSnapshotRepository snapshotRepository;
    private final SnapshotRecalculador snapshotRecalculador;
    private final ActaEvidenciaRepository evidenciaRepository;
    private PersonaRepository personaRepository;

    public ActaService(
            ActaRepository actaRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            SnapshotRecalculador snapshotRecalculador,
            ActaEvidenciaRepository evidenciaRepository) {
        this.actaRepository = actaRepository;
        this.eventoRepository = eventoRepository;
        this.snapshotRepository = snapshotRepository;
        this.snapshotRecalculador = snapshotRecalculador;
        this.evidenciaRepository = evidenciaRepository;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ActaService(
            ActaRepository actaRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            SnapshotRecalculador snapshotRecalculador,
            ActaEvidenciaRepository evidenciaRepository,
            PersonaRepository personaRepository) {
        this(actaRepository, eventoRepository, snapshotRepository, snapshotRecalculador, evidenciaRepository);
        this.personaRepository = personaRepository;
    }

    public ComandoResultado labrar(LabrarActaCommand cmd) {
        if (cmd.resultadoFirmaInfractor() == null) {
            throw new PrecondicionVioladaException("resultadoFirmaInfractor es obligatorio.");
        }

        if (cmd.resultadoFirmaInfractor() == ResultadoFirmaInfractor.FIRMADA) {
            boolean tieneEvidenciaFirma = cmd.evidenciasActa() != null
                    && cmd.evidenciasActa().stream()
                    .anyMatch(e -> e.tipoEvid() == TipoEvidenciaActa.FIRMA_OLOGRAFA_INFRACTOR);
            if (!tieneEvidenciaFirma) {
                throw new PrecondicionVioladaException(
                        "resultadoFirmaInfractor=FIRMADA requiere evidencia FIRMA_OLOGRAFA_INFRACTOR.");
            }
        }

        Long id = actaRepository.nextId();
        String uuidTecnico = UUID.randomUUID().toString();

        FalActa acta = new FalActa(
                id,
                uuidTecnico,
                cmd.tipoActa(),
                cmd.idDependencia() != null ? cmd.idDependencia() : 1L,
                cmd.idInspector() != null ? cmd.idInspector() : 1L,
                cmd.fechaActa() != null ? cmd.fechaActa() : java.time.LocalDate.now(),
                LocalDateTime.now(),
                cmd.domicilioHecho(),
                cmd.observaciones(),
                cmd.latInfr(),
                null,
                cmd.resultadoFirmaInfractor(),
                cmd.idPersonaInfractor(),
                LocalDateTime.now(),
                null
        );
        if (cmd.infractorDocumento() != null) acta.setInfractorDocumento(cmd.infractorDocumento());
        if (cmd.infractorNombre() != null) acta.setInfractorNombre(cmd.infractorNombre());
        if (cmd.idPersonaInfractor() == null && cmd.infractorNombre() != null
                && personaRepository != null) {
            FalPersona personaMinimal = crearPersonaMinimal(
                    cmd.infractorDocumento(), cmd.infractorNombre());
            acta.setIdPersonaInfractor(personaMinimal.getId());
        }
        actaRepository.guardar(acta);

        if (cmd.evidenciasActa() != null) {
            LocalDateTime ahora = LocalDateTime.now();
            for (EvidenciaActaItem item : cmd.evidenciasActa()) {
                FalActaEvidencia evidencia = new FalActaEvidencia(
                        evidenciaRepository.nextId(),
                        acta.getId(),
                        item.tipoEvid(),
                        item.storageKey(),
                        ahora
                );
                evidenciaRepository.guardar(evidencia);
            }
        }

        registrarEvento(acta.getId(), TipoEventoActa.ACTLAB, null, null,
                String.valueOf(cmd.idInspector()), "Acta labrada - tipo: " + cmd.tipoActa().name());

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(id, id.toString(), TipoEventoActa.ACTLAB.codigo(),
                "Acta labrada correctamente. Bloque: CAPT");
    }

    public ComandoResultado completarCaptura(CompletarCapturaCommand cmd) {
        FalActa acta = actaRepository.buscarPorId(cmd.idActa())
                .orElseThrow(() -> new ActaNoEncontradaException(cmd.idActa()));

        if (acta.estaCerrada()) {
            throw new PrecondicionVioladaException("El acta esta cerrada. No se puede completar captura.");
        }
        if (acta.getBloqueActual() != BloqueActual.CAPT) {
            throw new PrecondicionVioladaException(
                    "Completar captura requiere bloque CAPT. Bloque actual: "
                            + acta.getBloqueActual().codigo());
        }

        acta.setBloqueActual(BloqueActual.ENRI);
        actaRepository.guardar(acta);

        registrarEvento(acta.getId(), TipoEventoActa.ACTCAP, null, null,
                null, cmd.observaciones());

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(acta.getId(), acta.getId().toString(),
                TipoEventoActa.ACTCAP.codigo(),
                "Captura completada. Bloque: ENRI");
    }

    public ComandoResultado enriquecer(EnriquecerActaCommand cmd) {
        FalActa acta = actaRepository.buscarPorId(cmd.idActa())
                .orElseThrow(() -> new ActaNoEncontradaException(cmd.idActa()));

        if (acta.estaCerrada()) {
            throw new PrecondicionVioladaException("El acta esta cerrada. No se puede enriquecer.");
        }
        if (acta.getBloqueActual() != BloqueActual.ENRI) {
            throw new PrecondicionVioladaException(
                    "Enriquecer requiere bloque ENRI. Bloque actual: "
                            + acta.getBloqueActual().codigo());
        }

        registrarEvento(acta.getId(), TipoEventoActa.ACTENR, null, null,
                null, cmd.observaciones());

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(acta.getId(), acta.getId().toString(),
                TipoEventoActa.ACTENR.codigo(),
                "Acta enriquecida. Bloque: ENRI");
    }

    public FalActa obtenerActa(Long id) {
        return actaRepository.buscarPorId(id)
                .orElseThrow(() -> new ActaNoEncontradaException(id));
    }

    public FalActaSnapshot obtenerSnapshot(Long idActa) {
        return snapshotRepository.buscarPorActa(idActa)
                .orElseThrow(() -> new ActaNoEncontradaException("snapshot de acta: " + idActa));
    }

    public List<FalActaEvidencia> listarEvidencias(Long idActa) {
        return evidenciaRepository.listarPorActa(idActa);
    }

    private FalPersona crearPersonaMinimal(String nroDoc, String nombreMostrar) {
        Long id = personaRepository.nextId();
        FalPersona persona = new FalPersona(id, TipoPersona.FISICA, LocalDateTime.now(), "SYS");
        if (nroDoc != null && !nroDoc.isBlank()) {
            String nroDocNorm = nroDoc.strip().replaceAll("[^0-9a-zA-Z\\\\-]", "");
            if (nroDocNorm.length() > 20) nroDocNorm = nroDocNorm.substring(0, 20);
            persona.setNroDoc(nroDocNorm.isBlank() ? null : nroDocNorm);
        }
        if (nombreMostrar != null && !nombreMostrar.isBlank()) {
            String nm = nombreMostrar.strip();
            persona.setNombreMostrar(nm.length() > 64 ? nm.substring(0, 64) : nm);
        }
        return personaRepository.guardar(persona);
    }

    private void registrarEvento(Long idActa, TipoEventoActa tipo,
                                  Long idDocuRel, Long idNotifRel,
                                  String idUserEvt, String descripcionLegible) {
        FalActaEvento evento = FalActaEvento.builder()
                .actaId(idActa)
                .tipoEvt(tipo)
                .origenEvt(idUserEvt != null ? OrigenEvento.USUARIO_WEB : OrigenEvento.PROCESO_AUTOMATICO)
                .fhEvt(LocalDateTime.now())
                .idDocuRel(idDocuRel)
                .idNotifRel(idNotifRel)
                .idUserEvt(idUserEvt)
                .actorTipo(idUserEvt != null ? ActorTipoEvento.USUARIO_INTERNO : ActorTipoEvento.SISTEMA)
                .descripcionLegible(descripcionLegible)
                .build();
        eventoRepository.registrar(evento);
    }
}