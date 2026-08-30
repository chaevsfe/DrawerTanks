package com.chaevsfe.drawertanks.item;

import com.chaevsfe.drawertanks.block.tile.BlockEntityTank;
import com.chaevsfe.drawertanks.components.TankContents;
import com.chaevsfe.drawertanks.components.TankUpgrades;
import com.chaevsfe.drawertanks.core.ModDataComponents;
import com.chaevsfe.drawertanks.platform.Bridges;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ItemTank extends BlockItem
{
    public ItemTank (Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText (ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);

        long capacityBuckets = capacityFor(stack) / BlockEntityTank.DROPLETS_PER_BUCKET;

        TankContents contents = stack.get(ModDataComponents.TANK_CONTENTS.get());
        if (contents != null && !contents.isEmpty()) {
            Component name = Bridges.CLIENT_FLUID != null
                ? Bridges.CLIENT_FLUID.fluidName(contents.fluid(), contents.components())
                : Component.translatable(contents.fluid().defaultFluidState().createLegacyBlock().getBlock().getDescriptionId());
            tooltip.accept(Component.translatable("tooltip.drawertanks.contents",
                name, buckets(contents.amount()), capacityBuckets).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.accept(Component.translatable("tooltip.drawertanks.empty").withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable("tooltip.drawertanks.capacity", capacityBuckets).withStyle(ChatFormatting.GRAY));
        }
    }

    private static long capacityFor (ItemStack stack) {
        List<ItemStack> upgrades = new ArrayList<>();
        TankUpgrades component = stack.get(ModDataComponents.TANK_UPGRADES.get());
        if (component != null) {
            for (ItemStackWithSlot slotStack : component.upgrades())
                upgrades.add(slotStack.stack());
        }
        return BlockEntityTank.computeCapacityDroplets(upgrades);
    }

    private static String buckets (long droplets) {
        double value = droplets / (double) BlockEntityTank.DROPLETS_PER_BUCKET;
        if (value == Math.floor(value))
            return Long.toString((long) value);
        return String.format("%.1f", value);
    }
}
