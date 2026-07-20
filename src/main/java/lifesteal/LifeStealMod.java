package com.lifesteal;

import com.lifesteal.command.LifeStealCommands;
import com.lifesteal.config.ModConfig;
import com.lifesteal.data.LifeStealState;
import com.lifesteal.gui.ReviveGui;
import com.lifesteal.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import java.util.UUID;

public class LifeStealMod implements ModInitializer {
    public static final String MOD_ID = "lifesteal";
    public static ModConfig CONFIG;

    @Override
    public void onInitialize() {
        CONFIG = ModConfig.load();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LifeStealCommands.register(dispatcher);
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            LifeStealState state = LifeStealState.getServerState(newPlayer.getServer());
            int currentHearts = state.getHearts(oldPlayer, CONFIG.startingHearts);
            state.updateMaxHealthAttribute(newPlayer, currentHearts);
        });

        ServerPlayerEvents.ALLOW_DEATH.register((player, damageSource, damageAmount) -> {
            MinecraftServer server = player.getServer();
            if (server == null) return true;

            LifeStealState state = LifeStealState.getServerState(server);
            int currentHearts = state.getHearts(player, CONFIG.startingHearts);

            if (damageSource.getAttacker() instanceof ServerPlayerEntity killer) {
                int killerHearts = state.getHearts(killer, CONFIG.startingHearts);
                if (killerHearts < CONFIG.maxHearts) {
                    state.setHearts(killer, killerHearts + 1);
                    killer.playSoundToPlayer(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0F, 1.0F);
                }
                
                if (CONFIG.broadcastKills) {
                    server.getPlayerManager().broadcast(Text.literal(killer.getName().getString() + " stole a heart from " + player.getName().getString() + "!").formatted(Formatting.RED), false);
                }
            }

            int finalHearts = currentHearts - 1;
            state.setHearts(player, Math.max(finalHearts, 0));

            if (finalHearts <= 0) {
                LifeStealState.DeadPlayerData data = new LifeStealState.DeadPlayerData();
                data.uuid = player.getUuid();
                data.username = player.getName().getString();
                data.dimension = player.getWorld().getRegistryKey().getValue().toString();
                data.x = player.getX();
                data.y = player.getY();
                data.z = player.getZ();
                data.timestamp = System.currentTimeMillis();
                data.killerName = damageSource.getAttacker() != null ? damageSource.getAttacker().getName().getString() : "Environment";
                data.heartsLost = currentHearts;

                state.deadPlayers.put(player.getUuid(), data);
                state.markDirty();

                server.execute(() -> {
                    player.networkHandler.disconnect(Text.literal("You have lost all your hearts.").formatted(Formatting.RED));
                });
            }
            return true;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient()) return ActionResult.PASS;
            ServerPlayerEntity spe = (ServerPlayerEntity) player;
            ItemStack stack = player.getStackInHand(hand);

            if (ModItems.isHeart(stack)) {
                LifeStealState state = LifeStealState.getServerState(spe.getServer());
                int currentHearts = state.getHearts(spe, CONFIG.startingHearts);
                if (currentHearts >= CONFIG.maxHearts) {
                    spe.sendMessage(Text.literal("You have already reached maximum hearts!").formatted(Formatting.RED), true);
                    return ActionResult.FAIL;
                }
                state.setHearts(spe, currentHearts + 1);
                stack.decrement(1);
                spe.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0F, 1.2F);
                return ActionResult.SUCCESS;
            }

            if (ModItems.isReviveBeacon(stack)) {
                ReviveGui.open(spe);
                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        });
    }

    public static void executeRevival(MinecraftServer server, UUID targetUuid, ServerPlayerEntity reviver, boolean isAdmin) {
        LifeStealState state = LifeStealState.getServerState(server);
        LifeStealState.DeadPlayerData data = state.deadPlayers.remove(targetUuid);
        if (data == null) return;

        state.playerHearts.put(targetUuid, 10);
        state.markDirty();

        if (!isAdmin && reviver != null) {
            ItemStack handStack = reviver.getStackInHand(Hand.MAIN_HAND);
            if (ModItems.isReviveBeacon(handStack)) {
                handStack.decrement(1);
            } else {
                ItemStack offStack = reviver.getStackInHand(Hand.OFF_HAND);
                if (ModItems.isReviveBeacon(offStack)) {
                    offStack.decrement(1);
                }
            }
        }

        server.getCommandManager().executeWithPrefix(server.getCommandSource(), "pardon " + data.username);

        if (CONFIG.broadcastRevives) {
            server.getPlayerManager().broadcast(Text.literal(data.username + " has been revived!").formatted(Formatting.GREEN, Formatting.BOLD), false);
        }
    }
}