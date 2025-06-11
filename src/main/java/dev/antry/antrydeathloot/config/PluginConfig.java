package dev.antry.antrydeathloot.config;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Logger;

/**
 * Immutable configuration class using Lombok
 */
@Data
@Builder
public class PluginConfig {
    @NonNull
    private final String prefix;
    
    private final int chestBreakTime;
    private final boolean allowInstantBreak;
    private final boolean announceDeathChest;
    
    @NonNull
    private final String deathChestMessage;
    
    private final String chestBreakMessage;
    
    // Falling chest settings
    private final boolean fallingChestEnabled;
    private final int fallingChestHeight;
    
    // Hologram settings
    private final boolean hologramEnabled;
    private final double hologramHeight;
    private final double hologramLineSpacing;
    
    @NonNull
    private final String hologramFirstLine;
    
    @NonNull
    private final String hologramSecondLine;
    
    /**
     * Load configuration from Bukkit FileConfiguration
     * @param config the file configuration
     * @param logger the logger for warnings
     * @return PluginConfig instance
     */
    public static PluginConfig fromFileConfiguration(@NonNull FileConfiguration config, Logger logger) {
        PluginConfigBuilder builder = PluginConfig.builder()
            .prefix(config.getString("prefix", "&f&l[&3&lAntryDeathLoot&f&l] "))
            .chestBreakTime(config.getInt("chest-break-time", 10))
            .allowInstantBreak(config.getBoolean("allow-instant-break", true))
            .announceDeathChest(config.getBoolean("announce-death-chest", true))
            .deathChestMessage(config.getString("death-chest-message", "&c%player%'s death chest has been created! It will break in %time% seconds!"))
            .chestBreakMessage(config.getString("chest-break-message", "&cDeath chest is breaking!"))
            .fallingChestEnabled(config.getBoolean("falling-chest.enabled", true))
            .fallingChestHeight(config.getInt("falling-chest.height", 20))
            .hologramEnabled(config.getBoolean("hologram.enabled", true))
            .hologramHeight(config.getDouble("hologram.height", 1.0))
            .hologramLineSpacing(config.getDouble("hologram.line-spacing", 0.3))
            .hologramFirstLine(config.getString("hologram.first-line", "&7%player%'s &fLoot"))
            .hologramSecondLine(config.getString("hologram.second-line", "&fTime remaining: &c%seconds%s"));
        
        // Sound handling is now done through VersionUtils, no configuration needed
        
        return builder.build();
    }
    
    /**
     * Check if the configuration is valid
     * @return true if valid
     */
    public boolean isValid() {
        return chestBreakTime > 0 && 
               hologramHeight >= 0 && 
               hologramLineSpacing >= 0 && 
               fallingChestHeight > 0;
    }
} 