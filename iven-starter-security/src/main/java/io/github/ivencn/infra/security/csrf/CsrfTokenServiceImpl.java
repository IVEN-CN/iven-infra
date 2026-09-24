package io.github.ivencn.infra.security.csrf;

import io.github.ivencn.infra.security.cookie.CookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * CSRF Token 服务实现类，使用 Double Submit Cookie 模式实现 CSRF 防护。
 */
@Slf4j
@RequiredArgsConstructor
public class CsrfTokenServiceImpl implements CsrfTokenService {

    private static final int TOKEN_LENGTH = 32; // 32 字节 = 256 位

    private final SecureRandom secureRandom = new SecureRandom();
    private final CookieProperties cookieProperties;
    private final CsrfProperties csrfProperties;

    @Override
    public String generateCsrfToken() {
        byte[] bytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public void setCsrfTokenCookie(HttpServletResponse response, String csrfToken) {
        response.addHeader("Set-Cookie", buildCsrfCookieHeader(csrfToken));
        log.debug("Set CSRF token cookie");
    }

    @Override
    public String getCsrfTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        String csrfCookieName = cookieProperties.getCsrfCookieName();
        for (Cookie cookie : cookies) {
            if (csrfCookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    @Override
    public String getCsrfTokenFromHeader(HttpServletRequest request) {
        return request.getHeader(csrfProperties.getHeaderName());
    }

    @Override
    public boolean validateCsrfToken(HttpServletRequest request) {
        String cookieToken = getCsrfTokenFromCookie(request);
        String headerToken = getCsrfTokenFromHeader(request);

        if (cookieToken == null || headerToken == null) {
            log.warn(
                    "CSRF token missing - cookie: {}, header: {}",
                    cookieToken != null ? "present" : "null",
                    headerToken != null ? "present" : "null");
            return false;
        }

        boolean valid = constantTimeEquals(cookieToken, headerToken);
        if (!valid) {
            log.warn("CSRF token mismatch");
        }
        return valid;
    }

    @Override
    public void clearCsrfTokenCookie(HttpServletResponse response) {
        response.addHeader("Set-Cookie", buildClearCsrfCookieHeader());
        log.debug("Cleared CSRF token cookie");
    }

    /**
     * 构建 CSRF Token Cookie 的 Set-Cookie header 值（不设置 HttpOnly，允许前端 JS 读取）。
     */
    private String buildCsrfCookieHeader(String csrfToken) {
        StringBuilder sb = new StringBuilder();
        sb.append(cookieProperties.getCsrfCookieName()).append("=").append(csrfToken);
        sb.append("; Path=").append(cookieProperties.getPath());
        sb.append("; Max-Age=").append(cookieProperties.getMaxAge());
        sb.append("; SameSite=").append(cookieProperties.getSameSite());

        if (cookieProperties.isSecure()) {
            sb.append("; Secure");
        }

        String domain = cookieProperties.getDomain();
        if (domain != null && !domain.isEmpty()) {
            sb.append("; Domain=").append(domain);
        }

        return sb.toString();
    }

    /**
     * 构建清除 CSRF Token Cookie 的 Set-Cookie header 值。
     */
    private String buildClearCsrfCookieHeader() {
        StringBuilder sb = new StringBuilder();
        sb.append(cookieProperties.getCsrfCookieName()).append("=;");
        sb.append("; Path=").append(cookieProperties.getPath());
        sb.append("; Max-Age=0");

        String domain = cookieProperties.getDomain();
        if (domain != null && !domain.isEmpty()) {
            sb.append("; Domain=").append(domain);
        }

        return sb.toString();
    }

    /**
     * 常量时间字符串比较，防止时序攻击。
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }

        return result == 0;
    }
}
