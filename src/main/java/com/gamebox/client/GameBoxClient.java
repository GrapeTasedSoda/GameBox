package com.gamebox.client;

import com.gamebox.GameBoxMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * 客户端入口：把 NeoForge 内置配置界面接到模组列表的"配置"按钮上。
 */
@Mod(value = GameBoxMod.MOD_ID, dist = Dist.CLIENT)
public final class GameBoxClient {

    public GameBoxClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
