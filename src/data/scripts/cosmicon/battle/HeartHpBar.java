package data.scripts.cosmicon.battle;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import data.scripts.cosmicon.util.ColorHelper;
import data.scripts.cosmicon.util.CoordHelper;
import data.scripts.cosmicon.util.GLStateUtil;

public class HeartHpBar {
    public static final float HEART_SIZE = 80f;
    
    private static final float FLASH_DURATION = 0.3f;
    private static final float MAX_DRAIN_DURATION = 1.0f;
    private static final float MIN_DRAIN_DURATION = 0.2f;
    
    private static final Color HEART_FILL = new Color(200, 50, 50);
    private static final Color HEART_OUTLINE = new Color(150, 30, 30);
    private static final Color HEART_FLASH = new Color(255, 255, 255);
    private static final Color HEART_EMPTY = new Color(80, 30, 30);
    
    private float x;
    private float y;
    
    private int currentHp;
    private int maxHp;
    private float displayHpPercent;
    private float targetHpPercent;
    
    private boolean flashing;
    private float flashElapsed;
    
    private boolean draining;
    private float drainElapsed;
    private float drainDuration;
    
    private CustomPanelAPI panel;
    private LabelAPI hpLabel;
    
    public HeartHpBar() {
        this.x = 0f;
        this.y = 0f;
        this.currentHp = 0;
        this.maxHp = 0;
        this.displayHpPercent = 1f;
        this.targetHpPercent = 1f;
        this.flashing = false;
        this.flashElapsed = 0f;
        this.draining = false;
        this.drainElapsed = 0f;
        this.drainDuration = 0f;
    }
    
    public void init(CustomPanelAPI panel) {
        this.panel = panel;
        createHpLabel();
    }
    
    private void createHpLabel() {
        if (panel == null) return;
        
        SettingsAPI settings = Global.getSettings();
        hpLabel = settings.createLabel("0/0", Fonts.DEFAULT_SMALL);
        hpLabel.setColor(Color.WHITE);
        hpLabel.setAlignment(Alignment.MID);
        panel.addComponent((UIComponentAPI) hpLabel)
            .setSize(HEART_SIZE, 20)
            .inTL(x, y + HEART_SIZE / 2f - 10f);
        hpLabel.setOpacity(0f);
    }
    
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        if (hpLabel != null) {
            hpLabel.getPosition().inTL(x, y + HEART_SIZE / 2f - 10f);
        }
    }
    
    public void setHp(int current, int max) {
        this.currentHp = current;
        this.maxHp = max;
        if (max > 0) {
            this.displayHpPercent = (float) current / max;
            this.targetHpPercent = displayHpPercent;
        } else {
            this.displayHpPercent = 0f;
            this.targetHpPercent = 0f;
        }
        updateHpLabel();
    }
    
    private void updateHpLabel() {
        if (hpLabel != null) {
            int displayHp = Math.round(displayHpPercent * maxHp);
            hpLabel.setText(String.format("%d/%d", displayHp, maxHp));
            hpLabel.setOpacity(1f);
        }
    }
    
    public void flashDamage() {
        flashing = true;
        flashElapsed = 0f;
    }
    
    public void drainTo(int targetHp) {
        if (maxHp <= 0) return;
        
        this.targetHpPercent = (float) Math.max(0, targetHp) / maxHp;
        
        float hpDiff = displayHpPercent - targetHpPercent;
        if (hpDiff > 0f) {
            draining = true;
            drainElapsed = 0f;
            drainDuration = Math.max(MIN_DRAIN_DURATION, 
                Math.min(MAX_DRAIN_DURATION, hpDiff * 2f));
        }
    }
    
    public void advance(float amount) {
        if (flashing) {
            flashElapsed += amount;
            if (flashElapsed >= FLASH_DURATION) {
                flashing = false;
                flashElapsed = 0f;
            }
        }
        
        if (draining) {
            drainElapsed += amount;
            float progress = Math.min(1f, drainElapsed / drainDuration);
            float eased = easeOutQuad(progress);
            
            float startPercent = displayHpPercent;
            displayHpPercent = startPercent + (targetHpPercent - startPercent) * eased;
            
            updateHpLabel();
            
            if (progress >= 1f) {
                draining = false;
                drainElapsed = 0f;
                displayHpPercent = targetHpPercent;
                currentHp = Math.round(targetHpPercent * maxHp);
            }
        }
    }
    
    private float easeOutQuad(float t) {
        return 1f - (1f - t) * (1f - t);
    }
    
    public void render(float panelX, float panelY, float panelHeight, float alphaMult) {
        GLStateUtil.resetBlendState();
        
        float renderX = panelX + x;
        float glY = CoordHelper.uiToGlY(panelY, panelHeight, y + HEART_SIZE);
        
        renderHeartFill(renderX, glY, alphaMult);
        renderHeartOutline(renderX, glY, alphaMult);
        
        if (flashing) {
            renderFlashOverlay(renderX, glY, alphaMult);
        }
        
        GLStateUtil.resetColor();
    }
    
    private void renderHeartFill(float x, float y, float alphaMult) {
        float fillHeight = HEART_SIZE * displayHpPercent;
        
        if (fillHeight > 0f) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            
            float scale = Global.getSettings().getScreenScaleMult();
            float scissorY = y + HEART_SIZE - fillHeight;
            GL11.glScissor(
                (int)(x * scale),
                (int)(scissorY * scale),
                (int)(HEART_SIZE * scale),
                (int)(fillHeight * scale)
            );
            
            float[] c = ColorHelper.toGLComponents(HEART_FILL, alphaMult);
            GL11.glColor4f(c[0], c[1], c[2], c[3]);
            
            drawHeartShape(x, y, HEART_SIZE);
            
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
        
        float emptyHeight = HEART_SIZE * (1f - displayHpPercent);
        if (emptyHeight > 0f) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            
            float scale = Global.getSettings().getScreenScaleMult();
            GL11.glScissor(
                (int)(x * scale),
                (int)(y * scale),
                (int)(HEART_SIZE * scale),
                (int)(emptyHeight * scale)
            );
            
            float[] c = ColorHelper.toGLComponents(HEART_EMPTY, alphaMult * 0.5f);
            GL11.glColor4f(c[0], c[1], c[2], c[3]);
            
            drawHeartShape(x, y, HEART_SIZE);
            
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }
    
    private void renderHeartOutline(float x, float y, float alphaMult) {
        float[] c = ColorHelper.toGLComponents(HEART_OUTLINE, alphaMult);
        GL11.glColor4f(c[0], c[1], c[2], c[3]);
        GL11.glLineWidth(2f);
        
        drawHeartOutline(x, y, HEART_SIZE);
    }
    
    private void renderFlashOverlay(float x, float y, float alphaMult) {
        float flashProgress = flashElapsed / FLASH_DURATION;
        float flashAlpha = (1f - flashProgress) * 0.8f;
        
        float[] c = ColorHelper.toGLComponents(HEART_FLASH, alphaMult * flashAlpha);
        GL11.glColor4f(c[0], c[1], c[2], c[3]);
        
        drawHeartShape(x, y, HEART_SIZE);
    }
    
    private void drawHeartShape(float x, float y, float size) {
        float lobeRadius = size * 0.25f;
        float centerX = x + size / 2f;
        float lobeY = y + size - lobeRadius;
        float pointY = y;
        float middleY = y + size * 0.35f;
        
        GL11.glBegin(GL11.GL_POLYGON);
        
        int segments = 16;
        for (int i = 0; i <= segments; i++) {
            float angle = (float) Math.PI + (float) Math.PI * i / segments;
            float lx = centerX - lobeRadius + (float) Math.cos(angle) * lobeRadius;
            float ly = lobeY + (float) Math.sin(angle) * lobeRadius;
            GL11.glVertex2f(lx, ly);
        }
        
        GL11.glVertex2f(centerX - lobeRadius, middleY);
        GL11.glVertex2f(centerX, pointY);
        GL11.glVertex2f(centerX + lobeRadius, middleY);
        
        for (int i = 0; i <= segments; i++) {
            float angle = (float) Math.PI * 2 - (float) Math.PI * i / segments;
            float rx = centerX + lobeRadius + (float) Math.cos(angle) * lobeRadius;
            float ry = lobeY + (float) Math.sin(angle) * lobeRadius;
            GL11.glVertex2f(rx, ry);
        }
        
        GL11.glEnd();
    }
    
    private void drawHeartOutline(float x, float y, float size) {
        float lobeRadius = size * 0.25f;
        float centerX = x + size / 2f;
        float lobeY = y + size - lobeRadius;
        float pointY = y;
        float middleY = y + size * 0.35f;
        
        GL11.glBegin(GL11.GL_LINE_LOOP);
        
        int segments = 16;
        for (int i = 0; i <= segments; i++) {
            float angle = (float) Math.PI + (float) Math.PI * i / segments;
            float lx = centerX - lobeRadius + (float) Math.cos(angle) * lobeRadius;
            float ly = lobeY + (float) Math.sin(angle) * lobeRadius;
            GL11.glVertex2f(lx, ly);
        }
        
        GL11.glVertex2f(centerX - lobeRadius, middleY);
        GL11.glVertex2f(centerX, pointY);
        GL11.glVertex2f(centerX + lobeRadius, middleY);
        
        for (int i = segments; i >= 0; i--) {
            float angle = (float) Math.PI * 2 - (float) Math.PI * i / segments;
            float rx = centerX + lobeRadius + (float) Math.cos(angle) * lobeRadius;
            float ry = lobeY + (float) Math.sin(angle) * lobeRadius;
            GL11.glVertex2f(rx, ry);
        }
        
        GL11.glEnd();
    }
    
    public boolean isAnimating() {
        return flashing || draining;
    }
    
    public boolean isFlashing() {
        return flashing;
    }
    
    public boolean isDraining() {
        return draining;
    }
    
    public float getX() {
        return x;
    }
    
    public float getY() {
        return y;
    }
    
    public int getCurrentHp() {
        return currentHp;
    }
    
    public int getMaxHp() {
        return maxHp;
    }
    
    public float getDisplayHpPercent() {
        return displayHpPercent;
    }
    
    public void setLabelVisible(boolean visible) {
        if (hpLabel != null) {
            hpLabel.setOpacity(visible ? 1f : 0f);
        }
    }
    
    public void forceComplete() {
        flashing = false;
        draining = false;
        flashElapsed = 0f;
        drainElapsed = 0f;
        displayHpPercent = targetHpPercent;
        currentHp = Math.round(targetHpPercent * maxHp);
        updateHpLabel();
    }
    
    public void cleanup() {
        if (hpLabel != null && panel != null) {
            panel.removeComponent((com.fs.starfarer.api.ui.UIComponentAPI) hpLabel);
        }
        hpLabel = null;
        panel = null;
        flashing = false;
        draining = false;
    }
}