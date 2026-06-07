package data.scripts.cosmicon.battle;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.graphics.SpriteAPI;

import data.scripts.cosmicon.util.UnifiedCoord;
import data.scripts.cosmicon.util.EasingUtil;
import data.scripts.cosmicon.util.GLStateUtil;

public class DiceAnimator {
    private static final float SETTLE_DURATION = 0.15f;
    private static final float REVEAL_DURATION = 0.1f;
    private static final float TOTAL_DURATION = 6.75f;
    private static final float SCALE_BOUNCE = 0.08f;
    
    private static final float DROP_DURATION = 0.4f;
    private static final float INITIAL_SCALE = 1.5f;
    private static final float[] TRAVEL_DISTANCES = AnimationConstants.TRAVEL_DISTANCES;
    private static final float BASE_DURATION_PER_CYCLE = AnimationConstants.BASE_DURATION_PER_CYCLE;
    
    private static final float PICKUP_DURATION = 0.4f;
    private static final float CENTERING_TRAVEL_DURATION = 0.6f;
    private static final float CENTERING_DROP_DURATION = 0.4f;
    private static final float CENTERING_SCALE = 1.4f;
    private static final float ROLL_PICKUP_DURATION = 0.3f;
    
    private static final float SCATTER_PICKUP_DURATION = 0.4f;
    private static final float SCATTER_TRAVEL_DURATION = 0.6f;
    private static final float SCATTER_DROP_DURATION = 0.4f;
    
    private static final float TRAVEL_TO_REST_DURATION = 0.5f;
    private static final float REST_DROP_DURATION = 0.3f;
    private static final float VALUE_CHANGE_DURATION = 0.4f;
    private static final float VALUE_CHANGE_SCALE = 1.3f;
    
    private enum Phase { STATIONARY_PREVIEW, SCATTER_PICKUP, SCATTER_TRAVEL, SCATTER_DROP,
                         ROLL_PICKUP, DROP, TRAVEL, SETTLE, REVEAL,
                         WAITING_FOR_CENTERING, PICKUP, CENTERING_TRAVEL, CENTERING_DROP,
                         TRAVEL_TO_REST, REST_DROP, RESTING, VALUE_CHANGE, COMPLETE }
    
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
    private float scatterTargetX;
    private float scatterTargetY;
    
    private float restStartX;
    private float restStartY;
    
    private int bounceCount;
    private float[] bounceHeights;
    private float bounceScale;
    private float bounceYOffset;
    
    private Phase phase;
    private float phaseElapsed;

    private int stationaryFrameIndex;
    private int stationaryResultIndex;
    private float rollPickupStartScale;

    private boolean reserve;

    private StatusEffectProcessor.StatusEffect diceEffect;
    
    public static float getTotalDuration() {
        return TOTAL_DURATION;
    }
    
    public float getDisplaySize() {
        return type != null ? type.getDisplaySize() : AnimationConstants.DICE_SIZE;
    }

    public void setDiceEffect(StatusEffectProcessor.StatusEffect effect) {
        this.diceEffect = effect;
    }
    
public DiceAnimator() {
        resetCommonFields();
        scale = 1f;
        directionRad = 0f;
        bounceCount = 0;
        bounceHeights = new float[0];
        bounceScale = 1f;
        bounceYOffset = 0f;
        travelDistance = 0f;
        travelProgress = 0f;
        rotation = 0f;
        phase = Phase.COMPLETE;
        diceEffect = null;
    }

    private void resetCommonFields() {
        complete = false;
        currentFrame = 0;
        posXOffset = 0f;
        posYOffset = 0f;
        phaseElapsed = 0f;
    }

    private void applyDirectional(float rotation, float travelDistance, int bounceCount,
                                   float[] bounceHeights) {
        this.rotation = rotation;
        this.directionRad = (float) Math.toRadians(rotation);
        this.travelDistance = travelDistance;
        this.bounceCount = bounceCount;
        this.bounceHeights = bounceHeights;
        this.bounceScale = 1f;
        this.bounceYOffset = 0f;
    }
    
    public void rerollWithNewPath(int newFinalValue, float startX, float startY,
                                   float rotation, float travelDistance, 
                                   int bounceCount, float[] bounceHeights,
                                   float targetCenterX, float targetCenterY) {
        this.finalValue = newFinalValue;

        this.scatterTargetX = startX;
        this.scatterTargetY = startY;

        this.elapsed = 0f;
        resetCommonFields();
        this.travelProgress = 0f;

        applyDirectional(rotation, travelDistance, bounceCount, bounceHeights);
        this.phase = Phase.SCATTER_PICKUP;
        this.scale = 1f;
        this.stationaryFrameIndex = AnimationConstants.FRAME_COUNT - 1;
        if (type == DiceType.PRISMATIC) {
            this.stationaryResultIndex = (int)(Math.random() * 6);
        } else if (type != null) {
            this.stationaryResultIndex = type.getMaxFace();
        }
        this.targetCenterX = targetCenterX;
        this.targetCenterY = targetCenterY;
    }
    
    public void startStationaryPreview(DiceType type, int result, float targetCenterX, float targetCenterY) {
        this.type = type;
        this.finalValue = result;
        resetCommonFields();
        this.stationaryFrameIndex = AnimationConstants.FRAME_COUNT - 1;
        if (type == DiceType.PRISMATIC) {
            this.stationaryResultIndex = (int)(Math.random() * 6);
        } else {
            this.stationaryResultIndex = type.getMaxFace();
        }
        this.x = targetCenterX;
        this.y = targetCenterY;
        this.scale = 1.0f;
        this.phase = Phase.STATIONARY_PREVIEW;
        this.elapsed = 0f;
        this.rotation = 0f;
        this.directionRad = 0f;
        this.travelDistance = 0f;
        this.travelProgress = 0f;
        this.bounceCount = 0;
        this.bounceHeights = new float[0];
        this.bounceScale = 1f;
        this.bounceYOffset = 0f;
        this.targetCenterX = targetCenterX;
        this.targetCenterY = targetCenterY;
    }
    
    

public void startScatterFromPreview(float scatterX, float scatterY, float delay,
                                          float rotation, float travelDistance, int bounceCount,
                                          float[] bounceHeights, float targetCenterX, float targetCenterY) {
        this.scatterTargetX = scatterX;
        this.scatterTargetY = scatterY;
        this.targetCenterX = targetCenterX;
        this.targetCenterY = targetCenterY;
        applyDirectional(rotation, travelDistance, bounceCount, bounceHeights);
        this.rollPickupStartScale = scale;
        resetCommonFields();
        this.elapsed = -delay;
        this.phase = Phase.SCATTER_PICKUP;
    }
    
    
    
    public void startFromScatterPosition(DiceType type, int finalValue,
                                          float scatterX, float scatterY, float delay,
                                          float rotation, float travelDistance, int bounceCount,
                                          float[] bounceHeights, float targetCenterX, float targetCenterY) {
        this.type = type;
        this.finalValue = finalValue;
        this.x = scatterX;
        this.y = scatterY;
        this.scatterTargetX = scatterX;
        this.scatterTargetY = scatterY;
        this.targetCenterX = targetCenterX;
        this.targetCenterY = targetCenterY;
        this.elapsed = -delay;
        resetCommonFields();
        applyDirectional(rotation, travelDistance, bounceCount, bounceHeights);
        this.phase = Phase.DROP;
        this.scale = INITIAL_SCALE;
        this.stationaryFrameIndex = AnimationConstants.FRAME_COUNT - 1;
        if (type == DiceType.PRISMATIC) {
            this.stationaryResultIndex = (int)(Math.random() * 6);
        } else {
            this.stationaryResultIndex = 1 + (int)(Math.random() * type.getMaxFace());
        }
    }
    
    public void advance(float amount) {
        if (complete) return;
        
        elapsed += amount;
        
        if (elapsed < 0f) return;
        
        advanceDirectional(amount);
    }
    
    private void advanceDirectional(float amount) {
        phaseElapsed += amount;
        
        switch (phase) {
            case STATIONARY_PREVIEW, WAITING_FOR_CENTERING, RESTING -> { }
            case SCATTER_PICKUP -> advanceScatterPickup();
            case SCATTER_TRAVEL -> advanceScatterTravel();
            case SCATTER_DROP -> advanceScatterDrop();
            case ROLL_PICKUP -> advanceRollPickup();
            case DROP -> advanceDrop();
            case TRAVEL -> advanceTravel();
            case SETTLE -> advanceSettle();
            case REVEAL -> advanceReveal();
            case PICKUP -> advancePickup();
            case CENTERING_TRAVEL -> advanceCenteringTravel();
            case CENTERING_DROP -> advanceCenteringDrop();
            case TRAVEL_TO_REST -> advanceTravelToRest();
            case REST_DROP -> advanceRestDrop();
            case VALUE_CHANGE -> advanceValueChange();
            case COMPLETE -> complete = true;
        }
    }
    
    private void advanceRollPickup() {
        currentFrame = stationaryFrameIndex;
        float progress = Math.min(1f, phaseElapsed / ROLL_PICKUP_DURATION);
        float eased = EasingUtil.easeInQuad(progress);
        scale = rollPickupStartScale + (CENTERING_SCALE - rollPickupStartScale) * eased;
        
        if (phaseElapsed >= ROLL_PICKUP_DURATION) {
            scale = INITIAL_SCALE;
            phase = Phase.DROP;
            phaseElapsed = 0f;
        }
    }
    
    private void advanceScatterPickup() {
        currentFrame = stationaryFrameIndex;
        float progress = Math.min(1f, phaseElapsed / SCATTER_PICKUP_DURATION);
        float eased = EasingUtil.easeOutQuad(progress);
        scale = 1f + (CENTERING_SCALE - 1f) * eased;
        
        if (phaseElapsed >= SCATTER_PICKUP_DURATION) {
            scale = CENTERING_SCALE;
            phase = Phase.SCATTER_TRAVEL;
            phaseElapsed = 0f;
        }
    }
    
    private void advanceScatterTravel() {
        currentFrame = stationaryFrameIndex;
        float progress = Math.min(1f, phaseElapsed / SCATTER_TRAVEL_DURATION);
        float eased = EasingUtil.easeInOutQuad(progress);
        posXOffset = (scatterTargetX - x) * eased;
        posYOffset = (scatterTargetY - y) * eased;
        
        if (phaseElapsed >= SCATTER_TRAVEL_DURATION) {
            posXOffset = scatterTargetX - x;
            posYOffset = scatterTargetY - y;
            phase = Phase.SCATTER_DROP;
            phaseElapsed = 0f;
        }
    }
    
    private void advanceScatterDrop() {
        float progress = Math.min(1f, phaseElapsed / SCATTER_DROP_DURATION);
        float eased = EasingUtil.easeInQuad(progress);
        scale = CENTERING_SCALE - (CENTERING_SCALE - 1f) * eased;
        
        if (phaseElapsed >= SCATTER_DROP_DURATION) {
            scale = 1f;
            x = scatterTargetX;
            y = scatterTargetY;
            posXOffset = 0f;
            posYOffset = 0f;
            phase = Phase.TRAVEL;
            phaseElapsed = 0f;
            travelProgress = 0f;
        }
    }
    
    private void advanceDrop() {
        scale = INITIAL_SCALE - (0.5f * phaseElapsed / DROP_DURATION);
        currentFrame = (int)(phaseElapsed * 12f) % AnimationConstants.FRAME_COUNT;
        
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
        currentFrame = (int)frameIndex % AnimationConstants.FRAME_COUNT;
        
        posXOffset = (float)Math.cos(directionRad) * travelDistance * travelProgress;
        posYOffset = (float)Math.sin(directionRad) * travelDistance * travelProgress;
        
        calculateBounceAtStart();
        scale = bounceScale;
        posYOffset += bounceYOffset;
        
        if (phaseElapsed >= travelDuration) {
            travelProgress = 1.0f;
            currentFrame = AnimationConstants.FRAME_COUNT - 1;
            posXOffset = (float)Math.cos(directionRad) * travelDistance;
            posYOffset = (float)Math.sin(directionRad) * travelDistance;
            scale = 1f;
            phase = Phase.SETTLE;
            phaseElapsed = 0f;
        }
    }
    
    private float calculateFrameIndexWithSlowdown(float travelDuration) {
        float progress = phaseElapsed / travelDuration;
        float currentFrameRate = AnimationConstants.FRAME_RATE_START - (AnimationConstants.FRAME_RATE_START - AnimationConstants.FRAME_RATE_END) * progress;
        float avgFrameRate = (AnimationConstants.FRAME_RATE_START + currentFrameRate) / 2f;
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
    
    private void calculateBounceAtStart() {
        if (bounceCount == 0) {
            bounceScale = 1f;
            bounceYOffset = 0f;
            return;
        }

        float bounceTotalTime = bounceCount * AnimationConstants.BOUNCE_DURATION;
        if (phaseElapsed >= bounceTotalTime) {
            bounceScale = 1f;
            bounceYOffset = 0f;
            return;
        }

        int bounceIndex = (int)(phaseElapsed / AnimationConstants.BOUNCE_DURATION);
        if (bounceIndex >= bounceHeights.length) {
            bounceScale = 1f;
            bounceYOffset = 0f;
            return;
        }

        float bounceLocalTime = phaseElapsed - bounceIndex * AnimationConstants.BOUNCE_DURATION;
        float bounceProgress = bounceLocalTime / AnimationConstants.BOUNCE_DURATION;
        float bounceHeight = bounceHeights[bounceIndex];
        float sinValue = (float)Math.sin(bounceProgress * Math.PI);

        bounceScale = 1f + (bounceHeight - 1f) * sinValue;
        float scaleDelta = bounceScale - 1f;
        bounceYOffset = -scaleDelta * getDisplaySize() * 0.5f;
    }
    
    private void advanceSettle() {
        currentFrame = AnimationConstants.FRAME_COUNT - 1;
        scale = 1f;
        
        if (phaseElapsed >= SETTLE_DURATION) {
            phase = Phase.REVEAL;
            phaseElapsed = 0f;
        }
    }
    
    private void advanceReveal() {
        currentFrame = AnimationConstants.FRAME_COUNT - 1;
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
        currentFrame = AnimationConstants.FRAME_COUNT - 1;
        float progress = phaseElapsed / PICKUP_DURATION;
        progress = Math.min(1f, progress);
        float eased = EasingUtil.easeOutQuad(progress);
        scale = 1f + (CENTERING_SCALE - 1f) * eased;
        
        if (phaseElapsed >= PICKUP_DURATION) {
            scale = CENTERING_SCALE;
            phase = Phase.CENTERING_TRAVEL;
            phaseElapsed = 0f;
        }
    }
    
    private void advanceCenteringTravel() {
        currentFrame = AnimationConstants.FRAME_COUNT - 1;
        float progress = phaseElapsed / CENTERING_TRAVEL_DURATION;
        progress = Math.min(1f, progress);
        float eased = EasingUtil.easeInOutQuad(progress);
        
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
        currentFrame = AnimationConstants.FRAME_COUNT - 1;
        float progress = phaseElapsed / CENTERING_DROP_DURATION;
        progress = Math.min(1f, progress);
        float eased = EasingUtil.easeInQuad(progress);
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
    

    public void render(float panelX, float panelY, float panelWidth, float panelHeight, float alphaMult) {
        if (elapsed < 0f) return;
        
        SpriteAPI sprite;
        boolean isScatterPickupOrTravel = phase == Phase.SCATTER_PICKUP || phase == Phase.SCATTER_TRAVEL || phase == Phase.SCATTER_DROP;
        boolean isRestPhase = isRestOrTravel();
        if (phase == Phase.STATIONARY_PREVIEW || isScatterPickupOrTravel || isRestPhase) {
            if (type == DiceType.PRISMATIC) {
                int faceIdx = stationaryResultIndex;
                if (faceIdx < 0 || faceIdx >= 6) faceIdx = 0;
                sprite = DiceSpriteRegistry.getPrismaticFrame(faceIdx, stationaryFrameIndex);
            } else if (type != null) {
                int resultIdx = stationaryResultIndex;
                if (resultIdx <= 0) resultIdx = type.getMaxFace();
                sprite = DiceSpriteRegistry.getFrame(type, resultIdx, stationaryFrameIndex);
            } else {
                return;
            }
        } else if (type == DiceType.PRISMATIC) {
            sprite = DiceSpriteRegistry.getPrismaticFrame(finalValue, currentFrame);
        } else if (type != null) {
            sprite = DiceSpriteRegistry.getFrame(type, finalValue, currentFrame);
        } else {
            return;
        }
        if (sprite == null) return;
        
        GLStateUtil.resetBlendState();
        
        float displaySize = getDisplaySize();
        float centeringOffset = (AnimationConstants.DICE_SIZE - displaySize) / 2f;
        
        boolean isSettledPhase = phase == Phase.PICKUP || phase == Phase.CENTERING_TRAVEL || 
                                  phase == Phase.CENTERING_DROP ||
                                  phase == Phase.SCATTER_PICKUP || phase == Phase.SCATTER_TRAVEL ||
                                  phase == Phase.SCATTER_DROP || phase == Phase.TRAVEL_TO_REST ||
                                  phase == Phase.REST_DROP || phase == Phase.RESTING ||
                                  phase == Phase.VALUE_CHANGE;
        
        // Use existing context if available (parent already set it), otherwise create one
        UnifiedCoord.PanelContext existingCtx = UnifiedCoord.getCurrentOrNull();
        boolean needsContextCleanup = existingCtx == null;
        
        if (needsContextCleanup) {
            UnifiedCoord.setCurrent(new UnifiedCoord.PanelContext(panelX, panelY, panelWidth, panelHeight));
        }
        try {
            UnifiedCoord dicePos = new UnifiedCoord(x + posXOffset + centeringOffset, y + posYOffset + centeringOffset);
            
            float extraHeight = displaySize * (scale - 1f);
            float extraWidth = displaySize * (scale - 1f);
            float renderX = dicePos.glX() - extraWidth / 2f;
            float renderY = dicePos.glSpriteY(displaySize) - extraHeight / 2f;
            if (type == DiceType.BLUE_D4) {
                renderY -= 5f;
            }

            float visualRotation = getVisualRotation(isSettledPhase);
            DiceSpriteRenderer.render(sprite, renderX, renderY, alphaMult, scale, displaySize, visualRotation);

            if (diceEffect != null) {
                boolean isUp = (diceEffect == StatusEffectProcessor.StatusEffect.ARISE);
                float arrowSize = 10f;
                float arrowX = renderX + displaySize - arrowSize - 2f;
                float arrowY = renderY + 2f;
                BattleRenderingUtils.renderIndicatorArrow(arrowX, arrowY, isUp, arrowSize, arrowSize, alphaMult);
            }
        } finally {
            if (needsContextCleanup) {
                UnifiedCoord.clearCurrent();
            }
        }
        
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    private float getVisualRotation(boolean isSettledPhase)
    {
        float visualRotation;
        if (phase == Phase.STATIONARY_PREVIEW) {
            visualRotation = 0f;
        } else if (phase == Phase.WAITING_FOR_CENTERING) {
            visualRotation = 180f - rotation;
            if (type == DiceType.BLUE_D4) {
                visualRotation -= 90f;
            }
        } else if (isSettledPhase || phase == Phase.COMPLETE) {
            visualRotation = 0f;
        } else {
            visualRotation = 180f - rotation;
            if (type == DiceType.BLUE_D4) {
                visualRotation -= 90f;
            }
        }
        return visualRotation;
    }

    public boolean isRunning() {
        return !complete;
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
    
    public void startTravelToRestFrom(DiceAnimator source, DiceType diceType, int value,
                                        float targetX, float targetY) {
        this.type = diceType;
        this.finalValue = value;
        this.x = source.getX() + source.getPosXOffset();
        this.y = source.getY() + source.getPosYOffset();
        this.restStartX = x;
        this.restStartY = y;
        this.targetCenterX = targetX;
        this.targetCenterY = targetY;
        this.phase = Phase.TRAVEL_TO_REST;
        resetCommonFields();
        this.elapsed = 0f;
        this.scale = source.getScale();
        this.stationaryFrameIndex = AnimationConstants.FRAME_COUNT - 1;
        this.stationaryResultIndex = value;
    }
    
    public void startFromRestPosition(DiceType type, int finalValue,
                                       float restX, float restY,
                                       float scatterX, float scatterY,
                                       float delay,
                                       float rotation, float travelDistance, int bounceCount,
                                       float[] bounceHeights, float targetCenterX, float targetCenterY) {
        this.type = type;
        this.finalValue = finalValue;
        this.x = restX;
        this.y = restY;
        this.scatterTargetX = scatterX;
        this.scatterTargetY = scatterY;
        this.targetCenterX = targetCenterX;
        this.targetCenterY = targetCenterY;
        this.elapsed = -delay;
        resetCommonFields();
        applyDirectional(rotation, travelDistance, bounceCount, bounceHeights);
        this.phase = Phase.SCATTER_PICKUP;
        this.scale = 1f;
        this.stationaryFrameIndex = AnimationConstants.FRAME_COUNT - 1;
        this.stationaryResultIndex = finalValue;
        this.rollPickupStartScale = 1f;
    }
    
    public void animateValueChange(int newValue) {
        this.finalValue = newValue;
        this.phase = Phase.VALUE_CHANGE;
        resetCommonFields();
        this.stationaryResultIndex = newValue;
    }
    
    private void advanceTravelToRest() {
        currentFrame = stationaryFrameIndex;
        float progress = Math.min(1f, phaseElapsed / TRAVEL_TO_REST_DURATION);
        float eased = EasingUtil.easeInOutQuad(progress);
        
        float currentX = restStartX + (targetCenterX - restStartX) * eased;
        float currentY = restStartY + (targetCenterY - restStartY) * eased;
        
        posXOffset = currentX - x;
        posYOffset = currentY - y;
        
        scale = 1f + (CENTERING_SCALE - 1f) * (float)Math.sin(progress * Math.PI);
        
        if (phaseElapsed >= TRAVEL_TO_REST_DURATION) {
            x = targetCenterX;
            y = targetCenterY;
            posXOffset = 0f;
            posYOffset = 0f;
            phase = Phase.REST_DROP;
            phaseElapsed = 0f;
        }
    }
    
    private void advanceRestDrop() {
        currentFrame = stationaryFrameIndex;
        float progress = Math.min(1f, phaseElapsed / REST_DROP_DURATION);
        float eased = EasingUtil.easeInQuad(progress);
        scale = CENTERING_SCALE - (CENTERING_SCALE - 1f) * eased;
        
        if (phaseElapsed >= REST_DROP_DURATION) {
            scale = 1f;
            phase = Phase.RESTING;
            phaseElapsed = 0f;
            complete = true;
        }
    }
    
    private void advanceValueChange() {
        float progress = Math.min(1f, phaseElapsed / VALUE_CHANGE_DURATION);
        
        if (progress < 0.3f) {
            float shrinkProgress = progress / 0.3f;
            scale = 1f - (1f - 0.7f) * EasingUtil.easeInQuad(shrinkProgress);
        } else if (progress < 0.5f) {
            float growProgress = (progress - 0.3f) / 0.2f;
            scale = 0.7f + (VALUE_CHANGE_SCALE - 0.7f) * EasingUtil.easeOutQuad(growProgress);
        } else {
            float settleProgress = (progress - 0.5f) / 0.5f;
            scale = VALUE_CHANGE_SCALE - (VALUE_CHANGE_SCALE - 1f) * EasingUtil.easeInQuad(settleProgress);
        }
        
        if (phaseElapsed >= VALUE_CHANGE_DURATION) {
            scale = 1f;
            phase = Phase.RESTING;
            phaseElapsed = 0f;
            complete = true;
        }
    }
    
    public boolean isAtRest() {
        return phase == Phase.RESTING;
    }
    
    public boolean isRestOrTravel() {
        return phase == Phase.RESTING || phase == Phase.TRAVEL_TO_REST || phase == Phase.REST_DROP || phase == Phase.VALUE_CHANGE;
    }
    
    public boolean isReserve() {
        return reserve;
    }
    
    public void setReserve(boolean reserve) {
        this.reserve = reserve;
    }
    
    public void forceComplete() {
        // Snap to target grid slot so visuals and hitboxes agree on position.
        // Callers that clear the list (clear/clearOpponentAnimators) are unaffected.
        x = targetCenterX;
        y = targetCenterY;

        elapsed = TOTAL_DURATION;
        currentFrame = AnimationConstants.FRAME_COUNT - 1;
        scale = 1f;
        posXOffset = 0f;
        posYOffset = 0f;
        rotation = 0f;
        directionRad = 0f;
        bounceCount = 0;
        bounceHeights = new float[0];
        bounceScale = 1f;
        bounceYOffset = 0f;
        travelDistance = 0f;
        travelProgress = 0f;
        stationaryFrameIndex = AnimationConstants.FRAME_COUNT - 1;
        stationaryResultIndex = 0;
        rollPickupStartScale = 0f;
        centeringStartX = 0f;
        centeringStartY = 0f;
        scatterTargetX = 0f;
        scatterTargetY = 0f;
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
    
    public float getVisualX() {
        float displaySize = getDisplaySize();
        float centeringOffset = (AnimationConstants.DICE_SIZE - displaySize) / 2f;
        return x + posXOffset + centeringOffset;
    }
    
    public float getVisualY() {
        float displaySize = getDisplaySize();
        float centeringOffset = (AnimationConstants.DICE_SIZE - displaySize) / 2f;
        return y + posYOffset + centeringOffset;
    }
    
    
    
    public float getTargetSlotX() {
        return targetCenterX;
    }
    
    public float getTargetSlotY() {
        return targetCenterY;
    }
    
    public float getPosXOffset() {
        return posXOffset;
    }
    
    public float getPosYOffset() {
        return posYOffset;
    }

    public void setType(DiceType newType) {
        this.type = newType;
    }
}