package data.scripts.cosmicon.util;

import org.lwjgl.opengl.GL11;

public final class GLStateUtil {
    private GLStateUtil() {}

    public static void resetBlendState() {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void enableTexturing() {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    public static void disableTexturing() {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    public static void enableBlend() {
        GL11.glEnable(GL11.GL_BLEND);
    }

    public static void disableBlend() {
        GL11.glDisable(GL11.GL_BLEND);
    }

    public static void setBlendFuncStandard() {
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void enableTexturingWithBlend() {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void resetColor() {
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }
}