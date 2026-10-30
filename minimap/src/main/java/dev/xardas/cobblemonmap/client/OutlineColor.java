package dev.xardas.cobblemonmap.client;

import dev.ftb.mods.ftblibrary.config.NameMap;
import net.minecraft.network.chat.Component;

/**
 * Outline color: the main one (Main) and the ones for highlighted Pokemon (shiny, legendary, own, ...).
 *
 * Choosing a color costs nothing when drawing; it is only the vertex color of the white outline
 * texture. OFF means different things depending on the option: in Main "no outline", in categories
 * "this category is not highlighted", for own/other players' Pokemon "same as wild" (see {@link IconAppearance}).
 */
public enum OutlineColor {
    OFF("off", -1),
    BLACK("black", 0x000000),
    WHITE("white", 0xFFFFFF),
    GRAY("gray", 0x8A8A8A),
    RED("red", 0xE02020),
    ORANGE("orange", 0xFF8A1E),
    GOLD("gold", 0xFFC31E),
    YELLOW("yellow", 0xFFF23D),
    GREEN("green", 0x2FC23A),
    LIGHT_BLUE("light_blue", 0x5CD2FF),
    DARK_BLUE("dark_blue", 0x1F3FBF),
    PURPLE("purple", 0x6A1BC2),
    PINK("pink", 0xFF5CC8);

    public final String id;
    /** RGB, or -1 for OFF */
    public final int rgb;

    OutlineColor(String id, int rgb) {
        this.id = id;
        this.rgb = rgb;
    }

    public static NameMap<OutlineColor> nameMap(OutlineColor defaultValue) {
        return NameMap.of(defaultValue, values())
                .id(color -> color.id)
                .name(color -> Component.translatable("cobblemonmap.color." + color.id))
                .create();
    }
}
