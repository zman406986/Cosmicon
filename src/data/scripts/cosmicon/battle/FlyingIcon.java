package data.scripts.cosmicon.battle;

import java.awt.Color;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import data.scripts.cosmicon.util.CoordHelper;
import data.scripts.cosmicon.util.EasingUtil;
import data.scripts.cosmicon.util.GLStateUtil;

public class FlyingIcon {
    private float startX, startY;
    private float targetX, targetY;
    private float currentX, currentY;
    private float duration;
    private float elapsed;
    private final float size;
    private boolean complete;
    private final SpriteAPI sprite;
    private int value;
    private final Color color;
    
    private LabelAPI valueLabel;
    private CustomPanelAPI labelPanel;
    private boolean labelCreated;
    
    private static final float LABEL_HEIGHT = 30f;
    
    public FlyingIcon(SpriteAPI sprite, float size, Color color) {
        this.sprite = sprite;
        this.size = size;
        this.color = color;
        this.complete = false;
        this.elapsed = 0f;
        this.labelCreated = false;
    }
    
    public void startFrom(float x, float y) {
        this.startX = x;
        this.startY = y;
        this.currentX = x;
        this.currentY = y;
    }
    
    public void flyTo(float x, float y, float duration) {
        this.targetX = x;
        this.targetY = y;
        this.duration = duration;
        this.elapsed = 0f;
        this.complete = false;
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
        if (complete) return;
        elapsed += amount;
        float progress = Math.min(elapsed / duration, 1f);
        float eased = EasingUtil.easeOutQuad(progress);
        currentX = startX + (targetX - startX) * eased;
        currentY = startY + (targetY - startY) * eased;
        if (progress >= 1f) {
            complete = true;
        }
        updateLabelPosition();
    }
    
    public boolean isComplete() { return complete; }
    public float getX() { return currentX; }
    public float getY() { return currentY; }
    public float getSize() { return size; }
    public SpriteAPI getSprite() { return sprite; }
    public int getValue() { return value; }
    
    public void createLabel(CustomPanelAPI panel) {
        if (labelCreated || panel == null) return;
        
        SettingsAPI settings = Global.getSettings();
        valueLabel = settings.createLabel(String.valueOf(value), Fonts.INSIGNIA_LARGE);
        valueLabel.setColor(color);
        valueLabel.setAlignment(Alignment.MID);
        
        float labelWidth = valueLabel.computeTextWidth(String.valueOf(value)) + 10f;
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
    
    public void render(float panelX, float panelY, float panelHeight, float alphaMult) {
        if (sprite == null) return;
        
        GLStateUtil.enableTexturingWithBlend();
        
        float glX = panelX + currentX - size / 2f;
        float glY = CoordHelper.uiToGlY(panelY, panelHeight, currentY + size / 2f);
        
        sprite.setSize(size, size);
        sprite.setAlphaMult(alphaMult);
        sprite.render(glX, glY);
        
        GLStateUtil.disableTexturing();
    }
}