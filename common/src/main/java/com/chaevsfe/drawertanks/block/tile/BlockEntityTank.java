package com.chaevsfe.drawertanks.block.tile;

import com.chaevsfe.drawertanks.block.tile.tiledata.TankData;
import com.chaevsfe.drawertanks.components.TankContents;
import com.chaevsfe.drawertanks.components.TankUpgrades;
import com.chaevsfe.drawertanks.core.ModBlockEntities;
import com.chaevsfe.drawertanks.core.ModDataComponents;
import com.jaquadro.minecraft.storagedrawers.block.tile.BaseBlockEntity;
import com.jaquadro.minecraft.storagedrawers.block.tile.tiledata.UpgradeData;
import com.jaquadro.minecraft.storagedrawers.capabilities.BasicDrawerAttributes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;

public class BlockEntityTank extends BaseBlockEntity
{
    public static final int BASE_CAPACITY_BUCKETS = 8;
    public static final long DROPLETS_PER_BUCKET = 81000L;
    public static final long DROPLETS_PER_MB = 81L;
    public static final int UPGRADE_SLOTS = 7;

    private final TankData tankData = new TankData();
    private final TankUpgradeData upgradeData = new TankUpgradeData();
    private final TankAttributes attributes = new TankAttributes();

    private Object platformFluidHandler;
    private boolean syncPending;

    public BlockEntityTank (BlockPos pos, BlockState state) {
        super(ModBlockEntities.TANK.get(), pos, state);

        upgradeData.setDrawerAttributes(attributes);
        injectData(upgradeData);
        injectData(tankData);
    }

    public TankData tankData () {
        return tankData;
    }

    public UpgradeData upgrades () {
        return upgradeData;
    }

    public boolean isVoid () {
        return attributes.isVoid();
    }

    public boolean isUnlimitedVending () {
        return attributes.isUnlimitedVending();
    }

    public long capacityDroplets () {
        if (attributes.isUnlimitedStorage() || attributes.isUnlimitedVending())
            return Long.MAX_VALUE / 4;

        long buckets = (long) BASE_CAPACITY_BUCKETS * upgradeData.getStorageMultiplier();
        if (upgradeData.hasOneStackUpgrade())
            buckets = 1;

        return buckets * DROPLETS_PER_BUCKET;
    }

    public float fillFraction () {
        long capacity = capacityDroplets();
        if (capacity <= 0 || tankData.isEmpty())
            return 0;

        return Math.min(1f, (float) ((double) tankData.getAmount() / capacity));
    }

    public Object platformFluidHandler () {
        return platformFluidHandler;
    }

    public void setPlatformFluidHandler (Object handler) {
        platformFluidHandler = handler;
    }

    public void onContentsChanged () {
        setChanged();
        syncPending = true;
    }

    public ItemStack tryRemoveUpgrade () {
        for (int i = upgradeData.getSlotCount() - 1; i >= 0; i--) {
            ItemStack upgrade = upgradeData.getUpgrade(i);
            if (upgrade.isEmpty())
                continue;

            if (!upgradeData.setUpgrade(i, ItemStack.EMPTY))
                return ItemStack.EMPTY;

            if (tankData.getAmount() > capacityDroplets()) {
                upgradeData.setUpgrade(i, upgrade);
                return ItemStack.EMPTY;
            }

            return upgrade;
        }

        return ItemStack.EMPTY;
    }

    public static void serverTick (Level level, BlockPos pos, BlockState state, BlockEntityTank tank) {
        if (tank.syncPending && (level.getGameTime() & 3) == 0) {
            tank.syncPending = false;
            tank.markBlockForUpdate();
        }
    }

    @Override
    public boolean dataPacketRequiresRenderUpdate () {
        return false;
    }

    @Override
    protected void applyImplicitComponents (DataComponentGetter input) {
        super.applyImplicitComponents(input);

        tankData.fromContents(input.get(ModDataComponents.TANK_CONTENTS.get()));

        TankUpgrades upgrades = input.get(ModDataComponents.TANK_UPGRADES.get());
        if (upgrades != null) {
            for (ItemStackWithSlot slotStack : upgrades.upgrades()) {
                if (slotStack.isValidInContainer(upgradeData.getSlotCount()))
                    upgradeData.setUpgrade(slotStack.slot(), slotStack.stack());
            }
        }
    }

    @Override
    protected void collectImplicitComponents (DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);

        if (!tankData.isEmpty())
            builder.set(ModDataComponents.TANK_CONTENTS.get(), tankData.toContents());

        List<ItemStackWithSlot> upgrades = new ArrayList<>();
        for (int i = 0; i < upgradeData.getSlotCount(); i++) {
            ItemStack upgrade = upgradeData.getUpgrade(i);
            if (!upgrade.isEmpty())
                upgrades.add(new ItemStackWithSlot(i, upgrade));
        }
        if (!upgrades.isEmpty())
            builder.set(ModDataComponents.TANK_UPGRADES.get(), new TankUpgrades(upgrades));
    }

    @Override
    public void removeComponentsFromTag (ValueOutput output) {
        output.discard("Fluid");
        output.discard("FluidComponents");
        output.discard("Amount");
        output.discard("Upgrades");
    }

    private class TankUpgradeData extends UpgradeData
    {
        public TankUpgradeData () {
            super(UPGRADE_SLOTS);
        }

        @Override
        protected void onUpgradeChanged (ItemStack oldUpgrade, ItemStack newUpgrade) {
            if (getLevel() != null && !getLevel().isClientSide()) {
                onContentsChanged();
                getLevel().updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
            }
        }
    }

    private class TankAttributes extends BasicDrawerAttributes
    {
        @Override
        protected void onAttributeChanged () {
            if (getLevel() != null && !getLevel().isClientSide())
                onContentsChanged();
        }
    }
}
