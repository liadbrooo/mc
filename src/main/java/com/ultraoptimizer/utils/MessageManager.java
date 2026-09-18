package com.ultraoptimizer.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import com.ultraoptimizer.UltraOptimizerMain;

/**
 * MessageManager - Handles all messages with modern formatting
 */
public class MessageManager {

    private final UltraOptimizerMain plugin;
    private final MiniMessage miniMessage;

    // Color codes
    public static final String PRIMARY = "#FFD700";    // Gold
    public static final String SECONDARY = "#FFA500";  // Orange
    public static final String ACCENT = "#FF6347";     // Tomato
    public static final String SUCCESS = "#00FF00";    // Green
    public static final String ERROR = "#FF0000";      // Red
    public static final String INFO = "#00BFFF";       // Deep Sky Blue
    public static final String WARNING = "#FFD700";    // Gold
    public static final String GRAY = "#808080";       // Gray
    public static final String DARK_GRAY = "#404040";  // Dark Gray

    public MessageManager(UltraOptimizerMain plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    /**
     * Parse a MiniMessage string to Component
     * @param message The message to parse
     * @return Component
     */
    public Component parse(String message) {
        return miniMessage.deserialize(message);
    }

    /**
     * Get the prefix for messages
     * @return Component prefix
     */
    public Component getPrefix() {
        return parse("<gold><bold>[UltraOptimizer]</bold></gold> ");
    }

    /**
     * Send a message to a player
     * @param player The player to send to
     * @param message The message to send
     */
    public void sendMessage(Player player, String message) {
        player.sendMessage(getPrefix().append(parse(message)));
    }

    /**
     * Send a message to the console
     * @param message The message to send
     */
    public void sendConsole(String message) {
        Bukkit.getConsoleSender().sendMessage(getPrefix().append(parse(message)));
    }

    /**
     * Broadcast a message to all players with permission
     * @param message The message to broadcast
     * @param permission The required permission
     */
    public void broadcast(String message, String permission) {
        Component fullMessage = getPrefix().append(parse(message));
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(permission)) {
                player.sendMessage(fullMessage);
            }
        }
    }

    /**
     * Send an action bar message to a player
     * @param player The player to send to
     * @param message The message to send
     */
    public void sendActionBar(Player player, String message) {
        player.sendActionBar(parse(message));
    }

    /**
     * Send a title to a player
     * @param player The player to send to
     * @param title The title text
     * @param subtitle The subtitle text
     * @param fadeIn Fade in time (ticks)
     * @param stay Stay time (ticks)
     * @param fadeOut Fade out time (ticks)
     */
    public void sendTitle(Player player, String title, String subtitle, 
                          int fadeIn, int stay, int fadeOut) {
        player.showTitle(net.kyori.adventure.title.Title.title(
            parse(title),
            subtitle != null ? parse(subtitle) : Component.empty(),
            net.kyori.adventure.title.Title.Times.times(
                java.time.Duration.ofMillis(fadeIn * 50L),
                java.time.Duration.ofMillis(stay * 50L),
                java.time.Duration.ofMillis(fadeOut * 50L)
            )
        ));
    }

    /**
     * Format performance stats message
     * @param tps Current TPS
     * @param memoryUsed Memory used in MB
     * @param memoryMax Max memory in MB
     * @param entityCount Entity count
     * @return Formatted Component
     */
    public Component formatPerformanceStats(double tps, long memoryUsed, long memoryMax, int entityCount) {
        NamedTextColor tpsColor = tps >= 19.0 ? NamedTextColor.GREEN : 
                                  tps >= 18.0 ? NamedTextColor.YELLOW : NamedTextColor.RED;
        
        return parse("<gray>TPS: </gray>")
            .append(Component.text(String.format("%.2f", tps)).color(tpsColor))
            .append(parse(" <gray>| Memory: </gray>"))
            .append(Component.text(memoryUsed).color(NamedTextColor.GOLD))
            .append(parse("<gray>/</gray>"))
            .append(Component.text(memoryMax).color(NamedTextColor.GOLD))
            .append(parse("<gray>MB | Entities: </gray>"))
            .append(Component.text(entityCount).color(NamedTextColor.AQUA));
    }

    /**
     * Get error message component
     * @param message Error message
     * @return Component
     */
    public Component getError(String message) {
        return getPrefix().append(parse("<red>" + message + "</red>"));
    }

    /**
     * Get success message component
     * @param message Success message
     * @return Component
     */
    public Component getSuccess(String message) {
        return getPrefix().append(parse("<green>" + message + "</green>"));
    }

    /**
     * Get info message component
     * @param message Info message
     * @return Component
     */
    public Component getInfo(String message) {
        return getPrefix().append(parse("<aqua>" + message + "</aqua>"));
    }

    /**
     * Get warning message component
     * @param message Warning message
     * @return Component
     */
    public Component getWarning(String message) {
        return getPrefix().append(parse("<yellow>" + message + "</yellow>"));
    }
}
