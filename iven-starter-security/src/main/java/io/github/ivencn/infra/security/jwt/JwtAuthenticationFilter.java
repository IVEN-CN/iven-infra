package io.github.ivencn.infra.security.jwt;

import io.github.ivencn.infra.security.auth.AuthTokenService;
import io.github.ivencn.infra.security.cookie.CookieProperties;
import io.github.ivencn.infra.security.http.FailAuthEntryPoint;
import io.github.ivencn.infra.security.principal.SecurityPrincipal;
import io.github.ivencn.infra.security.principal.UserCTX;
import io.github.ivencn.infra.security.spi.UserDetailsLoader;
import io.github.ivencn.infra.core.exception.BizException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * JWT 认证过滤器，优先从 Cookie 中提取 JWT Token，fallback 到 Authorization Header。
 *
 * <p>
 * 用户信息的取数完全由 {@link UserDetailsLoader} SPI 决定，过滤器不依赖任何业务类型。
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthTokenService authTokenService;
    private final UserDetailsLoader userDetailsLoader;
    private final FailAuthEntryPoint failAuthEntryPoint;
    private final CookieProperties cookieProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = extractJwtFromRequest(request);

            if (jwt != null) {
                JwtClaims payload = jwtService.parse(jwt);

                if (payload != null) {
                    Optional<Long> userId = authTokenService.validateToken(payload.getJti());

                    if (userId.isPresent() && userId.get().equals(payload.getSubjectId())) {
                        Optional<SecurityPrincipal> principalOpt = userDetailsLoader.loadById(userId.get());
                        if (principalOpt.isEmpty()) {
                            log.warn("User not found for userId: {}", userId.get());
                            failAuthEntryPoint.commence(request, response,
                                    new BizException(HttpStatus.UNAUTHORIZED, "用户不存在"));
                            return;
                        }

                        SecurityPrincipal principal = principalOpt.get();
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                principal, payload, Collections.emptyList());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        UserCTX.setPrincipal(principal);

                        log.debug("JWT authenticated for user: {}", payload.getSubjectId());
                    } else {
                        // 白名单没有，说明已经退出，非法 token
                        log.warn("Token not found in whitelist or userId mismatch: {}", payload.getJti());
                        failAuthEntryPoint.commence(request, response,
                                new BizException(HttpStatus.UNAUTHORIZED, "无效的Token"));
                        return;
                    }
                } else {
                    log.debug("Failed to parse JWT token");
                    failAuthEntryPoint.commence(request, response,
                            new BizException(HttpStatus.UNAUTHORIZED, "无效的Token"));
                    return;
                }
            }

            filterChain.doFilter(request, response);

        } finally {
            UserCTX.clear();
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 从请求中提取 JWT 令牌，优先 Cookie，fallback 到 Authorization Header。
     *
     * @param request
     *            HTTP 请求
     * @return JWT 令牌，如果没有则返回 null
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String tokenFromCookie = extractJwtFromCookie(request);
        if (tokenFromCookie != null) {
            log.debug("JWT extracted from cookie");
            return tokenFromCookie;
        }

        String tokenFromHeader = extractJwtFromHeader(request);
        if (tokenFromHeader != null) {
            log.debug("JWT extracted from Authorization header (fallback)");
            return tokenFromHeader;
        }

        return null;
    }

    /**
     * 从 Cookie 中提取 JWT Token。
     */
    private String extractJwtFromCookie(HttpServletRequest request) {
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
     * 从 Authorization Header 中提取 JWT Token。
     */
    private String extractJwtFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}
