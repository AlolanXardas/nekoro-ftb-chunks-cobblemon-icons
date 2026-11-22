package dev.xardas.nekoroftbchunkscobblemonicons.client;

import dev.ftb.mods.ftblibrary.config.NameMap;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Pulsing outline of rare Pokemon: off, fading, or shifting to a chosen color.
 *
 * The animation is only a change of vertex color over time, with no extra drawing.
 */
public record RarePulse(String id, boolean fade, int rgb) {

    public static final RarePulse OFF = new RarePulse("off", false, -1);
    public static final RarePulse FADE = new RarePulse("fade", true, -1);
    public static final List<RarePulse> VALUES = createValues();
    public static final RarePulse DEFAULT = VALUES.stream()
            .filter(pulse -> pulse.id.equals(OutlineColor.BLACK.id))
            .findFirst()
            .orElse(OFF);
    public static final NameMap<RarePulse> NAME_MAP = NameMap.of(DEFAULT, VALUES)
            .id(RarePulse::id)
            .name(pulse -> Component.translatable(pulse.rgb >= 0 ? "nekoroftbchunkscobblemonicons.color." + pulse.id : "nekoroftbchunkscobblemonicons.pulse." + pulse.id))
            .create();

    /** Length of one pulse cycle in milliseconds. */
    private static final long PERIOD_MS = 1400L;

    private static List<RarePulse> createValues() {
        List<RarePulse> values = new ArrayList<>();
        values.add(OFF);
        values.add(FADE);
        for (OutlineColor color : OutlineColor.values()) {
            if (color != OutlineColor.OFF) {
                values.add(new RarePulse(color.id, false, color.rgb));
            }
        }
        return List.copyOf(values);
    }

    /** @return 0..1, smoothly back and forth */
    public static float phase() {
        double t = (System.currentTimeMillis() % PERIOD_MS) / (double) PERIOD_MS;
        return (float) (0.5 - 0.5 * Math.cos(t * 2 * Math.PI));
    }

    public static int lerpRgb(int from, int to, float t) {
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (r << 16) | (g << 8) | b;
    }
}
