package data.scripts.cosmicon.battle;

import java.awt.Color;
import java.util.List;

import org.lwjgl.input.Mouse;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate.DialogCallbacks;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.Strings;
import data.scripts.cosmicon.util.ColorHelper;
import data.scripts.cosmicon.util.GLStateUtil;
import data.scripts.cosmicon.util.UIComponentFactory;

public class CoinFlipPanelUI extends BaseCustomUIPanelPlugin {

    private static final float FLIP_HINT_Y_OFFSET = 210f;
    private static final float RESULT_TEXT_Y_OFFSET = 180f;
    private static final float CLICK_HINT_Y_OFFSET = 220f;

    private CustomPanelAPI panel;
    private DialogCallbacks callbacks;
    private CoinFlipAnimator animator;

    private LabelAPI flipHintLabel;
    private LabelAPI resultLabel;
    private LabelAPI clickHintLabel;

    private boolean dismissed;
    private int lastMouseButtonState;
    private float postRevealTimer;
    private static final float POST_REVEAL_DELAY = 1.0f;

    public CoinFlipPanelUI() {
        this.lastMouseButtonState = 0;
        this.postRevealTimer = 0f;
        this.dismissed = false;
    }

    public void init(CustomPanelAPI panel, DialogCallbacks callbacks) {
        this.panel = panel;
        this.callbacks = callbacks;
        this.animator = new CoinFlipAnimator();

        callbacks.getPanelFader().setDurationOut(0.3f);

        createLabels();
    }

    private void createLabels() {
        PositionAPI pos = panel.getPosition();
        float w = pos.getWidth();
        float h = pos.getHeight();

        float flipHintY = h / 2f - FLIP_HINT_Y_OFFSET;

        flipHintLabel = UIComponentFactory.createLabel(
            panel, Strings.get("coinflip.flipping"), Fonts.INSIGNIA_VERY_LARGE,
            ColorHelper.PHASE_LABEL, Alignment.MID, w, 40f, 0f, flipHintY);
        flipHintLabel.setOpacity(0.9f);

        resultLabel = UIComponentFactory.createLabel(
            panel, "", Fonts.INSIGNIA_LARGE,
            Color.WHITE, Alignment.MID, w, 40f, 0f, h / 2f - RESULT_TEXT_Y_OFFSET);
        resultLabel.setOpacity(0f);

        clickHintLabel = UIComponentFactory.createLabel(
            panel, Strings.get("coinflip.click_to_skip"), Fonts.DEFAULT_SMALL,
            new Color(180, 180, 180), Alignment.MID, w, 20f, 0f, h / 2f - CLICK_HINT_Y_OFFSET);
        clickHintLabel.setOpacity(0.6f);
    }

    @Override
    public void advance(float amount) {
        if (dismissed || animator == null) return;

        animator.advance(amount);

        if (animator.isComplete()) {
            postRevealTimer += amount;
            if (postRevealTimer >= POST_REVEAL_DELAY) {
                dismiss();
            }
        }

        updateLabels();
    }

    private void updateLabels() {
        if (animator == null) return;

        float resultAlpha = animator.getResultTextAlpha();
        if (resultAlpha > 0f && resultLabel != null) {
            boolean playerIsAttacker = animator.isPlayerAttacker();
            String text = playerIsAttacker
                ? Strings.get("coinflip.you_attack_first")
                : Strings.get("coinflip.opponent_attacks_first");
            resultLabel.setText(text);
            resultLabel.setOpacity(resultAlpha);

            Color textColor = playerIsAttacker ? ColorHelper.ATTACK_VALUE : ColorHelper.DEFENSE_VALUE;
            resultLabel.setColor(textColor);
        }

        if (animator.isComplete() && clickHintLabel != null) {
            clickHintLabel.setOpacity(0f);
        }
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (panel == null) return;

        PositionAPI pos = panel.getPosition();
        float x = pos.getX();
        float y = pos.getY();
        float w = pos.getWidth();
        float h = pos.getHeight();

        GLStateUtil.resetBlendState();
        Misc.renderQuad(x, y, w, h, BattleRenderingUtils.COLOR_BG_DARK, alphaMult);

        float boardMargin = 40f;
        Misc.renderQuad(x + boardMargin, y + boardMargin, w - boardMargin * 2f, h - boardMargin * 2f,
            BattleRenderingUtils.COLOR_BG_BOARD, alphaMult * 0.6f);

        if (animator != null) {
            float centerX = x + w / 2f;
            float centerY = y + h / 2f;
            animator.render(centerX, centerY, alphaMult);
        }

        GLStateUtil.resetColor();
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (dismissed || animator == null) return;

        int currentButton = Mouse.isButtonDown(0) ? 1 : 0;

        if (currentButton == 1 && lastMouseButtonState == 0) {
            if (!animator.isComplete()) {
                animator.skip();
                postRevealTimer = POST_REVEAL_DELAY;
            } else {
                dismiss();
            }
        }

        lastMouseButtonState = currentButton;
    }

    private void dismiss() {
        if (dismissed) return;
        dismissed = true;

        cleanupLabels();

        if (callbacks != null) {
            callbacks.dismissDialog();
        }
    }

    private void cleanupLabels() {
        if (flipHintLabel != null) {
            panel.removeComponent((UIComponentAPI) flipHintLabel);
            flipHintLabel = null;
        }
        if (resultLabel != null) {
            panel.removeComponent((UIComponentAPI) resultLabel);
            resultLabel = null;
        }
        if (clickHintLabel != null) {
            panel.removeComponent((UIComponentAPI) clickHintLabel);
            clickHintLabel = null;
        }
    }

    public boolean isPlayerAttacker() {
        return animator != null && animator.isPlayerAttacker();
    }

    public void cleanup() {
        cleanupLabels();
        animator = null;
        panel = null;
        callbacks = null;
    }
}
