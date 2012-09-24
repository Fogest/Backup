package com.bukkitbackup.full.threading.tasks;

import com.bukkitbackup.full.config.Settings;
import com.bukkitbackup.full.config.Strings;
import com.bukkitbackup.full.utils.FileUtils;
import static com.bukkitbackup.full.utils.FileUtils.FILE_SEPARATOR;
import com.bukkitbackup.full.utils.LogUtils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.World;

/**
 * Backup - The simple server backup solution.
 *
 * @author Domenic Horner (gamerx)
 */
public class BackupWorlds {

    private final Server pluginServer;
    private final Settings settings;
    private final Strings strings;
    private final String worldContainer;
    private final String backupPath;
    private final boolean useTemp;
    private final boolean shouldZIP;
    private final boolean splitBackup;
    private final String tempDestination;
    private final List<String> ignoredWorlds;
    private final boolean backupSeeds;

    /**
     * This should be the place where all the settings and paths for the backup
     * are defined.
     *
     * @param plugin
     * @param settings
     * @param strings
     */
    public BackupWorlds(Server server, final Settings settings, Strings strings) {

        this.pluginServer = server;
        this.settings = settings;
        this.strings = strings;

        // Create list of worlds we need to backup.
        ignoredWorlds = getIgnoredWorldNames();

        // Build folder paths.
        worldContainer = pluginServer.getWorldContainer().getName();

        // Get backup properties.
        backupPath = settings.getStringProperty("backuppath", "backups");
        shouldZIP = settings.getBooleanProperty("zipbackup", true);
        splitBackup = settings.getBooleanProperty("splitbackup", false);
        useTemp = settings.getBooleanProperty("usetemp", true);
        backupSeeds = settings.getBooleanProperty("backupworldseed", true);

        // Generate the worldStore.
        if (useTemp) {
            String tempFolder = settings.getStringProperty("tempfoldername", "");
            if (!tempFolder.equals("")) { // Absolute.
                tempDestination = tempFolder.concat(FILE_SEPARATOR);
            } else { // Relative.
                tempDestination = backupPath.concat(FILE_SEPARATOR).concat("temp").concat(FILE_SEPARATOR);
            }
        } else { // No temp folder.
            tempDestination = backupPath.concat(FILE_SEPARATOR);
        }
    }

    // The actual backup should be done here.
    public void doWorlds(String backupName) throws Exception {

        LinkedList<String> worldsToBackup = getWorldsToBackup();

        // Alert the user.
        if (worldsToBackup == null) {
            LogUtils.sendLog(strings.getString("noworlds"));
        }

        // Loops each world that needs to backed up, and do the required copies.
        while (!worldsToBackup.isEmpty()) {
            
            String currentWorldName = worldsToBackup.removeFirst();

            //pluginServer.getWorld(currentWorldName).save();



            // Get the current worlds seed.
            String worldSeed = String.valueOf(pluginServer.getWorld(currentWorldName).getSeed());

            // Check for split backup.
            if (splitBackup) {

                // Init backup path variable.
                String thisWorldBackupPath = backupPath.concat(FILE_SEPARATOR).concat(currentWorldName);
                // backups/world

                // Check if we have a custom container for worlds.
                if (!worldContainer.equals(".")) {
                    thisWorldBackupPath = backupPath.concat(FILE_SEPARATOR).concat(worldContainer).concat(FILE_SEPARATOR).concat(currentWorldName);
                    // backup/custom/world
                }

                // Set up destinations for temp and full backups.
                String thisWorldBackupFolder = thisWorldBackupPath.concat(FILE_SEPARATOR).concat(backupName);
                // backup/world/yymmdd-hhmmss

                // Check this backup folder exists.
                FileUtils.checkFolderAndCreate(new File(thisWorldBackupPath));

                // If we arent using the temp folder.
                if (useTemp) {
                    thisWorldBackupFolder = tempDestination.concat(currentWorldName).concat(FILE_SEPARATOR).concat(backupName);
                    // backups/temp/world/yymmdd-hhmmss
                }

                // Check this backup folder exists.
                FileUtils.checkFolderAndCreate(new File(thisWorldBackupFolder));

                // World seed backup.
                if (backupSeeds) {
                    try {
                        BufferedWriter out = new BufferedWriter(new FileWriter(thisWorldBackupFolder.concat(FILE_SEPARATOR).concat("worldSeed.txt")));
                        out.write("Level seed for '" + currentWorldName + "':");
                        out.newLine();
                        out.write(worldSeed);
                        out.close();
                    } catch (IOException ex) {
                        LogUtils.exceptionLog(ex, "Error saving level seed.");
                    }
                }

                // Copy the current world into it's backup folder.
                FileUtils.copyDirectory(worldContainer.concat(FILE_SEPARATOR).concat(currentWorldName), thisWorldBackupFolder.concat(FILE_SEPARATOR).concat(currentWorldName));

                // Check and ZIP folder.
                if (useTemp || shouldZIP) {
                    FileUtils.doCopyAndZIP(thisWorldBackupFolder, thisWorldBackupPath.concat(FILE_SEPARATOR).concat(backupName), shouldZIP, useTemp);
                }

            } else { // Not a split backup.

                // The folder where we should put the world folders.
                String copyDestination = tempDestination.concat(backupName).concat(FILE_SEPARATOR).concat(currentWorldName);

                // If we have a custom world-container.
                if (!worldContainer.equals(".")) {
                    copyDestination = tempDestination.concat(backupName).concat(FILE_SEPARATOR).concat(worldContainer).concat(FILE_SEPARATOR).concat(currentWorldName);
                }

                // Create this folder.
                FileUtils.checkFolderAndCreate(new File(copyDestination));

                // Bacup level seeds.
                if (backupSeeds) {
                    try {
                        BufferedWriter out = new BufferedWriter(new FileWriter(copyDestination.concat(FILE_SEPARATOR).concat("worldSeed.txt")));
                        out.write("Level seed for '" + currentWorldName + "':");
                        out.newLine();
                        out.write(worldSeed);
                        out.close();
                    } catch (IOException ex) {
                        LogUtils.exceptionLog(ex, "Error saving level seed.");
                    }
                }

                // Copy the current world into it's backup folder.
                FileUtils.copyDirectory(worldContainer.concat(FILE_SEPARATOR).concat(currentWorldName), copyDestination);

            }
        }
    }

    /**
     * Function to get world names to ignore.
     *
     * @return A List[] of the world names we should not be backing up.
     */
    private List<String> getIgnoredWorldNames() {

        // Get skipped worlds form config.
        List<String> worldNames = Arrays.asList(settings.getStringProperty("skipworlds", "").split(";"));

        // Loop all ignored worlds.
        if (worldNames.size() > 0 && !worldNames.get(0).isEmpty()) {

            // Log what worlds are disabled.
            LogUtils.sendLog(strings.getString("disabledworlds"));
            LogUtils.sendLog(worldNames.toString());
        }

        // Return the world names.
        return worldNames;
    }

    private LinkedList<String> getWorldsToBackup() {
        LinkedList<String> toBackup = new LinkedList<String>();
        for (World loopWorld : pluginServer.getWorlds()) {
            if ((loopWorld.getName() != null) && !loopWorld.getName().isEmpty() && (!ignoredWorlds.contains(loopWorld.getName()))) {
                toBackup.add(loopWorld.getName());
            }
        }
        return toBackup;
    }
}