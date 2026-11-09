package com.gamebox.core.hook;

/**
 * 生物生命值钩子：用于 "所有生物一滴血"。
 */
public interface EntityHealthHook {

    /**
     * @param entityTypeId 实体类型注册 ID（如 "minecraft:zombie"）
     * @return true 表示该实体应被限制为 1 点最大生命值。
     */
    boolean limitToOneHealth(String entityTypeId);
}
