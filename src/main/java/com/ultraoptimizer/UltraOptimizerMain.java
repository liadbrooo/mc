package com.ultraoptimizer;

import com.ultraoptimizer.commands.UltraOptimizerCommand;
import com.ultraoptimizer.listeners.PlayerJoinListener;
import com.ultraoptimizer.tasks.*;
import com.ultraoptimizer.utils.*;
import com.ultraoptimizer.database.DatabaseManager;
import com.ultraoptimizer.api.UltraOptimizerAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * UltraOptimizer - Modern High-Performance Server Optimizer
 * 
 * Features:
 * - Advanced Entity Optimization
 * - Chunk Management System
 * - Tile Entity Optimization
 * - Mob AI Optimization
 * - Redstone Optimization
 * - Network Packet Throttling
 * - Memory Management
 * - Auto-Restart System
 * - Real-time Performance Monitoring
 * - Modern GUI Interface
 * - Database Statistics
 * - REST API Support
 * 
 * @author UltraOptimizer
 * @version 1.0.0
 */
public class UltraOptimizerMain extends JavaPlugin {

    private static UltraOptimizerMain instance;
    private UltraOptimizerAPI api;
    private DatabaseManager databaseManager;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private PerformanceMonitor performanceMonitor;
    
    private final List<BukkitTask> activeTasks = new ArrayList<>();
    private boolean enabled = false;

    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers
        initializeManagers();
        
        // Check if plugin is enabled in config
        if (!configManager.getConfig().getBoolean("enabled", true)) {
            getLogger().warning("UltraOptimizer is disabled in config.yml!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Register commands
        registerCommands();
        
        // Register listeners
        registerListeners();
        
        // Initialize database
        if (configManager.getConfig().getBoolean("advanced.database.enabled", true)) {
            databaseManager = new DatabaseManager(this);
            databaseManager.initialize();
        }
        
        // Start optimization tasks
        startOptimizationTasks();
        
        // Initialize API
        api = new UltraOptimizerAPI(this);
        
        // Enable status
        enabled = true;
        
        // Send enable message
        sendEnableMessage();
        
        // Log startup
        getLogger().info("§6§lUltraOptimizer §r§7has been enabled successfully!");
        getLogger().info("§7Version: §f" + getDescription().getVersion());
        String author = getDescription().getAuthors().isEmpty() ? "Unknown" : getDescription().getAuthors().get(0);
        getLogger().info("§7Author: §f" + author);
    }

    @Override
    public void onDisable() {
        // Cancel all active tasks
        cancelAllTasks();
        
        // Close database connection
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        
        // Save statistics
        if (performanceMonitor != null) {
            performanceMonitor.saveStatistics();
        }
        
        // Send disable message
        if (enabled) {
            Bukkit.getConsoleSender().sendMessage(Component.text("UltraOptimizer has been disabled.", NamedTextColor.GRAY));
        }
        
        enabled = false;
    }

    private void initializeManagers() {
        // Save default config
        saveDefaultConfig();
        
        // Initialize config manager
        configManager = new ConfigManager(this);
        
        // Initialize message manager
        messageManager = new MessageManager(this);
        
        // Initialize performance monitor
        performanceMonitor = new PerformanceMonitor(this);
    }

    private void registerCommands() {
        UltraOptimizerCommand command = new UltraOptimizerCommand(this);
        getCommand("ultraoptimizer").setExecutor(command);
        getCommand("ultraoptimizer").setTabCompleter(command);
        getCommand("uo").setExecutor(command);
        getCommand("uo").setTabCompleter(command);
    }

    private void registerListeners() {
        // Player join listener for notifications
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        
        // Additional listeners will be added in future updates
        // - EntitySpawnListener
        // - ChunkLoadListener
        // - RedstoneListener
    }

    private void startOptimizationTasks() {
        // Entity optimization task
        if (configManager.getConfig().getBoolean("entity-optimization.enabled", true)) {
            activeTasks.add(new EntityOptimizationTask(this).start());
        }
        
        // Performance monitoring task
        if (configManager.getConfig().getBoolean("monitoring.enabled", true)) {
            activeTasks.add(new PerformanceMonitoringTask(this).start());
        }
        
        // Memory management task
        if (configManager.getConfig().getBoolean("memory-management.enabled", true)) {
            activeTasks.add(new MemoryManagementTask(this).start());
        }
        
        // Chunk optimization task
        if (configManager.getConfig().getBoolean("chunk-optimization.enabled", true)) {
            activeTasks.add(new ChunkOptimizationTask(this).start());
        }
        
        // Mob AI optimization task
        if (configManager.getConfig().getBoolean("mob-ai.enabled", true)) {
            activeTasks.add(new MobAIOptimizationTask(this).start());
        }
        
        // Auto-restart task (if enabled)
        if (configManager.getConfig().getBoolean("auto-restart.enabled", false)) {
            activeTasks.add(new AutoRestartTask(this).start());
        }
    }

    private void cancelAllTasks() {
        for (BukkitTask task : activeTasks) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        activeTasks.clear();
    }

    private void sendEnableMessage() {
        if (configManager.getConfig().getBoolean("notifications.console", true)) {
            Bukkit.getConsoleSender().sendMessage(Component.text("============================================", NamedTextColor.DARK_GRAY));
            Bukkit.getConsoleSender().sendMessage(Component.text("        UltraOptimizer Enabled", NamedTextColor.GOLD));
            Bukkit.getConsoleSender().sendMessage(Component.text("============================================", NamedTextColor.DARK_GRAY));
            Bukkit.getConsoleSender().sendMessage(Component.text("Entity Optimization: ", NamedTextColor.GRAY)
                    .append(Component.text(configManager.getConfig().getBoolean("entity-optimization.enabled") ? "✓" : "✗", 
                            configManager.getConfig().getBoolean("entity-optimization.enabled") ? NamedTextColor.GREEN : NamedTextColor.RED)));
            Bukkit.getConsoleSender().sendMessage(Component.text("Chunk Optimization: ", NamedTextColor.GRAY)
                    .append(Component.text(configManager.getConfig().getBoolean("chunk-optimization.enabled") ? "✓" : "✗", 
                            configManager.getConfig().getBoolean("chunk-optimization.enabled") ? NamedTextColor.GREEN : NamedTextColor.RED)));
            Bukkit.getConsoleSender().sendMessage(Component.text("Mob AI Optimization: ", NamedTextColor.GRAY)
                    .append(Component.text(configManager.getConfig().getBoolean("mob-ai.enabled") ? "✓" : "✗", 
                            configManager.getConfig().getBoolean("mob-ai.enabled") ? NamedTextColor.GREEN : NamedTextColor.RED)));
            Bukkit.getConsoleSender().sendMessage(Component.text("Performance Monitoring: ", NamedTextColor.GRAY)
                    .append(Component.text(configManager.getConfig().getBoolean("monitoring.enabled") ? "✓" : "✗", 
                            configManager.getConfig().getBoolean("monitoring.enabled") ? NamedTextColor.GREEN : NamedTextColor.RED)));
            Bukkit.getConsoleSender().sendMessage(Component.text("============================================", NamedTextColor.DARK_GRAY));
        }
    }

    // Getters
    public static UltraOptimizerMain getInstance() {
        return instance;
    }

    public UltraOptimizerAPI getAPI() {
        return api;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }

    public boolean isPluginEnabled() {
        return enabled;
    }
}
