package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.ai.AttackRerollAI;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.DiceEvaluator;

import java.util.*;

public class DanHengAI extends AttackRerollAI {

    @Override
    public String getCharacterId() {
        return CharacterIds.DAN_HENG;
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.dan_heng.name");
    }

    @Override
    public int getTargetThreshold(boolean isAttacking) {
        return isAttacking ? 18 : 10;
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking) {
        if (selectedValues == null || selectedValues.isEmpty()) return PassiveEvaluation.notTriggered();
        return DiceEvaluator.sumAtLeast(selectedValues, 18)
            ? PassiveEvaluation.triggered(10f, Strings.get("character.dan_heng.passive_desc"))
            : PassiveEvaluation.notTriggered();
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        if (selectedValues == null || selectedValues.isEmpty()) return 0f;
        return DiceEvaluator.sumAtLeast(selectedValues, 18) ? 10f : 0f;
    }

    @Override
    protected SubsetEvaluator getSubsetEvaluator() {
        return (selectedValues, selectedTypes, isAttacking, state, forPlayer) -> {
            int sum = DiceEvaluator.sumOfValues(selectedValues);
            if (!isAttacking) return sum;
            float bonus = DiceEvaluator.sumAtLeast(selectedValues, 18) ? 10f : 0f;
            if (bonus == 0f && sum >= 14) {
                bonus = (sum - 14) * 0.5f;
            }
            return sum + bonus;
        };
    }
}
