package com.gamebox.core.minigame;

import java.util.Collections;
import java.util.List;

/**
 * 声明式配置 schema：小游戏只声明配置项（键/类型/默认值/范围/注释），
 * 具体落到什么配置系统由平台适配器决定。
 */
public final class ConfigSchema {

    public enum Type { BOOLEAN, DOUBLE, INT, STRING_LIST }

    public record Entry(String key, Type type, Object defaultValue, double min, double max, String comment) {
    }

    private final List<Entry> entries;

    private ConfigSchema(List<Entry> entries) {
        this.entries = List.copyOf(entries);
    }

    public List<Entry> entries() {
        return entries;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<Entry> entries = new java.util.ArrayList<>();

        public Builder bool(String key, boolean defaultValue, String comment) {
            entries.add(new Entry(key, Type.BOOLEAN, defaultValue, 0, 0, comment));
            return this;
        }

        public Builder doubleVal(String key, double defaultValue, double min, double max, String comment) {
            entries.add(new Entry(key, Type.DOUBLE, defaultValue, min, max, comment));
            return this;
        }

        public Builder intVal(String key, int defaultValue, int min, int max, String comment) {
            entries.add(new Entry(key, Type.INT, defaultValue, min, max, comment));
            return this;
        }

        /** 字符串列表（如方块/物品/实体 ID 白名单），ID 形如 "minecraft:stone"，可省略命名空间。 */
        public Builder listVal(String key, List<String> defaultValue, String comment) {
            entries.add(new Entry(key, Type.STRING_LIST, List.copyOf(defaultValue), 0, 0, comment));
            return this;
        }

        public ConfigSchema build() {
            return new ConfigSchema(Collections.unmodifiableList(entries));
        }
    }
}
