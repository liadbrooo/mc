package com.ultraoptimizer.api;

import com.ultraoptimizer.UltraOptimizerMain;
import com.ultraoptimizer.utils.PerformanceMonitor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.Collection;

/**
 * UltraOptimizerAPI - Public API for other plugins to interact with UltraOptimizer
 */
public class UltraOptimizerAPI {

    private final UltraOptimizerMain plugin;
    private final PerformanceMonitor performanceMonitor;

    public UltraOptimizerAPI(UltraOptimizerMain plugin) {
        this.plugin = plugin;
        this.performanceMonitor = plugin.getPerformanceMonitor();
    }

    /**
     * Get current server TPS
     * @return Current TPS
     */
    public double getCurrentTPS() {
        return performanceMonitor.getCurrentTPS();
    }

    /**
     * Get average TPS over the last hour
     * @return Average TPS
     */
    public double getAverageTPS() {
        return performanceMonitor.getAverageTPS();
    }

    /**
     * Get current memory usage in MB
     * @return Memory usage in MB
     */
    public long getMemoryUsage() {
        return performanceMonitor.getLastMemoryUsage();
    }

    /**
     * Get total entity count
     * @return Entity count
     */
    public int getEntityCount() {
        return performanceMonitor.getTotalEntities();
    }

    /**
     * Get entity count by type
     * @return Map of entity type to count
     */
    public java.util.Map<String, Integer> getEntityCountsByType() {
        return performanceMonitor.getEntityCountsByType();
    }

    /**
     * Get loaded chunk count
     * @return Chunk count
     */
    public int getLoadedChunks() {
        return performanceMonitor.getLoadedChunks();
    }

    /**
     * Get online player count
     * @return Player count
     */
    public int getOnlinePlayers() {
        return performanceMonitor.getOnlinePlayers();
    }

    /**
     * Check if server performance is optimal
     * @return true if TPS >= 19.5 and memory usage < 80%
     */
    public boolean isPerformanceOptimal() {
        return getCurrentTPS() >= 19.5 && getMemoryUsagePercent() < 80;
    }

    /**
     * Get memory usage percentage
     * @return Memory usage percent
     */
    public int getMemoryUsagePercent() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();
        return (int) ((used * 100) / max);
    }

    /**
     * Force entity cleanup in a specific world
     * @param world The world to clean up
     * @param maxEntities Maximum entities to remove
     * @return Number of entities removed
     */
    public int forceEntityCleanup(World world, int maxEntities) {
        Collection<Entity> entities = world.getEntities();
        int removed = 0;
        
        for (Entity entity : entities) {
            if (removed >= maxEntities) break;
            
            // Don't remove players or named entities
            if (entity instanceof org.bukkit.entity.Player) continue;
            if (entity.getCustomName() != null) continue;
            
            entity.remove();
            removed++;
        }
        
        performanceMonitor.incrementEntitiesRemoved(removed);
        return removed;
    }

    /**
     * Force entity cleanup across all worlds
     * @param maxEntities Maximum entities to remove
     * @return Number of entities removed
     */
    public int forceGlobalEntityCleanup(int maxEntities) {
        int removed = 0;
        
        for (World world : Bukkit.getWorlds()) {
            if (removed >= maxEntities) break;
            removed += forceEntityCleanup(world, maxEntities - removed);
        }
        
        return removed;
    }

    /**
     * Trigger garbage collection hint
     */
    public void triggerGarbageCollection() {
        System.gc();
        plugin.getLogger().info("Garbage collection triggered via API.");
    }

    /**
     * Get formatted performance summary
     * @return Performance summary string
     */
    public String getPerformanceSummary() {
        return String.format(
            "TPS: %.2f | Memory: %dMB | Entities: %d | Chunks: %d | Players: %d",
            getCurrentTPS(),
            getMemoryUsage(),
            getEntityCount(),
            getLoadedChunks(),
            getOnlinePlayers()
        );
    }

    /**
     * Check if optimization is enabled
     * @return true if enabled
     */
    public boolean isOptimizationEnabled() {
        return plugin.isEnabled();
    }

    /**
     * Get total entities removed since startup
     * @return Total entities removed
     */
    public long getTotalEntitiesRemoved() {
        return performanceMonitor.getTotalEntitiesRemoved();
    }

    /**
     * Get total lag spikes prevented since startup
     * @return Total lag spikes prevented
     */
    public long getTotalLagSpikesPrevented() {
        return performanceMonitor.getTotalLagSpikesPrevented();
    }

    /**
     * Get plugin uptime in milliseconds
     * @return Uptime in ms
     */
    public long getUptime() {
        return performanceMonitor.getUptime();
    }

    /**
     * Get formatted uptime string
     * @return Formatted uptime
     */
    public String getFormattedUptime() {
        return performanceMonitor.getFormattedUptime();
    }
}
