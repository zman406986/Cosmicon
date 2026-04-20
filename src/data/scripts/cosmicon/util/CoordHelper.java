package data.scripts.cosmicon.util;

import com.fs.starfarer.api.ui.PositionAPI;

/**
 * Unified coordinate system helper for Starsector custom UI panels.
 * 
 * THREE COORDINATE SYSTEMS:
 * ┌─────────────────────────────────────────────────────────────────┐
 * │ UI:    Y=0 at TOP, Y increases DOWN (inTL, Labels, Buttons)     │
 * │ GL:    Y=0 at BOTTOM, Y increases UP (render, sprites, mouse)   │
 * │ Mouse: Same as GL                                                │
 * └─────────────────────────────────────────────────────────────────┘
 * 
 * DIAGRAM (Panel at GL position panelX, panelY):
 * ┌──────────────────────────┐ ← UI Y=0, GL Y=panelY+height
 * │                          │
 * │        PANEL AREA        │
 * │                          │
 * └──────────────────────────┘ ← UI Y=height, GL Y=panelY
 * 
 * USAGE PATTERN:
 * 1. UI elements (labels, buttons): Use UI coords, position with inTL(x, y)
 *    where y is pixels from TOP of panel.
 * 
 * 2. GL rendering (sprites, shapes): Convert UI coords to GL before render:
 *    float glY = CoordHelper.uiToGlY(pos, uiY);
 *    sprite.render(glX, glY);  // Sprite uses BOTTOM-LEFT anchor
 * 
 * 3. Mouse input: Convert mouse GL coords to panel-relative UI coords:
 *    float[] uiPos = CoordHelper.mouseToPanelUi(pos, mouseX, mouseY);
 * 
 * CRITICAL: Always pass PositionAPI or (panelGlY, height) explicitly.
 * NEVER use static mutable state for panel bounds - it causes multi-panel conflicts.
 * 
 * ELEMENT ANCHORS:
 * - Labels/Buttons: TOP-LEFT anchor (inTL positions top-left corner)
 * - Sprites: BOTTOM-LEFT anchor (render positions bottom-left corner)
 * - Shapes/Polygons: Usually CENTER anchor
 */
public final class CoordHelper {
    private CoordHelper() {}
    
    // ========================================================================
    // CORE Y CONVERSIONS
    // ========================================================================
    
    /**
     * Convert UI Y coordinate to GL Y coordinate.
     * 
     * @param panelGlY    GL Y of panel's bottom edge (from pos.getY())
     * @param panelHeight Panel height (from pos.getHeight())
     * @param uiY         UI Y coordinate (pixels from TOP of panel)
     * @return GL Y coordinate for rendering
     * 
     * Example: If panel bottom is at GL Y=100, height=700:
     *   uiY=0 (top)    → glY=800 (panel top in GL)
     *   uiY=350 (mid)  → glY=450 (panel center in GL)
     *   uiY=700 (bot)  → glY=100 (panel bottom in GL)
     */
    public static float uiToGlY(float panelGlY, float panelHeight, float uiY) {
        return panelGlY + panelHeight - uiY;
    }
    
    /**
     * Convert UI Y coordinate to GL Y using PositionAPI.
     * Convenience method that extracts panel bounds from position.
     */
    public static float uiToGlY(PositionAPI pos, float uiY) {
        return uiToGlY(pos.getY(), pos.getHeight(), uiY);
    }
    
    /**
     * Convert GL Y coordinate to UI Y coordinate.
     * 
     * @param panelGlY    GL Y of panel's bottom edge
     * @param panelHeight Panel height
     * @param glY         GL Y coordinate
     * @return UI Y coordinate (pixels from TOP of panel)
     */
    public static float glToUiY(float panelGlY, float panelHeight, float glY) {
        return panelGlY + panelHeight - glY;
    }
    
    /**
     * Convert GL Y coordinate to UI Y using PositionAPI.
     */
    public static float glToUiY(PositionAPI pos, float glY) {
        return glToUiY(pos.getY(), pos.getHeight(), glY);
    }
    
    // ========================================================================
    // ELEMENT CENTERING HELPERS
    // ========================================================================
    
    /**
     * Get GL Y for centering an element vertically.
     * Use when rendering shapes/polygons centered on a point.
     * 
     * @param uiTopLeftY  UI Y of element's TOP edge (from inTL positioning)
     * @param elementHeight Height of the element
     * @return GL Y of the element's CENTER
     */
    public static float uiTopLeftToGlCenterY(float panelGlY, float panelHeight,
            float uiTopLeftY, float elementHeight) {
        float uiCenterY = uiTopLeftY + elementHeight / 2f;
        return uiToGlY(panelGlY, panelHeight, uiCenterY);
    }
    
    /**
     * Get GL Y for centering an element using PositionAPI.
     */
    public static float uiTopLeftToGlCenterY(PositionAPI pos,
            float uiTopLeftY, float elementHeight) {
        return uiTopLeftToGlCenterY(pos.getY(), pos.getHeight(), uiTopLeftY, elementHeight);
    }
    
    /**
     * Get GL Y for sprite rendering from UI TOP-LEFT position.
     * Sprites use BOTTOM-LEFT anchor, so this converts TOP-LEFT to BOTTOM-LEFT.
     * 
     * @param uiTopLeftY  UI Y of sprite's desired TOP edge
     * @param spriteHeight Height of the sprite
     * @return GL Y for sprite.render() call (BOTTOM-LEFT position)
     */
    public static float uiTopLeftToGlSpriteY(float panelGlY, float panelHeight,
            float uiTopLeftY, float spriteHeight) {
        float uiBottomY = uiTopLeftY + spriteHeight;
        return uiToGlY(panelGlY, panelHeight, uiBottomY);
    }
    
    /**
     * Get GL Y for sprite rendering using PositionAPI.
     */
    public static float uiTopLeftToGlSpriteY(PositionAPI pos,
            float uiTopLeftY, float spriteHeight) {
        return uiTopLeftToGlSpriteY(pos.getY(), pos.getHeight(), uiTopLeftY, spriteHeight);
    }
    
    // ========================================================================
    // RECTANGLE CONVERSIONS
    // ========================================================================
    
    /**
     * Immutable result container for rectangle conversions.
     * Provides convenient accessors for common rendering scenarios.
     */
    public static final class GlRect {
        public final float x;
        public final float y;
        public final float width;
        public final float height;
        
        public GlRect(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
        
        /** GL Y of TOP edge (for rendering borders at top) */
        public float topY() { return y + height; }
        
        /** GL Y of CENTER (for centered shape rendering) */
        public float centerY() { return y + height / 2f; }
        
        /** GL X of CENTER */
        public float centerX() { return x + width / 2f; }
        
        /** GL Y of BOTTOM edge (same as y field) */
        public float bottomY() { return y; }
        
        /** GL X of RIGHT edge */
        public float rightX() { return x + width; }
    }
    
    /**
     * Convert UI rectangle (TOP-LEFT anchor) to GL rectangle (BOTTOM-LEFT anchor).
     * 
     * @param uiX         UI X (left edge, same as GL X)
     * @param uiY         UI Y (TOP edge in UI coords)
     * @param width       Width (same in both systems)
     * @param height      Height (same in both systems)
     * @return GlRect with GL coordinates: y is BOTTOM edge in GL coords
     */
    public static GlRect uiRectToGl(float panelGlY, float panelHeight,
            float uiX, float uiY, float width, float height) {
        float glY = uiToGlY(panelGlY, panelHeight, uiY + height);
        return new GlRect(uiX, glY, width, height);
    }
    
    /**
     * Convert UI rectangle using PositionAPI.
     */
    public static GlRect uiRectToGl(PositionAPI pos,
            float uiX, float uiY, float width, float height) {
        return uiRectToGl(pos.getY(), pos.getHeight(), uiX, uiY, width, height);
    }
    
    // ========================================================================
    // MOUSE INPUT HELPERS
    // ========================================================================
    
    /**
     * Convert mouse GL coordinates to panel-relative UI coordinates.
     * Use for click detection against UI-positioned elements (labels, hitboxes).
     * 
     * Result: x=0 at panel's left edge, y=0 at panel's TOP edge.
     * 
     * @param mouseX      Mouse GL X (from Mouse.getX())
     * @param mouseY      Mouse GL Y (from Mouse.getY())
     * @param panelGlX    Panel GL X (from pos.getX())
     * @param panelGlY    Panel GL Y (from pos.getY())
     * @param panelWidth  Panel width
     * @param panelHeight Panel height
     * @return float[2]: index 0 = panel-relative UI X, index 1 = panel-relative UI Y
     */
    public static float[] mouseToPanelUi(int mouseX, int mouseY,
            float panelGlX, float panelGlY, float panelWidth, float panelHeight) {
        float uiX = mouseX - panelGlX;
        float uiY = glToUiY(panelGlY, panelHeight, mouseY);
        return new float[] { uiX, uiY };
    }
    
    /**
     * Convert mouse coordinates using PositionAPI.
     */
    public static float[] mouseToPanelUi(int mouseX, int mouseY, PositionAPI pos) {
        return mouseToPanelUi(mouseX, mouseY,
            pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight());
    }
    
    /**
     * Check if panel-relative UI coords are inside a UI rectangle.
     * Use for click/hitbox detection after converting mouse to UI coords.
     * 
     * @param uiX, uiY   Point to check (panel-relative UI coords from mouseToPanelUi)
     * @param rectUiX    Rectangle UI X (left edge)
     * @param rectUiY    Rectangle UI Y (TOP edge)
     * @param rectW      Rectangle width
     * @param rectH      Rectangle height
     * @return true if point is inside rectangle
     */
    public static boolean isInsideUiRect(float uiX, float uiY,
            float rectUiX, float rectUiY, float rectW, float rectH) {
        return uiX >= rectUiX && uiX <= rectUiX + rectW
            && uiY >= rectUiY && uiY <= rectUiY + rectH;
    }
    
    // ========================================================================
    // PANEL CENTER HELPERS
    // ========================================================================
    
    /**
     * Get UI Y for an element centered vertically in the panel.
     * 
     * @param elementHeight Height of the element to center
     * @param panelHeight   Panel height
     * @return UI Y (TOP edge) that centers the element vertically
     */
    public static float centeredUiY(float elementHeight, float panelHeight) {
        return (panelHeight - elementHeight) / 2f;
    }
    
    /**
     * Get UI X for an element centered horizontally in the panel.
     * 
     * @param elementWidth Width of the element to center
     * @param panelWidth   Panel width
     * @return UI X (left edge) that centers the element horizontally
     */
    public static float centeredUiX(float elementWidth, float panelWidth) {
        return (panelWidth - elementWidth) / 2f;
    }
    
    /**
     * Get GL Y for the vertical center of the panel.
     * Useful for centered shape/polygon rendering.
     */
    public static float panelCenterGlY(float panelGlY, float panelHeight) {
        return panelGlY + panelHeight / 2f;
    }
    
    /**
     * Get GL X for the horizontal center of the panel.
     */
    public static float panelCenterGlX(float panelGlX, float panelWidth) {
        return panelGlX + panelWidth / 2f;
    }
}