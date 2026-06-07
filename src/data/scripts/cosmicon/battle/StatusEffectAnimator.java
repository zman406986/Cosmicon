package data.scripts.cosmicon.battle;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import data.scripts.cosmicon.util.ColorHelper;
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
    private static final float LOOP_GLOW_PAUSE = 0.3f;

    private final List<BoxAnimation> animations = new ArrayList<>();
    private final List<LoopingGlowAnimation> loopingAnimations = new ArrayList<>();

    public void triggerAddAnimation(float uiX, float uiY, float width, float height) {

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

    public void triggerLoopingGlowAnimation(float uiX, float uiY, float width, float height) {

        LoopingGlowAnimation anim = new LoopingGlowAnimation();
        anim.uiX = uiX - LABEL_PADDING;
        anim.uiY = uiY - LABEL_PADDING;
        anim.width = width + LABEL_PADDING * 2f;
        anim.height = height + LABEL_PADDING * 2f;
        anim.elapsed = 0f;
        anim.cycleDuration = PROCESS_BOX_DURATION;
        anim.boxCount = PROCESS_BOX_COUNT;
        anim.staggerDelay = PROCESS_STAGGER_DELAY;
        anim.pauseDuration = LOOP_GLOW_PAUSE;
        anim.color = ColorHelper.PRISMATIC_GOLD;
        loopingAnimations.add(anim);
    }

    public void stopLoopingGlowAnimations() {
        loopingAnimations.clear();
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

        for (LoopingGlowAnimation anim : loopingAnimations) {
            anim.elapsed += amount;
            float totalCycleDuration = anim.cycleDuration + anim.staggerDelay * (anim.boxCount - 1) + anim.pauseDuration;
            if (anim.elapsed >= totalCycleDuration) {
                anim.elapsed -= totalCycleDuration;
                if (anim.elapsed < 0f) anim.elapsed = 0f;
            }
        }
    }

    public void render(float panelX, float panelY, float panelWidth, float panelHeight, float alphaMult) {
        if (animations.isEmpty() && loopingAnimations.isEmpty()) return;



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

            for (LoopingGlowAnimation anim : loopingAnimations) {
                renderLoopingAnimation(anim, alphaMult);
            }

            GLStateUtil.resetColor();
        } finally {
            if (needsContextCleanup) {
                UnifiedCoord.clearCurrent();
            }
        }
    }

    private void renderSingleBox(float uiX, float uiY, float width, float height,
                                 float elapsed, float duration, Color color, float alphaMult) {
        float progress = elapsed / duration;
        float expand = EXPAND_AMOUNT * progress;
        float alpha = INITIAL_ALPHA * (1f - progress) * alphaMult;

        float x = uiX - expand;
        float y = uiY - expand;
        float w = width + expand * 2f;
        float h = height + expand * 2f;

        UnifiedCoord.PanelContext ctx = UnifiedCoord.getCurrent();
        float glX1 = ctx.panelX() + x;
        float glY1 = ctx.panelY() + ctx.panelHeight() - y;
        float glX2 = ctx.panelX() + x + w;
        float glY2 = ctx.panelY() + ctx.panelHeight() - (y + h);

        float[] c = ColorHelper.toGLComponents(color, alpha);
        GL11.glColor4f(c[0], c[1], c[2], c[3]);
        GL11.glLineWidth(2f);

        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(glX1, glY1);
        GL11.glVertex2f(glX2, glY1);
        GL11.glVertex2f(glX2, glY2);
        GL11.glVertex2f(glX1, glY2);
        GL11.glEnd();
    }

    private void renderAnimation(BoxAnimation anim, float alphaMult) {
        for (int box = 0; box < anim.boxCount; box++) {
            float boxStart = box * anim.staggerDelay;
            float boxElapsed = anim.elapsed - boxStart;
            if (boxElapsed < 0f) continue;
            if (boxElapsed >= anim.duration) continue;

            renderSingleBox(anim.uiX, anim.uiY, anim.width, anim.height,
                    boxElapsed, anim.duration, anim.color, alphaMult);
        }
    }

    public boolean hasActiveAnimations() {
        return !animations.isEmpty() || !loopingAnimations.isEmpty();
    }

    public void clear() {
        animations.clear();
        loopingAnimations.clear();
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

    private void renderLoopingAnimation(LoopingGlowAnimation anim, float alphaMult) {
        float totalBoxDuration = anim.cycleDuration + anim.staggerDelay * (anim.boxCount - 1);
        float effectiveElapsed = anim.elapsed;
        if (effectiveElapsed >= totalBoxDuration) return;

        for (int box = 0; box < anim.boxCount; box++) {
            float boxStart = box * anim.staggerDelay;
            float boxElapsed = effectiveElapsed - boxStart;
            if (boxElapsed < 0f) continue;
            if (boxElapsed >= anim.cycleDuration) continue;

            renderSingleBox(anim.uiX, anim.uiY, anim.width, anim.height,
                    boxElapsed, anim.cycleDuration, anim.color, alphaMult);
        }
    }

    private static class LoopingGlowAnimation {
        float uiX;
        float uiY;
        float width;
        float height;
        float elapsed;
        float cycleDuration;
        int boxCount;
        float staggerDelay;
        float pauseDuration;
        Color color;
    }
}
