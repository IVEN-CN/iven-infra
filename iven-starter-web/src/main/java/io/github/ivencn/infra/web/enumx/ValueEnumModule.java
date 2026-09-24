package io.github.ivencn.infra.web.enumx;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;

/**
 * Jackson 模块：增强实现 {@link ValueEnum} 的枚举反序列化。
 *
 * <p>
 * 反序列化优先按 {@link ValueEnum#getValue()} 匹配，兜底按枚举名（含大小写不敏感），
 * 全部失败抛出 {@link JsonMappingException}。序列化行为保持 Jackson 默认（枚举名），
 * 需要输出业务值的枚举可在常量上显式声明 {@code @JsonValue}，不受影响。
 * 注册为 Spring Bean 后自动生效，业务枚举无需手写 {@code @JsonCreator}。
 * </p>
 */
public class ValueEnumModule extends SimpleModule {

    public ValueEnumModule() {
        super("iven-value-enum");
    }

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        context.addDeserializers(new ValueEnumDeserializers());
    }

    static class ValueEnumDeserializers extends Deserializers.Base {

        @Override
        @SuppressWarnings("unchecked")
        public JsonDeserializer<?> findEnumDeserializer(Class<?> enumType,
                DeserializationConfig config,
                BeanDescription beanDesc) {
            if (ValueEnum.class.isAssignableFrom(enumType)) {
                return new ValueEnumDeserializer((Class<? extends Enum<?>>) enumType.asSubclass(Enum.class));
            }
            return null;
        }
    }

    /**
     * 按目标枚举类型解析 value/name 的通用反序列化器。
     */
    static class ValueEnumDeserializer extends JsonDeserializer<ValueEnum> {

        private final Class<? extends Enum<?>> enumType;

        ValueEnumDeserializer(Class<? extends Enum<?>> enumType) {
            this.enumType = enumType;
        }

        @Override
        public ValueEnum deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (enumType == null) {
                throw JsonMappingException.from(p, "ValueEnum target type is unknown");
            }
            String source = p.getValueAsString();
            if (source == null || source.isEmpty()) {
                return null;
            }
            for (Enum<?> constant : enumType.getEnumConstants()) {
                if (source.equals(((ValueEnum) constant).getValue())) {
                    return (ValueEnum) constant;
                }
            }
            try {
                @SuppressWarnings({ "unchecked", "rawtypes" })
                ValueEnum byName = (ValueEnum) Enum.valueOf((Class) enumType, source);
                return byName;
            } catch (IllegalArgumentException ignored) {
                // 继续尝试不区分大小写匹配
            }
            for (Enum<?> constant : enumType.getEnumConstants()) {
                if (constant.name().equalsIgnoreCase(source)) {
                    return (ValueEnum) constant;
                }
            }
            throw JsonMappingException.from(p,
                    "Cannot convert '" + source + "' to " + enumType.getSimpleName());
        }
    }
}
