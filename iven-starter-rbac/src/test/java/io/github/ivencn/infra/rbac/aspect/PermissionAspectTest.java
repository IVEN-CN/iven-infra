package io.github.ivencn.infra.rbac.aspect;

import io.github.ivencn.infra.rbac.spi.SuperAdminPolicy;
import io.github.ivencn.infra.security.annotation.RequiresPermission;
import io.github.ivencn.infra.security.principal.AccessLevel;
import io.github.ivencn.infra.rbac.role.Role;
import io.github.ivencn.infra.security.principal.SecurityPrincipal;
import io.github.ivencn.infra.security.principal.UserCTX;
import io.github.ivencn.infra.core.exception.BizException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionAspectTest {

    enum TestRole implements Role {
        SUPER(100), NORMAL(10);

        private final int level;

        TestRole(int level) {
            this.level = level;
        }

        @Override
        public int level() {
            return level;
        }
    }

    private final SuperAdminPolicy policy = role -> role != null && role.level() >= 100;
    private final PermissionAspect aspect = new PermissionAspect(policy);

    @RequiresPermission(value = "thing:view", name = "查看", access = AccessLevel.PUBLIC)
    void publicEndpoint() {
    }

    @RequiresPermission(value = "thing:view", name = "查看", access = AccessLevel.AUTHENTICATED)
    void authenticatedEndpoint() {
    }

    @RequiresPermission(value = "thing:edit", name = "编辑", access = AccessLevel.PROTECTED)
    void protectedEndpoint() {
    }

    @AfterEach
    void clear() {
        UserCTX.clear();
    }

    private org.aspectj.lang.ProceedingJoinPoint okJoinPoint() {
        org.aspectj.lang.ProceedingJoinPoint pjp = mock(org.aspectj.lang.ProceedingJoinPoint.class);
        try {
            when(pjp.proceed()).thenReturn("ok");
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
        return pjp;
    }

    private RequiresPermission annotationOf(String methodName) throws Exception {
        return getClass().getDeclaredMethod(methodName).getAnnotation(RequiresPermission.class);
    }

    @Test
    @DisplayName("PUBLIC 无需登录直接放行")
    void publicPasses() throws Throwable {
        Object result = aspect.checkPermission(okJoinPoint(), annotationOf("publicEndpoint"));
        assertThat(result).isEqualTo("ok");
    }

    @Test
    @DisplayName("AUTHENTICATED 未登录抛 401")
    void authenticatedRequiresLogin() throws Exception {
        assertThatThrownBy(() -> aspect.checkPermission(okJoinPoint(), annotationOf("authenticatedEndpoint")))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("PROTECTED 已登录且有权限放行")
    void protectedWithPermissionPasses() throws Throwable {
        UserCTX.setPrincipal(new SecurityPrincipal(1L, TestRole.NORMAL, Set.of("thing:edit")));
        Object result = aspect.checkPermission(okJoinPoint(), annotationOf("protectedEndpoint"));
        assertThat(result).isEqualTo("ok");
    }

    @Test
    @DisplayName("PROTECTED 已登录但无权限抛 403")
    void protectedWithoutPermissionDenied() throws Exception {
        UserCTX.setPrincipal(new SecurityPrincipal(1L, TestRole.NORMAL, Set.of("thing:view")));
        assertThatThrownBy(() -> aspect.checkPermission(okJoinPoint(), annotationOf("protectedEndpoint")))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("超级管理员短路放行（即使无权限标识）")
    void superAdminBypasses() throws Throwable {
        UserCTX.setPrincipal(new SecurityPrincipal(1L, TestRole.SUPER, Set.of()));
        Object result = aspect.checkPermission(okJoinPoint(), annotationOf("protectedEndpoint"));
        assertThat(result).isEqualTo("ok");
    }
}
