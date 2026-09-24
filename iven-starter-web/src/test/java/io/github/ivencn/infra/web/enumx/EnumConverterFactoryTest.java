package io.github.ivencn.infra.web.enumx;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnumConverterFactoryTest {

    enum PlainEnum {
        FOO,
        BAR
    }

    enum ValuedEnum implements ValueEnum {
        ONE("1"),
        TWO("2");

        private final String value;

        ValuedEnum(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }
    }

    private final EnumConverterFactory factory = new EnumConverterFactory();

    @Test
    @DisplayName("实现 ValueEnum 的枚举按业务值转换")
    void convertsByValue() {
        Converter<String, ValuedEnum> converter = factory.getConverter(ValuedEnum.class);
        assertThat(converter.convert("2")).isEqualTo(ValuedEnum.TWO);
    }

    @Test
    @DisplayName("普通枚举按名称转换（不区分大小写）")
    void convertsByNameFallback() {
        Converter<String, PlainEnum> converter = factory.getConverter(PlainEnum.class);
        assertThat(converter.convert("foo")).isEqualTo(PlainEnum.FOO);
    }

    @Test
    @DisplayName("空值返回 null，无效值抛 IllegalArgumentException")
    void edgeCases() {
        Converter<String, ValuedEnum> converter = factory.getConverter(ValuedEnum.class);
        assertThat(converter.convert("")).isNull();
        assertThat(converter.convert(null)).isNull();
        assertThatThrownBy(() -> converter.convert("bogus")).isInstanceOf(IllegalArgumentException.class);
    }
}
