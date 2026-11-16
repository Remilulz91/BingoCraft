package fr.bingocraft.client.config;

import fr.bingocraft.config.BingoCraftConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Cloth Config screen for BingoCraft. Only referenced when Cloth Config is loaded.
 */
public final class BingoCraftConfigScreen {

    private BingoCraftConfigScreen() { }

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("config.bingocraft.title"))
                .setSavingRunnable(BingoCraftConfig::save);

        BingoCraftConfig cfg = BingoCraftConfig.get();
        ConfigEntryBuilder entry = builder.entryBuilder();

        // === Cards category ===
        ConfigCategory cards = builder.getOrCreateCategory(Text.translatable("config.bingocraft.category.cards"));
        cards.addEntry(entry.startIntSlider(Text.translatable("config.bingocraft.cardSize"), cfg.cardSize, 3, 5)
                .setDefaultValue(5)
                .setTooltip(Text.translatable("config.bingocraft.cardSize.tooltip"))
                .setSaveConsumer(v -> cfg.cardSize = v).build());
        cards.addEntry(entry.startIntSlider(Text.translatable("config.bingocraft.easyPercent"), cfg.easyPercent, 0, 100)
                .setDefaultValue(40)
                .setTooltip(Text.translatable("config.bingocraft.mix.tooltip"))
                .setSaveConsumer(v -> cfg.easyPercent = v).build());
        cards.addEntry(entry.startIntSlider(Text.translatable("config.bingocraft.mediumPercent"), cfg.mediumPercent, 0, 100)
                .setDefaultValue(40)
                .setTooltip(Text.translatable("config.bingocraft.mix.tooltip"))
                .setSaveConsumer(v -> cfg.mediumPercent = v).build());
        cards.addEntry(entry.startIntSlider(Text.translatable("config.bingocraft.hardPercent"), cfg.hardPercent, 0, 100)
                .setDefaultValue(20)
                .setTooltip(Text.translatable("config.bingocraft.mix.tooltip"))
                .setSaveConsumer(v -> cfg.hardPercent = v).build());
        cards.addEntry(entry.startIntField(Text.translatable("config.bingocraft.checkIntervalTicks"), cfg.checkIntervalTicks)
                .setMin(1).setMax(200).setDefaultValue(10)
                .setTooltip(Text.translatable("config.bingocraft.checkIntervalTicks.tooltip"))
                .setSaveConsumer(v -> cfg.checkIntervalTicks = v).build());

        return builder.build();
    }
}
