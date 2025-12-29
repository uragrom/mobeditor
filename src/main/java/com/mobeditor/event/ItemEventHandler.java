package com.mobeditor.event;

import com.mobeditor.MobEditorMod;
import com.mobeditor.config.MobConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

public class ItemEventHandler {

    // UUID для модификаторов (должны быть уникальными для нашего мода)
    private static final UUID CUSTOM_DAMAGE_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final UUID CUSTOM_SPEED_UUID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");
    private static final UUID CUSTOM_ARMOR_UUID = UUID.fromString("c3d4e5f6-a7b8-9012-cdef-123456789012");
    private static final UUID CUSTOM_TOUGHNESS_UUID = UUID.fromString("d4e5f6a7-b8c9-0123-defa-234567890123");
    private static final UUID CUSTOM_KNOCKBACK_UUID = UUID.fromString("e5f6a7b8-c9d0-1234-efab-345678901234");
    private static final UUID CUSTOM_MOVEMENT_UUID = UUID.fromString("f6a7b8c9-d0e1-2345-fabc-456789012345");

    @SubscribeEvent
    public void onItemAttributeModifier(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        EquipmentSlot slot = event.getSlotType();

        if (stack.isEmpty()) {
            return;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        MobConfig.ItemStats stats = MobEditorMod.getConfig().getItemStats(itemId.toString());

        if (stats == null || !stats.hasAnyStats()) {
            return;
        }

        // Урон атаки (только для оружия в основной руке)
        if (stats.getAttackDamage() != null && slot == EquipmentSlot.MAINHAND) {
            // Удаляем стандартный модификатор урона и добавляем свой
            event.removeAttribute(Attributes.ATTACK_DAMAGE);
            event.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                    CUSTOM_DAMAGE_UUID,
                    "Custom Attack Damage",
                    stats.getAttackDamage(),
                    AttributeModifier.Operation.ADDITION));
        }

        // Скорость атаки (только для оружия в основной руке)
        if (stats.getAttackSpeed() != null && slot == EquipmentSlot.MAINHAND) {
            event.removeAttribute(Attributes.ATTACK_SPEED);
            event.addModifier(Attributes.ATTACK_SPEED, new AttributeModifier(
                    CUSTOM_SPEED_UUID,
                    "Custom Attack Speed",
                    stats.getAttackSpeed() - 4.0, // Базовая скорость 4.0
                    AttributeModifier.Operation.ADDITION));
        }

        // Броня (для брони)
        if (stats.getArmor() != null && isArmorSlot(slot)) {
            event.removeAttribute(Attributes.ARMOR);
            event.addModifier(Attributes.ARMOR, new AttributeModifier(
                    CUSTOM_ARMOR_UUID,
                    "Custom Armor",
                    stats.getArmor(),
                    AttributeModifier.Operation.ADDITION));
        }

        // Прочность брони
        if (stats.getArmorToughness() != null && isArmorSlot(slot)) {
            event.removeAttribute(Attributes.ARMOR_TOUGHNESS);
            event.addModifier(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(
                    CUSTOM_TOUGHNESS_UUID,
                    "Custom Armor Toughness",
                    stats.getArmorToughness(),
                    AttributeModifier.Operation.ADDITION));
        }

        // Сопротивление отбрасыванию
        if (stats.getKnockbackResist() != null && isArmorSlot(slot)) {
            event.addModifier(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(
                    CUSTOM_KNOCKBACK_UUID,
                    "Custom Knockback Resistance",
                    stats.getKnockbackResist(),
                    AttributeModifier.Operation.ADDITION));
        }

        // Скорость передвижения
        if (stats.getMovementSpeed() != null) {
            event.addModifier(Attributes.MOVEMENT_SPEED, new AttributeModifier(
                    CUSTOM_MOVEMENT_UUID,
                    "Custom Movement Speed",
                    stats.getMovementSpeed(),
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private boolean isArmorSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST ||
                slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
    }
}
