package com.chaevsfe.drawertanks.block.tile;

import com.chaevsfe.drawertanks.ModConstants;
import com.chaevsfe.drawertanks.block.tile.tiledata.TankData;
import com.chaevsfe.drawertanks.components.TankContents;
import com.chaevsfe.drawertanks.config.TankConfig;
import com.mojang.serialization.Codec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;

public class LinkedChannels extends SavedData
{
    public static class Pool
    {
        public final TankData data = new TankData();
        public long version;
        public boolean fluidLocked;

        LinkedChannels owner;
        private TankTarget target;

        public void changed () {
            version++;
            if (owner != null)
                owner.setDirty();
        }

        // one target per pool, so same-channel tanks share a single transaction participant
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
                        return (long) TankConfig.linkedChannelCapacityBuckets * BlockEntityTank.DROPLETS_PER_BUCKET;
                    }

                    @Override
                    public void onChanged () {
                        changed();
                    }

                    @Override
                    public boolean isFluidLocked () {
                        return Pool.this.fluidLocked;
                    }
                };
            }
            return target;
        }
    }

    private static final Codec<LinkedChannels> CODEC = Codec.unboundedMap(Codec.STRING, TankContents.CODEC)
        .xmap(LinkedChannels::fromMap, LinkedChannels::toMap);

    // vanilla dereferences dataFixType on load without a null check, so this must be a real constant
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

    private static LinkedChannels fromMap (Map<String, TankContents> map) {
        LinkedChannels channels = new LinkedChannels();
        map.forEach((key, contents) -> channels.pool(key).data.fromContents(contents));
        return channels;
    }

    private Map<String, TankContents> toMap () {
        Map<String, TankContents> out = new HashMap<>();
        pools.forEach((key, pool) -> {
            if (!pool.data.isEmpty())
                out.put(key, pool.data.toContents());
        });
        return out;
    }
}
