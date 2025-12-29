package com.mobeditor.event;

import com.mobeditor.MobEditorMod;
import com.mobeditor.config.MobConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class MobEventHandler {

    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity livingEntity)) {
            return;
        }

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(livingEntity.getType());

        // Применяем кастомное здоровье БЕЗ ОГРАНИЧЕНИЙ
        Double customHealth = MobEditorMod.getConfig().getMobHealth(entityId.toString());
        if (customHealth != null && customHealth > 0) {
            AttributeInstance healthAttribute = livingEntity.getAttribute(Attributes.MAX_HEALTH);
            if (healthAttribute != null) {
                UUID modifierId = UUID.nameUUIDFromBytes(("mobeditor_health_" + entityId.toString()).getBytes());

                // Удаляем все старые модификаторы MobEditor
                healthAttribute.getModifiers().forEach(mod -> {
                    if (mod.getName().contains("MobEditor")) {
                        healthAttribute.removeModifier(mod.getId());
                    }
                });

                try {
                    // Получаем текущее базовое значение здоровья
                    double currentBase = healthAttribute.getBaseValue();

                    // КРИТИЧЕСКИ ВАЖНО: Используем правильную формулу для MULTIPLY_TOTAL
                    // MULTIPLY_TOTAL работает так: final = base * (1 + multiplier1) * (1 +
                    // multiplier2) * ...
                    // Поэтому для получения нужного значения: multiplier = (target / base) - 1

                    // Если значение очень большое, используем комбинированный подход
                    if (customHealth > currentBase * 1000.0) {
                        // Для экстремально больших значений устанавливаем большое базовое значение
                        // и используем MULTIPLY_TOTAL для остатка
                        double largeBase = Math.max(1000.0, currentBase);
                        healthAttribute.setBaseValue(largeBase);

                        double multiplier = (customHealth / largeBase) - 1.0;

                        // Разбиваем множитель на части если он слишком большой
                        if (multiplier > 1000.0) {
                            double remainingMultiplier = multiplier;
                            int modifierCount = 0;

                            while (remainingMultiplier > 1000.0 && modifierCount < 20) {
                                UUID multiplyModifierId = UUID.nameUUIDFromBytes(
                                        ("mobeditor_health_mult_" + entityId.toString() + "_" + modifierCount)
                                                .getBytes());
                                AttributeModifier multiplyModifier = new AttributeModifier(
                                        multiplyModifierId,
                                        "MobEditor Health Multiply " + modifierCount,
                                        1000.0,
                                        AttributeModifier.Operation.MULTIPLY_TOTAL);
                                healthAttribute.addPermanentModifier(multiplyModifier);
                                remainingMultiplier -= 1000.0;
                                modifierCount++;
                            }

                            if (remainingMultiplier > 0.01) {
                                UUID finalMultiplyModifierId = UUID.nameUUIDFromBytes(
                                        ("mobeditor_health_mult_final_" + entityId.toString()).getBytes());
                                AttributeModifier finalMultiplyModifier = new AttributeModifier(
                                        finalMultiplyModifierId,
                                        "MobEditor Health Multiply Final",
                                        remainingMultiplier,
                                        AttributeModifier.Operation.MULTIPLY_TOTAL);
                                healthAttribute.addPermanentModifier(finalMultiplyModifier);
                            }
                        } else {
                            // Один модификатор MULTIPLY_TOTAL
                            AttributeModifier modifier = new AttributeModifier(
                                    modifierId,
                                    "MobEditor Health Multiply",
                                    multiplier,
                                    AttributeModifier.Operation.MULTIPLY_TOTAL);
                            healthAttribute.addPermanentModifier(modifier);
                        }
                    } else {
                        // Для нормальных значений используем MULTIPLY_TOTAL напрямую
                        double multiplier = (customHealth / currentBase) - 1.0;
                        AttributeModifier modifier = new AttributeModifier(
                                modifierId,
                                "MobEditor Health Multiply",
                                multiplier,
                                AttributeModifier.Operation.MULTIPLY_TOTAL);
                        healthAttribute.addPermanentModifier(modifier);
                    }

                    // Проверяем результат
                    double finalMaxHealth = healthAttribute.getValue();

                    // Если все еще не совпадает, используем ADDITION для точной корректировки
                    if (Math.abs(finalMaxHealth - customHealth) > 0.1) {
                        double difference = customHealth - finalMaxHealth;
                        UUID additionalModifierId = UUID
                                .nameUUIDFromBytes(("mobeditor_health_add_" + entityId.toString()).getBytes());
                        AttributeModifier additionalModifier = new AttributeModifier(
                                additionalModifierId,
                                "MobEditor Health Additional",
                                difference,
                                AttributeModifier.Operation.ADDITION);
                        healthAttribute.addPermanentModifier(additionalModifier);
                        finalMaxHealth = healthAttribute.getValue();
                    }

                    // Устанавливаем текущее здоровье
                    float targetHealth = (float) Math.min(finalMaxHealth, customHealth);
                    // Используем setHealth напрямую - он должен работать для любых значений
                    livingEntity.setHealth(targetHealth);

                    MobEditorMod.LOGGER.info("Установлено HP для {}: {} (базовое: {}, итоговое: {}, текущее: {})",
                            entityId, customHealth, healthAttribute.getBaseValue(),
                            healthAttribute.getValue(), livingEntity.getHealth());

                } catch (Exception e) {
                    MobEditorMod.LOGGER.error("Ошибка установки HP для {}", entityId, e);
                    // Fallback: используем простой подход
                    try {
                        double baseHealth = Math.max(healthAttribute.getBaseValue(), 1.0);
                        double multiplier = (customHealth / baseHealth) - 1.0;
                        AttributeModifier modifier = new AttributeModifier(
                                modifierId,
                                "MobEditor Health Fallback",
                                multiplier,
                                AttributeModifier.Operation.MULTIPLY_TOTAL);
                        healthAttribute.addPermanentModifier(modifier);
                        double finalHealth = healthAttribute.getValue();
                        livingEntity.setHealth((float) Math.min(finalHealth, customHealth));
                    } catch (Exception e2) {
                        MobEditorMod.LOGGER.error("Критическая ошибка установки HP для {}", entityId, e2);
                    }
                }
            }
        }

        // Применяем кастомный урон
        Double customDamage = MobEditorMod.getConfig().getMobDamage(entityId.toString());
        if (customDamage != null) {
            AttributeInstance damageAttribute = livingEntity.getAttribute(Attributes.ATTACK_DAMAGE);
            if (damageAttribute != null) {
                damageAttribute.setBaseValue(customDamage);
            }
        }
    }

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        LivingEntity entity = event.getEntity();
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());

        // Очищаем стандартный лут, если настроено
        if (MobEditorMod.getConfig().shouldClearDefaultLoot(entityId.toString())) {
            event.getDrops().clear();
        }

        List<MobConfig.LootEntry> customLoot = MobEditorMod.getConfig().getLoot(entityId.toString());

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

            ItemStack stack = new ItemStack(item, count);

            // Создаём ItemEntity и добавляем в дропы
            net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                    entity.level(),
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    stack);

            event.getDrops().add(itemEntity);
        }
    }
}
