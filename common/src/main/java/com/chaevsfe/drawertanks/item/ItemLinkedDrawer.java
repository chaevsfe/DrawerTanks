package com.chaevsfe.drawertanks.item;

import com.chaevsfe.drawertanks.block.tile.LinkedChannelData;
import com.chaevsfe.drawertanks.core.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

public class ItemLinkedDrawer extends BlockItem
{
    public ItemLinkedDrawer (Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText (ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);

        tooltip.accept(LinkedChannelData.channelLine(stack.get(ModDataComponents.LINK_CHANNELS.get())));

        ItemStackTemplate held = stack.get(ModDataComponents.LINK_ITEM.get());
        if (held != null)
            tooltip.accept(Component.translatable("tooltip.drawertanks.link.holds", held.create().getHoverName()).withStyle(ChatFormatting.GRAY));
    }
}
