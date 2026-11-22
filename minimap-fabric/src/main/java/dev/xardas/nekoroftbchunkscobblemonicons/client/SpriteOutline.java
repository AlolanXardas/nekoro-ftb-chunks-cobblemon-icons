package dev.xardas.nekoroftbchunkscobblemonicons.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.Optional;

/**
 * Generates an outline or shadow texture for a Pokemon sprite.
 *
 * The outline is only the pixels AROUND the shape; there is none under the Pokemon itself. Drawing
 * several shifted black copies of the sprite instead would, with transparency set, stack up under
 * the middle of the icon into an almost black blob and hide Pokemon underneath.
 *
 * The texture covers the crop from {@link SpriteBounds} widened by {@link #padding} on every side.
 * It is computed once per sprite and style on first draw (render thread, the only place a texture
 * may be uploaded) and then kept in the TextureManager. The texture is white; the color (black or
 * the shiny outline color) comes from the vertex color when drawing.
 */
public final class SpriteOutline {

    private static final int WHITE = 0xFFFFFFFF;
    private static final Logger LOGGER = LogUtils.getLogger();

    private SpriteOutline() {
    }

    /** Outline thickness in sprite pixels: ~5% of the crop's longer side, at least 2 px. */
    public static int padding(SpriteBounds bounds) {
        return Math.max(2, Math.round(Math.max(bounds.width(), bounds.height()) * 0.05F));
    }

    /** @return the outline texture, or empty when the sprite cannot be loaded */
    public static Optional<ResourceLocation> create(ResourceLocation texture, SpriteBounds bounds, OutlineStyle style) {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (resource.isEmpty() || !bounds.measured()) {
            return Optional.empty();
        }
        try (InputStream in = resource.get().open(); NativeImage sprite = NativeImage.read(in)) {
            NativeImage outline = style == OutlineStyle.SHADOW ? shadow(sprite, bounds) : ring(sprite, bounds);
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(texture.getNamespace(),
                    "outline/" + style.id + "/" + texture.getPath());
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(outline));
            return Optional.of(location);
        } catch (Exception e) {
            LOGGER.warn("Could not create {} for {}, drawing the icon without it", style.id, texture, e);
            return Optional.empty();
        }
    }

    private static NativeImage ring(NativeImage sprite, SpriteBounds bounds) {
        int pad = padding(bounds);
        int radiusSq = pad * pad;
        NativeImage out = new NativeImage(bounds.width() + 2 * pad, bounds.height() + 2 * pad, true);
        for (int y = 0; y < out.getHeight(); y++) {
            for (int x = 0; x < out.getWidth(); x++) {
                int sx = bounds.x() - pad + x;
                int sy = bounds.y() - pad + y;
                if (visible(sprite, sx, sy)) {
                    continue;
                }
                if (anyVisibleWithin(sprite, sx, sy, pad, radiusSq)) {
                    out.setPixelRGBA(x, y, WHITE);
                }
            }
        }
        return out;
    }

    private static NativeImage shadow(NativeImage sprite, SpriteBounds bounds) {
        int pad = padding(bounds);
        NativeImage out = new NativeImage(bounds.width() + 2 * pad, bounds.height() + 2 * pad, true);
        for (int y = 0; y < out.getHeight(); y++) {
            for (int x = 0; x < out.getWidth(); x++) {
                int sx = bounds.x() - pad + x;
                int sy = bounds.y() - pad + y;
                if (!visible(sprite, sx, sy) && visible(sprite, sx - pad, sy - pad)) {
                    out.setPixelRGBA(x, y, WHITE);
                }
            }
        }
        return out;
    }

    private static boolean anyVisibleWithin(NativeImage sprite, int cx, int cy, int radius, int radiusSq) {
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx * dx + dy * dy <= radiusSq && visible(sprite, cx + dx, cy + dy)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean visible(NativeImage sprite, int x, int y) {
        return x >= 0 && y >= 0 && x < sprite.getWidth() && y < sprite.getHeight()
                && SpriteBounds.isVisible(sprite.getPixelRGBA(x, y));
    }
}
