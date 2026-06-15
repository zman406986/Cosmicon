package data.scripts.cosmicon.battle;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import data.scripts.cosmicon.util.ColorHelper;
import data.scripts.cosmicon.util.GLStateUtil;
import data.scripts.cosmicon.util.UnifiedCoord;

public class SlashTrailEffect {
    private static final float TRAIL_CORE_WIDTH = 4f;
    private static final float TRAIL_GLOW_WIDTH = 10f;
    private static final float GLOW_ALPHA = 0.25f;
    private static final float RED_FADE_DURATION = 0.45f;

    private static final Color COLOR_WHITE = new Color(255, 255, 255);
    private static final Color COLOR_CYAN_GLOW = new Color(170, 220, 255);
    private static final Color COLOR_RED = new Color(255, 55, 35);
    private static final Color COLOR_RED_GLOW = new Color(255, 90, 60);

    private float startX, startY;
    private float endX, endY;
    private float totalDist;
    private float flightSpeed;
    private float elapsed;
    private float fadeElapsed;
    private boolean active;
    private boolean redPhase;
    private boolean fading;
    private float transitionDist;

    public void trigger(float startX, float startY, float flightSpeed) {
        this.startX = startX;
        this.startY = startY;
        this.endX = startX;
        this.endY = startY;
        this.flightSpeed = flightSpeed;
        this.elapsed = 0f;
        this.fadeElapsed = 0f;
        this.active = true;
        this.redPhase = false;
        this.fading = false;
        this.transitionDist = 0f;
        this.totalDist = 0f;
    }

    public void updateEndPosition(float x, float y) {
        this.endX = x;
        this.endY = y;
        float dx = x - startX;
        float dy = y - startY;
        this.totalDist = (float) Math.sqrt(dx * dx + dy * dy);
    }

    public void transitionToRed() {
        this.redPhase = true;
        this.transitionDist = 0f;
    }

    public void advance(float amount) {
        if (!active) return;
        elapsed += amount;

        if (redPhase && !fading) {
            transitionDist += flightSpeed * amount;
            if (transitionDist >= totalDist) {
                transitionDist = totalDist;
                fading = true;
                fadeElapsed = 0f;
            }
        }

        if (fading) {
            fadeElapsed += amount;
            if (fadeElapsed >= RED_FADE_DURATION) {
                active = false;
            }
        }
    }

    public boolean isComplete() {
        return !active;
    }

    public void clear() {
        active = false;
        redPhase = false;
        fading = false;
    }

    public float getTransitionDistance() {
        return transitionDist;
    }

    public void render(float panelX, float panelY, float panelWidth, float panelHeight, float alphaMult) {
        if (!active) return;

        float glX1 = panelX + startX;
        float glY1 = panelY + panelHeight - startY;
        float glX2 = panelX + endX;
        float glY2 = panelY + panelHeight - endY;

        float alpha = fading
            ? (1f - fadeElapsed / RED_FADE_DURATION) * alphaMult
            : alphaMult;

        if (redPhase) {
            float t = totalDist > 0f ? Math.min(1f, transitionDist / totalDist) : 1f;
            float transX = startX + (endX - startX) * t;
            float transY = startY + (endY - startY) * t;
            float glTX = panelX + transX;
            float glTY = panelY + panelHeight - transY;

            if (transitionDist > 0.5f) {
                renderThickLine(glX1, glY1, glTX, glTY, COLOR_RED, COLOR_RED_GLOW, alpha);
            }
            if (transitionDist < totalDist - 0.5f) {
                renderThickLine(glTX, glTY, glX2, glY2, COLOR_WHITE, COLOR_CYAN_GLOW, alpha);
            }
        } else {
            renderThickLine(glX1, glY1, glX2, glY2, COLOR_WHITE, COLOR_CYAN_GLOW, alpha);
        }
    }

    private void renderThickLine(float x1, float y1, float x2, float y2,
                                 Color coreColor, Color glowColor, float alpha) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.5f) return;

        float perpX = -dy / len;
        float perpY = dx / len;

        float halfCore = TRAIL_CORE_WIDTH / 2f;
        float halfGlow = TRAIL_GLOW_WIDTH / 2f;

        GLStateUtil.resetBlendState();

        float[] gc = ColorHelper.toGLComponents(glowColor, alpha * GLOW_ALPHA);
        GL11.glColor4f(gc[0], gc[1], gc[2], gc[3]);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x1 + perpX * halfGlow, y1 + perpY * halfGlow);
        GL11.glVertex2f(x1 - perpX * halfGlow, y1 - perpY * halfGlow);
        GL11.glVertex2f(x2 - perpX * halfGlow, y2 - perpY * halfGlow);
        GL11.glVertex2f(x2 + perpX * halfGlow, y2 + perpY * halfGlow);
        GL11.glEnd();

        float[] cc = ColorHelper.toGLComponents(coreColor, alpha);
        GL11.glColor4f(cc[0], cc[1], cc[2], cc[3]);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x1 + perpX * halfCore, y1 + perpY * halfCore);
        GL11.glVertex2f(x1 - perpX * halfCore, y1 - perpY * halfCore);
        GL11.glVertex2f(x2 - perpX * halfCore, y2 - perpY * halfCore);
        GL11.glVertex2f(x2 + perpX * halfCore, y2 + perpY * halfCore);
        GL11.glEnd();

        GLStateUtil.resetColor();
    }
}
