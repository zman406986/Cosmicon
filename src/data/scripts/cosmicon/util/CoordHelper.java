package data.scripts.cosmicon.util;

/**
 * Coordinate conversions for Starsector UI panels.
 * UI: Y=0 at TOP, increases DOWN (labels, buttons, inTL positioning)
 * GL: Y=0 at BOTTOM, increases UP (sprites, shapes, mouse)
 * Sprites anchor BOTTOM-LEFT; Labels/Buttons anchor TOP-LEFT.
 */
public final class CoordHelper {
    private CoordHelper() {}
    
    public static float uiToGlY(float panelGlY, float panelHeight, float uiY) {
        return panelGlY + panelHeight - uiY;
    }
    
    public static float glToUiY(float panelGlY, float panelHeight, float glY) {
        return panelGlY + panelHeight - glY;
    }
    
    public static float uiTopLeftToGlSpriteY(float panelGlY, float panelHeight,
            float uiTopLeftY, float spriteHeight) {
        float uiBottomY = uiTopLeftY + spriteHeight;
        return uiToGlY(panelGlY, panelHeight, uiBottomY);
    }
    
    /**
     * Converts mouse screen coordinates to panel-local UI coordinates.
     * Mouse coordinates are in GL space (Y=0 at bottom).
     * UI coordinates have Y=0 at top, increasing downward.
     */
    public static float[] mouseToPanelUi(int mouseX, int mouseY,
            float panelX, float panelY, float panelHeight) {
        float uiX = mouseX - panelX;
        float uiY = panelHeight - (mouseY - panelY);
        return new float[] { uiX, uiY };
    }
    
    public static boolean isInsideUiRect(float uiX, float uiY,
            float rectUiX, float rectUiY, float rectW, float rectH) {
        return uiX >= rectUiX && uiX <= rectUiX + rectW
            && uiY >= rectUiY && uiY <= rectUiY + rectH;
    }
}