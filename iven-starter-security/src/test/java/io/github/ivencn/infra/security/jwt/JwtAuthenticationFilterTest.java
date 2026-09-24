package io.github.ivencn.infra.security.jwt;

import io.github.ivencn.infra.security.auth.AuthTokenService;
import io.github.ivencn.infra.security.http.FailAuthEntryPoint;
import io.github.ivencn.infra.security.cookie.CookieProperties;
import io.github.ivencn.infra.security.principal.SecurityPrincipal;
import io.github.ivencn.infra.security.principal.UserCTX;
import io.github.ivencn.infra.security.spi.UserDetailsLoader;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private AuthTokenService authTokenService;
    private UserDetailsLoader userDetailsLoader;
    private FailAuthEntryPoint entryPoint;
    private JwtAuthenticationFilter filter;

    private static final Long USER_ID = 42L;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("testsecretkey_must_be_at_least_256_bits_long!!");
        jwtService = new JwtService(props);
        authTokenService = mock(AuthTokenService.class);
        userDetailsLoader = mock(UserDetailsLoader.class);
        entryPoint = mock(FailAuthEntryPoint.class);
        filter = new JwtAuthenticationFilter(jwtService, authTokenService, userDetailsLoader, entryPoint,
                new CookieProperties());
    }

    @AfterEach
    void tearDown() {
        UserCTX.clear();
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest requestWithValidToken() {
        String token = jwtService.issue(USER_ID);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return request;
    }

    @Test
    @DisplayName("有效 token：建立认证上下文并写入 UserCTX（请求结束后清理）")
    void establishesContext() throws Exception {
        MockHttpServletRequest request = requestWithValidToken();
        when(authTokenService.validateToken(any())).thenReturn(Optional.of(USER_ID));
        SecurityPrincipal principal = new SecurityPrincipal(USER_ID, null, Set.of("user:view"));
        when(userDetailsLoader.loadById(USER_ID)).thenReturn(Optional.of(principal));

        FilterChain chain = mock(FilterChain.class);
        AtomicReference<Object> capturedPrincipal = new AtomicReference<>();
        AtomicReference<Object> capturedCtxPrincipal = new AtomicReference<>();
        org.mockito.Mockito.doAnswer(invocation -> {
            capturedPrincipal.set(SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            capturedCtxPrincipal.set(UserCTX.getPrincipal());
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(capturedPrincipal.get()).isEqualTo(principal);
        assertThat(capturedCtxPrincipal.get()).isEqualTo(principal);
        // finally 块清理：请求结束后上下文被清空
        assertThat(UserCTX.getPrincipal()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("白名单不存在该 jti：拒绝并调用入口点")
    void rejectsTokenNotInWhitelist() throws Exception {
        MockHttpServletRequest request = requestWithValidToken();
        when(authTokenService.validateToken(any())).thenReturn(Optional.empty());

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        verify(entryPoint).commence(any(), any(), any(io.github.ivencn.infra.core.exception.BizException.class));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("token 无法解析：拒绝")
    void rejectsUnparseableToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer garbage");

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        verify(entryPoint).commence(any(), any(), any(io.github.ivencn.infra.core.exception.BizException.class));
    }

    @Test
    @DisplayName("用户已被删除：拒绝")
    void rejectsMissingUser() throws Exception {
        MockHttpServletRequest request = requestWithValidToken();
        when(authTokenService.validateToken(any())).thenReturn(Optional.of(USER_ID));
        when(userDetailsLoader.loadById(USER_ID)).thenReturn(Optional.empty());

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        verify(entryPoint).commence(any(), any(), any(io.github.ivencn.infra.core.exception.BizException.class));
        assertThat(UserCTX.getPrincipal()).isNull();
    }

    @Test
    @DisplayName("无 token：直接放行且不做任何校验")
    void passesThroughWithoutToken() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
        verify(entryPoint, never()).commence(any(), any(), any(io.github.ivencn.infra.core.exception.BizException.class));
    }

    @Test
    @DisplayName("从 Cookie 提取 token 优先于 Header")
    void cookieTakesPrecedence() throws Exception {
        String cookieToken = jwtService.issue(USER_ID);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("auth_token", cookieToken));
        when(authTokenService.validateToken(any())).thenReturn(Optional.of(USER_ID));
        when(userDetailsLoader.loadById(USER_ID))
                .thenReturn(Optional.of(new SecurityPrincipal(USER_ID, null, Set.of())));

        FilterChain chain = mock(FilterChain.class);
        AtomicReference<Object> capturedCtxPrincipal = new AtomicReference<>();
        org.mockito.Mockito.doAnswer(invocation -> {
            capturedCtxPrincipal.set(UserCTX.getPrincipal());
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(capturedCtxPrincipal.get()).isEqualTo(new SecurityPrincipal(USER_ID, null, Set.of()));
    }
}
