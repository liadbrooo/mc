package com.ultraoptimizer.listeners;

import com.ultraoptimizer.UltraOptimizerMain;
import com.ultraoptimizer.utils.MessageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * PlayerJoinListener - Handles player join events
 */
public class PlayerJoinListener implements Listener {

    private final UltraOptimizerMain plugin;
    private final MessageManager messageManager;

    public PlayerJoinListener(UltraOptimizerMain plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Send notification to admins with permission
        if (player.hasPermission("ultraoptimizer.notify")) {
            // Schedule a small delay to ensure player is fully loaded
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                sendWelcomeMessage(player);
            }, 40L); // 2 seconds delay
        }
    }

    /**
     * Send welcome/status message to admin players
     * @param player The player to send to
     */
    private void sendWelcomeMessage(Player player) {
        if (!plugin.isEnabled()) return;
        
        // Send performance summary if TPS is low
        double tps = plugin.getPerformanceMonitor().getCurrentTPS();
        if (tps < 19.0) {
            messageManager.sendMessage(player, 
                "<yellow>⚠ Server performance is degraded!</yellow>");
            messageManager.sendMessage(player, 
                "<gray>Current TPS: </gray><red>" + String.format("%.2f", tps) + "</red>");
            messageManager.sendMessage(player, 
                "<gray>Use </gray><gold>/uo status</gold> <gray>for more information.</gray>");
        } else {
            // Send subtle status
            player.sendMessage(Component.text("[UltraOptimizer] ", NamedTextColor.GOLD)
                .append(Component.text("Server running optimally. ", NamedTextColor.GREEN))
                .append(Component.text("TPS: ", NamedTextColor.GRAY))
                .append(Component.text(String.format("%.2f", tps), NamedTextColor.GREEN)));
        }
    }
}
