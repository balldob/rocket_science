package dev.rocketscience.modules.world;

import dev.rocketscience.hud.Notifications;
import dev.rocketscience.module.Category;
import dev.rocketscience.module.Module;
import dev.rocketscience.render.Render3D;
import dev.rocketscience.setting.*;
import dev.rocketscience.util.ChatUtil;
import dev.rocketscience.util.ColorUtil;
import dev.rocketscience.util.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;

/**
 * Scores loaded chunks by signs of player activity and highlights the suspicious ones.
 *
 * <h2>Scoring</h2>
 * Each entry of the block list is {@code id[=weight]} or {@code #tag[=weight]}, e.g.
 * {@code minecraft:hopper=3} or {@code #minecraft:beds}. Every matching block adds its weight to the
 * chunk score (built-in defaults for common "player" blocks, 2 for anything else). Clusters of
 * 4+ storage blocks add a bonus, since natural structures rarely contain that many.
 *
 * <h2>Performance</h2>
 * Blocks with block entities (chests, beds, signs...) are counted from the chunk's block entity
 * map. Other blocks (crafting tables, anvils...) are found by first asking each 16^3 section's
 * palette whether it contains any target at all ({@link ChunkSection#hasAny}) and only then
 * scanning that section. A handful of chunks is processed per tick (Scan Rate).
 */
public class BaseFinder extends Module {
    /** Built-in weights for the default list; anything else defaults to {@link #DEFAULT_WEIGHT}. */
    private static final Map<String, Double> WEIGHTS = Map.ofEntries(
            Map.entry("minecraft:chest", 1.0),
            Map.entry("minecraft:trapped_chest", 2.0),
            Map.entry("minecraft:barrel", 1.0),
            Map.entry("minecraft:ender_chest", 4.0),
            Map.entry("#minecraft:shulker_boxes", 5.0),
            Map.entry("#minecraft:beds", 2.0),
            Map.entry("minecraft:crafting_table", 1.0),
            Map.entry("minecraft:furnace", 1.0),
            Map.entry("minecraft:blast_furnace", 2.0),
            Map.entry("minecraft:smoker", 2.0),
            Map.entry("#minecraft:all_signs", 2.0),
            Map.entry("minecraft:hopper", 3.0),
            Map.entry("minecraft:enchanting_table", 3.0),
            Map.entry("#minecraft:anvil", 2.0),
            Map.entry("minecraft:beacon", 6.0),
            Map.entry("minecraft:brewing_stand", 1.0),
            Map.entry("minecraft:respawn_anchor", 4.0)
    );
    private static final double DEFAULT_WEIGHT = 2.0;
    private static final Set<String> STORAGE = Set.of("minecraft:chest", "minecraft:trapped_chest", "minecraft:barrel");

    /** A compiled list entry. */
    private record Matcher(String label, double weight, boolean storage) {
    }

    /** Scan result for one chunk. */
    public record ChunkResult(ChunkPos pos, double score, int minY, int maxY, Map<String, Integer> counts) {
    }

    private final StringListSetting blocks = stringList("Blocks", "Blocks (id or #tag, optional =weight) that indicate players",
            new ArrayList<>(WEIGHTS.keySet()).stream().sorted().toList(), BaseFinder::isValidEntry)
            .onChange(v -> this.dirty = true);
    private final DoubleSetting threshold = doubleSetting("Threshold", "Minimum score to flag a chunk", 10, 1, 100, 1);
    private final IntSetting scanRate = intSetting("Scan Rate", "Chunks scanned per tick", 4, 1, 32);
    private final BoolSetting alerts = bool("Alerts", "Chat + toast when a new chunk is flagged", true);
    private final ColorSetting color = color("Color", "Highlight colour", 0xFFE63946);
    private final DoubleSetting opacity = doubleSetting("Fill Opacity", "Box transparency", 0.15, 0, 1, 0.05);

    private final Map<Long, ChunkResult> results = new HashMap<>();
    private final Set<Long> alerted = new HashSet<>();
    private final ArrayDeque<Long> queue = new ArrayDeque<>();
    private final Set<Long> queued = new HashSet<>();
    private Map<Block, Matcher> compiled = Map.of();
    private boolean dirty = true;

    public BaseFinder() {
        super("Base Finder", "Detect chunks with signs of player activity", Category.WORLD);
    }

    @Override
    public String getInfo() {
        return String.valueOf(flagged().size());
    }

    /** Chunks whose score meets the threshold. */
    public List<ChunkResult> flagged() {
        List<ChunkResult> list = new ArrayList<>();
        for (ChunkResult r : results.values()) if (r.score >= threshold.get()) list.add(r);
        return list;
    }

    @Override
    protected void onEnable() {
        dirty = true;
    }

    @Override
    protected void onDisable() {
        results.clear();
        queue.clear();
        queued.clear();
    }

    @Override
    public void onWorldLeave() {
        results.clear();
        alerted.clear();
        queue.clear();
        queued.clear();
        dirty = true; // tags may differ on the next server
    }

    /** Fabric chunk-load event: scan freshly loaded chunks first. */
    public void onChunkLoad(WorldChunk chunk) {
        if (!isEnabled()) return;
        long key = chunk.getPos().toLong();
        if (queued.add(key)) queue.addFirst(key);
    }

    public void onChunkUnload(WorldChunk chunk) {
        results.remove(chunk.getPos().toLong());
    }

    @Override
    public void onTick() {
        if (mc.world == null || mc.player == null) return;
        if (dirty) compile();
        if (queue.isEmpty()) {
            // Continuous re-scan so newly placed/broken blocks are picked up
            WorldUtil.forEachLoadedChunk(c -> {
                long key = c.getPos().toLong();
                if (queued.add(key)) queue.addLast(key);
            });
        }
        for (int i = 0; i < scanRate.get() && !queue.isEmpty(); i++) {
            long key = queue.pollFirst();
            queued.remove(key);
            ChunkPos cp = new ChunkPos(key);
            WorldChunk chunk = WorldUtil.getChunk(cp.x, cp.z);
            if (chunk == null) {
                results.remove(key);
                continue;
            }
            ChunkResult r = scan(chunk);
            if (r == null) results.remove(key);
            else {
                results.put(key, r);
                if (r.score >= threshold.get() && alerted.add(key) && alerts.get()) alert(r);
            }
        }
    }

    /** Resolves the list entries against the block registry (tags need a joined world). */
    private void compile() {
        Map<Block, Matcher> map = new HashMap<>();
        for (String raw : blocks.get()) {
            String[] parts = raw.split("=", 2);
            String spec = parts[0].trim();
            double weight = WEIGHTS.getOrDefault(spec, DEFAULT_WEIGHT);
            if (parts.length > 1) {
                try {
                    weight = Double.parseDouble(parts[1].trim());
                } catch (NumberFormatException ignored) {
                    // validated on input; keep default
                }
            }
            String label = spec.replace("minecraft:", "");
            Matcher m = new Matcher(label, weight, STORAGE.contains(spec));
            if (spec.startsWith("#")) {
                Identifier tagId = Identifier.tryParse(spec.substring(1));
                if (tagId == null) continue;
                TagKey<Block> tag = TagKey.of(RegistryKeys.BLOCK, tagId);
                for (Block b : Registries.BLOCK) {
                    if (b.getDefaultState().isIn(tag)) map.putIfAbsent(b, m);
                }
            } else {
                Block block = findBlock(Identifier.tryParse(spec));
                if (block != null) map.put(block, m);
            }
        }
        compiled = map;
        dirty = false;
        results.clear();
        alerted.clear();
    }

    private ChunkResult scan(WorldChunk chunk) {
        Map<Block, Matcher> targets = compiled;
        if (targets.isEmpty()) return null;
        Map<String, Integer> counts = new TreeMap<>();
        double score = 0;
        int storage = 0;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

        // 1) blocks with block entities
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            Matcher m = targets.get(be.getCachedState().getBlock());
            if (m == null) continue;
            score += m.weight;
            if (m.storage) storage++;
            counts.merge(m.label, 1, Integer::sum);
            minY = Math.min(minY, be.getPos().getY());
            maxY = Math.max(maxY, be.getPos().getY());
        }

        // 2) plain blocks: only scan sections whose palette contains a target
        ChunkSection[] sections = chunk.getSectionArray();
        for (int i = 0; i < sections.length; i++) {
            ChunkSection section = sections[i];
            if (section == null || section.isEmpty()) continue;
            if (!section.hasAny(s -> isPlainTarget(targets, s))) continue;
            int baseY = ChunkSectionPos.getBlockCoord(chunk.sectionIndexToCoord(i));
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (!isPlainTarget(targets, state)) continue;
                        Matcher m = targets.get(state.getBlock());
                        score += m.weight;
                        counts.merge(m.label, 1, Integer::sum);
                        minY = Math.min(minY, baseY + y);
                        maxY = Math.max(maxY, baseY + y);
                    }
                }
            }
        }

        if (counts.isEmpty()) return null;
        if (storage >= 4) score += storage * 0.5; // cluster bonus
        return new ChunkResult(chunk.getPos(), score, minY, maxY, counts);
    }

    private static boolean isPlainTarget(Map<Block, Matcher> targets, BlockState state) {
        return !state.hasBlockEntity() && targets.containsKey(state.getBlock());
    }

    private void alert(ChunkResult r) {
        int x = r.pos.getCenterX(), z = r.pos.getCenterZ();
        StringBuilder sb = new StringBuilder();
        r.counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(4)
                .forEach(e -> sb.append(sb.isEmpty() ? "" : ", ").append(e.getValue()).append(' ').append(e.getKey()));
        ChatUtil.info(String.format("Possible base at X %d, Z %d (Y %d-%d) score %.0f: %s", x, z, r.minY, r.maxY, r.score, sb));
        Notifications.push("Base Finder", String.format("Score %.0f at %d, %d", r.score, x, z), Notifications.Type.WARNING, 5000);
    }

    @Override
    public void onRender3D(float tickDelta) {
        int c = color.get();
        int fill = ColorUtil.withAlpha(c, opacity.get());
        for (ChunkResult r : results.values()) {
            if (r.score < threshold.get()) continue;
            int x = r.pos.getStartX(), z = r.pos.getStartZ();
            Render3D.box(new Box(x, r.minY, z, x + 16, r.maxY + 1, z + 16), fill, c, 2f);
        }
    }

    /** Validates {@code id[=weight]} / {@code #tag[=weight]} entries typed in the GUI or via .set. */
    private static boolean isValidEntry(String raw) {
        String[] parts = raw.split("=", 2);
        if (parts.length > 1) {
            try {
                Double.parseDouble(parts[1].trim());
            } catch (NumberFormatException e) {
                return false;
            }
        }
        String spec = parts[0].trim();
        if (spec.startsWith("#")) return Identifier.tryParse(spec.substring(1)) != null;
        return findBlock(Identifier.tryParse(spec)) != null;
    }

    /**
     * Looks a block up by id. Iterating with {@code getId} is O(n) but only runs when the list
     * changes, and {@code getId} is stable across Minecraft versions (unlike the by-id getters).
     */
    private static Block findBlock(Identifier id) {
        if (id == null) return null;
        for (Block b : Registries.BLOCK) {
            if (id.equals(Registries.BLOCK.getId(b))) return b;
        }
        return null;
    }
}
