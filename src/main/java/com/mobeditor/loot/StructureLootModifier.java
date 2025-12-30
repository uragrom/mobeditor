package com.mobeditor.loot;

import com.google.common.base.Suppliers;
import com.mobeditor.MobEditorMod;
import com.mobeditor.config.MobConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class StructureLootModifier extends LootModifier {

    private static final Random RANDOM = new Random();

    public static final Supplier<Codec<StructureLootModifier>> CODEC = Suppliers
            .memoize(() -> RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, StructureLootModifier::new)));

    public StructureLootModifier(LootItemCondition[] conditionsIn) {
        super(conditionsIn);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot,
            LootContext context) {
        // Получаем ID таблицы лута
        ResourceLocation lootTableId = context.getQueriedLootTableId();
        
        // Логируем все вызовы для отладки
        if (lootTableId != null && lootTableId.toString().contains("chest")) {
            MobEditorMod.LOGGER.debug("StructureLootModifier: Вызван для таблицы лута: {}", lootTableId);
        }
        
        if (lootTableId == null) {
            return generatedLoot;
        }

        // Проверяем, что конфигурация загружена
        if (MobEditorMod.getConfig() == null) {
            MobEditorMod.LOGGER.warn("StructureLootModifier: Конфигурация не загружена!");
            return generatedLoot;
        }

        // Получаем кастомный лут для этой таблицы лута
        List<MobConfig.LootEntry> customLoot = MobEditorMod.getConfig().getStructureLoot(lootTableId.toString());

        if (!customLoot.isEmpty()) {
            MobEditorMod.LOGGER.info("StructureLootModifier: Найдено {} записей для таблицы лута: {}", 
                    customLoot.size(), lootTableId);
        }

        for (MobConfig.LootEntry lootEntry : customLoot) {
            // Проверяем шанс
            if (RANDOM.nextFloat() > lootEntry.getChance()) {
                continue;
            }

            ResourceLocation itemId = new ResourceLocation(lootEntry.getItemId());

            if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
                MobEditorMod.LOGGER.warn("StructureLootModifier: Предмет не найден: {}", itemId);
                continue;
            }

            Item item = BuiltInRegistries.ITEM.get(itemId);

            // Случайное количество между min и max
            int count = lootEntry.getMinCount();
            if (lootEntry.getMaxCount() > lootEntry.getMinCount()) {
                count += RANDOM.nextInt(lootEntry.getMaxCount() - lootEntry.getMinCount() + 1);
            }

            ItemStack stack = new ItemStack(item, count);
            generatedLoot.add(stack);
            
            MobEditorMod.LOGGER.info("StructureLootModifier: Добавлен предмет {} x{} для таблицы {}", 
                    itemId, count, lootTableId);
        }

        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}

