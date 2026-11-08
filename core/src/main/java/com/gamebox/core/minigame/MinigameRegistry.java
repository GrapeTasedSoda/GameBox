package com.gamebox.core.minigame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 启动期静态注册所有小游戏；顺序即配置文件/指令中的展示顺序。
 */
public final class MinigameRegistry {

    private final Map<MinigameId, Minigame> games = new LinkedHashMap<>();

    public void register(Minigame game) {
        MinigameId id = game.id();
        if (games.containsKey(id)) {
            throw new IllegalStateException("Duplicate minigame id: " + id);
        }
        games.put(id, game);
    }

    public List<Minigame> registered() {
        return Collections.unmodifiableList(new ArrayList<>(games.values()));
    }

    public Minigame get(MinigameId id) {
        return games.get(id);
    }
}
