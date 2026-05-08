package data.scripts.cosmicon.battle;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;

import data.scripts.cosmicon.util.ColorHelper;
import data.scripts.cosmicon.util.EasingUtil;
import data.scripts.cosmicon.util.GLStateUtil;
import data.scripts.cosmicon.util.UnifiedCoord;

public class ValueChangeAnimator {
    private static final float DELTA_SHOW_DURATION = 0.3f;
    private static final float FLASH_DURATION = 0.15f;
    private static final float DELTA_FLY_DURATION = 0.4f;
    private static final float LABEL_HEIGHT = 35f;
    private static final float DELTA_START_OFFSET_Y = -20f;
    private static final float DELTA_FLY_DISTANCE = 40f;

    private enum Phase { IDLE, DELTA_SHOW, FLASH, DELTA_FLY, COMPLETE }

    private record QueuedChange(String deltaText, Color color, int delta)
    {
    }

    private final List<QueuedChange> changeQueue;
    private Phase phase;
    private float elapsed;
    
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
    private float flashIntensity;
    
    private boolean isAttack;
    private SpriteAPI roleIcon;

    private float cachedDeltaLabelWidth = -1f;
    private float cachedTotalLabelWidth = -1f;

    public ValueChangeAnimator() {
        changeQueue = new ArrayList<>();
        phase = Phase.IDLE;
        elapsed = 0f;
        labelsCreated = false;
        currentValue = 0;
        iconSize = 0f;
        isAttack = true;
    }

    public void queueChange(String deltaText, Color color, int delta) {
        changeQueue.add(new QueuedChange(deltaText, color, delta));
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
            flashIntensity = 0f;
            
            if (labelsCreated) {
                updateExistingLabels();
            } else {
                createLabels();
            }
        }
    }

    private void createLabels() {
        if (labelsCreated || panel == null) return;
        
        SettingsAPI settings = Global.getSettings();
        
        QueuedChange current = changeQueue.get(0);
        deltaLabel = settings.createLabel(current.deltaText, Fonts.INSIGNIA_VERY_LARGE);
        deltaLabel.setColor(current.color);
        deltaLabel.setAlignment(Alignment.MID);
        
        float deltaWidth = deltaLabel.computeTextWidth(current.deltaText) + 20f;
        this.cachedDeltaLabelWidth = deltaWidth;
        panel.addComponent((UIComponentAPI) deltaLabel)
            .setSize(deltaWidth, LABEL_HEIGHT)
            .inTL(iconCenterX - deltaWidth / 2f, deltaCurrentY - LABEL_HEIGHT / 2f);
        
        totalLabel = settings.createLabel(String.valueOf(currentValue), Fonts.INSIGNIA_VERY_LARGE);
        totalLabel.setColor(isAttack ? ColorHelper.ATTACK_VALUE : ColorHelper.DEFENSE_VALUE);
        totalLabel.setAlignment(Alignment.MID);
        
        float totalWidth = totalLabel.computeTextWidth(String.valueOf(currentValue)) + 20f;
        this.cachedTotalLabelWidth = totalWidth;
        panel.addComponent((UIComponentAPI) totalLabel)
            .setSize(totalWidth, LABEL_HEIGHT)
            .inTL(iconCenterX - totalWidth / 2f, iconCenterY - LABEL_HEIGHT / 2f);
        
        labelsCreated = true;
    }

    private void updateExistingLabels() {
        if (!labelsCreated || changeQueue.isEmpty()) return;

        QueuedChange current = changeQueue.get(0);
        deltaLabel.setText(current.deltaText);
        deltaLabel.setColor(current.color);
        cachedDeltaLabelWidth = -1f;

        totalLabel.setText(String.valueOf(currentValue));
        totalLabel.setColor(isAttack ? ColorHelper.ATTACK_VALUE : ColorHelper.DEFENSE_VALUE);
        totalLabel.setOpacity(1f);
        cachedTotalLabelWidth = -1f;
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
            elapsed = 0f;
            phase = Phase.FLASH;
            return;
        }
        
        float progress = elapsed / DELTA_SHOW_DURATION;
        
        deltaAlpha = EasingUtil.easeOutQuad(progress);
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
        
        deltaCurrentY = iconCenterY + DELTA_START_OFFSET_Y - DELTA_FLY_DISTANCE * EasingUtil.easeOutQuad(progress);
        deltaAlpha = 1f - EasingUtil.easeInQuad(progress);
    }

    private void processNextChange() {
        QueuedChange completed = changeQueue.remove(0);
        currentValue += completed.delta;
        updateTotalLabel();
        
        if (changeQueue.isEmpty()) {
            phase = Phase.COMPLETE;
            if (deltaLabel != null) {
                deltaLabel.setOpacity(0f);
            }
            if (totalLabel != null) {
                totalLabel.setOpacity(0f);
            }
            return;
        }
        
        QueuedChange next = changeQueue.get(0);
        deltaLabel.setText(next.deltaText);
        deltaLabel.setColor(next.color);
        
        cachedDeltaLabelWidth = -1f;
        float deltaWidth = deltaLabel.computeTextWidth(next.deltaText) + 20f;
        cachedDeltaLabelWidth = deltaWidth;
        ((UIComponentAPI) deltaLabel).getPosition().setSize(deltaWidth, LABEL_HEIGHT);
        
        deltaCurrentY = iconCenterY + DELTA_START_OFFSET_Y;
        deltaAlpha = 0f;
        flashIntensity = 0f;
        
        elapsed = 0f;
        phase = Phase.DELTA_SHOW;
    }

    private void updateTotalLabel() {
        if (totalLabel != null) {
            String totalText = String.valueOf(currentValue);
            totalLabel.setText(totalText);
            cachedTotalLabelWidth = -1f;
            float width = totalLabel.computeTextWidth(totalText) + 20f;
            cachedTotalLabelWidth = width;
            ((UIComponentAPI) totalLabel).getPosition()
                .setSize(width, LABEL_HEIGHT)
                .inTL(iconCenterX - width / 2f, iconCenterY - LABEL_HEIGHT / 2f);
        }
    }

    private void updateLabelPositions() {
        if (deltaLabel != null && labelsCreated) {
            String text = changeQueue.isEmpty() ? "" : changeQueue.get(0).deltaText;
            if (cachedDeltaLabelWidth < 0f) {
                cachedDeltaLabelWidth = deltaLabel.computeTextWidth(text) + 20f;
            }
            float width = cachedDeltaLabelWidth;
            ((UIComponentAPI) deltaLabel).getPosition()
                .setSize(width, LABEL_HEIGHT)
                .inTL(iconCenterX - width / 2f, deltaCurrentY - LABEL_HEIGHT / 2f);
            deltaLabel.setOpacity(deltaAlpha);
        }
        
        if (totalLabel != null && labelsCreated) {
            String totalText = String.valueOf(currentValue);
            if (cachedTotalLabelWidth < 0f) {
                cachedTotalLabelWidth = totalLabel.computeTextWidth(totalText) + 20f;
            }
            float width = cachedTotalLabelWidth;
            ((UIComponentAPI) totalLabel).getPosition()
                .setSize(width, LABEL_HEIGHT)
                .inTL(iconCenterX - width / 2f, iconCenterY - LABEL_HEIGHT / 2f);
        }
    }

    public void render(float panelX, float panelY, float panelHeight, float alphaMult) {
        if (phase == Phase.IDLE) return;
        
        if (phase != Phase.COMPLETE && flashIntensity > 0f && roleIcon != null) {
            UnifiedCoord.PanelContext existingCtx = UnifiedCoord.getCurrentOrNull();
            boolean needsContextCleanup = existingCtx == null;
            
            if (needsContextCleanup) {
                UnifiedCoord.setCurrent(new UnifiedCoord.PanelContext(panelX, panelY, BattleRenderingUtils.PANEL_WIDTH, panelHeight));
            }
            try {
                renderIconFlash(alphaMult);
            } finally {
                if (needsContextCleanup) {
                    UnifiedCoord.clearCurrent();
                }
            }
        }
    }

    private void renderIconFlash(float alphaMult) {
        GLStateUtil.enableTexturingWithBlend();
        
        UnifiedCoord pos = new UnifiedCoord(iconCenterX - iconSize / 2f, iconCenterY - iconSize / 2f);
        float glX = pos.glX();
        float glY = pos.glSpriteY(iconSize);
        
        roleIcon.setSize(iconSize, iconSize);
        float baseAlpha = BattleRenderingUtils.ROLE_ICON_OPACITY;
        float flashAlpha = baseAlpha + (1f - baseAlpha) * flashIntensity;
        roleIcon.setAlphaMult(alphaMult * flashAlpha);
        roleIcon.render(glX, glY);
        
        GLStateUtil.disableTexturing();
    }

    public boolean isComplete() {
        return phase == Phase.COMPLETE || phase == Phase.IDLE;
    }

    public int getCurrentValue() {
        return currentValue;
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
        phase = Phase.IDLE;
        elapsed = 0f;
        deltaCurrentY = 0f;
        deltaAlpha = 0f;
        flashIntensity = 0f;
        changeQueue.clear();
    }

    public void reset() {
        cleanup();
        currentValue = 0;
        iconSize = 0f;
        iconCenterX = 0f;
        iconCenterY = 0f;
        roleIcon = null;
    }
}