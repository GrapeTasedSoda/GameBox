package com.gamebox.core.hook;

/**
 * 玩家挖掘速度钩子。适配器在 PlayerEvent.BreakSpeed（客户端预测与服务端均触发）
 * 中转发，对返回的最大倍率生效。
 */
public interface BreakSpeedHook {

    /**
     * @param blockId 被挖掘方块的注册 ID（如 "minecraft:stone"）
     * @return 破坏速度倍率，{@code <= 1} 表示不做修改。
     */
    float breakSpeedMultiplier(boolean creativeMode, float originalSpeed, String blockId);
}
