package dev.antry.antrydeathloot.managers;

import dev.antry.antrydeathloot.AntryDeathLoot;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class HologramManager {
    private static AntryDeathLoot plugin;

    public static void initialize(AntryDeathLoot main) {
        plugin = main;
    }

    public static List<ArmorStand> createHologram(Location location, Player player, int seconds) {
        List<ArmorStand> hologramLines = new ArrayList<>();
        
        double height = plugin.getConfig().getDouble("hologram.height", 1.0);
        double lineSpacing = plugin.getConfig().getDouble("hologram.line-spacing", 0.3);
        
        Location holoLoc = location.getBlock().getLocation().add(0.5, height, 0.5);
        holoLoc.setYaw(0);
        holoLoc.setPitch(0);
        
        try {
            String firstLine = plugin.getConfig().getString("hologram.first-line", "&7%player%'s &fLoot")
                                   .replace("%player%", player.getName());
            firstLine = ChatColor.translateAlternateColorCodes('&', firstLine);
            hologramLines.add(spawnHologramLine(holoLoc.clone(), firstLine));
            
            String secondLine = plugin.getConfig().getString("hologram.second-line", "&fTime remaining: &c%seconds%s")
                                    .replace("%seconds%", String.valueOf(seconds));
            secondLine = ChatColor.translateAlternateColorCodes('&', secondLine);
            hologramLines.add(spawnHologramLine(holoLoc.clone().subtract(0, lineSpacing, 0), secondLine));
        } catch (Exception e) {
            plugin.getLogger().warning("Error creating hologram: " + e.getMessage());
        }
        
        return hologramLines;
    }

    private static ArmorStand spawnHologramLine(Location location, String text) {
        ArmorStand hologram = location.getWorld().spawn(location, ArmorStand.class);
        
        // Make the armor stand a perfect hologram
        hologram.setVisible(false);
        hologram.setGravity(false);
        hologram.setCanPickupItems(false);
        hologram.setCustomName(text);
        hologram.setCustomNameVisible(true);
        hologram.setMarker(true);
        hologram.setSmall(true);
        hologram.setBasePlate(false);
        hologram.setArms(false);
        
        return hologram;
    }

    public static void removeHologram(List<ArmorStand> hologramLines) {
        if (hologramLines != null) {
            hologramLines.forEach(ArmorStand::remove);
            hologramLines.clear();
        }
    }

    public static void updateTimer(List<ArmorStand> hologramLines, int seconds) {
        if (hologramLines != null && hologramLines.size() >= 2) {
            ArmorStand timerLine = hologramLines.get(1);
            String secondLine = plugin.getConfig().getString("hologram.second-line", "&fTime remaining: &c%seconds%s")
                                    .replace("%seconds%", String.valueOf(seconds));
            secondLine = ChatColor.translateAlternateColorCodes('&', secondLine);
            timerLine.setCustomName(secondLine);
        }
    }
} 