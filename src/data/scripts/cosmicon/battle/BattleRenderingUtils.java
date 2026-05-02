package data.scripts.cosmicon.battle;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.cosmicon.util.ColorHelper;
import data.scripts.cosmicon.util.GLStateUtil;

public final class BattleRenderingUtils {
    private BattleRenderingUtils() {}

    public static final float PANEL_WIDTH = 1000f;
    public static final float PANEL_HEIGHT = 700f;

    public static final float CARD_WIDTH = 180f;
    public static final float CARD_HEIGHT = 254f;

    public static final float PORTRAIT_SCALE = 0.95f;
    public static final float DICE_ICON_SIZE = 22f;
    public static final float DICE_ICON_SPACING = 21f;
    public static final float DICE_POOL_RIGHT_MARGIN = 3f;
    public static final float DICE_POOL_TOP_MARGIN = 0f;
    public static final float ATK_DEF_ICON_SIZE = 49f;
    public static final float ATK_LEFT_MARGIN = 1f;
    public static final float DEF_RIGHT_MARGIN = 1f;
    public static final float ATK_DEF_BOTTOM_MARGIN = 1f;

    public static final float PORTRAIT_DISPLAY_W = CARD_WIDTH * PORTRAIT_SCALE;
    public static final float PORTRAIT_DISPLAY_H = CARD_HEIGHT * PORTRAIT_SCALE;

    public static final float MARGIN = 20f;
    public static final float STATUS_BOX_WIDTH = 180f;
    public static final float STATUS_BOX_PADDING = 15f;
    public static final Color COLOR_STATUS_BG = new Color(30, 35, 45, 220);
    public static final Color COLOR_STATUS_BORDER = new Color(70, 75, 90);
    public static final float OPPONENT_DICE_ZONE_OFFSET_X = 30f;
    public static final float OPPONENT_DICE_ZONE_Y_OFFSET = 80f;
    public static final float OPPONENT_DICE_ZONE_W = 350f;
    public static final float OPPONENT_DICE_ZONE_H = 80f;
    public static final float BUTTON_WIDTH = 120f;
    public static final float BUTTON_HEIGHT = 40f;

    public static final Color COLOR_BG_DARK = new Color(25, 25, 35);
    public static final Color COLOR_BG_BOARD = new Color(45, 50, 70);
    public static final Color COLOR_CARD_BG = new Color(60, 65, 85);
    public static final Color COLOR_HP_TEXT = new Color(255, 255, 255);
    public static final Color COLOR_TEXT = new Color(220, 220, 230);
    public static final Color COLOR_SHADOW = new Color(0, 0, 0, 80);
    public static final float HP_CIRCLE_RADIUS = 19f;
    public static final float HP_CIRCLE_INNER_RADIUS = 12f;
    public static final Color COLOR_HP_CIRCLE_BG = new Color(40, 40, 40, 150);

    public static final Color COLOR_ATTACK_SIDE = new Color(255, 120, 120, 40);
    public static final Color COLOR_DEFENSE_SIDE = new Color(120, 180, 255, 40);
    public static final float ROLE_ICON_OPACITY = 0.25f;
    public static final float ROLE_ICON_SIZE_RATIO = 0.6f;

    public static void renderBattleBackground(float x, float y, float w, float h, 
            float rotationAngle, float alphaMult, boolean showRoles, boolean hideRoleIcons) {
        GLStateUtil.resetBlendState();

        Misc.renderQuad(x, y, w, h, COLOR_BG_DARK, alphaMult);

        float boardMargin = 40f;
        Misc.renderQuad(x + boardMargin, y + boardMargin, w - boardMargin * 2, h - boardMargin * 2, 
            COLOR_BG_BOARD, alphaMult * 0.6f);

        if (showRoles) {
            renderSideBackgrounds(x, y, w, h, rotationAngle, alphaMult);
            if (!hideRoleIcons) {
                renderRoleIconOverlay(x, y, w, h, rotationAngle, alphaMult);
            }
        }
    }

    static void setupScissor(float panelX, float panelY, float panelW, float panelH) {
        float scale = Global.getSettings().getScreenScaleMult();
        
        int scissorX = Math.round(panelX * scale);
        int scissorY = Math.round(panelY * scale);
        int scissorW = Math.round(panelW * scale);
        int scissorH = Math.round(panelH * scale);
        
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorW, scissorH);
    }

    private static void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public static void renderHpCircle(float glCenterX, float glCenterY, float radius, float fillFraction, float alphaMult) {
        float clampedFill = Math.max(0f, Math.min(1f, fillFraction));
        float innerRadius = HP_CIRCLE_INNER_RADIUS;

        GLStateUtil.resetBlendState();

        float[] bg = ColorHelper.toGLComponents(COLOR_HP_CIRCLE_BG, alphaMult);
        GL11.glColor4f(bg[0], bg[1], bg[2], bg[3]);
        drawRingArc(glCenterX, glCenterY, radius, innerRadius, 0f, (float) (2 * Math.PI));

        if (clampedFill > 0f) {
            Color hpColor = getHpColor(clampedFill);
            float[] fg = ColorHelper.toGLComponents(hpColor, alphaMult);
            GL11.glColor4f(fg[0], fg[1], fg[2], fg[3]);

            float startAngle = (float) (-Math.PI / 2);
            float sweepAngle = (float) (2 * Math.PI * clampedFill);
            drawRingArc(glCenterX, glCenterY, radius, innerRadius, startAngle, startAngle + sweepAngle);
        }

        GLStateUtil.resetColor();
    }

    private static Color getHpColor(float fillFraction) {
        if (fillFraction > 0.6f) {
            return new Color(50, 205, 50, 220);
        } else if (fillFraction > 0.4f) {
            return new Color(255, 215, 0, 220);
        } else if (fillFraction > 0.2f) {
            return new Color(255, 140, 0, 220);
        } else {
            return new Color(220, 50, 50, 220);
        }
    }

    private static void drawRingArc(float cx, float cy, float outerRadius, float innerRadius, float startAngle, float endAngle) {
        int segments = 36;
        float angleStep = (endAngle - startAngle) / segments;

        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        for (int i = 0; i <= segments; i++) {
            float angle = startAngle + i * angleStep;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            GL11.glVertex2f(cx + outerRadius * cos, cy + outerRadius * sin);
            GL11.glVertex2f(cx + innerRadius * cos, cy + innerRadius * sin);
        }
        GL11.glEnd();
    }

    private static void renderSideBackgrounds(float x, float y, float w, float h, 
            float rotationAngle, float alphaMult) {
        float halfH = h / 2f;
        float centerX = x + w / 2f;
        float centerY = y + h / 2f;

        float extendAmount = halfH * 0.5f;
        float renderH = h + extendAmount * 2f;
        float renderY = y - extendAmount;
        float renderHalfH = renderH / 2f;

        setupScissor(x, y, w, h);

        GL11.glPushMatrix();
        GL11.glTranslatef(centerX, centerY, 0f);
        GL11.glRotatef(rotationAngle, 0f, 0f, 1f);
        GL11.glTranslatef(-centerX, -centerY, 0f);

        Misc.renderQuad(x, renderY, w, renderHalfH, COLOR_ATTACK_SIDE, alphaMult);
        Misc.renderQuad(x, renderY + renderHalfH, w, renderHalfH, COLOR_DEFENSE_SIDE, alphaMult);

        GL11.glPopMatrix();

        disableScissor();
    }

    private static void renderRoleIconOverlay(float x, float y, float w, float h, 
            float rotationAngle, float alphaMult) {
        float halfH = h / 2f;
        float iconSize = halfH * ROLE_ICON_SIZE_RATIO;
        float defIconCenterX = x + w / 2f;
        float centerY = y + h / 2f;

        GLStateUtil.enableTexturingWithBlend();

        SpriteAPI atkIcon = CosmiconSprites.getAtkIcon();
        SpriteAPI defIcon = CosmiconSprites.getDefIcon();

        if (atkIcon == null || defIcon == null) {
            GLStateUtil.disableTexturing();
            return;
        }

        GL11.glPushMatrix();
        GL11.glTranslatef(defIconCenterX, centerY, 0f);
        GL11.glRotatef(rotationAngle, 0f, 0f, 1f);
        GL11.glTranslatef(-defIconCenterX, -centerY, 0f);

        float atkIconCenterY = y + halfH / 2f;
        
        GL11.glPushMatrix();
        GL11.glTranslatef(defIconCenterX, atkIconCenterY, 0f);
        GL11.glRotatef(-rotationAngle, 0f, 0f, 1f);
        atkIcon.setSize(iconSize, iconSize);
        atkIcon.setAlphaMult(alphaMult * ROLE_ICON_OPACITY);
        atkIcon.render(-iconSize / 2f, -iconSize / 2f);
        GL11.glPopMatrix();

        float defIconCenterY = y + halfH + halfH / 2f;
        
        GL11.glPushMatrix();
        GL11.glTranslatef(defIconCenterX, defIconCenterY, 0f);
        GL11.glRotatef(-rotationAngle, 0f, 0f, 1f);
        defIcon.setSize(iconSize, iconSize);
        defIcon.setAlphaMult(alphaMult * ROLE_ICON_OPACITY);
        defIcon.render(-iconSize / 2f, -iconSize / 2f);
        GL11.glPopMatrix();

        GL11.glPopMatrix();

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

        renderDicePoolIcons(x, y, alphaMult);

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

    public static void renderDicePoolIcons(float cardX, float cardY, float alphaMult) {
        GLStateUtil.enableTexturingWithBlend();

        float startX = cardX + CARD_WIDTH - DICE_POOL_RIGHT_MARGIN - DICE_ICON_SIZE;
        float startY = cardY + CARD_HEIGHT - DICE_POOL_TOP_MARGIN - DICE_ICON_SIZE;

        List<DiceType> order = Arrays.asList(
            DiceType.ORANGE_D8, DiceType.PURPLE_D6, DiceType.BLUE_D4, DiceType.PRISMATIC
        );

        float offsetY = 0f;
        for (DiceType type : order) {
            SpriteAPI icon = CosmiconSprites.getDiceIcon(type);
            if (icon != null) {
                icon.setSize(DICE_ICON_SIZE, DICE_ICON_SIZE);
                icon.setAlphaMult(alphaMult);
                icon.render(startX, startY - offsetY);
            }
            offsetY += DICE_ICON_SPACING;
        }

        GLStateUtil.disableTexturing();
    }

    public static void renderStatusEffectBox(float x, float y, float w, float h, float alphaMult) {
        GLStateUtil.resetBlendState();

        Misc.renderQuad(x + 3, y - 3, w, h, COLOR_SHADOW, alphaMult * 0.3f);
        Misc.renderQuad(x, y, w, h, COLOR_STATUS_BG, alphaMult);

        float radius = 8f;
        float[] c = ColorHelper.toGLComponents(COLOR_STATUS_BORDER, alphaMult);
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

    public static void renderOpponentDiceZone(float x, float y, float alphaMult) {
        GLStateUtil.resetBlendState();
        Misc.renderQuad(x, y, OPPONENT_DICE_ZONE_W, OPPONENT_DICE_ZONE_H, ColorHelper.OPPONENT_DICE_ZONE_BG, alphaMult);
        
        GL11.glLineWidth(2f);
        float[] c = ColorHelper.toGLComponents(new Color(120, 80, 80, 150), alphaMult);
        GL11.glColor4f(c[0], c[1], c[2], c[3]);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + OPPONENT_DICE_ZONE_W, y);
        GL11.glVertex2f(x + OPPONENT_DICE_ZONE_W, y + OPPONENT_DICE_ZONE_H);
        GL11.glVertex2f(x, y + OPPONENT_DICE_ZONE_H);
        GL11.glEnd();
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