package data.scripts.cosmicon.battle;

import java.awt.Color;
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
        if (!diceRollManager.isComplete()) return;

        BattleState.Phase phase = battleState.getCurrentPhase();
        if (phase != BattleState.Phase.SELECTING_ATTACK &&
            phase != BattleState.Phase.SELECTING_DEFENSE) {
            return;
        }

        boolean playerShouldSelect = (battleState.isAttacker(true) &&
            phase == BattleState.Phase.SELECTING_ATTACK) ||
            (battleState.isDefender(true) &&
            phase == BattleState.Phase.SELECTING_DEFENSE);

        if (playerShouldSelect) {
            renderIndicationsForSide(battleState, diceRollManager, alphaMult, true);
        } else {
            renderIndicationsForSide(battleState, diceRollManager, alphaMult, false);
        }
    }

    private static void renderIndicationsForSide(BattleState battleState, DiceRollManager diceRollManager,
                                                   float alphaMult, boolean forPlayer) {
        CharacterCard card = forPlayer ? battleState.getPlayerCard() : battleState.getOpponentCard();
        if (card == null) return;

        String characterId = card.getId();
        List<Integer> values = forPlayer ? battleState.getPlayerDiceValues() : battleState.getOpponentDiceValues();
        List<DiceType> types = forPlayer ? battleState.getPlayerDiceTypes() : battleState.getOpponentDiceTypes();
        List<DiceAnimator> animators = forPlayer ? diceRollManager.getAnimators() : diceRollManager.getOpponentAnimators();

        if (values == null || types == null || animators == null) return;
        if (values.size() != types.size()) return;

        int currentHp = forPlayer ? battleState.getPlayerHp() : battleState.getOpponentHp();
        int maxHp = card.getMaxHp();
        int currentToughness = (forPlayer ? battleState.getPlayerEffects() : battleState.getOpponentEffects())
            .getLayers(StatusEffectProcessor.StatusEffect.TOUGHNESS);
        boolean isAttacking = battleState.isAttacker(forPlayer);

        GLStateUtil.resetBlendState();
        float[] c = ColorHelper.toGLComponents(INDICATOR_COLOR, alphaMult);
        GL11.glColor4f(c[0], c[1], c[2], c[3]);
        GL11.glLineWidth(LINE_WIDTH);

        for (int i = 0; i < Math.min(values.size(), animators.size()); i++) {
            boolean isPrismatic = types.get(i) == DiceType.PRISMATIC;
            boolean isIndicative = CharacterPassives.isDieIndicativeForPassive(
                characterId, values.get(i), isPrismatic,
                values, isAttacking,
                currentHp, maxHp, currentToughness);

            if (!isIndicative) continue;

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
