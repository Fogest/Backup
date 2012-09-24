package com.bukkitbackup.full.config;

import com.bukkitbackup.full.utils.LogUtils;
import java.io.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Backup - The simple server backup solution.
 *
 * @author Domenic Horner (gamerx)
 */
public class Strings {

    private File stringsFile;
    private FileConfiguration strings;

    /**
     * Loads the strings configuration file. If it does not exist, it creates it
     * from defaults.
     *
     * @param stringsFile The file that strings should be loaded from.
     */
    public Strings(File stringsFile) {

        this.stringsFile = stringsFile;

        try {

            // Check strings file exists, and create is needed.
            if (!stringsFile.exists()) {
                createDefaultStrings();
            }

        } catch (Exception e) {
            LogUtils.exceptionLog(e, "Error checking for strings file.");
        }

        // Load strings from configuration file.
        loadStrings();
    }

    /**
     * Checks the version of the strings file. Notifies user if it requires an
     * update.
     *
     * @param requiredVersion The required version from the settings file.
     */
    public void checkStringsVersion(String requiredVersion) {

        // Get the version information from the file.
        String stringVersion = strings.getString("version", null);

        // Check we got a version from the config file.
        if (stringVersion == null) {
            LogUtils.sendLog("Failed to get strings file verison.");
        } else if (!stringVersion.equals(requiredVersion)) {
            LogUtils.sendLog(this.getString("stringsupdate"));
        }

    }

    /**
     * Load strings configuration into memory from file.
     */
    private void loadStrings() {
        strings = new YamlConfiguration();
        try {
            strings.load(stringsFile);
        } catch (Exception e) {
            LogUtils.exceptionLog(e, "Error loading strings file.");
        }
    }

    /**
     * Method to create (or re-create) the strings configuration file.
     */
    private void createDefaultStrings() {

        // Check if it exists, if it does, delete it.
        if (stringsFile.exists()) {
            stringsFile.delete();
        }

        // Initalize buffers and reader.
        BufferedReader bReader = null;
        BufferedWriter bWriter = null;
        String line;

        try {

            // Open a stream to the properties file in the jar, because we can only access over the class loader.
            bReader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/resources/strings.yml")));
            bWriter = new BufferedWriter(new FileWriter(stringsFile));

            // Read the default configuration into the config file.
            while ((line = bReader.readLine()) != null) {
                bWriter.write(line);
                bWriter.newLine();
            }
        } catch (IOException ioe) {
            LogUtils.exceptionLog(ioe, "Error opening streams.");
        } // Close the open buffers.
        finally {
            try {
                if (bReader != null) {
                    bReader.close();
                }
                if (bWriter != null) {
                    bWriter.close();
                }
            } catch (IOException ioe) {
                LogUtils.exceptionLog(ioe, "Error closing streams.");
            }
        }
    }

    /**
     * Method used when doing string file updates.
     */
    public void doStringsUpdate() {
        loadStrings();
    }

    /**
     * Gets a value of the string property.
     *
     * @param sname The identifier for the string.
     * @return The string from properties, with colors encoded.
     */
    public String getString(String property) {

        // Get string for this name.
        String string = strings.getString(property);

        // If we cannot find a string for this, return default.
        if (string != null) {
            return colorizeString(string);
        } else {
            return strings.getString("stringnotfound") + property;
        }
    }

    /**
     * Gets a value of the string property, and replaces options.
     *
     * @param property The identifier for the string.
     * @param option The variable to replace %%ARG%% with.
     * @return The string from properties, with colors encoded, and text
     * replaced.
     */
    public String getString(String property, String option) {

        // Get string for this name.
        String string = strings.getString(property);

        // If we cannot find a string for this, return default.
        if (string != null) {
            return colorizeString(string.replaceAll("%%ARG%%", option));
        } else {
            return strings.getString("stringnotfound") + property;
        }
    }

    /**
     * Gets a value of the string property, and replaces options.
     *
     * @param property The identifier for the string.
     * @param optionOne The variable to replace %%ARG%% with.
     * @param optionTwo The variable to replace %%ARG1%% with.
     * @return The string from properties, with colors encoded, and text
     * replaced.
     */
    public String getString(String property, String optionOne, String optionTwo) {

        // Get string for this name.
        String string = strings.getString(property);

        // If we cannot find a string for this, return default.
        if (string != null) {
            string = string.replaceAll("%%ARG%%", optionOne);
            string = string.replaceAll("%%ARG1%%", optionTwo);

            return colorizeString(string);
        } else {
            return strings.getString("stringnotfound") + property;
        }
    }

    /**
     * Encodes the color codes, and returns the encoded string. If the parameter
     * is blank or null, return blank.
     *
     * @param toColour The string to encode.
     * @return The encoded string.
     */
    private String colorizeString(String toColor) {

        // Check we got passed a string.
        if (toColor != null) {
            return toColor.replaceAll("&([0-9a-fklmnor])", "\u00A7$1");
        } else {
            return "";
        }
    }
}
