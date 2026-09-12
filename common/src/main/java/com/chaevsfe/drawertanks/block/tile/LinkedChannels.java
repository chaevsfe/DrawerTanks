package com.chaevsfe.drawertanks.block.tile;

import com.chaevsfe.drawertanks.ModConstants;
import com.chaevsfe.drawertanks.block.tile.tiledata.TankData;
import com.chaevsfe.drawertanks.config.TankConfig;
import com.jaquadro.minecraft.storagedrawers.api.storage.attribute.LockAttribute;
import com.jaquadro.minecraft.storagedrawers.block.tile.tiledata.UpgradeData;
import com.jaquadro.minecraft.storagedrawers.capabilities.BasicDrawerAttributes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LinkedChannels extends SavedData
{
    // A dye channel owns its fluid AND its upgrades and key state, like an ender chest: every linked
    // tank on the pattern is a window onto this, so they all read and write the same thing.
    public static class Pool
    {
        public final TankData data = new TankData();

        // the raw saved entry when it could not be decoded; see ParkedChannel
        Dynamic<?> parked;

        public boolean isUnresolved () {
            return parked != null;
        }
        public long version;

        LinkedChannels owner;
        private TankTarget target;
        public Object platformHandler;

        public final PoolAttributes attributes = new PoolAttributes();
        public final PoolUpgrades upgrades = new PoolUpgrades();

        public Pool () {
            upgrades.setDrawerAttributes(attributes);
        }

        public void changed () {
            version++;
            if (owner != null)
                owner.setDirty();
        }

        // worth persisting if it holds anything at all, not just fluid
        public boolean isEmpty () {
            if (data.hasFluid())
                return false;
            if (attributes.isItemLocked(LockAttribute.LOCK_EMPTY) || attributes.isConcealed() || attributes.isShowingQuantity())
                return false;

            for (int i = 0; i < upgrades.getSlotCount(); i++) {
                if (!upgrades.getUpgrade(i).isEmpty())
                    return false;
            }
            return true;
        }

        public long capacityDroplets () {
            if (attributes.isUnlimitedStorage() || attributes.isUnlimitedVending())
                return Long.MAX_VALUE / 4;

            long buckets = (long) TankConfig.linkedChannelCapacityBuckets * upgrades.getStorageMultiplier();
            if (upgrades.hasOneStackUpgrade())
                buckets = 1;

            return buckets * BlockEntityTank.DROPLETS_PER_BUCKET;
        }

        public TankTarget target () {
            if (target == null) {
                target = new TankTarget()
                {
                    @Override
                    public TankData data () {
                        return Pool.this.data;
                    }

                    @Override
                    public long capacity () {
                        return Pool.this.capacityDroplets();
                    }

                    @Override
                    public void onChanged () {
                        changed();
                    }

                    @Override
                    public boolean isVoid () {
                        return attributes.isVoid();
                    }

                    @Override
                    public boolean isUnlimitedVending () {
                        return attributes.isUnlimitedVending();
                    }

                    @Override
                    public boolean isFluidLocked () {
                        return attributes.isItemLocked(LockAttribute.LOCK_EMPTY);
                    }

                    @Override
                    public boolean isUnresolved () {
                        return parked != null;
                    }
                };
            }
            return target;
        }

        public class PoolAttributes extends BasicDrawerAttributes
        {
            @Override
            protected void onAttributeChanged () {
                data.setRetainFluid(isItemLocked(LockAttribute.LOCK_EMPTY));
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

            // syncUpgrades() is private in Storage Drawers, but setDrawerAttributes re-runs it
            void load (List<ItemStackWithSlot> stacks) {
                Arrays.fill(upgrades, ItemStack.EMPTY);
                for (ItemStackWithSlot slotStack : stacks) {
                    if (slotStack.isValidInContainer(upgrades.length))
                        upgrades[slotStack.slot()] = slotStack.stack().copy();
                }
                setDrawerAttributes(attributes);
            }
        }
    }

    // Field names for fluid/components/amount match the original codec, so channels written before
    // upgrades existed still load; everything added since is optional.
    private record ChannelEntry(Fluid fluid, DataComponentPatch components, long amount,
                                List<ItemStackWithSlot> upgrades, boolean locked, boolean concealed, boolean showQuantity)
    {
        static final Codec<ChannelEntry> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(ChannelEntry::fluid),
                DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(ChannelEntry::components),
                Codec.LONG.fieldOf("amount").forGetter(ChannelEntry::amount),
                ItemStackWithSlot.CODEC.listOf().optionalFieldOf("upgrades", List.of()).forGetter(ChannelEntry::upgrades),
                Codec.BOOL.optionalFieldOf("locked", false).forGetter(ChannelEntry::locked),
                Codec.BOOL.optionalFieldOf("concealed", false).forGetter(ChannelEntry::concealed),
                Codec.BOOL.optionalFieldOf("show_quantity", false).forGetter(ChannelEntry::showQuantity)
            ).apply(instance, ChannelEntry::new));
    }

    // entries are decoded one by one so a single unreadable channel is parked instead of taking
    // the rest of the file down with it or being dropped on the next save
    private static final Codec<LinkedChannels> CODEC = Codec.unboundedMap(Codec.STRING, Codec.PASSTHROUGH)
        .xmap(LinkedChannels::fromRaw, LinkedChannels::toRaw);

    // both loaders currently patch vanilla's unguarded DataFixTypes.update call to tolerate null,
    // but vanilla itself does not; pass a real constant rather than depend on that patch
    public static final SavedDataType<LinkedChannels> TYPE = new SavedDataType<>(
        ModConstants.loc("linked_channels"), LinkedChannels::new, CODEC, DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES);

    private final Map<String, Pool> pools = new HashMap<>();

    public static LinkedChannels get (MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Pool pool (String key) {
        return pools.computeIfAbsent(key, k -> {
            Pool pool = new Pool();
            pool.owner = this;
            return pool;
        });
    }

    private static LinkedChannels fromRaw (Map<String, Dynamic<?>> map) {
        LinkedChannels channels = new LinkedChannels();
        map.forEach((key, raw) -> {
            Pool pool = channels.pool(key);
            DataResult<ChannelEntry> parsed = ChannelEntry.CODEC.parse(raw);
            if (parsed.result().isEmpty()) {
                pool.parked = raw;
                ParkedChannel.report("fluid", key + " " + ParkedChannel.describe(raw), parsed.error().map(Object::toString).orElse("?"));
                return;
            }
            ChannelEntry entry = parsed.result().get();
            pool.upgrades.load(entry.upgrades());
            pool.attributes.setItemLocked(LockAttribute.LOCK_EMPTY, entry.locked());
            pool.attributes.setItemLocked(LockAttribute.LOCK_POPULATED, entry.locked());
            pool.attributes.setIsConcealed(entry.concealed());
            pool.attributes.setIsShowingQuantity(entry.showQuantity());
            pool.data.setRetainFluid(entry.locked());
            pool.data.setFluid(entry.fluid(), entry.components());
            pool.data.setAmount(entry.amount());
        });
        return channels;
    }

    private Map<String, Dynamic<?>> toRaw () {
        Map<String, Dynamic<?>> out = new HashMap<>();
        pools.forEach((key, pool) -> {
            if (pool.parked != null) {
                out.put(key, pool.parked);
                return;
            }
            if (pool.isEmpty())
                return;

            ChannelEntry entry = new ChannelEntry(pool.data.getFluid(), pool.data.getComponents(), pool.data.getAmount(),
                pool.upgrades.toList(),
                pool.attributes.isItemLocked(LockAttribute.LOCK_EMPTY),
                pool.attributes.isConcealed(),
                pool.attributes.isShowingQuantity());
            ChannelEntry.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, entry).result()
                .ifPresent(tag -> out.put(key, new Dynamic<>(net.minecraft.nbt.NbtOps.INSTANCE, tag)));
        });
        return out;
    }
}
