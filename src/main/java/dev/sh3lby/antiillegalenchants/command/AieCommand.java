package dev.sh3lby.antiillegalenchants.command;

import dev.sh3lby.antiillegalenchants.action.ItemScanService;
import dev.sh3lby.antiillegalenchants.action.ScanReport;
import dev.sh3lby.antiillegalenchants.check.CheckResult;
import dev.sh3lby.antiillegalenchants.check.EnchantCheckService;
import dev.sh3lby.antiillegalenchants.config.ConfigManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class AieCommand implements CommandExecutor, TabCompleter {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final ConfigManager configManager;
    private final EnchantCheckService checkService;
    private final ItemScanService scanService;

    public AieCommand(ConfigManager configManager, EnchantCheckService checkService, ItemScanService scanService) {
        this.configManager = configManager;
        this.checkService = checkService;
        this.scanService = scanService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "scan" -> scan(sender, label, args);
            case "scanall" -> scanAll(sender);
            case "reload" -> reload(sender);
            case "debug" -> debug(sender, label, args);
            default -> {
                sendUsage(sender, label);
                yield true;
            }
        };
    }

    private boolean scan(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("aie.admin.scan")) {
            sender.sendMessage("No permission.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("Usage: /" + label + " scan <player>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("Player not found: " + args[1]);
            return true;
        }
        ScanReport report = scanService.scanPlayer(target, "CommandScan");
        sender.sendMessage("Scanned " + target.getName() + ": checked=" + report.checkedCount() + " illegal=" + report.illegalCount());
        return true;
    }

    private boolean scanAll(CommandSender sender) {
        if (!sender.hasPermission("aie.admin.scanall")) {
            sender.sendMessage("No permission.");
            return true;
        }
        int players = 0;
        int checked = 0;
        int illegal = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (scanService.bypasses(player)) {
                continue;
            }
            ScanReport report = scanService.scanPlayer(player, "CommandScanAll");
            players++;
            checked += report.checkedCount();
            illegal += report.illegalCount();
        }
        sender.sendMessage("Scanned players=" + players + " checked=" + checked + " illegal=" + illegal);
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission("aie.admin.reload")) {
            sender.sendMessage("No permission.");
            return true;
        }
        configManager.load();
        sender.sendMessage(LEGACY.deserialize(configManager.settings().reloadSuccessMessage()));
        return true;
    }

    private boolean debug(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("aie.admin.debug")) {
            sender.sendMessage("No permission.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("Usage: /" + label + " debug <player>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("Player not found: " + args[1]);
            return true;
        }
        ItemStack item = target.getInventory().getItemInMainHand();
        CheckResult result = checkService.check(item);
        sender.sendMessage("Item=" + item.getType());
        sender.sendMessage("Enchants=" + enchantments(item));
        sender.sendMessage("Result=" + result.summary());
        return true;
    }

    private String enchantments(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return "{}";
        }
        return checkService.getAllEnchantments(meta).entrySet().stream()
            .map(entry -> entry.getKey().getKey().getKey() + "=" + entry.getValue())
            .collect(Collectors.joining(", ", "{", "}"));
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage("Usage: /" + label + " <scan|scanall|reload|debug>");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("scan", "scanall", "reload", "debug").stream()
                .filter(option -> option.startsWith(args[0].toLowerCase(Locale.ROOT)))
                .toList();
        }
        if (args.length == 2 && ("scan".equalsIgnoreCase(args[0]) || "debug".equalsIgnoreCase(args[0]))) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    names.add(player.getName());
                }
            }
            return names;
        }
        return List.of();
    }
}
