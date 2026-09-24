package io.github.ivencn.infra.web.util;

/**
 * 通用字符串工具类。
 */
public final class StringUtils {

    private StringUtils() {
        // 工具类，禁止实例化
    }

    /**
     * 将字符串截断至指定长度，超长时追加省略号 "..."。
     *
     * <p>
     * 边界处理：
     * </p>
     * <ul>
     * <li>{@code value} 为 {@code null} 时返回 {@code null}</li>
     * <li>长度小于等于 {@code maxLength} 时原样返回</li>
     * <li>{@code maxLength} 不足以容纳 "..." 时，直接返回前 {@code maxLength} 个字符</li>
     * <li>{@code maxLength} 为负数时抛出 {@link IllegalArgumentException}</li>
     * </ul>
     *
     * @param value
     *            原始字符串
     * @param maxLength
     *            最大长度（必须大于等于 0）
     * @return 截断后的字符串
     */
    public static String truncateWithEllipsis(String value, int maxLength) {
        if (maxLength < 0) {
            throw new IllegalArgumentException("maxLength 必须大于等于 0");
        }
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        if (maxLength <= 3) {
            return value.substring(0, maxLength);
        }
        return value.substring(0, maxLength - 3) + "...";
    }
}
