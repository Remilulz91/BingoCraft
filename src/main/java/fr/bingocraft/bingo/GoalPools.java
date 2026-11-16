package fr.bingocraft.bingo;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.bingocraft.BingoCraft;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The items a card can ask for, by difficulty. Data-driven: every datapack (and every mod)
 * can ship files under {@code data/<namespace>/bingo_pools/*.json}:
 *
 * <pre>
 * { "difficulty": "easy", "items": ["minecraft:oak_log", "othermod:copper_gear"] }
 * </pre>
 *
 * All files are merged. Items whose mod is not installed are skipped, so a pool can safely list
 * items of optional mods. To remove the vanilla items, override
 * {@code data/bingocraft/bingo_pools/vanilla_<tier>.json} with an empty list.
 */
public final class GoalPools {

    private static final String FOLDER = "bingo_pools";

    private static Map<Difficulty, List<Item>> pools = emptyPools();

    private GoalPools() { }

    public static List<Item> get(Difficulty difficulty) {
        return pools.get(difficulty);
    }

    public static int size(Difficulty difficulty) {
        return pools.get(difficulty).size();
    }

    public static void register() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return BingoCraft.id(FOLDER);
                    }

                    @Override
                    public void reload(ResourceManager manager) {
                        load(manager);
                    }
                });
    }

    private static void load(ResourceManager manager) {
        Map<Difficulty, Set<Item>> merged = new EnumMap<>(Difficulty.class);
        for (Difficulty d : Difficulty.values()) merged.put(d, new LinkedHashSet<>());
        int skipped = 0;

        Map<Identifier, Resource> files = manager.findResources(FOLDER, id -> id.getPath().endsWith(".json"));
        for (Map.Entry<Identifier, Resource> file : files.entrySet()) {
            try (Reader reader = file.getValue().getReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                Difficulty difficulty = Difficulty.byId(json.get("difficulty").getAsString());
                if (difficulty == null) {
                    BingoCraft.LOGGER.warn("[Pools] {}: unknown difficulty, file ignored", file.getKey());
                    continue;
                }
                for (JsonElement element : json.getAsJsonArray("items")) {
                    Identifier itemId = Identifier.tryParse(element.getAsString());
                    if (itemId == null || !Registries.ITEM.containsId(itemId)) {
                        skipped++;
                        continue;
                    }
                    merged.get(difficulty).add(Registries.ITEM.get(itemId));
                }
            } catch (Exception e) {
                BingoCraft.LOGGER.error("[Pools] {}: could not be read ({})", file.getKey(), e.getMessage());
            }
        }

        // An item listed in two tiers keeps the easiest one.
        Set<Item> seen = new LinkedHashSet<>();
        Map<Difficulty, List<Item>> result = new EnumMap<>(Difficulty.class);
        for (Difficulty d : Difficulty.values()) {
            List<Item> list = new ArrayList<>();
            for (Item item : merged.get(d)) {
                if (seen.add(item)) list.add(item);
            }
            result.put(d, Collections.unmodifiableList(list));
        }
        pools = result;

        BingoCraft.LOGGER.info("[Pools] Loaded {} easy / {} medium / {} hard goals from {} file(s){}",
                size(Difficulty.EASY), size(Difficulty.MEDIUM), size(Difficulty.HARD), files.size(),
                skipped > 0 ? " (" + skipped + " unknown item(s) skipped)" : "");
    }

    private static Map<Difficulty, List<Item>> emptyPools() {
        Map<Difficulty, List<Item>> map = new EnumMap<>(Difficulty.class);
        for (Difficulty d : Difficulty.values()) map.put(d, List.of());
        return map;
    }
}
