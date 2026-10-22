package dev.sh3lby.antiillegalenchants.action;

import dev.sh3lby.antiillegalenchants.check.CheckResult;
import dev.sh3lby.antiillegalenchants.check.EnchantCheckService;
import dev.sh3lby.antiillegalenchants.config.ConfigManager;
import dev.sh3lby.antiillegalenchants.log.IllegalEnchantLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ItemScanService {
    private static final String ADMIN_PERMISSION = "aie.admin.notify";
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final ConfigManager configManager;
    private final EnchantCheckService checkService;
    private final IllegalItemActionService actionService;
    private final IllegalEnchantLogger logger;

    public ItemScanService(
        ConfigManager configManager,
        EnchantCheckService checkService,
        IllegalItemActionService actionService,
        IllegalEnchantLogger logger
    ) {
        this.configManager = configManager;
        this.checkService = checkService;
        this.actionService = actionService;
        this.logger = logger;
    }

    public ScanReport scanInventory(Player player, Inventory inventory, String source) {
        ScanReport report = new ScanReport();
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            int slot = i;
            scanSlot(contents[i], next -> inventory.setItem(slot, next), new ScanContext(player, source, true), report);
        }
        return report;
    }

    public ScanReport scanPlayer(Player player, String source) {
        ScanReport report = scanInventory(player, player.getInventory(), source);
        scanSlot(player.getItemOnCursor(), player::setItemOnCursor, new ScanContext(player, source + ":Cursor", true), report);
        return report;
    }

    public boolean scanSlot(Supplier<ItemStack> getter, Consumer<ItemStack> setter, ScanContext context) {
        return scanSlot(getter.get(), setter, context, new ScanReport());
    }

    public boolean scanSlot(ItemStack item, Consumer<ItemStack> setter, ScanContext context) {
        return scanSlot(item, setter, context, new ScanReport());
    }

    public boolean scanSlot(ItemStack item, Consumer<ItemStack> setter, ScanContext context, ScanReport report) {
        return scanSlot(item, setter, context, report, true);
    }

    public boolean scanAnvilResult(ItemStack item, Consumer<ItemStack> setter, ScanContext context) {
        return scanSlot(item, setter, context, new ScanReport(), false, IllegalItemAction.STRIP, false);
    }

    private boolean scanSlot(ItemStack item, Consumer<ItemStack> setter, ScanContext context, ScanReport report, boolean validateItemApplicability) {
        return scanSlot(item, setter, context, report, validateItemApplicability, configManager.settings().action(), true);
    }

    private boolean scanSlot(
        ItemStack item,
        Consumer<ItemStack> setter,
        ScanContext context,
        ScanReport report,
        boolean validateItemApplicability,
        IllegalItemAction action,
        boolean emitSideEffects
    ) {
        if (checkService.isEmpty(item)) {
            return false;
        }
        ItemStack original = item.clone();
        report.checked();
        ItemStack current = item;
        CheckResult result = validateItemApplicability ? checkService.check(current) : checkService.checkAnvilResult(current);
        if (result.illegalItem()) {
            report.illegal();
            if (emitSideEffects && configManager.settings().logToFile()) {
                logger.log(context.player(), current, result, action, context.source());
            }
            if (emitSideEffects) {
                notify(context.player(), current, result, context.notifyPlayer());
            }
            current = actionService.apply(current, result, action);
        }
        if (!checkService.isEmpty(current)) {
            current = actionService.processNested(current, action);
        }
        if (!sameItem(original, current)) {
            setter.accept(current);
            return true;
        }
        return false;
    }

    public boolean bypasses(Player player) {
        return player != null && player.hasPermission(configManager.settings().bypassPermission());
    }

    private void notify(Player player, ItemStack item, CheckResult result, boolean notifyPlayer) {
        ConfigManager.Settings settings = configManager.settings();
        if (player != null && notifyPlayer && settings.notifyPlayer()) {
            player.sendMessage(LEGACY.deserialize(settings.playerItemDeletedMessage()));
        }
        if (!settings.notifyAdmins()) {
            return;
        }
        String playerName = player == null ? "unknown" : player.getName();
        String message = settings.adminAlertMessage()
            .replace("%player%", playerName)
            .replace("%item%", item == null ? "AIR" : item.getType().name())
            .replace("%reason%", result.summary());
        Component component = LEGACY.deserialize(message);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission(ADMIN_PERMISSION)) {
                online.sendMessage(component);
            }
        }
    }

    private boolean sameItem(ItemStack first, ItemStack second) {
        if (first == null || second == null) {
            return first == second;
        }
        return first.getAmount() == second.getAmount() && first.isSimilar(second);
    }
}
