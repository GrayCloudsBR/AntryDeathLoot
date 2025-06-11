package dev.antry.antrydeathloot;

import dev.antry.antrydeathloot.config.PluginConfig;
import dev.antry.antrydeathloot.managers.DeathChestManager;
import dev.antry.antrydeathloot.managers.MessageManager;
import dev.antry.antrydeathloot.managers.HologramManager;
import dev.antry.antrydeathloot.utils.VersionUtils;
import lombok.Getter;
import lombok.extern.java.Log;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.event.block.BlockDamageEvent;

import java.util.ArrayList;
import java.util.List;

@Log
public final class AntryDeathLoot extends JavaPlugin implements Listener {
    
    @Getter
    private DeathChestManager deathChestManager;
    
    @Getter
    private PluginConfig pluginConfig;
    
    private boolean isShuttingDown = false;

    @Override
    public void onEnable() {
        try {
            // Log version compatibility information
            VersionUtils.logVersionInfo();
            
            // Save default config
            saveDefaultConfig();
            
            // Load configuration
            this.pluginConfig = PluginConfig.fromFileConfiguration(getConfig(), getLogger());
            
            if (!pluginConfig.isValid()) {
                getLogger().severe("Invalid configuration detected! Please check your config.yml");
                setEnabled(false);
                return;
            }
            
            // Initialize managers
            this.deathChestManager = new DeathChestManager(this);
            MessageManager.initialize(this);
            HologramManager.initialize(this);
            
            // Register events
            getServer().getPluginManager().registerEvents(this, this);
            
            log.info("AntryDeathLoot v" + getDescription().getVersion() + " has been enabled!");
            log.info("Configuration loaded - Break time: " + pluginConfig.getChestBreakTime() + "s, " +
                    "Holograms: " + (pluginConfig.isHologramEnabled() ? "enabled" : "disabled") + ", " +
                    "Falling chests: " + (pluginConfig.isFallingChestEnabled() ? "enabled" : "disabled"));
                    
        } catch (Exception e) {
            log.severe("Failed to enable AntryDeathLoot: " + e.getMessage());
            e.printStackTrace();
            setEnabled(false);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (isShuttingDown || deathChestManager == null) {
            return;
        }
        
        try {
            // Create a copy of the drops before clearing them
            List<ItemStack> drops = new ArrayList<>(event.getDrops());
            
            // Clear default drops
            event.getDrops().clear();
            
            // Create death chest with the copied drops
            deathChestManager.createDeathChest(
                event.getEntity(),
                event.getEntity().getLocation(),
                drops
            );
            
            // Send message
            MessageManager.sendDeathChestMessage(event.getEntity());
        } catch (Exception e) {
            log.warning("Error handling player death for " + event.getEntity().getName() + ": " + e.getMessage());
            // Restore drops if chest creation failed
            if (event.getDrops().isEmpty() && !event.getDrops().isEmpty()) {
                event.getDrops().addAll(event.getDrops());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isShuttingDown || deathChestManager == null || event.getBlock().getType() != Material.CHEST) {
            return;
        }
        
        try {
            Location location = event.getBlock().getLocation();
            if (deathChestManager.isDeathChest(location)) {
                event.setCancelled(true);
                
                if (!pluginConfig.isAllowInstantBreak()) {
                    return;
                }
                
                deathChestManager.cancelBreakTask(location);
                Bukkit.getScheduler().runTask(this, () -> {
                    if (!isShuttingDown) {
                        deathChestManager.breakChest(location);
                    }
                });
            }
        } catch (Exception e) {
            log.warning("Error handling block break: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockDamage(BlockDamageEvent event) {
        if (isShuttingDown || deathChestManager == null || event.getBlock().getType() != Material.CHEST) {
            return;
        }
        
        try {
            Location location = event.getBlock().getLocation();
            if (deathChestManager.isDeathChest(location)) {
                if (!pluginConfig.isAllowInstantBreak()) {
                    return;
                }
                event.setInstaBreak(true);
            }
        } catch (Exception e) {
            log.warning("Error handling block damage: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        isShuttingDown = true;
        
        try {
            if (deathChestManager != null) {
                log.info("Cleaning up death chests...");
                deathChestManager.cleanup();
                deathChestManager = null;
            }
            
            // Clean up managers
            HologramManager.cleanup();
            MessageManager.cleanup();
            
            log.info("AntryDeathLoot has been disabled successfully!");
        } catch (Exception e) {
            log.severe("Error during plugin shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Check if the plugin is shutting down
     * @return true if the plugin is in shutdown process
     */
    public boolean isShuttingDown() {
        return isShuttingDown;
    }
}
