package com.bukkitbackup.full.threading;

import com.bukkitbackup.full.config.Settings;
import com.bukkitbackup.full.config.Strings;
import com.bukkitbackup.full.utils.LogUtils;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

/**
 * Backup - The simple server backup solution.
 *
 * @author Domenic Horner (gamerx)
 */
public class BackupScheduler implements Runnable {

    private final Plugin plugin;
    private final PrepareBackup prepareBackup;
    private final Settings settings;
    private final Strings strings;
    private final Server pluginServer;
    private final String[] timesArray;

    public BackupScheduler(Plugin plugin, PrepareBackup prepareBackup, Settings settings, Strings strings, String[] timesArray) {
        this.plugin = plugin;
        this.prepareBackup = prepareBackup;
        this.pluginServer = plugin.getServer();
        this.settings = settings;
        this.strings = strings;
        this.timesArray = timesArray;
    }

    public void run() {
        
        // Loop to check if we need to backup.
        while(true) {
            
            LogUtils.sendDebug("Checking if we should backup. (M:0008)");
            
            // Get current time, and format it to our requirements.
            Calendar calendarInstance = Calendar.getInstance();
            String timeNow = new SimpleDateFormat("HH:mm").format(calendarInstance.getTime());
            
            LogUtils.sendDebug("Time is: "+ timeNow + " (M:0009)");
            
            // Loop the array of times we want to backup at.
            for (int j = 0; j < timesArray.length; j++) {
                
                // If we want to backup this minute, schedule a backupTask.
                if(timesArray[j].equals(timeNow)) {
                    pluginServer.getScheduler().scheduleAsyncDelayedTask(plugin, prepareBackup);
                }
            }
            
            // This sleeps the thread for 30 seconds in order to do another check.
            try {
                Thread.sleep(30000);
            } catch (InterruptedException ex) {
                LogUtils.exceptionLog(ex);
            }
            
        }
        
    }
}
