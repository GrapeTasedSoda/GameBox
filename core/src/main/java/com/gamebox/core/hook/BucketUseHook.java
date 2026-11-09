package com.gamebox.core.hook;

/**
 * 桶类物品使用的钩子（服务端）。
 */
public interface BucketUseHook {

    /**
     * @param itemId 手持的桶 ID（如 "minecraft:water_bucket"）
     * @return true 表示玩家尝试倾倒该桶后，产生的空桶应被回收（桶消失）。
     */
    boolean vanishOnPour(String itemId);
}
