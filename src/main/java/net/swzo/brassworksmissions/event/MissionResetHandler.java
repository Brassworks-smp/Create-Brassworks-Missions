package net.swzo.brassworksmissions.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.swzo.brassworksmissions.BrassworksmissionsMod;
import net.swzo.brassworksmissions.config.Config;
import net.swzo.brassworksmissions.missions.MissionController;
import net.swzo.brassworksmissions.missions.PlayerMissionData;
import net.swzo.brassworksmissions.network.BrassworksmissionsModVariables;

import java.time.DayOfWeek;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

import static net.neoforged.neoforge.common.NeoForge.EVENT_BUS;

@EventBusSubscriber(modid = BrassworksmissionsMod.MODID)
public class MissionResetHandler {

    private static List<DayOfWeek> getResetDays(int category) {
        List<? extends String> configured = category == 2
                ? Config.SERVER.CATEGORY_TWO_RESET_DAYS.get()
                : Config.SERVER.MISSION_RESET_DAYS.get();
        return configured.stream()
                .map(String::toUpperCase)
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toList());
    }

    public static long getMostRecentResetTimestamp(int category) {
        List<DayOfWeek> resetDays = getResetDays(category);
        if (resetDays.isEmpty()) {
            return -1;
        }
        int resetHour = category == 2
                ? Config.SERVER.CATEGORY_TWO_RESET_HOUR.get()
                : Config.SERVER.MISSION_RESET_HOUR.get();

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime mostRecentReset = null;

        for (DayOfWeek day : resetDays) {
            ZonedDateTime lastOccurrence = now.with(TemporalAdjusters.previousOrSame(day))
                    .withHour(resetHour)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);

            if (lastOccurrence.isAfter(now)) {
                lastOccurrence = lastOccurrence.minusWeeks(1);
            }

            if (mostRecentReset == null || lastOccurrence.isAfter(mostRecentReset)) {
                mostRecentReset = lastOccurrence;
            }
        }

        return mostRecentReset != null ? mostRecentReset.toEpochSecond() : -1;
    }

    public static long getNextResetTimestamp(int category) {
        List<DayOfWeek> resetDays = getResetDays(category);
        if (resetDays.isEmpty()) {
            return Long.MAX_VALUE;
        }
        int resetHour = category == 2
                ? Config.SERVER.CATEGORY_TWO_RESET_HOUR.get()
                : Config.SERVER.MISSION_RESET_HOUR.get();

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime nextReset = null;

        for (DayOfWeek day : resetDays) {
            ZonedDateTime nextOccurrence = now.with(TemporalAdjusters.nextOrSame(day))
                    .withHour(resetHour)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);

            if (nextOccurrence.isBefore(now) || nextOccurrence.isEqual(now)) {
                nextOccurrence = nextOccurrence.plusWeeks(1);
            }

            if (nextReset == null || nextOccurrence.isBefore(nextReset)) {
                nextReset = nextOccurrence;
            }
        }
        return nextReset != null ? nextReset.toEpochSecond() : Long.MAX_VALUE;
    }

    public static long getNextWeeklyResetTimestamp() {
        return getNextResetTimestamp(1);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            checkAndResetMissions(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 == 0) {
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                checkAndResetMissions(player);
            }
        }
    }

    public static void checkAndResetMissions(ServerPlayer player) {
        BrassworksmissionsModVariables.PlayerVariables playerVariables = player.getData(BrassworksmissionsModVariables.PLAYER_VARIABLES);
        long categoryOneResetTime = getMostRecentResetTimestamp(1);
        long categoryTwoResetTime = Config.SERVER.CATEGORY_TWO_ENABLED.get()
                ? getMostRecentResetTimestamp(2)
                : -1;
        boolean resetCategoryOne = playerVariables.lastWeeklyResetTime < categoryOneResetTime;
        boolean resetCategoryTwo = categoryTwoResetTime != -1
                && playerVariables.lastCategoryTwoResetTime < categoryTwoResetTime;

        if (playerVariables.missionData == null) {
            playerVariables.missionData = new PlayerMissionData();
            MissionController.reassignMissions(player);
            playerVariables.lastWeeklyResetTime = categoryOneResetTime;
            playerVariables.lastCategoryTwoResetTime = categoryTwoResetTime;
            playerVariables.syncPlayerVariables(player);
            EVENT_BUS.post(new MissionEvent.Reset(player));
            return;
        }

        if (resetCategoryOne && resetCategoryTwo) {
            MissionController.reassignMissions(player);
        } else if (resetCategoryOne) {
            MissionController.reassignMissions(player, 1);
        } else if (resetCategoryTwo) {
            MissionController.reassignMissions(player, 2);
        } else {
            return;
        }

        if (resetCategoryOne) {
            playerVariables.lastWeeklyResetTime = categoryOneResetTime;
        }
        if (resetCategoryTwo) {
            playerVariables.lastCategoryTwoResetTime = categoryTwoResetTime;
        }
        BrassworksmissionsMod.LOGGER.info(
                "Reset {} missions for player {}.",
                resetCategoryOne && resetCategoryTwo
                        ? "both mission categories"
                        : "mission category " + (resetCategoryOne ? 1 : 2),
                player.getName().getString()
        );
            playerVariables.syncPlayerVariables(player);
            EVENT_BUS.post(new MissionEvent.Reset(player));
    }
}

