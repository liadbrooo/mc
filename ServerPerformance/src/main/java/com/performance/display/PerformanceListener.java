package com.performance.display;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PerformanceListener implements Listener {

    private final ServerPerformancePlugin plugin;

    public PerformanceListener(ServerPerformancePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Optional: Auto-start performance display if configured
        if (plugin.getConfig().getBoolean("auto-start", false)) {
            plugin.startDisplay(event.getPlayer());
        }
        
        // Send welcome message if enabled
        if (plugin.getConfig().getBoolean("join-message", true)) {
            event.getPlayer().sendMessage("§bWillkommen! Verwende §6/performance §bfür Server-Infos.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up any running tasks for this player
        plugin.stopDisplay(event.getPlayer());
    }
}
