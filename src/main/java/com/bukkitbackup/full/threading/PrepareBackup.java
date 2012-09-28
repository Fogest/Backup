package com.bukkitbackup.full.threading;

import com.bukkitbackup.full.BackupFull;
import com.bukkitbackup.full.config.Settings;
import com.bukkitbackup.full.config.Strings;
import com.bukkitbackup.full.utils.LogUtils;
import java.util.Arrays;
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
public class PrepareBackup implements Runnable {

    private final Plugin plugin;
    private final Server pluginServer;
    private final Settings settings;
    private final Strings strings;
    public static boolean backupInProgress = false;
    public static boolean backupEnabled = true;
    public boolean isLastBackup = false;
    public boolean isManualBackup;

    public PrepareBackup(Plugin plugin, Settings settings, Strings strings) {
        this.plugin = plugin;
        this.pluginServer = plugin.getServer();
        this.settings = settings;
        this.strings = strings;
    }

    @Override
    public synchronized void run() {
        if (backupInProgress) {
            LogUtils.sendLog(strings.getString("backupinprogress"));
        } else {
            checkShouldDoBackup();
        }
    }

    /**
     * This method decides whether the doBackup should be run.
     *
     * It checks: - Online players. - Bypass node. - Manual doBackup.
     *
     * It then runs the doBackup if needed.
     */
    private void checkShouldDoBackup() {

        // If it is a manual doBackup, start it, otherwise, perform checks.
        if (isManualBackup) {
            prepareBackup();
        } else if (backupEnabled) {

            // No player checking.
            if (settings.getBooleanProperty("backupemptyserver", false)) {
                prepareBackup();
            } else {

                // Checking online players.
                if (pluginServer.getOnlinePlayers().length == 0) {

                    // Check if last backup
                    if (isLastBackup) {
                        LogUtils.sendLog(strings.getString("lastbackup"));
                        prepareBackup();
                        isLastBackup = false;
                    } else {
                        LogUtils.sendLog(strings.getString("abortedbackup"));
                    }
                } else {

                    // Default don't do backup.
                    boolean doBackup = false;

                    // Get all online players.
                    Player[] players = pluginServer.getOnlinePlayers();

                    // Loop players.
                    for (int player = 0; player < players.length; player++) {
                        Player currentplayer = players[player];

                        // If any players do not have the node, do the doBackup.
                        if (!currentplayer.hasPermission("backup.bypass")) {
                            doBackup = true;
                        }
                    }

                    // Final check if we should do the backup.
                    if (doBackup) {
                        prepareBackup();
                    } else {
                        LogUtils.sendLog(strings.getString("skipbackupbypass"));
                    }
                }
            }
        } else {
            LogUtils.sendLog(strings.getString("backupoff"));
        }
    }

    /**
     * Prepared for, and starts, a doBackup.
     */
    protected void prepareBackup() {

        // Tell the world!
        backupInProgress = true;

        // Notify doBackup has started.
        notifyStarted();

        // Save all players to worlds.
        pluginServer.savePlayers();

        // Turn off auto-saving of worlds.
        for (World world : pluginServer.getWorlds()) {
            world.setAutoSave(false);
        }
        
        // Perform final world save before backup.
        for (World world : pluginServer.getWorlds()) {
            world.save();
        }

        // Scedule the doBackup.
        pluginServer.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

            @Override
            public void run() {
                pluginServer.getScheduler().scheduleAsyncDelayedTask(plugin, BackupFull.backupTask);
            }
        });
        isManualBackup = false;
    }

    /**
     * Notify that the backup has started.
     *
     */
    private void notifyStarted() {

        // Get message.
        String startBackupMessage = strings.getString("backupstarted");

        // Check the string is set.
        if (startBackupMessage != null && !startBackupMessage.trim().isEmpty()) {

            // Check if we are using multiple lines.
            if (startBackupMessage.contains(";;")) {

                // Convert to array of lines.
                List<String> messageList = Arrays.asList(startBackupMessage.split(";;"));

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
                    pluginServer.broadcastMessage(startBackupMessage);
                } else {

                    // Get all players.
                    Player[] players = pluginServer.getOnlinePlayers();

                    // Loop through all online players.
                    for (int pos = 0; pos < players.length; pos++) {
                        Player currentplayer = players[pos];

                        // If the current player has the right permissions, notify them.
                        if (currentplayer.hasPermission("backup.notify")) {
                            currentplayer.sendMessage(startBackupMessage);
                        }
                    }
                }
            }
        }
    }
}
