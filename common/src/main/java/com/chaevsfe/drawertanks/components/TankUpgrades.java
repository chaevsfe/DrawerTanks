package com.chaevsfe.drawertanks.components;

import com.mojang.serialization.Codec;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record TankUpgrades(List<ItemStackWithSlot> upgrades)
{
    public static final Codec<TankUpgrades> CODEC = ItemStackWithSlot.CODEC.listOf().xmap(TankUpgrades::new, TankUpgrades::upgrades);

    @Override
    public boolean equals (Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof TankUpgrades other))
            return false;
        if (upgrades.size() != other.upgrades.size())
            return false;

        for (int i = 0; i < upgrades.size(); i++) {
            ItemStackWithSlot a = upgrades.get(i);
            ItemStackWithSlot b = other.upgrades.get(i);
            if (a.slot() != b.slot() || !ItemStack.matches(a.stack(), b.stack()))
                return false;
        }

        return true;
    }

    @Override
    public int hashCode () {
        int hash = 1;
        for (ItemStackWithSlot slotStack : upgrades)
            hash = 31 * hash + 31 * slotStack.slot() + ItemStack.hashItemAndComponents(slotStack.stack());
        return hash;
    }
}
