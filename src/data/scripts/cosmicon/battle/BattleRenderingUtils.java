package data.scripts.cosmicon.battle;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.cosmicon.util.ColorHelper;
import data.scripts.cosmicon.util.GLStateUtil;

public final class BattleRenderingUtils {
    private BattleRenderingUtils() {}

    public static final float PANEL_WIDTH = 1000f;
    public static final float PANEL_HEIGHT = 700f;

    public static final float CARD_WIDTH = 180f;
    public static final float CARD_HEIGHT = 220f;

    public static final float PORTRAIT_SCALE = 0.85f;
    public static final float DICE_ICON_SIZE = 22f;
    public static final float DICE_ICON_SPACING = 26f;
    public static final float DICE_POOL_RIGHT_MARGIN = 12f;
    public static final float DICE_POOL_TOP_MARGIN = 12f;
    public static final float ATK_DEF_ICON_SIZE = 28f;
    public static final float ATK_LEFT_MARGIN = 15f;
    public static final float DEF_RIGHT_MARGIN = 15f;
    public static final float ATK_DEF_BOTTOM_MARGIN = 15f;

    public static final float PORTRAIT_DISPLAY_W = CARD_WIDTH * PORTRAIT_SCALE;
    public static final float PORTRAIT_DISPLAY_H = CARD_HEIGHT * PORTRAIT_SCALE;

    public static final float MARGIN = 20f;
    public static final float BUTTON_WIDTH = 120f;
    public static final float BUTTON_HEIGHT = 40f;

    public static final Color COLOR_BG_DARK = new Color(25, 25, 35);
    public static final Color COLOR_BG_BOARD = new Color(45, 50, 70);
    public static final Color COLOR_CARD_BG = new Color(60, 65, 85);
    public static final Color COLOR_HP_TEXT = new Color(255, 255, 255);
    public static final Color COLOR_PASSIVE_BG = new Color(35, 38, 50, 220);
    public static final Color COLOR_PASSIVE_BORDER = new Color(80, 85, 100);
    public static final Color COLOR_TEXT = new Color(220, 220, 230);
    public static final Color COLOR_SHADOW = new Color(0, 0, 0, 80);

    public static final Color COLOR_ATTACK_SIDE = new Color(255, 120, 120, 40);
    public static final Color COLOR_DEFENSE_SIDE = new Color(120, 180, 255, 40);
    public static final float ROLE_ICON_OPACITY = 0.25f;
    public static final float ROLE_ICON_SIZE_RATIO = 0.6f;

    public static void renderBattleBackground(float x, float y, float w, float h, 
            float transitionProgress, float alphaMult, boolean showRoles) {
        GLStateUtil.resetBlendState();

        Misc.renderQuad(x, y, w, h, COLOR_BG_DARK, alphaMult);

        float boardMargin = 40f;
        Misc.renderQuad(x + boardMargin, y + boardMargin, w - boardMargin * 2, h - boardMargin * 2, 
            COLOR_BG_BOARD, alphaMult * 0.6f);

        if (showRoles) {
            renderSideBackgrounds(x, y, w, h, transitionProgress, alphaMult);
            renderRoleIconOverlay(x, y, w, h, transitionProgress, alphaMult);
        }
    }

    private static void renderSideBackgrounds(float x, float y, float w, float h, 
            float transition, float alphaMult) {
        float halfH = h / 2f;

        Color bottomColor = blendSideColors(COLOR_ATTACK_SIDE, COLOR_DEFENSE_SIDE, transition);
        Color topColor = blendSideColors(COLOR_DEFENSE_SIDE, COLOR_ATTACK_SIDE, transition);

        Misc.renderQuad(x, y, w, halfH, bottomColor, alphaMult);
        Misc.renderQuad(x, y + halfH, w, halfH, topColor, alphaMult);
    }

    private static Color blendSideColors(Color color1, Color color2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = (int) (color1.getRed() + (color2.getRed() - color1.getRed()) * t);
        int g = (int) (color1.getGreen() + (color2.getGreen() - color1.getGreen()) * t);
        int b = (int) (color1.getBlue() + (color2.getBlue() - color1.getBlue()) * t);
        int a = (int) (color1.getAlpha() + (color2.getAlpha() - color1.getAlpha()) * t);
        return new Color(r, g, b, a);
    }

    private static void renderRoleIconOverlay(float x, float y, float w, float h, 
            float transition, float alphaMult) {
        float halfH = h / 2f;
        float iconSize = halfH * ROLE_ICON_SIZE_RATIO;

        GLStateUtil.enableTexturingWithBlend();

        SpriteAPI atkIcon = CosmiconSprites.getAtkIcon();
        SpriteAPI defIcon = CosmiconSprites.getDefIcon();

        if (atkIcon == null || defIcon == null) {
            GLStateUtil.disableTexturing();
            return;
        }

        float bottomIconAlpha = (1f - transition) * ROLE_ICON_OPACITY + transition * ROLE_ICON_OPACITY * 0.3f;
        float topIconAlpha = transition * ROLE_ICON_OPACITY + (1f - transition) * ROLE_ICON_OPACITY * 0.3f;

        SpriteAPI bottomIcon = (1f - transition) >= 0.5f ? atkIcon : defIcon;
        SpriteAPI topIcon = transition >= 0.5f ? atkIcon : defIcon;

        float bottomIconX = x + (w - iconSize) / 2f;
        float bottomIconY = y + (halfH - iconSize) / 2f;
        bottomIcon.setSize(iconSize, iconSize);
        bottomIcon.setAlphaMult(alphaMult * bottomIconAlpha);
        bottomIcon.render(bottomIconX, bottomIconY);

        float topIconX = x + (w - iconSize) / 2f;
        float topIconY = y + halfH + (halfH - iconSize) / 2f;
        topIcon.setSize(iconSize, iconSize);
        topIcon.setAlphaMult(alphaMult * topIconAlpha);
        topIcon.render(topIconX, topIconY);

        GLStateUtil.disableTexturing();
    }

    public static void renderCardPlaceholder(float x, float y, float w, float h, Color color, float alphaMult) {
        GLStateUtil.resetBlendState();

        float portraitY = y + h - 80f - 50f;
        Misc.renderQuad(x + 15, portraitY, w - 30, 80, color.darker(), alphaMult * 0.5f);
    }

    public static void renderCharacterCard(float x, float y, CharacterCard card, float alphaMult) {
        GLStateUtil.enableTexturingWithBlend();

        SpriteAPI portrait = CosmiconSprites.getPortrait(card.getId());
        if (portrait != null) {
            float portraitX = x + (CARD_WIDTH - PORTRAIT_DISPLAY_W) / 2f;
            float portraitY = y + (CARD_HEIGHT - PORTRAIT_DISPLAY_H) / 2f;
            portrait.setSize(PORTRAIT_DISPLAY_W, PORTRAIT_DISPLAY_H);
            portrait.setAlphaMult(alphaMult);
            portrait.render(portraitX, portraitY);
        } else {
            GLStateUtil.disableTexturing();
            Misc.renderQuad(x, y, CARD_WIDTH, CARD_HEIGHT, COLOR_CARD_BG, alphaMult * 0.7f);
            GLStateUtil.enableTexturing();
        }

        SpriteAPI frame = CosmiconSprites.getFrame();
        if (frame != null) {
            frame.setSize(CARD_WIDTH, CARD_HEIGHT);
            frame.setAlphaMult(alphaMult);
            frame.render(x, y);
        }

        renderDicePoolIcons(x, y, card.getDicePool(), alphaMult);

        GLStateUtil.disableTexturing();

        SpriteAPI atkIcon = CosmiconSprites.getAtkIcon();
        if (atkIcon != null) {
            GLStateUtil.enableTexturing();
            atkIcon.setSize(ATK_DEF_ICON_SIZE, ATK_DEF_ICON_SIZE);
            atkIcon.setAlphaMult(alphaMult);
            float atkX = x + ATK_LEFT_MARGIN;
            float atkY = y + ATK_DEF_BOTTOM_MARGIN;
            atkIcon.render(atkX, atkY);
            GLStateUtil.disableTexturing();
        }

        SpriteAPI defIcon = CosmiconSprites.getDefIcon();
        if (defIcon != null) {
            GLStateUtil.enableTexturing();
            defIcon.setSize(ATK_DEF_ICON_SIZE, ATK_DEF_ICON_SIZE);
            defIcon.setAlphaMult(alphaMult);
            float defX = x + CARD_WIDTH - DEF_RIGHT_MARGIN - ATK_DEF_ICON_SIZE;
            float defY = y + ATK_DEF_BOTTOM_MARGIN;
            defIcon.render(defX, defY);
            GLStateUtil.disableTexturing();
        }

        GLStateUtil.resetColor();
    }

    public static void renderDicePoolIcons(float cardX, float cardY, List<DiceType> pool, float alphaMult) {
        Map<DiceType, Integer> counts = new HashMap<>();
        for (DiceType d : pool) {
            counts.merge(d, 1, Integer::sum);
        }

        GLStateUtil.enableTexturingWithBlend();

        float startX = cardX + CARD_WIDTH - DICE_POOL_RIGHT_MARGIN - DICE_ICON_SIZE;
        float startY = cardY + CARD_HEIGHT - DICE_POOL_TOP_MARGIN - DICE_ICON_SIZE;

        List<DiceType> order = Arrays.asList(
            DiceType.PRISMATIC_D12, DiceType.ORANGE_D8, DiceType.PURPLE_D6, DiceType.BLUE_D4
        );

        float offsetY = 0f;
        for (DiceType type : order) {
            int count = counts.getOrDefault(type, 0);
            if (count > 0) {
                SpriteAPI icon = CosmiconSprites.getDiceIcon(type);
                if (icon != null) {
                    icon.setSize(DICE_ICON_SIZE, DICE_ICON_SIZE);
                    icon.setAlphaMult(alphaMult);
                    icon.render(startX, startY - offsetY);
                }
                offsetY += DICE_ICON_SPACING;
            }
        }

        GLStateUtil.disableTexturing();
    }

    public static void renderPassiveBox(float x, float y, float w, float h, float alphaMult) {
        GLStateUtil.resetBlendState();

        Misc.renderQuad(x + 3, y - 3, w, h, COLOR_SHADOW, alphaMult * 0.3f);

        Misc.renderQuad(x, y, w, h, COLOR_PASSIVE_BG, alphaMult);

        renderRoundedBorder(x, y, w, h, alphaMult);
    }

    private static void renderRoundedBorder(float x, float y, float w, float h, float alphaMult) {
        float radius = 8f;
        GLStateUtil.resetBlendState();

        float[] c = ColorHelper.toGLComponents(COLOR_PASSIVE_BORDER, alphaMult);
        GL11.glColor4f(c[0], c[1], c[2], c[3]);
        GL11.glLineWidth(2f);

        GL11.glBegin(GL11.GL_LINE_LOOP);

        int segments = 8;
        float step = (float) Math.PI / 2 / segments;

        for (int i = 0; i <= segments; i++) {
            float angle = (float) Math.PI + i * step;
            GL11.glVertex2f(x + radius + (float) Math.cos(angle) * radius, y + radius + (float) Math.sin(angle) * radius);
        }

        for (int i = 0; i <= segments; i++) {
            float angle = (float) Math.PI * 1.5f + i * step;
            GL11.glVertex2f(x + w - radius + (float) Math.cos(angle) * radius, y + radius + (float) Math.sin(angle) * radius);
        }

        for (int i = 0; i <= segments; i++) {
            float angle = i * step;
            GL11.glVertex2f(x + w - radius + (float) Math.cos(angle) * radius, y + h - radius + (float) Math.sin(angle) * radius);
        }

        for (int i = 0; i <= segments; i++) {
            float angle = (float) Math.PI / 2 + i * step;
            GL11.glVertex2f(x + radius + (float) Math.cos(angle) * radius, y + h - radius + (float) Math.sin(angle) * radius);
        }

        GL11.glEnd();
        GLStateUtil.resetColor();
    }

    public static void renderDiceZone(float x, float y, float w, float h, float alphaMult) {
        GLStateUtil.resetBlendState();

        Misc.renderQuad(x, y, w, h, ColorHelper.DICE_ZONE_BG, alphaMult);

        float dashLen = 10f;
        float gapLen = 5f;

        GL11.glLineWidth(2f);
        GL11.glColor4f(0.5f, 0.55f, 0.7f, 0.6f * alphaMult);

        renderDashedLine(x, y, x + w, y, dashLen, gapLen);
        renderDashedLine(x + w, y, x + w, y + h, dashLen, gapLen);
        renderDashedLine(x + w, y + h, x, y + h, dashLen, gapLen);
        renderDashedLine(x, y + h, x, y, dashLen, gapLen);

        GLStateUtil.resetColor();
    }

    private static void renderDashedLine(float x1, float y1, float x2, float y2, float dashLen, float gapLen) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist < 0.001f) return;

        float ux = dx / dist;
        float uy = dy / dist;

        float current = 0;
        boolean drawing = true;

        GL11.glBegin(GL11.GL_LINES);

        while (current < dist) {
            float segLen = drawing ? dashLen : gapLen;
            float end = Math.min(current + segLen, dist);

            if (drawing) {
                float sx = x1 + ux * current;
                float sy = y1 + uy * current;
                float ex = x1 + ux * end;
                float ey = y1 + uy * end;
                GL11.glVertex2f(sx, sy);
                GL11.glVertex2f(ex, ey);
            }

            current += segLen;
            drawing = !drawing;
        }

        GL11.glEnd();
    }
}