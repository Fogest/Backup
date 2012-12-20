package com.bukkitbackup.full;

import com.bukkitbackup.full.config.Settings;
import com.bukkitbackup.full.config.Strings;
import com.bukkitbackup.full.config.UpdateChecker;
import com.bukkitbackup.full.events.CommandHandler;
import com.bukkitbackup.full.events.EventListener;
import com.bukkitbackup.full.threading.BackupScheduler;
import com.bukkitbackup.full.threading.BackupTask;
import com.bukkitbackup.full.threading.PrepareBackup;
import com.bukkitbackup.full.threading.tasks.BackupEverything;
import com.bukkitbackup.full.threading.tasks.BackupPlugins;
import com.bukkitbackup.full.threading.tasks.BackupWorlds;
import com.bukkitbackup.full.utils.FileUtils;
import com.bukkitbackup.full.utils.LogUtils;
import com.bukkitbackup.full.utils.MetricUtils;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Backup - The simple server backup solution.
 *
 * @author Domenic Horner (gamerx)
 */
public class BackupFull extends JavaPlugin {

    // Public variables for class comms.
    private static PrepareBackup prepareBackup;
    public static BackupTask backupTask;
    public static BackupWorlds backupWorlds;
    public static BackupPlugins backupPlugins;
    public static BackupEverything backupEverything;
    
    // Private variables for this class.
    private static Settings settings;
    private static Strings strings;
    private File thisDataFolder;
    private String clientUID;

    @Override
    public void onLoad() {

        // Set Data Folder, Init log utils.
        thisDataFolder = this.getDataFolder();
        LogUtils.initLogUtils(this);
        FileUtils.checkFolderAndCreate(thisDataFolder);

        // Setup Configuration Files.
        strings = new Strings(new File(thisDataFolder, "strings.yml"));
        settings = new Settings(new File(thisDataFolder, "config.yml"), strings);

        // Run version checking on configurations.
        //@TODO Refactor Settings & Strings Loading Code.
        strings.checkStringsVersion(settings.getStringProperty("requiredstrings", ""));
        settings.checkSettingsVersion(this.getDescription().getVersion());

        // Complete loading log utils.
        LogUtils.finishInitLogUtils(settings.getBooleanProperty("displaylog", true), settings.getBooleanProperty("debugenabled", false));

        // BukkitMetrics Loading. (Not Plugin-Specific)
        try {
            MetricUtils metricUtils = new MetricUtils(this);
            metricUtils.start();
            clientUID = metricUtils.guid;
        } catch (IOException ex) {
            LogUtils.exceptionLog(ex, "Exception loading metrics.");
        }

    }

    @Override
    public void onEnable() {

        // Get server and plugin manager instances.
        Server pluginServer = getServer();
        PluginManager pluginManager = pluginServer.getPluginManager();

        // Check backup path.
        FileUtils.checkFolderAndCreate(new File(settings.getStringProperty("backuppath", "backups")));

        // Setup backup tasks.
        backupEverything = new BackupEverything(settings);
        backupWorlds = new BackupWorlds(pluginServer, settings, strings);
        backupPlugins = new BackupPlugins(settings, strings);
        backupTask = new BackupTask(this, settings, strings);

        // Create new "PrepareBackup" instance.
        prepareBackup = new PrepareBackup(this, settings, strings);

        // Initalize Command Listener.
        getCommand("backup").setExecutor(new CommandHandler(prepareBackup, this, settings, strings));
        getCommand("bu").setExecutor(new CommandHandler(prepareBackup, this, settings, strings));

        // Initalize Event Listener.
        EventListener eventListener = new EventListener(prepareBackup, this, settings, strings);
        pluginManager.registerEvents(eventListener, this);

        // Check if the main backup should run at specific times.
        String backupInterval = settings.getStringProperty("backupinterval", "15M").trim().toLowerCase();
        int backupMinutes = 0;
        String[] backupSchedArray = null;

        // Use regex to match the backup interval string.
        if (backupInterval.matches("^[0-9]+$")) {

            // Parse it for minutes.
            backupMinutes = Integer.parseInt(backupInterval);
            LogUtils.sendDebug("Entry is set to minutes. (M:0002)");

        } else if (backupInterval.matches("[0-9]+[a-z]")) {

            // Parse it for larger time periods.
            Pattern timePattern = Pattern.compile("^([0-9]+)[a-z]$");
            Matcher amountTime = timePattern.matcher(backupInterval);
            Pattern letterPattern = Pattern.compile("^[0-9]+([a-z])$");
            Matcher letterTime = letterPattern.matcher(backupInterval);
            if (letterTime.matches() && amountTime.matches()) {
                String letter = letterTime.group(1);
                int time = Integer.parseInt(amountTime.group(1));
                if (letter.equals("m")) {
                    backupMinutes = time;
                } else if (letter.equals("h")) {
                    backupMinutes = time * 60;
                } else if (letter.equals("d")) {
                    backupMinutes = time * 1440;
                } else if (letter.equals("w")) {
                    backupMinutes = time * 10080;
                } else {
                    LogUtils.sendLog(strings.getString("unknowntimeident"));
                    backupMinutes = time;
                }
            } else {
                LogUtils.sendLog(strings.getString("checkbackupinterval"));
            }
            LogUtils.sendDebug("Found correctly-formatted time (M:0001)");

        } else if (backupInterval.matches("^ta\\[(.*)\\]$")) {

            // Found time-array string.
            Pattern letterPattern = Pattern.compile("^ta\\[(.*)\\]$");
            Matcher array = letterPattern.matcher(backupInterval);
            backupSchedArray = array.toString().split(",");
            LogUtils.sendDebug("Found time array string. (M:0003)");

        } else {

            // Nothing found.
            LogUtils.sendLog(strings.getString("checkbackupinterval"));
            backupMinutes = 0;
            LogUtils.sendDebug("No correct backup interval string found. (M:0004)");

        }

        if (backupMinutes != -1 && backupMinutes != 0) {

            LogUtils.sendDebug("Doing recurring backup interval code. (M:0005)");

            // Convert to server ticks.
            int backupIntervalInTicks = (backupMinutes * 1200);

            // Should the schedule repeat?
            if (settings.getBooleanProperty("norepeat", false)) {
                pluginServer.getScheduler().scheduleAsyncDelayedTask(this, prepareBackup, backupIntervalInTicks);
                LogUtils.sendLog(strings.getString("norepeatenabled", Integer.toString(backupMinutes)));
            } else {
                pluginServer.getScheduler().scheduleAsyncRepeatingTask(this, prepareBackup, backupIntervalInTicks, backupIntervalInTicks);
            }
        } else if (backupSchedArray != null) {

            LogUtils.sendDebug("Doing time array backup code. (M:0006)");
            BackupScheduler backupScheduler = new BackupScheduler(this, prepareBackup, settings, strings, backupSchedArray);
            pluginServer.getScheduler().scheduleAsyncDelayedTask(this, backupScheduler);

        } else {

            LogUtils.sendDebug("Disabled automatic backup. (M:0007)");
            LogUtils.sendLog(strings.getString("disbaledauto"));
        }

        // Configure save-all schedule.
        int saveAllInterval = settings.getSaveAllInterval();
        if (saveAllInterval != 0 && saveAllInterval != -1) {

            // Convert to server ticks.
            int saveAllIntervalInTicks = (saveAllInterval * 1200);

            LogUtils.sendLog(strings.getString("savealltimeron", Integer.toString(saveAllInterval)));

            // Syncronised save-all.

            // Create new Runnable instance.
            Runnable saveAllTask = new Runnable() {

                public Server pluginServer = BackupFull.this.getServer();

                @Override
                public void run() {
                    pluginServer.savePlayers();
                    for (World world : pluginServer.getWorlds()) {
                        world.save();
                    }

                }
            };
            pluginServer.getScheduler().scheduleSyncRepeatingTask(this, saveAllTask, saveAllIntervalInTicks, saveAllIntervalInTicks);
        }

        // Update & version checking loading.
        if (settings.getBooleanProperty("enableversioncheck", true)) {
            pluginServer.getScheduler().scheduleAsyncDelayedTask(this, new UpdateChecker(this.getDescription(), strings, clientUID));
        }

        // Notify loading complete.
        LogUtils.sendLog(this.getDescription().getFullName() + " has completed loading!");
    }

    @Override
    public void onDisable() {

        // Stop and scheduled tasks.
        this.getServer().getScheduler().cancelTasks(this);

        // Shutdown complete.
        LogUtils.sendLog(this.getDescription().getFullName() + " has completely un-loaded!");
    }
}
