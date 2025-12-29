package com.mobeditor.advancement;

import com.mobeditor.MobEditorMod;
import com.mobeditor.config.MobConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = MobEditorMod.MOD_ID)
public class AdvancementManager {

    // Кэш полученных достижений игроками (UUID игрока -> Set ID достижений)
    private static final Map<UUID, Set<String>> playerAchievements = new HashMap<>();

    /**
     * Проверяет, получил ли игрок достижение
     */
    public static boolean hasAchievement(UUID playerId, String achievementId) {
        return playerAchievements.getOrDefault(playerId, Collections.emptySet()).contains(achievementId);
    }

    /**
     * Отмечает достижение как полученное
     */
    public static void markAchievementGranted(UUID playerId, String achievementId) {
        playerAchievements.computeIfAbsent(playerId, k -> new HashSet<>()).add(achievementId);
    }

    /**
     * Сбрасывает достижения игрока (для тестов)
     */
    public static void resetPlayerAchievements(UUID playerId) {
        playerAchievements.remove(playerId);
    }

    /**
     * Выдает достижение игроку при убийстве моба
     */
    @SubscribeEvent
    public static void onMobKilled(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        // Проверяем, что убийца - игрок
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());

            // Ищем достижения для этого моба
            Map<String, MobConfig.AdvancementEntry> allAdvancements = MobEditorMod.getConfig().getAllAdvancements();

            for (Map.Entry<String, MobConfig.AdvancementEntry> entry : allAdvancements.entrySet()) {
                String advId = entry.getKey();
                MobConfig.AdvancementEntry adv = entry.getValue();

                if ("kill_mob".equals(adv.getTriggerType()) &&
                        entityId.toString().equals(adv.getTriggerTarget())) {

                    // Проверяем, не получено ли уже
                    if (!hasAchievement(player.getUUID(), advId)) {
                        grantAchievement(player, advId, adv);
                    }
                }
            }
        }
    }

    /**
     * Выдает достижение игроку при получении предмета
     */
    @SubscribeEvent
    public static void onItemObtained(PlayerEvent.ItemPickupEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack stack = event.getStack();
            if (stack.isEmpty()) {
                return;
            }

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

            // Ищем достижения для этого предмета
            Map<String, MobConfig.AdvancementEntry> allAdvancements = MobEditorMod.getConfig().getAllAdvancements();

            for (Map.Entry<String, MobConfig.AdvancementEntry> entry : allAdvancements.entrySet()) {
                String advId = entry.getKey();
                MobConfig.AdvancementEntry adv = entry.getValue();

                if ("obtain_item".equals(adv.getTriggerType()) &&
                        itemId.toString().equals(adv.getTriggerTarget())) {

                    // Проверяем, не получено ли уже
                    if (!hasAchievement(player.getUUID(), advId)) {
                        grantAchievement(player, advId, adv);
                    }
                }
            }
        }
    }

    /**
     * Выдает кастомное достижение игроку (показывает уведомление)
     */
    private static void grantAchievement(ServerPlayer player, String achievementId, MobConfig.AdvancementEntry entry) {
        try {
            // Отмечаем как полученное
            markAchievementGranted(player.getUUID(), achievementId);

            // Воспроизводим звук достижения
            player.connection.send(new ClientboundSoundPacket(
                    BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE),
                    SoundSource.PLAYERS,
                    player.getX(), player.getY(), player.getZ(),
                    1.0f, 1.0f, 0));

            // Отправляем сообщение в чат
            player.sendSystemMessage(Component.literal(""));
            player.sendSystemMessage(Component.literal("§6§l★ ДОСТИЖЕНИЕ ПОЛУЧЕНО! ★")
                    .withStyle(style -> style.withBold(true)));
            player.sendSystemMessage(Component.literal("§e" + entry.getTitle()));
            player.sendSystemMessage(Component.literal("§7" + entry.getDescription()));
            player.sendSystemMessage(Component.literal(""));

            // Показываем в ActionBar
            player.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.literal("§a✓ " + entry.getTitle())));

            // Показываем всем игрокам на сервере
            if (player.getServer() != null) {
                player.getServer().getPlayerList().broadcastSystemMessage(
                        Component.literal("§e" + player.getName().getString() + "§r получил достижение §6["
                                + entry.getTitle() + "]"),
                        false);
            }

            MobEditorMod.LOGGER.info("Выдано достижение '{}' игроку {}", entry.getTitle(),
                    player.getName().getString());

        } catch (Exception e) {
            MobEditorMod.LOGGER.error("Ошибка выдачи достижения: {}", entry.getTitle(), e);
        }
    }

    /**
     * Очистка при выходе игрока (опционально — можно сохранять в файл)
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // Достижения сохраняются в памяти до перезапуска сервера
        // Можно добавить сохранение в файл при необходимости
    }
}
