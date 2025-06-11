package dev.antry.antrydeathloot.managers;

import dev.antry.antrydeathloot.AntryDeathLoot;
import lombok.extern.java.Log;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@Log
public class MessageManager {
    private static AntryDeathLoot plugin;

    public static void initialize(AntryDeathLoot main) {
        plugin = main;
        log.info("MessageManager initialized");
    }

    private static String getPrefix() {
        if (plugin == null || plugin.getPluginConfig() == null) {
            return "[AntryDeathLoot] ";
        }
        return ChatColor.translateAlternateColorCodes('&', plugin.getPluginConfig().getPrefix());
    }

    public static void sendDeathChestMessage(Player player) {
        if (plugin == null || player == null || plugin.getPluginConfig() == null) {
            return;
        }
        
        try {
            if (plugin.getPluginConfig().isAnnounceDeathChest()) {
                String message = plugin.getPluginConfig().getDeathChestMessage()
                    .replace("%player%", player.getName())
                    .replace("%time%", String.valueOf(plugin.getPluginConfig().getChestBreakTime()));
                broadcast(message);
            }
        } catch (Exception e) {
            log.warning("Error sending death chest message: " + e.getMessage());
        }
    }

    public static void sendBreakMessage() {
        if (plugin == null || plugin.getPluginConfig() == null) {
            return;
        }
        
        try {
            String breakMessage = plugin.getPluginConfig().getChestBreakMessage();
            if (breakMessage != null && !breakMessage.isEmpty()) {
                broadcast(breakMessage);
            }
        } catch (Exception e) {
            log.warning("Error sending break message: " + e.getMessage());
        }
    }

    private static void broadcast(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        
        try {
            message = getPrefix() + ChatColor.translateAlternateColorCodes('&', message);
            Bukkit.broadcastMessage(message);
        } catch (Exception e) {
            log.warning("Error broadcasting message: " + e.getMessage());
        }
    }
    
    /**
     * Clean up the message manager
     */
    public static void cleanup() {
        log.info("MessageManager cleanup completed");
        plugin = null;
    }
} 