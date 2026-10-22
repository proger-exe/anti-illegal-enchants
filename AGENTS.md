# Repository Guidelines

## Project Structure & Module Organization

This repository is for the `AntiIllegalEnchants` Paper plugin described in `docs/techtask.md`. Keep that file as the product contract until code and docs exist.

Expected Java layout:

```text
src/main/java/dev/sh3lby/antiillegalenchants/
  AntiIllegalEnchantsPlugin.java
  config/
  check/
  action/
  listener/
  command/
  log/
src/main/resources/
  plugin.yml
  config.yml
src/test/java/
```

Keep enchant validation in `check`, item mutation in `action`, Bukkit event wiring in `listener`, commands in `command`, config parsing in `config`, and file logging in `log`. Avoid putting gameplay logic directly in the plugin main class.

## Build, Test, and Development Commands

No build scaffold exists yet. Prefer Maven for the initial Java/Paper setup unless the project later standardizes on another tool.

Useful commands once `pom.xml` exists:

```sh
mvn test
mvn -DskipTests package
```

`mvn test` runs the unit test suite. `mvn -DskipTests package` builds the plugin jar for local Paper testing.

## Coding Style & Naming Conventions

Use Java 21-compatible code for Minecraft/Paper 1.21.x. Use 4-space indentation, descriptive class names, and package-private helpers where public API is unnecessary. Name services by responsibility, for example `EnchantCheckService`, `IllegalItemActionService`, and `IllegalEnchantLogger`.

Prefer Bukkit/Paper APIs over ad hoc NBT parsing unless a feature explicitly needs lower-level item data. Keep config keys aligned with `docs/techtask.md`, using kebab-case YAML such as `banned-enchants` and `max-level-overrides`.

## Testing Guidelines

Add focused unit tests for enchant checks, banned combinations, whitelist behavior, and configured actions. Use names that describe behavior, for example `rejectsSharpnessAboveVanillaMax` or `stripsOnlyIllegalEnchantments`.

For Bukkit-heavy behavior, isolate pure logic behind services so it can be tested without a live server. Add regression tests for any bug fix that changes validation, shulker handling, config reload, or action behavior.

## Commit & Pull Request Guidelines

This folder has no usable git history yet, so use Conventional Commits going forward: `feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `build:`.

Pull requests should include a short summary, test evidence, and notes for any user-visible config or command changes. Update `docs/techtask.md` or add docs when behavior intentionally differs from the original spec.

## Security & Runtime Notes

Never scan all inventories every tick. Use event-based checks, manual `/aie scanall`, and chunk-load checks for dropped item entities. Do not call Bukkit APIs from async threads unless Paper explicitly documents the call as safe.
