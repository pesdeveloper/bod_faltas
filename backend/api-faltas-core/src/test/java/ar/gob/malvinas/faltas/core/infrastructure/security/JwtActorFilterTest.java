package ar.gob.malvinas.faltas.core.infrastructure.security;

import ar.gob.malvinas.faltas.core.support.JwtTestSupport;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios de JwtActorFilter con JWT firmados reales (HS256).
 * No usa contexto Spring. Decoder instanciado directamente desde JwtTestSupport.
 *
 * Casos cubiertos (prompt FIX-FALLO-NOTI-01-R2, seccion 10):
 *  1. token firmado valido -> pasa
 *  2. actor = sub del token
 *  3. sin token en endpoint protegido -> 401
 *  4. firma incorrecta -> 401
 *  5. token vencido -> 401
 *  6. issuer incorrecto -> 401
 *  7. audience incorrecta -> 401
 *  8. sin sub -> 401
 *  9. alg=none -> 401
 * 10. contexto limpio despues del chain
 */
@DisplayName("JwtActorFilter - JWT firmado real (HS256)")
class JwtActorFilterTest {

    private JwtActorFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtActorFilter(JwtTestSupport.buildTestDecoder());
    }

    @AfterEach
    void clear() {
        ActorContextHolder.clear();
    }

    // -----------------------------------------------------------------------
    // Caso 1 & 2: token firmado valido -> pasa; actor = sub
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("1+2. Token firmado valido: pasa y actor es el sub del JWT")
    void token_valido_pasa_y_actor_es_sub() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        req.addHeader("Authorization", "Bearer " + JwtTestSupport.token("usuario-demo-faltas"));
        FilterChain chain = mock(FilterChain.class);
        doAnswer(inv -> {
            assertThat(ActorContextHolder.subOr("x")).isEqualTo("usuario-demo-faltas");
            return null;
        }).when(chain).doFilter(req, res);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(200);
        verify(chain).doFilter(req, res);
    }

    // -----------------------------------------------------------------------
    // Caso 3: sin token en endpoint protegido -> 401
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("3. Sin token en endpoint protegido -> 401")
    void sin_token_en_endpoint_protegido_responde_401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/faltas/pagos/notificar");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
    }

    // -----------------------------------------------------------------------
    // Caso 4: firma incorrecta -> 401
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("4. Firma incorrecta -> 401")
    void firma_incorrecta_responde_401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        req.addHeader("Authorization", "Bearer " + JwtTestSupport.tokenFirmaIncorrecta("usuario"));
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
    }

    // -----------------------------------------------------------------------
    // Caso 5: token vencido -> 401
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("5. Token vencido -> 401")
    void token_vencido_responde_401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        req.addHeader("Authorization", "Bearer " + JwtTestSupport.tokenVencido("usuario"));
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
    }

    // -----------------------------------------------------------------------
    // Caso 6: issuer incorrecto -> 401
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("6. Issuer incorrecto -> 401")
    void issuer_incorrecto_responde_401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        req.addHeader("Authorization", "Bearer " + JwtTestSupport.tokenIssuerIncorrecto("usuario"));
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
    }

    // -----------------------------------------------------------------------
    // Caso 7: audience incorrecta -> 401
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("7. Audience incorrecta -> 401")
    void audience_incorrecta_responde_401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        req.addHeader("Authorization", "Bearer " + JwtTestSupport.tokenAudienceIncorrecta("usuario"));
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
    }

    // -----------------------------------------------------------------------
    // Caso 8: sin sub -> 401
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("8a. Token sin campo sub -> 401")
    void sin_sub_responde_401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        req.addHeader("Authorization", "Bearer " + JwtTestSupport.tokenNoSub());
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("8b. Token con sub vacio -> 401")
    void sub_vacio_responde_401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        req.addHeader("Authorization", "Bearer " + JwtTestSupport.tokenSubVacio());
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
    }

    // -----------------------------------------------------------------------
    // Caso 9: alg=none -> 401
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("9. alg=none rechazado -> 401")
    void alg_none_responde_401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        req.addHeader("Authorization", "Bearer " + JwtTestSupport.tokenAlgNone("usuario"));
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
    }

    // -----------------------------------------------------------------------
    // Caso 10: contexto limpio despues del chain
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("10. Contexto limpio despues del chain")
    void contexto_limpio_tras_request() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        req.addHeader("Authorization", "Bearer " + JwtTestSupport.token("usuario-demo-faltas"));
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(ActorContextHolder.get()).isNull();
    }

    // -----------------------------------------------------------------------
    // Sin token en endpoint libre: pasa sin error
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Sin token en endpoint libre: pasa (200)")
    void sin_token_no_establece_contexto() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        doAnswer(inv -> {
            assertThat(ActorContextHolder.get()).isNull();
            return null;
        }).when(chain).doFilter(req, res);

        filter.doFilter(req, res, chain);

        assertThat(ActorContextHolder.get()).isNull();
        assertThat(res.getStatus()).isEqualTo(200);
    }

    // -----------------------------------------------------------------------
    // Tokens no-UUID aceptados
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Sub no-UUID valido aceptado")
    void sub_no_uuid_valido_aceptado() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        req.addHeader("Authorization", "Bearer " + JwtTestSupport.token("actor-operativo-123"));
        FilterChain chain = mock(FilterChain.class);
        doAnswer(inv -> {
            assertThat(ActorContextHolder.subOr("x")).isEqualTo("actor-operativo-123");
            return null;
        }).when(chain).doFilter(req, res);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(200);
    }

    // -----------------------------------------------------------------------
    // Endpoint /numerar protegido
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Sin token en /numerar -> 401")
    void sin_token_en_endpoint_numerar_responde_401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/faltas/documentos/42/numerar");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Token valido en /numerar: pasa")
    void token_valido_en_endpoint_numerar_pasa() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/faltas/documentos/42/numerar");
        req.addHeader("Authorization", "Bearer " + JwtTestSupport.token("firmas-service-prod"));
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        doAnswer(inv -> {
            assertThat(ActorContextHolder.subOr("x")).isEqualTo("firmas-service-prod");
            return null;
        }).when(chain).doFilter(req, res);

        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
    }

    // -----------------------------------------------------------------------
    // Endpoint /firmar-real protegido
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Sin token en /firmar-real -> 401")
    void sin_token_en_endpoint_firmar_real_responde_401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/faltas/documentos/42/firmar-real");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Token valido en /firmar-real: pasa y contexto limpio al finalizar")
    void token_valido_en_firmar_real_pasa_y_contexto_limpio() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/faltas/documentos/99/firmar-real");
        req.addHeader("Authorization", "Bearer " + JwtTestSupport.token("actor-firmante"));
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        doAnswer(inv -> {
            assertThat(ActorContextHolder.subOr("x")).isEqualTo("actor-firmante");
            return null;
        }).when(chain).doFilter(req, res);

        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
        assertThat(ActorContextHolder.get()).isNull();
    }

    // -----------------------------------------------------------------------
    // Rutas nuevas: labrar, completar-captura, enriquecer
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/faltas/actas/labrar sin Bearer -> 401")
    void labrar_sin_token_responde_401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/faltas/actas/labrar");
        req.setRequestURI("/api/faltas/actas/labrar");
        req.setMethod("POST");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("POST /api/faltas/actas/labrar con Bearer valido: pasa; ActorContext contiene sub exacto")
    void labrar_con_token_valido_pasa_y_actor_es_sub() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/faltas/actas/labrar");
        req.setRequestURI("/api/faltas/actas/labrar");
        req.setMethod("POST");
        req.addHeader("Authorization", "Bearer " + JwtTestSupport.token("inspector-01"));
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        doAnswer(inv -> {
            assertThat(ActorContextHolder.subOr("x")).isEqualTo("inspector-01");
            return null;
        }).when(chain).doFilter(req, res);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(200);
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("POST /api/faltas/actas/labrar con sub ausente/blank -> 401")
    void labrar_con_sub_blank_responde_401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/faltas/actas/labrar");
        req.setRequestURI("/api/faltas/actas/labrar");
        req.setMethod("POST");
        req.addHeader("Authorization", "Bearer " + JwtTestSupport.tokenSubVacio());
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("POST /api/faltas/actas/{id}/completar-captura sin Bearer -> 401")
    void completar_captura_sin_token_responde_401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/faltas/actas/5/completar-captura");
        req.setRequestURI("/api/faltas/actas/5/completar-captura");
        req.setMethod("POST");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("POST /api/faltas/actas/{id}/completar-captura con Bearer valido: pasa; sub exacto")
    void completar_captura_con_token_valido_pasa() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/faltas/actas/5/completar-captura");
        req.setRequestURI("/api/faltas/actas/5/completar-captura");
        req.setMethod("POST");
        req.addHeader("Authorization", "Bearer " + JwtTestSupport.token("inspector-02"));
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        doAnswer(inv -> {
            assertThat(ActorContextHolder.subOr("x")).isEqualTo("inspector-02");
            return null;
        }).when(chain).doFilter(req, res);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(200);
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("POST /api/faltas/actas/{id}/enriquecer sin Bearer -> 401")
    void enriquecer_sin_token_responde_401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/faltas/actas/7/enriquecer");
        req.setRequestURI("/api/faltas/actas/7/enriquecer");
        req.setMethod("POST");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("POST /api/faltas/actas/{id}/enriquecer con Bearer valido: pasa; sub exacto")
    void enriquecer_con_token_valido_pasa() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/faltas/actas/7/enriquecer");
        req.setRequestURI("/api/faltas/actas/7/enriquecer");
        req.setMethod("POST");
        req.addHeader("Authorization", "Bearer " + JwtTestSupport.token("inspector-03"));
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        doAnswer(inv -> {
            assertThat(ActorContextHolder.subOr("x")).isEqualTo("inspector-03");
            return null;
        }).when(chain).doFilter(req, res);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(200);
        verify(chain).doFilter(req, res);
    }
}
