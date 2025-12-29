package com.mobeditor.event;

import com.mobeditor.MobEditorMod;
import com.mobeditor.config.MobConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = MobEditorMod.MOD_ID)
public class ArmorSetEventHandler {

    // Кэш для отслеживания игроков с активным полётом от комплекта
    private static final Map<UUID, Boolean> flightEnabledByArmor = new HashMap<>();

    /**
     * Проверяет, носит ли игрок полный комплект брони
     */
    private static MobConfig.ArmorSetBonus getWornArmorSetBonus(Player player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);

        // Получаем ID предметов брони (пустая строка если слот пустой)
        String helmetId = helmet.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(helmet.getItem()).toString();
        String chestId = chest.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(chest.getItem()).toString();
        String legsId = legs.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(legs.getItem()).toString();
        String bootsId = boots.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(boots.getItem()).toString();

        Map<String, MobConfig.ArmorSetBonus> allSets = MobEditorMod.getConfig().getAllArmorSetBonuses();

        for (Map.Entry<String, MobConfig.ArmorSetBonus> entry : allSets.entrySet()) {
            MobConfig.ArmorSetBonus bonus = entry.getValue();
            if (bonus.isComplete(helmetId, chestId, legsId, bootsId)) {
                return bonus;
            }
        }

        return null;
    }

    /**
     * Обработка атаки — применение эффектов на цель при атаке в полном сете
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        // Проверяем, что атакующий — игрок
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }

        // Проверяем, что цель — живое существо
        LivingEntity target = event.getEntity();

        // Получаем активный комплект брони
        MobConfig.ArmorSetBonus bonus = getWornArmorSetBonus(player);
        if (bonus == null) {
            return;
        }

        // Применяем эффекты атаки к цели
        for (MobConfig.AttackEffectEntry entry : bonus.getAttackEffects()) {
            try {
                ResourceLocation effectId = new ResourceLocation(entry.getEffectId());
                MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(effectId);

                if (effect != null) {
                    MobEffectInstance effectInstance = new MobEffectInstance(
                            effect,
                            entry.getDuration(),
                            entry.getAmplifier(),
                            false, // ambient
                            true, // showParticles
                            true // showIcon
                    );
                    target.addEffect(effectInstance);
                }
            } catch (Exception e) {
                MobEditorMod.LOGGER.error("Ошибка применения эффекта атаки: {}", entry.getEffectId(), e);
            }
        }
    }

    /**
     * Каждый тик проверяем активные комплекты для полёта и пассивных эффектов
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (event.player.level().isClientSide()) {
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        UUID playerId = player.getUUID();
        MobConfig.ArmorSetBonus bonus = getWornArmorSetBonus(player);

        // Обработка полёта
        boolean shouldHaveFlight = bonus != null && bonus.isAllowFlight();
        Boolean hadFlight = flightEnabledByArmor.get(playerId);

        if (shouldHaveFlight && (hadFlight == null || !hadFlight)) {
            // Включаем полёт
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
            flightEnabledByArmor.put(playerId, true);
        } else if (!shouldHaveFlight && hadFlight != null && hadFlight) {
            // Выключаем полёт (если не в креативе/спектаторе)
            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
            flightEnabledByArmor.put(playerId, false);
        }

        // Обработка пассивных эффектов (каждые 20 тиков = 1 секунда)
        if (bonus != null && player.tickCount % 20 == 0) {
            for (MobConfig.EffectEntry entry : bonus.getEffects()) {
                try {
                    ResourceLocation effectId = new ResourceLocation(entry.getEffectId());
                    MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(effectId);

                    if (effect != null) {
                        // Применяем эффект на 2 секунды (40 тиков) — с запасом для плавности
                        MobEffectInstance effectInstance = new MobEffectInstance(
                                effect,
                                40, // 2 секунды
                                entry.getAmplifier(),
                                true, // ambient (без частиц)
                                entry.isShowParticles(),
                                entry.isShowIcon());
                        player.addEffect(effectInstance);
                    }
                } catch (Exception e) {
                    MobEditorMod.LOGGER.error("Ошибка применения пассивного эффекта: {}", entry.getEffectId(), e);
                }
            }
        }
    }

    /**
     * Очистка кэша при выходе игрока
     */
    @SubscribeEvent
    public static void onPlayerLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        flightEnabledByArmor.remove(event.getEntity().getUUID());
    }
}
