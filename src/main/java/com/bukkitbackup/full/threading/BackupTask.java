package com.bukkitbackup.full.threading;

import com.bukkitbackup.full.BackupFull;
import com.bukkitbackup.full.config.Settings;
import com.bukkitbackup.full.config.Strings;
import com.bukkitbackup.full.threading.tasks.BackupEverything;
import com.bukkitbackup.full.threading.tasks.BackupPlugins;
import com.bukkitbackup.full.threading.tasks.BackupWorlds;
import com.bukkitbackup.full.utils.FileUtils;
import static com.bukkitbackup.full.utils.FileUtils.FILE_SEPARATOR;
import com.bukkitbackup.full.utils.LogUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Backup - The simple server backup solution.
 *
 * @author Domenic Horner (gamerx)
 */
public class BackupTask implements Runnable {

    private final Plugin plugin;
    private final Server pluginServer;
    private final Settings settings;
    private final Strings strings;
    private final boolean backupEverything;
    private final boolean splitBackup;
    private final boolean shouldZIP;
    private final boolean useTemp;
    private final String dateFormat;
    private final String worldContainer;
    private final String backupPath;
    private final String tempDestination;
    private String thisBackupName;
    // Threads.
    private final BackupWorlds worldBackupTask;
    private final BackupPlugins pluginBackupTask;
    private final BackupEverything everythingBackupTask;
    //private SyncSaveAll syncSaveAllUtil;

    public BackupTask(Plugin plugin, Settings settings, Strings strings) {

        // Retrieve parameters.
        this.plugin = plugin;
        this.pluginServer = plugin.getServer();
        this.settings = settings;
        this.strings = strings;

        // The worlds container, if any.
        worldContainer = pluginServer.getWorldContainer().getName();

        // Load settings.
        backupPath = settings.getStringProperty("backuppath", "backups");
        backupEverything = settings.getBooleanProperty("backupeverything", false);
        splitBackup = settings.getBooleanProperty("splitbackup", false);
        shouldZIP = settings.getBooleanProperty("zipbackup", true);
        useTemp = settings.getBooleanProperty("usetemp", true);
        dateFormat = settings.getStringProperty("dateformat", "%1$tY-%1$tm-%1$td-%1$tH-%1$tM-%1$tS");

        // Import backup tasks.
        everythingBackupTask = BackupFull.backupEverything;
        worldBackupTask = BackupFull.backupWorlds;
        pluginBackupTask = BackupFull.backupPlugins;

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

    @Override
    public void run() {

        // Get this instances folder name, set variables.
        thisBackupName = getBackupName();

        // Check if backupeverything enabled.
        if (backupEverything) {

            // Start the BackupEverything class.
            try {
                everythingBackupTask.doEverything(thisBackupName);
            } catch (Exception e) {
                LogUtils.exceptionLog(e, "Failed to backup worlds: Exception in BackupWorlds.");
            }
        } else {

            // Check if we should be backing up worlds.
            if (settings.getBooleanProperty("backupworlds", true)) {

                // Attempt to backup worlds.
                try {
                    worldBackupTask.doWorlds(thisBackupName);
                } catch (Exception e) {
                    LogUtils.exceptionLog(e, "Failed to backup worlds: Exception in BackupWorlds.");
                }
            } else {
                LogUtils.sendLog(strings.getString("skipworlds"));
            }

            // Check if we should be backing up plugins.
            if (settings.getBooleanProperty("backupplugins", true)) {

                // Attempt to backup plugins.
                try {
                    pluginBackupTask.doPlugins(thisBackupName);
                } catch (Exception e) {
                    LogUtils.exceptionLog(e, "Failed to backup plugins: Exception in BackupPlugins.");
                }
            } else {
                LogUtils.sendLog(strings.getString("skipplugins"));
            }

            // If this is a non-split backup, we need to ZIP the whole thing.
            if (!splitBackup) {
                FileUtils.doCopyAndZIP(tempDestination.concat(thisBackupName), backupPath.concat(FILE_SEPARATOR).concat(thisBackupName), shouldZIP, useTemp);
            }
        }

        // Perform cleaning on the backup folder.
        try {
            deleteOldBackups();
        } catch (Exception e) {
            LogUtils.exceptionLog(e, "Failed to delete old backups.");
        }

        // Perform finalization for this backup.
        finishBackup();
    }

    /**
     * Return a formatted date string, using the option from settings.
     *
     * @return The formatted date, as a string.
     */
    private String getBackupName() {
        Calendar calendar = Calendar.getInstance();
        String formattedDate;
        try {
            formattedDate = String.format(dateFormat, calendar);
        } catch (Exception e) {
            LogUtils.exceptionLog(e, "Exception formatting date.");
            formattedDate = String.format("%1$tY-%1$tm-%1$td-%1$tH-%1$tM-%1$tS", calendar);
        }
        return formattedDate;
    }

    /**
     * Check if we need to delete old backups, and perform required operations.
     *
     * @throws Exception
     */
    private void deleteOldBackups() throws Exception {

        File backupDir = new File(backupPath);

        LogUtils.sendDebug("Delete old backups. (M:0013)");

        if (splitBackup) { // Look inside the folders.

            LogUtils.sendDebug("Delete old backups. - Split Backup (M:0014)");

            // Check if we have a different container for worlds.
            if (!worldContainer.equals(".")) { // Custom.

                LogUtils.sendDebug("Delete old backups. - Custom world container. (M:0015)");

                backupDir = new File(backupPath.concat(FILE_SEPARATOR).concat(worldContainer));

                File[] worldFoldersToClean = backupDir.listFiles();
                for (int l = 0; l < worldFoldersToClean.length; l++) {
                    // Make sure we are cleaning a directory.
                    if (worldFoldersToClean[l].isDirectory()) {
                        cleanFolder(worldFoldersToClean[l]);
                    }
                }

                backupDir = new File(backupPath.concat(FILE_SEPARATOR).concat("plugins"));

                File[] pluginFolderToClean = backupDir.listFiles();
                for (int l = 0; l < pluginFolderToClean.length; l++) {
                    // Make sure we are cleaning a directory.
                    if (pluginFolderToClean[l].isDirectory()) {
                        cleanFolder(pluginFolderToClean[l]);
                    }
                }
            } else {

                LogUtils.sendDebug("Delete old backups. - Split backup. - No custom container. (M:0016)");

                File[] foldersToClean = backupDir.listFiles();
                for (int l = 0; l < foldersToClean.length; l++) {



                    // Make sure we are cleaning a directory.
                    if (foldersToClean[l].isDirectory()) {
                        cleanFolder(foldersToClean[l]);
                    }
                }
            }


        } else { // Clean entire directory.

            LogUtils.sendDebug("Delete old backups.- Plain and simple (M:0017)");

            cleanFolder(backupDir);
        }
    }

    private void cleanFolder(File folderToClean) throws IOException {

        LogUtils.sendDebug("Attempting to clean: " + folderToClean.toString() + " (M:0014)");

        try {

            // Get total backup limit.
            int backupLimit = settings.getBackupLimits();
            if (backupLimit != 0) {

                // List all the files inside this folder.
                File[] filesList = FileUtils.listItemsInDir(folderToClean);

                LogUtils.sendDebug("Files: (M:0018)");
                LogUtils.sendDebug(filesList.toString());

                // Check we listed the directory.
                if (filesList == null) {
                    LogUtils.sendLog(strings.getString("failedlistdir"));
                    return;
                }

                // Using size to limit backups.
                if (settings.useMaxSizeBackup) {

                    // Get total folder size.
                    int totalFolderSize = FileUtils.getTotalFolderSize(folderToClean);

                    // If the amount of files exceeds the max backups to keep.
                    if (totalFolderSize > backupLimit) {

                        // Create a list for deleted backups.
                        ArrayList<File> deletedList = new ArrayList<File>(filesList.length);

                        // Inti variables.
                        int maxModifiedIndex;
                        long maxModified;

                        // While the total folder size is bigger than the limit.
                        while (FileUtils.getTotalFolderSize(folderToClean) > backupLimit) {

                            // Create updated list.
                            filesList = FileUtils.listFilesInDir(folderToClean);

                            // List of all the backups.
                            ArrayList<File> backupList = new ArrayList<File>(filesList.length);
                            backupList.addAll(Arrays.asList(filesList));

                            // Loop backup list.
                            for (int i = 0; backupList.size() > 1; i++) {
                                maxModifiedIndex = 0;
                                maxModified = backupList.get(0).lastModified();
                                for (int j = 1; j < backupList.size(); ++j) {
                                    File currentFile = backupList.get(j);
                                    if (currentFile.lastModified() > maxModified) {
                                        maxModified = currentFile.lastModified();
                                        maxModifiedIndex = j;
                                    }
                                }
                                backupList.remove(maxModifiedIndex);
                            }

                            FileUtils.deleteDirectory(backupList.get(0));
                            deletedList.add(backupList.get(0));
                        }


                        // Inform the user what backups are being deleted.
                        LogUtils.sendLog(strings.getString("removeoldsize"));
                        LogUtils.sendLog(Arrays.toString(deletedList.toArray()));
                    }




                } else { // Using amount of backups.

                    // If the amount of files exceeds the max backups to keep.
                    if (filesList.length > backupLimit) {
                        ArrayList<File> backupList = new ArrayList<File>(filesList.length);
                        backupList.addAll(Arrays.asList(filesList));

                        int maxModifiedIndex;
                        long maxModified;

                        //Remove the newst backups from the list.
                        for (int i = 0; i < backupLimit; ++i) {
                            maxModifiedIndex = 0;
                            maxModified = backupList.get(0).lastModified();
                            for (int j = 1; j < backupList.size(); ++j) {
                                File currentFile = backupList.get(j);
                                if (currentFile.lastModified() > maxModified) {
                                    maxModified = currentFile.lastModified();
                                    maxModifiedIndex = j;
                                }
                            }
                            backupList.remove(maxModifiedIndex);
                        }

                        // Inform the user what backups are being deleted.
                        LogUtils.sendLog(strings.getString("removeoldage"));
                        LogUtils.sendLog(Arrays.toString(backupList.toArray()));

                        // Finally delete the backups.
                        for (File backupToDelete : backupList) {
                            FileUtils.deleteDir(backupToDelete);
                        }
                    }

                }
            }
        } catch (SecurityException se) {
            LogUtils.exceptionLog(se, "Failed to clean old backups: Security Exception.");
        }
    }

    /**
     * Creates a temporary Runnable that is running on the main thread by the
     * scheduler to prevent thread problems.
     */
    private void finishBackup() {

        // Do the FTP upload if required.
        if (shouldZIP && !splitBackup) {
            doFTPUpload(backupPath.concat(FILE_SEPARATOR).concat(thisBackupName) + ".zip");
        }

        // Create new Runnable instance.
        Runnable run = new Runnable() {

            @Override
            public void run() {

                // Should we enable auto-save again?
                if (settings.getBooleanProperty("enableautosave", true)) {
                    for (World world : pluginServer.getWorlds()) {
                        world.setAutoSave(true);
                    }
                }

                // Delete the temp directory.
                if (useTemp) {
                    FileUtils.deleteDir(new File(tempDestination));
                }

                // Notify that it has completed.
                notifyCompleted();
            }

            private void notifyCompleted() {
                String completedBackupMessage = strings.getString("backupfinished");

                // Check there is a message.
                if (completedBackupMessage != null && !completedBackupMessage.trim().isEmpty()) {

                    // Check if we are using multiple lines.
                    if (completedBackupMessage.contains(";;")) {

                        // Convert to array of lines.
                        List<String> messageList = Arrays.asList(completedBackupMessage.split(";;"));

                        // Loop the lines of this message.
                        for (int i = 0; i < messageList.size(); i++) {

                            // Retrieve this line of the message.
                            String thisMessage = messageList.get(i);

                            // Notify all players, regardless of the permission node.
                            if (settings.getBooleanProperty("notifyallplayers", true)) {
                                pluginServer.broadcastMessage(thisMessage);
                            } else {

                                // Get all players.
                                Player[] players = pluginServer.getOnlinePlayers();

                                // Loop through all online players.
                                for (int pos = 0; pos < players.length; pos++) {
                                    Player currentplayer = players[pos];

                                    // If the current player has the right permissions, notify them.
                                    if (currentplayer.hasPermission("backup.notify")) {
                                        currentplayer.sendMessage(thisMessage);
                                    }
                                }
                            }
                        }

                    } else {

                        // Notify all players, regardless of the permission node.
                        if (settings.getBooleanProperty("notifyallplayers", true)) {
                            pluginServer.broadcastMessage(completedBackupMessage);
                        } else {

                            // Get all players.
                            Player[] players = pluginServer.getOnlinePlayers();

                            // Loop through all online players.
                            for (int pos = 0; pos < players.length; pos++) {
                                Player currentplayer = players[pos];

                                // If the current player has the right permissions, notify them.
                                if (currentplayer.hasPermission("backup.notify")) {
                                    currentplayer.sendMessage(completedBackupMessage);
                                }
                            }
                        }
                    }
                }
            }
        };
        pluginServer.getScheduler().scheduleSyncDelayedTask(plugin, run);

        PrepareBackup.backupInProgress = false;

    }

    private void doFTPUpload(String ZIPFile) {

        // This runs in another thread to ensure it does nto affect server performance.
        if (settings.getBooleanProperty("ftpuploadenable", false)) {
            pluginServer.getScheduler().runTaskAsynchronously(plugin, new FTPUploader(settings, strings, ZIPFile));
        }
    }
}
