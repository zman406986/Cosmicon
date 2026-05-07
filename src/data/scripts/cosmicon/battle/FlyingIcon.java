package data.scripts.cosmicon.battle;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import data.scripts.cosmicon.util.UnifiedCoord;
import data.scripts.cosmicon.util.EasingUtil;
import data.scripts.cosmicon.util.GLStateUtil;

public class FlyingIcon {
    private static final float PULLBACK_DISTANCE = 40f;
    private static final float PULLBACK_DURATION = 0.4f;
    private static final float LABEL_HEIGHT = 35f;
    private static final float ROTATION_DURATION = 0.25f;
    
    private enum FlyPhase { IDLE, ROTATING, PULLBACK, READY, FLYING, COMPLETE }
    
    private float startX, startY;
    private float pullbackX, pullbackY;
    private float targetX, targetY;
    private float currentX, currentY;
    private float flyDuration;
    private float elapsed;
    private final float size;
    private FlyPhase flyPhase;
    private final SpriteAPI sprite;
    private int value;
    private final Color color;
    private boolean usePullback;
    private boolean useLinearFlight;
    private boolean autoLaunch;
    
    private float currentRotation = 0f;
    private float targetRotation = 0f;
    private float rotationElapsed = 0f;
    private float originalRotation = 0f;
    private float cachedLabelWidth = -1f;

    private LabelAPI valueLabel;
    private CustomPanelAPI labelPanel;
    private boolean labelCreated;
    
    public FlyingIcon(SpriteAPI sprite, float size, Color color, float startX, float startY) {
        this.sprite = sprite;
        this.size = size;
        this.color = color;
        this.flyPhase = FlyPhase.IDLE;
        this.elapsed = 0f;
        this.labelCreated = false;
        this.usePullback = false;
        this.autoLaunch = false;
        this.startX = startX;
        this.startY = startY;
        this.currentX = startX;
        this.currentY = startY;
    }
    
    public void setTargetRotation(float rotation) {
        this.targetRotation = rotation;
    }
    
    public float getRotation() {
        return currentRotation;
    }
    
    public void setRotation(float rotation) {
        this.currentRotation = rotation;
    }
    
    public void startRotation(float targetRotation) {
        if (Math.abs(targetRotation - currentRotation) > 0.5f) {
            this.originalRotation = currentRotation;
            this.targetRotation = targetRotation;
            this.rotationElapsed = 0f;
            flyPhase = FlyPhase.ROTATING;
        }
    }
    
    public boolean isRotating() {
        return flyPhase == FlyPhase.ROTATING;
    }
    
    public void flyTo(float x, float y, float duration, boolean useLinear, boolean skipPullback) {
        this.startX = currentX;
        this.startY = currentY;
        float dx = x - currentX;
        float dy = y - currentY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist > 0f) {
            pullbackX = currentX - (dx / dist) * PULLBACK_DISTANCE;
            pullbackY = currentY - (dy / dist) * PULLBACK_DISTANCE;
        } else {
            pullbackX = currentX;
            pullbackY = currentY;
        }
        if (skipPullback) {
            pullbackX = currentX;
            pullbackY = currentY;
        }
        this.targetX = x;
        this.targetY = y;
        this.flyDuration = duration;
        this.elapsed = 0f;
        this.rotationElapsed = 0f;
        this.usePullback = !skipPullback;
        this.useLinearFlight = useLinear;
        
        if (Math.abs(targetRotation - currentRotation) > 0.5f) {
            this.originalRotation = currentRotation;
            flyPhase = FlyPhase.ROTATING;
        } else if (skipPullback) {
            flyPhase = FlyPhase.READY;
        } else {
            flyPhase = FlyPhase.PULLBACK;
        }
    }
    
    public void flyDirectTo(float x, float y, float duration) {
        flyDirectTo(x, y, duration, false);
    }
    
    public void flyDirectTo(float x, float y, float duration, boolean useLinear) {
        this.startX = currentX;
        this.startY = currentY;
        this.pullbackX = currentX;
        this.pullbackY = currentY;
        this.targetX = x;
        this.targetY = y;
        this.flyDuration = duration;
        this.elapsed = 0f;
        this.rotationElapsed = 0f;
        this.usePullback = false;
        this.useLinearFlight = useLinear;
        
        if (Math.abs(targetRotation - currentRotation) > 0.5f) {
            this.originalRotation = currentRotation;
            flyPhase = FlyPhase.ROTATING;
        } else {
            flyPhase = FlyPhase.FLYING;
        }
    }
    
    public void drawbackThenLaunchTo(float targetX, float targetY,
                                     float drawbackDist, float drawbackDuration,
                                     float launchDuration, boolean useLinear) {
        float dx = targetX - currentX;
        float dy = targetY - currentY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist > 0f) {
            pullbackX = currentX - (dx / dist) * drawbackDist;
            pullbackY = currentY - (dy / dist) * drawbackDist;
        } else {
            pullbackX = currentX;
            pullbackY = currentY;
        }
        this.targetX = targetX;
        this.targetY = targetY;
        this.flyDuration = launchDuration;
        this.elapsed = 0f;
        this.usePullback = true;
        this.useLinearFlight = useLinear;
        this.autoLaunch = true;
        flyPhase = FlyPhase.PULLBACK;
    }
    
    public void setValue(int value) {
        this.value = value;
        this.cachedLabelWidth = -1f;
        updateLabelText();
    }
    
    private void updateLabelText() {
        if (valueLabel != null) {
            valueLabel.setText(String.valueOf(value));
        }
    }
    
    public void advance(float amount) {
        if (flyPhase == FlyPhase.COMPLETE || flyPhase == FlyPhase.IDLE) return;
        
        switch (flyPhase) {
            case ROTATING -> advanceRotation(amount);
            case PULLBACK -> {
                elapsed += amount;
                advancePullback();
            }
            case FLYING -> {
                elapsed += amount;
                advanceFlying();
            }
        }
        
        updateLabelPosition();
    }
    
    private void advanceRotation(float amount) {
        rotationElapsed += amount;
        
        if (rotationElapsed >= ROTATION_DURATION) {
            currentRotation = targetRotation;
            startX = currentX;
            startY = currentY;
            elapsed = 0f;
            if (usePullback) {
                flyPhase = FlyPhase.PULLBACK;
            } else {
                flyPhase = FlyPhase.FLYING;
            }
            return;
        }
        
        float progress = rotationElapsed / ROTATION_DURATION;
        float eased = EasingUtil.easeOutQuad(progress);
        currentRotation = this.originalRotation + (targetRotation - this.originalRotation) * eased;
    }
    
    private void advancePullback() {
        if (elapsed >= PULLBACK_DURATION) {
            currentX = pullbackX;
            currentY = pullbackY;
            startX = pullbackX;
            startY = pullbackY;
            elapsed = 0f;
            if (autoLaunch) {
                flyPhase = FlyPhase.FLYING;
                autoLaunch = false;
            } else {
                flyPhase = FlyPhase.READY;
            }
            return;
        }
        
        float progress = elapsed / PULLBACK_DURATION;
        float eased = EasingUtil.easeInQuad(progress);
        currentX = startX + (pullbackX - startX) * eased;
        currentY = startY + (pullbackY - startY) * eased;
    }
    
    public void startFlight() {
        if (flyPhase == FlyPhase.READY) {
            elapsed = 0f;
            flyPhase = FlyPhase.FLYING;
        }
    }
    
    private void advanceFlying() {
        if (elapsed >= flyDuration) {
            currentX = targetX;
            currentY = targetY;
            startX = targetX;
            startY = targetY;
            flyPhase = FlyPhase.COMPLETE;
            return;
        }
        
        float progress = elapsed / flyDuration;
        float eased = useLinearFlight ? progress : EasingUtil.easeOutQuad(progress);
        currentX = pullbackX + (targetX - pullbackX) * eased;
        currentY = pullbackY + (targetY - pullbackY) * eased;
    }
    
    public boolean isComplete() { return flyPhase == FlyPhase.COMPLETE; }

    public boolean isReady() { return flyPhase == FlyPhase.READY; }
    public boolean isFlying() { return flyPhase == FlyPhase.FLYING; }
    public float getX() { return currentX; }
    public float getY() { return currentY; }
    public float getSize() { return size; }
    public SpriteAPI getSprite() { return sprite; }
    public int getValue() { return value; }
    
    public void createLabel(CustomPanelAPI panel) {
        if (labelCreated || panel == null) return;
        
        SettingsAPI settings = Global.getSettings();
        String text = String.valueOf(value);
        valueLabel = settings.createLabel(text, Fonts.INSIGNIA_VERY_LARGE);
        valueLabel.setColor(color);
        valueLabel.setAlignment(Alignment.MID);
        
        float labelWidth = valueLabel.computeTextWidth(text) + 10f;
        this.cachedLabelWidth = labelWidth;
        panel.addComponent((UIComponentAPI) valueLabel)
            .setSize(labelWidth, LABEL_HEIGHT)
            .inTL(currentX - labelWidth / 2f, currentY - LABEL_HEIGHT / 2f);
        
        labelPanel = panel;
        labelCreated = true;
    }
    
    private void updateLabelPosition() {
        if (valueLabel != null && labelCreated) {
            if (cachedLabelWidth < 0f) {
                cachedLabelWidth = valueLabel.computeTextWidth(String.valueOf(value)) + 10f;
            }
            float labelWidth = cachedLabelWidth;
            ((UIComponentAPI) valueLabel).getPosition()
                .setSize(labelWidth, LABEL_HEIGHT)
                .inTL(currentX - labelWidth / 2f, currentY - LABEL_HEIGHT / 2f);
        }
    }
    
    public void setLabelOpacity(float alpha) {
        if (valueLabel != null) {
            valueLabel.setOpacity(alpha);
        }
    }
    
    public void cleanup() {
        if (valueLabel != null && labelPanel != null) {
            labelPanel.removeComponent((UIComponentAPI) valueLabel);
        }
        valueLabel = null;
        labelPanel = null;
        labelCreated = false;
    }
    
    public void render(float panelX, float panelY, float panelWidth, float panelHeight, float alphaMult) {
        if (sprite == null) return;
        
        UnifiedCoord.PanelContext existingCtx = UnifiedCoord.getCurrentOrNull();
        boolean needsContextCleanup = existingCtx == null;
        
        if (needsContextCleanup) {
            UnifiedCoord.setCurrent(new UnifiedCoord.PanelContext(panelX, panelY, panelWidth, panelHeight));
        }
        
        try {
            GLStateUtil.enableTexturingWithBlend();
            
            float uiX = currentX - size / 2f;
            float uiY = currentY - size / 2f;
            UnifiedCoord pos = new UnifiedCoord(uiX, uiY);
            float glX = pos.glX();
            float glY = pos.glSpriteY(size);
            
            sprite.setSize(size, size);
            sprite.setAlphaMult(alphaMult);
            
            if (Math.abs(currentRotation) > 0.5f) {
                float halfSize = size / 2f;
                GL11.glPushMatrix();
                GL11.glTranslatef(glX + halfSize, glY + halfSize, 0f);
                GL11.glRotatef(currentRotation, 0f, 0f, 1f);
                GL11.glTranslatef(-halfSize, -halfSize, 0f);
                sprite.bindTexture();
                GL11.glColor4f(1f, 1f, 1f, alphaMult);
                
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glTexCoord2f(0f, 0f);
                GL11.glVertex2f(0f, 0f);
                GL11.glTexCoord2f(1f, 0f);
                GL11.glVertex2f(size, 0f);
                GL11.glTexCoord2f(1f, 1f);
                GL11.glVertex2f(size, size);
                GL11.glTexCoord2f(0f, 1f);
                GL11.glVertex2f(0f, size);
                GL11.glEnd();
                
                GL11.glPopMatrix();
            } else {
                sprite.render(glX, glY);
            }
            
            GLStateUtil.disableTexturing();
        } finally {
            if (needsContextCleanup) {
                UnifiedCoord.clearCurrent();
            }
        }
    }
}