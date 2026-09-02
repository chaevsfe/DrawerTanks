package com.chaevsfe.drawertanks.block.tile;

import com.chaevsfe.drawertanks.core.ModDataComponents;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.DyeColor;

import java.util.ArrayList;
import java.util.List;

// Carries a linked block's five dye strips across break and place so its channel is not orphaned.
public final class LinkedChannelData
{
    private LinkedChannelData () { }

    public static void collect (DataComponentMap.Builder builder, DyeColor[] channels) {
        boolean any = false;
        List<Integer> ids = new ArrayList<>(channels.length);
        for (DyeColor color : channels) {
            DyeColor value = color == null ? DyeColor.WHITE : color;
            any |= value != DyeColor.WHITE;
            ids.add(value.getId());
        }

        if (any)
            builder.set(ModDataComponents.LINK_CHANNELS.get(), ids);
    }

    public static void apply (DataComponentGetter input, DyeColor[] channels) {
        applyIds(input.get(ModDataComponents.LINK_CHANNELS.get()), channels);
    }

    public static void applyIds (List<Integer> ids, DyeColor[] channels) {
        if (ids == null)
            return;

        for (int i = 0; i < channels.length; i++) {
            DyeColor color = i < ids.size() && ids.get(i) >= 0 ? DyeColor.byId(ids.get(i)) : null;
            channels[i] = color == null ? DyeColor.WHITE : color;
        }
    }

    public static boolean isBlank (DyeColor[] channels) {
        for (DyeColor color : channels) {
            if (color != null && color != DyeColor.WHITE)
                return false;
        }
        return true;
    }

    public static Component channelName (DyeColor[] channels) {
        if (isBlank(channels))
            return Component.translatable("tooltip.drawertanks.link.blank");

        MutableComponent line = null;
        for (DyeColor color : channels) {
            DyeColor dye = color == null ? DyeColor.WHITE : color;
            Component name = Component.translatable("color.minecraft." + dye.getSerializedName());
            line = line == null ? name.copy() : line.append("/").append(name);
        }
        return line == null ? Component.empty() : line;
    }

    // an item with no component is on the all-white channel
    public static Component channelLine (List<Integer> ids) {
        DyeColor[] channels = new DyeColor[BlockEntityLinkedTank.STRIPS];
        java.util.Arrays.fill(channels, DyeColor.WHITE);
        applyIds(ids, channels);
        return Component.translatable("tooltip.drawertanks.link.channel", channelName(channels)).withStyle(ChatFormatting.GRAY);
    }
}
