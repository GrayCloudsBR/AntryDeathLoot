package dev.antry.antrydeathloot.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.FallingBlock;
import org.bukkit.Location;

import java.util.logging.Logger;

/**
 * Utility class to handle version compatibility between Minecraft 1.8 and 1.21.5
 */
public class VersionUtils {
    private static final Logger logger = Bukkit.getLogger();
    private static final String VERSION = Bukkit.getVersion();
    private static final String BUKKIT_VERSION = Bukkit.getBukkitVersion();
    
    private static Boolean isLegacy = null;
    private static Boolean hasNewSoundSystem = null;
    private static Boolean hasNewMaterialSystem = null;
    
    /**
     * Check if we're running on a legacy version (< 1.13)
     */
    public static boolean isLegacyVersion() {
        if (isLegacy == null) {
            // For 1.7.10-1.12, we're always in legacy mode
            // Check if we're on a very old version by looking at the Bukkit version
            String bukkitVersion = BUKKIT_VERSION.toLowerCase();
            if (bukkitVersion.contains("1.7") || bukkitVersion.contains("1.8") || 
                bukkitVersion.contains("1.9") || bukkitVersion.contains("1.10") || 
                bukkitVersion.contains("1.11") || bukkitVersion.contains("1.12")) {
                isLegacy = true;
            } else {
                // For 1.13+, try to check for LEGACY_STONE
                try {
                    Material.valueOf("LEGACY_STONE");
                    isLegacy = false;
                } catch (IllegalArgumentException e) {
                    isLegacy = true;
                }
            }
        }
        return isLegacy;
    }
    
    /**
     * Check if the server has the new sound system (1.9+)
     */
    public static boolean hasNewSoundSystem() {
        if (hasNewSoundSystem == null) {
            String bukkitVersion = BUKKIT_VERSION.toLowerCase();
            // 1.7.10 has very limited sounds, 1.8+ has more, 1.9+ has the BLOCK_ prefix
            if (bukkitVersion.contains("1.7")) {
                hasNewSoundSystem = false;
            } else {
                try {
                    // Check if BLOCK_CHEST_OPEN exists (1.9+)
                    Sound.valueOf("BLOCK_CHEST_OPEN");
                    hasNewSoundSystem = true;
                } catch (IllegalArgumentException e) {
                    hasNewSoundSystem = false;
                }
            }
        }
        return hasNewSoundSystem;
    }
    
    /**
     * Check if the server has the new material system (1.13+)
     */
    public static boolean hasNewMaterialSystem() {
        if (hasNewMaterialSystem == null) {
            hasNewMaterialSystem = !isLegacyVersion();
        }
        return hasNewMaterialSystem;
    }
    
    /**
     * Get the chest open sound for the current version
     */
    public static Sound getChestOpenSound() {
        String bukkitVersion = BUKKIT_VERSION.toLowerCase();
        if (bukkitVersion.contains("1.7")) {
            // 1.7.10 has very limited sounds, use a basic one that exists
            try {
                return Sound.valueOf("CLICK");
            } catch (IllegalArgumentException e) {
                return null; // No sound available
            }
        } else if (hasNewSoundSystem()) {
            return Sound.valueOf("BLOCK_CHEST_OPEN");
        } else {
            return Sound.valueOf("CHEST_OPEN");
        }
    }
    
    /**
     * Get the chest close sound for the current version
     */
    public static Sound getChestCloseSound() {
        String bukkitVersion = BUKKIT_VERSION.toLowerCase();
        if (bukkitVersion.contains("1.7")) {
            // 1.7.10 has very limited sounds, use a basic one that exists
            try {
                return Sound.valueOf("CLICK");
            } catch (IllegalArgumentException e) {
                return null; // No sound available
            }
        } else if (hasNewSoundSystem()) {
            return Sound.valueOf("BLOCK_CHEST_CLOSE");
        } else {
            return Sound.valueOf("CHEST_CLOSE");
        }
    }
    
    /**
     * Get the block break sound for the current version
     */
    public static Sound getBlockBreakSound() {
        String bukkitVersion = BUKKIT_VERSION.toLowerCase();
        if (bukkitVersion.contains("1.7")) {
            // 1.7.10 has very limited sounds, use a basic one that exists
            try {
                return Sound.valueOf("DIG_WOOD");
            } catch (IllegalArgumentException e) {
                try {
                    return Sound.valueOf("CLICK");
                } catch (IllegalArgumentException e2) {
                    return null; // No sound available
                }
            }
        } else if (hasNewSoundSystem()) {
            return Sound.valueOf("BLOCK_WOOD_BREAK");
        } else {
            return Sound.valueOf("DIG_WOOD");
        }
    }
    
    /**
     * Create a falling block in a version-compatible way
     */
    @SuppressWarnings("deprecation")
    public static FallingBlock spawnFallingBlock(Location location, Material material) {
        try {
            // For all legacy versions (1.7.10-1.12), use the deprecated method with data value
            // The createBlockData() method doesn't exist in these versions
            return location.getWorld().spawnFallingBlock(location, material, (byte) 0);
        } catch (Exception e) {
            logger.warning("Failed to spawn falling block: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get version information string
     */
    public static String getVersionInfo() {
        return String.format("Bukkit: %s, Server: %s, Legacy: %s", 
                           BUKKIT_VERSION, VERSION, isLegacyVersion());
    }
    
    /**
     * Check if the current version supports a specific feature
     */
    public static boolean supportsFeature(String feature) {
        String bukkitVersion = BUKKIT_VERSION.toLowerCase();
        switch (feature.toLowerCase()) {
            case "armor_stands":
                // ArmorStands were added in 1.8, not available in 1.7.10
                return !bukkitVersion.contains("1.7");
            case "armor_stand_marker":
                // ArmorStand.setMarker() was added in 1.8.1
                return !bukkitVersion.contains("1.7");
            case "armor_stand_small":
                // ArmorStand.setSmall() was added in 1.8.1
                return !bukkitVersion.contains("1.7");
            case "custom_name_visible":
                // Custom names have been available since 1.8, but not on ArmorStands in 1.7.10
                return !bukkitVersion.contains("1.7");
            case "new_materials":
                return hasNewMaterialSystem();
            case "new_sounds":
                return hasNewSoundSystem();
            case "holograms":
                // Holograms using ArmorStands only work in 1.8+
                return !bukkitVersion.contains("1.7");
            default:
                return true;
        }
    }
    
    /**
     * Log version compatibility information
     */
    public static void logVersionInfo() {
        logger.info("=== AntryDeathLoot Version Compatibility ===");
        logger.info("Server version: " + VERSION);
        logger.info("Bukkit version: " + BUKKIT_VERSION);
        logger.info("Legacy mode: " + isLegacyVersion());
        logger.info("New sound system: " + hasNewSoundSystem());
        logger.info("New material system: " + hasNewMaterialSystem());
        logger.info("==========================================");
    }
} 