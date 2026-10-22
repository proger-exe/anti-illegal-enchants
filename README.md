# AntiIllegalEnchants

Paper 1.21 plugin that finds and neutralises items with impossible or banned enchantments: levels above vanilla maximum, enchantments on the wrong item type, conflicting pairs (Sharpness + Smite, Protection + Blast Protection, …) and anything on a configurable blacklist. Protects the economy and PvP from items produced by NBT editors, creative mode or exploits.

## How it works

- **Checks** (`check/`): pure logic over an item's enchantment map, no Bukkit calls, covered by MockBukkit tests.
- **Scanning** (`action/`): inventories are scanned on join, on item pickup, on inventory open/click and on a periodic task. Chests and shulkers are scanned when opened.
- **Actions**: per rule, either strip the offending enchantment, downgrade it to the allowed level or delete the item. Each action is logged with player, item and rule.
- **Config** (`config/`): vanilla limits, whitelist for admin-approved custom items, per-world toggles, messages.
- **Commands**: `/aie reload`, `/aie scan <player>`, `/aie stats`.

## Build

```bash
mvn package          # runs the MockBukkit test suite, produces target/anti-illegal-enchants-1.0.0.jar
```

Requires Java 21 and a Paper 1.21 server.

## Layout

```
src/main/java/dev/sh3lby/antiillegalenchants/
  check/      EnchantCheckService, CheckResult
  action/     ItemScanService, actions
  listener/   join / pickup / inventory / open listeners
  config/     ConfigManager
  command/    /aie
  log/        action log
src/test/java/...  EnchantCheckServiceTest (MockBukkit)
docs/techtask.md   original spec (RU)
```

## License

MIT
