package com.chaevsfe.drawertanks.item;

import com.chaevsfe.drawertanks.block.tile.BlockEntityLinkedTank;
import com.chaevsfe.drawertanks.core.ModDataComponents;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class ItemTankCoupler extends Item
{
    public ItemTankCoupler (Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn (UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();

        if (!(level.getBlockEntity(context.getClickedPos()) instanceof BlockEntityLinkedTank tank))
            return InteractionResult.PASS;

        if (level.isClientSide())
            return InteractionResult.SUCCESS;

        ItemStack stack = context.getItemInHand();
        GlobalPos here = GlobalPos.of(level.dimension(), context.getClickedPos());

        if (player != null && player.isShiftKeyDown()) {
            tank.setPartner(null);
            stack.remove(ModDataComponents.COUPLER_TARGET.get());
            message(player, "unlinked");
            return InteractionResult.SUCCESS;
        }

        GlobalPos selected = stack.get(ModDataComponents.COUPLER_TARGET.get());
        if (selected == null) {
            stack.set(ModDataComponents.COUPLER_TARGET.get(), here);
            message(player, "source_selected");
            return InteractionResult.SUCCESS;
        }

        if (selected.equals(here)) {
            message(player, "same_tank");
            return InteractionResult.SUCCESS;
        }

        tank.setPartner(selected);
        stack.remove(ModDataComponents.COUPLER_TARGET.get());
        message(player, "linked");
        return InteractionResult.SUCCESS;
    }

    private static void message (Player player, String key) {
        if (player != null)
            player.sendOverlayMessage(Component.translatable("message.drawertanks.coupler." + key));
    }
}
