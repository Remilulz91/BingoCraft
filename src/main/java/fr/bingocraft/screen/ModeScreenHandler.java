package fr.bingocraft.screen;

import fr.bingocraft.bingo.BingoMode;
import fr.bingocraft.bingo.BingoState;
import fr.bingocraft.bingo.Modes;
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
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Unit;

import java.util.List;

/**
 * Game mode picker ({@code /bingo mode}, OP only): endless (clock), race (firework rocket) or
 * off (barrier). The current mode glows.
 */
public class ModeScreenHandler extends GenericContainerScreenHandler {

    private static final int ROWS = 3;
    private static final int ENDLESS_SLOT = 11;
    private static final int RACE_SLOT = 15;
    private static final int OFF_SLOT = 22;

    private final SimpleInventory display;
    private final ServerPlayerEntity viewer;

    private ModeScreenHandler(int syncId, PlayerInventory playerInventory, SimpleInventory display,
                              ServerPlayerEntity viewer) {
        super(ScreenHandlerType.GENERIC_9X3, syncId, playerInventory, display, ROWS);
        this.display = display;
        this.viewer = viewer;
    }

    public static void open(ServerPlayerEntity player) {
        SimpleInventory display = new SimpleInventory(ROWS * 9);
        fill(display, BingoState.get(player.server).mode());
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, p) -> new ModeScreenHandler(syncId, playerInventory, display, player),
                Text.translatable("bingocraft.mode.title")));
    }

    public static void refresh(ServerPlayerEntity player) {
        if (player.currentScreenHandler instanceof ModeScreenHandler handler) {
            fill(handler.display, BingoState.get(player.server).mode());
            handler.sendContentUpdates();
        }
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        syncState(); // read-only: nothing moves
        if (actionType != SlotActionType.PICKUP && actionType != SlotActionType.QUICK_MOVE) return;
        BingoMode mode = switch (slotIndex) {
            case ENDLESS_SLOT -> BingoMode.ENDLESS;
            case RACE_SLOT -> BingoMode.RACE;
            case OFF_SLOT -> BingoMode.NONE;
            default -> null;
        };
        if (mode == null || mode == BingoState.get(viewer.server).mode()) return;
        Text error = Modes.set(viewer.server, mode);
        if (error != null) {
            viewer.sendMessage(error.copy().formatted(Formatting.RED), false);
            viewer.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 1.0f, 1.0f);
            return;
        }
        viewer.playSoundToPlayer(SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.MASTER, 0.6f, 1.0f);
        viewer.closeHandledScreen();
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    private static void fill(SimpleInventory display, BingoMode current) {
        ItemStack background = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        background.set(DataComponentTypes.HIDE_TOOLTIP, Unit.INSTANCE);
        for (int slot = 0; slot < display.size(); slot++) display.setStack(slot, background.copy());
        display.setStack(ENDLESS_SLOT, option(Items.CLOCK, BingoMode.ENDLESS, Formatting.AQUA, current));
        display.setStack(RACE_SLOT, option(Items.FIREWORK_ROCKET, BingoMode.RACE, Formatting.GOLD, current));
        display.setStack(OFF_SLOT, option(Items.BARRIER, BingoMode.NONE, Formatting.RED, current));
    }

    private static ItemStack option(Item item, BingoMode mode, Formatting color, BingoMode current) {
        ItemStack stack = new ItemStack(item);
        boolean selected = mode == current;
        stack.set(DataComponentTypes.CUSTOM_NAME, plain(Modes.name(mode).copy().formatted(color, Formatting.BOLD)));
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                plain(Text.translatable("bingocraft.mode.desc." + mode.id()).formatted(Formatting.GRAY)),
                Text.empty(),
                plain(Text.translatable(selected ? "bingocraft.mode.lore.current" : "bingocraft.mode.lore.pick")
                        .formatted(selected ? Formatting.GREEN : Formatting.YELLOW)))));
        stack.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
        if (selected) stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    private static Text plain(MutableText text) {
        return text.styled(style -> style.withItalic(false));
    }
}
