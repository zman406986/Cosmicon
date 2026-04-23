package data.scripts.cosmicon.battle;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;

import data.scripts.cosmicon.util.ColorHelper;
import data.scripts.cosmicon.util.CoordHelper;
import data.scripts.cosmicon.util.GLStateUtil;

public class ValueChangeAnimator {
    private static final float DELTA_SHOW_DURATION = 0.3f;
    private static final float FLASH_DURATION = 0.15f;
    private static final float DELTA_FLY_DURATION = 0.4f;
    private static final float CHANGE_DELAY = 0.1f;
    private static final float LABEL_HEIGHT = 30f;
    private static final float DELTA_START_OFFSET_Y = -20f;
    private static final float DELTA_FLY_DISTANCE = 40f;

    private enum Phase { IDLE, DELTA_SHOW, FLASH, DELTA_FLY, COMPLETE }

    private static class QueuedChange {
        String deltaText;
        Color color;
        
        QueuedChange(String deltaText, Color color) {
            this.deltaText = deltaText;
            this.color = color;
        }
    }

    private final List<QueuedChange> changeQueue;
    private Phase phase;
    private float elapsed;
    private float delayElapsed;
    
    private float iconCenterX;
    private float iconCenterY;
    private int currentValue;
    private float iconSize;
    
    private LabelAPI deltaLabel;
    private LabelAPI totalLabel;
    private CustomPanelAPI panel;
    private boolean labelsCreated;
    
    private float deltaCurrentY;
    private float deltaAlpha;
    private float deltaScale;
    private float flashIntensity;
    
    private boolean isAttack;
    private SpriteAPI roleIcon;

    public ValueChangeAnimator() {
        changeQueue = new ArrayList<>();
        phase = Phase.IDLE;
        elapsed = 0f;
        delayElapsed = 0f;
        labelsCreated = false;
        currentValue = 0;
        iconSize = 0f;
        isAttack = true;
    }

    public void queueChange(String deltaText, Color color) {
        changeQueue.add(new QueuedChange(deltaText, color));
    }

    public void start(float iconCenterX, float iconCenterY, int currentValue, 
            CustomPanelAPI panel, boolean isAttack) {
        this.iconCenterX = iconCenterX;
        this.iconCenterY = iconCenterY;
        this.currentValue = currentValue;
        this.panel = panel;
        this.isAttack = isAttack;
        
        this.iconSize = BattleRenderingUtils.PANEL_HEIGHT / 2f * BattleRenderingUtils.ROLE_ICON_SIZE_RATIO;
        this.roleIcon = isAttack ? CosmiconSprites.getAtkIcon() : CosmiconSprites.getDefIcon();
        
        if (!changeQueue.isEmpty()) {
            phase = Phase.DELTA_SHOW;
            elapsed = 0f;
            deltaCurrentY = iconCenterY + DELTA_START_OFFSET_Y;
            deltaAlpha = 0f;
            deltaScale = 0.5f;
            flashIntensity = 0f;
            
            createLabels();
        }
    }

    private void createLabels() {
        if (labelsCreated || panel == null) return;
        
        SettingsAPI settings = Global.getSettings();
        
        QueuedChange current = changeQueue.get(0);
        deltaLabel = settings.createLabel(current.deltaText, Fonts.INSIGNIA_LARGE);
        deltaLabel.setColor(current.color);
        deltaLabel.setAlignment(Alignment.MID);
        
        float deltaWidth = deltaLabel.computeTextWidth(current.deltaText) + 20f;
        panel.addComponent((UIComponentAPI) deltaLabel)
            .setSize(deltaWidth, LABEL_HEIGHT)
            .inTL(iconCenterX - deltaWidth / 2f, deltaCurrentY - LABEL_HEIGHT / 2f);
        
        totalLabel = settings.createLabel(String.valueOf(currentValue), Fonts.INSIGNIA_LARGE);
        totalLabel.setColor(isAttack ? ColorHelper.ATTACK_VALUE : ColorHelper.DEFENSE_VALUE);
        totalLabel.setAlignment(Alignment.MID);
        
        float totalWidth = totalLabel.computeTextWidth(String.valueOf(currentValue)) + 20f;
        panel.addComponent((UIComponentAPI) totalLabel)
            .setSize(totalWidth, LABEL_HEIGHT)
            .inTL(iconCenterX - totalWidth / 2f, iconCenterY - LABEL_HEIGHT / 2f);
        
        labelsCreated = true;
    }

    public void advance(float amount) {
        if (phase == Phase.IDLE || phase == Phase.COMPLETE) return;
        
        elapsed += amount;
        
        switch (phase) {
            case DELTA_SHOW -> advanceDeltaShow();
            case FLASH -> advanceFlash();
            case DELTA_FLY -> advanceDeltaFly();
        }
        
        updateLabelPositions();
    }

    private void advanceDeltaShow() {
        if (elapsed >= DELTA_SHOW_DURATION) {
            deltaAlpha = 1f;
            deltaScale = 1f;
            elapsed = 0f;
            phase = Phase.FLASH;
            return;
        }
        
        float progress = elapsed / DELTA_SHOW_DURATION;
        
        deltaAlpha = easeOutQuad(progress);
        
        float bounce = (float) Math.sin(progress * Math.PI);
        deltaScale = 0.5f + 0.7f * bounce + 0.5f * progress;
    }

    private void advanceFlash() {
        if (elapsed >= FLASH_DURATION) {
            flashIntensity = 0f;
            elapsed = 0f;
            phase = Phase.DELTA_FLY;
            return;
        }
        
        float progress = elapsed / FLASH_DURATION;
        flashIntensity = (float) Math.sin(progress * Math.PI) * 0.8f;
    }

    private void advanceDeltaFly() {
        if (elapsed >= DELTA_FLY_DURATION) {
            processNextChange();
            return;
        }
        
        float progress = elapsed / DELTA_FLY_DURATION;
        
        deltaCurrentY = iconCenterY + DELTA_START_OFFSET_Y - DELTA_FLY_DISTANCE * easeOutQuad(progress);
        deltaAlpha = 1f - easeInQuad(progress);
    }

    private void processNextChange() {
        changeQueue.remove(0);
        
        if (changeQueue.isEmpty()) {
            phase = Phase.COMPLETE;
            if (deltaLabel != null) {
                deltaLabel.setOpacity(0f);
            }
            return;
        }
        
        delayElapsed = 0f;
        
        QueuedChange next = changeQueue.get(0);
        deltaLabel.setText(next.deltaText);
        deltaLabel.setColor(next.color);
        
        float deltaWidth = deltaLabel.computeTextWidth(next.deltaText) + 20f;
        ((UIComponentAPI) deltaLabel).getPosition().setSize(deltaWidth, LABEL_HEIGHT);
        
        deltaCurrentY = iconCenterY + DELTA_START_OFFSET_Y;
        deltaAlpha = 0f;
        deltaScale = 0.5f;
        flashIntensity = 0f;
        
        elapsed = 0f;
        phase = Phase.DELTA_SHOW;
    }

    private void updateLabelPositions() {
        if (deltaLabel != null && labelsCreated) {
            String text = changeQueue.isEmpty() ? "" : changeQueue.get(0).deltaText;
            float width = deltaLabel.computeTextWidth(text) + 20f;
            ((UIComponentAPI) deltaLabel).getPosition()
                .setSize(width, LABEL_HEIGHT)
                .inTL(iconCenterX - width / 2f, deltaCurrentY - LABEL_HEIGHT / 2f);
            deltaLabel.setOpacity(deltaAlpha);
        }
        
        if (totalLabel != null && labelsCreated) {
            String totalText = String.valueOf(currentValue);
            float width = totalLabel.computeTextWidth(totalText) + 20f;
            ((UIComponentAPI) totalLabel).getPosition()
                .setSize(width, LABEL_HEIGHT)
                .inTL(iconCenterX - width / 2f, iconCenterY - LABEL_HEIGHT / 2f);
        }
    }

    public void render(float panelX, float panelY, float panelHeight, float alphaMult) {
        if (phase == Phase.IDLE) return;
        
        if (phase != Phase.COMPLETE && flashIntensity > 0f && roleIcon != null) {
            renderIconFlash(panelX, panelY, panelHeight, alphaMult);
        }
    }

    private void renderIconFlash(float panelX, float panelY, float panelHeight, float alphaMult) {
        GLStateUtil.enableTexturingWithBlend();
        
        float glX = panelX + iconCenterX - iconSize / 2f;
        float glY = CoordHelper.uiToGlY(panelY, panelHeight, iconCenterY + iconSize / 2f);
        
        roleIcon.setSize(iconSize, iconSize);
        float baseAlpha = BattleRenderingUtils.ROLE_ICON_OPACITY;
        float flashAlpha = baseAlpha + (1f - baseAlpha) * flashIntensity;
        roleIcon.setAlphaMult(alphaMult * flashAlpha);
        roleIcon.render(glX, glY);
        
        GLStateUtil.disableTexturing();
    }

    private float easeOutQuad(float t) {
        return 1f - (1f - t) * (1f - t);
    }

    private float easeInQuad(float t) {
        return t * t;
    }

    public boolean isComplete() {
        return phase == Phase.COMPLETE;
    }

    public int getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(int value) {
        this.currentValue = value;
        if (totalLabel != null && labelsCreated) {
            totalLabel.setText(String.valueOf(value));
        }
    }

    public boolean hasQueuedChanges() {
        return !changeQueue.isEmpty();
    }

    public void forceComplete() {
        phase = Phase.COMPLETE;
        elapsed = 0f;
        changeQueue.clear();
        if (deltaLabel != null) {
            deltaLabel.setOpacity(0f);
        }
    }

    public void cleanup() {
        if (deltaLabel != null && panel != null) {
            panel.removeComponent((UIComponentAPI) deltaLabel);
        }
        if (totalLabel != null && panel != null) {
            panel.removeComponent((UIComponentAPI) totalLabel);
        }
        deltaLabel = null;
        totalLabel = null;
        panel = null;
        labelsCreated = false;
        changeQueue.clear();
    }

    public void reset() {
        cleanup();
        phase = Phase.IDLE;
        elapsed = 0f;
        delayElapsed = 0f;
        currentValue = 0;
        iconSize = 0f;
        deltaCurrentY = 0f;
        deltaAlpha = 0f;
        deltaScale = 1f;
        flashIntensity = 0f;
    }
}