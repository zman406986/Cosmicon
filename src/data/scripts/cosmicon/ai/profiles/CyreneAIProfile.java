package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.util.PassiveEvaluator;
import java.util.List;

public class CyreneAIProfile extends AbstractCharacterAIProfile {

    private static final int CUMULATIVE_THRESHOLD = 24;

    @Override
    public String getCharacterId() {
        return "cyrene";
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.cyrene.name");
    }

    @Override
    public float getRiskTolerance() {
        return 0.7f;
    }

    @Override
    public int getTargetThreshold(boolean isAttacking) {
        return isAttacking ? 16 : 12;
    }

    @Override
    public boolean shouldOptimizeForPassive(boolean isAttacking) {
        return true;
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking) {
        if (selectedValues == null || selectedValues.isEmpty()) {
            return PassiveEvaluation.notTriggered();
        }

        int sum = PassiveEvaluator.sumOfValues(selectedValues);
        float bonus = calculateCumulativeBonus(sum);
        
        return PassiveEvaluation.potential(1f, bonus, 
            Strings.format("character.cyrene.passive_progress", sum));
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        if (selectedValues == null || selectedValues.isEmpty()) return 0f;
        
        int sum = PassiveEvaluator.sumOfValues(selectedValues);
        return calculateCumulativeBonus(sum);
    }

    private float calculateCumulativeBonus(int turnValue) {
        float bonus = turnValue * 0.5f;
        
        if (turnValue >= 20) {
            bonus += 10f;
        }
        
        return bonus;
    }

    @Override
    protected float calculatePassiveBonus(List<Integer> selectedValues) {
        if (selectedValues == null || selectedValues.isEmpty()) return 0f;
        int sum = PassiveEvaluator.sumOfValues(selectedValues);
        return calculateCumulativeBonus(sum);
    }
}