package io.github.ivencn.infra.web.enumx;

/**
 * 领域枚举的稳定业务值。
 *
 * <p>
 * 实现该接口的枚举在 MVC 参数绑定、JSON 序列化与持久化三个通道统一按
 * {@link #getValue()} 进行转换，无需手写 {@code @JsonCreator}/{@code @JsonValue}。
 * </p>
 */
public interface ValueEnum {

    String getValue();
}
