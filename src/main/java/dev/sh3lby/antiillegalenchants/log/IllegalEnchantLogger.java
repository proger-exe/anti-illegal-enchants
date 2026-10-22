package dev.sh3lby.antiillegalenchants.log;

import dev.sh3lby.antiillegalenchants.action.IllegalItemAction;
import dev.sh3lby.antiillegalenchants.check.CheckResult;
import dev.sh3lby.antiillegalenchants.check.EnchantCheckService;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class IllegalEnchantLogger {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JavaPlugin plugin;
    private final EnchantCheckService checkService;
    private final Path logFile;

    public IllegalEnchantLogger(JavaPlugin plugin, EnchantCheckService checkService) {
        this.plugin = plugin;
        this.checkService = checkService;
        this.logFile = plugin.getDataFolder().toPath().resolve("illegal-enchants.log");
    }

    public void log(Player player, ItemStack item, CheckResult result, IllegalItemAction action, String source) {
        try {
            Files.createDirectories(logFile.getParent());
            Files.writeString(
                logFile,
                format(player, item, result, action, source) + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to write illegal enchant log: " + ex.getMessage());
        }
    }

    private String format(Player player, ItemStack item, CheckResult result, IllegalItemAction action, String source) {
        String playerName = player == null ? "unknown" : player.getName();
        UUID uuid = player == null ? null : player.getUniqueId();
        return "[" + FORMATTER.format(LocalDateTime.now()) + "]"
            + " Player=" + playerName
            + " UUID=" + (uuid == null ? "unknown" : uuid)
            + " Item=" + (item == null ? "AIR" : item.getType())
            + " Enchants=" + enchantments(item)
            + " Reason=" + result.summary()
            + " Action=" + action
            + " Source=" + source;
    }

    private String enchantments(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return "{}";
        }
        ItemMeta meta = item.getItemMeta();
        Map<Enchantment, Integer> enchants = checkService.getAllEnchantments(meta);
        return enchants.entrySet().stream()
            .map(entry -> entry.getKey().getKey().getKey() + "=" + entry.getValue())
            .collect(Collectors.joining(", ", "{", "}"));
    }
}
