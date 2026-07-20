package com.lifesteal.command;

import com.lifesteal.LifeStealMod;
import com.lifesteal.config.ModConfig;
import com.lifesteal.data.LifeStealState;
import com.lifesteal.item.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

public class LifeStealCommands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        
        dispatcher.register(CommandManager.literal("withdraw")
                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 64))
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                            if (!LifeStealMod.CONFIG.enableWithdraw) {
                                player.sendMessage(Text.literal("Withdrawal is disabled.").formatted(Formatting.RED), false);
                                return 0;
                            }
                            int amount = IntegerArgumentType.getInteger(context, "amount");
                            LifeStealState state = LifeStealState.getServerState(player.getServer());
                            int currentHearts = state.getHearts(player, LifeStealMod.CONFIG.startingHearts);

                            if (currentHearts - amount < LifeStealMod.CONFIG.minHearts) {
                                player.sendMessage(Text.literal("You cannot withdraw past your minimum heart count!").formatted(Formatting.RED), false);
                                return 0;
                            }

                            state.setHearts(player, currentHearts - amount);
                            player.getInventory().offerOrDrop(ModItems.createHeartItem(amount));
                            player.sendMessage(Text.literal("Successfully withdrew " + amount + " heart(s).").formatted(Formatting.GREEN), false);
                            return 1;
                        })));

        dispatcher.register(CommandManager.literal("lifesteal")
                .then(CommandManager.literal("status")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                            LifeStealState state = LifeStealState.getServerState(player.getServer());
                            int current = state.getHearts(player, LifeStealMod.CONFIG.startingHearts);
                            player.sendMessage(Text.literal("Current Hearts: ").formatted(Formatting.GRAY).append(Text.literal(String.valueOf(current)).formatted(Formatting.RED)), false);
                            player.sendMessage(Text.literal("Max Hearts: ").formatted(Formatting.GRAY).append(Text.literal(String.valueOf(LifeStealMod.CONFIG.maxHearts)).formatted(Formatting.DARK_RED)), false);
                            return 1;
                        }))
                .then(CommandManager.literal("reload")
                        .requires(source -> source.hasPermissionLevel(4))
                        .executes(context -> {
                            LifeStealMod.CONFIG = ModConfig.load();
                            context.getSource().sendMessage(Text.literal("LifeSteal configuration reloaded!").formatted(Formatting.GREEN));
                            return 1;
                        }))
                .then(CommandManager.literal("revive")
                        .requires(source -> source.hasPermissionLevel(4))
                        .then(CommandManager.argument("player", StringArgumentType.string())
                                .executes(context -> {
                                    String name = StringArgumentType.getString(context, "player");
                                    LifeStealState state = LifeStealState.getServerState(context.getSource().getServer());
                                    UUID foundUuid = null;
                                    for (LifeStealState.DeadPlayerData data : state.deadPlayers.values()) {
                                        if (data.username.equalsIgnoreCase(name)) {
                                            foundUuid = data.uuid;
                                            break;
                                        }
                                    }
                                    if (foundUuid == null) {
                                        context.getSource().sendMessage(Text.literal("Player not found in data stores.").formatted(Formatting.RED));
                                        return 0;
                                    }
                                    LifeStealMod.executeRevival(context.getSource().getServer(), foundUuid, null, true);
                                    context.getSource().sendMessage(Text.literal(name + " has been administratively revived.").formatted(Formatting.GREEN));
                                    return 1;
                                })))
                .then(CommandManager.literal("sethearts")
                        .requires(source -> source.hasPermissionLevel(4))
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 1000))
                                        .executes(context -> {
                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                            LifeStealState state = LifeStealState.getServerState(context.getSource().getServer());
                                            state.setHearts(target, amount);
                                            context.getSource().sendMessage(Text.literal("Set hearts of " + target.getName().getString() + " to " + amount).formatted(Formatting.GREEN));
                                            return 1;
                                        }))))
                .then(CommandManager.literal("giveheart")
                        .requires(source -> source.hasPermissionLevel(4))
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> {
                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                            target.getInventory().offerOrDrop(ModItems.createHeartItem(amount));
                                            return 1;
                                        }))))
                .then(CommandManager.literal("giverevivebeacon")
                        .requires(source -> source.hasPermissionLevel(4))
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                                .executes(context -> {
                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
                                    target.getInventory().offerOrDrop(ModItems.createReviveBeaconItem());
                                    return 1;
                                }))));
    }
}