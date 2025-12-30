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
import java.util.List;
import java.util.Random;

public class StructureLootEventHandler {

    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public void onLootTableLoad(LootTableLoadEvent event) {
        if (MobEditorMod.getConfig() == null) {
            return;
        }

        ResourceLocation lootTableId = event.getName();
        List<MobConfig.LootEntry> customLoot = MobEditorMod.getConfig().getStructureLoot(lootTableId.toString());

        if (customLoot.isEmpty()) {
            return;
        }

        MobEditorMod.LOGGER.info("StructureLootEventHandler: Модифицируем таблицу лута: {}", lootTableId);

        LootTable originalTable = event.getTable();

        try {
            // Получаем пулы из исходной таблицы через рефлексию
            Field poolsField = LootTable.class.getDeclaredField("pools");
            poolsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<LootPool> originalPools = (List<LootPool>) poolsField.get(originalTable);

            // Создаём новый изменяемый список для пулов
            java.util.ArrayList<LootPool> newPools = new java.util.ArrayList<>(originalPools);

            // Создаём новый пул для кастомного лута
            LootPool.Builder customPoolBuilder = LootPool.lootPool().name("mobeditor_custom_loot");

            for (MobConfig.LootEntry lootEntry : customLoot) {
                ResourceLocation itemId = new ResourceLocation(lootEntry.getItemId());

                if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
                    MobEditorMod.LOGGER.warn("Предмет не найден: {}", itemId);
                    continue;
                }

                Item item = BuiltInRegistries.ITEM.get(itemId);

                // Преобразуем шанс в вес (шанс 1.0 = 100% = вес 100)
                int weight = Math.max(1, (int) (lootEntry.getChance() * 100));

                LootItem.Builder lootItemBuilder = LootItem.lootTableItem(item)
                        .setWeight(weight)
                        .apply(SetItemCountFunction.setCount(
                                UniformGenerator.between((float) lootEntry.getMinCount(), (float) lootEntry.getMaxCount())));

                customPoolBuilder.add(lootItemBuilder);
                
                MobEditorMod.LOGGER.info("StructureLootEventHandler: Добавлен предмет {} (x{}-{}, шанс: {}%)", 
                        itemId, lootEntry.getMinCount(), lootEntry.getMaxCount(), lootEntry.getChance() * 100);
            }

            // Добавляем кастомный пул в список
            newPools.add(customPoolBuilder.build());
            
            // Заменяем поле pools в исходной таблице новым списком
            poolsField.set(originalTable, newPools);

            MobEditorMod.LOGGER.info("StructureLootEventHandler: Таблица лута {} успешно модифицирована! Добавлено {} пулов, новый пул содержит {} предметов.", 
                    lootTableId, newPools.size(), customLoot.size());

        } catch (Exception e) {
            MobEditorMod.LOGGER.error("Ошибка при модификации таблицы лута {}: {}", lootTableId, e.getMessage(), e);
            e.printStackTrace();
        }
    }
}

