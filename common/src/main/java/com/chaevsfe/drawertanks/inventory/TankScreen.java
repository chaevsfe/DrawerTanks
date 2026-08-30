package com.chaevsfe.drawertanks.inventory;

import com.chaevsfe.drawertanks.ModConstants;
import com.chaevsfe.drawertanks.block.tile.BlockEntityTank;
import com.chaevsfe.drawertanks.block.tile.tiledata.TankData;
import com.chaevsfe.drawertanks.platform.Bridges;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class TankScreen extends AbstractContainerScreen<ContainerTank>
{
    private static final Identifier BACKGROUND = ModConstants.loc("textures/gui/tank.png");

    private static final int GAUGE_X = 80;
    private static final int GAUGE_Y = 18;
    private static final int GAUGE_W = 16;
    private static final int GAUGE_H = 56;

    private final Inventory inventory;

    public TankScreen (ContainerTank container, Inventory playerInv, Component name) {
        super(container, playerInv, name, 176, 199);
        inventory = playerInv;
    }

    @Override
    public void extractBackground (GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(graphics, mouseX, mouseY, partialTicks);

        int guiX = (width - imageWidth) / 2;
        int guiY = (height - imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, guiX, guiY, 0, 0, imageWidth, imageHeight, 256, 256);

        for (Slot slot : menu.getUpgradeSlots()) {
            if (slot.container instanceof InventoryTankUpgrade upgrades && upgrades.slotIsLocked(slot.getContainerSlot()))
                graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, guiX + slot.x, guiY + slot.y, 176, 0, 16, 16, 256, 256);
        }

        extractFluid(graphics, guiX, guiY);
    }

    private void extractFluid (GuiGraphicsExtractor graphics, int guiX, int guiY) {
        BlockEntityTank tank = menu.getTank();
        if (tank == null || tank.tankData().isEmpty())
            return;

        TankData data = tank.tankData();
        FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(data.getFluid().defaultFluidState());
        if (model == null)
            return;

        TextureAtlasSprite sprite = model.stillMaterial().sprite();
        int color = Bridges.CLIENT_FLUID != null ? Bridges.CLIENT_FLUID.color(data.getFluid(), data.getComponents()) : 0xFFFFFFFF;
        if ((color >>> 24) == 0)
            color |= 0xFF000000;

        int fillHeight = Math.max(1, (int) (tank.fillFraction() * GAUGE_H));
        int y = guiY + GAUGE_Y + GAUGE_H;
        while (fillHeight > 0) {
            int segment = Math.min(16, fillHeight);
            y -= segment;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, guiX + GAUGE_X, y, GAUGE_W, segment, color);
            fillHeight -= segment;
        }
    }

    @Override
    protected void extractLabels (GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, 8, 6, 0xFF404040, false);
        graphics.text(this.font, I18n.get("container.storagedrawers.upgrades"), 8, 75, 0xFF404040, false);
        graphics.text(this.font, this.inventory.getDisplayName().getString(), 8, this.imageHeight - 96 + 2, 0xFF404040, false);

        BlockEntityTank tank = menu.getTank();
        if (tank != null) {
            String label = amountLabel(tank);
            graphics.text(this.font, label, 168 - this.font.width(label), 42, 0xFF404040, false);
        }
    }

    private static String amountLabel (BlockEntityTank tank) {
        long capacity = tank.capacityDroplets();
        if (tank.tankData().isEmpty() && capacity <= 0)
            return "";

        String held = trimmed(tank.tankData().getAmount() / (double) BlockEntityTank.DROPLETS_PER_BUCKET);
        if (BlockEntityTank.isUnlimitedCapacity(capacity))
            return held + " B";

        return held + " / " + (capacity / BlockEntityTank.DROPLETS_PER_BUCKET) + " B";
    }

    private static String trimmed (double buckets) {
        if (buckets == Math.floor(buckets))
            return Long.toString((long) buckets);
        return String.format(java.util.Locale.ROOT, "%.1f", buckets);
    }
}
