package uwu.lopyluna.create_bs.diagnostics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.context.CommandContext;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import uwu.lopyluna.create_bs.content.vault.TieredVaultBlockEntity;
import uwu.lopyluna.create_bs.content.vault.VaultInventoryWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;

public final class VaultDiagnostics {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int DETAIL_LIMIT = 1024;
    private static final DateTimeFormatter FILE_TIME =
        DateTimeFormatter.ofPattern("uuuuMMdd-HHmmss-SSS").withZone(ZoneOffset.UTC);
    private static volatile Session session;

    private VaultDiagnostics() {}

    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("create_bs")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("diagnostics")
                .then(Commands.literal("start").executes(VaultDiagnostics::start))
                .then(Commands.literal("status").executes(VaultDiagnostics::status))
                .then(Commands.literal("stop").executes(VaultDiagnostics::stop))));
    }

    public static boolean isRecording() {
        Session current = session;
        return current != null && current.running;
    }

    private static InventoryStats inventory(VaultInventoryWrapper inventory) {
        Session current = session;
        if (current == null || !current.running) return null;
        Level level = inventory.getOwner().getLevel();
        MinecraftServer server = current.server;
        if (level == null || server == null || level.getServer() != server || !server.isSameThread()) return null;
        InventoryStats stats = current.inventories.get(inventory.getId());
        if (stats != null) return stats;
        if (current.inventories.size() >= DETAIL_LIMIT) {
            current.overflowUsed = true;
            return current.overflow;
        }
        stats = new InventoryStats();
        TieredVaultBlockEntity owner = inventory.getOwner();
        BlockPos pos = owner.getBlockPos();
        stats.info = new JsonObject();
        stats.info.addProperty("handler_id", inventory.getId());
        stats.info.addProperty("dimension", level.dimension().location().toString());
        stats.info.addProperty("block", String.valueOf(ForgeRegistries.BLOCKS.getKey(owner.getBlockState().getBlock())));
        stats.info.addProperty("x", pos.getX());
        stats.info.addProperty("y", pos.getY());
        stats.info.addProperty("z", pos.getZ());
        stats.info.addProperty("width", owner.getWidth());
        stats.info.addProperty("length", owner.getHeight());
        stats.info.addProperty("slots", inventory.getSlots());
        current.inventories.put(inventory.getId(), stats);
        return stats;
    }

    public static ModeStats localCacheQuery(VaultInventoryWrapper inventory, boolean input, boolean hit) {
        InventoryStats stats = inventory(inventory);
        if (stats == null) return null;
        ModeStats mode = stats.mode(input);
        mode.queries++;
        if (hit) mode.localHits++;
        return mode;
    }

    public static void queryAfterCacheMiss(VaultInventoryWrapper inventory, boolean input, ModeStats alreadyCounted) {
        InventoryStats stats = inventory(inventory);
        if (stats != null && stats.mode(input) != alreadyCounted) stats.mode(input).queries++;
    }

    public static ModeStats sharedRequest(VaultInventoryWrapper inventory, boolean input, boolean hit) {
        InventoryStats stats = inventory(inventory);
        if (stats == null) return null;
        ModeStats mode = stats.mode(input);
        mode.sharedRequests++;
        if (hit) {
            mode.sharedHits++;
            mode.avoidedSlots += inventory.getSlots();
        }
        return mode;
    }

    public static void versionIncrement(VaultInventoryWrapper inventory) {
        InventoryStats stats = inventory(inventory);
        if (stats != null) stats.versionIncrements++;
    }

    public static void memberClearSignal(VaultInventoryWrapper inventory) {
        InventoryStats stats = inventory(inventory);
        if (stats != null) stats.memberClearSignals++;
    }

    public static void packagePrepared(VaultInventoryWrapper inventory) {
        InventoryStats stats = inventory(inventory);
        if (stats != null) stats.preparedPackages++;
    }

    public static void packageReceived(PackagerBlockEntity packager) {
        if (!isRecording()) return;
        if (packager.targetInventory.getInventory() instanceof VaultInventoryWrapper inventory) {
            InventoryStats stats = inventory(inventory);
            if (stats != null) stats.receivedPackages++;
        }
    }

    public static void packagesTaken(PackagerBlockEntity packager, int count) {
        if (!isRecording()) return;
        if (packager.targetInventory.getInventory() instanceof VaultInventoryWrapper inventory) {
            InventoryStats stats = inventory(inventory);
            if (stats != null) stats.takenPackages += count;
        }
    }

    private static int start(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (session != null) {
            source.sendFailure(message(session.running ? "already_running" : "export_pending"));
            return 0;
        }
        session = new Session(source.getServer());
        source.sendSuccess(() -> message("started"), false);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        Session current = session;
        if (current == null) {
            context.getSource().sendFailure(message("not_running"));
            return 0;
        }
        if (!current.running) {
            context.getSource().sendFailure(message("export_pending"));
            return 0;
        }
        context.getSource().sendSuccess(() -> message("status", seconds(System.nanoTime() - current.startedNanos)), false);
        sendTotals(context.getSource(), current.totals());
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> context) {
        Session current = session;
        if (current == null) {
            context.getSource().sendFailure(message("not_running"));
            return 0;
        }
        if (current.running) current.freeze("command");
        try {
            Path report = current.save();
            session = null;
            context.getSource().sendSuccess(() -> message("saved", seconds(current.elapsedNanos), report.toString()), false);
            sendTotals(context.getSource(), current.totals());
            return 1;
        } catch (IOException exception) {
            LOGGER.error("Could not export vault diagnostics; frozen counters retained for another stop attempt", exception);
            context.getSource().sendFailure(message("save_failed"));
            return 0;
        }
    }

    public static void serverStopping(ServerStoppingEvent event) {
        Session current = session;
        if (current == null || !current.running || current.server != event.getServer()) return;
        current.freeze("server_stop");
        try {
            LOGGER.info("Vault diagnostics saved on server stop: {}", current.save());
            session = null;
        } catch (IOException exception) {
            LOGGER.error("Could not export vault diagnostics on server stop; frozen counters retained", exception);
        }
    }

    private static Component message(String key, Object... args) {
        return Component.translatable("create_bs.diagnostics." + key, args);
    }

    private static String seconds(long nanos) {
        return String.format(Locale.ROOT, "%.2f", nanos / 1_000_000_000.0);
    }

    private static void sendTotals(CommandSourceStack source, InventoryStats totals) {
        ModeStats combined = new ModeStats();
        combined.add(totals.extractable);
        combined.add(totals.input);
        source.sendSuccess(() -> message("summary", combined.queries, combined.localHits, combined.sharedHits,
            combined.builds, combined.scannedSlots), false);
        source.sendSuccess(() -> message("packages", totals.preparedPackages, totals.takenPackages, totals.receivedPackages), false);
    }

    private static String modVersion(String id) {
        return ModList.get().getModContainerById(id)
            .map(container -> container.getModInfo().getVersion().toString()).orElse("unknown");
    }

    private static final class Session {
        private MinecraftServer server;
        private volatile boolean running = true;
        private final Instant startedAt = Instant.now();
        private final long startedNanos = System.nanoTime();
        private final int startedTick;
        private final Int2ObjectOpenHashMap<InventoryStats> inventories = new Int2ObjectOpenHashMap<>();
        private final InventoryStats overflow = new InventoryStats();
        private boolean overflowUsed;
        private Instant stoppedAt;
        private long elapsedNanos;
        private long elapsedTicks;
        private String endReason;

        private Session(MinecraftServer server) {
            this.server = server;
            startedTick = server.getTickCount();
        }

        private void freeze(String reason) {
            running = false;
            stoppedAt = Instant.now();
            elapsedNanos = System.nanoTime() - startedNanos;
            elapsedTicks = Integer.toUnsignedLong(server.getTickCount() - startedTick);
            endReason = reason;
            server = null; // A frozen report must not retain a stopped world/server.
        }

        private InventoryStats totals() {
            InventoryStats total = new InventoryStats();
            inventories.values().forEach(total::add);
            total.add(overflow);
            return total;
        }

        private Path save() throws IOException {
            JsonObject report = new JsonObject();
            report.addProperty("schema_version", 1);
            report.addProperty("mod_version", modVersion("create_bs"));
            report.addProperty("minecraft_version", SharedConstants.getCurrentVersion().getName());
            report.addProperty("forge_version", modVersion("forge"));
            report.addProperty("create_version", modVersion("create"));
            report.addProperty("started_at_utc", startedAt.toString());
            report.addProperty("stopped_at_utc", stoppedAt.toString());
            report.addProperty("duration_seconds", elapsedNanos / 1_000_000_000.0);
            report.addProperty("server_tick_delta", elapsedTicks);
            report.addProperty("end_reason", endReason);
            report.addProperty("scope", "Server-thread operations on Bigger Storage vault handlers only");
            report.addProperty("inventory_detail_limit", DETAIL_LIMIT);
            report.addProperty("inventory_details_truncated", overflowUsed);
            report.add("totals", totals().json());
            JsonArray details = new JsonArray();
            inventories.values().stream()
                .sorted(Comparator.comparingLong((InventoryStats stats) -> stats.extractable.buildNanos + stats.input.buildNanos).reversed())
                .forEach(stats -> details.add(stats.json()));
            report.add("inventories", details);
            report.add("overflow_totals", overflow.json());
            JsonArray notes = new JsonArray();
            notes.add("query_calls include Create-local hits; shared_requests exclude those early returns");
            notes.add("version_increment_calls count wrapper increments, not packages or unique slot changes");
            notes.add("member_or_structure_clear_signals can accompany the same mutation's version increment");
            notes.add("completed_scanned_slots exclude aborted scans; build/copy failures are separate counters");
            notes.add("avoided_slot_visits estimate shared hits times handler slot count, not measured time saved");
            notes.add("elapsed build/copy times include scheduling and GC and may overlap in reentrant calls");
            notes.add("package counts cover preparation, successful unwrap calls and capability extraction; player hand pickups are excluded");
            notes.add("package counters measure flow within this window, not queue length; pre-existing queued packages can be taken during recording");
            notes.add("starting diagnostics does not reset or invalidate inventory caches");
            report.add("notes", notes);
            Path directory = FMLPaths.GAMEDIR.get().resolve("local/performance-diagnostics/create_bs");
            Files.createDirectories(directory);
            Path output = directory.resolve("packager-diagnostics-" + FILE_TIME.format(stoppedAt)
                + "-" + UUID.randomUUID().toString().substring(0, 8) + ".json");
            Files.writeString(output, GSON.toJson(report) + "\n", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return output.toAbsolutePath();
        }
    }

    private static final class InventoryStats {
        private JsonObject info;
        private final ModeStats extractable = new ModeStats();
        private final ModeStats input = new ModeStats();
        private long versionIncrements, memberClearSignals, preparedPackages, takenPackages, receivedPackages;

        private ModeStats mode(boolean inputSlots) { return inputSlots ? input : extractable; }

        private void add(InventoryStats other) {
            versionIncrements += other.versionIncrements;
            memberClearSignals += other.memberClearSignals;
            preparedPackages += other.preparedPackages;
            takenPackages += other.takenPackages;
            receivedPackages += other.receivedPackages;
            extractable.add(other.extractable);
            input.add(other.input);
        }

        private JsonObject json() {
            JsonObject result = info == null ? new JsonObject() : info.deepCopy();
            result.addProperty("version_increment_calls", versionIncrements);
            result.addProperty("member_or_structure_clear_signals", memberClearSignals);
            result.addProperty("packages_prepared", preparedPackages);
            result.addProperty("packages_taken_via_capability", takenPackages);
            result.addProperty("successful_unwrap_calls", receivedPackages);
            result.add("extractable", extractable.json());
            result.add("input_slots", input.json());
            return result;
        }
    }

    public static final class ModeStats {
        private long queries, localHits, sharedRequests, sharedHits, builds, retainedBuilds, discardedBuilds, buildFailures;
        private long scannedSlots, avoidedSlots, buildNanos, maxBuildNanos, copies, copyFailures, copyNanos, maxCopyNanos;

        public void rebuilt(long nanos, int slots, boolean completed, boolean retained) {
            builds++;
            scannedSlots += slots;
            buildNanos += nanos;
            maxBuildNanos = Math.max(maxBuildNanos, nanos);
            if (!completed) buildFailures++;
            else if (retained) retainedBuilds++;
            else discardedBuilds++;
        }

        public void copied(long nanos, boolean completed) {
            copies++;
            if (!completed) copyFailures++;
            copyNanos += nanos;
            maxCopyNanos = Math.max(maxCopyNanos, nanos);
        }

        private void add(ModeStats other) {
            queries += other.queries; localHits += other.localHits;
            sharedRequests += other.sharedRequests; sharedHits += other.sharedHits;
            builds += other.builds; retainedBuilds += other.retainedBuilds;
            discardedBuilds += other.discardedBuilds; buildFailures += other.buildFailures;
            scannedSlots += other.scannedSlots; avoidedSlots += other.avoidedSlots;
            buildNanos += other.buildNanos; maxBuildNanos = Math.max(maxBuildNanos, other.maxBuildNanos);
            copies += other.copies; copyFailures += other.copyFailures;
            copyNanos += other.copyNanos; maxCopyNanos = Math.max(maxCopyNanos, other.maxCopyNanos);
        }

        private JsonObject json() {
            JsonObject result = new JsonObject();
            result.addProperty("query_calls", queries);
            result.addProperty("create_local_cache_hits", localHits);
            result.addProperty("shared_requests", sharedRequests);
            result.addProperty("shared_cache_hits", sharedHits);
            result.addProperty("shared_hit_rate", sharedRequests == 0 ? 0 : (double) sharedHits / sharedRequests);
            result.addProperty("build_attempts", builds);
            result.addProperty("retained_builds", retainedBuilds);
            result.addProperty("discarded_builds", discardedBuilds);
            result.addProperty("build_failures", buildFailures);
            result.addProperty("completed_scanned_slots", scannedSlots);
            result.addProperty("avoided_slot_visits", avoidedSlots);
            result.addProperty("build_total_ms", buildNanos / 1_000_000.0);
            result.addProperty("build_max_ms", maxBuildNanos / 1_000_000.0);
            result.addProperty("build_mean_ms", builds == 0 ? 0 : buildNanos / 1_000_000.0 / builds);
            result.addProperty("copy_calls", copies);
            result.addProperty("copy_failures", copyFailures);
            result.addProperty("copy_total_ms", copyNanos / 1_000_000.0);
            result.addProperty("copy_max_ms", maxCopyNanos / 1_000_000.0);
            return result;
        }
    }
}
