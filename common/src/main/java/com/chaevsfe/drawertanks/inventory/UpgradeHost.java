package com.chaevsfe.drawertanks.inventory;

import com.jaquadro.minecraft.storagedrawers.block.tile.tiledata.UpgradeData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// A block whose upgrade slots the shared upgrade menu can drive. Tanks measure their contents in
// droplets and linked drawers in items, so the capacity checks are expressed abstractly.
public interface UpgradeHost
{
    UpgradeData upgrades ();

    boolean acceptsUpgrades ();

    void refreshUpgradeMirror ();

    boolean upgradeFitsContents (ItemStack upgrade);

    long storedAmount ();

    long capacityWithout (int slot);

    long capacityWithSwap (int slot, ItemStack incoming);

    Level hostLevel ();

    BlockPos hostPos ();

    void hostChanged ();
}
