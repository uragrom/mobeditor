package com.mobeditor.sound;

import com.mobeditor.MobEditorMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class CustomSoundManager {

    private static final Map<String, SoundInstance> activeSounds = new HashMap<>();

    @OnlyIn(Dist.CLIENT)
    public static SoundEvent getOrRegisterSound(String musicFile) {
        if (musicFile == null || musicFile.isEmpty()) {
            MobEditorMod.LOGGER.warn("Пустое имя файла музыки");
            return null;
        }

        MobEditorMod.LOGGER.info("Поиск SoundEvent для файла: {}", musicFile);

        // Пытаемся получить звук из динамически загруженных звуков
        SoundEvent soundEvent = DynamicSoundLoader.getSound(musicFile);

        if (soundEvent != null) {
            // Проверяем, что звук действительно зарегистрирован
            ResourceLocation soundId = soundEvent.getLocation();
            SoundEvent registeredSound = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(soundId);
            if (registeredSound != null) {
                MobEditorMod.LOGGER.info("✓ Найден динамически загруженный звук для: {} -> {}", musicFile,
                        soundId);
                return registeredSound;
            } else {
                MobEditorMod.LOGGER.warn("Звук найден в кэше, но не зарегистрирован в реестре: {}", soundId);
            }
        }

        // Fallback: используем generic SoundEvent
        ResourceLocation genericId = new ResourceLocation(MobEditorMod.MOD_ID, "boss_music_generic");
        soundEvent = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(genericId);

        if (soundEvent != null) {
            MobEditorMod.LOGGER.warn("Используется generic SoundEvent для: {} (звук не найден в динамических)",
                    musicFile);
            return soundEvent;
        }

        MobEditorMod.LOGGER.error("✗ Не удалось найти SoundEvent для: {}", musicFile);
        MobEditorMod.LOGGER.error("Доступные звуки: {}", DynamicSoundLoader.getAllSounds().keySet());
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    public static SoundInstance playCustomMusic(String musicFile) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() == null || mc.player == null) {
            MobEditorMod.LOGGER.warn("Не удалось воспроизвести музыку: SoundManager или Player недоступны");
            return null;
        }

        MobEditorMod.LOGGER.info("=== Попытка воспроизведения музыки: {} ===", musicFile);

        // Останавливаем предыдущую музыку для этого файла, если она играет
        SoundInstance existingSound = activeSounds.get(musicFile);
        if (existingSound != null) {
            mc.getSoundManager().stop(existingSound);
            activeSounds.remove(musicFile);
            MobEditorMod.LOGGER.info("Остановлена предыдущая музыка для: {}", musicFile);
        }

        try {
            // Получаем SoundEvent (динамически загруженный или generic)
            SoundEvent soundEvent = getOrRegisterSound(musicFile);
            if (soundEvent == null) {
                MobEditorMod.LOGGER.error("✗ Не удалось получить SoundEvent для: {}", musicFile);
                return null;
            }

            ResourceLocation soundId = soundEvent.getLocation();
            MobEditorMod.LOGGER.info("Используется SoundEvent: {}", soundId);

            // Проверяем, что звук действительно зарегистрирован
            SoundEvent registeredSound = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(soundId);
            if (registeredSound == null) {
                MobEditorMod.LOGGER.error("✗ SoundEvent не найден в реестре: {}", soundId);
                return null;
            }

            // Создаем SoundInstance через SimpleSoundInstance.forMusic
            // Используем MUSIC как SoundSource для правильного воспроизведения
            SoundInstance soundInstance = net.minecraft.client.resources.sounds.SimpleSoundInstance.forMusic(
                    registeredSound);

            if (soundInstance == null) {
                MobEditorMod.LOGGER.error("✗ Не удалось создать SoundInstance для: {}", soundId);
                return null;
            }

            // Воспроизводим звук
            mc.getSoundManager().play(soundInstance);
            activeSounds.put(musicFile, soundInstance);

            MobEditorMod.LOGGER.info("✓ Музыка запущена: {} через SoundEvent: {}", musicFile, soundId);
            MobEditorMod.LOGGER.info("SoundInstance: {} (категория: MUSIC)", soundInstance);

            return soundInstance;

        } catch (Exception e) {
            MobEditorMod.LOGGER.error("✗ Ошибка воспроизведения музыки: {}", musicFile, e);
            e.printStackTrace();
            return null;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void stopMusic(String musicFile) {
        SoundInstance sound = activeSounds.remove(musicFile);
        if (sound != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getSoundManager() != null) {
                mc.getSoundManager().stop(sound);
                MobEditorMod.LOGGER.info("Остановлена музыка: {}", musicFile);
            }
        }
    }

    /**
     * Получить ResourceLocation для звука по имени файла
     * Формат: mobeditor:имя_файла (без расширения .ogg)
     */
    @OnlyIn(Dist.CLIENT)
    public static ResourceLocation getSoundResourceLocation(String musicFile) {
        String soundName = musicFile.toLowerCase();
        if (soundName.endsWith(".ogg")) {
            soundName = soundName.substring(0, soundName.length() - 4);
        }
        // Сохраняем первую букву, заменяем только остальные символы
        if (soundName.length() > 0) {
            char firstChar = soundName.charAt(0);
            String rest = soundName.substring(1).replaceAll("[^a-z0-9_]", "_");
            soundName = firstChar + rest;
        } else {
            soundName = soundName.replaceAll("[^a-z0-9_]", "_");
        }
        return new ResourceLocation(MobEditorMod.MOD_ID, soundName);
    }
}
