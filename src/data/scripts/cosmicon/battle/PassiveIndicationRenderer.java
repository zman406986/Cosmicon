package data.scripts.cosmicon.battle;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import data.scripts.cosmicon.util.CharacterPassives;
import data.scripts.cosmicon.util.ColorHelper;
import data.scripts.cosmicon.util.GLStateUtil;
import data.scripts.cosmicon.util.UnifiedCoord;

public class PassiveIndicationRenderer {

    private static final float INDICATOR_INSET = 6f;
    private static final float DASH_LEN = 8f;
    private static final float GAP_LEN = 4f;
    private static final float LINE_WIDTH = 2f;
    private static final Color INDICATOR_COLOR = new Color(255, 220, 100, 180);

    public static void renderIndications(BattleState battleState, DiceRollManager diceRollManager,
                                          float alphaMult) {
        if (battleState == null || diceRollManager == null) return;
        if (battleState.getCurrentPhase() != BattleState.Phase.SELECTING_ATTACK &&
            battleState.getCurrentPhase() != BattleState.Phase.SELECTING_DEFENSE) {
            return;
        }

        boolean playerShouldSelect = (battleState.isAttacker(true) &&
            battleState.getCurrentPhase() == BattleState.Phase.SELECTING_ATTACK) ||
            (battleState.isDefender(true) &&
            battleState.getCurrentPhase() == BattleState.Phase.SELECTING_DEFENSE);

        if (!playerShouldSelect) return;

        CharacterCard card = battleState.getPlayerCard();
        if (card == null) return;

        String characterId = card.getId();
        List<Integer> values = battleState.getPlayerDiceValues();
        List<DiceType> types = battleState.getPlayerDiceTypes();
        List<Boolean> selected = battleState.getPlayerDiceSelected();
        List<DiceAnimator> animators = diceRollManager.getAnimators();

        if (values == null || types == null || selected == null || animators == null) return;
        if (values.size() != types.size() || values.size() != selected.size()) return;

        int currentHp = battleState.getPlayerHp();
        int maxHp = card.getMaxHp();
        int currentToughness = battleState.getPlayerEffects().getLayers(
            StatusEffectProcessor.StatusEffect.TOUGHNESS);
        boolean isAttacking = battleState.isPlayerAttacker();

        List<Integer> currentlySelectedValues = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            if (selected.get(i)) {
                currentlySelectedValues.add(values.get(i));
            }
        }

        GLStateUtil.resetBlendState();
        float[] c = ColorHelper.toGLComponents(INDICATOR_COLOR, alphaMult);
        GL11.glColor4f(c[0], c[1], c[2], c[3]);
        GL11.glLineWidth(LINE_WIDTH);

        for (int i = 0; i < Math.min(values.size(), animators.size()); i++) {
            if (selected.get(i)) continue;

            boolean isPrismatic = types.get(i) == DiceType.PRISMATIC;
            boolean wouldTrigger = CharacterPassives.wouldDiceTriggerPassive(
                characterId, values.get(i), isPrismatic,
                currentlySelectedValues, isAttacking,
                currentHp, maxHp, currentToughness);

            if (!wouldTrigger) continue;

            DiceAnimator animator = animators.get(i);
            if (animator == null) continue;

            float visX = animator.getVisualX();
            float visY = animator.getVisualY();
            float displaySize = animator.getDisplaySize();
            float centeringOffset = (AnimationConstants.DICE_SIZE - displaySize) / 2f;
            float boxX = visX - centeringOffset + INDICATOR_INSET;
            float boxY = visY - centeringOffset + INDICATOR_INSET;
            UnifiedCoord dicePos = new UnifiedCoord(boxX, boxY).bottomLeft(
                AnimationConstants.DICE_SIZE - INDICATOR_INSET * 2f);

            float diceX = dicePos.glX();
            float diceY = dicePos.glY();
            float boxSize = AnimationConstants.DICE_SIZE - INDICATOR_INSET * 2f;

            renderDashedRect(diceX, diceY, boxSize, boxSize, DASH_LEN, GAP_LEN);
        }

        GLStateUtil.resetColor();
    }

    private static void renderDashedRect(float x, float y, float w, float h, float dashLen, float gapLen) {
        BattleRenderingUtils.renderDashedLine(x, y, x + w, y, dashLen, gapLen);
        BattleRenderingUtils.renderDashedLine(x + w, y, x + w, y + h, dashLen, gapLen);
        BattleRenderingUtils.renderDashedLine(x + w, y + h, x, y + h, dashLen, gapLen);
        BattleRenderingUtils.renderDashedLine(x, y + h, x, y, dashLen, gapLen);
    }
}
