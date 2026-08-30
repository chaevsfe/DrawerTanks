package com.chaevsfe.drawertanks.block.tile;

import com.chaevsfe.drawertanks.block.tile.tiledata.TankData;
import com.chaevsfe.drawertanks.components.TankContents;
import com.chaevsfe.drawertanks.components.TankUpgrades;
import com.chaevsfe.drawertanks.core.ModBlockEntities;
import com.chaevsfe.drawertanks.core.ModDataComponents;
import com.chaevsfe.drawertanks.config.TankConfig;
import com.chaevsfe.drawertanks.inventory.ContainerTank;
import com.jaquadro.minecraft.storagedrawers.block.tile.BaseBlockEntity;
import com.jaquadro.minecraft.storagedrawers.block.tile.tiledata.UpgradeData;
import com.jaquadro.minecraft.storagedrawers.capabilities.BasicDrawerAttributes;
import com.jaquadro.minecraft.storagedrawers.config.ModCommonConfig;
import com.jaquadro.minecraft.storagedrawers.item.ItemUpgradeStorage;
import com.texelsaurus.minecraft.chameleon.inventory.ContentMenuProvider;
import com.texelsaurus.minecraft.chameleon.inventory.content.PositionContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;

public class BlockEntityTank extends BaseBlockEntity
{
    public static final long DROPLETS_PER_BUCKET = 81000L;
    public static final long DROPLETS_PER_MB = 81L;
    public static final int UPGRADE_SLOTS = 7;

    private final TankData tankData = new TankData();
    private final TankUpgradeData upgradeData = new TankUpgradeData();
    private final TankAttributes attributes = new TankAttributes();

    private Object platformFluidHandler;
    private boolean syncPending;
    private long lastSyncTime = -100;

    public BlockEntityTank (BlockPos pos, BlockState state) {
        this(ModBlockEntities.TANK.get(), pos, state);
    }

    protected BlockEntityTank (net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);

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

        long buckets = (long) TankConfig.baseCapacityBuckets * upgradeData.getStorageMultiplier();
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
        if (getLevel() == null || getLevel().isClientSide())
            return;

        long now = getLevel().getGameTime();
        if (now - lastSyncTime >= 4) {
            lastSyncTime = now;
            syncPending = false;
            markBlockForUpdate();
        } else
            syncPending = true;
    }

    public boolean upgradeFitsContents (ItemStack upgrade) {
        if (upgrade.getItem() == com.jaquadro.minecraft.storagedrawers.core.ModItems.ONE_STACK_UPGRADE.get())
            return tankData.getAmount() <= DROPLETS_PER_BUCKET;
        return true;
    }

    public long capacityDropletsWithout (int slot) {
        return capacityDropletsWithSwap(slot, ItemStack.EMPTY);
    }

    public long capacityDropletsWithSwap (int slot, ItemStack incoming) {
        List<ItemStack> upgrades = new ArrayList<>();
        for (int i = 0; i < upgradeData.getSlotCount(); i++)
            upgrades.add(i == slot ? incoming : upgradeData.getUpgrade(i));
        return computeCapacityDroplets(upgrades);
    }

    public static boolean isUnlimitedCapacity (long capacityDroplets) {
        return capacityDroplets >= Long.MAX_VALUE / 8;
    }

    public static long computeCapacityDroplets (Iterable<ItemStack> upgrades) {
        boolean unlimited = false;
        boolean oneStack = false;
        int multiplier = 0;

        for (ItemStack upgrade : upgrades) {
            Item item = upgrade.getItem();
            if (item instanceof ItemUpgradeStorage storage)
                multiplier += ModCommonConfig.INSTANCE.UPGRADES.getLevelMult(storage.level.getLevel());
            else if (item == com.jaquadro.minecraft.storagedrawers.core.ModItems.CREATIVE_STORAGE_UPGRADE.get()
                && ModCommonConfig.INSTANCE.UPGRADES.creativeStorageUpgrade.enableUpgrade.get())
                unlimited = true;
            else if (item == com.jaquadro.minecraft.storagedrawers.core.ModItems.CREATIVE_VENDING_UPGRADE.get()
                && ModCommonConfig.INSTANCE.UPGRADES.creativeVendingUpgrade.enableUpgrade.get())
                unlimited = true;
            else if (item == com.jaquadro.minecraft.storagedrawers.core.ModItems.ONE_STACK_UPGRADE.get()
                && ModCommonConfig.INSTANCE.UPGRADES.oneStackUpgrade.enableUpgrade.get())
                oneStack = true;
        }

        if (unlimited)
            return Long.MAX_VALUE / 4;
        if (multiplier == 0)
            multiplier = ModCommonConfig.INSTANCE.UPGRADES.getLevelMult(0);

        long buckets = oneStack ? 1 : (long) TankConfig.baseCapacityBuckets * multiplier;
        return buckets * DROPLETS_PER_BUCKET;
    }

    public Component getDisplayName () {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    public static class ContentProvider implements ContentMenuProvider<PositionContent>
    {
        private final BlockEntityTank entity;

        public ContentProvider (BlockEntityTank entity) {
            this.entity = entity;
        }

        @Override
        public PositionContent createContent (ServerPlayer player) {
            return new PositionContent(entity.getBlockPos());
        }

        @Override
        public Component getDisplayName () {
            return entity.getDisplayName();
        }

        @Override
        public AbstractContainerMenu createMenu (int id, Inventory inventory, Player player) {
            return new ContainerTank(id, inventory, entity);
        }
    }

    public static void serverTick (Level level, BlockPos pos, BlockState state, BlockEntityTank tank) {
        if (tank.syncPending && level.getGameTime() - tank.lastSyncTime >= 4) {
            tank.syncPending = false;
            tank.lastSyncTime = level.getGameTime();
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
                    upgradeData.forceSetUpgrade(slotStack.slot(), slotStack.stack());
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
        output.discard("DataVersion");
    }

    private class TankUpgradeData extends UpgradeData
    {
        public TankUpgradeData () {
            super(UPGRADE_SLOTS);
        }

        void forceSetUpgrade (int slot, ItemStack stack) {
            upgrades[slot] = stack;
            setDrawerAttributes(attributes);
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
