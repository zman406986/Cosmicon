package data.scripts.cosmicon.tutorial;

import java.awt.Color;
import java.util.List;

import org.lwjgl.opengl.GL11;

import data.scripts.cosmicon.battle.AnimationConstants;
import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.BattleUIButtons;
import data.scripts.cosmicon.battle.DiceAnimator;
import data.scripts.cosmicon.battle.DiceRollManager;
import data.scripts.cosmicon.util.ColorHelper;
import data.scripts.cosmicon.util.GLStateUtil;
import data.scripts.cosmicon.util.UnifiedCoord;

public class TutorialIndicationRenderer {

    private static final float PULSE_SPEED = 3f;
    private static final float DICE_INSET = 2f;
    private static final float BTN_PADDING = 4f;
    private static final float DICE_FILL_ALPHA = 0.15f;

    private TutorialController controller;
    private DiceRollManager diceRollManager;
    private BattleUIButtons buttons;
    private BattleState battleState;
    private float pulseTimer;

    public void init(TutorialController controller, DiceRollManager diceRollManager,
                     BattleUIButtons buttons, BattleState battleState) {
        this.controller = controller;
        this.diceRollManager = diceRollManager;
        this.buttons = buttons;
        this.battleState = battleState;
        this.pulseTimer = 0f;
    }

    public void advance(float amount) {
        pulseTimer += amount * PULSE_SPEED;
    }

    public void render(float alphaMult) {
        if (controller == null || controller.isComplete()) return;

        float pulse = 0.6f + 0.4f * (float) Math.sin(pulseTimer);

        renderDiceIndications(alphaMult, pulse);
        renderButtonIndications(alphaMult, pulse);
    }

    private void renderDiceIndications(float alphaMult, float pulse) {
        List<Integer> indices = controller.getIndicatedDiceIndices();
        if (indices.isEmpty()) return;

        List<DiceAnimator> animators = diceRollManager.getAnimators();
        if (animators == null || animators.isEmpty()) return;

        GLStateUtil.resetBlendState();

        float[] c = ColorHelper.toGLComponents(ColorHelper.TUTORIAL_INDICATION, alphaMult);
        float outlineAlpha = c[3] * pulse;
        float fillAlpha = DICE_FILL_ALPHA * alphaMult * pulse;

        for (int idx : indices) {
            if (idx < 0 || idx >= animators.size()) continue;
            DiceAnimator animator = animators.get(idx);
            if (animator == null) continue;

            float visX = animator.getVisualX();
            float visY = animator.getVisualY();
            float displaySize = animator.getDisplaySize();
            float centeringOffset = (AnimationConstants.DICE_SIZE - displaySize) / 2f;
            float boxX = visX - centeringOffset + DICE_INSET;
            float boxY = visY - centeringOffset + DICE_INSET;
            float boxSize = AnimationConstants.DICE_SIZE - DICE_INSET * 2f;

            UnifiedCoord dicePos = new UnifiedCoord(boxX, boxY).bottomLeft(boxSize);
            float glX = dicePos.glX();
            float glY = dicePos.glY();

            GL11.glColor4f(c[0], c[1], c[2], fillAlpha);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(glX, glY);
            GL11.glVertex2f(glX + boxSize, glY);
            GL11.glVertex2f(glX + boxSize, glY + boxSize);
            GL11.glVertex2f(glX, glY + boxSize);
            GL11.glEnd();

            GL11.glColor4f(c[0], c[1], c[2], outlineAlpha);
            GL11.glLineWidth(3f);
            GL11.glBegin(GL11.GL_LINE_LOOP);
            GL11.glVertex2f(glX, glY);
            GL11.glVertex2f(glX + boxSize, glY);
            GL11.glVertex2f(glX + boxSize, glY + boxSize);
            GL11.glVertex2f(glX, glY + boxSize);
            GL11.glEnd();
        }

        GLStateUtil.resetColor();
    }

    private void renderButtonIndications(float alphaMult, float pulse) {
        boolean indicateConfirm = controller.shouldIndicateConfirmButton();
        boolean indicateReroll = controller.shouldIndicateRerollButton();
        boolean indicatePrismatic = controller.shouldIndicatePrismaticButton();

        if (!indicateConfirm && !indicateReroll && !indicatePrismatic) return;

        GLStateUtil.resetBlendState();

        float[] c = ColorHelper.toGLComponents(ColorHelper.TUTORIAL_INDICATION, alphaMult);
        float outlineAlpha = c[3] * pulse;

        GL11.glColor4f(c[0], c[1], c[2], outlineAlpha);
        GL11.glLineWidth(2f);

        if (indicateConfirm) {
            renderButtonOutline(buttons.getConfirmBtnX(), buttons.getConfirmBtnY(),
                buttons.getConfirmBtnW(), buttons.getConfirmBtnH());
        }

        if (indicateReroll) {
            renderButtonOutline(buttons.getRerollBtnX(), buttons.getRerollBtnY(),
                buttons.getRerollBtnW(), buttons.getRerollBtnH());
        }

        if (indicatePrismatic) {
            renderButtonOutline(buttons.getPlayerPrismaticBtnX(), buttons.getPlayerPrismaticBtnY(),
                40f, 40f);
        }

        GLStateUtil.resetColor();
    }

    private void renderButtonOutline(float uiX, float uiY, float w, float h) {
        float padX = BTN_PADDING;
        float padY = BTN_PADDING;
        float totalW = w + padX * 2f;
        float totalH = h + padY * 2f;
        UnifiedCoord pos = new UnifiedCoord(uiX - padX, uiY - padY).bottomLeft(totalH);
        float glX = pos.glX();
        float glY = pos.glY();

        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(glX, glY);
        GL11.glVertex2f(glX + totalW, glY);
        GL11.glVertex2f(glX + totalW, glY + totalH);
        GL11.glVertex2f(glX, glY + totalH);
        GL11.glEnd();
    }

    public void cleanup() {
        controller = null;
        diceRollManager = null;
        buttons = null;
        battleState = null;
    }
}
