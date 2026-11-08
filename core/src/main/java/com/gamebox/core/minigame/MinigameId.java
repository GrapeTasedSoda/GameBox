package com.gamebox.core.minigame;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 小游戏的稳定标识符，用于配置键、指令与存档。
 */
public record MinigameId(String value) {

    private static final Pattern VALID = Pattern.compile("[a-z][a-z0-9_]{1,63}");

    public MinigameId {
        Objects.requireNonNull(value, "value");
        if (!VALID.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid minigame id: " + value);
        }
    }

    public static MinigameId of(String value) {
        return new MinigameId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
