package dev.sh3lby.antiillegalenchants.listener;

import dev.sh3lby.antiillegalenchants.AntiIllegalEnchantsPlugin;
import dev.sh3lby.antiillegalenchants.action.ScanContext;
import dev.sh3lby.antiillegalenchants.config.ConfigManager;
import dev.sh3lby.antiillegalenchants.action.ItemScanService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.ListIterator;

public final class PlayerListener implements Listener {
    private final AntiIllegalEnchantsPlugin plugin;
    private final ConfigManager configManager;
    private final ItemScanService scanService;

    public PlayerListener(AntiIllegalEnchantsPlugin plugin, ConfigManager configManager, ItemScanService scanService) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.scanService = scanService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!configManager.settings().checkOnJoin() || scanService.bypasses(event.getPlayer())) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> scanService.scanPlayer(event.getPlayer(), "PlayerJoin"));
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (!configManager.settings().checkOnDrop() || scanService.bypasses(event.getPlayer())) {
            return;
        }
        scanService.scanSlot(
            event.getItemDrop().getItemStack(),
            item -> {
                if (item == null) {
                    event.getItemDrop().remove();
                } else {
                    event.getItemDrop().setItemStack(item);
                }
            },
            new ScanContext(event.getPlayer(), "PlayerDrop", true)
        );
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!configManager.settings().checkOnDeath() || scanService.bypasses(player)) {
            return;
        }
        List<ItemStack> drops = event.getDrops();
        ListIterator<ItemStack> iterator = drops.listIterator();
        while (iterator.hasNext()) {
            ItemStack drop = iterator.next();
            scanService.scanSlot(drop, item -> {
                if (item == null) {
                    iterator.remove();
                } else {
                    iterator.set(item);
                }
            }, new ScanContext(player, "PlayerDeath", true));
        }
    }
}
