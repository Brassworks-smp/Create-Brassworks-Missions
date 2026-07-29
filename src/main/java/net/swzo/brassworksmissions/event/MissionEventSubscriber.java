package net.swzo.brassworksmissions.event;

import com.simibubi.create.AllBlocks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingUseTotemEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.swzo.brassworksmissions.BrassworksmissionsMod;
import net.swzo.brassworksmissions.missions.ActiveMission;
import net.swzo.brassworksmissions.missions.IMissionType;
import net.swzo.brassworksmissions.missions.MissionController;
import net.swzo.brassworksmissions.missions.MissionRegistry;
import net.swzo.brassworksmissions.missions.types.create.MoveOnBeltMissionType;
import net.swzo.brassworksmissions.missions.types.vanilla.DriveBoatMissionType;
import net.swzo.brassworksmissions.missions.types.vanilla.FlyElytraMissionType;
import net.swzo.brassworksmissions.missions.types.vanilla.RideMobMissionType;
import net.swzo.brassworksmissions.missions.types.vanilla.WalkMissionType;
import net.swzo.brassworksmissions.network.BrassworksmissionsModVariables;
import net.swzo.brassworksmissions.util.DistanceManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

import static net.neoforged.neoforge.common.NeoForge.EVENT_BUS;

@EventBusSubscriber(modid = BrassworksmissionsMod.MODID)
public class MissionEventSubscriber {

    private static final Map<UUID, String> lastMovementType = new HashMap<>();

    private static <E> void handleEvent(E event, ServerPlayer player, BiFunction<IMissionType, ActiveMission, Boolean> check) {
        boolean needsSync = false;
        var missions = MissionController.getMissionData(player).getMissions();

        for (int slot = 0; slot < missions.length; slot++) {
            ActiveMission mission = missions[slot];

            if (mission == null || mission.isComplete()) {
                continue;
            }

            IMissionType type = MissionRegistry.getMissionType(mission.getMissionType());
            if (type != null && check.apply(type, mission)) {
                needsSync = true;
                if (mission.isComplete()) {
                    EVENT_BUS.post(new MissionEvent.Completed(player, mission, slot));
                }
            }
        }

        if (needsSync) {
            player.getData(BrassworksmissionsModVariables.PLAYER_VARIABLES).syncPlayerVariables(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            final int TIMEOUT_TICKS = 40;
            UUID uuid = player.getUUID();

            String currentMovementType = null;
            if (player.isFallFlying()) {
                currentMovementType = FlyElytraMissionType.ID;
            } else if (player.getVehicle() instanceof Boat) {
                currentMovementType = DriveBoatMissionType.ID;
            } else if (player.isPassenger() && player.getVehicle() instanceof LivingEntity) {
                currentMovementType = RideMobMissionType.ID;
            } else if (AllBlocks.BELT.has(player.level().getBlockState(player.getOnPos()))) {
                currentMovementType = MoveOnBeltMissionType.ID;
            } else if (player.onGround()) {
                currentMovementType = WalkMissionType.ID;
            }

            String lastType = lastMovementType.get(uuid);

            if (currentMovementType != null) {
                if (currentMovementType.equals(lastType)) {
                    DistanceManager.track(player, currentMovementType, TIMEOUT_TICKS);
                }
                lastMovementType.put(uuid, currentMovementType);
            } else {
                lastMovementType.remove(uuid);
            }

            handleEvent(event, player, (type, mission) -> type.onPlayerTick(event, mission));
        }
    }
    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            handleEvent(event, player, (type, mission) -> type.onBlockBroken(event, mission));
        }
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            handleEvent(event, player, (type, mission) -> type.onBlockPlaced(event, mission));
        }
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            handleEvent(event, player, (type, mission) -> type.onItemCrafted(event, mission));
        }
    }

    @SubscribeEvent
    public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            handleEvent(event, player, (type, mission) -> type.onItemSmelted(event, mission));
        }
    }

    @SubscribeEvent
    public static void onItemPickedUp(ItemEntityPickupEvent.Post event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            handleEvent(event, player, (type, mission) -> type.onItemPickedUp(event, mission));
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            handleEvent(event, player, (type, mission) -> type.onLivingDeath(event, mission));
        }
    }

    @SubscribeEvent
    public static void onItemFished(ItemFishedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            handleEvent(event, player, (type, mission) -> type.onItemFished(event, mission));
        }
    }

    @SubscribeEvent
    public static void onUseTotem(LivingUseTotemEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            handleEvent(event, player, (type, mission) -> type.onLivingUseTotem(event, mission));
        }
    }

    @SubscribeEvent
    public static void onBreedAnimal(BabyEntitySpawnEvent event) {
        if (event.getCausedByPlayer() instanceof ServerPlayer player) {
            handleEvent(event, player, (type, mission) -> type.onBabyEntitySpawn(event, mission));
        }
    }

    @SubscribeEvent
    public static void onItemConsumed(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            handleEvent(event, player, (type, mission) -> type.onItemConsumed(event, mission));
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        lastMovementType.remove(event.getEntity().getUUID());
        if (event.getEntity() instanceof ServerPlayer player) {
            DistanceManager.clear(player);
        }
    }
}
