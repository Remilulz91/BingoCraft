package fr.bingocraft.team;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The race teams, picked from the wool menu ({@code /bingo team}). Each one is backed by a vanilla
 * scoreboard team "bingo_&lt;id&gt;", so name colours and the tab list work as usual.
 */
public enum BingoTeam {
    RED("red", Formatting.RED, Items.RED_WOOL),
    BLUE("blue", Formatting.BLUE, Items.BLUE_WOOL),
    GREEN("green", Formatting.GREEN, Items.LIME_WOOL),
    YELLOW("yellow", Formatting.YELLOW, Items.YELLOW_WOOL),
    ORANGE("orange", Formatting.GOLD, Items.ORANGE_WOOL),
    PURPLE("purple", Formatting.DARK_PURPLE, Items.PURPLE_WOOL),
    AQUA("aqua", Formatting.AQUA, Items.LIGHT_BLUE_WOOL),
    PINK("pink", Formatting.LIGHT_PURPLE, Items.PINK_WOOL);

    public static final String PREFIX = "bingo_";

    private final String id;
    private final Formatting color;
    private final Item wool;

    BingoTeam(String id, Formatting color, Item wool) {
        this.id = id;
        this.color = color;
        this.wool = wool;
    }

    public String id() {
        return id;
    }

    public Formatting color() {
        return color;
    }

    public Item wool() {
        return wool;
    }

    /** Name of the backing scoreboard team. */
    public String scoreboardName() {
        return PREFIX + id;
    }

    /** "Red" / "Rouge", in the team colour. */
    public MutableText displayName() {
        return Text.translatable("bingocraft.team." + id).formatted(color);
    }

    /** RGB of the team colour, for fireworks. */
    public int rgb() {
        Integer value = color.getColorValue();
        return value != null ? value : 0xFFFFFF;
    }

    public static BingoTeam byId(String id) {
        for (BingoTeam t : values()) if (t.id.equals(id)) return t;
        return null;
    }

    public static BingoTeam byScoreboardName(String name) {
        if (name == null || !name.startsWith(PREFIX)) return null;
        return byId(name.substring(PREFIX.length()));
    }
}
