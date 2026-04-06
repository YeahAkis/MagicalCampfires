package org.notionsmp.magicalCampfire;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

public class MagicalCampfireListener implements Listener {
    private boolean globalEnabled;
    private CampfireSettings campfireSettings;
    private CampfireSettings soulCampfireSettings;

    public MagicalCampfireListener() {
        loadSettings();
        startCampfireTask();
        startSoulCampfireTask();
    }

    public void reload() {
        loadSettings();
    }

    private void loadSettings() {
        FileConfiguration config = MagicalCampfire.getInstance().getConfig();
        ConfigurationSection settings = config.getConfigurationSection("settings");

        globalEnabled = settings.getBoolean("enabled", true);
        campfireSettings = new CampfireSettings(settings.getConfigurationSection("campfire"));
        soulCampfireSettings = new CampfireSettings(settings.getConfigurationSection("soul_campfire"));
    }

    private void startCampfireTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!globalEnabled || !campfireSettings.enabled) return;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    handleCampfire(player, Material.CAMPFIRE, campfireSettings);
                }
            }
        }.runTaskTimer(MagicalCampfire.getInstance(), 0L, campfireSettings.interval);
    }

    private void startSoulCampfireTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!globalEnabled || !soulCampfireSettings.enabled) return;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    handleCampfire(player, Material.SOUL_CAMPFIRE, soulCampfireSettings);
                }
            }
        }.runTaskTimer(MagicalCampfire.getInstance(), 0L, soulCampfireSettings.interval);
    }

    private void handleCampfire(Player player, Material type, CampfireSettings settings) {
        Location playerLoc = player.getLocation();
        if (player.isDead() || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        boolean inRange = false;

        for (int x = -settings.range; x <= settings.range; x++) {
            for (int y = -settings.range; y <= settings.range; y++) {
                for (int z = -settings.range; z <= settings.range; z++) {
                    Location loc = playerLoc.clone().add(x, y, z);
                    Block block = loc.getBlock();
                    if (block.getType() == type) {
                        if (!settings.isLit) {
                            inRange = true;
                            break;
                        }
                        BlockData data = block.getBlockData();
                        if (data instanceof Campfire && ((Campfire) data).isLit()) {
                            inRange = true;
                            break;
                        }
                    }
                }
            }
        }

        if (inRange) {
            double newHealth = Math.min(player.getHealth() + settings.amount, player.getMaxHealth());
            player.setHealth(newHealth);
            if (settings.actionbarEnabled) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(settings.actionbarMessage));
            }
        }
    }

    private static class CampfireSettings {
        public final boolean enabled;
        public final boolean isLit;
        public final int interval;
        public final double amount;
        public final int range;
        public final boolean actionbarEnabled;
        public final String actionbarMessage;

        public CampfireSettings(ConfigurationSection section) {
            enabled = section.getBoolean("enabled", true);
            isLit = section.getBoolean("is_lit", true);
            interval = section.getInt("interval", 40);
            amount = section.getDouble("amount", 1);
            range = section.getInt("range", 3);
            actionbarEnabled = section.getConfigurationSection("actionbar").getBoolean("enabled", false);
            actionbarMessage = section.getConfigurationSection("actionbar").getString("message", "§aYou're getting healed.");
        }
    }
}