package com.chaevsfe.drawertanks.block.tile;

import com.chaevsfe.drawertanks.ModConstants;
import com.chaevsfe.drawertanks.block.tile.tiledata.TankData;
import com.chaevsfe.drawertanks.components.TankContents;
import com.mojang.serialization.Codec;
import net.minecraft.server.MinecraftServer;
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
    }

    private static final Codec<LinkedChannels> CODEC = Codec.unboundedMap(Codec.STRING, TankContents.CODEC)
        .xmap(LinkedChannels::fromMap, LinkedChannels::toMap);

    public static final SavedDataType<LinkedChannels> TYPE = new SavedDataType<>(
        ModConstants.loc("linked_channels"), LinkedChannels::new, CODEC, (net.minecraft.util.datafix.DataFixTypes) null);

    private final Map<String, Pool> pools = new HashMap<>();

    public static LinkedChannels get (MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Pool pool (String key) {
        return pools.computeIfAbsent(key, k -> new Pool());
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
