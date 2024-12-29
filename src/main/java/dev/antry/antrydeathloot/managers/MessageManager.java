package dev.antry.antrydeathloot.managers;

import dev.antry.antrydeathloot.AntryDeathLoot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class MessageManager {
    private static AntryDeathLoot plugin;

    public static void initialize(AntryDeathLoot main) {
        plugin = main;
    }

    private static String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfig().getString("prefix", "&f&l[&3&lAntryDeathLoot&f&l] "));
    }

    public static void sendDeathChestMessage(Player player) {
        if (plugin.getConfig().getBoolean("announce-death-chest")) {
            String message = plugin.getConfig().getString("death-chest-message", "&c%player%'s death chest has been created!");
            message = message.replace("%player%", player.getName())
                           .replace("%time%", String.valueOf(plugin.getConfig().getInt("chest-break-time", 10)));
            broadcast(message);
        }
    }

    public static void sendBreakMessage() {
        String breakMessage = plugin.getConfig().getString("chest-break-message");
        if (breakMessage != null && !breakMessage.isEmpty()) {
            broadcast(breakMessage);
        }
    }

    private static void broadcast(String message) {
        message = getPrefix() + ChatColor.translateAlternateColorCodes('&', message);
        Bukkit.broadcastMessage(message);
    }
} 