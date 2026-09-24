package io.github.ivencn.infra.web.exception;

import io.github.ivencn.infra.core.exception.BizException;
import io.github.ivencn.infra.web.response.ResponseMessage;

import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import lombok.extern.slf4j.Slf4j;

/**
 * 全局异常处理器，将异常映射为 {@link ResponseMessage} 统一错误结构。
 *
 * <p>
 * 应用可通过自定义 {@code @RestControllerAdvice} 追加业务异常的映射。
 * </p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常（@Valid）。
     *
     * @param ex
     *            校验异常
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseMessage<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().isEmpty()
                ? HttpStatus.BAD_REQUEST.getReasonPhrase()
                : ex.getBindingResult().getAllErrors().getFirst().getDefaultMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseMessage.error(HttpStatus.BAD_REQUEST.value(), message));
    }

    /**
     * 处理参数校验异常（@Validated 的方法参数约束，ConstraintViolationException）。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResponseMessage<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations()
                .stream()
                .findFirst()
                .map(v -> v.getMessage())
                .orElse(HttpStatus.BAD_REQUEST.getReasonPhrase());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseMessage.error(HttpStatus.BAD_REQUEST.value(), message));
    }

    /**
     * 处理参数绑定异常。
     *
     * @param ex
     *            绑定异常
     * @return 错误响应
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ResponseMessage<Void>> handleBindException(BindException ex) {
        String message = ex.getAllErrors().isEmpty()
                ? HttpStatus.BAD_REQUEST.getReasonPhrase()
                : ex.getAllErrors().getFirst().getDefaultMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseMessage.error(HttpStatus.BAD_REQUEST.value(), message));
    }

    /**
     * 处理缺少必需请求参数异常。
     *
     * @param ex
     *            缺少参数异常
     * @return 错误响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ResponseMessage<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex) {
        String message = "缺少必需的请求参数: " + ex.getParameterName();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseMessage.error(HttpStatus.BAD_REQUEST.value(), message));
    }

    /**
     * 处理方法参数类型不匹配异常（如枚举转换失败）。
     *
     * @param ex
     *            类型不匹配异常
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ResponseMessage<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex) {
        String message = "参数类型不匹配: " + ex.getName();
        if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
            message = "无效的枚举值: " + ex.getValue();
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseMessage.error(HttpStatus.BAD_REQUEST.value(), message));
    }

    /**
     * 处理框架/业务异常。
     *
     * @param ex
     *            业务异常
     * @return 错误响应，携带异常中的附加数据（若有）
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<ResponseMessage<?>> handleBizException(BizException ex) {
        logException(ex);
        if (ex.getData() != null) {
            return ResponseEntity.status(ex.getCode())
                    .body(ResponseMessage.error(ex.getCode().value(), ex.getMessage(), ex.getData()));
        }
        return ResponseEntity.status(ex.getCode())
                .body(ResponseMessage.error(ex.getCode().value(), ex.getMessage()));
    }

    /**
     * 处理非法参数异常。
     *
     * @param ex
     *            非法参数异常
     * @return 错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseMessage<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        logException(ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseMessage.error(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    /**
     * 处理未捕获的系统异常。
     *
     * @param ex
     *            系统异常
     * @return 错误响应，不泄漏堆栈信息
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseMessage<Void>> handleException(Exception ex) {
        logException(ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseMessage.error(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private void logException(Exception ex) {
        log.error("未捕获的异常: {}", ex.getMessage(), ex);
    }
}
