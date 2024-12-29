package dev.antry.antrydeathloot.managers;

import dev.antry.antrydeathloot.AntryDeathLoot;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Effect;
import org.bukkit.entity.Item;
import org.bukkit.entity.FallingBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class DeathChestManager {
    private final AntryDeathLoot plugin;
    private final HashMap<Location, UUID> deathChests;
    private final HashMap<Location, List<ArmorStand>> chestHolograms;
    private final HashMap<Location, List<Integer>> breakTasks = new HashMap<>();

    public DeathChestManager(AntryDeathLoot plugin) {
        this.plugin = plugin;
        this.deathChests = new HashMap<>();
        this.chestHolograms = new HashMap<>();
    }

    private Location normalizeLocation(Location loc) {
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public void createDeathChest(Player player, Location location, List<ItemStack> items) {
        Location normalized = normalizeLocation(location);
        Block block = normalized.getBlock();
        block.setType(Material.CHEST);
        Chest chest = (Chest) block.getState();
        
        // Add items to chest
        if (!items.isEmpty()) {
            for (ItemStack item : items) {
                if (item != null) {
                    chest.getInventory().addItem(item);
                }
            }
        }

        // Store chest location
        deathChests.put(normalized, player.getUniqueId());

        // Create hologram
        if (plugin.getConfig().getBoolean("hologram.enabled", true)) {
            int breakTime = plugin.getConfig().getInt("chest-break-time", 10);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                List<ArmorStand> hologram = HologramManager.createHologram(normalized, player, breakTime);
                chestHolograms.put(normalized, hologram);
            }, 1L);
            
            scheduleChestBreak(normalized, breakTime);
        }
    }

    private void scheduleChestBreak(Location location, int breakTime) {
        List<Integer> taskIds = new ArrayList<>();
        
        // Update timer every second if holograms are enabled
        if (plugin.getConfig().getBoolean("hologram.enabled", true)) {
            for (int i = breakTime - 1; i > 0; i--) {
                final int seconds = i;
                int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    List<ArmorStand> hologram = chestHolograms.get(location);
                    if (hologram != null) {
                        HologramManager.updateTimer(hologram, seconds);
                    }
                }, (breakTime - i) * 20L).getTaskId();
                taskIds.add(taskId);
            }
        }

        // Store the final break task ID
        int finalTaskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (deathChests.containsKey(location)) {
                breakChest(location);
            }
        }, breakTime * 20L).getTaskId();
        taskIds.add(finalTaskId);
        
        breakTasks.put(location, taskIds);
    }

    public void cancelBreakTask(Location location) {
        Location normalized = normalizeLocation(location);
        List<Integer> taskIds = breakTasks.remove(normalized);
        if (taskIds != null) {
            for (Integer taskId : taskIds) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
        }
    }

    public void breakChest(Location location) {
        Location normalized = normalizeLocation(location);
        
        if (!deathChests.containsKey(normalized)) return;
        
        Block block = normalized.getBlock();
        if (block.getType() != Material.CHEST) return;
        
        Chest chest = (Chest) block.getState();
        
        // 1. Get and clear items
        ItemStack[] items = chest.getInventory().getContents();
        chest.getInventory().clear();
        
        // 2. Remove hologram
        List<ArmorStand> hologram = chestHolograms.remove(normalized);
        if (hologram != null) {
            HologramManager.removeHologram(hologram);
        }
        
        // 3. Break chest with effect
        block.getWorld().playEffect(block.getLocation(), Effect.SMOKE, 0);
        block.setType(Material.AIR);
        
        // 4. Drop items
        for (ItemStack item : items) {
            if (item != null) {
                Item droppedItem = normalized.getWorld().dropItemNaturally(normalized, item);
                droppedItem.setVelocity(droppedItem.getVelocity().multiply(0.5));
            }
        }
        
        // 5. Play sound and send message
        playBreakSound(normalized);
        MessageManager.sendBreakMessage();
        
        // 6. Clean up tracking
        deathChests.remove(normalized);
    }

    private void playBreakSound(Location location) {
        String soundName = plugin.getConfig().getString("chest-break-sound");
        if (soundName != null && !soundName.equalsIgnoreCase("NONE")) {
            try {
                Sound sound = Sound.valueOf(soundName);
                location.getWorld().playSound(location, sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid sound in config: " + soundName);
            }
        }
    }

    public void cleanup() {
        for (Location loc : new ArrayList<>(deathChests.keySet())) {
            Block block = loc.getBlock();
            if (block.getType() == Material.CHEST) {
                breakChest(loc);
            }
        }
        // Clean up any remaining holograms
        chestHolograms.values().forEach(HologramManager::removeHologram);
        chestHolograms.clear();
        deathChests.clear();
        breakTasks.clear();
    }

    public boolean isDeathChest(Location location) {
        return deathChests.containsKey(normalizeLocation(location));
    }
} 