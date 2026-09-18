package com.ultraoptimizer.tasks;

import com.ultraoptimizer.UltraOptimizerMain;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

/**
 * MemoryManagementTask - Manages server memory and triggers GC hints
 */
public class MemoryManagementTask {

    private final UltraOptimizerMain plugin;
    private final int gcIntervalMinutes;
    private final boolean gcEnabled;

    public MemoryManagementTask(UltraOptimizerMain plugin) {
        this.plugin = plugin;
        this.gcEnabled = plugin.getConfigManager().getConfig()
            .getBoolean("memory-management.gc-hints.enabled", true);
        this.gcIntervalMinutes = plugin.getConfigManager().getConfig()
            .getInt("memory-management.gc-hints.interval-minutes", 10);
    }

    /**
     * Start the memory management task
     * @return BukkitTask
     */
    public BukkitTask start() {
        if (!gcEnabled) {
            return null;
        }
        
        // Convert minutes to ticks (20 ticks per second)
        int intervalTicks = gcIntervalMinutes * 60 * 20;
        
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::manageMemory, 
            intervalTicks, intervalTicks);
    }

    /**
     * Run memory management
     */
    private void manageMemory() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        int memoryPercent = (int) ((usedMemory * 100) / maxMemory);
        
        // Log memory status if high
        if (memoryPercent > 70) {
            String level = memoryPercent > 90 ? "§cCRITICAL" : memoryPercent > 80 ? "§eHIGH" : "§7Moderate";
            plugin.getLogger().info("Memory Usage: " + level + " §7(" + memoryPercent + "%)");
        }
        
        // Trigger GC hint if memory is very high
        if (memoryPercent > 85 && gcEnabled) {
            triggerGarbageCollection();
        }
    }

    /**
     * Trigger garbage collection hint
     */
    private void triggerGarbageCollection() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            // Clear some caches first
            System.gc();
            
            if (plugin.getConfigManager().getConfig().getBoolean("debug", false)) {
                Runtime runtime = Runtime.getRuntime();
                long freedMemory = runtime.totalMemory() - runtime.freeMemory();
                plugin.getLogger().info("GC triggered. Current memory usage: " + 
                    (freedMemory / (1024 * 1024)) + "MB");
            }
        });
    }
}
