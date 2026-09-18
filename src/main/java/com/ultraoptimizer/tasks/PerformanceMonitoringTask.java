package com.ultraoptimizer.tasks;

import com.ultraoptimizer.UltraOptimizerMain;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

/**
 * PerformanceMonitoringTask - Monitors and logs server performance
 */
public class PerformanceMonitoringTask {

    private final UltraOptimizerMain plugin;
    private final int reportInterval;

    public PerformanceMonitoringTask(UltraOptimizerMain plugin) {
        this.plugin = plugin;
        this.reportInterval = plugin.getConfigManager().getConfig()
            .getInt("monitoring.tps.report-interval-ticks", 1200);
    }

    /**
     * Start the monitoring task
     * @return BukkitTask
     */
    public BukkitTask start() {
        return Bukkit.getScheduler().runTaskTimer(plugin, this::monitor, 
            reportInterval, reportInterval);
    }

    /**
     * Run monitoring
     */
    private void monitor() {
        // Update all metrics
        plugin.getPerformanceMonitor().updateMetrics();
        
        // Log to database
        var pm = plugin.getPerformanceMonitor();
        if (plugin.getDatabaseManager() != null && plugin.getDatabaseManager().isConnected()) {
            plugin.getDatabaseManager().logStatistics(
                pm.getCurrentTPS(),
                pm.getLastMemoryUsage(),
                pm.getTotalEntities(),
                pm.getLoadedChunks(),
                pm.getOnlinePlayers()
            );
        }
        
        // Send periodic report to console if TPS is low
        double tps = pm.getCurrentTPS();
        double avgTps = pm.getAverageTPS();
        
        if (tps < 19.0 || avgTps < 19.5) {
            String status = tps >= 19.0 ? "§7[Performance Report]" : "§c⚠ [Performance Alert]";
            Bukkit.getConsoleSender().sendMessage(status + 
                " TPS: §" + (tps >= 19.0 ? "a" : tps >= 18.0 ? "e" : "c") + 
                String.format("%.2f", tps) + " §7| Avg: §" + 
                (avgTps >= 19.0 ? "a" : avgTps >= 18.0 ? "e" : "c") +
                String.format("%.2f", avgTps) + 
                " §7| Entities: §b" + pm.getTotalEntities() +
                " §7| Memory: §6" + pm.getLastMemoryUsage() + "MB");
        }
    }
}
