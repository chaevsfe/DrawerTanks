package com.chaevsfe.drawertanks.client.renderer;

import com.chaevsfe.drawertanks.block.BlockTank;
import com.chaevsfe.drawertanks.block.tile.BlockEntityLinkedTank;
import com.chaevsfe.drawertanks.block.tile.BlockEntityTank;
import com.chaevsfe.drawertanks.block.tile.tiledata.TankData;
import com.chaevsfe.drawertanks.client.renderer.state.TankRenderState;
import com.chaevsfe.drawertanks.platform.Bridges;
import com.jaquadro.minecraft.storagedrawers.config.ModCommonConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class BlockEntityTankRenderer implements BlockEntityRenderer<BlockEntityTank, TankRenderState>
{
    private static final float UNIT = 0.0625f;
    private static final float WIN_MIN = 3;
    private static final float WIN_MAX = 13;
    private static final float DEPTH = 0.5f;


    private final Font font;

    public BlockEntityTankRenderer (BlockEntityRendererProvider.Context context) {
        font = context.font();
    }

    @Override
    public TankRenderState createRenderState () {
        return new TankRenderState();
    }

    @Override
    public void extractRenderState (BlockEntityTank blockEntity, TankRenderState renderState, float partialTick, Vec3 cameraPos, @Nullable ModelFeatureRenderer.CrumblingOverlay crumbleOverlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPos, crumbleOverlay);

        renderState.blockState = blockEntity.getBlockState();

        // the tank is a full opaque cube, so light at its own position is zero; sample in front of the window
        if (blockEntity.getLevel() != null && renderState.blockState.hasProperty(BlockTank.FACING)) {
            Direction facing = renderState.blockState.getValue(BlockTank.FACING);
            renderState.lightCoords = LightCoordsUtil.getLightCoords(blockEntity.getLevel(), blockEntity.getBlockPos().relative(facing));
        }

        int enforcedLight = blockEntity.upgrades().hasIlluminationUpgrade()
            ? ModCommonConfig.INSTANCE.UPGRADES.illuminationUpgrade.illuminationLevel.get()
            : ModCommonConfig.INSTANCE.UPGRADES.illuminationUpgrade.minIlluminationLevel.get();
        int enforcedBlockLight = Math.max(renderState.lightCoords & 0xFFFF, enforcedLight * 16);
        renderState.lightCoords = (renderState.lightCoords & 0xFFFF0000) | enforcedBlockLight;

        renderState.channelSprites = null;
        if (blockEntity instanceof BlockEntityLinkedTank linked) {
            var channels = linked.getChannels();
            var sprites = new TextureAtlasSprite[channels.length];
            for (int i = 0; i < sprites.length; i++) {
                var color = channels[i] == null ? net.minecraft.world.item.DyeColor.WHITE : channels[i];
                sprites[i] = Minecraft.getInstance().getAtlasManager().get(new SpriteId(TextureAtlas.LOCATION_BLOCKS,
                    Identifier.withDefaultNamespace("block/" + color.getSerializedName() + "_wool")));
            }
            renderState.channelSprites = sprites;
        }

        TankData data = blockEntity.tankData();
        boolean concealed = blockEntity.isConcealed();
        renderState.hasFluid = !concealed && data.hasFluid();
        renderState.amountText = null;
        if (!renderState.hasFluid)
            return;

        renderState.ghost = data.getAmount() <= 0;
        renderState.fill = renderState.ghost ? 0.12f : blockEntity.fillFraction();
        if (blockEntity.isShowingQuantity() && data.getAmount() > 0)
            renderState.amountText = amountLabel(data.getAmount());

        FluidState fluidState = data.getFluid().defaultFluidState();
        FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluidState);
        if (model == null) {
            renderState.hasFluid = false;
            return;
        }
        renderState.sprite = model.stillMaterial().sprite();

        if (Bridges.CLIENT_FLUID != null) {
            renderState.color = Bridges.CLIENT_FLUID.color(data.getFluid(), data.getComponents());

            int luminance = Bridges.CLIENT_FLUID.luminance(data.getFluid(), data.getComponents());
            int blockLight = Math.max(renderState.lightCoords & 0xFFFF, luminance * 16);
            renderState.lightCoords = (renderState.lightCoords & 0xFFFF0000) | blockLight;
        }
    }

    @Override
    public void submit (TankRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        boolean drawFluid = renderState.hasFluid && renderState.sprite != null;
        boolean drawStuds = renderState.channelSprites != null;
        if (!drawFluid && !drawStuds)
            return;

        if (!(renderState.blockState.getBlock() instanceof BlockTank))
            return;

        poseStack.pushPose();

        Direction side = renderState.blockState.getValue(BlockTank.FACING);
        alignRendering(poseStack, side);

        if (drawStuds)
            submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.solidMovingBlock(), new ChannelStuds(renderState));

        if (drawFluid)
            submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.translucentMovingBlock(), new FluidQuad(renderState));

        if (renderState.amountText != null) {
            poseStack.pushPose();
            poseStack.translate(0.5f, 0.32f, 1.005f);
            poseStack.scale(1 / 128f, -1 / 128f, 1);
            int width = font.width(renderState.amountText);
            submitNodeCollector.submitText(poseStack, -width / 2f, 0,
                FormattedCharSequence.forward(renderState.amountText, Style.EMPTY),
                false, Font.DisplayMode.POLYGON_OFFSET, renderState.lightCoords, 0xFFFFFFFF, 0, 0);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private static String amountLabel (long droplets) {
        double value = droplets / (double) BlockEntityTank.DROPLETS_PER_BUCKET;
        if (value == Math.floor(value))
            return (long) value + " B";
        return String.format(java.util.Locale.ROOT, "%.1f B", value);
    }

    private void alignRendering (PoseStack poseStack, Direction side) {
        poseStack.translate(.5f, 0, .5f);
        poseStack.mulPose((new Matrix4f()).rotateYXZ(getRotationYForSide2D(side), 0, 0));
        poseStack.translate(-.5f, 0, -.5f);
    }

    private static final float[] sideRotationY2D = { 0, 0, 2, 0, 3, 1 };

    private float getRotationYForSide2D (Direction side) {
        return sideRotationY2D[side.ordinal()] * 90 * (float) Math.PI / 180f;
    }

    record ChannelStuds(TankRenderState renderState) implements SubmitNodeCollector.CustomGeometryRenderer
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

                // top
                quad(matrix, pose, vertexConsumer, light,
                    x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, ua, va, ub, vb);
                // front and back
                quad(matrix, pose, vertexConsumer, light,
                    x1, y1, z2, x1, y2, z2, x2, y2, z2, x2, y1, z2, ua, vBot, ub, vTop);
                quad(matrix, pose, vertexConsumer, light,
                    x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, ua, vBot, ub, vTop);
                // left and right
                quad(matrix, pose, vertexConsumer, light,
                    x1, y1, z1, x1, y2, z1, x1, y2, z2, x1, y1, z2, uzA, vBot, uzB, vTop);
                quad(matrix, pose, vertexConsumer, light,
                    x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, uzA, vBot, uzB, vTop);
            }
        }

        private static void quad (Matrix4f matrix, PoseStack.Pose pose, VertexConsumer buffer, int light,
                                  float ax, float ay, float az, float bx, float by, float bz,
                                  float cx, float cy, float cz, float dx, float dy, float dz,
                                  float u1, float v1, float u2, float v2) {
            FluidQuad.addVertex(matrix, pose, buffer, light, ax, ay, az, u1, v1, 1f, 1f, 1f, 1f);
            FluidQuad.addVertex(matrix, pose, buffer, light, bx, by, bz, u1, v2, 1f, 1f, 1f, 1f);
            FluidQuad.addVertex(matrix, pose, buffer, light, cx, cy, cz, u2, v2, 1f, 1f, 1f, 1f);
            FluidQuad.addVertex(matrix, pose, buffer, light, dx, dy, dz, u2, v1, 1f, 1f, 1f, 1f);

            FluidQuad.addVertex(matrix, pose, buffer, light, dx, dy, dz, u2, v1, 1f, 1f, 1f, 1f);
            FluidQuad.addVertex(matrix, pose, buffer, light, cx, cy, cz, u2, v2, 1f, 1f, 1f, 1f);
            FluidQuad.addVertex(matrix, pose, buffer, light, bx, by, bz, u1, v2, 1f, 1f, 1f, 1f);
            FluidQuad.addVertex(matrix, pose, buffer, light, ax, ay, az, u1, v1, 1f, 1f, 1f, 1f);
        }
    }

    record FluidQuad(TankRenderState renderState) implements SubmitNodeCollector.CustomGeometryRenderer
    {
        @Override
        public void render (PoseStack.Pose pose, VertexConsumer vertexConsumer) {
            var sprite = renderState.sprite;

            float u0 = sprite.getU0();
            float u1 = sprite.getU1();
            float v0 = sprite.getV0();
            float v1 = sprite.getV1();

            float x1 = UNIT * WIN_MIN;
            float x2 = UNIT * WIN_MAX;
            float y1 = UNIT * WIN_MIN;
            float y2 = UNIT * (WIN_MIN + (WIN_MAX - WIN_MIN) * renderState.fill);
            float z = 1 - UNIT * DEPTH;

            if (y2 <= y1)
                return;

            float su1 = u0 + (WIN_MIN / 16f) * (u1 - u0);
            float su2 = u0 + (WIN_MAX / 16f) * (u1 - u0);
            float svBottom = v1 - (WIN_MIN / 16f) * (v1 - v0);
            float svTop = v1 - ((y2 / UNIT) / 16f) * (v1 - v0);

            int color = renderState.color;
            float a = ((color >>> 24) & 0xFF) / 255f;
            if (a <= 0)
                a = 1f;
            if (renderState.ghost)
                a *= 0.45f;
            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;

            Matrix4f matrix = pose.pose();
            int light = renderState.lightCoords;

            addVertex(matrix, pose, vertexConsumer, light, x2, y1, z, su2, svBottom, r, g, b, a);
            addVertex(matrix, pose, vertexConsumer, light, x2, y2, z, su2, svTop, r, g, b, a);
            addVertex(matrix, pose, vertexConsumer, light, x1, y2, z, su1, svTop, r, g, b, a);
            addVertex(matrix, pose, vertexConsumer, light, x1, y1, z, su1, svBottom, r, g, b, a);
        }

        static void addVertex (Matrix4f matrix, PoseStack.Pose pose, VertexConsumer buffer, int light, float x, float y, float z, float u, float v, float r, float g, float b, float a) {
            buffer.addVertex(matrix, x, y, z).setColor(r, g, b, a).setUv(u, v).setLight(light).setNormal(pose, 0, 0, 1);
        }
    }
}
