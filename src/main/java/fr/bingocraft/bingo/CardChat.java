package fr.bingocraft.bingo;

import net.minecraft.item.Item;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders a card in chat: one line per row, one square per cell (green = done), the item name
 * shown when hovering a square. Stand-in until the card screen and HUD exist.
 */
public final class CardChat {

    private CardChat() { }

    public static Text goalName(BingoCard card, int index) {
        Item item = card.item(index);
        return item != null
                ? Text.translatable(item.getTranslationKey())
                : Text.literal(card.goal(index).toString()).formatted(Formatting.STRIKETHROUGH);
    }

    public static List<Text> render(BingoCard card) {
        List<Text> lines = new ArrayList<>();
        lines.add(Text.translatable("bingocraft.card.header", card.completedCount(), card.cellCount())
                .formatted(Formatting.GOLD));
        for (int row = 0; row < card.size(); row++) {
            MutableText line = Text.literal(" ");
            for (int col = 0; col < card.size(); col++) {
                int index = row * card.size() + col;
                boolean done = card.isDone(index);
                Text hover = Text.literal("#" + (index + 1) + " ").formatted(Formatting.GRAY)
                        .append(goalName(card, index).copy().formatted(done ? Formatting.GREEN : Formatting.WHITE));
                line.append(Text.literal("■ ").styled(style -> style
                        .withColor(done ? Formatting.GREEN : Formatting.DARK_GRAY)
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))));
            }
            lines.add(line);
        }
        lines.add(Text.translatable("bingocraft.card.hover_hint").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
        return lines;
    }
}
