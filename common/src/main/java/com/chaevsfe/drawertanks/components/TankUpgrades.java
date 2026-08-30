package com.chaevsfe.drawertanks.components;

import com.mojang.serialization.Codec;
import net.minecraft.world.ItemStackWithSlot;

import java.util.List;

public record TankUpgrades(List<ItemStackWithSlot> upgrades)
{
    public static final Codec<TankUpgrades> CODEC = ItemStackWithSlot.CODEC.listOf().xmap(TankUpgrades::new, TankUpgrades::upgrades);
}
