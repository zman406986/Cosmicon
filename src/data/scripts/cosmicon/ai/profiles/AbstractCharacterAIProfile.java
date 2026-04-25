package data.scripts.cosmicon.ai.profiles;

import data.scripts.cosmicon.ai.CharacterAIProfile;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.util.PassiveEvaluator;
import data.scripts.cosmicon.util.PassiveResults.PassiveResult;
import java.util.List;

public abstract class AbstractCharacterAIProfile implements CharacterAIProfile {

    @Override
    public boolean prefersHighValues(boolean isAttacking) {
        return true;
    }

    @Override
    public boolean shouldOptimizeForPassive(boolean isAttacking) {
        return isAttacking;
    }

    @Override
    public boolean isAttackPassive() {
        return CharacterAIProfile.super.isAttackPassive();
    }

    protected boolean hasValidValues(List<Integer> selectedValues) {
        return selectedValues == null || selectedValues.isEmpty();
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking) {
        if (hasValidValues(selectedValues)) {
            return PassiveEvaluation.notTriggered();
        }

        PassiveResult result = PassiveEvaluator.evaluateForCharacter(getCharacterId(), selectedValues, isAttacking);
        return PassiveEvaluator.toPassiveEvaluation(result, getPassiveDescription(selectedValues));
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        if (hasValidValues(selectedValues)) return 0f;
        return calculatePassiveBonus(selectedValues);
    }

    protected String getPassiveDescription(List<Integer> selectedValues) {
        return "";
    }

    protected abstract float calculatePassiveBonus(List<Integer> selectedValues);
}