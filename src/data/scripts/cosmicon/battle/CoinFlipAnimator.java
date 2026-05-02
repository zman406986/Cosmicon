package data.scripts.cosmicon.battle;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.graphics.SpriteAPI;

import data.scripts.cosmicon.util.EasingUtil;
import data.scripts.cosmicon.util.GLStateUtil;

public class CoinFlipAnimator {

    private static final float SPIN_UP_DURATION = 0.4f;
    private static final float FLIP_DURATION = 1.2f;
    private static final float SETTLE_DURATION = 0.3f;
    private static final float REVEAL_DURATION = 0.4f;
    private static final float TOTAL_DURATION = SPIN_UP_DURATION + FLIP_DURATION + SETTLE_DURATION + REVEAL_DURATION;

    private static final float COIN_SIZE = 120f;
    private static final float COIN_SHADOW_OFFSET = 8f;

    public enum Face {
        ATTACK,
        DEFENSE
    }

    private enum Phase {
        SPIN_UP,
        FLIP,
        SETTLE,
        REVEAL,
        COMPLETE
    }

    private final boolean playerIsAttacker;
    private final Face winningFace;
    private float elapsed;
    private Phase phase;
    private float phaseElapsed;
    private boolean complete;
    private boolean skipped;

    private float coinScale;
    private float coinRotation;
    private float coinSquash;
    private float shadowAlpha;
    private float shadowScale;
    private float resultTextAlpha;

    public CoinFlipAnimator() {
        this.playerIsAttacker = Math.random() < 0.5f;
        this.winningFace = this.playerIsAttacker ? Face.ATTACK : Face.DEFENSE;
        this.elapsed = 0f;
        this.phase = Phase.SPIN_UP;
        this.phaseElapsed = 0f;
        this.complete = false;
        this.skipped = false;
        this.coinScale = 0f;
        this.coinRotation = 0f;
        this.coinSquash = 1f;
        this.shadowAlpha = 0f;
        this.shadowScale = 0f;
        this.resultTextAlpha = 0f;
    }

    public boolean isPlayerAttacker() {
        return playerIsAttacker;
    }

    public Face getWinningFace() {
        return winningFace;
    }

    public boolean isComplete() {
        return complete;
    }

    public void skip() {
        if (complete || skipped) return;
        skipped = true;
        elapsed = TOTAL_DURATION;
        phase = Phase.COMPLETE;
        phaseElapsed = 0f;
        coinScale = 1f;
        coinRotation = 0f;
        coinSquash = 1f;
        shadowAlpha = 0.4f;
        shadowScale = 1f;
        resultTextAlpha = 1f;
        complete = true;
    }

    public void advance(float amount) {
        if (complete) return;

        elapsed += amount;
        phaseElapsed += amount;

        switch (phase) {
            case SPIN_UP -> advanceSpinUp();
            case FLIP -> advanceFlip();
            case SETTLE -> advanceSettle();
            case REVEAL -> advanceReveal();
            case COMPLETE -> complete = true;
        }
    }

    private void advanceSpinUp() {
        float progress = Math.min(1f, phaseElapsed / SPIN_UP_DURATION);
        float eased = EasingUtil.easeOutCubic(progress);

        coinScale = eased;
        shadowAlpha = eased * 0.4f;
        shadowScale = eased;
        coinRotation = eased * 360f * 2f;

        if (phaseElapsed >= SPIN_UP_DURATION) {
            phase = Phase.FLIP;
            phaseElapsed = 0f;
        }
    }

    private void advanceFlip() {
        float progress = Math.min(1f, phaseElapsed / FLIP_DURATION);
        float eased = EasingUtil.easeOutCubic(progress);

        float flipCount = 5f;
        float totalRotation = flipCount * 360f;
        coinRotation = totalRotation * eased;

        float squashFrequency = flipCount * (float) Math.PI;
        coinSquash = (float) Math.cos(squashFrequency * eased);

        float bounce = (float) Math.sin(progress * (float) Math.PI);
        coinScale = 1f + bounce * 0.15f;
        shadowScale = 1f - bounce * 0.2f;
        shadowAlpha = 0.4f - bounce * 0.1f;

        if (phaseElapsed >= FLIP_DURATION) {
            phase = Phase.SETTLE;
            phaseElapsed = 0f;
        }
    }

    private void advanceSettle() {
        float progress = Math.min(1f, phaseElapsed / SETTLE_DURATION);
        float eased = EasingUtil.easeOutQuad(progress);

        float targetSquash = winningFace == Face.ATTACK ? 1f : -1f;
        coinSquash = targetSquash * eased + (1f - eased) * coinSquash;
        coinScale = 1f + (1f - eased) * 0.1f;
        coinRotation = coinRotation * (1f - eased);

        if (phaseElapsed >= SETTLE_DURATION) {
            phase = Phase.REVEAL;
            phaseElapsed = 0f;
            coinSquash = targetSquash;
            coinScale = 1f;
            coinRotation = 0f;
        }
    }

    private void advanceReveal() {
        float progress = Math.min(1f, phaseElapsed / REVEAL_DURATION);
        resultTextAlpha = EasingUtil.easeOutQuad(progress);

        if (phaseElapsed >= REVEAL_DURATION) {
            phase = Phase.COMPLETE;
            complete = true;
        }
    }

    public void render(float centerX, float centerY, float alphaMult) {
        if (phase == Phase.COMPLETE && !skipped && elapsed <= 0f) return;

        GLStateUtil.resetBlendState();

        renderShadow(centerX, centerY, alphaMult);
        renderCoin(centerX, centerY, alphaMult);

        if (resultTextAlpha > 0f) {
            renderResultText(centerX, centerY, alphaMult);
        }

        GLStateUtil.resetColor();
    }

    private void renderShadow(float centerX, float centerY, float alphaMult) {
        float shadowY = centerY - COIN_SIZE / 2f - COIN_SHADOW_OFFSET;
        float shadowW = COIN_SIZE * shadowScale * 0.8f;
        float shadowH = COIN_SIZE * 0.15f * shadowScale;

        GLStateUtil.resetBlendState();

        float[] c = new float[] { 0f, 0f, 0f, shadowAlpha * alphaMult * 0.5f };
        GL11.glColor4f(c[0], c[1], c[2], c[3]);

        drawEllipse(centerX - shadowW / 2f, shadowY - shadowH / 2f, shadowW, shadowH, 16);

        GLStateUtil.resetColor();
    }

    private void renderCoin(float centerX, float centerY, float alphaMult) {
        SpriteAPI atkIcon = CosmiconSprites.getAtkIcon();
        SpriteAPI defIcon = CosmiconSprites.getDefIcon();
        if (atkIcon == null || defIcon == null) return;

        float size = COIN_SIZE * coinScale;
        float halfSize = size / 2f;

        float absSquash = Math.abs(coinSquash);
        boolean showAttack = coinSquash >= 0f;

        GLStateUtil.enableTexturingWithBlend();

        GL11.glPushMatrix();
        GL11.glTranslatef(centerX, centerY, 0f);
        GL11.glRotatef(coinRotation, 0f, 0f, 1f);
        GL11.glScalef(absSquash, 1f, 1f);

        SpriteAPI faceSprite = showAttack ? atkIcon : defIcon;
        faceSprite.setSize(size, size);
        faceSprite.setAlphaMult(alphaMult);
        faceSprite.render(-halfSize, -halfSize);

        GL11.glPopMatrix();

        GLStateUtil.disableTexturing();

        renderCoinBorder(centerX, centerY, size, absSquash, alphaMult);
    }

    private void renderCoinBorder(float centerX, float centerY, float size, float absSquash, float alphaMult) {
        GLStateUtil.resetBlendState();

        float[] c = new float[] { 0.9f, 0.85f, 0.6f, 0.8f * alphaMult };
        GL11.glColor4f(c[0], c[1], c[2], c[3]);
        GL11.glLineWidth(3f);

        int segments = 32;
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i <= segments; i++) {
            float angle = 2f * (float) Math.PI * i / segments;
            float x = centerX + (float) Math.cos(angle) * size / 2f * absSquash;
            float y = centerY + (float) Math.sin(angle) * size / 2f;
            GL11.glVertex2f(x, y);
        }
        GL11.glEnd();

        GLStateUtil.resetColor();
    }

    private void renderResultText(float centerX, float centerY, float alphaMult) {
    }

    private void drawEllipse(float x, float y, float w, float h, int segments) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(x + w / 2f, y + h / 2f);
        for (int i = 0; i <= segments; i++) {
            float angle = 2f * (float) Math.PI * i / segments;
            GL11.glVertex2f(x + w / 2f + (float) Math.cos(angle) * w / 2f,
                            y + h / 2f + (float) Math.sin(angle) * h / 2f);
        }
        GL11.glEnd();
    }

    public float getResultTextAlpha() {
        return resultTextAlpha;
    }

    public float getCoinScale() {
        return coinScale;
    }
}
