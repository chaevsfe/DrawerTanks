package com.chaevsfe.drawertanks.client.renderer.state;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.state.BlockState;

public class TankRenderState extends BlockEntityRenderState
{
    public BlockState blockState;
    public boolean hasFluid;
    public float fill;
    public TextureAtlasSprite sprite;
    public int color = 0xFFFFFFFF;

    public TankRenderState () { }
}
