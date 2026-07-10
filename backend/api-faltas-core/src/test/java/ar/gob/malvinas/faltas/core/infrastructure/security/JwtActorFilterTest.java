package ar.gob.malvinas.faltas.core.infrastructure.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtActorFilterTest {

    @AfterEach
    void clear() { ActorContextHolder.clear(); }

    @Test
    void sin_token_no_establece_contexto() throws Exception {
        JwtActorFilter filter = new JwtActorFilter();
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

    @Test
    void sub_valido_establece_contexto() throws Exception {
        JwtActorFilter filter = new JwtActorFilter();
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        req.addHeader("Authorization", "Bearer " + token("usuario-demo-faltas"));
        FilterChain chain = mock(FilterChain.class);
        doAnswer(inv -> {
            assertThat(ActorContextHolder.subOr("x")).isEqualTo("usuario-demo-faltas");
            return null;
        }).when(chain).doFilter(req, res);
        filter.doFilter(req, res, chain);
        assertThat(ActorContextHolder.get()).isNull();
    }

    @Test
    void sub_no_uuid_valido_aceptado() throws Exception {
        JwtActorFilter filter = new JwtActorFilter();
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        req.addHeader("Authorization", "Bearer " + token("actor-operativo-123"));
        FilterChain chain = mock(FilterChain.class);
        doAnswer(inv -> {
            assertThat(ActorContextHolder.subOr("x")).isEqualTo("actor-operativo-123");
            return null;
        }).when(chain).doFilter(req, res);
        filter.doFilter(req, res, chain);
    }

    @Test
    void bearer_sin_sub_responde_401() throws Exception {
        JwtActorFilter filter = new JwtActorFilter();
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        req.addHeader("Authorization", "Bearer invalid.token");
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(req, res, chain);
        assertThat(res.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void sub_vacio_responde_401() throws Exception {
        JwtActorFilter filter = new JwtActorFilter();
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        req.addHeader("Authorization", "Bearer " + token(""));
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(req, res, chain);
        assertThat(res.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void sub_mayor_36_responde_401() throws Exception {
        JwtActorFilter filter = new JwtActorFilter();
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        req.addHeader("Authorization", "Bearer " + token("a".repeat(37)));
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(req, res, chain);
        assertThat(res.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void contexto_limpio_tras_request() throws Exception {
        JwtActorFilter filter = new JwtActorFilter();
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        req.addHeader("Authorization", "Bearer " + token("usuario-demo-faltas"));
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(req, res, chain);
        assertThat(ActorContextHolder.get()).isNull();
    }

    @Test
    void sin_token_en_endpoint_protegido_responde_401() throws Exception {
        JwtActorFilter filter = new JwtActorFilter();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/faltas/pagos/notificar-movimiento");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(req, res, chain);
        assertThat(res.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void token_valido_en_endpoint_protegido_pasa() throws Exception {
        JwtActorFilter filter = new JwtActorFilter();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/faltas/pagos/notificar-movimiento");
        req.addHeader("Authorization", "Bearer " + token("usuario-demo-faltas"));
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        doAnswer(inv -> {
            assertThat(ActorContextHolder.subOr("x")).isEqualTo("usuario-demo-faltas");
            return null;
        }).when(chain).doFilter(req, res);
        filter.doFilter(req, res, chain);
        verify(chain).doFilter(req, res);
    }


    @Test
    void sin_token_en_endpoint_numerar_responde_401() throws Exception {
        JwtActorFilter filter = new JwtActorFilter();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/faltas/documentos/42/numerar");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(req, res, chain);
        assertThat(res.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void token_valido_en_endpoint_numerar_pasa() throws Exception {
        JwtActorFilter filter = new JwtActorFilter();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/faltas/documentos/42/numerar");
        req.addHeader("Authorization", "Bearer " + token("firmas-service-prod"));
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        doAnswer(inv -> {
            assertThat(ActorContextHolder.subOr("x")).isEqualTo("firmas-service-prod");
            return null;
        }).when(chain).doFilter(req, res);
        filter.doFilter(req, res, chain);
        verify(chain).doFilter(req, res);
    }

    @Test
    void ruta_numerar_requiere_auth_pero_otra_ruta_docu_no_requiere() throws Exception {
        JwtActorFilter filter = new JwtActorFilter();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/faltas/documentos/42/firmar-real"); // no es /numerar
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(req, res, chain); // sin token -> debe pasar (no protegida)
        assertThat(res.getStatus()).isEqualTo(200);
    }
    private static String token(String sub) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"sub\":\"" + sub + "\"}").getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".sig";
    }
}
