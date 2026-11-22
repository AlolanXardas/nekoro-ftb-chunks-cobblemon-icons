package dev.xardas.nekoroftbchunkscobblemonicons.client;

import dev.ftb.mods.ftblibrary.config.NameMap;
import net.minecraft.network.chat.Component;

/** Which caught badges (Caught indicator) to show on wild Pokemon. */
public enum PokedexBadge {
    UNCAUGHT("uncaught", true, false),
    CAUGHT("caught", false, true),
    BOTH("both", true, true),
    OFF("off", false, false);

    public static final NameMap<PokedexBadge> NAME_MAP = NameMap.of(BOTH, values())
            .id(badge -> badge.id)
            .name(badge -> Component.translatable("nekoroftbchunkscobblemonicons.caught_indicator." + badge.id))
            .create();

    public final String id;
    public final boolean showUncaught;
    public final boolean showCaught;

    PokedexBadge(String id, boolean showUncaught, boolean showCaught) {
        this.id = id;
        this.showUncaught = showUncaught;
        this.showCaught = showCaught;
    }
}
