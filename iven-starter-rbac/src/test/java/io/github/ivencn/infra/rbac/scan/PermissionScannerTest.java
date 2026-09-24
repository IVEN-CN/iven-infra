package io.github.ivencn.infra.rbac.scan;

import io.github.ivencn.infra.rbac.model.PermissionDefinition;
import io.github.ivencn.infra.rbac.spi.PermissionRegistry;
import io.github.ivencn.infra.security.annotation.RequiresPermission;
import io.github.ivencn.infra.security.principal.AccessLevel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionScannerTest {

    @RestController
    @RequestMapping("/users")
    static class UserController {

        @RequiresPermission(value = "user:create", name = "创建用户")
        @GetMapping("/create")
        public String create() {
            return "ok";
        }

        @GetMapping("/plain")
        public String plain() {
            return "ok";
        }
    }

    @RestController
    static class BadController {

        @RequiresPermission(value = "user:create", name = "另一个名字")
        @GetMapping("/other")
        public String other() {
            return "ok";
        }
    }

    @RestController
    static class InvalidFormatController {

        @RequiresPermission(value = "USER_CREATE")
        @GetMapping("/bad")
        public String bad() {
            return "ok";
        }
    }

    private final List<PermissionDefinition> synced = new ArrayList<>();

    private final PermissionRegistry registry = definitions -> synced.addAll(definitions);

    private RequestMappingHandlerMapping mapping(Object controller, Method method, String path,
            RequestMethod httpMethod) throws Exception {
        RequestMappingInfo info = RequestMappingInfo.paths(path).methods(httpMethod).build();
        HandlerMethod handlerMethod = new HandlerMethod(controller, method);
        Map<RequestMappingInfo, HandlerMethod> methods = new HashMap<>();
        methods.put(info, handlerMethod);
        RequestMappingHandlerMapping handlerMapping = mock(RequestMappingHandlerMapping.class);
        when(handlerMapping.getHandlerMethods()).thenReturn(methods);
        return handlerMapping;
    }

    @BeforeEach
    void reset() {
        synced.clear();
    }

    @Test
    @DisplayName("扫描注解方法产出权限定义并交付 Registry")
    void scansAndSyncs() throws Exception {
        UserController controller = new UserController();
        PermissionScanner scanner = new PermissionScanner(
                mapping(controller, UserController.class.getMethod("create"), "/users/create", RequestMethod.GET),
                registry);

        scanner.afterPropertiesSet();

        assertThat(synced).hasSize(1);
        PermissionDefinition def = synced.get(0);
        assertThat(def.value()).isEqualTo("user:create");
        assertThat(def.name()).isEqualTo("创建用户");
        assertThat(def.url()).isEqualTo("/users/create");
        assertThat(def.method()).isEqualTo("GET");
        assertThat(def.accessLevel()).isEqualTo(AccessLevel.PROTECTED);
    }

    @Test
    @DisplayName("无注解方法不参与扫描")
    void skipsUnannotated() throws Exception {
        UserController controller = new UserController();
        PermissionScanner scanner = new PermissionScanner(
                mapping(controller, UserController.class.getMethod("plain"), "/users/plain", RequestMethod.GET),
                registry);

        scanner.afterPropertiesSet();

        assertThat(synced).isEmpty();
    }

    @Test
    @DisplayName("同名 value 的 name/access 冲突导致启动失败")
    void duplicateValueConflictFails() throws Exception {
        UserController controller = new UserController();
        Map<RequestMappingInfo, HandlerMethod> methods = new HashMap<>();
        methods.put(
                RequestMappingInfo.paths("/users/create").methods(RequestMethod.GET).build(),
                new HandlerMethod(controller, UserController.class.getMethod("create")));
        methods.put(
                RequestMappingInfo.paths("/other").methods(RequestMethod.GET).build(),
                new HandlerMethod(new BadController(), BadController.class.getMethod("other")));
        RequestMappingHandlerMapping handlerMapping = mock(RequestMappingHandlerMapping.class);
        when(handlerMapping.getHandlerMethods()).thenReturn(methods);

        PermissionScanner scanner = new PermissionScanner(handlerMapping, registry);

        assertThatThrownBy(scanner::afterPropertiesSet)
                .isInstanceOf(RuntimeException.class)
                .cause()
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("权限值格式非法导致启动失败")
    void invalidFormatFails() throws Exception {
        InvalidFormatController controller = new InvalidFormatController();
        PermissionScanner scanner = new PermissionScanner(
                mapping(controller, InvalidFormatController.class.getMethod("bad"), "/bad", RequestMethod.GET),
                registry);

        assertThatThrownBy(scanner::afterPropertiesSet)
                .isInstanceOf(RuntimeException.class)
                .cause()
                .isInstanceOf(IllegalArgumentException.class);
    }
}
