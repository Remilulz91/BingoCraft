package fr.bingocraft.bingo;

import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * A square bingo card: size x size goals, each one an item to obtain, stored row by row.
 * Goals are saved as item ids, so a card survives the removal of a mod: the goal is then
 * unreachable ({@link #item(int)} returns null) instead of corrupting the save.
 */
public final class BingoCard {

    private final int size;
    private final Identifier[] goals;
    private final Item[] items;
    private final boolean[] done;

    public BingoCard(int size, List<Identifier> goals) {
        if (goals.size() != size * size) {
            throw new IllegalArgumentException("A " + size + "x" + size + " card needs " + size * size + " goals");
        }
        this.size = size;
        this.goals = goals.toArray(new Identifier[0]);
        this.items = new Item[this.goals.length];
        this.done = new boolean[this.goals.length];
        for (int i = 0; i < this.goals.length; i++) {
            items[i] = Registries.ITEM.containsId(this.goals[i]) ? Registries.ITEM.get(this.goals[i]) : null;
        }
    }

    public int size() {
        return size;
    }

    public int cellCount() {
        return goals.length;
    }

    public Identifier goal(int index) {
        return goals[index];
    }

    /** The item of this cell, or null if its mod is no longer installed. */
    public Item item(int index) {
        return items[index];
    }

    /** Display name of a goal; the raw id, struck through, if its mod is gone. */
    public Text goalName(int index) {
        Item item = items[index];
        return item != null
                ? Text.translatable(item.getTranslationKey())
                : Text.literal(goals[index].toString()).formatted(Formatting.STRIKETHROUGH);
    }

    public boolean isDone(int index) {
        return done[index];
    }

    /**
     * Marks a cell as done. Returns false if it already was. Callers save the change through
     * {@link BingoState} so the world data is marked dirty.
     */
    public boolean markDone(int index) {
        if (done[index]) return false;
        done[index] = true;
        return true;
    }

    public int completedCount() {
        int n = 0;
        for (boolean d : done) if (d) n++;
        return n;
    }

    public boolean isComplete() {
        return completedCount() == done.length;
    }

    /** True if a whole row or a whole column is done (diagonals don't count). */
    public boolean hasRowOrColumn() {
        for (int i = 0; i < size; i++) {
            boolean row = true;
            boolean col = true;
            for (int j = 0; j < size; j++) {
                row &= done[i * size + j];
                col &= done[j * size + i];
            }
            if (row || col) return true;
        }
        return false;
    }

    /** Same goals, nothing done: every team of a race plays its own copy of one card. */
    public BingoCard blankCopy() {
        return new BingoCard(size, List.of(goals));
    }

    // === NBT ===

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("size", size);
        NbtList list = new NbtList();
        for (Identifier goal : goals) list.add(NbtString.of(goal.toString()));
        nbt.put("goals", list);
        byte[] bytes = new byte[done.length];
        for (int i = 0; i < done.length; i++) bytes[i] = (byte) (done[i] ? 1 : 0);
        nbt.putByteArray("done", bytes);
        return nbt;
    }

    /** Reads a card back; returns null if the data is unusable. */
    public static BingoCard fromNbt(NbtCompound nbt) {
        int size = nbt.getInt("size");
        NbtList list = nbt.getList("goals", NbtElement.STRING_TYPE);
        if (size <= 0 || list.size() != size * size) return null;
        Identifier[] ids = new Identifier[list.size()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = Identifier.tryParse(list.getString(i));
            if (ids[i] == null) return null;
        }
        BingoCard card = new BingoCard(size, List.of(ids));
        byte[] bytes = nbt.getByteArray("done");
        for (int i = 0; i < Math.min(bytes.length, card.done.length); i++) card.done[i] = bytes[i] != 0;
        return card;
    }
}
