package data.scripts.cosmicon.util;

import com.fs.starfarer.api.Global;
import org.lwjgl.input.Mouse;

/** Read starsector reference api/UI tutorial concise.md first. UI coords: Y=0 at TOP (↓), GL coords: Y=0 at BOTTOM (↑). */
public final class UnifiedCoord {
    
    public record PanelContext(float panelX, float panelY, float panelWidth, float panelHeight, float screenScaleMult) {
        public PanelContext(float panelX, float panelY, float panelWidth, float panelHeight) {
            this(panelX, panelY, panelWidth, panelHeight, Global.getSettings().getScreenScaleMult());
        }
    }
    
    private static PanelContext currentContext;
    
    public static void setCurrent(PanelContext context) { currentContext = context; }
    public static PanelContext getCurrent() {
        if (currentContext == null) throw new IllegalStateException("PanelContext not set. Call setCurrent() first.");
        return currentContext;
    }
    public static PanelContext getCurrentOrNull() { return currentContext; }
    public static void clearCurrent() { currentContext = null; }
    
    private final float uiX, uiY;
    private final PanelContext context;
    
    public UnifiedCoord(float uiX, float uiY) { this.uiX = uiX; this.uiY = uiY; this.context = null; }
    public UnifiedCoord(float uiX, float uiY, PanelContext context) { this.uiX = uiX; this.uiY = uiY; this.context = context; }
    
    private PanelContext ctx() { return context != null ? context : getCurrent(); }
    
    public float uiX() { return uiX; }
    public float uiY() { return uiY; }
    
    public float glX() { return ctx().panelX() + uiX; }
    public float glY() { PanelContext c = ctx(); return c.panelY() + c.panelHeight() - uiY; }
    public float glSpriteY(float spriteHeight) { PanelContext c = ctx(); return c.panelY() + c.panelHeight() - uiY - spriteHeight; }
    
    public boolean isInsideRect(float rectX, float rectY, float rectW, float rectH) {
        return uiX >= rectX && uiX <= rectX + rectW && uiY >= rectY && uiY <= rectY + rectH;
    }
    
    public static UnifiedCoord fromMouse() {
        PanelContext c = getCurrent();
        float scale = c.screenScaleMult();
        float glX = Mouse.getX() / scale, glY = Mouse.getY() / scale;
        return new UnifiedCoord(glX - c.panelX(), c.panelHeight() - (glY - c.panelY()));
    }
    public static UnifiedCoord fromMouse(PanelContext ctx) {
        float scale = ctx.screenScaleMult();
        float glX = Mouse.getX() / scale, glY = Mouse.getY() / scale;
        return new UnifiedCoord(glX - ctx.panelX(), ctx.panelHeight() - (glY - ctx.panelY()), ctx);
    }
    
    public UnifiedCoord offset(float dx, float dy) { return new UnifiedCoord(uiX + dx, uiY + dy, context); }
    public UnifiedCoord bottomLeft(float height) { return new UnifiedCoord(uiX, uiY + height, context); }
    
    @Override public String toString() { return String.format("UnifiedCoord[uiX=%.1f, uiY=%.1f]", uiX, uiY); }
}