package dev.nullkeeper.strengthsmptrollitems;

import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public class StrengthSmpTrollItemsPlugin extends JavaPlugin {
    private RuntimeComponents runtime;

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();
            runtime = RuntimeComponents.start(this);
        } catch (RuntimeException exception) {
            getLogger().log(Level.SEVERE, "Strength SMP Troll Items could not start", exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (runtime != null) {
            runtime.close();
            runtime = null;
        }
    }
}
