package io.github.ivencn.infra.rbac.scan;

import io.github.ivencn.infra.rbac.model.PermissionDefinition;
import io.github.ivencn.infra.rbac.spi.PermissionRegistry;
import io.github.ivencn.infra.rbac.validate.PermissionResolver;
import io.github.ivencn.infra.rbac.validate.PermissionValidator;
import io.github.ivencn.infra.security.principal.AccessLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 权限扫描器。
 *
 * <p>
 * 启动时扫描所有 Controller 方法的 {@code @RequiresPermission} 注解，校验格式与
 * value 全局唯一性（同名 value 的 name/access 冲突时启动失败），产出
 * {@link PermissionDefinition} 列表并交付 {@link PermissionRegistry} SPI 同步。
 * 差异计算与持久化由应用实现。
 * </p>
 *
 * <p>
 * 无需 {@code @Order}：控制器映射通过构造函数注入形成依赖，Spring 保证路由表先初始化完成。
 * </p>
 */
public class PermissionScanner implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(PermissionScanner.class);

    private final RequestMappingHandlerMapping handlerMapping;
    private final PermissionRegistry permissionRegistry;

    public PermissionScanner(RequestMappingHandlerMapping handlerMapping, PermissionRegistry permissionRegistry) {
        this.handlerMapping = handlerMapping;
        this.permissionRegistry = permissionRegistry;
    }

    @Override
    public void afterPropertiesSet() {
        logger.info("Starting permission scan...");
        long startTime = System.currentTimeMillis();

        try {
            List<PermissionDefinition> scanned = scanControllerMethods();
            assertNoConflicts(scanned);
            permissionRegistry.sync(scanned);
            logger.info("Permission scan completed in {}ms, {} entries", System.currentTimeMillis() - startTime,
                    scanned.size());
        } catch (Exception e) {
            logger.error("Permission scan failed", e);
            throw new RuntimeException("Failed to scan permissions", e);
        }
    }

    /**
     * 扫描 Controller 方法产出权限定义。
     */
    List<PermissionDefinition> scanControllerMethods() {
        List<PermissionDefinition> permissions = new ArrayList<>();
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            RequestMappingInfo mappingInfo = entry.getKey();
            HandlerMethod handlerMethod = entry.getValue();

            PermissionResolver.PermissionInfo permissionInfo = PermissionResolver.resolve(handlerMethod);
            if (!permissionInfo.hasPermission()) {
                continue;
            }

            PermissionValidator.validate(
                    permissionInfo.getValue(),
                    handlerMethod.getBeanType().getName(),
                    handlerMethod.getMethod().getName());

            String url = extractUrl(mappingInfo);

            Set<RequestMethod> methods = mappingInfo.getMethodsCondition().getMethods();
            if (methods.isEmpty()) {
                methods = EnumSet.of(RequestMethod.GET);
            }

            for (RequestMethod method : methods) {
                permissions.add(new PermissionDefinition(
                        permissionInfo.getValue(),
                        permissionInfo.getName(),
                        url,
                        method.name(),
                        permissionInfo.getAccess()));
            }
        }

        return permissions;
    }

    /**
     * 校验同一 value 的定义不产生 name/access 冲突（全局唯一性）。
     *
     * @throws IllegalStateException
     *             同一 value 出现不同 name 或 access 时
     */
    private void assertNoConflicts(List<PermissionDefinition> scanned) {
        Map<String, PermissionDefinition> byValue = new HashMap<>();
        for (PermissionDefinition def : scanned) {
            PermissionDefinition existing = byValue.putIfAbsent(def.value(), def);
            if (existing != null
                    && (!existing.name().equals(def.name()) || existing.accessLevel() != def.accessLevel())) {
                throw new IllegalStateException(String.format(
                        "Duplicate permission value '%s' with conflicting name/access: %s vs %s",
                        def.value(), existing, def));
            }
        }
    }

    /**
     * 提取 URL 路径（只取第一个）。
     */
    private String extractUrl(RequestMappingInfo mappingInfo) {
        if (mappingInfo.getPathPatternsCondition() != null) {
            return mappingInfo.getPathPatternsCondition().getPatternValues().iterator().next();
        }
        return "";
    }
}
