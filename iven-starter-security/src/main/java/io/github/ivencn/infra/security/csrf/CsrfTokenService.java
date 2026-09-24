package io.github.ivencn.infra.security.csrf;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * CSRF Token 服务接口，提供 CSRF Token 的生成、存储、验证功能。
 */
public interface CsrfTokenService {

    /**
     * 生成新的 CSRF Token。
     */
    String generateCsrfToken();

    /**
     * 设置 CSRF Token 到 Cookie（非 HttpOnly，允许前端读取）。
     */
    void setCsrfTokenCookie(HttpServletResponse response, String csrfToken);

    /**
     * 从 Cookie 中获取 CSRF Token。
     */
    String getCsrfTokenFromCookie(HttpServletRequest request);

    /**
     * 从请求头中获取 CSRF Token。
     */
    String getCsrfTokenFromHeader(HttpServletRequest request);

    /**
     * 验证 CSRF Token（Double Submit Cookie 模式），比较 Cookie 与 Header 中 token 是否一致。
     */
    boolean validateCsrfToken(HttpServletRequest request);

    /**
     * 清除 CSRF Token Cookie。
     */
    void clearCsrfTokenCookie(HttpServletResponse response);
}
