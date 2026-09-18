package com.ultraoptimizer.tasks;

import com.ultraoptimizer.UltraOptimizerMain;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumSet;
import java.util.Set;

/**
 * MobAIOptimizationTask - Optimizes mob AI based on distance from players
 */
public class MobAIOptimizationTask {

    private final UltraOptimizerMain plugin;
    private final boolean enabled;
    private final int fullAIRadius;
    private final int reducedAIRadius;
    private final int noAIBeyond;
    private final Set<EntityType> reducedAIMobs;
    private final boolean reducePathfinding;
    private final int maxPathfindsPerTick;

    public MobAIOptimizationTask(UltraOptimizerMain plugin) {
        this.plugin = plugin;
        
        var config = plugin.getConfigManager().getConfig();
        this.enabled = config.getBoolean("mob-ai.enabled", true);
        this.fullAIRadius = config.getInt("mob-ai.distance-based-ai.full-ai-radius", 32);
        this.reducedAIRadius = config.getInt("mob-ai.distance-based-ai.reduced-ai-radius", 64);
        this.noAIBeyond = config.getInt("mob-ai.distance-based-ai.no-ai-beyond", 96);
        
        this.reducedAIMobs = EnumSet.noneOf(EntityType.class);
        for (String typeName : config.getStringList("mob-ai.reduced-ai-mobs")) {
            try {
                reducedAIMobs.add(EntityType.valueOf(typeName.toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Ignore invalid entity types
            }
        }
        
        this.reducePathfinding = config.getBoolean("mob-ai.pathfinding.reduce-updates", true);
        this.maxPathfindsPerTick = config.getInt("mob-ai.pathfinding.max-pathfinds-per-tick", 10);
    }

    /**
     * Start the mob AI optimization task
     * @return BukkitTask
     */
    public BukkitTask start() {
        if (!enabled) {
            return null;
        }
        
        // Run every 10 ticks (0.5 seconds)
        return Bukkit.getScheduler().runTaskTimer(plugin, this::optimize, 10, 10);
    }

    /**
     * Run optimization
     */
    private void optimize() {
        int pathfindCount = 0;
        
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof Mob)) continue;
                
                Mob mob = (Mob) entity;
                double distanceToNearestPlayer = getDistanceToNearestPlayer(mob);
                
                // Apply AI optimization based on distance
                if (distanceToNearestPlayer > noAIBeyond) {
                    // Disable AI completely for far mobs
                    mob.setAI(false);
                } else if (distanceToNearestPlayer > reducedAIRadius && 
                           reducedAIMobs.contains(mob.getType())) {
                    // Reduced AI for specific mob types at medium distance
                    mob.setAI(true); // Keep AI enabled but limit behavior
                    // Note: Full AI limitation would require NMS or PaperAPI
                } else if (distanceToNearestPlayer <= fullAIRadius) {
                    // Full AI for nearby mobs
                    mob.setAI(true);
                }
                
                // Limit pathfinding updates
                if (reducePathfinding && pathfindCount < maxPathfindsPerTick) {
                    // Pathfinding is handled internally by the server
                    // This is a placeholder for future advanced pathfinding control
                    pathfindCount++;
                }
            }
        }
        
        // Log optimization stats in debug mode
        if (plugin.getConfigManager().getConfig().getBoolean("debug", false)) {
            plugin.getLogger().fine("Mob AI optimization completed. Pathfinds: " + pathfindCount);
        }
    }

    /**
     * Get distance to nearest player
     * @param mob The mob to check
     * @return Distance in blocks, or Double.MAX_VALUE if no players in world
     */
    private double getDistanceToNearestPlayer(Mob mob) {
        double minDistance = Double.MAX_VALUE;
        
        for (Player player : mob.getWorld().getPlayers()) {
            double distance = mob.getLocation().distance(player.getLocation());
            if (distance < minDistance) {
                minDistance = distance;
            }
        }
        
        return minDistance;
    }

    /**
     * Manually disable AI for a specific entity
     * @param entity The entity
     */
    public void disableAIForEntity(Entity entity) {
        if (entity instanceof Mob) {
            ((Mob) entity).setAI(false);
        }
    }

    /**
     * Manually enable AI for a specific entity
     * @param entity The entity
     */
    public void enableAIForEntity(Entity entity) {
        if (entity instanceof Mob) {
            ((Mob) entity).setAI(true);
        }
    }
}
