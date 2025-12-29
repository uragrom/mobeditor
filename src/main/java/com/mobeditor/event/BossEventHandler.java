package com.mobeditor.event;

import com.mobeditor.MobEditorMod;
import com.mobeditor.config.MobConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BossEventHandler {

    // Активные боссбары: Entity UUID -> BossBar
    private static final Map<UUID, ServerBossEvent> activeBossBars = new ConcurrentHashMap<>();

    // Игроки рядом с боссами (для музыки): Player UUID -> Boss Entity UUID
    private static final Map<UUID, UUID> playersNearBoss = new ConcurrentHashMap<>();

    // Активная музыка для игроков: Player UUID -> Music File
    private static final Map<UUID, String> activeMusic = new ConcurrentHashMap<>();

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity livingEntity)) {
            return;
        }

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(livingEntity.getType());
        MobConfig.BossSettings settings = MobEditorMod.getConfig().getBossSettings(entityId.toString());

        if (settings == null || !settings.isBoss()) {
            return;
        }

        // Создаём боссбар
        String bossName = settings.getBossName() != null ? settings.getBossName() : livingEntity.getName().getString();

        ServerBossEvent bossBar = new ServerBossEvent(
                Component.literal(bossName),
                getBossBarColor(settings.getBarColor()),
                getBossBarOverlay(settings.getBarStyle()));

        bossBar.setDarkenScreen(settings.isDarkenScreen());
        bossBar.setCreateWorldFog(settings.isCreateFog());

        activeBossBars.put(livingEntity.getUUID(), bossBar);

        MobEditorMod.LOGGER.debug("Босс {} заспавнен", entityId);
    }

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        LivingEntity entity = event.getEntity();
        ServerBossEvent bossBar = activeBossBars.get(entity.getUUID());

        if (bossBar == null) {
            return;
        }

        // Обновляем прогресс боссбара
        bossBar.setProgress(entity.getHealth() / entity.getMaxHealth());

        // Получаем настройки босса
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        MobConfig.BossSettings settings = MobEditorMod.getConfig().getBossSettings(entityId.toString());

        if (settings == null) {
            return;
        }

        int range = settings.getMusicRange();

        // Обновляем игроков в радиусе
        entity.level().players().forEach(player -> {
            if (player instanceof ServerPlayer serverPlayer) {
                double distance = player.distanceTo(entity);

                if (distance <= range) {
                    // Добавляем игрока к боссбару
                    if (!bossBar.getPlayers().contains(serverPlayer)) {
                        bossBar.addPlayer(serverPlayer);
                        playersNearBoss.put(player.getUUID(), entity.getUUID());

                        // Отправляем информацию о музыке на клиент
                        if (settings.getMusicFile() != null && !settings.getMusicFile().isEmpty()) {
                            sendBossMusicToClient(serverPlayer, settings.getMusicFile(), entity.getUUID());
                        }
                    }
                } else {
                    // Убираем игрока из боссбара
                    if (bossBar.getPlayers().contains(serverPlayer)) {
                        bossBar.removePlayer(serverPlayer);
                        playersNearBoss.remove(player.getUUID());

                        // Останавливаем музыку на клиенте
                        stopBossMusicOnClient(serverPlayer);
                    }
                }
            }
        });
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        LivingEntity entity = event.getEntity();
        ServerBossEvent bossBar = activeBossBars.remove(entity.getUUID());

        if (bossBar != null) {
            bossBar.removeAllPlayers();

            // Убираем связи игроков
            playersNearBoss.entrySet().removeIf(entry -> entry.getValue().equals(entity.getUUID()));

            MobEditorMod.LOGGER.debug("Босс {} убит", entity.getType().getDescriptionId());
        }
    }

    private BossEvent.BossBarColor getBossBarColor(String color) {
        try {
            return BossEvent.BossBarColor.valueOf(color.toUpperCase());
        } catch (Exception e) {
            return BossEvent.BossBarColor.RED;
        }
    }

    private BossEvent.BossBarOverlay getBossBarOverlay(String style) {
        try {
            return BossEvent.BossBarOverlay.valueOf(style.toUpperCase());
        } catch (Exception e) {
            return BossEvent.BossBarOverlay.PROGRESS;
        }
    }

    // Проверка является ли сущность кастомным боссом
    public static boolean isCustomBoss(LivingEntity entity) {
        return activeBossBars.containsKey(entity.getUUID());
    }

    // Получить активные боссбары
    public static Map<UUID, ServerBossEvent> getActiveBossBars() {
        return activeBossBars;
    }

    private void sendBossMusicToClient(ServerPlayer player, String musicFile, UUID bossId) {
        // Используем простой способ - отправляем сообщение через команду
        // В реальности лучше использовать кастомный пакет, но для простоты используем
        // существующий механизм
        activeMusic.put(player.getUUID(), musicFile);

        // Отправляем команду на клиент для воспроизведения музыки
        // Для этого нужно использовать пакет или клиентский обработчик
        // Пока что просто сохраняем информацию - клиентский обработчик будет проверять
        // это
    }

    private void stopBossMusicOnClient(ServerPlayer player) {
        activeMusic.remove(player.getUUID());
    }

    public static String getActiveMusicForPlayer(UUID playerId) {
        return activeMusic.get(playerId);
    }

    public static boolean isPlayerNearBoss(UUID playerId) {
        return playersNearBoss.containsKey(playerId);
    }
}
