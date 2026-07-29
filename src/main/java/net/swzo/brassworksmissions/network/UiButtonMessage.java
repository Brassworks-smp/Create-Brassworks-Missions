package net.swzo.brassworksmissions.network;

import com.simibubi.create.AllSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.swzo.brassworksmissions.BrassworksmissionsMod;
import net.swzo.brassworksmissions.missions.MissionController;
import net.swzo.brassworksmissions.util.UiUtils;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber
public record UiButtonMessage(int buttonID, int x, int y, int z, int slot) implements CustomPacketPayload {

    public static final Type<UiButtonMessage> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BrassworksmissionsMod.MODID, "ui_buttons"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UiButtonMessage> STREAM_CODEC = StreamCodec.of(
            (RegistryFriendlyByteBuf buffer, UiButtonMessage message) -> {
                buffer.writeInt(message.buttonID);
                buffer.writeInt(message.x);
                buffer.writeInt(message.y);
                buffer.writeInt(message.z);
                buffer.writeInt(message.slot);
            },
            (RegistryFriendlyByteBuf buffer) ->
                    new UiButtonMessage(
                            buffer.readInt(),
                            buffer.readInt(),
                            buffer.readInt(),
                            buffer.readInt(),
                            buffer.readInt()
                    )
    );

    @Override
    public @NotNull Type<UiButtonMessage> type() {
        return TYPE;
    }

    public static void handleData(final UiButtonMessage message, final IPayloadContext context) {
        if (context.flow() == PacketFlow.SERVERBOUND) {
            context.enqueueWork(() ->
                    handleButtonAction(context.player(),
                            message.buttonID,
                            message.x,
                            message.y,
                            message.z,
                            message.slot)
            ).exceptionally(e -> {
                context.connection().disconnect(Component.literal(e.getMessage()));
                return null;
            });
        }
    }

    public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z, int slot) {
        Level world = entity.level();

        if (!world.hasChunkAt(new BlockPos(x, y, z)))
            return;

        if (buttonID == 0) {
            if (entity instanceof ServerPlayer serverPlayer) {

                MissionController.claimAllRewards(serverPlayer);
                AllSoundEvents.STOCK_TICKER_TRADE.playOnServer(world, serverPlayer.blockPosition());
            }
            UiUtils.closeUi(entity);
        }

        if (buttonID == 1) {
            UiUtils.closeUi(entity);
        }

        if (buttonID == 2) {
            if (entity instanceof ServerPlayer serverPlayer) {
                BrassworksmissionsModVariables.PlayerVariables playerVariables = entity.getData(BrassworksmissionsModVariables.PLAYER_VARIABLES);
                int selectedSlot = (int) playerVariables.SelectedMission;
                if (selectedSlot >= 0 && selectedSlot < playerVariables.missionData.getSlotCount()) {
                    MissionController.rerollMission(serverPlayer, selectedSlot);
                }
            }
        }

        if (buttonID == 3) {
            if (entity instanceof ServerPlayer serverPlayer) {
                BrassworksmissionsModVariables.PlayerVariables playerVariables = serverPlayer.getData(BrassworksmissionsModVariables.PLAYER_VARIABLES);
                if (slot < 0 || slot >= playerVariables.missionData.getSlotCount()) {
                    return;
                }
                Integer missionSlot = slot;

                if (playerVariables.trackedMissions.contains(missionSlot)) {
                    playerVariables.trackedMissions.remove(missionSlot);
                } else {
                    playerVariables.trackedMissions.add(missionSlot);
                }

                playerVariables.syncPlayerVariables(serverPlayer);
            }
        }
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        BrassworksmissionsMod.addNetworkMessage(UiButtonMessage.TYPE, UiButtonMessage.STREAM_CODEC, UiButtonMessage::handleData);
    }
}
