package io.github.ivencn.infra.core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 框架级业务异常基类。
 *
 * <p>
 * 应用可继承本异常扩展业务异常层次，或直接抛出以复用上层（如 web 模块）
 * 统一异常处理器的错误响应。仅依赖 spring-web 的 {@link HttpStatus}，无任何自动装配。
 * </p>
 */
@Getter
public class BizException extends RuntimeException {

    private final HttpStatus code;
    private final Object data;

    public BizException(HttpStatus code, String message) {
        this(code, message, null);
    }

    public BizException(HttpStatus code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }
}
