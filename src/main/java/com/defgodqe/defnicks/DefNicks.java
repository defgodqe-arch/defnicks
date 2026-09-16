package com.defgodqe.defnicks;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class DefNicks extends JavaPlugin implements CommandExecutor {
    private int attempts;

    @Override
    public void onEnable() {
        if (getCommand("defnicks") != null) {
            getCommand("defnicks").setExecutor(this);
        }

        // TAB and Essentials may finish loading after this plugin. Apply the
        // official TAB nickname configuration once both plugins are available.
        Bukkit.getScheduler().runTaskTimer(this, task -> {
            if (configureTab()) {
                task.cancel();
                return;
            }
            attempts++;
            if (attempts >= 30) {
                task.cancel();
                getLogger().warning("Could not configure TAB. Make sure TAB and Essentials are installed and enabled.");
            }
        }, 20L, 20L);
    }

    private boolean configureTab() {
        if (!Bukkit.getPluginManager().isPluginEnabled("TAB")) return false;
        if (!Bukkit.getPluginManager().isPluginEnabled("Essentials")) return false;

        // TAB officially supports Essentials' nickname placeholder in
        // customtabname. _DEFAULT_ applies to groups without an override.
        boolean success = Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "tab group _DEFAULT_ customtabname %essentials_nickname%"
        );

        if (success) {
            getLogger().info("TAB is now configured to display Essentials nicknames.");
        }
        return success;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("defnicks")) return false;

        if (!sender.hasPermission("defnicks.admin")) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }

        attempts = 0;
        if (configureTab()) {
            sender.sendMessage("defnicks: TAB is configured to use Essentials nicknames.");
        } else {
            sender.sendMessage("defnicks: TAB or Essentials is not enabled yet.");
        }
        return true;
    }
}
