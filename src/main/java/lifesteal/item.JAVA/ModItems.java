package com.lifesteal.item;

import com.lifesteal.LifeStealMod;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class ModItems {

    public static ItemStack createHeartItem(int count) {
        ItemStack stack = new ItemStack(Items.APPLE, count);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Heart").formatted(Formatting.RED, Formatting.BOLD));
        stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(991001));
        
        LoreComponent lore = new LoreComponent(List.of(
                Text.literal("Right-click to consume").formatted(Formatting.GRAY),
                Text.literal("Adds +1 permanent heart.").formatted(Formatting.DARK_RED)
        ));
        stack.set(DataComponentTypes.LORE, lore);
        return stack;
    }

    public static ItemStack createReviveBeaconItem() {
        ItemStack stack = new ItemStack(Items.BEACON, 1);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Revive Beacon").formatted(Formatting.GOLD, Formatting.BOLD));
        stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(991002));
        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        
        LoreComponent lore = new LoreComponent(List.of(
                Text.literal("Right-click to open").formatted(Formatting.GRAY),
                Text.literal("the Revival Screen.").formatted(Formatting.YELLOW)
        ));
        stack.set(DataComponentTypes.LORE, lore);
        return stack;
    }

    public static boolean isHeart(ItemStack stack) {
        if (stack.getItem() != Items.APPLE) return false;
        CustomModelDataComponent model = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        return model != null && model.value() == 991001;
    }

    public static boolean isReviveBeacon(ItemStack stack) {
        if (stack.getItem() != Items.BEACON) return false;
        CustomModelDataComponent model = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        return model != null && model.value() == 991002;
    }
}