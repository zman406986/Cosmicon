package data.scripts.cosmicon.battle;

import org.lwjgl.opengl.GL11;
import com.fs.starfarer.api.graphics.SpriteAPI;

public class DiceSpriteRenderer {
    public static void render(SpriteAPI sprite, float x, float y, float alphaMult, float scale, float size) {
        if (sprite == null) return;
        
        float scaledSize = size * scale;
        
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        sprite.bindTexture();
        
        GL11.glColor4f(1f, 1f, 1f, alphaMult);
        
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0f, 0f);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(1f, 0f);
        GL11.glVertex2f(x + scaledSize, y);
        GL11.glTexCoord2f(1f, 1f);
        GL11.glVertex2f(x + scaledSize, y + scaledSize);
        GL11.glTexCoord2f(0f, 1f);
        GL11.glVertex2f(x, y + scaledSize);
        GL11.glEnd();
        
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }
    
    public static void render(SpriteAPI sprite, float x, float y, float alphaMult, float scale, float size, float rotationDegrees) {
        if (sprite == null) return;
        
        if (rotationDegrees == 0f) {
            render(sprite, x, y, alphaMult, scale, size);
            return;
        }
        
        float scaledSize = size * scale;
        float halfSize = scaledSize / 2f;
        float centerX = x + halfSize;
        float centerY = y + halfSize;
        
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        sprite.bindTexture();
        
        GL11.glColor4f(1f, 1f, 1f, alphaMult);
        
        GL11.glPushMatrix();
        GL11.glTranslatef(centerX, centerY, 0f);
        GL11.glRotatef(rotationDegrees, 0f, 0f, 1f);
        GL11.glTranslatef(-halfSize, -halfSize, 0f);
        
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0f, 0f);
        GL11.glVertex2f(0f, 0f);
        GL11.glTexCoord2f(1f, 0f);
        GL11.glVertex2f(scaledSize, 0f);
        GL11.glTexCoord2f(1f, 1f);
        GL11.glVertex2f(scaledSize, scaledSize);
        GL11.glTexCoord2f(0f, 1f);
        GL11.glVertex2f(0f, scaledSize);
        GL11.glEnd();
        
        GL11.glPopMatrix();
        
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }
}