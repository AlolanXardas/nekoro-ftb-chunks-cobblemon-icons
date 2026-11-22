package dev.xardas.nekoroftbchunkscobblemonicons.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;

/**
 * Color filter for a Cobblemon variant that has no sprite of its own (e.g. Torterra's trees,
 * dyed Wooloo wool, Vivillon wings added by Cobblemon).
 *
 * Two modes, from sprite_tints.json:
 * <ul>
 *     <li>hues: pixels with a hue in the range get the target color's hue and saturation, and
 *     their brightness scaled to its brightness, so the sprite's shading stays,</li>
 *     <li>whites: white and light gray pixels are multiplied by the color (like dyeing wool).</li>
 * </ul>
 * The optional top limits the filter to the upper part of the crop: Torterra's tree and body
 * are both green, so without it the whole turtle would be recolored.
 *
 * Computed once per sprite, on first draw (see {@link MapSpriteIcon}).
 */
public record SpriteTint(int red, int green, int blue, float hueMin, float hueMax, boolean whites, float top) {

    /** Typical brightness of recolored pixels; a target color with this brightness keeps brightness unchanged. */
    private static final float REFERENCE_VALUE = 0.7F;
    private static final float MIN_SATURATION = 0.12F;
    private static final float WHITE_MAX_SATURATION = 0.22F;
    private static final float WHITE_MIN_VALUE = 0.5F;
    /** How strongly a pixel's saturation moves toward the target color's saturation. */
    private static final float SATURATION_PULL = 0.8F;

    public static SpriteTint fromJson(JsonObject json) {
        int rgb = Integer.parseInt(json.get("color").getAsString().substring(1), 16);
        float hueMin = 0F;
        float hueMax = 360F;
        if (json.has("hues")) {
            JsonArray hues = json.getAsJsonArray("hues");
            hueMin = hues.get(0).getAsFloat();
            hueMax = hues.get(1).getAsFloat();
        }
        boolean whites = json.has("whites") && json.get("whites").getAsBoolean();
        float top = json.has("top") ? json.get("top").getAsFloat() : 1F;
        return new SpriteTint((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, hueMin, hueMax, whites, top);
    }

    /** @return a new image of the same size as the sprite; the caller closes both */
    public NativeImage apply(NativeImage sprite, SpriteBounds bounds) {
        NativeImage out = new NativeImage(sprite.getWidth(), sprite.getHeight(), false);
        out.copyFrom(sprite);
        float[] target = toHsv(red, green, blue);
        int limitY = bounds.y() + Math.round(bounds.height() * top);
        for (int y = bounds.y(); y < Math.min(limitY, sprite.getHeight()); y++) {
            for (int x = bounds.x(); x < bounds.x() + bounds.width(); x++) {
                int abgr = sprite.getPixelRGBA(x, y);
                if (!SpriteBounds.isVisible(abgr)) {
                    continue;
                }
                int recolored = whites ? tintWhite(abgr) : shiftHue(abgr, target);
                if (recolored != abgr) {
                    out.setPixelRGBA(x, y, recolored);
                }
            }
        }
        return out;
    }

    private int tintWhite(int abgr) {
        int r = abgr & 0xFF;
        int g = (abgr >> 8) & 0xFF;
        int b = (abgr >> 16) & 0xFF;
        float[] hsv = toHsv(r, g, b);
        if (hsv[1] >= WHITE_MAX_SATURATION || hsv[2] <= WHITE_MIN_VALUE) {
            return abgr;
        }
        return abgr(abgr >>> 24, r * red / 255, g * green / 255, b * blue / 255);
    }

    private int shiftHue(int abgr, float[] target) {
        int r = abgr & 0xFF;
        int g = (abgr >> 8) & 0xFF;
        int b = (abgr >> 16) & 0xFF;
        float[] hsv = toHsv(r, g, b);
        if (hsv[1] < MIN_SATURATION || hsv[0] < hueMin || hsv[0] > hueMax) {
            return abgr;
        }
        float saturation = hsv[1] + (target[1] - hsv[1]) * SATURATION_PULL;
        float value = Math.min(1F, hsv[2] * target[2] / REFERENCE_VALUE);
        int[] rgb = fromHsv(target[0], saturation, value);
        return abgr(abgr >>> 24, rgb[0], rgb[1], rgb[2]);
    }

    private static int abgr(int a, int r, int g, int b) {
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    /** @return {hue 0..360, saturation 0..1, brightness 0..1} */
    private static float[] toHsv(int r8, int g8, int b8) {
        float r = r8 / 255F;
        float g = g8 / 255F;
        float b = b8 / 255F;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float hue = 0F;
        if (delta > 0F) {
            if (max == r) {
                hue = 60F * (((g - b) / delta) % 6F);
            } else if (max == g) {
                hue = 60F * (((b - r) / delta) + 2F);
            } else {
                hue = 60F * (((r - g) / delta) + 4F);
            }
            if (hue < 0F) {
                hue += 360F;
            }
        }
        return new float[]{hue, max > 0F ? delta / max : 0F, max};
    }

    private static int[] fromHsv(float hue, float saturation, float value) {
        float c = value * saturation;
        float x = c * (1F - Math.abs((hue / 60F) % 2F - 1F));
        float m = value - c;
        float r;
        float g;
        float b;
        switch ((int) Math.floor(hue / 60F) % 6) {
            case 0 -> { r = c; g = x; b = 0F; }
            case 1 -> { r = x; g = c; b = 0F; }
            case 2 -> { r = 0F; g = c; b = x; }
            case 3 -> { r = 0F; g = x; b = c; }
            case 4 -> { r = x; g = 0F; b = c; }
            default -> { r = c; g = 0F; b = x; }
        }
        return new int[]{Math.round((r + m) * 255F), Math.round((g + m) * 255F), Math.round((b + m) * 255F)};
    }
}
