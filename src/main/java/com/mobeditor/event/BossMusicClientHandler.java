package com.mobeditor.event;

import com.mobeditor.MobEditorMod;
import com.mobeditor.config.MobConfig;
import com.mobeditor.sound.CustomSoundManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = MobEditorMod.MOD_ID, value = Dist.CLIENT)
public class BossMusicClientHandler {

    // Активная музыка для каждого босса: Boss UUID -> Music File
    private static final Map<UUID, String> activeBossMusic = new ConcurrentHashMap<>();

    // Текущая воспроизводимая музыка: Player UUID -> Boss UUID
    private static UUID currentBossMusic = null;
    private static String currentMusicFile = null;

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            currentBossMusic = null;
            currentMusicFile = null;
            return;
        }

        Player player = mc.player;
        UUID nearestBoss = null;
        String nearestMusic = null;
        double nearestDistance = Double.MAX_VALUE;

        // Ищем ближайшего босса с музыкой
        for (LivingEntity entity : mc.level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(256))) {

            ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            MobConfig.BossSettings settings = MobEditorMod.getConfig().getBossSettings(entityId.toString());

            if (settings != null && settings.isBoss() && settings.getMusicFile() != null
                    && !settings.getMusicFile().isEmpty()) {

                double distance = player.distanceTo(entity);
                int range = settings.getMusicRange();

                if (distance <= range && distance < nearestDistance) {
                    nearestBoss = entity.getUUID();
                    nearestMusic = settings.getMusicFile();
                    nearestDistance = distance;
                }
            }
        }

        // Если нашли нового босса с музыкой
        if (nearestBoss != null && !nearestBoss.equals(currentBossMusic)) {
            if (currentBossMusic != null) {
                // Останавливаем старую музыку
                stopMusic();
            }

            // Запускаем новую музыку
            currentBossMusic = nearestBoss;
            currentMusicFile = nearestMusic;
            playMusic(currentMusicFile);

        } else if (nearestBoss == null && currentBossMusic != null) {
            // Нет боссов рядом - останавливаем музыку
            stopMusic();
            currentBossMusic = null;
            currentMusicFile = null;
        }
    }

    // Текущий воспроизводимый звук
    private static SoundInstance currentSoundInstance = null;

    @OnlyIn(Dist.CLIENT)
    private static void playMusic(String musicFile) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getSoundManager() == null || mc.player == null) {
                MobEditorMod.LOGGER.warn("Не удалось воспроизвести музыку: SoundManager или Player недоступны");
                return;
            }

            if (musicFile == null || musicFile.isEmpty()) {
                MobEditorMod.LOGGER.warn("Пустое имя файла музыки");
                return;
            }

            MobEditorMod.LOGGER.info("=== Попытка воспроизведения музыки босса: {} ===", musicFile);

            // Останавливаем предыдущую музыку если играет
            if (currentSoundInstance != null) {
                mc.getSoundManager().stop(currentSoundInstance);
                currentSoundInstance = null;
                MobEditorMod.LOGGER.info("Остановлена предыдущая музыка");
            }

            // Используем CustomSoundManager для воспроизведения
            currentSoundInstance = CustomSoundManager.playCustomMusic(musicFile);

            if (currentSoundInstance != null) {
                MobEditorMod.LOGGER.info("✓ Музыка успешно запущена: {}", musicFile);
            } else {
                MobEditorMod.LOGGER.error("✗ Не удалось воспроизвести музыку: {}", musicFile);
                MobEditorMod.LOGGER.error("Проверьте:");
                MobEditorMod.LOGGER.error("1. Файл {} находится в src/main/resources/assets/mobeditor/sounds/",
                        musicFile);
                MobEditorMod.LOGGER.error("2. Звук зарегистрирован в sounds.json");
                MobEditorMod.LOGGER.error("3. Запустите задачу generateSoundsJson перед компиляцией");
            }

        } catch (Exception e) {
            MobEditorMod.LOGGER.error("Ошибка воспроизведения музыки босса: {}", musicFile, e);
            e.printStackTrace();
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void stopMusic() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() != null) {
            // Останавливаем текущий звук если есть
            if (currentSoundInstance != null) {
                mc.getSoundManager().stop(currentSoundInstance);
                currentSoundInstance = null;
            }
            // Останавливаем всю музыку
            mc.getSoundManager().stop(null, SoundSource.MUSIC);
        }
    }
}
