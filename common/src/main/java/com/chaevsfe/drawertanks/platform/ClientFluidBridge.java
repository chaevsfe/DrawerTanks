package com.chaevsfe.drawertanks.platform;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;

public interface ClientFluidBridge
{
    int color (Fluid fluid, DataComponentPatch components);

    int luminance (Fluid fluid, DataComponentPatch components);

    Component fluidName (Fluid fluid, DataComponentPatch components);
}
