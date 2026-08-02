package net.swzo.brassworksmissions;

import com.mojang.logging.LogUtils;
import com.tterrag.registrate.util.RegistrateDistExecutor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.swzo.brassworksmissions.config.Config;
import net.swzo.brassworksmissions.init.BrassworksmissionsModMenus;
import net.swzo.brassworksmissions.init.CustomStats;
import net.swzo.brassworksmissions.missions.MissionManager;
import net.swzo.brassworksmissions.missions.MissionRegistry;
import net.swzo.brassworksmissions.network.BrassworksmissionsModVariables;
import net.swzo.brassworksmissions.network.MenuStateUpdateMessage;
import net.swzo.brassworksmissions.network.OpenMissionsUIMessage;
import net.swzo.brassworksmissions.network.UiButtonMessage;
import net.swzo.brassworksmissions.network.UpdateSelectedMissionMessage;
import org.slf4j.Logger;

@Mod("brassworksmissions")
public class BrassworksmissionsMod {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MODID = "brassworksmissions";

    private static final MissionManager missionManager = new MissionManager();

    public BrassworksmissionsMod(IEventBus modEventBus, ModContainer container) {
        NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::registerNetworking);
        modEventBus.addListener(this::commonSetup);

        CustomStats.CUSTOM_STATS.register(modEventBus);
        BrassworksmissionsModVariables.ATTACHMENT_TYPES.register(modEventBus);
        BrassworksmissionsModMenus.REGISTRY.register(modEventBus);
        container.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
        container.registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);
        RegistrateDistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BrassworksmissionsClient.registerConfigScreen(container));
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(MissionRegistry::registerMissionTypes);
    }

    @SubscribeEvent
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        missionManager.initialize();
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        if (Config.SERVER.LIVE_RELOAD.get()) {
            missionManager.startLiveReload(event.getServer());
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        missionManager.stopLiveReload();
    }

    public static MissionManager getMissionManager() {
        return missionManager;
    }
    public static ItemStack getRewardItem() {
        String configuredItem = Config.SERVER.REWARD_ITEM.get();
        ResourceLocation rewardId = ResourceLocation.tryParse(configuredItem);
        if (rewardId == null || !BuiltInRegistries.ITEM.containsKey(rewardId)) {
            LOGGER.error("Unknown mission reward item '{}'. Using minecraft:diamond.", configuredItem);
            return new ItemStack(Items.DIAMOND);
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(rewardId));
    }

    private void registerNetworking(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(MODID);
        registrar.playToClient(
                BrassworksmissionsModVariables.PlayerVariablesSyncMessage.TYPE,
                BrassworksmissionsModVariables.PlayerVariablesSyncMessage.STREAM_CODEC,
                BrassworksmissionsModVariables.PlayerVariablesSyncMessage::handleData
        );
        registrar.playToServer(
                UpdateSelectedMissionMessage.TYPE,
                UpdateSelectedMissionMessage.STREAM_CODEC,
                UpdateSelectedMissionMessage::handleData
        );
        registrar.playToServer(
                OpenMissionsUIMessage.TYPE,
                OpenMissionsUIMessage.STREAM_CODEC,
                OpenMissionsUIMessage::handleData
        );
        registrar.playToServer(
                UiButtonMessage.TYPE,
                UiButtonMessage.STREAM_CODEC,
                UiButtonMessage::handleData
        );
        registrar.playBidirectional(
                MenuStateUpdateMessage.TYPE,
                MenuStateUpdateMessage.STREAM_CODEC,
                MenuStateUpdateMessage::handleMenuState
        );
    }

}
