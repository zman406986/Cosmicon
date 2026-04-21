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
    
    public static float getTotalDuration() {
        return TOTAL_DURATION;
    }
    
    public float getDisplaySize() {
        return type != null ? type.getDisplaySize() : DICE_SIZE;
    }
    
    public DiceAnimator() {
        scale = 1f;
        complete = false;
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
    }
    
    public void reroll(int newFinalValue) {
        this.finalValue = newFinalValue;
        this.elapsed = 0f;
        this.scale = 1f;
        this.complete = false;
        this.currentFrame = 0;
        this.bounceAmplitude = 0f;
    }
    
    public void advance(float amount) {
        if (complete) return;
        
        elapsed += amount;
        
        if (elapsed < 0f) return;
        
        if (elapsed < ROLL_DURATION) {
            float rollProgress = elapsed / ROLL_DURATION;
            currentFrame = (int)(rollProgress * 6f * 48f) % 48;
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
        
        DiceSpriteRenderer.render(sprite, renderX, renderY, alphaMult, scale, displaySize);
        
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
}