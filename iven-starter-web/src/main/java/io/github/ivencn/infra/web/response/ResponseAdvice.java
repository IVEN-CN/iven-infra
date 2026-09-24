package io.github.ivencn.infra.web.response;

import io.github.ivencn.infra.web.response.ResponseMessage;

import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 响应体建议：将 {@link ResponseMessage} 携带的业务状态码同步为 HTTP 状态码。
 */
@ControllerAdvice
public class ResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        // 返回 true 以便在 beforeBodyWrite 中检查所有返回值类型
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
            MethodParameter returnType,
            org.springframework.http.MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {

        Integer status = null;

        if (body instanceof ResponseMessage) {
            status = ((ResponseMessage<?>) body).getCode();
        } else if (body instanceof ResponseEntity) {
            Object inner = ((ResponseEntity<?>) body).getBody();
            if (inner instanceof ResponseMessage) {
                status = ((ResponseMessage<?>) inner).getCode();
            }
        }

        if (status != null && response instanceof ServletServerHttpResponse) {
            ((ServletServerHttpResponse) response).getServletResponse().setStatus(status);
        }

        return body;
    }
}
