package net.swzo.brassworksmissions;

import net.createmod.catnip.config.ui.BaseConfigScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrassworksmissionsClient {
    private static final Logger log = LoggerFactory.getLogger(BrassworksmissionsClient.class);
    public static void onCtorClient(IEventBus modEventBus, IEventBus forgeEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (mc, parent) -> new BaseConfigScreen(parent, BrassworksmissionsMod.MODID));
    }
}