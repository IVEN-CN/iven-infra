package io.github.ivencn.infra.web.enumx;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValueEnumModuleTest {

    enum TestStatus implements ValueEnum {
        ACTIVE("active"),
        INACTIVE("inactive");

        private final String value;

        TestStatus(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }
    }

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new ValueEnumModule());

    @Test
    @DisplayName("序列化保持默认枚举名（兼容既有前端协议），显式 @JsonValue 的枚举除外")
    void serializesByNameByDefault() throws Exception {
        assertThat(mapper.writeValueAsString(TestStatus.ACTIVE)).isEqualTo("\"ACTIVE\"");
    }

    @Test
    @DisplayName("反序列化按业务值匹配")
    void deserializesByValue() throws Exception {
        assertThat(mapper.readValue("\"inactive\"", TestStatus.class)).isEqualTo(TestStatus.INACTIVE);
    }

    @Test
    @DisplayName("反序列化兜底接受枚举名（兼容既有协议）")
    void deserializesByEnumNameFallback() throws Exception {
        assertThat(mapper.readValue("\"ACTIVE\"", TestStatus.class)).isEqualTo(TestStatus.ACTIVE);
    }

    @Test
    @DisplayName("无效值抛出异常")
    void invalidValueThrows() {
        assertThatThrownBy(() -> mapper.readValue("\"bogus\"", TestStatus.class))
                .hasMessageContaining("bogus");
    }

    @Test
    @DisplayName("作为 POJO 字段参与完整 JSON 往返（输出枚举名，输入接受业务值）")
    void fieldRoundTrip() throws Exception {
        record Wrapper(TestStatus status) {
        }
        Wrapper wrapper = new Wrapper(TestStatus.ACTIVE);
        String json = mapper.writeValueAsString(wrapper);
        assertThat(json).contains("\"status\":\"ACTIVE\"");
        assertThat(mapper.readValue(json, Wrapper.class)).isEqualTo(wrapper);
    }
}
