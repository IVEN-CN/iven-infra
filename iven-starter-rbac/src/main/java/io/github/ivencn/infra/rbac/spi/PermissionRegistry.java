package io.github.ivencn.infra.rbac.spi;

import io.github.ivencn.infra.rbac.model.PermissionDefinition;

import java.util.List;

/**
 * 权限注册 SPI，由应用实现权限定义的持久化与同步。
 *
 * <p>
 * 框架扫描器在启动时产出全量 {@link PermissionDefinition} 并交由本接口同步，
 * 框架不依赖任何业务权限实体或存储实现。
 * </p>
 */
public interface PermissionRegistry {

    /**
     * 全量同步扫描结果。应用负责差异计算（新增/更新/幽灵数据删除）。
     *
     * @param definitions
     *            本次扫描到的全部权限定义
     */
    void sync(List<PermissionDefinition> definitions);
}
