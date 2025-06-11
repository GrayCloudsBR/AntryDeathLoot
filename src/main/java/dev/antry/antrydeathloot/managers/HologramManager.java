package dev.antry.antrydeathloot.managers;

import dev.antry.antrydeathloot.AntryDeathLoot;
import lombok.extern.java.Log;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Log
public class HologramManager {
    private static AntryDeathLoot plugin;
    private static final Map<Location, List<ArmorStand>> activeHolograms = new ConcurrentHashMap<>();

    public static void initialize(AntryDeathLoot main) {
        plugin = main;
        log.info("HologramManager initialized");
    }

    public static List<ArmorStand> createHologram(Location location, Player player, int seconds) {
        List<ArmorStand> hologramLines = new ArrayList<>();
        
        if (plugin == null || plugin.getPluginConfig() == null || !plugin.getPluginConfig().isHologramEnabled()) {
            return hologramLines;
        }
        
        try {
            double height = plugin.getPluginConfig().getHologramHeight();
            double lineSpacing = plugin.getPluginConfig().getHologramLineSpacing();
            
            Location holoLoc = location.getBlock().getLocation().add(0.5, height, 0.5);
            holoLoc.setYaw(0);
            holoLoc.setPitch(0);
            
            String firstLine = plugin.getPluginConfig().getHologramFirstLine()
                                   .replace("%player%", player.getName());
            firstLine = ChatColor.translateAlternateColorCodes('&', firstLine);
            ArmorStand firstStand = spawnHologramLine(holoLoc.clone(), firstLine);
            if (firstStand != null) {
                hologramLines.add(firstStand);
            }
            
            String secondLine = plugin.getPluginConfig().getHologramSecondLine()
                                    .replace("%seconds%", String.valueOf(seconds));
            secondLine = ChatColor.translateAlternateColorCodes('&', secondLine);
            ArmorStand secondStand = spawnHologramLine(holoLoc.clone().subtract(0, lineSpacing, 0), secondLine);
            if (secondStand != null) {
                hologramLines.add(secondStand);
            }
            
            // Track the hologram for cleanup
            if (!hologramLines.isEmpty()) {
                activeHolograms.put(normalizeLocation(location), new ArrayList<>(hologramLines));
                log.fine("Created hologram with " + hologramLines.size() + " lines for " + player.getName());
            }
            
        } catch (Exception e) {
            log.warning("Error creating hologram: " + e.getMessage());
            // Clean up any partially created holograms
            removeHologram(hologramLines);
            hologramLines.clear();
        }
        
        return hologramLines;
    }

    private static ArmorStand spawnHologramLine(Location location, String text) {
        try {
            if (location.getWorld() == null) {
                log.warning("Cannot spawn hologram in null world");
                return null;
            }
            
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
        } catch (Exception e) {
            log.warning("Failed to spawn hologram line: " + e.getMessage());
            return null;
        }
    }

    public static void removeHologram(List<ArmorStand> hologramLines) {
        if (hologramLines == null) {
            return;
        }
        
        try {
            int removed = 0;
            for (ArmorStand stand : hologramLines) {
                if (stand != null && stand.isValid()) {
                    stand.remove();
                    removed++;
                }
            }
            hologramLines.clear();
            
            if (removed > 0) {
                log.fine("Removed " + removed + " hologram entities");
            }
        } catch (Exception e) {
            log.warning("Error removing hologram: " + e.getMessage());
        }
    }

    public static void updateTimer(List<ArmorStand> hologramLines, int seconds) {
        if (hologramLines == null || hologramLines.size() < 2 || plugin == null || plugin.getPluginConfig() == null) {
            return;
        }
        
        try {
            ArmorStand timerLine = hologramLines.get(1);
            if (timerLine != null && timerLine.isValid()) {
                String secondLine = plugin.getPluginConfig().getHologramSecondLine()
                                        .replace("%seconds%", String.valueOf(seconds));
                secondLine = ChatColor.translateAlternateColorCodes('&', secondLine);
                timerLine.setCustomName(secondLine);
            }
        } catch (Exception e) {
            log.warning("Error updating hologram timer: " + e.getMessage());
        }
    }
    
    private static Location normalizeLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return null;
        }
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
    
    /**
     * Clean up all active holograms and reset the manager
     */
    public static void cleanup() {
        try {
            log.info("Cleaning up " + activeHolograms.size() + " active holograms...");
            
            for (List<ArmorStand> hologram : activeHolograms.values()) {
                removeHologram(hologram);
            }
            activeHolograms.clear();
            
            log.info("Hologram cleanup completed.");
        } catch (Exception e) {
            log.severe("Error during hologram cleanup: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Remove a hologram from tracking when it's destroyed
     * @param location The location of the hologram to stop tracking
     */
    public static void untrackHologram(Location location) {
        Location normalized = normalizeLocation(location);
        if (normalized != null) {
            List<ArmorStand> removed = activeHolograms.remove(normalized);
            if (removed != null) {
                log.fine("Untracked hologram at " + normalized);
            }
        }
    }
    
    /**
     * Get the number of active holograms
     * @return count of active holograms
     */
    public static int getActiveHologramCount() {
        return activeHolograms.size();
    }
} 