package com.ultraoptimizer.tasks;

import com.ultraoptimizer.UltraOptimizerMain;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * EntityOptimizationTask - Automatically removes excess entities
 */
public class EntityOptimizationTask {

    private final UltraOptimizerMain plugin;
    private final int checkInterval;
    private final int mobThreshold;
    private final int itemThreshold;
    private final int arrowThreshold;
    private final int expOrbThreshold;
    private final int minecartThreshold;
    private final int boatThreshold;
    private final List<String> blacklistedWorlds;
    private final boolean protectNamed;
    private final int protectionRadius;

    public EntityOptimizationTask(UltraOptimizerMain plugin) {
        this.plugin = plugin;
        
        var config = plugin.getConfigManager().getConfig();
        this.checkInterval = config.getInt("entity-optimization.auto-remove.check-interval-ticks", 100);
        this.mobThreshold = config.getInt("entity-optimization.auto-remove.thresholds.mobs", 200);
        this.itemThreshold = config.getInt("entity-optimization.auto-remove.thresholds.items", 100);
        this.arrowThreshold = config.getInt("entity-optimization.auto-remove.thresholds.arrows", 50);
        this.expOrbThreshold = config.getInt("entity-optimization.auto-remove.thresholds.experience-orbs", 30);
        this.minecartThreshold = config.getInt("entity-optimization.auto-remove.thresholds.minecarts", 20);
        this.boatThreshold = config.getInt("entity-optimization.auto-remove.thresholds.boats", 10);
        this.blacklistedWorlds = config.getStringList("entity-optimization.blacklisted-worlds");
        this.protectNamed = config.getBoolean("entity-optimization.protect-named-entities", true);
        this.protectionRadius = config.getInt("entity-optimization.protection-radius", 16);
    }

    /**
     * Start the optimization task
     * @return BukkitTask
     */
    public BukkitTask start() {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::optimize, 
            checkInterval, checkInterval);
    }

    /**
     * Run optimization
     */
    private void optimize() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            int totalRemoved = 0;
            
            for (World world : Bukkit.getWorlds()) {
                // Skip blacklisted worlds
                if (blacklistedWorlds.contains(world.getName())) {
                    continue;
                }
                
                totalRemoved += optimizeWorld(world);
            }
            
            if (totalRemoved > 0) {
                plugin.getPerformanceMonitor().incrementEntitiesRemoved(totalRemoved);
                plugin.getDatabaseManager().logOptimization("ENTITY_CLEANUP", totalRemoved, 
                    "Auto-cleanup: exceeded thresholds");
                
                if (plugin.getConfigManager().getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("Removed " + totalRemoved + " entities across all worlds.");
                }
            }
        });
    }

    /**
     * Optimize entities in a specific world
     * @param world The world to optimize
     * @return Number of entities removed
     */
    private int optimizeWorld(World world) {
        int removed = 0;
        
        // Count entities by type
        int mobCount = 0;
        int itemCount = 0;
        int arrowCount = 0;
        int expOrbCount = 0;
        int minecartCount = 0;
        int boatCount = 0;
        
        List<Entity> toRemove = new ArrayList<>();
        
        for (Entity entity : world.getEntities()) {
            // Skip players
            if (entity instanceof Player) continue;
            
            // Skip named entities if configured
            if (protectNamed && entity.getCustomName() != null) continue;
            
            // Skip entities near players
            if (isNearPlayer(entity)) continue;
            
            // Count and mark for removal
            if (entity instanceof Mob) {
                mobCount++;
                if (mobCount > mobThreshold) toRemove.add(entity);
            } else if (entity instanceof Item) {
                itemCount++;
                if (itemCount > itemThreshold) toRemove.add(entity);
            } else if (entity instanceof Arrow) {
                arrowCount++;
                if (arrowCount > arrowThreshold) toRemove.add(entity);
            } else if (entity instanceof ExperienceOrb) {
                expOrbCount++;
                if (expOrbCount > expOrbThreshold) toRemove.add(entity);
            } else if (entity instanceof Minecart) {
                minecartCount++;
                if (minecartCount > minecartThreshold) toRemove.add(entity);
            } else if (entity instanceof Boat) {
                boatCount++;
                if (boatCount > boatThreshold) toRemove.add(entity);
            }
        }
        
        // Remove marked entities
        for (Entity entity : toRemove) {
            if (entity.isValid()) {
                entity.remove();
                removed++;
            }
        }
        
        return removed;
    }

    /**
     * Check if entity is near a player
     * @param entity The entity to check
     * @return true if near player
     */
    private boolean isNearPlayer(Entity entity) {
        for (Player player : entity.getWorld().getPlayers()) {
            if (entity.getLocation().distance(player.getLocation()) <= protectionRadius) {
                return true;
            }
        }
        return false;
    }
}
