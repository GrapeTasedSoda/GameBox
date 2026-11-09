package com.gamebox.config;

import com.gamebox.core.minigame.ConfigSchema;
import com.gamebox.core.minigame.ConfigView;
import com.gamebox.core.minigame.Minigame;
import com.gamebox.core.minigame.MinigameId;
import com.gamebox.core.minigame.MinigameManager;
import com.gamebox.core.minigame.MinigameRegistry;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 把每个小游戏声明的 schema 翻译成 NeoForge 的 ModConfigSpec。
 * 每个小游戏在配置文件中占据一个独立段落（[minigame_id]）。
 */
public final class GameBoxConfig {

    private final ModConfigSpec spec;
    private final Map<MinigameId, Map<String, ModConfigSpec.ConfigValue<?>>> values = new LinkedHashMap<>();
    private final Map<MinigameId, ConfigView> views = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    public GameBoxConfig(MinigameRegistry registry) {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        for (Minigame game : registry.registered()) {
            ConfigSchema schema = buildSchema(game);
            builder.push(game.id().value());
            Map<String, ModConfigSpec.ConfigValue<?>> map = new LinkedHashMap<>();
            for (ConfigSchema.Entry entry : schema.entries()) {
                switch (entry.type()) {
                    case BOOLEAN -> map.put(entry.key(),
                            builder.comment(entry.comment()).define(entry.key(), (Boolean) entry.defaultValue()));
                    case DOUBLE -> map.put(entry.key(),
                            builder.comment(entry.comment()).defineInRange(entry.key(), (Double) entry.defaultValue(),
                                    entry.min(), entry.max(), Double.class));
                    case INT -> map.put(entry.key(),
                            builder.comment(entry.comment()).defineInRange(entry.key(), (Integer) entry.defaultValue(),
                                    (int) entry.min(), (int) entry.max(), Integer.class));
                    case STRING_LIST -> map.put(entry.key(),
                            builder.comment(entry.comment()).defineListAllowEmpty(entry.key(),
                                    (List<String>) entry.defaultValue(), GameBoxConfig::isValidId));
                }
            }
            builder.pop();
            values.put(game.id(), Map.copyOf(map));
            views.put(game.id(), new PerGameView(map));
        }
        this.spec = builder.build();
    }

    private static ConfigSchema buildSchema(Minigame game) {
        ConfigSchema.Builder schemaBuilder = ConfigSchema.builder();
        game.declareConfig(schemaBuilder);
        return schemaBuilder.build();
    }

    private static boolean isValidId(Object value) {
        if (!(value instanceof String text)) {
            return false;
        }
        if (text.equals("*") || text.endsWith(":*")) {
            return true;
        }
        return Identifier.tryParse(text) != null;
    }

    public ModConfigSpec spec() {
        return spec;
    }

    public ConfigView view(Minigame game) {
        return views.get(game.id());
    }

    /** 配置文件加载/重载后，把所有游戏状态收敛到配置值。 */
    public void applyStates(MinigameManager manager) {
        manager.syncAll(game -> view(game).getBool("enabled"), this::view);
    }

    private static final class PerGameView implements ConfigView {
        private final Map<String, ModConfigSpec.ConfigValue<?>> map;

        private PerGameView(Map<String, ModConfigSpec.ConfigValue<?>> map) {
            this.map = map;
        }

        @Override
        public boolean getBool(String key) {
            return (Boolean) map.get(key).get();
        }

        @Override
        public double getDouble(String key) {
            return (Double) map.get(key).get();
        }

        @Override
        public int getInt(String key) {
            return (Integer) map.get(key).get();
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<String> getStringList(String key) {
            return (List<String>) map.get(key).get();
        }
    }
}
