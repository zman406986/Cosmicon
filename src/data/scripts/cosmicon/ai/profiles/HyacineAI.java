package data.scripts.cosmicon.ai.profiles;

import data.scripts.Strings;
import data.scripts.cosmicon.ai.AttackRerollAI;
import data.scripts.cosmicon.battle.BattleState;
import data.scripts.cosmicon.battle.DiceType;
import data.scripts.cosmicon.util.CharacterIds;
import data.scripts.cosmicon.util.DiceEvaluator;

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
        Set<Integer> result = findComboForSpecificValue(pool, requiredCount, 6);
        return result != null ? Collections.singletonList(result) : Collections.emptyList();
    }
}
