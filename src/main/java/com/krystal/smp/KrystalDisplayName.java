package com.krystal.smp;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KrystalDisplayName extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private final Map<UUID, String> nicknames = new ConcurrentHashMap<>();
    private final Map<UUID, TextDisplay> nametags = new ConcurrentHashMap<>();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final PlainTextComponentSerializer plainText = PlainTextComponentSerializer.plainText();
    private Scoreboard scoreboard;
    private Team hiddenNameTeam;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        createHiddenNameTeam();
        loadNicknames();
        Bukkit.getPluginManager().registerEvents(this, this);
        if (getCommand("nick") != null) {
            getCommand("nick").setExecutor(this);
            getCommand("nick").setTabCompleter(this);
        }
        Bukkit.getScheduler().runTaskTimer(this, this::updateNametags, 1L, 1L);
        for (Player player : Bukkit.getOnlinePlayers()) applyDisplay(player);
        getLogger().info("KrystalDisplayName enabled.");
    }

    @Override
    public void onDisable() {
        for (TextDisplay display : nametags.values()) if (display != null && !display.isDead()) display.remove();
        nametags.clear();
        restoreVanillaNameTags();
        saveNicknames();
    }

    private void loadNicknames() {
        nicknames.clear();
        if (!getConfig().isConfigurationSection("nicknames")) return;
        var section = getConfig().getConfigurationSection("nicknames");
        if (section == null) return;
        for (String uuidString : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                String nickname = section.getString(uuidString);
                if (nickname != null && !nickname.isBlank()) nicknames.put(uuid, nickname);
            } catch (IllegalArgumentException ignored) {
                getLogger().warning("Invalid UUID in nicknames: " + uuidString);
            }
        }
    }

    private void saveNicknames() {
        getConfig().set("nicknames", null);
        for (Map.Entry<UUID, String> entry : nicknames.entrySet()) getConfig().set("nicknames." + entry.getKey(), entry.getValue());
        saveConfig();
    }

    private Component getDisplayComponent(Player player) {
        String nickname = nicknames.get(player.getUniqueId());
        if (nickname == null || nickname.isBlank()) return Component.text(player.getName());
        if (!getConfig().getBoolean("allow-colors", true)) return Component.text(stripFormatting(nickname));
        try { return miniMessage.deserialize(nickname); }
        catch (Exception e) { return Component.text(stripFormatting(nickname)); }
    }

    private String stripFormatting(String text) {
        try { return plainText.serialize(miniMessage.deserialize(text)); }
        catch (Exception e) { return text.replaceAll("<[^>]*>", ""); }
    }

    private void applyDisplay(Player player) {
        Component display = getDisplayComponent(player);
        player.displayName(display);
        player.playerListName(getConfig().getBoolean("tab-enabled", true) ? display : Component.text(player.getName()));
        if (getConfig().getBoolean("nametag-enabled", true)) createOrUpdateNametag(player);
        if (getConfig().getBoolean("hide-vanilla-nametag", true)) hideVanillaNameTag(player);
    }

    private void createHiddenNameTeam() {
        hiddenNameTeam = scoreboard.getTeam("krystal_hidden");
        if (hiddenNameTeam == null) hiddenNameTeam = scoreboard.registerNewTeam("krystal_hidden");
        hiddenNameTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
    }

    private void hideVanillaNameTag(Player player) {
        if (hiddenNameTeam != null && !hiddenNameTeam.hasEntry(player.getName())) hiddenNameTeam.addEntry(player.getName());
    }

    private void restoreVanillaNameTags() {
        if (hiddenNameTeam == null) return;
        for (String entry : new ArrayList<>(hiddenNameTeam.getEntries())) hiddenNameTeam.removeEntry(entry);
    }

    private void createOrUpdateNametag(Player player) {
        TextDisplay display = nametags.get(player.getUniqueId());
        if (display == null || display.isDead() || !display.isValid()) {
            display = createNametag(player);
            nametags.put(player.getUniqueId(), display);
        }
        updateNametag(player, display);
    }

    private TextDisplay createNametag(Player player) {
        TextDisplay display = player.getWorld().spawn(getNametagLocation(player), TextDisplay.class);
        display.setBillboard(Display.Billboard.CENTER);
        display.setDefaultBackground(false);
        display.setShadowed(getConfig().getBoolean("nametag-shadow", true));
        display.setSeeThrough(getConfig().getBoolean("nametag-see-through", false));
        display.setLineWidth(400);
        display.setTextOpacity((byte) -1);
        display.setTransformationMatrix(new Matrix4f().scale(0.5f));
        display.setInvulnerable(true);
        display.setPersistent(false);
        return display;
    }

    private void updateNametag(Player player, TextDisplay display) {
        if (display.isDead() || !display.isValid()) return;
        if (!display.getWorld().equals(player.getWorld())) {
            display.remove();
            display = createNametag(player);
            nametags.put(player.getUniqueId(), display);
        }
        display.text(getDisplayComponent(player));
        display.teleport(getNametagLocation(player));
    }

    private Location getNametagLocation(Player player) {
        return player.getLocation().clone().add(0.0, getConfig().getDouble("nametag-height", 2.35), 0.0);
    }

    private void removeNametag(Player player) {
        TextDisplay display = nametags.remove(player.getUniqueId());
        if (display != null && !display.isDead()) display.remove();
        if (hiddenNameTeam != null) hiddenNameTeam.removeEntry(player.getName());
    }

    private void updateNametags() {
        if (!getConfig().getBoolean("nametag-enabled", true)) return;
        for (Player player : Bukkit.getOnlinePlayers()) createOrUpdateNametag(player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(this, () -> applyDisplay(event.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) { removeNametag(event.getPlayer()); }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (!getConfig().getBoolean("chat-enabled", true)) return;
        event.renderer((source, sourceDisplayName, message, viewer) -> getDisplayComponent(source).append(Component.text(": ")).append(message));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!getConfig().getBoolean("death-messages-enabled", true)) return;
        Component deathMessage = event.deathMessage();
        if (deathMessage == null) return;
        Player player = event.getPlayer();
        TextReplacementConfig replacement = TextReplacementConfig.builder()
                .matchLiteral(player.getName())
                .replacement(getDisplayComponent(player))
                .build();
        event.deathMessage(deathMessage.replaceText(replacement));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("nick")) return false;
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command."));
            return true;
        }
        if (!player.hasPermission("krystal.nick")) {
            player.sendMessage(Component.text("You do not have permission to change your nickname."));
            return true;
        }
        if (args.length == 0) { sendUsage(player); return true; }
        if (args.length == 1 && args[0].equalsIgnoreCase("reset")) {
            nicknames.remove(player.getUniqueId());
            saveNicknames();
            applyDisplay(player);
            player.sendMessage(Component.text("Your nickname has been reset."));
            return true;
        }
        String nickname = String.join(" ", args).trim();
        String plainNickname = stripFormatting(nickname);
        int maxLength = getConfig().getInt("max-length", 32);
        if (plainNickname.isBlank()) { player.sendMessage(Component.text("Your nickname cannot be empty.")); return true; }
        if (plainNickname.length() > maxLength) {
            player.sendMessage(Component.text("Your nickname is too long. Maximum: " + maxLength));
            return true;
        }
        if (!getConfig().getBoolean("allow-colors", true)) nickname = plainNickname;
        else try { miniMessage.deserialize(nickname); } catch (Exception e) {
            player.sendMessage(Component.text("That nickname contains invalid formatting.")); return true;
        }
        if (getConfig().getBoolean("require-unique-names", true)) {
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (!other.equals(player) && plainText.serialize(getDisplayComponent(other)).equalsIgnoreCase(plainNickname)) {
                    player.sendMessage(Component.text("That display name is already being used."));
                    return true;
                }
            }
        }
        nicknames.put(player.getUniqueId(), nickname);
        saveNicknames();
        applyDisplay(player);
        player.sendMessage(Component.text("Your display name is now: ").append(getDisplayComponent(player)));
        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text("Krystal SMP Nickname Commands"));
        player.sendMessage(Component.text("/nick <name>"));
        player.sendMessage(Component.text("/nick reset"));
        player.sendMessage(Component.text("Example: /nick Krystal"));
        player.sendMessage(Component.text("Gradient: /nick <gradient:#00ffff:#9b5cff>Krystal</gradient>"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("nick") || args.length != 1) return List.of();
        String input = args[0].toLowerCase(Locale.ROOT);
        return List.of("reset").stream().filter(v -> v.startsWith(input)).toList();
    }
}
