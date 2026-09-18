package com.ultraoptimizer.tasks;

import com.ultraoptimizer.UltraOptimizerMain;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * AutoRestartTask - Handles automatic server restarts
 */
public class AutoRestartTask {

    private final UltraOptimizerMain plugin;
    private final boolean enabled;
    private final List<Map<String, Object>> scheduledRestarts;
    private final boolean ramThresholdEnabled;
    private final int ramThresholdPercent;
    private final int warningThresholdPercent;
    
    private boolean warningSent = false;
    private int countdownMinutes = -1;
    private LocalTime nextRestartTime = null;

    public AutoRestartTask(UltraOptimizerMain plugin) {
        this.plugin = plugin;
        
        var config = plugin.getConfigManager().getConfig();
        this.enabled = config.getBoolean("auto-restart.enabled", false);
        this.ramThresholdEnabled = config.getBoolean("auto-restart.ram-threshold.enabled", true);
        this.ramThresholdPercent = config.getInt("auto-restart.ram-threshold.threshold-percent", 90);
        this.warningThresholdPercent = config.getInt("auto-restart.ram-threshold.warning-threshold", 80);
        this.scheduledRestarts = (List<Map<String, Object>>) (List<?>) config.getMapList("auto-restart.scheduled-restarts");
    }

    /**
     * Start the auto-restart task
     * @return BukkitTask
     */
    public BukkitTask start() {
        if (!enabled) {
            return null;
        }
        
        // Check every minute (1200 ticks)
        return Bukkit.getScheduler().runTaskTimer(plugin, this::check, 1200, 1200);
    }

    /**
     * Check for restart conditions
     */
    private void check() {
        // Check scheduled restarts
        checkScheduledRestarts();
        
        // Check RAM threshold
        if (ramThresholdEnabled) {
            checkRAMThreshold();
        }
    }

    /**
     * Check scheduled restart times
     */
    private void checkScheduledRestarts() {
        LocalTime currentTime = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        
        for (Map<String, Object> restart : scheduledRestarts) {
            String timeStr = (String) restart.get("time");
            int warningMinutes = ((Number) restart.getOrDefault("delay-warning-minutes", 10)).intValue();
            
            try {
                LocalTime restartTime = LocalTime.parse(timeStr, formatter);
                
                // Check if it's time to warn (calculate warning time properly)
                LocalTime warningTime = restartTime.minusMinutes(warningMinutes);
                
                if (currentTime.getHour() == warningTime.getHour() && 
                    currentTime.getMinute() == warningTime.getMinute()) {
                    
                    sendRestartWarning(restartTime, warningMinutes);
                }
                
                // Check if it's time to restart
                if (currentTime.getHour() == restartTime.getHour() && 
                    currentTime.getMinute() == restartTime.getMinute()) {
                    
                    executeRestart();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid restart time format: " + timeStr);
            }
        }
    }

    /**
     * Check RAM usage threshold
     */
    private void checkRAMThreshold() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        int memoryPercent = (int) ((usedMemory * 100) / maxMemory);
        
        // Send warning if above warning threshold
        if (memoryPercent >= warningThresholdPercent && !warningSent) {
            plugin.getMessageManager().sendConsole(
                "<yellow>⚠ Memory usage is high: " + memoryPercent + "%</yellow>"
            );
            warningSent = true;
        }
        
        // Reset warning if below threshold
        if (memoryPercent < warningThresholdPercent) {
            warningSent = false;
        }
        
        // Trigger restart if above critical threshold
        if (memoryPercent >= ramThresholdPercent) {
            plugin.getMessageManager().sendConsole(
                "<red>🔄 Critical memory usage (" + memoryPercent + "%). Initiating restart...</red>"
            );
            executeRestart();
        }
    }

    /**
     * Send restart warning to all players
     * @param restartTime The scheduled restart time
     * @param minutes Minutes until restart
     */
    private void sendRestartWarning(LocalTime restartTime, int minutes) {
        String message = "<gold>⚠ Server restart in " + minutes + " minutes!</gold>";
        
        // Broadcast to all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getMessageManager().sendMessage(player, message);
            player.sendActionBar(Component.text("Server restarting in " + minutes + " minutes!", NamedTextColor.GOLD));
        }
        
        plugin.getMessageManager().sendConsole("Scheduled restart in " + minutes + " minutes.");
        
        // Schedule countdown
        scheduleCountdown(minutes);
    }

    /**
     * Schedule countdown messages
     * @param minutes Minutes until restart
     */
    private void scheduleCountdown(int minutes) {
        countdownMinutes = minutes;
        
        // Countdown at 5, 3, 1 minutes
        int[] countdownTimes = {5, 3, 1};
        
        for (int time : countdownTimes) {
            if (time <= minutes) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    String message = "<red>⚠ Server restarting in " + time + " minute" + 
                                    (time > 1 ? "s" : "") + "!</red>";
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        plugin.getMessageManager().sendMessage(player, message);
                    }
                }, (minutes - time) * 60 * 20L);
            }
        }
    }

    /**
     * Execute server restart
     */
    private void executeRestart() {
        plugin.getMessageManager().sendConsole("<green>Saving world and restarting...</green>");
        
        // Save all worlds
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kick(Component.text("Server restarting...", NamedTextColor.GOLD));
        }
        
        // Save worlds
        Bukkit.getWorlds().forEach(world -> world.save());
        
        // Dispatch restart command (requires server configuration)
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");
        
        // Fallback: stop server (if restart command not available)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getMessageManager().sendConsole("<yellow>Restart command not available. Stopping server...</yellow>");
            Bukkit.shutdown();
        }, 100L);
    }
}
