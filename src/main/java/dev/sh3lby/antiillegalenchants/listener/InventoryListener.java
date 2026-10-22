package dev.sh3lby.antiillegalenchants.listener;

import dev.sh3lby.antiillegalenchants.AntiIllegalEnchantsPlugin;
import dev.sh3lby.antiillegalenchants.action.ItemScanService;
import dev.sh3lby.antiillegalenchants.action.ScanContext;
import dev.sh3lby.antiillegalenchants.config.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.inventory.ItemStack;

public final class InventoryListener implements Listener {
    private final AntiIllegalEnchantsPlugin plugin;
    private final ConfigManager configManager;
    private final ItemScanService scanService;

    public InventoryListener(AntiIllegalEnchantsPlugin plugin, ConfigManager configManager, ItemScanService scanService) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.scanService = scanService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!configManager.settings().checkOnInventoryClick() || !(event.getWhoClicked() instanceof Player player) || scanService.bypasses(player)) {
            return;
        }
        boolean changed = scanService.scanSlot(event.getCurrentItem(), event::setCurrentItem, new ScanContext(player, "InventoryClick:Current", true));
        changed |= scanService.scanSlot(event.getCursor(), event::setCursor, new ScanContext(player, "InventoryClick:Cursor", true));
        if (changed) {
            event.setCancelled(true);
            plugin.getServer().getScheduler().runTask(plugin, player::updateInventory);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!configManager.settings().checkOnInventoryClick() || !(event.getWhoClicked() instanceof Player player) || scanService.bypasses(player)) {
            return;
        }
        boolean changed = scanService.scanSlot(event.getCursor(), event::setCursor, new ScanContext(player, "InventoryDrag:Cursor", true));
        if (changed) {
            event.setCancelled(true);
            plugin.getServer().getScheduler().runTask(plugin, player::updateInventory);
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!configManager.settings().checkOnCraft() || !(event.getWhoClicked() instanceof Player player) || scanService.bypasses(player)) {
            return;
        }
        scanResult(event.getCurrentItem(), event::setCurrentItem, player, "Craft");
    }

    @EventHandler
    public void onAnvil(PrepareAnvilEvent event) {
        if (!configManager.settings().checkOnAnvil()) {
            return;
        }
        Player player = event.getViewers().stream()
            .filter(Player.class::isInstance)
            .map(Player.class::cast)
            .findFirst()
            .orElse(null);
        if (scanService.bypasses(player)) {
            return;
        }
        scanService.scanAnvilResult(event.getResult(), event::setResult, new ScanContext(player, "Anvil", true));
    }

    @EventHandler
    public void onGrindstone(PrepareGrindstoneEvent event) {
        if (!configManager.settings().checkOnGrindstone()) {
            return;
        }
        Player player = event.getViewers().stream()
            .filter(Player.class::isInstance)
            .map(Player.class::cast)
            .findFirst()
            .orElse(null);
        if (scanService.bypasses(player)) {
            return;
        }
        scanResult(event.getResult(), event::setResult, player, "Grindstone");
    }

    private void scanResult(ItemStack item, java.util.function.Consumer<ItemStack> setter, Player player, String source) {
        scanService.scanSlot(item, setter, new ScanContext(player, source, true));
    }
}
