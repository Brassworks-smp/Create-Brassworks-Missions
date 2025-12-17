package net.swzo.brassworksmissions;

import com.mojang.logging.LogUtils;
import com.tterrag.registrate.util.RegistrateDistExecutor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.Tuple;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.swzo.brassworksmissions.config.Config;
import net.swzo.brassworksmissions.init.BrassworksmissionsModMenus;
import net.swzo.brassworksmissions.init.CustomStats;
import net.swzo.brassworksmissions.missions.MissionManager;
import net.swzo.brassworksmissions.missions.MissionRegistry;
import net.swzo.brassworksmissions.missions.reward.RewardManager;
import net.swzo.brassworksmissions.network.BrassworksmissionsModVariables;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mod("brassworksmissions")
public class BrassworksmissionsMod {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MODID = "brassworksmissions";

    private static final MissionManager missionManager = new MissionManager();
    private static RewardManager rewardManager = new RewardManager();

    public BrassworksmissionsMod(IEventBus modEventBus, ModContainer container) {
        NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::registerNetworking);
        modEventBus.addListener(this::commonSetup);

        CustomStats.CUSTOM_STATS.register(modEventBus);
        BrassworksmissionsModVariables.ATTACHMENT_TYPES.register(modEventBus);
        BrassworksmissionsModMenus.REGISTRY.register(modEventBus);
        container.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
        container.registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);
        RegistrateDistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BrassworksmissionsClient.onCtorClient(modEventBus, NeoForge.EVENT_BUS, container));
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(MissionRegistry::registerMissionTypes);
    }

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(missionManager);
        event.addListener(rewardManager);
    }

    public static MissionManager getMissionManager() {
        return missionManager;
    }
    public static RewardManager getRewardManager() {
        return rewardManager;
    }

    private static boolean networkingRegistered = false;
    private static final Map<CustomPacketPayload.Type<?>, NetworkMessage<?>> MESSAGES = new HashMap<>();

    private record NetworkMessage<T extends CustomPacketPayload>(StreamCodec<? extends FriendlyByteBuf, T> reader, IPayloadHandler<T> handler) {
    }

    public static <T extends CustomPacketPayload> void addNetworkMessage(CustomPacketPayload.Type<T> id, StreamCodec<? extends FriendlyByteBuf, T> reader, IPayloadHandler<T> handler) {
        if (networkingRegistered)
            throw new IllegalStateException("Cannot register new network messages after networking has been registered");
        MESSAGES.put(id, new NetworkMessage<>(reader, handler));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void registerNetworking(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(MODID);
        MESSAGES.forEach((id, networkMessage) -> registrar.playBidirectional(id, ((NetworkMessage) networkMessage).reader(), ((NetworkMessage) networkMessage).handler()));
        networkingRegistered = true;
    }

    private static final Collection<Tuple<Runnable, Integer>> workQueue = new ConcurrentLinkedQueue<>();

    public static void queueServerWork(int tick, Runnable action) {
        if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER)
            workQueue.add(new Tuple<>(action, tick));
    }

    @SubscribeEvent
    public void tick(ServerTickEvent.Post event) {
        List<Tuple<Runnable, Integer>> actions = new ArrayList<>();
        workQueue.forEach(work -> {
            work.setB(work.getB() - 1);
            if (work.getB() == 0)
                actions.add(work);
        });
        actions.forEach(e -> e.getA().run());
        workQueue.removeAll(actions);
    }
}