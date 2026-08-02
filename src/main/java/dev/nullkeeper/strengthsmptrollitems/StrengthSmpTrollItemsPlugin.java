package dev.nullkeeper.strengthsmptrollitems;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public class StrengthSmpTrollItemsPlugin extends JavaPlugin {
    private final ProtocolManager protocolManagerOverride;
    private RuntimeComponents runtime;

    public StrengthSmpTrollItemsPlugin() {
        this.protocolManagerOverride = null;
    }

    public StrengthSmpTrollItemsPlugin(ProtocolManager protocolManagerOverride) {
        this.protocolManagerOverride = Objects.requireNonNull(
                protocolManagerOverride,
                "protocolManagerOverride");
    }

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();
            ProtocolManager protocolManager = protocolManagerOverride == null
                    ? ProtocolLibrary.getProtocolManager()
                    : protocolManagerOverride;
            runtime = RuntimeComponents.start(this, protocolManager);
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
