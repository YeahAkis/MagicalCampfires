package org.notionsmp.magicalcampfireFabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class MagicalCampfireFabric implements ModInitializer {
    public static MagicalCampfireFabric INSTANCE;
    private MagicalCampfireSettings campfire;
    private MagicalCampfireSettings soulCampfire;
    private boolean enabled;
    private MinecraftServer server;

    @Override
    public void onInitialize() {
        INSTANCE = this;
        reloadConfig();

        ServerLifecycleEvents.SERVER_STARTED.register((MinecraftServer srv) -> {
            server = srv;
            startTimers();
        });
    }

    public void reloadConfig() {
        File configFile = new File("config/magicalcampfire.json");
        MagicalCampfireConfig config = MagicalCampfireConfig.load(configFile);
        enabled = config.enabled;
        campfire = config.campfire;
        soulCampfire = config.soulCampfire;
    }

    private void startTimers() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!enabled || !campfire.enabled) return;
                for (Level Level : server.getAllLevels()) {
                    for (var player : Level.players()) {
                        if (player instanceof ServerPlayer serverPlayer)
                            handleCampfire(serverPlayer, Blocks.CAMPFIRE, campfire);
                    }
                }
            }
        }, 0, campfire.interval);

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!enabled || !soulCampfire.enabled) return;
                for (Level Level : server.getAllLevels()) {
                    for (var player : Level.players()) {
                        if (player instanceof ServerPlayer serverPlayer)
                            handleCampfire(serverPlayer, Blocks.SOUL_CAMPFIRE, soulCampfire);
                    }
                }
            }
        }, 0, soulCampfire.interval);
    }

    private void handleCampfire(ServerPlayer player, Block block, MagicalCampfireSettings settings) {
        BlockPos center = BlockPos.containing(player.position());
        Level Level = player.level();
        boolean found = false;

        for (int dx = -settings.range; dx <= settings.range && !found; dx++) {
            for (int dy = -settings.range; dy <= settings.range && !found; dy++) {
                for (int dz = -settings.range; dz <= settings.range && !found; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = Level.getBlockState(pos);
                    if (state.getBlock() == block && state.hasProperty(CampfireBlock.LIT) && state.getValue(CampfireBlock.LIT)) {
                        found = true;
                    }
                }
            }
        }

        if (found) {
            float newHealth = Math.min(player.getHealth() + (float) settings.amount, player.getMaxHealth());
            player.setHealth(newHealth);
            if (settings.actionbarEnabled) {
                player.sendSystemMessage(Component.literal(settings.actionbarMessage), true);
            }
        }
    }
}
