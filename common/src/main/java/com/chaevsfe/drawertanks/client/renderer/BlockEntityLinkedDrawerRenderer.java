package com.chaevsfe.drawertanks.client.renderer;

import com.chaevsfe.drawertanks.block.BlockLinkedDrawer;
import com.chaevsfe.drawertanks.block.tile.BlockEntityLinkedDrawer;
import com.chaevsfe.drawertanks.client.renderer.state.LinkedDrawerRenderState;
import com.jaquadro.minecraft.storagedrawers.util.CountFormatter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class BlockEntityLinkedDrawerRenderer implements BlockEntityRenderer<BlockEntityLinkedDrawer, LinkedDrawerRenderState>
{
    private static final float UNIT = 0.0625f;
    private static final float FRONT_RECESS = 1.5f * 0.0625f;
    private static final Matrix3f ITEM_LIGHT_ROTATION_3D = (new Matrix3f()).rotationYXZ(.36f, -.36f, -.014f);
    private static final float[] sideRotationY2D = { 0, 0, 2, 0, 3, 1 };

    private final ItemModelResolver itemModelResolver;
    private final Font font;

    public BlockEntityLinkedDrawerRenderer (BlockEntityRendererProvider.Context context) {
        itemModelResolver = context.itemModelResolver();
        font = context.font();
    }

    @Override
    public LinkedDrawerRenderState createRenderState () {
        return new LinkedDrawerRenderState();
    }

    @Override
    public void extractRenderState (BlockEntityLinkedDrawer blockEntity, LinkedDrawerRenderState renderState, float partialTick, Vec3 cameraPos, @Nullable ModelFeatureRenderer.CrumblingOverlay crumbleOverlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPos, crumbleOverlay);

        renderState.blockState = blockEntity.getBlockState();
        renderState.itemState = null;
        renderState.countText = null;
        renderState.channelSprites = null;

        if (blockEntity.getLevel() != null && renderState.blockState.hasProperty(BlockLinkedDrawer.FACING)) {
            Direction facing = renderState.blockState.getValue(BlockLinkedDrawer.FACING);
            renderState.lightCoords = LightCoordsUtil.getLightCoords(blockEntity.getLevel(), blockEntity.getBlockPos().relative(facing));
        }

        var channels = blockEntity.getChannels();
        var sprites = new TextureAtlasSprite[channels.length];
        for (int i = 0; i < sprites.length; i++) {
            var color = channels[i] == null ? net.minecraft.world.item.DyeColor.WHITE : channels[i];
            sprites[i] = Minecraft.getInstance().getAtlasManager().get(new SpriteId(TextureAtlas.LOCATION_BLOCKS,
                Identifier.withDefaultNamespace("block/" + color.getSerializedName() + "_wool")));
        }
        renderState.channelSprites = sprites;

        ItemStack item = blockEntity.displayItem();
        if (!item.isEmpty()) {
            renderState.itemState = new ItemStackRenderState();
            itemModelResolver.updateForTopItem(renderState.itemState, item, ItemDisplayContext.GUI, blockEntity.getLevel(), null, (int) blockEntity.getBlockPos().asLong());
            long count = blockEntity.displayCount();
            renderState.countText = CountFormatter.format(font, (int) Math.min(Integer.MAX_VALUE, count));
        }
    }

    @Override
    public void submit (LinkedDrawerRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        if (!(renderState.blockState.getBlock() instanceof BlockLinkedDrawer))
            return;

        poseStack.pushPose();

        Direction side = renderState.blockState.getValue(BlockLinkedDrawer.FACING);
        alignRendering(poseStack, side);

        if (renderState.channelSprites != null)
            submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.solidMovingBlock(), new Strips(renderState));

        if (renderState.itemState != null && !renderState.itemState.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0.5f, 0.5f, 1f - FRONT_RECESS + 0.0025f);
            poseStack.mulPose((new Matrix4f()).scale(10 / 16f, 10 / 16f, 0.001f));
            poseStack.last().trustedNormals = true;
            poseStack.last().normal().rotateYXZ(-getRotationYForSide2D(side), 0, 0).mul(ITEM_LIGHT_ROTATION_3D);
            renderState.itemState.submit(poseStack, submitNodeCollector, renderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }

        if (renderState.countText != null) {
            poseStack.pushPose();
            poseStack.translate(0.5f, 0.28f, 1f - FRONT_RECESS + 0.005f);
            poseStack.scale(1 / 128f, -1 / 128f, 1);
            int width = font.width(renderState.countText);
            submitNodeCollector.submitText(poseStack, -width / 2f, 0,
                FormattedCharSequence.forward(renderState.countText, Style.EMPTY),
                false, Font.DisplayMode.POLYGON_OFFSET, renderState.lightCoords, 0xFFFFFFFF, 0, 0);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private void alignRendering (PoseStack poseStack, Direction side) {
        poseStack.translate(.5f, 0, .5f);
        poseStack.mulPose((new Matrix4f()).rotateYXZ(getRotationYForSide2D(side), 0, 0));
        poseStack.translate(-.5f, 0, -.5f);
    }

    private float getRotationYForSide2D (Direction side) {
        return sideRotationY2D[side.ordinal()] * 90 * (float) Math.PI / 180f;
    }

    record Strips(LinkedDrawerRenderState renderState) implements SubmitNodeCollector.CustomGeometryRenderer
    {
        @Override
        public void render (PoseStack.Pose pose, VertexConsumer vertexConsumer) {
            Matrix4f matrix = pose.pose();
            int light = renderState.lightCoords;

            for (int i = 0; i < renderState.channelSprites.length; i++) {
                var sprite = renderState.channelSprites[i];
                if (sprite == null)
                    continue;

                float x1 = (1 + i * 3) * UNIT;
                float x2 = x1 + 2 * UNIT;
                float z1 = 3 * UNIT;
                float z2 = 13 * UNIT;
                float y1 = 1f;
                float y2 = 1f + UNIT;

                float u0 = sprite.getU0();
                float du = sprite.getU1() - u0;
                float v0 = sprite.getV0();
                float dv = sprite.getV1() - v0;
                float ua = u0 + du * (1 + i * 3) / 16f;
                float ub = u0 + du * (3 + i * 3) / 16f;
                float uzA = u0 + du * 3 / 16f;
                float uzB = u0 + du * 13 / 16f;
                float va = v0 + dv * 3 / 16f;
                float vb = v0 + dv * 13 / 16f;
                float vTop = v0;
                float vBot = v0 + dv / 16f;

                quad(matrix, pose, vertexConsumer, light, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, ua, va, ub, vb);
                quad(matrix, pose, vertexConsumer, light, x1, y1, z2, x1, y2, z2, x2, y2, z2, x2, y1, z2, ua, vBot, ub, vTop);
                quad(matrix, pose, vertexConsumer, light, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, ua, vBot, ub, vTop);
                quad(matrix, pose, vertexConsumer, light, x1, y1, z1, x1, y2, z1, x1, y2, z2, x1, y1, z2, uzA, vBot, uzB, vTop);
                quad(matrix, pose, vertexConsumer, light, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, uzA, vBot, uzB, vTop);
            }
        }

        private static void quad (Matrix4f matrix, PoseStack.Pose pose, VertexConsumer buffer, int light,
                                  float ax, float ay, float az, float bx, float by, float bz,
                                  float cx, float cy, float cz, float dx, float dy, float dz,
                                  float u1, float v1, float u2, float v2) {
            vertex(matrix, pose, buffer, light, ax, ay, az, u1, v1);
            vertex(matrix, pose, buffer, light, bx, by, bz, u1, v2);
            vertex(matrix, pose, buffer, light, cx, cy, cz, u2, v2);
            vertex(matrix, pose, buffer, light, dx, dy, dz, u2, v1);

            vertex(matrix, pose, buffer, light, dx, dy, dz, u2, v1);
            vertex(matrix, pose, buffer, light, cx, cy, cz, u2, v2);
            vertex(matrix, pose, buffer, light, bx, by, bz, u1, v2);
            vertex(matrix, pose, buffer, light, ax, ay, az, u1, v1);
        }

        private static void vertex (Matrix4f matrix, PoseStack.Pose pose, VertexConsumer buffer, int light, float x, float y, float z, float u, float v) {
            buffer.addVertex(matrix, x, y, z).setColor(1f, 1f, 1f, 1f).setUv(u, v).setLight(light).setNormal(pose, 0, 1, 0);
        }
    }
}
