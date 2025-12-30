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
        
        if (lootTableId == null) {
            return generatedLoot;
        }

        // Получаем кастомный лут для этой таблицы лута
        List<MobConfig.LootEntry> customLoot = MobEditorMod.getConfig().getStructureLoot(lootTableId.toString());

        for (MobConfig.LootEntry lootEntry : customLoot) {
            // Проверяем шанс
            if (RANDOM.nextFloat() > lootEntry.getChance()) {
                continue;
            }

            ResourceLocation itemId = new ResourceLocation(lootEntry.getItemId());

            if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
                continue;
            }

            Item item = BuiltInRegistries.ITEM.get(itemId);

            // Случайное количество между min и max
            int count = lootEntry.getMinCount();
            if (lootEntry.getMaxCount() > lootEntry.getMinCount()) {
                count += RANDOM.nextInt(lootEntry.getMaxCount() - lootEntry.getMinCount() + 1);
            }

            generatedLoot.add(new ItemStack(item, count));
        }

        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}

