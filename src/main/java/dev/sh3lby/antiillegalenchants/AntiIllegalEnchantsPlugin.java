package dev.sh3lby.antiillegalenchants;

import dev.sh3lby.antiillegalenchants.action.IllegalItemActionService;
import dev.sh3lby.antiillegalenchants.action.ItemScanService;
import dev.sh3lby.antiillegalenchants.check.EnchantCheckService;
import dev.sh3lby.antiillegalenchants.command.AieCommand;
import dev.sh3lby.antiillegalenchants.config.ConfigManager;
import dev.sh3lby.antiillegalenchants.listener.EntityListener;
import dev.sh3lby.antiillegalenchants.listener.InventoryListener;
import dev.sh3lby.antiillegalenchants.listener.PlayerListener;
import dev.sh3lby.antiillegalenchants.log.IllegalEnchantLogger;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class AntiIllegalEnchantsPlugin extends JavaPlugin {
    private ConfigManager configManager;
    private EnchantCheckService checkService;
    private ItemScanService scanService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        configManager.load();

        checkService = new EnchantCheckService(configManager);
        IllegalItemActionService actionService = new IllegalItemActionService(checkService);
        IllegalEnchantLogger illegalEnchantLogger = new IllegalEnchantLogger(this, checkService);
        scanService = new ItemScanService(configManager, checkService, actionService, illegalEnchantLogger);

        getServer().getPluginManager().registerEvents(new PlayerListener(this, configManager, scanService), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this, configManager, scanService), this);
        getServer().getPluginManager().registerEvents(new EntityListener(configManager, scanService), this);

        AieCommand command = new AieCommand(configManager, checkService, scanService);
        PluginCommand pluginCommand = getCommand("aie");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }
    }

    public ConfigManager configManager() {
        return configManager;
    }

    public EnchantCheckService checkService() {
        return checkService;
    }

    public ItemScanService scanService() {
        return scanService;
    }
}
