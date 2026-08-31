package com.chaevsfe.drawertanks.integration;

import com.chaevsfe.drawertanks.block.tile.BlockEntityLinkedDrawer;
import com.chaevsfe.drawertanks.block.tile.BlockEntityLinkedTank;
import com.chaevsfe.drawertanks.block.tile.BlockEntityTank;
import com.chaevsfe.drawertanks.block.tile.tiledata.TankData;
import com.chaevsfe.drawertanks.platform.Bridges;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerAttributes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// no Jade types in here: common has no jade dependency, so each loader's plugin renders these lines
public final class TankOverlay
{
    public boolean showContent = true;
    public boolean showCapacity = true;
    public boolean showStatus = true;
    public boolean showChannel = true;

    public List<Component> tank (BlockEntityTank tank) {
        List<Component> out = new ArrayList<>();
        TankData data = tank.tankData();
        IDrawerAttributes attrs = tank.getDrawerAttributes();
        long capacity = tank.capacityDroplets();
        boolean unlimited = BlockEntityTank.isUnlimitedCapacity(capacity);

        if (showContent && !tank.isConcealed()) {
            if (data.hasFluid()) {
                Component name = fluidName(data);
                out.add(unlimited
                    ? Component.translatable("tooltip.drawertanks.waila.contents_nolimit", name, buckets(data.getAmount()))
                    : Component.translatable("tooltip.drawertanks.waila.contents", name, buckets(data.getAmount()), buckets(capacity)));
            }
            else
                out.add(Component.translatable("tooltip.drawertanks.waila.empty"));
        }

        if (showCapacity) {
            out.add(unlimited
                ? Component.translatable("tooltip.drawertanks.waila.nolimit")
                : Component.translatable("tooltip.drawertanks.waila.limit", buckets(capacity), tank.upgrades().getStorageMultiplier()));
        }

        if (showStatus)
            addStatus(out, tank.isFluidLocked(), tank.isVoid(), attrs.isUnlimitedStorage() || attrs.isUnlimitedVending(), tank.isConcealed());

        if (showChannel && tank instanceof BlockEntityLinkedTank linked)
            out.add(Component.translatable("tooltip.drawertanks.waila.channel", channel(linked.getChannels())));

        return out;
    }

    public List<Component> drawer (BlockEntityLinkedDrawer drawer) {
        List<Component> out = new ArrayList<>();
        IDrawerAttributes attrs = drawer.getDrawerAttributes();
        ItemStack item = drawer.displayItem();
        boolean unlimited = attrs.isUnlimitedStorage() || attrs.isUnlimitedVending();

        if (showContent && !drawer.isConcealed()) {
            out.add(item.isEmpty()
                ? Component.translatable("tooltip.drawertanks.waila.empty")
                : Component.translatable("tooltip.drawertanks.waila.items", item.getDisplayName(), drawer.displayCount()));
        }

        if (showCapacity) {
            if (unlimited)
                out.add(Component.translatable("tooltip.drawertanks.waila.nolimit_stacks"));
            else {
                int stackSize = item.isEmpty() ? 64 : item.getMaxStackSize();
                long stacks = drawer.capacityItems(item) / Math.max(1, stackSize);
                out.add(Component.translatable("tooltip.drawertanks.waila.limit_stacks", stacks, drawer.upgrades().getStorageMultiplier()));
            }
        }

        // a void upgrade fits a channel but the item handlers never read it, so it is not reported here
        if (showStatus)
            addStatus(out, drawer.isChannelLocked(), false, unlimited, drawer.isConcealed());

        if (showChannel)
            out.add(Component.translatable("tooltip.drawertanks.waila.channel", channel(drawer.getChannels())));

        return out;
    }

    private static void addStatus (List<Component> out, boolean locked, boolean voiding, boolean creative, boolean concealed) {
        List<Component> badges = new ArrayList<>();
        if (locked)
            badges.add(Component.translatable("tooltip.drawertanks.waila.locked"));
        if (voiding)
            badges.add(Component.translatable("tooltip.drawertanks.waila.void"));
        if (creative)
            badges.add(Component.translatable("tooltip.drawertanks.waila.creative"));
        if (concealed)
            badges.add(Component.translatable("tooltip.drawertanks.waila.concealed"));

        if (badges.isEmpty())
            return;

        MutableComponent line = badges.get(0).copy();
        for (int i = 1; i < badges.size(); i++)
            line.append(", ").append(badges.get(i));
        out.add(line);
    }

    private static Component channel (DyeColor[] channels) {
        MutableComponent line = null;
        for (DyeColor color : channels) {
            DyeColor dye = color == null ? DyeColor.WHITE : color;
            Component name = Component.translatable("color.minecraft." + dye.getSerializedName());
            line = line == null ? name.copy() : line.append("/").append(name);
        }
        return line == null ? Component.empty() : line;
    }

    private static Component fluidName (TankData data) {
        if (Bridges.CLIENT_FLUID != null)
            return Bridges.CLIENT_FLUID.fluidName(data.getFluid(), data.getComponents());

        return Component.translatable(data.getFluid().defaultFluidState().createLegacyBlock().getBlock().getDescriptionId());
    }

    private static String buckets (long droplets) {
        double value = droplets / (double) BlockEntityTank.DROPLETS_PER_BUCKET;
        if (value == Math.floor(value))
            return Long.toString((long) value);
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
