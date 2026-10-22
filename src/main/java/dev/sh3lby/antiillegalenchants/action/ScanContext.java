package dev.sh3lby.antiillegalenchants.action;

import org.bukkit.entity.Player;

public record ScanContext(Player player, String source, boolean notifyPlayer) {
}
