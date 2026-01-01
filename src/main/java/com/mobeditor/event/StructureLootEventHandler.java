package com.mobeditor.event;

import com.mobeditor.MobEditorMod;
import com.mobeditor.config.MobConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class StructureLootEventHandler {

    @SubscribeEvent
    public void onLootTableLoad(LootTableLoadEvent event) {
        // Проверяем, что конфиг загружен
        if (MobEditorMod.getConfig() == null) {
            MobEditorMod.LOGGER.warn("StructureLootEventHandler: Конфигурация не загружена, пропускаем модификацию лута");
            return;
        }

        ResourceLocation lootTableId = event.getName();
        if (lootTableId == null) {
            return;
        }
        
        String lootTableIdString = lootTableId.toString();
        
        // Проверяем, есть ли кастомный лут для этой таблицы
        List<MobConfig.LootEntry> customLoot = MobEditorMod.getConfig().getStructureLoot(lootTableIdString);

        if (customLoot == null || customLoot.isEmpty()) {
            return;
        }

        MobEditorMod.LOGGER.info("StructureLootEventHandler: Модифицируем таблицу лута: {}", lootTableId);

        LootTable originalTable = event.getTable();

        try {
            // Получаем пулы из исходной таблицы
            List<LootPool> originalPools = getPoolsFromTable(originalTable);
            
            if (originalPools == null) {
                MobEditorMod.LOGGER.warn("StructureLootEventHandler: Не удалось получить пулы из таблицы лута: {}. Global Loot Modifier будет использован как резервный механизм.", lootTableId);
                return;
            }

            // Проверяем, не был ли уже добавлен наш пул (избегаем дублирования)
            boolean hasCustomPool = originalPools.stream()
                    .anyMatch(pool -> {
                        try {
                            Field nameField = LootPool.class.getDeclaredField("name");
                            nameField.setAccessible(true);
                            String poolName = (String) nameField.get(pool);
                            return "mobeditor_custom_loot".equals(poolName);
                        } catch (Exception e) {
                            return false;
                        }
                    });

            if (hasCustomPool) {
                MobEditorMod.LOGGER.debug("StructureLootEventHandler: Пул mobeditor_custom_loot уже существует в таблице: {}", lootTableId);
                return;
            }

            // Создаём новый изменяемый список для пулов
            List<LootPool> newPools = new ArrayList<>(originalPools);

            // Создаём новый пул для кастомного лута
            LootPool.Builder customPoolBuilder = LootPool.lootPool().name("mobeditor_custom_loot");

            int addedItems = 0;
            for (MobConfig.LootEntry lootEntry : customLoot) {
                ResourceLocation itemId = new ResourceLocation(lootEntry.getItemId());

                if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
                    MobEditorMod.LOGGER.warn("StructureLootEventHandler: Предмет не найден: {}", itemId);
                    continue;
                }

                Item item = BuiltInRegistries.ITEM.get(itemId);

                // Преобразуем шанс в вес (шанс 1.0 = 100% = вес 100)
                // Для правильной работы шанса используем вес, где шанс 1.0 = вес 1
                int weight = Math.max(1, (int) (lootEntry.getChance() * 100));

                LootItem.Builder lootItemBuilder = LootItem.lootTableItem(item)
                        .setWeight(weight)
                        .apply(SetItemCountFunction.setCount(
                                UniformGenerator.between((float) lootEntry.getMinCount(), (float) lootEntry.getMaxCount())));

                customPoolBuilder.add(lootItemBuilder);
                addedItems++;
                
                MobEditorMod.LOGGER.debug("StructureLootEventHandler: Добавлен предмет {} (x{}-{}, шанс: {}%, вес: {})", 
                        itemId, lootEntry.getMinCount(), lootEntry.getMaxCount(), lootEntry.getChance() * 100, weight);
            }

            if (addedItems > 0) {
                // Добавляем кастомный пул в список
                newPools.add(customPoolBuilder.build());
                
                // Заменяем пулы в исходной таблице
                setPoolsInTable(originalTable, newPools);

                MobEditorMod.LOGGER.info("StructureLootEventHandler: Таблица лута {} успешно модифицирована! Добавлено {} предметов в новый пул. Всего пулов: {}", 
                        lootTableId, addedItems, newPools.size());
            } else {
                MobEditorMod.LOGGER.warn("StructureLootEventHandler: Не удалось добавить ни одного предмета в таблицу лута: {}", lootTableId);
            }

        } catch (Exception e) {
            MobEditorMod.LOGGER.error("Ошибка при модификации таблицы лута {}: {}", lootTableId, e.getMessage(), e);
            // Не выбрасываем исключение, чтобы Global Loot Modifier мог работать как резервный механизм
        }
    }

    /**
     * Получает список пулов из LootTable используя рефлексию
     * Пробует несколько вариантов имен полей для совместимости с разными версиями
     */
    @SuppressWarnings("unchecked")
    private List<LootPool> getPoolsFromTable(LootTable table) {
        // Список возможных имен полей для пулов
        String[] possibleFieldNames = {"pools", "f_79109_", "f_79110_"};
        
        for (String fieldName : possibleFieldNames) {
            try {
                Field poolsField = LootTable.class.getDeclaredField(fieldName);
                poolsField.setAccessible(true);
                Object pools = poolsField.get(table);
                if (pools instanceof List) {
                    return (List<LootPool>) pools;
                }
            } catch (NoSuchFieldException e) {
                // Пробуем следующее имя поля
                continue;
            } catch (Exception e) {
                MobEditorMod.LOGGER.debug("Ошибка при получении поля {} из LootTable: {}", fieldName, e.getMessage());
                continue;
            }
        }
        
        // Если ничего не сработало, пробуем найти поле по типу
        try {
            Field[] fields = LootTable.class.getDeclaredFields();
            for (Field field : fields) {
                if (List.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Object value = field.get(table);
                    if (value instanceof List) {
                        List<?> list = (List<?>) value;
                        if (!list.isEmpty() && list.get(0) instanceof LootPool) {
                            return (List<LootPool>) list;
                        }
                    }
                }
            }
        } catch (Exception e) {
            MobEditorMod.LOGGER.debug("Ошибка при поиске поля pools в LootTable: {}", e.getMessage());
        }
        
        MobEditorMod.LOGGER.warn("Не удалось найти поле pools в LootTable. Используйте Global Loot Modifier как резервный механизм.");
        return null;
    }

    /**
     * Устанавливает список пулов в LootTable используя рефлексию
     * Пробует несколько вариантов имен полей для совместимости с разными версиями
     */
    private void setPoolsInTable(LootTable table, List<LootPool> pools) {
        // Список возможных имен полей для пулов
        String[] possibleFieldNames = {"pools", "f_79109_", "f_79110_"};
        
        for (String fieldName : possibleFieldNames) {
            try {
                Field poolsField = LootTable.class.getDeclaredField(fieldName);
                poolsField.setAccessible(true);
                if (List.class.isAssignableFrom(poolsField.getType())) {
                    poolsField.set(table, pools);
                    return;
                }
            } catch (NoSuchFieldException e) {
                // Пробуем следующее имя поля
                continue;
            } catch (Exception e) {
                MobEditorMod.LOGGER.debug("Ошибка при установке поля {} в LootTable: {}", fieldName, e.getMessage());
                continue;
            }
        }
        
        // Если ничего не сработало, пробуем найти поле по типу
        try {
            Field[] fields = LootTable.class.getDeclaredFields();
            for (Field field : fields) {
                if (List.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Object value = field.get(table);
                    if (value instanceof List) {
                        List<?> list = (List<?>) value;
                        if (!list.isEmpty() && list.get(0) instanceof LootPool) {
                            field.set(table, pools);
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            MobEditorMod.LOGGER.warn("Не удалось установить поле pools в LootTable: {}", e.getMessage());
        }
    }
}

