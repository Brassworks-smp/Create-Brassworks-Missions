package net.swzo.brassworksmissions.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.swzo.brassworksmissions.BrassworksmissionsMod;
import net.swzo.brassworksmissions.init.BrassworksmissionsModMenus;
import net.swzo.brassworksmissions.init.BrassworksmissionsModScreens;

public record MenuStateUpdateMessage(int elementType, String name, Object elementState) implements CustomPacketPayload {

	public static final Type<MenuStateUpdateMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BrassworksmissionsMod.MODID, "guistate_update"));
	public static final StreamCodec<RegistryFriendlyByteBuf, MenuStateUpdateMessage> STREAM_CODEC = StreamCodec.of(MenuStateUpdateMessage::write, MenuStateUpdateMessage::read);
	public static void write(FriendlyByteBuf buffer, MenuStateUpdateMessage message) {
		buffer.writeInt(message.elementType);
		buffer.writeUtf(message.name);
		if (message.elementType == 0) {
			buffer.writeUtf((String) message.elementState);
		} else if (message.elementType == 1) {
			buffer.writeBoolean((boolean) message.elementState);
		} else if (message.elementType == 2) {
            buffer.writeBoolean((boolean) message.elementState);
        }
	}

	public static MenuStateUpdateMessage read(FriendlyByteBuf buffer) {
		int elementType = buffer.readInt();
		String name = buffer.readUtf();
		Object elementState = null;
		if (elementType == 0) {
			elementState = buffer.readUtf();
		} else if (elementType == 1) {
			elementState = buffer.readBoolean();
		} else if (elementType == 2) {
            elementState = buffer.readBoolean();
        }
		return new MenuStateUpdateMessage(elementType, name, elementState);
	}

	@Override
	public Type<MenuStateUpdateMessage> type() {
		return TYPE;
	}

	public static void handleMenuState(final MenuStateUpdateMessage message, final IPayloadContext context) {
		if (message.name.length() > 256 || message.elementState instanceof String string && string.length() > 8192)
			return;
		context.enqueueWork(() -> {
			if (context.player().containerMenu instanceof BrassworksmissionsModMenus.MenuAccessor menu) {
				menu.getMenuState().put(message.elementType + ":" + message.name, message.elementState);
				if (context.flow() == PacketFlow.CLIENTBOUND && Minecraft.getInstance().screen instanceof BrassworksmissionsModScreens.ScreenAccessor accessor) {
					accessor.updateMenuState(message.elementType, message.name, message.elementState);
				}
			}
		}).exceptionally(e -> {
			context.connection().disconnect(Component.literal(e.getMessage()));
			return null;
		});
	}
}
