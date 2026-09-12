package com.chaevsfe.drawertanks.block.tile;

import com.mojang.serialization.Dynamic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// a channel entry that no longer decodes (its mod is missing, or an id changed) is kept as the raw
// data it was saved with and written back untouched, so it comes back when the mod does
final class ParkedChannel
{
    private static final Logger LOG = LoggerFactory.getLogger("drawertanks");
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();

    private ParkedChannel () { }

    static void report (String kind, String key, String error) {
        if (!REPORTED.add(kind + ":" + key))
            return;
        LOG.error("Linked {} channel [{}] could not be read ({}). Its data has been kept and will be retried on "
            + "every load; nothing can be put into that channel until it reads again.", kind, key, error);
    }

    static String describe (Dynamic<?> raw) {
        return raw.get("item").get("id").asString().result()
            .or(() -> raw.get("fluid").asString().result())
            .orElse("?");
    }
}
