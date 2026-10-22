package dev.sh3lby.antiillegalenchants.config;

import dev.sh3lby.antiillegalenchants.action.IllegalItemAction;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

public final class ConfigManager {
    private final JavaPlugin plugin;
    private final Logger logger;
    private Settings settings;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        settings = new Settings(
            IllegalItemAction.fromString(config.getString("settings.action")),
            config.getBoolean("settings.notify-player", true),
            config.getBoolean("settings.notify-admins", true),
            config.getBoolean("settings.log-to-file", true),
            config.getBoolean("settings.check-on-join", true),
            config.getBoolean("settings.check-on-inventory-click", true),
            config.getBoolean("settings.check-on-pickup", true),
            config.getBoolean("settings.check-on-drop", true),
            config.getBoolean("settings.check-on-craft", true),
            config.getBoolean("settings.check-on-anvil", true),
            config.getBoolean("settings.check-on-grindstone", true),
            config.getBoolean("settings.check-on-villager-trade", true),
            config.getBoolean("settings.check-on-death", true),
            config.getBoolean("settings.check-dropped-items-on-chunk-load", true),
            config.getString("bypass.permission", "aie.bypass"),
            loadEnchantSet(config.getStringList("banned-enchants")),
            loadBannedCombinations(config.getList("banned-combinations")),
            loadMaxOverrides(config.getConfigurationSection("max-level-overrides")),
            loadWhitelist(config),
            config.getString("messages.player-item-deleted", "&cВаш предмет был удалён: обнаружены невозможные зачарования."),
            config.getString("messages.admin-alert", "&c[AntiIllegalEnchants] &f%player%: %item% удалён. Причина: %reason%."),
            config.getString("messages.reload-success", "&aAntiIllegalEnchants config reloaded.")
        );
    }

    public Settings settings() {
        return settings;
    }

    private Set<Enchantment> loadEnchantSet(List<String> keys) {
        Set<Enchantment> enchants = new HashSet<>();
        for (String key : keys) {
            parseEnchant(key).ifPresent(enchantment -> {
                if (enchantment == Enchantment.VANISHING_CURSE) {
                    logger.warning("Ignoring banned-enchants entry for vanishing_curse: it is always allowed.");
                    return;
                }
                enchants.add(enchantment);
            });
        }
        return enchants;
    }

    @SuppressWarnings("unchecked")
    private List<BannedCombination> loadBannedCombinations(List<?> rawList) {
        List<BannedCombination> combinations = new ArrayList<>();
        if (rawList == null) {
            return combinations;
        }
        for (Object raw : rawList) {
            if (!(raw instanceof List<?> values) || values.size() < 2) {
                logger.warning("Ignoring invalid banned-combinations entry: " + raw);
                continue;
            }
            Optional<Enchantment> first = parseEnchant(String.valueOf(values.get(0)));
            Optional<Enchantment> second = parseEnchant(String.valueOf(values.get(1)));
            if (first.isPresent() && second.isPresent()) {
                combinations.add(new BannedCombination(first.get(), second.get()));
            }
        }
        return combinations;
    }

    private Map<Enchantment, Integer> loadMaxOverrides(ConfigurationSection section) {
        Map<Enchantment, Integer> overrides = new HashMap<>();
        if (section == null) {
            return overrides;
        }
        for (String key : section.getKeys(false)) {
            parseEnchant(key).ifPresent(enchantment -> overrides.put(enchantment, section.getInt(key)));
        }
        return overrides;
    }

    private Whitelist loadWhitelist(FileConfiguration config) {
        Set<Material> materials = new HashSet<>();
        for (String name : config.getStringList("whitelist.materials")) {
            Material material = Material.matchMaterial(name);
            if (material == null) {
                logger.warning("Ignoring unknown whitelist material: " + name);
            } else {
                materials.add(material);
            }
        }

        Set<Integer> customModelData = new HashSet<>(config.getIntegerList("whitelist.custom-model-data"));
        Set<String> displayNames = new HashSet<>(config.getStringList("whitelist.display-names"));
        Set<NamespacedKey> pdcKeys = new HashSet<>();
        for (String raw : config.getStringList("whitelist.pdc-keys")) {
            NamespacedKey key = NamespacedKey.fromString(raw);
            if (key == null) {
                logger.warning("Ignoring invalid whitelist pdc key: " + raw);
            } else {
                pdcKeys.add(key);
            }
        }
        return new Whitelist(materials, customModelData, displayNames, pdcKeys);
    }

    private Optional<Enchantment> parseEnchant(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.toLowerCase(Locale.ROOT).replace("minecraft:", "");
        Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(normalized));
        if (enchantment == null) {
            logger.warning("Ignoring unknown enchantment: " + raw);
            return Optional.empty();
        }
        return Optional.of(enchantment);
    }

    public record Settings(
        IllegalItemAction action,
        boolean notifyPlayer,
        boolean notifyAdmins,
        boolean logToFile,
        boolean checkOnJoin,
        boolean checkOnInventoryClick,
        boolean checkOnPickup,
        boolean checkOnDrop,
        boolean checkOnCraft,
        boolean checkOnAnvil,
        boolean checkOnGrindstone,
        boolean checkOnVillagerTrade,
        boolean checkOnDeath,
        boolean checkDroppedItemsOnChunkLoad,
        String bypassPermission,
        Set<Enchantment> bannedEnchants,
        List<BannedCombination> bannedCombinations,
        Map<Enchantment, Integer> maxLevelOverrides,
        Whitelist whitelist,
        String playerItemDeletedMessage,
        String adminAlertMessage,
        String reloadSuccessMessage
    ) {
    }

    public record Whitelist(
        Set<Material> materials,
        Set<Integer> customModelData,
        Set<String> displayNames,
        Set<NamespacedKey> pdcKeys
    ) {
    }
}
