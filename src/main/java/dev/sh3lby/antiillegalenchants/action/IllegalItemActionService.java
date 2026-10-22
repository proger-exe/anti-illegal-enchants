package dev.sh3lby.antiillegalenchants.action;

import dev.sh3lby.antiillegalenchants.check.CheckResult;
import dev.sh3lby.antiillegalenchants.check.EnchantCheckService;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class IllegalItemActionService {
    private final EnchantCheckService checkService;

    public IllegalItemActionService(EnchantCheckService checkService) {
        this.checkService = checkService;
    }

    public ItemStack apply(ItemStack item, CheckResult result, IllegalItemAction action) {
        if (checkService.isEmpty(item) || result.legalItem()) {
            return item;
        }
        return switch (action) {
            case DELETE -> null;
            case LOG_ONLY -> item;
            case REPLACE -> replaceClean(item);
            case STRIP -> stripIllegal(item, result);
        };
    }

    public ItemStack processNested(ItemStack item, IllegalItemAction action) {
        if (!checkService.hasNestedInventory(item)) {
            return item;
        }
        List<ItemStack> original = checkService.nestedItems(item);
        List<ItemStack> processed = new ArrayList<>();
        boolean changed = false;
        for (ItemStack nested : original) {
            CheckResult result = checkService.check(nested);
            ItemStack next = result.illegalItem() ? apply(nested, result, action) : nested;
            if (!checkService.isEmpty(next)) {
                ItemStack nestedProcessed = processNested(next, action);
                if (!sameItem(next, nestedProcessed)) {
                    changed = true;
                }
                next = nestedProcessed;
            }
            if (!sameItem(nested, next)) {
                changed = true;
            }
            processed.add(next);
        }
        if (changed) {
            checkService.replaceNestedItems(item, processed);
        }
        return item;
    }

    private boolean sameItem(ItemStack first, ItemStack second) {
        if (first == null || second == null) {
            return first == second;
        }
        return first.getAmount() == second.getAmount() && first.isSimilar(second);
    }

    private ItemStack replaceClean(ItemStack item) {
        ItemStack clean = new ItemStack(item.getType(), item.getAmount());
        return clean.getType() == Material.AIR ? null : clean;
    }

    private ItemStack stripIllegal(ItemStack item, CheckResult result) {
        ItemStack clone = item.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta == null) {
            return clone;
        }
        Set<Enchantment> offending = checkService.offendingEnchantments(result);
        for (Enchantment enchantment : offending) {
            meta.removeEnchant(enchantment);
            if (meta instanceof EnchantmentStorageMeta storageMeta) {
                storageMeta.removeStoredEnchant(enchantment);
            }
        }
        clone.setItemMeta(meta);
        return clone;
    }
}
