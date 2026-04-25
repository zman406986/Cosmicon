package data.scripts.cosmicon.util;

import com.fs.starfarer.api.Global;

/**
 * Coordinate conversions for Starsector UI panels.
 * COORDINATE SYSTEMS:
 * - UI coords: Y=0 at TOP, increases DOWN (used by inTL, labels, buttons)
 * - GL coords: Y=0 at BOTTOM, increases UP (used by render, sprites, Mouse.getX/Y)
 * - Mouse coords: GL coords scaled by screenScaleMult
 * UNIFIED APPROACH:
 * Define all positions in UI coords (like inTL), use CoordHelper for conversions.
 */
public final class CoordHelper {
    private CoordHelper() {}
    
    private static float getScreenScale() {
        return Global.getSettings().getScreenScaleMult();
    }
    
    /**
     * Converts UI Y to GL Y for rendering.
     * UI Y=0 (top of panel) → GL Y = panelGlY + panelHeight (top of panel in GL)
     */
    public static float uiToGlY(float panelGlY, float panelHeight, float uiY) {
        return panelGlY + panelHeight - uiY;
    }
    
    /**
     * Converts UI top-left position to GL sprite render Y.
     * Sprites anchor at BOTTOM-LEFT, so we need the UI bottom edge.
     */
    public static float uiTopLeftToGlSpriteY(float panelGlY, float panelHeight,
            float uiTopLeftY, float spriteHeight) {
        float uiBottomY = uiTopLeftY + spriteHeight;
        return uiToGlY(panelGlY, panelHeight, uiBottomY);
    }
    
    /**
     * Converts mouse coordinates to panel-local UI coordinates.
     * Mouse.getX/Y() returns scaled GL coordinates.
     * Result: UI coords (Y=0 at top, increases down), relative to panel origin.
     */
    public static float[] mouseToPanelUi(int mouseX, int mouseY,
            float panelX, float panelY, float panelHeight) {
        float scale = getScreenScale();
        float glX = mouseX / scale;
        float glY = mouseY / scale;
        
        float uiX = glX - panelX;
        float uiY = panelHeight - (glY - panelY);
        return new float[] { uiX, uiY };
    }
    
    /**
     * Tests if a point (in UI coords) is inside a UI-defined rectangle.
     */
    public static boolean isInsideUiRect(float uiX, float uiY,
            float rectUiX, float rectUiY, float rectW, float rectH) {
        return uiX >= rectUiX && uiX <= rectUiX + rectW
            && uiY >= rectUiY && uiY <= rectUiY + rectH;
    }
}