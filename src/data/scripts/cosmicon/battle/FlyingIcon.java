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
    
    private float currentRotation = 0f;
    private float targetRotation = 0f;
    private float rotationElapsed = 0f;
    private float originalRotation = 0f;
    private boolean positionInitialized = false;
    
    private LabelAPI valueLabel;
    private CustomPanelAPI labelPanel;
    private boolean labelCreated;
    
    public FlyingIcon(SpriteAPI sprite, float size, Color color) {
        this.sprite = sprite;
        this.size = size;
        this.color = color;
        this.flyPhase = FlyPhase.IDLE;
        this.elapsed = 0f;
        this.labelCreated = false;
        this.usePullback = false;
    }
    
    public void startFrom(float x, float y) {
        this.startX = x;
        this.startY = y;
        this.currentX = x;
        this.currentY = y;
        this.originalRotation = currentRotation;
        this.positionInitialized = true;
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
    
    public void flyTo(float x, float y, float duration) {
        flyTo(x, y, duration, false, false);
    }
    
    public void flyTo(float x, float y, float duration, boolean useLinear) {
        flyTo(x, y, duration, useLinear, useLinear);
    }
    
    public void flyTo(float x, float y, float duration, boolean useLinear, boolean skipPullback) {
        if (!positionInitialized) {
            currentX = 0f;
            currentY = 0f;
            originalRotation = currentRotation;
        }
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
        if (!positionInitialized) {
            currentX = 0f;
            currentY = 0f;
            originalRotation = currentRotation;
        }
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
            flyPhase = FlyPhase.ROTATING;
        } else {
            flyPhase = FlyPhase.FLYING;
        }
    }
    
    public void setValue(int value) {
        this.value = value;
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
            flyPhase = usePullback ? FlyPhase.PULLBACK : FlyPhase.READY;
            return;
        }
        
        float progress = rotationElapsed / ROTATION_DURATION;
        float eased = EasingUtil.easeOutQuad(progress);
        currentRotation = originalRotation + (targetRotation - originalRotation) * eased;
    }
    
    private void advancePullback() {
        if (elapsed >= PULLBACK_DURATION) {
            currentX = pullbackX;
            currentY = pullbackY;
            startX = pullbackX;
            startY = pullbackY;
            elapsed = 0f;
            flyPhase = FlyPhase.READY;
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
        panel.addComponent((UIComponentAPI) valueLabel)
            .setSize(labelWidth, LABEL_HEIGHT)
            .inTL(currentX - labelWidth / 2f, currentY - LABEL_HEIGHT / 2f);
        
        labelPanel = panel;
        labelCreated = true;
    }
    
    private void updateLabelPosition() {
        if (valueLabel != null && labelCreated) {
            float labelWidth = valueLabel.computeTextWidth(String.valueOf(value)) + 10f;
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
    
    public void forceComplete() {
        flyPhase = FlyPhase.COMPLETE;
        elapsed = 0f;
        currentX = targetX;
        currentY = targetY;
        currentRotation = targetRotation;
        rotationElapsed = 0f;
        usePullback = false;
        useLinearFlight = false;
        if (valueLabel != null) {
            valueLabel.setOpacity(0f);
        }
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
            float uiY = currentY + size / 2f;
            UnifiedCoord pos = new UnifiedCoord(uiX, uiY);
            float glX = pos.glX();
            float glY = pos.glY();
            
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