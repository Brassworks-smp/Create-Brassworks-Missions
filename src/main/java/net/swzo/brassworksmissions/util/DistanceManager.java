package net.swzo.brassworksmissions.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.swzo.brassworksmissions.missions.types.vanilla.AbstractDistanceMissionType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DistanceManager {

    private static final Map<UUID, Map<String, Vec3>> lastPositions = new HashMap<>();
    private static final Map<UUID, Map<String, Double>> accumulatedDistances = new HashMap<>();
    private static final Map<UUID, Map<String, Long>> lastTickTracked = new HashMap<>();

    public static void track(ServerPlayer player, String missionId, int timeoutTicks) {
        UUID uuid = player.getUUID();
        long currentTime = player.level().getGameTime();

        lastPositions.computeIfAbsent(uuid, k -> new HashMap<>());
        accumulatedDistances.computeIfAbsent(uuid, k -> new HashMap<>());
        lastTickTracked.computeIfAbsent(uuid, k -> new HashMap<>());

        long lastTime = lastTickTracked.get(uuid).getOrDefault(missionId, currentTime);

        if (currentTime - lastTime > timeoutTicks) {
            accumulatedDistances.get(uuid).put(missionId, 0.0);
        }

        Vec3 currentPos = player.position();
        Vec3 lastPos = lastPositions.get(uuid).get(missionId);

        if (lastPos != null) {
            long tickDiff = currentTime - lastTime;

            if (tickDiff <= timeoutTicks) {
                double distance = currentPos.distanceTo(lastPos);

                if (distance > 0.01) {
                    double total = accumulatedDistances.get(uuid).getOrDefault(missionId, 0.0) + distance;

                    if (total >= 1.0) {
                        int fullBlocks = (int) total;

                        AbstractDistanceMissionType.handleDistanceChange(player, missionId, fullBlocks);
                        total -= fullBlocks;
                    }
                    accumulatedDistances.get(uuid).put(missionId, total);
                }
            }
        }

        lastPositions.get(uuid).put(missionId, currentPos);
        lastTickTracked.get(uuid).put(missionId, currentTime);
    }

    public static void clear(ServerPlayer player) {
        UUID uuid = player.getUUID();
        lastPositions.remove(uuid);
        accumulatedDistances.remove(uuid);
        lastTickTracked.remove(uuid);
    }
}
