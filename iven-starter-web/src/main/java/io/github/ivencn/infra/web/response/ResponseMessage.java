package io.github.ivencn.infra.web.response;

import io.github.ivencn.infra.core.exception.BizException;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.Setter;

/**
 * 统一 API 响应体封装。
 *
 * @param <T>
 *            载荷类型
 */
@Setter
@Getter
public class ResponseMessage<T> {

    private Integer code;
    private String msg;
    private T data;

    /**
     * 创建空响应。
     */
    public ResponseMessage() {
    }

    /**
     * 创建带完整字段的响应。
     *
     * @param code
     *            响应码
     * @param msg
     *            响应消息
     * @param data
     *            响应数据
     */
    public ResponseMessage(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 创建默认成功响应。
     *
     * @param data
     *            响应数据
     * @param <T>
     *            载荷类型
     * @return 成功响应
     */
    public static <T> ResponseMessage<T> success(T data) {
        return new ResponseMessage<>(HttpStatus.OK.value(), "Success", data);
    }

    /**
     * 创建自定义消息的成功响应。
     *
     * @param msg
     *            响应消息
     * @param data
     *            响应数据
     * @param <T>
     *            载荷类型
     * @return 成功响应
     */
    public static <T> ResponseMessage<T> success(String msg, T data) {
        return new ResponseMessage<>(HttpStatus.OK.value(), msg, data);
    }

    /**
     * 创建空载荷成功响应。
     *
     * @param <T>
     *            载荷类型
     * @return 成功响应
     */
    public static <T> ResponseMessage<T> success() {
        return success(null);
    }

    /**
     * 使用预定义错误码创建错误响应。
     *
     * @param status
     *            响应状态
     * @param <T>
     *            载荷类型
     * @return 错误响应
     */
    public static <T> ResponseMessage<T> error(HttpStatus status) {
        return new ResponseMessage<>(status.value(), status.getReasonPhrase(), null);
    }

    /**
     * 创建自定义错误响应。
     *
     * @param code
     *            响应码
     * @param msg
     *            响应消息
     * @param <T>
     *            载荷类型
     * @return 错误响应
     */
    public static <T> ResponseMessage<T> error(Integer code, String msg) {
        return new ResponseMessage<>(code, msg, null);
    }

    /**
     * 创建自定义错误响应。
     *
     * @param code
     *            响应码
     * @param msg
     *            响应消息
     * @param data
     *            响应体
     * @param <T>
     *            载荷类型
     * @return 错误响应
     */
    public static <T> ResponseMessage<T> error(Integer code, String msg, T data) {
        return new ResponseMessage<>(code, msg, data);
    }

    /**
     * 使用业务异常创建错误响应。
     *
     * @param e
     *            业务异常
     * @param <T>
     *            载荷类型
     * @return 错误响应
     */
    public static <T> ResponseMessage<T> error(BizException e) {
        return new ResponseMessage<>(e.getCode().value(), e.getMessage(), null);
    }
}
