package io.github.ivencn.infra.security.spi;

import io.github.ivencn.infra.security.principal.SecurityPrincipal;

import java.util.Optional;

/**
 * 用户详情加载 SPI。
 *
 * <p>
 * 框架的 JWT 认证过滤器通过该接口将 token 中的主体ID解析为 {@link SecurityPrincipal}，
 * 用户信息、角色与权限的取数逻辑完全由应用实现，框架不依赖任何业务类型。
 * </p>
 */
public interface UserDetailsLoader {

    /**
     * 按主体ID加载用户安全主体。
     *
     * @param subjectId
     *            JWT 主题ID（用户ID）
     * @return 安全主体；用户不存在时返回 empty
     */
    Optional<SecurityPrincipal> loadById(Long subjectId);
}
