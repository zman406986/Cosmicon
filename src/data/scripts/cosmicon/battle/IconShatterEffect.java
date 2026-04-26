package data.scripts.cosmicon.battle;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import data.scripts.cosmicon.util.ColorHelper;
import data.scripts.cosmicon.util.GLStateUtil;
import data.scripts.cosmicon.util.UnifiedCoord;

public class IconShatterEffect {
    private static final float SHATTER_DURATION = 0.5f;
    private static final int MIN_PARTICLES = 12;
    private static final int MAX_PARTICLES = 20;
    
    private final List<ShatterParticle> particles;
    
    public IconShatterEffect() {
        particles = new ArrayList<>();
    }
    
    public void trigger(float centerX, float centerY, Color iconColor, float iconSize) {
        particles.clear();
        
        int particleCount = MIN_PARTICLES + (int)(Math.random() * (MAX_PARTICLES - MIN_PARTICLES + 1));
        
        for (int i = 0; i < particleCount; i++) {
            ShatterParticle p = new ShatterParticle();
            p.x = centerX;
            p.y = centerY;
            
            float angle = (float)(Math.random() * 2 * Math.PI);
            float speed = 40f + (float)(Math.random() * 80f);
            p.vx = (float)Math.cos(angle) * speed;
            p.vy = (float)Math.sin(angle) * speed;
            
            float sizeVariation = (float)Math.random();
            if (sizeVariation < 0.3f) {
                p.scale = iconSize * 0.25f + (float)(Math.random() * iconSize * 0.15f);
            } else if (sizeVariation < 0.7f) {
                p.scale = iconSize * 0.12f + (float)(Math.random() * iconSize * 0.1f);
            } else {
                p.scale = iconSize * 0.06f + (float)(Math.random() * iconSize * 0.05f);
            }
            
            p.alpha = 1f;
            p.lifetime = SHATTER_DURATION * (0.7f + (float)(Math.random() * 0.3f));
            p.elapsed = 0f;
            p.color = iconColor;
            p.rotation = (float)(Math.random() * 360f);
            p.rotationSpeed = -180f + (float)(Math.random() * 360f);
            
            particles.add(p);
        }
    }
    
    public void advance(float amount) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            ShatterParticle p = particles.get(i);
            p.elapsed += amount;
            p.x += p.vx * amount;
            p.y += p.vy * amount;
            p.vx *= 0.95f;
            p.vy *= 0.95f;
            p.rotation += p.rotationSpeed * amount;
            
            float progress = p.elapsed / p.lifetime;
            p.alpha = 1f - progress;
            p.scale *= (1f - 0.4f * amount);
            
            if (p.elapsed >= p.lifetime) {
                particles.remove(i);
            }
        }
    }
    
    public void render(float panelX, float panelY, float panelHeight, float alphaMult) {
        if (particles.isEmpty()) return;
        
        // Use existing context if available, otherwise create one
        UnifiedCoord.PanelContext existingCtx = UnifiedCoord.getCurrentOrNull();
        boolean needsContextCleanup = existingCtx == null;
        
        if (needsContextCleanup) {
            UnifiedCoord.setCurrent(new UnifiedCoord.PanelContext(panelX, panelY, 0, panelHeight));
        }
        try {
            GLStateUtil.resetBlendState();
            
            for (ShatterParticle p : particles) {
                GLStateUtil.resetBlendState();
                
                UnifiedCoord pos = new UnifiedCoord(p.x, p.y);
                float glX = pos.glX();
                float glY = pos.glY();
                
                float[] c = ColorHelper.toGLComponents(p.color, p.alpha * alphaMult);
                GL11.glColor4f(c[0], c[1], c[2], c[3]);
                
                float halfSize = p.scale / 2f;
                float radians = (float)Math.toRadians(p.rotation);
                float cos = (float)Math.cos(radians);
                float sin = (float)Math.sin(radians);
                
                float[] corners = {
                    -halfSize, -halfSize,
                    halfSize, -halfSize,
                    halfSize, halfSize,
                    -halfSize, halfSize
                };
                
                GL11.glBegin(GL11.GL_QUADS);
                for (int i = 0; i < 4; i++) {
                    float localX = corners[i * 2];
                    float localY = corners[i * 2 + 1];
                    float rotatedX = localX * cos - localY * sin;
                    float rotatedY = localX * sin + localY * cos;
                    GL11.glVertex2f(glX + rotatedX, glY + rotatedY);
                }
                GL11.glEnd();
            }
            
            GLStateUtil.resetColor();
        } finally {
            if (needsContextCleanup) {
                UnifiedCoord.clearCurrent();
            }
        }
    }
    
    public boolean isComplete() {
        return particles.isEmpty();
    }
    
    public void clear() {
        particles.clear();
    }
    
    private static class ShatterParticle {
        float x;
        float y;
        float vx;
        float vy;
        float scale;
        float alpha;
        float lifetime;
        float elapsed;
        Color color;
        float rotation;
        float rotationSpeed;
    }
}