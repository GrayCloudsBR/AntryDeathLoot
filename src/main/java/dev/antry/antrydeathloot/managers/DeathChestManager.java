package dev.antry.antrydeathloot.managers;

import dev.antry.antrydeathloot.AntryDeathLoot;
import lombok.Getter;
import lombok.extern.java.Log;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.FallingBlock;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Effect;
import org.bukkit.entity.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Log
public class DeathChestManager {
    private final AntryDeathLoot plugin;
    
    @Getter
    private final ConcurrentHashMap<Location, UUID> deathChests;
    private final ConcurrentHashMap<Location, List<ArmorStand>> chestHolograms;
    private final ConcurrentHashMap<Location, List<Integer>> breakTasks;

    public DeathChestManager(AntryDeathLoot plugin) {
        this.plugin = plugin;
        this.deathChests = new ConcurrentHashMap<>();
        this.chestHolograms = new ConcurrentHashMap<>();
        this.breakTasks = new ConcurrentHashMap<>();
        
        log.info("DeathChestManager initialized");
    }

    private Location normalizeLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return null;
        }
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public void createDeathChest(Player player, Location location, List<ItemStack> items) {
        if (player == null || location == null || plugin.isShuttingDown() || plugin.getPluginConfig() == null) {
            return;
        }
        
        Location normalized = normalizeLocation(location);
        if (normalized == null) {
            log.warning("Cannot create death chest at invalid location");
            return;
        }
        
        try {
            // Create falling chest animation if enabled
            if (plugin.getPluginConfig().isFallingChestEnabled()) {
                createFallingChest(normalized, player, items);
            } else {
                createStaticChest(normalized, player, items);
            }
            
        } catch (Exception e) {
            log.warning("Error creating death chest for " + player.getName() + ": " + e.getMessage());
            // Clean up partial state
            deathChests.remove(normalized);
            cleanupChestResources(normalized);
        }
    }
    
    private void createFallingChest(Location location, Player player, List<ItemStack> items) {
        int fallHeight = plugin.getPluginConfig().getFallingChestHeight();
        Location fallLocation = location.clone().add(0, fallHeight, 0);
        
        // Create falling block
        FallingBlock fallingChest = fallLocation.getWorld().spawnFallingBlock(fallLocation, Material.CHEST, (byte) 0);
        fallingChest.setDropItem(false);
        
        // Monitor when it lands and create the actual chest
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                ticks++;
                
                // Safety check - cancel after 10 seconds
                if (ticks > 200 || fallingChest.isDead() || !fallingChest.isValid()) {
                    createStaticChest(location, player, items);
                    return;
                }
                
                // Check if it landed
                if (fallingChest.isOnGround() || Math.abs(fallingChest.getLocation().getY() - location.getY()) < 1) {
                    fallingChest.remove();
                    createStaticChest(location, player, items);
                }
            }
        }, 1L, 1L);
    }
    
    private void createStaticChest(Location location, Player player, List<ItemStack> items) {
        try {
            Block block = location.getBlock();
            block.setType(Material.CHEST);
            Chest chest = (Chest) block.getState();
            
            // Add items to chest
            if (items != null && !items.isEmpty()) {
                for (ItemStack item : items) {
                    if (item != null && item.getType() != Material.AIR) {
                        try {
                            chest.getInventory().addItem(item);
                        } catch (Exception e) {
                            log.warning("Failed to add item to death chest: " + e.getMessage());
                        }
                    }
                }
            }

            // Store chest location
            deathChests.put(location, player.getUniqueId());

            // Create hologram and schedule break
            if (plugin.getPluginConfig().isHologramEnabled()) {
                int breakTime = plugin.getPluginConfig().getChestBreakTime();
                
                // Create hologram after a short delay to ensure chest is fully created
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!plugin.isShuttingDown() && deathChests.containsKey(location)) {
                        try {
                            List<ArmorStand> hologram = HologramManager.createHologram(location, player, breakTime);
                            if (hologram != null && !hologram.isEmpty()) {
                                chestHolograms.put(location, hologram);
                            }
                        } catch (Exception e) {
                            log.warning("Failed to create hologram for death chest: " + e.getMessage());
                        }
                    }
                }, 1L);
                
                scheduleChestBreak(location, breakTime);
            }
            
            log.info("Created death chest for " + player.getName() + " at " + 
                     location.getWorld().getName() + " " + location.getBlockX() + 
                     "," + location.getBlockY() + "," + location.getBlockZ());
                     
        } catch (Exception e) {
            log.warning("Error creating static chest: " + e.getMessage());
            throw e; // Re-throw to trigger cleanup in parent method
        }
    }

    private void scheduleChestBreak(Location location, int breakTime) {
        if (location == null || breakTime <= 0 || plugin.isShuttingDown()) {
            return;
        }
        
        List<Integer> taskIds = new CopyOnWriteArrayList<>();
        
        try {
            // Update timer every second if holograms are enabled
            if (plugin.getPluginConfig().isHologramEnabled()) {
                for (int i = breakTime - 1; i > 0; i--) {
                    final int seconds = i;
                    int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (!plugin.isShuttingDown() && deathChests.containsKey(location)) {
                            try {
                                List<ArmorStand> hologram = chestHolograms.get(location);
                                if (hologram != null && !hologram.isEmpty()) {
                                    HologramManager.updateTimer(hologram, seconds);
                                }
                            } catch (Exception e) {
                                log.warning("Error updating hologram timer: " + e.getMessage());
                            }
                        }
                    }, (breakTime - i) * 20L).getTaskId();
                    taskIds.add(taskId);
                }
            }

            // Schedule the final break task
            int finalTaskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!plugin.isShuttingDown() && deathChests.containsKey(location)) {
                    breakChest(location);
                }
            }, breakTime * 20L).getTaskId();
            taskIds.add(finalTaskId);
            
            breakTasks.put(location, taskIds);
        } catch (Exception e) {
            log.warning("Error scheduling chest break: " + e.getMessage());
            // Clean up any tasks that were created
            for (Integer taskId : taskIds) {
                try {
                    Bukkit.getScheduler().cancelTask(taskId);
                } catch (Exception ignored) {}
            }
        }
    }

    public void cancelBreakTask(Location location) {
        Location normalized = normalizeLocation(location);
        if (normalized == null) {
            return;
        }
        
        List<Integer> taskIds = breakTasks.remove(normalized);
        if (taskIds != null) {
            for (Integer taskId : taskIds) {
                try {
                    if (taskId != null) {
                        Bukkit.getScheduler().cancelTask(taskId);
                    }
                } catch (Exception e) {
                    log.warning("Error cancelling break task: " + e.getMessage());
                }
            }
        }
    }

    public void breakChest(Location location) {
        Location normalized = normalizeLocation(location);
        if (normalized == null || !deathChests.containsKey(normalized)) {
            return;
        }
        
        try {
            Block block = normalized.getBlock();
            if (block.getType() != Material.CHEST) {
                // Chest was already broken somehow, just clean up tracking
                cleanupChestResources(normalized);
                return;
            }
            
            Chest chest = (Chest) block.getState();
            
            // 1. Get and clear items
            ItemStack[] items = chest.getInventory().getContents();
            chest.getInventory().clear();
            
            // 2. Clean up resources
            cleanupChestResources(normalized);
            
            // 3. Break chest with effect
            try {
                block.getWorld().playEffect(block.getLocation(), Effect.SMOKE, 0);
                block.setType(Material.AIR);
            } catch (Exception e) {
                log.warning("Error creating break effect: " + e.getMessage());
                // Still try to break the chest
                block.setType(Material.AIR);
            }
            
            // 4. Drop items
            for (ItemStack item : items) {
                if (item != null && item.getType() != Material.AIR) {
                    try {
                        Item droppedItem = normalized.getWorld().dropItemNaturally(normalized, item);
                        droppedItem.setVelocity(droppedItem.getVelocity().multiply(0.5));
                    } catch (Exception e) {
                        log.warning("Error dropping item from death chest: " + e.getMessage());
                    }
                }
            }
            
            // 5. Play sound and send message
            playBreakSound(normalized);
            MessageManager.sendBreakMessage();
            
            log.info("Death chest broken at " + normalized.getWorld().getName() + 
                     " " + normalized.getBlockX() + "," + normalized.getBlockY() + 
                     "," + normalized.getBlockZ());
                     
        } catch (Exception e) {
            log.warning("Error breaking death chest: " + e.getMessage());
            // Ensure cleanup happens even if breaking fails
            cleanupChestResources(normalized);
        }
    }
    
    private void cleanupChestResources(Location location) {
        if (location == null) {
            return;
        }
        
        try {
            // Cancel any break tasks
            cancelBreakTask(location);
            
            // Remove and clean up hologram
            List<ArmorStand> hologram = chestHolograms.remove(location);
            if (hologram != null) {
                HologramManager.removeHologram(hologram);
                HologramManager.untrackHologram(location);
            }
            
            // Remove from tracking
            deathChests.remove(location);
            
        } catch (Exception e) {
            log.warning("Error during chest resource cleanup: " + e.getMessage());
        }
    }

    private void playBreakSound(Location location) {
        if (location == null || location.getWorld() == null || plugin.getPluginConfig() == null) {
            return;
        }
        
        try {
            Sound sound = plugin.getPluginConfig().getChestBreakSound();
            if (sound != null) {
                location.getWorld().playSound(location, sound, 1.0f, 1.0f);
            }
        } catch (Exception e) {
            log.warning("Error playing break sound: " + e.getMessage());
        }
    }

    public void cleanup() {
        try {
            log.info("Cleaning up " + deathChests.size() + " death chests...");
            
            // Create a copy of locations to avoid concurrent modification
            List<Location> locations = new ArrayList<>(deathChests.keySet());
            
            for (Location loc : locations) {
                try {
                    Block block = loc.getBlock();
                    if (block.getType() == Material.CHEST) {
                        breakChest(loc);
                    } else {
                        // Just clean up resources if chest is already gone
                        cleanupChestResources(loc);
                    }
                } catch (Exception e) {
                    log.warning("Error cleaning up death chest at " + loc + ": " + e.getMessage());
                    // Force cleanup even if breaking fails
                    cleanupChestResources(loc);
                }
            }
            
            // Force clear all collections
            deathChests.clear();
            chestHolograms.clear();
            breakTasks.clear();
            
            log.info("Death chest cleanup completed.");
            
        } catch (Exception e) {
            log.severe("Error during death chest manager cleanup: " + e.getMessage());
            e.printStackTrace();
            
            // Force clear collections as last resort
            try {
                deathChests.clear();
                chestHolograms.clear();
                breakTasks.clear();
            } catch (Exception ignored) {}
        }
    }

    public boolean isDeathChest(Location location) {
        Location normalized = normalizeLocation(location);
        return normalized != null && deathChests.containsKey(normalized);
    }
    
    /**
     * Get the number of active death chests
     * @return number of active death chests
     */
    public int getActiveChestCount() {
        return deathChests.size();
    }
    
    /**
     * Get the owner of a death chest
     * @param location the location of the chest
     * @return the UUID of the owner, or null if not a death chest
     */
    public UUID getChestOwner(Location location) {
        Location normalized = normalizeLocation(location);
        return normalized != null ? deathChests.get(normalized) : null;
    }
} 