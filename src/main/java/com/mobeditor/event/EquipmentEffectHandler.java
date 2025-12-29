package com.mobeditor.event;

import com.mobeditor.MobEditorMod;
import com.mobeditor.config.MobConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Map;

public class EquipmentEffectHandler {

    // Применяем эффекты каждые 20 тиков (1 секунда)
    private int tickCounter = 0;

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (event.player.level().isClientSide()) {
            return;
        }

        tickCounter++;
        if (tickCounter < 20) {
            return;
        }
        tickCounter = 0;

        Player player = event.player;

        // Проверяем предмет в основной руке
        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty()) {
            applyItemEffects(player, mainHand);
        }

        // Проверяем предмет в дополнительной руке
        ItemStack offHand = player.getOffhandItem();
        if (!offHand.isEmpty()) {
            applyItemEffects(player, offHand);
        }

        // Проверяем броню
        for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                EquipmentSlot.FEET }) {
            ItemStack armorPiece = player.getItemBySlot(slot);
            if (!armorPiece.isEmpty()) {
                applyItemEffects(player, armorPiece);
            }
        }

        // Проверяем комплекты брони
        checkArmorSets(player);
    }

    private void applyItemEffects(Player player, ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        List<MobConfig.EffectEntry> effects = MobEditorMod.getConfig().getItemEffects(itemId.toString());

        for (MobConfig.EffectEntry entry : effects) {
            applyEffect(player, entry);
        }
    }

    private void checkArmorSets(Player player) {
        // Получаем ID всех элементов брони
        String helmetId = getItemId(player.getItemBySlot(EquipmentSlot.HEAD));
        String chestId = getItemId(player.getItemBySlot(EquipmentSlot.CHEST));
        String legsId = getItemId(player.getItemBySlot(EquipmentSlot.LEGS));
        String bootsId = getItemId(player.getItemBySlot(EquipmentSlot.FEET));

        // Проверяем все комплекты
        Map<String, MobConfig.ArmorSetBonus> sets = MobEditorMod.getConfig().getAllArmorSetBonuses();

        for (MobConfig.ArmorSetBonus bonus : sets.values()) {
            if (bonus.isComplete(helmetId, chestId, legsId, bootsId)) {
                // Применяем все эффекты комплекта
                for (MobConfig.EffectEntry effect : bonus.getEffects()) {
                    applyEffect(player, effect);
                }
            }
        }
    }

    private String getItemId(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private void applyEffect(Player player, MobConfig.EffectEntry entry) {
        try {
            ResourceLocation effectId = new ResourceLocation(entry.getEffectId());
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(effectId);

            if (effect != null) {
                // Длительность 2 секунды (40 тиков), чтобы эффект не мигал
                MobEffectInstance instance = new MobEffectInstance(
                        effect,
                        40, // 2 секунды
                        entry.getAmplifier(),
                        false, // ambient
                        entry.isShowParticles(),
                        entry.isShowIcon());
                player.addEffect(instance);
            }
        } catch (Exception e) {
            MobEditorMod.LOGGER.warn("Не удалось применить эффект: {}", entry.getEffectId());
        }
    }
}
