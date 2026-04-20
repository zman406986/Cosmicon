package data.scripts.cosmicon.battle;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import data.scripts.cosmicon.util.ColorHelper;
import data.scripts.cosmicon.util.CoordHelper;
import data.scripts.cosmicon.util.CosmiconRandom;
import data.scripts.cosmicon.util.GLStateUtil;

public class DiceAnimator {

    public static final float DICE_SIZE = 60f;

    private static final float ROLL_DURATION = 0.3f;
    private static final float SETTLE_DURATION = 0.15f;
    private static final float REVEAL_DURATION = 0.1f;
    private static final float TOTAL_DURATION = ROLL_DURATION + SETTLE_DURATION + REVEAL_DURATION;

    public static float getTotalDuration() {
        return TOTAL_DURATION;
    }

    private static final float JITTER_RANGE = 5f;
    private static final float SCALE_BOUNCE = 0.08f;

    private static final SettingsAPI settings = Global.getSettings();

    private DiceType type;
    private int finalValue;
    private float x;
    private float y;
    private float elapsed;
    private int currentValue;
    private int[] shuffledFaces;
    private int cycleIndex;
    private float jitterX;
    private float jitterY;
    private float scale;
    private boolean complete;

    private CustomPanelAPI panel;
    private LabelAPI numberLabel;
    private PositionAPI labelPosition;

    public DiceAnimator() {
        this.scale = 1f;
        this.complete = false;
    }

    public void init(CustomPanelAPI panel) {
        this.panel = panel;
        createLabel();
    }

    private void createLabel() {
        numberLabel = settings.createLabel("1", Fonts.DEFAULT_SMALL);
        numberLabel.setColor(Color.WHITE);
        numberLabel.setAlignment(Alignment.MID);
        labelPosition = panel.addComponent((UIComponentAPI) numberLabel)
            .setSize(DICE_SIZE, DICE_SIZE)
            .inTL(0f, 0f);
        numberLabel.setOpacity(0f);
    }

    public void start(DiceType type, int finalValue, float x, float y, float delay) {
        this.type = type;
        this.finalValue = finalValue;
        this.x = x;
        this.y = y;
        this.elapsed = -delay;
        this.shuffledFaces = shuffleFaces(type.getMaxFace());
        this.cycleIndex = 0;
        this.currentValue = shuffledFaces[0];
        this.jitterX = 0f;
        this.jitterY = 0f;
        this.scale = 1f;
        this.complete = false;

        if (numberLabel != null) {
            numberLabel.setText(String.valueOf(currentValue));
            numberLabel.setColor(type.getNumberColor());
            labelPosition.inTL(x, y);
            numberLabel.setOpacity(0f);
        }
    }

    public void reroll(int newFinalValue) {
        this.finalValue = newFinalValue;
        this.elapsed = 0f;
        this.shuffledFaces = shuffleFaces(type.getMaxFace());
        this.cycleIndex = 0;
        this.currentValue = shuffledFaces[0];
        this.jitterX = 0f;
        this.jitterY = 0f;
        this.scale = 1f;
        this.complete = false;

        if (numberLabel != null) {
            numberLabel.setText(String.valueOf(currentValue));
            numberLabel.setOpacity(1f);
        }
    }

    private int[] shuffleFaces(int maxFace) {
        int[] faces = new int[maxFace];
        for (int i = 0; i < maxFace; i++) {
            faces[i] = i + 1;
        }
        for (int i = faces.length - 1; i > 0; i--) {
            int j = CosmiconRandom.nextInt(i + 1);
            int tmp = faces[i];
            faces[i] = faces[j];
            faces[j] = tmp;
        }
        return faces;
    }

    public void advance(float amount) {
        if (complete) return;

        elapsed += amount;

        if (elapsed < 0f) {
            if (numberLabel != null) {
                numberLabel.setOpacity(0f);
            }
            return;
        }

        if (numberLabel != null && numberLabel.getOpacity() < 1f) {
            numberLabel.setOpacity(1f);
        }

        if (elapsed < ROLL_DURATION) {
            currentValue = shuffledFaces[cycleIndex++ % shuffledFaces.length];
            jitterX = (CosmiconRandom.nextFloat() * 2f - 1f) * JITTER_RANGE;
            jitterY = (CosmiconRandom.nextFloat() * 2f - 1f) * JITTER_RANGE;
        } else if (elapsed < ROLL_DURATION + SETTLE_DURATION) {
            float settleProgress = (elapsed - ROLL_DURATION) / SETTLE_DURATION;
            currentValue = shuffledFaces[cycleIndex++ % shuffledFaces.length];
            float jitterMult = 0.5f * (1f - settleProgress);
            jitterX *= jitterMult;
            jitterY *= jitterMult;
            if (CosmiconRandom.nextFloat() < 0.3f) {
                jitterX = (CosmiconRandom.nextFloat() * 2f - 1f) * JITTER_RANGE * jitterMult;
                jitterY = (CosmiconRandom.nextFloat() * 2f - 1f) * JITTER_RANGE * jitterMult;
            }
        } else if (elapsed < TOTAL_DURATION) {
            currentValue = finalValue;
            jitterX = 0f;
            jitterY = 0f;
            float revealProgress = (elapsed - ROLL_DURATION - SETTLE_DURATION) / REVEAL_DURATION;
            scale = 1f + SCALE_BOUNCE * (float) Math.sin(revealProgress * Math.PI);
        } else {
            currentValue = finalValue;
            jitterX = 0f;
            jitterY = 0f;
            scale = 1f;
            complete = true;
        }

        updateLabel();
    }

    private void updateLabel() {
        if (numberLabel != null && labelPosition != null) {
            numberLabel.setText(String.valueOf(currentValue));
            float offsetX = jitterX + (DICE_SIZE * (1f - scale)) / 2f;
            float offsetY = jitterY + (DICE_SIZE * (1f - scale)) / 2f;
            labelPosition.inTL(x + offsetX, y + offsetY);
        }
    }

    public void render(float panelX, float panelY, float panelHeight, float alphaMult) {
        if (elapsed < 0f) return;

        GLStateUtil.resetBlendState();

        float renderSize = DICE_SIZE * scale;
        float centerX = panelX + x + jitterX + DICE_SIZE / 2f;
        float centerY = CoordHelper.uiToGlY(panelY, panelHeight, y + jitterY + DICE_SIZE / 2f);

        Color bodyColor = type.getBodyColor();
        if (elapsed >= ROLL_DURATION + SETTLE_DURATION && !complete) {
            bodyColor = brightenColor(bodyColor);
        }

        renderDiceShape(centerX, centerY, renderSize, bodyColor, alphaMult);

        Color borderColor = ColorHelper.DICE_BORDER;
        renderDiceBorder(centerX, centerY, renderSize, borderColor, alphaMult);

        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    private void renderDiceShape(float cx, float cy, float size, Color color, float alphaMult) {
        float[] c = ColorHelper.toGLComponents(color, alphaMult);
        GL11.glColor4f(c[0], c[1], c[2], c[3]);

        int vertices = type.getVertices();
        float radius = size / 2f;
        float angleOffset = getAngleOffset();

        GL11.glBegin(GL11.GL_POLYGON);
        for (int i = 0; i < vertices; i++) {
            float angle = angleOffset + (float) (2 * Math.PI * i / vertices);
            float vx = cx + (float) Math.cos(angle) * radius;
            float vy = cy + (float) Math.sin(angle) * radius;
            GL11.glVertex2f(vx, vy);
        }
        GL11.glEnd();
    }

    private void renderDiceBorder(float cx, float cy, float size, Color color, float alphaMult) {
        float[] c = ColorHelper.toGLComponents(color, alphaMult);
        GL11.glColor4f(c[0], c[1], c[2], c[3]);
        GL11.glLineWidth(2f);

        int vertices = type.getVertices();
        float radius = size / 2f;
        float angleOffset = getAngleOffset();

        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < vertices; i++) {
            float angle = angleOffset + (float) (2 * Math.PI * i / vertices);
            float vx = cx + (float) Math.cos(angle) * radius;
            float vy = cy + (float) Math.sin(angle) * radius;
            GL11.glVertex2f(vx, vy);
        }
        GL11.glEnd();
    }

    private float getAngleOffset() {
        return switch (type) {
            case BLUE_D4, PRISMATIC_D12 -> (float) (-Math.PI / 2);
            case PURPLE_D6 -> (float) (-Math.PI / 4);
            case ORANGE_D8 -> 0f;
        };
    }

    private Color brightenColor(Color color) {
        int r = Math.min(255, (int) (color.getRed() * 1.3f));
        int g = Math.min(255, (int) (color.getGreen() * 1.3f));
        int b = Math.min(255, (int) (color.getBlue() * 1.3f));
        return new Color(r, g, b, color.getAlpha());
    }

    public boolean isComplete() {
        return complete;
    }

    public boolean isActive() {
        return elapsed >= 0f && !complete;
    }

    public void forceComplete() {
        elapsed = TOTAL_DURATION;
        currentValue = finalValue;
        jitterX = 0f;
        jitterY = 0f;
        scale = 1f;
        complete = true;
        updateLabel();
    }

    public LabelAPI getNumberLabel() {
        return numberLabel;
    }
}
