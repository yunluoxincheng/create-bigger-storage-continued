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
import java.lang.ref.WeakReference;
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
    private static long nextSessionId;

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
        if (!isRecording()) return null;
        return inventory(inventory.diagnosticsOrigin());
    }

    /** Value metadata plus a weak server identity; safe to retain across recording windows. */
    public static final class Origin {
        private final int handlerId;
        private final WeakReference<MinecraftServer> server;
        private final JsonObject info;

        private Origin(int handlerId, MinecraftServer server, JsonObject info) {
            this.handlerId = handlerId;
            this.server = new WeakReference<>(server);
            this.info = info;
        }
    }

    public static Origin origin(VaultInventoryWrapper inventory) {
        TieredVaultBlockEntity owner = inventory.getOwner();
        Level level = owner.getLevel();
        if (level == null || level.getServer() == null) return null;
        BlockPos pos = owner.getBlockPos();
        JsonObject info = new JsonObject();
        info.addProperty("handler_id", inventory.getId());
        info.addProperty("dimension", level.dimension().location().toString());
        info.addProperty("block", String.valueOf(ForgeRegistries.BLOCKS.getKey(owner.getBlockState().getBlock())));
        info.addProperty("x", pos.getX());
        info.addProperty("y", pos.getY());
        info.addProperty("z", pos.getZ());
        info.addProperty("width", owner.getWidth());
        info.addProperty("length", owner.getHeight());
        info.addProperty("slots", inventory.getSlots());
        return new Origin(inventory.getId(), level.getServer(), info);
    }

    private static InventoryStats inventory(Origin origin) {
        Session current = session;
        if (current == null || !current.running || origin == null) return null;
        MinecraftServer server = current.server;
        if (server == null || origin.server.get() != server || !server.isSameThread()) return null;
        InventoryStats stats = current.inventories.get(origin.handlerId);
        if (stats != null) return stats;
        if (current.inventories.size() >= DETAIL_LIMIT) {
            current.overflowUsed = true;
            return current.overflow;
        }
        stats = new InventoryStats(current.id);
        stats.info = origin.info.deepCopy();
        current.inventories.put(origin.handlerId, stats);
        return stats;
    }

    public static ModeStats query(VaultInventoryWrapper inventory, boolean input, boolean internal, boolean logical,
                                  boolean hit) {
        InventoryStats stats = inventory(inventory);
        if (stats == null) return null;
        ModeStats mode = stats.mode(input);
        if (logical) {
            mode.queries++;
            if (internal) mode.stockQueries++;
            else mode.publicQueries++;
        }
        mode.queryAttempts++;
        if (hit) mode.localHits++;
        return mode;
    }

    public static ModeStats sharedRequest(VaultInventoryWrapper inventory, boolean input, boolean hit, boolean getter) {
        InventoryStats stats = inventory(inventory);
        if (stats == null) return null;
        ModeStats mode = stats.mode(input);
        mode.sharedRequests++;
        if (getter) mode.getterRequests++;
        else mode.directRequests++;
        if (hit) {
            mode.sharedHits++;
            mode.avoidedSlots += inventory.getSlots();
        }
        return mode;
    }

    public enum CopyReason { PUBLIC_FRESH, PUBLIC_LOCAL, ARRIVAL_BEFORE, ARRIVAL_AFTER, SAVE, DIRECT_API, EAGER_FALLBACK }
    public enum DeferredEvent { INSTALL, MATERIALIZE, DROP }

    public static void deferred(Origin origin, boolean input, DeferredEvent event) {
        InventoryStats stats = inventory(origin);
        if (stats == null) return;
        ModeStats mode = stats.mode(input);
        switch (event) {
            case INSTALL -> mode.deferredInstalls++;
            case MATERIALIZE -> mode.deferredMaterializations++;
            case DROP -> mode.deferredDrops++;
        }
    }

    public static CopyEvent copyStarted(Origin origin, boolean input, CopyReason reason) {
        InventoryStats stats = inventory(origin);
        return stats == null ? null : new CopyEvent(stats.mode(input), reason);
    }

    public static final class CopyEvent {
        private final ModeStats stats;
        private final CopyReason reason;
        private final long started = System.nanoTime();
        private boolean recorded, discarded;
        private CopyEvent previous;

        private CopyEvent(ModeStats stats, CopyReason reason) { this.stats = stats; this.reason = reason; }

        public CopyEvent retainAfter(CopyEvent previous) {
            this.previous = previous;
            return this;
        }

        public static void discardSince(CopyEvent current, CopyEvent checkpoint) {
            for (; current != null && current != checkpoint; current = current.previous) current.discarded();
        }

        public void finished(boolean completed, int groups) {
            if (!stats.active()) return;
            stats.copied(System.nanoTime() - started, completed, reason, groups);
            recorded = completed;
        }

        public void discarded() {
            if (recorded && !discarded && stats.active()) {
                stats.copyDiscards++;
                discarded = true;
            }
        }
    }

    public static void versionIncrement(VaultInventoryWrapper inventory) {
        InventoryStats stats = inventory(inventory);
        if (stats != null) stats.versionIncrements++;
    }

    public static void memberClearSignal(VaultInventoryWrapper inventory) {
        InventoryStats stats = inventory(inventory);
        if (stats != null) stats.memberClearSignals++;
    }

    public static void memberSlotChange(VaultInventoryWrapper inventory) {
        InventoryStats stats = inventory(inventory);
        if (stats != null) stats.memberSlotChanges++;
    }

    public static void idleRelease(VaultInventoryWrapper inventory) {
        InventoryStats stats = inventory(inventory);
        if (stats != null) stats.idleReleases++;
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
        ModeStats combined = new ModeStats(0);
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
        private final long id = ++nextSessionId;
        private MinecraftServer server;
        private volatile boolean running = true;
        private final Instant startedAt = Instant.now();
        private final long startedNanos = System.nanoTime();
        private final int startedTick;
        private final Int2ObjectOpenHashMap<InventoryStats> inventories = new Int2ObjectOpenHashMap<>();
        private final InventoryStats overflow = new InventoryStats(id);
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
            InventoryStats total = new InventoryStats(0);
            inventories.values().forEach(total::add);
            total.add(overflow);
            return total;
        }

        private Path save() throws IOException {
            JsonObject report = new JsonObject();
            report.addProperty("schema_version", 3);
            report.addProperty("summary_strategy", "R2 dirty-slot contributions and affected-Item replay");
            report.addProperty("copy_strategy", "C3 internal deferred snapshots; independent public, arrival and save summaries");
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
            notes.add("query_calls are logical boolean-getter queries; query_attempts include bounded retries; forwarding getters do not count twice");
            notes.add("query_calls split into stock_check_queries and public_queries; local hits and getter_shared_requests classify attempts, not logical queries");
            notes.add("shared_requests split into getter_shared_requests and direct_api_shared_requests; copy_calls are independent materialization attempts, not shared requests");
            notes.add("copy_calls equal the sum of copy_reason_calls; copy_discarded counts completed copies rejected before arrival side effects");
            notes.add("copies and deferred events belong to the snapshot origin handler/mode, which may differ from the current getter mode");
            notes.add("pending snapshots may cross recording windows; installs need not equal materializations plus drops within one window");
            notes.add("operations crossing stop/start do not update frozen counters; deferred copies started in a later window belong to that window");
            notes.add("version_increment_calls count wrapper increments, not packages or unique slot changes");
            notes.add("member_or_structure_clear_signals can accompany the same mutation's version increment");
            notes.add("member_slot_change_signals count callbacks, not distinct dirty slots; same-content assignments can conservatively advance versions");
            notes.add("build_attempts include full and incremental rebuilds and may exceed shared misses when reentrant changes require retries");
            notes.add("completed_scanned_slots count actual inventory slot reads, not in-memory contribution visits");
            notes.add("incremental_avoided_slot_visits estimate retained incremental builds times unread handler slots");
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
        private final ModeStats extractable;
        private final ModeStats input;
        private long versionIncrements, memberClearSignals, memberSlotChanges, idleReleases;
        private long preparedPackages, takenPackages, receivedPackages;

        private InventoryStats(long recordingId) {
            extractable = new ModeStats(recordingId);
            input = new ModeStats(recordingId);
        }

        private ModeStats mode(boolean inputSlots) { return inputSlots ? input : extractable; }

        private void add(InventoryStats other) {
            versionIncrements += other.versionIncrements;
            memberClearSignals += other.memberClearSignals;
            memberSlotChanges += other.memberSlotChanges;
            idleReleases += other.idleReleases;
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
            result.addProperty("member_slot_change_signals", memberSlotChanges);
            result.addProperty("idle_cache_releases", idleReleases);
            result.addProperty("packages_prepared", preparedPackages);
            result.addProperty("packages_taken_via_capability", takenPackages);
            result.addProperty("successful_unwrap_calls", receivedPackages);
            result.add("extractable", extractable.json());
            result.add("input_slots", input.json());
            return result;
        }
    }

    public static final class ModeStats {
        private final long recordingId;
        private long stockQueries, publicQueries, queryAttempts, getterRequests, directRequests, incompleteAttempts;
        private long deferredInstalls, deferredMaterializations, deferredDrops, copyDiscards, copiedGroups;
        private final long[] copyReasons = new long[CopyReason.values().length];
        private long queries, localHits, sharedRequests, sharedHits, builds, retainedBuilds, discardedBuilds, buildFailures;
        private long scannedSlots, avoidedSlots, buildNanos, maxBuildNanos, copies, copyFailures, copyNanos, maxCopyNanos;
        private long fullBuilds, incrementalBuilds, dirtySlots, contributionVisits, recomputedBuckets, recomputedGroups;
        private long incrementalAvoidedSlots;

        private ModeStats(long recordingId) { this.recordingId = recordingId; }

        private boolean active() {
            Session current = session;
            return current != null && current.running && current.id == recordingId;
        }

        public void incompleteAttempt() { if (active()) incompleteAttempts++; }

        public void rebuilt(long nanos, int slots, boolean completed, boolean retained, boolean incremental,
                            int dirty, int contributions, int buckets, int groups, int totalSlots) {
            if (!active()) return;
            builds++;
            if (incremental) incrementalBuilds++;
            else fullBuilds++;
            scannedSlots += slots;
            buildNanos += nanos;
            maxBuildNanos = Math.max(maxBuildNanos, nanos);
            if (!completed) buildFailures++;
            else if (retained) retainedBuilds++;
            else discardedBuilds++;
            if (completed) {
                dirtySlots += dirty;
                contributionVisits += contributions;
                recomputedBuckets += buckets;
                recomputedGroups += groups;
                if (retained && incremental) incrementalAvoidedSlots += totalSlots - slots;
            }
        }

        private void copied(long nanos, boolean completed, CopyReason reason, int groups) {
            copies++;
            copyReasons[reason.ordinal()]++;
            if (!completed) copyFailures++;
            if (completed) copiedGroups += groups;
            copyNanos += nanos;
            maxCopyNanos = Math.max(maxCopyNanos, nanos);
        }

        private void add(ModeStats other) {
            stockQueries += other.stockQueries; publicQueries += other.publicQueries;
            queryAttempts += other.queryAttempts; getterRequests += other.getterRequests;
            directRequests += other.directRequests; incompleteAttempts += other.incompleteAttempts;
            deferredInstalls += other.deferredInstalls; deferredMaterializations += other.deferredMaterializations;
            deferredDrops += other.deferredDrops; copyDiscards += other.copyDiscards; copiedGroups += other.copiedGroups;
            for (int i = 0; i < copyReasons.length; i++) copyReasons[i] += other.copyReasons[i];
            queries += other.queries; localHits += other.localHits;
            sharedRequests += other.sharedRequests; sharedHits += other.sharedHits;
            builds += other.builds; retainedBuilds += other.retainedBuilds;
            discardedBuilds += other.discardedBuilds; buildFailures += other.buildFailures;
            fullBuilds += other.fullBuilds; incrementalBuilds += other.incrementalBuilds;
            dirtySlots += other.dirtySlots; contributionVisits += other.contributionVisits;
            recomputedBuckets += other.recomputedBuckets; recomputedGroups += other.recomputedGroups;
            incrementalAvoidedSlots += other.incrementalAvoidedSlots;
            scannedSlots += other.scannedSlots; avoidedSlots += other.avoidedSlots;
            buildNanos += other.buildNanos; maxBuildNanos = Math.max(maxBuildNanos, other.maxBuildNanos);
            copies += other.copies; copyFailures += other.copyFailures;
            copyNanos += other.copyNanos; maxCopyNanos = Math.max(maxCopyNanos, other.maxCopyNanos);
        }

        private JsonObject json() {
            JsonObject result = new JsonObject();
            result.addProperty("query_calls", queries);
            result.addProperty("stock_check_queries", stockQueries);
            result.addProperty("public_queries", publicQueries);
            result.addProperty("query_attempts", queryAttempts);
            result.addProperty("query_incomplete_attempts", incompleteAttempts);
            result.addProperty("getter_shared_requests", getterRequests);
            result.addProperty("direct_api_shared_requests", directRequests);
            result.addProperty("create_local_cache_hits", localHits);
            result.addProperty("shared_requests", sharedRequests);
            result.addProperty("shared_cache_hits", sharedHits);
            result.addProperty("shared_hit_rate", sharedRequests == 0 ? 0 : (double) sharedHits / sharedRequests);
            result.addProperty("build_attempts", builds);
            result.addProperty("full_build_attempts", fullBuilds);
            result.addProperty("incremental_build_attempts", incrementalBuilds);
            result.addProperty("completed_dirty_slots", dirtySlots);
            result.addProperty("non_empty_contribution_visits", contributionVisits);
            result.addProperty("recomputed_item_buckets", recomputedBuckets);
            result.addProperty("recomputed_groups", recomputedGroups);
            result.addProperty("incremental_avoided_slot_visits", incrementalAvoidedSlots);
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
            result.addProperty("copy_discarded", copyDiscards);
            result.addProperty("copied_groups", copiedGroups);
            result.addProperty("deferred_installs", deferredInstalls);
            result.addProperty("deferred_materializations", deferredMaterializations);
            result.addProperty("deferred_drops", deferredDrops);
            JsonObject reasons = new JsonObject();
            for (CopyReason reason : CopyReason.values())
                reasons.addProperty(reason.name().toLowerCase(Locale.ROOT), copyReasons[reason.ordinal()]);
            result.add("copy_reason_calls", reasons);
            result.addProperty("copy_total_ms", copyNanos / 1_000_000.0);
            result.addProperty("copy_max_ms", maxCopyNanos / 1_000_000.0);
            return result;
        }
    }
}
