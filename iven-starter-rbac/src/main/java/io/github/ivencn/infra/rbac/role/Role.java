package io.github.ivencn.infra.rbac.role;

/**
 * 角色抽象。
 *
 * <p>
 * 框架不内置任何角色枚举：应用可枚举实现本接口（附 level 层级语义），也可将角色落库后实现。
 * </p>
 */
public interface Role {

    /**
     * 角色名称。
     */
    String name();

    /**
     * 角色层级，数值越大权限越高。
     */
    int level();

    /**
     * 层级比较：当前角色是否大于等于目标角色。
     *
     * @param target
     *            目标角色
     * @return true 如果当前角色级别不低于目标角色
     */
    default boolean isAtLeast(Role target) {
        return this.level() >= target.level();
    }
}
