package com.chaevsfe.drawertanks.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

// Drawer-key state carried on the item so a locked or concealed tank comes back the same way.
public record TankAttributesData(boolean locked, boolean concealed, boolean showQuantity)
{
    public static final TankAttributesData EMPTY = new TankAttributesData(false, false, false);

    public static final Codec<TankAttributesData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.BOOL.optionalFieldOf("locked", false).forGetter(TankAttributesData::locked),
            Codec.BOOL.optionalFieldOf("concealed", false).forGetter(TankAttributesData::concealed),
            Codec.BOOL.optionalFieldOf("show_quantity", false).forGetter(TankAttributesData::showQuantity)
        ).apply(instance, TankAttributesData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TankAttributesData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL, TankAttributesData::locked,
        ByteBufCodecs.BOOL, TankAttributesData::concealed,
        ByteBufCodecs.BOOL, TankAttributesData::showQuantity,
        TankAttributesData::new);

    public boolean isEmpty () {
        return !locked && !concealed && !showQuantity;
    }
}
