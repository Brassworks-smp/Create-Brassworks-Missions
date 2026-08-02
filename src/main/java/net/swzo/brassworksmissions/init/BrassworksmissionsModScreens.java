package net.swzo.brassworksmissions.init;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.swzo.brassworksmissions.client.gui.UiScreen;

@EventBusSubscriber(Dist.CLIENT)
public class BrassworksmissionsModScreens {
	@SubscribeEvent
	public static void clientLoad(RegisterMenuScreensEvent event) {
		event.register(BrassworksmissionsModMenus.UI.get(), UiScreen::new);
	}

	public interface ScreenAccessor {
		void updateMenuState(int elementType, String name, Object elementState);
	}
}
