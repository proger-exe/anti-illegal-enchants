package dev.sh3lby.antiillegalenchants.listener;

import dev.sh3lby.antiillegalenchants.action.ItemScanService;
import dev.sh3lby.antiillegalenchants.action.ScanContext;
import dev.sh3lby.antiillegalenchants.config.ConfigManager;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.world.ChunkLoadEvent;

public final class EntityListener implements Listener {
    private final ConfigManager configManager;
    private final ItemScanService scanService;

    public EntityListener(ConfigManager configManager, ItemScanService scanService) {
        this.configManager = configManager;
        this.scanService = scanService;
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!configManager.settings().checkOnPickup()) {
            return;
        }
        Player player = event.getEntity() instanceof Player pickedBy ? pickedBy : null;
        if (scanService.bypasses(player)) {
            return;
        }
        scanService.scanSlot(
            event.getItem().getItemStack(),
            item -> {
                if (item == null) {
                    event.setCancelled(true);
                    event.getItem().remove();
                } else {
                    event.getItem().setItemStack(item);
                }
            },
            new ScanContext(player, "EntityPickup", true)
        );
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!configManager.settings().checkDroppedItemsOnChunkLoad()) {
            return;
        }
        Chunk chunk = event.getChunk();
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Item itemEntity) {
                scanService.scanSlot(
                    itemEntity.getItemStack(),
                    item -> {
                        if (item == null) {
                            itemEntity.remove();
                        } else {
                            itemEntity.setItemStack(item);
                        }
                    },
                    new ScanContext(null, "ChunkLoad", false)
                );
            }
        }
    }
}
