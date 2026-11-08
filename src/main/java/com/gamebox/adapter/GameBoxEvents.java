package com.gamebox.adapter;

import com.gamebox.core.hook.BlockBrokenHook;
import com.gamebox.core.hook.BreakSpeedHook;
import com.gamebox.core.minigame.Minigame;
import com.gamebox.core.minigame.MinigameManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * NeoForge 事件桥：把加载器事件转发给已激活小游戏实现的 core 钩子。
 */
public final class GameBoxEvents {

    private final MinigameManager manager;

    public GameBoxEvents(MinigameManager manager) {
        this.manager = manager;
    }

    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.isCanceled()) {
            return;
        }
        boolean creative = event.getEntity().isCreative();
        float multiplier = 1.0f;
        for (Minigame game : manager.activeGames()) {
            if (game instanceof BreakSpeedHook hook) {
                float m = hook.breakSpeedMultiplier(creative, event.getOriginalSpeed());
                if (m > multiplier) {
                    multiplier = m;
                }
            }
        }
        if (multiplier > 1.0f) {
            float speed = event.getOriginalSpeed() * multiplier;
            // 无法正常采集的方块（如基岩）原速为 0，也给一个足够瞬间破坏的速度
            if (speed <= 1.0e-3f) {
                speed = 1000.0f;
            }
            event.setNewSpeed(speed);
        }
    }

    @SubscribeEvent
    public void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) {
            return;
        }
        boolean shatter = false;
        for (Minigame game : manager.activeGames()) {
            if (game instanceof BlockBrokenHook hook && hook.shatterToolOnBlockBreak()) {
                shatter = true;
                break;
            }
        }
        if (!shatter) {
            return;
        }
        ItemStack tool = event.getTool();
        if (!tool.isEmpty() && tool.isDamageableItem()) {
            tool.setDamageValue(tool.getMaxDamage());
        }
    }
}
