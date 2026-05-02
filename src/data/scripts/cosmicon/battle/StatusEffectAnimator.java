package data.scripts.cosmicon.battle;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import data.scripts.cosmicon.util.ColorHelper;
import data.scripts.cosmicon.util.CosmiconLogger;
import data.scripts.cosmicon.util.GLStateUtil;
import data.scripts.cosmicon.util.UnifiedCoord;

public class StatusEffectAnimator {

    private static final float SINGLE_BOX_DURATION = 0.35f;
    private static final float PROCESS_BOX_DURATION = 0.3f;
    private static final float PROCESS_STAGGER_DELAY = 0.12f;
    private static final int PROCESS_BOX_COUNT = 3;
    private static final float EXPAND_AMOUNT = 6f;
    private static final float LABEL_PADDING = 2f;
    private static final float INITIAL_ALPHA = 0.8f;

    private final List<BoxAnimation> animations = new ArrayList<>();

    public void triggerAddAnimation(float uiX, float uiY, float width, float height) {
        CosmiconLogger.debug("[StatusAnim] ADD triggered at uiX=%.1f uiY=%.1f w=%.1f h=%.1f", uiX, uiY, width, height);
        BoxAnimation anim = new BoxAnimation();
        anim.uiX = uiX - LABEL_PADDING;
        anim.uiY = uiY - LABEL_PADDING;
        anim.width = width + LABEL_PADDING * 2f;
        anim.height = height + LABEL_PADDING * 2f;
        anim.elapsed = 0f;
        anim.duration = SINGLE_BOX_DURATION;
        anim.boxCount = 1;
        anim.staggerDelay = 0f;
        anim.color = Color.WHITE;
        animations.add(anim);
    }

    public void triggerProcessAnimation(float uiX, float uiY, float width, float height) {
        CosmiconLogger.debug("[StatusAnim] PROCESS triggered at uiX=%.1f uiY=%.1f w=%.1f h=%.1f", uiX, uiY, width, height);
        BoxAnimation anim = new BoxAnimation();
        anim.uiX = uiX - LABEL_PADDING;
        anim.uiY = uiY - LABEL_PADDING;
        anim.width = width + LABEL_PADDING * 2f;
        anim.height = height + LABEL_PADDING * 2f;
        anim.elapsed = 0f;
        anim.duration = PROCESS_BOX_DURATION;
        anim.boxCount = PROCESS_BOX_COUNT;
        anim.staggerDelay = PROCESS_STAGGER_DELAY;
        anim.color = ColorHelper.PRISMATIC_GOLD;
        animations.add(anim);
    }

    public void advance(float amount) {
        for (int i = animations.size() - 1; i >= 0; i--) {
            BoxAnimation anim = animations.get(i);
            anim.elapsed += amount;
            float totalDuration = anim.duration + anim.staggerDelay * (anim.boxCount - 1);
            if (anim.elapsed >= totalDuration) {
                animations.remove(i);
            }
        }
    }

    public void render(float panelX, float panelY, float panelWidth, float panelHeight, float alphaMult) {
        if (animations.isEmpty()) return;

        CosmiconLogger.debug("[StatusAnim] RENDER count=%d alphaMult=%.2f panel=(%.0f,%.0f,%.0f,%.0f)",
            animations.size(), alphaMult, panelX, panelY, panelWidth, panelHeight);

        UnifiedCoord.PanelContext existingCtx = UnifiedCoord.getCurrentOrNull();
        boolean needsContextCleanup = existingCtx == null;

        if (needsContextCleanup) {
            UnifiedCoord.setCurrent(new UnifiedCoord.PanelContext(panelX, panelY, panelWidth, panelHeight));
        }
        try {
            GLStateUtil.resetBlendState();

            for (BoxAnimation anim : animations) {
                renderAnimation(anim, alphaMult);
            }

            GLStateUtil.resetColor();
        } finally {
            if (needsContextCleanup) {
                UnifiedCoord.clearCurrent();
            }
        }
    }

    private void renderAnimation(BoxAnimation anim, float alphaMult) {
        for (int box = 0; box < anim.boxCount; box++) {
            float boxStart = box * anim.staggerDelay;
            float boxElapsed = anim.elapsed - boxStart;
            if (boxElapsed < 0f) continue;
            if (boxElapsed >= anim.duration) continue;

            float progress = boxElapsed / anim.duration;
            float expand = EXPAND_AMOUNT * progress;
            float alpha = INITIAL_ALPHA * (1f - progress) * alphaMult;

            float x = anim.uiX - expand;
            float y = anim.uiY - expand;
            float w = anim.width + expand * 2f;
            float h = anim.height + expand * 2f;

            UnifiedCoord topLeft = new UnifiedCoord(x, y);
            UnifiedCoord bottomRight = new UnifiedCoord(x + w, y + h);
            float glX1 = topLeft.glX();
            float glY1 = topLeft.glY();
            float glX2 = bottomRight.glX();
            float glY2 = bottomRight.glY();

            CosmiconLogger.debug("[StatusAnim] box=%d progress=%.2f glX1=%.0f glY1=%.0f glX2=%.0f glY2=%.0f alpha=%.2f",
                box, progress, glX1, glY1, glX2, glY2, alpha);

            float[] c = ColorHelper.toGLComponents(anim.color, alpha);
            GL11.glColor4f(c[0], c[1], c[2], c[3]);
            GL11.glLineWidth(2f);

            GL11.glBegin(GL11.GL_LINE_LOOP);
            GL11.glVertex2f(glX1, glY1);
            GL11.glVertex2f(glX2, glY1);
            GL11.glVertex2f(glX2, glY2);
            GL11.glVertex2f(glX1, glY2);
            GL11.glEnd();
        }
    }

    public boolean hasActiveAnimations() {
        return !animations.isEmpty();
    }

    public void clear() {
        animations.clear();
    }

    private static class BoxAnimation {
        float uiX;
        float uiY;
        float width;
        float height;
        float elapsed;
        float duration;
        int boxCount;
        float staggerDelay;
        Color color;
    }
}
