package data.scripts.cosmicon.battle;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.graphics.SpriteAPI;

import data.scripts.cosmicon.util.ColorHelper;
import data.scripts.cosmicon.util.GLStateUtil;
import data.scripts.cosmicon.util.UnifiedCoord;

public class IconSplitEffect {
    private static final float SPLIT_ANIMATION_DURATION = 0.3f;
    private static final float MAX_ROTATION_ANGLE = 70f;
    
    private SpriteAPI sprite;
    private float iconSize;
    private Color iconColor;
    private float centerX;
    private float centerY;
    
    private float leftRotation;
    private float rightRotation;
    private float elapsed;
    private float restoreElapsed;
    private boolean active;
    private boolean restoring;
    
    public IconSplitEffect() {
        active = false;
        restoring = false;
        leftRotation = 0f;
        rightRotation = 0f;
        elapsed = 0f;
        restoreElapsed = 0f;
    }
    
    public void trigger(SpriteAPI sprite, float centerX, float centerY, Color iconColor, float iconSize) {
        this.sprite = sprite;
        this.centerX = centerX;
        this.centerY = centerY;
        this.iconColor = iconColor;
        this.iconSize = iconSize;
        
        this.leftRotation = 0f;
        this.rightRotation = 0f;
        this.elapsed = 0f;
        this.restoreElapsed = 0f;
        this.active = true;
        this.restoring = false;
    }
    
    public void startRestore(float restoreDuration) {
        this.restoring = true;
        this.restoreElapsed = 0f;
        this.restoreDuration = restoreDuration;
    }
    
    private float restoreDuration;
    
    public void advance(float amount) {
        if (!active) return;
        
        if (restoring) {
            restoreElapsed += amount;
            float progress = Math.min(restoreElapsed / restoreDuration, 1f);
            leftRotation = MAX_ROTATION_ANGLE * (1f - progress);
            rightRotation = MAX_ROTATION_ANGLE * (1f - progress);
            
            if (progress >= 1f) {
                leftRotation = 0f;
                rightRotation = 0f;
                active = false;
                restoring = false;
            }
        } else if (elapsed < SPLIT_ANIMATION_DURATION) {
            elapsed += amount;
            float progress = Math.min(elapsed / SPLIT_ANIMATION_DURATION, 1f);
            leftRotation = MAX_ROTATION_ANGLE * progress;
            rightRotation = MAX_ROTATION_ANGLE * progress;
        }
    }
    
    public void render(float panelX, float panelY, float panelWidth, float panelHeight, float alphaMult) {
        if (!active || sprite == null) return;
        
        UnifiedCoord.PanelContext existingCtx = UnifiedCoord.getCurrentOrNull();
        boolean needsContextCleanup = existingCtx == null;
        
        if (needsContextCleanup) {
            UnifiedCoord.setCurrent(new UnifiedCoord.PanelContext(panelX, panelY, panelWidth, panelHeight));
        }
        
        try {
            GLStateUtil.enableTexturingWithBlend();
            
            UnifiedCoord pos = new UnifiedCoord(centerX, centerY);
            float glCenterX = pos.glX();
            float glCenterY = pos.glY();
            
            float halfSize = iconSize / 2f;
            float fullHeight = iconSize;
            float[] c = ColorHelper.toGLComponents(iconColor, alphaMult);
            
            sprite.setAlphaMult(alphaMult);
            sprite.bindTexture();
            GL11.glColor4f(c[0], c[1], c[2], c[3]);
            
            float pivotY = glCenterY - halfSize;
            
            GL11.glPushMatrix();
            GL11.glTranslatef(glCenterX, pivotY, 0f);
            GL11.glRotatef(-leftRotation, 0f, 0f, 1f);
            GL11.glTranslatef(-halfSize, 0f, 0f);
            
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0f, 1f);
            GL11.glVertex2f(0f, 0f);
            GL11.glTexCoord2f(0.5f, 1f);
            GL11.glVertex2f(halfSize, 0f);
            GL11.glTexCoord2f(0.5f, 0f);
            GL11.glVertex2f(halfSize, fullHeight);
            GL11.glTexCoord2f(0f, 0f);
            GL11.glVertex2f(0f, fullHeight);
            GL11.glEnd();
            
            GL11.glPopMatrix();
            
            GL11.glPushMatrix();
            GL11.glTranslatef(glCenterX, pivotY, 0f);
            GL11.glRotatef(rightRotation, 0f, 0f, 1f);
            GL11.glTranslatef(0f, 0f, 0f);
            
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0.5f, 1f);
            GL11.glVertex2f(0f, 0f);
            GL11.glTexCoord2f(1f, 1f);
            GL11.glVertex2f(halfSize, 0f);
            GL11.glTexCoord2f(1f, 0f);
            GL11.glVertex2f(halfSize, fullHeight);
            GL11.glTexCoord2f(0.5f, 0f);
            GL11.glVertex2f(0f, fullHeight);
            GL11.glEnd();
            
            GL11.glPopMatrix();
            
            GLStateUtil.disableTexturing();
        } finally {
            if (needsContextCleanup) {
                UnifiedCoord.clearCurrent();
            }
        }
    }
    
    public boolean isActive() {
        return active;
    }
    
    public boolean isRestoring() {
        return restoring;
    }
    
    public void clear() {
        active = false;
        restoring = false;
        sprite = null;
        leftRotation = 0f;
        rightRotation = 0f;
        elapsed = 0f;
        restoreElapsed = 0f;
    }
}