package io.github.ivencn.infra.security.annotation;

import io.github.ivencn.infra.security.principal.AccessLevel;
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

class AccessLevelFallbackAspectTest {

    @RequiresPermission(value = "thing:view", name = "查看", access = AccessLevel.PUBLIC)
    void publicEndpoint() {
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
    @DisplayName("降级模式：PUBLIC 放行")
    void publicPasses() throws Throwable {
        AccessLevelFallbackAspect aspect = new AccessLevelFallbackAspect("authenticated");
        assertThat(aspect.checkAccessLevel(okJoinPoint(), annotationOf("publicEndpoint"))).isEqualTo("ok");
    }

    @Test
    @DisplayName("降级模式：PROTECTED 已登录放行（authenticated 兜底）")
    void protectedDegradesToAuthenticated() throws Throwable {
        AccessLevelFallbackAspect aspect = new AccessLevelFallbackAspect("authenticated");
        UserCTX.setPrincipal(new SecurityPrincipal(1L, null, Set.of()));
        assertThat(aspect.checkAccessLevel(okJoinPoint(), annotationOf("protectedEndpoint"))).isEqualTo("ok");
    }

    @Test
    @DisplayName("降级模式：PROTECTED 未登录抛 401")
    void unauthenticatedRejected() throws Exception {
        AccessLevelFallbackAspect aspect = new AccessLevelFallbackAspect("authenticated");
        assertThatThrownBy(() -> aspect.checkAccessLevel(okJoinPoint(), annotationOf("protectedEndpoint")))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("deny 兜底：PROTECTED 即使已登录也拒绝")
    void denyFallbackRejects() throws Exception {
        AccessLevelFallbackAspect aspect = new AccessLevelFallbackAspect("deny");
        UserCTX.setPrincipal(new SecurityPrincipal(1L, null, Set.of()));
        assertThatThrownBy(() -> aspect.checkAccessLevel(okJoinPoint(), annotationOf("protectedEndpoint")))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
