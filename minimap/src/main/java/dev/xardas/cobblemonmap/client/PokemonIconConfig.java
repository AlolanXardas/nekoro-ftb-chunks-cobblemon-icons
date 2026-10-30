package dev.xardas.cobblemonmap.client;

import dev.ftb.mods.ftbchunks.api.client.icon.MapType;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.ConfigValue;
import dev.ftb.mods.ftblibrary.snbt.config.BaseValue;
import dev.ftb.mods.ftblibrary.snbt.config.EnumValue;
import dev.ftb.mods.ftblibrary.snbt.config.SNBTConfig;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Look of Pokemon icons, editable in game.
 *
 * There is no config file or screen of our own: the group is attached to the FTB Chunks client
 * config at the moment FTB Chunks registers it in FTB Library's ConfigManager (ConfigManagerMixin).
 * Registering only queues the config to be loaded later, so the group exists before
 * ftbchunks-client.snbt is read; it shows up in the FTB Chunks settings screen (settings button on
 * the large map) and is saved together with the rest.
 *
 * The size multiplier is applied on top of what FTB Chunks computes: scale from
 * faces/cobblemon/pokemon.json and the 1.5x reduction on the minimap with "Large entities" off.
 *
 * Until the group is attached (e.g. if FTB Chunks renamed its config), every option returns
 * its default value instead of crashing the game.
 */
public class PokemonIconConfig {

    private static final String FTB_CHUNKS_CLIENT_CONFIG = "ftbchunks-client";

    private static boolean registered = false;

    // Icons
    private static final Option<Double> MINIMAP_SCALE = new Option<>(1D);
    private static final Option<Double> LARGE_MAP_SCALE = new Option<>(1D);
    private static final Option<Integer> MINIMAP_OPACITY = new Option<>(100);
    private static final Option<Integer> LARGE_MAP_OPACITY = new Option<>(100);
    private static final Option<Boolean> SCALE_BY_SIZE = new Option<>(true);
    private static final Option<Boolean> DIM_IN_BATTLE = new Option<>(true);
    // Outline
    private static final Option<OutlineStyle> OUTLINE_TYPE = new Option<>(OutlineStyle.FULL);
    private static final Option<Integer> MINIMAP_OUTLINE_OPACITY = new Option<>(50);
    private static final Option<Integer> LARGE_MAP_OUTLINE_OPACITY = new Option<>(50);
    private static final Option<OutlineColor> MAIN_COLOR = new Option<>(OutlineColor.BLACK);
    private static final Option<OutlineColor> SHINY_COLOR = new Option<>(OutlineColor.GOLD);
    private static final Option<OutlineColor> MYTHICAL_COLOR = new Option<>(OutlineColor.PURPLE);
    private static final Option<OutlineColor> LEGENDARY_COLOR = new Option<>(OutlineColor.RED);
    private static final Option<OutlineColor> ULTRA_BEAST_COLOR = new Option<>(OutlineColor.DARK_BLUE);
    private static final Option<OutlineColor> PARADOX_COLOR = new Option<>(OutlineColor.WHITE);
    private static final Option<OutlineColor> OWN_COLOR = new Option<>(OutlineColor.OFF);
    private static final Option<OutlineColor> OTHER_PLAYERS_COLOR = new Option<>(OutlineColor.OFF);
    private static final Option<RarePulse> RARE_PULSE = new Option<>(RarePulse.DEFAULT);
    // Visibility
    private static final Option<Boolean> SHOW_CAUGHT = new Option<>(true);
    private static final Option<Boolean> SHOW_MINE = new Option<>(true);
    private static final Option<Boolean> SHOW_OTHER_PLAYERS = new Option<>(true);
    private static final Option<Boolean> ALWAYS_SHOW_RARE = new Option<>(true);
    private static final Option<Boolean> SHOW_ONLY_RARE = new Option<>(false);
    private static final Option<List<String>> ALWAYS_SHOWN = new Option<>(List.of());
    private static final Option<List<String>> ALWAYS_HIDDEN = new Option<>(List.of());
    private static final Option<PokedexBadge> CAUGHT_INDICATOR = new Option<>(PokedexBadge.BOTH);
    // Tooltip
    private static final Option<Boolean> TOOLTIP_LEVEL = new Option<>(true);
    private static final Option<Boolean> TOOLTIP_GENDER = new Option<>(true);
    private static final Option<Boolean> TOOLTIP_SHINY = new Option<>(true);
    private static final Option<Boolean> TOOLTIP_VARIANT = new Option<>(true);

    public static void onClientConfigRegistered(SNBTConfig config) {
        if (!FTB_CHUNKS_CLIENT_CONFIG.equals(config.key) || registered) {
            return;
        }
        registered = true;
        SNBTConfig root = config.addGroup("cobblemon_icons", 100);

        // Descriptions (tooltips) are in lang/en_us.json; comments go into ftbchunks-client.snbt.
        SNBTConfig icons = root.addGroup("icons", 0);
        MINIMAP_SCALE.bind(icons.addDouble("minimap_scale", MINIMAP_SCALE.fallback, 0.1D, 5D)
                .comment("Size multiplier for Pokemon icons on the minimap"));
        LARGE_MAP_SCALE.bind(icons.addDouble("large_map_scale", LARGE_MAP_SCALE.fallback, 0.1D, 5D)
                .comment("Size multiplier for Pokemon icons on the large map"));
        // Percent as an integer, because typing 50 into FTB Library fields is easier than 0.5.
        MINIMAP_OPACITY.bind(icons.addInt("minimap_opacity", MINIMAP_OPACITY.fallback, 0, 100)
                .comment("Icon opacity on the minimap, in percent"));
        LARGE_MAP_OPACITY.bind(icons.addInt("large_map_opacity", LARGE_MAP_OPACITY.fallback, 0, 100)
                .comment("Icon opacity on the large map, in percent"));
        SCALE_BY_SIZE.bind(icons.addBoolean("scale_by_species_size", SCALE_BY_SIZE.fallback)
                .comment("Bigger Pokemon get bigger icons"));
        DIM_IN_BATTLE.bind(icons.addBoolean("dim_in_battle", DIM_IN_BATTLE.fallback)
                .comment("Draw Pokemon in battle darker and half transparent"));

        SNBTConfig outline = root.addGroup("outline", 1);
        OUTLINE_TYPE.bind(outline.addEnum("type", OutlineStyle.NAME_MAP, OUTLINE_TYPE.fallback)
                .comment("none, shadow or full"));
        MINIMAP_OUTLINE_OPACITY.bind(outline.addInt("minimap_opacity", MINIMAP_OUTLINE_OPACITY.fallback, 0, 100)
                .comment("Outline opacity on the minimap, in percent"));
        LARGE_MAP_OUTLINE_OPACITY.bind(outline.addInt("large_map_opacity", LARGE_MAP_OUTLINE_OPACITY.fallback, 0, 100)
                .comment("Outline opacity on the large map, in percent"));
        MAIN_COLOR.bind(colorOption(outline, "main", MAIN_COLOR));
        SHINY_COLOR.bind(colorOption(outline, "shiny", SHINY_COLOR));
        MYTHICAL_COLOR.bind(colorOption(outline, "mythical", MYTHICAL_COLOR));
        LEGENDARY_COLOR.bind(colorOption(outline, "legendary", LEGENDARY_COLOR));
        ULTRA_BEAST_COLOR.bind(colorOption(outline, "ultra_beast", ULTRA_BEAST_COLOR));
        PARADOX_COLOR.bind(colorOption(outline, "paradox", PARADOX_COLOR));
        OWN_COLOR.bind(colorOption(outline, "own", OWN_COLOR));
        OTHER_PLAYERS_COLOR.bind(colorOption(outline, "other_players", OTHER_PLAYERS_COLOR));
        RARE_PULSE.bind(outline.addEnum("rare_pulse", RarePulse.NAME_MAP, RARE_PULSE.fallback));

        SNBTConfig visibility = root.addGroup("visibility", 2);
        SHOW_CAUGHT.bind(visibility.addBoolean("show_caught", SHOW_CAUGHT.fallback));
        SHOW_MINE.bind(visibility.addBoolean("show_mine", SHOW_MINE.fallback));
        SHOW_OTHER_PLAYERS.bind(visibility.addBoolean("show_other_players", SHOW_OTHER_PLAYERS.fallback));
        ALWAYS_SHOW_RARE.bind(visibility.addBoolean("always_show_rare", ALWAYS_SHOW_RARE.fallback));
        SHOW_ONLY_RARE.bind(visibility.addBoolean("show_only_rare", SHOW_ONLY_RARE.fallback));
        ALWAYS_SHOWN.bind(visibility.addStringList("always_shown_species", ALWAYS_SHOWN.fallback)
                .comment("Species names, e.g. \"ditto\" or \"cobblemon:ditto\""));
        ALWAYS_HIDDEN.bind(visibility.addStringList("always_hidden_species", ALWAYS_HIDDEN.fallback)
                .comment("Species names, e.g. \"zubat\" or \"cobblemon:zubat\""));
        CAUGHT_INDICATOR.bind(visibility.addEnum("caught_indicator", PokedexBadge.NAME_MAP, CAUGHT_INDICATOR.fallback)
                .comment("uncaught, caught, both or off"));

        SNBTConfig tooltip = root.addGroup("tooltip", 3);
        TOOLTIP_LEVEL.bind(tooltip.addBoolean("level", TOOLTIP_LEVEL.fallback));
        TOOLTIP_GENDER.bind(tooltip.addBoolean("gender", TOOLTIP_GENDER.fallback));
        TOOLTIP_SHINY.bind(tooltip.addBoolean("shiny", TOOLTIP_SHINY.fallback));
        TOOLTIP_VARIANT.bind(tooltip.addBoolean("variant", TOOLTIP_VARIANT.fallback));
    }

    private static EnumValue<OutlineColor> colorOption(SNBTConfig group, String key, Option<OutlineColor> option) {
        return group.addEnum(key, OutlineColor.nameMap(option.fallback), option.fallback);
    }

    public static double scaleFor(MapType mapType) {
        return (mapType.isMinimap() ? MINIMAP_SCALE : LARGE_MAP_SCALE).get();
    }

    /** @return 0..255 */
    public static int alphaFor(MapType mapType) {
        return percentToAlpha((mapType.isMinimap() ? MINIMAP_OPACITY : LARGE_MAP_OPACITY).get());
    }

    public static boolean scaleBySpeciesSize() {
        return SCALE_BY_SIZE.get();
    }

    public static boolean dimInBattle() {
        return DIM_IN_BATTLE.get();
    }

    public static OutlineStyle outlineType() {
        return OUTLINE_TYPE.get();
    }

    /** Outline opacity, 0..255, for every color except the pulse of rare Pokemon. */
    public static int outlineAlpha(MapType mapType) {
        return percentToAlpha((mapType.isMinimap() ? MINIMAP_OUTLINE_OPACITY : LARGE_MAP_OUTLINE_OPACITY).get());
    }

    public static OutlineColor mainColor() {
        return MAIN_COLOR.get();
    }

    public static OutlineColor shinyColor() {
        return SHINY_COLOR.get();
    }

    public static OutlineColor mythicalColor() {
        return MYTHICAL_COLOR.get();
    }

    public static OutlineColor legendaryColor() {
        return LEGENDARY_COLOR.get();
    }

    public static OutlineColor ultraBeastColor() {
        return ULTRA_BEAST_COLOR.get();
    }

    public static OutlineColor paradoxColor() {
        return PARADOX_COLOR.get();
    }

    public static OutlineColor ownColor() {
        return OWN_COLOR.get();
    }

    public static OutlineColor otherPlayersColor() {
        return OTHER_PLAYERS_COLOR.get();
    }

    public static RarePulse rarePulse() {
        return RARE_PULSE.get();
    }

    public static boolean showCaught() {
        return SHOW_CAUGHT.get();
    }

    public static boolean showMine() {
        return SHOW_MINE.get();
    }

    public static boolean showOtherPlayers() {
        return SHOW_OTHER_PLAYERS.get();
    }

    public static boolean alwaysShowRare() {
        return ALWAYS_SHOW_RARE.get();
    }

    public static boolean showOnlyRare() {
        return SHOW_ONLY_RARE.get();
    }

    public static List<String> alwaysShownSpecies() {
        return ALWAYS_SHOWN.get();
    }

    public static List<String> alwaysHiddenSpecies() {
        return ALWAYS_HIDDEN.get();
    }

    public static PokedexBadge caughtIndicator() {
        return CAUGHT_INDICATOR.get();
    }

    public static boolean tooltipLevel() {
        return TOOLTIP_LEVEL.get();
    }

    public static boolean tooltipGender() {
        return TOOLTIP_GENDER.get();
    }

    public static boolean tooltipShiny() {
        return TOOLTIP_SHINY.get();
    }

    public static boolean tooltipVariant() {
        return TOOLTIP_VARIANT.get();
    }

    private static int percentToAlpha(int percent) {
        return Math.round(percent * 255F / 100F);
    }

    /**
     * Sets the order of our options in the finished settings screen group (SNBTConfigMixin),
     * in the order they are attached in onClientConfigRegistered.
     */
    public static void applyDisplayOrder(SNBTConfig config, ConfigGroup parentGroup) {
        ConfigGroup group = null;
        for (BaseValue<?> child : config.get()) {
            Integer order = DISPLAY_ORDER.get(child);
            if (order == null) {
                continue;
            }
            if (group == null) {
                group = parentGroup.getOrCreateSubgroup(config.key);
            }
            for (ConfigValue<?> value : group.getValues()) {
                if (value.id.equals(child.key)) {
                    value.setOrder(order);
                }
            }
        }
    }

    /** Display order of our options; filled in Option.bind. */
    private static final Map<BaseValue<?>, Integer> DISPLAY_ORDER = new IdentityHashMap<>();

    /** Value from the FTB Chunks config, or the default until the config is attached. */
    private static final class Option<T> {
        private final T fallback;
        private Supplier<T> value;

        private Option(T fallback) {
            this.fallback = fallback;
        }

        private void bind(BaseValue<T> configValue) {
            DISPLAY_ORDER.put(configValue, DISPLAY_ORDER.size());
            this.value = configValue::get;
        }

        private T get() {
            return value == null ? fallback : value.get();
        }
    }
}
