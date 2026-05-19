package data.scripts.cosmicon.ai.profiles;

import data.scripts.cosmicon.ai.AttackRerollAI;
import data.scripts.cosmicon.battle.DiceType;
import java.util.List;

public abstract class SimplePassiveAI extends AttackRerollAI {

    protected abstract boolean checkPassiveCondition(List<Integer> selectedValues, boolean isAttacking);

    protected abstract float computePassiveBonus(List<Integer> selectedValues, boolean isAttacking);

    protected abstract String getPassiveDescription(List<Integer> selectedValues, boolean isAttacking);

    @Override
    public final PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues,
            List<DiceType> selectedTypes, boolean isAttacking) {
        if (selectedValues.isEmpty()) return PassiveEvaluation.notTriggered();
        if (checkPassiveCondition(selectedValues, isAttacking)) {
            return PassiveEvaluation.triggered(computePassiveBonus(selectedValues, isAttacking),
                                               getPassiveDescription(selectedValues, isAttacking));
        }
        return PassiveEvaluation.notTriggered();
    }

    @Override
    public final float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        if (selectedValues.isEmpty()) return 0f;
        return checkPassiveCondition(selectedValues, isAttacking)
            ? computePassiveBonus(selectedValues, isAttacking) : 0f;
    }
}
