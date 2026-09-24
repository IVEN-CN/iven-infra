package io.github.ivencn.infra.security.cookie;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Cookie 服务接口，封装认证相关 Cookie 的设置、读取和清除操作。
 */
public interface CookieService {

    /**
     * 设置认证 Token Cookie（HttpOnly）。
     */
    void setAuthTokenCookie(HttpServletResponse response, String token);

    /**
     * 设置 CSRF Token Cookie（非 HttpOnly，允许 JS 读取）。
     */
    void setCsrfTokenCookie(HttpServletResponse response, String csrfToken);

    /**
     * 设置认证相关 Cookie（auth_token + csrf_token），登录时调用。
     */
    default void setAuthCookies(HttpServletResponse response, String authToken, String csrfToken) {
        setAuthTokenCookie(response, authToken);
        setCsrfTokenCookie(response, csrfToken);
    }

    /**
     * 从 Cookie 中读取认证 Token。
     */
    String getAuthTokenFromCookie(HttpServletRequest request);

    /**
     * 从 Cookie 中读取 CSRF Token。
     */
    String getCsrfTokenFromCookie(HttpServletRequest request);

    /**
     * 清除认证相关 Cookie（登出时调用）。
     */
    void clearAuthCookies(HttpServletResponse response);
}
