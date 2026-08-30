package com.chaevsfe.drawertanks.client.model;

import com.chaevsfe.drawertanks.core.ModBlocks;
import com.jaquadro.minecraft.storagedrawers.block.tile.modelprops.FramedModelProperties;
import com.jaquadro.minecraft.storagedrawers.client.model.ItemModelStore;
import com.jaquadro.minecraft.storagedrawers.client.model.PlatformDecoratedModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.world.level.block.state.BlockState;

@Environment(EnvType.CLIENT)
public class TankModelPlugin implements ModelLoadingPlugin
{
    public static class UnbakedProxyModel implements BlockStateModel.UnbakedRoot
    {
        private final BlockStateModel.UnbakedRoot parent;

        public UnbakedProxyModel (BlockStateModel.UnbakedRoot parent) {
            this.parent = parent;
        }

        @Override
        public BlockStateModel bake (BlockState state, ModelBaker modelBaker) {
            BlockStateModel original = parent.bake(state, modelBaker);
            BlockStateModel proxy = new PlatformDecoratedModel<>(original, new TankMaterialDecorator(), FramedModelProperties.INSTANCE);
            ItemModelStore.models.put(state, proxy);
            return proxy;
        }

        @Override
        public void resolveDependencies (Resolver resolver) {
            parent.resolveDependencies(resolver);
        }

        @Override
        public Object visualEqualityGroup (BlockState blockState) {
            return parent.visualEqualityGroup(blockState);
        }
    }

    @Override
    public void initialize (Context pluginContext) {
        pluginContext.modifyBlockModelOnLoad().register((original, context) -> {
            if (context.state() == null)
                return original;
            if (context.state().getBlock() != ModBlocks.FRAMED_TANK.get())
                return original;
            if (original instanceof UnbakedProxyModel)
                return original;
            return new UnbakedProxyModel(original);
        });
    }
}
