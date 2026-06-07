package data.scripts.cosmicon.battle;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import data.scripts.cosmicon.util.ColorHelper;
import data.scripts.cosmicon.util.GLStateUtil;
import data.scripts.cosmicon.util.UnifiedCoord;

public class ImpactEffect {
    private static final float FLASH_DURATION = 0.2f;
    private static final float PARTICLE_LIFETIME = 0.55f;
    private static final float SHOCKWAVE_DURATION = 0.4f;
    private static final float SHOCKWAVE_MAX_RADIUS = 120f;

    private static final Color COLOR_FLASH_WHITE = new Color(255, 255, 220);
    private static final Color COLOR_WHITE = new Color(255, 255, 255);
    
    private boolean flashActive;
    private float flashX;
    private float flashY;
    private float flashSize;
    private float flashElapsed;
    private Color flashColor;
    
    private final List<Particle> particles;
    
    private boolean shockwaveActive;
    private float shockwaveX;
    private float shockwaveY;
    private float shockwaveElapsed;
    
    public ImpactEffect() {
        flashActive = false;
        flashElapsed = 0f;
        particles = new ArrayList<>();
        shockwaveActive = false;
        shockwaveElapsed = 0f;
    }
    
    public void triggerFlash(float x, float y, float size, Color color) {
        this.flashX = x;
        this.flashY = y;
        this.flashSize = size;
        this.flashColor = color;
        this.flashElapsed = 0f;
        this.flashActive = true;
    }
    
    public void triggerParticles(float x, float y, int count, Color color) {
        for (int i = 0; i < count; i++) {
            Particle p = new Particle();
            p.x = x;
            p.y = y;
            
            float angle = (float)(Math.random() * 2 * Math.PI);
            float speed = 80f + (float)(Math.random() * 160f);
            p.vx = (float)Math.cos(angle) * speed;
            p.vy = (float)Math.sin(angle) * speed;
            
            p.scale = 3f + (float)(Math.random() * 5f);
            p.alpha = 1f;
            p.lifetime = PARTICLE_LIFETIME;
            p.elapsed = 0f;
            p.color = color;
            
            particles.add(p);
        }
    }
    
    public void triggerShockwave(float x, float y) {
        this.shockwaveX = x;
        this.shockwaveY = y;
        this.shockwaveElapsed = 0f;
        this.shockwaveActive = true;
    }
    
    public void triggerHeavyImpact(float x, float y, Color color) {
        triggerFlash(x, y, 120f, COLOR_FLASH_WHITE);
        triggerShockwave(x, y);
        
        for (int i = 0; i < 24; i++) {
            Particle p = new Particle();
            p.x = x;
            p.y = y;
            float angle = (float)(Math.random() * 2 * Math.PI);
            float speed = 130f + (float)(Math.random() * 250f);
            p.vx = (float)Math.cos(angle) * speed;
            p.vy = (float)Math.sin(angle) * speed;
            p.scale = 5f + (float)(Math.random() * 8f);
            p.alpha = 1f;
            p.lifetime = 0.55f + (float)(Math.random() * 0.35f);
            p.elapsed = 0f;
            p.color = color;
            particles.add(p);
        }
        
        for (int i = 0; i < 10; i++) {
            Particle p = new Particle();
            p.x = x;
            p.y = y;
            float angle = (float)(Math.random() * 2 * Math.PI);
            float speed = 50f + (float)(Math.random() * 90f);
            p.vx = (float)Math.cos(angle) * speed;
            p.vy = (float)Math.sin(angle) * speed;
            p.scale = 6f + (float)(Math.random() * 6f);
            p.alpha = 1f;
            p.lifetime = 0.3f + (float)(Math.random() * 0.2f);
            p.elapsed = 0f;
            p.color = COLOR_WHITE;
            particles.add(p);
        }
    }
    
    public void advance(float amount) {
        if (flashActive) {
            flashElapsed += amount;
            if (flashElapsed >= FLASH_DURATION) {
                flashActive = false;
            }
        }
        
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.elapsed += amount;
            p.x += p.vx * amount;
            p.y += p.vy * amount;
            p.vx *= 0.92f;
            p.vy *= 0.92f;
            
            float progress = p.elapsed / p.lifetime;
            p.alpha = 1f - progress;
            p.scale *= (1f - 0.3f * amount);
            
            if (p.elapsed >= p.lifetime) {
                particles.remove(i);
            }
        }
        
        if (shockwaveActive) {
            shockwaveElapsed += amount;
            if (shockwaveElapsed >= SHOCKWAVE_DURATION) {
                shockwaveActive = false;
            }
        }
    }
    
    public void render(float panelX, float panelY, float panelWidth, float panelHeight, float alphaMult) {
        UnifiedCoord.PanelContext existingCtx = UnifiedCoord.getCurrentOrNull();
        boolean needsContextCleanup = existingCtx == null;
        
        if (needsContextCleanup) {
            UnifiedCoord.setCurrent(new UnifiedCoord.PanelContext(panelX, panelY, panelWidth, panelHeight));
        }
        try {
            renderFlash(alphaMult);
            renderParticles(alphaMult);
            renderShockwave(alphaMult);
        } finally {
            if (needsContextCleanup) {
                UnifiedCoord.clearCurrent();
            }
        }
    }
    
    private void renderFlash(float alphaMult) {
        if (!flashActive) return;
        
        GLStateUtil.resetBlendState();
        
        float progress = flashElapsed / FLASH_DURATION;
        float scaleProgress = (float)Math.sin(progress * Math.PI);
        float currentSize = flashSize * (0.3f + 0.7f * scaleProgress);
        float alpha = (1f - progress) * 0.8f * alphaMult;
        
        UnifiedCoord pos = new UnifiedCoord(flashX, flashY);
        float glX = pos.glX();
        float glY = pos.glY();
        
        float[] c = ColorHelper.toGLComponents(flashColor, alpha);
        GL11.glColor4f(c[0], c[1], c[2], c[3]);
        
        int segments = 16;
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(glX, glY);
        
        for (int i = 0; i <= segments; i++) {
            float angle = (float)(2 * Math.PI * i / segments);
            float px = glX + (float)Math.cos(angle) * currentSize;
            float py = glY + (float)Math.sin(angle) * currentSize;
            GL11.glVertex2f(px, py);
        }
        GL11.glEnd();
        
        GLStateUtil.resetColor();
    }
    
    private void renderParticles(float alphaMult) {
        if (particles.isEmpty()) return;
        
        GLStateUtil.resetBlendState();
        
        for (Particle p : particles) {
            UnifiedCoord pos = new UnifiedCoord(p.x, p.y);
            float glX = pos.glX();
            float glY = pos.glY();
            
            float[] c = ColorHelper.toGLComponents(p.color, p.alpha * alphaMult);
            GL11.glColor4f(c[0], c[1], c[2], c[3]);
            
            float halfSize = p.scale / 2f;
            
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(glX - halfSize, glY - halfSize);
            GL11.glVertex2f(glX + halfSize, glY - halfSize);
            GL11.glVertex2f(glX + halfSize, glY + halfSize);
            GL11.glVertex2f(glX - halfSize, glY + halfSize);
            GL11.glEnd();
        }
        
        GLStateUtil.resetColor();
    }
    
    private void renderShockwave(float alphaMult) {
        if (!shockwaveActive) return;
        
        GLStateUtil.resetBlendState();
        
        float progress = shockwaveElapsed / SHOCKWAVE_DURATION;
        float currentRadius = SHOCKWAVE_MAX_RADIUS * progress;
        float alpha = (1f - progress) * 0.5f * alphaMult;
        
        UnifiedCoord pos = new UnifiedCoord(shockwaveX, shockwaveY);
        float glX = pos.glX();
        float glY = pos.glY();
        
        GL11.glLineWidth(3f);
        float[] c = ColorHelper.toGLComponents(COLOR_WHITE, alpha);
        GL11.glColor4f(c[0], c[1], c[2], c[3]);
        
        int segments = 32;
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < segments; i++) {
            float angle = (float)(2 * Math.PI * i / segments);
            float px = glX + (float)Math.cos(angle) * currentRadius;
            float py = glY + (float)Math.sin(angle) * currentRadius;
            GL11.glVertex2f(px, py);
        }
        GL11.glEnd();
        
        GLStateUtil.resetColor();
    }
    
    public boolean isComplete() {
        return !flashActive && particles.isEmpty() && !shockwaveActive;
    }
    
    public void clear() {
        flashActive = false;
        particles.clear();
        shockwaveActive = false;
    }
    
    private static class Particle {
        float x;
        float y;
        float vx;
        float vy;
        float scale;
        float alpha;
        float lifetime;
        float elapsed;
        Color color;
    }
}