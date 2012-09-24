package com.bukkitbackup.full.config;

import com.bukkitbackup.full.utils.LogUtils;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Backup - The simple server backup solution.
 *
 * @author Domenic Horner (gamerx)
 */
public final class Settings {

    private static Strings strings;
    private static File configFile;
    private static FileConfiguration settings;
    public boolean useMaxSizeBackup = false;

    public Settings(File configFile, Strings strings) {

        Settings.configFile = configFile;
        Settings.strings = strings;

        try {

            // Checks if configuration file exists, creates it if it does not.
            if (!configFile.exists()) {
                LogUtils.sendLog(strings.getString("newconfigfile"));
                
                BufferedReader bReader = null;
                BufferedWriter bWriter = null;
                String line;

                try {

                    // Open a stream to the configuration file in the jar, because we can only access over the class loader.
                    bReader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/resources/config.yml")));
                    bWriter = new BufferedWriter(new FileWriter(configFile));

                    // Writeout the new configuration file.
                    while ((line = bReader.readLine()) != null) {
                        bWriter.write(line);
                        bWriter.newLine();
                    }

                } catch (Exception e) {
                    LogUtils.exceptionLog(e, "Error opening stream.");
                } finally {
                    try {

                        // Confirm the streams are closed.
                        if (bReader != null) {
                            bReader.close();
                        }
                        if (bWriter != null) {
                            bWriter.close();
                        }
                    } catch (Exception e) {
                        LogUtils.exceptionLog(e, "Error closing configuration stream.");
                    }
                }
            }

            // Initialize the configuration, and populate with settings.
            settings = new YamlConfiguration();
            settings.load(configFile);

        } catch (Exception e) {
            LogUtils.exceptionLog(e, "Failed to load configuration.");
        }
    }

    public void checkSettingsVersion(String requiredVersion) {

        // Get the version information from the file.
        String configVersion = settings.getString("version", null);

        // Check we got a version from the config file.
        if (configVersion == null) {
            LogUtils.sendLog(strings.getString("failedtogetpropsver"));
        } else if (!configVersion.equals(requiredVersion)) {
            LogUtils.sendLog(strings.getString("configupdate"));
        }
    }

    /**
     * Gets the value of a integer property.
     *
     * @param property The name of the property.
     * @param defaultInt Set the default value of the integer.
     * @return The value of the property.
     */
    public int getIntProperty(String property, int defaultInt) {
        return settings.getInt(property, defaultInt);
    }

    /**
     * Gets the value of a boolean property.
     *
     * @param property The name of the property.
     * @param defaultBool Set the default value of the boolean.
     * @return The value of the property.
     */
    public boolean getBooleanProperty(String property, boolean defaultBool) {
        return settings.getBoolean(property, defaultBool);
    }

    /**
     * Gets a value of the string property.
     *
     * @param property The name of the property.
     * @param defaultString Set the default value of the string.
     * @return The value of the property.
     */
    public String getStringProperty(String property, String defaultString) {
        return settings.getString(property, defaultString);
    }

    /**
     * Method to convert human readable time, to minutes. - Checks string for no
     * automatic backup. - Checks for if only number (as minutes). - Checks for
     * properly formatted string. - If unknown amount of time, sets as minutes.
     *
     * @return The amount of time, in minutes.
     */
    public int getSaveAllInterval() {
        String settingInterval = getStringProperty("saveallinterval", "15M").trim().toLowerCase();
        // If it is null or set to disable.
        if (settingInterval.equals("-1") || settingInterval == null) {
            return 0;
        }
        // If it is just a number, return minutes.
        if (settingInterval.matches("^[0-9]+$")) {
            return Integer.parseInt(settingInterval);
        } else if (settingInterval.matches("[0-9]+[a-z]")) {
            Pattern timePattern = Pattern.compile("^([0-9]+)[a-z]$");
            Matcher amountTime = timePattern.matcher(settingInterval);
            Pattern letterPattern = Pattern.compile("^[0-9]+([a-z])$");
            Matcher letterTime = letterPattern.matcher(settingInterval);
            if (letterTime.matches() && amountTime.matches()) {
                String letter = letterTime.group(1);
                int time = Integer.parseInt(amountTime.group(1));
                if (letter.equals("m")) {
                    return time;
                } else if (letter.equals("h")) {
                    return time * 60;
                } else if (letter.equals("d")) {
                    return time * 1440;
                } else if (letter.equals("w")) {
                    return time * 10080;
                } else {
                    LogUtils.sendLog(strings.getString("unknowntimeident"));
                    return time;
                }
            } else {
                LogUtils.sendLog(strings.getString("checkbackupinterval"));
                return 0;
            }
        } else {
            LogUtils.sendLog(strings.getString("checkbackupinterval"));
            return 0;
        }
    }

    public int getBackupLimits() {
        String limitSetting = getStringProperty("maxbackups", "25").trim().toLowerCase();

        // If it is null or set to disable.
        if (limitSetting.equals("-1") || limitSetting == null) {
            return 0;
        }

        // If it is just a number, return minutes.
        if (limitSetting.matches("^[0-9]+$")) {
            LogUtils.sendDebug("Max Backups: Amount (M:0011)");
            
            return Integer.parseInt(limitSetting);
        } else if (limitSetting.matches("^[0-9]+[a-z]$")) {
            LogUtils.sendDebug("Max Backups: Size (M:0010)");
            
            useMaxSizeBackup = true;
            Pattern timePattern = Pattern.compile("^([0-9]+)[a-z]$");
            Matcher amountTime = timePattern.matcher(limitSetting);
            Pattern letterPattern = Pattern.compile("^[0-9]+([a-z])$");
            Matcher letterTime = letterPattern.matcher(limitSetting);
            if (letterTime.matches() && amountTime.matches()) {
                String letter = letterTime.group(1);
                int bytes = Integer.parseInt(amountTime.group(1));
                
                if (letter.equals("k")) {
                    return bytes;
                } else if (letter.equals("k")) {
                    return bytes * 1024;
                } else if (letter.equals("m")) {
                    return bytes * 1048576;
                } else if (letter.equals("g")) {
                    return bytes * 1073741824;
                } else {
                    LogUtils.sendLog(strings.getString("unknownsizeident"));
                    return bytes;
                }
            } else {
                LogUtils.sendLog(strings.getString("checksizelimit"));
                return 0;
            }
        } else {
             LogUtils.sendDebug("Max Backups: Unknown (M:0012)");
            
            LogUtils.sendLog(strings.getString("checksizelimit"));
            return 0;
        }
    }
}
