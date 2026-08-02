package net.swzo.brassworksmissions.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.swzo.brassworksmissions.BrassworksmissionsMod;
import net.swzo.brassworksmissions.util.UiUtils;
import org.jetbrains.annotations.NotNull;

public record OpenMissionsUIMessage() implements CustomPacketPayload {

    public static final Type<OpenMissionsUIMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BrassworksmissionsMod.MODID, "open_missions_ui"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMissionsUIMessage> STREAM_CODEC = StreamCodec.unit(new OpenMissionsUIMessage());

    public static void handleData(final OpenMissionsUIMessage message, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                UiUtils.openUi(player.level(), player.getX(), player.getY(), player.getZ(), player);
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
