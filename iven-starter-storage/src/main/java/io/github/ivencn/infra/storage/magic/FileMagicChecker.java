package io.github.ivencn.infra.storage.magic;

/**
 * 文件魔数检查器。
 *
 * <p>
 * 通过文件头字节校验 Content-Type 是否真实匹配文件内容。
 * </p>
 */
public class FileMagicChecker {

    private static final byte[] JPEG_MAGIC = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };
    private static final byte[] PNG_MAGIC = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
    private static final byte[] PDF_MAGIC = new byte[] { 0x25, 0x50, 0x44, 0x46 };
    private static final byte[] ZIP_MAGIC = new byte[] { 0x50, 0x4B, 0x03, 0x04 };

    /**
     * 校验文件头字节是否与声明的 Content-Type 匹配。
     *
     * @param contentType
     *            声明的 Content-Type
     * @param header
     *            文件前几个字节
     * @return true 表示匹配或无法判断，false 表示不匹配
     */
    public boolean isValid(String contentType, byte[] header) {
        if (header == null || header.length < 3) {
            return true;
        }
        if (contentType == null || contentType.isBlank()) {
            return true;
        }

        String lowerCt = contentType.toLowerCase();
        return switch (lowerCt) {
            case "image/jpeg", "image/jpg" -> startsWith(header, JPEG_MAGIC);
            case "image/png" -> startsWith(header, PNG_MAGIC);
            case "application/pdf" -> startsWith(header, PDF_MAGIC);
            case "application/zip" -> startsWith(header, ZIP_MAGIC);
            default -> true;
        };
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
