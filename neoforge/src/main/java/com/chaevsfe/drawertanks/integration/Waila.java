package com.chaevsfe.drawertanks.integration;

import com.chaevsfe.drawertanks.ModConstants;
import com.chaevsfe.drawertanks.block.BlockLinkedDrawer;
import com.chaevsfe.drawertanks.block.BlockTank;
import com.chaevsfe.drawertanks.block.tile.BlockEntityLinkedDrawer;
import com.chaevsfe.drawertanks.block.tile.BlockEntityTank;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.JadeUI;

@snownee.jade.api.WailaPlugin(ModConstants.MOD_ID)
public class Waila implements IWailaPlugin
{
    private static final Identifier CONTENT = ModConstants.loc("display.content");
    private static final Identifier CAPACITY = ModConstants.loc("display.capacity");
    private static final Identifier STATUS = ModConstants.loc("display.status");
    private static final Identifier CHANNEL = ModConstants.loc("display.channel");

    @Override
    public void registerClient (IWailaClientRegistration registration) {
        registration.addConfig(CONTENT, true);
        registration.addConfig(CAPACITY, true);
        registration.addConfig(STATUS, true);
        registration.addConfig(CHANNEL, true);

        registration.registerBlockComponent(new WailaTank(), BlockTank.class);
        registration.registerBlockComponent(new WailaLinkedDrawer(), BlockLinkedDrawer.class);
    }

    private static TankOverlay overlay (IPluginConfig config) {
        TankOverlay overlay = new TankOverlay();
        overlay.showContent = config.get(CONTENT);
        overlay.showCapacity = config.get(CAPACITY);
        overlay.showStatus = config.get(STATUS);
        overlay.showChannel = config.get(CHANNEL);
        return overlay;
    }

    public static class WailaTank implements IBlockComponentProvider
    {
        @Override
        public @Nullable Element getIcon (BlockAccessor accessor, IPluginConfig config, Element currentIcon) {
            return JadeUI.item(new ItemStack(accessor.getBlock()));
        }

        @Override
        public void appendTooltip (ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (accessor.getBlockEntity() instanceof BlockEntityTank tank)
                tooltip.addAll(overlay(config).tank(tank));
        }

        @Override
        public Identifier getUid () {
            return ModConstants.loc("tank");
        }
    }

    public static class WailaLinkedDrawer implements IBlockComponentProvider
    {
        @Override
        public @Nullable Element getIcon (BlockAccessor accessor, IPluginConfig config, Element currentIcon) {
            return JadeUI.item(new ItemStack(accessor.getBlock()));
        }

        @Override
        public void appendTooltip (ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (accessor.getBlockEntity() instanceof BlockEntityLinkedDrawer drawer)
                tooltip.addAll(overlay(config).drawer(drawer));
        }

        @Override
        public Identifier getUid () {
            return ModConstants.loc("linked_drawer");
        }
    }
}
