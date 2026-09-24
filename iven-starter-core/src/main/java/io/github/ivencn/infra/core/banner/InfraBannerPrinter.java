package io.github.ivencn.infra.core.banner;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * IVEN-CN 框架启动横幅打印器。
 *
 * <p>
 * 任一 iven-infra starter 装配时调用 {@link #printOnce()}，同一 JVM 中仅打印一次 IVEN-CN ASCII logo。
 * 全局开关 {@code iven.banner.enabled}（默认 true）由 starter 的自动配置层控制，关闭时不调用本打印器。
 * </p>
 */
public final class InfraBannerPrinter {

    public static final String LOGO = """
                _____    _________   __      _______   __   ____      ____
               /  _/ |  / / ____/ | / /     / ____/ | / /  /  _/___  / __/________ _
               / / | | / / __/ /  |/ /_____/ /   /  |/ /   / // __ \\/ /_/ ___/ __ `/
             _/ /  | |/ / /___/ /|  /_____/ /___/ /|  /  _/ // / / / __/ /  / /_/ /
            /___/  |___/_____/_/ |_/      \\____/_/ |_/  /___/_/ /_/_/ /_/   \\__,_/
            """;

    public static final String TITLE = " :: IVEN-CN Infra ::  (io.github.iven-cn)";

    private static final AtomicBoolean PRINTED = new AtomicBoolean(false);

    private InfraBannerPrinter() {
    }

    /**
     * 打印横幅（若本 JVM 尚未打印过）。
     *
     * @return 本次调用是否实际执行了打印
     */
    public static boolean printOnce() {
        return printOnce(System.out);
    }

    static boolean printOnce(PrintStream out) {
        if (!PRINTED.compareAndSet(false, true)) {
            return false;
        }
        out.println();
        out.println(LOGO);
        out.println(TITLE);
        out.println();
        return true;
    }

    static void resetForTesting() {
        PRINTED.set(false);
    }
}
