# Mob Editor - Мод для Forge 1.20.1

Мод позволяет изменять здоровье, урон, лут мобов, характеристики предметов, создавать боссов и настраивать эффекты экипировки через команды. Все изменения сохраняются в конфигурационные файлы.

## Возможности

- ✅ Изменение максимального здоровья любого моба (до 1 миллиарда HP!)
- ✅ Изменение урона атаки любого моба
- ✅ Добавление кастомного лута с настройкой количества и шанса выпадения
- ✅ Изменение характеристик предметов (урон, скорость, прочность, броня)
- ✅ Создание боссов из любых мобов с босс-баром и кастомной музыкой
- ✅ **НОВОЕ:** Эффекты при взятии предмета в руку
- ✅ **НОВОЕ:** Эффекты при надевании брони
- ✅ **НОВОЕ:** Бонусы за полный комплект брони
- ✅ Сохранение всех настроек в JSON файлы
- ✅ Автодополнение команд с подсказками

## Установка

### Требования

- Minecraft 1.20.1
- Forge 47.0.0+ (рекомендуется 47.3.0)
- Java 17+

### Сборка мода

```bash
# Windows
.\gradlew.bat build

# Linux/Mac
./gradlew build
```

Готовый .jar файл будет в папке `build/libs/`

### Установка мода

1. Установите **Forge** для Minecraft 1.20.1
2. Скопируйте `mobeditor-1.0.0.jar` в папку `mods` вашего Minecraft

## Команды

Все команды начинаются с `/mobeditor` и требуют уровень прав 2 (оператор).

### Здоровье мобов

| Команда                                  | Описание                                |
| ---------------------------------------- | --------------------------------------- |
| `/mobeditor health set <моб> <здоровье>` | Установить здоровье моба (1-1000000000) |
| `/mobeditor health get <моб>`            | Посмотреть установленное здоровье       |
| `/mobeditor health remove <моб>`         | Удалить кастомное здоровье              |
| `/mobeditor health list`                 | Показать все настройки здоровья         |

**Примеры:**

```
/mobeditor health set minecraft:zombie 100
/mobeditor health set minecraft:warden 100000000
```

### Урон мобов

| Команда                              | Описание                      |
| ------------------------------------ | ----------------------------- |
| `/mobeditor damage set <моб> <урон>` | Установить урон атаки моба    |
| `/mobeditor damage get <моб>`        | Посмотреть установленный урон |
| `/mobeditor damage remove <моб>`     | Удалить кастомный урон        |
| `/mobeditor damage list`             | Показать все настройки урона  |

**Примеры:**

```
/mobeditor damage set minecraft:zombie 50
/mobeditor damage set minecraft:iron_golem 200
```

### Лут мобов

| Команда                                                   | Описание                         |
| --------------------------------------------------------- | -------------------------------- |
| `/mobeditor loot add <моб> <предмет> <мин> <макс> <шанс>` | Добавить предмет в лут           |
| `/mobeditor loot remove <моб> <предмет>`                  | Удалить предмет из лута          |
| `/mobeditor loot clear <моб>`                             | Очистить весь кастомный лут моба |
| `/mobeditor loot list`                                    | Показать все настройки лута      |

**Параметры:**

- `<мин>` - минимальное количество (1-64)
- `<макс>` - максимальное количество (1-64)
- `<шанс>` - шанс выпадения (0.0 - 1.0, где 1.0 = 100%)

**Примеры:**

```
/mobeditor loot add minecraft:zombie minecraft:diamond 1 3 0.5
/mobeditor loot add minecraft:ender_dragon minecraft:nether_star 10 10 1.0
```

### Характеристики предметов

| Команда                                                 | Описание                              |
| ------------------------------------------------------- | ------------------------------------- |
| `/mobeditor item damage <предмет> <урон>`               | Установить урон атаки                 |
| `/mobeditor item speed <предмет> <скорость>`            | Установить скорость атаки             |
| `/mobeditor item durability <предмет> <прочность>`      | Установить прочность                  |
| `/mobeditor item armor <предмет> <броня>`               | Установить показатель брони           |
| `/mobeditor item toughness <предмет> <прочность_брони>` | Установить прочность брони            |
| `/mobeditor item knockback <предмет> <сопротивление>`   | Установить сопротивление отбрасыванию |
| `/mobeditor item movespeed <предмет> <скорость>`        | Установить бонус к скорости движения  |
| `/mobeditor item get <предмет>`                         | Показать характеристики предмета      |
| `/mobeditor item remove <предмет>`                      | Удалить кастомные характеристики      |
| `/mobeditor item list`                                  | Показать все настройки предметов      |

**Примеры:**

```
/mobeditor item damage minecraft:diamond_sword 100
/mobeditor item speed minecraft:diamond_sword 10
/mobeditor item armor minecraft:diamond_chestplate 50
```

### Эффекты предметов (НОВОЕ!)

Добавляйте эффекты зелий к любым предметам. Эффекты работают пока предмет в руке или надет.

| Команда                                              | Описание                        |
| ---------------------------------------------------- | ------------------------------- |
| `/mobeditor effect add <предмет> <эффект> <уровень>` | Добавить эффект к предмету      |
| `/mobeditor effect remove <предмет> <эффект>`        | Удалить эффект с предмета       |
| `/mobeditor effect clear <предмет>`                  | Удалить все эффекты с предмета  |
| `/mobeditor effect get <предмет>`                    | Показать эффекты предмета       |
| `/mobeditor effect list`                             | Показать все настройки эффектов |

**Примеры:**

```
# Меч даёт силу 2 при взятии в руку
/mobeditor effect add minecraft:diamond_sword minecraft:strength 2

# Шлем даёт ночное зрение
/mobeditor effect add minecraft:diamond_helmet minecraft:night_vision 1

# Ботинки дают скорость
/mobeditor effect add minecraft:diamond_boots minecraft:speed 3
```

### Комплекты брони (НОВОЕ!)

Создавайте комплекты брони с бонусами за полный набор!

| Команда                                                                  | Описание                    |
| ------------------------------------------------------------------------ | --------------------------- |
| `/mobeditor armorset create <имя> <шлем> <нагрудник> <поножи> <ботинки>` | Создать комплект            |
| `/mobeditor armorset addeffect <имя> <эффект> <уровень>`                 | Добавить эффект к комплекту |
| `/mobeditor armorset get <имя>`                                          | Показать комплект           |
| `/mobeditor armorset remove <имя>`                                       | Удалить комплект            |
| `/mobeditor armorset list`                                               | Показать все комплекты      |

**Примеры:**

```
# Создаём комплект алмазной брони
/mobeditor armorset create diamond_set minecraft:diamond_helmet minecraft:diamond_chestplate minecraft:diamond_leggings minecraft:diamond_boots

# Добавляем бонусы за полный комплект
/mobeditor armorset addeffect diamond_set minecraft:resistance 2
/mobeditor armorset addeffect diamond_set minecraft:regeneration 1
/mobeditor armorset addeffect diamond_set minecraft:fire_resistance 1
```

### Боссы

| Команда                                            | Описание                                                             |
| -------------------------------------------------- | -------------------------------------------------------------------- |
| `/mobeditor boss set <моб> <true/false>`           | Сделать моба боссом                                                  |
| `/mobeditor boss color <моб> <цвет>`               | Цвет босс-бара (pink, blue, red, green, yellow, purple, white)       |
| `/mobeditor boss overlay <моб> <тип>`              | Тип полосы (progress, notched_6, notched_10, notched_12, notched_20) |
| `/mobeditor boss music <моб> <музыка> [дальность]` | Установить кастомную музыку                                          |
| `/mobeditor boss get <моб>`                        | Показать настройки босса                                             |
| `/mobeditor boss remove <моб>`                     | Удалить настройки босса                                              |
| `/mobeditor boss list`                             | Показать всех боссов                                                 |

**Примеры:**

```
/mobeditor boss set minecraft:warden true
/mobeditor boss color minecraft:warden red
/mobeditor health set minecraft:warden 5000
/mobeditor damage set minecraft:warden 100
```

### Утилиты

| Команда             | Описание                             |
| ------------------- | ------------------------------------ |
| `/mobeditor reload` | Перезагрузить конфигурацию из файлов |
| `/mobeditor save`   | Принудительно сохранить конфигурацию |

## Конфигурационные файлы

Файлы хранятся в папке `config/mobeditor/`:

| Файл                | Описание                                 |
| ------------------- | ---------------------------------------- |
| `mob_health.json`   | Настройки здоровья мобов                 |
| `mob_damage.json`   | Настройки урона мобов                    |
| `mob_loot.json`     | Настройки лута мобов                     |
| `item_stats.json`   | Характеристики предметов                 |
| `item_effects.json` | Эффекты предметов                        |
| `armor_sets.json`   | Комплекты брони с бонусами               |
| `mob_boss.json`     | Настройки боссов                         |
| `music/`            | Папка для кастомной музыки боссов (.ogg) |

## Доступные эффекты

Все стандартные эффекты Minecraft:

- `minecraft:speed` - Скорость
- `minecraft:strength` - Сила
- `minecraft:regeneration` - Регенерация
- `minecraft:resistance` - Сопротивление
- `minecraft:fire_resistance` - Огнеупорность
- `minecraft:night_vision` - Ночное зрение
- `minecraft:invisibility` - Невидимость
- `minecraft:jump_boost` - Прыгучесть
- `minecraft:haste` - Спешка
- И многие другие...

## Совместимость

- ✅ Работает на серверах и в одиночной игре
- ✅ Совместим с другими модами
- ✅ Поддержка неограниченного HP для эпичных боссов

## Лицензия

MIT License
