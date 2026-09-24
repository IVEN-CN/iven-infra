package io.github.ivencn.infra.web.enumx;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 将领域枚举的稳定业务值映射到数据库。
 *
 * <p>
 * 依赖 MyBatis（optional），应用需在 MyBatis 配置中为本模块注册的枚举显式注册该处理器，
 * 或通过 {@code default-enum-type-handler} 全局启用。
 * </p>
 *
 * @param <E>
 *            实现 {@link ValueEnum} 的枚举类型
 */
public class ValueEnumTypeHandler<E extends Enum<E> & ValueEnum> extends BaseTypeHandler<E> {

    private final Class<E> enumType;

    public ValueEnumTypeHandler(Class<E> enumType) {
        if (enumType == null) {
            throw new IllegalArgumentException("enumType must not be null");
        }
        this.enumType = enumType;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.getValue());
    }

    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private E parse(String value) {
        if (value == null) {
            return null;
        }
        for (E constant : enumType.getEnumConstants()) {
            if (constant.getValue().equals(value)) {
                return constant;
            }
        }
        throw new IllegalArgumentException("Unknown enum value '" + value + "' for " + enumType.getSimpleName());
    }
}
