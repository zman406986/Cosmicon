package data.scripts.cosmicon.util;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

public final class PopupRenderer {
    private PopupRenderer() {}

    public static void drawPopupBackground(float x, float y, float width, float height, Color bgColor, Color borderColor, float alphaMult) {
        GLStateUtil.resetBlendState();

        float[] bg = ColorHelper.toGLComponents(bgColor, alphaMult);
        GL11.glColor4f(bg[0], bg[1], bg[2], bg[3]);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + width, y);
        GL11.glVertex2f(x + width, y + height);
        GL11.glVertex2f(x, y + height);
        GL11.glEnd();

        float[] border = ColorHelper.toGLComponents(borderColor, alphaMult * 0.8f);
        GL11.glColor4f(border[0], border[1], border[2], border[3]);
        GL11.glLineWidth(2f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + width, y);
        GL11.glVertex2f(x + width, y + height);
        GL11.glVertex2f(x, y + height);
        GL11.glEnd();

        GLStateUtil.resetColor();
        GL11.glLineWidth(1f);
    }
}