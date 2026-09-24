package io.github.ivencn.infra.storage.magic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FileMagicCheckerTest {

    private final FileMagicChecker checker = new FileMagicChecker();

    private static final byte[] JPEG_HEADER = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00 };
    private static final byte[] PNG_HEADER = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
    private static final byte[] PDF_HEADER = new byte[] { 0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x37 };
    private static final byte[] EXE_HEADER = new byte[] { 0x4D, 0x5A, (byte) 0x90, 0x00 };

    @Test
    @DisplayName("魔数与声明的 Content-Type 匹配时通过")
    void matchingMagicPasses() {
        assertThat(checker.isValid("image/jpeg", JPEG_HEADER)).isTrue();
        assertThat(checker.isValid("image/png", PNG_HEADER)).isTrue();
        assertThat(checker.isValid("application/pdf", PDF_HEADER)).isTrue();
    }

    @Test
    @DisplayName("伪装扩展名：可执行文件头声明为图片时拒绝")
    void mismatchedMagicRejected() {
        assertThat(checker.isValid("image/png", EXE_HEADER)).isFalse();
        assertThat(checker.isValid("image/jpeg", EXE_HEADER)).isFalse();
        assertThat(checker.isValid("application/pdf", EXE_HEADER)).isFalse();
        assertThat(checker.isValid("application/zip", EXE_HEADER)).isFalse();
    }

    @Test
    @DisplayName("无法判断的 Content-Type 或过小文件头放行")
    void undecidablePasses() {
        assertThat(checker.isValid("text/plain", EXE_HEADER)).isTrue();
        assertThat(checker.isValid(null, EXE_HEADER)).isTrue();
        assertThat(checker.isValid("image/png", new byte[] { 0x01 })).isTrue();
        assertThat(checker.isValid("image/png", null)).isTrue();
    }
}
