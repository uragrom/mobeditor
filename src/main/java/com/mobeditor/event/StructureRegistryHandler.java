package com.mobeditor.event;

import com.mobeditor.MobEditorMod;
import com.mobeditor.config.MobConfig;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Обработчик регистрации структур - модифицирует структуры ДО их использования
 * Использует ServerAboutToStartEvent для модификации структур ПЕРЕД запуском
 * игры
 */
public class StructureRegistryHandler {

    // Кэш для хранения настроек структур
    private static final Map<String, MobConfig.StructureSettings> structureSettingsCache = new ConcurrentHashMap<>();

    // Флаг для отслеживания применения настроек
    private static boolean settingsApplied = false;

    /**
     * Применяет настройки структур ПЕРЕД запуском игры
     * Это самое раннее событие, когда можно модифицировать структуры
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        if (MobEditorMod.getConfig() == null) {
            MobEditorMod.LOGGER.warn("StructureRegistryHandler: Конфигурация не загружена!");
            return;
        }

        if (settingsApplied) {
            return;
        }

        MobEditorMod.LOGGER.info("StructureRegistryHandler: Применяем настройки структур ПЕРЕД запуском игры...");

        try {
            MinecraftServer server = event.getServer();

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
            int totalSets = 0;

            // Проходим по всем наборам структур
            for (var entry : structureSetRegistry.entrySet()) {
                ResourceLocation setKeyLocation = entry.getKey().location();
                StructureSet structureSet = entry.getValue();
                totalSets++;

                try {
                    // Получаем структуры из набора
                    List<StructureSet.StructureSelectionEntry> structures = structureSet.structures();
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
                                "StructureRegistryHandler: Найдены настройки для структуры: {} в наборе {}",
                                structureIdString, setKeyLocation);

                        // Если структура отключена, НЕ добавляем её в список
                        if (!settings.isEnabled()) {
                            disabledCount++;
                            MobEditorMod.LOGGER.info(
                                    "StructureRegistryHandler: Структура {} отключена и будет удалена из набора {}",
                                    structureIdString, setKeyLocation);
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
                                        "StructureRegistryHandler: Структура {} имеет измененный шанс спавна {}%",
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
                                    "StructureRegistryHandler: Список структур в наборе {} успешно изменен (было: {}, стало: {})",
                                    setKeyLocation, structures.size(), modifiedStructures.size());
                        } else {
                            MobEditorMod.LOGGER.error(
                                    "StructureRegistryHandler: КРИТИЧЕСКАЯ ОШИБКА - Не удалось изменить список структур в наборе {}",
                                    setKeyLocation);
                        }
                    }

                    // Модифицируем placement для изменения расстояния
                    if (hasSettings && hasDistanceSettings(structureSet, structureRegistry)) {
                        boolean modified = modifyStructurePlacementAggressive(structureSet, structureRegistry);
                        if (modified) {
                            spacingModifiedCount++;
                            MobEditorMod.LOGGER.info(
                                    "StructureRegistryHandler: Placement для набора {} успешно изменен",
                                    setKeyLocation);
                        }
                    }

                } catch (Exception e) {
                    MobEditorMod.LOGGER.error("StructureRegistryHandler: Ошибка при обработке набора структур {}: {}",
                            setKeyLocation, e.getMessage(), e);
                }
            }

            settingsApplied = true;

            MobEditorMod.LOGGER.info(
                    "StructureRegistryHandler: Обработано наборов: {}, отключено: {}, шанс изменен: {}, расстояние изменено: {}",
                    totalSets, disabledCount, modifiedCount, spacingModifiedCount);

        } catch (Exception e) {
            MobEditorMod.LOGGER.error("StructureRegistryHandler: КРИТИЧЕСКАЯ ОШИБКА при применении настроек структур",
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
            MobEditorMod.LOGGER.info("StructureRegistryHandler: Загружено {} настроек структур в кэш",
                    allSettings.size());

            // Детальное логирование всех настроек
            if (allSettings.isEmpty()) {
                MobEditorMod.LOGGER
                        .warn("StructureRegistryHandler: ВНИМАНИЕ! Настройки структур НЕ ЗАГРУЖЕНЫ из конфига!");
                MobEditorMod.LOGGER.warn(
                        "StructureRegistryHandler: Проверьте файл structure_settings.json в папке config/mobeditor/");
            } else {
                MobEditorMod.LOGGER.info("StructureRegistryHandler: Список загруженных настроек структур:");
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
            MobEditorMod.LOGGER.error("StructureRegistryHandler: КРИТИЧЕСКАЯ ОШИБКА! Конфигурация не загружена!");
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
     * Благодаря Access Transformer поле weight доступно для записи
     */
    private StructureSet.StructureSelectionEntry modifyStructureWeight(
            StructureSet.StructureSelectionEntry entry, double spawnChance) {
        try {
            // Ищем поле weight в StructureSelectionEntry
            Field weightField = findField(entry.getClass(), "weight");
            if (weightField == null) {
                weightField = findFieldByType(entry.getClass(), int.class);
            }

            if (weightField != null) {
                weightField.setAccessible(true);
                int currentWeight = weightField.getInt(entry);
                int newWeight = Math.max(1, (int) (currentWeight * spawnChance));

                if (newWeight != currentWeight) {
                    // Благодаря Access Transformer поле уже не final, можем напрямую записывать
                    weightField.setInt(entry, newWeight);
                    int verifyWeight = weightField.getInt(entry);
                    if (verifyWeight == newWeight) {
                        MobEditorMod.LOGGER.debug("Изменен вес структуры с {} на {} (шанс: {}%)",
                                currentWeight, newWeight, spawnChance * 100);
                        return entry;
                    }
                }
            }
        } catch (Exception e) {
            MobEditorMod.LOGGER.debug("Не удалось изменить вес структуры: {}", e.getMessage());
        }
        return entry;
    }

    /**
     * Модифицирует placement для изменения расстояния между структурами
     * Использует проверку типа RandomSpreadStructurePlacement и Access Transformer
     */
    private boolean modifyStructurePlacementAggressive(StructureSet structureSet,
            Registry<Structure> structureRegistry) {
        try {
            Field placementField = findField(StructureSet.class, "placement");
            if (placementField == null) {
                placementField = findFieldByType(StructureSet.class, StructurePlacement.class);
            }

            if (placementField == null) {
                return false;
            }

            placementField.setAccessible(true);
            StructurePlacement placement = (StructurePlacement) placementField.get(structureSet);

            if (placement == null) {
                return false;
            }

            // КРИТИЧНО: Проверяем, что placement является RandomSpreadStructurePlacement
            // Поля spacing и separation есть только в этом классе, а не в базовом
            // StructurePlacement
            if (!(placement instanceof RandomSpreadStructurePlacement spreadPlacement)) {
                MobEditorMod.LOGGER.debug("Placement {} не является RandomSpreadStructurePlacement, пропускаем",
                        placement.getClass().getSimpleName());
                return false;
            }

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

            // Благодаря Access Transformer поля spacing и separation доступны для записи
            if (minSpacing > 0) {
                Field spacingField = findField(RandomSpreadStructurePlacement.class, "spacing");
                if (spacingField != null) {
                    spacingField.setAccessible(true);
                    int currentSpacing = spacingField.getInt(spreadPlacement);
                    if (currentSpacing != minSpacing) {
                        spacingField.setInt(spreadPlacement, minSpacing);
                        int verifySpacing = spacingField.getInt(spreadPlacement);
                        if (verifySpacing == minSpacing) {
                            MobEditorMod.LOGGER.info("Изменено spacing в RandomSpreadStructurePlacement с {} на {}",
                                    currentSpacing, minSpacing);
                            modified = true;
                        }
                    }
                }
            }

            if (minSeparation > 0) {
                Field separationField = findField(RandomSpreadStructurePlacement.class, "separation");
                if (separationField != null) {
                    separationField.setAccessible(true);
                    int currentSeparation = separationField.getInt(spreadPlacement);
                    if (currentSeparation != minSeparation) {
                        separationField.setInt(spreadPlacement, minSeparation);
                        int verifySeparation = separationField.getInt(spreadPlacement);
                        if (verifySeparation == minSeparation) {
                            MobEditorMod.LOGGER.info("Изменено separation в RandomSpreadStructurePlacement с {} на {}",
                                    currentSeparation, minSeparation);
                            modified = true;
                        }
                    }
                }
            }

            return modified;
        } catch (Exception e) {
            MobEditorMod.LOGGER.error("Ошибка при модификации placement: {}", e.getMessage(), e);
            return false;
        }
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

        return replaceStructuresListByType(structureSet, newStructures);
    }

    /**
     * Заменяет список структур по имени поля
     * Благодаря Access Transformer поле structures доступно для записи
     */
    private boolean replaceStructuresListByFieldName(StructureSet structureSet,
            List<StructureSet.StructureSelectionEntry> newStructures, String fieldName) {
        try {
            Field structuresField = findField(StructureSet.class, fieldName);
            if (structuresField != null) {
                structuresField.setAccessible(true);

                // Благодаря Access Transformer поле уже не final, можем напрямую записывать
                List<StructureSet.StructureSelectionEntry> mutableList = new ArrayList<>(newStructures);
                structuresField.set(structureSet, mutableList);

                Object verifyValue = structuresField.get(structureSet);
                if (verifyValue instanceof List && ((List<?>) verifyValue).size() == newStructures.size()) {
                    MobEditorMod.LOGGER.info("Успешно заменен список через поле {} (было: {}, стало: {})",
                            fieldName, structureSet.structures().size(), newStructures.size());
                    return true;
                }
            }
        } catch (Exception e) {
            MobEditorMod.LOGGER.debug("Ошибка при замене списка через поле {}: {}", fieldName, e.getMessage());
        }
        return false;
    }

    /**
     * Заменяет список структур по типу
     * Благодаря Access Transformer поле structures доступно для записи
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
                            // Благодаря Access Transformer поле уже не final, можем напрямую записывать
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
            MobEditorMod.LOGGER.debug("Ошибка при замене списка по типу: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Заменяет список структур в StructureSet через рефлексию
     * Благодаря Access Transformer поле structures доступно для записи
     */
    private boolean replaceStructuresList(StructureSet structureSet,
            List<StructureSet.StructureSelectionEntry> newStructures) {
        try {
            Field structuresField = findField(StructureSet.class, "structures");
            if (structuresField == null) {
                structuresField = findFieldByType(StructureSet.class, List.class);
            }

            if (structuresField != null) {
                structuresField.setAccessible(true);

                // Благодаря Access Transformer поле уже не final, можем напрямую записывать
                Object currentValue = structuresField.get(structureSet);
                if (currentValue instanceof List) {
                    int oldSize = ((List<?>) currentValue).size();
                    List<StructureSet.StructureSelectionEntry> mutableList = new ArrayList<>(newStructures);

                    try {
                        structuresField.set(structureSet, mutableList);
                    } catch (IllegalAccessException e) {
                        MobEditorMod.LOGGER.warn("Не удалось установить значение через set(): {}", e.getMessage());
                        return false;
                    }

                    Object verifyValue = structuresField.get(structureSet);
                    if (verifyValue instanceof List) {
                        int newSize = ((List<?>) verifyValue).size();
                        if (newSize == newStructures.size()) {
                            MobEditorMod.LOGGER.info(
                                    "✓ УСПЕХ! Заменен список структур в StructureSet (было: {}, стало: {})",
                                    oldSize, newSize);
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            MobEditorMod.LOGGER.warn("Ошибка при замене списка структур в StructureSet: {}", e.getMessage());
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
}
