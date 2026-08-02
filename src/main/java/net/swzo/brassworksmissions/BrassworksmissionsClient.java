package net.swzo.brassworksmissions;

import net.createmod.catnip.config.ui.BaseConfigScreen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public class BrassworksmissionsClient {
    public static void registerConfigScreen(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (mc, parent) -> new BaseConfigScreen(parent, BrassworksmissionsMod.MODID));
    }
}
