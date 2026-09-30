package dev.arenaonly;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class ArenaOnly extends JavaPlugin implements Listener {

    private final Set<String> allowedWorlds = new HashSet<>();
    private final Set<String> blocked = new HashSet<>();
    private String bypassPerm;
    private String denyMessage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        load();
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void load() {
        reloadConfig();
        allowedWorlds.clear();
        blocked.clear();
        getConfig().getStringList("allowed-worlds")
                .forEach(w -> allowedWorlds.add(w.toLowerCase(Locale.ROOT)));
        getConfig().getStringList("blocked-commands")
                .forEach(c -> blocked.add(c.toLowerCase(Locale.ROOT)));
        bypassPerm = getConfig().getString("bypass-permission", "arenaonly.bypass");
        denyMessage = getConfig().getString("deny-message", "&cThat only works in the arena!");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        if (p.hasPermission(bypassPerm)) return;

        String msg = e.getMessage();
        if (msg.length() < 2) return;
        String label = msg.substring(1).split("\\s+", 2)[0].toLowerCase(Locale.ROOT);

        if (!isBlocked(label)) return;
        if (allowedWorlds.contains(p.getWorld().getName().toLowerCase(Locale.ROOT))) return;

        e.setCancelled(true);
        String worlds = String.join(", ", allowedWorlds);
        p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                denyMessage.replace("%world%", worlds)));
    }

    private boolean isBlocked(String label) {
        // strip "plugin:" prefix
        int colon = label.indexOf(':');
        String bare = colon >= 0 ? label.substring(colon + 1) : label;
        if (blocked.contains(bare)) return true;

        // resolve real command + its aliases (catches aliases you didn't list)
        Command cmd = Bukkit.getCommandMap().getCommand(bare);
        if (cmd != null) {
            if (blocked.contains(cmd.getName().toLowerCase(Locale.ROOT))) return true;
            for (String alias : cmd.getAliases()) {
                if (blocked.contains(alias.toLowerCase(Locale.ROOT))) return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            load();
            sender.sendMessage(ChatColor.GREEN + "ArenaOnly config reloaded.");
            return true;
        }
        return false;
    }
}
