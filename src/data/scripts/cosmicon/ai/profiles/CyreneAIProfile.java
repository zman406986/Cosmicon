package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.PassiveEvaluator;
import java.util.List;

public class CyreneAIProfile extends AbstractCharacterAIProfile {

    @Override
    public String getCharacterId() {
        return CharacterIds.CYRENE;
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
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking, BattleState state, boolean forPlayer) {
        if (selectedValues == null || selectedValues.isEmpty()) {
            return PassiveEvaluation.notTriggered();
        }

        int currentSum = PassiveEvaluator.sumOfValues(selectedValues);
        int cumulative = state.getCumulativeAtkDef(forPlayer);
        int projectedTotal = cumulative + currentSum;
        boolean alreadyMet = state.isCyreneThresholdMet(forPlayer);

        if (!alreadyMet && projectedTotal > 24) {
            float bonus = calculateCumulativeBonus(projectedTotal) + 20f;
            return PassiveEvaluation.triggered(bonus, 
                Strings.format("character.cyrene.passive_progress", projectedTotal));
        }

        if (alreadyMet) {
            float bonus = calculateCumulativeBonus(currentSum) + 10f;
            return PassiveEvaluation.potential(1f, bonus,
                Strings.format("character.cyrene.passive_progress", currentSum));
        }

        float bonus = calculateCumulativeBonus(currentSum);
        return PassiveEvaluation.potential(1f, bonus,
            Strings.format("character.cyrene.passive_progress", projectedTotal));
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
        if (hasNoValues(selectedValues)) return 0f;
        int sum = PassiveEvaluator.sumOfValues(selectedValues);
        return calculateCumulativeBonus(sum);
    }
}