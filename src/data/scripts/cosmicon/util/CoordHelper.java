package data.scripts.cosmicon.util;

import com.fs.starfarer.api.ui.PositionAPI;

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
    
    public static float uiToGlY(PositionAPI pos, float uiY) {
        return uiToGlY(pos.getY(), pos.getHeight(), uiY);
    }
    
    public static float glToUiY(float panelGlY, float panelHeight, float glY) {
        return panelGlY + panelHeight - glY;
    }
    
    public static float glToUiY(PositionAPI pos, float glY) {
        return glToUiY(pos.getY(), pos.getHeight(), glY);
    }
    
    public static float uiTopLeftToGlCenterY(float panelGlY, float panelHeight,
            float uiTopLeftY, float elementHeight) {
        float uiCenterY = uiTopLeftY + elementHeight / 2f;
        return uiToGlY(panelGlY, panelHeight, uiCenterY);
    }
    
    public static float uiTopLeftToGlCenterY(PositionAPI pos,
            float uiTopLeftY, float elementHeight) {
        return uiTopLeftToGlCenterY(pos.getY(), pos.getHeight(), uiTopLeftY, elementHeight);
    }
    
    // Sprites use BOTTOM-LEFT anchor; this converts UI TOP-LEFT to sprite render Y
    public static float uiTopLeftToGlSpriteY(float panelGlY, float panelHeight,
            float uiTopLeftY, float spriteHeight) {
        float uiBottomY = uiTopLeftY + spriteHeight;
        return uiToGlY(panelGlY, panelHeight, uiBottomY);
    }
    
    public static float uiTopLeftToGlSpriteY(PositionAPI pos,
            float uiTopLeftY, float spriteHeight) {
        return uiTopLeftToGlSpriteY(pos.getY(), pos.getHeight(), uiTopLeftY, spriteHeight);
    }

    public record GlRect(float x, float y, float width, float height)
    {

        public float topY() {return y + height;}

        public float centerY() {return y + height / 2f;}

        public float centerX() {return x + width / 2f;}

        public float bottomY() {return y;}

        public float rightX() {return x + width;}
        }
    
    public static GlRect uiRectToGl(float panelGlY, float panelHeight,
            float uiX, float uiY, float width, float height) {
        float glY = uiToGlY(panelGlY, panelHeight, uiY + height);
        return new GlRect(uiX, glY, width, height);
    }
    
    public static GlRect uiRectToGl(PositionAPI pos,
            float uiX, float uiY, float width, float height) {
        return uiRectToGl(pos.getY(), pos.getHeight(), uiX, uiY, width, height);
    }
    
    // Returns panel-relative coords: x=0 at left, y=0 at TOP
    public static float[] mouseToPanelUi(int mouseX, int mouseY,
            float panelGlX, float panelGlY, float panelWidth, float panelHeight) {
        float uiX = mouseX - panelGlX;
        float uiY = glToUiY(panelGlY, panelHeight, mouseY);
        return new float[] { uiX, uiY };
    }
    
    public static float[] mouseToPanelUi(int mouseX, int mouseY, PositionAPI pos) {
        return mouseToPanelUi(mouseX, mouseY,
            pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight());
    }
    
    public static boolean isInsideUiRect(float uiX, float uiY,
            float rectUiX, float rectUiY, float rectW, float rectH) {
        return uiX >= rectUiX && uiX <= rectUiX + rectW
            && uiY >= rectUiY && uiY <= rectUiY + rectH;
    }
    
    public static float centeredUiY(float elementHeight, float panelHeight) {
        return (panelHeight - elementHeight) / 2f;
    }
    
    public static float centeredUiX(float elementWidth, float panelWidth) {
        return (panelWidth - elementWidth) / 2f;
    }
    
    public static float panelCenterGlY(float panelGlY, float panelHeight) {
        return panelGlY + panelHeight / 2f;
    }
    
    public static float panelCenterGlX(float panelGlX, float panelWidth) {
        return panelGlX + panelWidth / 2f;
    }
}