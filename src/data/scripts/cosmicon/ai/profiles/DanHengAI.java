package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.DiceEvaluator;

import java.util.*;

public class DanHengAI extends SimplePassiveAI {

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
    protected boolean checkPassiveCondition(List<Integer> selectedValues, boolean isAttacking) {
        return DiceEvaluator.sumAtLeast(selectedValues, 18);
    }

    @Override
    protected float computePassiveBonus(List<Integer> selectedValues, boolean isAttacking) {
        return 10f;
    }

    @Override
    protected String getPassiveDescription(List<Integer> selectedValues, boolean isAttacking) {
        return Strings.get("character.dan_heng.passive_desc");
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
