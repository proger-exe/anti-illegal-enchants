# ТЗ: Плагин AntiIllegalEnchants

## 1. Назначение

Плагин предназначен для автоматического обнаружения и уничтожения предметов с невозможными или запрещёнными зачарованиями на Minecraft Paper-сервере.

Основная цель: защита экономики, PvP, выживания и честного геймплея от предметов, полученных через баги, креатив, NBT-редакторы, эксплойты или админский идиотизм.

## 2. Платформа

* Minecraft: 1.21.x
* Сервер: Paper
* Язык: Java
* API: Bukkit/Paper API
* Конфиг: `config.yml`
* Логи: отдельный файл `illegal-enchants.log`

## 3. Что считается невозможным зачарованием

Предмет считается нелегальным, если выполняется хотя бы одно условие:

### 3.1. Превышение максимального уровня

Примеры:

* Sharpness 6+
* Protection 5+
* Efficiency 6+
* Mending 2+
* Unbreaking 4+

Плагин должен сверять уровень зачарования с максимально допустимым vanilla-уровнем.

### 3.2. Несовместимые зачарования

Примеры:

* Silk Touch + Fortune
* Mending + Infinity
* Protection + Fire Protection
* Sharpness + Smite
* Loyalty + Riptide

Плагин должен проверять конфликтующие зачарования.

### 3.3. Зачарование не подходит предмету

Примеры:

* Sharpness на луке
* Power на мече
* Protection на палке
* Efficiency на еде
* Feather Falling на шлеме

Плагин должен проверять совместимость типа предмета и зачарования.

### 3.4. Запрещённые вручную зачарования

В конфиге должна быть возможность полностью запретить отдельные зачарования.

Пример:

```yml
banned-enchants:
  - mending
  - infinity
```

### 3.5. Запрещённые комбинации вручную

В конфиге должна быть возможность добавить свои запрещённые комбинации.

Пример:

```yml
banned-combinations:
  - [mending, infinity]
  - [silk_touch, fortune]
```

## 4. Где проводится проверка

Плагин должен проверять предметы:

* при входе игрока на сервер;
* при клике по предмету в инвентаре;
* при подборе предмета;
* при перемещении предмета между инвентарями;
* при крафте;
* при использовании наковальни;
* при использовании точила;
* при торговле с жителями;
* при смерти игрока;
* при выбрасывании предмета;
* при загрузке чанка, если предмет лежит как entity на земле.

## 5. Действие при обнаружении

По умолчанию нелегальный предмет уничтожается.

Дополнительные режимы через конфиг:

```yml
action: DELETE
```

Возможные значения:

* `DELETE` — удалить предмет;
* `STRIP` — удалить только нелегальные зачарования;
* `REPLACE` — заменить предмет на обычную версию без зачарований;
* `LOG_ONLY` — только записать в лог.

## 6. Уведомления

При обнаружении нелегального предмета плагин должен:

* удалить/очистить предмет;
* уведомить игрока;
* уведомить админов с permission;
* записать событие в лог.

Пример сообщения игроку:

```text
Ваш предмет был удалён: обнаружены невозможные зачарования.
```

Пример сообщения админам:

```text
[AntiIllegalEnchants] Sh3lbyCode: DIAMOND_SWORD удалён. Причина: Sharpness 10.
```

## 7. Логирование

Каждое срабатывание должно записываться в лог:

* дата и время;
* ник игрока;
* UUID игрока;
* тип предмета;
* все зачарования предмета;
* причина удаления;
* место обнаружения;
* действие плагина.

Пример:

```text
[2026-06-08 04:12:31] Player=Sh3lbyCode UUID=... Item=DIAMOND_SWORD Enchants={sharpness=10, mending=1} Reason=MAX_LEVEL_EXCEEDED Action=DELETE Source=InventoryClick
```

## 8. Команды

### `/aie scan <player>`

Проверить инвентарь конкретного игрока.

Permission:

```text
aie.admin.scan
```

### `/aie scanall`

Проверить инвентари всех онлайн-игроков.

Permission:

```text
aie.admin.scanall
```

### `/aie reload`

Перезагрузить конфиг.

Permission:

```text
aie.admin.reload
```

### `/aie debug <player>`

Показать зачарования предмета в руке игрока и результат проверки.

Permission:

```text
aie.admin.debug
```

## 9. Permissions

```text
aie.admin.notify — получать уведомления
aie.admin.scan — сканировать игрока
aie.admin.scanall — сканировать всех
aie.admin.reload — перезагружать конфиг
aie.admin.debug — debug-проверка
aie.bypass — предметы игрока не проверяются
```

## 10. Конфигурация

Пример `config.yml`:

```yml
settings:
  action: DELETE
  notify-player: true
  notify-admins: true
  log-to-file: true
  check-on-join: true
  check-on-inventory-click: true
  check-on-pickup: true
  check-on-drop: true
  check-on-anvil: true
  check-on-villager-trade: true
  check-dropped-items-on-chunk-load: true

bypass:
  permission: "aie.bypass"

banned-enchants:
  - vanishing_curse

banned-combinations:
  - [mending, infinity]
  - [silk_touch, fortune]

max-level-overrides:
  sharpness: 5
  protection: 4
  efficiency: 5
  unbreaking: 3
  mending: 1

messages:
  player-item-deleted: "&cВаш предмет был удалён: обнаружены невозможные зачарования."
  admin-alert: "&c[AntiIllegalEnchants] &f%player%: %item% удалён. Причина: %reason%."
  reload-success: "&aAntiIllegalEnchants config reloaded."
```

## 11. Проверка предмета

Алгоритм:

1. Получить `ItemStack`.
2. Если предмет пустой или `AIR` — пропустить.
3. Получить `ItemMeta`.
4. Получить список зачарований.
5. Проверить каждый enchant:

   * существует ли enchant;
   * разрешён ли для данного предмета;
   * не превышает ли max level;
   * не находится ли в banned-enchants.
6. Проверить пары зачарований на конфликт.
7. Если найдено нарушение — применить действие из конфига.

## 12. Производительность

Плагин не должен сканировать все предметы постоянно каждый тик.

Запрещено:

* глобальное сканирование всех инвентарей каждый тик;
* перебор всех entity без причины;
* тяжёлые async-операции с Bukkit API.

Разрешено:

* event-based проверки;
* ручной `/aie scanall`;
* проверка dropped items только при chunk load;
* debounce для частых inventory events.

## 13. Защита от обхода

Плагин должен проверять не только основной инвентарь, но и:

* armor slots;
* offhand;
* cursor item;
* opened inventory;
* shulker boxes в инвентаре;
* bundle items, если поддерживаются сервером;
* dropped item entities.

Если внутри шалкера лежит нелегальный предмет, плагин должен обработать его по тем же правилам.

## 14. Edge cases

Плагин должен корректно работать с:

* книгами зачарования;
* предметами с curse enchantments;
* предметами из лута;
* предметами после наковальни;
* предметами после точила;
* предметами из торговли жителей;
* кастомными предметами, если они используют обычные vanilla enchants.

## 15. Кастомные предметы

В конфиге должна быть возможность whitelist по:

* material;
* custom model data;
* display name;
* persistent data container key.

Пример:

```yml
whitelist:
  materials:
    - STICK
  custom-model-data:
    - 1001
  pdc-keys:
    - "minejust:custom_item"
```

## 16. Требования к коду

* Код должен быть модульным.
* Проверка зачарований должна быть вынесена в отдельный сервис.
* Логика действий — отдельный сервис.
* Конфиг — отдельный manager.
* Команды — отдельный command handler.
* Никакой логики “всё в Main.java”, потому что мы всё-таки люди, а не компостер с Gradle.

Рекомендуемая структура:

```text
src/main/java/
  me/minejust/antiillegalenchants/
    AntiIllegalEnchantsPlugin.java
    config/ConfigManager.java
    check/EnchantCheckService.java
    check/CheckResult.java
    action/IllegalItemActionService.java
    listener/InventoryListener.java
    listener/PlayerListener.java
    listener/EntityListener.java
    command/AieCommand.java
    log/IllegalEnchantLogger.java
```

## 17. Критерии готовности

Плагин считается готовым, если:

* удаляет предметы с завышенными уровнями зачарований;
* удаляет предметы с несовместимыми зачарованиями;
* удаляет предметы с зачарованиями не для своего типа;
* проверяет инвентарь при входе;
* проверяет предметы при клике/подборе/выбросе;
* умеет проверять шалкеры;
* пишет лог;
* уведомляет админов;
* поддерживает reload конфига;
* не вызывает заметных лагов на сервере.
