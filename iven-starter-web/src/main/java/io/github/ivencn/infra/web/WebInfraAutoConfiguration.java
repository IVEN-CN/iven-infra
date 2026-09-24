package io.github.ivencn.infra.web;

import io.github.ivencn.infra.core.banner.InfraBannerPrinter;
import io.github.ivencn.infra.web.enumx.EnumConverterFactory;
import io.github.ivencn.infra.web.enumx.ValueEnumModule;
import io.github.ivencn.infra.web.exception.GlobalExceptionHandler;
import io.github.ivencn.infra.web.log.RequestLoggingInterceptor;
import io.github.ivencn.infra.web.response.ResponseAdvice;
import jakarta.annotation.PostConstruct;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.format.FormatterRegistry;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * iven-starter-web 自动装配。开关：iven.web.enabled（默认 true）。
 *
 * <p>
 * 提供统一响应体、异常处理、分页 DTO、请求日志拦截器与枚举三通道序列化。
 * </p>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = "iven.web.enabled", havingValue = "true", matchIfMissing = true)
@Import({ResponseAdvice.class, GlobalExceptionHandler.class})
public class WebInfraAutoConfiguration {

    @PostConstruct
    void printBanner() {
        InfraBannerPrinter.printOnce();
    }

    /**
     * Jackson 模块：实现 {@link io.github.ivencn.infra.web.enumx.ValueEnum}
     * 的枚举按业务值序列化/反序列化。
     */
    @Bean
    public ValueEnumModule valueEnumModule() {
        return new ValueEnumModule();
    }

    /**
     * MVC 配置：注册请求日志拦截器与枚举转换器工厂。
     */
    @Bean
    public WebMvcConfigurer ivenWebMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(@NonNull InterceptorRegistry registry) {
                registry.addInterceptor(new RequestLoggingInterceptor());
            }

            @Override
            public void addFormatters(@NonNull FormatterRegistry registry) {
                registry.addConverterFactory(new EnumConverterFactory());
            }
        };
    }
}
