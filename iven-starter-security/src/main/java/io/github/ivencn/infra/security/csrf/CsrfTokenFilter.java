package io.github.ivencn.infra.security.csrf;

import io.github.ivencn.infra.security.cookie.CookieProperties;
import io.github.ivencn.infra.security.principal.UserCTX;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * CSRF Token 验证过滤器，对需要认证的状态修改请求进行 Double Submit Cookie 验证。
 *
 * <p>
 * 排除路径由 {@link CsrfProperties#getExcludedPaths()} 配置。
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class CsrfTokenFilter extends OncePerRequestFilter {

    private final CsrfTokenService csrfTokenService;
    private final CookieProperties cookieProperties;
    private final CsrfProperties csrfProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            if (!requiresCsrfValidation(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            if (!isUserAuthenticated(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            if (!csrfTokenService.validateCsrfToken(request)) {
                log.warn("CSRF validation failed for request: {} {}", request.getMethod(), request.getRequestURI());
                sendCsrfError(response);
                return;
            }

            log.debug("CSRF validation passed for request: {} {}", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);

        } finally {
            // 保险：确保 UserCTX 被清理
            UserCTX.clear();
        }
    }

    /**
     * 判断请求是否需要 CSRF 验证：状态修改方法且不在排除路径中。
     */
    private boolean requiresCsrfValidation(HttpServletRequest request) {
        String method = request.getMethod();

        boolean isModifyingMethod = HttpMethod.POST.matches(method)
                || HttpMethod.PUT.matches(method)
                || HttpMethod.DELETE.matches(method)
                || HttpMethod.PATCH.matches(method);

        if (!isModifyingMethod) {
            return false;
        }

        String requestURI = request.getRequestURI();
        for (String pattern : csrfProperties.getExcludedPaths()) {
            if (pathMatcher.match(pattern, requestURI)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 检查用户是否已登录（未登录用户不需要 CSRF 保护）。 跳过无 Cookie 但有认证信息的测试场景。
     */
    private boolean isUserAuthenticated(HttpServletRequest request) {
        boolean hasAuthentication = SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
                && UserCTX.getPrincipal() != null;

        if (!hasAuthentication) {
            return false;
        }

        String authCookie = extractAuthCookie(request);
        if (authCookie == null) {
            log.debug("Skipping CSRF validation for test scenario (no cookie but authenticated)");
            return false;
        }

        return true;
    }

    /**
     * 从请求中提取认证 Cookie。
     */
    private String extractAuthCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        String authCookieName = cookieProperties.getAuthCookieName();
        for (Cookie cookie : cookies) {
            if (authCookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * 发送 CSRF 验证失败的错误响应。
     */
    private void sendCsrfError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"msg\":\"CSRF Token 无效或缺失\",\"data\":null}");
    }
}
