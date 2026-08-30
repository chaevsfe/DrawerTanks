package com.chaevsfe.drawertanks.client.renderer.state;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.state.BlockState;

public class LinkedDrawerRenderState extends BlockEntityRenderState
{
    public BlockState blockState;
    public ItemStackRenderState itemState;
    public String countText;
    public TextureAtlasSprite[] channelSprites;
    public TextureAtlasSprite lockSprite;

    public LinkedDrawerRenderState () { }
}
