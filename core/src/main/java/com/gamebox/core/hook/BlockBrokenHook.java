package com.gamebox.core.hook;

/**
 * 方块被玩家破坏后（服务端，掉落物产生时）的钩子。
 */
public interface BlockBrokenHook {

    /** @return true 表示本次破坏所使用的工具应立即损失全部剩余耐久。 */
    boolean shatterToolOnBlockBreak();
}
