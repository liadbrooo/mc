package com.performance.display;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ServerPerformancePlugin extends JavaPlugin implements CommandExecutor {

    private final Map<UUID, BukkitTask> playerTasks = new HashMap<>();
    private int totalTicks = 0;
    private long totalTickTime = 0;
    
    // Colors for design
    private static final TextColor PRIMARY_COLOR = TextColor.color(0x00AAAA);
    private static final TextColor SECONDARY_COLOR = TextColor.color(0x008888);
    private static final TextColor ACCENT_COLOR = TextColor.color(0xFFAA00);
    private static final TextColor GOOD_COLOR = TextColor.color(0x00AA00);
    private static final TextColor WARNING_COLOR = TextColor.color(0xFFAA00);
    private static final TextColor DANGER_COLOR = TextColor.color(0xFF5555);

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        Objects.requireNonNull(getCommand("performance")).setExecutor(this);
        
        getServer().getPluginManager().registerEvents(new PerformanceListener(this), this);
        
        getLogger().info("§bServerPerformance Plugin wurde aktiviert!");
        getLogger().info("§7Verwende /performance für mehr Informationen.");
    }

    @Override
    public void onDisable() {
        for (BukkitTask task : playerTasks.values()) {
            task.cancel();
        }
        playerTasks.clear();
        getLogger().info("§cServerPerformance Plugin wurde deaktiviert!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Dieser Befehl kann nur von Spielern verwendet werden!", NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("performance.view")) {
            player.sendMessage(Component.text("Du hast keine Berechtigung dafür!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            toggleDisplay(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> startDisplay(player);
            case "stop" -> stopDisplay(player);
            case "info", "status" -> showDetailedInfo(player);
            case "help" -> showHelp(player);
            default -> showHelp(player);
        }

        return true;
    }

    private void toggleDisplay(Player player) {
        if (playerTasks.containsKey(player.getUniqueId())) {
            stopDisplay(player);
        } else {
            startDisplay(player);
        }
    }

    void startDisplay(Player player) {
        if (playerTasks.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("Die Performance-Anzeige läuft bereits!", WARNING_COLOR));
            return;
        }

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    playerTasks.remove(player.getUniqueId());
                    return;
                }
                displayPerformance(player);
            }
        }.runTaskTimer(this, 0L, 20L);

        playerTasks.put(player.getUniqueId(), task);
        
        player.sendMessage(Component.text()
            .append(Component.text("● ", GOOD_COLOR))
            .append(Component.text("Performance-Anzeige gestartet!", PRIMARY_COLOR))
            .build());
    }

    void stopDisplay(Player player) {
        BukkitTask task = playerTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            player.sendActionBar(Component.empty());
            player.sendMessage(Component.text()
                .append(Component.text("■ ", DANGER_COLOR))
                .append(Component.text("Performance-Anzeige gestoppt!", SECONDARY_COLOR))
                .build());
        } else {
            player.sendMessage(Component.text("Die Performance-Anzeige läuft nicht!", WARNING_COLOR));
        }
    }

    private void displayPerformance(Player player) {
        player.sendActionBar(createPerformanceDisplay(player));
    }

    private Component createPerformanceDisplay(Player player) {
        double tps = Math.min(20.0, Bukkit.getServer().getTPS()[0]);
        TextColor tpsColor = getPerformanceColor(tps, 20.0);
        
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        double memoryPercent = (usedMemory * 100.0) / maxMemory;
        TextColor memoryColor = getPerformanceColor(100 - memoryPercent, 100.0);
        
        double cpuUsage = getCpuUsage();
        TextColor cpuColor = getPerformanceColor(100 - cpuUsage, 100.0);
        
        int chunkCount = 0;
        int entityCount = 0;
        int playerCount = Bukkit.getOnlinePlayers().size();
        
        for (var world : Bukkit.getWorlds()) {
            chunkCount += world.getLoadedChunks().length;
            entityCount += world.getEntityCount();
        }
        
        double avgTickTime = tps > 0 ? Math.max(0, 50 - (tps * 2.5)) : 50;
        TextColor tickColor = getPerformanceColor(Math.max(0, 50 - avgTickTime), 50.0);
        
        return Component.text()
            .append(Component.text("▎ ", PRIMARY_COLOR))
            .append(Component.text("SERVER PERFORMANCE ", PRIMARY_COLOR))
            .append(Component.text("▎", PRIMARY_COLOR))
            .append(Component.text("  "))
            .append(Component.text("⚡ ", ACCENT_COLOR))
            .append(Component.text(String.format("%.1f", tps), tpsColor))
            .append(Component.text(" TPS ", NamedTextColor.GRAY))
            .append(Component.text("| ", NamedTextColor.DARK_GRAY))
            .append(Component.text("⏱ ", ACCENT_COLOR))
            .append(Component.text(String.format("%.1f", avgTickTime), tickColor))
            .append(Component.text("ms ", NamedTextColor.GRAY))
            .append(Component.text("| ", NamedTextColor.DARK_GRAY))
            .append(Component.text("💾 ", ACCENT_COLOR))
            .append(Component.text(formatBytes(usedMemory), memoryColor))
            .append(Component.text("/", NamedTextColor.GRAY))
            .append(Component.text(formatBytes(maxMemory), NamedTextColor.GRAY))
            .append(Component.text(" ", NamedTextColor.GRAY))
            .append(Component.text("| ", NamedTextColor.DARK_GRAY))
            .append(Component.text("🔥 ", ACCENT_COLOR))
            .append(Component.text(String.format("%.1f", cpuUsage), cpuColor))
            .append(Component.text("% CPU ", NamedTextColor.GRAY))
            .append(Component.text("| ", NamedTextColor.DARK_GRAY))
            .append(Component.text("📦 ", ACCENT_COLOR))
            .append(Component.text(entityCount, NamedTextColor.WHITE))
            .append(Component.text(" ", NamedTextColor.GRAY))
            .append(Component.text("| ", NamedTextColor.DARK_GRAY))
            .append(Component.text("🗺 ", ACCENT_COLOR))
            .append(Component.text(chunkCount, NamedTextColor.WHITE))
            .append(Component.text(" ", NamedTextColor.GRAY))
            .append(Component.text("| ", NamedTextColor.DARK_GRAY))
            .append(Component.text("👥 ", ACCENT_COLOR))
            .append(Component.text(playerCount, NamedTextColor.WHITE))
            .append(Component.text("/", NamedTextColor.GRAY))
            .append(Component.text(Bukkit.getMaxPlayers(), NamedTextColor.GRAY))
            .build();
    }

    private void showDetailedInfo(Player player) {
        player.sendMessage(Component.text()
            .append(Component.text("\n══════════════════════════════════════\n", NamedTextColor.DARK_GRAY))
            .append(Component.text("          SERVER PERFORMANCE INFO          \n", PRIMARY_COLOR))
            .append(Component.text("══════════════════════════════════════\n\n", NamedTextColor.DARK_GRAY))
            .build());

        double[] tpsValues = Bukkit.getServer().getTPS();
        player.sendMessage(createInfoLine("TPS (1min)", String.format("%.2f", tpsValues[0]), tpsValues[0], 20.0));
        player.sendMessage(createInfoLine("TPS (5min)", String.format("%.2f", tpsValues[1]), tpsValues[1], 20.0));
        player.sendMessage(createInfoLine("TPS (15min)", String.format("%.2f", tpsValues[2]), tpsValues[2], 20.0));

        Runtime runtime = Runtime.getRuntime();
        long usedMem = runtime.totalMemory() - runtime.freeMemory();
        long maxMem = runtime.maxMemory();
        double memPercent = (usedMem * 100.0) / maxMem;
        
        player.sendMessage(Component.text("\n  💾 SPEICHERNUTZUNG:", ACCENT_COLOR));
        player.sendMessage(createInfoLine("Belegt", formatBytes(usedMem), 100 - memPercent, 100.0));
        player.sendMessage(createInfoLine("Maximal", formatBytes(maxMem), 100.0, 100.0));
        player.sendMessage(createInfoLine("Frei", formatBytes(runtime.freeMemory()), memPercent, 100.0));

        double cpuUsage = getCpuUsage();
        player.sendMessage(Component.text("\n  🔥 PROZESSOR:", ACCENT_COLOR));
        player.sendMessage(createInfoLine("CPU Auslastung", String.format("%.1f%%", cpuUsage), 100 - cpuUsage, 100.0));
        
        player.sendMessage(Component.text("\n  🌍 WELTEN:", ACCENT_COLOR));
        for (var world : Bukkit.getWorlds()) {
            player.sendMessage(Component.text()
                .append(Component.text("    • ", NamedTextColor.GRAY))
                .append(Component.text(world.getName() + ": ", NamedTextColor.WHITE))
                .append(Component.text(world.getLoadedChunks().length + " Chunks, ", NamedTextColor.GRAY))
                .append(Component.text(world.getEntityCount() + " Entities", NamedTextColor.GRAY))
                .build());
        }

        player.sendMessage(Component.text("\n  ⚙️ SERVER:", ACCENT_COLOR));
        player.sendMessage(createInfoLine("Version", Bukkit.getVersion(), 100.0, 100.0));
        player.sendMessage(createInfoLine("Spieler", Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers(), 100.0, 100.0));
        
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        player.sendMessage(createInfoLine("OS", osBean.getName() + " (" + osBean.getArch() + ")", 100.0, 100.0));
        player.sendMessage(createInfoLine("Prozessoren", osBean.getAvailableProcessors() + " Kerne", 100.0, 100.0));

        player.sendMessage(Component.text("\n══════════════════════════════════════", NamedTextColor.DARK_GRAY));
    }

    private Component createInfoLine(String label, String value, double performance, double max) {
        TextColor color = getPerformanceColor(performance, max);
        return Component.text()
            .append(Component.text("    ", NamedTextColor.DARK_GRAY))
            .append(Component.text(label + ": ", NamedTextColor.GRAY))
            .append(Component.text(value, color))
            .append(Component.text("\n", NamedTextColor.GRAY))
            .build();
    }

    private void showHelp(Player player) {
        player.sendMessage(Component.text()
            .append(Component.text("\n══════════════════════════════════════\n", NamedTextColor.DARK_GRAY))
            .append(Component.text("          SERVER PERFORMANCE HELP          \n", PRIMARY_COLOR))
            .append(Component.text("══════════════════════════════════════\n\n", NamedTextColor.DARK_GRAY))
            .append(Component.text("  /performance §7- Anzeige umschalten\n", NamedTextColor.GOLD))
            .append(Component.text("  /performance start §7- Anzeige starten\n", NamedTextColor.GOLD))
            .append(Component.text("  /performance stop §7- Anzeige stoppen\n", NamedTextColor.GOLD))
            .append(Component.text("  /performance info §7- Detaillierte Infos\n", NamedTextColor.GOLD))
            .append(Component.text("  /performance help §7- Diese Hilfe\n", NamedTextColor.GOLD))
            .append(Component.text("\n══════════════════════════════════════", NamedTextColor.DARK_GRAY))
            .build());
    }

    private double getCpuUsage() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            double load = osBean.getSystemLoadAverage();
            int processors = osBean.getAvailableProcessors();
            if (load > 0 && processors > 0) {
                return Math.min(100.0, (load / processors) * 100.0);
            }
        } catch (Exception e) {
            // Fallback
        }
        double avgTickTime = totalTicks > 0 ? (double) totalTickTime / totalTicks : 0;
        return Math.min(100.0, avgTickTime * 2);
    }

    private TextColor getPerformanceColor(double value, double max) {
        double percent = (value / max) * 100.0;
        if (percent >= 90) return GOOD_COLOR;
        if (percent >= 70) return WARNING_COLOR;
        return DANGER_COLOR;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f MB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f GB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f TB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    public void recordTickTime(long tickTime) {
        totalTickTime += tickTime;
        totalTicks++;
        if (totalTicks > 1000) {
            totalTicks = 500;
            totalTickTime = totalTickTime / 2;
        }
    }
}
