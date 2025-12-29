package com.mobeditor.sound;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mobeditor.MobEditorMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = MobEditorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class DynamicSoundLoader {

    private static final Map<String, ResourceLocation> dynamicSoundIds = new HashMap<>();
    private static final Map<String, SoundEvent> dynamicSounds = new HashMap<>();
    private static final Map<String, String> fileNameToSoundName = new HashMap<>(); // Имя файла -> имя звука

    @SubscribeEvent
    public static void onRegisterSounds(RegisterEvent event) {
        if (event.getRegistryKey() == ForgeRegistries.SOUND_EVENTS.getRegistryKey()) {
            // Загружаем звуки из ресурсов мода
            loadSoundsFromResources();

            MobEditorMod.LOGGER.info("=== Начинаем регистрацию {} звуков ===", dynamicSoundIds.size());

            // Регистрируем каждый найденный звук
            int registeredCount = 0;
            for (Map.Entry<String, ResourceLocation> entry : dynamicSoundIds.entrySet()) {
                String soundName = entry.getKey();
                ResourceLocation soundId = entry.getValue();

                try {
                    // Создаем SoundEvent для этого файла
                    SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(soundId);
                    dynamicSounds.put(soundName, soundEvent);

                    // Регистрируем звук
                    event.register(ForgeRegistries.SOUND_EVENTS.getRegistryKey(), soundId, () -> soundEvent);
                    registeredCount++;
                    MobEditorMod.LOGGER.info("✓ Зарегистрирован звук: mobeditor:{}", soundName);
                } catch (Exception e) {
                    MobEditorMod.LOGGER.error("✗ Ошибка регистрации звука: {} - {}", soundId, e.getMessage());
                    e.printStackTrace();
                }
            }

            MobEditorMod.LOGGER.info("=== Регистрация завершена: {}/{} звуков ===", registeredCount,
                    dynamicSoundIds.size());
        }
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Проверяем, что звуки действительно зарегистрированы
        event.enqueueWork(() -> {
            MobEditorMod.LOGGER.info("=== Проверка зарегистрированных звуков ===");
            for (Map.Entry<String, SoundEvent> entry : dynamicSounds.entrySet()) {
                ResourceLocation soundId = entry.getValue().getLocation();
                SoundEvent registeredSound = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(soundId);
                if (registeredSound != null) {
                    MobEditorMod.LOGGER.info("✓ Звук доступен в реестре: {}", soundId);
                } else {
                    MobEditorMod.LOGGER.error("✗ Звук НЕ найден в реестре: {}", soundId);
                }
            }
        });

        MobEditorMod.LOGGER.info("=== Динамическая загрузка звуков завершена ===");
        MobEditorMod.LOGGER.info("Загружено звуков: {}", dynamicSounds.size());
        if (!dynamicSounds.isEmpty()) {
            MobEditorMod.LOGGER.info("Доступные звуки: {}", dynamicSounds.keySet());
            MobEditorMod.LOGGER.info("Маппинг файлов: {}", fileNameToSoundName);
        }
    }

    /**
     * Загружает звуки из ресурсов мода (из jar файла)
     */
    private static void loadSoundsFromResources() {
        InputStream soundsJsonStream = null;
        try {
            // Пытаемся загрузить sounds.json из ресурсов
            soundsJsonStream = DynamicSoundLoader.class.getResourceAsStream(
                    "/assets/mobeditor/sounds.json");

            if (soundsJsonStream != null) {
                // Парсим sounds.json и извлекаем имена звуков
                String jsonContent = new String(soundsJsonStream.readAllBytes());
                JsonObject soundsJson = JsonParser.parseString(jsonContent).getAsJsonObject();

                MobEditorMod.LOGGER.info("Загружен sounds.json с {} записями", soundsJson.size());

                // Регистрируем каждый звук из sounds.json
                Set<String> soundNames = soundsJson.keySet();
                for (String soundName : soundNames) {
                    if (soundName != null && !soundName.isEmpty() && !soundName.equals("boss_music_generic")) {
                        try {
                            ResourceLocation soundId = new ResourceLocation(MobEditorMod.MOD_ID, soundName);
                            dynamicSoundIds.put(soundName, soundId);

                            // Создаем маппинг для поиска по имени файла
                            // Пробуем разные варианты имени файла
                            String[] possibleFileNames = {
                                    soundName + ".ogg",
                                    soundName.substring(1) + ".ogg", // Без первого символа если это _
                                    soundName.toUpperCase() + ".ogg",
                                    soundName.substring(0, 1).toUpperCase() + soundName.substring(1) + ".ogg"
                            };

                            for (String fileName : possibleFileNames) {
                                fileNameToSoundName.put(fileName.toLowerCase(), soundName);
                            }

                            MobEditorMod.LOGGER.info(
                                    "Найден звук в sounds.json: mobeditor:{} (возможные имена файлов: {})",
                                    soundName, String.join(", ", possibleFileNames));
                        } catch (Exception e) {
                            MobEditorMod.LOGGER.error("Ошибка создания ResourceLocation для звука: {}", soundName, e);
                        }
                    }
                }
            } else {
                MobEditorMod.LOGGER.error("sounds.json не найден в ресурсах мода! Путь: /assets/mobeditor/sounds.json");
            }

        } catch (Exception e) {
            MobEditorMod.LOGGER.error("Ошибка загрузки звуков из ресурсов", e);
            e.printStackTrace();
        } finally {
            if (soundsJsonStream != null) {
                try {
                    soundsJsonStream.close();
                } catch (Exception e) {
                    // Игнорируем ошибки закрытия
                }
            }
        }
    }

    /**
     * Получить SoundEvent по имени файла
     * Поддерживает звуки из ресурсов мода (уже в jar) и из конфига (копируются при
     * компиляции)
     */
    public static SoundEvent getSound(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            MobEditorMod.LOGGER.warn("Пустое имя файла при поиске звука");
            return null;
        }

        MobEditorMod.LOGGER.info("Поиск SoundEvent для файла: {}", fileName);

        try {
            String fileNameLower = fileName.toLowerCase();

            // Пробуем найти по маппингу файлов (точное совпадение)
            String soundName = fileNameToSoundName.get(fileNameLower);
            if (soundName != null) {
                SoundEvent sound = dynamicSounds.get(soundName);
                if (sound != null) {
                    // Проверяем, что звук действительно зарегистрирован
                    ResourceLocation soundId = sound.getLocation();
                    SoundEvent registeredSound = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT
                            .get(soundId);
                    if (registeredSound != null) {
                        MobEditorMod.LOGGER.info("✓ Найден звук по маппингу: {} -> mobeditor:{}", fileName, soundName);
                        return registeredSound;
                    } else {
                        MobEditorMod.LOGGER.warn("Звук найден в кэше, но не зарегистрирован: {}", soundId);
                    }
                }
            }

            // Нормализуем имя файла
            String normalizedFileName = fileNameLower;
            if (normalizedFileName.endsWith(".ogg")) {
                normalizedFileName = normalizedFileName.substring(0, normalizedFileName.length() - 4);
            }

            // Пробуем найти напрямую по нормализованному имени
            // Заменяем недопустимые символы, но сохраняем первую букву
            String soundNameFromFile = normalizedFileName;
            if (soundNameFromFile.length() > 0) {
                char firstChar = soundNameFromFile.charAt(0);
                String rest = soundNameFromFile.substring(1).replaceAll("[^a-z0-9_]", "_");
                soundNameFromFile = firstChar + rest;
            } else {
                soundNameFromFile = soundNameFromFile.replaceAll("[^a-z0-9_]", "_");
            }

            MobEditorMod.LOGGER.info("Нормализованное имя: {}", soundNameFromFile);

            // Пробуем найти в зарегистрированных звуках
            SoundEvent sound = dynamicSounds.get(soundNameFromFile);
            if (sound != null) {
                ResourceLocation soundId = sound.getLocation();
                SoundEvent registeredSound = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(soundId);
                if (registeredSound != null) {
                    MobEditorMod.LOGGER.info("✓ Найден звук по нормализованному имени: {} -> mobeditor:{}", fileName,
                            soundNameFromFile);
                    return registeredSound;
                }
            }

            // Пробуем найти по частичному совпадению
            for (Map.Entry<String, SoundEvent> entry : dynamicSounds.entrySet()) {
                String registeredName = entry.getKey();
                String registeredNameLower = registeredName.toLowerCase();

                // Проверяем различные варианты совпадения
                if (registeredNameLower.equals(normalizedFileName) ||
                        registeredNameLower.equals(soundNameFromFile) ||
                        registeredNameLower.contains(normalizedFileName) ||
                        normalizedFileName.contains(registeredNameLower)) {
                    ResourceLocation soundId = entry.getValue().getLocation();
                    SoundEvent registeredSound = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT
                            .get(soundId);
                    if (registeredSound != null) {
                        MobEditorMod.LOGGER.info("✓ Найден звук по частичному совпадению: {} -> mobeditor:{}", fileName,
                                registeredName);
                        return registeredSound;
                    }
                }
            }

            // Пробуем найти без учета регистра и специальных символов
            for (Map.Entry<String, SoundEvent> entry : dynamicSounds.entrySet()) {
                String registeredName = entry.getKey();
                String registeredNormalized = registeredName.toLowerCase().replaceAll("[^a-z0-9]", "");
                String fileNormalized = normalizedFileName.replaceAll("[^a-z0-9]", "");

                if (registeredNormalized.equals(fileNormalized)) {
                    ResourceLocation soundId = entry.getValue().getLocation();
                    SoundEvent registeredSound = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT
                            .get(soundId);
                    if (registeredSound != null) {
                        MobEditorMod.LOGGER.info("✓ Найден звук по нормализованному совпадению: {} -> mobeditor:{}",
                                fileName, registeredName);
                        return registeredSound;
                    }
                }
            }

            MobEditorMod.LOGGER.error("✗ Звук не найден: {} (нормализовано: {})", fileName, soundNameFromFile);
            MobEditorMod.LOGGER.error("Доступные звуки: {}", dynamicSounds.keySet());
            MobEditorMod.LOGGER.error("Маппинг файлов: {}", fileNameToSoundName);
            return null;

        } catch (Exception e) {
            MobEditorMod.LOGGER.error("Ошибка получения звука: {}", fileName, e);
            e.printStackTrace();
            return null;
        }
    }

    public static Map<String, SoundEvent> getAllSounds() {
        return new HashMap<>(dynamicSounds);
    }
}
