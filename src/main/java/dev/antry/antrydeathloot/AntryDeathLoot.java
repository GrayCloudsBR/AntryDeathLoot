package dev.antry.antrydeathloot;

import dev.antry.antrydeathloot.managers.DeathChestManager;
import dev.antry.antrydeathloot.managers.MessageManager;
import dev.antry.antrydeathloot.managers.HologramManager;
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

public final class AntryDeathLoot extends JavaPlugin implements Listener {
    private DeathChestManager deathChestManager;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();
        
        // Initialize managers
        this.deathChestManager = new DeathChestManager(this);
        MessageManager.initialize(this);
        HologramManager.initialize(this);
        
        // Register events
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
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
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.CHEST) {
            Location location = event.getBlock().getLocation();
            if (deathChestManager.isDeathChest(location)) {
                event.setCancelled(true);
                
                if (!getConfig().getBoolean("allow-instant-break", true)) {
                    return;
                }
                
                deathChestManager.cancelBreakTask(location);
                Bukkit.getScheduler().runTask(this, () -> deathChestManager.breakChest(location));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockDamage(BlockDamageEvent event) {
        if (event.getBlock().getType() == Material.CHEST) {
            Location location = event.getBlock().getLocation();
            if (deathChestManager.isDeathChest(location)) {
                if (!getConfig().getBoolean("allow-instant-break", true)) {
                    return;
                }
                event.setInstaBreak(true);
            }
        }
    }

    @Override
    public void onDisable() {
        if (deathChestManager != null) {
            deathChestManager.cleanup();
        }
    }
}
