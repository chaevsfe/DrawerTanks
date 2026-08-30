package com.chaevsfe.drawertanks.block.tile;

import com.chaevsfe.drawertanks.ModConstants;
import com.chaevsfe.drawertanks.config.TankConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
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

        public boolean isEmpty () {
            return prototype.isEmpty() || count <= 0;
        }

        public void changed () {
            version++;
            if (owner != null)
                owner.setDirty();
        }

        public long capacity () {
            return capacityFor(prototype);
        }

        public static long capacityFor (ItemStack prototype) {
            int stackSize = prototype.isEmpty() ? 64 : prototype.getMaxStackSize();
            return (long) TankConfig.linkedChannelCapacityStacks * stackSize;
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

    private record PoolContents(ItemStack prototype, long count)
    {
        static final Codec<PoolContents> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                ItemStack.CODEC.fieldOf("item").forGetter(PoolContents::prototype),
                Codec.LONG.fieldOf("count").forGetter(PoolContents::count)
            ).apply(instance, PoolContents::new));
    }

    private static final Codec<LinkedItemChannels> CODEC = Codec.unboundedMap(Codec.STRING, PoolContents.CODEC)
        .xmap(LinkedItemChannels::fromMap, LinkedItemChannels::toMap);

    // vanilla dereferences dataFixType on load without a null check, so this must be a real constant
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
        map.forEach((key, contents) -> channels.pool(key).set(contents.prototype(), contents.count()));
        return channels;
    }

    private Map<String, PoolContents> toMap () {
        Map<String, PoolContents> out = new HashMap<>();
        pools.forEach((key, pool) -> {
            if (!pool.isEmpty())
                out.put(key, new PoolContents(pool.prototype, pool.count));
        });
        return out;
    }
}
