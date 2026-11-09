package com.gamebox.core.hook;

/**
 * 生物受到伤害的钩子（伤害减免已计算、生命值尚未扣除时触发）。
 */
public interface LivingDamageHook {

    /**
     * @param targetEntityTypeId 受伤生物的实体类型 ID（如 "minecraft:zombie"）
     * @param targetHealth       受伤生物当前生命值
     * @param targetAbsorption   受伤生物当前吸收护盾值
     * @param incomingDamage     即将扣除的伤害值
     * @return 覆盖后的伤害值；{@code < 0} 表示不做修改。
     */
    float overrideDamage(String targetEntityTypeId, float targetHealth, float targetAbsorption, float incomingDamage);
}
