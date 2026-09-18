package com.ultraoptimizer.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import com.ultraoptimizer.UltraOptimizerMain;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * ConfigManager - Handles all configuration files
 */
public class ConfigManager {

    private final UltraOptimizerMain plugin;
    private FileConfiguration config;
    private File configFile;

    public ConfigManager(UltraOptimizerMain plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        
        // Load configuration
        loadConfig();
    }

    /**
     * Load the configuration file
     */
    public void loadConfig() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }

        try {
            config = YamlConfiguration.loadConfiguration(configFile);
            
            // Add defaults from resource
            InputStream inputStream = plugin.getResource("config.yml");
            if (inputStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8)
                );
                config.setDefaults(defaultConfig);
                config.options().copyDefaults(true);
                saveConfig();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load config.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Save the configuration file
     */
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save config.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Reload the configuration
     */
    public void reloadConfig() {
        loadConfig();
        plugin.reloadConfig();
    }

    /**
     * Get the configuration
     * @return FileConfiguration
     */
    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * Get a specific section of the config
     * @param path The path to the section
     * @return ConfigurationSection of the section, or null if not found
     */
    public org.bukkit.configuration.ConfigurationSection getSection(String path) {
        if (config.contains(path)) {
            return config.getConfigurationSection(path);
        }
        return null;
    }

    /**
     * Check if a value exists in the config
     * @param path The path to check
     * @return true if exists, false otherwise
     */
    public boolean contains(String path) {
        return config.contains(path);
    }

    /**
     * Set a value in the config
     * @param path The path to set
     * @param value The value to set
     */
    public void set(String path, Object value) {
        config.set(path, value);
        saveConfig();
    }

    /**
     * Reset the config to defaults
     */
    public void resetToDefaults() {
        if (configFile.exists()) {
            configFile.delete();
        }
        plugin.saveDefaultConfig();
        loadConfig();
    }
}
