package com.gamebox.mc.fragile;

import com.gamebox.core.hook.BlockBrokenHook;
import com.gamebox.core.hook.BreakSpeedHook;
import com.gamebox.core.hook.BucketUseHook;
import com.gamebox.core.hook.EntityHealthHook;
import com.gamebox.core.hook.LivingDamageHook;
import com.gamebox.core.hook.ToolUseHook;
import com.gamebox.core.minigame.ConfigSchema;
import com.gamebox.core.minigame.ConfigView;
import com.gamebox.core.minigame.Minigame;
import com.gamebox.core.minigame.MinigameId;
import java.util.List;

/**
 * 一碰就碎：
 * <ul>
 *     <li>所有方块（含基岩）瞬间破坏</li>
 *     <li>所有生物只有 1 点生命值，任意伤害致死</li>
 *     <li>所有工具/武器使用一次后损毁</li>
 *     <li>桶使用一次后消失（可配置豁免）</li>
 *     <li>方块/物品/生物白名单可豁免对应规则</li>
 * </ul>
 */
public final class FragileMinigame implements Minigame, BreakSpeedHook, BlockBrokenHook, ToolUseHook,
        BucketUseHook, LivingDamageHook, EntityHealthHook {

    public static final MinigameId ID = MinigameId.of("fragile");

    private static final double DEFAULT_SPEED_MULTIPLIER = 1_000_000.0;
    private static final float LETHAL_DAMAGE = 1000.0f;

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
                "Tools and weapons lose all durability after one use. [工具/武器一碰就碎]");
        schema.bool("entities_one_health", true,
                "All living entities are limited to 1 health and die from any damage. [所有生物一滴血]");
        schema.bool("buckets_exempt", true,
                "When true, buckets are exempt from the fragile rules. [为 true 时桶不受一碰就碎影响]");
        schema.listVal("block_whitelist", List.of(),
                "Blocks exempt from instant breaking. Use IDs like minecraft:bedrock; namespace may be omitted. [方块白名单]");
        schema.listVal("item_whitelist", List.of(),
                "Items exempt from instant shattering. Use IDs like minecraft:diamond_pickaxe; supports modid:*. [物品白名单]");
        schema.listVal("entity_whitelist", List.of(),
                "Entities exempt from the 1-health rule. Use IDs like minecraft:zombie; supports modid:*. [生物白名单]");
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
    public float breakSpeedMultiplier(boolean creativeMode, float originalSpeed, String blockId) {
        ConfigView cfg = config;
        if (cfg == null || creativeMode || isListed(cfg, "block_whitelist", blockId)) {
            return 1.0f;
        }
        return (float) cfg.getDouble("break_speed_multiplier");
    }

    @Override
    public boolean shatterToolOnBlockBreak(String toolId) {
        return shouldShatterItem(toolId);
    }

    @Override
    public boolean shatterToolOnUse(String toolId) {
        return shouldShatterItem(toolId);
    }

    @Override
    public boolean vanishOnPour(String bucketItemId) {
        ConfigView cfg = config;
        if (cfg == null || cfg.getBool("buckets_exempt")) {
            return false;
        }
        return shouldShatterItem(bucketItemId);
    }

    @Override
    public boolean limitToOneHealth(String entityTypeId) {
        ConfigView cfg = config;
        return cfg != null
                && cfg.getBool("entities_one_health")
                && !isListed(cfg, "entity_whitelist", entityTypeId);
    }

    @Override
    public float overrideDamage(String targetEntityTypeId, float targetHealth, float targetAbsorption,
                                float incomingDamage) {
        if (!limitToOneHealth(targetEntityTypeId)) {
            return -1.0f;
        }
        return Math.max(LETHAL_DAMAGE, targetHealth + targetAbsorption + 1.0f);
    }

    private boolean shouldShatterItem(String itemId) {
        ConfigView cfg = config;
        return cfg != null
                && cfg.getBool("tools_break_instantly")
                && !isListed(cfg, "item_whitelist", itemId);
    }

    private static boolean isListed(ConfigView cfg, String key, String id) {
        List<String> entries = cfg.getStringList(key);
        if (entries.isEmpty()) {
            return false;
        }
        for (String raw : entries) {
            if (raw == null) {
                continue;
            }
            String entry = raw.trim();
            if (entry.isEmpty()) {
                continue;
            }
            if (entry.equals(id)) {
                return true;
            }
            // 允许省略命名空间（默认 minecraft）
            if (entry.indexOf(':') < 0 && ("minecraft:" + entry).equals(id)) {
                return true;
            }
            // 支持 "*" 与 "modid:*"
            if (entry.equals("*")) {
                return true;
            }
            if (entry.endsWith(":*")) {
                String namespace = entry.substring(0, entry.length() - 1);
                if (id.startsWith(namespace)) {
                    return true;
                }
            }
        }
        return false;
    }
}
