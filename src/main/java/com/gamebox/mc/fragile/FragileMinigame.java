package com.gamebox.mc.fragile;

import com.gamebox.core.hook.BreakSpeedHook;
import com.gamebox.core.hook.BlockBrokenHook;
import com.gamebox.core.minigame.ConfigSchema;
import com.gamebox.core.minigame.ConfigView;
import com.gamebox.core.minigame.Minigame;
import com.gamebox.core.minigame.MinigameId;

/**
 * 一碰就碎：所有方块瞬间破坏，使用的工具立即碎裂。
 */
public final class FragileMinigame implements Minigame, BreakSpeedHook, BlockBrokenHook {

    public static final MinigameId ID = MinigameId.of("fragile");

    private static final double DEFAULT_SPEED_MULTIPLIER = 1_000_000.0;

    private volatile ConfigView config;

    @Override
    public MinigameId id() {
        return ID;
    }

    @Override
    public void declareConfig(ConfigSchema.Builder schema) {
        schema.bool("enabled", true, "Enable this minigame. [一碰就碎：开启后所有方块一碰就碎]");
        schema.doubleVal("break_speed_multiplier", DEFAULT_SPEED_MULTIPLIER, 1.0, 1.0e9,
                "Break speed multiplier applied to all blocks. [挖掘速度倍率]");
        schema.bool("tools_break_instantly", true,
                "The tool used to break a block loses all remaining durability instantly. [工具一碰就碎]");
    }

    @Override
    public void onEnable(ConfigView config) {
        this.config = config;
    }

    @Override
    public void onDisable() {
        this.config = null;
    }

    @Override
    public float breakSpeedMultiplier(boolean creativeMode, float originalSpeed) {
        ConfigView cfg = config;
        if (cfg == null || creativeMode) {
            return 1.0f;
        }
        return (float) cfg.getDouble("break_speed_multiplier");
    }

    @Override
    public boolean shatterToolOnBlockBreak() {
        ConfigView cfg = config;
        return cfg != null && cfg.getBool("tools_break_instantly");
    }
}
