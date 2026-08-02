package net.swzo.brassworksmissions.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.swzo.brassworksmissions.BrassworksmissionsMod;


public class CustomStats {

    public static final DeferredRegister<ResourceLocation> CUSTOM_STATS =
            DeferredRegister.create(Registries.CUSTOM_STAT, BrassworksmissionsMod.MODID);
    public static final DeferredHolder<ResourceLocation, ResourceLocation> MISSIONS_COMPLETED =
            CUSTOM_STATS.register("missions_completed", () -> ResourceLocation.fromNamespaceAndPath(BrassworksmissionsMod.MODID, "missions_completed"));

}


