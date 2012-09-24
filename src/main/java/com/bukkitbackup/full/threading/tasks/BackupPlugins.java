package com.bukkitbackup.full.threading.tasks;

import com.bukkitbackup.full.config.Settings;
import com.bukkitbackup.full.config.Strings;
import com.bukkitbackup.full.utils.FileUtils;
import static com.bukkitbackup.full.utils.FileUtils.FILE_SEPARATOR;
import com.bukkitbackup.full.utils.LogUtils;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Backup - The simple server backup solution.
 *
 * @author Domenic Horner (gamerx)
 */
public class BackupPlugins {

    private final Strings strings;
    private final String backupPath;
    private final boolean shouldZIP;
    private final boolean splitBackup;
    private final boolean useTemp;
    private final String tempDestination;
    private final boolean pluginListMode;
    private final List<String> pluginList;
    private final FileFilter pluginsFileFilter;

    public BackupPlugins(Settings settings, Strings strings) {

        this.strings = strings;

        // Get the backup destination.
        backupPath = settings.getStringProperty("backuppath", "backups");

        // Get backup properties.
        shouldZIP = settings.getBooleanProperty("zipbackup", true);
        splitBackup = settings.getBooleanProperty("splitbackup", false);
        useTemp = settings.getBooleanProperty("usetemp", true);
        pluginListMode = settings.getBooleanProperty("pluginlistmode", true);
        pluginList = Arrays.asList(settings.getStringProperty("pluginlist", "").split(";"));

        // The FileFilter instance for skipped/enabled plugins.
        pluginsFileFilter = new FileFilter() {

            @Override
            public boolean accept(File name) {

                // Check if there are plugins listed.
                if (pluginList.size() > 0 && !pluginList.get(0).isEmpty()) {

                    // Loop each plugin.
                    for (int i = 0; i < pluginList.size(); i++) {

                        String findMe = "plugins".concat(FILE_SEPARATOR).concat(pluginList.get(i));

                        int isFound = name.getPath().indexOf(findMe);

                        // Check if the current plugin matches the string.
                        if (isFound != -1) {

                            // Return false for exclude, true to include.
                            if (pluginListMode) {
                                return false;
                            } else {
                                return true;
                            }
                        }
                    }
                }

                if (pluginListMode) {
                    return true;
                } else {
                    return false;
                }
            }
        };

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
    public void doPlugins(String backupName) throws IOException {

        // Setup Source and destination DIR's.
        File pluginsFolder = new File("plugins");

        // Touch the folder to update the modified date.
        pluginsFolder.setLastModified(System.currentTimeMillis());


        String thisTempDestination;
        if (splitBackup) {
            thisTempDestination = backupPath.concat(FILE_SEPARATOR).concat("plugins").concat(FILE_SEPARATOR).concat(backupName);
        } else {
            thisTempDestination = tempDestination.concat(backupName).concat(FILE_SEPARATOR).concat("plugins");
        }
        FileUtils.checkFolderAndCreate(new File(thisTempDestination));

        // Perform plugin backup.
        if (pluginList.size() > 0 && !pluginList.get(0).isEmpty()) {
            if (pluginListMode) {
                LogUtils.sendLog(strings.getString("disabledplugins"));
            } else {
                LogUtils.sendLog(strings.getString("enabledplugins"));
            }
            LogUtils.sendLog(pluginList.toString());
        }
        FileUtils.copyDirectory(pluginsFolder, new File(thisTempDestination), pluginsFileFilter, true);

        // Check if ZIP is required.
        if (splitBackup && shouldZIP) {
            String destination = backupPath.concat(FILE_SEPARATOR).concat("plugins").concat(FILE_SEPARATOR).concat(backupName);
            try {
                if (useTemp) {
                    FileUtils.zipDir(thisTempDestination, destination);
                    FileUtils.deleteDirectory(new File(thisTempDestination));
                    new File(thisTempDestination).delete();
                }
            } catch (Exception e) {
                LogUtils.exceptionLog(e);
            }
        }
    }
}
