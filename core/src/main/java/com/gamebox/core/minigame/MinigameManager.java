package com.gamebox.core.minigame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 管理各小游戏的激活状态（激活 = 事件钩子生效）。
 * 状态来源（配置文件/世界数据/指令）由平台适配器决定，本类只负责状态迁移。
 */
public final class MinigameManager {

    private final MinigameRegistry registry;
    private final Map<MinigameId, ConfigView> active = new HashMap<>();

    public MinigameManager(MinigameRegistry registry) {
        this.registry = registry;
    }

    public MinigameRegistry registry() {
        return registry;
    }

    public boolean isActive(MinigameId id) {
        return active.containsKey(id);
    }

    /** 当前激活的游戏列表（顺序与注册顺序一致）。 */
    public List<Minigame> activeGames() {
        List<Minigame> result = new ArrayList<>();
        for (Minigame game : registry.registered()) {
            if (active.containsKey(game.id())) {
                result.add(game);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * 将某个游戏的状态收敛到目标值；只有发生状态变化时才回调 onEnable/onDisable。
     */
    public void setState(MinigameId id, boolean enabled, ConfigView config) {
        boolean currentlyActive = active.containsKey(id);
        if (enabled == currentlyActive) {
            return;
        }
        if (enabled) {
            active.put(id, config);
            registry.get(id).onEnable(config);
        } else {
            active.remove(id);
            registry.get(id).onDisable();
        }
    }

    /** 一次性收敛全部游戏状态（配置加载/重载时调用）。 */
    public void syncAll(java.util.function.Function<Minigame, Boolean> enabledOf,
                        java.util.function.Function<Minigame, ConfigView> viewOf) {
        for (Minigame game : registry.registered()) {
            setState(game.id(), enabledOf.apply(game), viewOf.apply(game));
        }
    }

    public Set<MinigameId> activeIds() {
        return Collections.unmodifiableSet(new HashSet<>(active.keySet()));
    }
}
