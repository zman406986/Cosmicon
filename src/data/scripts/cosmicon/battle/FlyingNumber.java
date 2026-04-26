package data.scripts.cosmicon.battle;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import data.scripts.cosmicon.util.ColorHelper;
import data.scripts.cosmicon.util.EasingUtil;
import data.scripts.cosmicon.util.GLStateUtil;
import data.scripts.cosmicon.util.UnifiedCoord;

public class FlyingNumber {
    public static final Color DAMAGE_RESULT = new Color(255, 200, 50);
    
    private static final float IMPACT_DURATION = 0.15f;
    private static final float IMPACT_SCALE_MAX = 1.3f;
    private static final float SHATTER_DURATION = 0.4f;
    private static final int SHATTER_MIN_PARTICLES = 8;
    private static final int SHATTER_MAX_PARTICLES = 12;
    private static final float SHATTER_SPEED = 60f;
    private static final float LABEL_HEIGHT = 30f;
    
    private enum Phase { WAITING, FLYING, IMPACT, SHATTER, COMPLETE }
    
    private float startX;
    private float startY;
    private float targetX;
    private float targetY;
    private float currentX;
    private float currentY;

    private String displayText;
    private Color color;
    
    private Phase phase;
    private float elapsed;
    private float flyDuration;
    private float scale;
    
    private boolean shatterOnImpact;
    private final List<Particle> particles;
    private final Random random;
    
    private LabelAPI label;
    private CustomPanelAPI labelPanel;
    private boolean labelCreated;
    
    private static class Particle {
        float x;
        float y;
        float vx;
        float vy;
        float scale;
        float alpha;
        float lifetime;
        float elapsed;
        
        Particle(float x, float y, float vx, float vy, float scale, float lifetime) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.scale = scale;
            this.alpha = 1f;
            this.lifetime = lifetime;
            this.elapsed = 0f;
        }
    }
    
    public FlyingNumber() {
        phase = Phase.WAITING;
        elapsed = 0f;
        scale = 1f;
        flyDuration = 0.6f;
        shatterOnImpact = false;
        particles = new ArrayList<>();
        random = new Random();
        labelCreated = false;
        displayText = "";
    }
    
    public void startFrom(float startX, float startY) {
        this.startX = startX;
        this.startY = startY;
        this.currentX = startX;
        this.currentY = startY;
    }
    
    public void flyTo(float targetX, float targetY, float duration) {
        this.targetX = targetX;
        this.targetY = targetY;
        this.flyDuration = duration;
        this.phase = Phase.FLYING;
        this.elapsed = 0f;
    }
    
    public void setValue(int value) {
        this.displayText = String.valueOf(value);
        updateLabelText();
    }
    
    public void setColor(Color color) {
        this.color = color;
        updateLabelColor();
    }
    
    public void setShatterOnImpact(boolean shatter) {
        this.shatterOnImpact = shatter;
    }
    
    public void advance(float amount) {
        if (phase == Phase.WAITING || phase == Phase.COMPLETE) return;
        
        elapsed += amount;
        
        switch (phase) {
            case FLYING -> advanceFlying();
            case IMPACT -> advanceImpact();
            case SHATTER -> advanceShatter(amount);
        }
        
        updateLabelPosition();
    }
    
    private void advanceFlying() {
        if (elapsed >= flyDuration) {
            currentX = targetX;
            currentY = targetY;
            elapsed = 0f;
            
            if (shatterOnImpact) {
                spawnShatterParticles();
                phase = Phase.SHATTER;
                if (label != null) {
                    label.setOpacity(0f);
                }
            } else {
                phase = Phase.IMPACT;
            }
            return;
        }
        
        float progress = elapsed / flyDuration;
        float eased = EasingUtil.easeOutQuad(progress);
        
        currentX = startX + (targetX - startX) * eased;
        currentY = startY + (targetY - startY) * eased;
        
        scale = 1f + 0.1f * (1f - eased);
    }
    
    private void advanceImpact() {
        if (elapsed >= IMPACT_DURATION) {
            scale = 1f;
            phase = Phase.COMPLETE;
            return;
        }
        
        float progress = elapsed / IMPACT_DURATION;
        float bounce = (float) Math.sin(progress * Math.PI);
        scale = 1f + (IMPACT_SCALE_MAX - 1f) * bounce;
    }
    
    private void advanceShatter(float amount) {
        if (elapsed >= SHATTER_DURATION) {
            phase = Phase.COMPLETE;
            particles.clear();
            return;
        }

        for (Particle p : particles)
        {
            p.elapsed += amount;
            p.x += p.vx * amount;
            p.y += p.vy * amount;
            p.alpha = Math.max(0f, 1f - (p.elapsed / p.lifetime));
            p.scale *= 0.98f;
        }
    }
    
    private void spawnShatterParticles() {
        particles.clear();
        int count = SHATTER_MIN_PARTICLES + random.nextInt(SHATTER_MAX_PARTICLES - SHATTER_MIN_PARTICLES + 1);
        
        for (int i = 0; i < count; i++) {
            float angle = (float) (random.nextFloat() * 2 * Math.PI);
            float speed = SHATTER_SPEED * (0.5f + random.nextFloat() * 0.5f);
            float vx = (float) Math.cos(angle) * speed;
            float vy = (float) Math.sin(angle) * speed;
            float pScale = 0.3f + random.nextFloat() * 0.4f;
            float lifetime = SHATTER_DURATION * (0.7f + random.nextFloat() * 0.3f);
            
            particles.add(new Particle(currentX, currentY, vx, vy, pScale, lifetime));
        }
    }
    
    public void render(float panelX, float panelY, float panelHeight, float alphaMult, CustomPanelAPI panel) {
        if (phase == Phase.WAITING) return;
        
        if (!labelCreated && panel != null && phase == Phase.FLYING) {
            createLabel(panel);
        }
        
        if (phase != Phase.SHATTER && phase != Phase.COMPLETE) {
            updateLabelOpacity(alphaMult);
        }
        
        if (phase == Phase.SHATTER) {
            // Use existing context if available, otherwise create one
            UnifiedCoord.PanelContext existingCtx = UnifiedCoord.getCurrentOrNull();
            boolean needsContextCleanup = existingCtx == null;
            
            if (needsContextCleanup) {
                UnifiedCoord.setCurrent(new UnifiedCoord.PanelContext(panelX, panelY, 0, panelHeight));
            }
            try {
                renderShatterParticles(alphaMult);
            } finally {
                if (needsContextCleanup) {
                    UnifiedCoord.clearCurrent();
                }
            }
        }
    }
    
    private void createLabel(CustomPanelAPI panel) {
        if (labelCreated) return;
        
        SettingsAPI settings = Global.getSettings();
        label = settings.createLabel(displayText, Fonts.INSIGNIA_LARGE);
        label.setColor(color);
        label.setAlignment(Alignment.MID);
        
        float labelWidth = label.computeTextWidth(displayText) + 10f;
        
        panel.addComponent((UIComponentAPI) label)
            .setSize(labelWidth, LABEL_HEIGHT)
            .inTL(currentX - labelWidth / 2f, currentY - LABEL_HEIGHT / 2f);
        
        labelPanel = panel;
        labelCreated = true;
    }
    
    private void updateLabelText() {
        if (label != null) {
            label.setText(displayText);
        }
    }
    
    private void updateLabelColor() {
        if (label != null) {
            label.setColor(color);
        }
    }
    
    private void updateLabelPosition() {
        if (label != null && labelCreated) {
            float labelWidth = label.computeTextWidth(displayText) + 10f;
            ((UIComponentAPI) label).getPosition()
                .setSize(labelWidth, LABEL_HEIGHT)
                .inTL(currentX - labelWidth / 2f, currentY - LABEL_HEIGHT / 2f);
        }
    }
    
    private void updateLabelOpacity(float alphaMult) {
        if (label != null) {
            label.setOpacity(alphaMult);
        }
    }
    
    private void renderShatterParticles(float alphaMult) {
        for (Particle particle : particles)
        {
            GLStateUtil.resetBlendState();

            UnifiedCoord pos = new UnifiedCoord(particle.x, particle.y);
            float glX = pos.glX();
            float glY = pos.glY();

            float size = 8f * particle.scale;
            float halfSize = size / 2f;

            float[] c = ColorHelper.toGLComponents(color, alphaMult * particle.alpha);
            GL11.glColor4f(c[0], c[1], c[2], c[3]);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(glX - halfSize, glY - halfSize);
            GL11.glVertex2f(glX + halfSize, glY - halfSize);
            GL11.glVertex2f(glX + halfSize, glY + halfSize);
            GL11.glVertex2f(glX - halfSize, glY + halfSize);
            GL11.glEnd();
        }
        
        GLStateUtil.resetColor();
    }
    
    public boolean isComplete() {
        return phase == Phase.COMPLETE;
    }
    
    public boolean hasImpacted() {
        return phase == Phase.IMPACT || phase == Phase.SHATTER || phase == Phase.COMPLETE;
    }
    
    public float getScale() {
        return scale;
    }
    
    public void forceComplete() {
        phase = Phase.COMPLETE;
        elapsed = 0f;
        scale = 1f;
        currentX = targetX;
        currentY = targetY;
        particles.clear();
        if (label != null) {
            label.setOpacity(0f);
        }
    }
    
    public void cleanup() {
        if (label != null && labelPanel != null) {
            labelPanel.removeComponent((UIComponentAPI) label);
        }
        label = null;
        labelPanel = null;
        labelCreated = false;
        particles.clear();
    }
}