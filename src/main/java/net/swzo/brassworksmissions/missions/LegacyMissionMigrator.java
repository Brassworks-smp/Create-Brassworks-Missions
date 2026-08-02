package net.swzo.brassworksmissions.missions;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.swzo.brassworksmissions.BrassworksmissionsMod;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


@EventBusSubscriber(modid = BrassworksmissionsMod.MODID)
public final class LegacyMissionMigrator {
    private static final Gson GSON = new Gson();
    private static final Type MISSION_LIST_TYPE = new TypeToken<List<Mission>>() {
    }.getType();
    private static final String LEGACY_DIRECTORY =
            "data/" + BrassworksmissionsMod.MODID + "/missions/";

    private static List<LegacyMissionFile> legacyFiles = List.of();
    private static String fingerprint = "";

    private LegacyMissionMigrator() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        scan(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            notifyAdministrator(player);
        }
    }

    public static synchronized void scan(MinecraftServer server) {
        Path datapacks = server.getWorldPath(LevelResource.DATAPACK_DIR);
        List<LegacyMissionFile> found = new ArrayList<>();
        if (Files.isDirectory(datapacks)) {
            try (var packs = Files.list(datapacks)) {
                packs.sorted().forEach(pack -> scanPack(pack, found));
            } catch (IOException e) {
                BrassworksmissionsMod.LOGGER.error("Could not scan '{}' for legacy mission datapacks.", datapacks, e);
            }
        }
        legacyFiles = List.copyOf(found);
        fingerprint = calculateFingerprint(legacyFiles);

        if (shouldNotify()) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                notifyAdministrator(player);
            }
        }
    }

    private static void scanPack(Path pack, List<LegacyMissionFile> found) {
        if (Files.isDirectory(pack)) {
            Path missionDirectory = pack.resolve(LEGACY_DIRECTORY);
            if (!Files.isDirectory(missionDirectory)) {
                return;
            }
            try (var files = Files.walk(missionDirectory)) {
                files.filter(Files::isRegularFile)
                        .filter(path -> isLegacyMissionPath(missionDirectory.relativize(path).toString()))
                        .sorted()
                        .forEach(path -> readFolderFile(pack, path, found));
            } catch (IOException e) {
                BrassworksmissionsMod.LOGGER.error("Could not inspect legacy mission datapack '{}'.", pack, e);
            }
            return;
        }

        String name = pack.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        if (!name.endsWith(".zip")) {
            return;
        }
        try (ZipFile zip = new ZipFile(pack.toFile())) {
            zip.stream()
                    .filter(entry -> !entry.isDirectory())
                    .filter(entry -> entry.getName().startsWith(LEGACY_DIRECTORY))
                    .filter(entry -> isLegacyMissionPath(entry.getName().substring(LEGACY_DIRECTORY.length())))
                    .sorted(java.util.Comparator.comparing(ZipEntry::getName))
                    .forEach(entry -> readZipEntry(pack, zip, entry, found));
        } catch (IOException e) {
            BrassworksmissionsMod.LOGGER.error("Could not inspect legacy mission datapack '{}'.", pack, e);
        }
    }

    private static boolean isLegacyMissionPath(String relativePath) {
        String normalized = relativePath.replace('\\', '/');
        return normalized.endsWith(".json") && !normalized.equals("mission_reward.json");
    }

    private static void readFolderFile(Path pack, Path file, List<LegacyMissionFile> found) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            addParsedFile(pack, file, null, reader, found);
        } catch (Exception e) {
            BrassworksmissionsMod.LOGGER.error("Could not read legacy mission file '{}'.", file, e);
        }
    }

    private static void readZipEntry(Path pack, ZipFile zip, ZipEntry entry, List<LegacyMissionFile> found) {
        try (Reader reader = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8)) {
            addParsedFile(pack, null, entry.getName(), reader, found);
        } catch (Exception e) {
            BrassworksmissionsMod.LOGGER.error(
                    "Could not read legacy mission file '{}' from '{}'.",
                    entry.getName(),
                    pack,
                    e
            );
        }
    }

    private static void addParsedFile(
            Path pack,
            Path file,
            String zipEntry,
            Reader reader,
            List<LegacyMissionFile> found
    ) {
        List<Mission> parsed = GSON.fromJson(reader, MISSION_LIST_TYPE);
        if (parsed == null || parsed.isEmpty()) {
            return;
        }
        List<Mission> valid = parsed.stream()
                .filter(mission -> mission != null && mission.validate().isEmpty())
                .toList();
        if (!valid.isEmpty()) {
            found.add(new LegacyMissionFile(pack, file, zipEntry, valid));
        }
    }

    public static synchronized boolean shouldNotify() {
        return !legacyFiles.isEmpty()
                && !BrassworksmissionsMod.getMissionManager().isLegacyFingerprintIgnored(fingerprint);
    }

    public static synchronized void notifyAdministrator(ServerPlayer player) {
        if (!shouldNotify() || !player.getServer().getPlayerList().isOp(player.getGameProfile())) {
            return;
        }
        int missionCount = legacyFiles.stream().mapToInt(file -> file.missions().size()).sum();
        player.sendSystemMessage(Component.literal(
                "Legacy mission datapack detected: " + missionCount + " mission(s) in "
                        + legacyFiles.size() + " file(s). Datapack missions are no longer loaded."
        ).withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal("Choose how to handle the legacy files: ")
                .append(button(
                        "[Transfer and delete]",
                        "/missions legacy transfer_and_delete",
                        "Copy the missions to missions.json, then remove only the legacy mission files."
                ))
                .append(Component.literal(" "))
                .append(button(
                        "[Delete]",
                        "/missions legacy delete",
                        "Remove only the legacy mission files without importing them."
                ))
                .append(Component.literal(" "))
                .append(button(
                        "[Ignore]",
                        "/missions legacy ignore",
                        "Hide this warning until the detected legacy files change."
                )));
    }

    private static MutableComponent button(String label, String command, String hover) {
        return Component.literal(label).withStyle(style -> style
                .withColor(ChatFormatting.YELLOW)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover))));
    }

    public static synchronized MissionManager.OperationResult transferAndDelete() {
        if (legacyFiles.isEmpty()) {
            return MissionManager.OperationResult.failure("No legacy mission datapack files were detected.");
        }
        List<Mission> missions = legacyFiles.stream().flatMap(file -> file.missions().stream()).toList();
        MissionManager.OperationResult transfer = BrassworksmissionsMod.getMissionManager()
                .importLegacyMissions(missions);
        if (!transfer.success()) {
            return transfer;
        }
        MissionManager.OperationResult deletion = deleteLegacyFiles();
        if (!deletion.success()) {
            return MissionManager.OperationResult.failure(
                    transfer.message() + " However, some legacy files could not be deleted: " + deletion.message()
            );
        }
        return MissionManager.OperationResult.success(transfer.message() + " Legacy mission files were deleted.");
    }

    public static synchronized MissionManager.OperationResult deleteLegacyFiles() {
        if (legacyFiles.isEmpty()) {
            return MissionManager.OperationResult.failure("No legacy mission datapack files were detected.");
        }
        Map<Path, Set<String>> zipEntries = new LinkedHashMap<>();
        List<String> failures = new ArrayList<>();
        for (LegacyMissionFile legacyFile : legacyFiles) {
            try {
                if (legacyFile.file() != null) {
                    Files.deleteIfExists(legacyFile.file());
                } else {
                    zipEntries.computeIfAbsent(legacyFile.pack(), ignored -> new LinkedHashSet<>())
                            .add(legacyFile.zipEntry());
                }
            } catch (IOException e) {
                failures.add(legacyFile.displayName() + ": " + e.getMessage());
            }
        }
        for (Map.Entry<Path, Set<String>> entry : zipEntries.entrySet()) {
            try {
                rewriteZipWithout(entry.getKey(), entry.getValue());
            } catch (IOException e) {
                failures.add(entry.getKey().getFileName() + ": " + e.getMessage());
            }
        }
        if (!failures.isEmpty()) {
            return MissionManager.OperationResult.failure(String.join("; ", failures));
        }
        int deleted = legacyFiles.size();
        legacyFiles = List.of();
        fingerprint = "";
        return MissionManager.OperationResult.success("Deleted " + deleted + " legacy mission file(s).");
    }

    public static synchronized MissionManager.OperationResult ignore() {
        if (legacyFiles.isEmpty()) {
            return MissionManager.OperationResult.failure("No legacy mission datapack files were detected.");
        }
        return BrassworksmissionsMod.getMissionManager().ignoreLegacyMissions(fingerprint);
    }

    private static void rewriteZipWithout(Path zipPath, Set<String> removedEntries) throws IOException {
        Path parent = zipPath.toAbsolutePath().getParent();
        Path temporary = Files.createTempFile(parent, zipPath.getFileName().toString() + "-", ".tmp");
        boolean moved = false;
        try {
            try (ZipInputStream input = new ZipInputStream(Files.newInputStream(zipPath));
                 ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(temporary))) {
                ZipEntry entry;
                while ((entry = input.getNextEntry()) != null) {
                    if (!removedEntries.contains(entry.getName())) {
                        ZipEntry copy = new ZipEntry(entry.getName());
                        if (entry.getTime() >= 0L) {
                            copy.setTime(entry.getTime());
                        }
                        output.putNextEntry(copy);
                        input.transferTo(output);
                        output.closeEntry();
                    }
                    input.closeEntry();
                }
            }
            try {
                Files.move(temporary, zipPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, zipPath, StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    private static String calculateFingerprint(List<LegacyMissionFile> files) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (LegacyMissionFile file : files) {
                digest.update(file.displayName().getBytes(StandardCharsets.UTF_8));
                digest.update(GSON.toJson(file.missions()).getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private record LegacyMissionFile(Path pack, Path file, String zipEntry, List<Mission> missions) {
        private String displayName() {
            return file != null ? file.toString() : pack + "!/" + zipEntry;
        }
    }
}

