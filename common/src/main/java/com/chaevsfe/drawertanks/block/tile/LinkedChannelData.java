package com.chaevsfe.drawertanks.block.tile;

import com.chaevsfe.drawertanks.core.ModDataComponents;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
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
        List<Integer> ids = input.get(ModDataComponents.LINK_CHANNELS.get());
        if (ids == null)
            return;

        for (int i = 0; i < channels.length; i++) {
            DyeColor color = i < ids.size() && ids.get(i) >= 0 ? DyeColor.byId(ids.get(i)) : null;
            channels[i] = color == null ? DyeColor.WHITE : color;
        }
    }
}
