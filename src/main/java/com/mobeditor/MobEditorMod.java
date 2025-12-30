package com.mobeditor;

import com.mobeditor.command.MobEditorCommands;
import com.mobeditor.config.MobConfig;
import com.mobeditor.event.ArmorSetEventHandler;
import com.mobeditor.event.BossEventHandler;
import com.mobeditor.event.EquipmentEffectHandler;
import com.mobeditor.event.ItemEventHandler;
import com.mobeditor.event.MobEventHandler;
import com.mobeditor.loot.CustomLootModifier;
import com.mobeditor.loot.StructureLootModifier;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(MobEditorMod.MOD_ID)
public class MobEditorMod {
    public static final String MOD_ID = "mobeditor";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static MobConfig mobConfig;

    public static final DeferredRegister<com.mojang.serialization.Codec<? extends net.minecraftforge.common.loot.IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS = DeferredRegister
            .create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MOD_ID);

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister
            .create(ForgeRegistries.SOUND_EVENTS, MOD_ID);

    public static final RegistryObject<SoundEvent> BOSS_MUSIC_GENERIC = SOUND_EVENTS.register("boss_music_generic",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD_ID, "boss_music_generic")));

    static {
        LOOT_MODIFIER_SERIALIZERS.register("custom_mob_loot", CustomLootModifier.CODEC);
        LOOT_MODIFIER_SERIALIZERS.register("structure_loot", StructureLootModifier.CODEC);
    }

    public MobEditorMod() {
        LOGGER.info("Mob Editor мод загружается...");

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Регистрация сериализаторов лута
        LOOT_MODIFIER_SERIALIZERS.register(modEventBus);

        // Регистрация звуковых событий
        SOUND_EVENTS.register(modEventBus);

        // Регистрация событий мода
        modEventBus.addListener(this::commonSetup);

        // Регистрация событий Forge
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new MobEventHandler());
        MinecraftForge.EVENT_BUS.register(new ItemEventHandler());
        MinecraftForge.EVENT_BUS.register(new BossEventHandler());
        MinecraftForge.EVENT_BUS.register(new EquipmentEffectHandler());
        MinecraftForge.EVENT_BUS.register(ArmorSetEventHandler.class);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Загрузка конфигурации
        mobConfig = new MobConfig();
        mobConfig.load();

        LOGGER.info("Mob Editor мод успешно загружен!");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        MobEditorCommands.register(event.getDispatcher());
        LOGGER.info("Mob Editor команды зарегистрированы");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        if (mobConfig != null) {
            mobConfig.save();
            LOGGER.info("Mob Editor конфигурация сохранена");
        }
    }

    public static MobConfig getConfig() {
        return mobConfig;
    }
}
