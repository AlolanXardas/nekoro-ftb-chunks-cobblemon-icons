package dev.xardas.nekoroftbchunkscobblemonicons.client;

import dev.ftb.mods.ftblibrary.config.NameMap;
import net.minecraft.network.chat.Component;

/** Outline under a Pokemon icon, replacing the square FTB Chunks draws under mob icons. */
public enum OutlineStyle {
    NONE("none"),
    /** Silhouette shifted right and down. */
    SHADOW("shadow"),
    /** A ring around the whole shape. */
    FULL("full");

    public static final NameMap<OutlineStyle> NAME_MAP = NameMap.of(FULL, values())
            .id(style -> style.id)
            .name(style -> Component.translatable("nekoroftbchunkscobblemonicons.outline_type." + style.id))
            .create();

    public final String id;

    OutlineStyle(String id) {
        this.id = id;
    }
}
