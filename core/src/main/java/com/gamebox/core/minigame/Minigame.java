package com.gamebox.core.minigame;

/**
 * 所有小游戏的公共接口。实现类还应实现 core.hook 包中的钩子接口
 * （如 {@code BreakSpeedHook}），平台适配器会按实现的钩子桥接对应事件。
 */
public interface Minigame {

    MinigameId id();

    /** 声明本游戏的配置项；在注册时调用一次。 */
    void declareConfig(ConfigSchema.Builder schema);

    /** 游戏被激活时调用，config 可缓存起来供钩子读取。 */
    default void onEnable(ConfigView config) {
    }

    /** 游戏被停用或配置卸载时调用。实现方应清理缓存状态。 */
    default void onDisable() {
    }
}
