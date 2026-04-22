package data.scripts.cosmicon.battle;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;

import data.scripts.cosmicon.util.CoordHelper;
import data.scripts.cosmicon.util.GLStateUtil;

public class DiceAnimator {
    public static final float DICE_SIZE = 80f;
    
    private static final float ROLL_DURATION = 6.0f;
    private static final float SETTLE_DURATION = 0.15f;
    private static final float REVEAL_DURATION = 0.1f;
    private static final float TOTAL_DURATION = ROLL_DURATION + SETTLE_DURATION + REVEAL_DURATION;
    private static final float SCALE_BOUNCE = 0.08f;
    
    private static final float DROP_DURATION = 0.4f;
    private static final float TRAVEL_SPEED = 300f;
    private static final float BOUNCE_DURATION = 0.15f;
    private static final float INITIAL_SCALE = 1.5f;
    private static final int FRAME_COUNT = 48;
    
    private enum Phase { ROLLING, DROP, TRAVEL, BOUNCE, SETTLE, REVEAL, COMPLETE }
    
    private DiceType type;
    private int finalValue;
    private float x;
    private float y;
    private float elapsed;
    private float scale;
    private boolean complete;
    
    private int currentFrame;
    private CustomPanelAPI panel;
    
    private float bounceAmplitude = 0f;
    private float posXOffset = 0f;
    private float posYOffset = 0f;
    
    private float rotation;
    private float directionRad;
    private float travelDistance;
    private float travelProgress;
    
    private int bounceCount;
    private float[] bounceHeights;
    
    private Phase phase;
    private int currentBounce;
    private float phaseElapsed;
    
    private boolean useDirectionalAnimation;
    
    public static float getTotalDuration() {
        return TOTAL_DURATION;
    }
    
    public float getDisplaySize() {
        return type != null ? type.getDisplaySize() : DICE_SIZE;
    }
    
    public DiceAnimator() {
        scale = 1f;
        complete = false;
        useDirectionalAnimation = false;
        bounceCount = 0;
        bounceHeights = new float[0];
        travelDistance = 0f;
        travelProgress = 0f;
        rotation = 0f;
        directionRad = 0f;
        phase = Phase.ROLLING;
        currentBounce = 0;
        phaseElapsed = 0f;
    }
    
    public void init(CustomPanelAPI panel) {
        this.panel = panel;
    }
    
public void start(DiceType type, int finalValue, float x, float y, float delay) {
        this.type = type;
        this.finalValue = finalValue;
        this.x = x;
        this.y = y;
        this.elapsed = -delay;
        this.scale = 1f;
        this.complete = false;
        this.currentFrame = 0;
        this.bounceAmplitude = 0f;
        this.posXOffset = 0f;
        this.posYOffset = 0f;
        this.useDirectionalAnimation = false;
        this.phase = Phase.ROLLING;
    }
    
    public void start(DiceType type, int finalValue, float x, float y, float delay,
                      float rotation, float travelDistance, int bounceCount, float[] bounceHeights) {
        this.type = type;
        this.finalValue = finalValue;
        this.x = x;
        this.y = y;
        this.elapsed = -delay;
        this.complete = false;
        this.currentFrame = 0;
        this.bounceAmplitude = 0f;
        this.posXOffset = 0f;
        this.posYOffset = 0f;
        
        this.rotation = rotation;
        this.directionRad = (float)Math.toRadians(rotation);
        this.travelDistance = travelDistance;
        this.travelProgress = 0f;
        this.bounceCount = bounceCount;
        this.bounceHeights = bounceHeights;
        this.currentBounce = 0;
        this.phase = Phase.DROP;
        this.phaseElapsed = 0f;
        this.scale = INITIAL_SCALE;
        this.useDirectionalAnimation = true;
    }
    
    public void reroll(int newFinalValue) {
        this.finalValue = newFinalValue;
        this.elapsed = 0f;
        this.scale = 1f;
        this.complete = false;
        this.currentFrame = 0;
        this.bounceAmplitude = 0f;
        this.phaseElapsed = 0f;
        if (useDirectionalAnimation) {
            this.phase = Phase.DROP;
            this.scale = INITIAL_SCALE;
        } else {
            this.phase = Phase.ROLLING;
        }
    }
    
    public void advance(float amount) {
        if (complete) return;
        
        elapsed += amount;
        
        if (elapsed < 0f) return;
        
        if (useDirectionalAnimation) {
            advanceDirectional(amount);
        } else {
            advanceSimple();
        }
    }
    
    private void advanceDirectional(float amount) {
        phaseElapsed += amount;
        
        switch (phase) {
            case DROP -> advanceDrop();
            case TRAVEL -> advanceTravel(amount);
            case BOUNCE -> advanceBounce();
            case SETTLE -> advanceSettle();
            case REVEAL -> advanceReveal();
            case COMPLETE -> complete = true;
        }
    }
    
    private void advanceDrop() {
        scale = INITIAL_SCALE - (0.5f * phaseElapsed / DROP_DURATION);
        currentFrame = (int)(phaseElapsed * 12f) % FRAME_COUNT;
        
        if (phaseElapsed >= DROP_DURATION) {
            scale = 1f;
            phase = Phase.TRAVEL;
            phaseElapsed = 0f;
        }
    }
    
    private void advanceTravel(float amount) {
        travelProgress += amount * TRAVEL_SPEED / travelDistance;
        phaseElapsed += amount;
        
        // Calculate total frames needed for smooth cycling (min 2 cycles)
        float travelDuration = travelDistance / TRAVEL_SPEED;
        int cycles = Math.max(2, (int)(travelDuration / 0.5f));
        int totalFrames = cycles * FRAME_COUNT - 1; // -1 so we land on 47 at end
        
        // Frame cycles smoothly: 0→47→0→47...
        int rawFrame = (int)(travelProgress * totalFrames);
        currentFrame = rawFrame % FRAME_COUNT;
        
        posXOffset = (float)Math.cos(directionRad) * travelDistance * Math.min(travelProgress, 1f);
        posYOffset = (float)Math.sin(directionRad) * travelDistance * Math.min(travelProgress, 1f);
        
        if (travelProgress >= 1.0f) {
            travelProgress = 1.0f;
            currentFrame = FRAME_COUNT - 1; // Force final frame 47
            posXOffset = (float)Math.cos(directionRad) * travelDistance;
            posYOffset = (float)Math.sin(directionRad) * travelDistance;
            
            if (bounceCount > 0) {
                phase = Phase.BOUNCE;
                currentBounce = 0;
            } else {
                phase = Phase.SETTLE;
            }
            phaseElapsed = 0f;
        }
    }
    
    private void advanceBounce() {
        if (currentBounce >= bounceHeights.length) {
            phase = Phase.SETTLE;
            phaseElapsed = 0f;
            return;
        }
        
        float bounceHeight = bounceHeights[currentBounce];
        float bounceProgress = phaseElapsed / BOUNCE_DURATION;
        scale = 1f + (bounceHeight - 1f) * (float)Math.sin(bounceProgress * Math.PI);
        currentFrame = 47;
        
        if (phaseElapsed >= BOUNCE_DURATION) {
            currentBounce++;
            phaseElapsed = 0f;
            
            if (currentBounce >= bounceCount) {
                phase = Phase.SETTLE;
                scale = 1f;
            }
        }
    }
    
    private void advanceSettle() {
        currentFrame = 47;
        scale = 1f;
        
        if (phaseElapsed >= SETTLE_DURATION) {
            phase = Phase.REVEAL;
            phaseElapsed = 0f;
        }
    }
    
    private void advanceReveal() {
        currentFrame = 47;
        float revealProgress = phaseElapsed / REVEAL_DURATION;
        scale = 1f + SCALE_BOUNCE * (float)Math.sin(revealProgress * Math.PI);
        
        if (phaseElapsed >= REVEAL_DURATION) {
            scale = 1f;
            phase = Phase.COMPLETE;
            complete = true;
        }
    }
    
    private void advanceSimple() {
        if (elapsed < ROLL_DURATION) {
            float rollProgress = elapsed / ROLL_DURATION;
            currentFrame = (int)(rollProgress * 6f * FRAME_COUNT) % FRAME_COUNT;
            scale = 1f;
        } else if (elapsed < ROLL_DURATION + SETTLE_DURATION) {
            currentFrame = 47;
            scale = 1f;
        } else if (elapsed < TOTAL_DURATION) {
            currentFrame = 47;
            float revealProgress = (elapsed - ROLL_DURATION - SETTLE_DURATION) / REVEAL_DURATION;
            scale = 1f + SCALE_BOUNCE * (float)Math.sin(revealProgress * Math.PI);
        } else {
            currentFrame = 47;
            scale = 1f;
            complete = true;
        }
    }
    
    public void render(float panelX, float panelY, float panelHeight, float alphaMult) {
        if (elapsed < 0f) return;
        
        SpriteAPI sprite = DiceSpriteRegistry.getFrame(type, finalValue, currentFrame);
        if (sprite == null) return;
        
        GLStateUtil.resetBlendState();
        
        float displaySize = getDisplaySize();
        float glBaseY = CoordHelper.uiTopLeftToGlSpriteY(panelY, panelHeight, y + posYOffset, displaySize);
        float extraHeight = displaySize * (scale - 1f);
        float renderY = glBaseY - extraHeight / 2f;
        
        float renderX = panelX + x + posXOffset;
        float extraWidth = displaySize * (scale - 1f);
        renderX -= extraWidth / 2f;
        
        float visualRotation = 180f - rotation; // Reflect across vertical axis for visual alignment
        DiceSpriteRenderer.render(sprite, renderX, renderY, alphaMult, scale, displaySize, visualRotation);
        
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }
    
    protected float calculateBounceScale(float elapsed) {
        return 1f;
    }
    
    protected float[] calculateRollPosition(float elapsed) {
        return new float[]{0f, 0f};
    }
    
    public boolean isComplete() {
        return complete;
    }
    
    public boolean isActive() {
        return elapsed >= 0f && !complete;
    }
    
    public void forceComplete() {
        elapsed = TOTAL_DURATION;
        currentFrame = 47;
        scale = 1f;
        complete = true;
        phase = Phase.COMPLETE;
    }
    
    public float getX() {
        return x;
    }
    
    public float getY() {
        return y;
    }
    
    public float getScale() {
        return scale;
    }
    
    public int getFinalValue() {
        return finalValue;
    }
    
    public float getRotation() {
        return rotation;
    }
    
    public float getPosXOffset() {
        return posXOffset;
    }
    
    public float getPosYOffset() {
        return posYOffset;
    }
}