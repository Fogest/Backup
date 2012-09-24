package com.bukkitbackup.full.events;

import com.bukkitbackup.full.config.Settings;
import com.bukkitbackup.full.config.Strings;
import com.bukkitbackup.full.threading.PrepareBackup;
import com.bukkitbackup.full.utils.LogUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/**
 * Backup - The simple server backup solution.
 *
 * @author Domenic Horner (gamerx)
 */
public class EventListener implements Listener {

    private PrepareBackup prepareBackup = null;
    private Plugin plugin;
    private Settings settings;
    private Strings strings;
    private int lastBackupID;

    /**
     * Constructor for listening for login events.
     *
     * @param backupTask The BackupTast to call.
     * @param plugin Plugin to link this class too.
     */
    public EventListener(PrepareBackup backupTask, Plugin plugin, Settings settings, Strings strings) {
        this.prepareBackup = backupTask;
        this.plugin = plugin;
        this.settings = settings;
        this.strings = strings;
        lastBackupID = -2;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerPart(event);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerKick(PlayerKickEvent event) {
        playerPart(event);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        playerJoin(event);
    }

    /**
     * Called when a player leaves the server.
     *
     */
    // @TODO determine how we should handle this if backups are at specific times.
    // for now, i set it to 15 mins.
    private void playerPart(PlayerEvent event) {
        int onlinePlayers = plugin.getServer().getOnlinePlayers().length;
        // Check if it was the last player, and we need to stop backups after this last player leaves.
        if (onlinePlayers == 1 && !settings.getBooleanProperty("backupemptyserver", false)) {
            prepareBackup.isLastBackup = true;
            //int intervalInMinutes = settings.getBackupInterval();
            int intervalInMinutes = 15;
            if (intervalInMinutes != 0) {
                int interval = intervalInMinutes * 1200;
                lastBackupID = plugin.getServer().getScheduler().scheduleAsyncDelayedTask(plugin, prepareBackup, interval);
                LogUtils.sendLog(strings.getString("schedlastbackup", Integer.toString(intervalInMinutes)));
            } else {
                LogUtils.sendLog(strings.getString("disbaledauto"));
            }
        }
    }

    /**
     * Called when a player joins the server.
     *
     */
    private void playerJoin(PlayerEvent event) {
        if (lastBackupID != -2) {
            plugin.getServer().getScheduler().cancelTask(lastBackupID);
            lastBackupID = -2;
            prepareBackup.isLastBackup = false;
            LogUtils.sendLog(strings.getString("stoppedlastjoined"));
        }
    }
}
