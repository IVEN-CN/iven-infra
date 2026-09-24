package io.github.ivencn.infra.web.enumx;

import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ValueEnumTypeHandlerTest {

    enum Level implements ValueEnum {
        LOW("low"),
        HIGH("high");

        private final String value;

        Level(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }
    }

    private final ValueEnumTypeHandler<Level> handler = new ValueEnumTypeHandler<>(Level.class);

    @Test
    @DisplayName("写入时使用业务值")
    void setsValue() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        handler.setNonNullParameter(ps, 1, Level.HIGH, JdbcType.VARCHAR);
        verify(ps).setString(1, "high");
    }

    @Test
    @DisplayName("按列名读取并按业务值还原枚举")
    void getsByColumnName() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("level")).thenReturn("low");
        assertThat(handler.getNullableResult(rs, "level")).isEqualTo(Level.LOW);
    }

    @Test
    @DisplayName("null 列值返回 null")
    void nullValue() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("level")).thenReturn(null);
        assertThat(handler.getNullableResult(rs, "level")).isNull();
    }

    @Test
    @DisplayName("未知业务值抛 IllegalArgumentException")
    void unknownValue() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("level")).thenReturn("bogus");
        assertThatThrownBy(() -> handler.getNullableResult(rs, "level"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
