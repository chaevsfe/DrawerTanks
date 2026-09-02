package com.chaevsfe.drawertanks.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;

// a fluid type without an amount: what a linked tank's channel held when the block broke
public record LinkFluid(Fluid fluid, DataComponentPatch components)
{
    public static final Codec<LinkFluid> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(LinkFluid::fluid),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(LinkFluid::components)
        ).apply(instance, LinkFluid::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, LinkFluid> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.registry(Registries.FLUID), LinkFluid::fluid,
        DataComponentPatch.STREAM_CODEC, LinkFluid::components,
        LinkFluid::new
    );
}
