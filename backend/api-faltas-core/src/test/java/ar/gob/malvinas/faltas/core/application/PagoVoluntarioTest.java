package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.command.ConfirmarPagoVoluntarioCommand;
import ar.gob.malvinas.faltas.core.application.command.CompletarCapturaCommand;
import ar.gob.malvinas.faltas.core.application.command.FijarMontoPagoVoluntarioCommand;
import ar.gob.malvinas.faltas.core.application.command.InformarPagoVoluntarioCommand;
import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.application.command.ObservarPagoVoluntarioCommand;
import ar.gob.malvinas.faltas.core.application.command.SolicitarPagoVoluntarioCommand;
import ar.gob.malvinas.faltas.core.application.command.VencerPagoVoluntarioCommand;
import ar.gob.malvinas.faltas.core.application.port.BloqueantesMaterialesChecker;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.application.service.NoOpBloqueantesMaterialesChecker;
import ar.gob.malvinas.faltas.core.application.service.PagoVoluntarioService;
import ar.gob.malvinas.faltas.core.domain.enums.AccionPendiente;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.CodigoBandeja;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoPagoVoluntario;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.model.FalPagoVoluntario;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.NotificacionRepository;
import ar.gob.malvinas.faltas.core.repository.ApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.PagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryFalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoCondenaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEvidenciaRepository;
import ar.gob.malvinas.faltas.core.repository.PagoCondenaRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Flujo de pago voluntario")
class PagoVoluntarioTest {

    private ActaRepository actaRepo;
    private ActaEventoRepository eventoRepo;
    private ActaSnapshotRepository snapshotRepo;
    private DocumentoRepository docRepo;
    private NotificacionRepository notifRepo;
    private PagoVoluntarioRepository pagoRepo;
    private FalloActaRepository falloRepo;
    private ApelacionActaRepository apelacionRepo;


    private ActaService actaService;
    private PagoVoluntarioService pagoService;

    @BeforeEach
    void setUp() {
        actaRepo = new InMemoryActaRepository();
        eventoRepo = new InMemoryActaEventoRepository();
        snapshotRepo = new InMemoryActaSnapshotRepository();
        docRepo = new InMemoryDocumentoRepository();
        notifRepo = new InMemoryNotificacionRepository();
        pagoRepo = new InMemoryPagoVoluntarioRepository();
        falloRepo = new InMemoryFalloActaRepository();
        apelacionRepo = new InMemoryApelacionActaRepository();


        PagoCondenaRepository pagoCondenaRepo = new InMemoryPagoCondenaRepository();
        SnapshotRecalculador recalc = new SnapshotRecalculador(eventoRepo, docRepo, notifRepo, pagoRepo, falloRepo, apelacionRepo, pagoCondenaRepo);
        actaService = new ActaService(actaRepo, eventoRepo, snapshotRepo, recalc, new InMemoryActaEvidenciaRepository());
        pagoService = new PagoVoluntarioService(
                actaRepo, eventoRepo, snapshotRepo, pagoRepo, recalc,
                new NoOpBloqueantesMaterialesChecker());
    }

    // -------------------------------------------------------------------------
    // Casos felices
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Casos felices")
    class CasosFelices {

        @Test
        @DisplayName("1. Solicitar pago voluntario registra PAGVSO")
        void solicitar_pago_registra_pagvso() {
            Long idActa = labrarYCompletarCaptura();

            ComandoResultado resultado = pagoService.solicitar(
                    new SolicitarPagoVoluntarioCommand(idActa, "infractor solicita pago"));

            assertThat(resultado.tipoEvento()).isEqualTo(TipoEventoActa.PAGVSO.codigo());

            List<FalActaEvento> eventos = eventoRepo.buscarPorActa(idActa);
            assertThat(eventos).extracting(FalActaEvento::tipoEvt)
                    .contains(TipoEventoActa.PAGVSO);

            Optional<FalPagoVoluntario> pago = pagoRepo.buscarPorActa(idActa);
            assertThat(pago).isPresent();
            assertThat(pago.get().getEstadoPagoVoluntario()).isEqualTo(EstadoPagoVoluntario.SOLICITADO);

            FalActa acta = actaRepo.buscarPorId(idActa).orElseThrow();
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
        }

        @Test
        @DisplayName("2. Fijar monto registra PAGVMF")
        void fijar_monto_registra_pagvmf() {
            Long idActa = labrarYSolicitar();

            ComandoResultado resultado = pagoService.fijarMonto(
                    new FijarMontoPagoVoluntarioCommand(idActa, new BigDecimal("1500.00"), null));

            assertThat(resultado.tipoEvento()).isEqualTo(TipoEventoActa.PAGVMF.codigo());

            Optional<FalPagoVoluntario> pago = pagoRepo.buscarPorActa(idActa);
            assertThat(pago).isPresent();
            assertThat(pago.get().getEstadoPagoVoluntario()).isEqualTo(EstadoPagoVoluntario.MONTO_FIJADO);
            assertThat(pago.get().getMonto()).isEqualByComparingTo(new BigDecimal("1500.00"));
        }

        @Test
        @DisplayName("3. Informar pago registra PAGINF, no registra PAGCMP")
        void informar_pago_registra_paginf_no_pagcmp() {
            Long idActa = labrarYSolicitar();

            ComandoResultado resultado = pagoService.informar(
                    new InformarPagoVoluntarioCommand(idActa, "REF-2026-001", "pago realizado"));

            assertThat(resultado.tipoEvento()).isEqualTo(TipoEventoActa.PAGINF.codigo());

            Optional<FalPagoVoluntario> pago = pagoRepo.buscarPorActa(idActa);
            assertThat(pago).isPresent();
            assertThat(pago.get().getEstadoPagoVoluntario())
                    .isEqualTo(EstadoPagoVoluntario.PENDIENTE_CONFIRMACION);
            assertThat(pago.get().getReferenciaPago()).isEqualTo("REF-2026-001");

            List<FalActaEvento> eventos = eventoRepo.buscarPorActa(idActa);
            assertThat(eventos).extracting(FalActaEvento::tipoEvt).contains(TipoEventoActa.PAGINF);
            assertThat(eventos).extracting(FalActaEvento::tipoEvt)
                    .doesNotContain(TipoEventoActa.PAGCMP);
        }

        @Test
        @DisplayName("4. Confirmar pago sin bloqueantes: PAGCNF y CIERRA en ese orden, acta cerrada")
        void confirmar_pago_registra_pagcnf_y_cierra_en_orden() {
            Long idActa = labrarSolicitarEInformar();

            ComandoResultado resultado = pagoService.confirmar(
                    new ConfirmarPagoVoluntarioCommand(idActa, null));

            assertThat(resultado.tipoEvento()).isEqualTo(TipoEventoActa.PAGCNF.codigo());

            FalActa acta = actaRepo.buscarPorId(idActa).orElseThrow();
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.CERR);
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.CERRADA);
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.PAGO_VOLUNTARIO_CONFIRMADO);
            assertThat(acta.estaCerrada()).isTrue();

            List<FalActaEvento> eventos = eventoRepo.buscarPorActa(idActa);
            List<TipoEventoActa> tipos = eventos.stream()
                    .map(FalActaEvento::tipoEvt).collect(Collectors.toList());
            int idxPagcnf = tipos.indexOf(TipoEventoActa.PAGCNF);
            int idxCierra = tipos.indexOf(TipoEventoActa.CIERRA);
            assertThat(idxPagcnf).isGreaterThanOrEqualTo(0);
            assertThat(idxCierra).isGreaterThan(idxPagcnf);

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(idActa).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.CERRADAS);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.NINGUNA);
        }

        @Test
        @DisplayName("5. Observar pago registra PAGOBS y no cierra")
        void observar_pago_registra_pagobs_y_no_cierra() {
            Long idActa = labrarSolicitarEInformar();

            ComandoResultado resultado = pagoService.observar(
                    new ObservarPagoVoluntarioCommand(idActa, "Monto insuficiente", null));

            assertThat(resultado.tipoEvento()).isEqualTo(TipoEventoActa.PAGOBS.codigo());

            FalActa acta = actaRepo.buscarPorId(idActa).orElseThrow();
            assertThat(acta.estaCerrada()).isFalse();
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.SIN_RESULTADO_FINAL);

            Optional<FalPagoVoluntario> pago = pagoRepo.buscarPorActa(idActa);
            assertThat(pago.get().getEstadoPagoVoluntario()).isEqualTo(EstadoPagoVoluntario.OBSERVADO);

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(idActa).orElseThrow();
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.CORREGIR_PAGO);
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_ANALISIS);
        }

        @Test
        @DisplayName("6. Vencer pago registra PAGVVN y habilita analisis/fallo")
        void vencer_pago_registra_pagvvn_y_habilita_analisis() {
            Long idActa = labrarYSolicitar();

            ComandoResultado resultado = pagoService.vencer(
                    new VencerPagoVoluntarioCommand(idActa, "plazo expirado"));

            assertThat(resultado.tipoEvento()).isEqualTo(TipoEventoActa.PAGVVN.codigo());

            Optional<FalPagoVoluntario> pago = pagoRepo.buscarPorActa(idActa);
            assertThat(pago.get().getEstadoPagoVoluntario()).isEqualTo(EstadoPagoVoluntario.VENCIDO);

            FalActa acta = actaRepo.buscarPorId(idActa).orElseThrow();
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
            assertThat(acta.estaCerrada()).isFalse();

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(idActa).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_ANALISIS);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.DICTAR_FALLO);
        }

        @Test
        @DisplayName("Snapshot con PENDIENTE_CONFIRMACION muestra bandeja PENDIENTE_CONFIRMACION_PAGO")
        void snapshot_pendiente_confirmacion_muestra_bandeja_correcta() {
            Long idActa = labrarSolicitarEInformar();

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(idActa).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_CONFIRMACION_PAGO);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.CONFIRMAR_PAGO);
        }

        @Test
        @DisplayName("Flujo completo: solicitar -> monto -> informar -> confirmar = CERRADA con CIERRA en timeline")
        void flujo_completo_solicitar_hasta_confirmar() {
            Long idActa = labrarYCompletarCaptura();

            pagoService.solicitar(new SolicitarPagoVoluntarioCommand(idActa, null));
            pagoService.fijarMonto(new FijarMontoPagoVoluntarioCommand(idActa, new BigDecimal("500"), null));
            pagoService.informar(new InformarPagoVoluntarioCommand(idActa, "REF-FULL-001", null));
            pagoService.confirmar(new ConfirmarPagoVoluntarioCommand(idActa, null));

            FalActa acta = actaRepo.buscarPorId(idActa).orElseThrow();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.PAGO_VOLUNTARIO_CONFIRMADO);
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.CERRADA);

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(idActa)
                    .stream().map(FalActaEvento::tipoEvt).collect(Collectors.toList());
            assertThat(tipos).contains(TipoEventoActa.PAGVSO, TipoEventoActa.PAGVMF,
                    TipoEventoActa.PAGINF, TipoEventoActa.PAGCNF, TipoEventoActa.CIERRA);
            assertThat(tipos.indexOf(TipoEventoActa.PAGCNF))
                    .isLessThan(tipos.indexOf(TipoEventoActa.CIERRA));
        }

        @Test
        @DisplayName("Informar despues de observar: infractor corrige y re-informa")
        void informar_despues_de_observar_permite_reintento() {
            Long idActa = labrarSolicitarEInformar();
            pagoService.observar(new ObservarPagoVoluntarioCommand(idActa, "monto incorrecto", null));

            ComandoResultado resultado = pagoService.informar(
                    new InformarPagoVoluntarioCommand(idActa, "REF-2026-002", "pago corregido"));

            assertThat(resultado.tipoEvento()).isEqualTo(TipoEventoActa.PAGINF.codigo());
            Optional<FalPagoVoluntario> pago = pagoRepo.buscarPorActa(idActa);
            assertThat(pago.get().getEstadoPagoVoluntario())
                    .isEqualTo(EstadoPagoVoluntario.PENDIENTE_CONFIRMACION);
        }
    }

    // -------------------------------------------------------------------------
    // Casos invalidos
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Casos invalidos")
    class CasosInvalidos {

        @Test
        @DisplayName("1. No permitir confirmar pago sin pago informado (PENDIENTE_CONFIRMACION)")
        void no_confirmar_sin_informar() {
            Long idActa = labrarYSolicitar();

            assertThatThrownBy(() ->
                    pagoService.confirmar(new ConfirmarPagoVoluntarioCommand(idActa, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("PENDIENTE_CONFIRMACION");
        }

        @Test
        @DisplayName("2. No permitir informar pago vencido")
        void no_informar_pago_vencido() {
            Long idActa = labrarYSolicitar();
            pagoService.vencer(new VencerPagoVoluntarioCommand(idActa, null));

            assertThatThrownBy(() ->
                    pagoService.informar(new InformarPagoVoluntarioCommand(idActa, "REF-X", null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("vencido");
        }

        @Test
        @DisplayName("3. No permitir doble confirmacion")
        void no_doble_confirmacion() {
            Long idActa = labrarSolicitarEInformar();
            pagoService.confirmar(new ConfirmarPagoVoluntarioCommand(idActa, null));

            assertThatThrownBy(() ->
                    pagoService.confirmar(new ConfirmarPagoVoluntarioCommand(idActa, null)))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("4. Confirmar pago con bloqueantes activos: falla y no muta estado")
        void confirmar_pago_con_bloqueantes_activos_falla() {
            Long idActa = labrarSolicitarEInformar();

            PagoCondenaRepository pagoCondenaRepo = new InMemoryPagoCondenaRepository();
        SnapshotRecalculador recalc = new SnapshotRecalculador(eventoRepo, docRepo, notifRepo, pagoRepo, falloRepo, apelacionRepo, pagoCondenaRepo);
            BloqueantesMaterialesChecker siempreBloqueado = actaId -> true;
            PagoVoluntarioService srvConBloqueantes = new PagoVoluntarioService(
                    actaRepo, eventoRepo, snapshotRepo, pagoRepo, recalc, siempreBloqueado);

            List<FalActaEvento> eventosAntes = eventoRepo.buscarPorActa(idActa);
            int cantidadAntes = eventosAntes.size();

            assertThatThrownBy(() ->
                    srvConBloqueantes.confirmar(new ConfirmarPagoVoluntarioCommand(idActa, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("bloqueantes");

            Optional<FalPagoVoluntario> pago = pagoRepo.buscarPorActa(idActa);
            assertThat(pago.get().getEstadoPagoVoluntario())
                    .isEqualTo(EstadoPagoVoluntario.PENDIENTE_CONFIRMACION);

            FalActa acta = actaRepo.buscarPorId(idActa).orElseThrow();
            assertThat(acta.estaCerrada()).isFalse();
            assertThat(acta.getBloqueActual()).isNotEqualTo(BloqueActual.CERR);
            assertThat(acta.getSituacionAdministrativa()).isNotEqualTo(SituacionAdministrativaActa.CERRADA);

            List<FalActaEvento> eventosAfter = eventoRepo.buscarPorActa(idActa);
            assertThat(eventosAfter).hasSize(cantidadAntes);
            assertThat(eventosAfter).extracting(FalActaEvento::tipoEvt)
                    .doesNotContain(TipoEventoActa.PAGCNF, TipoEventoActa.CIERRA);
        }

        @Test
        @DisplayName("5. No permitir confirmar si acta ya esta cerrada/anulada")
        void no_confirmar_si_acta_cerrada() {
            Long idActa = labrarSolicitarEInformar();
            pagoService.confirmar(new ConfirmarPagoVoluntarioCommand(idActa, null));

            assertThatThrownBy(() ->
                    pagoService.solicitar(new SolicitarPagoVoluntarioCommand(idActa, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("cerrada");
        }

        @Test
        @DisplayName("6. No permitir monto cero o negativo")
        void no_monto_cero_o_negativo() {
            Long idActa = labrarYSolicitar();

            assertThatThrownBy(() ->
                    pagoService.fijarMonto(
                            new FijarMontoPagoVoluntarioCommand(idActa, BigDecimal.ZERO, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("mayor a cero");

            assertThatThrownBy(() ->
                    pagoService.fijarMonto(
                            new FijarMontoPagoVoluntarioCommand(idActa, new BigDecimal("-1"), null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("mayor a cero");
        }

        @Test
        @DisplayName("7. TipoEventoActa no incluye PAGVOL")
        void tipo_evento_no_incluye_pagvol() {
            Set<String> codigos = Arrays.stream(TipoEventoActa.values())
                    .map(TipoEventoActa::codigo)
                    .collect(Collectors.toSet());
            assertThat(codigos).doesNotContain("PAGVOL");

            Set<String> nombres = Arrays.stream(TipoEventoActa.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());
            assertThat(nombres).doesNotContain("PAGVOL");
        }

        @Test
        @DisplayName("7b. TipoEventoActa incluye los 7 eventos de pago voluntario y CIERRA")
        void tipo_evento_incluye_7_eventos_pago_voluntario_y_cierra() {
            Set<String> codigos = Arrays.stream(TipoEventoActa.values())
                    .map(TipoEventoActa::codigo)
                    .collect(Collectors.toSet());
            assertThat(codigos).contains("PAGVSO", "PAGVMF", "PAGINF", "PAGCMP",
                    "PAGCNF", "PAGOBS", "PAGVVN", "CIERRA");
            assertThat(codigos).doesNotContain("ACTCER");
        }

        @Test
        @DisplayName("8. EstadoPagoVoluntario no incluye PAGO_INFORMADO")
        void estado_pago_voluntario_no_incluye_pago_informado() {
            Set<String> nombres = Arrays.stream(EstadoPagoVoluntario.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());
            assertThat(nombres).doesNotContain("PAGO_INFORMADO");
        }

        @Test
        @DisplayName("ResultadoFinalActa no incluye PAGO_INFORMADO ni PAGO_VOLUNTARIO (sin confirmar)")
        void resultado_final_no_incluye_estados_invalidos() {
            Set<String> nombres = Arrays.stream(ResultadoFinalActa.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());
            assertThat(nombres).doesNotContain("PAGO_INFORMADO");
            assertThat(nombres).doesNotContain("PAGO_VOLUNTARIO");
            assertThat(nombres).contains("PAGO_VOLUNTARIO_CONFIRMADO");
            assertThat(nombres).contains("SIN_RESULTADO_FINAL");
        }

        @Test
        @DisplayName("No solicitar pago en bloque CAPT")
        void no_solicitar_en_capt() {
            Long idActa = actaService.labrar(cmdLabrar()).idActa();

            assertThatThrownBy(() ->
                    pagoService.solicitar(new SolicitarPagoVoluntarioCommand(idActa, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("ENRI o ANAL");
        }

        @Test
        @DisplayName("No informar pago sin referencia")
        void no_informar_sin_referencia() {
            Long idActa = labrarYSolicitar();

            assertThatThrownBy(() ->
                    pagoService.informar(new InformarPagoVoluntarioCommand(idActa, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("referencia de pago");

            assertThatThrownBy(() ->
                    pagoService.informar(new InformarPagoVoluntarioCommand(idActa, "  ", null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("referencia de pago");
        }

        @Test
        @DisplayName("No observar pago sin motivo")
        void no_observar_sin_motivo() {
            Long idActa = labrarSolicitarEInformar();

            assertThatThrownBy(() ->
                    pagoService.observar(new ObservarPagoVoluntarioCommand(idActa, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("motivo de observacion");
        }

        @Test
        @DisplayName("No observar pago en estado SOLICITADO (requiere PENDIENTE_CONFIRMACION)")
        void no_observar_en_solicitado() {
            Long idActa = labrarYSolicitar();

            assertThatThrownBy(() ->
                    pagoService.observar(new ObservarPagoVoluntarioCommand(idActa, "motivo", null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("PENDIENTE_CONFIRMACION");
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private LabrarActaCommand cmdLabrar() {
        return new LabrarActaCommand(
                "TRANSITO", "DEP-001", "INS-001",
                LocalDate.now(), "Av. Argentina 123", "San Martin 456",
                null, null, null, "Juan Perez", "12345678",
                ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null
        );
    }

    private Long labrarYCompletarCaptura() {
        Long idActa = actaService.labrar(cmdLabrar()).idActa();
        actaService.completarCaptura(new CompletarCapturaCommand(idActa, null));
        return idActa;
    }

    private Long labrarYSolicitar() {
        Long idActa = labrarYCompletarCaptura();
        pagoService.solicitar(new SolicitarPagoVoluntarioCommand(idActa, null));
        return idActa;
    }

    private Long labrarSolicitarEInformar() {
        Long idActa = labrarYSolicitar();
        pagoService.informar(new InformarPagoVoluntarioCommand(idActa, "REF-TEST-001", null));
        return idActa;
    }
}



