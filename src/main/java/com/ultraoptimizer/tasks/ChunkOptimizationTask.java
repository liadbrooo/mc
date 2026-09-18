package com.ultraoptimizer.tasks;

import com.ultraoptimizer.UltraOptimizerMain;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * ChunkOptimizationTask - Optimizes chunk loading and unloading
 */
public class ChunkOptimizationTask {

    private final UltraOptimizerMain plugin;
    private final boolean aggressiveUnload;
    private final int idleTimeoutSeconds;
    private final boolean spawnPreload;
    private final int spawnRadius;

    public ChunkOptimizationTask(UltraOptimizerMain plugin) {
        this.plugin = plugin;
        
        var config = plugin.getConfigManager().getConfig();
        this.aggressiveUnload = config.getBoolean("chunk-optimization.aggressive-unload.enabled", true);
        this.idleTimeoutSeconds = config.getInt("chunk-optimization.aggressive-unload.idle-timeout-seconds", 300);
        this.spawnPreload = config.getBoolean("chunk-optimization.spawn-preload.enabled", true);
        this.spawnRadius = config.getInt("chunk-optimization.spawn-preload.radius", 3);
    }

    /**
     * Start the chunk optimization task
     * @return BukkitTask
     */
    public BukkitTask start() {
        // Run every 60 seconds (1200 ticks)
        return Bukkit.getScheduler().runTaskTimer(plugin, this::optimize, 
            1200, 1200);
    }

    /**
     * Run optimization
     */
    private void optimize() {
        if (spawnPreload) {
            preloadSpawnChunks();
        }
        
        // Additional optimizations can be added here
        // For now, we just log chunk statistics
        logChunkStats();
    }

    /**
     * Pre-load chunks around spawn points
     */
    private void preloadSpawnChunks() {
        for (World world : Bukkit.getWorlds()) {
            if (!world.getName().contains("_nether") && !world.getName().contains("_end")) {
                int spawnX = world.getSpawnLocation().getBlockX() >> 4;
                int spawnZ = world.getSpawnLocation().getBlockZ() >> 4;
                
                for (int x = spawnX - spawnRadius; x <= spawnX + spawnRadius; x++) {
                    for (int z = spawnZ - spawnRadius; z <= spawnZ + spawnRadius; z++) {
                        // Check if chunk is loaded
                        if (!world.isChunkLoaded(x, z)) {
                            // Load chunk asynchronously
                            world.getChunkAtAsync(x, z).thenAccept(chunk -> {
                                // Chunk loaded successfully
                            });
                        }
                    }
                }
            }
        }
    }

    /**
     * Log chunk statistics
     */
    private void logChunkStats() {
        if (!plugin.getConfigManager().getConfig().getBoolean("debug", false)) {
            return;
        }
        
        int totalChunks = 0;
        List<String> worldChunkInfo = new ArrayList<>();
        
        for (World world : Bukkit.getWorlds()) {
            int chunkCount = world.getLoadedChunks().length;
            totalChunks += chunkCount;
            worldChunkInfo.add(world.getName() + ": " + chunkCount);
        }
        
        plugin.getLogger().fine("Chunk Statistics - Total: " + totalChunks + " | " + String.join(", ", worldChunkInfo));
    }

    /**
     * Get total loaded chunks across all worlds
     * @return Total chunk count
     */
    public int getTotalLoadedChunks() {
        int total = 0;
        for (World world : Bukkit.getWorlds()) {
            total += world.getLoadedChunks().length;
        }
        return total;
    }
}
