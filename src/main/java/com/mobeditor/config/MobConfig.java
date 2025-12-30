package com.mobeditor.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mobeditor.MobEditorMod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MobConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("mobeditor");
    private static final Path HEALTH_CONFIG = CONFIG_DIR.resolve("mob_health.json");
    private static final Path DAMAGE_CONFIG = CONFIG_DIR.resolve("mob_damage.json");
    private static final Path LOOT_CONFIG = CONFIG_DIR.resolve("mob_loot.json");
    private static final Path ITEM_CONFIG = CONFIG_DIR.resolve("item_stats.json");
    private static final Path BOSS_CONFIG = CONFIG_DIR.resolve("mob_boss.json");
    private static final Path EFFECTS_CONFIG = CONFIG_DIR.resolve("item_effects.json");
    private static final Path ARMOR_SET_CONFIG = CONFIG_DIR.resolve("armor_sets.json");
    private static final Path EXPLOSIVE_ITEMS_CONFIG = CONFIG_DIR.resolve("explosive_items.json");
    private static final Path ADVANCEMENTS_CONFIG = CONFIG_DIR.resolve("advancements.json");
    private static final Path CLEAR_LOOT_CONFIG = CONFIG_DIR.resolve("clear_default_loot.json");
    private static final Path STRUCTURE_LOOT_CONFIG = CONFIG_DIR.resolve("structure_loot.json");
    public static final Path MUSIC_DIR = CONFIG_DIR.resolve("music");

    // Карта здоровья мобов: EntityType ID -> Max Health
    private Map<String, Double> mobHealthMap = new ConcurrentHashMap<>();

    // Список мобов с отключённым стандартным лутом
    private Set<String> clearDefaultLootMobs = ConcurrentHashMap.newKeySet();

    // Карта урона мобов: EntityType ID -> Attack Damage
    private Map<String, Double> mobDamageMap = new ConcurrentHashMap<>();

    // Карта лута мобов: EntityType ID -> List of LootEntry
    private Map<String, List<LootEntry>> mobLootMap = new ConcurrentHashMap<>();

    // Карта характеристик предметов: Item ID -> ItemStats
    private Map<String, ItemStats> itemStatsMap = new ConcurrentHashMap<>();

    // Карта настроек боссов: EntityType ID -> BossSettings
    private Map<String, BossSettings> bossSettingsMap = new ConcurrentHashMap<>();

    // Карта эффектов предметов: Item ID -> List of EffectEntry
    private Map<String, List<EffectEntry>> itemEffectsMap = new ConcurrentHashMap<>();

    // Карта комплектов брони: Set Name -> ArmorSetBonus
    private Map<String, ArmorSetBonus> armorSetBonusMap = new ConcurrentHashMap<>();

    // Карта взрывающихся предметов: Item ID -> ExplosiveItemSettings
    private Map<String, ExplosiveItemSettings> explosiveItemsMap = new ConcurrentHashMap<>();

    // Карта достижений: Advancement ID -> AdvancementEntry
    private Map<String, AdvancementEntry> advancementsMap = new ConcurrentHashMap<>();

    // Карта лута структур: Loot Table ID -> List of LootEntry
    private Map<String, List<LootEntry>> structureLootMap = new ConcurrentHashMap<>();

    public void load() {
        try {
            Files.createDirectories(CONFIG_DIR);

            // Загрузка здоровья мобов
            if (Files.exists(HEALTH_CONFIG)) {
                try (Reader reader = Files.newBufferedReader(HEALTH_CONFIG)) {
                    Type type = new TypeToken<Map<String, Double>>() {
                    }.getType();
                    Map<String, Double> loaded = GSON.fromJson(reader, type);
                    if (loaded != null) {
                        mobHealthMap.putAll(loaded);
                    }
                }
            }

            // Загрузка урона мобов
            if (Files.exists(DAMAGE_CONFIG)) {
                try (Reader reader = Files.newBufferedReader(DAMAGE_CONFIG)) {
                    Type type = new TypeToken<Map<String, Double>>() {
                    }.getType();
                    Map<String, Double> loaded = GSON.fromJson(reader, type);
                    if (loaded != null) {
                        mobDamageMap.putAll(loaded);
                    }
                }
            }

            // Загрузка лута мобов
            if (Files.exists(LOOT_CONFIG)) {
                try (Reader reader = Files.newBufferedReader(LOOT_CONFIG)) {
                    Type type = new TypeToken<Map<String, List<LootEntry>>>() {
                    }.getType();
                    Map<String, List<LootEntry>> loaded = GSON.fromJson(reader, type);
                    if (loaded != null) {
                        mobLootMap.putAll(loaded);
                    }
                }
            }

            // Загрузка характеристик предметов
            if (Files.exists(ITEM_CONFIG)) {
                try (Reader reader = Files.newBufferedReader(ITEM_CONFIG)) {
                    Type type = new TypeToken<Map<String, ItemStats>>() {
                    }.getType();
                    Map<String, ItemStats> loaded = GSON.fromJson(reader, type);
                    if (loaded != null) {
                        itemStatsMap.putAll(loaded);
                    }
                }
            }

            // Загрузка настроек боссов
            if (Files.exists(BOSS_CONFIG)) {
                try (Reader reader = Files.newBufferedReader(BOSS_CONFIG)) {
                    Type type = new TypeToken<Map<String, BossSettings>>() {
                    }.getType();
                    Map<String, BossSettings> loaded = GSON.fromJson(reader, type);
                    if (loaded != null) {
                        bossSettingsMap.putAll(loaded);
                    }
                }
            }

            // Загрузка эффектов предметов
            if (Files.exists(EFFECTS_CONFIG)) {
                try (Reader reader = Files.newBufferedReader(EFFECTS_CONFIG)) {
                    Type type = new TypeToken<Map<String, List<EffectEntry>>>() {
                    }.getType();
                    Map<String, List<EffectEntry>> loaded = GSON.fromJson(reader, type);
                    if (loaded != null) {
                        itemEffectsMap.putAll(loaded);
                    }
                }
            }

            // Загрузка комплектов брони
            if (Files.exists(ARMOR_SET_CONFIG)) {
                try (Reader reader = Files.newBufferedReader(ARMOR_SET_CONFIG)) {
                    Type type = new TypeToken<Map<String, ArmorSetBonus>>() {
                    }.getType();
                    Map<String, ArmorSetBonus> loaded = GSON.fromJson(reader, type);
                    if (loaded != null) {
                        armorSetBonusMap.putAll(loaded);
                    }
                }
            }

            // Загрузка взрывающихся предметов
            if (Files.exists(EXPLOSIVE_ITEMS_CONFIG)) {
                try (Reader reader = Files.newBufferedReader(EXPLOSIVE_ITEMS_CONFIG)) {
                    Type type = new TypeToken<Map<String, ExplosiveItemSettings>>() {
                    }.getType();
                    Map<String, ExplosiveItemSettings> loaded = GSON.fromJson(reader, type);
                    if (loaded != null) {
                        explosiveItemsMap.putAll(loaded);
                    }
                }
            }

            // Загрузка достижений
            if (Files.exists(ADVANCEMENTS_CONFIG)) {
                try (Reader reader = Files.newBufferedReader(ADVANCEMENTS_CONFIG)) {
                    Type type = new TypeToken<Map<String, AdvancementEntry>>() {
                    }.getType();
                    Map<String, AdvancementEntry> loaded = GSON.fromJson(reader, type);
                    if (loaded != null) {
                        advancementsMap.putAll(loaded);
                    }
                }
            }

            // Загрузка списка мобов с отключённым лутом
            if (Files.exists(CLEAR_LOOT_CONFIG)) {
                try (Reader reader = Files.newBufferedReader(CLEAR_LOOT_CONFIG)) {
                    Type type = new TypeToken<Set<String>>() {
                    }.getType();
                    Set<String> loaded = GSON.fromJson(reader, type);
                    if (loaded != null) {
                        clearDefaultLootMobs.addAll(loaded);
                    }
                }
            }

            // Загрузка лута структур
            if (Files.exists(STRUCTURE_LOOT_CONFIG)) {
                try (Reader reader = Files.newBufferedReader(STRUCTURE_LOOT_CONFIG)) {
                    Type type = new TypeToken<Map<String, List<LootEntry>>>() {
                    }.getType();
                    Map<String, List<LootEntry>> loaded = GSON.fromJson(reader, type);
                    if (loaded != null) {
                        structureLootMap.putAll(loaded);
                    }
                }
            }

            // Создаём папку для музыки
            Files.createDirectories(MUSIC_DIR);

            MobEditorMod.LOGGER.info(
                    "Загружено {} настроек здоровья, {} настроек урона, {} настроек лута, {} настроек предметов, {} настроек боссов",
                    mobHealthMap.size(), mobDamageMap.size(), mobLootMap.size(), itemStatsMap.size(),
                    bossSettingsMap.size());

        } catch (IOException e) {
            MobEditorMod.LOGGER.error("Ошибка загрузки конфигурации", e);
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_DIR);

            // Сохранение здоровья
            try (Writer writer = Files.newBufferedWriter(HEALTH_CONFIG)) {
                GSON.toJson(mobHealthMap, writer);
            }

            // Сохранение урона
            try (Writer writer = Files.newBufferedWriter(DAMAGE_CONFIG)) {
                GSON.toJson(mobDamageMap, writer);
            }

            // Сохранение лута
            try (Writer writer = Files.newBufferedWriter(LOOT_CONFIG)) {
                GSON.toJson(mobLootMap, writer);
            }

            // Сохранение характеристик предметов
            try (Writer writer = Files.newBufferedWriter(ITEM_CONFIG)) {
                GSON.toJson(itemStatsMap, writer);
            }

            // Сохранение настроек боссов
            try (Writer writer = Files.newBufferedWriter(BOSS_CONFIG)) {
                GSON.toJson(bossSettingsMap, writer);
            }

            // Сохранение эффектов предметов
            try (Writer writer = Files.newBufferedWriter(EFFECTS_CONFIG)) {
                GSON.toJson(itemEffectsMap, writer);
            }

            // Сохранение комплектов брони
            try (Writer writer = Files.newBufferedWriter(ARMOR_SET_CONFIG)) {
                GSON.toJson(armorSetBonusMap, writer);
            }

            // Сохранение взрывающихся предметов
            try (Writer writer = Files.newBufferedWriter(EXPLOSIVE_ITEMS_CONFIG)) {
                GSON.toJson(explosiveItemsMap, writer);
            }

            // Сохранение достижений
            try (Writer writer = Files.newBufferedWriter(ADVANCEMENTS_CONFIG)) {
                GSON.toJson(advancementsMap, writer);
            }

            // Сохранение списка мобов с отключённым лутом
            try (Writer writer = Files.newBufferedWriter(CLEAR_LOOT_CONFIG)) {
                GSON.toJson(clearDefaultLootMobs, writer);
            }

            // Сохранение лута структур
            try (Writer writer = Files.newBufferedWriter(STRUCTURE_LOOT_CONFIG)) {
                GSON.toJson(structureLootMap, writer);
            }

        } catch (IOException e) {
            MobEditorMod.LOGGER.error("Ошибка сохранения конфигурации", e);
        }
    }

    // ==================== Здоровье ====================

    public void setMobHealth(String entityTypeId, double health) {
        mobHealthMap.put(entityTypeId, health);
        save();
    }

    public Double getMobHealth(String entityTypeId) {
        return mobHealthMap.get(entityTypeId);
    }

    public void removeMobHealth(String entityTypeId) {
        mobHealthMap.remove(entityTypeId);
        save();
    }

    public Map<String, Double> getAllMobHealth() {
        return new HashMap<>(mobHealthMap);
    }

    // ==================== Урон ====================

    public void setMobDamage(String entityTypeId, double damage) {
        mobDamageMap.put(entityTypeId, damage);
        save();
    }

    public Double getMobDamage(String entityTypeId) {
        return mobDamageMap.get(entityTypeId);
    }

    public void removeMobDamage(String entityTypeId) {
        mobDamageMap.remove(entityTypeId);
        save();
    }

    public Map<String, Double> getAllMobDamage() {
        return new HashMap<>(mobDamageMap);
    }

    // ==================== Лут ====================

    public void addLoot(String entityTypeId, LootEntry entry) {
        mobLootMap.computeIfAbsent(entityTypeId, k -> new ArrayList<>()).add(entry);
        save();
    }

    public void removeLoot(String entityTypeId, String itemId) {
        List<LootEntry> entries = mobLootMap.get(entityTypeId);
        if (entries != null) {
            entries.removeIf(e -> e.getItemId().equals(itemId));
            if (entries.isEmpty()) {
                mobLootMap.remove(entityTypeId);
            }
            save();
        }
    }

    public void clearLoot(String entityTypeId) {
        mobLootMap.remove(entityTypeId);
        save();
    }

    public List<LootEntry> getLoot(String entityTypeId) {
        return mobLootMap.getOrDefault(entityTypeId, Collections.emptyList());
    }

    public Map<String, List<LootEntry>> getAllLoot() {
        return new HashMap<>(mobLootMap);
    }

    // ==================== Очистка стандартного лута ====================

    public void setClearDefaultLoot(String entityTypeId, boolean clear) {
        if (clear) {
            clearDefaultLootMobs.add(entityTypeId);
        } else {
            clearDefaultLootMobs.remove(entityTypeId);
        }
        save();
    }

    public boolean shouldClearDefaultLoot(String entityTypeId) {
        return clearDefaultLootMobs.contains(entityTypeId);
    }

    public Set<String> getAllClearDefaultLootMobs() {
        return new HashSet<>(clearDefaultLootMobs);
    }

    // ==================== Предметы ====================

    public void setItemStats(String itemId, ItemStats stats) {
        itemStatsMap.put(itemId, stats);
        save();
    }

    public ItemStats getItemStats(String itemId) {
        return itemStatsMap.get(itemId);
    }

    public void removeItemStats(String itemId) {
        itemStatsMap.remove(itemId);
        save();
    }

    public Map<String, ItemStats> getAllItemStats() {
        return new HashMap<>(itemStatsMap);
    }

    // ==================== Боссы ====================

    public void setBossSettings(String entityTypeId, BossSettings settings) {
        bossSettingsMap.put(entityTypeId, settings);
        save();
    }

    public BossSettings getBossSettings(String entityTypeId) {
        return bossSettingsMap.get(entityTypeId);
    }

    public void removeBossSettings(String entityTypeId) {
        bossSettingsMap.remove(entityTypeId);
        save();
    }

    public Map<String, BossSettings> getAllBossSettings() {
        return new HashMap<>(bossSettingsMap);
    }

    public List<String> getAvailableMusic() {
        List<String> musicFiles = new ArrayList<>();
        try {
            if (Files.exists(MUSIC_DIR)) {
                Files.list(MUSIC_DIR)
                        .filter(p -> p.toString().endsWith(".ogg"))
                        .forEach(p -> musicFiles.add(p.getFileName().toString()));
            }
        } catch (IOException e) {
            MobEditorMod.LOGGER.error("Ошибка чтения папки музыки", e);
        }
        return musicFiles;
    }

    // ==================== Эффекты предметов ====================

    public void addItemEffect(String itemId, EffectEntry effect) {
        itemEffectsMap.computeIfAbsent(itemId, k -> new ArrayList<>()).add(effect);
        save();
    }

    public void removeItemEffect(String itemId, String effectId) {
        List<EffectEntry> effects = itemEffectsMap.get(itemId);
        if (effects != null) {
            effects.removeIf(e -> e.getEffectId().equals(effectId));
            if (effects.isEmpty()) {
                itemEffectsMap.remove(itemId);
            }
            save();
        }
    }

    public void clearItemEffects(String itemId) {
        itemEffectsMap.remove(itemId);
        save();
    }

    public List<EffectEntry> getItemEffects(String itemId) {
        return itemEffectsMap.getOrDefault(itemId, Collections.emptyList());
    }

    public Map<String, List<EffectEntry>> getAllItemEffects() {
        return new HashMap<>(itemEffectsMap);
    }

    // ==================== Комплекты брони ====================

    public void setArmorSetBonus(String setName, ArmorSetBonus bonus) {
        armorSetBonusMap.put(setName, bonus);
        save();
    }

    public ArmorSetBonus getArmorSetBonus(String setName) {
        return armorSetBonusMap.get(setName);
    }

    public void removeArmorSetBonus(String setName) {
        armorSetBonusMap.remove(setName);
        save();
    }

    public Map<String, ArmorSetBonus> getAllArmorSetBonuses() {
        return new HashMap<>(armorSetBonusMap);
    }

    // ==================== EffectEntry класс ====================

    public static class EffectEntry {
        private String effectId; // minecraft:speed, minecraft:strength и т.д.
        private int amplifier = 0; // Уровень эффекта (0 = I, 1 = II, и т.д.)
        private boolean showParticles = false;
        private boolean showIcon = true;

        public EffectEntry() {
        }

        public EffectEntry(String effectId, int amplifier) {
            this.effectId = effectId;
            this.amplifier = amplifier;
        }

        public String getEffectId() {
            return effectId;
        }

        public void setEffectId(String effectId) {
            this.effectId = effectId;
        }

        public int getAmplifier() {
            return amplifier;
        }

        public void setAmplifier(int amplifier) {
            this.amplifier = amplifier;
        }

        public boolean isShowParticles() {
            return showParticles;
        }

        public void setShowParticles(boolean showParticles) {
            this.showParticles = showParticles;
        }

        public boolean isShowIcon() {
            return showIcon;
        }

        public void setShowIcon(boolean showIcon) {
            this.showIcon = showIcon;
        }

        @Override
        public String toString() {
            return effectId + " " + (amplifier + 1);
        }
    }

    // ==================== ArmorSetBonus класс ====================

    public static class ArmorSetBonus {
        private String helmet; // ID шлема
        private String chestplate; // ID нагрудника
        private String leggings; // ID поножей
        private String boots; // ID ботинок
        private List<EffectEntry> effects = new ArrayList<>(); // Эффекты за полный комплект
        private List<AttackEffectEntry> attackEffects = new ArrayList<>(); // Эффекты при атаке на моба
        private boolean allowFlight = false; // Разрешение полёта как в креативе

        public ArmorSetBonus() {
        }

        public String getHelmet() {
            return helmet;
        }

        public void setHelmet(String helmet) {
            this.helmet = helmet;
        }

        public String getChestplate() {
            return chestplate;
        }

        public void setChestplate(String chestplate) {
            this.chestplate = chestplate;
        }

        public String getLeggings() {
            return leggings;
        }

        public void setLeggings(String leggings) {
            this.leggings = leggings;
        }

        public String getBoots() {
            return boots;
        }

        public void setBoots(String boots) {
            this.boots = boots;
        }

        public List<EffectEntry> getEffects() {
            return effects;
        }

        public void setEffects(List<EffectEntry> effects) {
            this.effects = effects;
        }

        public void addEffect(EffectEntry effect) {
            this.effects.add(effect);
        }

        public List<AttackEffectEntry> getAttackEffects() {
            return attackEffects;
        }

        public void setAttackEffects(List<AttackEffectEntry> attackEffects) {
            this.attackEffects = attackEffects;
        }

        public void addAttackEffect(AttackEffectEntry effect) {
            this.attackEffects.add(effect);
        }

        public boolean isAllowFlight() {
            return allowFlight;
        }

        public void setAllowFlight(boolean allowFlight) {
            this.allowFlight = allowFlight;
        }

        public boolean isComplete(String helmetId, String chestId, String legsId, String bootsId) {
            // Все 4 части должны точно совпадать
            return helmet != null && helmet.equals(helmetId) &&
                    chestplate != null && chestplate.equals(chestId) &&
                    leggings != null && leggings.equals(legsId) &&
                    boots != null && boots.equals(bootsId);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (helmet != null)
                sb.append("Шлем: ").append(helmet).append(" ");
            if (chestplate != null)
                sb.append("Нагрудник: ").append(chestplate).append(" ");
            if (leggings != null)
                sb.append("Поножи: ").append(leggings).append(" ");
            if (boots != null)
                sb.append("Ботинки: ").append(boots).append(" ");
            sb.append("Эффекты: ").append(effects.size());
            if (!attackEffects.isEmpty())
                sb.append(", Эффекты атаки: ").append(attackEffects.size());
            if (allowFlight)
                sb.append(", Полёт: Да");
            return sb.toString();
        }
    }

    // ==================== AttackEffectEntry класс ====================

    public static class AttackEffectEntry {
        private String effectId; // minecraft:slowness, minecraft:poison и т.д.
        private int amplifier = 0; // Уровень эффекта (0 = I, 1 = II, и т.д.)
        private int duration = 100; // Длительность в тиках (20 тиков = 1 секунда)

        public AttackEffectEntry() {
        }

        public AttackEffectEntry(String effectId, int amplifier, int duration) {
            this.effectId = effectId;
            this.amplifier = amplifier;
            this.duration = duration;
        }

        public String getEffectId() {
            return effectId;
        }

        public void setEffectId(String effectId) {
            this.effectId = effectId;
        }

        public int getAmplifier() {
            return amplifier;
        }

        public void setAmplifier(int amplifier) {
            this.amplifier = amplifier;
        }

        public int getDuration() {
            return duration;
        }

        public void setDuration(int duration) {
            this.duration = duration;
        }

        @Override
        public String toString() {
            return effectId + " " + (amplifier + 1) + " (" + (duration / 20) + "s)";
        }
    }

    // ==================== BossSettings класс ====================

    public static class BossSettings {
        private boolean isBoss = true;
        private String bossName; // Имя босса на боссбаре
        private String barColor = "RED"; // Цвет: PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE
        private String barStyle = "PROGRESS"; // Стиль: PROGRESS, NOTCHED_6, NOTCHED_10, NOTCHED_12, NOTCHED_20
        private String musicFile; // Имя файла музыки (из папки music)
        private int musicRange = 50; // Радиус воспроизведения музыки
        private boolean darkenScreen = false; // Затемнение экрана как у Дракона
        private boolean createFog = false; // Туман как у Визера

        public BossSettings() {
        }

        public BossSettings(boolean isBoss, String bossName) {
            this.isBoss = isBoss;
            this.bossName = bossName;
        }

        public boolean isBoss() {
            return isBoss;
        }

        public void setBoss(boolean boss) {
            isBoss = boss;
        }

        public String getBossName() {
            return bossName;
        }

        public void setBossName(String bossName) {
            this.bossName = bossName;
        }

        public String getBarColor() {
            return barColor;
        }

        public void setBarColor(String barColor) {
            this.barColor = barColor;
        }

        public String getBarStyle() {
            return barStyle;
        }

        public void setBarStyle(String barStyle) {
            this.barStyle = barStyle;
        }

        public String getMusicFile() {
            return musicFile;
        }

        public void setMusicFile(String musicFile) {
            this.musicFile = musicFile;
        }

        public int getMusicRange() {
            return musicRange;
        }

        public void setMusicRange(int musicRange) {
            this.musicRange = musicRange;
        }

        public boolean isDarkenScreen() {
            return darkenScreen;
        }

        public void setDarkenScreen(boolean darkenScreen) {
            this.darkenScreen = darkenScreen;
        }

        public boolean isCreateFog() {
            return createFog;
        }

        public void setCreateFog(boolean createFog) {
            this.createFog = createFog;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(isBoss ? "Босс" : "Не босс");
            if (bossName != null)
                sb.append(", Имя: ").append(bossName);
            sb.append(", Цвет: ").append(barColor);
            if (musicFile != null)
                sb.append(", Музыка: ").append(musicFile);
            return sb.toString();
        }
    }

    // ==================== ItemStats класс ====================

    public static class ItemStats {
        private Double attackDamage; // Урон атаки
        private Double attackSpeed; // Скорость атаки
        private Integer durability; // Прочность
        private Double armor; // Броня
        private Double armorToughness; // Прочность брони
        private Double knockbackResist; // Сопротивление отбрасыванию
        private Double movementSpeed; // Скорость передвижения (для брони)

        public ItemStats() {
        }

        public Double getAttackDamage() {
            return attackDamage;
        }

        public void setAttackDamage(Double attackDamage) {
            this.attackDamage = attackDamage;
        }

        public Double getAttackSpeed() {
            return attackSpeed;
        }

        public void setAttackSpeed(Double attackSpeed) {
            this.attackSpeed = attackSpeed;
        }

        public Integer getDurability() {
            return durability;
        }

        public void setDurability(Integer durability) {
            this.durability = durability;
        }

        public Double getArmor() {
            return armor;
        }

        public void setArmor(Double armor) {
            this.armor = armor;
        }

        public Double getArmorToughness() {
            return armorToughness;
        }

        public void setArmorToughness(Double armorToughness) {
            this.armorToughness = armorToughness;
        }

        public Double getKnockbackResist() {
            return knockbackResist;
        }

        public void setKnockbackResist(Double knockbackResist) {
            this.knockbackResist = knockbackResist;
        }

        public Double getMovementSpeed() {
            return movementSpeed;
        }

        public void setMovementSpeed(Double movementSpeed) {
            this.movementSpeed = movementSpeed;
        }

        public boolean hasAnyStats() {
            return attackDamage != null || attackSpeed != null || durability != null ||
                    armor != null || armorToughness != null || knockbackResist != null || movementSpeed != null;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (attackDamage != null)
                sb.append("Урон: ").append(String.format("%.1f", attackDamage)).append(" ");
            if (attackSpeed != null)
                sb.append("Скорость: ").append(String.format("%.2f", attackSpeed)).append(" ");
            if (durability != null)
                sb.append("Прочность: ").append(durability).append(" ");
            if (armor != null)
                sb.append("Броня: ").append(String.format("%.1f", armor)).append(" ");
            if (armorToughness != null)
                sb.append("Прочность брони: ").append(String.format("%.1f", armorToughness)).append(" ");
            if (knockbackResist != null)
                sb.append("Сопр. откид.: ").append(String.format("%.2f", knockbackResist)).append(" ");
            if (movementSpeed != null)
                sb.append("Скорость движ.: ").append(String.format("%.2f", movementSpeed)).append(" ");
            return sb.toString().trim();
        }
    }

    // ==================== LootEntry класс ====================

    public static class LootEntry {
        private String itemId;
        private int minCount;
        private int maxCount;
        private float chance; // 0.0 - 1.0

        public LootEntry() {
        }

        public LootEntry(String itemId, int minCount, int maxCount, float chance) {
            this.itemId = itemId;
            this.minCount = minCount;
            this.maxCount = maxCount;
            this.chance = chance;
        }

        public String getItemId() {
            return itemId;
        }

        public int getMinCount() {
            return minCount;
        }

        public int getMaxCount() {
            return maxCount;
        }

        public float getChance() {
            return chance;
        }

        public void setMinCount(int minCount) {
            this.minCount = minCount;
        }

        public void setMaxCount(int maxCount) {
            this.maxCount = maxCount;
        }

        public void setChance(float chance) {
            this.chance = chance;
        }

        @Override
        public String toString() {
            return String.format("%s (x%d-%d, %.1f%%)", itemId, minCount, maxCount, chance * 100);
        }
    }

    // ==================== ExplosiveItemSettings класс ====================

    public static class ExplosiveItemSettings {
        private float explosionPower = 4.0f; // Сила взрыва
        private boolean causesFire = false; // Вызывает ли взрыв огонь
        private boolean breaksBlocks = true; // Разрушает ли блоки

        public ExplosiveItemSettings() {
        }

        public ExplosiveItemSettings(float explosionPower, boolean causesFire, boolean breaksBlocks) {
            this.explosionPower = explosionPower;
            this.causesFire = causesFire;
            this.breaksBlocks = breaksBlocks;
        }

        public float getExplosionPower() {
            return explosionPower;
        }

        public void setExplosionPower(float explosionPower) {
            this.explosionPower = explosionPower;
        }

        public boolean isCausesFire() {
            return causesFire;
        }

        public void setCausesFire(boolean causesFire) {
            this.causesFire = causesFire;
        }

        public boolean isBreaksBlocks() {
            return breaksBlocks;
        }

        public void setBreaksBlocks(boolean breaksBlocks) {
            this.breaksBlocks = breaksBlocks;
        }
    }

    // ==================== AdvancementEntry класс ====================

    public static class AdvancementEntry {
        private String title; // Название достижения
        private String description; // Описание достижения
        private String parentId; // ID родительского достижения (для веток)
        private String iconItem; // ID предмета для иконки
        private String triggerType; // Тип триггера: "kill_mob" или "obtain_item"
        private String triggerTarget; // Цель триггера: ID моба или предмета
        private String frame = "task"; // Тип рамки: task, challenge, goal

        public AdvancementEntry() {
        }

        public AdvancementEntry(String title, String description, String triggerType, String triggerTarget) {
            this.title = title;
            this.description = description;
            this.triggerType = triggerType;
            this.triggerTarget = triggerTarget;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getParentId() {
            return parentId;
        }

        public void setParentId(String parentId) {
            this.parentId = parentId;
        }

        public String getIconItem() {
            return iconItem;
        }

        public void setIconItem(String iconItem) {
            this.iconItem = iconItem;
        }

        public String getTriggerType() {
            return triggerType;
        }

        public void setTriggerType(String triggerType) {
            this.triggerType = triggerType;
        }

        public String getTriggerTarget() {
            return triggerTarget;
        }

        public void setTriggerTarget(String triggerTarget) {
            this.triggerTarget = triggerTarget;
        }

        public String getFrame() {
            return frame;
        }

        public void setFrame(String frame) {
            this.frame = frame;
        }
    }

    // ==================== Взрывающиеся предметы ====================

    public void setExplosiveItem(String itemId, ExplosiveItemSettings settings) {
        explosiveItemsMap.put(itemId, settings);
        save();
    }

    public ExplosiveItemSettings getExplosiveItem(String itemId) {
        return explosiveItemsMap.get(itemId);
    }

    public void removeExplosiveItem(String itemId) {
        explosiveItemsMap.remove(itemId);
        save();
    }

    public Map<String, ExplosiveItemSettings> getAllExplosiveItems() {
        return new HashMap<>(explosiveItemsMap);
    }

    // ==================== Достижения ====================

    public void setAdvancement(String advancementId, AdvancementEntry entry) {
        advancementsMap.put(advancementId, entry);
        save();
    }

    public AdvancementEntry getAdvancement(String advancementId) {
        return advancementsMap.get(advancementId);
    }

    public void removeAdvancement(String advancementId) {
        advancementsMap.remove(advancementId);
        save();
    }

    public Map<String, AdvancementEntry> getAllAdvancements() {
        return new HashMap<>(advancementsMap);
    }

    public List<AdvancementEntry> getAdvancementsByTrigger(String triggerType, String triggerTarget) {
        List<AdvancementEntry> result = new ArrayList<>();
        for (AdvancementEntry entry : advancementsMap.values()) {
            if (entry.getTriggerType() != null && entry.getTriggerType().equals(triggerType) &&
                    entry.getTriggerTarget() != null && entry.getTriggerTarget().equals(triggerTarget)) {
                result.add(entry);
            }
        }
        return result;
    }

    // ==================== Лут структур ====================

    public void addStructureLoot(String lootTableId, LootEntry entry) {
        structureLootMap.computeIfAbsent(lootTableId, k -> new ArrayList<>()).add(entry);
        save();
    }

    public void removeStructureLoot(String lootTableId, String itemId) {
        List<LootEntry> entries = structureLootMap.get(lootTableId);
        if (entries != null) {
            entries.removeIf(e -> e.getItemId().equals(itemId));
            if (entries.isEmpty()) {
                structureLootMap.remove(lootTableId);
            }
            save();
        }
    }

    public void clearStructureLoot(String lootTableId) {
        structureLootMap.remove(lootTableId);
        save();
    }

    public List<LootEntry> getStructureLoot(String lootTableId) {
        return structureLootMap.getOrDefault(lootTableId, Collections.emptyList());
    }

    public Map<String, List<LootEntry>> getAllStructureLoot() {
        return new HashMap<>(structureLootMap);
    }
}
