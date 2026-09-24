package io.github.ivencn.infra.security.http;

import io.github.ivencn.infra.security.cookie.CookieService;
import io.github.ivencn.infra.core.exception.BizException;
import io.github.ivencn.infra.web.response.ResponseMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * 认证失败入口点：输出统一错误响应结构；token 失效时清除认证 Cookie。
 */
@RequiredArgsConstructor
public class FailAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CookieService cookieService;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        ResponseMessage<Object> responseMessage = ResponseMessage.error(403, authException.getMessage());
        response.getWriter().write(objectMapper.writeValueAsString(responseMessage));
    }

    /**
     * 业务异常入口（token 失效等）：清除认证 Cookie 并输出异常对应的错误响应。
     */
    public void commence(HttpServletRequest request, HttpServletResponse response, BizException exception)
            throws IOException, ServletException {
        cookieService.clearAuthCookies(response);
        response.setStatus(exception.getCode().value());
        response.setContentType("application/json;charset=UTF-8");
        ResponseMessage<Object> responseMessage = ResponseMessage.error(exception);
        response.getWriter().write(objectMapper.writeValueAsString(responseMessage));
    }
}
