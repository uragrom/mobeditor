package com.mobeditor.command;

import com.mobeditor.MobEditorMod;
import com.mobeditor.config.MobConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.ChatFormatting;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MobEditorCommands {

        private static final SuggestionProvider<CommandSourceStack> ENTITY_SUGGESTIONS = (context, builder) -> {
                return SharedSuggestionProvider.suggestResource(
                                BuiltInRegistries.ENTITY_TYPE.keySet().stream(),
                                builder);
        };

        private static final SuggestionProvider<CommandSourceStack> ITEM_SUGGESTIONS = (context, builder) -> {
                return SharedSuggestionProvider.suggestResource(
                                BuiltInRegistries.ITEM.keySet().stream(),
                                builder);
        };

        private static final SuggestionProvider<CommandSourceStack> LOOT_TABLE_SUGGESTIONS = (context, builder) -> {
                try {
                        // Используем серверный реестр для получения таблиц лута
                        var lootData = context.getSource().getServer().getLootData();
                        var lootTables = lootData.getKeys(net.minecraft.world.level.storage.loot.LootDataType.TABLE);
                        return SharedSuggestionProvider.suggestResource(lootTables.stream(), builder);
                } catch (Exception e) {
                        // Fallback: предлагаем популярные таблицы лута сундуков
                        List<String> commonLootTables = Arrays.asList(
                                        "minecraft:chests/simple_dungeon",
                                        "minecraft:chests/abandoned_mineshaft",
                                        "minecraft:chests/buried_treasure",
                                        "minecraft:chests/desert_pyramid",
                                        "minecraft:chests/end_city_treasure",
                                        "minecraft:chests/jungle_temple",
                                        "minecraft:chests/nether_bridge",
                                        "minecraft:chests/pillager_outpost",
                                        "minecraft:chests/shipwreck_treasure",
                                        "minecraft:chests/spawn_bonus_chest",
                                        "minecraft:chests/stronghold_corridor",
                                        "minecraft:chests/underwater_ruin_big",
                                        "minecraft:chests/village/village_armorer",
                                        "minecraft:chests/village/village_toolsmith",
                                        "minecraft:chests/woodland_mansion"
                        );
                        return SharedSuggestionProvider.suggest(commonLootTables.stream(), builder);
                }
        };

        public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
                dispatcher.register(
                                Commands.literal("mobeditor")
                                                .requires(source -> source.hasPermission(2))

                                                // ==================== Здоровье ====================
                                                .then(Commands.literal("health")

                                                                // /mobeditor health set <mob> <health>
                                                                .then(Commands.literal("set")
                                                                                .then(Commands.argument("mob",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ENTITY_SUGGESTIONS)
                                                                                                .then(Commands
                                                                                                                .argument("health",
                                                                                                                                DoubleArgumentType
                                                                                                                                                .doubleArg(1.0, Double.MAX_VALUE))
                                                                                                                .executes(MobEditorCommands::setHealth))))

                                                                // /mobeditor health get <mob>
                                                                .then(Commands.literal("get")
                                                                                .then(Commands.argument("mob",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ENTITY_SUGGESTIONS)
                                                                                                .executes(MobEditorCommands::getHealth)))

                                                                // /mobeditor health remove <mob>
                                                                .then(Commands.literal("remove")
                                                                                .then(Commands.argument("mob",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ENTITY_SUGGESTIONS)
                                                                                                .executes(MobEditorCommands::removeHealth)))

                                                                // /mobeditor health list
                                                                .then(Commands.literal("list")
                                                                                .executes(MobEditorCommands::listHealth)))

                                                // ==================== Урон ====================
                                                .then(Commands.literal("damage")

                                                                // /mobeditor damage set <mob> <damage>
                                                                .then(Commands.literal("set")
                                                                                .then(Commands.argument("mob",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ENTITY_SUGGESTIONS)
                                                                                                .then(Commands
                                                                                                                .argument("damage",
                                                                                                                                DoubleArgumentType
                                                                                                                                                .doubleArg(0.0, 1000.0))
                                                                                                                .executes(MobEditorCommands::setDamage))))

                                                                // /mobeditor damage get <mob>
                                                                .then(Commands.literal("get")
                                                                                .then(Commands.argument("mob",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ENTITY_SUGGESTIONS)
                                                                                                .executes(MobEditorCommands::getDamage)))

                                                                // /mobeditor damage remove <mob>
                                                                .then(Commands.literal("remove")
                                                                                .then(Commands.argument("mob",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ENTITY_SUGGESTIONS)
                                                                                                .executes(MobEditorCommands::removeDamage)))

                                                                // /mobeditor damage list
                                                                .then(Commands.literal("list")
                                                                                .executes(MobEditorCommands::listDamage)))

                                                // ==================== Лут ====================
                                                .then(Commands.literal("loot")

                                                                // /mobeditor loot add <mob> <item> <minCount>
                                                                // <maxCount> <chance>
                                                                .then(Commands.literal("add")
                                                                                .then(Commands.argument("mob",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ENTITY_SUGGESTIONS)
                                                                                                .then(Commands.argument(
                                                                                                                "item",
                                                                                                                ResourceLocationArgument
                                                                                                                                .id())
                                                                                                                .suggests(ITEM_SUGGESTIONS)
                                                                                                                .then(Commands
                                                                                                                                .argument("minCount",
                                                                                                                                                IntegerArgumentType
                                                                                                                                                                .integer(1, 64))
                                                                                                                                .then(Commands
                                                                                                                                                .argument("maxCount",
                                                                                                                                                                IntegerArgumentType
                                                                                                                                                                                .integer(1, 64))
                                                                                                                                                .then(Commands
                                                                                                                                                                .argument("chance",
                                                                                                                                                                                FloatArgumentType
                                                                                                                                                                                                .floatArg(0.0f,
                                                                                                                                                                                                                1.0f))
                                                                                                                                                                .executes(
                                                                                                                                                                                MobEditorCommands::addLoot)))))))

                                                                // /mobeditor loot remove <mob> <item>
                                                                .then(Commands.literal("remove")
                                                                                .then(Commands.argument("mob",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ENTITY_SUGGESTIONS)
                                                                                                .then(Commands.argument(
                                                                                                                "item",
                                                                                                                ResourceLocationArgument
                                                                                                                                .id())
                                                                                                                .suggests(ITEM_SUGGESTIONS)
                                                                                                                .executes(MobEditorCommands::removeLoot))))

                                                                // /mobeditor loot clear <mob>
                                                                .then(Commands.literal("clear")
                                                                                .then(Commands.argument("mob",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ENTITY_SUGGESTIONS)
                                                                                                .executes(MobEditorCommands::clearLoot)))

                                                                // /mobeditor loot list [mob]
                                                                .then(Commands.literal("list")
                                                                                .executes(MobEditorCommands::listAllLoot)
                                                                                .then(Commands.argument("mob",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ENTITY_SUGGESTIONS)
                                                                                                .executes(MobEditorCommands::listMobLoot)))

                                                                // /mobeditor loot cleardefault <mob>
                                                                .then(Commands.literal("cleardefault")
                                                                                .then(Commands.argument("mob",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ENTITY_SUGGESTIONS)
                                                                                                .executes(MobEditorCommands::clearDefaultLoot)))

                                                                // /mobeditor loot restoredefault <mob>
                                                                .then(Commands.literal("restoredefault")
                                                                                .then(Commands.argument("mob",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ENTITY_SUGGESTIONS)
                                                                                                .executes(MobEditorCommands::restoreDefaultLoot)))

                                                                // /mobeditor loot listcleared
                                                                .then(Commands.literal("listcleared")
                                                                                .executes(MobEditorCommands::listClearedLootMobs)))

                                                // ==================== Лут структур ====================
                                                .then(Commands.literal("structureloot")
                                                                // /mobeditor structureloot add <loot_table> <item> <min> <max> <chance>
                                                                .then(Commands.literal("add")
                                                                                .then(Commands.argument("loot_table",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(LOOT_TABLE_SUGGESTIONS)
                                                                                                .then(Commands.argument(
                                                                                                                "item",
                                                                                                                ResourceLocationArgument
                                                                                                                                .id())
                                                                                                                .suggests(ITEM_SUGGESTIONS)
                                                                                                                .then(Commands
                                                                                                                                .argument("minCount",
                                                                                                                                                IntegerArgumentType
                                                                                                                                                                .integer(1, 64))
                                                                                                                                .then(Commands
                                                                                                                                                .argument("maxCount",
                                                                                                                                                                IntegerArgumentType
                                                                                                                                                                                .integer(1, 64))
                                                                                                                                                .then(Commands
                                                                                                                                                                .argument("chance",
                                                                                                                                                                                FloatArgumentType
                                                                                                                                                                                                .floatArg(0.0f,
                                                                                                                                                                                                                1.0f))
                                                                                                                                                                .executes(MobEditorCommands::addStructureLoot)))))))

                                                                // /mobeditor structureloot remove <loot_table> <item>
                                                                .then(Commands.literal("remove")
                                                                                .then(Commands.argument("loot_table",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(LOOT_TABLE_SUGGESTIONS)
                                                                                                .then(Commands.argument(
                                                                                                                "item",
                                                                                                                ResourceLocationArgument
                                                                                                                                .id())
                                                                                                                .suggests(ITEM_SUGGESTIONS)
                                                                                                                .executes(MobEditorCommands::removeStructureLoot))))

                                                                // /mobeditor structureloot clear <loot_table>
                                                                .then(Commands.literal("clear")
                                                                                .then(Commands.argument("loot_table",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(LOOT_TABLE_SUGGESTIONS)
                                                                                                .executes(MobEditorCommands::clearStructureLoot)))

                                                                // /mobeditor structureloot list [loot_table]
                                                                .then(Commands.literal("list")
                                                                                .executes(MobEditorCommands::listAllStructureLoot)
                                                                                .then(Commands.argument("loot_table",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(LOOT_TABLE_SUGGESTIONS)
                                                                                                .executes(MobEditorCommands::listStructureLoot))))

                                                // ==================== Предметы ====================
                                                .then(Commands.literal("item")

                                                                // /mobeditor item damage <item> <value>
                                                                .then(Commands.literal("damage")
                                                                                .then(Commands.argument("item",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ITEM_SUGGESTIONS)
                                                                                                .then(Commands.argument(
                                                                                                                "value",
                                                                                                                DoubleArgumentType
                                                                                                                                .doubleArg(0.0, 1000.0))
                                                                                                                .executes(MobEditorCommands::setItemDamage))))

                                                                // /mobeditor item speed <item> <value>
                                                                .then(Commands.literal("speed")
                                                                                .then(Commands.argument("item",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ITEM_SUGGESTIONS)
                                                                                                .then(Commands.argument(
                                                                                                                "value",
                                                                                                                DoubleArgumentType
                                                                                                                                .doubleArg(0.0, 10.0))
                                                                                                                .executes(MobEditorCommands::setItemSpeed))))

                                                                // /mobeditor item durability <item> <value>
                                                                .then(Commands.literal("durability")
                                                                                .then(Commands.argument("item",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ITEM_SUGGESTIONS)
                                                                                                .then(Commands.argument(
                                                                                                                "value",
                                                                                                                IntegerArgumentType
                                                                                                                                .integer(1, 100000))
                                                                                                                .executes(MobEditorCommands::setItemDurability))))

                                                                // /mobeditor item armor <item> <value>
                                                                .then(Commands.literal("armor")
                                                                                .then(Commands.argument("item",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ITEM_SUGGESTIONS)
                                                                                                .then(Commands.argument(
                                                                                                                "value",
                                                                                                                DoubleArgumentType
                                                                                                                                .doubleArg(0.0, 100.0))
                                                                                                                .executes(MobEditorCommands::setItemArmor))))

                                                                // /mobeditor item toughness <item> <value>
                                                                .then(Commands.literal("toughness")
                                                                                .then(Commands.argument("item",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ITEM_SUGGESTIONS)
                                                                                                .then(Commands.argument(
                                                                                                                "value",
                                                                                                                DoubleArgumentType
                                                                                                                                .doubleArg(0.0, 100.0))
                                                                                                                .executes(MobEditorCommands::setItemToughness))))

                                                                // /mobeditor item knockback <item> <value>
                                                                .then(Commands.literal("knockback")
                                                                                .then(Commands.argument("item",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ITEM_SUGGESTIONS)
                                                                                                .then(Commands.argument(
                                                                                                                "value",
                                                                                                                DoubleArgumentType
                                                                                                                                .doubleArg(0.0, 1.0))
                                                                                                                .executes(MobEditorCommands::setItemKnockback))))

                                                                // /mobeditor item movespeed <item> <value>
                                                                .then(Commands.literal("movespeed")
                                                                                .then(Commands.argument("item",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ITEM_SUGGESTIONS)
                                                                                                .then(Commands.argument(
                                                                                                                "value",
                                                                                                                DoubleArgumentType
                                                                                                                                .doubleArg(-1.0, 5.0))
                                                                                                                .executes(MobEditorCommands::setItemMoveSpeed))))

                                                                // /mobeditor item get <item>
                                                                .then(Commands.literal("get")
                                                                                .then(Commands.argument("item",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ITEM_SUGGESTIONS)
                                                                                                .executes(MobEditorCommands::getItemStats)))

                                                                // /mobeditor item remove <item>
                                                                .then(Commands.literal("remove")
                                                                                .then(Commands.argument("item",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ITEM_SUGGESTIONS)
                                                                                                .executes(MobEditorCommands::removeItemStats)))

                                                                // /mobeditor item list
                                                                .then(Commands.literal("list")
                                                                                .executes(MobEditorCommands::listItemStats)))

                                                // ==================== Боссы ====================
                                                .then(Commands.literal("boss")

                                                                // /mobeditor boss set <mob> <name>
                                                                .then(Commands.literal("set")
                                                                                .then(Commands.argument("mob",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ENTITY_SUGGESTIONS)
                                                                                                .then(Commands.argument(
                                                                                                                "name",
                                                                                                                StringArgumentType
                                                                                                                                .greedyString())
                                                                                                                .executes(MobEditorCommands::setBoss))))

                                                                // /mobeditor boss color <mob> <color>
                                                                .then(Commands.literal("color")
                                                                                .then(Commands.argument("mob",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ENTITY_SUGGESTIONS)
                                                                                                .then(Commands.argument(
                                                                                                                "color",
                                                                                                                StringArgumentType
                                                                                                                                .word())
                                                                                                                .suggests(MobEditorCommands::suggestBossColors)
                                                                                                                .executes(MobEditorCommands::setBossColor))))

                                                                // /mobeditor boss style <mob> <style>
                                                                .then(Commands.literal("style")
                                                                                .then(Commands.argument("mob",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ENTITY_SUGGESTIONS)
                                                                                                .then(Commands.argument(
                                                                                                                "style",
                                                                                                                StringArgumentType
                                                                                                                                .word())
                                                                                                                .suggests(MobEditorCommands::suggestBossStyles)
                                                                                                                .executes(MobEditorCommands::setBossStyle))))

                                                                // /mobeditor boss music <mob> <filename>
                                                                .then(Commands.literal("music")
                                                                                .then(Commands.argument("mob",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ENTITY_SUGGESTIONS)
                                                                                                .then(Commands.argument(
                                                                                                                "filename",
                                                                                                                StringArgumentType
                                                                                                                                .word())
                                                                                                                .suggests(MobEditorCommands::suggestMusicFiles)
                                                                                                                .executes(MobEditorCommands::setBossMusic))))

                                                                // /mobeditor boss range <mob> <range>
                                                                .then(Commands.literal("range")
                                                                                .then(Commands.argument("mob",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ENTITY_SUGGESTIONS)
                                                                                                .then(Commands.argument(
                                                                                                                "range",
                                                                                                                IntegerArgumentType
                                                                                                                                .integer(10, 200))
                                                                                                                .executes(MobEditorCommands::setBossRange))))

                                                                // /mobeditor boss darken <mob> <true/false>
                                                                .then(Commands.literal("darken")
                                                                                .then(Commands.argument("mob",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ENTITY_SUGGESTIONS)
                                                                                                .then(Commands.argument(
                                                                                                                "enabled",
                                                                                                                BoolArgumentType.bool())
                                                                                                                .executes(MobEditorCommands::setBossDarken))))

                                                                // /mobeditor boss fog <mob> <true/false>
                                                                .then(Commands.literal("fog")
                                                                                .then(Commands.argument("mob",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ENTITY_SUGGESTIONS)
                                                                                                .then(Commands.argument(
                                                                                                                "enabled",
                                                                                                                BoolArgumentType.bool())
                                                                                                                .executes(MobEditorCommands::setBossFog))))

                                                                // /mobeditor boss get <mob>
                                                                .then(Commands.literal("get")
                                                                                .then(Commands.argument("mob",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ENTITY_SUGGESTIONS)
                                                                                                .executes(MobEditorCommands::getBossSettings)))

                                                                // /mobeditor boss remove <mob>
                                                                .then(Commands.literal("remove")
                                                                                .then(Commands.argument("mob",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ENTITY_SUGGESTIONS)
                                                                                                .executes(MobEditorCommands::removeBoss)))

                                                                // /mobeditor boss list
                                                                .then(Commands.literal("list")
                                                                                .executes(MobEditorCommands::listBosses))

                                                                // /mobeditor boss musiclist
                                                                .then(Commands.literal("musiclist")
                                                                                .executes(MobEditorCommands::listMusic)))

                                                // ==================== Эффекты ====================
                                                .then(Commands.literal("effect")

                                                                // /mobeditor effect add <item> <effect> <level>
                                                                .then(Commands.literal("add")
                                                                                .then(Commands.argument("item",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ITEM_SUGGESTIONS)
                                                                                                .then(Commands.argument(
                                                                                                                "effect",
                                                                                                                ResourceLocationArgument
                                                                                                                                .id())
                                                                                                                .suggests(MobEditorCommands::suggestEffects)
                                                                                                                .then(Commands.argument(
                                                                                                                                "level",
                                                                                                                                IntegerArgumentType
                                                                                                                                                .integer(1, 255))
                                                                                                                                .executes(MobEditorCommands::addItemEffect)))))

                                                                // /mobeditor effect remove <item> <effect>
                                                                .then(Commands.literal("remove")
                                                                                .then(Commands.argument("item",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ITEM_SUGGESTIONS)
                                                                                                .then(Commands.argument(
                                                                                                                "effect",
                                                                                                                ResourceLocationArgument
                                                                                                                                .id())
                                                                                                                .suggests(MobEditorCommands::suggestEffects)
                                                                                                                .executes(MobEditorCommands::removeItemEffect))))

                                                                // /mobeditor effect clear <item>
                                                                .then(Commands.literal("clear")
                                                                                .then(Commands.argument("item",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ITEM_SUGGESTIONS)
                                                                                                .executes(MobEditorCommands::clearItemEffects)))

                                                                // /mobeditor effect get <item>
                                                                .then(Commands.literal("get")
                                                                                .then(Commands.argument("item",
                                                                                                ResourceLocationArgument
                                                                                                                .id())
                                                                                                .suggests(ITEM_SUGGESTIONS)
                                                                                                .executes(MobEditorCommands::getItemEffects)))

                                                                // /mobeditor effect list
                                                                .then(Commands.literal("list")
                                                                                .executes(MobEditorCommands::listAllEffects)))

                                                // ==================== Комплекты брони ====================
                                                .then(Commands.literal("armorset")

                                                                // /mobeditor armorset create <name> <helmet> <chest>
                                                                // <legs> <boots>
                                                                .then(Commands.literal("create")
                                                                                .then(Commands.argument("name",
                                                                                                StringArgumentType
                                                                                                                .word())
                                                                                                .then(Commands.argument(
                                                                                                                "helmet",
                                                                                                                ResourceLocationArgument
                                                                                                                                .id())
                                                                                                                .suggests(ITEM_SUGGESTIONS)
                                                                                                                .then(Commands.argument(
                                                                                                                                "chest",
                                                                                                                                ResourceLocationArgument
                                                                                                                                                .id())
                                                                                                                                .suggests(ITEM_SUGGESTIONS)
                                                                                                                                .then(Commands.argument(
                                                                                                                                                "legs",
                                                                                                                                                ResourceLocationArgument
                                                                                                                                                                .id())
                                                                                                                                                .suggests(ITEM_SUGGESTIONS)
                                                                                                                                                .then(Commands.argument(
                                                                                                                                                                "boots",
                                                                                                                                                                ResourceLocationArgument
                                                                                                                                                                                .id())
                                                                                                                                                                .suggests(ITEM_SUGGESTIONS)
                                                                                                                                                                .executes(MobEditorCommands::createArmorSet)))))))

                                                                // /mobeditor armorset addeffect <name> <effect> <level>
                                                                .then(Commands.literal("addeffect")
                                                                                .then(Commands.argument("name",
                                                                                                StringArgumentType
                                                                                                                .word())
                                                                                                .suggests(MobEditorCommands::suggestArmorSets)
                                                                                                .then(Commands.argument(
                                                                                                                "effect",
                                                                                                                ResourceLocationArgument
                                                                                                                                .id())
                                                                                                                .suggests(MobEditorCommands::suggestEffects)
                                                                                                                .then(Commands.argument(
                                                                                                                                "level",
                                                                                                                                IntegerArgumentType
                                                                                                                                                .integer(1, 255))
                                                                                                                                .executes(MobEditorCommands::addArmorSetEffect)))))

                                                                // /mobeditor armorset get <name>
                                                                .then(Commands.literal("get")
                                                                                .then(Commands.argument("name",
                                                                                                StringArgumentType
                                                                                                                .word())
                                                                                                .suggests(MobEditorCommands::suggestArmorSets)
                                                                                                .executes(MobEditorCommands::getArmorSet)))

                                                                // /mobeditor armorset remove <name>
                                                                .then(Commands.literal("remove")
                                                                                .then(Commands.argument("name",
                                                                                                StringArgumentType
                                                                                                                .word())
                                                                                                .suggests(MobEditorCommands::suggestArmorSets)
                                                                                                .executes(MobEditorCommands::removeArmorSet)))

                                                                // /mobeditor armorset attackeffect <name> <effect>
                                                                // <level> <duration>
                                                                .then(Commands.literal("attackeffect")
                                                                                .then(Commands.argument("name",
                                                                                                StringArgumentType
                                                                                                                .word())
                                                                                                .suggests(MobEditorCommands::suggestArmorSets)
                                                                                                .then(Commands.argument(
                                                                                                                "effect",
                                                                                                                ResourceLocationArgument
                                                                                                                                .id())
                                                                                                                .suggests(MobEditorCommands::suggestEffects)
                                                                                                                .then(Commands.argument(
                                                                                                                                "level",
                                                                                                                                IntegerArgumentType
                                                                                                                                                .integer(1, 255))
                                                                                                                                .then(Commands.argument(
                                                                                                                                                "duration",
                                                                                                                                                IntegerArgumentType
                                                                                                                                                                .integer(1, 6000))
                                                                                                                                                .executes(MobEditorCommands::addArmorSetAttackEffect))))))

                                                                // /mobeditor armorset flight <name> <true/false>
                                                                .then(Commands.literal("flight")
                                                                                .then(Commands.argument("name",
                                                                                                StringArgumentType
                                                                                                                .word())
                                                                                                .suggests(MobEditorCommands::suggestArmorSets)
                                                                                                .then(Commands.argument(
                                                                                                                "enabled",
                                                                                                                BoolArgumentType.bool())
                                                                                                                .executes(MobEditorCommands::setArmorSetFlight))))

                                                                // /mobeditor armorset list
                                                                .then(Commands.literal("list")
                                                                                .executes(MobEditorCommands::listArmorSets)))

                                                // ==================== Достижения ====================
                                                .then(Commands.literal("advancement")
                                                                // /mobeditor advancement add <id> <triggerType>
                                                                // <triggerTarget> <title>
                                                                .then(Commands.literal("add")
                                                                                .then(Commands.argument("id",
                                                                                                StringArgumentType
                                                                                                                .word())
                                                                                                .then(Commands.argument(
                                                                                                                "triggerType",
                                                                                                                StringArgumentType
                                                                                                                                .word())
                                                                                                                .suggests((context,
                                                                                                                                builder) -> {
                                                                                                                        return builder.suggest(
                                                                                                                                        "kill_mob")
                                                                                                                                        .suggest("obtain_item")
                                                                                                                                        .buildFuture();
                                                                                                                })
                                                                                                                .then(Commands.argument(
                                                                                                                                "triggerTarget",
                                                                                                                                ResourceLocationArgument
                                                                                                                                                .id())
                                                                                                                                .suggests((context,
                                                                                                                                                builder) -> {
                                                                                                                                        String type = StringArgumentType
                                                                                                                                                        .getString(context,
                                                                                                                                                                        "triggerType");
                                                                                                                                        if ("kill_mob".equals(
                                                                                                                                                        type)) {
                                                                                                                                                return ENTITY_SUGGESTIONS
                                                                                                                                                                .getSuggestions(context,
                                                                                                                                                                                builder);
                                                                                                                                        } else {
                                                                                                                                                return ITEM_SUGGESTIONS
                                                                                                                                                                .getSuggestions(context,
                                                                                                                                                                                builder);
                                                                                                                                        }
                                                                                                                                })
                                                                                                                                .then(Commands.argument(
                                                                                                                                                "title",
                                                                                                                                                StringArgumentType
                                                                                                                                                                .greedyString())
                                                                                                                                                .executes(MobEditorCommands::addAdvancement))))))
                                                                // /mobeditor advancement remove <id>
                                                                .then(Commands.literal("remove")
                                                                                .then(Commands.argument("id",
                                                                                                StringArgumentType
                                                                                                                .word())
                                                                                                .suggests(MobEditorCommands::suggestAdvancements)
                                                                                                .executes(MobEditorCommands::removeAdvancement)))

                                                                // /mobeditor advancement setdesc <id> <description>
                                                                .then(Commands.literal("setdesc")
                                                                                .then(Commands.argument("id",
                                                                                                StringArgumentType
                                                                                                                .word())
                                                                                                .suggests(MobEditorCommands::suggestAdvancements)
                                                                                                .then(Commands.argument(
                                                                                                                "description",
                                                                                                                StringArgumentType
                                                                                                                                .greedyString())
                                                                                                                .executes(MobEditorCommands::setAdvancementDescription))))

                                                                // /mobeditor advancement get <id>
                                                                .then(Commands.literal("get")
                                                                                .then(Commands.argument("id",
                                                                                                StringArgumentType
                                                                                                                .word())
                                                                                                .suggests(MobEditorCommands::suggestAdvancements)
                                                                                                .executes(MobEditorCommands::getAdvancement)))

                                                                // /mobeditor advancement list
                                                                .then(Commands.literal("list")
                                                                                .executes(MobEditorCommands::listAdvancements)))

                                                // /mobeditor reload
                                                .then(Commands.literal("reload")
                                                                .executes(MobEditorCommands::reload))

                                                // /mobeditor save
                                                .then(Commands.literal("save")
                                                                .executes(MobEditorCommands::save)));
        }

        // ==================== Здоровье - Команды ====================

        private static int setHealth(CommandContext<CommandSourceStack> context) {
                ResourceLocation mobId = ResourceLocationArgument.getId(context, "mob");
                double health = DoubleArgumentType.getDouble(context, "health");

                if (!BuiltInRegistries.ENTITY_TYPE.containsKey(mobId)) {
                        context.getSource().sendFailure(Component.literal("Моб не найден: " + mobId));
                        return 0;
                }

                MobEditorMod.getConfig().setMobHealth(mobId.toString(), health);

                context.getSource().sendSuccess(() -> Component.literal("Здоровье ")
                                .append(Component.literal(mobId.toString()).withStyle(ChatFormatting.GOLD))
                                .append(" установлено на ")
                                .append(Component.literal(String.format("%.1f", health))
                                                .withStyle(ChatFormatting.GREEN))
                                .append(" ❤"), true);

                return 1;
        }

        private static int getHealth(CommandContext<CommandSourceStack> context) {
                ResourceLocation mobId = ResourceLocationArgument.getId(context, "mob");

                Double health = MobEditorMod.getConfig().getMobHealth(mobId.toString());

                if (health == null) {
                        context.getSource().sendSuccess(() -> Component.literal("Для ")
                                        .append(Component.literal(mobId.toString()).withStyle(ChatFormatting.GOLD))
                                        .append(" не установлено кастомное здоровье"), false);
                } else {
                        context.getSource().sendSuccess(() -> Component.literal("Здоровье ")
                                        .append(Component.literal(mobId.toString()).withStyle(ChatFormatting.GOLD))
                                        .append(": ")
                                        .append(Component.literal(String.format("%.1f", health))
                                                        .withStyle(ChatFormatting.GREEN))
                                        .append(" ❤"), false);
                }

                return 1;
        }

        private static int removeHealth(CommandContext<CommandSourceStack> context) {
                ResourceLocation mobId = ResourceLocationArgument.getId(context, "mob");

                MobEditorMod.getConfig().removeMobHealth(mobId.toString());

                context.getSource().sendSuccess(() -> Component.literal("Кастомное здоровье для ")
                                .append(Component.literal(mobId.toString()).withStyle(ChatFormatting.GOLD))
                                .append(" удалено"), true);

                return 1;
        }

        private static int listHealth(CommandContext<CommandSourceStack> context) {
                Map<String, Double> healthMap = MobEditorMod.getConfig().getAllMobHealth();

                if (healthMap.isEmpty()) {
                        context.getSource().sendSuccess(() -> Component.literal("Нет настроек здоровья мобов")
                                        .withStyle(ChatFormatting.YELLOW), false);
                        return 0;
                }

                context.getSource().sendSuccess(() -> Component.literal("=== Настройки здоровья мобов ===")
                                .withStyle(ChatFormatting.AQUA), false);

                healthMap.forEach((mob, health) -> {
                        context.getSource().sendSuccess(() -> Component.literal("• ")
                                        .append(Component.literal(mob).withStyle(ChatFormatting.GOLD))
                                        .append(": ")
                                        .append(Component.literal(String.format("%.1f", health))
                                                        .withStyle(ChatFormatting.GREEN))
                                        .append(" ❤"), false);
                });

                return 1;
        }

        // ==================== Урон - Команды ====================

        private static int setDamage(CommandContext<CommandSourceStack> context) {
                ResourceLocation mobId = ResourceLocationArgument.getId(context, "mob");
                double damage = DoubleArgumentType.getDouble(context, "damage");

                if (!BuiltInRegistries.ENTITY_TYPE.containsKey(mobId)) {
                        context.getSource().sendFailure(Component.literal("Моб не найден: " + mobId));
                        return 0;
                }

                MobEditorMod.getConfig().setMobDamage(mobId.toString(), damage);

                context.getSource().sendSuccess(() -> Component.literal("Урон ")
                                .append(Component.literal(mobId.toString()).withStyle(ChatFormatting.GOLD))
                                .append(" установлен на ")
                                .append(Component.literal(String.format("%.1f", damage)).withStyle(ChatFormatting.RED))
                                .append(" ⚔"), true);

                return 1;
        }

        private static int getDamage(CommandContext<CommandSourceStack> context) {
                ResourceLocation mobId = ResourceLocationArgument.getId(context, "mob");

                Double damage = MobEditorMod.getConfig().getMobDamage(mobId.toString());

                if (damage == null) {
                        context.getSource().sendSuccess(() -> Component.literal("Для ")
                                        .append(Component.literal(mobId.toString()).withStyle(ChatFormatting.GOLD))
                                        .append(" не установлен кастомный урон"), false);
                } else {
                        context.getSource().sendSuccess(() -> Component.literal("Урон ")
                                        .append(Component.literal(mobId.toString()).withStyle(ChatFormatting.GOLD))
                                        .append(": ")
                                        .append(Component.literal(String.format("%.1f", damage))
                                                        .withStyle(ChatFormatting.RED))
                                        .append(" ⚔"), false);
                }

                return 1;
        }

        private static int removeDamage(CommandContext<CommandSourceStack> context) {
                ResourceLocation mobId = ResourceLocationArgument.getId(context, "mob");

                MobEditorMod.getConfig().removeMobDamage(mobId.toString());

                context.getSource().sendSuccess(() -> Component.literal("Кастомный урон для ")
                                .append(Component.literal(mobId.toString()).withStyle(ChatFormatting.GOLD))
                                .append(" удалён"), true);

                return 1;
        }

        private static int listDamage(CommandContext<CommandSourceStack> context) {
                Map<String, Double> damageMap = MobEditorMod.getConfig().getAllMobDamage();

                if (damageMap.isEmpty()) {
                        context.getSource().sendSuccess(() -> Component.literal("Нет настроек урона мобов")
                                        .withStyle(ChatFormatting.YELLOW), false);
                        return 0;
                }

                context.getSource().sendSuccess(() -> Component.literal("=== Настройки урона мобов ===")
                                .withStyle(ChatFormatting.AQUA), false);

                damageMap.forEach((mob, damage) -> {
                        context.getSource().sendSuccess(() -> Component.literal("• ")
                                        .append(Component.literal(mob).withStyle(ChatFormatting.GOLD))
                                        .append(": ")
                                        .append(Component.literal(String.format("%.1f", damage))
                                                        .withStyle(ChatFormatting.RED))
                                        .append(" ⚔"), false);
                });

                return 1;
        }

        // ==================== Лут - Команды ====================

        private static int addLoot(CommandContext<CommandSourceStack> context) {
                ResourceLocation mobId = ResourceLocationArgument.getId(context, "mob");
                ResourceLocation itemId = ResourceLocationArgument.getId(context, "item");
                int minCount = IntegerArgumentType.getInteger(context, "minCount");
                int maxCount = IntegerArgumentType.getInteger(context, "maxCount");
                float chance = FloatArgumentType.getFloat(context, "chance");

                if (!BuiltInRegistries.ENTITY_TYPE.containsKey(mobId)) {
                        context.getSource().sendFailure(Component.literal("Моб не найден: " + mobId));
                        return 0;
                }

                if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
                        context.getSource().sendFailure(Component.literal("Предмет не найден: " + itemId));
                        return 0;
                }

                if (minCount > maxCount) {
                        context.getSource().sendFailure(Component.literal("minCount не может быть больше maxCount"));
                        return 0;
                }

                MobConfig.LootEntry entry = new MobConfig.LootEntry(itemId.toString(), minCount, maxCount, chance);
                MobEditorMod.getConfig().addLoot(mobId.toString(), entry);

                context.getSource().sendSuccess(() -> Component.literal("Добавлен лут для ")
                                .append(Component.literal(mobId.toString()).withStyle(ChatFormatting.GOLD))
                                .append(": ")
                                .append(Component.literal(itemId.toString()).withStyle(ChatFormatting.YELLOW))
                                .append(String.format(" x%d-%d (%.0f%%)", minCount, maxCount, chance * 100)), true);

                return 1;
        }

        private static int removeLoot(CommandContext<CommandSourceStack> context) {
                ResourceLocation mobId = ResourceLocationArgument.getId(context, "mob");
                ResourceLocation itemId = ResourceLocationArgument.getId(context, "item");

                MobEditorMod.getConfig().removeLoot(mobId.toString(), itemId.toString());

                context.getSource().sendSuccess(() -> Component.literal("Удалён лут ")
                                .append(Component.literal(itemId.toString()).withStyle(ChatFormatting.YELLOW))
                                .append(" для ")
                                .append(Component.literal(mobId.toString()).withStyle(ChatFormatting.GOLD)), true);

                return 1;
        }

        private static int clearLoot(CommandContext<CommandSourceStack> context) {
                ResourceLocation mobId = ResourceLocationArgument.getId(context, "mob");

                MobEditorMod.getConfig().clearLoot(mobId.toString());

                context.getSource().sendSuccess(() -> Component.literal("Весь кастомный лут для ")
                                .append(Component.literal(mobId.toString()).withStyle(ChatFormatting.GOLD))
                                .append(" удалён"), true);

                return 1;
        }

        private static int listMobLoot(CommandContext<CommandSourceStack> context) {
                ResourceLocation mobId = ResourceLocationArgument.getId(context, "mob");
                List<MobConfig.LootEntry> lootList = MobEditorMod.getConfig().getLoot(mobId.toString());

                if (lootList.isEmpty()) {
                        context.getSource().sendSuccess(() -> Component.literal("Нет кастомного лута для ")
                                        .append(Component.literal(mobId.toString()).withStyle(ChatFormatting.GOLD)),
                                        false);
                        return 0;
                }

                context.getSource().sendSuccess(() -> Component.literal("=== Лут для " + mobId + " ===")
                                .withStyle(ChatFormatting.AQUA), false);

                for (MobConfig.LootEntry entry : lootList) {
                        context.getSource().sendSuccess(() -> Component.literal("• ")
                                        .append(Component.literal(entry.getItemId()).withStyle(ChatFormatting.YELLOW))
                                        .append(String.format(" x%d-%d (%.0f%%)",
                                                        entry.getMinCount(), entry.getMaxCount(),
                                                        entry.getChance() * 100)),
                                        false);
                }

                return 1;
        }

        private static int listAllLoot(CommandContext<CommandSourceStack> context) {
                Map<String, List<MobConfig.LootEntry>> lootMap = MobEditorMod.getConfig().getAllLoot();

                if (lootMap.isEmpty()) {
                        context.getSource().sendSuccess(() -> Component.literal("Нет настроек кастомного лута")
                                        .withStyle(ChatFormatting.YELLOW), false);
                        return 0;
                }

                context.getSource().sendSuccess(() -> Component.literal("=== Все настройки лута ===")
                                .withStyle(ChatFormatting.AQUA), false);

                lootMap.forEach((mob, entries) -> {
                        context.getSource().sendSuccess(() -> Component.literal(mob + ":")
                                        .withStyle(ChatFormatting.GOLD), false);
                        for (MobConfig.LootEntry entry : entries) {
                                context.getSource().sendSuccess(() -> Component.literal("  • ")
                                                .append(Component.literal(entry.getItemId())
                                                                .withStyle(ChatFormatting.YELLOW))
                                                .append(String.format(" x%d-%d (%.0f%%)",
                                                                entry.getMinCount(), entry.getMaxCount(),
                                                                entry.getChance() * 100)),
                                                false);
                        }
                });

                return 1;
        }

        private static int clearDefaultLoot(CommandContext<CommandSourceStack> context) {
                ResourceLocation mobId = ResourceLocationArgument.getId(context, "mob");

                if (!BuiltInRegistries.ENTITY_TYPE.containsKey(mobId)) {
                        context.getSource().sendFailure(Component.literal("Моб не найден: " + mobId));
                        return 0;
                }

                MobEditorMod.getConfig().setClearDefaultLoot(mobId.toString(), true);

                context.getSource().sendSuccess(() -> Component.literal("Стандартный лут для ")
                                .append(Component.literal(mobId.toString()).withStyle(ChatFormatting.GOLD))
                                .append(" отключён. Теперь будет падать только кастомный лут."), true);

                return 1;
        }

        private static int restoreDefaultLoot(CommandContext<CommandSourceStack> context) {
                ResourceLocation mobId = ResourceLocationArgument.getId(context, "mob");

                MobEditorMod.getConfig().setClearDefaultLoot(mobId.toString(), false);

                context.getSource().sendSuccess(() -> Component.literal("Стандартный лут для ")
                                .append(Component.literal(mobId.toString()).withStyle(ChatFormatting.GOLD))
                                .append(" восстановлен."), true);

                return 1;
        }

        private static int listClearedLootMobs(CommandContext<CommandSourceStack> context) {
                java.util.Set<String> mobs = MobEditorMod.getConfig().getAllClearDefaultLootMobs();

                if (mobs.isEmpty()) {
                        context.getSource()
                                        .sendSuccess(() -> Component
                                                        .literal("Нет мобов с отключённым стандартным лутом")
                                                        .withStyle(ChatFormatting.YELLOW), false);
                        return 0;
                }

                context.getSource().sendSuccess(() -> Component.literal("=== Мобы с отключённым стандартным лутом ===")
                                .withStyle(ChatFormatting.RED), false);

                for (String mob : mobs) {
                        context.getSource().sendSuccess(() -> Component.literal("• ")
                                        .append(Component.literal(mob).withStyle(ChatFormatting.GOLD)), false);
                }

                return 1;
        }

        // ==================== Лут структур - Команды ====================

        private static int addStructureLoot(CommandContext<CommandSourceStack> context) {
                ResourceLocation lootTableId = ResourceLocationArgument.getId(context, "loot_table");
                ResourceLocation itemId = ResourceLocationArgument.getId(context, "item");
                int minCount = IntegerArgumentType.getInteger(context, "minCount");
                int maxCount = IntegerArgumentType.getInteger(context, "maxCount");
                float chance = FloatArgumentType.getFloat(context, "chance");

                if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
                        context.getSource().sendFailure(Component.literal("Предмет не найден: " + itemId));
                        return 0;
                }

                if (minCount > maxCount) {
                        context.getSource().sendFailure(Component.literal("minCount не может быть больше maxCount"));
                        return 0;
                }

                MobConfig.LootEntry entry = new MobConfig.LootEntry(itemId.toString(), minCount, maxCount, chance);
                MobEditorMod.getConfig().addStructureLoot(lootTableId.toString(), entry);

                context.getSource().sendSuccess(() -> Component.literal("Добавлен лут для структуры ")
                                .append(Component.literal(lootTableId.toString()).withStyle(ChatFormatting.GOLD))
                                .append(": ")
                                .append(Component.literal(itemId.toString()).withStyle(ChatFormatting.YELLOW))
                                .append(String.format(" x%d-%d (%.0f%%)", minCount, maxCount, chance * 100)), true);

                return 1;
        }

        private static int removeStructureLoot(CommandContext<CommandSourceStack> context) {
                ResourceLocation lootTableId = ResourceLocationArgument.getId(context, "loot_table");
                ResourceLocation itemId = ResourceLocationArgument.getId(context, "item");

                MobEditorMod.getConfig().removeStructureLoot(lootTableId.toString(), itemId.toString());

                context.getSource().sendSuccess(() -> Component.literal("Удалён лут ")
                                .append(Component.literal(itemId.toString()).withStyle(ChatFormatting.YELLOW))
                                .append(" для структуры ")
                                .append(Component.literal(lootTableId.toString()).withStyle(ChatFormatting.GOLD)), true);

                return 1;
        }

        private static int clearStructureLoot(CommandContext<CommandSourceStack> context) {
                ResourceLocation lootTableId = ResourceLocationArgument.getId(context, "loot_table");

                MobEditorMod.getConfig().clearStructureLoot(lootTableId.toString());

                context.getSource().sendSuccess(() -> Component.literal("Весь кастомный лут для структуры ")
                                .append(Component.literal(lootTableId.toString()).withStyle(ChatFormatting.GOLD))
                                .append(" удалён"), true);

                return 1;
        }

        private static int listStructureLoot(CommandContext<CommandSourceStack> context) {
                ResourceLocation lootTableId = ResourceLocationArgument.getId(context, "loot_table");
                List<MobConfig.LootEntry> lootList = MobEditorMod.getConfig().getStructureLoot(lootTableId.toString());

                if (lootList.isEmpty()) {
                        context.getSource().sendSuccess(() -> Component.literal("Нет кастомного лута для структуры ")
                                        .append(Component.literal(lootTableId.toString()).withStyle(ChatFormatting.GOLD)),
                                        false);
                        return 0;
                }

                context.getSource().sendSuccess(() -> Component.literal("=== Лут для структуры " + lootTableId + " ===")
                                .withStyle(ChatFormatting.AQUA), false);

                for (MobConfig.LootEntry entry : lootList) {
                        context.getSource().sendSuccess(() -> Component.literal("• ")
                                        .append(Component.literal(entry.getItemId()).withStyle(ChatFormatting.YELLOW))
                                        .append(String.format(" x%d-%d (%.0f%%)",
                                                        entry.getMinCount(), entry.getMaxCount(),
                                                        entry.getChance() * 100)),
                                        false);
                }

                return 1;
        }

        private static int listAllStructureLoot(CommandContext<CommandSourceStack> context) {
                Map<String, List<MobConfig.LootEntry>> lootMap = MobEditorMod.getConfig().getAllStructureLoot();

                if (lootMap.isEmpty()) {
                        context.getSource().sendSuccess(() -> Component.literal("Нет настроек кастомного лута структур")
                                        .withStyle(ChatFormatting.YELLOW), false);
                        return 0;
                }

                context.getSource().sendSuccess(() -> Component.literal("=== Все настройки лута структур ===")
                                .withStyle(ChatFormatting.AQUA), false);

                lootMap.forEach((lootTable, entries) -> {
                        context.getSource().sendSuccess(() -> Component.literal(lootTable + ":")
                                        .withStyle(ChatFormatting.GOLD), false);
                        for (MobConfig.LootEntry entry : entries) {
                                context.getSource().sendSuccess(() -> Component.literal("  • ")
                                                .append(Component.literal(entry.getItemId())
                                                                .withStyle(ChatFormatting.YELLOW))
                                                .append(String.format(" x%d-%d (%.0f%%)",
                                                                entry.getMinCount(), entry.getMaxCount(),
                                                                entry.getChance() * 100)),
                                                false);
                        }
                });

                return 1;
        }

        // ==================== Предметы - Команды ====================

        private static int setItemDamage(CommandContext<CommandSourceStack> context) {
                ResourceLocation itemId = ResourceLocationArgument.getId(context, "item");
                double value = DoubleArgumentType.getDouble(context, "value");

                if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
                        context.getSource().sendFailure(Component.literal("Предмет не найден: " + itemId));
                        return 0;
                }

                MobConfig.ItemStats stats = MobEditorMod.getConfig().getItemStats(itemId.toString());
                if (stats == null)
                        stats = new MobConfig.ItemStats();
                stats.setAttackDamage(value);
                MobEditorMod.getConfig().setItemStats(itemId.toString(), stats);

                context.getSource().sendSuccess(() -> Component.literal("Урон ")
                                .append(Component.literal(itemId.toString()).withStyle(ChatFormatting.YELLOW))
                                .append(" установлен на ")
                                .append(Component.literal(String.format("%.1f", value)).withStyle(ChatFormatting.RED))
                                .append(" ⚔"), true);
                return 1;
        }

        private static int setItemSpeed(CommandContext<CommandSourceStack> context) {
                ResourceLocation itemId = ResourceLocationArgument.getId(context, "item");
                double value = DoubleArgumentType.getDouble(context, "value");

                if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
                        context.getSource().sendFailure(Component.literal("Предмет не найден: " + itemId));
                        return 0;
                }

                MobConfig.ItemStats stats = MobEditorMod.getConfig().getItemStats(itemId.toString());
                if (stats == null)
                        stats = new MobConfig.ItemStats();
                stats.setAttackSpeed(value);
                MobEditorMod.getConfig().setItemStats(itemId.toString(), stats);

                context.getSource().sendSuccess(() -> Component.literal("Скорость атаки ")
                                .append(Component.literal(itemId.toString()).withStyle(ChatFormatting.YELLOW))
                                .append(" установлена на ")
                                .append(Component.literal(String.format("%.2f", value)).withStyle(ChatFormatting.AQUA)),
                                true);
                return 1;
        }

        private static int setItemDurability(CommandContext<CommandSourceStack> context) {
                ResourceLocation itemId = ResourceLocationArgument.getId(context, "item");
                int value = IntegerArgumentType.getInteger(context, "value");

                if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
                        context.getSource().sendFailure(Component.literal("Предмет не найден: " + itemId));
                        return 0;
                }

                MobConfig.ItemStats stats = MobEditorMod.getConfig().getItemStats(itemId.toString());
                if (stats == null)
                        stats = new MobConfig.ItemStats();
                stats.setDurability(value);
                MobEditorMod.getConfig().setItemStats(itemId.toString(), stats);

                context.getSource().sendSuccess(() -> Component.literal("Прочность ")
                                .append(Component.literal(itemId.toString()).withStyle(ChatFormatting.YELLOW))
                                .append(" установлена на ")
                                .append(Component.literal(String.valueOf(value)).withStyle(ChatFormatting.GREEN)),
                                true);
                return 1;
        }

        private static int setItemArmor(CommandContext<CommandSourceStack> context) {
                ResourceLocation itemId = ResourceLocationArgument.getId(context, "item");
                double value = DoubleArgumentType.getDouble(context, "value");

                if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
                        context.getSource().sendFailure(Component.literal("Предмет не найден: " + itemId));
                        return 0;
                }

                MobConfig.ItemStats stats = MobEditorMod.getConfig().getItemStats(itemId.toString());
                if (stats == null)
                        stats = new MobConfig.ItemStats();
                stats.setArmor(value);
                MobEditorMod.getConfig().setItemStats(itemId.toString(), stats);

                context.getSource().sendSuccess(() -> Component.literal("Броня ")
                                .append(Component.literal(itemId.toString()).withStyle(ChatFormatting.YELLOW))
                                .append(" установлена на ")
                                .append(Component.literal(String.format("%.1f", value)).withStyle(ChatFormatting.BLUE))
                                .append(" 🛡"), true);
                return 1;
        }

        private static int setItemToughness(CommandContext<CommandSourceStack> context) {
                ResourceLocation itemId = ResourceLocationArgument.getId(context, "item");
                double value = DoubleArgumentType.getDouble(context, "value");

                if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
                        context.getSource().sendFailure(Component.literal("Предмет не найден: " + itemId));
                        return 0;
                }

                MobConfig.ItemStats stats = MobEditorMod.getConfig().getItemStats(itemId.toString());
                if (stats == null)
                        stats = new MobConfig.ItemStats();
                stats.setArmorToughness(value);
                MobEditorMod.getConfig().setItemStats(itemId.toString(), stats);

                context.getSource().sendSuccess(() -> Component.literal("Прочность брони ")
                                .append(Component.literal(itemId.toString()).withStyle(ChatFormatting.YELLOW))
                                .append(" установлена на ")
                                .append(Component.literal(String.format("%.1f", value))
                                                .withStyle(ChatFormatting.DARK_BLUE)),
                                true);
                return 1;
        }

        private static int setItemKnockback(CommandContext<CommandSourceStack> context) {
                ResourceLocation itemId = ResourceLocationArgument.getId(context, "item");
                double value = DoubleArgumentType.getDouble(context, "value");

                if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
                        context.getSource().sendFailure(Component.literal("Предмет не найден: " + itemId));
                        return 0;
                }

                MobConfig.ItemStats stats = MobEditorMod.getConfig().getItemStats(itemId.toString());
                if (stats == null)
                        stats = new MobConfig.ItemStats();
                stats.setKnockbackResist(value);
                MobEditorMod.getConfig().setItemStats(itemId.toString(), stats);

                context.getSource().sendSuccess(() -> Component.literal("Сопротивление откид. ")
                                .append(Component.literal(itemId.toString()).withStyle(ChatFormatting.YELLOW))
                                .append(" установлено на ")
                                .append(Component.literal(String.format("%.2f", value))
                                                .withStyle(ChatFormatting.DARK_GREEN)),
                                true);
                return 1;
        }

        private static int setItemMoveSpeed(CommandContext<CommandSourceStack> context) {
                ResourceLocation itemId = ResourceLocationArgument.getId(context, "item");
                double value = DoubleArgumentType.getDouble(context, "value");

                if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
                        context.getSource().sendFailure(Component.literal("Предмет не найден: " + itemId));
                        return 0;
                }

                MobConfig.ItemStats stats = MobEditorMod.getConfig().getItemStats(itemId.toString());
                if (stats == null)
                        stats = new MobConfig.ItemStats();
                stats.setMovementSpeed(value);
                MobEditorMod.getConfig().setItemStats(itemId.toString(), stats);

                context.getSource().sendSuccess(() -> Component.literal("Скорость движения ")
                                .append(Component.literal(itemId.toString()).withStyle(ChatFormatting.YELLOW))
                                .append(" установлена на ")
                                .append(Component.literal(String.format("%.2f", value))
                                                .withStyle(ChatFormatting.LIGHT_PURPLE)),
                                true);
                return 1;
        }

        private static int getItemStats(CommandContext<CommandSourceStack> context) {
                ResourceLocation itemId = ResourceLocationArgument.getId(context, "item");

                MobConfig.ItemStats stats = MobEditorMod.getConfig().getItemStats(itemId.toString());

                if (stats == null || !stats.hasAnyStats()) {
                        context.getSource().sendSuccess(() -> Component.literal("Для ")
                                        .append(Component.literal(itemId.toString()).withStyle(ChatFormatting.YELLOW))
                                        .append(" не установлены кастомные характеристики"), false);
                        return 0;
                }

                context.getSource().sendSuccess(() -> Component.literal("=== " + itemId + " ===")
                                .withStyle(ChatFormatting.AQUA), false);

                if (stats.getAttackDamage() != null) {
                        context.getSource().sendSuccess(() -> Component.literal("  Урон: ")
                                        .append(Component.literal(String.format("%.1f", stats.getAttackDamage()))
                                                        .withStyle(ChatFormatting.RED)),
                                        false);
                }
                if (stats.getAttackSpeed() != null) {
                        context.getSource().sendSuccess(() -> Component.literal("  Скорость атаки: ")
                                        .append(Component.literal(String.format("%.2f", stats.getAttackSpeed()))
                                                        .withStyle(ChatFormatting.AQUA)),
                                        false);
                }
                if (stats.getDurability() != null) {
                        context.getSource().sendSuccess(() -> Component.literal("  Прочность: ")
                                        .append(Component.literal(String.valueOf(stats.getDurability()))
                                                        .withStyle(ChatFormatting.GREEN)),
                                        false);
                }
                if (stats.getArmor() != null) {
                        context.getSource().sendSuccess(() -> Component.literal("  Броня: ")
                                        .append(Component.literal(String.format("%.1f", stats.getArmor()))
                                                        .withStyle(ChatFormatting.BLUE)),
                                        false);
                }
                if (stats.getArmorToughness() != null) {
                        context.getSource().sendSuccess(() -> Component.literal("  Прочность брони: ")
                                        .append(Component.literal(String.format("%.1f", stats.getArmorToughness()))
                                                        .withStyle(ChatFormatting.DARK_BLUE)),
                                        false);
                }
                if (stats.getKnockbackResist() != null) {
                        context.getSource().sendSuccess(() -> Component.literal("  Сопр. откидыванию: ")
                                        .append(Component.literal(String.format("%.2f", stats.getKnockbackResist()))
                                                        .withStyle(ChatFormatting.DARK_GREEN)),
                                        false);
                }
                if (stats.getMovementSpeed() != null) {
                        context.getSource().sendSuccess(() -> Component.literal("  Скорость движения: ")
                                        .append(Component.literal(String.format("%.2f", stats.getMovementSpeed()))
                                                        .withStyle(ChatFormatting.LIGHT_PURPLE)),
                                        false);
                }

                return 1;
        }

        private static int removeItemStats(CommandContext<CommandSourceStack> context) {
                ResourceLocation itemId = ResourceLocationArgument.getId(context, "item");

                MobEditorMod.getConfig().removeItemStats(itemId.toString());

                context.getSource().sendSuccess(() -> Component.literal("Кастомные характеристики для ")
                                .append(Component.literal(itemId.toString()).withStyle(ChatFormatting.YELLOW))
                                .append(" удалены"), true);
                return 1;
        }

        private static int listItemStats(CommandContext<CommandSourceStack> context) {
                Map<String, MobConfig.ItemStats> itemMap = MobEditorMod.getConfig().getAllItemStats();

                if (itemMap.isEmpty()) {
                        context.getSource().sendSuccess(() -> Component.literal("Нет настроек характеристик предметов")
                                        .withStyle(ChatFormatting.YELLOW), false);
                        return 0;
                }

                context.getSource().sendSuccess(() -> Component.literal("=== Настройки предметов ===")
                                .withStyle(ChatFormatting.AQUA), false);

                itemMap.forEach((item, stats) -> {
                        context.getSource().sendSuccess(() -> Component.literal("• ")
                                        .append(Component.literal(item).withStyle(ChatFormatting.YELLOW))
                                        .append(": ")
                                        .append(Component.literal(stats.toString()).withStyle(ChatFormatting.GRAY)),
                                        false);
                });

                return 1;
        }

        // ==================== Боссы - Команды ====================

        private static final List<String> BOSS_COLORS = Arrays.asList("PINK", "BLUE", "RED", "GREEN", "YELLOW",
                        "PURPLE", "WHITE");
        private static final List<String> BOSS_STYLES = Arrays.asList("PROGRESS", "NOTCHED_6", "NOTCHED_10",
                        "NOTCHED_12", "NOTCHED_20");

        private static CompletableFuture<Suggestions> suggestBossColors(CommandContext<CommandSourceStack> context,
                        SuggestionsBuilder builder) {
                BOSS_COLORS.forEach(builder::suggest);
                return builder.buildFuture();
        }

        private static CompletableFuture<Suggestions> suggestBossStyles(CommandContext<CommandSourceStack> context,
                        SuggestionsBuilder builder) {
                BOSS_STYLES.forEach(builder::suggest);
                return builder.buildFuture();
        }

        private static CompletableFuture<Suggestions> suggestMusicFiles(CommandContext<CommandSourceStack> context,
                        SuggestionsBuilder builder) {
                MobEditorMod.getConfig().getAvailableMusic().forEach(builder::suggest);
                return builder.buildFuture();
        }

        private static int setBoss(CommandContext<CommandSourceStack> context) {
                ResourceLocation mobId = ResourceLocationArgument.getId(context, "mob");
                String bossName = StringArgumentType.getString(context, "name");

                if (!BuiltInRegistries.ENTITY_TYPE.containsKey(mobId)) {
                        context.getSource().sendFailure(Component.literal("Моб не найден: " + mobId));
                        return 0;
                }

                MobConfig.BossSettings settings = MobEditorMod.getConfig().getBossSettings(mobId.toString());
                if (settings == null)
                        settings = new MobConfig.BossSettings();
                settings.setBoss(true);
                settings.setBossName(bossName);
                MobEditorMod.getConfig().setBossSettings(mobId.toString(), settings);

                context.getSource().sendSuccess(() -> Component.literal("Моб ")
                                .append(Component.literal(mobId.toString()).withStyle(ChatFormatting.GOLD))
                                .append(" теперь босс с именем ")
                                .append(Component.literal(bossName).withStyle(ChatFormatting.RED)), true);
                return 1;
        }

        private static int setBossColor(CommandContext<CommandSourceStack> context) {
                ResourceLocation mobId = ResourceLocationArgument.getId(context, "mob");
                String color = StringArgumentType.getString(context, "color").toUpperCase();

                if (!BOSS_COLORS.contains(color)) {
                        context.getSource().sendFailure(Component
                                        .literal("Неверный цвет. Доступные: " + String.join(", ", BOSS_COLORS)));
                        return 0;
                }

                MobConfig.BossSettings settings = MobEditorMod.getConfig().getBossSettings(mobId.toString());
                if (settings == null)
                        settings = new MobConfig.BossSettings();
                settings.setBarColor(color);
                MobEditorMod.getConfig().setBossSettings(mobId.toString(), settings);

                context.getSource().sendSuccess(() -> Component.literal("Цвет боссбара ")
                                .append(Component.literal(mobId.toString()).withStyle(ChatFormatting.GOLD))
                                .append(" установлен на ")
                                .append(Component.literal(color).withStyle(ChatFormatting.AQUA)), true);
                return 1;
        }

        private static int setBossStyle(CommandContext<CommandSourceStack> context) {
                ResourceLocation mobId = ResourceLocationArgument.getId(context, "mob");
                String style = StringArgumentType.getString(context, "style").toUpperCase();

                if (!BOSS_STYLES.contains(style)) {
                        context.getSource().sendFailure(Component
                                        .literal("Неверный стиль. Доступные: " + String.join(", ", BOSS_STYLES)));
                        return 0;
                }

                MobConfig.BossSettings settings = MobEditorMod.getConfig().getBossSettings(mobId.toString());
                if (settings == null)
                        settings = new MobConfig.BossSettings();
                settings.setBarStyle(style);
                MobEditorMod.getConfig().setBossSettings(mobId.toString(), settings);

                context.getSource().sendSuccess(() -> Component.literal("Стиль боссбара ")
                                .append(Component.literal(mobId.toString()).withStyle(ChatFormatting.GOLD))
                                .append(" установлен на ")
                                .append(Component.literal(style).withStyle(ChatFormatting.AQUA)), true);
                return 1;
        }

        private static int setBossMusic(CommandContext<CommandSourceStack> context) {
                ResourceLocation mobId = ResourceLocationArgument.getId(context, "mob");
                String filename = StringArgumentType.getString(context, "filename");

                MobConfig.BossSettings settings = MobEditorMod.getConfig().getBossSettings(mobId.toString());
                if (settings == null)
                        settings = new MobConfig.BossSettings();
                settings.setMusicFile(filename);
                MobEditorMod.getConfig().setBossSettings(mobId.toString(), settings);

                context.getSource().sendSuccess(() -> Component.literal("Музыка босса ")
                                .append(Component.literal(mobId.toString()).withStyle(ChatFormatting.GOLD))
                                .append(" установлена на ")
                                .append(Component.literal(filename).withStyle(ChatFormatting.LIGHT_PURPLE)), true);
                return 1;
        }

        private static int setBossRange(CommandContext<CommandSourceStack> context) {
                ResourceLocation mobId = ResourceLocationArgument.getId(context, "mob");
                int range = IntegerArgumentType.getInteger(context, "range");

                MobConfig.BossSettings settings = MobEditorMod.getConfig().getBossSettings(mobId.toString());
                if (settings == null)
                        settings = new MobConfig.BossSettings();
                settings.setMusicRange(range);
                MobEditorMod.getConfig().setBossSettings(mobId.toString(), settings);

                context.getSource().sendSuccess(() -> Component.literal("Радиус босса ")
                                .append(Component.literal(mobId.toString()).withStyle(ChatFormatting.GOLD))
                                .append(" установлен на ")
                                .append(Component.literal(String.valueOf(range)).withStyle(ChatFormatting.GREEN))
                                .append(" блоков"), true);
                return 1;
        }

        private static int setBossDarken(CommandContext<CommandSourceStack> context) {
                ResourceLocation mobId = ResourceLocationArgument.getId(context, "mob");
                boolean enabled = BoolArgumentType.getBool(context, "enabled");

                MobConfig.BossSettings settings = MobEditorMod.getConfig().getBossSettings(mobId.toString());
                if (settings == null)
                        settings = new MobConfig.BossSettings();
                settings.setDarkenScreen(enabled);
                MobEditorMod.getConfig().setBossSettings(mobId.toString(), settings);

                context.getSource().sendSuccess(() -> Component.literal("Затемнение экрана для ")
                                .append(Component.literal(mobId.toString()).withStyle(ChatFormatting.GOLD))
                                .append(enabled ? " включено" : " выключено"), true);
                return 1;
        }

        private static int setBossFog(CommandContext<CommandSourceStack> context) {
                ResourceLocation mobId = ResourceLocationArgument.getId(context, "mob");
                boolean enabled = BoolArgumentType.getBool(context, "enabled");

                MobConfig.BossSettings settings = MobEditorMod.getConfig().getBossSettings(mobId.toString());
                if (settings == null)
                        settings = new MobConfig.BossSettings();
                settings.setCreateFog(enabled);
                MobEditorMod.getConfig().setBossSettings(mobId.toString(), settings);

                context.getSource().sendSuccess(() -> Component.literal("Туман для ")
                                .append(Component.literal(mobId.toString()).withStyle(ChatFormatting.GOLD))
                                .append(enabled ? " включён" : " выключен"), true);
                return 1;
        }

        private static int getBossSettings(CommandContext<CommandSourceStack> context) {
                ResourceLocation mobId = ResourceLocationArgument.getId(context, "mob");

                MobConfig.BossSettings settings = MobEditorMod.getConfig().getBossSettings(mobId.toString());

                if (settings == null) {
                        context.getSource().sendSuccess(() -> Component.literal("Для ")
                                        .append(Component.literal(mobId.toString()).withStyle(ChatFormatting.GOLD))
                                        .append(" не установлены настройки босса"), false);
                        return 0;
                }

                context.getSource().sendSuccess(() -> Component.literal("=== Босс: " + mobId + " ===")
                                .withStyle(ChatFormatting.DARK_RED), false);
                context.getSource().sendSuccess(() -> Component.literal("  Имя: ")
                                .append(Component
                                                .literal(settings.getBossName() != null ? settings.getBossName()
                                                                : "По умолчанию")
                                                .withStyle(ChatFormatting.RED)),
                                false);
                context.getSource().sendSuccess(() -> Component.literal("  Цвет: ")
                                .append(Component.literal(settings.getBarColor()).withStyle(ChatFormatting.AQUA)),
                                false);
                context.getSource().sendSuccess(() -> Component.literal("  Стиль: ")
                                .append(Component.literal(settings.getBarStyle()).withStyle(ChatFormatting.AQUA)),
                                false);
                context.getSource().sendSuccess(() -> Component.literal("  Радиус: ")
                                .append(Component.literal(String.valueOf(settings.getMusicRange()))
                                                .withStyle(ChatFormatting.GREEN)),
                                false);
                context.getSource().sendSuccess(() -> Component.literal("  Музыка: ")
                                .append(Component
                                                .literal(settings.getMusicFile() != null ? settings.getMusicFile()
                                                                : "Нет")
                                                .withStyle(ChatFormatting.LIGHT_PURPLE)),
                                false);
                context.getSource().sendSuccess(() -> Component.literal("  Затемнение: ")
                                .append(Component.literal(settings.isDarkenScreen() ? "Да" : "Нет")
                                                .withStyle(ChatFormatting.GRAY)),
                                false);
                context.getSource().sendSuccess(() -> Component.literal("  Туман: ")
                                .append(Component.literal(settings.isCreateFog() ? "Да" : "Нет")
                                                .withStyle(ChatFormatting.GRAY)),
                                false);

                return 1;
        }

        private static int removeBoss(CommandContext<CommandSourceStack> context) {
                ResourceLocation mobId = ResourceLocationArgument.getId(context, "mob");

                MobEditorMod.getConfig().removeBossSettings(mobId.toString());

                context.getSource().sendSuccess(() -> Component.literal("Настройки босса для ")
                                .append(Component.literal(mobId.toString()).withStyle(ChatFormatting.GOLD))
                                .append(" удалены"), true);
                return 1;
        }

        private static int listBosses(CommandContext<CommandSourceStack> context) {
                Map<String, MobConfig.BossSettings> bossMap = MobEditorMod.getConfig().getAllBossSettings();

                if (bossMap.isEmpty()) {
                        context.getSource().sendSuccess(() -> Component.literal("Нет настроек боссов")
                                        .withStyle(ChatFormatting.YELLOW), false);
                        return 0;
                }

                context.getSource().sendSuccess(() -> Component.literal("=== Настройки боссов ===")
                                .withStyle(ChatFormatting.DARK_RED), false);

                bossMap.forEach((mob, settings) -> {
                        context.getSource().sendSuccess(() -> Component.literal("• ")
                                        .append(Component.literal(mob).withStyle(ChatFormatting.GOLD))
                                        .append(": ")
                                        .append(Component.literal(settings.toString()).withStyle(ChatFormatting.GRAY)),
                                        false);
                });

                return 1;
        }

        private static int listMusic(CommandContext<CommandSourceStack> context) {
                List<String> musicFiles = MobEditorMod.getConfig().getAvailableMusic();

                if (musicFiles.isEmpty()) {
                        context.getSource()
                                        .sendSuccess(() -> Component.literal(
                                                        "Нет музыкальных файлов в папке config/mobeditor/music/")
                                                        .withStyle(ChatFormatting.YELLOW), false);
                        context.getSource().sendSuccess(() -> Component.literal("Добавьте файлы .ogg в эту папку")
                                        .withStyle(ChatFormatting.GRAY), false);
                        return 0;
                }

                context.getSource().sendSuccess(() -> Component.literal("=== Доступные музыкальные файлы ===")
                                .withStyle(ChatFormatting.LIGHT_PURPLE), false);

                for (String file : musicFiles) {
                        context.getSource().sendSuccess(() -> Component.literal("• ")
                                        .append(Component.literal(file).withStyle(ChatFormatting.AQUA)), false);
                }

                return 1;
        }

        // ==================== Эффекты - Команды ====================

        private static CompletableFuture<Suggestions> suggestEffects(CommandContext<CommandSourceStack> context,
                        SuggestionsBuilder builder) {
                BuiltInRegistries.MOB_EFFECT.keySet().forEach(id -> builder.suggest(id.toString()));
                return builder.buildFuture();
        }

        private static CompletableFuture<Suggestions> suggestArmorSets(CommandContext<CommandSourceStack> context,
                        SuggestionsBuilder builder) {
                MobEditorMod.getConfig().getAllArmorSetBonuses().keySet().forEach(builder::suggest);
                return builder.buildFuture();
        }

        private static int addItemEffect(CommandContext<CommandSourceStack> context) {
                ResourceLocation itemId = ResourceLocationArgument.getId(context, "item");
                ResourceLocation effectId = ResourceLocationArgument.getId(context, "effect");
                int level = IntegerArgumentType.getInteger(context, "level");

                if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
                        context.getSource().sendFailure(Component.literal("Предмет не найден: " + itemId));
                        return 0;
                }

                if (!BuiltInRegistries.MOB_EFFECT.containsKey(effectId)) {
                        context.getSource().sendFailure(Component.literal("Эффект не найден: " + effectId));
                        return 0;
                }

                MobConfig.EffectEntry entry = new MobConfig.EffectEntry(effectId.toString(), level - 1);
                MobEditorMod.getConfig().addItemEffect(itemId.toString(), entry);

                context.getSource().sendSuccess(() -> Component.literal("Добавлен эффект ")
                                .append(Component.literal(effectId.toString()).withStyle(ChatFormatting.LIGHT_PURPLE))
                                .append(" " + level + " для ")
                                .append(Component.literal(itemId.toString()).withStyle(ChatFormatting.YELLOW)), true);
                return 1;
        }

        private static int removeItemEffect(CommandContext<CommandSourceStack> context) {
                ResourceLocation itemId = ResourceLocationArgument.getId(context, "item");
                ResourceLocation effectId = ResourceLocationArgument.getId(context, "effect");

                MobEditorMod.getConfig().removeItemEffect(itemId.toString(), effectId.toString());

                context.getSource().sendSuccess(() -> Component.literal("Удалён эффект ")
                                .append(Component.literal(effectId.toString()).withStyle(ChatFormatting.LIGHT_PURPLE))
                                .append(" для ")
                                .append(Component.literal(itemId.toString()).withStyle(ChatFormatting.YELLOW)), true);
                return 1;
        }

        private static int clearItemEffects(CommandContext<CommandSourceStack> context) {
                ResourceLocation itemId = ResourceLocationArgument.getId(context, "item");

                MobEditorMod.getConfig().clearItemEffects(itemId.toString());

                context.getSource().sendSuccess(() -> Component.literal("Все эффекты для ")
                                .append(Component.literal(itemId.toString()).withStyle(ChatFormatting.YELLOW))
                                .append(" удалены"), true);
                return 1;
        }

        private static int getItemEffects(CommandContext<CommandSourceStack> context) {
                ResourceLocation itemId = ResourceLocationArgument.getId(context, "item");
                List<MobConfig.EffectEntry> effects = MobEditorMod.getConfig().getItemEffects(itemId.toString());

                if (effects.isEmpty()) {
                        context.getSource().sendSuccess(() -> Component.literal("Для ")
                                        .append(Component.literal(itemId.toString()).withStyle(ChatFormatting.YELLOW))
                                        .append(" не установлены эффекты"), false);
                        return 0;
                }

                context.getSource().sendSuccess(() -> Component.literal("=== Эффекты " + itemId + " ===")
                                .withStyle(ChatFormatting.LIGHT_PURPLE), false);

                for (MobConfig.EffectEntry entry : effects) {
                        context.getSource().sendSuccess(() -> Component.literal("  • ")
                                        .append(Component.literal(entry.getEffectId()).withStyle(ChatFormatting.AQUA))
                                        .append(" " + (entry.getAmplifier() + 1)), false);
                }

                return 1;
        }

        private static int listAllEffects(CommandContext<CommandSourceStack> context) {
                Map<String, List<MobConfig.EffectEntry>> effectsMap = MobEditorMod.getConfig().getAllItemEffects();

                if (effectsMap.isEmpty()) {
                        context.getSource().sendSuccess(() -> Component.literal("Нет настроек эффектов предметов")
                                        .withStyle(ChatFormatting.YELLOW), false);
                        return 0;
                }

                context.getSource().sendSuccess(() -> Component.literal("=== Эффекты предметов ===")
                                .withStyle(ChatFormatting.LIGHT_PURPLE), false);

                effectsMap.forEach((item, effects) -> {
                        StringBuilder sb = new StringBuilder();
                        for (MobConfig.EffectEntry e : effects) {
                                if (sb.length() > 0)
                                        sb.append(", ");
                                sb.append(e.getEffectId()).append(" ").append(e.getAmplifier() + 1);
                        }
                        context.getSource().sendSuccess(() -> Component.literal("• ")
                                        .append(Component.literal(item).withStyle(ChatFormatting.YELLOW))
                                        .append(": ")
                                        .append(Component.literal(sb.toString()).withStyle(ChatFormatting.GRAY)),
                                        false);
                });

                return 1;
        }

        // ==================== Комплекты брони - Команды ====================

        private static int createArmorSet(CommandContext<CommandSourceStack> context) {
                String setName = StringArgumentType.getString(context, "name");
                ResourceLocation helmet = ResourceLocationArgument.getId(context, "helmet");
                ResourceLocation chest = ResourceLocationArgument.getId(context, "chest");
                ResourceLocation legs = ResourceLocationArgument.getId(context, "legs");
                ResourceLocation boots = ResourceLocationArgument.getId(context, "boots");

                MobConfig.ArmorSetBonus bonus = new MobConfig.ArmorSetBonus();
                bonus.setHelmet(helmet.toString());
                bonus.setChestplate(chest.toString());
                bonus.setLeggings(legs.toString());
                bonus.setBoots(boots.toString());

                MobEditorMod.getConfig().setArmorSetBonus(setName, bonus);

                context.getSource().sendSuccess(() -> Component.literal("Создан комплект брони ")
                                .append(Component.literal(setName).withStyle(ChatFormatting.GOLD))
                                .append(". Добавьте эффекты командой /mobeditor armorset addeffect"), true);
                return 1;
        }

        private static int addArmorSetEffect(CommandContext<CommandSourceStack> context) {
                String setName = StringArgumentType.getString(context, "name");
                ResourceLocation effectId = ResourceLocationArgument.getId(context, "effect");
                int level = IntegerArgumentType.getInteger(context, "level");

                MobConfig.ArmorSetBonus bonus = MobEditorMod.getConfig().getArmorSetBonus(setName);
                if (bonus == null) {
                        context.getSource().sendFailure(Component.literal("Комплект не найден: " + setName));
                        return 0;
                }

                if (!BuiltInRegistries.MOB_EFFECT.containsKey(effectId)) {
                        context.getSource().sendFailure(Component.literal("Эффект не найден: " + effectId));
                        return 0;
                }

                bonus.addEffect(new MobConfig.EffectEntry(effectId.toString(), level - 1));
                MobEditorMod.getConfig().setArmorSetBonus(setName, bonus);

                context.getSource().sendSuccess(() -> Component.literal("Добавлен эффект ")
                                .append(Component.literal(effectId.toString()).withStyle(ChatFormatting.LIGHT_PURPLE))
                                .append(" " + level + " к комплекту ")
                                .append(Component.literal(setName).withStyle(ChatFormatting.GOLD)), true);
                return 1;
        }

        private static int getArmorSet(CommandContext<CommandSourceStack> context) {
                String setName = StringArgumentType.getString(context, "name");

                MobConfig.ArmorSetBonus bonus = MobEditorMod.getConfig().getArmorSetBonus(setName);
                if (bonus == null) {
                        context.getSource().sendFailure(Component.literal("Комплект не найден: " + setName));
                        return 0;
                }

                context.getSource().sendSuccess(() -> Component.literal("=== Комплект: " + setName + " ===")
                                .withStyle(ChatFormatting.GOLD), false);
                context.getSource().sendSuccess(() -> Component.literal("  Шлем: ")
                                .append(Component.literal(bonus.getHelmet()).withStyle(ChatFormatting.YELLOW)), false);
                context.getSource().sendSuccess(() -> Component.literal("  Нагрудник: ")
                                .append(Component.literal(bonus.getChestplate()).withStyle(ChatFormatting.YELLOW)),
                                false);
                context.getSource().sendSuccess(() -> Component.literal("  Поножи: ")
                                .append(Component.literal(bonus.getLeggings()).withStyle(ChatFormatting.YELLOW)),
                                false);
                context.getSource().sendSuccess(() -> Component.literal("  Ботинки: ")
                                .append(Component.literal(bonus.getBoots()).withStyle(ChatFormatting.YELLOW)), false);

                if (!bonus.getEffects().isEmpty()) {
                        context.getSource().sendSuccess(
                                        () -> Component.literal("  Пассивные эффекты:")
                                                        .withStyle(ChatFormatting.LIGHT_PURPLE),
                                        false);
                        for (MobConfig.EffectEntry e : bonus.getEffects()) {
                                context.getSource().sendSuccess(() -> Component.literal("    • ")
                                                .append(Component
                                                                .literal(e.getEffectId() + " " + (e.getAmplifier() + 1))
                                                                .withStyle(ChatFormatting.AQUA)),
                                                false);
                        }
                }

                if (!bonus.getAttackEffects().isEmpty()) {
                        context.getSource().sendSuccess(
                                        () -> Component.literal("  Эффекты при атаке:").withStyle(ChatFormatting.RED),
                                        false);
                        for (MobConfig.AttackEffectEntry e : bonus.getAttackEffects()) {
                                context.getSource().sendSuccess(() -> Component.literal("    • ")
                                                .append(Component
                                                                .literal(e.getEffectId() + " " + (e.getAmplifier() + 1)
                                                                                + " (" + (e.getDuration() / 20) + "s)")
                                                                .withStyle(ChatFormatting.DARK_RED)),
                                                false);
                        }
                }

                context.getSource().sendSuccess(() -> Component.literal("  Полёт: ")
                                .append(Component.literal(bonus.isAllowFlight() ? "Да ✓" : "Нет ✗")
                                                .withStyle(bonus.isAllowFlight() ? ChatFormatting.GREEN
                                                                : ChatFormatting.GRAY)),
                                false);

                return 1;
        }

        private static int removeArmorSet(CommandContext<CommandSourceStack> context) {
                String setName = StringArgumentType.getString(context, "name");

                MobEditorMod.getConfig().removeArmorSetBonus(setName);

                context.getSource().sendSuccess(() -> Component.literal("Комплект брони ")
                                .append(Component.literal(setName).withStyle(ChatFormatting.GOLD))
                                .append(" удалён"), true);
                return 1;
        }

        private static int listArmorSets(CommandContext<CommandSourceStack> context) {
                Map<String, MobConfig.ArmorSetBonus> sets = MobEditorMod.getConfig().getAllArmorSetBonuses();

                if (sets.isEmpty()) {
                        context.getSource().sendSuccess(() -> Component.literal("Нет комплектов брони")
                                        .withStyle(ChatFormatting.YELLOW), false);
                        return 0;
                }

                context.getSource().sendSuccess(() -> Component.literal("=== Комплекты брони ===")
                                .withStyle(ChatFormatting.GOLD), false);

                sets.forEach((name, bonus) -> {
                        context.getSource().sendSuccess(() -> Component.literal("• ")
                                        .append(Component.literal(name).withStyle(ChatFormatting.YELLOW))
                                        .append(": ")
                                        .append(Component.literal(bonus.getEffects().size() + " эффектов")
                                                        .withStyle(ChatFormatting.GRAY)),
                                        false);
                });

                return 1;
        }

        private static int addArmorSetAttackEffect(CommandContext<CommandSourceStack> context) {
                String setName = StringArgumentType.getString(context, "name");
                ResourceLocation effectId = ResourceLocationArgument.getId(context, "effect");
                int level = IntegerArgumentType.getInteger(context, "level");
                int duration = IntegerArgumentType.getInteger(context, "duration");

                MobConfig.ArmorSetBonus bonus = MobEditorMod.getConfig().getArmorSetBonus(setName);
                if (bonus == null) {
                        context.getSource().sendFailure(Component.literal("Комплект не найден: " + setName));
                        return 0;
                }

                if (!BuiltInRegistries.MOB_EFFECT.containsKey(effectId)) {
                        context.getSource().sendFailure(Component.literal("Эффект не найден: " + effectId));
                        return 0;
                }

                bonus.addAttackEffect(new MobConfig.AttackEffectEntry(effectId.toString(), level - 1, duration));
                MobEditorMod.getConfig().setArmorSetBonus(setName, bonus);

                context.getSource().sendSuccess(() -> Component.literal("Добавлен эффект атаки ")
                                .append(Component.literal(effectId.toString()).withStyle(ChatFormatting.LIGHT_PURPLE))
                                .append(" " + level + " (" + duration + " тиков) к комплекту ")
                                .append(Component.literal(setName).withStyle(ChatFormatting.GOLD)), true);
                return 1;
        }

        private static int setArmorSetFlight(CommandContext<CommandSourceStack> context) {
                String setName = StringArgumentType.getString(context, "name");
                boolean enabled = BoolArgumentType.getBool(context, "enabled");

                MobConfig.ArmorSetBonus bonus = MobEditorMod.getConfig().getArmorSetBonus(setName);
                if (bonus == null) {
                        context.getSource().sendFailure(Component.literal("Комплект не найден: " + setName));
                        return 0;
                }

                bonus.setAllowFlight(enabled);
                MobEditorMod.getConfig().setArmorSetBonus(setName, bonus);

                context.getSource().sendSuccess(() -> Component.literal("Полёт для комплекта ")
                                .append(Component.literal(setName).withStyle(ChatFormatting.GOLD))
                                .append(enabled ? " включён ✓" : " выключен ✗"), true);
                return 1;
        }

        // ==================== Достижения ====================

        private static int addAdvancement(CommandContext<CommandSourceStack> context) {
                String id = StringArgumentType.getString(context, "id");
                String triggerType = StringArgumentType.getString(context, "triggerType");
                ResourceLocation triggerTarget = ResourceLocationArgument.getId(context, "triggerTarget");
                String title = StringArgumentType.getString(context, "title");

                // Проверяем тип триггера
                if (!"kill_mob".equals(triggerType) && !"obtain_item".equals(triggerType)) {
                        context.getSource().sendFailure(Component
                                        .literal("Неверный тип триггера! Используйте: kill_mob или obtain_item"));
                        return 0;
                }

                // Проверяем цель триггера
                if ("kill_mob".equals(triggerType) && !BuiltInRegistries.ENTITY_TYPE.containsKey(triggerTarget)) {
                        context.getSource().sendFailure(Component.literal("Моб не найден: " + triggerTarget));
                        return 0;
                }
                if ("obtain_item".equals(triggerType) && !BuiltInRegistries.ITEM.containsKey(triggerTarget)) {
                        context.getSource().sendFailure(Component.literal("Предмет не найден: " + triggerTarget));
                        return 0;
                }

                // Описание генерируется автоматически
                String description;
                if ("kill_mob".equals(triggerType)) {
                        description = "Убейте " + triggerTarget.getPath();
                } else {
                        description = "Получите " + triggerTarget.getPath();
                }

                MobConfig.AdvancementEntry entry = new MobConfig.AdvancementEntry(title, description, triggerType,
                                triggerTarget.toString());
                entry.setIconItem("minecraft:book");
                entry.setFrame("task");

                MobEditorMod.getConfig().setAdvancement(id, entry);

                context.getSource().sendSuccess(
                                () -> Component.literal("✓ Достижение добавлено!")
                                                .withStyle(ChatFormatting.GREEN),
                                true);
                context.getSource().sendSuccess(
                                () -> Component.literal("  ID: ")
                                                .append(Component.literal(id).withStyle(ChatFormatting.YELLOW)),
                                false);
                context.getSource().sendSuccess(
                                () -> Component.literal("  Название: ")
                                                .append(Component.literal(title).withStyle(ChatFormatting.GOLD)),
                                false);
                context.getSource().sendSuccess(
                                () -> Component.literal("  Триггер: ")
                                                .append(Component.literal(triggerType + " -> " + triggerTarget)
                                                                .withStyle(ChatFormatting.AQUA)),
                                false);
                return 1;
        }

        private static CompletableFuture<Suggestions> suggestAdvancements(CommandContext<CommandSourceStack> context,
                        SuggestionsBuilder builder) {
                MobEditorMod.getConfig().getAllAdvancements().keySet().forEach(builder::suggest);
                return builder.buildFuture();
        }

        private static int removeAdvancement(CommandContext<CommandSourceStack> context) {
                String id = StringArgumentType.getString(context, "id");

                if (MobEditorMod.getConfig().getAdvancement(id) == null) {
                        context.getSource().sendFailure(Component.literal("Достижение не найдено: " + id));
                        return 0;
                }

                MobEditorMod.getConfig().removeAdvancement(id);

                context.getSource().sendSuccess(
                                () -> Component.literal("Достижение удалено: " + id)
                                                .withStyle(ChatFormatting.GREEN),
                                true);
                return 1;
        }

        private static int setAdvancementDescription(CommandContext<CommandSourceStack> context) {
                String id = StringArgumentType.getString(context, "id");
                String description = StringArgumentType.getString(context, "description");

                MobConfig.AdvancementEntry entry = MobEditorMod.getConfig().getAdvancement(id);
                if (entry == null) {
                        context.getSource().sendFailure(Component.literal("Достижение не найдено: " + id));
                        return 0;
                }

                entry.setDescription(description);
                MobEditorMod.getConfig().setAdvancement(id, entry);

                context.getSource().sendSuccess(
                                () -> Component.literal("Описание достижения ")
                                                .append(Component.literal(id).withStyle(ChatFormatting.YELLOW))
                                                .append(" изменено на: ")
                                                .append(Component.literal(description).withStyle(ChatFormatting.GRAY)),
                                true);
                return 1;
        }

        private static int getAdvancement(CommandContext<CommandSourceStack> context) {
                String id = StringArgumentType.getString(context, "id");

                MobConfig.AdvancementEntry entry = MobEditorMod.getConfig().getAdvancement(id);
                if (entry == null) {
                        context.getSource().sendFailure(Component.literal("Достижение не найдено: " + id));
                        return 0;
                }

                context.getSource().sendSuccess(
                                () -> Component.literal("=== Достижение: " + id + " ===")
                                                .withStyle(ChatFormatting.GOLD),
                                false);
                context.getSource().sendSuccess(
                                () -> Component.literal("  Название: ")
                                                .append(Component.literal(entry.getTitle())
                                                                .withStyle(ChatFormatting.YELLOW)),
                                false);
                context.getSource().sendSuccess(
                                () -> Component.literal("  Описание: ")
                                                .append(Component.literal(entry.getDescription())
                                                                .withStyle(ChatFormatting.GRAY)),
                                false);
                context.getSource().sendSuccess(
                                () -> Component.literal("  Триггер: ")
                                                .append(Component.literal(entry.getTriggerType())
                                                                .withStyle(ChatFormatting.AQUA)),
                                false);
                context.getSource().sendSuccess(
                                () -> Component.literal("  Цель: ")
                                                .append(Component.literal(entry.getTriggerTarget())
                                                                .withStyle(ChatFormatting.GREEN)),
                                false);

                return 1;
        }

        private static int listAdvancements(CommandContext<CommandSourceStack> context) {
                Map<String, MobConfig.AdvancementEntry> advancements = MobEditorMod.getConfig()
                                .getAllAdvancements();

                if (advancements.isEmpty()) {
                        context.getSource().sendSuccess(
                                        () -> Component.literal("Нет достижений")
                                                        .withStyle(ChatFormatting.YELLOW),
                                        false);
                        return 0;
                }

                context.getSource().sendSuccess(
                                () -> Component.literal("=== Достижения ===")
                                                .withStyle(ChatFormatting.GOLD),
                                false);

                for (Map.Entry<String, MobConfig.AdvancementEntry> entry : advancements.entrySet()) {
                        MobConfig.AdvancementEntry adv = entry.getValue();
                        context.getSource().sendSuccess(
                                        () -> Component.literal("  " + entry.getKey() + ": " + adv.getTitle() + " ("
                                                        + adv.getTriggerType() + " -> " + adv.getTriggerTarget() + ")")
                                                        .withStyle(ChatFormatting.YELLOW),
                                        false);
                }

                return 1;
        }

        // ==================== Утилиты ====================

        private static int reload(CommandContext<CommandSourceStack> context) {
                MobEditorMod.getConfig().load();
                context.getSource().sendSuccess(() -> Component.literal("Конфигурация перезагружена!")
                                .withStyle(ChatFormatting.GREEN), true);
                return 1;
        }

        private static int save(CommandContext<CommandSourceStack> context) {
                MobEditorMod.getConfig().save();
                context.getSource().sendSuccess(() -> Component.literal("Конфигурация сохранена!")
                                .withStyle(ChatFormatting.GREEN), true);
                return 1;
        }
}
