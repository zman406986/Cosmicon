package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.util.PassiveEvaluator;
import data.scripts.cosmicon.util.PassiveEvaluator.PassiveResult;
import java.util.List;

public class SparxieAIProfile extends AbstractCharacterAIProfile {

    private static final float HACK_BONUS_VALUE = 5f;

    @Override
    public String getCharacterId() {
        return "sparxie";
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.sparxie.name");
    }

    @Override
    public boolean shouldOptimizeForPassive(boolean isAttacking) {
        return true;
    }

    @Override
    public float getRiskTolerance()
    {
        return super.getRiskTolerance();
    }

    @Override
    public int getTargetThreshold(boolean isAttacking)
    {
        return super.getTargetThreshold(isAttacking);
    }

    @Override
    public boolean isDefensePassive() {
        return true;
    }

    @Override
    public boolean isAttackPassive()
    {
        return super.isAttackPassive();
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking) {
        if (selectedValues == null || selectedValues.isEmpty()) {
            return PassiveEvaluation.notTriggered();
        }

        PassiveResult result = PassiveEvaluator.evaluateForCharacter("sparxie", selectedValues, isAttacking);

        if (PassiveEvaluator.hasIdenticalNumbers(selectedValues)) {
            return PassiveEvaluator.toPassiveEvaluation(result,
                Strings.get("character.sparxie.passive_desc"));
        }

        return PassiveEvaluation.notTriggered();
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        if (selectedValues == null || selectedValues.isEmpty()) return 0f;
        return PassiveEvaluator.hasIdenticalNumbers(selectedValues) ? HACK_BONUS_VALUE : 0f;
    }

    @Override
    protected float calculatePassiveBonus(List<Integer> selectedValues) {
        if (selectedValues == null || selectedValues.isEmpty()) return 0f;
        return PassiveEvaluator.hasIdenticalNumbers(selectedValues) ? HACK_BONUS_VALUE : 0f;
    }
}