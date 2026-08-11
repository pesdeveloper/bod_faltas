package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.service.TalonarioService;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.support.JwtTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Smoke HTTP end-to-end de la Parte 3D (S20), contra MariaDB real, filesystem real (directorio
 * temporal aislado) y JWT real (firmado con la clave de test, validado por el
 * {@code JwtDecoder} real del contexto).
 *
 * <p>Cubre exactamente los ocho pasos exigidos por el prompt: alta JSON sin fotos, alta
 * multipart con fotos, GET acta con metadata, POST carga posterior, GET/stream de fotografia,
 * repeticion idempotente del alta, duplicado rechazado, usuario no autorizado rechazado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("jdbc")
@EnabledIfEnvironmentVariable(named = "FALTAS_DB_URL", matches = ".+")
@DisplayName("Smoke HTTP Wave 4 Parte 3D: alta con fotografias, carga posterior, descarga")
class ActaFotografiasHttpSmokeIT {

    private static final String MARCA = "WAVE4P3DHTTP_";
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 8, 6, 15, 0, 0);

    private static Path raizStorage;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) throws IOException {
        raizStorage = Files.createTempDirectory("faltas-it-p3d-http-evidencias-");
        registry.add("faltas.storage.evidencias.root-dir", () -> raizStorage.toString());
    }

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private JdbcClient jdbc;
    @Autowired private TalonarioService talonarioService;

    private String actor;
    private String bearer;
    private Long idDependencia;
    private Long idInspector;
    private Long idTalonario;

    @BeforeEach
    void setUp() {
        actor = UUID.randomUUID().toString();
        bearer = "Bearer " + JwtTestSupport.token(actor);
        idDependencia = insertarDependencia();
        idInspector = insertarInspector(idDependencia);
        idTalonario = crearTalonario(idDependencia);
    }

    @AfterEach
    void limpiar() {
        List<Long> actas = jdbc.sql("SELECT id FROM fal_acta WHERE id_dep = :d").param("d", idDependencia)
                .query(Long.class).list();
        for (Long id : actas) {
            jdbc.sql("DELETE FROM fal_acta_evidencia WHERE id_acta = :id").param("id", id).update();
            jdbc.sql("DELETE FROM fal_acta_evento WHERE acta_id = :id").param("id", id).update();
            jdbc.sql("DELETE FROM fal_acta_snapshot WHERE id_acta = :id").param("id", id).update();
            jdbc.sql("DELETE FROM fal_observacion WHERE entidad_tipo = 1 AND entidad_id = :id").param("id", id).update();
            jdbc.sql("DELETE FROM fal_acta_transito WHERE acta_id = :id").param("id", id).update();
            jdbc.sql("DELETE FROM fal_acta WHERE id = :id").param("id", id).update();
        }
        jdbc.sql("DELETE FROM num_talonario_ambito WHERE talonario_id = :id").param("id", idTalonario).update();
        Long idPol = jdbc.sql("SELECT politica_id FROM num_talonario WHERE id = :id")
                .param("id", idTalonario).query(Long.class).single();
        jdbc.sql("DELETE FROM num_talonario WHERE id = :id").param("id", idTalonario).update();
        jdbc.sql("DELETE FROM num_politica WHERE id = :id").param("id", idPol).update();
        jdbc.sql("DELETE FROM fal_inspector_version WHERE id_insp = :id").param("id", idInspector).update();
        jdbc.sql("DELETE FROM fal_inspector WHERE id = :id").param("id", idInspector).update();
        jdbc.sql("DELETE FROM fal_dependencia_version WHERE id_dep = :id").param("id", idDependencia).update();
        jdbc.sql("DELETE FROM fal_dependencia WHERE id = :id").param("id", idDependencia).update();
    }

    @Test
    @DisplayName("Flujo completo: JSON sin fotos -> multipart con foto -> GET -> carga posterior -> descarga -> idempotencia -> duplicado -> 401")
    void flujoCompleto() throws Exception {
        // 1. Alta JSON sin fotografias (contrato anterior, compatibilidad).
        String idTecnico = UUID.randomUUID().toString();
        String comandoJson = comandoJson(idTecnico);
        MvcResult altaJson = mvc.perform(post("/api/faltas/actas/labrar")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(comandoJson))
                .andExpect(status().isCreated())
                .andReturn();
        Long idActa = extraerIdActa(altaJson);
        assertThat(idActa).isNotNull();

        // 6. Repeticion idempotente del alta (mismo id_tecnico, esta vez con una fotografia
        //    diferente en el payload): debe devolver la misma acta y NO agregar la fotografia.
        MockMultipartFile comandoParteIdempotente = new MockMultipartFile(
                "comando", "", MediaType.APPLICATION_JSON_VALUE, comandoJson(idTecnico).getBytes());
        MockMultipartFile fotoIgnorada = new MockMultipartFile(
                "fotografias", "ignorada.jpg", "image/jpeg", jpeg(Color.RED));
        MvcResult reintento = mvc.perform(multipart("/api/faltas/actas/labrar")
                        .file(comandoParteIdempotente).file(fotoIgnorada)
                        .header("Authorization", bearer))
                .andExpect(status().isCreated())
                .andReturn();
        assertThat(extraerIdActa(reintento)).isEqualTo(idActa);
        assertThat(fotosDeLaActa(idActa)).as("el reintento idempotente no agrega fotografias").isEmpty();

        // 2. Alta multipart con fotografias, sobre una acta NUEVA (otro id_tecnico).
        String idTecnico2 = UUID.randomUUID().toString();
        MockMultipartFile comandoParte2 = new MockMultipartFile(
                "comando", "", MediaType.APPLICATION_JSON_VALUE, comandoJson(idTecnico2).getBytes());
        MockMultipartFile foto1 = new MockMultipartFile(
                "fotografias", "primera.jpg", "image/jpeg", jpeg(Color.BLUE));
        MvcResult altaMultipart = mvc.perform(multipart("/api/faltas/actas/labrar")
                        .file(comandoParte2).file(foto1)
                        .header("Authorization", bearer))
                .andExpect(status().isCreated())
                .andReturn();
        Long idActa2 = extraerIdActa(altaMultipart);
        assertThat(idActa2).isNotEqualTo(idActa);

        // 3. GET acta con metadata (endpoint de fotografias).
        MvcResult listado1 = mvc.perform(get("/api/faltas/actas/{id}/fotografias", idActa2)
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> fotos1 = mapper.readValue(
                listado1.getResponse().getContentAsString(), List.class);
        assertThat(fotos1).hasSize(1);
        assertThat(fotos1.get(0)).doesNotContainKey("storageKey");
        Long fotoId1 = ((Number) fotos1.get(0).get("id")).longValue();

        // 4. POST carga posterior.
        MockMultipartFile foto2 = new MockMultipartFile(
                "fotografias", "segunda.jpg", "image/jpeg", jpeg(Color.GREEN));
        mvc.perform(multipart("/api/faltas/actas/{id}/fotografias", idActa2)
                        .file(foto2)
                        .header("Authorization", bearer))
                .andExpect(status().isCreated());
        assertThat(fotosDeLaActa(idActa2)).hasSize(2);

        // 5. GET/stream de fotografia.
        MvcResult descarga = mvc.perform(get("/api/faltas/actas/{id}/fotografias/{fotoId}/archivo", idActa2, fotoId1)
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG))
                .andReturn();
        assertThat(descarga.getResponse().getContentAsByteArray()).isNotEmpty();

        // 7. Duplicado rechazado (misma imagen que ya existe en la acta).
        MockMultipartFile fotoDuplicada = new MockMultipartFile(
                "fotografias", "duplicada.jpg", "image/jpeg", jpeg(Color.GREEN));
        mvc.perform(multipart("/api/faltas/actas/{id}/fotografias", idActa2)
                        .file(fotoDuplicada)
                        .header("Authorization", bearer))
                .andExpect(status().isUnprocessableEntity());
        assertThat(fotosDeLaActa(idActa2)).as("el duplicado no se incorporo").hasSize(2);

        // 8. Usuario no autorizado (sin Bearer) rechazado, tanto en carga posterior como en descarga.
        MockMultipartFile fotoSinAuth = new MockMultipartFile(
                "fotografias", "sinauth.jpg", "image/jpeg", jpeg(Color.YELLOW));
        mvc.perform(multipart("/api/faltas/actas/{id}/fotografias", idActa2).file(fotoSinAuth))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/faltas/actas/{id}/fotografias/{fotoId}/archivo", idActa2, fotoId1))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────────────────────────────────────────────────────
    // Soporte
    // ─────────────────────────────────────────────────────────────────────

    private static byte[] jpeg(Color color) {
        try {
            BufferedImage img = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
            var g = img.createGraphics();
            g.setColor(color);
            g.fillRect(0, 0, 20, 20);
            g.dispose();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "jpg", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private String comandoJson(String idTecnico) throws Exception {
        return mapper.writeValueAsString(Map.ofEntries(
                Map.entry("tipoActa", "TRANSITO"),
                Map.entry("idDependencia", String.valueOf(idDependencia)),
                Map.entry("idInspector", String.valueOf(idInspector)),
                Map.entry("domicilioHecho", MARCA + "domicilio"),
                Map.entry("resultadoFirmaInfractor", "SE_NIEGA_A_FIRMAR"),
                Map.entry("situacionIdentificacionInfractor", "DATOS_INSUFICIENTES"),
                Map.entry("idTecnico", idTecnico),
                Map.entry("transito", Map.of(
                        "siRetLicencia", false, "siRetVehiculo", false,
                        "siControlAlcoholemia", false, "mediciones", List.of()))
        ));
    }

    @SuppressWarnings("unchecked")
    private Long extraerIdActa(MvcResult resultado) throws Exception {
        Map<String, Object> body = mapper.readValue(resultado.getResponse().getContentAsString(), Map.class);
        return ((Number) body.get("idActa")).longValue();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fotosDeLaActa(Long idActa) throws Exception {
        MvcResult r = mvc.perform(get("/api/faltas/actas/{id}/fotografias", idActa)
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readValue(r.getResponse().getContentAsString(), List.class);
    }

    private Long insertarDependencia() {
        String nom = MARCA + "DEP_" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.sql("INSERT INTO fal_dependencia (nom_dep, fh_alta, id_user_alta) VALUES (:nom, :fh, :u)")
                .param("nom", nom).param("fh", T0).param("u", actor).update();
        Long id = jdbc.sql("SELECT id FROM fal_dependencia WHERE nom_dep = :nom")
                .param("nom", nom).query(Long.class).single();
        jdbc.sql("""
                INSERT INTO fal_dependencia_version (id_dep, ver_dep, tipo_acta, fh_vig_desde, fh_alta, id_user_alta)
                VALUES (:id, 1, :tipo, :vig, :fh, :u)
                """).param("id", id).param("tipo", TipoActa.TRANSITO.codigo())
                .param("vig", LocalDate.of(2026, 1, 1)).param("fh", T0).param("u", actor).update();
        return id;
    }

    private Long insertarInspector(Long idDep) {
        String nom = MARCA + "INSP_" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.sql("INSERT INTO fal_inspector (id_user, legajo_insp, nom_insp, fh_alta, id_user_alta) VALUES (:u, :leg, :nom, :fh, :usr)")
                .param("u", UUID.randomUUID().toString()).param("leg", (int) (Math.random() * 900000) + 1)
                .param("nom", nom).param("fh", T0).param("usr", actor).update();
        Long id = jdbc.sql("SELECT id FROM fal_inspector WHERE nom_insp = :nom")
                .param("nom", nom).query(Long.class).single();
        jdbc.sql("""
                INSERT INTO fal_inspector_version (id_insp, ver_insp, legajo_insp, nom_insp, id_dep, ver_dep, fh_vig_desde, fh_alta, id_user_alta)
                VALUES (:id, 1, :leg, :nom, :idDep, 1, :vig, :fh, :usr)
                """).param("id", id).param("leg", (int) (Math.random() * 900000) + 1).param("nom", nom)
                .param("idDep", idDep).param("vig", LocalDate.of(2026, 1, 1)).param("fh", T0).param("usr", actor).update();
        return id;
    }

    private Long crearTalonario(Long idDep) {
        var politica = talonarioService.crearPolitica(new ar.gob.malvinas.faltas.core.application.command.CrearPoliticaNumeracionCommand(
                ("P" + UUID.randomUUID().toString().substring(0, 7)).toUpperCase(),
                "Politica HTTP smoke " + MARCA,
                ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion.ACTA,
                false, false, null, false, null, false, (short) 6,
                "{NRO}", true, LocalDate.of(2026, 1, 1), null, actor));
        var talonario = talonarioService.crearTalonario(new ar.gob.malvinas.faltas.core.application.command.CrearTalonarioCommand(
                politica.getId(),
                ("T" + UUID.randomUUID().toString().substring(0, 7)).toUpperCase(),
                "Talonario HTTP smoke " + MARCA,
                ar.gob.malvinas.faltas.core.domain.enums.TipoTalonario.ELECTRONICO,
                ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion.ACTA,
                null, null, 1, null,
                ("SEQHTTP_" + UUID.randomUUID()).substring(0, 24), true, false, null, null, actor));
        talonarioService.crearAmbito(new ar.gob.malvinas.faltas.core.application.command.CrearTalonarioAmbitoCommand(
                talonario.getId(), ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion.ACTA, null,
                TipoActa.TRANSITO.codigo(), idDep, (short) 1,
                ar.gob.malvinas.faltas.core.domain.enums.AlcanceTalonario.DEPENDENCIA,
                (short) 1, LocalDate.of(2026, 1, 1), null, true, actor));
        return talonario.getId();
    }
}
