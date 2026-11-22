package dev.xardas.nekoroftbchunkscobblemonicons.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A Pokemon sprite as a map icon.
 *
 * Draws only the visible part of the sprite (the transparent margins of the canvas are cut off
 * with UVs from {@link SpriteBounds}), fitted into the given area with its proportions kept and
 * centered in it. The area FTB Chunks gives us is centered on the entity's position, so centering
 * the sprite in the area centers it on the Pokemon. The optional outline/shadow ({@link SpriteOutline})
 * sticks out of the area by the outline thickness.
 *
 * The icon also ignores attempts to color it: since FTB Chunks 2101.1.22 EntityMapIcon.draw()
 * calls icon.withColor(Color4I.BLACK), which turned sprites into black blobs. Vanilla
 * EntityImageIcon behaves the same way (it inherits Icon.withColor() -> copy() -> this).
 */
public class MapSpriteIcon extends Icon {

    /**
     * Side of the inner square the delegate is drawn in.
     * All positioning is done on the PoseStack in floats, because FTB Chunks calls draw()
     * with w=h=1 on the minimap; computing the center with ints would round it to zero.
     */
    private static final int UNIT = 64;
    private static final int OPAQUE = 255;
    private static final int BLACK = 0x000000;
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Caught badges: side of the 32x32 texture as a fraction of the sprite's longer side, and how much
     * it overlaps the sprite. The pictures are about 16 pixels in the middle of the texture, so the
     * drawn picture is about half of this size.
     */
    private static final float BADGE_SIZE = 0.68F;
    private static final float BADGE_OVERLAP = 0.42F;
    /** Badge textures by path (BadgeIcon.texture), loaded on first use. */
    private static final Map<String, FadeCache> BADGES = new HashMap<>();

    private final ResourceLocation texture;
    private final SpriteBounds bounds;
    @Nullable
    private final SpriteTint tint;
    private final float aspectRatio;
    /** Null until the first draw if a tinted texture has to be generated first. */
    private FadeCache sprite;
    /** Generated outlines; a null value means generation failed, so we do not retry every frame. */
    private final Map<OutlineStyle, FadeCache> outlines = new EnumMap<>(OutlineStyle.class);

    public MapSpriteIcon(ResourceLocation texture, SpriteBounds bounds, @Nullable SpriteTint tint) {
        this.texture = texture;
        this.bounds = bounds;
        this.tint = tint;
        this.aspectRatio = bounds.aspectRatio();
        if (tint == null) {
            this.sprite = croppedSprite(texture);
        }
    }

    @Override
    public void draw(GuiGraphics graphics, int x, int y, int w, int h) {
        draw(graphics, x, y, w, h, IconAppearance.plain(OPAQUE));
    }

    public void draw(GuiGraphics graphics, int x, int y, int w, int h, IconAppearance look) {
        float drawWidth = w;
        float drawHeight = h;

        if (aspectRatio >= 1F) {
            drawHeight = h / aspectRatio;
        } else {
            drawWidth = w * aspectRatio;
        }
        float left = x + (w - drawWidth) / 2F;
        float top = y + (h - drawHeight) / 2F;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(left, top, 0F);
        pose.scale(drawWidth / UNIT, drawHeight / UNIT, 1F);
        if (look.outline != OutlineStyle.NONE) {
            drawOutline(graphics, look.outline, look.outlineRgb, look.outlineAlpha);
        }
        drawWithAlpha(graphics, sprite().withColor(look.spriteRgb, look.alpha), look.alpha);
        pose.popPose();

        if (look.badgeTexture != null) {
            // Size from the FTB Chunks area, not from the sprite, so it is the same for narrow
            // and wide Pokemon; badgeScale cancels the scaling by species height.
            drawBadge(graphics, BADGES.computeIfAbsent(look.badgeTexture, path -> new FadeCache(new ImageIcon(
                            ResourceLocation.fromNamespaceAndPath("nekoroftbchunkscobblemonicons", path)))),
                    left + drawWidth, top, Math.max(w, h) * look.badgeScale, look.alpha);
        }
    }

    /**
     * Badge in the sprite's top right corner, partly over it, so with several Pokemon next to each
     * other you can tell which one it belongs to. It is square, so it is drawn outside the UNIT
     * scaling, which differs horizontally and vertically for non-square sprites.
     */
    private static void drawBadge(GuiGraphics graphics, FadeCache badge, float spriteRight, float spriteTop,
                                  float spriteSize, int alpha) {
        float size = spriteSize * BADGE_SIZE;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(spriteRight - size * (1F - BADGE_OVERLAP), spriteTop - size * BADGE_OVERLAP, 0F);
        pose.scale(size / UNIT, size / UNIT, 1F);
        drawWithAlpha(graphics, badge.withColor(0xFFFFFF, alpha), alpha);
        pose.popPose();
    }

    private FadeCache sprite() {
        if (sprite == null) {
            sprite = createTinted().map(this::croppedSprite).orElseGet(() -> croppedSprite(texture));
        }
        return sprite;
    }

    private FadeCache croppedSprite(ResourceLocation location) {
        return new FadeCache(new ImageIcon(location).withUV(bounds.u0(), bounds.v0(), bounds.u1(), bounds.v1()));
    }

    /** Tinted copy of the sprite as a dynamic texture; render thread only, hence created lazily. */
    private Optional<ResourceLocation> createTinted() {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (resource.isEmpty() || tint == null) {
            return Optional.empty();
        }
        try (InputStream in = resource.get().open(); NativeImage original = NativeImage.read(in)) {
            NativeImage tinted = tint.apply(original, bounds);
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(texture.getNamespace(),
                    "tint/" + Integer.toHexString(tint.hashCode()) + "/" + texture.getPath());
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(tinted));
            return Optional.of(location);
        } catch (Exception e) {
            LOGGER.warn("Could not apply color filter to {}, drawing it without the filter", texture, e);
            return Optional.empty();
        }
    }

    private void drawOutline(GuiGraphics graphics, OutlineStyle style, int rgb, int alpha) {
        if (alpha <= 0) {
            return;
        }
        // Not computeIfAbsent: it does not store null, and a failed generation has to be remembered.
        // The outline is built from the original sprite, since the color filter does not change the shape.
        if (!outlines.containsKey(style)) {
            outlines.put(style, SpriteOutline.create(texture, bounds, style)
                    .map(location -> new FadeCache(new ImageIcon(location)))
                    .orElse(null));
        }
        FadeCache outline = outlines.get(style);
        if (outline == null) {
            return;
        }
        // The outline texture is the sprite crop widened by pad on every side; in UNIT units
        // that is pad * UNIT / crop side, separately horizontally and vertically.
        int pad = SpriteOutline.padding(bounds);
        float padX = (float) pad * UNIT / bounds.width();
        float padY = (float) pad * UNIT / bounds.height();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(-padX, -padY, 0F);
        pose.scale((UNIT + 2F * padX) / UNIT, (UNIT + 2F * padY) / UNIT, 1F);
        drawWithAlpha(graphics, outline.withColor(rgb, alpha), alpha);
        pose.popPose();
    }

    /**
     * GuiHelper.drawTexturedRect from FTB Library does not enable blending; it relies on the state
     * left by earlier drawing. At full alpha that does not matter (the shader discards fully
     * transparent margin pixels), but a semi-transparent color without blending is drawn as opaque.
     * On Fabric blending happened to be enabled already and on NeoForge it was not, so transparency
     * only worked on one loader. The state is restored so icons drawn after ours look the same.
     */
    private static void drawWithAlpha(GuiGraphics graphics, ImageIcon icon, int alpha) {
        if (alpha >= OPAQUE) {
            icon.draw(graphics, 0, 0, UNIT, UNIT);
            return;
        }
        boolean blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        if (!blendWasEnabled) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }
        icon.draw(graphics, 0, 0, UNIT, UNIT);
        if (!blendWasEnabled) {
            RenderSystem.disableBlend();
        }
    }

    @Override
    public Icon withColor(Color4I color) {
        return this;
    }

    @Override
    public Icon withTint(Color4I color) {
        return this;
    }

    @Override
    public Icon copy() {
        return this;
    }

    @Override
    public double aspectRatio() {
        return aspectRatio;
    }

    @Override
    public int hashCode() {
        return texture.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o == this || (o instanceof MapSpriteIcon other && texture.equals(other.texture)
                && Objects.equals(tint, other.tint));
    }

    @Override
    public String toString() {
        return texture + " " + bounds + (tint == null ? "" : " " + tint);
    }

    /**
     * The last used color variant of an icon. ImageIcon sends its color as the vertex color, which
     * multiplies the texture, so a white outline texture can take any color and alpha costs nothing
     * when drawing. The variant is kept only to avoid creating a new object every frame; the color
     * changes only when settings are edited or when switching between the minimap and the large map.
     */
    private static final class FadeCache {
        private static final int OPAQUE_WHITE = 0xFFFFFFFF;

        private final ImageIcon base;
        private ImageIcon colored;
        private int coloredArgb = OPAQUE_WHITE;

        private FadeCache(ImageIcon base) {
            this.base = base;
            this.colored = base;
        }

        private ImageIcon withColor(int rgb, int alpha) {
            int argb = (alpha << 24) | (rgb & 0xFFFFFF);
            if (argb == OPAQUE_WHITE) {
                return base;
            }
            if (argb != coloredArgb) {
                colored = base.withColor(Color4I.rgba(argb));
                coloredArgb = argb;
            }
            return colored;
        }
    }
}
