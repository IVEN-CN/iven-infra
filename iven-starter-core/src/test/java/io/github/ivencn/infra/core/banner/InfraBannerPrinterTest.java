package io.github.ivencn.infra.core.banner;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InfraBannerPrinterTest {

    private PrintStream originalOut;
    private ByteArrayOutputStream captured;

    @BeforeEach
    void captureStdout() {
        originalOut = System.out;
        captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
        InfraBannerPrinter.resetForTesting();
    }

    @AfterEach
    void restoreStdout() {
        System.setOut(originalOut);
        InfraBannerPrinter.resetForTesting();
    }

    @Test
    @DisplayName("首次调用打印 IVEN-CN logo 与 IVEN-CN Infra 字样")
    void printsLogoOnFirstCall() {
        boolean printed = InfraBannerPrinter.printOnce();
        assertThat(printed).isTrue();
        String output = captured.toString(StandardCharsets.UTF_8);
        assertThat(output).contains("IVEN-CN Infra");
        assertThat(output).contains("_____");
    }

    @Test
    @DisplayName("同 JVM 内多次调用仅打印一次")
    void printsOnlyOnce() {
        assertThat(InfraBannerPrinter.printOnce()).isTrue();
        assertThat(InfraBannerPrinter.printOnce()).isFalse();
        assertThat(InfraBannerPrinter.printOnce()).isFalse();
        String output = captured.toString(StandardCharsets.UTF_8);
        assertThat(output.split("IVEN-CN Infra", -1).length - 1).isEqualTo(1);
    }
}
