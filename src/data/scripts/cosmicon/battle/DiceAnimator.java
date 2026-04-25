package data.scripts.cosmicon.battle;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.graphics.SpriteAPI;

import data.scripts.cosmicon.util.CoordHelper;
import data.scripts.cosmicon.util.GLStateUtil;

public class DiceAnimator {
    public static final float DICE_SIZE = 80f;
    
    private static final float ROLL_DURATION = 6.0f;
    private static final float SETTLE_DURATION = 0.15f;
    private static final float REVEAL_DURATION = 0.1f;
    private static final float RETURN_DURATION = 0.5f;
    private static final float TOTAL_DURATION = ROLL_DURATION + SETTLE_DURATION + REVEAL_DURATION + RETURN_DURATION;
    private static final float SCALE_BOUNCE = 0.08f;
    
    private static final float DROP_DURATION = 0.4f;
    private static final float BOUNCE_DURATION = 0.3f;
    private static final float INITIAL_SCALE = 1.5f;
    private static final int FRAME_COUNT = 48;
    private static final float[] TRAVEL_DISTANCES = {150f, 250f, 350f};
    private static final float FRAME_RATE_START = 96f;
    private static final float FRAME_RATE_END = 24f;
    private static final float BASE_DURATION_PER_CYCLE = 0.8f;
    
    private static final float PICKUP_DURATION = 0.4f;
    private static final float CENTERING_TRAVEL_DURATION = 0.6f;
    private static final float CENTERING_DROP_DURATION = 0.4f;
    private static final float CENTERING_SCALE = 1.4f;
    private static final float ROLL_PICKUP_DURATION = 0.3f;
    
    private enum Phase { STATIONARY_PREVIEW, ROLL_PICKUP, ROLLING, DROP, TRAVEL, SETTLE, REVEAL, 
                         WAITING_FOR_CENTERING, PICKUP, CENTERING_TRAVEL, CENTERING_DROP, COMPLETE }
    
    private DiceType type;
    private int finalValue;
    private float x;
    private float y;
    private float elapsed;
    private float scale;
    private boolean complete;
    
    private int currentFrame;
    private float posXOffset = 0f;
    private float posYOffset = 0f;
    
    private float rotation;
    private float directionRad;
    private float travelDistance;
    private float travelProgress;
    
    private float centeringStartX;
    private float centeringStartY;
    private float targetCenterX;
    private float targetCenterY;
    
    private int bounceCount;
    private float[] bounceHeights;
    
    private Phase phase;
    private float phaseElapsed;
    
    private boolean useDirectionalAnimation;
    
    private int stationaryFrameIndex;
    private int stationaryResultIndex;
    private float rollPickupStartScale;
    
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
        phaseElapsed = 0f;
    }
    
    public void init() {
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
        this.posXOffset = 0f;
        this.posYOffset = 0f;
        this.useDirectionalAnimation = false;
        this.phase = Phase.ROLLING;
    }
    
    public void start(DiceType type, int finalValue, float x, float y, float delay,
                      float rotation, float travelDistance, int bounceCount, float[] bounceHeights,
                      float targetCenterX, float targetCenterY) {
        this.type = type;
        this.finalValue = finalValue;
        this.x = x;
        this.y = y;
        this.elapsed = -delay;
        this.complete = false;
        this.currentFrame = 0;
        this.posXOffset = 0f;
        this.posYOffset = 0f;
        
        this.rotation = rotation;
        this.directionRad = (float)Math.toRadians(rotation);
        this.travelDistance = travelDistance;
        this.bounceCount = bounceCount;
        this.bounceHeights = bounceHeights;
        this.phase = Phase.DROP;
        this.phaseElapsed = 0f;
        this.scale = INITIAL_SCALE;
        this.useDirectionalAnimation = true;
        this.targetCenterX = targetCenterX;
        this.targetCenterY = targetCenterY;
    }
    
    public void reroll(int newFinalValue) {
        this.finalValue = newFinalValue;
        this.elapsed = 0f;
        this.scale = 1f;
        this.complete = false;
        this.currentFrame = 0;
        this.phaseElapsed = 0f;
        this.travelProgress = 0f;
        this.posXOffset = 0f;
        this.posYOffset = 0f;
        if (useDirectionalAnimation) {
            this.phase = Phase.DROP;
            this.scale = INITIAL_SCALE;
        } else {
            this.phase = Phase.ROLLING;
        }
    }
    
    public void rerollWithNewPath(int newFinalValue, float startX, float startY,
                                   float rotation, float travelDistance, 
                                   int bounceCount, float[] bounceHeights,
                                   float targetCenterX, float targetCenterY) {
        this.finalValue = newFinalValue;
        
        float displaySize = type != null ? type.getDisplaySize() : DICE_SIZE;
        float centeringOffset = (DICE_SIZE - displaySize) / 2f;
        this.x = startX - centeringOffset;
        this.y = startY - centeringOffset;
        
        this.elapsed = 0f;
        this.complete = false;
        this.currentFrame = 0;
        this.phaseElapsed = 0f;
        this.travelProgress = 0f;
        this.posXOffset = 0f;
        this.posYOffset = 0f;
        
        this.rotation = rotation;
        this.directionRad = (float)Math.toRadians(rotation);
        this.travelDistance = travelDistance;
        this.bounceCount = bounceCount;
        this.bounceHeights = bounceHeights;
        this.useDirectionalAnimation = true;
        this.phase = Phase.DROP;
        this.scale = INITIAL_SCALE;
        this.targetCenterX = targetCenterX - centeringOffset;
        this.targetCenterY = targetCenterY - centeringOffset;
    }
    
    public void startStationaryPreview(DiceType type, int result, float targetCenterX, float targetCenterY) {
        this.type = type;
        this.finalValue = result;
        this.stationaryFrameIndex = 0;
        if (type == DiceType.PRISMATIC) {
            this.stationaryResultIndex = (int)(Math.random() * 6);
        } else {
            this.stationaryResultIndex = 1 + (int)(Math.random() * type.getMaxFace());
        }
        this.x = targetCenterX;
        this.y = targetCenterY;
        this.posXOffset = 0f;
        this.posYOffset = 0f;
        this.scale = 1.0f;
        this.phase = Phase.STATIONARY_PREVIEW;
        this.complete = false;
        this.phaseElapsed = 0f;
        this.elapsed = 0f;
        this.currentFrame = 0;
    }
    
    public void startRollFromStationary(float rotation, float travelDistance, int bounceCount, 
                                         float[] bounceHeights, float delay, float targetCenterX, float targetCenterY) {
        this.rotation = rotation;
        this.directionRad = (float)Math.toRadians(rotation);
        this.travelDistance = travelDistance;
        this.bounceCount = bounceCount;
        this.bounceHeights = bounceHeights;
        this.targetCenterX = targetCenterX;
        this.targetCenterY = targetCenterY;
        this.rollPickupStartScale = scale;
        this.phaseElapsed = 0f;
        this.elapsed = -delay;
        this.phase = Phase.ROLL_PICKUP;
        this.useDirectionalAnimation = true;
    }
    
    public void advance(float amount) {
        if (complete) return;
        
        elapsed += amount;
        
        if (elapsed < 0f) return;
        
        if (phase == Phase.STATIONARY_PREVIEW || useDirectionalAnimation) {
            advanceDirectional(amount);
        } else {
            advanceSimple();
        }
    }
    
    private void advanceDirectional(float amount) {
        phaseElapsed += amount;
        
        switch (phase) {
            case STATIONARY_PREVIEW -> { }
            case ROLL_PICKUP -> advanceRollPickup();
            case DROP -> advanceDrop();
            case TRAVEL -> advanceTravel();
            case SETTLE -> advanceSettle();
            case REVEAL -> advanceReveal();
            case WAITING_FOR_CENTERING -> { }
            case PICKUP -> advancePickup();
            case CENTERING_TRAVEL -> advanceCenteringTravel();
            case CENTERING_DROP -> advanceCenteringDrop();
            case COMPLETE -> complete = true;
        }
    }
    
    private void advanceRollPickup() {
        currentFrame = stationaryFrameIndex;
        float progress = Math.min(1f, phaseElapsed / ROLL_PICKUP_DURATION);
        float eased = easeInQuad(progress);
        scale = rollPickupStartScale + (CENTERING_SCALE - rollPickupStartScale) * eased;
        
        if (phaseElapsed >= ROLL_PICKUP_DURATION) {
            scale = INITIAL_SCALE;
            phase = Phase.DROP;
            phaseElapsed = 0f;
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
    
    private void advanceTravel() {
        float travelDuration = calculateTravelDuration();
        travelProgress = Math.min(1f, phaseElapsed / travelDuration);
        
        float frameIndex = calculateFrameIndexWithSlowdown(travelDuration);
        currentFrame = (int)frameIndex % FRAME_COUNT;
        
        posXOffset = (float)Math.cos(directionRad) * travelDistance * travelProgress;
        posYOffset = (float)Math.sin(directionRad) * travelDistance * travelProgress;
        
        scale = calculateBounceAtStart();
        
        if (phaseElapsed >= travelDuration) {
            travelProgress = 1.0f;
            currentFrame = FRAME_COUNT - 1;
            posXOffset = (float)Math.cos(directionRad) * travelDistance;
            posYOffset = (float)Math.sin(directionRad) * travelDistance;
            scale = 1f;
            phase = Phase.SETTLE;
            phaseElapsed = 0f;
        }
    }
    
    private float calculateFrameIndexWithSlowdown(float travelDuration) {
        float progress = phaseElapsed / travelDuration;
        float currentFrameRate = FRAME_RATE_START - (FRAME_RATE_START - FRAME_RATE_END) * progress;
        float avgFrameRate = (FRAME_RATE_START + currentFrameRate) / 2f;
        return phaseElapsed * avgFrameRate;
    }
    
    private float calculateTravelDuration() {
        int cycleCount = getCycleCountForDistance(travelDistance);
        return BASE_DURATION_PER_CYCLE * cycleCount;
    }
    
    private int getCycleCountForDistance(float distance) {
        for (int i = 0; i < TRAVEL_DISTANCES.length; i++) {
            if (distance <= TRAVEL_DISTANCES[i]) {
                return i + 1;
            }
        }
        return TRAVEL_DISTANCES.length;
    }
    
    private float calculateBounceAtStart() {
        if (bounceCount == 0) return 1f;
        
        float bounceTotalTime = bounceCount * BOUNCE_DURATION;
        if (phaseElapsed >= bounceTotalTime) return 1f;
        
        int bounceIndex = (int)(phaseElapsed / BOUNCE_DURATION);
        if (bounceIndex >= bounceHeights.length) return 1f;
        
        float bounceLocalTime = phaseElapsed - bounceIndex * BOUNCE_DURATION;
        float bounceProgress = bounceLocalTime / BOUNCE_DURATION;
        float bounceHeight = bounceHeights[bounceIndex];
        return 1f + (bounceHeight - 1f) * (float)Math.sin(bounceProgress * Math.PI);
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
            centeringStartX = x + posXOffset;
            centeringStartY = y + posYOffset;
            phase = Phase.WAITING_FOR_CENTERING;
            phaseElapsed = 0f;
        }
    }
    
    private void advancePickup() {
        currentFrame = 47;
        float progress = phaseElapsed / PICKUP_DURATION;
        progress = Math.min(1f, progress);
        float eased = easeOutQuad(progress);
        scale = 1f + (CENTERING_SCALE - 1f) * eased;
        
        if (phaseElapsed >= PICKUP_DURATION) {
            scale = CENTERING_SCALE;
            phase = Phase.CENTERING_TRAVEL;
            phaseElapsed = 0f;
        }
    }
    
    private void advanceCenteringTravel() {
        currentFrame = 47;
        float progress = phaseElapsed / CENTERING_TRAVEL_DURATION;
        progress = Math.min(1f, progress);
        float eased = easeInOutQuad(progress);
        
        float currentX = centeringStartX + (targetCenterX - centeringStartX) * eased;
        float currentY = centeringStartY + (targetCenterY - centeringStartY) * eased;
        
        posXOffset = currentX - x;
        posYOffset = currentY - y;
        
        if (phaseElapsed >= CENTERING_TRAVEL_DURATION) {
            x = targetCenterX;
            y = targetCenterY;
            posXOffset = 0f;
            posYOffset = 0f;
            phase = Phase.CENTERING_DROP;
            phaseElapsed = 0f;
        }
    }
    
    private void advanceCenteringDrop() {
        currentFrame = 0;
        float progress = phaseElapsed / CENTERING_DROP_DURATION;
        progress = Math.min(1f, progress);
        float eased = easeInQuad(progress);
        scale = CENTERING_SCALE - (CENTERING_SCALE - 1f) * eased;
        
        if (phaseElapsed >= CENTERING_DROP_DURATION) {
            scale = 1f;
            posXOffset = 0f;
            posYOffset = 0f;
            rotation = 0f;
            directionRad = 0f;
            phase = Phase.COMPLETE;
            complete = true;
        }
    }
    
    private float easeOutQuad(float t) {
        return 1f - (1f - t) * (1f - t);
    }
    
    private float easeInQuad(float t) {
        return t * t;
    }
    
    private float easeInOutQuad(float t) {
        return t < 0.5f ? 2f * t * t : 1f - (1f - t) * (1f - t) * 2f;
    }
    
    private void advanceSimple() {
        float returnStartTime = ROLL_DURATION + SETTLE_DURATION + REVEAL_DURATION;
        
        if (elapsed < ROLL_DURATION) {
            float rollProgress = elapsed / ROLL_DURATION;
            currentFrame = (int)(rollProgress * 6f * FRAME_COUNT) % FRAME_COUNT;
            scale = 1f;
        } else if (elapsed < ROLL_DURATION + SETTLE_DURATION) {
            currentFrame = 47;
            scale = 1f;
        } else if (elapsed < returnStartTime) {
            currentFrame = 47;
            float revealProgress = (elapsed - ROLL_DURATION - SETTLE_DURATION) / REVEAL_DURATION;
            scale = 1f + SCALE_BOUNCE * (float)Math.sin(revealProgress * Math.PI);
        } else if (elapsed < TOTAL_DURATION) {
            currentFrame = 0;
            scale = 1f;
        } else {
            currentFrame = 0;
            scale = 1f;
            complete = true;
        }
    }
    
    public void render(float panelX, float panelY, float panelHeight, float alphaMult) {
        if (elapsed < 0f) return;
        
        SpriteAPI sprite;
        if (phase == Phase.STATIONARY_PREVIEW) {
            if (type == DiceType.PRISMATIC) {
                sprite = DiceSpriteRegistry.getPrismaticFrame(stationaryResultIndex, stationaryFrameIndex);
            } else {
                sprite = DiceSpriteRegistry.getFrame(type, stationaryResultIndex, stationaryFrameIndex);
            }
        } else if (type == DiceType.PRISMATIC) {
            sprite = DiceSpriteRegistry.getPrismaticFrame(finalValue, currentFrame);
        } else {
            sprite = DiceSpriteRegistry.getFrame(type, finalValue, currentFrame);
        }
        if (sprite == null) return;
        
        GLStateUtil.resetBlendState();
        
        float displaySize = getDisplaySize();
        float centeringOffset = (DICE_SIZE - displaySize) / 2f;
        
        float renderX = panelX + x + posXOffset + centeringOffset;
        float renderY;
        
        boolean isCenteringPhase = phase == Phase.PICKUP || phase == Phase.CENTERING_TRAVEL || 
                                   phase == Phase.CENTERING_DROP;
        
        if (isCenteringPhase || phase == Phase.COMPLETE || (complete && useDirectionalAnimation)) {
            float glBaseY = CoordHelper.uiTopLeftToGlSpriteY(panelY, panelHeight, y + posYOffset + centeringOffset, displaySize);
            float extraHeight = displaySize * (scale - 1f);
            renderY = glBaseY - extraHeight / 2f;
        } else {
            float glBaseY = CoordHelper.uiTopLeftToGlSpriteY(panelY, panelHeight, y + posYOffset + centeringOffset, displaySize);
            float extraHeight = displaySize * (scale - 1f);
            renderY = glBaseY - extraHeight / 2f;
        }
        
        float extraWidth = displaySize * (scale - 1f);
        renderX -= extraWidth / 2f;
        
        float visualRotation;
        if (phase == Phase.STATIONARY_PREVIEW) {
            visualRotation = 0f;
        } else if (phase == Phase.WAITING_FOR_CENTERING) {
            visualRotation = 180f - rotation;
            if (type == DiceType.BLUE_D4) {
                visualRotation -= 90f;
            }
        } else if (isCenteringPhase || phase == Phase.COMPLETE || (complete && useDirectionalAnimation)) {
            visualRotation = 0f;
        } else if (complete) {
            visualRotation = 0f;
        } else {
            visualRotation = 180f - rotation;
            if (type == DiceType.BLUE_D4) {
                visualRotation -= 90f;
            }
        }
        DiceSpriteRenderer.render(sprite, renderX, renderY, alphaMult, scale, displaySize, visualRotation);
        
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }
    
    public boolean isComplete() {
        return complete;
    }
    
    public boolean isReadyForCentering() {
        return phase == Phase.WAITING_FOR_CENTERING;
    }
    
    public void startCenteringAnimation() {
        if (phase == Phase.WAITING_FOR_CENTERING) {
            phase = Phase.PICKUP;
            phaseElapsed = 0f;
        }
    }
    
    public boolean isActive() {
        return elapsed >= 0f && !complete;
    }
    
    public void forceComplete() {
        elapsed = TOTAL_DURATION;
        currentFrame = 0;
        scale = 1f;
        posXOffset = 0f;
        posYOffset = 0f;
        rotation = 0f;
        directionRad = 0f;
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
    
    public float getRotation() {
        return rotation;
    }

    public boolean isCenteredPhase() {
        return phase == Phase.PICKUP || phase == Phase.CENTERING_TRAVEL ||
               phase == Phase.CENTERING_DROP || phase == Phase.COMPLETE ||
               (complete && useDirectionalAnimation);
    }
    
    public float getVisualX() {
        float displaySize = getDisplaySize();
        float centeringOffset = (DICE_SIZE - displaySize) / 2f;
        return x + posXOffset + centeringOffset;
    }
    
    public float getVisualY() {
        float displaySize = getDisplaySize();
        float centeringOffset = (DICE_SIZE - displaySize) / 2f;
        return y + posYOffset + centeringOffset;
    }
    
    public boolean isStationaryPreview() {
        return phase == Phase.STATIONARY_PREVIEW;
    }
}