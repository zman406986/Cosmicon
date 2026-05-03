package data.scripts.cosmicon.tutorial;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;

import data.scripts.cosmicon.battle.BattleRenderingUtils;
import data.scripts.cosmicon.util.GLStateUtil;
import data.scripts.cosmicon.util.UIComponentFactory;
import data.scripts.cosmicon.util.UnifiedCoord;

public class TutorialUIRenderer {

    private static final float TEXT_BOX_WIDTH = 360f;
    private static final float TEXT_BOX_HEIGHT = 60f;
    private static final float TEXT_BOX_MARGIN = 10f;
    private static final float PULSE_SPEED = 3f;

    private TutorialController controller;
    private LabelAPI tutorialTextLabel;
    private CustomPanelAPI panel;
    private float pulseTimer;
    private String lastStepText;

    public void init(TutorialController controller, CustomPanelAPI panel) {
        this.controller = controller;
        this.panel = panel;
        this.pulseTimer = 0f;
        this.lastStepText = "";
    }

    public void advance(float amount) {
        pulseTimer += amount * PULSE_SPEED;
        updateTextLabel();
    }

    private void updateTextLabel() {
        if (controller == null || panel == null) return;

        String text = controller.getTutorialText();
        if (text.isEmpty()) {
            if (tutorialTextLabel != null && panel != null) {
                panel.removeComponent((UIComponentAPI) tutorialTextLabel);
                tutorialTextLabel = null;
            }
            return;
        }

        if (!text.equals(lastStepText)) {
            lastStepText = text;

            if (tutorialTextLabel != null) {
                panel.removeComponent((UIComponentAPI) tutorialTextLabel);
            }

            float labelX = (BattleRenderingUtils.PANEL_WIDTH - TEXT_BOX_WIDTH) / 2f + TEXT_BOX_MARGIN;
            float labelY = 40f;
            tutorialTextLabel = UIComponentFactory.createLabel(
                panel, text, Fonts.INSIGNIA_LARGE,
                new Color(255, 255, 220, 255), Alignment.MID,
                TEXT_BOX_WIDTH - 2f * TEXT_BOX_MARGIN, TEXT_BOX_HEIGHT,
                labelX, labelY
            );
        }
    }

    public void render(float panelX, float panelY, float panelW, float panelH, float alphaMult) {
        if (controller == null || controller.isTutorialComplete()) return;

        String text = controller.getTutorialText();
        if (text.isEmpty()) return;

        renderTextBox(alphaMult);
    }

    private void renderTextBox(float alphaMult) {
        GLStateUtil.resetBlendState();

        float boxW = TEXT_BOX_WIDTH;
        float boxH = TEXT_BOX_HEIGHT;
        float boxUiX = (BattleRenderingUtils.PANEL_WIDTH - boxW) / 2f;
        float boxUiY = 35f;

        UnifiedCoord boxPos = new UnifiedCoord(boxUiX, boxUiY);
        float glX = boxPos.glX();
        float glY = boxPos.glSpriteY(boxH);

        float bgAlpha = 0.75f * alphaMult;
        GL11.glColor4f(0.05f, 0.05f, 0.15f, bgAlpha);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(glX, glY);
        GL11.glVertex2f(glX + boxW, glY);
        GL11.glVertex2f(glX + boxW, glY + boxH);
        GL11.glVertex2f(glX, glY + boxH);
        GL11.glEnd();

        float borderAlpha = 0.9f * alphaMult;
        float pulse = 0.5f + 0.5f * (float) Math.sin(pulseTimer);
        float r = 0.8f + 0.2f * pulse;
        float g = 0.7f + 0.1f * pulse;
        float b = 0.2f;
        GL11.glColor4f(r, g, b, borderAlpha);
        GL11.glLineWidth(2f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(glX, glY);
        GL11.glVertex2f(glX + boxW, glY);
        GL11.glVertex2f(glX + boxW, glY + boxH);
        GL11.glVertex2f(glX, glY + boxH);
        GL11.glEnd();

        GLStateUtil.resetColor();
    }

    public void cleanup() {
        if (tutorialTextLabel != null && panel != null) {
            panel.removeComponent((UIComponentAPI) tutorialTextLabel);
        }
        tutorialTextLabel = null;
        panel = null;
        controller = null;
    }
}
