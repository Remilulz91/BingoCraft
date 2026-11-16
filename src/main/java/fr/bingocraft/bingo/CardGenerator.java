package fr.bingocraft.bingo;

import fr.bingocraft.config.BingoCraftConfig;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Deals a new card from the goal pools, following the difficulty mix of the config
 * (e.g. 40 % easy / 40 % medium / 20 % hard). When a tier is too small for its share,
 * the missing cells are filled with items from the other tiers.
 */
public final class CardGenerator {

    public static final int MIN_SIZE = 3;
    public static final int MAX_SIZE = 5;

    private CardGenerator() { }

    /** Returns a new card, or null if the pools hold fewer than size² items in total. */
    public static BingoCard generate(int size, Random random) {
        int total = size * size;
        Map<Difficulty, Integer> wanted = split(total);

        Set<Item> picked = new LinkedHashSet<>();
        for (Difficulty d : Difficulty.values()) {
            List<Item> pool = new ArrayList<>(GoalPools.get(d));
            Collections.shuffle(pool, random);
            for (int i = 0; i < Math.min(wanted.get(d), pool.size()); i++) picked.add(pool.get(i));
        }

        if (picked.size() < total) {
            List<Item> rest = new ArrayList<>();
            for (Difficulty d : Difficulty.values()) {
                for (Item item : GoalPools.get(d)) if (!picked.contains(item)) rest.add(item);
            }
            Collections.shuffle(rest, random);
            for (Item item : rest) {
                if (picked.size() >= total) break;
                picked.add(item);
            }
        }
        if (picked.size() < total) return null;

        List<Identifier> goals = new ArrayList<>(total);
        for (Item item : picked) goals.add(Registries.ITEM.getId(item));
        Collections.shuffle(goals, random); // hard goals anywhere on the card, not grouped
        return new BingoCard(size, goals);
    }

    /** Number of cells per tier for a card of {@code total} cells. */
    private static Map<Difficulty, Integer> split(int total) {
        BingoCraftConfig cfg = BingoCraftConfig.get();
        int e = Math.max(0, cfg.easyPercent);
        int m = Math.max(0, cfg.mediumPercent);
        int h = Math.max(0, cfg.hardPercent);
        int sum = e + m + h;
        if (sum == 0) { m = 1; sum = 1; }

        int hard = Math.round(total * h / (float) sum);
        int medium = Math.min(total - hard, Math.round(total * m / (float) sum));
        int easy = total - hard - medium;

        Map<Difficulty, Integer> map = new EnumMap<>(Difficulty.class);
        map.put(Difficulty.EASY, easy);
        map.put(Difficulty.MEDIUM, medium);
        map.put(Difficulty.HARD, hard);
        return map;
    }
}
