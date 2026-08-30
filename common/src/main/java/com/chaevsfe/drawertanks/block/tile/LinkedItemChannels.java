package com.chaevsfe.drawertanks.block.tile;

import com.chaevsfe.drawertanks.ModConstants;
import com.chaevsfe.drawertanks.config.TankConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import com.jaquadro.minecraft.storagedrawers.block.tile.tiledata.UpgradeData;
import com.jaquadro.minecraft.storagedrawers.capabilities.BasicDrawerAttributes;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LinkedItemChannels extends SavedData
{
    public static class Pool
    {
        public ItemStack prototype = ItemStack.EMPTY;
        public long count;
        public long version;

        LinkedItemChannels owner;
        // one handler per pool, so same-channel drawers share a single transaction participant
        public Object platformHandler;

        // the channel owns its upgrades, like the fluid channels do
        public final PoolAttributes attributes = new PoolAttributes();
        public final PoolUpgrades upgrades = new PoolUpgrades();

        public Pool () {
            upgrades.setDrawerAttributes(attributes);
        }

        public class PoolAttributes extends BasicDrawerAttributes
        {
            @Override
            protected void onAttributeChanged () {
                changed();
            }
        }

        public class PoolUpgrades extends UpgradeData
        {
            public PoolUpgrades () {
                super(BlockEntityTank.UPGRADE_SLOTS);
            }

            @Override
            protected void onUpgradeChanged (ItemStack oldUpgrade, ItemStack newUpgrade) {
                changed();
            }

            List<ItemStackWithSlot> toList () {
                List<ItemStackWithSlot> out = new ArrayList<>();
                for (int i = 0; i < upgrades.length; i++) {
                    if (!upgrades[i].isEmpty())
                        out.add(new ItemStackWithSlot(i, upgrades[i].copy()));
                }
                return out;
            }

            void load (List<ItemStackWithSlot> stacks) {
                java.util.Arrays.fill(upgrades, ItemStack.EMPTY);
                for (ItemStackWithSlot slotStack : stacks) {
                    if (slotStack.isValidInContainer(upgrades.length))
                        upgrades[slotStack.slot()] = slotStack.stack().copy();
                }
                setDrawerAttributes(attributes);
            }
        }

        public boolean isEmpty () {
            return prototype.isEmpty() || count <= 0;
        }

        // worth persisting if it holds items or upgrades
        public boolean isBlank () {
            if (!isEmpty())
                return false;

            for (int i = 0; i < upgrades.getSlotCount(); i++) {
                if (!upgrades.getUpgrade(i).isEmpty())
                    return false;
            }
            return true;
        }

        public void changed () {
            version++;
            if (owner != null)
                owner.setDirty();
        }

        public long capacity () {
            return capacityFor(prototype);
        }

        public long capacityFor (ItemStack reference) {
            if (attributes.isUnlimitedStorage() || attributes.isUnlimitedVending())
                return Long.MAX_VALUE / 4;

            int stackSize = reference.isEmpty() ? 64 : reference.getMaxStackSize();
            long stacks = (long) TankConfig.linkedChannelCapacityStacks * upgrades.getStorageMultiplier();
            if (upgrades.hasOneStackUpgrade())
                stacks = 1;

            return stacks * stackSize;
        }

        public void set (ItemStack prototype, long count) {
            this.prototype = prototype.copyWithCount(1);
            this.count = count;
            if (this.count <= 0) {
                this.prototype = ItemStack.EMPTY;
                this.count = 0;
            }
        }
    }

    private record PoolContents(ItemStack prototype, long count, List<ItemStackWithSlot> upgrades)
    {
        static final Codec<PoolContents> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                ItemStack.CODEC.optionalFieldOf("item", ItemStack.EMPTY).forGetter(PoolContents::prototype),
                Codec.LONG.optionalFieldOf("count", 0L).forGetter(PoolContents::count),
                ItemStackWithSlot.CODEC.listOf().optionalFieldOf("upgrades", List.of()).forGetter(PoolContents::upgrades)
            ).apply(instance, PoolContents::new));
    }

    private static final Codec<LinkedItemChannels> CODEC = Codec.unboundedMap(Codec.STRING, PoolContents.CODEC)
        .xmap(LinkedItemChannels::fromMap, LinkedItemChannels::toMap);

    // both loaders currently patch vanilla's unguarded DataFixTypes.update call to tolerate null,
    // but vanilla itself does not; pass a real constant rather than depend on that patch
    public static final SavedDataType<LinkedItemChannels> TYPE = new SavedDataType<>(
        ModConstants.loc("linked_item_channels"), LinkedItemChannels::new, CODEC, DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES);

    private final Map<String, Pool> pools = new HashMap<>();

    public static LinkedItemChannels get (MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Pool pool (String key) {
        return pools.computeIfAbsent(key, k -> {
            Pool pool = new Pool();
            pool.owner = this;
            return pool;
        });
    }

    private static LinkedItemChannels fromMap (Map<String, PoolContents> map) {
        LinkedItemChannels channels = new LinkedItemChannels();
        map.forEach((key, contents) -> {
            Pool pool = channels.pool(key);
            pool.upgrades.load(contents.upgrades());
            pool.set(contents.prototype(), contents.count());
        });
        return channels;
    }

    private Map<String, PoolContents> toMap () {
        Map<String, PoolContents> out = new HashMap<>();
        pools.forEach((key, pool) -> {
            if (!pool.isBlank())
                out.put(key, new PoolContents(pool.prototype, pool.count, pool.upgrades.toList()));
        });
        return out;
    }
}
