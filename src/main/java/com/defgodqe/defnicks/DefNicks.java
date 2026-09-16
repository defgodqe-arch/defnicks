package com.defgodqe.defnicks;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class DefNicks extends JavaPlugin implements CommandExecutor {
    private int attempts;

    @Override
    public void onEnable() {
        if (getCommand("defnicks") != null) {
            getCommand("defnicks").setExecutor(this);
        }

        Bukkit.getScheduler().runTaskTimer(this, task -> {
            if (configureTab()) {
                task.cancel();
                return;
            }
            attempts++;
            if (attempts >= 60) {
                task.cancel();
                getLogger().warning("Required plugins: Essentials/EssentialsX, PlaceholderAPI, and TAB.");
                getLogger().warning("Run /papi ecloud download Essentials, then /papi reload.");
            }
        }, 20L, 20L);
    }

    private boolean configureTab() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Essentials")) return false;
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return false;
        if (!Bukkit.getPluginManager().isPluginEnabled("TAB")) return false;

        // Verify that PlaceholderAPI can resolve Essentials' nickname expansion.
        Player player = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (player != null) {
            String parsed = PlaceholderAPI.setPlaceholders(player, "%essentials_nickname%");
            if (parsed.equals("%essentials_nickname%")) {
                getLogger().warning("PlaceholderAPI is enabled, but the Essentials expansion is not loaded.");
                getLogger().warning("Run /papi ecloud download Essentials and then /papi reload.");
                return false;
            }
        }

        // TAB supports PlaceholderAPI placeholders in customtabname.
        boolean success = Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "tab group _DEFAULT_ customtabname %essentials_nickname%"
        );

        if (success) {
            getLogger().info("defnicks: TAB + PlaceholderAPI + Essentials nickname integration is enabled.");
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
            sender.sendMessage("§a[defnicks] TAB is now using Essentials nicknames through PlaceholderAPI.");
        } else {
            sender.sendMessage("§c[defnicks] Setup is incomplete. Check the console.");
        }
        return true;
    }
}
