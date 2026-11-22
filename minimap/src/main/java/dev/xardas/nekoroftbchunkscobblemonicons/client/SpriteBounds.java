package dev.xardas.nekoroftbchunkscobblemonicons.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FastColor;
import org.slf4j.Logger;

import java.io.InputStream;

/**
 * Rectangle of the visible (non-transparent) part of a sprite, in canvas pixels.
 *
 * Pokemon sprites have a 96x96 or 128x128 canvas, but the Pokemon fills it very differently:
 * measured on a sample of 300 files, from 27% to 100% of the canvas side, median 64%.
 * The margins are not constant (left 0..43 px, top 0..47 px), so no single fixed UV crop works
 * for all sprites. That is why the bounding box is computed per sprite.
 *
 * Cost: one PNG decode per icon, lazily on first use, with the result kept in
 * {@link PokemonIconCache}. The scan is one bulk pixel copy and a pass over ~9-16 thousand
 * alpha values, so it takes microseconds; the real cost is loading the PNG from the jar,
 * which also happens only once.
 *
 * @param measured false when the sprite could not be measured; then the whole canvas is drawn
 *                 and no outline is generated
 */
public record SpriteBounds(int x, int y, int width, int height, int imageWidth, int imageHeight, boolean measured) {

    /** The whole canvas, as a fallback when the sprite cannot be measured. */
    public static final SpriteBounds FULL = new SpriteBounds(0, 0, 1, 1, 1, 1, false);

    /** A pixel counts as visible when it is not fully transparent. */
    private static final int MIN_ALPHA = 1;
    private static final Logger LOGGER = LogUtils.getLogger();

    public static SpriteBounds measure(ResourceLocation location, Resource resource) {
        try (InputStream in = resource.open(); NativeImage image = NativeImage.read(in)) {
            return measure(image);
        } catch (Exception e) {
            LOGGER.warn("Could not measure sprite {}, drawing the whole canvas", location, e);
            return FULL;
        }
    }

    /** @param abgr pixel in NativeImage format (ABGR32, alpha in the highest byte) */
    static boolean isVisible(int abgr) {
        return FastColor.ABGR32.alpha(abgr) >= MIN_ALPHA;
    }

    private static SpriteBounds measure(NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        // NativeImage.read(InputStream) always gives RGBA format, so getPixelsRGBA() is allowed.
        int[] pixels = image.getPixelsRGBA();

        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < height; y++) {
            int row = y * width;
            for (int x = 0; x < width; x++) {
                if (!isVisible(pixels[row + x])) {
                    continue;
                }
                if (x < minX) {
                    minX = x;
                }
                if (x > maxX) {
                    maxX = x;
                }
                if (y < minY) {
                    minY = y;
                }
                if (y > maxY) {
                    maxY = y;
                }
            }
        }

        if (maxX < 0) {
            // the sprite is fully transparent, nothing to crop
            return FULL;
        }

        return new SpriteBounds(minX, minY, maxX - minX + 1, maxY - minY + 1, width, height, true);
    }

    public float u0() {
        return (float) x / imageWidth;
    }

    public float v0() {
        return (float) y / imageHeight;
    }

    public float u1() {
        return (float) (x + width) / imageWidth;
    }

    public float v1() {
        return (float) (y + height) / imageHeight;
    }

    public float aspectRatio() {
        return (float) width / height;
    }
}
