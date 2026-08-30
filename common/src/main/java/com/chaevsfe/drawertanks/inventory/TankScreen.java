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
    private static final Identifier DRAWER_BACKGROUND = ModConstants.loc("textures/gui/linked_drawer.png");

    private static final int GAUGE_X = 80;
    private static final int GAUGE_Y = 18;
    private static final int GAUGE_W = 16;
    // right edge of the label area minus the gauge, so text can never sit on top of the gauge
    private static final int LABEL_MAX_WIDTH = 168 - (GAUGE_X + GAUGE_W + 3);
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
        Identifier background = menu.getTank() instanceof BlockEntityTank ? BACKGROUND : DRAWER_BACKGROUND;
        graphics.blit(RenderPipelines.GUI_TEXTURED, background, guiX, guiY, 0, 0, imageWidth, imageHeight, 256, 256);

        for (Slot slot : menu.getUpgradeSlots()) {
            if (slot.container instanceof InventoryTankUpgrade upgrades && upgrades.slotIsLocked(slot.getContainerSlot()))
                graphics.blit(RenderPipelines.GUI_TEXTURED, background, guiX + slot.x, guiY + slot.y, 176, 0, 16, 16, 256, 256);
        }

        net.minecraft.world.inventory.Slot display = menu.getDisplaySlot();
        if (display != null)
            graphics.blit(RenderPipelines.GUI_TEXTURED, background, guiX + display.x - 1, guiY + display.y - 1, 176, 0, 18, 18, 256, 256);

        extractFluid(graphics, guiX, guiY);
        extractTicks(graphics, guiX, guiY);
    }

    private void extractTicks (GuiGraphicsExtractor graphics, int guiX, int guiY) {
        if (!(menu.getTank() instanceof BlockEntityTank tank))
            return;

        long capacity = tank.capacityDroplets();
        if (capacity <= 0 || BlockEntityTank.isUnlimitedCapacity(capacity))
            return;

        long capacityBuckets = capacity / BlockEntityTank.DROPLETS_PER_BUCKET;
        int divisions = capacityBuckets >= 2 && capacityBuckets <= 32 ? (int) capacityBuckets : 4;
        int major = capacityBuckets >= 2 && capacityBuckets <= 32 ? 4 : 2;

        for (int i = 1; i < divisions; i++) {
            int y = guiY + GAUGE_Y + GAUGE_H - Math.round(i * (float) GAUGE_H / divisions);
            int width = i % major == 0 ? 6 : 3;
            graphics.fill(guiX + GAUGE_X, y, guiX + GAUGE_X + width, y + 1, 0xFFA03028);
        }
    }

    private void extractFluid (GuiGraphicsExtractor graphics, int guiX, int guiY) {
        if (!(menu.getTank() instanceof BlockEntityTank tank) || tank.tankData().isEmpty())
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

        if (!(menu.getTank() instanceof BlockEntityTank tank)) {
            drawStoredItemLabel(graphics);
            return;
        }

        long capacity = tank.capacityDroplets();
        if (tank.tankData().isEmpty() && capacity <= 0)
            return;

        String held = compact(tank.tankData().getAmount() / (double) BlockEntityTank.DROPLETS_PER_BUCKET);
        if (BlockEntityTank.isUnlimitedCapacity(capacity)) {
            drawRight(graphics, held + " B", 42);
            return;
        }

        String cap = compact(capacity / (double) BlockEntityTank.DROPLETS_PER_BUCKET);
        String single = held + " / " + cap + " B";
        // a channel with storage upgrades runs to five figures, which would print over the gauge
        if (this.font.width(single) <= LABEL_MAX_WIDTH) {
            drawRight(graphics, single, 42);
        } else {
            drawRight(graphics, held, 36);
            drawRight(graphics, "/ " + cap + " B", 46);
        }
    }

    // a linked drawer has no fluid gauge, so show what the channel holds instead
    private void drawStoredItemLabel (GuiGraphicsExtractor graphics) {
        if (!(menu.getTank() instanceof com.chaevsfe.drawertanks.block.tile.BlockEntityLinkedDrawer drawer))
            return;

        if (drawer.displayItem().isEmpty()) {
            graphics.centeredText(this.font, I18n.get("tooltip.drawertanks.empty"), 88, 60, 0xFF404040);
            return;
        }

        String label = compact(drawer.displayCount()) + " / " + compact(drawer.capacityItems());
        graphics.centeredText(this.font, label, 88, 60, 0xFF404040);
    }

    private void drawRight (GuiGraphicsExtractor graphics, String text, int y) {
        int x = Math.max(GAUGE_X + GAUGE_W + 3, 168 - this.font.width(text));
        graphics.text(this.font, text, x, y, 0xFF404040, false);
    }

    private static String compact (double buckets) {
        if (buckets >= 1_000_000)
            return trimmed(buckets / 1_000_000) + "M";
        if (buckets >= 10_000)
            return trimmed(buckets / 1000) + "k";
        return trimmed(buckets);
    }

    private static String trimmed (double buckets) {
        if (buckets == Math.floor(buckets))
            return Long.toString((long) buckets);
        return String.format(java.util.Locale.ROOT, "%.1f", buckets);
    }
}
