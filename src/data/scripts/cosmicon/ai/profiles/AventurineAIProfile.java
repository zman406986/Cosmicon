package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.StatusEffectProcessor.StatusEffect;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.PassiveEvaluator;
import java.util.List;

public class AventurineAIProfile extends AbstractCharacterAIProfile {

    private static final int TOUGHNESS_BURST_THRESHOLD = 7;
    private static final float BURST_DAMAGE_VALUE = 7f;

    @Override
    public String getCharacterId() {
        return CharacterIds.AVENTURINE;
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.aventurine.name");
    }

    @Override
    public boolean shouldOptimizeForPassive(boolean isAttacking) {
        return true;
    }

    @Override
    protected String getPassiveDescription(List<Integer> selectedValues) {
        int oddCount = PassiveEvaluator.countOddNumbers(selectedValues);
        return Strings.format("character.aventurine.passive_desc", oddCount);
    }

    @Override
    protected float calculatePassiveBonus(List<Integer> selectedValues) {
        float bonus = PassiveEvaluator.countOddNumbers(selectedValues);
        bonus += 7f;
        return bonus;
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking,
                                       BattleState state, boolean forPlayer) {
        if (state == null) return getPassiveBonusValue(selectedValues, isAttacking);

        int currentToughness = state.getEffects(forPlayer).getLayers(StatusEffect.TOUGHNESS);
        int oddCount = PassiveEvaluator.countOddNumbers(selectedValues);
        int newTotal = currentToughness + oddCount;

        if (newTotal >= TOUGHNESS_BURST_THRESHOLD) {
            int remainder = newTotal - TOUGHNESS_BURST_THRESHOLD;
            return BURST_DAMAGE_VALUE + remainder;
        }
        return oddCount;
    }
}