package com.ultraoptimizer.utils;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import com.ultraoptimizer.UltraOptimizerMain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PerformanceMonitor - Monitors server performance metrics
 */
public class PerformanceMonitor {

    private final UltraOptimizerMain plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    
    // Performance metrics
    private double currentTPS;
    private long lastMemoryUsage;
    private int totalEntities;
    private int loadedChunks;
    private int onlinePlayers;
    
    // History for averaging
    private final List<Double> tpsHistory = new ArrayList<>();
    private final Map<String, Integer> entityCountsByType = new HashMap<>();
    
    // Statistics
    private long totalEntitiesRemoved;
    private long totalLagSpikesPrevented;
    private long uptime;
    
    // Alert thresholds
    private double tpsAlertThreshold;
    private int memoryAlertThreshold;
    private int entityAlertThreshold;

    public PerformanceMonitor(UltraOptimizerMain plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messageManager = plugin.getMessageManager();
        
        // Load thresholds from config
        loadThresholds();
        
        this.uptime = System.currentTimeMillis();
    }

    /**
     * Load alert thresholds from config
     */
    private void loadThresholds() {
        tpsAlertThreshold = configManager.getConfig().getDouble("monitoring.tps.alert-threshold", 18.0);
        memoryAlertThreshold = configManager.getConfig().getInt("monitoring.memory.alert-threshold-percent", 85);
        entityAlertThreshold = configManager.getConfig().getInt("monitoring.entities.alert-threshold", 500);
    }

    /**
     * Update all performance metrics
     */
    public void updateMetrics() {
        updateTPS();
        updateMemory();
        updateEntityCount();
        updateChunkCount();
        updatePlayerCount();
    }

    /**
     * Calculate current TPS
     */
    private void updateTPS() {
        currentTPS = Bukkit.getServer().getTPS()[0]; // Overall TPS
        tpsHistory.add(currentTPS);
        
        // Keep only last 60 readings (1 hour at 1 minute intervals)
        if (tpsHistory.size() > 60) {
            tpsHistory.remove(0);
        }
        
        // Check for low TPS alert
        if (currentTPS < tpsAlertThreshold) {
            handleLowTPSAlert();
        }
    }

    /**
     * Update memory usage
     */
    private void updateMemory() {
        Runtime runtime = Runtime.getRuntime();
        lastMemoryUsage = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024); // MB
        
        // Check for high memory alert
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        int memoryPercent = (int) ((lastMemoryUsage * 100) / maxMemory);
        
        if (memoryPercent >= memoryAlertThreshold) {
            handleHighMemoryAlert(memoryPercent);
        }
    }

    /**
     * Update entity count
     */
    private void updateEntityCount() {
        AtomicInteger count = new AtomicInteger(0);
        entityCountsByType.clear();
        
        for (World world : Bukkit.getWorlds()) {
            // Skip blacklisted worlds
            if (configManager.getConfig().getStringList("entity-optimization.blacklisted-worlds")
                    .contains(world.getName())) {
                continue;
            }
            
            for (Entity entity : world.getEntities()) {
                count.incrementAndGet();
                
                // Count by type
                String type = entity.getType().name();
                entityCountsByType.put(type, entityCountsByType.getOrDefault(type, 0) + 1);
            }
        }
        
        totalEntities = count.get();
        
        // Check for high entity count alert
        if (totalEntities >= entityAlertThreshold) {
            handleHighEntityCountAlert();
        }
    }

    /**
     * Update loaded chunk count
     */
    private void updateChunkCount() {
        loadedChunks = 0;
        for (World world : Bukkit.getWorlds()) {
            loadedChunks += world.getLoadedChunks().length;
        }
    }

    /**
     * Update online player count
     */
    private void updatePlayerCount() {
        onlinePlayers = Bukkit.getOnlinePlayers().size();
    }

    /**
     * Handle low TPS alert
     */
    private void handleLowTPSAlert() {
        if (!configManager.getConfig().getBoolean("notifications.console", true)) {
            return;
        }
        
        String message = "<red>⚠ Low TPS detected: </red><gold>" + 
                        String.format("%.2f", currentTPS) + "</gold> <gray>(Threshold: " + 
                        tpsAlertThreshold + ")</gray>";
        
        messageManager.sendConsole(message);
        
        // Send to online admins
        if (configManager.getConfig().getBoolean("notifications.admin-chat", true)) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("ultraoptimizer.notify")) {
                    messageManager.sendMessage(player, message);
                }
            }
        }
    }

    /**
     * Handle high memory alert
     */
    private void handleHighMemoryAlert(int percent) {
        if (!configManager.getConfig().getBoolean("notifications.console", true)) {
            return;
        }
        
        String message = "<red>⚠ High memory usage: </red><gold>" + percent + "%</gold>";
        messageManager.sendConsole(message);
        
        // Send action bar alert if enabled
        if (configManager.getConfig().getBoolean("notifications.action-bar-alerts", true)) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("ultraoptimizer.notify")) {
                    messageManager.sendActionBar(player, "<red>⚠ High Memory Usage: " + percent + "%</red>");
                }
            }
        }
    }

    /**
     * Handle high entity count alert
     */
    private void handleHighEntityCountAlert() {
        if (!configManager.getConfig().getBoolean("notifications.console", true)) {
            return;
        }
        
        String message = "<yellow>⚠ High entity count: </yellow><gold>" + totalEntities + "</gold> <gray>(Threshold: " + 
                        entityAlertThreshold + ")</gray>";
        messageManager.sendConsole(message);
    }

    /**
     * Get average TPS over the last hour
     * @return Average TPS
     */
    public double getAverageTPS() {
        if (tpsHistory.isEmpty()) {
            return currentTPS;
        }
        
        return tpsHistory.stream().mapToDouble(Double::doubleValue).average().orElse(currentTPS);
    }

    /**
     * Get entity counts by type
     * @return Map of entity type to count
     */
    public Map<String, Integer> getEntityCountsByType() {
        return new HashMap<>(entityCountsByType);
    }

    /**
     * Save statistics (called on disable)
     */
    public void saveStatistics() {
        plugin.getLogger().info("=== Performance Statistics ===");
        plugin.getLogger().info("Total entities removed: " + totalEntitiesRemoved);
        plugin.getLogger().info("Total lag spikes prevented: " + totalLagSpikesPrevented);
        plugin.getLogger().info("Uptime: " + getFormattedUptime());
        plugin.getLogger().info("Average TPS: " + String.format("%.2f", getAverageTPS()));
        plugin.getLogger().info("==============================");
    }

    /**
     * Get formatted uptime string
     * @return Formatted uptime
     */
    public String getFormattedUptime() {
        long uptimeSeconds = (System.currentTimeMillis() - uptime) / 1000;
        long hours = uptimeSeconds / 3600;
        long minutes = (uptimeSeconds % 3600) / 60;
        long seconds = uptimeSeconds % 60;
        
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
    }

    // Getters
    public double getCurrentTPS() {
        return currentTPS;
    }

    public long getLastMemoryUsage() {
        return lastMemoryUsage;
    }

    public int getTotalEntities() {
        return totalEntities;
    }

    public int getLoadedChunks() {
        return loadedChunks;
    }

    public int getOnlinePlayers() {
        return onlinePlayers;
    }

    public long getTotalEntitiesRemoved() {
        return totalEntitiesRemoved;
    }

    public void incrementEntitiesRemoved(long amount) {
        totalEntitiesRemoved += amount;
    }

    public long getTotalLagSpikesPrevented() {
        return totalLagSpikesPrevented;
    }

    public void incrementLagSpikesPrevented() {
        totalLagSpikesPrevented++;
    }

    public long getUptime() {
        return uptime;
    }
}
