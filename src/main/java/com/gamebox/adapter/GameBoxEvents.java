package com.gamebox.adapter;

import com.gamebox.core.hook.BlockBrokenHook;
import com.gamebox.core.hook.BreakSpeedHook;
import com.gamebox.core.hook.BucketUseHook;
import com.gamebox.core.hook.EntityHealthHook;
import com.gamebox.core.hook.LivingDamageHook;
import com.gamebox.core.hook.ToolUseHook;
import com.gamebox.core.minigame.Minigame;
import com.gamebox.core.minigame.MinigameManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * NeoForge 事件桥：把加载器事件转发给已激活小游戏实现的 core 钩子。
 */
public final class GameBoxEvents {

    private static final Identifier ONE_HEALTH_MODIFIER_ID =
            Identifier.fromNamespaceAndPath("gamebox", "fragile_one_health");

    private final MinigameManager manager;

    public GameBoxEvents(MinigameManager manager) {
        this.manager = manager;
    }

    // ------------------------------------------------------------------
    // 方块：瞬间破坏
    // ------------------------------------------------------------------

    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.isCanceled()) {
            return;
        }
        boolean creative = event.getEntity().isCreative();
        String blockId = registryId(BuiltInRegistries.BLOCK.getKey(event.getState().getBlock()));
        float multiplier = 1.0f;
        for (Minigame game : manager.activeGames()) {
            if (game instanceof BreakSpeedHook hook) {
                float m = hook.breakSpeedMultiplier(creative, event.getOriginalSpeed(), blockId);
                if (m > multiplier) {
                    multiplier = m;
                }
            }
        }
        if (multiplier > 1.0f) {
            float speed = event.getOriginalSpeed() * multiplier;
            // 无法正常采集的方块（如基岩）原速为 0，也给一个足够瞬间破坏的速度
            if (speed <= 1.0e-3f) {
                speed = 1000.0f;
            }
            event.setNewSpeed(speed);
        }
    }

    // ------------------------------------------------------------------
    // 工具/武器：使用一次后损毁
    // ------------------------------------------------------------------

    @SubscribeEvent
    public void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack tool = event.getTool();
        if (tool.isEmpty() || !tool.isDamageableItem()) {
            return;
        }
        String toolId = registryId(BuiltInRegistries.ITEM.getKey(tool.getItem()));
        if (shouldShatterOnBlockBreak(toolId)) {
            shatter(tool, player, InteractionHand.MAIN_HAND);
        }
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (event.isCanceled()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        if (weapon.isEmpty() || !weapon.isDamageableItem()) {
            return;
        }
        String weaponId = registryId(BuiltInRegistries.ITEM.getKey(weapon.getItem()));
        if (shouldShatterOnUse(weaponId)) {
            shatter(weapon, player, InteractionHand.MAIN_HAND);
        }
    }

    @SubscribeEvent
    @SuppressWarnings("removal")
    public void onArrowLoose(ArrowLooseEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!event.hasAmmo() || event.getCharge() <= 0) {
            return;
        }
        ItemStack bow = event.getBow();
        if (bow.isEmpty() || !bow.isDamageableItem()) {
            return;
        }
        String bowId = registryId(BuiltInRegistries.ITEM.getKey(bow.getItem()));
        if (shouldShatterOnUse(bowId)) {
            InteractionHand hand = player.getMainHandItem().getItem() == bow.getItem()
                    ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            // 延迟到射击结算后损毁，避免影响本次箭矢生成
            player.level().getServer().execute(() -> {
                ItemStack current = player.getItemInHand(hand);
                if (!current.isEmpty() && current.getItem() == bow.getItem()) {
                    shatter(current, player, hand);
                }
            });
        }
    }

    /**
     * 右键使用工具/桶：物品的实际效果在事件之后才结算，因此延迟到下一 tick 检查结果。
     */
    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        scheduleItemUseCheck(event);
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        scheduleItemUseCheck(event);
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        scheduleItemUseCheck(event);
    }

    private void scheduleItemUseCheck(PlayerInteractEvent event) {
        if (event instanceof ICancellableEvent cancellable && cancellable.isCanceled()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack original = event.getItemStack();
        if (original.isEmpty()) {
            return;
        }
        InteractionHand hand = event.getHand();
        ItemStack snapshot = original.copy();
        String itemId = registryId(BuiltInRegistries.ITEM.getKey(original.getItem()));

        boolean bucketRule = original.getItem() instanceof BucketItem
                && shouldVanishBucket(itemId);
        boolean toolRule = original.isDamageableItem() && shouldShatterOnUse(itemId);
        if (!bucketRule && !toolRule) {
            return;
        }

        player.level().getServer().execute(() -> {
            ItemStack current = player.getItemInHand(hand);
            if (current.isEmpty()) {
                return;
            }
            if (bucketRule) {
                // 成功装/倒液体后，桶会变成另一种桶物品（空桶<->液体桶），此时抹除
                if (current.getItem() instanceof BucketItem && current.getItem() != snapshot.getItem()) {
                    player.setItemInHand(hand, ItemStack.EMPTY);
                }
                return;
            }
            // 右键成功消耗了耐久 -> 直接损毁
            if (current.getItem() == snapshot.getItem()
                    && current.isDamageableItem()
                    && current.getDamageValue() > snapshot.getDamageValue()) {
                shatter(current, player, hand);
            }
        });
    }

    // ------------------------------------------------------------------
    // 生物：一滴血
    // ------------------------------------------------------------------

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        applyOneHealthState(living);
    }

    /**
     * 配置重载后刷新已加载实体：白名单内的实体移除一滴血修饰符，其余补上。
     */
    public void refreshLoadedEntities() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        // 配置重载发生在文件监听线程，实体遍历必须回到服务器主线程
        server.execute(() -> {
            for (ServerLevel level : server.getAllLevels()) {
                for (Entity entity : level.getAllEntities()) {
                    if (entity instanceof LivingEntity living) {
                        applyOneHealthState(living);
                    }
                }
            }
        });
    }

    private void applyOneHealthState(LivingEntity living) {
        String entityTypeId = registryId(BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()));
        AttributeInstance maxHealth = living.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        if (!isOneHealthEntity(entityTypeId)) {
            maxHealth.removeModifier(ONE_HEALTH_MODIFIER_ID);
            return;
        }
        // 使用临时修饰符：与实体基础生命值无关，且不会写进存档
        double delta = 1.0 - maxHealth.getBaseValue();
        maxHealth.addOrUpdateTransientModifier(
                new AttributeModifier(ONE_HEALTH_MODIFIER_ID, delta, AttributeModifier.Operation.ADD_VALUE));
        if (living.getHealth() > 1.0f) {
            living.setHealth(1.0f);
        }
    }

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        String entityTypeId = registryId(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
        float damage = event.getNewDamage();
        for (Minigame game : manager.activeGames()) {
            if (game instanceof LivingDamageHook hook) {
                float override = hook.overrideDamage(entityTypeId, entity.getHealth(),
                        entity.getAbsorptionAmount(), damage);
                if (override >= 0.0f) {
                    damage = override;
                }
            }
        }
        if (damage != event.getNewDamage()) {
            event.setNewDamage(damage);
        }
    }

    // ------------------------------------------------------------------
    // 内部工具方法
    // ------------------------------------------------------------------

    private boolean shouldShatterOnBlockBreak(String itemId) {
        for (Minigame game : manager.activeGames()) {
            if (game instanceof BlockBrokenHook hook && hook.shatterToolOnBlockBreak(itemId)) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldShatterOnUse(String itemId) {
        for (Minigame game : manager.activeGames()) {
            if (game instanceof ToolUseHook hook && hook.shatterToolOnUse(itemId)) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldVanishBucket(String itemId) {
        for (Minigame game : manager.activeGames()) {
            if (game instanceof BucketUseHook hook && hook.vanishOnPour(itemId)) {
                return true;
            }
        }
        return false;
    }

    private boolean isOneHealthEntity(String entityTypeId) {
        for (Minigame game : manager.activeGames()) {
            if (game instanceof EntityHealthHook hook && hook.limitToOneHealth(entityTypeId)) {
                return true;
            }
        }
        return false;
    }

    private static void shatter(ItemStack stack, ServerPlayer player, InteractionHand hand) {
        if (!stack.isEmpty() && stack.isDamageableItem()) {
            stack.hurtAndBreak(stack.getMaxDamage(), player, hand);
        }
    }

    private static String registryId(@org.jetbrains.annotations.Nullable Identifier id) {
        return id == null ? "minecraft:air" : id.toString();
    }
}
