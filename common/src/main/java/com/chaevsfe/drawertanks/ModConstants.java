package com.chaevsfe.drawertanks;

import net.minecraft.resources.Identifier;

public final class ModConstants
{
    public static final String MOD_ID = "drawertanks";

    public static Identifier loc(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
