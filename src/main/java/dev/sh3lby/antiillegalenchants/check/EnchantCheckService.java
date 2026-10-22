package dev.sh3lby.antiillegalenchants.check;

import dev.sh3lby.antiillegalenchants.config.BannedCombination;
import dev.sh3lby.antiillegalenchants.config.ConfigManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EnchantCheckService {
    private final ConfigManager configManager;

    public EnchantCheckService(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public CheckResult check(ItemStack item) {
        return check(item, true);
    }

    public CheckResult checkAnvilResult(ItemStack item) {
        return check(item, false);
    }

    private CheckResult check(ItemStack item, boolean validateItemApplicability) {
        if (isEmpty(item) || item.getType() == Material.ELYTRA) {
            return CheckResult.legal();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || isWhitelisted(item, meta)) {
            return CheckResult.legal();
        }

        Map<Enchantment, Integer> enchants = getAllEnchantments(meta);
        enchants.remove(Enchantment.VANISHING_CURSE);
        if (enchants.isEmpty()) {
            return CheckResult.legal();
        }

        List<EnchantViolation> violations = new ArrayList<>();
        ConfigManager.Settings settings = configManager.settings();

        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            Enchantment enchantment = entry.getKey();
            int level = entry.getValue();

            if (settings.bannedEnchants().contains(enchantment)) {
                violations.add(new EnchantViolation(
                    ViolationType.BANNED_ENCHANT,
                    Set.of(enchantment),
                    enchantment.getKey().getKey() + " is banned"
                ));
            }

            int maxLevel = settings.maxLevelOverrides().getOrDefault(enchantment, enchantment.getMaxLevel());
            if (level > maxLevel) {
                violations.add(new EnchantViolation(
                    ViolationType.MAX_LEVEL_EXCEEDED,
                    Set.of(enchantment),
                    enchantment.getKey().getKey() + " " + level + " > " + maxLevel
                ));
            }

            if (validateItemApplicability && !(meta instanceof EnchantmentStorageMeta) && !enchantment.canEnchantItem(item)) {
                violations.add(new EnchantViolation(
                    ViolationType.INVALID_FOR_ITEM,
                    Set.of(enchantment),
                    enchantment.getKey().getKey() + " cannot enchant " + item.getType()
                ));
            }
        }

        List<Enchantment> keys = new ArrayList<>(enchants.keySet());
        for (int i = 0; i < keys.size(); i++) {
            for (int j = i + 1; j < keys.size(); j++) {
                Enchantment first = keys.get(i);
                Enchantment second = keys.get(j);
                if (first.conflictsWith(second) || second.conflictsWith(first)) {
                    violations.add(new EnchantViolation(
                        ViolationType.CONFLICTING_ENCHANTS,
                        Set.of(first, second),
                        first.getKey().getKey() + " conflicts with " + second.getKey().getKey()
                    ));
                }
                for (BannedCombination combination : settings.bannedCombinations()) {
                    if (combination.matches(first, second)) {
                        violations.add(new EnchantViolation(
                            ViolationType.BANNED_COMBINATION,
                            Set.of(first, second),
                            first.getKey().getKey() + " + " + second.getKey().getKey() + " is banned"
                        ));
                    }
                }
            }
        }

        return CheckResult.illegal(violations);
    }

    public Set<Enchantment> offendingEnchantments(CheckResult result) {
        Set<Enchantment> enchantments = new LinkedHashSet<>();
        for (EnchantViolation violation : result.violations()) {
            enchantments.addAll(violation.enchantments());
        }
        return enchantments;
    }

    public Map<Enchantment, Integer> getAllEnchantments(ItemMeta meta) {
        Map<Enchantment, Integer> enchants = new LinkedHashMap<>(meta.getEnchants());
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            enchants.putAll(storageMeta.getStoredEnchants());
        }
        return enchants;
    }

    public boolean hasNestedInventory(ItemStack item) {
        if (isEmpty(item) || item.getItemMeta() == null) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta instanceof BlockStateMeta blockStateMeta && blockStateMeta.getBlockState() instanceof ShulkerBox
            || meta instanceof BundleMeta;
    }

    public List<ItemStack> nestedItems(ItemStack item) {
        if (isEmpty(item) || item.getItemMeta() == null) {
            return List.of();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof BlockStateMeta blockStateMeta && blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox) {
            return new ArrayList<>(Arrays.asList(shulkerBox.getInventory().getContents()));
        }
        if (meta instanceof BundleMeta bundleMeta) {
            return bundleMeta.getItems();
        }
        return List.of();
    }

    public void replaceNestedItems(ItemStack item, List<ItemStack> contents) {
        if (isEmpty(item) || item.getItemMeta() == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof BlockStateMeta blockStateMeta && blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox) {
            shulkerBox.getInventory().setContents(contents.toArray(ItemStack[]::new));
            blockStateMeta.setBlockState(shulkerBox);
            item.setItemMeta(blockStateMeta);
        } else if (meta instanceof BundleMeta bundleMeta) {
            bundleMeta.setItems(contents.stream().filter(nested -> !isEmpty(nested)).toList());
            item.setItemMeta(bundleMeta);
        }
    }

    public boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR || item.getAmount() <= 0;
    }

    private boolean isWhitelisted(ItemStack item, ItemMeta meta) {
        ConfigManager.Whitelist whitelist = configManager.settings().whitelist();
        if (whitelist.materials().contains(item.getType())) {
            return true;
        }
        if (meta.hasCustomModelData() && whitelist.customModelData().contains(meta.getCustomModelData())) {
            return true;
        }
        if (meta.hasDisplayName()) {
            String legacyName = LegacyComponentSerializer.legacySection().serialize(meta.displayName());
            if (whitelist.displayNames().contains(legacyName)) {
                return true;
            }
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return whitelist.pdcKeys().stream().anyMatch(container::has);
    }
}
