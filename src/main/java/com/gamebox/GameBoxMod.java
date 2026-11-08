package com.gamebox;

import com.gamebox.adapter.GameBoxEvents;
import com.gamebox.config.GameBoxConfig;
import com.gamebox.core.minigame.MinigameManager;
import com.gamebox.core.minigame.MinigameRegistry;
import com.gamebox.mc.fragile.FragileMinigame;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

/**
 * 游戏盒子（Game Box）：可开关的原版小游戏合集。
 */
@Mod(GameBoxMod.MOD_ID)
public final class GameBoxMod {

    public static final String MOD_ID = "gamebox";
    public static final Logger LOGGER = LogUtils.getLogger();

    private final GameBoxConfig config;
    private final MinigameManager manager;

    public GameBoxMod(IEventBus modEventBus, ModContainer modContainer) {
        MinigameRegistry registry = new MinigameRegistry();
        registry.register(new FragileMinigame());
        this.manager = new MinigameManager(registry);
        this.config = new GameBoxConfig(registry);

        modContainer.registerConfig(ModConfig.Type.LOCAL, config.spec());

        modEventBus.addListener(this::onConfigEvent);
        NeoForge.EVENT_BUS.register(new GameBoxEvents(manager));
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);

        LOGGER.info("Game Box loaded with {} minigame(s) registered", registry.registered().size());
    }

    private void onConfigEvent(ModConfigEvent event) {
        if (event.getConfig().getSpec() == config.spec()) {
            config.applyStates(manager);
            LOGGER.info("Game Box config (re)loaded, active minigames: {}", manager.activeIds());
        }
    }

    private void onServerStarting(ServerStartingEvent event) {
        // 兜底：确保服务器启动时状态与配置一致
        config.applyStates(manager);
        LOGGER.info("Game Box server starting, active minigames: {}", manager.activeIds());
    }
}
