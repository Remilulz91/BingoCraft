package fr.bingocraft.screen;

import fr.bingocraft.bingo.BingoCard;
import fr.bingocraft.bingo.BingoMode;
import fr.bingocraft.bingo.BingoState;
import fr.bingocraft.race.Race;
import fr.bingocraft.race.RaceManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Unit;

import java.util.ArrayList;
import java.util.List;

/**
 * The card shown as a double chest (9x6). Built entirely server-side on the vanilla chest
 * screen, so it needs nothing from the client. Read-only: every click is refused and the
 * client is resynced, so nothing can be taken out.
 *
 * Layout: the card is centred in the five top rows (a 5x5 fills columns 3-7), the bottom row
 * holds the progress item. Done goals glow and say so in their tooltip.
 */
public class CardScreenHandler extends GenericContainerScreenHandler {

    private static final int ROWS = 6;
    private static final int COLUMNS = 9;
    private static final int CARD_ROWS = 5;
    private static final int PROGRESS_SLOT = 5 * COLUMNS + 4;

    private final SimpleInventory display;

    private CardScreenHandler(int syncId, PlayerInventory playerInventory, SimpleInventory display) {
        super(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, display, ROWS);
        this.display = display;
    }

    /** What the player should see: their race card during a race, their own card otherwise. */
    private record View(BingoCard card, List<Text> info) { }

    private static View view(ServerPlayerEntity player, BingoState state) {
        if (state.mode() == BingoMode.NONE) return null;
        Race race = state.race();
        if (state.mode() == BingoMode.RACE) {
            if (race == null) return null;
            if (!race.isRunning()) return null;
            BingoCard card = RaceManager.cardOf(player, state);
            if (card == null) return null; // not racing
            List<Text> info = new ArrayList<>();
            info.add(Text.translatable("bingocraft.screen.race.goal",
                    Text.translatable("bingocraft.race.goal")).formatted(Formatting.YELLOW));
            info.add(Text.translatable("bingocraft.screen.race.side",
                    RaceManager.teamName(race.teamOf(player.getUuid()))).formatted(Formatting.YELLOW));
            info.add(Text.translatable("bingocraft.screen.race.time",
                    RaceManager.formatTime(player.server.getOverworld().getTime() - race.startTime()))
                    .formatted(Formatting.YELLOW));
            return new View(card, info);
        }
        BingoCard card = state.get(player.getUuid());
        if (card == null) return null;
        return new View(card, List.of(Text.translatable("bingocraft.screen.completed_cards",
                state.completedCards(player.getUuid())).formatted(Formatting.YELLOW)));
    }

    /** Opens the card the player should see. Returns false if there is none (yet). */
    public static boolean open(ServerPlayerEntity player, BingoState state) {
        View view = view(player, state);
        if (view == null) return false;
        SimpleInventory display = new SimpleInventory(ROWS * COLUMNS);
        fill(display, view);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, p) -> new CardScreenHandler(syncId, playerInventory, display),
                Text.translatable("bingocraft.screen.title")));
        return true;
    }

    /**
     * Redraws the card if the player is looking at it: called when a goal is ticked and when a
     * new card replaces a completed one.
     */
    public static void refresh(ServerPlayerEntity player, BingoState state) {
        if (!(player.currentScreenHandler instanceof CardScreenHandler handler)) return;
        View view = view(player, state);
        if (view == null) {
            player.closeHandledScreen();
            return;
        }
        fill(handler.display, view);
        handler.sendContentUpdates();
    }

    // === Read-only ===

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        // Nothing moves. The client already predicted the click: resend the real contents.
        syncState();
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    // === Drawing ===

    private static void fill(SimpleInventory display, View view) {
        BingoCard card = view.card();
        ItemStack background = new ItemStack(Items.BLACK_STAINED_GLASS_PANE);
        background.set(DataComponentTypes.HIDE_TOOLTIP, Unit.INSTANCE);
        for (int slot = 0; slot < display.size(); slot++) display.setStack(slot, background.copy());

        int size = card.size();
        int rowOffset = (CARD_ROWS - size) / 2;
        int colOffset = (COLUMNS - size) / 2;
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                int index = row * size + col;
                display.setStack((row + rowOffset) * COLUMNS + col + colOffset, goalStack(card, index));
            }
        }
        display.setStack(PROGRESS_SLOT, progressStack(card, view.info()));
    }

    private static ItemStack goalStack(BingoCard card, int index) {
        Item item = card.item(index);
        boolean done = card.isDone(index);

        ItemStack stack = new ItemStack(item != null ? item : Items.BARRIER);
        stack.set(DataComponentTypes.CUSTOM_NAME, plain(card.goalName(index).copy()
                .formatted(done ? Formatting.GREEN : Formatting.WHITE)));
        Text status = item == null
                ? Text.translatable("bingocraft.screen.goal.missing_mod").formatted(Formatting.RED)
                : done
                        ? Text.translatable("bingocraft.screen.goal.done").formatted(Formatting.GREEN)
                        : Text.translatable("bingocraft.screen.goal.todo").formatted(Formatting.GRAY);
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                plain(status.copy()),
                Text.literal(modName(card.goal(index).getNamespace())).styled(style -> style
                        .withColor(Formatting.BLUE).withItalic(true)))));
        stack.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        if (done) stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    private static ItemStack progressStack(BingoCard card, List<Text> info) {
        boolean complete = card.isComplete();
        ItemStack stack = new ItemStack(complete ? Items.NETHER_STAR : Items.WRITABLE_BOOK);
        stack.set(DataComponentTypes.CUSTOM_NAME, plain(Text.translatable("bingocraft.screen.progress",
                card.completedCount(), card.cellCount()).formatted(Formatting.GOLD)));
        List<Text> lore = new ArrayList<>();
        lore.add(plain(Text.translatable(complete ? "bingocraft.screen.progress.complete" : "bingocraft.screen.progress.hint")
                .formatted(Formatting.GRAY)));
        for (Text line : info) lore.add(plain(line.copy()));
        stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
        return stack;
    }

    /** Name of the mod an item comes from ("Minecraft" for vanilla), as Mod Menu shows it. */
    private static String modName(String namespace) {
        if ("minecraft".equals(namespace)) return "Minecraft";
        return FabricLoader.getInstance().getModContainer(namespace)
                .map(mod -> mod.getMetadata().getName())
                .orElse(namespace);
    }

    /** Item names and lore are italic by default; card text is not. */
    private static Text plain(net.minecraft.text.MutableText text) {
        return text.styled(style -> style.withItalic(false));
    }
}
