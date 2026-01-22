package com.mobeditor.event;

import com.mobeditor.MobEditorMod;
import com.mobeditor.config.MobConfig;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Обработчик событий для управления спавном структур.
 * Применяет настройки структур при запуске сервера, используя несколько
 * методов:
 * 1. Удаление отключенных структур из списка
 * 2. Изменение веса структур для изменения шанса спавна
 * 3. Изменение spacing/separation в placement для изменения расстояния
 * 4. Перехват генерации структур во время их создания
 */
public class StructureSpawnEventHandler {

    // Кэш для хранения настроек структур
    private static final Map<String, MobConfig.StructureSettings> structureSettingsCache = new ConcurrentHashMap<>();

    // Флаг для отслеживания применения настроек
    private static boolean settingsApplied = false;

    /**
     * Применяет настройки структур при запуске сервера
     * Используем ServerAboutToStartEvent с максимальным приоритетом - это самое
     * раннее событие
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        MobEditorMod.LOGGER.info("StructureSpawnEventHandler: Событие ServerAboutToStartEvent получено!");
        applyStructureSettings(event.getServer());
    }

    /**
     * Резервный метод - применяет настройки при ServerStartingEvent
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onServerStarting(ServerStartingEvent event) {
        if (!settingsApplied) {
            MobEditorMod.LOGGER.info("StructureSpawnEventHandler: Резервное применение через ServerStartingEvent");
            applyStructureSettings(event.getServer());
        }
    }

    /**
     * Сбрасывает флаг при остановке сервера
     */
    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        resetSettingsApplied();
    }

    /**
     * Основной метод применения настроек структур
     * Использует агрессивный подход - создает новые объекты вместо изменения
     * существующих
     */
    private void applyStructureSettings(MinecraftServer server) {
        if (MobEditorMod.getConfig() == null) {
            MobEditorMod.LOGGER.warn("StructureSpawnEventHandler: Конфигурация не загружена!");
            return;
        }

        if (settingsApplied) {
            MobEditorMod.LOGGER.debug("StructureSpawnEventHandler: Настройки уже применены, пропускаем");
            return;
        }

        MobEditorMod.LOGGER.info("StructureSpawnEventHandler: Применяем настройки структур (агрессивный режим)...");

        try {
            // Получаем реестр наборов структур
            var structureSetRegistry = server.registryAccess()
                    .registryOrThrow(Registries.STRUCTURE_SET);

            // Получаем реестр структур
            var structureRegistry = server.registryAccess()
                    .registryOrThrow(Registries.STRUCTURE);

            // Загружаем настройки в кэш
            loadStructureSettingsCache();

            int modifiedCount = 0;
            int disabledCount = 0;
            int spacingModifiedCount = 0;
            int totalStructures = 0;
            int totalSets = 0;

            // Проходим по всем наборам структур
            for (Map.Entry<ResourceKey<StructureSet>, StructureSet> entry : structureSetRegistry.entrySet()) {
                ResourceKey<StructureSet> setKey = entry.getKey();
                StructureSet structureSet = entry.getValue();
                totalSets++;

                // Получаем структуры из набора
                List<StructureSet.StructureSelectionEntry> structures = structureSet.structures();
                totalStructures += structures.size();

                // Создаём новый список для замены
                List<StructureSet.StructureSelectionEntry> modifiedStructures = new ArrayList<>();
                boolean listChanged = false;
                boolean hasSettings = false;

                for (StructureSet.StructureSelectionEntry selectionEntry : structures) {
                    Structure structure = selectionEntry.structure().value();
                    ResourceLocation structureId = structureRegistry.getKey(structure);

                    if (structureId == null) {
                        modifiedStructures.add(selectionEntry);
                        continue;
                    }

                    String structureIdString = structureId.toString();
                    MobConfig.StructureSettings settings = structureSettingsCache.get(structureIdString);

                    if (settings == null) {
                        modifiedStructures.add(selectionEntry);
                        continue;
                    }

                    hasSettings = true;
                    MobEditorMod.LOGGER.info(
                            "StructureSpawnEventHandler: Найдены настройки для структуры: {} в наборе {}",
                            structureIdString, setKey.location());

                    // Если структура отключена, НЕ добавляем её в список
                    if (!settings.isEnabled()) {
                        disabledCount++;
                        MobEditorMod.LOGGER.info(
                                "StructureSpawnEventHandler: Структура {} отключена и будет удалена из набора {}",
                                structureIdString, setKey.location());
                        listChanged = true;
                        continue;
                    }

                    // Модифицируем вес структуры для изменения шанса спавна
                    StructureSet.StructureSelectionEntry modifiedEntry = selectionEntry;
                    if (settings.getSpawnChance() < 1.0) {
                        modifiedEntry = modifyStructureWeight(selectionEntry, settings.getSpawnChance());
                        if (modifiedEntry != selectionEntry) {
                            modifiedCount++;
                            listChanged = true;
                            MobEditorMod.LOGGER.info(
                                    "StructureSpawnEventHandler: Структура {} имеет измененный шанс спавна {}%",
                                    structureIdString, settings.getSpawnChance() * 100);
                        }
                    }

                    modifiedStructures.add(modifiedEntry);
                }

                // Если список изменился, заменяем его АГРЕССИВНО
                if (listChanged) {
                    boolean replaced = replaceStructuresListAggressive(structureSet, modifiedStructures);
                    if (replaced) {
                        MobEditorMod.LOGGER.info(
                                "StructureSpawnEventHandler: Список структур в наборе {} успешно изменен (было: {}, стало: {})",
                                setKey.location(), structures.size(), modifiedStructures.size());
                    } else {
                        MobEditorMod.LOGGER.error(
                                "StructureSpawnEventHandler: КРИТИЧЕСКАЯ ОШИБКА - Не удалось изменить список структур в наборе {}",
                                setKey.location());
                    }
                }

                // Модифицируем placement для изменения расстояния
                if (hasSettings && hasDistanceSettings(structureSet, structureRegistry)) {
                    boolean modified = modifyStructurePlacementAggressive(structureSet, structureRegistry);
                    if (modified) {
                        spacingModifiedCount++;
                        MobEditorMod.LOGGER.info("StructureSpawnEventHandler: Placement для набора {} успешно изменен",
                                setKey.location());
                    } else {
                        MobEditorMod.LOGGER.warn(
                                "StructureSpawnEventHandler: Не удалось изменить placement для набора {}",
                                setKey.location());
                    }
                }
            }

            settingsApplied = true;

            MobEditorMod.LOGGER.info(
                    "StructureSpawnEventHandler: Обработано наборов: {}, структур: {}, отключено: {}, шанс изменен: {}, расстояние изменено: {}",
                    totalSets, totalStructures, disabledCount, modifiedCount, spacingModifiedCount);

            // Логируем все настройки структур для отладки
            MobEditorMod.LOGGER.info("StructureSpawnEventHandler: Загружено настроек структур в кэш: {}",
                    structureSettingsCache.size());
            if (MobEditorMod.LOGGER.isDebugEnabled()) {
                for (Map.Entry<String, MobConfig.StructureSettings> entry : structureSettingsCache.entrySet()) {
                    MobEditorMod.LOGGER.debug("  - {}: {}", entry.getKey(), entry.getValue());
                }
            }

        } catch (Exception e) {
            MobEditorMod.LOGGER.error("StructureSpawnEventHandler: КРИТИЧЕСКАЯ ОШИБКА при применении настроек структур",
                    e);
            e.printStackTrace();
        }
    }

    /**
     * Загружает настройки структур в кэш
     */
    private void loadStructureSettingsCache() {
        structureSettingsCache.clear();
        if (MobEditorMod.getConfig() != null) {
            Map<String, MobConfig.StructureSettings> allSettings = MobEditorMod.getConfig().getAllStructureSettings();
            structureSettingsCache.putAll(allSettings);
            MobEditorMod.LOGGER.info("StructureSpawnEventHandler: Загружено {} настроек структур в кэш",
                    allSettings.size());

            // Детальное логирование всех настроек
            if (allSettings.isEmpty()) {
                MobEditorMod.LOGGER
                        .warn("StructureSpawnEventHandler: ВНИМАНИЕ! Настройки структур НЕ ЗАГРУЖЕНЫ из конфига!");
                MobEditorMod.LOGGER.warn(
                        "StructureSpawnEventHandler: Проверьте файл structure_settings.json в папке config/mobeditor/");
            } else {
                MobEditorMod.LOGGER.info("StructureSpawnEventHandler: Список загруженных настроек структур:");
                for (Map.Entry<String, MobConfig.StructureSettings> entry : allSettings.entrySet()) {
                    MobConfig.StructureSettings settings = entry.getValue();
                    MobEditorMod.LOGGER.info("  - {}: enabled={}, spawnChance={}%, spacing={}, separation={}",
                            entry.getKey(),
                            settings.isEnabled(),
                            (int) (settings.getSpawnChance() * 100),
                            settings.getSpacing() > 0 ? settings.getSpacing() : "default",
                            settings.getSeparation() > 0 ? settings.getSeparation() : "default");
                }
            }
        } else {
            MobEditorMod.LOGGER.error("StructureSpawnEventHandler: КРИТИЧЕСКАЯ ОШИБКА! Конфигурация не загружена!");
        }
    }

    /**
     * Проверяет, есть ли структуры с настройками расстояния в наборе
     */
    private boolean hasDistanceSettings(StructureSet structureSet, Registry<Structure> structureRegistry) {
        List<StructureSet.StructureSelectionEntry> structures = structureSet.structures();
        for (StructureSet.StructureSelectionEntry selectionEntry : structures) {
            Structure structure = selectionEntry.structure().value();
            ResourceLocation structureId = structureRegistry.getKey(structure);
            if (structureId != null) {
                MobConfig.StructureSettings settings = structureSettingsCache.get(structureId.toString());
                if (settings != null && (settings.getSpacing() > 0 || settings.getSeparation() > 0 ||
                        settings.getMinDistance() > 0 || settings.getMaxDistance() < Integer.MAX_VALUE)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Модифицирует вес структуры для изменения шанса спавна
     */
    private StructureSet.StructureSelectionEntry modifyStructureWeight(
            StructureSet.StructureSelectionEntry entry, double spawnChance) {
        try {
            // Ищем поле weight в StructureSelectionEntry
            Field weightField = findField(entry.getClass(), "weight");
            if (weightField == null) {
                // Пробуем найти по типу (int)
                weightField = findFieldByType(entry.getClass(), int.class);
            }

            if (weightField != null) {
                weightField.setAccessible(true);

                // Пытаемся снять final модификатор
                if (Modifier.isFinal(weightField.getModifiers())) {
                    try {
                        Field modifiersField = Field.class.getDeclaredField("modifiers");
                        modifiersField.setAccessible(true);
                        modifiersField.setInt(weightField, weightField.getModifiers() & ~Modifier.FINAL);
                    } catch (Exception e) {
                        // Игнорируем
                    }
                }
                int currentWeight = weightField.getInt(entry);
                // Новый вес = старый вес * шанс спавна
                int newWeight = Math.max(1, (int) (currentWeight * spawnChance));

                if (newWeight != currentWeight) {
                    weightField.setInt(entry, newWeight);
                    // Проверяем, что изменение применено
                    int verifyWeight = weightField.getInt(entry);
                    if (verifyWeight == newWeight) {
                        MobEditorMod.LOGGER.debug("Изменен вес структуры с {} на {} (шанс: {}%)",
                                currentWeight, newWeight, spawnChance * 100);
                        return entry;
                    } else {
                        MobEditorMod.LOGGER.warn(
                                "Не удалось проверить изменение веса структуры (ожидалось: {}, получено: {})",
                                newWeight, verifyWeight);
                    }
                }
            } else {
                MobEditorMod.LOGGER.debug("Не удалось найти поле weight в StructureSelectionEntry");
            }

        } catch (Exception e) {
            MobEditorMod.LOGGER.debug("Не удалось изменить вес структуры: {}", e.getMessage());
        }
        return entry;
    }

    /**
     * Агрессивная модификация placement - пробует все возможные способы
     */
    private boolean modifyStructurePlacementAggressive(StructureSet structureSet,
            Registry<Structure> structureRegistry) {
        // Пробуем стандартный метод
        if (modifyStructurePlacement(structureSet, structureRegistry)) {
            return true;
        }

        // Пробуем альтернативные подходы
        return modifyStructurePlacementAlternative(structureSet, structureRegistry);
    }

    /**
     * Альтернативный метод модификации placement
     */
    private boolean modifyStructurePlacementAlternative(StructureSet structureSet,
            Registry<Structure> structureRegistry) {
        try {
            // Пробуем найти placement через разные имена полей
            String[] possiblePlacementNames = { "placement", "f_79108_", "f_79109_", "structurePlacement" };
            StructurePlacement placement = null;
            Field placementField = null;

            for (String fieldName : possiblePlacementNames) {
                Field field = findField(StructureSet.class, fieldName);
                if (field != null) {
                    field.setAccessible(true);
                    Object value = field.get(structureSet);
                    if (value instanceof StructurePlacement) {
                        placement = (StructurePlacement) value;
                        placementField = field;
                        break;
                    }
                }
            }

            if (placement == null) {
                placementField = findFieldByType(StructureSet.class, StructurePlacement.class);
                if (placementField != null) {
                    placementField.setAccessible(true);
                    placement = (StructurePlacement) placementField.get(structureSet);
                }
            }

            if (placement == null) {
                return false;
            }

            // Получаем настройки
            List<StructureSet.StructureSelectionEntry> structures = structureSet.structures();
            int minSpacing = -1;
            int minSeparation = -1;

            for (StructureSet.StructureSelectionEntry selectionEntry : structures) {
                Structure structure = selectionEntry.structure().value();
                ResourceLocation structureId = structureRegistry.getKey(structure);
                if (structureId != null) {
                    MobConfig.StructureSettings settings = structureSettingsCache.get(structureId.toString());
                    if (settings != null) {
                        if (settings.getSpacing() > 0 && (minSpacing == -1 || settings.getSpacing() < minSpacing)) {
                            minSpacing = settings.getSpacing();
                        }
                        if (settings.getSeparation() > 0
                                && (minSeparation == -1 || settings.getSeparation() < minSeparation)) {
                            minSeparation = settings.getSeparation();
                        }
                    }
                }
            }

            boolean modified = false;
            if (minSpacing > 0) {
                if (modifyPlacementField(placement, "spacing", minSpacing)) {
                    modified = true;
                }
            }
            if (minSeparation > 0) {
                if (modifyPlacementField(placement, "separation", minSeparation)) {
                    modified = true;
                }
            }

            return modified;
        } catch (Exception e) {
            MobEditorMod.LOGGER.debug("Ошибка при альтернативной модификации placement: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Модифицирует placement для изменения расстояния (spacing, separation)
     */
    private boolean modifyStructurePlacement(StructureSet structureSet, Registry<Structure> structureRegistry) {
        try {
            // Получаем placement через рефлексию
            Field placementField = findField(StructureSet.class, "placement");
            if (placementField == null) {
                placementField = findFieldByType(StructureSet.class, StructurePlacement.class);
            }

            if (placementField == null) {
                MobEditorMod.LOGGER.debug("Не удалось найти поле placement в StructureSet");
                return false;
            }

            placementField.setAccessible(true);

            // Пытаемся снять final модификатор
            if (Modifier.isFinal(placementField.getModifiers())) {
                try {
                    Field modifiersField = Field.class.getDeclaredField("modifiers");
                    modifiersField.setAccessible(true);
                    modifiersField.setInt(placementField, placementField.getModifiers() & ~Modifier.FINAL);
                } catch (Exception e) {
                    // Игнорируем
                }
            }
            StructurePlacement placement = (StructurePlacement) placementField.get(structureSet);

            if (placement == null) {
                MobEditorMod.LOGGER.debug("Placement в StructureSet равен null");
                return false;
            }

            // Получаем настройки для всех структур в наборе
            // Применяем максимальные/минимальные значения для всего набора
            List<StructureSet.StructureSelectionEntry> structures = structureSet.structures();
            int minSpacing = -1;
            int minSeparation = -1;

            for (StructureSet.StructureSelectionEntry selectionEntry : structures) {
                Structure structure = selectionEntry.structure().value();
                ResourceLocation structureId = structureRegistry.getKey(structure);
                if (structureId != null) {
                    MobConfig.StructureSettings settings = structureSettingsCache.get(structureId.toString());
                    if (settings != null) {
                        if (settings.getSpacing() > 0 && (minSpacing == -1 || settings.getSpacing() < minSpacing)) {
                            minSpacing = settings.getSpacing();
                        }
                        if (settings.getSeparation() > 0
                                && (minSeparation == -1 || settings.getSeparation() < minSeparation)) {
                            minSeparation = settings.getSeparation();
                        }
                    }
                }
            }

            boolean modified = false;

            // Модифицируем spacing
            if (minSpacing > 0) {
                if (modifyPlacementField(placement, "spacing", minSpacing)) {
                    modified = true;
                }
            }

            // Модифицируем separation
            if (minSeparation > 0) {
                if (modifyPlacementField(placement, "separation", minSeparation)) {
                    modified = true;
                }
            }

            return modified;

        } catch (Exception e) {
            MobEditorMod.LOGGER.debug("Ошибка при модификации placement: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Модифицирует поле в placement через рефлексию
     * Поддерживает различные типы StructurePlacement (RandomSpread, ConcentricRings
     * и т.д.)
     */
    private boolean modifyPlacementField(StructurePlacement placement, String fieldName, int value) {
        try {
            // Пробуем найти поле по имени
            Field field = findField(placement.getClass(), fieldName);
            if (field == null) {
                // Пробуем найти по типу с предпочтительным именем
                field = findFieldByType(placement.getClass(), int.class, fieldName);
            }

            if (field != null) {
                field.setAccessible(true);

                // Пытаемся снять final модификатор (может не работать в Java 17+)
                if (Modifier.isFinal(field.getModifiers())) {
                    try {
                        Field modifiersField = Field.class.getDeclaredField("modifiers");
                        modifiersField.setAccessible(true);
                        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                    } catch (Exception e) {
                        // Игнорируем - в Java 17+ это нормально
                    }
                }
                int currentValue = field.getInt(placement);

                if (currentValue != value) {
                    field.setInt(placement, value);
                    // Проверяем, что изменение применено
                    int verifyValue = field.getInt(placement);
                    if (verifyValue == value) {
                        MobEditorMod.LOGGER.info("Изменено поле {} в placement {} с {} на {}",
                                fieldName, placement.getClass().getSimpleName(), currentValue, value);
                        return true;
                    } else {
                        MobEditorMod.LOGGER.warn(
                                "Не удалось проверить изменение поля {} в placement {} (ожидалось: {}, получено: {})",
                                fieldName, placement.getClass().getSimpleName(), value, verifyValue);
                    }
                } else {
                    MobEditorMod.LOGGER.debug("Поле {} в placement {} уже имеет значение {}",
                            fieldName, placement.getClass().getSimpleName(), value);
                    return true;
                }
            } else {
                // Пробуем найти поле в родительских классах или интерфейсах
                MobEditorMod.LOGGER.debug(
                        "Не удалось найти поле {} в placement класса {}. Пробуем альтернативные методы...",
                        fieldName, placement.getClass().getName());

                // Пробуем найти все поля типа int для отладки
                if (MobEditorMod.LOGGER.isDebugEnabled()) {
                    Field[] allFields = placement.getClass().getDeclaredFields();
                    for (Field f : allFields) {
                        if (f.getType() == int.class && !Modifier.isStatic(f.getModifiers())) {
                            MobEditorMod.LOGGER.debug("Найдено поле типа int: {} в классе {}", f.getName(),
                                    placement.getClass().getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            MobEditorMod.LOGGER.debug("Не удалось изменить поле {} в placement {}: {}",
                    fieldName, placement.getClass().getName(), e.getMessage());
        }
        return false;
    }

    /**
     * Агрессивная замена списка структур - пробует все возможные способы
     */
    private boolean replaceStructuresListAggressive(StructureSet structureSet,
            List<StructureSet.StructureSelectionEntry> newStructures) {
        // Пробуем стандартный метод
        if (replaceStructuresList(structureSet, newStructures)) {
            return true;
        }

        // Пробуем альтернативные имена полей
        String[] possibleFieldNames = { "structures", "f_79109_", "f_79110_", "entries", "structureEntries" };
        for (String fieldName : possibleFieldNames) {
            if (replaceStructuresListByFieldName(structureSet, newStructures, fieldName)) {
                return true;
            }
        }

        // Пробуем найти поле по типу и заменить его
        return replaceStructuresListByType(structureSet, newStructures);
    }

    /**
     * Заменяет список структур по имени поля
     */
    private boolean replaceStructuresListByFieldName(StructureSet structureSet,
            List<StructureSet.StructureSelectionEntry> newStructures, String fieldName) {
        try {
            Field structuresField = findField(StructureSet.class, fieldName);
            if (structuresField != null) {
                structuresField.setAccessible(true);

                // Пытаемся снять final модификатор
                if (Modifier.isFinal(structuresField.getModifiers())) {
                    try {
                        Field modifiersField = Field.class.getDeclaredField("modifiers");
                        modifiersField.setAccessible(true);
                        modifiersField.setInt(structuresField, structuresField.getModifiers() & ~Modifier.FINAL);
                    } catch (Exception e) {
                        // Игнорируем
                    }
                }
                List<StructureSet.StructureSelectionEntry> mutableList = new ArrayList<>(newStructures);
                structuresField.set(structureSet, mutableList);

                // Проверяем
                Object verifyValue = structuresField.get(structureSet);
                if (verifyValue instanceof List && ((List<?>) verifyValue).size() == newStructures.size()) {
                    MobEditorMod.LOGGER.info("Успешно заменен список через поле {} (было: {}, стало: {})",
                            fieldName, structureSet.structures().size(), newStructures.size());
                    return true;
                }
            }
        } catch (Exception e) {
            // Игнорируем
        }
        return false;
    }

    /**
     * Заменяет список структур по типу
     */
    private boolean replaceStructuresListByType(StructureSet structureSet,
            List<StructureSet.StructureSelectionEntry> newStructures) {
        try {
            Field[] fields = StructureSet.class.getDeclaredFields();
            for (Field field : fields) {
                if (List.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Object value = field.get(structureSet);
                    if (value instanceof List) {
                        List<?> list = (List<?>) value;
                        if (!list.isEmpty() && list.get(0) instanceof StructureSet.StructureSelectionEntry) {
                            if (Modifier.isFinal(field.getModifiers())) {
                                try {
                                    Field modifiersField = Field.class.getDeclaredField("modifiers");
                                    modifiersField.setAccessible(true);
                                    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                                } catch (Exception e) {
                                    // Игнорируем
                                }
                            }
                            List<StructureSet.StructureSelectionEntry> mutableList = new ArrayList<>(newStructures);
                            field.set(structureSet, mutableList);
                            MobEditorMod.LOGGER.info("Успешно заменен список через поле типа List ({}): {}",
                                    field.getName(), newStructures.size());
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Игнорируем
        }
        return false;
    }

    /**
     * Заменяет список структур в StructureSet через рефлексию
     */
    private boolean replaceStructuresList(StructureSet structureSet,
            List<StructureSet.StructureSelectionEntry> newStructures) {
        try {
            // Ищем поле structures в StructureSet
            Field structuresField = findField(StructureSet.class, "structures");
            if (structuresField == null) {
                structuresField = findFieldByType(StructureSet.class, List.class);
            }

            if (structuresField != null) {
                structuresField.setAccessible(true);

                // Для final полей пытаемся снять модификатор
                if (Modifier.isFinal(structuresField.getModifiers())) {
                    try {
                        Field modifiersField = Field.class.getDeclaredField("modifiers");
                        modifiersField.setAccessible(true);
                        modifiersField.setInt(structuresField, structuresField.getModifiers() & ~Modifier.FINAL);
                    } catch (Exception e) {
                        MobEditorMod.LOGGER.debug("Не удалось изменить final: {}", e.getMessage());
                    }
                }

                // Проверяем, что это действительно список структур
                Object currentValue = structuresField.get(structureSet);
                if (currentValue instanceof List) {
                    int oldSize = ((List<?>) currentValue).size();
                    // Создаем изменяемый список (если оригинальный immutable)
                    List<StructureSet.StructureSelectionEntry> mutableList = new ArrayList<>(newStructures);
                    structuresField.set(structureSet, mutableList);

                    // Пытаемся установить значение напрямую, даже если поле final
                    try {
                        structuresField.set(structureSet, mutableList);
                    } catch (IllegalAccessException e) {
                        // Если не получилось через set, пробуем через Unsafe (если доступен)
                        MobEditorMod.LOGGER.warn("Не удалось установить значение через set(): {}", e.getMessage());
                        // Продолжаем - может быть значение все равно изменилось
                    }

                    // Проверяем, что изменение применено
                    Object verifyValue = structuresField.get(structureSet);
                    if (verifyValue instanceof List) {
                        int newSize = ((List<?>) verifyValue).size();
                        if (newSize == newStructures.size()) {
                            MobEditorMod.LOGGER.info(
                                    "✓ УСПЕХ! Заменен список структур в StructureSet (было: {}, стало: {})",
                                    oldSize, newSize);
                            return true;
                        } else {
                            MobEditorMod.LOGGER.warn(
                                    "⚠ Размер списка изменился, но не соответствует ожидаемому (ожидалось: {}, получено: {})",
                                    newStructures.size(), newSize);
                        }
                    } else {
                        MobEditorMod.LOGGER.warn(
                                "⚠ Не удалось проверить замену списка структур (ожидалось: {}, получено: {})",
                                newStructures.size(),
                                verifyValue != null ? verifyValue.getClass().getName() : "null");
                    }
                } else {
                    MobEditorMod.LOGGER.warn("Поле structures в StructureSet не является списком: {}",
                            currentValue != null ? currentValue.getClass().getName() : "null");
                }
            } else {
                MobEditorMod.LOGGER.warn("Не удалось найти поле structures в StructureSet для замены списка");
            }

        } catch (Exception e) {
            MobEditorMod.LOGGER.warn("Ошибка при замене списка структур в StructureSet: {}", e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Находит поле в классе по имени
     */
    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            try {
                Field field = currentClass.getDeclaredField(fieldName);
                return field;
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Находит поле в классе по типу
     */
    private Field findFieldByType(Class<?> clazz, Class<?> fieldType) {
        return findFieldByType(clazz, fieldType, null);
    }

    /**
     * Находит поле в классе по типу с опциональным именем
     */
    private Field findFieldByType(Class<?> clazz, Class<?> fieldType, String preferredName) {
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            try {
                Field[] fields = currentClass.getDeclaredFields();
                for (Field field : fields) {
                    if (fieldType.isAssignableFrom(field.getType())) {
                        // Если указано предпочтительное имя, проверяем его
                        if (preferredName == null || field.getName().contains(preferredName) ||
                                field.getName().equals(preferredName)) {
                            return field;
                        }
                    }
                }
                currentClass = currentClass.getSuperclass();
            } catch (Exception e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Проверяет, должна ли структура быть заблокирована
     */
    public static boolean shouldBlockStructure(ResourceLocation structureId) {
        if (MobEditorMod.getConfig() == null) {
            return false;
        }

        String structureIdString = structureId.toString();

        // Используем кэш, если он доступен
        MobConfig.StructureSettings settings = structureSettingsCache.get(structureIdString);
        if (settings == null) {
            settings = MobEditorMod.getConfig().getStructureSettings(structureIdString);
            if (settings != null) {
                structureSettingsCache.put(structureIdString, settings);
            }
        }

        if (settings != null && !settings.isEnabled()) {
            return true;
        }

        // Проверяем шанс спавна
        double spawnChance = settings != null ? settings.getSpawnChance()
                : MobEditorMod.getConfig().getStructureSpawnChance(structureIdString);
        if (spawnChance < 1.0 && Math.random() > spawnChance) {
            return true;
        }

        return false;
    }

    /**
     * Получает настройки структуры
     */
    public static MobConfig.StructureSettings getStructureSettings(ResourceLocation structureId) {
        if (MobEditorMod.getConfig() == null) {
            return null;
        }

        String structureIdString = structureId.toString();

        // Используем кэш, если он доступен
        MobConfig.StructureSettings settings = structureSettingsCache.get(structureIdString);
        if (settings == null) {
            settings = MobEditorMod.getConfig().getStructureSettings(structureIdString);
            if (settings != null) {
                structureSettingsCache.put(structureIdString, settings);
            }
        }

        return settings;
    }

    /**
     * Сбрасывает флаг применения настроек (для перезагрузки)
     */
    public static void resetSettingsApplied() {
        settingsApplied = false;
        structureSettingsCache.clear();
        MobEditorMod.LOGGER.debug("StructureSpawnEventHandler: Флаг применения настроек сброшен");
    }

    /**
     * Перезагружает настройки структур из конфига
     */
    public static void reloadStructureSettings() {
        structureSettingsCache.clear();
        if (MobEditorMod.getConfig() != null) {
            Map<String, MobConfig.StructureSettings> allSettings = MobEditorMod.getConfig().getAllStructureSettings();
            structureSettingsCache.putAll(allSettings);
            MobEditorMod.LOGGER.info("StructureSpawnEventHandler: Перезагружено {} настроек структур",
                    allSettings.size());
        }
        settingsApplied = false;
    }
}
