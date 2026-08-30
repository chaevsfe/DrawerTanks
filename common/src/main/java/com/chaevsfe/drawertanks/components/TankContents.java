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
import net.minecraft.world.level.material.Fluids;

public record TankContents(Fluid fluid, DataComponentPatch components, long amount)
{
    public static final TankContents EMPTY = new TankContents(Fluids.EMPTY, DataComponentPatch.EMPTY, 0);

    public static final Codec<TankContents> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(TankContents::fluid),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(TankContents::components),
            Codec.LONG.fieldOf("amount").forGetter(TankContents::amount)
        ).apply(instance, TankContents::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, TankContents> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.registry(Registries.FLUID),
        TankContents::fluid,
        DataComponentPatch.STREAM_CODEC,
        TankContents::components,
        ByteBufCodecs.VAR_LONG,
        TankContents::amount,
        TankContents::new
    );

    public boolean isEmpty () {
        return fluid == Fluids.EMPTY || amount <= 0;
    }
}
