package io.github.ivencn.infra.rbac;

import io.github.ivencn.infra.rbac.aspect.PermissionAspect;
import io.github.ivencn.infra.rbac.scan.PermissionScanner;
import io.github.ivencn.infra.rbac.spi.PermissionRegistry;
import io.github.ivencn.infra.rbac.spi.SuperAdminPolicy;
import io.github.ivencn.infra.core.banner.InfraBannerPrinter;
import jakarta.annotation.PostConstruct;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * iven-starter-rbac 自动装配。开关：iven.rbac.enabled（默认 false）。
 *
 * <p>
 * 需要应用提供 {@link PermissionRegistry} 实现（权限定义持久化）；
 * {@link SuperAdminPolicy} 不提供时默认无超级管理员。
 * </p>
 */
@AutoConfiguration(after = io.github.ivencn.infra.security.SecurityInfraAutoConfiguration.class)
@ConditionalOnProperty(name = "iven.rbac.enabled", havingValue = "true")
public class RbacInfraAutoConfiguration {

    @PostConstruct
    void printBanner() {
        InfraBannerPrinter.printOnce();
    }

    @Bean
    @ConditionalOnBean(PermissionRegistry.class)
    public PermissionScanner permissionScanner(
            @org.springframework.beans.factory.annotation.Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
            PermissionRegistry permissionRegistry) {
        return new PermissionScanner(handlerMapping, permissionRegistry);
    }

    @Bean
    public PermissionAspect permissionAspect(SuperAdminPolicy superAdminPolicy) {
        return new PermissionAspect(superAdminPolicy);
    }

    @Bean
    @ConditionalOnMissingBean
    public SuperAdminPolicy superAdminPolicy() {
        return SuperAdminPolicy.NONE;
    }
}
