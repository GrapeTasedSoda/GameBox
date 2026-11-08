package com.gamebox.core.minigame;

/**
 * 平台适配器为每个小游戏提供的只读配置视图。
 * 键名为小游戏 schema 中声明的相对键（如 "enabled"），不含小游戏前缀。
 */
public interface ConfigView {

    boolean getBool(String key);

    double getDouble(String key);

    int getInt(String key);
}
