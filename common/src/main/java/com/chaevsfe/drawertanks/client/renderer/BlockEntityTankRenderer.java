package com.chaevsfe.drawertanks.client.renderer;

import com.chaevsfe.drawertanks.block.BlockTank;
import com.chaevsfe.drawertanks.block.tile.BlockEntityTank;
import com.chaevsfe.drawertanks.block.tile.tiledata.TankData;
import com.chaevsfe.drawertanks.client.renderer.state.TankRenderState;
import com.chaevsfe.drawertanks.platform.Bridges;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class BlockEntityTankRenderer implements BlockEntityRenderer<BlockEntityTank, TankRenderState>
{
    private static final float UNIT = 0.0625f;
    private static final float WIN_MIN = 3;
    private static final float WIN_MAX = 13;
    private static final float DEPTH = 2;

    public BlockEntityTankRenderer (BlockEntityRendererProvider.Context context) {
    }

    @Override
    public TankRenderState createRenderState () {
        return new TankRenderState();
    }

    @Override
    public void extractRenderState (BlockEntityTank blockEntity, TankRenderState renderState, float partialTick, Vec3 cameraPos, @Nullable ModelFeatureRenderer.CrumblingOverlay crumbleOverlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPos, crumbleOverlay);

        renderState.blockState = blockEntity.getBlockState();

        TankData data = blockEntity.tankData();
        renderState.hasFluid = !data.isEmpty();
        if (!renderState.hasFluid)
            return;

        renderState.fill = blockEntity.fillFraction();

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
        if (!renderState.hasFluid || renderState.sprite == null)
            return;

        if (!(renderState.blockState.getBlock() instanceof BlockTank))
            return;

        poseStack.pushPose();

        Direction side = renderState.blockState.getValue(BlockTank.FACING);
        alignRendering(poseStack, side);

        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.translucentMovingBlock(), new FluidQuad(renderState));

        poseStack.popPose();
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

        private static void addVertex (Matrix4f matrix, PoseStack.Pose pose, VertexConsumer buffer, int light, float x, float y, float z, float u, float v, float r, float g, float b, float a) {
            buffer.addVertex(matrix, x, y, z).setColor(r, g, b, a).setUv(u, v).setLight(light).setNormal(pose, 0, 0, 1);
        }
    }
}
