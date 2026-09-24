package io.github.ivencn.infra.web.enumx;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

/**
 * 枚举转换器工厂。
 *
 * <p>
 * 用于将字符串转换为枚举类型，支持 {@link ValueEnum} 稳定业务值。 如果枚举实现了
 * {@link ValueEnum}，则根据业务值进行转换；否则根据枚举名称进行转换（不区分大小写兜底）。
 * </p>
 */
public class EnumConverterFactory implements ConverterFactory<String, Enum<?>> {

    @Override
    public <T extends Enum<?>> Converter<String, T> getConverter(Class<T> targetType) {
        return new StringToEnumConverter<>(targetType);
    }

    private static class StringToEnumConverter<T extends Enum<?>> implements Converter<String, T> {
        private final Class<T> enumType;

        public StringToEnumConverter(Class<T> enumType) {
            this.enumType = enumType;
        }

        @Override
        public T convert(String source) {
            if (source == null || source.isEmpty()) {
                return null;
            }

            // 优先根据领域枚举的稳定业务值进行转换，避免依赖持久化框架注解。
            T[] enumConstants = enumType.getEnumConstants();
            for (T constant : enumConstants) {
                if (constant instanceof ValueEnum valueEnum && source.equals(valueEnum.getValue())) {
                    return constant;
                }
            }

            // 如果未找到匹配业务值，则尝试根据枚举名称进行转换
            try {
                // 使用反射调用Enum.valueOf方法，避免泛型类型约束问题
                return (T) Enum.class.getMethod("valueOf", Class.class, String.class)
                        .invoke(
                                null,
                                enumType,
                                source.toUpperCase());
            } catch (Exception e) {
                // 如果仍然失败，尝试不区分大小写查找
                for (T constant : enumConstants) {
                    if (constant.name().equalsIgnoreCase(source)) {
                        return constant;
                    }
                }
                if (e.getCause() instanceof IllegalArgumentException) {
                    throw (IllegalArgumentException) e.getCause();
                }
                throw new IllegalArgumentException("Cannot convert " + source + " to " + enumType.getSimpleName(), e);
            }
        }
    }
}
