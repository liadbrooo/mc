package com.ultraoptimizer.database;

import com.ultraoptimizer.UltraOptimizerMain;

import java.sql.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * DatabaseManager - Handles SQLite and MySQL database operations
 */
public class DatabaseManager {

    private final UltraOptimizerMain plugin;
    private Connection connection;
    private boolean usingMySQL = false;

    public DatabaseManager(UltraOptimizerMain plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize the database connection
     */
    public void initialize() {
        String dbType = plugin.getConfigManager().getConfig().getString("advanced.database.type", "sqlite");
        
        if ("mysql".equalsIgnoreCase(dbType)) {
            setupMySQL();
        } else {
            setupSQLite();
        }
    }

    /**
     * Setup SQLite database
     */
    private void setupSQLite() {
        try {
            String dbPath = plugin.getDataFolder().getAbsolutePath() + "/data.db";
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            
            plugin.getLogger().info("SQLite database connected successfully.");
            
            // Create tables
            createTables();
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to SQLite database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Setup MySQL database
     */
    private void setupMySQL() {
        try {
            String host = plugin.getConfigManager().getConfig().getString("advanced.database.mysql.host", "localhost");
            int port = plugin.getConfigManager().getConfig().getInt("advanced.database.mysql.port", 3306);
            String database = plugin.getConfigManager().getConfig().getString("advanced.database.mysql.database", "ultraoptimizer");
            String username = plugin.getConfigManager().getConfig().getString("advanced.database.mysql.username", "root");
            String password = plugin.getConfigManager().getConfig().getString("advanced.database.mysql.password", "");
            
            String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&autoReconnect=true", host, port, database);
            
            connection = DriverManager.getConnection(url, username, password);
            
            plugin.getLogger().info("MySQL database connected successfully.");
            usingMySQL = true;
            
            // Create tables
            createTables();
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to MySQL database: " + e.getMessage());
            plugin.getLogger().warning("Falling back to SQLite...");
            setupSQLite();
        }
    }

    /**
     * Create database tables if they don't exist
     */
    private void createTables() {
        try (Statement statement = connection.createStatement()) {
            
            // Statistics table
            statement.execute(
                "CREATE TABLE IF NOT EXISTS statistics (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "tps REAL," +
                "memory_used BIGINT," +
                "entity_count INTEGER," +
                "chunk_count INTEGER," +
                "player_count INTEGER" +
                ")"
            );
            
            // Optimization log table
            statement.execute(
                "CREATE TABLE IF NOT EXISTS optimization_log (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "action_type VARCHAR(50)," +
                "entities_removed INTEGER," +
                "reason TEXT" +
                ")"
            );
            
            // Performance history table
            statement.execute(
                "CREATE TABLE IF NOT EXISTS performance_history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "avg_tps REAL," +
                "min_tps REAL," +
                "max_tps REAL," +
                "avg_memory REAL," +
                "peak_memory BIGINT" +
                ")"
            );
            
            plugin.getLogger().info("Database tables created successfully.");
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create database tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Execute an async SQL update
     * @param sql SQL query
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> executeUpdateAsync(String sql) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.executeUpdate();
                return true;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error executing update: " + e.getMessage(), e);
                return false;
            }
        });
    }

    /**
     * Execute an async SQL query
     * @param sql SQL query
     * @return CompletableFuture<ResultSet>
     */
    public CompletableFuture<ResultSet> executeQueryAsync(String sql) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PreparedStatement statement = connection.prepareStatement(sql);
                return statement.executeQuery();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error executing query: " + e.getMessage(), e);
                return null;
            }
        });
    }

    /**
     * Log statistics to database
     * @param tps Current TPS
     * @param memoryUsed Memory used in bytes
     * @param entityCount Entity count
     * @param chunkCount Chunk count
     * @param playerCount Player count
     */
    public void logStatistics(double tps, long memoryUsed, int entityCount, int chunkCount, int playerCount) {
        String sql = "INSERT INTO statistics (tps, memory_used, entity_count, chunk_count, player_count) VALUES (?, ?, ?, ?, ?)";
        
        CompletableFuture.runAsync(() -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setDouble(1, tps);
                statement.setLong(2, memoryUsed);
                statement.setInt(3, entityCount);
                statement.setInt(4, chunkCount);
                statement.setInt(5, playerCount);
                statement.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error logging statistics: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Log optimization action
     * @param actionType Type of action
     * @param entitiesRemoved Number of entities removed
     * @param reason Reason for optimization
     */
    public void logOptimization(String actionType, int entitiesRemoved, String reason) {
        String sql = "INSERT INTO optimization_log (action_type, entities_removed, reason) VALUES (?, ?, ?)";
        
        CompletableFuture.runAsync(() -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, actionType);
                statement.setInt(2, entitiesRemoved);
                statement.setString(3, reason);
                statement.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error logging optimization: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Check if connection is valid
     * @return true if connected
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Get the database connection
     * @return Connection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Check if using MySQL
     * @return true if MySQL, false if SQLite
     */
    public boolean isUsingMySQL() {
        return usingMySQL;
    }

    /**
     * Shutdown database connection
     */
    public void shutdown() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    plugin.getLogger().info("Database connection closed.");
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error closing database connection: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
