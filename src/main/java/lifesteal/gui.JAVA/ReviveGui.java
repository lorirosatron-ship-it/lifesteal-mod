package com.lifesteal.gui;

import com.lifesteal.LifeStealMod;
import com.lifesteal.data.LifeStealState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import com.mojang.authlib.GameProfile;

import java.text.SimpleDateFormat;
import java.util.*;

public class ReviveGui {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public static void open(ServerPlayerEntity player) {
        LifeStealState state = LifeStealState.getServerState(player.getServer());
        List<LifeStealState.DeadPlayerData> deadList = new ArrayList<>(state.deadPlayers.values());

        if (deadList.isEmpty()) {
            player.sendMessage(Text.literal("No players are awaiting revival.").formatted(Formatting.YELLOW), false);
            return;
        }

        int slots = ((deadList.size() / 9) + 1) * 9;
        if (slots > 54) slots = 54;

        SimpleInventory inv = new SimpleInventory(slots);
        Map<Integer, UUID> slotToUuidMap = new HashMap<>();

        for (int i = 0; i < Math.min(deadList.size(), slots); i++) {
            LifeStealState.DeadPlayerData data = deadList.get(i);
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(DataComponentTypes.PROFILE, new ProfileComponent(new GameProfile(data.uuid, data.username)));
            head.set(DataComponentTypes.CUSTOM_NAME, Text.literal(data.username).formatted(Formatting.GOLD, Formatting.BOLD));
            
            LoreComponent lore = new LoreComponent(List.of(
                    Text.literal("Death Date: ").formatted(Formatting.GRAY).append(Text.literal(DATE_FORMAT.format(new Date(data.timestamp))).formatted(Formatting.WHITE)),
                    Text.literal("Hearts Lost: ").formatted(Formatting.GRAY).append(Text.literal(String.valueOf(data.heartsLost)).formatted(Formatting.RED)),
                    Text.literal("Killer: ").formatted(Formatting.GRAY).append(Text.literal(data.killerName).formatted(Formatting.WHITE)),
                    Text.literal("Dimension: ").formatted(Formatting.GRAY).append(Text.literal(data.dimension).formatted(Formatting.DARK_GREEN)),
                    Text.literal("Location: ").formatted(Formatting.GRAY).append(Text.literal(String.format("X: %.1f Y: %.1f Z: %.1f", data.x, data.y, data.z)).formatted(Formatting.GREEN)),
                    Text.literal(""),
                    Text.literal("Click to Revive!").formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC)
            ));
            head.set(DataComponentTypes.LORE, lore);
            inv.setStack(i, head);
            slotToUuidMap.put(i, data.uuid);
        }

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, playerInv, p) -> 
            new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X3, syncId, playerInv, inv, 3) {
                @Override
                public boolean onSlotClick(int slotIndex, int button, net.minecraft.screen.slot.SlotActionType actionType, PlayerEntity playerEntity) {
                    if (slotIndex >= 0 && slotIndex < inv.size()) {
                        UUID targetUuid = slotToUuidMap.get(slotIndex);
                        if (targetUuid != null && playerEntity instanceof ServerPlayerEntity spe) {
                            spe.closeHandledScreen();
                            LifeStealMod.executeRevival(spe.getServer(), targetUuid, spe, false);
                        }
                    }
                    return false;
                }
                @Override public boolean canUse(PlayerEntity player) { return true; }
            }, Text.literal("Revive Players")));
    }
}