package com.ultraoptimizer.commands;

import com.ultraoptimizer.UltraOptimizerMain;
import com.ultraoptimizer.utils.MessageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UltraOptimizerCommand - Main command handler for UltraOptimizer
 */
public class UltraOptimizerCommand implements CommandExecutor, TabCompleter {

    private final UltraOptimizerMain plugin;
    private final MessageManager messageManager;

    public UltraOptimizerCommand(UltraOptimizerMain plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "status":
                cmdStatus(sender);
                break;
            case "reload":
                cmdReload(sender);
                break;
            case "cleanup":
                cmdCleanup(sender, args);
                break;
            case "stats":
                cmdStats(sender);
                break;
            case "help":
                sendHelp(sender);
                break;
            case "version":
                cmdVersion(sender);
                break;
            default:
                messageManager.sendMessage(sender instanceof Player ? (Player) sender : null, 
                    "<red>Unknown command. Use /uo help for usage.</red>");
                break;
        }

        return true;
    }

    /**
     * Show server performance status
     */
    private void cmdStatus(CommandSender sender) {
        if (!sender.hasPermission("ultraoptimizer.admin")) {
            messageManager.sendMessage(sender instanceof Player ? (Player) sender : null,
                "<red>You don't have permission to use this command.</red>");
            return;
        }

        double tps = plugin.getPerformanceMonitor().getCurrentTPS();
        long memory = plugin.getPerformanceMonitor().getLastMemoryUsage();
        int entities = plugin.getPerformanceMonitor().getTotalEntities();
        int chunks = plugin.getPerformanceMonitor().getLoadedChunks();
        int players = plugin.getPerformanceMonitor().getOnlinePlayers();

        sender.sendMessage(Component.text("============================================", NamedTextColor.DARK_GRAY));
        sender.sendMessage(Component.text("        UltraOptimizer Status", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("============================================", NamedTextColor.DARK_GRAY));
        sender.sendMessage(messageManager.formatPerformanceStats(tps, memory, 
            Runtime.getRuntime().maxMemory() / (1024 * 1024), entities));
        sender.sendMessage(Component.text("Loaded Chunks: ", NamedTextColor.GRAY)
            .append(Component.text(chunks, NamedTextColor.AQUA)));
        sender.sendMessage(Component.text("Online Players: ", NamedTextColor.GRAY)
            .append(Component.text(players, NamedTextColor.AQUA)));
        sender.sendMessage(Component.text("Entities Removed: ", NamedTextColor.GRAY)
            .append(Component.text(plugin.getPerformanceMonitor().getTotalEntitiesRemoved(), NamedTextColor.GREEN)));
        sender.sendMessage(Component.text("Lag Spikes Prevented: ", NamedTextColor.GRAY)
            .append(Component.text(plugin.getPerformanceMonitor().getTotalLagSpikesPrevented(), NamedTextColor.GREEN)));
        sender.sendMessage(Component.text("Uptime: ", NamedTextColor.GRAY)
            .append(Component.text(plugin.getPerformanceMonitor().getFormattedUptime(), NamedTextColor.YELLOW)));
        sender.sendMessage(Component.text("============================================", NamedTextColor.DARK_GRAY));
    }

    /**
     * Reload configuration
     */
    private void cmdReload(CommandSender sender) {
        if (!sender.hasPermission("ultraoptimizer.admin")) {
            messageManager.sendMessage(sender instanceof Player ? (Player) sender : null,
                "<red>You don't have permission to use this command.</red>");
            return;
        }

        plugin.getConfigManager().reloadConfig();
        messageManager.sendConsole("<green>Configuration reloaded successfully!</green>");
        
        if (sender instanceof Player) {
            messageManager.sendMessage((Player) sender, "<green>Configuration reloaded!</green>");
        }
    }

    /**
     * Force entity cleanup
     */
    private void cmdCleanup(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ultraoptimizer.admin")) {
            messageManager.sendMessage(sender instanceof Player ? (Player) sender : null,
                "<red>You don't have permission to use this command.</red>");
            return;
        }

        int maxEntities = 100; // Default
        if (args.length > 1) {
            try {
                maxEntities = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                messageManager.sendMessage(sender instanceof Player ? (Player) sender : null,
                    "<red>Invalid number. Usage: /uo cleanup [amount]</red>");
                return;
            }
        }

        int removed = plugin.getAPI().forceGlobalEntityCleanup(maxEntities);
        messageManager.sendConsole("<green>Removed " + removed + " entities.</green>");
        
        if (sender instanceof Player) {
            messageManager.sendMessage((Player) sender, 
                "<green>Successfully removed </green><gold>" + removed + "</gold><green> entities.</green>");
        }
    }

    /**
     * Show detailed statistics
     */
    private void cmdStats(CommandSender sender) {
        if (!sender.hasPermission("ultraoptimizer.admin")) {
            messageManager.sendMessage(sender instanceof Player ? (Player) sender : null,
                "<red>You don't have permission to use this command.</red>");
            return;
        }

        sender.sendMessage(Component.text("============================================", NamedTextColor.DARK_GRAY));
        sender.sendMessage(Component.text("        Performance Statistics", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("============================================", NamedTextColor.DARK_GRAY));
        
        // Entity breakdown
        var entityCounts = plugin.getPerformanceMonitor().getEntityCountsByType();
        sender.sendMessage(Component.text("Top Entity Types:", NamedTextColor.YELLOW));
        
        entityCounts.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(10)
            .forEach(entry -> {
                sender.sendMessage(Component.text("  • ", NamedTextColor.GRAY)
                    .append(Component.text(entry.getKey(), NamedTextColor.WHITE))
                    .append(Component.text(": ", NamedTextColor.GRAY))
                    .append(Component.text(entry.getValue().toString(), NamedTextColor.AQUA)));
            });
        
        sender.sendMessage(Component.text("============================================", NamedTextColor.DARK_GRAY));
    }

    /**
     * Show version information
     */
    private void cmdVersion(CommandSender sender) {
        String version = plugin.getDescription().getVersion();
        String authors = String.join(", ", plugin.getDescription().getAuthors());
        
        sender.sendMessage(Component.text("============================================", NamedTextColor.DARK_GRAY));
        sender.sendMessage(Component.text("        UltraOptimizer", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("============================================", NamedTextColor.DARK_GRAY));
        sender.sendMessage(Component.text("Version: ", NamedTextColor.GRAY)
            .append(Component.text(version, NamedTextColor.GREEN)));
        sender.sendMessage(Component.text("Author: ", NamedTextColor.GRAY)
            .append(Component.text(authors, NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Status: ", NamedTextColor.GRAY)
            .append(Component.text(plugin.isEnabled() ? "Enabled" : "Disabled", 
                plugin.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED)));
        sender.sendMessage(Component.text("============================================", NamedTextColor.DARK_GRAY));
    }

    /**
     * Send help message
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("============================================", NamedTextColor.DARK_GRAY));
        sender.sendMessage(Component.text("        UltraOptimizer Help", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("============================================", NamedTextColor.DARK_GRAY));
        sender.sendMessage(Component.text("/uo status      ", NamedTextColor.YELLOW)
            .append(Component.text("- View server performance status", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/uo reload      ", NamedTextColor.YELLOW)
            .append(Component.text("- Reload configuration", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/uo cleanup [n] ", NamedTextColor.YELLOW)
            .append(Component.text("- Remove n entities (default: 100)", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/uo stats       ", NamedTextColor.YELLOW)
            .append(Component.text("- View detailed statistics", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/uo version     ", NamedTextColor.YELLOW)
            .append(Component.text("- Show plugin version", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/uo help        ", NamedTextColor.YELLOW)
            .append(Component.text("- Show this help message", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("============================================", NamedTextColor.DARK_GRAY));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = Arrays.asList("status", "reload", "cleanup", "stats", "help", "version");
            return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("cleanup")) {
            return Arrays.asList("50", "100", "200", "500");
        }
        
        return new ArrayList<>();
    }
}
