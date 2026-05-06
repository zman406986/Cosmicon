package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.ai.AttackRerollAI;
import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.DiceEvaluator;
import data.scripts.cosmicon.util.PassiveEvaluator;
import data.scripts.cosmicon.util.PassiveResults.PassiveResult;
import java.util.*;

public class HyacineAI extends AttackRerollAI {

    @Override
    public String getCharacterId() {
        return CharacterIds.HYACINE;
    }

    @Override
    public String getCharacterName() {
        return Strings.get("character.hyacine.name");
    }

    @Override
    public PassiveEvaluation evaluatePassiveTrigger(List<Integer> selectedValues, List<DiceType> selectedTypes, boolean isAttacking) {
        if (selectedValues == null || selectedValues.isEmpty()) return PassiveEvaluation.notTriggered();
        if (DiceEvaluator.allDiceEqualSix(selectedValues)) {
            return PassiveEvaluation.triggered(DiceEvaluator.sumOfValues(selectedValues) + 6f,
                Strings.get("character.hyacine.passive_desc_full"));
        }
        return PassiveEvaluation.potential(0.5f, DiceEvaluator.sumOfValues(selectedValues) * 0.5f,
            Strings.get("character.hyacine.passive_desc_half"));
    }

    @Override
    public float getPassiveBonusValue(List<Integer> selectedValues, boolean isAttacking) {
        if (selectedValues == null || selectedValues.isEmpty()) return 0f;
        int sum = DiceEvaluator.sumOfValues(selectedValues);
        if (DiceEvaluator.allDiceEqualSix(selectedValues)) return sum + 6f;
        return sum * 0.5f;
    }

    @Override
    protected List<Set<Integer>> generateComboCandidates(SimPool pool, int rerollsLeft, int requiredCount,
                                                          boolean isAttacking, BattleState state, boolean forPlayer) {
        List<Integer> nonSixIndices = new ArrayList<>();
        int sixCount = 0;
        for (int i = 0; i < pool.size(); i++) {
            if (pool.getValue(i) == 6) sixCount++;
            else nonSixIndices.add(i);
        }
        if (sixCount >= requiredCount - 1 && sixCount < requiredCount && !nonSixIndices.isEmpty()) {
            return Collections.singletonList(new HashSet<>(nonSixIndices));
        }
        return Collections.emptyList();
    }
}
