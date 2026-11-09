package com.gamebox.core.hook;

/**
 * 物品被"使用一次"后的钩子（当前用于攻击生物后的武器消耗）。
 */
public interface ToolUseHook {

    /**
     * @param toolId 使用的物品 ID
     * @return true 表示该物品应在这次使用后立即损失全部剩余耐久。
     */
    boolean shatterToolOnUse(String toolId);
}
